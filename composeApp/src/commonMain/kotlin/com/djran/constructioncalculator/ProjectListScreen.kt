package com.djran.constructioncalculator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun ProjectListScreen(
    projects: List<String>,
    cloudSync: CloudSync,
    cloudLinks: Map<String, CloudLink>,
    /** CLOUD / SYNCED tapped (or sign-in needed, with an empty name). */
    onCloudSync: (String) -> Unit,
    onJoinByCode: (String) -> Unit,
    onProjectSelected: (String) -> Unit,
    onAddProject: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onPrintProject: (String) -> Unit,
    onPrintAll: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var newProjectName by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var joinCode by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    var teamProject by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(Palette.Bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Palette.Bg).padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Palette.Navy, modifier = Modifier.size(30.dp)) }
            Text("Project Management", color = Palette.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (cloudSync.isBusy) {
                CircularProgressIndicator(color = Palette.Navy, strokeWidth = 2.dp, modifier = Modifier.padding(end = 8.dp).size(20.dp))
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Palette.Orange, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("NEW JOB", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onPrintAll,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = BlueTool, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PRINT ALL", fontWeight = FontWeight.Bold)
                }
            }

            if (cloudSyncSupported) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { if (cloudSync.isSignedIn) showJoinDialog = true else onCloudSync("") },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null, tint = BlueTool)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("JOIN PROJECT WITH CODE", color = BlueTool, fontWeight = FontWeight.Bold)
                }
                cloudSync.message?.let { text ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text,
                        color = if (cloudSync.messageIsError) LabelRed else Color(0xFF2E7D32),
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().clickable { cloudSync.clearMessage() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("SELECT PROJECT", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                items(projects) { name ->
                    val isCloud = cloudLinks.containsKey(name)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                            .clickable { onProjectSelected(name) }
                            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, modifier = Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        if (cloudSyncSupported) {
                            // Same CLOUD / SYNCED and TEAM buttons as the Android app's project list
                            Button(
                                onClick = { onCloudSync(name) },
                                enabled = !cloudSync.isBusy,
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (isCloud) Color(0xFF2E7D32) else Color(0xFF455A64),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text(if (isCloud) "SYNCED" else "CLOUD", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = { teamProject = name },
                                enabled = isCloud && cloudSync.isSignedIn,
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF37474F), contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text("TEAM", fontSize = 11.sp)
                            }
                        }
                        IconButton(onClick = { onPrintProject(name) }) {
                            Icon(Icons.Default.Share, contentDescription = "Print", tint = BlueTool)
                        }
                        IconButton(onClick = { pendingDelete = name }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("New Project Name") },
            text = {
                OutlinedTextField(
                    value = newProjectName,
                    onValueChange = { newProjectName = it },
                    label = { Text("Enter name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newProjectName.isNotBlank()) {
                        onAddProject(newProjectName)
                        newProjectName = ""
                        showDialog = false
                    }
                }) { Text("CREATE") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("CANCEL") }
            }
        )
    }

    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Join Project") },
            text = {
                OutlinedTextField(
                    value = joinCode,
                    onValueChange = { joinCode = it.uppercase().take(6) },
                    label = { Text("6-character invite code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (joinCode.isNotBlank()) {
                        onJoinByCode(joinCode)
                        joinCode = ""
                        showJoinDialog = false
                    }
                }) { Text("JOIN") }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) { Text("CANCEL") }
            }
        )
    }

    pendingDelete?.let { name ->
        val link = cloudLinks[name]
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete Project?") },
            text = {
                Text(
                    if (link != null) "Permanently remove '$name' and all its data, including its cloud copy and team access?"
                    else "Permanently remove '$name' and all its data?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDelete = null
                        if (link != null && cloudSync.isSignedIn) {
                            scope.launch {
                                // The owner deletes the cloud copy, a member just leaves the team (as in the app)
                                try { cloudSync.removeFromCloud(link.cloudId) } catch (_: Exception) { }
                                onDeleteProject(name)
                            }
                        } else {
                            onDeleteProject(name)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red, contentColor = Color.White)
                ) { Text("DELETE") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("CANCEL") }
            }
        )
    }

    teamProject?.let { name ->
        cloudLinks[name]?.let { link ->
            TeamDialog(name, link.cloudId, cloudSync, onDismiss = { teamProject = null })
        }
    }
}

@Composable
private fun TeamDialog(projectName: String, cloudId: String, cloudSync: CloudSync, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf<TeamInfo?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(cloudId) {
        try { info = cloudSync.teamInfo(cloudId) } catch (e: Exception) { error = e.message ?: "Couldn't load team info" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Team: $projectName") },
        text = {
            Column {
                val team = info
                val problem = error
                when {
                    problem != null -> Text(problem, color = LabelRed)
                    team == null -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    else -> {
                        Text("Invite code", fontSize = 12.sp, color = Color.Gray)
                        Text(team.inviteCode, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = BlueTool)
                        Text("Share this code so others can join with JOIN PROJECT WITH CODE.", fontSize = 12.sp, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Owner: ${team.ownerEmail}", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Members (${team.memberEmails.size}):", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        team.memberEmails.forEach { Text(it, fontSize = 14.sp) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE") }
        },
        dismissButton = {
            if (info?.isOwner == true) {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            val code = cloudSync.regenerateInviteCode(cloudId)
                            info = info?.copy(inviteCode = code)
                        } catch (e: Exception) {
                            error = e.message ?: "Couldn't change the invite code"
                        }
                    }
                }) { Text("NEW CODE") }
            }
        }
    )
}
