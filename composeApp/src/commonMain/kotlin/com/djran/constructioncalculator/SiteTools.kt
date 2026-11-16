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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.drawText
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

// ---------------- Roof Takeoff plan drawing (Android RoofDiagramView) ----------------

/** Roof plan, top down: perimeter, ridge, hips or valleys at each end, commons at OC, span/length and pitch. */
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoofPlan(
    span: Double, length: Double, pitch: Double, oc: Double, leftMode: Int, rightMode: Int,
    measurer: androidx.compose.ui.text.TextMeasurer
) {
    if (span <= 0 || length <= 0) return
    val w = size.width; val h = size.height
    val u = w / 400f
    val red = Color(0xFFD32F2F); val blue = Color(0xFF1D4ED8)
    fun txt(t: String, x: Float, y: Float, sz: Float, c: Color) {
        val m = measurer.measure(t, androidx.compose.ui.text.TextStyle(fontSize = (sz * u / density).sp, color = c, fontWeight = FontWeight.Bold))
        drawText(m, topLeft = androidx.compose.ui.geometry.Offset(x - m.size.width / 2f, y - m.size.height / 2f))
    }
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, c: Color = red, sw: Float = 1.6f) =
        drawLine(c, androidx.compose.ui.geometry.Offset(x1, y1), androidx.compose.ui.geometry.Offset(x2, y2), sw * u)
    fun arrow(x1: Float, y1: Float, x2: Float, y2: Float) {
        line(x1, y1, x2, y2, blue, 1f)
        val hl = 5 * u; val a = atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())
        for ((px, py, d) in listOf(Triple(x2, y2, -1.0), Triple(x1, y1, 1.0))) {
            line(px, py, (px + d * hl * cos(a - PI / 6)).toFloat(), (py + d * hl * sin(a - PI / 6)).toFloat(), blue, 1f)
            line(px, py, (px + d * hl * cos(a + PI / 6)).toFloat(), (py + d * hl * sin(a + PI / 6)).toFloat(), blue, 1f)
        }
    }
    drawRect(Color.White)
    txt("ROOF PLAN VIEW (TOP DOWN)", w / 2, 12 * u, 11f, Color(0xFF0A2A66))
    val scale = min((w - 70 * u) / length.toFloat(), (h - 80 * u) / span.toFloat())
    val sLen = length.toFloat() * scale; val sSpan = span.toFloat() * scale; val sOC = oc.toFloat() * scale
    val cx = w / 2; val cy = h / 2 + 6 * u
    val left = cx - sLen / 2; val right = cx + sLen / 2; val top = cy - sSpan / 2; val bottom = cy + sSpan / 2
    var rs = left; if (leftMode == 1) rs += sSpan / 2
    var re = right; if (rightMode == 1) re -= sSpan / 2
    drawRect(red, androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Size(sLen, sSpan), style = androidx.compose.ui.graphics.drawscope.Stroke(1.6f * u))
    if (re > rs) line(rs, cy, re, cy) else drawCircle(red, 2.5f * u, androidx.compose.ui.geometry.Offset(cx, cy))
    when (leftMode) { 1 -> { line(left, top, rs, cy); line(left, bottom, rs, cy) }; 2 -> { line(left, cy, rs + sSpan / 2, top); line(left, cy, rs + sSpan / 2, bottom) } }
    when (rightMode) { 1 -> { line(right, top, re, cy); line(right, bottom, re, cy) }; 2 -> { line(right, cy, re - sSpan / 2, top); line(right, cy, re - sSpan / 2, bottom) } }
    if (sOC > 3 * u) {
        var x = rs + sOC
        while (x < re - 2 * u) { line(x, top, x, cy, red, 0.8f); line(x, cy, x, bottom, red, 0.8f); x += sOC }
    }
    arrow(left, bottom + 14 * u, right, bottom + 14 * u)
    txt(FractionUtils.formatInches(length), cx, bottom + 24 * u, 9f, blue)
    arrow(left - 14 * u, top, left - 14 * u, bottom)
    rotate(-90f, androidx.compose.ui.geometry.Offset(left - 24 * u, cy)) { txt(FractionUtils.formatInches(span), left - 24 * u, cy, 9f, blue) }
    // pitch triangle
    val tw = 22 * u; val th = (pitch / 12.0 * tw).toFloat()
    val px = right - 26 * u; val py = top - 8 * u
    line(px, py, px + tw, py, red, 1f); line(px + tw, py, px + tw, py - th, red, 1f); line(px, py, px + tw, py - th, red, 1f)
    txt("12", px + tw / 2, py + 5 * u, 7f, Color.Black)
    txt(JackResults.pn(pitch), px + tw + 6 * u, py - th / 2, 7f, Color.Black)
}

@Composable
fun RoofPlanDrawing(span: Double, length: Double, pitch: Double, oc: Double, leftMode: Int, rightMode: Int) {
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(maxWidth * 0.7f)) { drawRoofPlan(span, length, pitch, oc, leftMode, rightMode, measurer) }
    }
}
