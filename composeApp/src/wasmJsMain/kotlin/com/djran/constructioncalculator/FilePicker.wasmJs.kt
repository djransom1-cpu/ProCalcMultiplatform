package com.djran.constructioncalculator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.Promise

/** Browser file chooser; the file comes back as base64 (empty if nothing was picked). */
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun jsPickFile(): Promise<JsString> = js("""
    new Promise(function(resolve) {
        var input = document.createElement('input');
        input.type = 'file';
        input.accept = '.ccp,application/zip,application/octet-stream';
        input.onchange = function() {
            var f = input.files && input.files[0];
            if (!f) { resolve(''); return; }
            var r = new FileReader();
            r.onload = function() { var s = String(r.result); resolve(s.substring(s.indexOf(',') + 1)); };
            r.onerror = function() { resolve(''); };
            r.readAsDataURL(f);
        };
        input.click();
    })
""")

@OptIn(ExperimentalEncodingApi::class)
@Composable
actual fun rememberFilePicker(onPicked: (ByteArray) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    return {
        scope.launch {
            val b64 = runCatching { jsPickFile().await<JsString>().toString() }.getOrDefault("")
            if (b64.isNotEmpty()) runCatching { Base64.decode(b64) }.getOrNull()?.let(onPicked)
        }
    }
}
