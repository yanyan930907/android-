package com.example.finalproject

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight

// 1. 群組與成員資料結構
data class Group(val id: String, val name: String, val memberCount: Int, val memberIds: List<String> = emptyList())
data class Member(val uid: String, val name: String, val fcmToken: String?)

// 2. 群組總管
@Composable
fun GroupMainScreen(onBackToHome: () -> Unit) {
    var selectedGroup by remember { mutableStateOf<Group?>(null) }

    if (selectedGroup == null) {
        GroupListScreen(
            onGroupClick = { clickedGroup -> selectedGroup = clickedGroup },
            onBackToHome = onBackToHome
        )
    } else {
        GroupDetailScreen(
            group = selectedGroup!!,
            onBackClick = { selectedGroup = null }
        )
    }
}

// 3. 群組列表畫面
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(onGroupClick: (Group) -> Unit, onBackToHome: () -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    val realGroups = remember { mutableStateListOf<Group>() }

    DisposableEffect(currentUser?.uid) {
        var listener: ListenerRegistration? = null
        if (currentUser != null) {
            listener = db.collection("groups")
                .whereArrayContains("members", currentUser.uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) return@addSnapshotListener
                    if (snapshot != null) {
                        realGroups.clear()
                        for (doc in snapshot.documents) {
                            val id = doc.getString("groupId") ?: doc.id
                            val name = doc.getString("groupName") ?: "未知群組"
                            @Suppress("UNCHECKED_CAST")
                            val members = doc.get("members") as? List<String> ?: emptyList()
                            realGroups.add(Group(id, name, members.size, members))
                        }
                    }
                }
        }
        onDispose { listener?.remove() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的群組", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackToHome) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "回首頁")
                    }
                },
                actions = {
                    TextButton(onClick = { showJoinDialog = true }) {
                        Text("加入", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "新增群組", tint = Color.White)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)
        ) {
            if (realGroups.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("目前沒有群組，點擊右下角建立一個吧！", color = Color.Gray)
                    }
                }
            } else {
                items(realGroups) { group ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onGroupClick(group) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = group.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(text = "${group.memberCount} 位成員", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                        }
                    }
                }
            }
        }

        if (showCreateDialog) {
            CreateGroupDialog(onDismiss = { showCreateDialog = false }, onCreate = { name ->
                val uid = currentUser?.uid ?: return@CreateGroupDialog
                val groupRef = db.collection("groups").document()
                val data = hashMapOf(
                    "groupId" to groupRef.id,
                    "groupName" to name,
                    "members" to listOf(uid),
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                groupRef.set(data).addOnSuccessListener { showCreateDialog = false }
            })
        }

        if (showJoinDialog) {
            JoinGroupDialog(onDismiss = { showJoinDialog = false }, onJoin = { code ->
                val uid = currentUser?.uid ?: return@JoinGroupDialog
                db.collection("groups").document(code).update("members", FieldValue.arrayUnion(uid))
                    .addOnSuccessListener { showJoinDialog = false }
                    .addOnFailureListener { Toast.makeText(context, "加入失敗", Toast.LENGTH_SHORT).show() }
            })
        }
    }
}

// 4. 群組詳細頁面 (包含成員列表與呼叫起床按鈕)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(group: Group, onBackClick: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val members = remember { mutableStateListOf<Member>() }
    var isLoading by remember { mutableStateOf(true) }

    // 取得成員詳細資料
    LaunchedEffect(group.id) {
        if (group.memberIds.isNotEmpty()) {
            db.collection("users")
                .whereIn("uid", group.memberIds.take(10)) // Firestore whereIn 限制 10 個
                .get()
                .addOnSuccessListener { snapshot ->
                    members.clear()
                    for (doc in snapshot.documents) {
                        members.add(Member(
                            uid = doc.getString("uid") ?: "",
                            name = doc.getString("username") ?: "匿名成員",
                            fcmToken = doc.getString("fcmToken")
                        ))
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 邀請碼區塊
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    clipboardManager.setText(AnnotatedString(group.id))
                    Toast.makeText(context, "邀請碼已複製", Toast.LENGTH_SHORT).show()
                },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("群組邀請碼", style = MaterialTheme.typography.labelSmall)
                        Text(group.id, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("成員列表", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            
            // 成員列表
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(members) { member ->
                            ListItem(
                                headlineContent = { Text(member.name) },
                                leadingContent = {
                                    Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                                        Text(member.name.take(1).uppercase(), fontWeight = FontWeight.Bold)
                                    }
                                },
                                trailingContent = {
                                    if (member.fcmToken != null) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "已連線", tint = Color.Green, modifier = Modifier.size(16.dp))
                                    }
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        }
                    }
                }
            }

            // 呼叫起床按鈕
            Button(
                onClick = {
                    Toast.makeText(context, "正在呼叫所有成員起床...", Toast.LENGTH_SHORT).show()
                    
                    val targetTokens = members.mapNotNull { it.fcmToken }
                    if (targetTokens.isEmpty()) {
                        Toast.makeText(context, "目前沒有成員在線", Toast.LENGTH_SHORT).show()
                    } else {
                        db.collection("calls").add(hashMapOf(
                            "groupId" to group.id,
                            "callerName" to (FirebaseAuth.getInstance().currentUser?.displayName ?: "有人"),
                            "tokens" to targetTokens,
                            "timestamp" to FieldValue.serverTimestamp()
                        )).addOnSuccessListener {
                            Toast.makeText(context, "呼叫訊號已發出！", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("呼叫起床", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CreateGroupDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("建立群組") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("群組名稱") }) },
        confirmButton = { Button(onClick = { onCreate(name) }) { Text("建立") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun JoinGroupDialog(onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加入群組") },
        text = { OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("邀請碼") }) },
        confirmButton = { Button(onClick = { onJoin(code) }) { Text("加入") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
