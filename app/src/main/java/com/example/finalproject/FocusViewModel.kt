package com.example.finalproject

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

class FocusViewModel : ViewModel() {
    var timerSeconds by mutableStateOf(0)
        private set
    
    var isRunning by mutableStateOf(false)
        private set
    
    var selectedType by mutableStateOf("工作")
        private set
    
    private var timerJob: Job? = null
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun onTypeChange(newType: String) {
        selectedType = newType
    }

    fun toggleTimer() {
        if (isRunning) {
            pauseTimer()
            saveRecordToFirebase()
        } else {
            startTimer()
        }
    }

    fun startTimer() {
        if (isRunning) return
        isRunning = true
        timerJob = viewModelScope.launch {
            while (isRunning) {
                delay(1000)
                timerSeconds++
            }
        }
    }

    fun pauseTimer() {
        isRunning = false
        timerJob?.cancel()
    }

    private fun saveRecordToFirebase() {
        val userId = auth.currentUser?.uid ?: return
        val duration = timerSeconds
        if (duration <= 0) return

        val record = hashMapOf(
            "type" to selectedType,
            "durationSeconds" to duration,
            "timestamp" to Date(),
            "userId" to userId
        )

        db.collection("focus_records")
            .add(record)
            .addOnSuccessListener {
                // Optionally reset timer after saving, or keep it. 
                // The user said "continue timing" when coming back, 
                // so maybe I shouldn't reset it automatically until they stop it manually?
                // But usually, stopping saves a session.
                timerSeconds = 0 
            }
    }

    fun formatTime(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(mins, secs)
    }
}
