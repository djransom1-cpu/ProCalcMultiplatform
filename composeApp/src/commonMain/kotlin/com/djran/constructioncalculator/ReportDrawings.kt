package com.djran.constructioncalculator

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/** A drawing for the PDF report: PNG bytes plus its pixel size and a caption. */
class ReportImage(val caption: String, val png: ByteArray, val width: Int, val height: Int)

/** PNG-encodes a bitmap (platform image codecs). */
expect fun ImageBitmap.encodePng(): ByteArray?

/**
 * Renders the same diagrams the Data Sheet shows into images for the PDF report,
 * using the shared DrawScope drawing functions so the two always match.
 */
object ReportDrawings {

    // Drawings are rendered at 2x so they stay sharp in print
    private val reportDensity = Density(2f, 1f)
    private const val WIDTH_DP = 500

    fun render(caption: String, heightDp: Int, insetDp: Int = 16, draw: DrawScope.() -> Unit): ReportImage? {
        val w = (WIDTH_DP * reportDensity.density).toInt()
        val h = (heightDp * reportDensity.density).toInt()
        return try {
            val bitmap = ImageBitmap(w, h)
            CanvasDrawScope().draw(reportDensity, LayoutDirection.Ltr, Canvas(bitmap), Size(w.toFloat(), h.toFloat())) {
                drawRect(Color.White)
                val pad = insetDp * reportDensity.density
                inset(pad) { draw() }
            }
            bitmap.encodePng()?.let { ReportImage(caption, it, w, h) }
        } catch (e: Throwable) {
            println("Report drawing failed for $caption: ${e.message}")
            null
        }
    }

    /** Adds drawings to the sections that have them on the Data Sheet. */
    fun attach(
        sections: List<SummarySection>,
        stairEntries: List<StairEntry>,
        rafterEntries: List<RafterEntry>,
        gazeboEntries: List<GazeboEntry>,
        arcEntries: List<ArcEntry>,
        floorEntries: List<FloorEntry>,
        measurer: TextMeasurer
    ): List<SummarySection> = sections.map { section ->
        val images: List<ReportImage> = when (section.title) {
            "STAIR SCHEDULE" -> stairEntries.mapNotNull { e ->
                render(e.name, 240) {
                    drawStairDiagram(
                        FractionUtils.parse(e.rise), FractionUtils.parse(e.totalRun),
                        e.riserCount.toIntOrNull() ?: 1, 11.875, 1.0, 0.0,
                        FractionUtils.parse(e.distOut), true
                    )
                }
            }
            "RAFTER SCHEDULE" -> rafterEntries.mapNotNull { e ->
                render(e.name, 260) {
                    drawRafterDetail(
                        FractionUtils.parse(e.commonRun), FractionUtils.parse(e.commonRise),
                        FractionUtils.parse(e.pitch), FractionUtils.parse(e.heel), 7.25,
                        FractionUtils.parse(e.overhang).takeIf { it > 0 } ?: 12.0
                    )
                }
            }
            "GAZEBO SCHEDULE" -> gazeboEntries.flatMap { e ->
                val params = GazeboEngine.Params(
                    FractionUtils.parseFeet(e.diameter), FractionUtils.parse(e.ridgeSpan),
                    e.sides.toIntOrNull() ?: 8, e.pitch.toDoubleOrNull() ?: 0.0,
                    FractionUtils.parse(e.overhang), FractionUtils.parse(e.heel),
                    FractionUtils.parse(e.joistOC).takeIf { it > 0 } ?: 16.0
                )
                if (params.span <= 0 || params.numSides < 3) return@flatMap emptyList()
                val res = GazeboEngine.calculate(params)
                listOf(GazeboMode.LAYOUT to "layout", GazeboMode.RAFTER to "rafter", GazeboMode.FLOOR to "floor").mapNotNull { (mode, label) ->
                    render("${e.name} – $label", 300) { drawGazeboDiagram(mode, params, res) }
                }
            }
            "ARC SCHEDULE" -> arcEntries.mapNotNull { e ->
                val sol = e.solution() ?: return@mapNotNull null
                render(e.name, 280, insetDp = 12) { drawArcDiagram(sol, ArcMath.offsetTable(sol, e.stationCount()), measurer) }
            }
            "FLOORING SCHEDULE" -> floorEntries.mapNotNull { e ->
                render(e.name, 220) {
                    drawFloorDiagram(
                        FractionUtils.parseFeet(e.length), FractionUtils.parseFeet(e.width),
                        FractionUtils.parse(e.spacing.replace("OC", "")).takeIf { it > 0 } ?: 16.0,
                        e.beams.toIntOrNull() ?: 0, e.direction
                    )
                }
            }
            else -> emptyList()
        }
        if (images.isEmpty()) section else section.copy(images = images)
    }
}

/** The arc a saved entry describes (its chord and spring are stored as entered). */
fun ArcEntry.solution(): ArcMath.Solution? =
    ArcMath.solveChordSpring(FractionUtils.parseFeet(chord), FractionUtils.parse(height)).solution

/** Layout stations saved with the entry; older saves use the default. */
fun ArcEntry.stationCount(): Int =
    stations.toIntOrNull()?.takeIf { it in 1..ArcMath.MAX_STATIONS } ?: ArcMath.DEFAULT_STATIONS
