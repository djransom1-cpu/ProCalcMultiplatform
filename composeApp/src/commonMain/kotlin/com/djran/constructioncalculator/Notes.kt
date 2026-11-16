package com.djran.constructioncalculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import kotlin.math.*

/*
 * Notes, matching the Android app: sketch with a pen or eraser, typed notes, and shape calcs
 * (rectangle, circle, triangle) with area and perimeter. Android's handwriting-math recognition
 * uses Google's on-device recognizer, which the web and desktop builds don't have.
 */

/** A shape calc saved in a project's notes. Dimensions in inches. */
@Serializable
data class NoteShape(val kind: String, val a: Double, val b: Double = 0.0, val c: Double = 0.0)

object NoteShapes {
    private fun sqFt(sqIn: Double) = StoryPoleMath.fixed(sqIn / 144.0, 2)
    private fun f(x: Double) = FractionUtils.formatInches(x)

    /** Heron's formula; null when the three sides can't form a triangle. */
    fun triangleArea(a: Double, b: Double, c: Double): Double? {
        val s = (a + b + c) / 2
        val r = s * (s - a) * (s - b) * (s - c)
        return if (r <= 0) null else sqrt(r)
    }

    fun lines(sh: NoteShape): List<String> = when (sh.kind) {
        "Rectangle" -> listOf("${f(sh.a)} x ${f(sh.b)}", "Area: ${sqFt(sh.a * sh.b)} sq ft", "Perimeter: ${f(2 * (sh.a + sh.b))}")
        "Circle" -> listOf("r = ${f(sh.a)}", "Area: ${sqFt(PI * sh.a * sh.a)} sq ft", "Circumference: ${f(2 * PI * sh.a)}")
        else -> listOf("${f(sh.a)}, ${f(sh.b)}, ${f(sh.c)}", "Area: ${sqFt(triangleArea(sh.a, sh.b, sh.c) ?: 0.0)} sq ft", "Perimeter: ${f(sh.a + sh.b + sh.c)}")
    }
}

@Composable
private fun ShapeSketch(sh: NoteShape, modifier: Modifier) {
    Canvas(modifier.background(Color.White)) {
        val st = Stroke(3f)
        when (sh.kind) {
            "Rectangle" -> {
                val k = min(size.width / sh.a.toFloat().coerceAtLeast(0.01f), size.height / sh.b.toFloat().coerceAtLeast(0.01f)) * 0.8f
                val w = sh.a.toFloat() * k; val h = sh.b.toFloat() * k
                drawRect(Color.Black, Offset((size.width - w) / 2, (size.height - h) / 2), androidx.compose.ui.geometry.Size(w, h), style = st)
            }
            "Circle" -> drawCircle(Color.Black, min(size.width, size.height) * 0.4f, center, style = st)
            else -> {
                val a = sh.a; val b = sh.b; val c = sh.c
                val cosA = ((b * b) + (a * a) - (c * c)) / (2 * b * a)
                val ang = acos(cosA.coerceIn(-1.0, 1.0))
                val px = b * cos(ang); val py = b * sin(ang)
                val minX = min(0.0, px); val maxX = max(a, px)
                val k = min(size.width / (maxX - minX), size.height / py.coerceAtLeast(0.01)).toFloat() * 0.85f
                val ox = (size.width - (maxX - minX).toFloat() * k) / 2 - minX.toFloat() * k
                val oy = size.height - (size.height - py.toFloat() * k) / 2
                val p = Path().apply { moveTo(ox, oy); lineTo(ox + a.toFloat() * k, oy); lineTo(ox + px.toFloat() * k, oy - py.toFloat() * k); close() }
                drawPath(p, Color.Black, style = st)
            }
        }
    }
}

@Composable
fun JobNotesScreen(
    savedPaths: List<SerializablePath>,
    onUpdateNotes: (List<SerializablePath>) -> Unit,
    textNote: String = "",
    onTextChange: (String) -> Unit = {},
    shapes: List<NoteShape> = emptyList(),
    onShapesChange: (List<NoteShape>) -> Unit = {},
    onBack: () -> Unit
) {
    val paths = remember { mutableStateListOf<Pair<Path, Color>>() }
    var eraser by remember { mutableStateOf(false) }
    var shapeKind by remember { mutableStateOf<String?>(null) }
    var showShapeMenu by remember { mutableStateOf(false) }
    var d1 by remember { mutableStateOf("") }; var d2 by remember { mutableStateOf("") }; var d3 by remember { mutableStateOf("") }
    var shapeError by remember { mutableStateOf("") }

    LaunchedEffect(savedPaths) {
        fun toPath(sp: SerializablePath) = Path().apply {
            if (sp.points.isNotEmpty()) { moveTo(sp.points[0].x, sp.points[0].y); sp.points.drop(1).forEach { lineTo(it.x, it.y) } }
        } to Color(sp.color)
        when {
            savedPaths.isEmpty() -> paths.clear()
            paths.size < savedPaths.size -> for (i in paths.size until savedPaths.size) paths.add(toPath(savedPaths[i]))
            paths.size > savedPaths.size -> { paths.clear(); savedPaths.forEach { paths.add(toPath(it)) } }
        }
    }

    Column(Modifier.fillMaxSize().background(Palette.Bg)) {
        Row(Modifier.fillMaxWidth().background(Palette.Bg).padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Palette.Navy, modifier = Modifier.size(30.dp)) }
            Text("S-Pen / Notes", color = Palette.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        // Tools, like the Android app: PEN, ERASER, CLEAR ALL, SHAPE
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            @Composable
            fun tool(label: String, on: Boolean, onClick: () -> Unit) = Button(onClick = onClick, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(backgroundColor = if (on) Palette.Navy else Color(0xFFE3E8F0), contentColor = if (on) Color.White else Palette.Ink),
                elevation = ButtonDefaults.elevation(0.dp), contentPadding = PaddingValues(4.dp)) { Text(label, fontSize = 12.sp) }
            tool("PEN", !eraser) { eraser = false }
            tool("ERASER", eraser) { eraser = true }
            tool("CLEAR ALL", false) { paths.clear(); onUpdateNotes(emptyList()) }
            Box(Modifier.weight(1f)) {
                tool("SHAPE", false) { showShapeMenu = true }
                DropdownMenu(expanded = showShapeMenu, onDismissRequest = { showShapeMenu = false }) {
                    listOf("Rectangle", "Circle", "Triangle").forEach { k ->
                        DropdownMenuItem(onClick = { showShapeMenu = false; shapeKind = k; d1 = ""; d2 = ""; d3 = ""; shapeError = "" }) { Text(k) }
                    }
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(16.dp).border(1.dp, Color.Gray, RoundedCornerShape(8.dp))) {
            SketchCanvas(paths = paths, penColor = if (eraser) Color.White else Color.Black, onRawPathAdded = { offsets ->
                onUpdateNotes(savedPaths + SerializablePath(offsets.map { SerializablePoint(it.x, it.y) }, (if (eraser) Color.White else Color.Black).toArgb()))
            })
        }
        if (shapes.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                shapes.takeLast(3).forEachIndexed { i, sh ->
                    val idx = shapes.size - minOf(3, shapes.size) + i
                    Card(elevation = 0.dp, border = BorderStroke(1.dp, Color(0xFFCCCCCC)), modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(sh.kind, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), color = Palette.Ink)
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray,
                                    modifier = Modifier.size(18.dp).clickable { onShapesChange(shapes.filterIndexed { k, _ -> k != idx }) })
                            }
                            ShapeSketch(sh, Modifier.fillMaxWidth().height(60.dp))
                            NoteShapes.lines(sh).forEachIndexed { k, l -> Text(l, fontSize = 11.sp, color = if (k == 0) Color.Black else Color(0xFF1565C0), fontWeight = if (k > 0) FontWeight.Bold else FontWeight.Normal) }
                        }
                    }
                }
            }
        }
        OutlinedTextField(value = textNote, onValueChange = onTextChange, placeholder = { Text("Tap to type notes...") },
            modifier = Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 16.dp, vertical = 8.dp))
    }

    shapeKind?.let { kind ->
        AlertDialog(
            onDismissRequest = { shapeKind = null },
            title = { Text(if (kind == "Triangle") "Triangle (3 sides)" else kind) },
            text = {
                Column {
                    when (kind) {
                        "Rectangle" -> { OutlinedTextField(d1, { d1 = it }, label = { Text("Width (e.g. 10' 6\")") }); OutlinedTextField(d2, { d2 = it }, label = { Text("Height (e.g. 8')") }) }
                        "Circle" -> OutlinedTextField(d1, { d1 = it }, label = { Text("Radius (e.g. 2' 6\")") })
                        else -> { OutlinedTextField(d1, { d1 = it }, label = { Text("Side A") }); OutlinedTextField(d2, { d2 = it }, label = { Text("Side B") }); OutlinedTextField(d3, { d3 = it }, label = { Text("Side C") }) }
                    }
                    if (shapeError.isNotEmpty()) Text(shapeError, color = Color.Red, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val a = FractionUtils.parse(d1); val b = FractionUtils.parse(d2); val c = FractionUtils.parse(d3)
                    when (kind) {
                        "Rectangle" -> if (a > 0 && b > 0) { onShapesChange(shapes + NoteShape(kind, a, b)); shapeKind = null } else shapeError = "Enter both dimensions"
                        "Circle" -> if (a > 0) { onShapesChange(shapes + NoteShape(kind, a)); shapeKind = null } else shapeError = "Enter a radius"
                        else -> when {
                            a <= 0 || b <= 0 || c <= 0 -> shapeError = "Enter all three sides"
                            NoteShapes.triangleArea(a, b, c) == null -> shapeError = "Those three sides can't form a triangle"
                            else -> { onShapesChange(shapes + NoteShape(kind, a, b, c)); shapeKind = null }
                        }
                    }
                }) { Text("INSERT") }
            },
            dismissButton = { TextButton(onClick = { shapeKind = null }) { Text("Cancel") } }
        )
    }
}

