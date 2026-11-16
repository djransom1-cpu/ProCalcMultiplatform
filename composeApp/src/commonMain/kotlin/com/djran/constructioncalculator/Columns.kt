package com.djran.constructioncalculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.pow

/** Round Column, matching the Android app: quantity, volume, and rebar (vertical + ties, or a linear count). */
object ColumnMath {
    class Out(val circumference: Double, val areaSqIn: Double, val volYdEach: Double, val surfaceSqIn: Double, val rebarFt: Double)

    /** Height: a plain number is feet, like the Android app; ' or " read as entered. */
    fun heightInches(input: String): Double {
        val h = FractionUtils.parse(input)
        return if (input.isNotBlank() && !input.contains("'") && !input.contains("\"")) h * 12.0 else h
    }

    fun compute(diameterIn: Double, heightIn: Double, qty: Double, ties: Boolean, vert: Double, tieOc: Double, top: Double, bottom: Double): Out {
        val r = diameterIn / 2.0
        val circ = PI * diameterIn
        val area = PI * r.pow(2)
        val vol = area * heightIn / 46656.0
        var rebar = 0.0
        if (ties) {
            if (tieOc > 0) rebar = vert * (heightIn / 12.0) + ((heightIn / tieOc) + 1) * (circ / 12.0)
        } else rebar = (top + bottom) * (diameterIn / 12.0)
        return Out(circ, area, vol, circ * heightIn + 2 * area, rebar * qty)
    }
}

@Composable
fun ColumnCalculator(
    entries: List<ColumnEntry>,
    onUpdateEntries: (List<ColumnEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var qty by remember { mutableStateOf("1") }
    var name by remember { mutableStateOf("") }
    var diameter by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var ties by remember { mutableStateOf(true) }
    var vert by remember { mutableStateOf("0") }
    var tieOc by remember { mutableStateOf("12") }
    var top by remember { mutableStateOf("0") }
    var bottom by remember { mutableStateOf("0") }
    var editingIndex by remember { mutableStateOf(-1) }
    var message by remember { mutableStateOf("") }

    @Composable
    fun field(label: String, value: String, modifier: Modifier, set: (String) -> Unit) =
        ConstructionTextField(value = value, onValueChange = set, label = label, modifier = modifier, onFocus = onFocus)

    ToolScreen(Screen.ColumnCalculator, onBack) {
        Column(Modifier.padding(16.dp)) {
            Text("ADD COLUMN / CIRCLE", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row {
                field("Qty", qty, Modifier.weight(0.6f)) { qty = it }
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name (e.g. Column A)") }, modifier = Modifier.weight(1.4f))
            }
            Spacer(Modifier.height(8.dp))
            Row {
                field("Diameter (in)", diameter, Modifier.weight(1f)) { diameter = it }
                Spacer(Modifier.width(8.dp))
                field("Height (ft)", height, Modifier.weight(1f)) { height = it }
            }
            Text("COLUMN REBAR", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            SpSegmented(listOf("Vertical + Ties", "Linear Count"), if (ties) 0 else 1) { ties = it == 0 }
            Spacer(Modifier.height(8.dp))
            if (ties) Row {
                field("Num Vert Bars", vert, Modifier.weight(1f)) { vert = it }
                Spacer(Modifier.width(8.dp))
                field("Ties OC (in)", tieOc, Modifier.weight(1f)) { tieOc = it }
            } else Row {
                field("Bars Top", top, Modifier.weight(1f)) { top = it }
                Spacer(Modifier.width(8.dp))
                field("Bars Bottom", bottom, Modifier.weight(1f)) { bottom = it }
            }
            Spacer(Modifier.height(16.dp))
            Row {
                Button(onClick = {
                    val d = FractionUtils.parse(diameter); val h = ColumnMath.heightInches(height)
                    if (d <= 0 || h <= 0) { message = "Enter Diameter and Height"; return@Button }
                    val q = qty.toDoubleOrNull() ?: 1.0
                    val o = ColumnMath.compute(d, h, q, ties, vert.toDoubleOrNull() ?: 0.0, tieOc.toDoubleOrNull() ?: 12.0, top.toDoubleOrNull() ?: 0.0, bottom.toDoubleOrNull() ?: 0.0)
                    val entry = ColumnEntry(
                        id = if (editingIndex != -1) entries[editingIndex].id else (entries.size + 1).toString(),
                        name = name.trim().ifEmpty { "Column" }, diameter = diameter, height = height,
                        volume = "${StoryPoleMath.fixed(o.volYdEach * q, 4)} cu yd",
                        surfaceArea = "${StoryPoleMath.fixed(o.areaSqIn / 144.0, 2)} sq ft",
                        circumference = FractionUtils.formatInches(o.circumference),
                        qty = q.toInt().toString(), rebarFt = StoryPoleMath.fixed(o.rebarFt, 0),
                        rebarMode = if (ties) 0 else 1, vertBars = vert, tieOc = tieOc, barsTop = top, barsBottom = bottom
                    )
                    if (editingIndex != -1) { onUpdateEntries(entries.toMutableList().also { it[editingIndex] = entry }); editingIndex = -1 }
                    else onUpdateEntries(entries + entry)
                    message = "Column tabulated & saved"
                }, modifier = Modifier.weight(2f).height(56.dp), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)) {
                    Text(if (editingIndex != -1) "UPDATE ITEM" else "ADD TO LIST")
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { onUpdateEntries(emptyList()) }, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppTheme.colors.danger), colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.danger)) { Text("CLEAR LIST") }
            }
            if (message.isNotEmpty()) Text(message, fontSize = 13.sp, color = AppTheme.colors.muted, modifier = Modifier.padding(top = 6.dp))

            // Totals and list, like the Android table
            val totRebar = entries.sumOf { it.rebarFt.toDoubleOrNull() ?: 0.0 }
            val totVol = entries.sumOf { it.volume.substringBefore(" ").toDoubleOrNull() ?: 0.0 }
            Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(2.dp, AppTheme.colors.accent), modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Row(Modifier.padding(14.dp)) {
                    Text("${StoryPoleMath.fixed(totRebar, 0)}' Rebar Total", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = AppTheme.colors.ink)
                    Text("${StoryPoleMath.fixed(totVol, 4)} cu yd Total", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = AppTheme.colors.accent, textAlign = TextAlign.End)
                }
            }
            if (entries.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)) {
                    listOf("Name" to 1.4f, "Qty" to 0.5f, "Diameter" to 0.9f, "Vol (Yd)" to 1f, "Rebar" to 0.8f, "" to 1f).forEach { (t, w) ->
                        Text(t, Modifier.weight(w), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.muted)
                    }
                }
                entries.reversed().forEachIndexed { index, e ->
                    val idx = entries.size - 1 - index
                    Row(Modifier.fillMaxWidth().background(if (index % 2 == 1) AppTheme.colors.line.copy(alpha = 0.3f) else androidx.compose.ui.graphics.Color.Transparent).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(e.name, Modifier.weight(1.4f), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppTheme.colors.ink)
                        Text(e.qty, Modifier.weight(0.5f), fontSize = 14.sp, color = AppTheme.colors.ink)
                        Text(e.diameter, Modifier.weight(0.9f), fontSize = 14.sp, color = AppTheme.colors.ink)
                        Text(e.volume.substringBefore(" "), Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
                        Text(if (e.rebarFt.isBlank()) "-" else e.rebarFt + "'", Modifier.weight(0.8f), fontSize = 14.sp, color = AppTheme.colors.ink)
                        Row(Modifier.weight(1f)) {
                            IconButton(onClick = {
                                editingIndex = idx; name = e.name; qty = e.qty.ifBlank { "1" }; diameter = e.diameter; height = e.height
                                ties = e.rebarMode == 0; vert = e.vertBars; tieOc = e.tieOc; top = e.barsTop; bottom = e.barsBottom
                            }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTheme.colors.muted) }
                            IconButton(onClick = { onUpdateEntries(entries.filterIndexed { k, _ -> k != idx }) }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppTheme.colors.muted) }
                        }
                    }
                }
            }
        }
    }
}
