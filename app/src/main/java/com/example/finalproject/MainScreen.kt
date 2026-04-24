package com.example.finalproject

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
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

    LaunchedEffect(selectedScreen) {
        if (selectedScreen != Screen.Focus && focusViewModel.isRunning) {
            focusViewModel.pauseTimer()
        }
    }

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
            if (selectedScreen != Screen.Groups) {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            text = selectedScreen.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ) 
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp // 移除陰影，追求扁平化
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
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            indicatorColor = Color.Transparent // 移除選中背景，追求簡約
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        val modifier = if (selectedScreen == Screen.Groups) {
            Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())
        } else {
            Modifier.fillMaxSize().padding(innerPadding)
        }

        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            when (selectedScreen) {
                is Screen.Home -> HomeTabContent(userEmail)
                is Screen.Focus -> FocusTabContent(focusViewModel)
                is Screen.Groups -> GroupMainScreen(onBackToHome = { selectedScreen = Screen.Home })
                is Screen.Settings -> SettingsTabContent(onLogout)
            }
        }
    }
}

@Composable
fun FocusTabContent(viewModel: FocusViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("工作", "學習", "運動", "休息", "閱讀")
    
    // 統一使用純色主題
    val mainColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. 類型選擇器 - 極簡純色線條
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color.LightGray), RoundedCornerShape(12.dp))
                .clickable(enabled = !viewModel.isRunning && viewModel.timerSeconds == 0) { expanded = true }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val isInteractionDisabled = viewModel.isRunning || viewModel.timerSeconds > 0
                Text(
                    text = viewModel.selectedType,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isInteractionDisabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    Icons.Default.ArrowDropDown, 
                    contentDescription = null, 
                    tint = if (isInteractionDisabled) Color.Gray.copy(alpha = 0.38f) else Color.Gray
                )
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

        // 2. 計時器數字 - 純粹的大數字
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = viewModel.formatTime(viewModel.timerSeconds),
                fontSize = 88.sp,
                fontWeight = FontWeight.W200, // 極細字體，追求大方
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
            // 裝飾性線條
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(2.dp)
                    .background(mainColor.copy(alpha = 0.3f))
            )
        }

        // 3. 按鈕區域
        if (viewModel.isRunning) {
            // 執行中：顯示 STOP 鍵
            Surface(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .clickable { viewModel.toggleTimer() },
                color = Color.Red
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "STOP",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = Color.White
                    )
                }
            }
        } else if (viewModel.timerSeconds > 0) {
            // 暫停中：顯示 CONTINUE 與 COMPLETE 鍵
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // CONTINUE 鍵
                Surface(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.startTimer() },
                    color = mainColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "CONTINUE",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // COMPLETE 鍵
                Surface(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.completeTimer() },
                    color = Color.Gray // 使用灰色或另一個顏色區分
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "COMPLETE",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            // 未開始：顯示 START 鍵
            Surface(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .clickable { viewModel.toggleTimer() },
                color = mainColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "START",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = Color.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun HomeTabContent(userEmail: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "歡迎回來", style = MaterialTheme.typography.headlineSmall)
        Text(text = userEmail, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
    }
}

@Composable
fun SettingsTabContent(onLogout: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onLogout,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5F5F5), contentColor = Color.Red),
            elevation = null
        ) {
            Text("登出帳號")
        }
    }
}
