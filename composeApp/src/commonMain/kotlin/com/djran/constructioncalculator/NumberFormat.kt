package com.djran.constructioncalculator

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.floor

/**
 * Multiplatform replacement for "%.Nf".format(x) — String.format is JVM-only
 * and breaks the iOS and Wasm builds. Rounds half away from zero, like "%.Nf".
 */
fun Double.toFixed(decimals: Int): String {
    if (isNaN() || isInfinite()) return toString()
    val factor = 10.0.pow(decimals)
    val scaled = floor(abs(this) * factor + 0.5).toLong()
    val sign = if (this < 0 && scaled != 0L) "-" else ""
    if (decimals <= 0) return sign + scaled.toString()
    val f = factor.toLong()
    val whole = scaled / f
    val frac = (scaled % f).toString().padStart(decimals, '0')
    return "$sign$whole.$frac"
}
