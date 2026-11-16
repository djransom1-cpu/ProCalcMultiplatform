package com.djran.constructioncalculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import kotlin.math.*

/*
 * Grade / Slope and Land Surveying, matching the Android app (SiteSurveyActivity and
 * LandSurveyActivity), saved per project and shown on the Data Sheet.
 */

@Serializable
data class GradeEntry(val name: String, val distance: String, val slope: String, val slopePercent: String, val totalFall: String)

@Serializable
data class LandSurveyEntry(val note: String, val data: String)

object SiteMath {
    /** Fall over a horizontal distance: slope per foot and percent grade. Distance and fall in inches. */
    fun grade(distIn: Double, fallIn: Double): Pair<Double, Double> = (fallIn / (distIn / 12.0)) to (fallIn / distIn * 100.0)

    fun acres(sqFt: Double) = sqFt / 43560.0

    /** Slope distance and vertical angle (degrees) to horizontal and vertical distance. */
    fun resolve(slopeDist: Double, angleDeg: Double): Pair<Double, Double> {
        val a = angleDeg * PI / 180.0
        return slopeDist * cos(a) to slopeDist * sin(a)
    }

    /** Distance and quadrant bearing from point 1 to point 2 (North/East coordinates). */
    fun inverse(n1: Double, e1: Double, n2: Double, e2: Double): Pair<Double, String>? {
        val dn = n2 - n1; val de = e2 - e1
        if (abs(dn) <= 0.0001 && abs(de) <= 0.0001) return null
        val az = (atan2(de, dn) * 180.0 / PI + 360.0) % 360.0
        return sqrt(dn * dn + de * de) to bearing(az)
    }

    fun bearing(az: Double): String = when {
        az <= 90.0 -> "N ${dms(az)} E"
        az <= 180.0 -> "S ${dms(180.0 - az)} E"
        az <= 270.0 -> "S ${dms(az - 180.0)} W"
        else -> "N ${dms(360.0 - az)} W"
    }

    fun dms(dec: Double): String {
        val d = floor(dec).toInt()
        val mf = (dec - d) * 60.0
        val m = floor(mf).toInt()
        val s = ((mf - m) * 60.0).roundToInt()
        return "$d° ${m.toString().padStart(2, '0')}' ${s.toString().padStart(2, '0')}\""
    }
}

@Composable
fun GradeSlopeCalculator(
    entries: List<GradeEntry>,
    onUpdateEntries: (List<GradeEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }
    var fall by remember { mutableStateOf("") }
    var slopePerFt by remember { mutableStateOf("") }

    ToolScreen(Screen.GradeSlope, onBack) {
        Column(Modifier.padding(16.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Section/Point Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row {
                ConstructionTextField(value = distance, onValueChange = { distance = it }, label = "Horizontal Dist (ft)", modifier = Modifier.weight(1f), onFocus = onFocus)
                Spacer(Modifier.width(8.dp))
                ConstructionTextField(value = fall, onValueChange = { fall = it }, label = "Total Fall (in)", modifier = Modifier.weight(1f), onFocus = onFocus)
            }
            Spacer(Modifier.height(8.dp))
            ConstructionTextField(value = slopePerFt, onValueChange = { slopePerFt = it }, label = "Known Slope per foot (in)", modifier = Modifier.fillMaxWidth(), onFocus = onFocus)
            SpHint("Enter the distance and either the total fall or a known slope per foot.")
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                val distIn = FractionUtils.parseFeet(distance)
                var fallIn = FractionUtils.parse(fall)
                if (fallIn == 0.0) fallIn = FractionUtils.parse(slopePerFt) * distIn / 12.0
                if (distIn > 0 && fallIn != 0.0) {
                    val (perFt, pct) = SiteMath.grade(distIn, fallIn)
                    onUpdateEntries(entries + GradeEntry(name.ifEmpty { "Point ${entries.size + 1}" }, FractionUtils.formatInches(distIn),
                        FractionUtils.formatInches(perFt), "${StoryPoleMath.fixed(pct, 2)}%", FractionUtils.formatInches(fallIn)))
                    distance = ""; fall = ""; name = ""; slopePerFt = ""
                }
            }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)) { Text("ADD TO LIST") }

            if (entries.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp)) {
                    listOf("Name" to 1.3f, "Distance" to 1f, "Fall" to 1f, "Slope" to 1f, "%" to 0.8f, "" to 0.5f).forEach { (t, w) ->
                        Text(t, Modifier.weight(w), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.muted)
                    }
                }
                entries.forEachIndexed { i, e ->
                    Row(Modifier.fillMaxWidth().background(if (i % 2 == 1) AppTheme.colors.line.copy(alpha = 0.3f) else Color.Transparent).padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(e.name, Modifier.weight(1.3f), fontWeight = FontWeight.Bold, color = AppTheme.colors.ink)
                        Text(e.distance, Modifier.weight(1f), color = AppTheme.colors.ink)
                        Text(e.totalFall, Modifier.weight(1f), color = AppTheme.colors.ink)
                        Text(e.slope, Modifier.weight(1f), fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
                        Text(e.slopePercent, Modifier.weight(0.8f), color = AppTheme.colors.ink)
                        IconButton(onClick = { onUpdateEntries(entries.filterIndexed { k, _ -> k != i }) }, modifier = Modifier.weight(0.5f)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppTheme.colors.muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LandSurveyCalculator(
    entries: List<LandSurveyEntry>,
    onUpdateEntries: (List<LandSurveyEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var sqFt by remember { mutableStateOf("") }
    var slopeDist by remember { mutableStateOf("") }
    var vertAngle by remember { mutableStateOf("") }
    var n1 by remember { mutableStateOf("") }; var e1 by remember { mutableStateOf("") }
    var n2 by remember { mutableStateOf("") }; var e2 by remember { mutableStateOf("") }
    var acreRes by remember { mutableStateOf("0.000 Acres") }
    var slopeRes by remember { mutableStateOf("HD: 0 | VD: 0") }
    var invRes by remember { mutableStateOf("Dist: 0 | Brg: -") }
    var showSave by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }

    @Composable
    fun result(t: String) = Text(t, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, modifier = Modifier.padding(vertical = 6.dp))
    @Composable
    fun head(t: String) = Text(t, color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))

    ToolScreen(Screen.LandSurvey, onBack) {
        Column(Modifier.padding(16.dp)) {
            head("ACREAGE CONVERTER")
            ConstructionTextField(value = sqFt, onValueChange = { sqFt = it }, label = "Total Square Feet", modifier = Modifier.fillMaxWidth(), onFocus = onFocus)
            result(acreRes)
            head("SLOPE & VERTICAL RESOLVE")
            Row {
                ConstructionTextField(value = slopeDist, onValueChange = { slopeDist = it }, label = "Slope Dist (ft)", modifier = Modifier.weight(1f), onFocus = onFocus)
                Spacer(Modifier.width(8.dp))
                ConstructionTextField(value = vertAngle, onValueChange = { vertAngle = it }, label = "Vert Angle (°)", modifier = Modifier.weight(1f), onFocus = onFocus)
            }
            result(slopeRes)
            head("POINT-TO-POINT (BEARING)")
            Text("From (Pt 1):", fontSize = 13.sp, color = AppTheme.colors.muted)
            Row {
                ConstructionTextField(value = n1, onValueChange = { n1 = it }, label = "North", modifier = Modifier.weight(1f), onFocus = onFocus)
                Spacer(Modifier.width(8.dp))
                ConstructionTextField(value = e1, onValueChange = { e1 = it }, label = "East", modifier = Modifier.weight(1f), onFocus = onFocus)
            }
            Text("To (Pt 2):", fontSize = 13.sp, color = AppTheme.colors.muted, modifier = Modifier.padding(top = 6.dp))
            Row {
                ConstructionTextField(value = n2, onValueChange = { n2 = it }, label = "North", modifier = Modifier.weight(1f), onFocus = onFocus)
                Spacer(Modifier.width(8.dp))
                ConstructionTextField(value = e2, onValueChange = { e2 = it }, label = "East", modifier = Modifier.weight(1f), onFocus = onFocus)
            }
            result(invRes)
            Row(Modifier.padding(top = 8.dp)) {
                Button(onClick = {
                    val sf = FractionUtils.parse(sqFt)
                    if (sf > 0) acreRes = "${StoryPoleMath.fixed(SiteMath.acres(sf), 3)} Acres"
                    val sd = FractionUtils.parse(slopeDist)
                    if (sd > 0) {
                        val (hd, vd) = SiteMath.resolve(sd, vertAngle.toDoubleOrNull() ?: 0.0)
                        slopeRes = "HD: ${FractionUtils.formatInches(hd)} | VD: ${FractionUtils.formatInches(vd)}"
                    }
                    SiteMath.inverse(n1.toDoubleOrNull() ?: 0.0, e1.toDoubleOrNull() ?: 0.0, n2.toDoubleOrNull() ?: 0.0, e2.toDoubleOrNull() ?: 0.0)?.let { (d, brg) ->
                        invRes = "Dist: ${StoryPoleMath.fixed(d, 3)}' | Brg: $brg"
                    }
                }, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)) { Text("CALCULATE SOLUTIONS") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { note = ""; showSave = true }, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(12.dp)) { Text("SAVE TO PROJECT") }
            }
            if (entries.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp)) {
                    Text("Note", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.muted)
                    Text("Solution Data", Modifier.weight(2.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.muted)
                }
                entries.forEachIndexed { i, e ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(e.note, Modifier.weight(1f), fontWeight = FontWeight.Bold, color = AppTheme.colors.ink)
                        Text(e.data, Modifier.weight(2f), fontSize = 13.sp, color = AppTheme.colors.ink)
                        IconButton(onClick = { onUpdateEntries(entries.filterIndexed { k, _ -> k != i }) }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppTheme.colors.muted) }
                    }
                    Divider()
                }
            }
        }
    }

    if (showSave) {
        AlertDialog(
            onDismissRequest = { showSave = false },
            title = { Text("Save Solution") },
            text = { OutlinedTextField(value = note, onValueChange = { note = it }, placeholder = { Text("Note (e.g. Back property line)") }, singleLine = true) },
            confirmButton = {
                Button(onClick = {
                    val data = buildString {
                        if (acreRes != "0.000 Acres") append("Area: $acreRes | ")
                        if (slopeRes != "HD: 0 | VD: 0") append("Slope: $slopeRes | ")
                        if (invRes != "Dist: 0 | Brg: -") append("Inverse: $invRes")
                    }.trimEnd(' ', '|')
                    if (data.isNotBlank()) onUpdateEntries(entries + LandSurveyEntry(note.ifBlank { "Survey Result" }, data))
                    showSave = false
                }) { Text("SAVE") }
            },
            dismissButton = { TextButton(onClick = { showSave = false }) { Text("Cancel") } }
        )
    }
}

/** Data Sheet sections for grade/slope points and land survey solutions. */
@Composable
fun SiteToolsDataSheet(grades: List<GradeEntry>, surveys: List<LandSurveyEntry>) {
    if (grades.isNotEmpty()) {
        SummaryTable("GRADE / SLOPE", listOf("Name", "Distance", "Fall", "Slope / ft", "%")) {
            grades.forEach { g ->
                Row(Modifier.padding(vertical = 8.dp)) {
                    TableCell(g.name, 120.dp, isBold = true); TableCell(g.distance, 90.dp); TableCell(g.totalFall, 80.dp)
                    TableCell(g.slope, 90.dp); TableCell(g.slopePercent, 70.dp)
                }
                Divider()
            }
        }
    }
    if (surveys.isNotEmpty()) {
        SummaryTable("LAND SURVEYING", listOf("Note", "Solution")) {
            surveys.forEach { s ->
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text(s.note, fontWeight = FontWeight.Bold, color = Color(0xFF0A2A66))
                    Text(s.data, fontSize = 13.sp, color = Color.Black)
                }
                Divider()
            }
        }
    }
}

fun siteToolsReport(grades: List<GradeEntry>, surveys: List<LandSurveyEntry>): List<SummarySection> = buildList {
    if (grades.isNotEmpty()) add(SummarySection("GRADE / SLOPE", listOf("Name", "Distance", "Fall", "Slope / ft", "%"),
        grades.map { listOf(it.name, it.distance, it.totalFall, it.slope, it.slopePercent) }))
    if (surveys.isNotEmpty()) add(SummarySection("LAND SURVEYING", listOf("Note", "Solution"), surveys.map { listOf(it.note, it.data) }))
}
