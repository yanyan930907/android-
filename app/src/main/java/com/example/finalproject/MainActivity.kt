package com.example.finalproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
