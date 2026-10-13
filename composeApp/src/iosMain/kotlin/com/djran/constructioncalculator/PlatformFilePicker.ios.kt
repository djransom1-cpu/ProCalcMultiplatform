package com.djran.constructioncalculator

import androidx.compose.runtime.Composable

@Composable
actual fun rememberFilePicker(): FilePicker = IosFilePicker()

class IosFilePicker : FilePicker {
    override fun pickFile(extension: String, onResult: (String?) -> Unit) {
        // iOS document picker stub - matches IosFileDownloader's stub pattern.
        onResult(null)
    }
}
