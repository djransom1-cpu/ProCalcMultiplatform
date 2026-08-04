package com.djran.constructioncalculator

import kotlin.math.*

object StairEngine {
    data class StairResult(
        val riserCount: Int, 
        val actualRise: Double, 
        val treadCount: Int, 
        val stringerRun: Double, 
        val totalRun: Double, 
        val stringerLen: Double, 
        val treadsOut: Double, 
        val distOut: Double, 
        val angleDeg: Double
    )

    fun calculate(
        totRise: Double, 
        desRise: Double = 7.5, 
        treadWidth: Double = 10.25, 
        floorThick: Double = 11.875, 
        nosing: Double = 1.0, 
        headroomReq: Double = 81.0
    ): StairResult {
        val riserCount = ceil(totRise / desRise).toInt().coerceAtLeast(1)
        val actualRise = totRise / riserCount
        val treadCount = (riserCount - 1).coerceAtLeast(0)
        val stringerRun = treadWidth - nosing
        val stringerLen = sqrt(totRise.pow(2) + (treadCount * stringerRun).pow(2))
        val treadsOut = (headroomReq + floorThick) / actualRise
        val distOut = (treadsOut * stringerRun) + nosing
        val angleDeg = atan(actualRise / stringerRun) * (180.0 / PI)
        
        return StairResult(
            riserCount, actualRise, treadCount, stringerRun, 
            (treadCount * stringerRun + nosing), stringerLen, 
            treadsOut, distOut, angleDeg
        )
    }
}

object RafterEngine {
    data class RafterResult(
        val run: Double, 
        val rise: Double, 
        val totRise: Double, 
        val rafterLen: Double, 
        val tailLen: Double, 
        val overall: Double, 
        val heel: Double,
        val pitch: Double,
        val hipRun: Double, 
        val hipPitch: Double, 
        val hipLen: Double, 
        val hipTailLen: Double,
        val hipOverall: Double,
        val hipRise: Double,
        val hipOARise: Double,
        val angleDeg: Double
    )

    fun calculate(
        span: Double, 
        pitchVal: Double, 
        ridge: Double = 1.5, 
        heel: Double = 4.0, 
        overhang: Double = 12.0
    ): RafterResult {
        val run = (span / 2.0) - (ridge / 2.0)
        val rise = run * (pitchVal / 12.0)
        val diagFactor = sqrt(pitchVal.pow(2) + 144.0)
        val rafterLen = (diagFactor / 12.0) * run
        val tailLen = (diagFactor / 12.0) * overhang
        
        val hipRun = run / cos(45.0 * (PI / 180.0))
        val hipRise = rise // Rise is same for hip and common to reach same ridge
        val hipPitch = hipRise / (hipRun / 12.0)
        val hipDiagFactor = sqrt(hipPitch.pow(2) + 144.0)
        val hipLen = (hipDiagFactor / 12.0) * hipRun
        
        val hipTailRun = overhang / cos(45.0 * (PI / 180.0))
        val hipTailLen = (hipDiagFactor / 12.0) * hipTailRun
        
        val angleDeg = atan(pitchVal / 12.0) * (180.0 / PI)
        
        return RafterResult(
            run = run,
            rise = rise,
            totRise = rise + heel,
            rafterLen = rafterLen,
            tailLen = tailLen,
            overall = rafterLen + tailLen,
            heel = heel,
            pitch = pitchVal,
            hipRun = hipRun,
            hipPitch = hipPitch,
            hipLen = hipLen,
            hipTailLen = hipTailLen,
            hipOverall = hipLen + hipTailLen,
            hipRise = hipRise,
            hipOARise = hipRise + heel,
            angleDeg = angleDeg
        )
    }
}

object ConcreteEngine {
    fun calculateVolume(l: Double, w: Double, tInches: Double): Double {
        return (l * w * (tInches / 12.0)) / 27.0 // Returns Cubic Yards
    }

    fun calculateRebarGrid(l: Double, w: Double, ocInches: Double, mats: Int = 1): Double {
        val linesW = floor(w * 12.0 / ocInches) + 1
        val linesL = floor(l * 12.0 / ocInches) + 1
        return (linesW * l + linesL * w) * mats // Returns Linear Feet
    }
}

object WallEngine {
    fun estimateStuds(length: Double, oc: Double = 16.0): Int {
        // Standard framing practice: (L / OC) * 1.25 for junctions/corners + 2 end studs
        return ceil((length * 12.0 / oc) * 1.25).toInt() + 2
    }

    fun estimatePlates(length: Double): Double {
        return length * 3.0 // Standard 3-plate wall (1 sill, 2 top)
    }

    /** Calculates blocking rows for walls > 10ft. Every 8ft interval. */
    fun estimateBlocking(length: Double, height: Double): Double {
        if (height <= 10.0) return 0.0
        val rows = floor(height / 8.0).toInt()
        return length * rows
    }

    fun estimateSheathing(length: Double, height: Double): Int {
        return ceil((length * height) / 32.0).toInt() // Standard 4x8 sheets
    }
}

object CrownEngine {
    fun calculateSettings(spring: Double, wallAngle: Double): Pair<Double, Double> {
        val s = spring * (PI / 180.0)
        
        // Smart Angle Logic: If user inputs Turn Angle (e.g. 45), use it.
        // If user inputs Wall Angle (e.g. 135), convert to Turn (45).
        val turnAngle = if (wallAngle < 90.0) wallAngle else abs(180.0 - wallAngle)
        val t2 = (turnAngle / 2.0) * (PI / 180.0)
        
        val miter = atan(tan(t2) * sin(s)) * (180.0 / PI)
        val bevel = asin(sin(t2) * cos(s)) * (180.0 / PI)
        return miter to bevel
    }
}

object PineLineEngine {
    fun calculate(
        pitch: Double, 
        heel: Double, 
        wall: Double, 
        brick: Double, 
        friezeThick: Double, 
        overhang: Double, 
        fascia: Double, 
        reveal: Double
    ): Double {
        val totalRun = wall + brick + friezeThick + overhang
        val drop = totalRun * (pitch / 12.0)
        val pineLineHt = (heel - drop) - (fascia - reveal)
        return pineLineHt // Vertical height from wall plate
    }
}

object ArcEngine {
    fun solveFromChordAndHeight(chord: Double, height: Double): Double {
        // Radius = (Chord²/8H) + H/2
        return (chord.pow(2) / (8.0 * height)) + (height / 2.0)
    }

    fun arcLength(radius: Double, angleDeg: Double): Double {
        return (PI * radius * angleDeg) / 180.0
    }
}

object CoordinateEngine {
    data class Point(val n: Double, val e: Double, val elev: Double = 0.0)

    fun addDeltaPoint(lastPt: Point, nDelta: Double, eDelta: Double, elDelta: Double): Point {
        return Point(
            lastPt.n + nDelta,
            lastPt.e + eDelta,
            lastPt.elev + elDelta
        )
    }

    fun dist(p1: Point, p2: Point): Double {
        return sqrt((p1.n - p2.n).pow(2) + (p1.e - p2.e).pow(2))
    }

    fun calculateNetArea(pts: List<Point>, arcHeights: List<Double>): Double {
        if (pts.size < 3) return 0.0
        var area = 0.0
        var j = pts.size - 1
        for (i in pts.indices) {
            area += (pts[j].e + pts[i].e) * (pts[i].n - pts[j].n)
            val h = arcHeights.getOrElse(i) { 0.0 }
            if (h != 0.0) {
                val chord = dist(pts[j], pts[i])
                area += calculateCircularSegmentArea(chord, h) * 2.0
            }
            j = i
        }
        return abs(area / 2.0)
    }

    private fun calculateCircularSegmentArea(chord: Double, height: Double): Double {
        if (height == 0.0 || chord == 0.0) return 0.0
        val r = (height / 2.0) + (chord * chord) / (8.0 * height)
        val theta = 2.0 * asin(chord / (2.0 * r))
        return 0.5 * r * r * (theta - sin(theta))
    }
}

object TakeoffEngine {
    fun getPixelsPerFoot(pixelDist: Double, realFeet: Double): Double = pixelDist / realFeet

    fun calculateArea(points: List<Pair<Double, Double>>, pxPerFt: Double): Double {
        if (points.size < 3) return 0.0
        var sum = 0.0
        for (i in points.indices) {
            val p1 = points[i]; val p2 = points[(i + 1) % points.size]
            sum += (p1.first / pxPerFt * p2.second / pxPerFt - p2.first / pxPerFt * p1.second / pxPerFt)
        }
        return abs(sum / 2.0)
    }
}

object ColumnEngine {
    data class ColumnResult(
        val circumference: Double,
        val surfaceArea: Double,
        val volume: Double,
        val crossSectionArea: Double
    )

    fun calculate(diameter: Double, height: Double): ColumnResult {
        val radius = diameter / 2.0
        val circ = PI * diameter
        val csArea = PI * radius.pow(2)
        val surfArea = (circ * height) + (2 * csArea)
        val vol = csArea * height
        
        return ColumnResult(circ, surfArea, vol, csArea)
    }
}

object FramingEngine {

    data class Params(
        val wallHeight: Double,
        val headerHeightBottom: Double,
        val headerDepth: Double,
        val roWidth: Double,
        val roHeight: Double,
        val winCount: Int = 1,
        val spaceBetween: Double = 0.0,
        val studOC: Double = 16.0,
        val isDoor: Boolean = false,
        val studSize: String = "2x4"
    )

    data class Result(
        val studHeight: Double,
        val headerLength: Double,
        val headerNominal: String,
        val jackCount: Int,
        val jackLength: Double,
        val sillCount: Int,
        val sillLength: Double,
        val lowerCrippleCount: Int,
        val lowerCrippleLength: Double,
        val upperCrippleCount: Int,
        val upperCrippleLength: Double,
        val totalROWidth: Double
    )

    fun calculate(p: Params): Result {
        val totalROWidth = (p.winCount * p.roWidth) + (max(0.0, (p.winCount - 1).toDouble()) * p.spaceBetween)

        // 60-Inch Rule: > 60" individual RO requires double trimmers
        val jacksPerSide = if (p.roWidth > 60.0) 2 else 1
        val jackCount = 2 * jacksPerSide

        val headerLength = totalROWidth + (jackCount * 1.5)
        val jackLength = p.headerHeightBottom - 1.5
        val studHeight = p.wallHeight - 4.5 // Standard 3-plate assembly

        val sillLength = p.roWidth
        val lowerCripLen = p.headerHeightBottom - p.roHeight - 3.0
        val upperCripLen = p.wallHeight - (p.headerHeightBottom + p.headerDepth) - 3.0

        // Count Logic
        val cripsPerWin = (ceil(p.roWidth / p.studOC) + 1).toInt()
        val totalLowerCrips = if (p.isDoor) 0 else p.winCount * cripsPerWin
        val totalUpperCrips = (ceil(totalROWidth / p.studOC) + 1).toInt()

        return Result(
            studHeight = studHeight,
            headerLength = headerLength,
            headerNominal = getNominalSize(p.headerDepth),
            jackCount = jackCount,
            jackLength = jackLength,
            sillCount = if (p.isDoor) 0 else p.winCount,
            sillLength = sillLength,
            lowerCrippleCount = totalLowerCrips,
            lowerCrippleLength = lowerCripLen,
            upperCrippleCount = totalUpperCrips,
            upperCrippleLength = upperCripLen,
            totalROWidth = totalROWidth
        )
    }

    private fun getNominalSize(depth: Double): String {
        return when {
            abs(depth - 3.5) < 0.2 -> "2x4"
            abs(depth - 5.5) < 0.2 -> "2x6"
            abs(depth - 7.25) < 0.2 -> "2x8"
            abs(depth - 9.25) < 0.2 -> "2x10"
            abs(depth - 11.25) < 0.2 -> "2x12"
            else -> "${depth}\" Depth"
        }
    }
}

object GazeboEngine {

    data class Params(
        val span: Double,         // Total Span (Incircle Diameter Z)
        val ridgeSpan: Double,    // Optional Ridge Span
        val numSides: Int,        // Number of sides (e.g., 8)
        val pitch: Double,        // Roof Pitch (/12)
        val overhang: Double,     // Overhang in inches
        val heel: Double,         // Heel height in inches
        val joistOC: Double = 16.0
    )

    data class Result(
        val sideLen: Double,
        val apothem: Double,      // 'b' in diagram
        val halfSide: Double,     // 'a' in diagram
        val radius: Double,       // 'c' (Circumradius)
        val ridgeLen: Double,
        val runRafter: Double,
        val verticalRise: Double,
        val mainRafterLen: Double,
        val hipRafterLen: Double,
        val overallMainLen: Double,
        val overallHipLen: Double,
        val hipPitch: Double,
        val angleRad: Double
    )

    fun calculate(p: Params): Result {
        val angleA = (360.0 / p.numSides) / 2.0
        val aRad = angleA * (PI / 180.0)

        // Footprint Geometry
        val b_layout = 0.5 * p.span
        val a_layout = tan(aRad) * b_layout
        val c_layout = b_layout / cos(aRad)
        val sideLen = a_layout * 2.0

        // Ridge Geometry
        val b_ridge = 0.5 * p.ridgeSpan
        val a_ridge = tan(aRad) * b_ridge
        val c_ridge = b_ridge / cos(aRad)

        // Rafter Math
        val run_rafter = b_layout - b_ridge
        val diagFactor = sqrt(p.pitch.pow(2) + 144.0)
        val verticalRise = run_rafter * (p.pitch / 12.0)

        val rafterToWall = (diagFactor / 12.0) * run_rafter
        val rafterTail = (diagFactor / 12.0) * p.overhang

        // Hip Math
        val run_hip = c_layout - c_ridge
        val hipPitch = if (run_hip != 0.0) (verticalRise / (run_hip / 12.0)) else 0.0
        val hipDiagFactor = sqrt(hipPitch.pow(2) + 144.0)
        val hipRafterToWall = (hipDiagFactor / 12.0) * run_hip
        val hipTailRun = p.overhang / cos(aRad)
        val hipTail = (hipDiagFactor / 12.0) * hipTailRun

        return Result(
            sideLen = sideLen,
            apothem = b_layout,
            halfSide = a_layout,
            radius = c_layout,
            ridgeLen = a_ridge * 2.0,
            runRafter = run_rafter,
            verticalRise = verticalRise,
            mainRafterLen = rafterToWall,
            hipRafterLen = hipRafterToWall,
            overallMainLen = rafterToWall + rafterTail,
            overallHipLen = hipRafterToWall + hipTail,
            hipPitch = hipPitch,
            angleRad = aRad
        )
    }
}

object CircularStairEngine {
    data class CircStairResult(
        val riserCount: Int,
        val actualRise: Double,
        val treadAngle: Double,
        val totalRotation: Double,
        val innerTreadWidth: Double,
        val outerTreadWidth: Double,
        val walkLineTread: Double,
        val totalRun: Double
    )

    fun calculate(
        totRise: Double,
        radius: Double,
        innerRadius: Double,
        rotationDeg: Double,
        desRise: Double = 7.5
    ): CircStairResult {
        val riserCount = ceil(totRise / desRise).toInt().coerceAtLeast(1)
        val actualRise = totRise / riserCount
        val treadCount = (riserCount - 1).coerceAtLeast(1)
        
        val treadAngle = rotationDeg / treadCount
        
        // Walk line is typically 12" from the outer edge or at a specific offset
        val walkLineRadius = radius - 12.0
        
        val innerTread = (2 * PI * innerRadius) * (treadAngle / 360.0)
        val outerTread = (2 * PI * radius) * (treadAngle / 360.0)
        val walkTread = (2 * PI * walkLineRadius) * (treadAngle / 360.0)
        
        val totalRun = (2 * PI * walkLineRadius) * (rotationDeg / 360.0)
        
        return CircStairResult(
            riserCount, actualRise, treadAngle, rotationDeg,
            innerTread, outerTread, walkTread, totalRun
        )
    }
}

object TrigEngine {
    data class RightTriangleResult(
        val sideA: Double,
        val sideB: Double,
        val hypotenuse: Double,
        val angleA: Double,
        val angleB: Double
    )

    fun solveRight(a: Double? = null, b: Double? = null, c: Double? = null, angleA: Double? = null): RightTriangleResult {
        var ra = a ?: 0.0; var rb = b ?: 0.0; var rc = c ?: 0.0
        var angA = angleA ?: 0.0; var angB = 0.0

        if (ra > 0 && rb > 0) {
            rc = sqrt(ra.pow(2) + rb.pow(2))
            angA = atan(ra / rb) * (180.0 / PI)
        } else if (ra > 0 && rc > 0) {
            rb = sqrt(rc.pow(2) - ra.pow(2))
            angA = asin(ra / rc) * (180.0 / PI)
        } else if (rb > 0 && rc > 0) {
            ra = sqrt(rc.pow(2) - rb.pow(2))
            angA = acos(rb / rc) * (180.0 / PI)
        } else if (ra > 0 && angA > 0) {
            val rad = angA * (PI / 180.0)
            rc = ra / sin(rad)
            rb = ra / tan(rad)
        } else if (rc > 0 && angA > 0) {
            val rad = angA * (PI / 180.0)
            ra = rc * sin(rad)
            rb = rc * cos(rad)
        }

        angB = 90.0 - angA
        return RightTriangleResult(ra, rb, rc, angA, angB)
    }
}

object MathEngine {
    fun evaluate(expression: String, xValue: Double = 0.0): Double {
        val tokens = tokenize(expression.lowercase().replace("pi", PI.toString()).replace("e", E.toString()).replace("x", "($xValue)"))
        return try {
            Parser(tokens).parse()
        } catch (e: Exception) {
            Double.NaN
        }
    }

    private fun tokenize(input: String): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                c.isWhitespace() -> i++
                c in "+-*/^()" -> {
                    result.add(c.toString())
                    i++
                }
                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    while (i < input.length && (input[i].isDigit() || input[i] == '.')) {
                        sb.append(input[i])
                        i++
                    }
                    result.add(sb.toString())
                }
                c.isLetter() -> {
                    val sb = StringBuilder()
                    while (i < input.length && input[i].isLetter()) {
                        sb.append(input[i])
                        i++
                    }
                    result.add(sb.toString())
                }
                else -> i++
            }
        }
        return result
    }

    private class Parser(val tokens: List<String>) {
        private var pos = 0

        fun parse(): Double = parseExpression()

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                if (eat("+")) x += parseTerm()
                else if (eat("-")) x -= parseTerm()
                else return x
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat("*")) x *= parseFactor()
                else if (eat("/")) x /= parseFactor()
                else return x
            }
        }

        private fun parseFactor(): Double {
            if (eat("+")) return parseFactor()
            if (eat("-")) return -parseFactor()

            var x: Double
            if (eat("(")) {
                x = parseExpression()
                eat(")")
            } else if (isNumber(peek())) {
                x = eat().toDouble()
            } else {
                val func = eat()
                x = parseFactor()
                x = when (func) {
                    "sin" -> sin(x)
                    "cos" -> cos(x)
                    "tan" -> tan(x)
                    "sqrt" -> sqrt(x)
                    "log" -> log(x, 10.0)
                    "ln" -> ln(x)
                    "abs" -> abs(x)
                    else -> 0.0
                }
            }

            if (eat("^")) x = x.pow(parseFactor())
            return x
        }

        private fun peek() = if (pos < tokens.size) tokens[pos] else ""
        private fun eat() = tokens[pos++]
        private fun eat(s: String): Boolean {
            if (peek() == s) {
                pos++
                return true
            }
            return false
        }
        private fun isNumber(s: String) = s.isNotEmpty() && (s[0].isDigit() || (s.length > 1 && s[0] == '-' && s[1].isDigit()))
    }
}
