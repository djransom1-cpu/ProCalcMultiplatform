package com.djran.constructioncalculator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import java.util.TimeZone

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun localUtcOffsetMinutes(epochMillis: Long): Int = TimeZone.getDefault().getOffset(epochMillis) / 60_000

@Composable
actual fun SystemBarsAppearance(darkBackground: Boolean, followSystem: Boolean) {
    // The window has its own title bar; nothing to do
}

/** Back handlers in the order they opened; Esc runs the newest one. */
object DesktopBack {
    internal val handlers = mutableListOf<() -> Unit>()
    fun back(): Boolean {
        val h = handlers.lastOrNull() ?: return false
        h(); return true
    }
}

@Composable
actual fun BackGesture(enabled: Boolean, onBack: () -> Unit) {
    val latest by rememberUpdatedState(onBack)
    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}
        val h = { latest() }
        DesktopBack.handlers += h
        onDispose { DesktopBack.handlers -= h }
    }
}
