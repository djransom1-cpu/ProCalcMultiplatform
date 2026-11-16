package com.djran.constructioncalculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.djran.surveycore.SurveyCore
import com.djran.surveycore.SurveyCore.AngleMode
import com.djran.surveycore.SurveyCore.KnownObs
import com.djran.surveycore.SurveyCore.Shot
import com.djran.surveycore.SurveyCore.Tolerances
import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

private val SURVEY_DEFAULTS = linkedMapOf(
    "angMode" to "dec", "tolH" to "0.02", "tolV" to "0.03", "tolA" to "10", "tolD" to "0.02", "sdA" to "5", "sdD" to "0.010",
    "dA" to "45°30'15\"", "dB" to "12°45'50\"", "dOp" to "+", "dWrap" to "1", "dConv" to "N 36 52 12 E",
    "iN1" to "5000.000", "iE1" to "5000.000", "iZ1" to "100.000", "iN2" to "5200.000", "iE2" to "5150.000", "iZ2" to "102.100",
    "sN" to "5000.000", "sE" to "5000.000", "sZ" to "100.000", "sHI" to "5.25", "bsBy" to "pt",
    "bsN" to "5200.000", "bsE" to "5150.000", "bsZ" to "102.100", "bsAz" to "", "bsSet" to "0°00'00\"",
    "bHA" to "0°00'04\"", "bMode" to "SD", "bHD" to "250.006", "bSD" to "250.013", "bZA" to "89°34'29.6\"", "bHR" to "5.00",
    "cN" to "4900.000", "cE" to "5120.000", "cZ" to "98.750", "cHA" to "92°56'12\"", "cMode" to "SD", "cHD" to "156.212",
    "cSD" to "156.219", "cZA" to "90°32'52.6\"", "cHR" to "5.00",
    "rMode" to "HD", "rCount" to "3", "rHI" to "5.25", "rHR" to "5.00",
    "rN1" to "5200.000", "rE1" to "5150.000", "rZ1" to "102.100", "rHA1" to "0°00'00\"", "rD1" to "250.004",
    "rN2" to "4900.000", "rE2" to "5120.000", "rZ2" to "98.750", "rHA2" to "92°56'10\"", "rD2" to "156.203",
    "rN3" to "5080.000", "rE3" to "4850.000", "rZ3" to "101.200", "rHA3" to "261°12'12\"", "rD3" to "170.006",
    "tab" to "dms"
)
private val SURVEY_TABS = listOf("dms" to "DMS", "inv" to "Inverse", "setup" to "Setup", "bs" to "BS Check", "chk" to "Check Shot", "res" to "Resection", "set" to "⚙")
private val surveyJson = Json { ignoreUnknownKeys = true }
private const val SURVEY_KEY = "survey_calc_v1"

/** One line of a result: label, value, optional PASS/FAIL. */
private data class SRow(val label: String, val value: String, val ok: Boolean? = null, val big: Boolean = false)

/** A computed result card. */
private class SOut {
    var errors: List<String> = emptyList()
    var verdict: Pair<Boolean, String>? = null
    var title = ""; var big = ""; var sub = ""
    val rows = mutableListOf<SRow>()
    val hints = mutableListOf<String>()
    var warn: String? = null
    var action: Pair<String, () -> Unit>? = null
}

private fun fixedStr(x: Double, d: Int) = SurveyCore.fixed(x, d)
private fun toDeg(r: Double) = r * 180.0 / PI

/**
 * Survey Calc (Site): DMS math, inverse, setup, backsight check, check shot, resection
 * (2-point direct, 3–5 point least squares). Math in SurveyCore, the same file the Android apps use.
 */
@Composable
fun SurveyCalculator(onBack: () -> Unit, projectName: String = "", onSaveRecord: ((SurveyRecordEntry) -> Unit)? = null) {
    val c = AppTheme.colors
    val settings = remember { try { Settings() } catch (e: Throwable) { null } }
    val v = remember { mutableStateMapOf<String, String>() }
    var focusedKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        v.clear(); v.putAll(SURVEY_DEFAULTS)
        try { settings?.getStringOrNull(SURVEY_KEY)?.let { v.putAll(surveyJson.decodeFromString<Map<String, String>>(it)) } } catch (e: Exception) {}
    }
    fun set(k: String, value: String) { v[k] = value; settings?.putString(SURVEY_KEY, surveyJson.encodeToString(v.toMap())) }

    val mode = if (v["angMode"] == "dmss") AngleMode.DMSS else AngleMode.DECIMAL
    fun ang(k: String) = SurveyCore.parseAngle(v[k], mode)
    fun num(k: String) = SurveyCore.parseNum(v[k])
    val tol = Tolerances(num("tolH") ?: 0.02, num("tolV") ?: 0.03, num("tolA") ?: 10.0, num("tolD") ?: 0.02)

    // ---- field helpers ----
    @Composable
    fun field(k: String, label: String, isAngle: Boolean = false, modifier: Modifier = Modifier) {
        val s = v[k] ?: ""
        val bad = s.isNotBlank() && (if (isAngle) SurveyCore.parseAngle(s, mode) == null else SurveyCore.parseNum(s) == null)
        OutlinedTextField(
            value = s, onValueChange = { set(k, it) }, label = { Text(label) }, singleLine = true, isError = bad,
            keyboardOptions = KeyboardOptions(keyboardType = if (isAngle) KeyboardType.Ascii else KeyboardType.Decimal),
            modifier = modifier.onFocusChanged { if (it.isFocused) focusedKey = k }
        )
    }
    @Composable
    fun row(vararg items: @Composable (Modifier) -> Unit) {
        Row(Modifier.fillMaxWidth()) {
            items.forEachIndexed { i, item ->
                if (i > 0) Spacer(Modifier.width(8.dp))
                item(Modifier.weight(1f))
            }
        }
    }
    @Composable
    fun seg(k: String, opts: List<Pair<String, String>>) {
        SpSegmented(opts.map { it.second }, opts.indexOfFirst { it.first == v[k] }.coerceAtLeast(0)) { set(k, opts[it].first) }
    }
    @Composable
    fun distFields(p: String) {
        seg(p + "Mode", listOf("HD" to "Horizontal dist", "SD" to "Slope dist + zenith"))
        Spacer(Modifier.height(8.dp))
        if (v[p + "Mode"] == "HD") row({ field(p + "HD", "HD (ft)", modifier = it) }, { field(p + "HR", "Rod ht (ft)", modifier = it) })
        else row({ field(p + "SD", "SD (ft)", modifier = it) }, { field(p + "ZA", "Zenith", true, it) }, { field(p + "HR", "Rod ht", modifier = it) })
    }

    // ---- shared computations ----
    fun errs() = mutableListOf<String>()
    fun needA(k: String, name: String, e: MutableList<String>) = ang(k) ?: run { e += "$name: enter a valid angle."; 0.0 }
    fun needN(k: String, name: String, e: MutableList<String>) = num(k) ?: run { e += "$name: enter a valid number."; 0.0 }
    fun shot(p: String, e: MutableList<String>): Shot =
        if (v[p + "Mode"] == "HD") SurveyCore.shotFromHd(needN(p + "HD", "Horizontal distance", e))
        else SurveyCore.shotFromSlope(needN(p + "SD", "Slope distance", e), needA(p + "ZA", "Zenith angle", e))
    fun setup(e: MutableList<String>): SurveyCore.Setup {
        val n = needN("sN", "Station N", e); val east = needN("sE", "Station E", e)
        val st = needA("bsSet", "Backsight circle reading", e)
        return if (v["bsBy"] == "pt") SurveyCore.setupFromPoint(n, east, num("sZ"), num("sHI"), needN("bsN", "Backsight N", e), needN("bsE", "Backsight E", e), num("bsZ"), st)
        else SurveyCore.setupFromAzimuth(n, east, num("sZ"), num("sHI"), needA("bsAz", "Backsight azimuth", e), st)
    }
    fun fmtT(x: Double) = if (x == kotlin.math.floor(x)) x.toLong().toString() else x.toString()
    fun setupSummary(): String {
        val e = errs(); val s = setup(e)
        return if (e.isNotEmpty()) "Setup incomplete — open the Setup tab."
        else "Station ${SurveyCore.fmtCoord(s.n)} N, ${SurveyCore.fmtCoord(s.e)} E · BS azimuth ${SurveyCore.fmtDms(s.bsAz)}" +
            (s.bsHd?.let { " · BS dist ${SurveyCore.fmtFt(it)}" } ?: "") + " · gun set to ${SurveyCore.fmtDms(s.bsSetting)} on BS"
    }

    // Save a result to the project's Data Sheet, like the Android app
    val typeNames = mapOf("dms" to "DMS math", "inv" to "Inverse", "setup" to "Setup", "bs" to "Backsight check", "chk" to "Check shot", "res" to "Resection")
    fun saver(tab: String): ((SOut) -> Unit)? {
        val save = onSaveRecord ?: return null
        if (projectName.isBlank()) return null
        return { o ->
            val lines = mutableListOf<String>()
            if (o.big.isNotEmpty()) lines += (if (o.title.isNotEmpty()) "${o.title}: " else "") + o.big
            if (o.sub.isNotEmpty()) lines += o.sub
            o.rows.forEach { r -> lines += "${r.label}: ${r.value}" + when (r.ok) { true -> " (PASS)"; false -> " (FAIL)"; null -> "" } }
            o.warn?.let { lines += it }
            save(SurveyRecordEntry(tab, typeNames[tab] ?: tab, currentTimeMillis(), o.verdict?.first, lines))
        }
    }

    fun compute(tab: String): SOut? {
        val o = SOut(); val e = errs()
        try {
            when (tab) {
                "dms" -> {
                    val a = needA("dA", "Angle A", e); val isN = v["dOp"] == "×" || v["dOp"] == "÷"
                    val b = if (isN) needN("dB", "Number", e) else needA("dB", "Angle B", e)
                    if (isN && v["dOp"] == "÷" && b == 0.0) e += "Can't divide by zero."
                    if (e.isEmpty()) {
                        var r = when (v["dOp"]) { "+" -> a + b; "−" -> a - b; "×" -> a * b; else -> a / b }
                        if (v["dWrap"] == "1") r = SurveyCore.norm360(r)
                        o.title = "Result"; o.big = SurveyCore.fmtDms(r)
                        o.sub = "${fixedStr(r, 6)}° · ${SurveyCore.fmtDms(r, 1)}" + if (r in 0.0..360.0) " · ${SurveyCore.fmtBearing(r)}" else ""
                        o.action = "Use as Angle A" to { set("dA", SurveyCore.fmtDms(r, 1)) }
                    }
                }
                "inv" -> {
                    val n1 = needN("iN1", "From N", e); val e1 = needN("iE1", "From E", e); val n2 = needN("iN2", "To N", e); val e2 = needN("iE2", "To E", e)
                    if (e.isEmpty()) {
                        val iv = SurveyCore.inverse(n1, e1, n2, e2)
                        o.title = "Azimuth · Horizontal distance"; o.big = "${SurveyCore.fmtDms(iv.az)} · ${SurveyCore.fmtFt(iv.hd)}"
                        o.sub = "${SurveyCore.fmtBearing(iv.az)} · back az ${SurveyCore.fmtDms(SurveyCore.norm360(iv.az + 180))}"
                        o.rows += SRow("ΔN", SurveyCore.fmtSignedFt(iv.dN)); o.rows += SRow("ΔE", SurveyCore.fmtSignedFt(iv.dE))
                        val z1 = num("iZ1"); val z2 = num("iZ2")
                        if (z1 != null && z2 != null) {
                            val dz = z2 - z1
                            o.rows += SRow("ΔElev", SurveyCore.fmtSignedFt(dz)); o.rows += SRow("Slope distance", SurveyCore.fmtFt(hypot(iv.hd, dz)))
                            o.rows += SRow("Grade", if (iv.hd > 0) fixedStr(dz / iv.hd * 100, 2) + "%" else "—")
                            o.rows += SRow("Zenith angle", SurveyCore.fmtDms(90 - toDeg(atan2(dz, iv.hd)), 1))
                        }
                    }
                }
                "setup" -> {
                    val s = setup(e)
                    if (e.isEmpty()) {
                        o.title = "Backsight azimuth"; o.big = SurveyCore.fmtDms(s.bsAz, 1)
                        o.sub = SurveyCore.fmtBearing(s.bsAz) + (s.bsHd?.let { " · distance ${SurveyCore.fmtFt(it)}" } ?: "")
                        o.hints += "Shot azimuth = ${SurveyCore.fmtDms(s.bsAz)} + (gun reading − ${SurveyCore.fmtDms(s.bsSetting)}). Used by BS Check and Check Shot."
                    }
                }
                "bs" -> {
                    val s = setup(e)
                    if (e.isNotEmpty()) { o.errors = listOf("Finish the Setup tab first."); return o }
                    val ha = needA("bHA", "Gun reading", e); val sh = shot("b", e)
                    if (e.isEmpty()) {
                        val r = SurveyCore.backsightCheck(s, ha, sh, num("bHR"), tol)
                        o.verdict = r.allOk to if (r.allOk) "BACKSIGHT CHECKS" else "BACKSIGHT OUT OF TOLERANCE"
                        o.rows += SRow("Angle error", SurveyCore.fmtSecErr(r.angleErr), r.angleOk)
                        o.rows += SRow("Side miss at ${SurveyCore.fmtFt(s.bsHd ?: sh.hd, 2)}", SurveyCore.fmtSignedFt(r.sideMiss), r.sideOk)
                        o.rows += if (r.distErr != null) SRow("Distance error (${SurveyCore.fmtFt(sh.hd)} vs ${SurveyCore.fmtFt(s.bsHd)})", SurveyCore.fmtSignedFt(r.distErr), r.distOk)
                                  else SRow("Distance error", "needs backsight point")
                        o.rows += if (r.elevErr != null) SRow("Elevation error", SurveyCore.fmtSignedFt(r.elevErr), r.elevOk)
                                  else SRow("Elevation error", "needs SD + zenith, HI, HR, elevations")
                        o.hints += "Tolerances: ${fmtT(tol.angleSec)}\" angle · ${fmtT(tol.horizontal)}' side · ${fmtT(tol.distance)}' distance · ${fmtT(tol.vertical)}' vertical (change in ⚙)."
                    }
                }
                "chk" -> {
                    val s = setup(e)
                    if (e.isNotEmpty()) { o.errors = listOf("Finish the Setup tab first."); return o }
                    val kn = needN("cN", "Check point N", e); val ke = needN("cE", "Check point E", e)
                    val ha = needA("cHA", "Gun reading", e); val sh = shot("c", e)
                    if (e.isEmpty()) {
                        val r = SurveyCore.checkShot(s, kn, ke, num("cZ"), ha, sh, num("cHR"), tol)
                        o.verdict = r.allOk to if (r.allOk) "CHECK SHOT GOOD" else "CHECK SHOT OUT OF TOLERANCE"
                        o.rows += SRow("Horizontal miss", SurveyCore.fmtFt(r.miss), r.missOk, big = true)
                        o.rows += SRow("ΔN (measured − known)", SurveyCore.fmtSignedFt(r.dN))
                        o.rows += SRow("ΔE (measured − known)", SurveyCore.fmtSignedFt(r.dE))
                        o.rows += if (r.dZ != null) SRow("ΔElev (measured − known)", SurveyCore.fmtSignedFt(r.dZ), r.elevOk) else SRow("ΔElev", "needs SD + zenith, HI, HR, elevations")
                        o.rows += SRow("Angle error", SurveyCore.fmtSecErr(r.angleErr), r.angleOk)
                        o.rows += SRow("Distance error", SurveyCore.fmtSignedFt(r.distErr), r.distOk)
                        o.rows += SRow("Measured point", "${SurveyCore.fmtCoord(r.measN)} N, ${SurveyCore.fmtCoord(r.measE)} E" + (r.measZ?.let { ", " + fixedStr(it, 3) } ?: ""))
                        o.rows += SRow("Shot azimuth / HD", "${SurveyCore.fmtDms(r.shotAz)} / ${SurveyCore.fmtFt(r.shotHd)}")
                        o.rows += SRow("Computed azimuth / HD", "${SurveyCore.fmtDms(r.compAz)} / ${SurveyCore.fmtFt(r.compHd)}")
                        o.hints += "Pass/fail uses the horizontal miss (${fmtT(tol.horizontal)}') and vertical (${fmtT(tol.vertical)}'). Angle and distance are broken out to show which one is off."
                    }
                }
                "res" -> {
                    val n = (v["rCount"]?.toIntOrNull() ?: 2).coerceIn(2, 5)
                    val pts = (1..n).map { i ->
                        val pn = needN("rN$i", "Point $i N", e); val pe = needN("rE$i", "Point $i E", e)
                        val ha = needA("rHA$i", "Point $i reading", e)
                        val sh = if (v["rMode"] == "HD") SurveyCore.shotFromHd(needN("rD$i", "Point $i HD", e))
                                 else SurveyCore.shotFromSlope(needN("rSD$i", "Point $i SD", e), needA("rZA$i", "Point $i zenith", e))
                        KnownObs(pn, pe, num("rZ$i"), ha, sh)
                    }
                    if (e.isEmpty()) {
                        val r = SurveyCore.resection(pts, num("rHI"), num("rHR"), tol, num("sdA") ?: 5.0, num("sdD") ?: 0.010)
                        o.verdict = r.allOk to if (r.allOk) "RESECTION CHECKS" else "RESECTION OUT OF TOLERANCE"
                        o.title = if (r.leastSquares) "Station · least squares, $n points" else "Station · direct, 2 points"
                        o.big = "${SurveyCore.fmtCoord(r.n)} N · ${SurveyCore.fmtCoord(r.e)} E"
                        o.sub = r.z?.let { "Elev ${fixedStr(it, 3)}" + if (r.zCount > 1) " (avg of ${r.zCount})" else "" } ?: "Elevation needs SD + zenith, HI, HR and point elevations"
                        if (!r.leastSquares) {
                            o.rows += SRow("Angle misclosure", SurveyCore.fmtSecErr(r.angleMisclosure), abs(r.angleMisclosure!! * 3600) <= tol.angleSec)
                            o.rows += SRow("Baseline misclosure (${SurveyCore.fmtFt(r.baselineMeasured)} vs ${SurveyCore.fmtFt(r.baselineKnown)})", SurveyCore.fmtSignedFt(r.baselineMisclosure), abs(r.baselineMisclosure!!) <= tol.distance)
                            o.hints += "Add a 3rd point for a least-squares solution with a redundant check on every point."
                        } else {
                            r.residuals.forEach { p -> o.rows += SRow("Point ${p.index}: angle ${SurveyCore.fmtSecErr(p.angleRes)} · dist ${SurveyCore.fmtSignedFt(p.distRes)}", "${SurveyCore.fmtFt(p.miss)} miss", p.ok) }
                            o.rows += SRow("Std. error of station (N / E)", "±${fixedStr(r.sigmaN ?: 0.0, 3)}' / ±${fixedStr(r.sigmaE ?: 0.0, 3)}'")
                            o.rows += SRow("Std. dev. of unit weight (σ₀)", r.sigma0?.let { "${fixedStr(it, 2)} (${r.redundancy} redundant)" } ?: "—")
                            r.worstIndex?.let { w -> o.warn = "Point $w has the largest miss (${SurveyCore.fmtFt(r.residuals.first { it.index == w }.miss)}). Re-shoot it or check its coordinates." }
                        }
                        r.zSpread?.let { o.rows += SRow("Elevation spread", SurveyCore.fmtFt(it), it <= tol.vertical) }
                        o.rows += SRow("Azimuth to point 1", SurveyCore.fmtDms(r.azToPoint1, 1))
                        o.rows += SRow("Orientation (add to gun readings)", (if (r.orientation >= 0) "+" else "−") + SurveyCore.fmtDms(abs(r.orientation), 1))
                        o.action = "Use as Setup (backsight = point 1)" to {
                            set("sN", fixedStr(r.n, 3)); set("sE", fixedStr(r.e, 3)); r.z?.let { set("sZ", fixedStr(it, 3)) }
                            set("bsBy", "pt"); set("bsN", v["rN1"] ?: ""); set("bsE", v["rE1"] ?: ""); set("bsZ", v["rZ1"] ?: "")
                            set("bsSet", v["rHA1"] ?: "0"); set("sHI", v["rHI"] ?: ""); set("tab", "setup")
                        }
                    }
                }
                else -> return null
            }
        } catch (ex: SurveyCore.ResectionException) { o.errors = listOf(ex.message ?: "Can't solve."); return o }
        if (e.isNotEmpty()) o.errors = e
        return o
    }

    ToolScreen(Screen.SurveyCalc, onBack) {
        if (v.isEmpty()) return@ToolScreen
        val tab = v["tab"] ?: "dms"
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
            SURVEY_TABS.forEach { (k, name) -> SpChip(name, selected = tab == k) { set("tab", k) } }
        }
        // Quick keys type into the last box you tapped
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
            listOf("°" to "°", "'" to "'", "\"" to "\"", "space" to " ", "−" to "-").forEach { (lbl, ins) ->
                Box(Modifier.weight(1f).padding(end = 6.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, c.line, RoundedCornerShape(8.dp)).background(c.surface)
                    .clickable { focusedKey?.let { k -> set(k, (v[k] ?: "") + ins) } }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(lbl, color = c.ink, fontSize = 17.sp)
                }
            }
        }
        Column(Modifier.padding(16.dp)) {
            when (tab) {
                "dms" -> {
                    val isN = v["dOp"] == "×" || v["dOp"] == "÷"
                    SpCard {
                        SpLabel("Angle math")
                        field("dA", "Angle A", true, Modifier.fillMaxWidth())
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            listOf("+", "−", "×", "÷").forEach { op ->
                                val on = v["dOp"] == op
                                Box(Modifier.weight(1f).padding(end = 6.dp).clip(RoundedCornerShape(9.dp)).background(if (on) c.accent else c.surface)
                                    .border(1.dp, if (on) c.accent else c.line, RoundedCornerShape(9.dp)).clickable { set("dOp", op) }.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center) { Text(op, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (on) Color.White else c.ink) }
                            }
                        }
                        if (isN) field("dB", "Number", false, Modifier.fillMaxWidth()) else field("dB", "Angle B", true, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        seg("dWrap", listOf("1" to "Wrap to 0–360°", "0" to "Keep as is"))
                        SpHint("Type 45°30'15\", 45 30 15, 45-30-15, or decimal degrees${if (mode == AngleMode.DMSS) " (single numbers read as DDD.MMSS)" else ""}. Bearings work too: N 45 30 E.")
                    }
                    SurveyResult(compute("dms"), saver("dms"))
                    SpCard {
                        SpLabel("Convert")
                        field("dConv", "Angle, decimal degrees, or bearing", true, Modifier.fillMaxWidth())
                        val cv = ang("dConv")
                        if (cv == null) SpHint("Enter an angle or bearing.") else listOf(
                            "DMS" to SurveyCore.fmtDms(cv, 1), "Decimal degrees" to "${fixedStr(cv, 6)}°", "Azimuth" to SurveyCore.fmtDms(SurveyCore.norm360(cv)),
                            "Bearing" to SurveyCore.fmtBearing(cv), "Back azimuth" to SurveyCore.fmtDms(SurveyCore.norm360(cv + 180)),
                            "Supplement (180° −)" to SurveyCore.fmtDms(180 - cv), "Complement (90° −)" to SurveyCore.fmtDms(90 - cv)
                        ).forEach { (l, x) -> SurveyRowView(SRow(l, x)) }
                    }
                }
                "inv" -> {
                    SpCard { SpLabel("From point"); row({ field("iN1", "North", modifier = it) }, { field("iE1", "East", modifier = it) }, { field("iZ1", "Elev (opt)", modifier = it) }) }
                    SpCard { SpLabel("To point"); row({ field("iN2", "North", modifier = it) }, { field("iE2", "East", modifier = it) }, { field("iZ2", "Elev (opt)", modifier = it) }) }
                    SurveyResult(compute("inv"), saver("inv"))
                }
                "setup" -> {
                    SpCard {
                        SpLabel("Occupied station")
                        row({ field("sN", "North", modifier = it) }, { field("sE", "East", modifier = it) })
                        row({ field("sZ", "Elevation", modifier = it) }, { field("sHI", "Instrument ht (ft)", modifier = it) })
                    }
                    SpCard {
                        SpLabel("Backsight")
                        seg("bsBy", listOf("pt" to "Known point", "az" to "Known azimuth"))
                        Spacer(Modifier.height(8.dp))
                        if (v["bsBy"] == "pt") row({ field("bsN", "North", modifier = it) }, { field("bsE", "East", modifier = it) }, { field("bsZ", "Elev (opt)", modifier = it) })
                        else field("bsAz", "Backsight azimuth or bearing", true, Modifier.fillMaxWidth())
                        field("bsSet", "Gun reading set on backsight", true, Modifier.fillMaxWidth())
                        SpHint("Angles are read right (clockwise) from the backsight. Set 0°00'00\", or set the backsight azimuth so the gun reads azimuths.")
                    }
                    SurveyResult(compute("setup"), saver("setup"))
                }
                "bs" -> {
                    SpCard {
                        SpLabel("Shoot the backsight again"); SpHint(setupSummary())
                        Spacer(Modifier.height(8.dp))
                        field("bHA", "Gun reading on backsight", true, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp)); distFields("b")
                    }
                    SurveyResult(compute("bs"), saver("bs"))
                }
                "chk" -> {
                    SpCard { SpLabel("Known check point"); SpHint(setupSummary()); row({ field("cN", "North", modifier = it) }, { field("cE", "East", modifier = it) }, { field("cZ", "Elev (opt)", modifier = it) }) }
                    SpCard { SpLabel("What the gun measured"); field("cHA", "Gun reading on check point", true, Modifier.fillMaxWidth()); Spacer(Modifier.height(8.dp)); distFields("c") }
                    SurveyResult(compute("chk"), saver("chk"))
                }
                "res" -> {
                    val n = (v["rCount"]?.toIntOrNull() ?: 2).coerceIn(2, 5)
                    SpCard {
                        SpLabel("Resection")
                        seg("rMode", listOf("HD" to "Horizontal dist", "SD" to "Slope dist + zenith"))
                        Spacer(Modifier.height(8.dp))
                        row({ field("rHI", "Instrument ht", modifier = it) }, { field("rHR", "Rod ht", modifier = it) })
                        SpHint("2 points: direct solution with angle and baseline misclosure. 3–5 points: least-squares solution with residuals for every point.")
                    }
                    for (i in 1..n) SpCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) { SpLabel("Known point $i") }
                            if (i > 2 && i == n) TextButton(onClick = { set("rCount", (n - 1).toString()) }) { Text("✕ remove", color = c.danger) }
                        }
                        row({ field("rN$i", "North", modifier = it) }, { field("rE$i", "East", modifier = it) }, { field("rZ$i", "Elev (opt)", modifier = it) })
                        if (v["rMode"] == "HD") row({ field("rHA$i", "Gun reading", true, it) }, { field("rD$i", "HD (ft)", modifier = it) })
                        else row({ field("rHA$i", "Gun reading", true, it) }, { field("rSD$i", "SD (ft)", modifier = it) }, { field("rZA$i", "Zenith", true, it) })
                    }
                    if (n < 5) Button(onClick = { set("rCount", (n + 1).toString()) }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = c.eq, contentColor = c.eqInk)) { Text("＋ Add known point") }
                    SurveyResult(compute("res"), saver("res"))
                }
                else -> {
                    SpCard {
                        SpLabel("Tolerances")
                        row({ field("tolH", "Horizontal (ft)", modifier = it) }, { field("tolV", "Vertical (ft)", modifier = it) })
                        row({ field("tolA", "Angle (seconds)", modifier = it) }, { field("tolD", "Distance (ft)", modifier = it) })
                    }
                    SpCard {
                        SpLabel("Least-squares weighting (instrument std. dev.)")
                        row({ field("sdA", "Angle (seconds)", modifier = it) }, { field("sdD", "Distance (ft)", modifier = it) })
                        SpHint("Used by the 3–5 point resection. Use your gun's spec, e.g. 5\" and 0.010'.")
                    }
                    SpCard {
                        SpLabel("A single number in an angle box means")
                        seg("angMode", listOf("dec" to "Decimal degrees", "dmss" to "DDD.MMSS"))
                        SpHint("DDD.MMSS is the HP-style shorthand: 45.3015 = 45°30'15\". Symbols or spaces always read as D M S.")
                    }
                    SpCard {
                        OutlinedButton(onClick = { v.clear(); v.putAll(SURVEY_DEFAULTS); set("tab", "set") }, modifier = Modifier.fillMaxWidth()) { Text("Reset example values") }
                        SpHint("Inputs are saved on this device.")
                    }
                }
            }
        }
    }
}

@Composable
private fun SurveyResult(o: SOut?, onSave: ((SOut) -> Unit)? = null) {
    if (o == null) return
    val c = AppTheme.colors
    if (o.errors.isNotEmpty()) { SpCard { Text(o.errors.joinToString("\n"), color = c.danger) }; return }
    Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(2.dp, c.accent), backgroundColor = c.surface, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Column(Modifier.padding(14.dp)) {
            o.verdict?.let { (ok, text) ->
                val fg = if (ok) Color(0xFF15803D) else Color(0xFFB3261E)
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (ok) Color(0xFFECFDF3) else Color(0xFFF9DEDC)).padding(10.dp), contentAlignment = Alignment.Center) {
                    Text(text, color = fg, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(10.dp))
            }
            if (o.title.isNotEmpty()) Text(o.title.uppercase(), color = c.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            if (o.big.isNotEmpty()) Text(o.big, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = c.ink)
            if (o.sub.isNotEmpty()) Text(o.sub, fontSize = 14.sp, color = c.muted)
            o.rows.forEach { SurveyRowView(it) }
            o.warn?.let { Text(it, color = Color(0xFFB3261E), modifier = Modifier.padding(top = 6.dp)) }
            o.hints.forEach { SpHint(it) }
            onSave?.let { save ->
                var saved by remember(o.big, o.rows.size) { mutableStateOf(false) }
                OutlinedButton(onClick = { save(o); saved = true }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Text(if (saved) "✓ Saved to Data Sheet" else "Save to Data Sheet")
                }
            }
            o.action?.let { (label, act) ->
                Button(onClick = act, modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = c.eq, contentColor = c.eqInk)) { Text(label) }
            }
        }
    }
}

@Composable
private fun SurveyRowView(r: SRow) {
    val c = AppTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(r.label, color = c.ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(r.value, color = c.ink, fontWeight = FontWeight.Bold, fontSize = if (r.big) 22.sp else 15.sp, textAlign = TextAlign.End)
        r.ok?.let { ok ->
            val fg = if (ok) Color(0xFF15803D) else Color(0xFFB3261E)
            Box(Modifier.padding(start = 6.dp).clip(RoundedCornerShape(10.dp)).background(if (ok) Color(0xFFECFDF3) else Color(0xFFF9DEDC))
                .border(1.dp, fg, RoundedCornerShape(10.dp)).padding(horizontal = 7.dp, vertical = 2.dp)) {
                Text(if (ok) "PASS" else "FAIL", color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
