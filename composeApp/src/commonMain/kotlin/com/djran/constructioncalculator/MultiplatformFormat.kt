package com.djran.constructioncalculator

import kotlin.math.abs
import kotlin.math.round

/**
 * Minimal, dependency-free stand-in for Java's String.format/"%.2f".format(x), which
 * Kotlin/Wasm and Kotlin/Native don't provide (no java.util.Formatter). Supports exactly
 * the specifier subset this codebase uses: %d, %0Nd, %s, %.Nf, %0W.Nf, and %%.
 */
fun sprintf(pattern: String, vararg args: Any?): String {
    val sb = StringBuilder()
    var argIndex = 0
    var i = 0
    while (i < pattern.length) {
        val c = pattern[i]
        if (c != '%') {
            sb.append(c)
            i++
            continue
        }
        i++
        if (i >= pattern.length) break
        if (pattern[i] == '%') {
            sb.append('%')
            i++
            continue
        }

        var zeroPad = false
        if (pattern[i] == '0') {
            zeroPad = true
            i++
        }
        var width = 0
        while (i < pattern.length && pattern[i].isDigit()) {
            width = width * 10 + (pattern[i] - '0')
            i++
        }
        var decimals = -1
        if (i < pattern.length && pattern[i] == '.') {
            i++
            decimals = 0
            while (i < pattern.length && pattern[i].isDigit()) {
                decimals = decimals * 10 + (pattern[i] - '0')
                i++
            }
        }
        if (i >= pattern.length) break
        val conv = pattern[i]
        i++

        val arg = args.getOrNull(argIndex)
        argIndex++

        val piece = when (conv) {
            'd' -> {
                val n = (arg as Number).toLong().toString()
                if (zeroPad && width > n.length) n.padStart(width, '0') else n
            }
            'f' -> {
                val d = (arg as Number).toDouble()
                val dec = if (decimals >= 0) decimals else 6
                val s = formatFixedDecimal(d, dec)
                if (zeroPad && width > s.length) s.padStart(width, '0') else s
            }
            's' -> arg.toString()
            else -> ""
        }
        sb.append(piece)
    }
    return sb.toString()
}

private fun pow10(n: Int): Long {
    var r = 1L
    repeat(n) { r *= 10 }
    return r
}

/** Fixed-point decimal formatting with round-half-up, matching Java's %.Nf for typical values. */
private fun formatFixedDecimal(value: Double, decimals: Int): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value > 0) "Infinity" else "-Infinity"

    val factor = pow10(decimals)
    val negative = value < 0
    val scaled = round(abs(value) * factor).toLong()
    val intPart = scaled / factor
    val fracPart = scaled % factor
    val sign = if (negative && scaled != 0L) "-" else ""
    return if (decimals > 0) {
        "$sign$intPart.${fracPart.toString().padStart(decimals, '0')}"
    } else {
        "$sign$intPart"
    }
}
