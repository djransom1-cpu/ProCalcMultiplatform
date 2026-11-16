package com.djran.constructioncalculator

import androidx.compose.runtime.Composable
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberFilePicker(onPicked: (ByteArray) -> Unit): () -> Unit = {
    val dialog = FileDialog(null as Frame?, "Import Project (.ccp)", FileDialog.LOAD).apply {
        directory = File(System.getProperty("user.home"), "Downloads").takeIf { it.isDirectory }?.path
        isVisible = true
    }
    dialog.file?.let { name -> runCatching { File(dialog.directory, name).readBytes() }.getOrNull()?.let(onPicked) }
}
