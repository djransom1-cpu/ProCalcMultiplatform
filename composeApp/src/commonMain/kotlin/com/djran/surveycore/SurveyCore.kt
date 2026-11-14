package com.djran.surveycore

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Survey Calc core — shared by ProCalc and SurveyorDataCollector. No Android imports.
 * Angles: decimal degrees. Distances/coordinates: decimal feet. Angles read right (clockwise).
 * Azimuth uses x = East, y = North: az = atan2(ΔE, ΔN).
 */
object SurveyCore {

    const val D2R = PI / 180.0
    private fun toDeg(r: Double) = r * 180.0 / PI

    /** Fixed-decimal text without String.format, so this file also compiles for the web build. */
    fun fixed(x: Double, d: Int): String {
        val p = 10.0.pow(d).toLong()
        val r = (abs(x) * p).roundToLong()
        val body = if (d > 0) "${r / p}." + (r % p).toString().padStart(d, '0') else "${r / p}"
        return if (x < 0 && r != 0L) "-$body" else body
    }
    private fun pad2(n: Long) = n.toString().padStart(2, '0')

    fun norm360(a: Double): Double = ((a % 360.0) + 360.0) % 360.0
    fun norm180(a: Double): Double { val x = norm360(a); return if (x > 180.0) x - 360.0 else x }

    // ================= parsing =================

    enum class AngleMode { DECIMAL, DMSS }   // what a single number in an angle box means

    private val NUM = Regex("""^\d*\.?\d+$""")
    private val SIGNED_NUM = Regex("""^[+-]?\d*\.?\d+$""")
    private val BEARING = Regex("""^([NS])\s*(.*?)\s*([EW])$""")

    fun parseNum(s: String?): Double? {
        val t = (s ?: "").trim()
        return if (SIGNED_NUM.matches(t)) t.toDouble() else null
    }

    private fun parseDmsCore(raw: String, mode: AngleMode): Double? {
        var s = raw.replace('’', '\'').replace('‘', '\'').replace('′', '\'').replace('`', '\'')
            .replace('“', '"').replace('”', '"').replace('″', '"').replace('º', '°').trim()
        var sign = 1.0
        if (s.startsWith("-")) { sign = -1.0; s = s.substring(1).trim() } else if (s.startsWith("+")) s = s.substring(1).trim()
        if (s.isEmpty()) return null
        val hasSym = Regex("""[°'"dmsDMS]""").containsMatchIn(s) || Regex("""\s|-""").containsMatchIn(s)
        if (!hasSym) {
            if (!NUM.matches(s)) return null
            if (mode == AngleMode.DMSS) {
                val ip = s.substringBefore('.').ifEmpty { "0" }
                val fp = if (s.contains('.')) s.substringAfter('.') else ""
                val f = (fp + "0000").substring(0, 4)
                val rest = if (fp.length > 4) fp.substring(4) else ""
                val m = f.substring(0, 2).toInt()
                val sec = (f.substring(2, 4) + if (rest.isNotEmpty()) ".$rest" else "").toDouble()
                if (m >= 60 || sec >= 60) return null
                return sign * (ip.toDouble() + m / 60.0 + sec / 3600.0)
            }
            return sign * s.toDouble()
        }
        val parts = s.replace(Regex("""[°dD]"""), " ").replace(Regex("""['mM]"""), " ").replace(Regex("""["sS]"""), " ")
            .replace("-", " ").trim().split(Regex("""\s+"""))
        if (parts.size > 3 || parts.any { !NUM.matches(it) }) return null
        val d = parts[0].toDouble()
        val m = parts.getOrNull(1)?.toDouble() ?: 0.0
        val sec = parts.getOrNull(2)?.toDouble() ?: 0.0
        if (m >= 60 || sec >= 60) return null
        return sign * (d + m / 60.0 + sec / 3600.0)
    }

    /** Accepts DMS (45°30'15", 45 30 15, 45-30-15), decimal, or a bearing (N 45 30 E → azimuth). */
    fun parseAngle(raw: String?, mode: AngleMode = AngleMode.DECIMAL): Double? {
        val s = (raw ?: "").trim()
        if (s.isEmpty()) return null
        BEARING.matchEntire(s.uppercase())?.let { b ->
            val q = parseDmsCore(b.groupValues[2], mode) ?: return null
            if (q < 0 || q > 90) return null
            val ns = b.groupValues[1]; val ew = b.groupValues[3]
            return when {
                ns == "N" && ew == "E" -> q
                ns == "S" && ew == "E" -> 180 - q
                ns == "S" && ew == "W" -> 180 + q
                else -> norm360(360 - q)
            }
        }
        return parseDmsCore(s, mode)
    }

    // ================= formatting =================

    /** 129°48'20"  or with [secDecimals]=1: 36°52'11.6" */
    fun fmtDms(a: Double?, secDecimals: Int = 0): String {
        if (a == null || !a.isFinite()) return "—"
        val neg = a < 0
        val p = 10.0.pow(secDecimals)
        var t = (abs(a) * 3600 * p).roundToLong() / p
        var d = floor(t / 3600).toLong(); t -= d * 3600
        var m = floor(t / 60).toLong()
        var s = ((t - m * 60) * p).roundToLong() / p
        if (s >= 60) { s -= 60; m++ }
        if (m >= 60) { m -= 60; d++ }
        val ss = if (secDecimals > 0) fixed(s, secDecimals).padStart(3 + secDecimals, '0') else pad2(s.roundToLong())
        return (if (neg) "-" else "") + "$d°" + pad2(m) + "'" + ss + "\""
    }

    fun fmtBearing(az: Double?): String {
        if (az == null || !az.isFinite()) return "—"
        val a = norm360(az)
        val (ns, ew, q) = when {
            a <= 90 -> Triple("N", "E", a)
            a <= 180 -> Triple("S", "E", 180 - a)
            a <= 270 -> Triple("S", "W", a - 180)
            else -> Triple("N", "W", 360 - a)
        }
        return "$ns ${fmtDms(q)} $ew"
    }

    /** +3.6" */
    fun fmtSecErr(a: Double?): String =
        if (a == null || !a.isFinite()) "—" else (if (a >= 0) "+" else "−") + fixed(abs(a * 3600), 1) + "\""

    fun fmtFt(x: Double?, d: Int = 3): String = if (x == null || !x.isFinite()) "—" else fixed(x, d) + "'"
    fun fmtSignedFt(x: Double?, d: Int = 3): String =
        if (x == null || !x.isFinite()) "—" else (if (x >= 0) "+" else "−") + fixed(abs(x), d) + "'"
    fun fmtCoord(x: Double?): String = if (x == null || !x.isFinite()) "—" else fixed(x, 3)

    // ================= geometry =================

    data class Inverse(val dN: Double, val dE: Double, val hd: Double, val az: Double)

    fun inverse(n1: Double, e1: Double, n2: Double, e2: Double): Inverse {
        val dn = n2 - n1; val de = e2 - e1
        return Inverse(dn, de, hypot(dn, de), norm360(toDeg(atan2(de, dn))))
    }

    /** A reduced shot: horizontal distance and (when known) vertical difference. */
    data class Shot(val hd: Double, val vd: Double?)

    fun shotFromHd(hd: Double) = Shot(hd, null)
    fun shotFromSlope(sd: Double, zenithDeg: Double) = Shot(sd * sin(zenithDeg * D2R), sd * cos(zenithDeg * D2R))

    data class Tolerances(val horizontal: Double = 0.02, val vertical: Double = 0.03, val angleSec: Double = 10.0, val distance: Double = 0.02)

    // ================= setup =================

    data class Setup(
        val n: Double, val e: Double, val z: Double?, val hi: Double?,
        val bsAz: Double, val bsHd: Double?, val bsZ: Double?, val bsSetting: Double
    ) {
        fun shotAzimuth(reading: Double) = norm360(bsAz + reading - bsSetting)
    }

    fun setupFromPoint(n: Double, e: Double, z: Double?, hi: Double?, bsN: Double, bsE: Double, bsZ: Double?, bsSetting: Double): Setup {
        val iv = inverse(n, e, bsN, bsE)
        return Setup(n, e, z, hi, iv.az, iv.hd, bsZ, bsSetting)
    }

    fun setupFromAzimuth(n: Double, e: Double, z: Double?, hi: Double?, bsAz: Double, bsSetting: Double) =
        Setup(n, e, z, hi, norm360(bsAz), null, null, bsSetting)

    // ================= backsight check =================

    data class BacksightCheck(
        val angleErr: Double, val angleOk: Boolean,
        val sideMiss: Double, val sideOk: Boolean,
        val distErr: Double?, val distOk: Boolean?,
        val elevErr: Double?, val elevOk: Boolean?
    ) { val allOk get() = angleOk && sideOk && distOk != false && elevOk != false }

    fun backsightCheck(s: Setup, reading: Double, shot: Shot, rodHt: Double?, tol: Tolerances): BacksightCheck {
        val aErr = norm180(reading - s.bsSetting)
        val hdUse = s.bsHd ?: shot.hd
        val side = hdUse * tan(aErr * D2R)
        val dErr = s.bsHd?.let { shot.hd - it }
        val zErr = if (shot.vd != null && s.z != null && s.hi != null && rodHt != null && s.bsZ != null)
            (s.z + s.hi + shot.vd - rodHt) - s.bsZ else null
        return BacksightCheck(
            aErr, abs(aErr * 3600) <= tol.angleSec,
            side, abs(side) <= tol.horizontal,
            dErr, dErr?.let { abs(it) <= tol.distance },
            zErr, zErr?.let { abs(it) <= tol.vertical }
        )
    }

    // ================= check shot =================

    data class CheckShot(
        val measN: Double, val measE: Double, val measZ: Double?,
        val dN: Double, val dE: Double, val dZ: Double?, val miss: Double,
        val angleErr: Double, val distErr: Double,
        val shotAz: Double, val compAz: Double, val compHd: Double, val shotHd: Double,
        val missOk: Boolean, val elevOk: Boolean?, val angleOk: Boolean, val distOk: Boolean
    ) { val allOk get() = missOk && elevOk != false }

    fun checkShot(s: Setup, knownN: Double, knownE: Double, knownZ: Double?, reading: Double, shot: Shot, rodHt: Double?, tol: Tolerances): CheckShot {
        val az = s.shotAzimuth(reading)
        val mn = s.n + shot.hd * cos(az * D2R)
        val me = s.e + shot.hd * sin(az * D2R)
        val iv = inverse(s.n, s.e, knownN, knownE)
        val dN = mn - knownN; val dE = me - knownE; val miss = hypot(dN, dE)
        val aErr = norm180(az - iv.az); val dErr = shot.hd - iv.hd
        val mz = if (shot.vd != null && s.z != null && s.hi != null && rodHt != null) s.z + s.hi + shot.vd - rodHt else null
        val dZ = if (mz != null && knownZ != null) mz - knownZ else null
        return CheckShot(mn, me, mz, dN, dE, dZ, miss, aErr, dErr, az, iv.az, iv.hd, shot.hd,
            miss <= tol.horizontal, dZ?.let { abs(it) <= tol.vertical }, abs(aErr * 3600) <= tol.angleSec, abs(dErr) <= tol.distance)
    }

    // ================= resection =================

    data class KnownObs(val n: Double, val e: Double, val z: Double?, val reading: Double, val shot: Shot)

    data class PointResidual(val index: Int, val angleRes: Double, val distRes: Double, val dN: Double, val dE: Double, val miss: Double, val ok: Boolean)

    data class Resection(
        val n: Double, val e: Double, val z: Double?, val zSpread: Double?, val zCount: Int,
        val leastSquares: Boolean,
        // 2-point direct
        val angleMisclosure: Double?, val baselineMeasured: Double?, val baselineKnown: Double?, val baselineMisclosure: Double?,
        // least squares
        val residuals: List<PointResidual>, val sigma0: Double?, val sigmaN: Double?, val sigmaE: Double?, val redundancy: Int,
        val azToPoint1: Double, val orientation: Double,
        val allOk: Boolean, val worstIndex: Int?
    )

    class ResectionException(message: String) : Exception(message)

    /**
     * 2 points: direct solution (distance circles; the measured angle picks the side and gives the misclosure).
     * 3–5 points: least squares on E, N and orientation ω (gun reading + ω = azimuth), weighted by instrument std. devs.
     */
    fun resection(pts: List<KnownObs>, hi: Double?, rodHt: Double?, tol: Tolerances, sdAngleSec: Double = 5.0, sdDist: Double = 0.010): Resection {
        if (pts.size < 2) throw ResectionException("Need at least 2 known points.")
        if (pts.size > 5) throw ResectionException("Use at most 5 known points.")
        val p1 = pts[0]; val p2 = pts[1]
        val base = inverse(p1.n, p1.e, p2.n, p2.e); val L = base.hd
        if (L == 0.0) throw ResectionException("Points 1 and 2 are the same.")
        val d1 = p1.shot.hd; val d2 = p2.shot.hd
        val mAng = norm360(p2.reading - p1.reading)
        val lm = sqrt(d1 * d1 + d2 * d2 - 2 * d1 * d2 * cos(mAng * D2R))
        val a = (d1 * d1 - d2 * d2 + L * L) / (2 * L)
        var h2 = d1 * d1 - a * a
        if (h2 < 0) {
            if (h2 > -1e-6 * L * L) h2 = 0.0
            else throw ResectionException("Distances to points 1 and 2 don't close on their baseline (${fmtFt(d1)} + ${fmtFt(d2)} vs ${fmtFt(L)}). Check the shots or the points.")
        }
        val h = sqrt(h2)
        val ux = (p2.e - p1.e) / L; val uy = (p2.n - p1.n) / L
        val bx = p1.e + a * ux; val by = p1.n + a * uy
        data class Cand(val e: Double, val n: Double, val res: Double)
        val cands = listOf(bx - h * uy to by + h * ux, bx + h * uy to by - h * ux).map { (x, y) ->
            val az1 = inverse(y, x, p1.n, p1.e).az; val az2 = inverse(y, x, p2.n, p2.e).az
            Cand(x, y, norm180(norm360(az2 - az1) - mAng))
        }.sortedBy { abs(it.res) }
        var stE = cands[0].e; var stN = cands[0].n
        val angRes = cands[0].res

        var residuals = emptyList<PointResidual>()
        var s0: Double? = null; var sN: Double? = null; var sE: Double? = null
        var omega: Double? = null
        val ls = pts.size >= 3
        if (ls) {
            val sa = sdAngleSec / 3600.0 * D2R
            val wa = 1 / (sa * sa); val wd = 1 / (sdDist * sdDist)
            var x = stE; var y = stN
            var w = run {
                var sx = 0.0; var sy = 0.0
                pts.forEach { p -> val t = (inverse(y, x, p.n, p.e).az - p.reading) * D2R; sx += cos(t); sy += sin(t) }
                atan2(sy, sx)
            }
            var nMat = Array(3) { DoubleArray(3) }
            for (it in 0 until 15) {
                nMat = Array(3) { DoubleArray(3) }
                val u = DoubleArray(3)
                pts.forEach { p ->
                    val dE = p.e - x; val dN = p.n - y; val d2s = dE * dE + dN * dN; val d = sqrt(d2s)
                    val az = atan2(dE, dN)
                    val l1 = norm180(toDeg(p.reading * D2R + w - az)) * D2R   // f = az − ω − r ; A·dx = −f
                    val a1 = doubleArrayOf(-dN / d2s, dE / d2s, -1.0)
                    val a2 = doubleArrayOf(-dE / d, -dN / d, 0.0); val l2 = p.shot.hd - d
                    for ((av, lv, wt) in listOf(Triple(a1, l1, wa), Triple(a2, l2, wd))) {
                        for (r in 0..2) { u[r] += av[r] * wt * lv; for (c in 0..2) nMat[r][c] += av[r] * wt * av[c] }
                    }
                }
                val dx = solve3(nMat, u) ?: throw ResectionException("Geometry too weak to solve — spread the known points around the station.")
                x += dx[0]; y += dx[1]; w += dx[2]
                if (abs(dx[0]) < 1e-7 && abs(dx[1]) < 1e-7 && abs(dx[2]) < 1e-10) break
            }
            var vtpv = 0.0
            residuals = pts.mapIndexed { i, p ->
                val iv = inverse(y, x, p.n, p.e)
                val shotAz = norm360(p.reading + toDeg(w))
                val vA = norm180(shotAz - iv.az); val vD = p.shot.hd - iv.hd
                vtpv += (vA * D2R) * (vA * D2R) * wa + vD * vD * wd
                val mn = y + p.shot.hd * cos(shotAz * D2R); val me = x + p.shot.hd * sin(shotAz * D2R)
                val dn = mn - p.n; val de = me - p.e; val miss = hypot(dn, de)
                PointResidual(i + 1, vA, vD, dn, de, miss, miss <= tol.horizontal && abs(vA * 3600) <= tol.angleSec && abs(vD) <= tol.distance)
            }
            val dof = 2 * pts.size - 3
            s0 = if (dof > 0) sqrt(vtpv / dof) else null
            val q = inv3(nMat)
            if (q != null && s0 != null) { sE = s0 * sqrt(q[0][0]); sN = s0 * sqrt(q[1][1]) }
            stE = x; stN = y; omega = norm180(toDeg(w))
        }

        // elevation from every point with a vertical
        val zs = pts.mapNotNull { p -> if (p.shot.vd != null && p.z != null && hi != null && rodHt != null) p.z - p.shot.vd - hi + rodHt else null }
        val z = if (zs.isNotEmpty()) zs.average() else null
        val zSpread = if (zs.size > 1) zs.max() - zs.min() else null
        val zOk = zSpread?.let { it <= tol.vertical }

        val az1 = inverse(stN, stE, p1.n, p1.e).az
        val orient = omega ?: norm180(az1 - p1.reading)

        val allOk: Boolean
        var worst: Int? = null
        if (!ls) {
            allOk = abs(angRes * 3600) <= tol.angleSec && abs(lm - L) <= tol.distance && zOk != false
        } else {
            allOk = residuals.all { it.ok } && zOk != false
            if (!allOk) worst = residuals.maxByOrNull { it.miss }?.index
        }
        return Resection(
            stN, stE, z, zSpread, zs.size, ls,
            if (ls) null else angRes, if (ls) null else lm, if (ls) null else L, if (ls) null else lm - L,
            residuals, s0, sN, sE, if (ls) 2 * pts.size - 3 else 1,
            az1, orient, allOk, worst
        )
    }

    // ---- 3×3 helpers ----
    private fun det3(m: Array<DoubleArray>) =
        m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1]) - m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0]) + m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0])

    fun inv3(m: Array<DoubleArray>): Array<DoubleArray>? {
        val d = det3(m)
        if (!d.isFinite() || abs(d) < 1e-30) return null
        fun cof(r: Int, k: Int): Double {
            val a = (0..2).filter { it != r }; val b = (0..2).filter { it != k }
            val v = m[a[0]][b[0]] * m[a[1]][b[1]] - m[a[0]][b[1]] * m[a[1]][b[0]]
            return if ((r + k) % 2 == 1) -v else v
        }
        return Array(3) { i -> DoubleArray(3) { j -> cof(j, i) / d } }
    }

    fun solve3(n: Array<DoubleArray>, u: DoubleArray): DoubleArray? {
        val i = inv3(n) ?: return null
        return DoubleArray(3) { r -> i[r][0] * u[0] + i[r][1] * u[1] + i[r][2] * u[2] }
    }
}
