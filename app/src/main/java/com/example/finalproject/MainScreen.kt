package com.example.finalproject

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val items = listOf(
        Screen.Home,
        Screen.Focus,
        Screen.Groups,
        Screen.Settings
    )

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
                is Screen.Focus -> Text("專注功能開發中...")
                is Screen.Groups -> Text("群組功能開發中...")
                is Screen.Settings -> SettingsTabContent(onLogout)
            }
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
