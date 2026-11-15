package com.djran.constructioncalculator

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/** ProCalc for Windows (and Mac/Linux): the same app as the web and phone builds, in a window. */
fun main() = application {
    val state = rememberWindowState(size = DpSize(1400.dp, 900.dp), position = WindowPosition.PlatformDefault)
    Window(
        onCloseRequest = ::exitApplication,
        title = "ProCalc",
        state = state,
        // Esc works like the phone's back gesture: closes the keypad, then the open tool
        onPreviewKeyEvent = { e ->
            if (e.type == KeyEventType.KeyDown && e.key == Key.Escape) DesktopBack.back() else false
        },
    ) {
        App()
    }
}
