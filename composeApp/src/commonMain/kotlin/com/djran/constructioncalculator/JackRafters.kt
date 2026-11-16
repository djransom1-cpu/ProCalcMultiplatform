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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/*
 * Jack rafters, irregular hips and flat tops on Rafter, Hip & Jack Cuts. Same math, results,
 * drawing and Data Sheet layout as the Android app (JackRafterMath is the same file).
 */

/** Jack card settings, remembered between visits. */
@Serializable
data class JackSettings(
    val irregular: Boolean = false,
    val endPitch: String = "4",
    val mated: Boolean = false,
    val oc: String = "16",
    val first: String = "16",
    val corners: String = "4",
    val jackThk: String = "1 1/2",
    val hipSel: String = "1.5",
    val hipCustom: String = "3 1/2",
    val markEdge: Boolean = false,
    val deduct: Boolean = true,
    val longestFirst: Boolean = true,
    val bldgLen: String = "",
    val ledger: String = "1 1/2",
    val joistOc: String = "16",
) {
    private fun num(s: String, def: Double): Double {
        if (s.isBlank()) return def
        val v = FractionUtils.parse(s)
        return if (v == 0.0 && s.trim('0', '.', ' ').isNotEmpty()) def else v
    }

    val hipThk get() = if (hipSel == "c") num(hipCustom, 3.5) else hipSel.toDoubleOrNull() ?: 1.5
    val hipLabel get() = when (hipSel) {
        "1.5" -> "2x (1-1/2)"; "1.75" -> "LVL 1-3/4"; "3.5" -> "2-ply 3-1/2"
        else -> "Custom ${FractionUtils.formatInches(hipThk, 16)}"
    }

    fun toSaved(span: Double, ridge: Double, pitch: Double, heel: Double, overhang: Double, flatTop: Boolean) = JackRafterMath.Saved(
        JackRafterMath.Input(
            span = span, ridge = ridge, mainPitch = pitch, heel = heel, overhang = overhang,
            jackThk = num(jackThk, 1.5), hipThk = hipThk, oc = num(oc, 16.0), firstMark = num(first, 16.0),
            corners = corners.trim().toIntOrNull() ?: 4, irregular = irregular, endPitch = num(endPitch, pitch),
            markTo = if (markEdge) JackRafterMath.MarkTo.NEAR_EDGE else JackRafterMath.MarkTo.CENTER,
            takeDeduction = deduct, mated = mated
        ),
        longestFirst = longestFirst, flatTop = flatTop, bldgLen = num(bldgLen, 0.0),
        ledgerThk = num(ledger, 1.5), joistOc = num(joistOc, 16.0), hipLabel = hipLabel
    )

    companion object {
        private const val KEY = "jack_settings_v1"
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        private val store by lazy { try { Settings() } catch (e: Throwable) { null } }
        fun load(): JackSettings = try { store?.getStringOrNull(KEY)?.let { json.decodeFromString<JackSettings>(it) } } catch (e: Throwable) { null } ?: JackSettings()
        fun save(s: JackSettings) { try { store?.putString(KEY, json.encodeToString(s)) } catch (_: Throwable) {} }

        /** Back from a saved layout (editing an entry). */
        fun from(sv: JackRafterMath.Saved): JackSettings {
            val i = sv.input
            val f = { x: Double -> FractionUtils.formatInches(x, 16).replace("\"", "") }
            val sel = when (i.hipThk) { 1.5 -> "1.5"; 1.75 -> "1.75"; 3.5 -> "3.5"; else -> "c" }
            return JackSettings(
                irregular = i.irregular, endPitch = JackResults.pn(i.endPitch), mated = i.mated, oc = f(i.oc), first = f(i.firstMark),
                corners = i.corners.toString(), jackThk = f(i.jackThk), hipSel = sel, hipCustom = f(i.hipThk),
                markEdge = i.markTo == JackRafterMath.MarkTo.NEAR_EDGE, deduct = i.takeDeduction, longestFirst = sv.longestFirst,
                bldgLen = if (sv.bldgLen > 0) FractionUtils.formatInches(sv.bldgLen, 16) else "", ledger = f(sv.ledgerThk), joistOc = f(sv.joistOc)
            )
        }
    }
}

/** Results text, shared by the screen, the Data Sheet and the PDF report. */
object JackResults {

    class Computed(val s: JackRafterMath.Saved, val r: JackRafterMath.Result, val deck: JackRafterMath.Deck?)

    fun compute(s: JackRafterMath.Saved): Computed {
        val r = JackRafterMath.compute(s.input)
        val d = if (s.flatTop && r.ok) JackRafterMath.deck(s.input, r, s.bldgLen, s.ledgerThk, s.joistOc) else null
        return Computed(s, r, d)
    }

    fun fromEntry(e: RafterEntry): Computed? = JackRafterMath.decode(e.jackData)?.let { compute(it) }

    private fun f(x: Double) = FractionUtils.formatInches(x, 16)
    fun pn(p: Double) = if (abs(p - kotlin.math.round(p)) < 1e-9) kotlin.math.round(p).toLong().toString() else StoryPoleMath.fixed(p, 2)
    private fun d1(x: Double) = StoryPoleMath.fixed(x, 1) + "°"

    fun hipTitle(c: Computed) = if (c.s.input.irregular) "HIP · IRREGULAR" else "HIP"
    fun hipSub(c: Computed) = "centerline, plate line to ridge · pitch ${StoryPoleMath.fixed(c.r.hipPitch, 2)}/12"

    fun hipRows(c: Computed): List<Pair<String, String>> {
        val r = c.r; val i = c.s.input
        val out = mutableListOf<Pair<String, String>>()
        out += "Hip length" to f(r.hipLength)
        out += "Overall to fascia corner" to f(r.hipOverall)
        out += "Hip angle off main wall" to d1(r.hipAngleDeg)
        out += "Hip plumb / level cut" to "${d1(JackRafterMath.plumbDeg(r.hipPitch))} / ${d1(90 - JackRafterMath.plumbDeg(r.hipPitch))}"
        if (i.irregular) {
            out += "Common (main walls)" to "${f(r.mainCommon)} · O/A ${f(r.mainCommonAll)}"
            out += "King common (end walls)" to "${f(r.kingCommon)} · O/A ${f(r.kingCommonAll)}"
            out += "Main run / end-wall run" to "${f(r.mainRun)} / ${f(r.endRun)}"
            out += "End-wall heel (fascias match)" to f(r.endHeel)
            out += "Hip on main plate, from corner" to f(r.hipOffMain)
            out += "Hip cheek cuts (main / end side)" to "${d1(r.end.cheekBevelDeg)} / ${d1(r.main.cheekBevelDeg)}"
        } else out += "Hip cheek cuts" to "45.0° each side"
        out += "Hip member" to c.s.hipLabel
        out += "Hip deduction (level)" to if (i.takeDeduction) (if (i.irregular) "${f(r.main.deduction)} main / ${f(r.end.deduction)} end" else f(r.main.deduction)) else "off"
        out += "Jacks for the whole roof" to r.totalJacks.toString()
        return out
    }

    fun matedNote(c: Computed): String? =
        if (c.s.input.irregular && c.s.input.mated)
            "Mated spacing puts the end-wall jacks at ${f(c.r.end.spacing)} OC so each pair meets at the hip. Check your sheathing layout; it no longer lands on ${f(c.s.input.oc)} centers."
        else null

    fun deckRows(c: Computed): List<Pair<String, String>> {
        val d = c.deck ?: return emptyList()
        val i = c.s.input
        val out = mutableListOf<Pair<String, String>>()
        out += "Top of rafters at deck edge, above plate" to f(i.heel + c.r.mainRun * i.mainPitch / 12.0)
        if (d.ok) {
            out += "Deck width x length (outside ledgers)" to "${f(d.width)} x ${f(d.length)}"
            out += "Ledger thickness" to f(c.s.ledgerThk)
            out += "Deck joists" to "${d.joistCount} @ ${f(d.joistLength)}, ${f(d.joistOc)} OC"
            out += "Hips land on the deck corners" to "${i.corners}"
        }
        return out
    }

    class SideTable(val title: String, val color: Color, val rows: List<Pair<String, String>>,
                    val headers: List<String>, val cells: List<List<String>>)

    val ORANGE = Color(0xFFC85F0C)
    val BLUE = Color(0xFF1D4ED8)

    fun sideTables(c: Computed): List<SideTable> {
        val r = c.r; val i = c.s.input
        fun one(s: JackRafterMath.Side, title: String, color: Color, showSpacing: Boolean): SideTable {
            val rows = mutableListOf<Pair<String, String>>()
            rows += "Common difference" to f(s.commonDiff)
            rows += "Plumb / seat cut" to "${pn(s.pitch)}/12 · ${d1(JackRafterMath.plumbDeg(s.pitch))} / ${d1(90 - JackRafterMath.plumbDeg(s.pitch))}"
            rows += "Cheek (side) cut bevel" to d1(s.cheekBevelDeg)
            if (showSpacing) rows += "Spacing on this wall" to "${f(s.spacing)} OC"
            val list = if (c.s.longestFirst) s.jacks.reversed() else s.jacks
            val cells = list.map { listOf("J${it.n}", f(it.mark), f(it.length), f(it.overall), r.qtyEach.toString()) }
            return SideTable(title, color, rows, listOf("Jack", "Plate mark", "To long pt", "Overall", "Qty"), cells)
        }
        return if (i.irregular) listOf(
            one(r.main, "MAIN WALLS (${pn(r.main.pitch)}/12)", ORANGE, false),
            one(r.end, "END WALLS (${pn(r.end.pitch)}/12)", BLUE, true)
        ) else listOf(one(r.main, "JACKS · BOTH WALLS, LEFT AND RIGHT PER CORNER", ORANGE, false))
    }

    fun planNote(c: Computed) = "Plate marks from the outside corner along each wall" +
        (if (c.s.input.markTo == JackRafterMath.MarkTo.NEAR_EDGE) " to the near edge of the jack" else " to the jack center") +
        ". Lengths along the top edge to the long point of the cheek cut."

    /** Plan height for a given width (same proportions as the Android drawing). */
    fun planAspect(c: Computed): Float {
        val ext = if (c.s.flatTop) 30.0 else 10.0
        val o = c.s.input.overhang
        return ((c.r.mainRun + ext + o) / (c.r.endRun + ext + o)).toFloat().coerceIn(0.33f, 1.4f)
    }

    /** Report sections for one saved entry (PDF and print). */
    fun reportSections(name: String, c: Computed): List<SummarySection> {
        if (!c.r.ok) return emptyList()
        val head = listOf("Item", "Value")
        val out = mutableListOf<SummarySection>()
        val rows = hipRows(c).map { listOf(it.first, it.second) } + listOfNotNull(matedNote(c)?.let { listOf("Note", it) })
        out += SummarySection("JACK RAFTERS: $name", head, rows)
        if (c.deck != null) out += SummarySection("FLAT TOP (DECK): $name", head,
            deckRows(c).map { listOf(it.first, it.second) } + c.deck.errors.map { listOf("Check", it) })
        sideTables(c).forEach { st ->
            out += SummarySection("$name · ${st.title}", st.headers,
                st.rows.map { listOf(it.first, it.second, "", "", "") } + st.cells)
        }
        return out
    }
}

private val NAVY = Color(0xFF0A2A66)
private val INK = Color(0xFF13213D)
private val MUTE = Color(0xFF5B6578)

/** Plan view of one hip corner, drawn to fit the scope (screen or PDF). */
fun DrawScope.drawJackPlan(c: JackResults.Computed, measurer: TextMeasurer) {
    val r = c.r
    if (!r.ok) return
    val i = c.s.input
    val o = i.overhang
    val rm = r.mainRun; val re = r.endRun
    val ext = if (c.s.flatTop) 30.0 else 10.0
    val u = size.width / 400f
    val padL = 34 * u; val pad = 22 * u
    val sc = min((size.width - padL - pad) / (re + ext + o), (size.height - 2 * pad) / (rm + ext + o)).toFloat()
    fun X(x: Double) = (padL + (x + o) * sc).toFloat()
    fun Y(y: Double) = (size.height - pad - (y + o) * sc).toFloat()
    fun txt(t: String, x: Float, y: Float, sz: Float, color: Color, align: Int = 0) {
        val m = measurer.measure(t, TextStyle(fontSize = (sz * u / density).sp, color = color, fontWeight = FontWeight.Bold))
        val x0 = when (align) { 1 -> x - m.size.width; 2 -> x - m.size.width / 2f; else -> x }
        drawText(m, topLeft = Offset(x0, y - m.size.height / 2f))
    }
    drawRect(Color.White)
    // overhang outline
    drawRect(Color(0xFFC9D0DC), Offset(X(-o), Y(rm + ext)), androidx.compose.ui.geometry.Size(X(re + ext) - X(-o), Y(-o) - Y(rm + ext)),
        style = Stroke(1 * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6 * u, 5 * u))))
    // plate lines (5-1/2" wall)
    val wall = Color(0xFF9AA6B8)
    drawRect(wall, Offset(X(0.0), Y(5.5)), androidx.compose.ui.geometry.Size(X(re + ext) - X(0.0), Y(0.0) - Y(5.5)))
    drawRect(wall, Offset(X(0.0), Y(rm + ext)), androidx.compose.ui.geometry.Size(X(5.5) - X(0.0), Y(0.0) - Y(rm + ext)))
    // jacks
    val jw = max(2.5f * u, (i.jackThk * sc).toFloat())
    r.main.jacks.forEach { j ->
        drawLine(JackResults.ORANGE, Offset(X(j.center), Y(-o)), Offset(X(j.center), Y(j.run)), jw, StrokeCap.Butt)
        txt("J${j.n}", X(j.center), Y(-o) + 9 * u, 9f, JackResults.ORANGE, 2)
    }
    r.end.jacks.forEach { j ->
        drawLine(JackResults.BLUE, Offset(X(-o), Y(j.center)), Offset(X(j.run), Y(j.center)), jw, StrokeCap.Butt)
        txt("J${j.n}", X(-o) - 3 * u, Y(j.center), 9f, JackResults.BLUE, 1)
    }
    // ridge or deck
    if (c.s.flatTop) {
        drawRect(Color(0xFFE6ECF5), Offset(X(re), Y(rm + ext)), androidx.compose.ui.geometry.Size(X(re + ext) - X(re), Y(rm) - Y(rm + ext)))
        var jx = re + c.s.joistOc / 2
        while (jx < re + ext && c.s.joistOc > 0) { drawLine(Color(0xFF8FA3C2), Offset(X(jx), Y(rm)), Offset(X(jx), Y(rm + ext)), 1 * u); jx += c.s.joistOc }
        val lw = max(2.5f * u, (1.5 * sc).toFloat())
        drawLine(NAVY, Offset(X(re), Y(rm)), Offset(X(re + ext), Y(rm)), lw)
        drawLine(NAVY, Offset(X(re), Y(rm)), Offset(X(re), Y(rm + ext)), lw)
    } else {
        drawLine(NAVY, Offset(X(re), Y(rm)), Offset(X(re + ext), Y(rm)), max(3f * u, (i.hipThk * sc).toFloat()))
    }
    // hip, through the fascia corner
    drawLine(INK, Offset(X(-o), Y(-o)), Offset(X(re), Y(rm)), max(3f * u, (i.hipThk * sc).toFloat()))
    txt("HIP", (X(-o) + X(re)) / 2 + 8 * u, (Y(-o) + Y(rm)) / 2, 11f, INK)
    txt(if (c.s.flatTop) "DECK" else "RIDGE", min(X(re) + 6 * u, size.width - 44 * u), Y(rm) - 8 * u, 11f, NAVY)
    if (r.hipOffMain > 0.05) {
        drawCircle(INK, 3.5f * u, Offset(X(r.hipOffMain), Y(0.0)))
        txt("hip on plate ${FractionUtils.formatInches(r.hipOffMain, 16)} from corner", X(r.hipOffMain) + 6 * u, Y(0.0) - 7 * u, 8f, INK)
    }
}

// ---------------- screen pieces ----------------

/** Jack inputs, shown with the roof inputs above Calculate. */
@Composable
fun JackInputs(s: JackSettings, flatTop: Boolean, onChange: (JackSettings) -> Unit, onFocus: (String, (String) -> Unit) -> Unit) {
    val c = AppTheme.colors
    @Composable
    fun field(label: String, value: String, modifier: Modifier, set: (String) -> Unit) =
        ConstructionTextField(value = value, onValueChange = set, label = label, modifier = modifier, onFocus = onFocus)

    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text("JACK RAFTERS (HIP ROOFS)", color = c.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        SpSegmented(listOf("Regular (one pitch)", "Irregular (two pitches)"), if (s.irregular) 1 else 0) { onChange(s.copy(irregular = it == 1)) }
        if (s.irregular) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) { field("End-wall pitch (/12)", s.endPitch, Modifier.weight(1f)) { onChange(s.copy(endPitch = it)) } }
            SpHint("Main pitch is on the long walls, this one on the end walls. The end-wall heel is set so both fascias land at the same height with equal overhangs, which moves the hip off the corner.")
            SpLabel("End-wall spacing", 10)
            SpSegmented(listOf("Same OC (keeps plywood layout)", "Mated (pairs meet at hip)"), if (s.mated) 1 else 0) { onChange(s.copy(mated = it == 1)) }
        }
        if (flatTop) {
            SpLabel("Flat top (deck)", 10)
            Row(Modifier.fillMaxWidth()) { field("Building length (outside)", s.bldgLen, Modifier.weight(1f)) { onChange(s.copy(bldgLen = it)) } }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                field("Ledger thickness", s.ledger, Modifier.weight(1f)) { onChange(s.copy(ledger = it)) }
                Spacer(Modifier.width(8.dp))
                field("Deck joists OC", s.joistOc, Modifier.weight(1f)) { onChange(s.copy(joistOc = it)) }
            }
            SpHint("Deck width is the Ridge box above, measured to the faces the rafters bear on. Deck joists span between the two long ledgers.")
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            field("On center", s.oc, Modifier.weight(1f)) { onChange(s.copy(oc = it)) }
            Spacer(Modifier.width(8.dp))
            field("First mark from corner", s.first, Modifier.weight(1f)) { onChange(s.copy(first = it)) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            field("Hip corners", s.corners, Modifier.weight(1f)) { onChange(s.copy(corners = it)) }
            Spacer(Modifier.width(8.dp))
            field("Jack thickness", s.jackThk, Modifier.weight(1f)) { onChange(s.copy(jackThk = it)) }
        }
        SpLabel("Hip member", 10)
        SpSegmented(listOf("2x 1-1/2", "LVL 1-3/4", "2-ply 3-1/2", "Custom"),
            when (s.hipSel) { "1.5" -> 0; "1.75" -> 1; "3.5" -> 2; else -> 3 }) {
            onChange(s.copy(hipSel = when (it) { 0 -> "1.5"; 1 -> "1.75"; 2 -> "3.5"; else -> "c" }))
        }
        if (s.hipSel == "c") Row(Modifier.fillMaxWidth().padding(top = 6.dp)) { field("Hip thickness", s.hipCustom, Modifier.weight(1f)) { onChange(s.copy(hipCustom = it)) } }
        SpLabel("Layout marks are to the", 10)
        SpSegmented(listOf("Center of jack", "Near edge of jack"), if (s.markEdge) 1 else 0) { onChange(s.copy(markEdge = it == 1)) }
        SpLabel("Hip deduction", 10)
        SpSegmented(listOf("Take it off", "Theoretical"), if (s.deduct) 0 else 1) { onChange(s.copy(deduct = it == 0)) }
        SpLabel("Order", 10)
        SpSegmented(listOf("Longest first", "Shortest first"), if (s.longestFirst) 0 else 1) { onChange(s.copy(longestFirst = it == 0)) }
    }
}

/** The full results block: hip card, deck card, plan, cut lists. */
@Composable
fun JackResultsBlock(jc: JackResults.Computed) {
    val c = AppTheme.colors
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        if (!jc.r.ok) {
            Text(jc.r.errors.joinToString("\n"), color = c.danger, fontSize = 14.sp)
            return@Column
        }
        Text("JACK RAFTER RESULTS", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        @Composable
        fun kv(k: String, v: String) = Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Text(k, Modifier.weight(1f), fontSize = 13.sp, color = Color(0xFF333333))
            Text(v, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.End)
        }
        Card(elevation = 0.dp, shape = RoundedCornerShape(12.dp), border = BorderStroke(2.dp, JackResults.ORANGE),
            backgroundColor = Color(0xFFFBE6D4), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text(JackResults.hipTitle(jc), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C4A14))
                Text(FractionUtils.formatInches(jc.r.hipLength, 16), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(JackResults.hipSub(jc), fontSize = 12.sp, color = Color(0xFF7C4A14))
                JackResults.hipRows(jc).drop(1).forEach { kv(it.first, it.second) }
                JackResults.matedNote(jc)?.let {
                    Text(it, fontSize = 12.sp, color = Color.Black, modifier = Modifier.fillMaxWidth().padding(top = 6.dp).background(Color(0xFFFFF7E6)).padding(8.dp))
                }
            }
        }
        jc.deck?.let { d ->
            Card(elevation = 0.dp, shape = RoundedCornerShape(12.dp), border = BorderStroke(2.dp, NAVY),
                backgroundColor = Color(0xFFEEF2F8), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("FLAT TOP (DECK)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NAVY)
                    JackResults.deckRows(jc).forEach { kv(it.first, it.second) }
                    if (!d.ok) Text(d.errors.joinToString("\n"), fontSize = 13.sp, color = Color(0xFFB3261E))
                }
            }
        }
        val measurer = rememberTextMeasurer()
        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            val h = maxWidth * JackResults.planAspect(jc)
            Canvas(Modifier.fillMaxWidth().height(h)) { drawJackPlan(jc, measurer) }
        }
        Text(JackResults.planNote(jc), fontSize = 12.sp, color = c.muted, modifier = Modifier.padding(top = 6.dp))
        JackResults.sideTables(jc).forEach { st ->
            Text(st.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = st.color, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
            Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 4.dp)) {
                st.rows.forEach { kv(it.first, it.second) }
            }
            Column(Modifier.fillMaxWidth().padding(top = 6.dp).background(Color(0xFFE3F2FD))) {
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    st.headers.forEach { Text(it, Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MUTE, textAlign = TextAlign.Center) }
                }
                st.cells.forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        row.forEachIndexed { k, v ->
                            Text(v, Modifier.weight(1f), fontSize = 13.sp, textAlign = TextAlign.Center,
                                fontWeight = if (k == 0 || k == 2) FontWeight.Bold else FontWeight.Normal,
                                color = if (k == 2) Color(0xFF1565C0) else Color.Black)
                        }
                    }
                }
            }
        }
    }
}
