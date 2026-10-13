package com.djran.constructioncalculator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/** Squaring / Diagonal: length and width give the diagonal; matching diagonals mean square corners. */
@Composable
fun SquareCalculator(
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var length by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var checks by remember { mutableStateOf<List<Triple<String, String, Double>>>(emptyList()) }

    // Same as the Android app: plain numbers are inches; use ' for feet
    val l = FractionUtils.parse(length)
    val w = FractionUtils.parse(width)
    val diag = if (l > 0 && w > 0) sqrt(l * l + w * w) else null

    ToolScreen(Screen.SquareCalculator, onBack) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("BUILDING DIMENSIONS", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(
                "Plain numbers are inches; use ' for feet (40' or 40' 6\").",
                color = AppTheme.colors.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(value = length, onValueChange = { length = it }, label = "Length", modifier = Modifier.weight(1f), onFocus = onFocus)
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(value = width, onValueChange = { width = it }, label = "Width", modifier = Modifier.weight(1f), onFocus = onFocus)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("Name (optional)") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            if (diag != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), backgroundColor = AppTheme.colors.surface, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("DIAGONAL", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 12.sp)
                        Text(FractionUtils.formatInches(diag), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.ink)
                        Text("Measure both diagonals. When they match, the corners are square.", fontSize = 13.sp, color = AppTheme.colors.muted)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SquareDiagram(l, w, diag)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        checks = checks + Triple(name.ifBlank { "Check ${checks.size + 1}" }, "${FractionUtils.formatInches(l)} x ${FractionUtils.formatInches(w)}", diag)
                        name = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)
                ) { Text("ADD TO LIST") }
            } else {
                Text("Enter a length and a width.", color = AppTheme.colors.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp))
            }

            if (checks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                MarksTable(listOf("Name", "Size", "Diagonal"), checks.map { listOf(it.first, it.second, FractionUtils.formatInches(it.third)) })
                TextButton(onClick = { checks = emptyList() }) { Text("Clear list", color = AppTheme.colors.danger) }
            }
        }
    }
}

@Composable
private fun SquareDiagram(l: Double, w: Double, diag: Double) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier = Modifier.fillMaxWidth().height(200.dp).background(Color.White)) {
        val pad = 40f
        val s = min((size.width - 2 * pad) / l.toFloat(), (size.height - 2 * pad) / w.toFloat())
        val rw = (l * s).toFloat(); val rh = (w * s).toFloat()
        val x0 = (size.width - rw) / 2f; val y0 = (size.height - rh) / 2f
        drawRect(Color(0xFFE9D9B8), Offset(x0, y0), Size(rw, rh))
        drawRect(Color(0xFF7A5A22), Offset(x0, y0), Size(rw, rh), style = Stroke(3f))
        val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        drawLine(Color(0xFFD9661F), Offset(x0, y0 + rh), Offset(x0 + rw, y0), 3f, pathEffect = dash)
        drawLine(Color(0xFF2F6F8F), Offset(x0, y0), Offset(x0 + rw, y0 + rh), 3f, pathEffect = dash)
        val style = TextStyle(fontSize = 12.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
        val lt = measurer.measure(FractionUtils.formatInches(l), style)
        drawText(lt, topLeft = Offset(x0 + rw / 2 - lt.size.width / 2f, y0 + rh + 6f))
        val wt = measurer.measure(FractionUtils.formatInches(w), style)
        drawText(wt, topLeft = Offset(x0 - wt.size.width - 6f, y0 + rh / 2 - wt.size.height / 2f))
        val dt = measurer.measure(FractionUtils.formatInches(diag), style.copy(color = Color(0xFFD9661F)))
        drawText(dt, topLeft = Offset(x0 + rw / 2 - dt.size.width / 2f, y0 + rh / 2 - dt.size.height - 4f))
    }
}
