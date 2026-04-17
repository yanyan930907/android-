package com.example.finalproject

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home_tab", "主頁", Icons.Default.Home)
    object Focus : Screen("focus_tab", "專注", Icons.Default.SelfImprovement)
    object Groups : Screen("groups_tab", "群組", Icons.Default.Groups)
    object Settings : Screen("settings_tab", "設定", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(userEmail: String, onLogout: () -> Unit) {
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val focusViewModel: FocusViewModel = viewModel()
    
    val items = listOf(
        Screen.Home,
        Screen.Focus,
        Screen.Groups,
        Screen.Settings
    )

    // Handle pausing when leaving the Focus tab
    LaunchedEffect(selectedScreen) {
        if (selectedScreen != Screen.Focus && focusViewModel.isRunning) {
            focusViewModel.pauseTimer()
        } else if (selectedScreen == Screen.Focus && !focusViewModel.isRunning && focusViewModel.timerSeconds > 0) {
            // Optional: Auto-resume when coming back? 
            // The user said "回來時可以繼續計時" (can continue timing when coming back).
            // Usually this means if it was running, it resumes.
            // But let's check if it was paused because of leaving the screen.
        }
    }

    // Handle Lifecycle (e.g. going to background)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                if (focusViewModel.isRunning) {
                    focusViewModel.pauseTimer()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = selectedScreen.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = selectedScreen == screen,
                        onClick = { selectedScreen = screen },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (selectedScreen) {
                is Screen.Home -> HomeTabContent(userEmail)
                is Screen.Focus -> FocusTabContent(focusViewModel)
                is Screen.Groups -> Text("群組功能開發中...")
                is Screen.Settings -> SettingsTabContent(onLogout)
            }
        }
    }
}

@Composable
fun FocusTabContent(viewModel: FocusViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("工作", "學習", "運動", "休息")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Type Selection Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "類型", fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(16.dp))
            Box {
                OutlinedCard(
                    onClick = { expanded = true },
                    modifier = Modifier.width(120.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = viewModel.selectedType)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                viewModel.onTypeChange(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        // Timer Display
        Text(
            text = viewModel.formatTime(viewModel.timerSeconds),
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Start/Stop Button
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(if (viewModel.isRunning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer)
                .clickable { viewModel.toggleTimer() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (viewModel.isRunning) "Stop" else "Start",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = if (viewModel.isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun HomeTabContent(userEmail: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "歡迎回來！", style = MaterialTheme.typography.headlineSmall)
        Text(text = userEmail, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SettingsTabContent(onLogout: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("登出帳號")
        }
    }
}
