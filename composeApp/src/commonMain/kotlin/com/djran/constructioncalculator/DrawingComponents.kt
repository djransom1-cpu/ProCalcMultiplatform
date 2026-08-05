package com.djran.constructioncalculator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.*
import androidx.compose.runtime.snapshots.SnapshotStateList

object DrawingUtils {
    /** Draws a professional dimension arrow with optional double-heads. */
    fun DrawScope.drawArrow(
        start: Offset,
        end: Offset,
        color: Color = Color.Gray,
        strokeWidth: Float = 2f,
        doubleHeaded: Boolean = true
    ) {
        drawLine(color, start, end, strokeWidth)
        val headLen = 12f
        val angle = atan2(end.y - start.y, end.x - start.x)

        // End head
        drawLine(color, end, Offset(end.x - headLen * cos(angle - PI/6).toFloat(), end.y - headLen * sin(angle - PI/6).toFloat()), strokeWidth)
        drawLine(color, end, Offset(end.x - headLen * cos(angle + PI/6).toFloat(), end.y - headLen * sin(angle + PI/6).toFloat()), strokeWidth)

        if (doubleHeaded) {
            drawLine(color, start, Offset(start.x + headLen * cos(angle - PI/6).toFloat(), start.y + headLen * sin(angle - PI/6).toFloat()), strokeWidth)
            drawLine(color, start, Offset(start.x + headLen * cos(angle + PI/6).toFloat(), start.y + headLen * sin(angle + PI/6).toFloat()), strokeWidth)
        }
    }
}

@Composable
fun StairDiagram(
    totalRise: Double,
    totalRun: Double,
    riserCount: Int,
    floorThickness: Double,
    nosing: Double,
    treadsOut: Double,
    distOut: Double,
    showOpening: Boolean = true
) {
    Canvas(modifier = Modifier.fillMaxWidth().height(240.dp).background(Color.White).padding(16.dp)) {
        if (totalRise <= 0 || riserCount <= 0) return@Canvas

        val w = size.width
        val h = size.height

        val leftMargin = 60f
        val rightMargin = 80f
        val topMargin = 40f
        val bottomMargin = 40f

        val drawW = (w - leftMargin - rightMargin).coerceAtLeast(10f)
        val drawH = (h - topMargin - bottomMargin).coerceAtLeast(10f)

        val actualNumRises = riserCount.coerceAtLeast(1)
        val actualRiseInches = totalRise / actualNumRises
        val actualRunInches = if (actualNumRises > 1) (totalRun / (actualNumRises - 1)).coerceAtLeast(1.0) else 10.0
        
        val scaleX = drawW / totalRun.toFloat().coerceAtLeast(1f)
        val scaleY = drawH / totalRise.toFloat().coerceAtLeast(1f)
        val scale = min(scaleX, scaleY)

        val sw = (actualRunInches * scale).toFloat()
        val sh = (actualRiseInches * scale).toFloat()
        val sf = (floorThickness * scale).toFloat()

        val groundY = h - bottomMargin
        val startX = leftMargin + 20f
        val topFloorY = groundY - (totalRise * scale).toFloat()
        val bottomFloorY = topFloorY + sf
        val headerX = startX + (actualNumRises - 1) * sw

        // 1. Draw Floor Header Box
        drawRect(Color.LightGray, Offset(headerX, topFloorY), Size(w - headerX - 10f, sf))
        drawRect(Color.Black, Offset(headerX, topFloorY), Size(w - headerX - 10f, sf), style = Stroke(2f))

        // 2. Draw Stair Tread / Riser Steps Path
        val topPath = Path()
        val bottomPath = Path()

        topPath.moveTo(startX, groundY)
        for (i in 0 until actualNumRises) {
            val curX = startX + i * sw
            val nextX = startX + (i + 1) * sw
            val nextY = groundY - (i + 1) * sh

            topPath.lineTo(curX, nextY) // Vertical Riser
            if (i < actualNumRises - 1) {
                topPath.lineTo(nextX, nextY) // Horizontal Tread
            }
        }
        topPath.lineTo(headerX, topFloorY)

        // Stringer Bottom Line
        val botBackX = startX + sw
        bottomPath.moveTo(startX, groundY)
        bottomPath.lineTo(botBackX, groundY)
        bottomPath.lineTo(headerX, bottomFloorY)

        drawPath(topPath, Color.Black, style = Stroke(3f))
        drawPath(bottomPath, BlueTool, style = Stroke(3f))

        // 3. Headroom Line (81" clearance line)
        if (showOpening) {
            val hrPx = (81.0 * scale).toFloat()
            drawLine(
                color = Color.Red,
                start = Offset(headerX, topFloorY + sf),
                end = Offset(headerX, (topFloorY + sf + hrPx).coerceAtMost(groundY)),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
        }
    }
}

@Composable
fun RafterDetail(
    run: Double,
    rise: Double,
    pitch: Double,
    heel: Double, // HAP
    lumberDepth: Double = 7.25,
    overhang: Double = 12.0
) {
    Canvas(modifier = Modifier.fillMaxWidth().height(260.dp).background(Color.White).padding(16.dp)) {
        if (run <= 0 || rise <= 0) return@Canvas

        val w = size.width
        val h = size.height

        val leftMargin = 120f
        val rightMargin = 80f
        val topMargin = 40f
        val bottomMargin = 40f

        val drawW = (w - leftMargin - rightMargin).coerceAtLeast(10f)
        val drawH = (h - topMargin - bottomMargin).coerceAtLeast(10f)

        val angleRad = atan(pitch / 12.0)
        val cosA = cos(angleRad).toFloat()
        val slope = (pitch / 12.0).toFloat()

        val actualDepthIn = lumberDepth
        val ridgeDepthIn = lumberDepth + 2.0

        val totalRunIn = run + overhang + 12.0
        val totalRiseIn = rise + ridgeDepthIn + 12.0
        val scale = min(drawW / totalRunIn.toFloat(), drawH / totalRiseIn.toFloat())

        val sRun = (run * scale).toFloat()
        val sRise = (rise * scale).toFloat()
        val sHeel = (heel * scale).toFloat()
        val sOverhang = (overhang * scale).toFloat()
        val sDepth = (actualDepthIn * scale).toFloat()
        val sRidgeDepth = (ridgeDepthIn * scale).toFloat()
        val sPlumbDepth = sDepth / cosA

        val ridgeFaceX = w - rightMargin
        val wallLineX = ridgeFaceX - sRun
        val tailEndX = wallLineX - sOverhang

        val topPeakY = topMargin + (sRidgeDepth * 0.2f)
        val topAtWallY = topPeakY + sRise
        val topAtTailY = topAtWallY + (sOverhang * slope)

        val botAtRidgeY = topPeakY + sPlumbDepth
        val botAtWallY = topAtWallY + sPlumbDepth
        val botAtTailY = topAtTailY + sPlumbDepth

        val cornerY = topAtWallY + sHeel
        val x_seatIntersect = wallLineX - (cornerY - botAtWallY) / slope

        // 1. Ridge Board
        drawRect(
            color = Color(0xFFD32F2F),
            topLeft = Offset(ridgeFaceX, topPeakY),
            size = Size(20f, sRidgeDepth),
            style = Stroke(3f)
        )

        // 2. Rafter Member Path
        val rafterPath = Path()
        rafterPath.moveTo(ridgeFaceX, topPeakY)
        rafterPath.lineTo(tailEndX, topAtTailY)
        rafterPath.lineTo(tailEndX, botAtTailY)
        rafterPath.lineTo(x_seatIntersect, botAtWallY + slope * (wallLineX - x_seatIntersect))
        rafterPath.lineTo(x_seatIntersect, cornerY)
        rafterPath.lineTo(wallLineX, cornerY)
        rafterPath.lineTo(wallLineX, botAtWallY)
        rafterPath.lineTo(ridgeFaceX, botAtRidgeY)
        rafterPath.close()

        drawPath(rafterPath, BlueTool, style = Stroke(4f))

        // 3. Dashed Theory Length Line
        drawLine(
            color = Color.Red,
            start = Offset(wallLineX, cornerY),
            end = Offset(ridgeFaceX, topPeakY),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )

        // 4. Pitch Triangle
        val triSize = 40f
        val triX = (wallLineX + ridgeFaceX) / 2f
        val triY = (topAtWallY + topPeakY) / 2f
        drawLine(Color.Gray, Offset(triX, triY), Offset(triX + triSize, triY))
        drawLine(Color.Gray, Offset(triX + triSize, triY), Offset(triX + triSize, triY - (triSize * slope)))
    }
}

@Composable
fun FloorDiagram(
    floorLength: Double,
    floorWidth: Double,
    joistOC: Double = 16.0,
    numBeams: Int = 0,
    spanDirection: String = "Span Length"
) {
    Canvas(modifier = Modifier.fillMaxWidth().height(220.dp).background(Color.White).padding(16.dp)) {
        if (floorLength <= 0 || floorWidth <= 0) return@Canvas

        val w = size.width
        val h = size.height

        val margin = 50f
        val drawW = (w - margin * 2).coerceAtLeast(10f)
        val drawH = (h - margin * 2).coerceAtLeast(10f)

        val scale = min(drawW / floorLength.toFloat(), drawH / floorWidth.toFloat())

        val sLen = (floorLength * scale).toFloat()
        val sWid = (floorWidth * scale).toFloat()

        val startX = (w - sLen) / 2f
        val startY = (h - sWid) / 2f

        // 1. Draw Perimeter (Rim Joists)
        drawRect(
            color = Color.Red,
            topLeft = Offset(startX, startY),
            size = Size(sLen, sWid),
            style = Stroke(3f)
        )

        // 2. Draw Beams if any
        if (numBeams > 0) {
            val beamSpacing = if (spanDirection == "Span Length") sLen / (numBeams + 1) else sWid / (numBeams + 1)
            for (i in 1..numBeams) {
                if (spanDirection == "Span Length") {
                    val bx = startX + i * beamSpacing
                    drawLine(Color.Red, Offset(bx, startY), Offset(bx, startY + sWid), strokeWidth = 4f)
                } else {
                    val by = startY + i * beamSpacing
                    drawLine(Color.Red, Offset(startX, by), Offset(startX + sLen, by), strokeWidth = 4f)
                }
            }
        }

        // 3. Draw Joists
        val sOC = (joistOC * scale).toFloat().coerceAtLeast(4f)
        if (spanDirection == "Span Length") {
            var curY = startY + sOC
            while (curY < startY + sWid) {
                drawLine(Color.Red, Offset(startX, curY), Offset(startX + sLen, curY), strokeWidth = 2f)
                curY += sOC
            }
        } else {
            var curX = startX + sOC
            while (curX < startX + sLen) {
                drawLine(Color.Red, Offset(curX, startY), Offset(curX, startY + sWid), strokeWidth = 2f)
                curX += sOC
            }
        }
    }
}

@Composable
fun FootingCrossSection(
    width: Double,
    thickness: Double,
    rebarMode: Int = 0, // 0 = Grid, 1 = Continuous (Top & Bottom)
    rebarOC: Double = 12.0,
    barsTop: Int = 0,
    barsBottom: Int = 2
) {
    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp).background(Color.White).padding(16.dp)) {
        if (width <= 0 || thickness <= 0) return@Canvas

        val scale = min((size.width * 0.8f) / width.toFloat(), (size.height * 0.8f) / thickness.toFloat())
        val wPx = (width * scale).toFloat()
        val tPx = (thickness * scale).toFloat()

        val startX = (size.width - wPx) / 2f
        val startY = (size.height - tPx) / 2f

        // Concrete Cross Section Block
        drawRect(Color(0xFFE0E0E0), Offset(startX, startY), Size(wPx, tPx))
        drawRect(Color.DarkGray, Offset(startX, startY), Size(wPx, tPx), style = Stroke(3f))

        val cover = (3.0 * scale).toFloat().coerceAtMost(tPx * 0.25f)

        if (rebarMode == 0) {
            // GRID REBAR: Bottom mat rebar dots
            val rebarY = startY + tPx - cover
            val rebarCount = max(2.0, width / rebarOC.coerceAtLeast(1.0)).toInt()
            val spacing = (wPx - (2 * cover)) / (rebarCount - 1).coerceAtLeast(1)

            // Main transverse line
            drawLine(Color.Red, Offset(startX + cover, rebarY), Offset(startX + wPx - cover, rebarY), strokeWidth = 3f)

            // Rebar dots
            for (i in 0 until rebarCount) {
                drawCircle(Color.Red, radius = 5f, center = Offset(startX + cover + i * spacing, rebarY))
            }
        } else {
            // CONTINUOUS BARS: Top & Bottom bars
            if (barsTop > 0) {
                val topY = startY + cover
                val topSpacing = if (barsTop > 1) (wPx - (2 * cover)) / (barsTop - 1) else 0f
                for (i in 0 until barsTop) {
                    val cx = if (barsTop == 1) startX + wPx / 2f else startX + cover + i * topSpacing
                    drawCircle(Color.Red, radius = 6f, center = Offset(cx, topY))
                }
            }
            if (barsBottom > 0) {
                val botY = startY + tPx - cover
                val botSpacing = if (barsBottom > 1) (wPx - (2 * cover)) / (barsBottom - 1) else 0f
                for (i in 0 until barsBottom) {
                    val cx = if (barsBottom == 1) startX + wPx / 2f else startX + cover + i * botSpacing
                    drawCircle(Color.Red, radius = 6f, center = Offset(cx, botY))
                }
            }
        }
    }
}


@Composable
fun SketchCanvas(
    modifier: Modifier = Modifier,
    paths: SnapshotStateList<Pair<Path, Color>>,
    onRawPathAdded: (List<Offset>) -> Unit = { _ -> }
) {
    // Local path being actively drawn
    var currentPath by remember { mutableStateOf(Path()) }
    val currentPoints = remember { mutableStateListOf<Offset>() }
    // Separate trigger for draw block
    var drawTrigger by remember { mutableStateOf(0) }

    val currentOnRawPathAdded by rememberUpdatedState(onRawPathAdded)

    Canvas(modifier = modifier
        .fillMaxSize()
        .background(Color.White)
        .pointerInput(paths) {
            detectDragGestures(
                onDragStart = { offset ->
                    currentPoints.clear()
                    currentPoints.add(offset)
                    val newPath = Path().apply { moveTo(offset.x, offset.y) }
                    currentPath = newPath
                    drawTrigger++
                },
                onDrag = { change, _ ->
                    val p = change.position
                    currentPoints.add(p)
                    
                    // Update the path object in place but force a redraw
                    currentPath.lineTo(p.x, p.y)
                    drawTrigger++
                    change.consume()
                },
                onDragEnd = {
                    if (currentPoints.isNotEmpty()) {
                        // Add to shared state list
                        paths.add(currentPath to Color.Black)
                        // Save to persistence
                        currentOnRawPathAdded(currentPoints.toList())
                    }
                    // Reset local path
                    currentPath = Path()
                    currentPoints.clear()
                    drawTrigger++
                }
            )
        }
    ) {
        // Observe drawTrigger to invalidate the canvas
        val _unused = drawTrigger
        
        // Draw existing paths
        paths.forEach { entry ->
            drawPath(
                path = entry.first,
                color = entry.second,
                style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        
        // Draw path currently being dragged
        if (currentPoints.isNotEmpty()) {
            drawPath(
                path = currentPath,
                color = Color.Black,
                style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
fun GraphPlotter(
    function: String,
    scale: Float = 40f,
    plotColor: Color = Color.Red
) {
    Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFFB9C4B1))) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // 1. Draw Grid
        val gridColor = Color.Gray.copy(alpha = 0.3f)
        var xGrid = cx % scale
        while (xGrid < w) {
            drawLine(gridColor, Offset(xGrid, 0f), Offset(xGrid, h), strokeWidth = 1f)
            xGrid += scale
        }
        var yGrid = cy % scale
        while (yGrid < h) {
            drawLine(gridColor, Offset(0f, yGrid), Offset(w, yGrid), strokeWidth = 1f)
            yGrid += scale
        }

        // 2. Draw Axes
        drawLine(Color.Black, Offset(cx, 0f), Offset(cx, h), strokeWidth = 2f)
        drawLine(Color.Black, Offset(0f, cy), Offset(w, cy), strokeWidth = 2f)

        // 3. Plot Function
        if (function.isNotBlank()) {
            val path = Path()
            var isFirstPoint = true

            for (pixelX in 0..w.toInt() step 2) {
                val mathX = (pixelX - cx) / scale
                val mathY = MathEngine.evaluate(function, mathX.toDouble())

                if (!mathY.isNaN() && mathY.isFinite()) {
                    val pixelY = cy - (mathY.toFloat() * scale)
                    
                    if (pixelY in -100f..(h + 100f)) {
                        if (isFirstPoint) {
                            path.moveTo(pixelX.toFloat(), pixelY)
                            isFirstPoint = false
                        } else {
                            path.lineTo(pixelX.toFloat(), pixelY)
                        }
                    } else {
                        isFirstPoint = true
                    }
                } else {
                    isFirstPoint = true
                }
            }
            drawPath(path, color = plotColor, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

enum class GazeboMode { LAYOUT, RAFTER, FLOOR }

@Composable
fun GazeboDiagram(
    mode: GazeboMode,
    params: GazeboEngine.Params,
    res: GazeboEngine.Result
) {
    Canvas(modifier = Modifier.fillMaxWidth().height(300.dp).background(Color.White).padding(16.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        when (mode) {
            GazeboMode.LAYOUT -> drawLayoutPlan(cx, cy, params, res)
            GazeboMode.RAFTER -> drawRafterFraming(cx, cy, params, res)
            GazeboMode.FLOOR -> drawFloorJoists(cx, cy, params, res)
        }
    }
}

private fun DrawScope.drawLayoutPlan(cx: Float, cy: Float, p: GazeboEngine.Params, r: GazeboEngine.Result) {
    val scale = (min(size.width, size.height) * 0.8f) / p.span.toFloat()
    val sR = (r.apothem * scale).toFloat()
    val sA = (r.halfSide * scale).toFloat()

    // Draw Reference Grid (Dashed)
    drawRect(
        color = Color.LightGray, 
        topLeft = Offset(cx - sR, cy - sR), 
        size = Size(sR * 2, sR * 2), 
        style = Stroke(1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
    )

    // Draw Polygon
    val polyPath = Path()
    val angleStep = 2.0 * PI / p.numSides
    for (i in 0 until p.numSides) {
        val angle = i * angleStep - PI / 2.0
        val px = cx + (r.radius * scale * cos(angle)).toFloat()
        val py = cy + (r.radius * scale * sin(angle)).toFloat()
        if (i == 0) polyPath.moveTo(px, py) else polyPath.lineTo(px, py)
    }
    polyPath.close()
    drawPath(polyPath, Color.Black, style = Stroke(3f))

    // Draw Theory Triangles (A, B, C)
    val firstVertexAngle = -PI / 2.0
    val vx = cx + (r.radius * scale * cos(firstVertexAngle)).toFloat()
    val vy = cy + (r.radius * scale * sin(firstVertexAngle)).toFloat()
    
    drawLine(Color.Red, Offset(cx, cy), Offset(cx, cy - sR), strokeWidth = 2f) // Base 'b'
    drawLine(Color.Blue, Offset(cx, cy), Offset(vx, vy), strokeWidth = 2f) // Radius 'c'
}

private fun DrawScope.drawRafterFraming(cx: Float, cy: Float, p: GazeboEngine.Params, r: GazeboEngine.Result) {
    val outerRadius = r.apothem + p.overhang
    val scale = (min(size.width, size.height) * 0.7f) / (outerRadius * 2).toFloat()

    val angleStep = 2.0 * PI / p.numSides

    // 1. Draw Building Line (Black)
    val buildingPath = Path()
    for (i in 0 until p.numSides) {
        val angle = i * angleStep - PI / 2.0
        val px = cx + (r.radius * scale * cos(angle)).toFloat()
        val py = cy + (r.radius * scale * sin(angle)).toFloat()
        if (i == 0) buildingPath.moveTo(px, py) else buildingPath.lineTo(px, py)
    }
    buildingPath.close()
    drawPath(buildingPath, Color.Black, style = Stroke(2f))

    // 2. Draw Subfascia (Red)
    val fasciaRadius = (outerRadius / cos(r.angleRad))
    val fasciaPath = Path()
    for (i in 0 until p.numSides) {
        val angle = i * angleStep - PI / 2.0
        val px = cx + (fasciaRadius * scale * cos(angle)).toFloat()
        val py = cy + (fasciaRadius * scale * sin(angle)).toFloat()
        if (i == 0) fasciaPath.moveTo(px, py) else fasciaPath.lineTo(px, py)
    }
    fasciaPath.close()
    drawPath(fasciaPath, Color.Red, style = Stroke(2f))

    // 3. Draw Hip Rafters (Red)
    for (i in 0 until p.numSides) {
        val angle = i * angleStep - PI / 2.0
        val startX = cx + (r.ridgeLen / 2.0 / sin(r.angleRad) * scale * cos(angle)).toFloat()
        val startY = cy + (r.ridgeLen / 2.0 / sin(r.angleRad) * scale * sin(angle)).toFloat()
        val endX = cx + (fasciaRadius * scale * cos(angle)).toFloat()
        val endY = cy + (fasciaRadius * scale * sin(angle)).toFloat()
        drawLine(Color.Red, Offset(startX, startY), Offset(endX, endY), strokeWidth = 2f)
    }
}

private fun DrawScope.drawFloorJoists(cx: Float, cy: Float, p: GazeboEngine.Params, r: GazeboEngine.Result) {
    val scale = (min(size.width, size.height) * 0.8f) / p.span.toFloat()
    val sR = (r.apothem * scale).toFloat()
    val sOC = (p.joistOC * scale).toFloat()

    val polyPath = Path()
    val angleStep = 2.0 * PI / p.numSides
    for (i in 0 until p.numSides) {
        val angle = i * angleStep - PI / 2.0
        val px = cx + (r.radius * scale * cos(angle)).toFloat()
        val py = cy + (r.radius * scale * sin(angle)).toFloat()
        if (i == 0) polyPath.moveTo(px, py) else polyPath.lineTo(px, py)
    }
    polyPath.close()

    clipPath(polyPath) {
        var x = cx - sR
        while (x <= cx + sR) {
            drawLine(Color.Blue, Offset(x, cy - sR), Offset(x, cy + sR), strokeWidth = 2f)
            x += sOC
        }
    }
    drawPath(polyPath, Color.Black, style = Stroke(3f))
}
