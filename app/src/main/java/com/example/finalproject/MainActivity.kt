package com.example.finalproject

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.finalproject.ui.LoginScreen
import com.example.finalproject.ui.theme.FinalProjectTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase Auth
        auth = Firebase.auth
        
        enableEdgeToEdge()
        setContent {
            FinalProjectTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LoginScreen(
                        modifier = Modifier.padding(innerPadding),
                        onLoginClick = { email, password ->
                            if (email.isNotEmpty() && password.isNotEmpty()) {
                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener(this) { task ->
                                        if (task.isSuccessful) {
                                            Log.d("Login", "signInWithEmail:success")
                                            Toast.makeText(this, "登入成功", Toast.LENGTH_SHORT).show()
                                            // Handle successful login (e.g., navigate to home)
                                        } else {
                                            Log.w("Login", "signInWithEmail:failure", task.exception)
                                            Toast.makeText(this, "登入失敗: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            } else {
                                Toast.makeText(this, "請輸入電子郵件與密碼", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onGoogleLoginClick = {
                            Log.d("Login", "Google Login Clicked")
                            Toast.makeText(this, "Google 登入功能需額外設定 Credentials API", Toast.LENGTH_SHORT).show()
                        },
                        onForgotPasswordClick = {
                            Log.d("Login", "Forgot Password Clicked")
                        },
                        onSignUpClick = {
                            Log.d("Login", "Sign Up Clicked")
                            // Navigate to registration screen or handle registration
                        }
                    )
                }
            }
        }
    }
}
