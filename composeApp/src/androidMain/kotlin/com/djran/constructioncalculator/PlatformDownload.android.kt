package com.djran.constructioncalculator

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun rememberFileDownloader(): FileDownloader {
    val context = LocalContext.current
    return AndroidFileDownloader(context)
}

class AndroidFileDownloader(private val context: Context) : FileDownloader {
    override fun downloadFile(fileName: String, content: String, mimeType: String) {
        try {
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { it.write(content.toByteArray()) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share $fileName"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
