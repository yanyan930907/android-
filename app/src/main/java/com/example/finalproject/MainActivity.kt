package com.example.finalproject

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.finalproject.ui.LoginScreen
import com.example.finalproject.ui.theme.FinalProjectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinalProjectTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LoginScreen(
                        modifier = Modifier.padding(innerPadding),
                        onLoginClick = { email, password ->
                            Log.d("Login", "Email: $email, Password: $password")
                            // 這裡實作登入邏輯
                        },
                        onGoogleLoginClick = {
                            Log.d("Login", "Google Login Clicked")
                            // 這裡實作 Google 登入邏輯
                        },
                        onForgotPasswordClick = {
                            Log.d("Login", "Forgot Password Clicked")
                        },
                        onSignUpClick = {
                            Log.d("Login", "Sign Up Clicked")
                        }
                    )
                }
            }
        }
    }
}
