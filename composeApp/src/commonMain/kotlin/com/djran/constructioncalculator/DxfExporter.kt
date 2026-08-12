package com.djran.constructioncalculator

import kotlin.math.*

object DxfExporter {

    fun generateDxf(
        stairList: List<StairEntry> = emptyList(),
        rafterList: List<RafterEntry> = emptyList(),
        floorList: List<FloorEntry> = emptyList(),
        coordList: List<CoordinatePoint> = emptyList(),
        pineLineList: List<PineLineEntry> = emptyList(),
        circStairList: List<CircularStairEntry> = emptyList()
    ): String {
        val sb = StringBuilder()
        
        // 1. DXF Header
        sb.append("0\nSECTION\n2\nENTITIES\n")

        var xOffset = 0.0

        // 2. Export Stairs
        stairList.forEach { stair ->
            addStairToDxf(sb, stair, xOffset, 0.0)
            xOffset += (FractionUtils.parse(stair.totalRun) + 100.0).coerceAtLeast(200.0)
        }

        // 2b. Export Circular Stairs
        circStairList.forEach { circ ->
            addCircStairToDxf(sb, circ, xOffset, 0.0)
            val outerRadius = FractionUtils.parse(circ.radius) + FractionUtils.parse(circ.walkTread)
            xOffset += (outerRadius * 2 + 100.0)
        }

        // 3. Export Rafters
        xOffset = 0.0
        val yOffsetRafters = -500.0
        rafterList.forEach { rafter ->
            addRafterToDxf(sb, rafter, xOffset, yOffsetRafters)
            xOffset += (FractionUtils.parse(rafter.span) + 100.0).coerceAtLeast(300.0)
        }

        // 4. Export Floors
        xOffset = 0.0
        val yOffsetFloors = -1000.0
        floorList.forEach { floor ->
            addFloorToDxf(sb, floor, xOffset, yOffsetFloors)
            xOffset += (FractionUtils.parse(floor.length) + 100.0).coerceAtLeast(400.0)
        }

        // 5. Export Coordinates
        if (coordList.isNotEmpty()) {
            addCoordinatesToDxf(sb, coordList)
        }

        // 6. Export Pine Line / Eaves
        xOffset = 0.0
        val yOffsetEaves = -1500.0
        pineLineList.forEach { eave ->
            addPineLineToDxf(sb, eave, xOffset, yOffsetEaves)
            xOffset += 200.0
        }

        // 7. DXF Footer
        sb.append("0\nENDSEC\n0\nEOF\n")
        return sb.toString()
    }

    private fun addStairToDxf(sb: StringBuilder, stair: StairEntry, ox: Double, oy: Double) {
        val riserCount = stair.riserCount.toIntOrNull() ?: 0
        val sRise = FractionUtils.parse(stair.actualRise)
        val sRun = FractionUtils.parse(stair.stringerRun)
        val fThick = 11.875

        var curX = ox
        var curY = oy
        for (i in 0 until riserCount) {
            val nextY = curY + sRise
            addLine(sb, curX, curY, curX, nextY, "LUMBER", 7) // Riser
            val nextX = curX + sRun
            addLine(sb, curX, nextY, nextX, nextY, "LUMBER", 7) // Tread
            curX = nextX
            curY = nextY
        }

        addLine(sb, curX, curY, curX + 48.0, curY, "LUMBER", 7)
        addLine(sb, curX, curY - fThick, curX + 48.0, curY - fThick, "LUMBER", 7)
        addLine(sb, curX, curY, curX, curY - fThick, "LUMBER", 7)

        addLine(sb, ox, oy, ox + sRun, oy, "LUMBER", 7)
        addLine(sb, ox + sRun, oy, curX, curY - fThick, "LUMBER", 7)
    }

    private fun addRafterToDxf(sb: StringBuilder, rafter: RafterEntry, ox: Double, oy: Double) {
        val run = FractionUtils.parse(rafter.commonRun)
        val rise = FractionUtils.parse(rafter.commonRise)
        val pitch = FractionUtils.parse(rafter.pitch)
        val heel = FractionUtils.parse(rafter.heel)
        val overhang = FractionUtils.parse(rafter.overhang).takeIf { it > 0 } ?: 12.0
        
        val angle = atan(pitch / 12.0)
        val cosA = cos(angle)
        val slope = pitch / 12.0

        val lumberDepth = getActualDepth(rafter.depth)
        val plumbDepth = lumberDepth / cosA

        val xRidge = ox + run
        val xWall = ox
        val xTail = ox - overhang
        
        val yTopWall = oy + heel
        val yTopRidge = yTopWall + rise
        val yTopTail = yTopWall - (overhang * slope)

        val yBotRidge = yTopRidge - plumbDepth
        val yBotWall = yTopWall - plumbDepth
        val yBotTail = yTopTail - plumbDepth
        
        val xSeatIntersect = xWall + (oy - yBotWall) / slope

        addLine(sb, xRidge, yTopRidge, xTail, yTopTail, "LUMBER", 7)
        addLine(sb, xTail, yTopTail, xTail, yBotTail, "LUMBER", 7)
        addLine(sb, xTail, yBotTail, xSeatIntersect, oy, "LUMBER", 7)
        addLine(sb, xSeatIntersect, oy, xWall, oy, "LUMBER", 7)
        addLine(sb, xWall, oy, xWall, yBotWall, "LUMBER", 7)
        addLine(sb, xWall, yBotWall, xRidge, yBotRidge, "LUMBER", 7)
        addLine(sb, xRidge, yBotRidge, xRidge, yTopRidge, "LUMBER", 7)

        addLine(sb, xWall, oy, xRidge, yTopRidge, "DIMENSIONS", 5)
    }

    private fun addFloorToDxf(sb: StringBuilder, floor: FloorEntry, ox: Double, oy: Double) {
        val len = FractionUtils.parse(floor.length)
        val wid = FractionUtils.parse(floor.width)
        val oc = FractionUtils.parse(floor.spacing).coerceAtLeast(1.0)

        // Perimeter
        addLine(sb, ox, oy, ox + len, oy, "LUMBER", 7)
        addLine(sb, ox + len, oy, ox + len, oy + wid, "LUMBER", 7)
        addLine(sb, ox + len, oy + wid, ox, oy + wid, "LUMBER", 7)
        addLine(sb, ox, oy + wid, ox, oy, "LUMBER", 7)

        // Joists
        var curY = oy + oc
        while (curY < oy + wid) {
            addLine(sb, ox, curY, ox + len, curY, "LUMBER", 7)
            curY += oc
        }
    }

    private fun addCoordinatesToDxf(sb: StringBuilder, list: List<CoordinatePoint>) {
        list.forEachIndexed { index, pt ->
            val n = pt.n
            val e = pt.e
            
            // Draw Point Marker (Cross)
            addLine(sb, e - 1.0, n, e + 1.0, n, "POINTS", 7)
            addLine(sb, e, n - 1.0, e, n + 1.0, "POINTS", 7)
            
            // Draw Label
            addText(sb, e + 1.5, n + 1.5, "${index + 1}: ${pt.description}", "POINTS", 7)

            // Draw connecting line to next point
            if (index < list.size - 1) {
                val nextPt = list[index + 1]
                addLine(sb, e, n, nextPt.e, nextPt.n, "SURVEY", 7)
            }
        }
    }

    private fun addPineLineToDxf(sb: StringBuilder, eave: PineLineEntry, ox: Double, oy: Double) {
        val pitch = FractionUtils.parse(eave.pitch)
        val heel = FractionUtils.parse(eave.heel)
        val overhang = FractionUtils.parse(eave.overhang)
        val wall = FractionUtils.parse(eave.wall)
        val fHeight = FractionUtils.parse(eave.fascia)
        val fThick = 0.75
        val fzHeight = 5.5
        val fzThick = FractionUtils.parse(eave.frieze)
        val reveal = FractionUtils.parse(eave.reveal)
        val brick = FractionUtils.parse(eave.brick)
        
        val angle = atan(pitch / 12.0)
        val slope = pitch / 12.0
        val cosA = cos(angle)
        val lDepth = 7.25
        val pDepth = lDepth / cosA
        
        val xWallIn = ox
        val xWallOut = ox + wall
        val xExtFace = xWallOut + brick
        val xFriezeFront = xExtFace + fzThick
        val xFasciaOut = xFriezeFront + overhang
        val xFasciaIn = xFasciaOut - fThick
        
        val yPlate = oy
        val yRafterTopAtWallOut = yPlate - heel
        
        fun getTopY(x: Double): Double = yRafterTopAtWallOut + (x - xWallOut) * slope
        fun getBotY(x: Double): Double = getTopY(x) + pDepth
        
        val yFasciaTop = getTopY(xFasciaOut)
        val yFasciaBot = yFasciaTop + fHeight
        val ySoffit = yFasciaBot - reveal
        val yPineLine = ySoffit
        
        // 1. Draw Rafter
        val xAttic = xWallIn - 10.0
        addLine(sb, xAttic, getTopY(xAttic), xFasciaOut, yFasciaTop, "RAFTER", 7)
        addLine(sb, xFasciaOut, yFasciaTop, xFasciaOut, yPineLine, "RAFTER", 7)
        addLine(sb, xFasciaOut, yPineLine, xWallOut, yPineLine, "RAFTER", 7)
        
        addLine(sb, xWallOut, yPineLine, xWallOut, yPlate, "RAFTER", 7)
        addLine(sb, xWallOut, yPlate, xWallIn, yPlate, "RAFTER", 7)
        addLine(sb, xWallIn, yPlate, xWallIn, getBotY(xWallIn), "RAFTER", 7)
        addLine(sb, xWallIn, getBotY(xWallIn), xAttic, getBotY(xAttic), "RAFTER", 7)
        addLine(sb, xAttic, getBotY(xAttic), xAttic, getTopY(xAttic), "RAFTER", 7)
        
        // 2. Draw Wall & Plates
        addLine(sb, xWallIn, yPlate, xWallOut, yPlate, "WALL", 7)
        addLine(sb, xWallIn, yPlate + 3.0, xWallOut, yPlate + 3.0, "WALL", 7)
        addLine(sb, xWallIn, yPlate, xWallIn, yPlate - 24.0, "WALL", 7)
        addLine(sb, xWallOut, yPlate, xWallOut, yPlate - 24.0, "WALL", 7)
        
        // 3. Draw Fascia
        addLine(sb, xFasciaOut, yFasciaTop, xFasciaOut, yFasciaBot, "FASCIA", 7)
        addLine(sb, xFasciaOut, yFasciaBot, xFasciaIn, yFasciaBot, "FASCIA", 7)
        addLine(sb, xFasciaIn, yFasciaBot, xFasciaIn, yFasciaTop, "FASCIA", 7)
        
        // 4. Draw Frieze & Soffit
        addLine(sb, xFriezeFront, ySoffit, xFasciaIn, ySoffit, "SOFFIT", 7)
        addLine(sb, xExtFace, yPineLine, xFriezeFront, yPineLine, "FRIEZE", 7)
        addLine(sb, xFriezeFront, yPineLine, xFriezeFront, yPineLine + fzHeight, "FRIEZE", 7)
        addLine(sb, xFriezeFront, yPineLine + fzHeight, xExtFace, yPineLine + fzHeight, "FRIEZE", 7)
        addLine(sb, xExtFace, yPineLine + fzHeight, xExtFace, yPineLine, "FRIEZE", 7)
        
        // 5. Pine Line
        addLine(sb, xWallIn - 6.0, yPineLine, xFasciaOut + 6.0, yPineLine, "PINELINE", 1)
    }

    private fun addCircStairToDxf(sb: StringBuilder, circ: CircularStairEntry, ox: Double, oy: Double) {
        val innerR = FractionUtils.parse(circ.radius)
        val tWidth = FractionUtils.parse(circ.walkTread)
        val outerR = innerR + tWidth
        val totalTreads = circ.numTreads.toDoubleOrNull() ?: 8.0
        val wedgeTreads = max(1.0, totalTreads)
        
        val totalSweep = 180.0
        val anglePerTread = totalSweep / wedgeTreads
        
        val centerX = ox
        val centerY = oy
        
        // 1. Draw Arcs
        addArc(sb, centerX, centerY, innerR, 90.0, -totalSweep, "LUMBER", 7)
        addArc(sb, centerX, centerY, outerR, 90.0, -totalSweep, "LUMBER", 7)
        
        // 2. Draw Wedge Treads
        for (i in 0..wedgeTreads.toInt()) {
            val a = (90.0 - i * anglePerTread) * (PI / 180.0)
            val x1 = centerX + innerR * cos(a)
            val y1 = centerY + innerR * sin(a)
            val x2 = centerX + outerR * cos(a)
            val y2 = centerY + outerR * sin(a)
            addLine(sb, x1, y1, x2, y2, "LUMBER", 7)
        }
    }

    private fun addArc(sb: StringBuilder, cx: Double, cy: Double, r: Double, start: Double, sweep: Double, layer: String, color: Int) {
        sb.append("0\nARC\n")
        sb.append("8\n$layer\n")
        sb.append("62\n$color\n")
        sb.append("10\n$cx\n20\n$cy\n30\n0.0\n")
        sb.append("40\n$r\n")
        sb.append("50\n$start\n")
        sb.append("51\n${start + sweep}\n")
    }

    private fun addText(sb: StringBuilder, x: Double, y: Double, text: String, layer: String, color: Int) {
        sb.append("0\nTEXT\n")
        sb.append("8\n$layer\n")
        sb.append("62\n$color\n")
        sb.append("10\n$x\n20\n$y\n30\n0.0\n")
        sb.append("40\n2.5\n")
        sb.append("1\n$text\n")
    }

    private fun addLine(sb: StringBuilder, x1: Double, y1: Double, x2: Double, y2: Double, layer: String, color: Int) {
        sb.append("0\nLINE\n")
        sb.append("8\n$layer\n")
        sb.append("62\n$color\n")
        sb.append("10\n$x1\n20\n$y1\n30\n0.0\n")
        sb.append("11\n$x2\n21\n$y2\n31\n0.0\n")
    }

    private fun getActualDepth(lumber: String): Double {
        return when {
            lumber.contains("2x4") -> 3.5
            lumber.contains("2x6") -> 5.5
            lumber.contains("2x8") -> 7.25
            lumber.contains("2x10") -> 9.25
            lumber.contains("2x12") -> 11.25
            else -> 7.25
        }
    }
}
