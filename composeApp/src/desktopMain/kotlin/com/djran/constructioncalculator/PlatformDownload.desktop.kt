package com.djran.constructioncalculator

import androidx.compose.runtime.Composable
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberFileDownloader(): FileDownloader = DesktopFileDownloader()

/** Asks where to save, like a browser download would. */
class DesktopFileDownloader : FileDownloader {
    override fun downloadFile(fileName: String, content: String, mimeType: String) {
        try {
            val dialog = FileDialog(null as Frame?, "Save $fileName", FileDialog.SAVE).apply {
                file = fileName
                directory = File(System.getProperty("user.home"), "Downloads").takeIf { it.isDirectory }?.path
                isVisible = true
            }
            val name = dialog.file ?: return
            File(dialog.directory, name).writeText(content)
        } catch (e: Throwable) {
            println("Desktop save error: ${e.message}")
        }
    }
}
