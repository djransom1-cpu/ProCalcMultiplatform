package com.djran.constructioncalculator

import androidx.compose.runtime.Composable

@Composable
actual fun rememberFilePicker(): FilePicker = WasmFilePicker()

class WasmFilePicker : FilePicker {
    override fun pickFile(extension: String, onResult: (String?) -> Unit) {
        // Web file-input picker stub - matches WasmFileDownloader's approach for the
        // download half of this pair; wiring a real <input type=file> + FileReader
        // round-trip needs a JS callback bridge best done once the wasmJs target
        // actually compiles again (see the separate .format() compile-failure fix).
        onResult(null)
    }
}
