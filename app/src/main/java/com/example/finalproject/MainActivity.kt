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
import androidx.compose.ui.unit.dp
import com.example.finalproject.ui.theme.FinalProjectTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinalProjectTheme {
                val auth = FirebaseAuth.getInstance()
                // 觀察目前的畫面狀態：login, signup, 或已登入(home)
                var currentScreen by remember { 
                    mutableStateOf(if (auth.currentUser != null) "home" else "login") 
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen) {
                        "login" -> {
                            LoginScreen(
                                onLoginSuccess = { currentScreen = "home" },
                                onNavigateToSignUp = { currentScreen = "signup" }
                            )
                        }
                        "signup" -> {
                            SignUpScreen(
                                onSignUpSuccess = { currentScreen = "home" },
                                onBackToLogin = { currentScreen = "login" }
                            )
                        }
                        "home" -> {
                            // 恢復之前版本的 MainAppContent
                            MainAppContent(
                                userEmail = auth.currentUser?.email ?: "使用者",
                                onLogout = {
                                    auth.signOut()
                                    currentScreen = "login"
                                }
                            )
                        }
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
