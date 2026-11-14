package com.djran.constructioncalculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * Gable Panel Cut: plywood for a gable end that dies into an upper roof.
 *
 * Inputs: distance to the stud (level run R, or measured along the bottom slope),
 * top pitch T (upper roof) and bottom pitch B (gable), both rise-per-12.
 * The panel has two vertical edges (outer end and stud end), a top edge at T and
 * a bottom edge at B, meeting a level datum line.
 */
object GableCutEngine {
    data class Result(
        val run: Double,          // level run, inches
        val studEndHeight: Double, // short vertical edge at the stud = R * T/12
        val outerEndHeight: Double, // tall vertical edge at the outer end = R * B/12
        val topCutLength: Double,
        val bottomCutLength: Double,
        val topAngleDeg: Double,
        val bottomAngleDeg: Double
    ) {
        /** Level bounding box of the panel (run x total height). */
        val panelHeight get() = studEndHeight + outerEndHeight

        // Panel laid on the sheet with the factory edge along the bottom (gable) slope.
        // "Along" runs down the factory edge from the outer bottom corner; "off" is square off it.
        /** Outer top corner (the level datum at the outer end). */
        val outerTopAlong get() = outerEndHeight * outerEndHeight / bottomCutLength
        val outerTopOff get() = run * outerEndHeight / bottomCutLength
        /** Stud top corner (where the top edge meets the stud). */
        val studTopAlong get() = bottomCutLength + outerEndHeight * studEndHeight / bottomCutLength
        val studTopOff get() = run * studEndHeight / bottomCutLength
        /** Sheet needed: length down the factory edge x width off it. */
        val sheetLength get() = maxOf(bottomCutLength, studTopAlong)
        val sheetWidth get() = maxOf(outerTopOff, studTopOff)
        val sheetsLong get() = ceil(sheetLength / 96.0 - 1e-9).toInt().coerceAtLeast(1)
        val sheetsWide get() = ceil(sheetWidth / 48.0 - 1e-9).toInt().coerceAtLeast(1)
        val sheetCount get() = sheetsLong * sheetsWide
        /** True when the piece fits on one 4x8 with the factory edge on the bottom slope. */
        val fitsOn4x8 get() = sheetCount == 1
    }

    fun calculate(distance: Double, topPitch: Double, bottomPitch: Double, measuredAlongSlope: Boolean): Result? {
        if (distance <= 0 || topPitch <= 0 || bottomPitch <= 0) return null
        val kT = topPitch / 12.0
        val kB = bottomPitch / 12.0
        val r = if (measuredAlongSlope) distance / sqrt(1 + kB * kB) else distance
        return Result(
            run = r,
            studEndHeight = r * kT,
            outerEndHeight = r * kB,
            topCutLength = r * sqrt(1 + kT * kT),
            bottomCutLength = r * sqrt(1 + kB * kB),
            topAngleDeg = atan(kT) * 180.0 / PI,
            bottomAngleDeg = atan(kB) * 180.0 / PI
        )
    }
}

private val GableTopColor = Color(0xFFD9661F)
private val GableBottomColor = Color(0xFF2F6F8F)
private val PlyFill = Color(0xFFE9D9B8)
private val PlyEdge = Color(0xFF7A5A22)

@Composable
fun GableCutCalculator(
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var distance by remember { mutableStateOf("80") }
    var topPitch by remember { mutableStateOf("5") }
    var bottomPitch by remember { mutableStateOf("7") }
    var alongSlope by remember { mutableStateOf(false) }

    val result = GableCutEngine.calculate(
        FractionUtils.parse(distance),
        pitchToDouble(topPitch, 0.0),
        pitchToDouble(bottomPitch, 0.0),
        alongSlope
    )

    ToolScreen(Screen.GableCut, onBack) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("GABLE UNDER AN UPPER ROOF", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            ConstructionTextField(
                value = distance,
                onValueChange = { distance = it },
                label = "Distance to stud (in or ft' in\")",
                modifier = Modifier.fillMaxWidth(),
                onFocus = onFocus
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = topPitch,
                    onValueChange = { topPitch = it },
                    label = "Top pitch (/12)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = bottomPitch,
                    onValueChange = { bottomPitch = it },
                    label = "Bottom pitch (/12)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ModeButton("Measured level", selected = !alongSlope, modifier = Modifier.weight(1f)) { alongSlope = false }
                Spacer(modifier = Modifier.width(8.dp))
                ModeButton("Along bottom slope", selected = alongSlope, modifier = Modifier.weight(1f)) { alongSlope = true }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (result == null) {
                Text("Enter a distance and both pitches above zero.", color = AppTheme.colors.muted, fontSize = 14.sp)
            } else {
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    GableCutDiagram(result, pitchToDouble(topPitch, 0.0), pitchToDouble(bottomPitch, 0.0))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), backgroundColor = AppTheme.colors.surface, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("CUT LIST", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        ResultRow("Horizontal run", FractionUtils.formatInches(result.run))
                        ResultRow("Outer end height", FractionUtils.formatInches(result.outerEndHeight), isBold = true)
                        ResultRow("Stud end height", FractionUtils.formatInches(result.studEndHeight), isBold = true)
                        ResultRow("Top cut length", FractionUtils.formatInches(result.topCutLength))
                        ResultRow("Bottom cut length", FractionUtils.formatInches(result.bottomCutLength))
                        ResultRow("Top pitch angle", "${result.topAngleDeg.roundToOneDecimal()}°")
                        ResultRow("Bottom pitch angle", "${result.bottomAngleDeg.roundToOneDecimal()}°")
                        ResultRow("Outer bottom corner", "${(90 - result.bottomAngleDeg).roundToOneDecimal()}°")
                        ResultRow("Stud top corner", "${(90 - result.topAngleDeg).roundToOneDecimal()}°")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), backgroundColor = AppTheme.colors.surface, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ON THE SHEET · FACTORY EDGE ON THE BOTTOM SLOPE", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        GableSheetDiagram(result)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (result.fitsOn4x8) "Fits on one 4x8 sheet."
                            else "Needs ${result.sheetCount} sheets (${result.sheetsLong} long x ${result.sheetsWide} wide). Plan a seam.",
                            color = if (result.fitsOn4x8) AppTheme.colors.success else GableTopColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ResultRow("Length down factory edge", FractionUtils.formatInches(result.sheetLength), isBold = true)
                        ResultRow("Width off factory edge", FractionUtils.formatInches(result.sheetWidth), isBold = true)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("LAYOUT MARKS (from the outer bottom corner)", fontWeight = FontWeight.Bold, color = AppTheme.colors.muted, fontSize = 11.sp)
                        ResultRow("Bottom cut", "factory edge, 0 to ${FractionUtils.formatInches(result.bottomCutLength)}")
                        ResultRow(
                            "Outer top corner",
                            "${FractionUtils.formatInches(result.outerTopAlong)} along, ${FractionUtils.formatInches(result.outerTopOff)} off"
                        )
                        ResultRow(
                            "Stud top corner",
                            "${FractionUtils.formatInches(result.studTopAlong)} along, ${FractionUtils.formatInches(result.studTopOff)} off"
                        )
                        Text(
                            "Measure along the factory edge, then square off it. Snap lines between the marks.",
                            fontSize = 12.sp,
                            color = AppTheme.colors.muted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Heights = run x pitch/12. Cut lengths = run x √(1 + (pitch/12)²).",
                    fontSize = 12.sp,
                    color = AppTheme.colors.muted
                )
            }
        }
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)
        ) { Text(label, fontSize = 13.sp, textAlign = TextAlign.Center) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, AppTheme.colors.line),
            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent, contentColor = AppTheme.colors.ink)
        ) { Text(label, fontSize = 13.sp, textAlign = TextAlign.Center) }
    }
}

/** Plywood panel drawing: level datum, top edge at top pitch, bottom edge at bottom pitch. */
@Composable
fun GableCutDiagram(r: GableCutEngine.Result, topPitch: Double, bottomPitch: Double) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier = Modifier.fillMaxWidth().height(300.dp).background(Color.White).padding(12.dp)) {
        val padL = 90f
        val padR = 90f
        val padT = 40f
        val padB = 50f
        val availW = (size.width - padL - padR).coerceAtLeast(10f)
        val availH = (size.height - padT - padB).coerceAtLeast(10f)
        val s = min(availW / r.run.toFloat(), availH / r.panelHeight.toFloat())
        val w = (r.run * s).toFloat()
        val hTop = (r.studEndHeight * s).toFloat()
        val hBot = (r.outerEndHeight * s).toFloat()
        val x0 = padL + (availW - w) / 2f
        val yTop = padT + (availH - (hTop + hBot)) / 2f
        val yDatum = yTop + hTop

        val a = Offset(x0, yDatum)          // outer end, at datum
        val b = Offset(x0, yDatum + hBot)   // outer end, bottom
        val c = Offset(x0 + w, yDatum)      // stud end, at datum
        val d = Offset(x0 + w, yTop)        // stud end, top

        val panel = Path().apply {
            moveTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); lineTo(a.x, a.y); close()
        }
        drawPath(panel, PlyFill)
        drawPath(panel, PlyEdge, style = Stroke(4f))

        drawLine(Color.Gray, a, c, 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
        drawLine(GableTopColor, a, d, 6f)
        drawLine(GableBottomColor, b, c, 6f)

        val small = TextStyle(fontSize = 12.sp, color = Color.DarkGray)
        val boldTop = TextStyle(fontSize = 12.sp, color = GableTopColor, fontWeight = FontWeight.Bold)
        val boldBot = TextStyle(fontSize = 12.sp, color = GableBottomColor, fontWeight = FontWeight.Bold)
        val boldDark = TextStyle(fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)

        labelCentered(measurer, "level ${FractionUtils.formatInches(r.run)}", Offset((a.x + c.x) / 2f, yDatum - 16f), small)

        // Sloped labels, rotated to follow each edge
        val angTop = radToDegF(atan2(d.y - a.y, d.x - a.x))
        val midTop = Offset((a.x + d.x) / 2f, (a.y + d.y) / 2f)
        rotate(angTop, midTop) {
            labelCentered(measurer, "${fmtPitch(topPitch)}/12 · ${FractionUtils.formatInches(r.topCutLength)}", Offset(midTop.x, midTop.y - 18f), boldTop)
        }
        val angBot = radToDegF(atan2(c.y - b.y, c.x - b.x))
        val midBot = Offset((b.x + c.x) / 2f, (b.y + c.y) / 2f)
        rotate(angBot, midBot) {
            labelCentered(measurer, "${fmtPitch(bottomPitch)}/12 · ${FractionUtils.formatInches(r.bottomCutLength)}", Offset(midBot.x, midBot.y + 18f), boldBot)
        }

        // Vertical dimensions
        with(DrawingUtils) {
            drawArrow(Offset(x0 - 24f, a.y), Offset(x0 - 24f, b.y), Color.DarkGray, 2f)
            drawArrow(Offset(c.x + 24f, d.y), Offset(c.x + 24f, c.y), Color.DarkGray, 2f)
        }
        labelRightAligned(measurer, FractionUtils.formatInches(r.outerEndHeight), Offset(x0 - 32f, (a.y + b.y) / 2f), boldDark)
        labelLeftAligned(measurer, FractionUtils.formatInches(r.studEndHeight), Offset(c.x + 32f, (d.y + c.y) / 2f), boldDark)

        labelCentered(measurer, "OUTER END", Offset(x0, size.height - 14f), small)
        labelCentered(measurer, "STUD / WALL", Offset(c.x, size.height - 14f), small)
    }
}

/** The panel laid on 4x8 sheet(s) with the factory edge along the bottom slope. */
@Composable
fun GableSheetDiagram(r: GableCutEngine.Result) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier = Modifier.fillMaxWidth().height(200.dp).background(Color.White)) {
        val pad = 28f
        val totalL = (r.sheetsLong * 96.0).toFloat()
        val totalW = (r.sheetsWide * 48.0).toFloat()
        val s = min((size.width - 2 * pad) / totalL, (size.height - 2 * pad) / totalW)
        val x0 = (size.width - totalL * s) / 2f
        val yBase = size.height - (size.height - totalW * s) / 2f   // factory edge line
        fun pt(along: Double, off: Double) = Offset(x0 + (along * s).toFloat(), yBase - (off * s).toFloat())

        for (i in 0 until r.sheetsLong) for (j in 0 until r.sheetsWide) {
            drawRect(
                Color.Gray,
                topLeft = Offset(x0 + i * 96f * s, yBase - (j + 1) * 48f * s),
                size = androidx.compose.ui.geometry.Size(96f * s, 48f * s),
                style = Stroke(1.5f)
            )
        }
        val b = pt(0.0, 0.0)
        val c = pt(r.bottomCutLength, 0.0)
        val d = pt(r.studTopAlong, r.studTopOff)
        val a = pt(r.outerTopAlong, r.outerTopOff)
        val panel = Path().apply { moveTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); lineTo(a.x, a.y); close() }
        drawPath(panel, PlyFill)
        drawPath(panel, PlyEdge, style = Stroke(3f))
        drawLine(GableBottomColor, b, c, 6f)
        drawLine(GableTopColor, a, d, 6f)

        val small = TextStyle(fontSize = 11.sp, color = Color.DarkGray)
        labelCentered(measurer, "FACTORY EDGE", Offset(x0 + totalL * s / 2f, yBase + 12f), small)
    }
}

private fun radToDegF(rad: Float): Float = (rad * 180.0 / PI).toFloat()

private fun fmtPitch(p: Double): String = if (p == floor(p)) p.toInt().toString() else p.roundToOneDecimal().toString()

private fun DrawScope.labelCentered(m: TextMeasurer, text: String, center: Offset, style: TextStyle) {
    val layout = m.measure(text, style)
    drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
}

private fun DrawScope.labelRightAligned(m: TextMeasurer, text: String, anchor: Offset, style: TextStyle) {
    val layout = m.measure(text, style)
    drawText(layout, topLeft = Offset(anchor.x - layout.size.width, anchor.y - layout.size.height / 2f))
}

private fun DrawScope.labelLeftAligned(m: TextMeasurer, text: String, anchor: Offset, style: TextStyle) {
    val layout = m.measure(text, style)
    drawText(layout, topLeft = Offset(anchor.x, anchor.y - layout.size.height / 2f))
}
