package com.example.finalproject

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import androidx.compose.ui.platform.LocalClipboardManager


// 1. 群組資料結構
data class Group(val id: String, val name: String, val memberCount: Int)

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

// 3. 群組列表畫面 (整合即時讀取與加入功能)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(onGroupClick: (Group) -> Unit, onBackToHome: () -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) } // 👇 控制加入群組彈窗

    // 👇 用來存放從資料庫抓下來的真實群組名單
    val realGroups = remember { mutableStateListOf<Group>() }

    // 👇 即時監聽資料庫 (只要在畫面停留，就會自動抓取最新資料)
    DisposableEffect(currentUser?.uid) {
        var listener: ListenerRegistration? = null
        if (currentUser != null) {
            // 查詢邏輯：找出所有 members 陣列中包含我 UID 的群組
            listener = db.collection("groups")
                .whereArrayContains("members", currentUser.uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Toast.makeText(context, "讀取資料失敗", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        realGroups.clear()
                        for (doc in snapshot.documents) {
                            val id = doc.getString("groupId") ?: doc.id
                            val name = doc.getString("groupName") ?: "未知群組"
                            // 抓出陣列大小作為人數
                            val members = doc.get("members") as? List<*>
                            val memberCount = members?.size ?: 0

                            realGroups.add(Group(id, name, memberCount))
                        }
                    }
                }
        }
        // 當畫面被銷毀或使用者離開時，停止監聽以節省資源
        onDispose {
            listener?.remove()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的群組") },
                navigationIcon = {
                    IconButton(onClick = onBackToHome) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "回首頁")
                    }
                },
                actions = {
                    // 👇 右上角的「加入」按鈕
                    TextButton(onClick = { showJoinDialog = true }) {
                        Text("加入", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "新增群組")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)
        ) {
            // 👇 判斷如果沒有群組，顯示提示文字
            if (realGroups.isEmpty()) {
                item {
                    Text(
                        text = "目前還沒有加入任何群組喔！\n點擊右上角加入，或右下角創建一個吧。",
                        modifier = Modifier.padding(top = 32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(realGroups) { group ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onGroupClick(group) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = group.name, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "成員數：${group.memberCount} 人", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "邀請碼：${group.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }

        // --- 創建群組彈窗 ---
        if (showCreateDialog) {
            CreateGroupDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { groupName ->
                    if (currentUser == null) return@CreateGroupDialog
                    val uid = currentUser.uid
                    val groupRef = db.collection("groups").document()
                    val groupId = groupRef.id

                    val groupData = hashMapOf(
                        "groupId" to groupId,
                        "groupName" to groupName,
                        "ownerUid" to uid,
                        "members" to listOf(uid),
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )

                    val batch = db.batch()
                    batch.set(groupRef, groupData)
                    val userRef = db.collection("users").document(uid)
                    batch.update(userRef, "joinedGroups", FieldValue.arrayUnion(groupId))

                    batch.commit().addOnSuccessListener {
                        Toast.makeText(context, "成功建立群組！", Toast.LENGTH_SHORT).show()
                        showCreateDialog = false
                    }.addOnFailureListener { e ->
                        Toast.makeText(context, "建立失敗: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        // --- 👇 新增：加入群組彈窗 ---
        if (showJoinDialog) {
            JoinGroupDialog(
                onDismiss = { showJoinDialog = false },
                onJoin = { inputGroupId ->
                    if (currentUser == null) return@JoinGroupDialog
                    val uid = currentUser.uid

                    val groupRef = db.collection("groups").document(inputGroupId)

                    // 1. 先去資料庫找找看這個群組存不存在
                    groupRef.get().addOnSuccessListener { document ->
                        if (document.exists()) {
                            // 2. 存在的話，使用批次更新把使用者加入群組
                            val batch = db.batch()

                            // 把自己加進群組的 members 陣列
                            batch.update(groupRef, "members", FieldValue.arrayUnion(uid))

                            // 把群組 ID 加進自己的 joinedGroups 陣列
                            val userRef = db.collection("users").document(uid)
                            batch.update(userRef, "joinedGroups", FieldValue.arrayUnion(inputGroupId))

                            batch.commit().addOnSuccessListener {
                                Toast.makeText(context, "成功加入群組！", Toast.LENGTH_SHORT).show()
                                showJoinDialog = false
                            }.addOnFailureListener { e ->
                                Toast.makeText(context, "加入失敗: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "找不到此群組，請確認邀請碼是否正確", Toast.LENGTH_LONG).show()
                        }
                    }.addOnFailureListener { e ->
                        Toast.makeText(context, "查詢失敗: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }
}

// 4. 創建群組彈窗
@Composable
fun CreateGroupDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var groupName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("建立新群組") },
        text = {
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("群組名稱") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { if (groupName.isNotEmpty()) onCreate(groupName) }) {
                Text("建立")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// 5. 👇 新增：加入群組彈窗 UI
@Composable
fun JoinGroupDialog(onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var inviteCode by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加入群組") },
        text = {
            OutlinedTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it.trim() }, // 濾掉空白鍵，避免輸入錯誤
                label = { Text("請輸入群組邀請碼") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { if (inviteCode.isNotEmpty()) onJoin(inviteCode) }) {
                Text("加入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// 6. 群組詳細頁面 (維持原樣)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(group: Group, onBackClick: () -> Unit) {
    // 1. 取得剪貼簿管理員與 Context (用來顯示 Toast)
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group.name) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "群組邀請碼", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // 2. 將邀請碼與複製按鈕包在一個有底色的區塊中，增加可點擊的視覺提示
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        // 點擊文字區塊時的複製動作
                        clipboardManager.setText(AnnotatedString(group.id))
                        Toast.makeText(context, "邀請碼已複製！", Toast.LENGTH_SHORT).show()
                    },
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = group.id,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "複製邀請碼",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "點擊即可複製邀請碼分享給朋友",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "排行榜功能開發中...", style = MaterialTheme.typography.bodyLarge)
        }
    }
}