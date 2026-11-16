package com.djran.constructioncalculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.djran.constructioncalculator.StoryPoleMath.Layer
import com.djran.constructioncalculator.StoryPoleMath.Mode
import com.djran.constructioncalculator.StoryPoleMath.Op
import com.djran.constructioncalculator.StoryPoleMath.Pole
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// ---------------- storage (this device) ----------------

@Serializable
private data class StoredLayer(val label: String = "", val amount: String = "", val op: String = "SUBTRACT")

@Serializable
private data class StoredPole(
    val name: String = "Pole", val mode: String = "LENGTH", val trade: String = "FRAMING",
    val startLabel: String = "Start", val start: String = "0", val endLabel: String = "Result", val target: String = "",
    val layers: List<StoredLayer> = emptyList()
)

@Serializable
private data class StoredState(val active: Int = 0, val poles: List<StoredPole> = emptyList(), val templates: List<StoredPole> = emptyList())

private fun Pole.toStored() = StoredPole(name, mode.name, trade, startLabel, start, endLabel, target, layers.map { StoredLayer(it.label, it.amount, it.op.name) })
private fun StoredPole.toPole() = Pole(
    name, runCatching { Mode.valueOf(mode) }.getOrDefault(Mode.LENGTH), trade, startLabel, start, endLabel, target,
    layers.map { Layer(it.label, it.amount, runCatching { Op.valueOf(it.op) }.getOrDefault(Op.SUBTRACT)) }.toMutableList()
)

private val storyJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
private const val STORY_KEY = "story_pole_v1"

private fun examplePoles(): List<Pole> {
    val a = StoryPolePresets.builtIns[0].pole.deepCopy().apply { name = "W8x18" }
    val b = a.deepCopy().apply { name = "W16x40"; layers[3] = Layer("Steel beam W16x40", "16", Op.SUBTRACT) }
    return listOf(a, b)
}

/**
 * Story Pole (Carpentry): work from a known height or elevation to the one the plans don't give,
 * adding (+) or subtracting (−) layers, or solving one unknown layer (?). Math in StoryPoleMath.
 */
@Composable
fun StoryPoleCalculator(
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit,
    projectName: String = "",
    onSaveToDataSheet: ((Pole) -> Unit)? = null
) {
    val c = AppTheme.colors
    val settings = remember { try { Settings() } catch (e: Throwable) { null } }
    // A version counter drives recomposition; poles are plain objects edited in place.
    var version by remember { mutableStateOf(0) }
    val poles = remember { mutableStateListOf<Pole>() }
    val templates = remember { mutableStateListOf<Pole>() }
    var active by remember { mutableStateOf(0) }
    var showNew by remember { mutableStateOf(false) }
    var showPreset by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val st = try { settings?.getStringOrNull(STORY_KEY)?.let { storyJson.decodeFromString<StoredState>(it) } } catch (e: Exception) { null }
        poles.clear(); templates.clear()
        if (st != null && st.poles.isNotEmpty()) {
            poles.addAll(st.poles.map { it.toPole() }); templates.addAll(st.templates.map { it.toPole() }); active = st.active
        } else poles.addAll(examplePoles())
        if (active !in poles.indices) active = 0
        version++
    }
    fun save() {
        settings?.putString(STORY_KEY, storyJson.encodeToString(StoredState(active, poles.map { it.toStored() }, templates.map { it.toStored() })))
    }
    fun touch() { version++; save() }

    ToolScreen(Screen.StoryPole, onBack) {
        if (poles.isEmpty()) return@ToolScreen
        val tick = version  // read so edits (which bump version) recompose this screen
        val p = poles[active.coerceIn(0, poles.size - 1)]
        val isElev = p.mode == Mode.ELEVATION
        val r = StoryPoleMath.compute(p)

        // ---- tabs ----
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
            poles.forEachIndexed { i, pole ->
                SpChip(pole.name.ifBlank { "Pole ${i + 1}" }, selected = i == active && !showNew) { active = i; showNew = false; touch() }
            }
            SpChip("＋ New", selected = showNew, accent = true) { showNew = !showNew }
        }

        Column(Modifier.padding(16.dp)) {
            if (showNew) {
                SpCard {
                    SpLabel("Start a new story pole")
                    SpListRow("Blank pole", "Start from 0 or any height, add your own layers") {
                        poles.add(StoryPolePresets.blank()); active = poles.size - 1; showNew = false; touch()
                    }
                    StoryPolePresets.builtIns.forEach { t ->
                        SpListRow(t.pole.name, "${StoryPolePresets.trade(t.pole.trade).name} · ${t.description}") {
                            poles.add(t.pole.deepCopy()); active = poles.size - 1; showNew = false; touch()
                        }
                    }
                    if (templates.isNotEmpty()) {
                        SpLabel("My templates", top = 12)
                        templates.forEachIndexed { i, t ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(1f)) {
                                    SpListRow("★ ${t.name}", "${t.layers.size} layers") {
                                        poles.add(t.deepCopy().apply { target = "" }); active = poles.size - 1; showNew = false; touch()
                                    }
                                }
                                TextButton(onClick = { templates.removeAt(i); touch() }) { Text("✕", color = c.danger) }
                            }
                        }
                    }
                }
                return@Column
            }

            // ---- name & trade ----
            SpCard {
                OutlinedTextField(value = p.name, onValueChange = { p.name = it; touch() }, label = { Text("Pole name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                SpLabel("Trade (sets which presets come first)", top = 10)
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    StoryPolePresets.trades.forEach { t -> SpChip(t.name, selected = p.trade == t.key, small = true) { p.trade = t.key; touch() } }
                }
            }

            // ---- mode & start ----
            SpCard {
                SpLabel("Working in")
                SpSegmented(listOf("Length / height", "Elevation"), if (isElev) 1 else 0) { idx ->
                    val to = if (idx == 1) Mode.ELEVATION else Mode.LENGTH
                    if (to != p.mode) {
                        p.start = StoryPoleMath.convertForMode(p.start, p.mode, to)
                        p.target = StoryPoleMath.convertForMode(p.target, p.mode, to)
                        p.mode = to; touch()
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = p.startLabel, onValueChange = { p.startLabel = it; touch() }, label = { Text("Start label") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                ConstructionTextField(value = p.start, onValueChange = { p.start = it; touch() },
                    label = if (isElev) "Start elevation" else "Start height", placeholder = if (isElev) "612.73" else "0",
                    modifier = Modifier.fillMaxWidth(), onFocus = onFocus)
                SpHint(if (isElev) "Plain number = decimal feet. Or feet-inches: 612' 8 3/4\"" else "Plain number = inches. Start at 0 for a straight length.")
            }

            // ---- layers ----
            SpCard {
                SpLabel("Layers — add (+), subtract (−), or solve (?)")
                p.layers.forEachIndexed { i, layer ->
                    val unknown = layer.op == Op.SOLVE
                    val row = r.rows.getOrNull(i)
                    Column(
                        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(10.dp))
                            .background(if (unknown) Color(0xFFEFF6FF) else c.bg)
                            .border(1.dp, if (unknown) Color(0xFF1D4ED8) else c.line, RoundedCornerShape(10.dp)).padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = layer.label, onValueChange = { layer.label = it; touch() }, label = { Text("Layer") }, singleLine = true, modifier = Modifier.weight(1f))
                            TextButton(onClick = { if (i > 0) { p.layers.add(i - 1, p.layers.removeAt(i)); touch() } }) { Text("▲", color = c.muted) }
                            TextButton(onClick = { if (i < p.layers.size - 1) { p.layers.add(i + 1, p.layers.removeAt(i)); touch() } }) { Text("▼", color = c.muted) }
                            TextButton(onClick = { p.layers.removeAt(i); touch() }) { Text("✕", color = c.muted) }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val ops = listOf(Op.SUBTRACT to "−", Op.ADD to "+", Op.SOLVE to "?")
                            val colors = listOf(Color(0xFFC2410C), Color(0xFF15803D), Color(0xFF1D4ED8))
                            Row(Modifier.border(1.dp, c.line, RoundedCornerShape(8.dp))) {
                                ops.forEachIndexed { k, (op, sym) ->
                                    val on = layer.op == op
                                    Box(
                                        Modifier.size(40.dp).clip(RoundedCornerShape(7.dp)).background(if (on) colors[k] else Color.Transparent)
                                            .clickable {
                                                if (op == Op.SOLVE) p.layers.forEachIndexed { j, l -> if (j != i && l.op == Op.SOLVE) l.op = Op.SUBTRACT }
                                                layer.op = op; touch()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) { Text(sym, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (on) Color.White else c.muted) }
                                }
                            }
                            Spacer(Modifier.width(6.dp))
                            if (unknown) {
                                Text("solve", color = Color(0xFF1D4ED8), modifier = Modifier.width(96.dp).padding(start = 8.dp))
                            } else {
                                val bad = layer.amount.isNotBlank() && StoryPoleMath.parseLen(layer.amount, false) == null
                                ConstructionTextField(value = layer.amount, onValueChange = { layer.amount = it; touch() },
                                    label = if (bad) "check" else "in", modifier = Modifier.width(110.dp), onFocus = onFocus)
                            }
                            Spacer(Modifier.width(6.dp))
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                val v = row?.value
                                if (i == r.unknownIndex && r.solved != null) {
                                    Text(StoryPoleMath.fmtFtIn(v), fontWeight = FontWeight.Bold, color = c.ink, maxLines = 1)
                                    Text("${if (r.solved >= 0) "+" else "−"} ${StoryPoleMath.fmtIn(abs(r.solved))} solved", fontSize = 12.sp, color = Color(0xFF1D4ED8))
                                } else if (v != null) {
                                    Text(StoryPoleMath.fmtFtIn(v), fontWeight = FontWeight.Bold, color = c.ink, maxLines = 1)
                                    Text(StoryPoleMath.fmtSecond(v, p.mode), fontSize = 12.sp, color = c.muted)
                                } else Text("—", color = c.muted)
                            }
                        }
                    }
                }
                Row {
                    Button(onClick = { p.layers.add(Layer()); touch() }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(backgroundColor = c.eq, contentColor = c.eqInk)) { Text("＋ Add layer") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { showPreset = !showPreset }, modifier = Modifier.weight(1f)) { Text(if (showPreset) "Hide presets" else "＋ Add preset") }
                }
                if (showPreset) {
                    val order = listOf(StoryPolePresets.trade(p.trade)) + StoryPolePresets.trades.filter { it.key != p.trade }
                    order.forEachIndexed { ti, t ->
                        t.groups.forEach { g ->
                            SpLabel("${if (ti == 0) "★ " else ""}${t.name} — ${g.name}", top = 10)
                            Row(Modifier.horizontalScroll(rememberScrollState())) {
                                g.presets.forEach { pr -> SpChip("${pr.label} · ${pr.amount}\"", selected = false, small = true) { p.layers.add(Layer(pr.label, pr.amount, Op.SUBTRACT)); touch() } }
                            }
                        }
                    }
                }
                SpHint("Amounts: plain number = inches. Also 11 1/4, 8.14, 1' 2 3/8\", .5'")
            }

            // ---- result label / known end ----
            SpCard {
                OutlinedTextField(value = p.endLabel, onValueChange = { p.endLabel = it; touch() }, label = { Text("What you're solving for") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (p.layers.any { it.op == Op.SOLVE }) {
                    Spacer(Modifier.height(6.dp))
                    ConstructionTextField(value = p.target, onValueChange = { p.target = it; touch() },
                        label = "Known end ${if (isElev) "elevation" else "height"} (solves the ? row)", modifier = Modifier.fillMaxWidth(), onFocus = onFocus)
                }
            }

            // ---- result ----
            if (!r.ok) {
                SpCard { Text(r.errors.joinToString("\n"), color = c.danger) }
            } else {
                val change = r.final!! - r.start!!
                val sub = when {
                    isElev -> "${StoryPoleMath.fmtDecFt(r.final)}  ·  ${if (change <= 0) "−" else "+"}${StoryPoleMath.fmtIn(abs(change))} from ${p.startLabel.ifBlank { "start" }}"
                    r.start == 0.0 -> "${StoryPoleMath.fmtIn(r.final)} total"
                    else -> "${StoryPoleMath.fmtIn(r.final)}  ·  ${if (change <= 0) "−" else "+"}${StoryPoleMath.fmtIn(abs(change))} from ${p.startLabel.ifBlank { "start" }}"
                }
                Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(2.dp, c.accent), backgroundColor = c.surface, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(p.endLabel.ifBlank { "Result" }.uppercase(), color = c.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(StoryPoleMath.fmtFtIn(r.final), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = c.ink)
                        Text(sub, fontSize = 14.sp, color = c.muted)
                        if (r.unknownIndex >= 0 && r.solved != null) {
                            Text("${p.layers[r.unknownIndex].label.ifBlank { "Unknown layer" }} = ${StoryPoleMath.fmtIn(abs(r.solved))} ${if (r.solved >= 0) "(adds / goes up)" else "(subtracts / goes down)"}",
                                fontSize = 15.sp, color = c.ink, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
                Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, c.line), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    StoryPoleDiagram(p, Modifier.fillMaxWidth().height((110 + 52 * max(1, p.layers.size)).dp))
                }
            }

            // ---- compare ----
            if (poles.size > 1) {
                SpCard {
                    SpLabel("Compare all poles")
                    poles.forEachIndexed { i, cp ->
                        val cr = StoryPoleMath.compute(cp)
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(cp.name.ifBlank { "Pole ${i + 1}" }, fontWeight = if (i == active) FontWeight.Bold else FontWeight.Normal, color = c.ink)
                                Text(cp.endLabel, fontSize = 12.sp, color = c.muted)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(if (cr.ok) StoryPoleMath.fmtFtIn(cr.final) else "check inputs", fontWeight = FontWeight.Bold, color = if (cr.ok) c.ink else c.danger)
                                if (cr.ok) Text(StoryPoleMath.fmtSecond(cr.final, cp.mode), fontSize = 12.sp, color = c.muted)
                            }
                        }
                    }
                }
            }

            // ---- actions ----
            SpCard {
                if (onSaveToDataSheet != null && projectName.isNotBlank()) {
                    var savedMsg by remember { mutableStateOf("") }
                    Button(onClick = { onSaveToDataSheet(p.deepCopy()); savedMsg = "Saved \"${p.name}\" to the $projectName Data Sheet." },
                        modifier = Modifier.fillMaxWidth(), enabled = StoryPoleMath.compute(p).ok,
                        colors = ButtonDefaults.buttonColors(backgroundColor = c.eq, contentColor = c.eqInk)) { Text("Save to Data Sheet ($projectName)") }
                    if (savedMsg.isNotEmpty()) SpHint(savedMsg)
                    Spacer(Modifier.height(8.dp))
                }
                Row {
                    OutlinedButton(onClick = { poles.add(p.deepCopy().apply { name = "${p.name.ifBlank { "Pole" }} (copy)" }); active = poles.size - 1; touch() }, modifier = Modifier.weight(1f)) { Text("⧉ Duplicate") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { templates.add(p.deepCopy().apply { target = "" }); touch() }, modifier = Modifier.weight(1f)) { Text("★ Save template") }
                }
                Row {
                    OutlinedButton(onClick = { if (poles.size > 1) { poles.removeAt(active); active = (active - 1).coerceAtLeast(0); touch() } }, modifier = Modifier.weight(1f), enabled = poles.size > 1) { Text("Delete pole") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { poles.clear(); poles.addAll(examplePoles()); active = 0; touch() }, modifier = Modifier.weight(1f)) { Text("Reset examples") }
                }
                SpHint("Poles and templates are saved on this device.")
            }
        }
    }
}

// ---------------- the drawing ----------------

@Composable
fun StoryPoleDiagram(p: Pole, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier.background(Color.White)) { drawStoryPole(p, measurer) }
}

/** The story pole drawing, for the screen, the Data Sheet and the PDF report. */
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStoryPole(p: Pole, measurer: androidx.compose.ui.text.TextMeasurer) {
    val r = StoryPoleMath.compute(p)
    run {
        if (!r.ok || p.layers.isEmpty()) return
        val w = size.width; val h = size.height
        val s = w / 400f
        val deltas = r.rows.map { it.delta ?: 0.0 }
        val levels = listOf(r.start!!) + r.rows.map { it.value ?: 0.0 }
        val n = p.layers.size
        val top = 34 * s; val bottom = 34 * s
        val avail = max(10f, h - top - bottom)
        val minBand = 30 * s
        val sumAbs = deltas.sumOf { abs(it) }.toFloat().coerceAtLeast(0.0001f)
        var bands = deltas.map { max(minBand, abs(it).toFloat() / sumAbs * avail) }
        fun positions(b: List<Float>): List<Float> {
            val ys = mutableListOf(0f)
            b.forEachIndexed { i, band -> ys += ys.last() + if (deltas[i] >= 0) -band else band }
            return ys
        }
        var ys = positions(bands)
        val range = (ys.max() - ys.min()).coerceAtLeast(1f)
        if (range > avail) { val k = avail / range; bands = bands.map { it * k }; ys = positions(bands) }
        val shift = top - ys.min() + (avail - (ys.max() - ys.min())) / 2f
        ys = ys.map { it + shift }
        val cols = IntArray(n)
        for (i in 1 until n) cols[i] = cols[i - 1] + if ((deltas[i] >= 0) != (deltas[i - 1] >= 0)) 1 else 0
        val numCols = cols.last() + 1
        val poleLeft = w * 0.36f; val poleRight = w * 0.60f
        val colW = (poleRight - poleLeft) / numCols
        val valueX = w - 8 * s; val labelRight = poleLeft - 8 * s
        val dash = PathEffect.dashPathEffect(floatArrayOf(5 * s, 4 * s))
        levels.indices.forEach { i -> drawLine(Color(0xFFB8C0CC), Offset(poleLeft, ys[i]), Offset(valueX - 4 * s, ys[i]), 1 * s, pathEffect = dash) }

        fun txt(t: String, x: Float, y: Float, size: Float, color: Color, bold: Boolean, alignRight: Boolean, maxW: Float = Float.MAX_VALUE) {
            val style = TextStyle(fontSize = (size * s / density).sp, color = color, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
            val m = measurer.measure(t, style, maxLines = 1, overflow = TextOverflow.Ellipsis,
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = if (maxW == Float.MAX_VALUE) Int.MAX_VALUE else max(1, maxW.toInt())))
            val x0 = if (alignRight) x - m.size.width else x
            drawText(m, topLeft = Offset(x0, y - m.size.height / 2f))
        }

        p.layers.forEachIndexed { i, layer ->
            val (f, l) = when {
                i == r.unknownIndex -> Color(0xFFDBE7FD) to Color(0xFF1D4ED8)
                deltas[i] >= 0 -> Color(0xFFDCF3E3) to Color(0xFF15803D)
                else -> Color(0xFFFBE6D4) to Color(0xFFC2410C)
            }
            val x0 = poleLeft + cols[i] * colW
            val y0 = min(ys[i], ys[i + 1]); val y1 = max(ys[i], ys[i + 1])
            drawRect(f, Offset(x0, y0), Size(colW, y1 - y0))
            drawRect(l, Offset(x0, y0), Size(colW, y1 - y0), style = Stroke(1.6f * s))
            if (y1 - y0 > 14 * s) txt(if (deltas[i] >= 0) "▲" else "▼", x0 + colW / 2 + 5 * s, (y0 + y1) / 2, 12f, l, true, true)
            val cy = (y0 + y1) / 2
            txt(layer.label.ifBlank { "Layer ${i + 1}" }, labelRight, cy - 6 * s, 11.5f, Color(0xFF13213D), true, true, labelRight - 6 * s)
            txt((if (deltas[i] >= 0) "+" else "−") + StoryPoleMath.fmtIn(abs(deltas[i])) + if (i == r.unknownIndex) " (solved)" else "", labelRight, cy + 8 * s, 10.5f, l, false, true)
        }
        levels.forEachIndexed { i, v ->
            val isStart = i == 0; val isEnd = i == levels.lastIndex
            val color = when { isEnd -> Color(0xFFC85F0C); isStart -> Color(0xFF0A2A66); else -> Color(0xFF13213D) }
            if (isStart || isEnd) txt((if (isEnd) p.endLabel.ifBlank { "Result" } else p.startLabel.ifBlank { "Start" }).uppercase(), valueX, ys[i] - 9 * s, 9.5f, color, true, true, valueX - poleRight - 6 * s)
            txt(StoryPoleMath.fmtFtIn(v), valueX, ys[i] + (if (isStart || isEnd) 8f else 0f) * s, if (isStart || isEnd) 13f else 11.5f, color, isStart || isEnd, true)
            if (isStart || isEnd) txt(StoryPoleMath.fmtSecond(v, p.mode), valueX, ys[i] + 22 * s, 9.5f, Color(0xFF5B6578), false, true)
            drawLine(if (isEnd) Color(0xFFC85F0C) else if (isStart) Color(0xFF0A2A66) else Color(0xFF5B6578),
                Offset(poleLeft - 4 * s, ys[i]), Offset(poleRight + 4 * s, ys[i]), (if (isStart || isEnd) 2.4f else 1.2f) * s)
        }
        txt("STORY POLE · ${p.name}".uppercase(), 8 * s, 14 * s, 11f, Color(0xFF0A2A66), true, false)
    }
}

// ---------------- small shared pieces (Story Pole and Survey Calc) ----------------

@Composable
internal fun SpCard(content: @Composable ColumnScope.() -> Unit) {
    val c = AppTheme.colors
    Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, c.line), backgroundColor = c.surface,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) { Column(Modifier.padding(12.dp), content = content) }
}

@Composable
internal fun SpLabel(t: String, top: Int = 0) {
    Text(t.uppercase(), color = AppTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = top.dp, bottom = 6.dp))
}

@Composable
internal fun SpHint(t: String) { Text(t, color = AppTheme.colors.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }

@Composable
internal fun SpChip(t: String, selected: Boolean, accent: Boolean = false, small: Boolean = false, onClick: () -> Unit) {
    val c = AppTheme.colors
    val bg = when { selected -> c.accent; accent -> c.eq; else -> c.surface }
    val fg = when { selected -> Color.White; accent -> c.eqInk; else -> c.ink }
    Box(
        Modifier.padding(end = 6.dp, bottom = 6.dp).clip(RoundedCornerShape(10.dp)).background(bg)
            .border(1.dp, if (selected || accent) bg else c.line, RoundedCornerShape(10.dp)).clickable(onClick = onClick)
            .padding(horizontal = if (small) 10.dp else 14.dp, vertical = if (small) 6.dp else 9.dp)
    ) { Text(t, color = fg, fontSize = if (small) 13.sp else 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1) }
}

@Composable
internal fun SpSegmented(options: List<String>, selected: Int, onPick: (Int) -> Unit) {
    val c = AppTheme.colors
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).border(1.dp, c.line, RoundedCornerShape(9.dp))) {
        options.forEachIndexed { i, o ->
            val on = i == selected
            Box(Modifier.weight(1f).background(if (on) c.ink else Color.Transparent).clickable { onPick(i) }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text(o, color = if (on) c.bg else c.muted, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun SpListRow(title: String, sub: String, onClick: () -> Unit) {
    val c = AppTheme.colors
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, c.line, RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(10.dp)) {
        Text(title, fontWeight = FontWeight.Bold, color = c.ink)
        Text(sub, fontSize = 12.sp, color = c.muted)
    }
}
