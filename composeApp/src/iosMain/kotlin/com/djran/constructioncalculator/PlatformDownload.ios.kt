package com.djran.constructioncalculator

import androidx.compose.runtime.Composable

@Composable
actual fun rememberFileDownloader(): FileDownloader = IosFileDownloader()

class IosFileDownloader : FileDownloader {
    override fun downloadFile(fileName: String, content: String, mimeType: String) {
        // iOS share sheet / file save stub
    }
}
