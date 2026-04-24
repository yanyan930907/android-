package com.example.finalproject

// 👇 幫你補齊了所有需要的 Import
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

// 👇 1. 直接放在檔案最外層，不要包在 class 裡面
// 裝載排行榜成員資料的暫存類別
data class RankedMember(val uid: String, val name: String, val totalTime: Long)

// 👇 2. Compose 函式也是直接放在最外層
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(group: Group, onBackClick: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val rankedMembers = remember { mutableStateListOf<RankedMember>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(group.id) {
        if (group.memberIds.isNotEmpty()) {
            db.collection("users")
                .whereIn("uid", group.memberIds.take(10)) // Firestore whereIn 限制最多 10 個
                .get()
                .addOnSuccessListener { snapshot ->
                    val tempList = mutableListOf<RankedMember>()
                    for (doc in snapshot.documents) {
                        tempList.add(
                            RankedMember(
                                uid = doc.getString("uid") ?: "",
                                name = doc.getString("username") ?: "匿名",
                                totalTime = doc.getLong("totalFocusTime") ?: 0L
                            )
                        )
                    }
                    // 在本地端依照專注時間由高到低排序
                    tempList.sortByDescending { it.totalTime }
                    rankedMembers.clear()
                    rankedMembers.addAll(tempList)
                    isLoading = false
                }
                .addOnFailureListener { isLoading = false }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🏆 群組排行榜", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (rankedMembers.isEmpty()) {
                Text("目前沒有資料", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    itemsIndexed(rankedMembers) { index, member ->
                        val rank = index + 1

                        // 判斷前三名的特殊視覺效果
                        val cardColor = when (rank) {
                            1 -> Color(0xFFFFF8E1) // 淡淡的金色背景
                            2 -> Color(0xFFF5F5F5) // 淡淡的銀色背景
                            3 -> Color(0xFFFFF0E6) // 淡淡的銅色背景
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }

                        val rankIconColor = when (rank) {
                            1 -> Color(0xFFFFD700) // 金色
                            2 -> Color(0xFFC0C0C0) // 銀色
                            3 -> Color(0xFFCD7F32) // 銅色
                            else -> Color.Transparent
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = cardColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (rank <= 3) 4.dp else 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 排名顯示區塊
                                Box(
                                    modifier = Modifier.size(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (rank <= 3) {
                                        Icon(Icons.Default.Star, contentDescription = "Top 3", tint = rankIconColor, modifier = Modifier.size(36.dp))
                                        Text("$rank", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color.White)
                                    } else {
                                        Text("$rank", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Gray)
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // 名字與時間
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = member.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }

                                // 專注時間格式化 (將秒數轉為 分:秒)
                                val mins = member.totalTime / 60
                                val secs = member.totalTime % 60
                                val timeString = String.format("%02d:%02d", mins, secs)

                                Text(
                                    text = timeString,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = if (rank <= 3) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}