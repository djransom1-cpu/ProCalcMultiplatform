package com.djran.constructioncalculator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import kotlin.math.*

/*
 * Site Coordinates map tools, matching the Android app: point numbers on the map, close the
 * loop, zoom, custom lines with arc heights, measure angle, distance between 2 points or 2
 * lines, divide a line, plot by D.M.S. and distance, takeoff per path with MARK HOLE, and
 * send the takeoff to Concrete, Floor or Roof. Coordinates are kept in inches.
 */

/** A custom line between two point numbers (as shown on the map), with an optional arc height (inches). */
@Serializable
data class CoordLine(val from: Int, val to: Int, val arc: Double = 0.0)

/** Values handed from the map takeoff to another tool (feet / square feet). */
object ToolImport {
    /** Length (ft) and name waiting for the next tool screen that opens. */
    var length: Double = 0.0
    var label: String = ""
    fun take(): Pair<String, String>? {
        if (length <= 0) return null
        val t = StoryPoleMath.fixed(length, 2) to label; length = 0.0; label = ""; return t
    }
}

object CoordMath {
    class NumPt(val num: Int, val path: Int, val idx: Int, val p: CoordinatePoint)

    fun numbered(paths: List<PlottedPath>): List<NumPt> {
        var n = 0
        return paths.flatMapIndexed { pi, path -> path.points.mapIndexed { i, p -> NumPt(++n, pi, i, p) } }
    }

    /** Arc length from chord and arc height (inches). */
    fun arcLength(chord: Double, h: Double): Double {
        val hh = abs(h)
        if (hh == 0.0 || chord == 0.0) return chord
        val r = hh / 2.0 + chord * chord / (8.0 * hh)
        return r * 2.0 * asin((chord / (2.0 * r)).coerceAtMost(1.0))
    }

    fun dist(a: CoordinatePoint, b: CoordinatePoint) = sqrt((a.n - b.n).pow(2) + (a.e - b.e).pow(2))

    /** Angle at the vertex between two neighbors, degrees (0-180). */
    fun angle(a: CoordinatePoint, v: CoordinatePoint, b: CoordinatePoint): Double {
        val a1 = atan2(a.n - v.n, a.e - v.e); val a2 = atan2(b.n - v.n, b.e - v.e)
        var d = abs(a1 - a2) * 180.0 / PI
        if (d > 180) d = 360 - d
        return d
    }

    private fun ptSeg(p: CoordinatePoint, a: CoordinatePoint, b: CoordinatePoint): Double {
        val dx = b.e - a.e; val dy = b.n - a.n
        val l2 = dx * dx + dy * dy
        if (l2 == 0.0) return dist(p, a)
        val t = (((p.e - a.e) * dx + (p.n - a.n) * dy) / l2).coerceIn(0.0, 1.0)
        return dist(p, CoordinatePoint(a.n + t * dy, a.e + t * dx))
    }

    private fun intersects(a: CoordinatePoint, b: CoordinatePoint, c: CoordinatePoint, d: CoordinatePoint): Boolean {
        fun o(p: CoordinatePoint, q: CoordinatePoint, r: CoordinatePoint) = (q.e - p.e) * (r.n - p.n) - (q.n - p.n) * (r.e - p.e)
        return o(a, b, c) * o(a, b, d) < 0 && o(c, d, a) * o(c, d, b) < 0
    }

    /** Clear distance between two segments (0 if they cross). */
    fun segDist(a: CoordinatePoint, b: CoordinatePoint, c: CoordinatePoint, d: CoordinatePoint): Double =
        if (intersects(a, b, c, d)) 0.0 else minOf(ptSeg(a, c, d), ptSeg(b, c, d), ptSeg(c, a, b), ptSeg(d, a, b))

    /** New point from a start, by azimuth (0 = North, 90 = East) and distance (inches). */
    fun plot(start: CoordinatePoint, azDeg: Double, distIn: Double): CoordinatePoint {
        val r = azDeg * PI / 180.0
        return CoordinatePoint(start.n + distIn * cos(r), start.e + distIn * sin(r), start.elev, "DMS Pt")
    }

    fun inPolygon(p: CoordinatePoint, poly: List<CoordinatePoint>): Boolean {
        var inside = false; var j = poly.size - 1
        for (i in poly.indices) {
            if ((poly[i].n > p.n) != (poly[j].n > p.n) && p.e < (poly[j].e - poly[i].e) * (p.n - poly[i].n) / (poly[j].n - poly[i].n) + poly[i].e) inside = !inside
            j = i
        }
        return inside
    }

    fun area(path: PlottedPath) = CoordinateEngine.calculateNetArea(path.points.map { CoordinateEngine.Point(it.n, it.e) }, path.arcHeights)

    /** Segment lengths (inches) of a path, with the closure when [closed]; curved sides use arc length. */
    fun sides(path: PlottedPath, closed: Boolean): List<Pair<String, Double>> {
        val out = mutableListOf<Pair<String, Double>>()
        fun seg(a: CoordinatePoint, b: CoordinatePoint, h: Double, name: String) {
            val c = dist(a, b)
            out += (name + if (h != 0.0) " (Curved)" else "") to if (h != 0.0) arcLength(c, h) else c
        }
        for (i in 0 until path.points.size - 1) seg(path.points[i], path.points[i + 1], path.arcHeights.getOrElse(i + 1) { 0.0 }, "Side ${i + 1}")
        if (closed && path.points.size > 2) seg(path.points.last(), path.points.first(), path.arcHeights.getOrElse(0) { 0.0 }, "Closure")
        return out
    }

    /** Net area of a path less any hole paths that start inside it (square inches). */
    fun netArea(paths: List<PlottedPath>, pi: Int): Double {
        val path = paths[pi]
        var a = area(path)
        paths.forEachIndexed { k, p ->
            if (k != pi && p.isHole && p.points.isNotEmpty() && inPolygon(p.points.first(), path.points)) a -= area(p)
        }
        return a
    }
}

/** The map: paths, closure, custom lines, point numbers; [zoom] scales about the center. */
@Composable
fun CoordinateMapView(paths: List<PlottedPath>, lines: List<CoordLine> = emptyList(), closed: Boolean = false, zoom: Float = 1f, highlight: Set<Int> = emptySet()) {
    val measurer = rememberTextMeasurer()
    Canvas(Modifier.fillMaxSize().background(Color.White)) {
        val nums = CoordMath.numbered(paths)
        if (nums.isEmpty()) return@Canvas
        val allPts = nums.map { it.p }
        val minN = allPts.minOf { it.n }; val maxN = allPts.maxOf { it.n }
        val minE = allPts.minOf { it.e }; val maxE = allPts.maxOf { it.e }
        val rangeN = (maxN - minN).coerceAtLeast(120.0); val rangeE = (maxE - minE).coerceAtLeast(120.0)
        val scale = min(size.height / rangeN, size.width / rangeE) * 0.8 * zoom
        val cN = (minN + maxN) / 2.0; val cE = (minE + maxE) / 2.0
        fun X(e: Double) = (size.width / 2f + (e - cE) * scale).toFloat()
        fun Y(n: Double) = (size.height / 2f - (n - cN) * scale).toFloat()
        fun seg(path: Path, a: CoordinatePoint, b: CoordinatePoint, h: Double) {
            if (h == 0.0) { path.lineTo(X(b.e), Y(b.n)); return }
            val mx = (X(a.e) + X(b.e)) / 2f; val my = (Y(a.n) + Y(b.n)) / 2f
            val dx = X(b.e) - X(a.e); val dy = Y(b.n) - Y(a.n); val d = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
            val hp = (h * scale).toFloat()
            path.quadraticTo(mx - dy / d * hp * 2f, my + dx / d * hp * 2f, X(b.e), Y(b.n))
        }
        paths.forEach { path ->
            if (path.points.isEmpty()) return@forEach
            val color = when { path.isHole -> Color(0xFFB3261E); path.isTakeoff -> BlueTool; else -> Color.Gray }
            val p = Path().apply { moveTo(X(path.points[0].e), Y(path.points[0].n)) }
            for (i in 0 until path.points.size - 1) seg(p, path.points[i], path.points[i + 1], path.arcHeights.getOrElse(i + 1) { 0.0 })
            if (closed && path.points.size > 2) seg(p, path.points.last(), path.points.first(), path.arcHeights.getOrElse(0) { 0.0 })
            drawPath(p, color, style = Stroke(if (path.isTakeoff || path.isHole) 4f else 2f))
        }
        lines.forEach { l ->
            val a = nums.firstOrNull { it.num == l.from }?.p ?: return@forEach
            val b = nums.firstOrNull { it.num == l.to }?.p ?: return@forEach
            val p = Path().apply { moveTo(X(a.e), Y(a.n)) }
            seg(p, a, b, l.arc)
            drawPath(p, Color(0xFF2E7D32), style = Stroke(3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
        }
        nums.forEach { np ->
            val c = Offset(X(np.p.e), Y(np.p.n))
            drawCircle(if (np.num in highlight) Color(0xFFFF9800) else Color.Red, if (np.num in highlight) 9f else 6f, c)
            val m = measurer.measure(np.num.toString(), TextStyle(fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold))
            drawText(m, topLeft = Offset(c.x + 7f, c.y - m.size.height - 2f))
        }
    }
}

@Composable
private fun NumField(label: String, v: String, set: (String) -> Unit) =
    OutlinedTextField(v, set, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())

/** MAP TOOLS dialogs. [tool] is the one picked from the menu. Results go to [onResult]; new points to [onPaths]. */
@Composable
fun CoordToolDialog(
    tool: String,
    paths: List<PlottedPath>,
    lines: List<CoordLine>,
    closed: Boolean,
    onPaths: (List<PlottedPath>) -> Unit,
    onDismiss: () -> Unit,
) {
    val nums = CoordMath.numbered(paths)
    fun pt(s: String) = s.trim().toIntOrNull()?.let { n -> nums.firstOrNull { it.num == n } }
    var a by remember { mutableStateOf("") }; var b by remember { mutableStateOf("") }
    var c by remember { mutableStateOf("") }; var d by remember { mutableStateOf("") }
    var deg by remember { mutableStateOf("") }; var min by remember { mutableStateOf("") }; var sec by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }

    fun neighbors(v: CoordMath.NumPt): List<CoordinatePoint> {
        val out = mutableListOf<CoordinatePoint>()
        val path = paths[v.path]
        if (v.idx > 0) out += path.points[v.idx - 1]
        if (v.idx < path.points.size - 1) out += path.points[v.idx + 1]
        if (closed && path.points.size > 2) {
            if (v.idx == 0) out += path.points.last()
            if (v.idx == path.points.size - 1) out += path.points.first()
        }
        lines.forEach { l ->
            if (l.from == v.num) pt(l.to.toString())?.let { out += it.p }
            if (l.to == v.num) pt(l.from.toString())?.let { out += it.p }
        }
        return out
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tool) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                when (tool) {
                    "MEASURE ANGLE (CORNER)" -> NumField("Corner point #", a) { a = it }
                    "DIST BETWEEN 2 POINTS" -> { NumField("Point #", a) { a = it }; NumField("Point #", b) { b = it } }
                    "DIST BETWEEN 2 LINES" -> {
                        Text("Line 1", fontSize = 12.sp); NumField("From #", a) { a = it }; NumField("To #", b) { b = it }
                        Text("Line 2", fontSize = 12.sp); NumField("From #", c) { c = it }; NumField("To #", d) { d = it }
                    }
                    "PLOT POINTS ON LINE" -> { Text("Divide line into equal segments", fontSize = 12.sp); NumField("From #", a) { a = it }; NumField("To #", b) { b = it }; NumField("Point Count", c) { c = it } }
                    "PLOT BY D.M.S. & DIST" -> {
                        Text("0 = North, 90 = East", fontSize = 12.sp)
                        NumField("Start Point #", a) { a = it }
                        Row { Box(Modifier.weight(1f)) { NumField("Degrees", deg) { deg = it } }; Box(Modifier.weight(1f)) { NumField("Minutes", min) { min = it } }; Box(Modifier.weight(1f)) { NumField("Seconds", sec) { sec = it } } }
                        NumField("Distance (ft/in)", b) { b = it }
                    }
                }
                if (result.isNotEmpty()) Text(result, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BlueTool, modifier = Modifier.padding(top = 10.dp))
            }
        },
        confirmButton = {
            Button(onClick = {
                when (tool) {
                    "MEASURE ANGLE (CORNER)" -> {
                        val v = pt(a); val nb = v?.let { neighbors(it) } ?: emptyList()
                        result = if (v == null || nb.size < 2) "Need 2 lines meeting here" else "Angle: ${StoryPoleMath.fixed(CoordMath.angle(nb[0], v.p, nb[1]), 2)}°"
                    }
                    "DIST BETWEEN 2 POINTS" -> {
                        val p1 = pt(a); val p2 = pt(b)
                        result = if (p1 == null || p2 == null) "Enter two point numbers" else "Direct Distance: ${FractionUtils.formatInches(CoordMath.dist(p1.p, p2.p))}"
                    }
                    "DIST BETWEEN 2 LINES" -> {
                        val p = listOf(a, b, c, d).map { pt(it) }
                        result = if (p.any { it == null }) "Enter four point numbers" else "Clear Distance: ${FractionUtils.formatInches(CoordMath.segDist(p[0]!!.p, p[1]!!.p, p[2]!!.p, p[3]!!.p))}"
                    }
                    "PLOT POINTS ON LINE" -> {
                        val p1 = pt(a); val p2 = pt(b); val n = c.trim().toIntOrNull() ?: 0
                        if (p1 == null || p2 == null || n <= 0) result = "Enter two points and a count" else {
                            val new = (1..n).map { i ->
                                CoordinatePoint(p1.p.n + (p2.p.n - p1.p.n) * i / (n + 1), p1.p.e + (p2.p.e - p1.p.e) * i / (n + 1), p1.p.elev, "Plotted Pt")
                            }
                            val list = paths.toMutableList()
                            if (p1.path == p2.path && abs(p1.idx - p2.idx) == 1) {
                                // Neighbors on one path: the new points go between them
                                val path = list[p1.path]
                                val first = minOf(p1.idx, p2.idx)
                                val ordered = if (p1.idx < p2.idx) new else new.reversed()
                                list[p1.path] = path.copy(points = path.points.take(first + 1) + ordered + path.points.drop(first + 1),
                                    arcHeights = if (path.arcHeights.isEmpty()) emptyList() else path.arcHeights.take(first + 1) + List(n) { 0.0 } + path.arcHeights.drop(first + 1))
                            } else list += PlottedPath(id = (list.size + 1).toString(), points = new, isTakeoff = false)
                            onPaths(list); onDismiss()
                        }
                    }
                    "PLOT BY D.M.S. & DIST" -> {
                        val s = pt(a); val dist = FractionUtils.parseFeet(b)
                        if (s == null || dist <= 0) result = "Enter a start point and distance" else {
                            val az = (deg.toDoubleOrNull() ?: 0.0) + (min.toDoubleOrNull() ?: 0.0) / 60.0 + (sec.toDoubleOrNull() ?: 0.0) / 3600.0
                            onPaths(paths + PlottedPath(id = (paths.size + 1).toString(), points = listOf(s.p, CoordMath.plot(s.p, az, dist)), isTakeoff = false))
                            onDismiss()
                        }
                    }
                }
            }) { Text(if (tool.startsWith("PLOT")) "PLOT" else "CALCULATE") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } }
    )
}

/** Lines dialog: close the loop, custom lines, and arc heights on each side. */
@Composable
fun CoordLinesDialog(
    paths: List<PlottedPath>, lines: List<CoordLine>, closed: Boolean,
    onPaths: (List<PlottedPath>) -> Unit, onLines: (List<CoordLine>) -> Unit, onClosed: (Boolean) -> Unit, onDismiss: () -> Unit
) {
    var from by remember { mutableStateOf("") }; var to by remember { mutableStateOf("") }; var arc by remember { mutableStateOf("") }
    val nums = CoordMath.numbered(paths)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lines") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = closed, onCheckedChange = onClosed); Text("Close the loop (last point back to first)")
                }
                Text("CUSTOM", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                lines.forEachIndexed { i, l ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${l.from} -> ${l.to}" + if (l.arc != 0.0) "  (arc ${FractionUtils.formatInches(l.arc)})" else "", Modifier.weight(1f))
                        IconButton(onClick = { onLines(lines.filterIndexed { k, _ -> k != i }) }) { Icon(Icons.Default.Delete, contentDescription = "Remove") }
                    }
                }
                Row {
                    Box(Modifier.weight(1f)) { NumField("From #", from) { from = it } }
                    Box(Modifier.weight(1f)) { NumField("To #", to) { to = it } }
                    Box(Modifier.weight(1f)) { NumField("Arc H (in)", arc) { arc = it } }
                }
                TextButton(onClick = {
                    val f = from.trim().toIntOrNull(); val t = to.trim().toIntOrNull()
                    if (f != null && t != null) { onLines(lines + CoordLine(f, t, FractionUtils.parse(arc))); from = ""; to = ""; arc = "" }
                }) { Text("+ ADD") }
                Text("TRAVERSE (arc height per side, inches)", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                nums.filter { it.idx > 0 }.forEach { np ->
                    val path = paths[np.path]
                    var h by remember(np.path, np.idx) { mutableStateOf(path.arcHeights.getOrElse(np.idx) { 0.0 }.let { if (it == 0.0) "" else FractionUtils.formatInches(it).replace("\"", "") }) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Line ${np.num - 1}->${np.num}", Modifier.weight(1f))
                        OutlinedTextField(h, { v ->
                            h = v
                            val arcs = MutableList(path.points.size) { k -> path.arcHeights.getOrElse(k) { 0.0 } }
                            arcs[np.idx] = FractionUtils.parse(v)
                            onPaths(paths.toMutableList().also { it[np.path] = path.copy(arcHeights = arcs) })
                        }, singleLine = true, modifier = Modifier.width(110.dp), placeholder = { Text("Arc H") })
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("DONE") } }
    )
}

/** Takeoff for one path: perimeter (with closure), net area less holes, MARK HOLE, and send to another tool. */
@Composable
fun CoordTakeoffDialog(
    paths: List<PlottedPath>, closed: Boolean,
    onPaths: (List<PlottedPath>) -> Unit, onSend: (Screen) -> Unit, onDismiss: () -> Unit
) {
    val choices = paths.indices.filter { paths[it].points.size >= 2 }
    var pick by remember { mutableStateOf(choices.lastOrNull { paths[it].isTakeoff } ?: choices.lastOrNull() ?: 0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (paths.getOrNull(pick)?.isHole == true) "Exclusion" else "Takeoff", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (choices.isEmpty()) { Text("Add at least two points to a path."); return@Column }
                Text("Path:", fontSize = 12.sp)
                Row(Modifier.fillMaxWidth()) {
                    choices.forEach { i ->
                        val on = i == pick
                        Text("${i + 1}", Modifier.padding(4.dp).background(if (on) BlueTool else Color(0xFFE3E8F0)).clickable { pick = i }.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = if (on) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                val path = paths[pick]
                val sides = CoordMath.sides(path, closed)
                val perim = sides.sumOf { it.second }
                val net = CoordMath.netArea(paths, pick) / 144.0
                Text("Perimeter: ${FractionUtils.formatInches(perim)}", fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                Text("Net Area: ${StoryPoleMath.fixed(net, 2)} sq ft", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                sides.forEach { Text("${it.first}: ${FractionUtils.formatInches(it.second)}", fontSize = 13.sp, color = Color.DarkGray) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { onPaths(paths.toMutableList().also { it[pick] = path.copy(isHole = !path.isHole) }) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (path.isHole) "MARK MAIN" else "MARK HOLE")
                }
                Text("Send to:", fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Same as the Android app: the area goes in as length x 1', the perimeter as a footing length
                    listOf(Triple("Slab", Screen.ConcreteCalculator, true), Triple("Footing", Screen.ConcreteCalculator, false),
                        Triple("Floor", Screen.FloorCalculator, true), Triple("Roof", Screen.RoofCalculator, true)).forEach { (label, scr, useArea) ->
                        Button(onClick = {
                            ToolImport.length = if (useArea) net else perim / 12.0
                            ToolImport.label = when { !useArea -> "Map Perimeter"; scr == Screen.RoofCalculator -> "Map Roof Area"; scr == Screen.FloorCalculator -> "Map Floor Area"; else -> "Map Area" }
                            onSend(scr)
                        }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(2.dp)) { Text(label, fontSize = 12.sp) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } }
    )
}
