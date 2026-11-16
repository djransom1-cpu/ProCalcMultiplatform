package com.djran.constructioncalculator

import kotlin.math.*

/**
 * All circular-stair math in one place. The calculator screen, the Data Sheet, the PDF and the
 * DXF all use this, so they always show the same numbers.
 *
 * Limits are the 2015 Michigan Residential Code (R 408.30519): 8-1/4" max riser, 9" min tread,
 * and - since the MRC has no circular-stair section - the winder rule for curved treads: 10" at the
 * walk line 12" from the narrow side, 6" at any point. Plus Dan's shop range: 6-1/4" min rise,
 * 7-1/2" best. Check the code your local inspector has adopted.
 */
object CircStairMath {

    const val MAX_RISE = 8.25              // Michigan R311.7.5.1 (IRC is 7-3/4")
    const val MIN_RISE = 6.25              // shop rule
    const val IDEAL_RISE = 7.5             // shop rule: best if possible
    const val MIN_WALKLINE_TREAD = 10.0    // MRC R311.7.4.2 / R311.7.5.2.1 winders, 12" from the narrow side
    const val BEST_WALKLINE_TREAD = 11.0   // 11"+ needs no nosing (R311.7.5.3 exception)
    const val MIN_NARROW_TREAD = 6.0       // MRC winders, any point on the tread
    const val WALKLINE_OFFSET = 12.0       // MRC R311.7.4 walkline
    const val MIN_STRAIGHT_TREAD = 9.0     // Michigan R311.7.5.2 (IRC is 10")
    const val MAX_TREAD_VARIATION = 0.375  // R311.7.5.2, 3/8" within a flight
    const val MIN_WIDTH = 36.0             // R311.7.1
    const val MIN_HEADROOM = 80.0          // R311.7.2, 6'8"
    const val NOSING_MIN = 0.75            // R311.7.5.3
    const val NOSING_MAX = 1.25            // R311.7.5.3
    const val MAX_BALUSTER_GAP = 4.0       // R312.1.3, 4" sphere
    const val BALUSTER_SIZE = 1.25         // assumed baluster width for the counts
    const val TREAD_THICKNESS = 1.0        // assumed finished tread thickness for the headroom check

    enum class Level { OK, WARN, FAIL }

    private fun deg(r: Double) = r * 180.0 / PI
    fun fx(x: Double, d: Int) = StoryPoleMath.fixed(x, d)

    data class Check(val level: Level, val label: String, val detail: String)
    data class Material(val item: String, val qty: Int, val size: String)

    data class Result(
        val totalRise: Double,
        val innerRadius: Double,
        val outerRadius: Double,
        val treadWidth: Double,
        val totalTreads: Int,
        val straightTreads: Int,
        val wedgeTreads: Int,
        val straightRun: Double,
        val nosing: Double,
        val risers: Int,
        val stepRise: Double,
        val walkRadius: Double,
        val walkRun: Double,
        val innerRun: Double,
        val outerRun: Double,
        val innerChord: Double,
        val outerChord: Double,
        val anglePerTreadDeg: Double,
        val sweepDeg: Double,
        val innerArcTotal: Double,
        val outerArcTotal: Double,
        val totalWalkRun: Double,
        val innerPitchDeg: Double,
        val outerPitchDeg: Double,
        val footprintX: Double,
        val footprintY: Double,
        val spaceLength: Double,
        val spaceWidth: Double,
        val fits: Boolean?,
        val minTreadsForRise: Int,
        val maxTreadsForRise: Int,
        val bestTreads: Int,
        val bestRise: Double,
        val checks: List<Check>,
        val materials: List<Material>
    ) {
        val allOk: Boolean get() = checks.none { it.level == Level.FAIL }
    }

    /** Reads a saved "circle size": new saves hold degrees ("227.6°"), old saves a fraction ("2/4"). */
    fun parseSweepDegrees(circleSize: String?): Double? {
        val s = circleSize?.trim().orEmpty()
        if (s.isEmpty()) return null
        if (s.contains("/")) {
            val p = s.split("/")
            val n = p.getOrNull(0)?.trim()?.toDoubleOrNull() ?: return null
            val d = p.getOrNull(1)?.trim()?.toDoubleOrNull() ?: return null
            return if (d == 0.0) null else 360.0 * n / d
        }
        return s.replace("°", "").trim().toDoubleOrNull()
    }

    /** Shared app's entry: [CircularStairEntry.walkTread] holds the tread WIDTH, [CircularStairEntry.walkLineTread] the walk-line depth. */
    fun fromEntry(e: CircularStairEntry): Result? = compute(
        totalRise = FractionUtils.parse(e.totalRise),
        innerRadius = FractionUtils.parse(e.radius),
        treadWidth = FractionUtils.parse(e.walkTread),
        totalTreads = e.numTreads.trim().toDoubleOrNull()?.toInt() ?: 0,
        straightTreads = e.straightTreads.trim().toDoubleOrNull()?.toInt() ?: 0,
        straightRun = FractionUtils.parse(e.straightRun),
        nosing = FractionUtils.parse(e.nosing),
        walkTread = FractionUtils.parse(e.walkLineTread),
        fallbackSweepDeg = parseSweepDegrees(e.circleSize),
        spaceLength = FractionUtils.parse(e.spaceLength),
        spaceWidth = FractionUtils.parse(e.spaceWidth)
    )

    /**
     * @param walkTread tread depth wanted on the walk line (12" out from the narrow side). The
     * sweep of the circle comes from this. If it is 0, [fallbackSweepDeg] is used instead
     * (old saves that stored a circle fraction).
     */
    fun compute(
        totalRise: Double,
        innerRadius: Double,
        treadWidth: Double,
        totalTreads: Int,
        straightTreads: Int,
        straightRun: Double,
        nosing: Double,
        walkTread: Double,
        fallbackSweepDeg: Double? = null,
        spaceLength: Double = 0.0,
        spaceWidth: Double = 0.0
    ): Result? {
        if (innerRadius <= 0 || treadWidth <= 0 || totalTreads <= 0) return null

        val straight = straightTreads.coerceIn(0, totalTreads - 1)
        val wedge = totalTreads - straight
        val risers = totalTreads + 1
        val stepRise = if (totalRise > 0) totalRise / risers else 0.0
        val outerR = innerRadius + treadWidth
        val walkR = innerRadius + min(WALKLINE_OFFSET, treadWidth)

        val theta = when {
            walkTread > 0 -> walkTread / walkR
            fallbackSweepDeg != null && fallbackSweepDeg > 0 -> (fallbackSweepDeg * PI / 180.0) / wedge
            else -> BEST_WALKLINE_TREAD / walkR
        }
        val walkRun = walkR * theta
        val innerRun = innerRadius * theta
        val outerRun = outerR * theta
        val innerChord = 2 * innerRadius * sin(theta / 2)
        val outerChord = 2 * outerR * sin(theta / 2)
        val sweepDeg = deg(theta * wedge)
        val innerArcTotal = innerRun * wedge
        val outerArcTotal = outerRun * wedge
        val straightLen = straight * straightRun

        val innerPitch = if (innerRun > 0) deg(atan(stepRise / innerRun)) else 0.0
        val outerPitch = if (outerRun > 0) deg(atan(stepRise / outerRun)) else 0.0

        // ---- Plan footprint (same layout as the drawing: straight run on the left, curve going up) ----
        val fp = footprint(innerRadius, outerR, sweepDeg, straightLen)
        val fits: Boolean? = if (spaceLength > 0 && spaceWidth > 0) {
            (fp.first <= spaceLength && fp.second <= spaceWidth) || (fp.first <= spaceWidth && fp.second <= spaceLength)
        } else null

        // Tread counts that keep the rise in range, and the one closest to the ideal rise.
        val minTreads = if (totalRise > 0) max(1, ceil(totalRise / MAX_RISE - 1e-9).toInt() - 1) else 0
        val maxTreads = if (totalRise > 0) max(minTreads, floor(totalRise / MIN_RISE + 1e-9).toInt() - 1) else 0
        var bestTreads = 0
        var bestRise = 0.0
        if (totalRise > 0) {
            bestTreads = (minTreads..maxTreads).minByOrNull { abs(totalRise / (it + 1) - IDEAL_RISE) } ?: minTreads
            bestRise = totalRise / (bestTreads + 1)
        }

        // ---- Code checks ----
        val checks = mutableListOf<Check>()
        val f: (Double) -> String = { FractionUtils.formatInches(it) }
        if (totalRise > 0) {
            val best = "Best: $bestTreads treads at ${f(bestRise)}"
            checks += when {
                stepRise > MAX_RISE + 1e-6 ->
                    Check(Level.FAIL, "Step rise", "${f(stepRise)} is over 8 1/4\". Use at least $minTreads treads. $best")
                stepRise < MIN_RISE - 1e-6 ->
                    Check(Level.FAIL, "Step rise", "${f(stepRise)} is under 6 1/4\". Use no more than $maxTreads treads. $best")
                totalTreads == bestTreads ->
                    Check(Level.OK, "Step rise", "${f(stepRise)} - closest to 7 1/2\" (range 6 1/4\" to 8 1/4\")")
                else ->
                    Check(Level.OK, "Step rise", "${f(stepRise)} is in range (6 1/4\" to 8 1/4\"). $best")
            }
        } else {
            checks += Check(Level.WARN, "Step rise", "Enter the total rise to get the step rise.")
        }
        checks += if (walkRun >= MIN_WALKLINE_TREAD - 1e-6)
            Check(Level.OK, "Walk-line tread", "${f(walkRun)} at 12\" from the narrow side (min 10\")")
        else
            Check(Level.FAIL, "Walk-line tread", "${f(walkRun)} at 12\" from the narrow side is under 10\"")
        checks += if (innerRun >= MIN_NARROW_TREAD - 1e-6)
            Check(Level.OK, "Narrow end", "${f(innerRun)} (min 6\")")
        else
            Check(Level.FAIL, "Narrow end", "${f(innerRun)} is under 6\". Make the inner radius bigger.")
        checks += if (treadWidth >= MIN_WIDTH - 1e-6)
            Check(Level.OK, "Stair width", "${f(treadWidth)} (min 36\" clear)")
        else
            Check(Level.FAIL, "Stair width", "${f(treadWidth)} is under 36\" clear width")
        if (nosing > NOSING_MAX + 1e-6) {
            checks += Check(Level.FAIL, "Nosing", "${f(nosing)} is over 1 1/4\"")
        } else if (walkRun < BEST_WALKLINE_TREAD && nosing < NOSING_MIN - 1e-6) {
            checks += Check(Level.FAIL, "Nosing", "Needs 3/4\" to 1 1/4\" when the tread is under 11\"")
        } else {
            checks += Check(Level.OK, "Nosing", if (nosing > 0) f(nosing) else "None (OK with 11\"+ treads)")
        }
        if (straight > 0) {
            checks += if (straightRun >= MIN_STRAIGHT_TREAD - 1e-6)
                Check(Level.OK, "Straight treads", "${f(straightRun)} run nosing-to-nosing, ${f(straightRun + nosing)} board with nosing (min 9\" run)")
            else
                Check(Level.FAIL, "Straight treads", "${f(straightRun)} run is under 9\"")
            // MRC R311.7.5.2.1: consistently shaped winders may differ from the rectangular treads.
        }
        if (sweepDeg > 360.0) {
            val risePerTurn = stepRise * (360.0 / deg(theta))
            val clear = risePerTurn - TREAD_THICKNESS
            checks += if (clear >= MIN_HEADROOM)
                Check(Level.OK, "Headroom", "Stair passes over itself; ${f(clear)} clear under the tread above (min 6' 8\", less any framing)")
            else
                Check(Level.FAIL, "Headroom", "Stair passes over itself with only ${f(clear)} clear (min 6' 8\")")
        }
        when (fits) {
            true -> checks += Check(Level.OK, "Fits space", "Needs ${f(fp.first)} x ${f(fp.second)}")
            false -> checks += Check(Level.FAIL, "Fits space",
                "Needs ${f(fp.first)} x ${f(fp.second)}; space is ${f(spaceLength)} x ${f(spaceWidth)}")
            null -> {}
        }

        // ---- Material list ----
        val mats = mutableListOf<Material>()
        // Wedge tread blank: front edge on a radius, nosing in front of it.
        val blankLen = outerR - innerRadius * cos(theta)
        val blankWid = outerR * sin(theta) + nosing
        mats += Material("Curved (wedge) treads", wedge,
            "Blank ${f(blankLen)} x ${f(blankWid)}; wide end ${f(outerChord + nosing)}, narrow end ${f(innerChord + nosing)}")
        if (straight > 0) mats += Material("Straight treads", straight, "${f(treadWidth)} x ${f(straightRun + nosing)}")
        mats += Material("Risers", risers, "${f(treadWidth)} x ${f(stepRise)} (check top & bottom for finish floor)")

        val outerHoriz = straightLen + outerArcTotal
        val innerHoriz = straightLen + innerArcTotal
        if (straight > 0) {
            mats += Material("Straight stringers", 2, "${f(hypot(straightLen, straight * stepRise))} on the pitch line, plus trim")
        }
        mats += Material("Outer curved stringer", 1,
            "${f(hypot(outerArcTotal, (wedge + 1) * stepRise))} developed on the pitch line, pitch ${fx(outerPitch, 1)}°")
        mats += Material("Inner curved stringer", 1,
            "${f(hypot(innerArcTotal, (wedge + 1) * stepRise))} developed on the pitch line, pitch ${fx(innerPitch, 1)}°")

        val railRise = totalTreads * stepRise
        mats += Material("Outer handrail", 1, "${f(hypot(outerHoriz, railRise))} along the curve")
        mats += Material("Inner handrail", 1, "${f(hypot(innerHoriz, railRise))} along the curve")

        val maxCC = MAX_BALUSTER_GAP + BALUSTER_SIZE
        val outerBal = wedge * max(1, ceil(outerRun / maxCC).toInt()) + straight * max(1, ceil(straightRun / maxCC).toInt())
        val innerBal = wedge * max(1, ceil(innerRun / maxCC).toInt()) + straight * max(1, ceil(straightRun / maxCC).toInt())
        mats += Material("Balusters, outer side", outerBal, "${max(1, ceil(outerRun / maxCC).toInt())} per curved tread (1 1/4\" balusters, 4\" sphere)")
        mats += Material("Balusters, inner side", innerBal, "${max(1, ceil(innerRun / maxCC).toInt())} per curved tread (skip if wall side)")

        return Result(
            totalRise, innerRadius, outerR, treadWidth, totalTreads, straight, wedge, straightRun, nosing,
            risers, stepRise, walkR, walkRun, innerRun, outerRun, innerChord, outerChord,
            deg(theta), sweepDeg, innerArcTotal, outerArcTotal,
            straightLen + walkR * theta * wedge, innerPitch, outerPitch,
            fp.first, fp.second, spaceLength, spaceWidth, fits, minTreads, maxTreads, bestTreads, bestRise, checks, mats
        )
    }

    /** Plan size (x, y) of the stair: straight run going left from the bottom of the circle, curve going up. */
    fun footprint(innerR: Double, outerR: Double, sweepDeg: Double, straightLen: Double): Pair<Double, Double> {
        if (sweepDeg >= 360.0) {
            return Pair(2 * outerR + straightLen, 2 * outerR)
        }
        var minX = 0.0; var maxX = 0.0; var minY = innerR; var maxY = outerR
        if (straightLen > 0) minX = -straightLen
        val steps = max(2, ceil(sweepDeg).toInt())
        for (i in 0..steps) {
            val a = ((90.0 - sweepDeg * i / steps) * PI / 180.0)
            for (r in doubleArrayOf(innerR, outerR)) {
                val x = r * cos(a); val y = r * sin(a)
                minX = min(minX, x); maxX = max(maxX, x); minY = min(minY, y); maxY = max(maxY, y)
            }
        }
        return Pair(maxX - minX, maxY - minY)
    }

    /** Plain-text report used on the Data Sheet and in the PDF. */
    fun reportLines(r: Result): List<String> {
        val f: (Double) -> String = { FractionUtils.formatInches(it) }
        val lines = mutableListOf<String>()
        lines += "Total Rise: ${f(r.totalRise)} | Risers: ${r.risers} | Step Rise: ${f(r.stepRise)}" +
            (if (r.bestTreads > 0 && r.bestTreads != r.totalTreads) " | Best: ${r.bestTreads} treads @ ${f(r.bestRise)}" else "")
        lines += "Treads: ${r.totalTreads} (${r.wedgeTreads} curved, ${r.straightTreads} straight) | Width: ${f(r.treadWidth)}"
        lines += "Inner R: ${f(r.innerRadius)} | Outer R: ${f(r.outerRadius)} | Walk-line R: ${f(r.walkRadius)}"
        lines += "Tread run - walk line: ${f(r.walkRun)} | inner: ${f(r.innerRun)} | outer: ${f(r.outerRun)}"
        lines += "Riser lines on chord - inner: ${f(r.innerChord)} | outer: ${f(r.outerChord)}"
        lines += "Angle per tread: ${fx(r.anglePerTreadDeg, 2)}° | Sweep: ${fx(r.sweepDeg, 1)}° | Arc - inner: ${f(r.innerArcTotal)}, outer: ${f(r.outerArcTotal)}"
        lines += "Plan footprint: ${f(r.footprintX)} x ${f(r.footprintY)}" +
            (if (r.fits != null) " | Space: ${f(r.spaceLength)} x ${f(r.spaceWidth)} -> ${if (r.fits) "FITS" else "DOES NOT FIT"}" else "")
        lines += "CODE CHECK:"
        r.checks.forEach { lines += "  ${tag(it.level)} ${it.label}: ${it.detail}" }
        lines += "MATERIAL LIST:"
        r.materials.forEach { lines += "  ${it.qty} x ${it.item} - ${it.size}" }
        return lines
    }

    fun tag(l: Level) = when (l) { Level.OK -> "[OK]"; Level.WARN -> "[CHECK]"; Level.FAIL -> "[FAIL]" }
}
