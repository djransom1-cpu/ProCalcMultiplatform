package com.djran.constructioncalculator

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * Gable studs: the first stud's length, the roof pitch, ascending or descending, and the
 * on-center spacing give every stud length under the rake. The bottom can sit on a level
 * plate or on a sloped one (a lower roof, like the Gable Panel Cut), sloping the same way
 * as the rake or the opposite way. Each stud changes by O.C. x (top slope - bottom slope).
 * Studs are 1 1/2" thick, so the two edges of a stud differ by 1 1/2" x that same slope.
 */
object GableStudEngine {
    const val STUD_THICKNESS = 1.5
    const val MAX_STUDS = 200

    enum class Bottom { Level, SameWay, OppositeWay }

    /** [leftEdge]/[rightEdge] are the stud's two edges; [bottomRise] is the plate height under its left edge. */
    data class Stud(
        val number: Int,
        val position: Double,
        val leftEdge: Double,
        val rightEdge: Double,
        val bottomRise: Double
    ) {
        val longPoint get() = max(leftEdge, rightEdge)
        val shortPoint get() = min(leftEdge, rightEdge)
    }

    data class Result(
        val studs: List<Stud>,
        val commonDifference: Double,   // signed change per stud (long point to long point)
        val topCutDeg: Double,
        val bottomCutDeg: Double,       // 0 when the bottom is level
        val topSlope: Double,           // signed rise per inch, + = rises to the right
        val bottomSlope: Double,
        val edgeDifference: Double,     // long point minus short point
        val stoppedAtZero: Boolean
    )

    fun calculate(
        firstLength: Double,
        pitch: Double,
        oc: Double,
        ascending: Boolean,
        firstIsLongPoint: Boolean,
        count: Int,
        wallLength: Double,
        bottom: Bottom = Bottom.Level,
        bottomPitch: Double = 0.0
    ): Result? {
        if (firstLength <= 0 || pitch <= 0 || oc <= 0) return null
        if (bottom != Bottom.Level && bottomPitch <= 0) return null
        val dir = if (ascending) 1.0 else -1.0
        val st = dir * pitch / 12.0
        val sb = when (bottom) {
            Bottom.Level -> 0.0
            Bottom.SameWay -> dir * bottomPitch / 12.0
            Bottom.OppositeWay -> -dir * bottomPitch / 12.0
        }
        val perInch = st - sb                       // change in stud length per inch along the wall
        val edgeDiff = STUD_THICKNESS * perInch     // right edge minus left edge
        val firstLeft = when {
            firstIsLongPoint && edgeDiff >= 0 -> firstLength - edgeDiff
            firstIsLongPoint -> firstLength
            edgeDiff >= 0 -> firstLength
            else -> firstLength + edgeDiff
        }
        val n = when {
            count > 0 -> count
            wallLength > 0 -> floor(wallLength / oc + 1e-6).toInt() + 1
            else -> return null
        }.coerceAtMost(MAX_STUDS)
        val studs = mutableListOf<Stud>()
        var stopped = false
        for (i in 0 until n) {
            val x = i * oc
            val left = firstLeft + x * perInch
            val right = left + edgeDiff
            if (min(left, right) <= 0) { stopped = true; break }
            studs += Stud(i + 1, x, left, right, x * sb)
        }
        if (studs.isEmpty()) return null
        return Result(
            studs = studs,
            commonDifference = oc * perInch,
            topCutDeg = atan(pitch / 12.0) * 180.0 / PI,
            bottomCutDeg = if (bottom == Bottom.Level) 0.0 else atan(bottomPitch / 12.0) * 180.0 / PI,
            topSlope = st,
            bottomSlope = sb,
            edgeDifference = abs(edgeDiff),
            stoppedAtZero = stopped
        )
    }
}

private val StudFill = Color(0xFFE9D9B8)
private val StudEdge = Color(0xFF7A5A22)
private val RakeColor = Color(0xFFD9661F)

@Composable
fun GableStudCalculator(
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var first by remember { mutableStateOf("24") }
    var pitch by remember { mutableStateOf("6") }
    var oc by remember { mutableStateOf("16") }
    var countText by remember { mutableStateOf("6") }
    var wall by remember { mutableStateOf("") }
    var ascending by remember { mutableStateOf(true) }
    var longPoint by remember { mutableStateOf(true) }
    var bottom by remember { mutableStateOf(GableStudEngine.Bottom.Level) }
    var bottomPitch by remember { mutableStateOf("7") }

    val result = GableStudEngine.calculate(
        FractionUtils.parse(first),
        pitchToDouble(pitch, 0.0),
        FractionUtils.parse(oc),
        ascending,
        longPoint,
        countText.toIntOrNull() ?: 0,
        FractionUtils.parse(wall),
        bottom,
        pitchToDouble(bottomPitch, 0.0)
    )

    ToolScreen(Screen.GableStuds, onBack) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("STUDS UNDER A GABLE RAKE", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(value = first, onValueChange = { first = it }, label = "First stud length", modifier = Modifier.weight(1f), onFocus = onFocus)
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(value = pitch, onValueChange = { pitch = it }, label = "Roof pitch (/12)", modifier = Modifier.weight(1f), onFocus = onFocus)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StudToggle("Ascending (rake rises)", ascending, Modifier.weight(1f)) { ascending = true }
                Spacer(modifier = Modifier.width(8.dp))
                StudToggle("Descending (rake drops)", !ascending, Modifier.weight(1f)) { ascending = false }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StudToggle("First measured at long point", longPoint, Modifier.weight(1f)) { longPoint = true }
                Spacer(modifier = Modifier.width(8.dp))
                StudToggle("First measured at short point", !longPoint, Modifier.weight(1f)) { longPoint = false }
            }
            Spacer(modifier = Modifier.height(8.dp))
            ConstructionTextField(value = oc, onValueChange = { oc = it }, label = "On center (O.C.)", modifier = Modifier.fillMaxWidth(), onFocus = onFocus)
            Spacer(modifier = Modifier.height(12.dp))
            Text("BOTTOM", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StudToggle("Level plate", bottom == GableStudEngine.Bottom.Level, Modifier.weight(1f)) { bottom = GableStudEngine.Bottom.Level }
                Spacer(modifier = Modifier.width(6.dp))
                StudToggle("Sloped, same way as rake", bottom == GableStudEngine.Bottom.SameWay, Modifier.weight(1f)) { bottom = GableStudEngine.Bottom.SameWay }
                Spacer(modifier = Modifier.width(6.dp))
                StudToggle("Sloped, opposite way", bottom == GableStudEngine.Bottom.OppositeWay, Modifier.weight(1f)) { bottom = GableStudEngine.Bottom.OppositeWay }
            }
            if (bottom != GableStudEngine.Bottom.Level) {
                Spacer(modifier = Modifier.height(8.dp))
                ConstructionTextField(value = bottomPitch, onValueChange = { bottomPitch = it }, label = "Bottom pitch (/12)", modifier = Modifier.fillMaxWidth(), onFocus = onFocus)
                Text(
                    "Same way: the bottom rises the same direction as the rake, like the Gable Panel Cut. Opposite way: it drops as the rake rises.",
                    color = AppTheme.colors.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Number of studs") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(value = wall, onValueChange = { wall = it }, label = "or Wall length", modifier = Modifier.weight(1f), onFocus = onFocus)
            }
            Text(
                "Number of studs wins when both are filled in. Plain numbers are inches; use ' for feet.",
                color = AppTheme.colors.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            if (result == null) {
                Text("Enter the first stud, pitch, O.C., and a number of studs or a wall length.", color = AppTheme.colors.muted, fontSize = 14.sp)
            } else {
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    GableStudDiagram(result, FractionUtils.parse(oc))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), backgroundColor = AppTheme.colors.surface, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("SUMMARY", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 12.sp)
                        ResultRow("Studs", "${result.studs.size}", isBold = true)
                        ResultRow(
                            "Each stud ${if (result.commonDifference >= 0) "grows" else "drops"} by",
                            FractionUtils.formatInches(abs(result.commonDifference)), isBold = true
                        )
                        ResultRow("Top cut angle (miter saw)", "${result.topCutDeg.roundToOneDecimal()}°")
                        if (result.bottomCutDeg > 0) ResultRow("Bottom cut angle (miter saw)", "${result.bottomCutDeg.roundToOneDecimal()}°")
                        ResultRow("Long to short point", FractionUtils.formatInches(result.edgeDifference))
                        ResultRow("Shortest stud (long point)", FractionUtils.formatInches(result.studs.minOf { it.longPoint }))
                        ResultRow("Longest stud (long point)", FractionUtils.formatInches(result.studs.maxOf { it.longPoint }))
                        if (result.stoppedAtZero) {
                            Text(
                                "Stopped at stud ${result.studs.size}: the next one would have no length left.",
                                color = RakeColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                MarksTable(
                    listOf("#", "O.C. mark", "Long point", "Short point"),
                    result.studs.map {
                        listOf(
                            "${it.number}",
                            FractionUtils.formatInches(it.position),
                            FractionUtils.formatInches(it.longPoint),
                            FractionUtils.formatInches(it.shortPoint)
                        )
                    }
                )
                Text(
                    "O.C. marks are measured from the first stud, level along the wall. Studs are 1 1/2\" thick; lengths are edge to edge, top cut to bottom cut.",
                    color = AppTheme.colors.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun StudToggle(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = 48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)
        ) { Text(label, fontSize = 12.sp, textAlign = TextAlign.Center) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 48.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, AppTheme.colors.line),
            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent, contentColor = AppTheme.colors.ink)
        ) { Text(label, fontSize = 12.sp, textAlign = TextAlign.Center) }
    }
}

/** Wall elevation: the plate (level or sloped), each stud to scale, and the rake line across the tops. */
@Composable
fun GableStudDiagram(r: GableStudEngine.Result, oc: Double) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier = Modifier.fillMaxWidth().height(280.dp).background(Color.White)) {
        val studs = r.studs
        val t = GableStudEngine.STUD_THICKNESS
        val padL = 24f; val padR = 24f; val padT = 34f; val padB = 44f
        val totalW = studs.last().position + t
        // Heights in wall coordinates: bottom of a point = x * sb; top = bottom + edge length
        fun bottomAt(x: Double) = x * r.bottomSlope
        var minY = 0.0; var maxY = 0.0
        studs.forEach { st ->
            for ((x, len) in listOf(st.position to st.leftEdge, st.position + t to st.rightEdge)) {
                minY = min(minY, bottomAt(x)); maxY = max(maxY, bottomAt(x) + len)
            }
        }
        val spanY = (maxY - minY).coerceAtLeast(1.0)
        val s = min((size.width - padL - padR) / totalW.toFloat(), (size.height - padT - padB) / spanY.toFloat())
        val x0 = padL + ((size.width - padL - padR) - totalW.toFloat() * s) / 2f
        val yZero = size.height - padB + (minY * s).toFloat()
        fun px(x: Double) = x0 + (x * s).toFloat()
        fun py(y: Double) = yZero - (y * s).toFloat()

        // Plate under the studs
        val plate = Path().apply {
            moveTo(px(0.0), py(bottomAt(0.0))); lineTo(px(totalW), py(bottomAt(totalW)))
            lineTo(px(totalW), py(bottomAt(totalW)) + 8f); lineTo(px(0.0), py(bottomAt(0.0)) + 8f); close()
        }
        drawPath(plate, Color(0xFF9E9E9E))

        studs.forEach { st ->
            val xl = st.position; val xr = st.position + t
            val p = Path().apply {
                moveTo(px(xl), py(bottomAt(xl))); lineTo(px(xr), py(bottomAt(xr)))
                lineTo(px(xr), py(bottomAt(xr) + st.rightEdge)); lineTo(px(xl), py(bottomAt(xl) + st.leftEdge)); close()
            }
            drawPath(p, StudFill)
            drawPath(p, StudEdge, style = Stroke(1.5f))
        }

        val first = studs.first(); val last = studs.last()
        drawLine(
            RakeColor,
            Offset(px(0.0), py(bottomAt(0.0) + first.leftEdge)),
            Offset(px(totalW), py(bottomAt(totalW) + last.rightEdge)),
            4f
        )

        val label = TextStyle(fontSize = 10.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
        val small = TextStyle(fontSize = 10.sp, color = Color.Gray)
        val stepPx = (oc * s).toFloat()
        val every = max(1, ceil(64f / max(stepPx, 1f)).toInt())
        studs.forEachIndexed { i, st ->
            if (i % every != 0 && i != studs.lastIndex) return@forEachIndexed
            val xc = st.position + t / 2
            val cx = px(xc)
            val txt = FractionUtils.formatInches(st.longPoint)
            val lay = measurer.measure(txt, label)
            val yTop = py(bottomAt(st.position + if (st.rightEdge >= st.leftEdge) t else 0.0) + st.longPoint)
            drawText(lay, topLeft = Offset(cx - lay.size.width / 2f, yTop - lay.size.height - 4f))
            val num = measurer.measure("#${st.number}", small)
            drawText(num, topLeft = Offset(cx - num.size.width / 2f, py(bottomAt(xc)) + 12f))
        }
    }
}
