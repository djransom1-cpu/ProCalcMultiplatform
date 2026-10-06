package com.djran.constructioncalculator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConstructionKeyboard(
    isVisible: Boolean,
    onValueChange: (String) -> Unit,
    currentValue: String,
    onDone: () -> Unit,
    onHide: () -> Unit
) {
    if (!isVisible) return

    Surface(
        elevation = 16.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = Palette.Bg,
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize()
        ) {
            // Header for the keyboard
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Numeric Input", fontWeight = FontWeight.Bold, color = Palette.Muted, fontSize = 12.sp)
                IconButton(onClick = onHide) {
                    Icon(Icons.Default.KeyboardHide, contentDescription = "Hide keypad", tint = Palette.Muted)
                }
            }

            val keys = listOf(
                listOf("7", "8", "9", "BACK"),
                listOf("4", "5", "6", "/"),
                listOf("1", "2", "3", "'"),
                listOf(".", "0", "\"", "SPACE"),
                listOf("+", "-")
            )

            keys.forEach { row ->
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    row.forEach { key ->
                        KeyButton(
                            label = key,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                handleKeyPress(key, currentValue, onValueChange)
                            }
                        )
                    }
                }
            }

            // Bottom row for Done
            Row(modifier = Modifier.height(60.dp).fillMaxWidth()) {
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Palette.Orange, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("DONE", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun KeyButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isSpecial = label == "BACK" || label == "SPACE" || label == "/" || label == "-" || label == "+" || label == "'" || label == "\""
    val bgColor = if (isSpecial) Palette.NavyTint else Color.White
    
    Card(
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = bgColor,
        modifier = modifier
            .padding(4.dp)
            .fillMaxHeight()
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (label == "BACK") {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace", tint = Palette.Navy)
            } else if (label == "SPACE") {
                Text("SPACE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Palette.Navy)
            } else {
                Text(
                    text = label,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSpecial) Palette.Navy else Palette.Ink
                )
            }
        }
    }
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
        },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = Palette.Navy,
            unfocusedBorderColor = Palette.FieldLine
        )
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
