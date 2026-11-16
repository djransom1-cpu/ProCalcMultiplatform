package com.djran.constructioncalculator

import androidx.compose.runtime.Composable

interface FileDownloader {
    fun downloadFile(fileName: String, content: String, mimeType: String = "text/plain")
    /** Saves or shares a binary file (a .ccp project backup). */
    fun downloadBytes(fileName: String, bytes: ByteArray, mimeType: String = "application/octet-stream") {}
}

@Composable
expect fun rememberFileDownloader(): FileDownloader
