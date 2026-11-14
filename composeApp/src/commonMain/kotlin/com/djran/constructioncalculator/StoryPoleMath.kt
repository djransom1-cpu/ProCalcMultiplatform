package com.djran.constructioncalculator

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * Story Pole — pure math, no Android imports (unit-tested).
 * All math runs in inches. Raw text the user typed is kept as typed.
 */
object StoryPoleMath {

    enum class Mode { LENGTH, ELEVATION }
    enum class Op { ADD, SUBTRACT, SOLVE }

    data class Layer(var label: String = "", var amount: String = "", var op: Op = Op.SUBTRACT)

    data class Pole(
        var name: String = "New pole",
        var mode: Mode = Mode.LENGTH,
        var trade: String = "FRAMING",
        var startLabel: String = "Start",
        var start: String = "0",
        var endLabel: String = "Result",
        var target: String = "",
        val layers: MutableList<Layer> = mutableListOf()
    ) {
        fun deepCopy(): Pole = copy(layers = layers.map { it.copy() }.toMutableList())
    }

    data class Row(val delta: Double?, val value: Double?)

    data class Result(
        val start: Double?,
        val rows: List<Row>,
        val final: Double?,
        val errors: List<String>,
        val unknownIndex: Int,
        val solved: Double?
    ) {
        val ok get() = errors.isEmpty() && final != null
    }

    // ---------------- parsing ----------------

    private val NUM = Regex("""^\d*\.?\d+$""")
    private val WHOLE_FRAC = Regex("""^(\d*\.?\d+)(?:[\s-]+(\d+)/(\d+))?$""")
    private val FRAC = Regex("""^(\d+)/(\d+)$""")

    private fun cleanQuotes(s: String) = s
        .replace('’', '\'').replace('‘', '\'').replace('′', '\'').replace('`', '\'')
        .replace('“', '"').replace('”', '"').replace('″', '"')
        .trim()

    private fun parseInchPart(raw: String): Double? {
        val s = raw.replace("\"", "").trim()
        if (s.isEmpty()) return 0.0
        WHOLE_FRAC.matchEntire(s)?.let { m ->
            var v = m.groupValues[1].toDouble()
            if (m.groupValues[2].isNotEmpty()) {
                val den = m.groupValues[3].toDouble()
                if (den == 0.0) return null
                v += m.groupValues[2].toDouble() / den
            }
            return v
        }
        FRAC.matchEntire(s)?.let { m ->
            val den = m.groupValues[2].toDouble()
            if (den == 0.0) return null
            return m.groupValues[1].toDouble() / den
        }
        return null
    }

    /**
     * Parses a length to inches. [plainFeet] = what a bare number means:
     * true → decimal feet (elevations), false → inches (layers, length mode).
     * Anything containing ' " or / is read as written. Returns null when unreadable.
     */
    fun parseLen(raw: String?, plainFeet: Boolean): Double? {
        var s = cleanQuotes(raw ?: "")
        if (s.isEmpty()) return null
        var sign = 1.0
        if (s.startsWith("-")) { sign = -1.0; s = s.substring(1).trim() }
        else if (s.startsWith("+")) s = s.substring(1).trim()
        if (s.contains('\'')) {
            val i = s.indexOf('\'')
            val fs = s.substring(0, i).trim()
            if (!NUM.matches(fs)) return null
            val rest = s.substring(i + 1).trim().removePrefix("-").trim()
            val inch = parseInchPart(rest) ?: return null
            return sign * (fs.toDouble() * 12 + inch)
        }
        if (s.contains('"') || s.contains('/')) {
            val v = parseInchPart(s) ?: return null
            return sign * v
        }
        if (!NUM.matches(s)) return null
        val v = s.toDouble()
        return sign * if (plainFeet) v * 12 else v
    }

    // ---------------- formatting ----------------

    private fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)
    private fun frac(n16: Long): String { val g = gcd(n16, 16); return "${n16 / g}/${16 / g}" }

    /** 610' 9-3/8"  ·  0' 2-3/8"  ·  91' 0-1/4"  (nearest 1/16") */
    fun fmtFtIn(inches: Double?): String {
        if (inches == null || !inches.isFinite()) return "—"
        val neg = inches < 0
        var s = (abs(inches) * 16).roundToLong()
        val ft = s / 192; s -= ft * 192
        val inch = s / 16; val f = s - inch * 16
        val inStr = if (f > 0) "$inch-${frac(f)}" else "$inch"
        return (if (neg) "-" else "") + "$ft' $inStr\""
    }

    /** 1-7/8"  ·  81"  ·  3/4" */
    fun fmtIn(inches: Double?): String {
        if (inches == null || !inches.isFinite()) return "—"
        val neg = inches < 0
        val s = (abs(inches) * 16).roundToLong()
        val w = s / 16; val f = s - w * 16
        val body = when {
            f == 0L -> "$w"
            w == 0L -> frac(f)
            else -> "$w-${frac(f)}"
        }
        return (if (neg) "-" else "") + body + "\""
    }

    /** 610.781' */
    fun fmtDecFt(inches: Double?): String =
        if (inches == null || !inches.isFinite()) "—" else fixed(inches / 12.0, 3) + "'"

    /** Fixed-decimal text without String.format, so this file also compiles for the web build. */
    fun fixed(x: Double, d: Int): String {
        var p = 1L; repeat(d) { p *= 10 }
        val r = (abs(x) * p).roundToLong()
        val body = if (d > 0) "${r / p}." + (r % p).toString().padStart(d, '0') else "${r / p}"
        return if (x < 0 && r != 0L) "-$body" else body
    }

    /** Second line under a value: decimal feet in elevation mode, total inches in length mode. */
    fun fmtSecond(inches: Double?, mode: Mode) = if (mode == Mode.ELEVATION) fmtDecFt(inches) else fmtIn(inches)

    // ---------------- calculation ----------------

    fun compute(p: Pole): Result {
        val plainFeet = p.mode == Mode.ELEVATION
        val errors = mutableListOf<String>()
        val start = parseLen(p.start, plainFeet)
        val unknowns = p.layers.indices.filter { p.layers[it].op == Op.SOLVE }
        if (unknowns.size > 1) errors += "Only one row can be the unknown (?)."
        val unknownIndex = if (unknowns.size == 1) unknowns[0] else -1
        val vals = p.layers.map { if (it.op == Op.SOLVE) null else parseLen(it.amount, false) }
        p.layers.forEachIndexed { i, l ->
            if (l.op != Op.SOLVE && vals[i] == null)
                errors += "Row ${i + 1}" + (if (l.label.isNotBlank()) " (${l.label})" else "") + ": enter an amount."
        }
        if (start == null) errors += "Start: enter a starting " + (if (plainFeet) "elevation." else "height.")

        var solved: Double? = null
        if (unknownIndex >= 0) {
            val t = parseLen(p.target, plainFeet)
            if (t == null) errors += "Enter the known end " + (if (plainFeet) "elevation" else "height") + " to solve the ? row."
            else if (errors.isEmpty()) {
                var sum = start!!
                p.layers.forEachIndexed { i, l -> if (i != unknownIndex) sum += sign(l.op) * vals[i]!! }
                solved = t - sum
            }
        }

        val rows = mutableListOf<Row>()
        var run: Double? = start
        p.layers.forEachIndexed { i, l ->
            val d: Double? = if (i == unknownIndex) solved else vals[i]?.let { sign(l.op) * it }
            run = if (run != null && d != null) run!! + d else null
            rows += Row(d, run)
        }
        val final = if (p.layers.isEmpty()) start else run
        return Result(start, rows, final, errors, unknownIndex, solved)
    }

    private fun sign(op: Op) = if (op == Op.ADD) 1.0 else -1.0

    /** Converts start/target text so the number keeps the same physical height when the mode flips. */
    fun convertForMode(text: String, from: Mode, to: Mode): String {
        if (from == to) return text
        val inches = parseLen(text, from == Mode.ELEVATION) ?: return text
        val v = if (to == Mode.ELEVATION) inches / 12.0 else inches
        return trimNum(v)
    }

    private fun trimNum(v: Double): String {
        val r = (v * 10000).roundToLong() / 10000.0
        return if (r == floor(r)) r.toLong().toString() else r.toString()
    }

    /** Shareable text summary, one line per layer. */
    fun asText(p: Pole): String {
        val r = compute(p)
        val sb = StringBuilder()
        sb.append(p.name).append('\n')
        sb.append("${p.startLabel}: ${fmtFtIn(r.start)} (${fmtSecond(r.start, p.mode)})\n")
        p.layers.forEachIndexed { i, l ->
            val row = r.rows[i]
            val d = row.delta
            val signCh = if (d != null && d >= 0) "+" else "−"
            sb.append("$signCh ${fmtIn(d?.let { abs(it) })}  ${l.label}${if (i == r.unknownIndex) " (solved)" else ""}  → ${fmtFtIn(row.value)}\n")
        }
        sb.append("= ${p.endLabel}: ${fmtFtIn(r.final)} (${fmtSecond(r.final, p.mode)})")
        return sb.toString()
    }
}
