package com.djran.constructioncalculator

import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.hypot
import kotlin.math.PI

/**
 * Jack rafters for a hip roof, regular or irregular (two pitches).
 * Pure Kotlin, no Android, so it can move to ProCalcMultiplatform as-is.
 *
 * All lengths in inches. "Main" = the long walls (main pitch); "end" = the end walls.
 * Jack lengths are along the top edge, from the outside of the wall (plate line)
 * to the long point of the cheek cut at the hip.
 */
object JackRafterMath {

    enum class MarkTo { CENTER, NEAR_EDGE }

    data class Input(
        val span: Double,            // outside of wall to outside of wall
        val ridge: Double,           // ridge thickness
        val mainPitch: Double,       // rise per 12, long walls
        val heel: Double,            // heel height on the main walls
        val overhang: Double,        // level overhang, same on every wall
        val jackThk: Double = 1.5,
        val hipThk: Double = 1.5,
        val oc: Double = 16.0,
        val firstMark: Double = 16.0, // first plate mark, from the outside corner
        val corners: Int = 4,
        val irregular: Boolean = false,
        val endPitch: Double = mainPitch,
        val markTo: MarkTo = MarkTo.CENTER,
        val takeDeduction: Boolean = true,
        val mated: Boolean = false    // irregular only: end-wall jacks meet the main jacks at the hip
    )

    data class Jack(
        val n: Int,
        val mark: Double,      // plate mark from the corner (as the user lays out)
        val center: Double,    // jack centerline from the corner
        val run: Double,       // level run, wall line to the hip centerline
        val length: Double,    // to the long point, plate line
        val overall: Double    // with the tail
    )

    data class Side(
        val pitch: Double,
        val spacing: Double,
        val commonDiff: Double,
        val cheekBevelDeg: Double,
        val deduction: Double,  // level, along the jack
        val jacks: List<Jack>
    )

    data class Result(
        val mainRun: Double,
        val endRun: Double,
        val endHeel: Double,
        val hipOffMain: Double,    // hip crosses the main plate line this far in from the corner
        val hipOutsideEnd: Double, // and the end plate line this far outside the corner
        val hipRunPlan: Double,    // plan, main plate crossing to ridge
        val hipLength: Double,     // centerline, main plate crossing to ridge
        val hipOverall: Double,    // centerline, fascia corner to ridge
        val hipPitch: Double,
        val hipAngleDeg: Double,   // off the main wall; 45 when regular
        val mainCommon: Double,    // common on the long walls, plate line to ridge
        val mainCommonAll: Double, // with the tail
        val kingCommon: Double,    // common on the end walls (king common), plate line to ridge end
        val kingCommonAll: Double, // with the tail
        val main: Side,
        val end: Side,
        val qtyEach: Int,          // per jack size, per side list
        val totalJacks: Int,
        val errors: List<String>
    ) {
        val ok get() = errors.isEmpty()
    }

    private fun deg(r: Double) = r * 180.0 / PI

    fun compute(i: Input): Result {
        val err = mutableListOf<String>()
        if (i.span <= 0) err += "Enter a span."
        if (i.mainPitch <= 0) err += "Enter the main pitch."
        if (i.irregular && i.endPitch <= 0) err += "Enter the end-wall pitch."
        if (i.oc <= 0) err += "Enter the on-center spacing."
        val pm = i.mainPitch
        val pe = if (i.irregular) i.endPitch else pm
        val rm = i.span / 2 - i.ridge / 2
        if (rm <= 0) err += "Ridge is wider than the span."
        // End-wall heel so both fascias land at the same height with the same overhang
        val he = if (i.irregular) i.heel - i.overhang * (pm - pe) / 12.0 else i.heel
        if (i.irregular && he <= 0) err += "End-wall heel works out below zero. Raise the main heel or shorten the overhang."
        val empty = Side(pe, i.oc, 0.0, 45.0, 0.0, emptyList())
        if (err.isNotEmpty()) return Result(0.0, 0.0, he, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 45.0, 0.0, 0.0, 0.0, 0.0, empty, empty, 0, 0, err)

        val top = i.heel + rm * pm / 12.0          // ridge height above the main plate
        val re = if (i.irregular) (top - he) / (pe / 12.0) else rm
        // The two planes meet at the fascia corner (fascias match), so in plan the hip runs at
        // tan(theta) = pe/pm off the main wall, from (-o,-o) to the ridge end (re, rm). It crosses
        // the main plate line x0 in from the corner and the end plate line y1 outside it.
        val tanT = pe / pm
        val theta = atan(tanT)
        val x0 = (i.heel - he) * 12.0 / pe
        val y1 = (i.heel - he) * 12.0 / pm
        val hipPitch = pm * pe / hypot(pm, pe)
        val hipPlan = hypot(re - x0, rm)             // main plate crossing to ridge
        val hipLen = hypot(hipPlan, rm * pm / 12.0)
        val hipPlanAll = hypot(re + i.overhang, rm + i.overhang)
        val hipOverall = hypot(hipPlanAll, top - (i.heel - i.overhang * pm / 12.0))
        val sfm = hypot(1.0, pm / 12.0)
        val sfe = hypot(1.0, pe / 12.0)

        val dedM = if (i.takeDeduction) (i.hipThk / 2) / cos(theta) else 0.0
        val dedE = if (i.takeDeduction) (i.hipThk / 2) / sin(theta) else 0.0
        val lpM = (i.jackThk / 2) * tanT             // long point past the jack's centerline
        val lpE = (i.jackThk / 2) / tanT
        fun ctr(m: Double) = if (i.markTo == MarkTo.NEAR_EDGE) m + i.jackThk / 2 else m
        fun mark(c: Double) = if (i.markTo == MarkTo.NEAR_EDGE) c - i.jackThk / 2 else c

        val mainJacks = mutableListOf<Jack>()
        var k = 0
        while (k < 500) {
            val m = i.firstMark + k * i.oc
            val c = ctr(m)
            if (c >= re - 0.01) break
            val run = (c - x0) * tanT
            val lvl = run - dedM + lpM
            if (lvl > 0) mainJacks += Jack(mainJacks.size + 1, m, c, run, lvl * sfm, (lvl + i.overhang) * sfm)
            k++
        }

        val mated = i.irregular && i.mated
        val sE = if (mated) i.oc * tanT else i.oc
        val endJacks = mutableListOf<Jack>()
        k = 0
        while (k < 500) {
            val c = if (mated) (ctr(i.firstMark + k * i.oc) - x0) * tanT else ctr(i.firstMark + k * i.oc)
            if (c >= rm - 0.01) break
            if (c > 0) {
                val run = x0 + c / tanT
                val lvl = run - dedE + lpE
                if (lvl > 0) endJacks += Jack(endJacks.size + 1, mark(c), c, run, lvl * sfe, (lvl + i.overhang) * sfe)
            }
            k++
        }

        val main = Side(pm, i.oc, i.oc * tanT * sfm, deg(theta), dedM, mainJacks)
        val end = Side(pe, sE, sE / tanT * sfe, 90.0 - deg(theta), dedE, endJacks)
        val qty = if (i.irregular) i.corners else 2 * i.corners
        val total = if (i.irregular) (mainJacks.size + endJacks.size) * i.corners else mainJacks.size * 2 * i.corners
        return Result(rm, re, he, x0, y1, hipPlan, hipLen, hipOverall, hipPitch, deg(theta),
            rm * sfm, (rm + i.overhang) * sfm, re * sfe, (re + i.overhang) * sfe, main, end, qty, total, emptyList())
    }

    /** Flat top (deck hip). The "ridge" on Rafter Cuts is the deck width to the faces the rafters bear on. */
    data class Deck(
        val width: Double,       // to the rafter bearing faces (outside of the long ledgers)
        val length: Double,      // building length - 2 end runs, outside of the end ledgers
        val height: Double,      // top of the rafters at the deck edge, above the plate
        val joistLength: Double, // between the two long ledgers
        val joistCount: Int,
        val joistOc: Double,
        val errors: List<String>
    ) { val ok get() = errors.isEmpty() }

    fun deck(i: Input, r: Result, buildingLength: Double, ledgerThk: Double = 1.5, joistOc: Double = 16.0, joistThk: Double = 1.5): Deck {
        val err = mutableListOf<String>()
        if (!r.ok) err += "Fix the roof first."
        if (buildingLength <= 0) err += "Enter the building length (outside of the end walls)."
        if (i.ridge <= 2 * ledgerThk) err += "Deck width must be wider than the two ledgers."
        if (joistOc <= 0) err += "Enter the joist spacing."
        val len = buildingLength - 2 * r.endRun
        if (err.isEmpty() && len <= 2 * ledgerThk) err += "Building is too short for a flat top with these runs."
        if (err.isNotEmpty()) return Deck(i.ridge, len, 0.0, 0.0, 0, joistOc, err)
        val inside = len - 2 * ledgerThk
        val count = kotlin.math.ceil((inside - joistThk) / joistOc - 1e-9).toInt() + 1
        return Deck(i.ridge, len, i.heel + r.mainRun * i.mainPitch / 12.0, i.ridge - 2 * ledgerThk, count, joistOc, emptyList())
    }

    /** Everything needed to rebuild the jack results later (Data Sheet, PDF, edit). */
    data class Saved(
        val input: Input,
        val longestFirst: Boolean = true,
        val flatTop: Boolean = false,
        val bldgLen: Double = 0.0,
        val ledgerThk: Double = 1.5,
        val joistOc: Double = 16.0,
        val hipLabel: String = "2x"
    )

    const val TAG = "JackData:"

    fun encode(s: Saved): String {
        val i = s.input
        val kv = listOf(
            "span" to i.span, "ridge" to i.ridge, "pm" to i.mainPitch, "heel" to i.heel, "o" to i.overhang,
            "jt" to i.jackThk, "ht" to i.hipThk, "oc" to i.oc, "first" to i.firstMark, "corners" to i.corners,
            "irr" to i.irregular, "pe" to i.endPitch, "edge" to (i.markTo == MarkTo.NEAR_EDGE),
            "ded" to i.takeDeduction, "mated" to i.mated, "desc" to s.longestFirst, "flat" to s.flatTop,
            "bl" to s.bldgLen, "lt" to s.ledgerThk, "joc" to s.joistOc, "hip" to s.hipLabel.replace(";", ",").replace("=", "-")
        )
        return TAG + kv.joinToString(";") { "${it.first}=${it.second}" }
    }

    fun decode(line: String): Saved? {
        if (!line.startsWith(TAG)) return null
        val m = line.removePrefix(TAG).split(";").mapNotNull { p ->
            val k = p.substringBefore("=", ""); if (k.isEmpty()) null else k to p.substringAfter("=")
        }.toMap()
        fun d(k: String) = m[k]?.toDoubleOrNull()
        fun b(k: String) = m[k] == "true"
        val span = d("span") ?: return null
        val pm = d("pm") ?: return null
        val i = Input(
            span = span, ridge = d("ridge") ?: 1.5, mainPitch = pm, heel = d("heel") ?: 4.0, overhang = d("o") ?: 12.0,
            jackThk = d("jt") ?: 1.5, hipThk = d("ht") ?: 1.5, oc = d("oc") ?: 16.0, firstMark = d("first") ?: 16.0,
            corners = m["corners"]?.toIntOrNull() ?: 4, irregular = b("irr"), endPitch = d("pe") ?: pm,
            markTo = if (b("edge")) MarkTo.NEAR_EDGE else MarkTo.CENTER, takeDeduction = m["ded"] != "false", mated = b("mated")
        )
        return Saved(i, m["desc"] != "false", b("flat"), d("bl") ?: 0.0, d("lt") ?: 1.5, d("joc") ?: 16.0, m["hip"] ?: "2x")
    }

    fun plumbDeg(pitch: Double) = deg(atan(pitch / 12.0))
}
