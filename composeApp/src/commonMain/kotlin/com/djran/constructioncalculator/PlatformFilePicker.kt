package com.djran.constructioncalculator

import androidx.compose.runtime.Composable

interface FilePicker {
    // Opens a platform file picker restricted to the given extension (e.g. "json") and
    // invokes onResult with the picked file's text content, or null if cancelled/failed.
    fun pickFile(extension: String, onResult: (String?) -> Unit)
}

@Composable
expect fun rememberFilePicker(): FilePicker
