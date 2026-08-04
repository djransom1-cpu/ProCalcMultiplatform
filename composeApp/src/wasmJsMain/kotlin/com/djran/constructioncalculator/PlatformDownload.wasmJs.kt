package com.djran.constructioncalculator

import androidx.compose.runtime.Composable

@Composable
actual fun rememberFileDownloader(): FileDownloader = WasmFileDownloader()

class WasmFileDownloader : FileDownloader {
    override fun downloadFile(fileName: String, content: String, mimeType: String) {
        try {
            triggerJsDownload(fileName, content, mimeType)
        } catch (e: Throwable) {
            println("Wasm Download Error: ${e.message}")
        }
    }
}

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun triggerJsDownload(fileName: String, content: String, mimeType: String) {
    js("""
        var blob = new Blob([content], { type: mimeType });
        var url = URL.createObjectURL(blob);
        var a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        a.click();
        URL.revokeObjectURL(url);
    """)
}
