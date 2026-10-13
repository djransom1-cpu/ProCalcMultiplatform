package com.djran.constructioncalculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.KeyboardHide
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The keypad for the tools' measurement fields, laid out and coloured like the calculator's:
 * units on top, digits in the middle, signs down the right and a tall Done key. The number
 * being typed shows above the keys, since the keypad can cover the field.
 */
@Composable
fun ConstructionKeyboard(
    isVisible: Boolean,
    onValueChange: (String) -> Unit,
    currentValue: String,
    onDone: () -> Unit,
    onHide: () -> Unit
) {
    if (!isVisible) return
    val c = LocalAppColors.current
    val press: (String) -> Unit = { key -> handleKeyPress(key, currentValue, onValueChange) }
    val keyHeight = 52.dp
    val gap = 8.dp

    Surface(
        elevation = 16.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = c.bg,
        contentColor = c.ink,
        // A hairline edge, since the shadow doesn't show on a dark page
        border = BorderStroke(1.dp, c.line),
        modifier = Modifier.widthIn(max = CalcMaxWidth).fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onHide) {
                    Icon(Icons.Outlined.KeyboardHide, contentDescription = "Hide keypad", tint = c.muted)
                }
                Text(
                    currentValue, color = c.ink, fontSize = 30.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, textAlign = TextAlign.End, modifier = Modifier.weight(1f),
                )
            }
            Row(Modifier.fillMaxWidth().height(keyHeight)) {
                Key(c.fn, c.fnInk, { press("'") }, label = "Feet") { KeyText("ft '", 22, FontWeight.SemiBold) }
                Key(c.fn, c.fnInk, { press("\"") }, label = "Inches") { KeyText("in \"", 22, FontWeight.SemiBold) }
                Key(c.num, c.numInk, { press(".") }, label = "Decimal point") { KeyText(".", 32, FontWeight.SemiBold) }
                Key(c.fn, c.fnInk, { press("BACK") }, label = "Backspace") {
                    Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = null, modifier = Modifier.size(28.dp))
                }
            }
            Row(Modifier.fillMaxWidth().height(keyHeight)) {
                KeyboardDigit("7", press); KeyboardDigit("8", press); KeyboardDigit("9", press)
                Key(c.op, c.opInk, { press("-") }, label = "Minus") { KeyText("−", 32) }
            }
            Row(Modifier.fillMaxWidth().height(keyHeight)) {
                KeyboardDigit("4", press); KeyboardDigit("5", press); KeyboardDigit("6", press)
                Key(c.op, c.opInk, { press("+") }, label = "Plus") { KeyText("+", 32) }
            }
            Row(Modifier.fillMaxWidth().height(keyHeight * 2 + gap)) {
                Column(Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(gap)) {
                    Row(Modifier.fillMaxWidth().height(keyHeight)) {
                        KeyboardDigit("1", press); KeyboardDigit("2", press); KeyboardDigit("3", press)
                    }
                    Row(Modifier.fillMaxWidth().height(keyHeight)) {
                        KeyboardDigit("0", press)
                        Key(c.fn, c.fnInk, { press("SPACE") }, label = "Space, between whole inches and a fraction") {
                            Text("space", color = c.fnInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                        }
                        Key(c.fn, c.fnInk, { press("/") }, label = "Fraction bar") { KeyText("/", 26, FontWeight.SemiBold) }
                    }
                }
                Row(Modifier.weight(1f).fillMaxHeight()) {
                    Key(c.eq, c.eqInk, onDone) { KeyText("Done", 20, FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun RowScope.KeyboardDigit(digit: String, press: (String) -> Unit) {
    val c = LocalAppColors.current
    Key(c.num, c.numInk, { press(digit) }) { KeyText(digit, 28) }
}

@Composable
fun ConstructionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    // Theme colours, like the tools' other text fields
    OutlinedTextField(
        value = value,
        onValueChange = { /* Suppressed */ },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        readOnly = true,
        modifier = modifier.onFocusChanged { focusState ->
            if (focusState.isFocused) {
                onFocus(value, onValueChange)
            }
        }
    )
}

private fun handleKeyPress(
    key: String,
    currentValue: String,
    onValueChange: (String) -> Unit
) {
    when (key) {
        "BACK" -> {
            if (currentValue.isNotEmpty()) {
                onValueChange(currentValue.dropLast(1))
            }
        }
        "SPACE" -> onValueChange(currentValue + " ")
        else -> onValueChange(currentValue + key)
    }
}
