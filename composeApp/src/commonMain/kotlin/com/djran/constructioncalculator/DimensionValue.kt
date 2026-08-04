package com.djran.constructioncalculator

import kotlin.math.*

/**
 * unitPower:
 * 0 = Scalar (No units, e.g. 5)
 * 1 = Linear (Inches, e.g. 12")
 * 2 = Area (Sq Inches, e.g. 144 sq in)
 * 3 = Volume (Cu Inches, e.g. 1728 cu in)
 */
data class DimensionValue(val value: Double, val unitPower: Int = 0) {

    operator fun plus(other: DimensionValue): DimensionValue {
        val targetPower = if (this.value == 0.0) other.unitPower else this.unitPower
        return DimensionValue(this.value + other.value, targetPower)
    }

    operator fun minus(other: DimensionValue): DimensionValue {
        val targetPower = if (this.value == 0.0) other.unitPower else this.unitPower
        return DimensionValue(this.value - other.value, targetPower)
    }

    operator fun times(other: DimensionValue): DimensionValue {
        val newPower = this.unitPower + other.unitPower
        return DimensionValue(this.value * other.value, newPower.coerceIn(0, 3))
    }

    operator fun div(other: DimensionValue): DimensionValue {
        if (other.value == 0.0) return DimensionValue(0.0, this.unitPower)
        val newPower = this.unitPower - other.unitPower
        return DimensionValue(this.value / other.value, newPower.coerceAtLeast(0))
    }

    override fun toString(): String {
        return when (unitPower) {
            0 -> formatScalar(value)
            1 -> formatLinear(value)
            2 -> formatArea(value)
            3 -> formatVolume(value)
            else -> formatScalar(value)
        }
    }

    private fun formatScalar(v: Double): String {
        return if (v % 1.0 == 0.0) v.toLong().toString()
        else {
            // Safer rounding for KMP to avoid Int overflow
            val factor = 10000.0
            val rounded = (v * factor).roundToLong() / factor
            rounded.toString().trimEnd('0').trimEnd('.')
        }
    }

    private fun formatLinear(totalInches: Double): String {
        val isNegative = totalInches < 0
        val total = abs(totalInches)
        val feet = (total / 12).toInt()
        val remainingInches = total % 12
        val wholeInches = remainingInches.toInt()
        val fractionalPart = remainingInches - wholeInches
        
        val sixteenths = (fractionalPart * 16).roundToInt()
        
        var finalFeet = feet
        var finalInches = wholeInches
        var finalSixteenths = sixteenths
        
        if (finalSixteenths == 16) {
            finalSixteenths = 0
            finalInches += 1
        }
        if (finalInches == 12) {
            finalInches = 0
            finalFeet += 1
        }
        
        val fraction = if (finalSixteenths > 0) {
            val common = gcd(finalSixteenths, 16)
            "${finalSixteenths / common}/${16 / common}"
        } else ""
        
        val result = buildString {
            if (isNegative) append("-")
            if (finalFeet != 0) append("${finalFeet}' ")
            if (finalInches != 0 || fraction.isNotEmpty() || finalFeet == 0) {
                append("${finalInches}")
                if (fraction.isNotEmpty()) {
                    append(" $fraction")
                }
                append("\"")
            }
        }.trim()
        return if (result == "-" || result.isEmpty()) "0\"" else result
    }

    private fun formatArea(sqInches: Double): String {
        val sqFt = sqInches / 144.0
        return formatScalar(sqFt) + " sq ft"
    }

    private fun formatVolume(cuInches: Double): String {
        val totalCuYds = cuInches / 46656.0
        return if (totalCuYds < 0.1) {
            val cuFt = cuInches / 1728.0
            formatScalar(cuFt) + " cu ft"
        } else {
            formatScalar(totalCuYds) + " cu yd"
        }
    }

    private fun gcd(a: Int, b: Int): Int {
        var x = a; var y = b
        while (y != 0) { val t = y; y = x % y; x = t }
        return x
    }
}
