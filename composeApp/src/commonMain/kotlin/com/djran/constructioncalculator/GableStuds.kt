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
import androidx.compose.ui.graphics.PathEffect
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
 * on-center spacing give every stud length under the rake. Each stud changes by
 * O.C. x pitch/12 (the common difference). Studs are 1 1/2" thick, so the short point
 * is 1 1/2" x pitch/12 less than the long point.
 */
object GableStudEngine {
    const val STUD_THICKNESS = 1.5
    const val MAX_STUDS = 200

    data class Stud(val number: Int, val position: Double, val longPoint: Double, val shortPoint: Double)

    data class Result(
        val studs: List<Stud>,
        val commonDifference: Double,
        val cutAngleDeg: Double,
        val ascending: Boolean,
        val stoppedAtZero: Boolean
    )

    /**
     * [firstLength] is measured at the long point when [firstIsLongPoint], else at the short point.
     * The stud count comes from [count] when it is above zero, otherwise from [wallLength]
     * (studs at 0, O.C., 2 x O.C. ... up to the wall length). Descending runs stop before a
     * stud would have no length left.
     */
    fun calculate(
        firstLength: Double,
        pitch: Double,
        oc: Double,
        ascending: Boolean,
        firstIsLongPoint: Boolean,
        count: Int,
        wallLength: Double
    ): Result? {
        if (firstLength <= 0 || pitch <= 0 || oc <= 0) return null
        val k = pitch / 12.0
        val shortToLong = STUD_THICKNESS * k
        val firstLong = if (firstIsLongPoint) firstLength else firstLength + shortToLong
        val n = when {
            count > 0 -> count
            wallLength > 0 -> floor(wallLength / oc + 1e-6).toInt() + 1
            else -> return null
        }.coerceAtMost(MAX_STUDS)
        val diff = oc * k
        val studs = mutableListOf<Stud>()
        var stopped = false
        for (i in 0 until n) {
            val long = if (ascending) firstLong + i * diff else firstLong - i * diff
            val short = long - shortToLong
            if (short <= 0) { stopped = true; break }
            studs += Stud(i + 1, i * oc, long, short)
        }
        if (studs.isEmpty()) return null
        return Result(studs, diff, atan(k) * 180.0 / PI, ascending, stopped)
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

    val result = GableStudEngine.calculate(
        FractionUtils.parse(first),
        pitchToDouble(pitch, 0.0),
        FractionUtils.parse(oc),
        ascending,
        longPoint,
        countText.toIntOrNull() ?: 0,
        FractionUtils.parse(wall)
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
                StudToggle("Ascending (next is longer)", ascending, Modifier.weight(1f)) { ascending = true }
                Spacer(modifier = Modifier.width(8.dp))
                StudToggle("Descending (next is shorter)", !ascending, Modifier.weight(1f)) { ascending = false }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StudToggle("First measured at long point", longPoint, Modifier.weight(1f)) { longPoint = true }
                Spacer(modifier = Modifier.width(8.dp))
                StudToggle("First measured at short point", !longPoint, Modifier.weight(1f)) { longPoint = false }
            }
            Spacer(modifier = Modifier.height(8.dp))
            ConstructionTextField(value = oc, onValueChange = { oc = it }, label = "On center (O.C.)", modifier = Modifier.fillMaxWidth(), onFocus = onFocus)
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
                            "Each stud ${if (result.ascending) "grows" else "drops"} by",
                            FractionUtils.formatInches(result.commonDifference), isBold = true
                        )
                        ResultRow("Cut angle (miter saw)", "${result.cutAngleDeg.roundToOneDecimal()}°")
                        ResultRow("Long to short point", FractionUtils.formatInches(GableStudEngine.STUD_THICKNESS * pitchToDouble(pitch, 0.0) / 12.0))
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
                    "O.C. marks are measured from the first stud. Studs are 1 1/2\" thick; the long point is on the high side of the rake.",
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

/** Wall elevation: bottom plate, each stud to scale, and the rake line across the long points. */
@Composable
fun GableStudDiagram(r: GableStudEngine.Result, oc: Double) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier = Modifier.fillMaxWidth().height(280.dp).background(Color.White)) {
        val studs = r.studs
        val t = GableStudEngine.STUD_THICKNESS
        val padL = 24f; val padR = 24f; val padT = 34f; val padB = 44f
        val totalW = (studs.last().position + t).toFloat()
        val maxH = studs.maxOf { it.longPoint }.toFloat()
        val s = min((size.width - padL - padR) / totalW, (size.height - padT - padB) / maxH)
        val x0 = padL + ((size.width - padL - padR) - totalW * s) / 2f
        val yBase = size.height - padB

        // Bottom plate
        drawRect(Color(0xFF9E9E9E), topLeft = Offset(x0 - 4f, yBase), size = Size(totalW * s + 8f, 8f))

        // Studs: long point on the high side
        studs.forEach { st ->
            val left = x0 + (st.position * s).toFloat()
            val right = left + (t * s).toFloat()
            val highOnRight = r.ascending
            val yLong = yBase - (st.longPoint * s).toFloat()
            val yShort = yBase - (st.shortPoint * s).toFloat()
            val p = Path().apply {
                moveTo(left, yBase); lineTo(right, yBase)
                lineTo(right, if (highOnRight) yLong else yShort)
                lineTo(left, if (highOnRight) yShort else yLong)
                close()
            }
            drawPath(p, StudFill)
            drawPath(p, StudEdge, style = Stroke(1.5f))
        }

        // Rake line along the tops
        val first = studs.first(); val last = studs.last()
        val xa = x0; val xb = x0 + totalW * s
        val ya = yBase - ((if (r.ascending) first.shortPoint else first.longPoint) * s).toFloat()
        val yb = yBase - ((if (r.ascending) last.longPoint else last.shortPoint) * s).toFloat()
        drawLine(RakeColor, Offset(xa, ya), Offset(xb, yb), 4f)

        // Labels: lengths above studs when there's room, numbers below
        val label = TextStyle(fontSize = 10.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
        val small = TextStyle(fontSize = 10.sp, color = Color.Gray)
        val stepPx = (oc * s).toFloat()
        val every = max(1, ceil(64f / max(stepPx, 1f)).toInt())
        studs.forEachIndexed { i, st ->
            val cx = x0 + ((st.position + t / 2) * s).toFloat()
            if (i % every == 0 || i == studs.lastIndex) {
                val txt = FractionUtils.formatInches(st.longPoint)
                val lay = measurer.measure(txt, label)
                val yTop = yBase - (st.longPoint * s).toFloat()
                drawText(lay, topLeft = Offset(cx - lay.size.width / 2f, yTop - lay.size.height - 4f))
            }
            if (i % every == 0 || i == studs.lastIndex) {
                val lay = measurer.measure("#${st.number}", small)
                drawText(lay, topLeft = Offset(cx - lay.size.width / 2f, yBase + 12f))
            }
        }
        if (studs.size >= 2) {
            val y = size.height - 12f
            val a = x0 + (t / 2 * s).toFloat(); val b = a + (oc * s).toFloat()
            drawLine(Color.Gray, Offset(a, y), Offset(b, y), 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
        }
    }
}
