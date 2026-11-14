package com.djran.constructioncalculator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

private val ArcColor = Color(0xFF1565C0)
private val SpringColor = Color(0xFFD84315)
private val OffsetColor = Color(0xFF2E7D32)

/**
 * Scaled arc sketch: chord as a level baseline, the arc above it, the spring at
 * mid-chord, and a tick at each layout station with its offset labeled. Flat arcs
 * get an exaggerated vertical scale (noted on the drawing) so the curve stays visible.
 */
@Composable
fun ArcDiagram(sol: ArcMath.Solution, stations: List<ArcMath.Station>) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier = Modifier.fillMaxWidth().height(280.dp).background(Color.White).padding(12.dp)) {
        drawArcDiagram(sol, stations, measurer)
    }
}

/** The arc sketch itself, shared by the screen and the PDF report. */
fun DrawScope.drawArcDiagram(sol: ArcMath.Solution, stations: List<ArcMath.Station>, measurer: TextMeasurer) {
    val padL = 24f
    val padR = 24f
    val padT = 64f   // room for station labels above the arc
    val padB = 44f   // room for the chord label
    val availW = (size.width - padL - padR).coerceAtLeast(10f)
    val availH = (size.height - padT - padB).coerceAtLeast(10f)

    val c = sol.chord
    val s = sol.spring
    val sx = min(availW / c, availH / s).toFloat()
    // Flat arc: stretch the vertical so the rise fills at least 40% of the height
    val minRise = availH * 0.4f
    val sy = if (s * sx < minRise) (minRise / s).toFloat() else sx
    val exaggeration = sy / sx

    val w = (c * sx).toFloat()
    val x0 = padL + (availW - w) / 2f
    val yBase = padT + availH - ((availH - s * sy).toFloat()) / 2f

    fun pt(along: Double, up: Double) = Offset(x0 + (along * sx).toFloat(), yBase - (up * sy).toFloat())

    // Chord baseline
    drawLine(Color.DarkGray, pt(0.0, 0.0), pt(c, 0.0), 3f)

    // Arc as a polyline from the offset function, so it follows the vertical exaggeration
    val steps = 120
    val arc = Path()
    for (i in 0..steps) {
        val along = c * i / steps
        val p = pt(along, ArcMath.offsetAt(sol, along - c / 2.0))
        if (i == 0) arc.moveTo(p.x, p.y) else arc.lineTo(p.x, p.y)
    }
    drawPath(arc, ArcColor, style = Stroke(5f))

    // Station offsets
    val small = TextStyle(fontSize = 11.sp, color = OffsetColor, fontWeight = FontWeight.Bold)
    val spacingPx = if (stations.size > 1) w / (stations.size - 1) else w
    for (st in stations) {
        val foot = pt(st.along, 0.0)
        val top = pt(st.along, st.offset)
        drawLine(Color.DarkGray, Offset(foot.x, foot.y - 6f), Offset(foot.x, foot.y + 6f), 2f)
        if (st.offset > 0.0) {
            drawLine(OffsetColor, foot, top, 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
        }
        val text = FractionUtils.formatInches(st.offset, 8)
        val layout = measurer.measure(text, small, density = this)
        val anchor = Offset(top.x, top.y - 10f)
        if (layout.size.width > spacingPx * 0.9f) {
            // Crowded: stand the label on end
            rotate(-90f, anchor) {
                drawText(layout, topLeft = Offset(anchor.x, anchor.y - layout.size.height / 2f))
            }
        } else {
            drawText(layout, topLeft = Offset(anchor.x - layout.size.width / 2f, anchor.y - layout.size.height))
        }
    }

    // Spring at mid-chord
    val midFoot = pt(c / 2.0, 0.0)
    val midTop = pt(c / 2.0, s)
    drawLine(SpringColor, midFoot, midTop, 3f)
    val springStyle = TextStyle(fontSize = 12.sp, color = SpringColor, fontWeight = FontWeight.Bold)
    arcLabelLeft(measurer, "S ${FractionUtils.formatInches(s, 8)}", Offset(midFoot.x + 8f, (midFoot.y + midTop.y) / 2f), springStyle)

    // Chord and radius labels
    val dark = TextStyle(fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
    arcLabelCentered(measurer, "Chord ${FractionUtils.formatInches(c)}", Offset(midFoot.x, yBase + 18f), dark)
    val note = if (exaggeration > 1.05f) "   (vertical ×${exaggeration.toDouble().roundToOneDecimal()})" else ""
    arcLabelCentered(
        measurer,
        "R ${FractionUtils.formatInches(sol.radius)}$note",
        Offset(midFoot.x, yBase + 36f),
        TextStyle(fontSize = 12.sp, color = ArcColor, fontWeight = FontWeight.Bold)
    )
}

private fun DrawScope.arcLabelCentered(m: TextMeasurer, text: String, center: Offset, style: TextStyle) {
    val layout = m.measure(text, style, density = this)
    drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
}

private fun DrawScope.arcLabelLeft(m: TextMeasurer, text: String, anchor: Offset, style: TextStyle) {
    val layout = m.measure(text, style, density = this)
    drawText(layout, topLeft = Offset(anchor.x, anchor.y - layout.size.height / 2f))
}
