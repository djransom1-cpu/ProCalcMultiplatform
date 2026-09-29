package com.djran.constructioncalculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(cloudSync: CloudSync, onSyncNow: () -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Text("Account & Sync", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (cloudSync.isSignedIn) {
                SignedInPanel(cloudSync, onSyncNow)
            } else {
                SignInPanel(cloudSync, onSignedIn = onSyncNow)
            }
        }
    }
}

@Composable
private fun SignedInPanel(cloudSync: CloudSync, onSyncNow: () -> Unit) {
    Icon(Icons.Default.CloudDone, contentDescription = null, tint = BlueTool, modifier = Modifier.size(72.dp))
    Spacer(Modifier.height(12.dp))
    Text("Signed in as", fontSize = 14.sp, color = Color.Gray)
    Text(cloudSync.email ?: "", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BlueTool)
    Spacer(Modifier.height(16.dp))
    Text(
        "Your projects sync automatically between this device and every other phone or browser signed in to this account.",
        fontSize = 14.sp,
        color = Color.DarkGray,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(24.dp))

    val statusText = when {
        cloudSync.isBusy -> "Syncing..."
        cloudSync.lastError != null -> cloudSync.lastError!!
        cloudSync.status.isNotEmpty() -> cloudSync.status
        else -> "Waiting to sync"
    }
    Text(
        statusText,
        fontSize = 14.sp,
        color = if (cloudSync.lastError != null && !cloudSync.isBusy) LabelRed else Color.DarkGray,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(16.dp))

    Button(
        onClick = onSyncNow,
        enabled = !cloudSync.isBusy,
        colors = ButtonDefaults.buttonColors(backgroundColor = BlueTool, contentColor = Color.White),
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Icon(Icons.Default.Sync, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Sync Now")
    }
    Spacer(Modifier.height(12.dp))
    OutlinedButton(
        onClick = { cloudSync.signOut() },
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Text("Sign Out", color = LabelRed)
    }
    Spacer(Modifier.height(8.dp))
    Text("Signing out keeps your projects on this device.", fontSize = 12.sp, color = Color.Gray)
}

@Composable
private fun SignInPanel(cloudSync: CloudSync, onSignedIn: () -> Unit) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    fun submit(action: suspend () -> Unit, onSuccess: () -> Unit = {}) {
        if (working) return
        working = true
        message = null
        scope.launch {
            try {
                action()
                onSuccess()
            } catch (e: Exception) {
                isError = true
                message = e.message ?: "Something went wrong"
            } finally {
                working = false
            }
        }
    }

    Icon(Icons.Default.CloudOff, contentDescription = null, tint = BlueTool, modifier = Modifier.size(72.dp))
    Spacer(Modifier.height(12.dp))
    Text("Sync your projects", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BlueTool)
    Spacer(Modifier.height(8.dp))
    Text(
        "Sign in with your email to keep projects in step between your phone and the web. Use the same login as Crewsync.",
        fontSize = 14.sp,
        color = Color.DarkGray,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(24.dp))

    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(16.dp))

    val canSubmit = !working && email.isNotBlank() && password.isNotEmpty()
    Button(
        onClick = { submit({ cloudSync.signIn(email, password) }, onSignedIn) },
        enabled = canSubmit,
        colors = ButtonDefaults.buttonColors(backgroundColor = BlueTool, contentColor = Color.White),
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        if (working) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text("Sign In")
        }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = { submit({ cloudSync.createAccount(email, password) }, onSignedIn) },
        enabled = canSubmit,
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Text("Create Account", color = BlueTool)
    }
    TextButton(
        onClick = {
            submit({ cloudSync.sendPasswordReset(email) }) {
                isError = false
                message = "Password reset email sent to ${email.trim()}"
            }
        },
        enabled = !working && email.isNotBlank()
    ) {
        Text("Forgot password?", color = BlueTool)
    }

    message?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = if (isError) LabelRed else BlueTool, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}
