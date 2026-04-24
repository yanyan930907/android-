package com.example.finalproject

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finalproject.ui.theme.FinalProjectTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        } else {
            Toast.makeText(this, "需要通知權限才能接收呼叫", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 設定初始語言為繁體中文 (如果尚未設定)
        // 注意：AppCompatDelegate.getApplicationLocales() 會讀取之前設定的值
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("zh-TW")
            AppCompatDelegate.setApplicationLocales(appLocale)
        }

        enableEdgeToEdge()
        
        askNotificationPermission()
        updateFcmToken()

        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            FinalProjectTheme(darkTheme = themeViewModel.isDarkMode) {
                val auth = FirebaseAuth.getInstance()
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
                                onLoginSuccess = { 
                                    currentScreen = "home"
                                    updateFcmToken()
                                },
                                onNavigateToSignUp = { currentScreen = "signup" }
                            )
                        }
                        "signup" -> {
                            SignUpScreen(
                                onSignUpSuccess = { 
                                    currentScreen = "home"
                                    updateFcmToken()
                                },
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

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun updateFcmToken() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .update("fcmToken", token)
            }
        }
    }
}
