package com.djran.constructioncalculator

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
actual fun rememberFilePicker(): FilePicker {
    val context = LocalContext.current
    var pendingCallback by remember { mutableStateOf<((String?) -> Unit)?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val callback = pendingCallback
        pendingCallback = null
        if (uri == null) {
            callback?.invoke(null)
            return@rememberLauncherForActivityResult
        }
        try {
            val text = context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).readText()
            }
            callback?.invoke(text)
        } catch (e: Exception) {
            e.printStackTrace()
            callback?.invoke(null)
        }
    }

    return remember {
        object : FilePicker {
            override fun pickFile(extension: String, onResult: (String?) -> Unit) {
                pendingCallback = onResult
                launcher.launch("*/*")
            }
        }
    }
}
