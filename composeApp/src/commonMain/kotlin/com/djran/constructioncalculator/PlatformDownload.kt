package com.djran.constructioncalculator

import androidx.compose.runtime.Composable

interface FileDownloader {
    fun downloadFile(fileName: String, content: String, mimeType: String = "text/plain")
}

@Composable
expect fun rememberFileDownloader(): FileDownloader
