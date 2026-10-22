package com.djran.constructioncalculator

import kotlin.math.*

/**
 * Arc (circular segment) math for the Arc page. All lengths are inches.
 *
 * R = radius, C = chord, S = spring (rise at mid-chord), L = arc length,
 * θ = central angle in radians.
 *
 * This file is plain Kotlin with no platform code. The same file lives in the
 * Android app (ConstructionCalculatorAndroid) and ProCalcMultiplatform; keep the
 * two copies identical so the builds cannot drift.
 */
object ArcMath {

    data class Solution(
        val radius: Double,
        val chord: Double,
        val spring: Double,
        val angleRad: Double,
        val arcLength: Double
    ) {
        val angleDeg: Double get() = angleRad * 180.0 / PI
    }

    /** Either a solution or an error message to show inline. */
    data class Outcome(val solution: Solution?, val error: String?)

    /** One layout station: distance along the chord from the left end, and offset up to the arc. */
    data class Station(val index: Int, val along: Double, val offset: Double)

    const val DEFAULT_STATIONS = 5
    const val MAX_STATIONS = 50

    private const val EPS = 1e-9

    // R = C²/8S + S/2
    fun radiusFromChordSpring(chord: Double, spring: Double): Double =
        chord * chord / (8.0 * spring) + spring / 2.0

    // S = R − √(R² − (C/2)²)
    fun springFromRadiusChord(radius: Double, chord: Double): Double {
        val half = chord / 2.0
        return radius - sqrt(max(0.0, radius * radius - half * half))
    }

    // C = 2√(2RS − S²)
    fun chordFromRadiusSpring(radius: Double, spring: Double): Double =
        2.0 * sqrt(max(0.0, 2.0 * radius * spring - spring * spring))

    // θ = 2·asin(C / 2R)
    fun centralAngle(radius: Double, chord: Double): Double =
        2.0 * asin(min(1.0, chord / (2.0 * radius)))

    /** Radius unknown: chord and spring given. */
    fun solveChordSpring(chord: Double, spring: Double): Outcome {
        if (chord <= 0.0 || spring <= 0.0) return fail("Chord and spring must be greater than zero.")
        if (spring > chord / 2.0 + EPS) return fail("Spring is more than half the chord: that is past a semicircle, which this page does not cover.")
        val r = radiusFromChordSpring(chord, spring)
        return ok(r, chord, spring)
    }

    /** Radius known, chord given. */
    fun solveRadiusChord(radius: Double, chord: Double): Outcome {
        if (radius <= 0.0 || chord <= 0.0) return fail("Radius and chord must be greater than zero.")
        if (chord > 2.0 * radius + EPS) return fail("Chord is longer than 2 × radius: no arc fits.")
        return ok(radius, chord, springFromRadiusChord(radius, chord))
    }

    /** Radius known, spring given. */
    fun solveRadiusSpring(radius: Double, spring: Double): Outcome {
        if (radius <= 0.0 || spring <= 0.0) return fail("Radius and spring must be greater than zero.")
        if (spring > radius + EPS) return fail("Spring is greater than the radius: that is past a semicircle, which this page does not cover.")
        return ok(radius, chordFromRadiusSpring(radius, spring), spring)
    }

    /**
     * Chord offset (layout) table. [interior] stations divide the chord evenly; the
     * two chord ends are always included, so the list has interior + 2 rows.
     * Offset at distance x from the chord midpoint: y = √(R² − x²) − (R − S).
     */
    fun offsetTable(sol: Solution, interior: Int): List<Station> {
        val n = interior.coerceIn(1, MAX_STATIONS)
        val c = sol.chord
        return (0..n + 1).map { i ->
            val along = c * i / (n + 1)
            Station(i, along, offsetAt(sol, along - c / 2.0))
        }
    }

    /** Offset from the chord up to the arc at distance [x] from the chord midpoint. */
    fun offsetAt(sol: Solution, x: Double): Double {
        val r = sol.radius
        val y = sqrt(max(0.0, r * r - x * x)) - (r - sol.spring)
        return if (abs(y) < 1e-7) 0.0 else max(0.0, y)
    }

    private fun ok(r: Double, c: Double, s: Double): Outcome {
        val theta = centralAngle(r, c)
        return Outcome(Solution(r, c, s, theta, r * theta), null)
    }

    private fun fail(msg: String) = Outcome(null, msg)
}
