package com.djran.constructioncalculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

/*
 * Circular Stairs, matching the Android app: code check (Michigan residential code), best tread
 * count, walk-line tread, fit check, plan + unrolled elevation drawing and a material list.
 * The math is CircStairMath (same file as Android).
 */

private val CS_RED = Color(0xFFD32F2F)
private val CS_BLUE = Color(0xFF1D4ED8)
private val CS_NAVY = Color(0xFF0A2A66)
private val CS_GREEN = Color(0xFF2E7D32)
private val CS_AMBER = Color(0xFFB26A00)

/** Plan view (top) and unrolled elevation on the walk line (bottom). */
fun DrawScope.drawCircStair(r: CircStairMath.Result, measurer: TextMeasurer) {
    val w = size.width; val h = size.height
    val u = w / 400f
    fun txt(t: String, x: Float, y: Float, sz: Float, color: Color, bold: Boolean = true, align: Int = 2) {
        val m = measurer.measure(t, TextStyle(fontSize = (sz * u / density).sp, color = color, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal))
        val x0 = when (align) { 1 -> x - m.size.width; 2 -> x - m.size.width / 2f; else -> x }
        drawText(m, topLeft = Offset(x0, y - m.size.height / 2f))
    }
    fun arrow(x1: Float, y1: Float, x2: Float, y2: Float) {
        drawLine(CS_BLUE, Offset(x1, y1), Offset(x2, y2), 1.2f * u)
        val head = 5 * u
        val a = atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())
        for ((px, py, dir) in listOf(Triple(x2, y2, -1.0), Triple(x1, y1, 1.0))) {
            drawLine(CS_BLUE, Offset(px, py), Offset((px + dir * head * cos(a - PI / 6)).toFloat(), (py + dir * head * sin(a - PI / 6)).toFloat()), 1.2f * u)
            drawLine(CS_BLUE, Offset(px, py), Offset((px + dir * head * cos(a + PI / 6)).toFloat(), (py + dir * head * sin(a + PI / 6)).toFloat()), 1.2f * u)
        }
    }
    drawRect(Color.White)
    val f = { x: Double -> FractionUtils.formatInches(x) }
    val member = 2f * u
    val splitY = h * 0.52f

    // ---- plan ----
    txt("PLAN VIEW (TOP DOWN)", w / 2, 12 * u, 11f, CS_NAVY)
    val outerR = r.outerRadius
    val straightLen = r.straightTreads * r.straightRun
    val scale = min((w - 50 * u) / (straightLen + outerR * 2 + 40).toFloat(), (splitY - 60 * u) / (outerR * 2 + 40).toFloat())
    val sIn = (r.innerRadius * scale).toFloat(); val sOut = (outerR * scale).toFloat()
    val sRun = (r.straightRun * scale).toFloat(); val sNos = (r.nosing * scale).toFloat()
    val cx = w / 2f + (straightLen * scale).toFloat() / 2f
    val cy = splitY / 2f + 10 * u
    if (r.straightTreads > 0) {
        for (i in 0 until r.straightTreads) {
            val x1 = cx - (i + 1) * sRun; val x2 = cx - i * sRun
            drawLine(CS_RED, Offset(x1, cy + sOut), Offset(x2, cy + sOut), member)
            drawLine(CS_RED, Offset(x1, cy + sIn), Offset(x2, cy + sIn), member)
            drawLine(CS_RED, Offset(x1, cy + sIn), Offset(x1, cy + sOut), member)
            if (r.nosing > 0) drawLine(CS_BLUE, Offset(x1 + sNos, cy + sIn), Offset(x1 + sNos, cy + sOut), 0.8f * u,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3 * u, 3 * u)))
        }
    }
    val sweep = -r.sweepDeg.toFloat()
    drawArc(CS_RED, 90f, sweep, false, Offset(cx - sIn, cy - sIn), Size(2 * sIn, 2 * sIn), style = Stroke(member))
    drawArc(CS_RED, 90f, sweep, false, Offset(cx - sOut, cy - sOut), Size(2 * sOut, 2 * sOut), style = Stroke(member))
    val per = abs(sweep) / max(1, r.wedgeTreads)
    for (i in 0..r.wedgeTreads) {
        val a = (90.0 - i * per) * PI / 180.0
        drawLine(CS_RED, Offset(cx + sIn * cos(a).toFloat(), cy + sIn * sin(a).toFloat()), Offset(cx + sOut * cos(a).toFloat(), cy + sOut * sin(a).toFloat()), member)
    }
    // walk line
    val sWalk = (r.walkRadius * scale).toFloat()
    drawArc(CS_GREEN, 90f, sweep, false, Offset(cx - sWalk, cy - sWalk), Size(2 * sWalk, 2 * sWalk),
        style = Stroke(0.8f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4 * u, 3 * u))))
    arrow(cx - sIn - 12 * u, cy + sIn, cx - sIn - 12 * u, cy + sOut)
    txt(f(r.treadWidth), cx - sIn - 14 * u, cy + (sIn + sOut) / 2, 8f, CS_BLUE, align = 1)
    arrow(cx, cy, cx + sIn, cy)
    txt("R: ${f(r.innerRadius)}", cx + sIn / 2, cy - 6 * u, 8f, Color.Black, false)
    txt("I-Arc: ${f(r.innerRun)}", cx + sIn + 4 * u, cy + 10 * u, 8f, Color.Black, false, 0)
    txt("O-Arc: ${f(r.outerRun)}", cx + sOut + 4 * u, cy + 20 * u, 8f, Color.Black, false, 0)

    // ---- elevation (unrolled walk line) ----
    txt("STAIR ELEVATION (UNROLLED WALK-PATH)", w / 2, splitY + 12 * u, 11f, CS_NAVY)
    if (r.totalRise <= 0) return
    val ground = h - 20 * u; val top = splitY + 34 * u
    val availH = ground - top
    val arcPath = r.walkRadius * r.sweepDeg * PI / 180.0
    val path = straightLen + arcPath
    val sx = ((w - 80 * u) / path).toFloat(); val sy = (availH / r.totalRise).toFloat()
    val sStep = (r.stepRise * sy).toFloat()
    val sStr = (r.straightRun * sx).toFloat(); val sArc = (arcPath / max(1, r.wedgeTreads) * sx).toFloat()
    var x = max(40 * u, (w - (path * sx).toFloat()) / 2f); var y = ground
    val p = Path().apply { moveTo(x, y) }
    for (i in 0 until r.totalTreads) {
        p.lineTo(x, y - sStep); y -= sStep
        val run = if (i < r.straightTreads) sStr else sArc
        p.lineTo(x + run, y)
        if (r.nosing > 0) drawLine(CS_RED, Offset(x + run, y), Offset(x + run + (r.nosing * sx).toFloat(), y), member)
        x += run
    }
    p.lineTo(x, y - sStep); y -= sStep
    drawLine(CS_RED, Offset(x, y), Offset(x + 18 * u, y), member)
    drawPath(p, CS_RED, style = Stroke(member))
    val sTotal = (r.totalRise * sy).toFloat()
    arrow(20 * u, ground, 20 * u, ground - sTotal)
    rotate(-90f, Offset(12 * u, ground - sTotal / 2)) { txt("Total Rise: ${f(r.totalRise)}", 12 * u, ground - sTotal / 2, 8f, CS_BLUE) }
    txt("${f(r.stepRise)} rise", max(40 * u, (w - (path * sx).toFloat()) / 2f) + 6 * u, ground - sStep / 2, 8f, CS_RED, align = 0)
}

@Composable
fun CircStairDrawing(r: CircStairMath.Result, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(maxWidth * 1.15f)) { drawCircStair(r, measurer) }
    }
}

@Composable
private fun CsSection(t: String) = Text(t, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, modifier = Modifier.padding(top = 18.dp, bottom = 4.dp))

@Composable
private fun CsRow(label: String, value: String, big: Boolean = false) {
    Row(Modifier.fillMaxWidth().background(if (big) AppTheme.colors.line.copy(alpha = 0.35f) else Color.Transparent).padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 15.sp, fontWeight = if (big) FontWeight.Bold else FontWeight.Normal, color = AppTheme.colors.ink)
        Text(value, fontSize = if (big) 19.sp else 16.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, textAlign = TextAlign.End)
    }
}

/** Results block (verdict, steps, layout, code check, material list), used on the screen. */
@Composable
fun CircStairResults(r: CircStairMath.Result) {
    val f = { x: Double -> FractionUtils.formatInches(x) }
    val fails = r.checks.count { it.level == CircStairMath.Level.FAIL }
    Column(Modifier.fillMaxWidth()) {
        Text(if (fails == 0) "PASSES CODE CHECK" + (if (r.fits == true) " AND FITS" else "") else "$fails PROBLEM${if (fails > 1) "S" else ""} - SEE CODE CHECK",
            color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().background(if (fails == 0) CS_GREEN else CS_RED, RoundedCornerShape(10.dp)).padding(12.dp))
        CsSection("STEPS")
        CsRow("Risers", "${r.risers}")
        CsRow("Step Rise", f(r.stepRise), big = true)
        if (r.bestTreads > 0) {
            CsRow("Best Treads (closest to 7 1/2\")", "${r.bestTreads} @ ${f(r.bestRise)}", big = r.bestTreads != r.totalTreads)
            CsRow("Tread Range (6 1/4\" - 8 1/4\" rise)", "${r.minTreadsForRise} to ${r.maxTreadsForRise}")
        }
        CsRow("Treads", "${r.totalTreads} (${r.wedgeTreads} curved, ${r.straightTreads} straight)")
        CsRow("Tread Run on Walk Line", f(r.walkRun), big = true)
        CsRow("Walk Line Radius", f(r.walkRadius))
        CsRow("Total Walk Run", f(r.totalWalkRun))
        CsSection("LAYOUT")
        CsRow("Outside Radius", f(r.outerRadius))
        CsRow("Angle per Tread", "${CircStairMath.fx(r.anglePerTreadDeg, 2)}°", big = true)
        CsRow("Sweep of Circle", "${CircStairMath.fx(r.sweepDeg, 1)}°", big = true)
        CsRow("Run per Tread - Inner Arc", f(r.innerRun))
        CsRow("Run per Tread - Outer Arc", f(r.outerRun))
        CsRow("Riser Spacing on Chord - Inner", f(r.innerChord))
        CsRow("Riser Spacing on Chord - Outer", f(r.outerChord))
        CsRow("Total Inner Arc", f(r.innerArcTotal))
        CsRow("Total Outer Arc", f(r.outerArcTotal))
        CsRow("Stringer Pitch - Inner / Outer", "${CircStairMath.fx(r.innerPitchDeg, 1)}° / ${CircStairMath.fx(r.outerPitchDeg, 1)}°")
        CsRow("Plan Footprint", "${f(r.footprintX)} x ${f(r.footprintY)}", big = true)
        CsSection("CODE CHECK")
        r.checks.forEach { c ->
            Text("${CircStairMath.tag(c.level)} ${c.label}: ${c.detail}", fontSize = 15.sp, modifier = Modifier.padding(4.dp),
                color = when (c.level) { CircStairMath.Level.OK -> CS_GREEN; CircStairMath.Level.WARN -> CS_AMBER; CircStairMath.Level.FAIL -> CS_RED })
        }
        Text("2015 Michigan Residential Code: 8 1/4\" max rise, curved treads per the winder rule (10\" at the walk line, 6\" min). 6 1/4\" min / 7 1/2\" best rise is the shop rule. Check with your inspector.",
            fontSize = 12.sp, color = AppTheme.colors.muted, modifier = Modifier.padding(4.dp))
        CsSection("MATERIAL LIST")
        r.materials.forEachIndexed { i, m ->
            Column(Modifier.fillMaxWidth().background(if (i % 2 == 0) Color.Transparent else AppTheme.colors.line.copy(alpha = 0.35f)).padding(6.dp)) {
                Text("${m.qty}  ${m.item}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.ink)
                Text(m.size, fontSize = 13.sp, color = AppTheme.colors.muted)
            }
        }
        Spacer(Modifier.height(12.dp))
        CircStairDrawing(r)
    }
}

@Composable
fun CircularStairCalculator(
    entries: List<CircularStairEntry>,
    onUpdateEntries: (List<CircularStairEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var totalRise by remember { mutableStateOf("") }
    var innerRadius by remember { mutableStateOf("18") }
    var treadWidth by remember { mutableStateOf("45") }
    var numTreads by remember { mutableStateOf("8") }
    var straightTreads by remember { mutableStateOf("0") }
    var straightRun by remember { mutableStateOf("10") }
    var nosing by remember { mutableStateOf("1") }
    var walkTread by remember { mutableStateOf("11") }
    var spaceLength by remember { mutableStateOf("") }
    var spaceWidth by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<CircStairMath.Result?>(null) }
    var message by remember { mutableStateOf("") }
    var editingIndex by remember { mutableStateOf(-1) }

    fun calc(): CircStairMath.Result? = CircStairMath.compute(
        totalRise = FractionUtils.parse(totalRise), innerRadius = FractionUtils.parse(innerRadius),
        treadWidth = FractionUtils.parse(treadWidth), totalTreads = numTreads.trim().toDoubleOrNull()?.toInt() ?: 0,
        straightTreads = straightTreads.trim().toDoubleOrNull()?.toInt() ?: 0, straightRun = FractionUtils.parse(straightRun),
        nosing = FractionUtils.parse(nosing), walkTread = FractionUtils.parse(walkTread),
        spaceLength = FractionUtils.parse(spaceLength), spaceWidth = FractionUtils.parse(spaceWidth)
    )

    @Composable
    fun field(label: String, value: String, modifier: Modifier, set: (String) -> Unit) =
        ConstructionTextField(value = value, onValueChange = set, label = label, modifier = modifier, onFocus = onFocus)

    ToolScreen(Screen.CircularStairCalculator, onBack) {
        Column(Modifier.padding(16.dp)) {
            Text("INPUTS", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Stair Name (e.g. Master Suite)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row { field("Total Rise (in)", totalRise, Modifier.weight(1f)) { totalRise = it }; Spacer(Modifier.width(8.dp)); field("Inner Radius (in)", innerRadius, Modifier.weight(1f)) { innerRadius = it } }
            Spacer(Modifier.height(8.dp))
            Row { field("Width of Tread (in)", treadWidth, Modifier.weight(1f)) { treadWidth = it }; Spacer(Modifier.width(8.dp)); field("Total No. of Treads (incl. straight)", numTreads, Modifier.weight(1f)) { numTreads = it } }
            Spacer(Modifier.height(8.dp))
            Row { field("Straight Treads at Start", straightTreads, Modifier.weight(1f)) { straightTreads = it }; Spacer(Modifier.width(8.dp)); field("Straight Tread Run (in)", straightRun, Modifier.weight(1f)) { straightRun = it } }
            Spacer(Modifier.height(8.dp))
            Row { field("Nosing (in)", nosing, Modifier.weight(1f)) { nosing = it }; Spacer(Modifier.width(8.dp)); field("Walk-line Tread Depth (in, 10 min, 11 best)", walkTread, Modifier.weight(1f)) { walkTread = it } }
            Text("Space available (optional) - checks that the stair fits", fontSize = 12.sp, color = AppTheme.colors.muted, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
            Row { field("Space Length (in or ft-in)", spaceLength, Modifier.weight(1f)) { spaceLength = it }; Spacer(Modifier.width(8.dp)); field("Space Width (in or ft-in)", spaceWidth, Modifier.weight(1f)) { spaceWidth = it } }
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                result = calc(); message = if (result == null) "Enter inner radius, tread width and number of treads" else ""
            }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)) { Text("CALCULATE CIRC STAIRS") }
            Spacer(Modifier.height(8.dp))
            Row {
                Button(onClick = {
                    val r = calc()
                    if (r == null) { message = "Enter inner radius, tread width and number of treads"; return@Button }
                    result = r; message = ""
                    val f = { x: Double -> FractionUtils.formatInches(x) }
                    val entry = CircularStairEntry(
                        id = if (editingIndex != -1) entries[editingIndex].id else (entries.size + 1).toString(),
                        name = name.ifBlank { "Stair ${entries.size + 1}" }, totalRise = totalRise, radius = innerRadius,
                        walkTread = treadWidth, numTreads = numTreads, straightTreads = straightTreads.ifBlank { "0" },
                        straightRun = straightRun, nosing = nosing, circleSize = "${CircStairMath.fx(r.sweepDeg, 1)}°",
                        outerArcTread = f(r.outerRun), innerArcTread = f(r.innerRun), outsideRadius = f(r.outerRadius),
                        anglePerTread = "${CircStairMath.fx(r.anglePerTreadDeg, 2)}°", walkLineTread = walkTread,
                        spaceLength = spaceLength, spaceWidth = spaceWidth, stepRise = f(r.stepRise)
                    )
                    if (editingIndex != -1) { onUpdateEntries(entries.toMutableList().also { it[editingIndex] = entry }); editingIndex = -1 }
                    else onUpdateEntries(entries + entry)
                    message = "Saved \"${entry.name}\" - shows on the Data Sheet"
                }, modifier = Modifier.weight(2f).height(52.dp), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)) {
                    Text(if (editingIndex != -1) "UPDATE ITEM" else "SAVE STAIR")
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { onUpdateEntries(emptyList()); result = null }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppTheme.colors.danger), colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.danger)) { Text("CLEAR LIST") }
            }
            if (message.isNotEmpty()) Text(message, fontSize = 13.sp, color = AppTheme.colors.muted, modifier = Modifier.padding(top = 6.dp))

            Text("RESULTS", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
            result?.let { CircStairResults(it) } ?: Text("Press CALCULATE to see results", color = AppTheme.colors.muted)

            if (entries.isNotEmpty()) {
                Text("SAVED STAIRS", color = AppTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 24.dp, bottom = 4.dp))
                entries.reversed().forEachIndexed { index, e ->
                    val idx = entries.size - 1 - index
                    Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(e.name, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = AppTheme.colors.ink)
                                IconButton(onClick = {
                                    editingIndex = idx; name = e.name; totalRise = e.totalRise; innerRadius = e.radius; treadWidth = e.walkTread
                                    numTreads = e.numTreads; straightTreads = e.straightTreads; straightRun = e.straightRun; nosing = e.nosing
                                    spaceLength = e.spaceLength; spaceWidth = e.spaceWidth
                                    walkTread = e.walkLineTread.ifBlank { CircStairMath.fromEntry(e)?.let { r -> FractionUtils.formatInches(r.walkRun).replace("\"", "") } ?: "11" }
                                    result = CircStairMath.fromEntry(e)
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTheme.colors.muted) }
                                IconButton(onClick = { onUpdateEntries(entries.filterIndexed { k, _ -> k != idx }) }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppTheme.colors.muted) }
                            }
                            CircStairMath.fromEntry(e)?.let { r ->
                                Text(CircStairMath.reportLines(r).joinToString("\n"), fontSize = 13.sp, color = AppTheme.colors.muted)
                                Spacer(Modifier.height(8.dp))
                                CircStairDrawing(r)
                            }
                        }
                    }
                }
            }
        }
    }
}
