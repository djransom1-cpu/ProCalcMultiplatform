package com.djran.constructioncalculator

/**
 * Desktop: on a wide window the home page (calculator) stays on the left and the page you're
 * on opens on the right.
 */
expect val wideSplitLayout: Boolean

/** Desktop: tool fields take typing from the real keyboard instead of popping up the keypad. */
expect val typeOnKeyboard: Boolean

/**
 * Desktop: keys typed while no text field has focus go to the calculator (digits, ' " + - * /,
 * Enter, Backspace, Delete). The window calls [handler]; the app sets it.
 */
object HardwareCalcInput {
    var handler: ((key: String) -> Boolean)? = null
}
