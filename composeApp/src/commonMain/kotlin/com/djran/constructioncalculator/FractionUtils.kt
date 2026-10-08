package com.djran.constructioncalculator

import kotlin.math.abs
import kotlin.math.roundToInt

object FractionUtils {
    
    /**
     * Unified Parser: Handles 16', 11", 5/8", 16' 11", 16' 11 5/8", etc.
     * Returns total inches as Double.
     */
    fun parse(input: String?): Double {
        if (input == null || input.isBlank()) return 0.0
        
        try {
            var cleanInput = input.trim()
            
            // Handle Area/Volume labels from results
            if (cleanInput.contains("sq ft")) {
                val num = cleanInput.replace("sq ft", "").trim().toDoubleOrNull() ?: 0.0
                return num * 144.0
            }
            if (cleanInput.contains("sq in")) {
                val num = cleanInput.replace("sq in", "").trim().toDoubleOrNull() ?: 0.0
                return num
            }
            if (cleanInput.contains("cu yd")) {
                val num = cleanInput.replace("cu yd", "").trim().toDoubleOrNull() ?: 0.0
                return num * 46656.0
            }
            if (cleanInput.contains("cu ft")) {
                val num = cleanInput.replace("cu ft", "").trim().toDoubleOrNull() ?: 0.0
                return num * 1728.0
            }
            if (cleanInput.contains("cu in")) {
                val num = cleanInput.replace("cu in", "").trim().toDoubleOrNull() ?: 0.0
                return num
            }
            if (cleanInput.contains("sq m")) {
                val num = cleanInput.replace("sq m", "").trim().toDoubleOrNull() ?: 0.0
                return num * 1550.0031 // sq m to sq in
            }
            if (cleanInput.contains("cu m")) {
                val num = cleanInput.replace("cu m", "").trim().toDoubleOrNull() ?: 0.0
                return num * 61023.7441 // cu m to cu in
            }
            if (cleanInput.contains("sq cm")) {
                val num = cleanInput.replace("sq cm", "").trim().toDoubleOrNull() ?: 0.0
                return num * 0.1550 // sq cm to sq in
            }
            if (cleanInput.contains("cu cm")) {
                val num = cleanInput.replace("cu cm", "").trim().toDoubleOrNull() ?: 0.0
                return num * 0.0610 // cu cm to cu in
            }
            if (cleanInput.contains("mm")) {
                val num = cleanInput.replace("mm", "").trim().toDoubleOrNull() ?: 0.0
                return num / 25.4
            }
            if (cleanInput.contains("cm")) {
                val num = cleanInput.replace("cm", "").trim().toDoubleOrNull() ?: 0.0
                return num / 2.54
            }
            if (cleanInput.contains(" m")) {
                val num = cleanInput.replace(" m", "").trim().toDoubleOrNull() ?: 0.0
                return num * 39.3701
            }

            // Handle signs for Coordinate/Site Survey pages
            var multiplier = 1.0
            if (cleanInput.startsWith("-")) {
                multiplier = -1.0
                cleanInput = cleanInput.substring(1).trim()
            } else if (cleanInput.startsWith("+")) {
                cleanInput = cleanInput.substring(1).trim()
            }

            // 1. Pre-clean: remove inch marks and trim
            cleanInput = cleanInput.replace("\"", "")
            var total = 0.0
            
            // 2. Handle Feet (')
            if (cleanInput.contains("'")) {
                val parts = cleanInput.split("'")
                val feetPart = parts[0].trim().toDoubleOrNull() ?: 0.0
                total += feetPart * 12.0
                
                if (parts.size > 1 && parts[1].isNotBlank()) {
                    total += parseInchesPart(parts[1])
                }
            } else {
                // 3. Pure Inch/Fraction input
                total += parseInchesPart(cleanInput)
            }
            
            return total * multiplier
            
        } catch (e: Exception) {
            return 0.0
        }
    }

    /** Helper for labeled (ft) fields. Plain numbers are treated as feet. */
    fun parseFeet(input: String?): Double {
        if (input == null || input.isBlank()) return 0.0
        val s = input.trim()
        if (!s.contains("'") && !s.contains("\"")) return (s.toDoubleOrNull() ?: 0.0) * 12.0
        return parse(input)
    }

    private fun parseInchesPart(input: String): Double {
        val clean = input.trim()
        if (clean.isBlank()) return 0.0
        
        // Handle mixed strings like "7 1/2" or "11 5/8"
        val tokens = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
        var sum = 0.0
        
        for (token in tokens) {
            if (token.contains("/")) {
                sum += parseFractionOnly(token)
            } else {
                sum += token.toDoubleOrNull() ?: 0.0
            }
        }
        return sum
    }

    private fun parseFractionOnly(input: String): Double {
        val parts = input.split("/")
        if (parts.size == 2) {
            val n = parts[0].trim().toDoubleOrNull() ?: 0.0
            val d = parts[1].trim().toDoubleOrNull() ?: 1.0
            return if (d != 0.0) n / d else 0.0
        }
        return input.toDoubleOrNull() ?: 0.0
    }

    fun formatInches(totalInches: Double): String {
        val total = totalInches
        val feet = (total / 12).toInt()
        val remainingInches = total % 12
        val wholeInches = remainingInches.toInt()
        val fractionalPart = remainingInches - wholeInches
        
        val fraction = decimalToFraction(fractionalPart)
        
        var finalWholeInches = wholeInches
        var finalFeet = feet
        var displayFraction = fraction
        
        if (fraction == "1") {
            displayFraction = ""
            finalWholeInches++
        }
        
        if (finalWholeInches >= 12) {
            finalWholeInches -= 12
            finalFeet++
        }
        
        return when {
            finalFeet > 0 && displayFraction.isNotEmpty() -> "${finalFeet}' ${finalWholeInches} ${displayFraction}\""
            finalFeet > 0 -> "${finalFeet}' ${finalWholeInches}\""
            displayFraction.isNotEmpty() -> "${finalWholeInches} ${displayFraction}\""
            else -> "${finalWholeInches}\""
        }
    }

    // Metric display. [mmDecimals] is the Menu's metric precision: 0 rounds lengths to the
    // nearest millimetre (metres to 3 decimals, centimetres to 1), 1 to a tenth of a
    // millimetre, 2 to a hundredth. Areas and volumes get the same extra decimals. Trailing
    // zeros are dropped (67 cm, 1.5 m). The unit is picked by size, so negative answers
    // get the same unit as positive ones.
    fun formatMetric(value: Double, power: Int = 1, mmDecimals: Int = 0): String {
        return when (power) {
            1 -> {
                val mm = value * 25.4
                if (abs(mm) >= 1000.0) "${(mm / 1000.0).roundTo(3 + mmDecimals)} m"
                else if (abs(mm) >= 10.0) "${(mm / 10.0).roundTo(1 + mmDecimals)} cm"
                else "${mm.roundTo(mmDecimals)} mm"
            }
            2 -> {
                val sqM = value / 1550.0031
                if (abs(sqM) >= 1.0) "${sqM.roundTo(3 + mmDecimals)} sq m"
                else "${(value / 0.1550).roundTo(1 + mmDecimals)} sq cm"
            }
            3 -> {
                val cuM = value / 61023.7441
                if (abs(cuM) >= 1.0) "${cuM.roundTo(3 + mmDecimals)} cu m"
                else "${(value / 0.0610).roundTo(1 + mmDecimals)} cu cm"
            }
            else -> value.toString()
        }
    }

    private fun Double.roundTo(decimals: Int): String {
        val fixed = toFixed(decimals)
        return if (decimals > 0) fixed.trimEnd('0').trimEnd('.') else fixed
    }

    private fun decimalToFraction(decimal: Double): String {
        if (decimal < 0.0001) return ""
        val sixteenths = (decimal * 16).roundToInt()
        if (sixteenths == 0) return ""
        if (sixteenths == 16) return "1"
        
        var num = sixteenths
        var den = 16
        val common = gcd(num, den)
        num /= common
        den /= common
        return "$num/$den"
    }

    private fun gcd(a: Int, b: Int): Int {
        var x = a; var y = b
        while (y != 0) { val t = y; y = x % y; x = t }
        return x
    }
}
