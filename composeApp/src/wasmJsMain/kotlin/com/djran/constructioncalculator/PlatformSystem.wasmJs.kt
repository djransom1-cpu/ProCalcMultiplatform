package com.djran.constructioncalculator

import androidx.compose.runtime.Composable

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun jsNow(): Double = js("Date.now()")

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun jsUtcOffsetMinutes(ms: Double): Int = js("-new Date(ms).getTimezoneOffset()")

actual fun currentTimeMillis(): Long = jsNow().toLong()

actual fun localUtcOffsetMinutes(epochMillis: Long): Int = jsUtcOffsetMinutes(epochMillis.toDouble())

@Composable
actual fun SystemBarsAppearance(darkBackground: Boolean, followSystem: Boolean) {
    // Browsers draw their own toolbars; nothing to do
}
