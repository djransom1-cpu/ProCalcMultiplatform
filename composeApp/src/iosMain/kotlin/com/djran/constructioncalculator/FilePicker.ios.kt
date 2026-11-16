package com.djran.constructioncalculator

import androidx.compose.runtime.Composable

// iOS document picker not wired yet
@Composable
actual fun rememberFilePicker(onPicked: (ByteArray) -> Unit): () -> Unit = {}
