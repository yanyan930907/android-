package com.example.finalproject

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
                tonalElevation = 0.dp
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
                            indicatorColor = Color.Transparent
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
    val mainColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
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
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = {
                        viewModel.onTypeChange(option)
                        expanded = false
                    })
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = viewModel.formatTime(viewModel.timerSeconds),
                fontSize = 88.sp,
                fontWeight = FontWeight.W200,
                fontFamily = FontFamily.Monospace
            )
            Box(modifier = Modifier.width(60.dp).height(2.dp).background(mainColor.copy(alpha = 0.3f)))
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
    var username by remember { mutableStateOf("載入中...") }
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(uid) {
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    username = doc.getString("username") ?: "使用者"
                }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "歡迎回來，$username", style = MaterialTheme.typography.headlineSmall)
        Text(text = userEmail, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
    }
}

@Composable
fun SettingsTabContent(onLogout: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val user = auth.currentUser

    var showNameDialog by remember { mutableStateOf(false) }
    var showPwdDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // 修改暱稱按鈕
        OutlinedButton(
            onClick = { showNameDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("修改暱稱")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 修改密碼按鈕
        OutlinedButton(
            onClick = { showPwdDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("修改密碼")
        }

        Spacer(modifier = Modifier.weight(1f))

        // 登出按鈕
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5F5F5), contentColor = Color.Red),
            elevation = null
        ) {
            Text("登出帳號")
        }
    }

    // 修改暱稱彈窗
    if (showNameDialog) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("修改暱稱") },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("新暱稱") })
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank() && user != null) {
                        db.collection("users").document(user.uid).update("username", newName)
                            .addOnSuccessListener {
                                Toast.makeText(context, "暱稱已更新", Toast.LENGTH_SHORT).show()
                                showNameDialog = false
                            }
                    }
                }) { Text("確定") }
            },
            dismissButton = { TextButton(onClick = { showNameDialog = false }) { Text("取消") } }
        )
    }

    // 修改密碼彈窗
    if (showPwdDialog) {
        var oldPwd by remember { mutableStateOf("") }
        var newPwd by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPwdDialog = false },
            title = { Text("修改密碼") },
            text = {
                Column {
                    OutlinedTextField(
                        value = oldPwd, onValueChange = { oldPwd = it },
                        label = { Text("原密碼") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPwd, onValueChange = { newPwd = it },
                        label = { Text("新密碼 (至少6碼)") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newPwd.length >= 6 && user?.email != null) {
                        val credential = EmailAuthProvider.getCredential(user.email!!, oldPwd)
                        user.reauthenticate(credential).addOnCompleteListener { reAuthTask ->
                            if (reAuthTask.isSuccessful) {
                                user.updatePassword(newPwd).addOnSuccessListener {
                                    Toast.makeText(context, "密碼已更新", Toast.LENGTH_SHORT).show()
                                    showPwdDialog = false
                                }.addOnFailureListener {
                                    Toast.makeText(context, "更新失敗: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "原密碼錯誤", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "新密碼格式不正確", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("確定") }
            },
            dismissButton = { TextButton(onClick = { showPwdDialog = false }) { Text("取消") } }
        )
    }
}
