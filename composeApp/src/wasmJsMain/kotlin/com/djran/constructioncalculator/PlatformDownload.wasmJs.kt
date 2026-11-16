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

    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    override fun downloadBytes(fileName: String, bytes: ByteArray, mimeType: String) {
        try {
            triggerJsBase64Download(fileName, kotlin.io.encoding.Base64.encode(bytes), mimeType)
        } catch (e: Throwable) {
            println("Wasm Download Error: ${e.message}")
        }
    }
}

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun triggerJsBase64Download(fileName: String, b64: String, mimeType: String) {
    js("""
        var bin = atob(b64);
        var arr = new Uint8Array(bin.length);
        for (var i = 0; i < bin.length; i++) arr[i] = bin.charCodeAt(i);
        var url = URL.createObjectURL(new Blob([arr], { type: mimeType }));
        var a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        a.click();
        URL.revokeObjectURL(url);
    """)
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
