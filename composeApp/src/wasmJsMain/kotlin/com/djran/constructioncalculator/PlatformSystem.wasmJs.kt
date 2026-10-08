package com.djran.constructioncalculator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.browser.window

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

// Each open back gesture adds a browser history entry, so the browser's back button (and the
// back swipe on phones and trackpads) closes that screen instead of leaving the app.
private val backHandlers = mutableListOf<() -> Unit>() // innermost last
private var listeningForBack = false
private var entriesToDrop = 0
private var ownHistoryMove = false

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@Composable
actual fun BackGesture(enabled: Boolean, onBack: () -> Unit) {
    val latestOnBack by rememberUpdatedState(onBack)
    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}
        if (!listeningForBack) {
            listeningForBack = true
            window.addEventListener("popstate", {
                if (ownHistoryMove) ownHistoryMove = false else backHandlers.lastOrNull()?.invoke()
            })
        }
        var closedByBrowser = false
        val handler = { closedByBrowser = true; latestOnBack() }
        backHandlers += handler
        window.history.pushState(null, "")
        onDispose {
            backHandlers -= handler
            // Closed in the app (back arrow, a tab): take its entry off the history again
            if (!closedByBrowser) dropHistoryEntry()
        }
    }
}

// Batched, so closing two at once (a tool and its keyboard) is one history move
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun dropHistoryEntry() {
    entriesToDrop++
    if (entriesToDrop > 1) return
    window.setTimeout({
        ownHistoryMove = true
        window.history.go(-entriesToDrop)
        entriesToDrop = 0
        null
    }, 0)
}
