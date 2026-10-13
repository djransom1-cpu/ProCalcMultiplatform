package com.djran.constructioncalculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(cloudSync: CloudSync, onOpenProjects: () -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Palette.Bg).padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Palette.Navy, modifier = Modifier.size(30.dp)) }
            Text("Account & Sync", color = Palette.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val user = cloudSync.user
            Icon(
                if (user != null) Icons.Default.CloudDone else Icons.Default.CloudOff,
                contentDescription = null,
                tint = BlueTool,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(12.dp))

            if (!cloudSyncSupported) {
                Text(
                    "Cloud sync is available in the Android app and on the web version of Pro Construction Calculator.",
                    fontSize = 14.sp, color = Color.DarkGray, textAlign = TextAlign.Center
                )
                return@Column
            }

            if (user == null) {
                Text("Sync your projects", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Sign in with the same Google account you use in the Android app. Projects you sync there show up here, and you can share projects with your team using an invite code.",
                    fontSize = 14.sp, color = Color.DarkGray, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { scope.launch { cloudSync.signIn() } },
                    enabled = !cloudSync.isBusy,
                    colors = ButtonDefaults.buttonColors(backgroundColor = BlueTool, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (cloudSync.isBusy) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Default.AccountCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sign in with Google")
                    }
                }
            } else {
                Text("Signed in as", fontSize = 14.sp, color = Color.Gray)
                Text(user.email, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                Spacer(Modifier.height(16.dp))
                Text(
                    "In Project List, tap CLOUD to sync a project (it then shows SYNCED; tap again to sync the latest changes), and TEAM to share its invite code.",
                    fontSize = 14.sp, color = Color.DarkGray, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onOpenProjects,
                    colors = ButtonDefaults.buttonColors(backgroundColor = BlueTool, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.List, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open Project List")
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { scope.launch { cloudSync.signOut() } },
                    enabled = !cloudSync.isBusy,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Sign Out", color = LabelRed)
                }
            }

            cloudSync.message?.let {
                Spacer(Modifier.height(16.dp))
                Text(
                    it,
                    color = if (cloudSync.messageIsError) LabelRed else BlueTool,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
