package com.example.finalproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp // 👉 解決 'dp' 報錯的關鍵匯入
import com.example.finalproject.ui.theme.FinalProjectTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinalProjectTheme {
                val auth = FirebaseAuth.getInstance()
                // 觀察登入狀態
                var isUserLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isUserLoggedIn) {
                        // 登入成功：顯示主功能
                        MainAppContent(
                            userEmail = auth.currentUser?.email ?: "使用者",
                            onLogout = {
                                auth.signOut()
                                isUserLoggedIn = false
                            }
                        )
                    } else {
                        // 尚未登入：顯示登入頁面
                        LoginScreen(onLoginSuccess = {
                            isUserLoggedIn = true
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContent(userEmail: String, onLogout: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "歡迎回來！", style = MaterialTheme.typography.headlineMedium)
        Text(text = userEmail, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onLogout) {
            Text("登出帳號")
        }
    }
}