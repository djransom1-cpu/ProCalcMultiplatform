package com.djran.constructioncalculator

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
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
        // Keys no text field took go to the calculator
        onKeyEvent = { e ->
            if (e.type != KeyEventType.KeyDown) false else {
                val key = when (e.key) {
                    Key.Enter, Key.NumPadEnter -> "ENTER"
                    Key.Backspace -> "BACK"
                    Key.Delete -> "DELETE"
                    Key.NumPadDivide -> "÷"
                    else -> e.utf16CodePoint.takeIf { it > 31 }?.toChar()?.toString()
                }
                key != null && HardwareCalcInput.handler?.invoke(key) == true
            }
        },
    ) {
        App()
    }
}
