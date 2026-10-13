package com.djran.constructioncalculator

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import com.russhwolf.settings.get
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlin.math.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import kotlinx.coroutines.launch
import kotlin.math.*

enum class Screen {
    Calculator,
    ConcreteCalculator,
    StairCalculator,
    RafterCalculator,
    ArcCalculator,
    WallCalculator,
    CrownCalculator,
    GazeboCalculator,
    RoofCalculator,
    CoordinateCalculator,
    MasonryCalculator,
    DrywallCalculator,
    DeckCalculator,
    FloorCalculator,
    HandrailCalculator,
    PineLineCalculator,
    CircularStairCalculator,
    TrigCalculator,
    ColumnCalculator,
    FramingCalculator,
    ScientificCalculator,
    GraphingCalculator,
    CalculatorHelp,
    JobNotes,
    ProjectList,
    JobSummary,
    About,
    SquareCalculator,
    GableCutCalculator,
    GradeSlopeCalculator,
    LandSurveyCalculator,
    Settings
}

@Serializable
data class StairEntry(
    val id: String = "",
    val name: String,
    val rise: String,
    val riserCount: String,
    val actualRise: String,
    val treadCount: String,
    val stringerRun: String,
    val totalRun: String,
    val stringerLen: String,
    val angleDeg: String,
    val treadsOut: String,
    val distOut: String,
    val headroom: String = "Clear"
)

@Serializable
data class RafterEntry(
    val id: String = "",
    val name: String,
    val span: String,
    val pitch: String,
    val ridge: String,
    val depth: String,
    val heel: String,
    val overhang: String,
    val commonLen: String,
    val commonOverall: String,
    val commonRun: String,
    val commonRise: String,
    val commonOARise: String,
    val commonPlumbSeat: String,
    val hipLen: String,
    val hipOverall: String,
    val hipRun: String,
    val hipRise: String,
    val hipOARise: String,
    val hipPitch: String,
    val hipPlumbSeat: String,
    val jack16: String,
    val jack24: String
)

@Serializable
data class ArcEntry(
    val id: String = "",
    val name: String,
    val chord: String,
    val height: String,
    val radius: String,
    val arcLength: String,
    val arcAngle: String
)

@Serializable
data class WallEntry(
    val id: String = "",
    val name: String,
    val length: String,
    val height: String,
    val spacing: String,
    val studs: String,
    val plates: String,
    val blocking: String = "0 lin ft",
    val sheets: String
)

@Serializable
data class CrownEntry(
    val id: String = "",
    val name: String,
    val spring: String,
    val wallAngle: String,
    val miter: String,
    val bevel: String
)

@Serializable
data class GazeboEntry(
    val id: String = "",
    val name: String,
    val sides: String,
    val diameter: String,
    val ridgeSpan: String,
    val pitch: String,
    val overhang: String,
    val heel: String,
    val sideLength: String,
    val apothem: String,
    val perimeter: String,
    val interiorAngle: String,
    val miter: String,
    val area: String,
    val verticalRise: String,
    val commonRafterLen: String,
    val commonOverall: String,
    val hipLen: String,
    val hipOverall: String,
    val commonPlumbSeat: String,
    val hipPlumbSeat: String,
    val ridgeOffset: String,
    val joistOC: String = "16"
)

@Serializable
data class ConcreteEntry(
    val id: String = "",
    val name: String,
    val qty: String,
    val volume: String,
    val rebar: String,
    val spacing: String,
    val dimensions: String
)

@Serializable
data class RoofEntry(
    val id: String = "",
    val name: String,
    // Framing takeoff fields, matching master's RoofCalcActivity.
    val span: String = "",
    val bldgLen: String = "",
    val pitch: String = "",
    val leftEnd: Int = 0, // 0 = Gable, 1 = Hip, 2 = Valley
    val rightEnd: Int = 0,
    val commonInfo: String = "",
    val hipInfo: String = "",
    val valleyInfo: String = "",
    val ridgeInfo: String = "",
    val subFascia: String = "",
    // Legacy shingle-coverage fields (the KMP-only version this replaces) - kept with
    // defaults so JobSummary/CSV export code and old locally-saved app state stay compatible.
    val area: String = "",
    val squares: String = "",
    val bundles: String = "",
    val sheets: String = "",
    val dimensions: String = ""
)

@Serializable
data class SquareEntry(
    val id: String = "",
    val name: String,
    val length: String,
    val width: String,
    val diagonal: String
)

@Serializable
data class SurveyEntry(
    val id: String = "",
    val name: String,
    val distance: String,
    val slope: String,
    val slopePercent: String,
    val totalFall: String
)

@Serializable
data class LandSurveyEntry(
    val id: String = "",
    val note: String,
    val data: String
)

@Serializable
data class AppSettingsData(
    // Index into [4, 8, 16, 32, 64] denominators - matches master's precision_options
    // spinner exactly (default index 2 = 1/16").
    val precisionIndex: Int = 2,
    val alwaysFloating: Boolean = false,
    val immersiveMode: Boolean = false
) {
    companion object {
        val DENOMINATORS = listOf(4, 8, 16, 32, 64)
        val LABELS = listOf("1/4\"", "1/8\"", "1/16\"", "1/32\"", "1/64\"")
    }
}

@Serializable
data class MasonryEntry(
    val id: String = "",
    val name: String,
    // Defaults on every field but name/id let old locally-saved app state (from before this
    // shape existed) still decode instead of failing outright - AppState is one JSON blob, so
    // a hard decode failure here would silently wipe every saved project, not just masonry.
    val qty: String = "1",
    val sqFt: String = "0.00 sq ft",
    val type: String = "Block",
    val units: String = "0",
    val mortar: String = "0",
    val sand: String = "0",
    val length: String = "",
    val height: String = ""
)

@Serializable
data class DrywallEntry(
    val id: String = "",
    val name: String,
    val area: String,
    val sheets: String,
    val sheetSize: String = "4x8",
    val mud: String,
    val tape: String,
    val length: String = "",
    val width: String = "",
    val wallHeight: String = "",
    val roomMode: Boolean = false
)

@Serializable
data class DeckEntry(
    val id: String = "",
    val name: String,
    val length: String,
    val width: String,
    val boardWidth: String,
    val gap: String,
    val boards: String,
    val fasteners: String,
    // Total linear feet of decking to purchase (rows x board length) - master's actual
    // purchasable quantity, computed but never surfaced in the original KMP port.
    val totalLF: String = "",
    val joistOC: String = "16",
    val runAlongWidth: Boolean = false
)

@Serializable
data class FloorEntry(
    val id: String = "",
    val name: String,
    val length: String,
    val width: String,
    val spacing: String,
    val joists: String,
    val sheets: String,
    // Master persists rim joist perimeter as its own column; this was computed for the
    // live results card but never actually saved into the entry.
    val rimJoist: String = ""
)

@Serializable
data class HandrailEntry(
    val id: String = "",
    val name: String,
    val length: String,
    val spindleWidth: String,
    val maxOpening: String,
    val spindles: String,
    // True center-to-center (O.C.) spacing = gap + spindle width - what an installer actually
    // marks on the rail. This field used to hold the raw gap mislabeled as "O.C.", which would
    // have set spindles a full spindle-width too close together.
    val spacing: String,
    val gap: String = ""
)

@Serializable
data class PineLineEntry(
    val id: String = "",
    val name: String,
    val pitch: String,
    val heel: String,
    val wall: String,
    val brick: String,
    val frieze: String,
    val overhang: String,
    val fascia: String,
    val reveal: String,
    val drop: String,
    val rafterLength: String = ""
)

@Serializable
data class CoordinatePoint(
    val n: Double,
    val e: Double,
    val elev: Double = 0.0,
    val description: String = ""
)

@Serializable
data class PlottedPath(
    val id: String = "",
    val points: List<CoordinatePoint> = emptyList(),
    val isTakeoff: Boolean = true,
    val isClosed: Boolean = false,
    val arcHeights: List<Double> = emptyList(),
    // Marks this path as a "hole" whose area is subtracted from whichever takeoff
    // path's outline contains it - matches master's MARK HOLE / MARK MAIN toggle.
    val isExclusion: Boolean = false
)

@Serializable
data class CircularStairEntry(
    val id: String = "",
    val name: String,
    val totalRise: String,
    // "radius" is master's own field name for INNER/post radius (confusing, but matching it
    // for parity - the outside radius is a derived result, not an input). "walkTread" holds
    // the tread WIDTH input (outer - inner radius), also master's naming.
    val radius: String,
    val walkTread: String,
    val numTreads: String,
    val straightTreads: String = "0",
    val straightRun: String = "10",
    val nosing: String = "1",
    val circleSize: String = "2/4",
    val outerArcTread: String = "",
    val innerArcTread: String = "",
    val outsideRadius: String = "",
    val anglePerTread: String = ""
)

@Serializable
data class TrigEntry(
    val id: String = "",
    val name: String,
    val sideA: String,
    val sideB: String,
    val hypotenuse: String,
    val angleA: String,
    val angleB: String
)

@Serializable
data class ColumnEntry(
    val id: String = "",
    val name: String,
    val diameter: String,
    val height: String,
    val volume: String,
    val surfaceArea: String,
    val circumference: String,
    val qty: String = "1",
    val rebarFt: String = "0",
    val rebarType: Int = 0,
    val rebarVert: String = "4",
    val rebarTies: String = "12",
    val barsTop: String = "0",
    val barsBottom: String = "0"
)

@Serializable
data class FramingEntry(
    val id: String = "",
    val name: String,
    val wallHeight: String,
    val headerHeightBottom: String,
    val headerDepth: String,
    val roWidth: String,
    val roHeight: String,
    // How many identical copies of this whole opening exist - independent of winCount, which
    // groups multiple windows under one shared header. Master multiplies all material counts
    // by this; the KMP port used to conflate it with winCount and silently drop it whenever
    // Multi-Window mode was off.
    val qty: String = "1",
    val winCount: String = "1",
    val spaceBetween: String = "0",
    val studOC: String = "16",
    val isDoor: Boolean = false,
    val isMultiWindow: Boolean = false,
    val studSize: String = "2x4",
    val notes: String = "",
    // Results
    val studHeight: String = "",
    val headerLength: String = "",
    val headerNominal: String = "",
    val jackCount: String = "",
    val jackLength: String = "",
    val sillCount: String = "",
    val sillLength: String = "",
    val lowerCrippleCount: String = "",
    val lowerCrippleLength: String = "",
    val upperCrippleCount: String = "",
    val upperCrippleLength: String = ""
)

data class SummarySection(val title: String, val headers: List<String>, val rows: List<List<String>>)

object SummaryReportGenerator {
    fun generateSections(
        stairEntries: List<StairEntry>,
        circularStairEntries: List<CircularStairEntry>,
        rafterEntries: List<RafterEntry>,
        gazeboEntries: List<GazeboEntry>,
        arcEntries: List<ArcEntry>,
        wallEntries: List<WallEntry>,
        crownEntries: List<CrownEntry>,
        concreteEntries: List<ConcreteEntry>,
        masonryEntries: List<MasonryEntry>,
        drywallEntries: List<DrywallEntry>,
        trigEntries: List<TrigEntry>,
        columnEntries: List<ColumnEntry>,
        roofEntries: List<RoofEntry>,
        floorEntries: List<FloorEntry>,
        deckEntries: List<DeckEntry>,
        handrailEntries: List<HandrailEntry>,
        framingEntries: List<FramingEntry>,
        pineLineEntries: List<PineLineEntry>,
        coordinatePaths: List<PlottedPath>,
        squareEntries: List<SquareEntry> = emptyList(),
        surveyEntries: List<SurveyEntry> = emptyList(),
        landSurveyEntries: List<LandSurveyEntry> = emptyList()
    ): List<SummarySection> {
        val sections = mutableListOf<SummarySection>()

        if (stairEntries.isNotEmpty()) {
            sections.add(SummarySection("STAIR SCHEDULE", listOf("Name", "Rise", "Risers", "Act Rise", "Treads", "Run", "Stringer", "Angle"), stairEntries.map { listOf(it.name, it.rise, it.riserCount, it.actualRise, it.treadCount, it.totalRun, it.stringerLen, it.angleDeg) }))
        }
        if (circularStairEntries.isNotEmpty()) {
            sections.add(SummarySection("CIRCULAR STAIR SCHEDULE", listOf("Name", "Rise", "Inner Rad", "Tread W", "Treads", "Circle", "Out Radius", "Angle/Trd"), circularStairEntries.map { listOf(it.name, it.totalRise, it.radius, it.walkTread, it.numTreads, it.circleSize, it.outsideRadius, it.anglePerTread) }))
        }
        if (rafterEntries.isNotEmpty()) {
            sections.add(SummarySection("RAFTER SCHEDULE", listOf("Name", "Span", "Pitch", "Heel", "O/H", "Com Len", "Hip Len", "Jack 16"), rafterEntries.map { listOf(it.name, it.span, it.pitch, it.heel, it.overhang, it.commonLen, it.hipLen, it.jack16) }))
        }
        if (gazeboEntries.isNotEmpty()) {
            sections.add(SummarySection("GAZEBO SCHEDULE", listOf("Name", "Sides", "Diam", "Pitch", "Area", "Com Len", "Hip Len"), gazeboEntries.map { listOf(it.name, it.sides, it.diameter, it.pitch, it.area, it.commonRafterLen, it.hipLen) }))
        }
        if (arcEntries.isNotEmpty()) {
            sections.add(SummarySection("ARC SCHEDULE", listOf("Name", "Chord", "Height", "Radius", "Arc Len", "Angle"), arcEntries.map { listOf(it.name, it.chord, it.height, it.radius, it.arcLength, it.arcAngle) }))
        }
        if (wallEntries.isNotEmpty()) {
            sections.add(SummarySection("WALL FRAMING SCHEDULE", listOf("Name", "Length", "Height", "Studs", "Plates", "Sheets"), wallEntries.map { listOf(it.name, it.length, it.height, it.studs, it.plates, it.sheets) }))
        }
        if (crownEntries.isNotEmpty()) {
            sections.add(SummarySection("CROWN MOLDING SCHEDULE", listOf("Name", "Spring", "Corner", "Miter", "Bevel"), crownEntries.map { listOf(it.name, it.spring, it.wallAngle, it.miter, it.bevel) }))
        }
        if (concreteEntries.isNotEmpty()) {
            sections.add(SummarySection("CONCRETE SCHEDULE", listOf("Name", "Qty", "Dimens", "Volume", "Rebar"), concreteEntries.map { listOf(it.name, it.qty, it.dimensions, it.volume, it.rebar) }))
        }
        if (masonryEntries.isNotEmpty()) {
            sections.add(SummarySection("MASONRY SCHEDULE", listOf("Name", "SqFt", "Type", "Units", "Mortar", "Sand"), masonryEntries.map { listOf(it.name, it.sqFt, it.type, it.units, it.mortar, it.sand) }))
        }
        if (drywallEntries.isNotEmpty()) {
            sections.add(SummarySection("DRYWALL SCHEDULE", listOf("Name", "Area", "Sheets", "Size", "Mud", "Tape"), drywallEntries.map { listOf(it.name, it.area, it.sheets, it.sheetSize, it.mud, it.tape) }))
        }
        if (trigEntries.isNotEmpty()) {
            sections.add(SummarySection("TRIG SOLUTIONS SCHEDULE", listOf("Name", "Side A", "Side B", "Hypot", "Angle A", "Angle B"), trigEntries.map { listOf(it.name, it.sideA, it.sideB, it.hypotenuse, it.angleA, it.angleB) }))
        }
        if (columnEntries.isNotEmpty()) {
            sections.add(SummarySection("COLUMN / CIRCLE SCHEDULE", listOf("Name", "Diam", "Height", "Volume", "Surf Area", "Circumf"), columnEntries.map { listOf(it.name, it.diameter, it.height, it.volume, it.surfaceArea, it.circumference) }))
        }
        if (roofEntries.isNotEmpty()) {
            sections.add(SummarySection(
                "ROOF SCHEDULE",
                listOf("Name", "Span", "Bldg Len", "Pitch", "Common", "Hip/Valley", "Ridge", "Sub-Fascia", "Squares"),
                roofEntries.map {
                    listOf(it.name, it.span, it.bldgLen, it.pitch, it.commonInfo, if (it.hipInfo.isNotEmpty()) it.hipInfo else it.valleyInfo, it.ridgeInfo, it.subFascia, it.squares)
                }
            ))
        }
        if (floorEntries.isNotEmpty()) {
            sections.add(SummarySection("FLOORING SCHEDULE", listOf("Name", "Length", "Width", "OC", "Joists", "Sheets"), floorEntries.map { listOf(it.name, it.length, it.width, it.spacing, it.joists, it.sheets) }))
        }
        if (deckEntries.isNotEmpty()) {
            sections.add(SummarySection("DECKING SCHEDULE", listOf("Name", "Length", "Width", "Gap", "Boards", "Fasteners"), deckEntries.map { listOf(it.name, it.length, it.width, it.gap, it.boards, it.fasteners) }))
        }
        if (handrailEntries.isNotEmpty()) {
            sections.add(SummarySection("HANDRAIL SCHEDULE", listOf("Name", "Length", "Count", "Spacing"), handrailEntries.map { listOf(it.name, it.length, it.spindles, it.spacing) }))
        }
        if (framingEntries.isNotEmpty()) {
            sections.add(SummarySection("OPENINGS SCHEDULE", listOf("Name", "Qty", "Header", "Jacks", "Lower Crip", "Upper Crip"), framingEntries.map { listOf(it.name, it.winCount, it.headerLength, "${it.jackCount}@${it.jackLength}", it.lowerCrippleCount, it.upperCrippleCount) }))
        }
        if (pineLineEntries.isNotEmpty()) {
            sections.add(SummarySection("PINE LINE SCHEDULE", listOf("Name", "Pitch", "Heel", "Wall", "O/H", "Drop"), pineLineEntries.map { listOf(it.name, it.pitch, it.heel, it.wall, it.overhang, it.drop) }))
        }
        if (coordinatePaths.isNotEmpty()) {
            sections.add(SummarySection("SITE SURVEY SCHEDULE", listOf("ID", "Type", "Points", "Area (sq ft)"), coordinatePaths.map { path ->
                val area = CoordinateEngine.calculateNetArea(path.points.map { CoordinateEngine.Point(it.n, it.e) }, path.arcHeights)
                listOf(path.id, if (path.isTakeoff) "TAKEOFF" else "BOUNDARY", path.points.size.toString(), (area / 144.0).roundToOneDecimal().toString())
            }))
        }
        if (squareEntries.isNotEmpty()) {
            sections.add(SummarySection("SQUARING SCHEDULE", listOf("Name", "Length", "Width", "Diagonal"), squareEntries.map { listOf(it.name, it.length, it.width, it.diagonal) }))
        }
        if (surveyEntries.isNotEmpty()) {
            sections.add(SummarySection("GRADE / SLOPE SCHEDULE", listOf("Name", "Run", "Slope/Ft", "Grade %", "Total Fall"), surveyEntries.map { listOf(it.name, it.distance, it.slope, it.slopePercent, it.totalFall) }))
        }
        if (landSurveyEntries.isNotEmpty()) {
            sections.add(SummarySection("LAND SURVEY SCHEDULE", listOf("Note", "Data"), landSurveyEntries.map { listOf(it.note, it.data) }))
        }

        return sections
    }
}

@Serializable
data class SerializablePoint(val x: Float, val y: Float)

@Serializable
data class SerializablePath(val points: List<SerializablePoint>, val color: Int)

@Serializable
data class AppState(
    val projectList: List<String>,
    val currentProjectName: String,
    val allStairEntries: Map<String, List<StairEntry>>,
    val allRafterEntries: Map<String, List<RafterEntry>>,
    val allArcEntries: Map<String, List<ArcEntry>>,
    val allWallEntries: Map<String, List<WallEntry>> = emptyMap(),
    val allCrownEntries: Map<String, List<CrownEntry>> = emptyMap(),
    val allGazeboEntries: Map<String, List<GazeboEntry>> = emptyMap(),
    val allConcreteEntries: Map<String, List<ConcreteEntry>> = emptyMap(),
    val allRoofEntries: Map<String, List<RoofEntry>> = emptyMap(),
    val allCoordinatePaths: Map<String, List<PlottedPath>> = emptyMap(),
    val allMasonryEntries: Map<String, List<MasonryEntry>> = emptyMap(),
    val allDrywallEntries: Map<String, List<DrywallEntry>> = emptyMap(),
    val allDeckEntries: Map<String, List<DeckEntry>> = emptyMap(),
    val allFloorEntries: Map<String, List<FloorEntry>> = emptyMap(),
    val allHandrailEntries: Map<String, List<HandrailEntry>> = emptyMap(),
    val allPineLineEntries: Map<String, List<PineLineEntry>> = emptyMap(),
    val allCircularStairEntries: Map<String, List<CircularStairEntry>> = emptyMap(),
    val allTrigEntries: Map<String, List<TrigEntry>> = emptyMap(),
    val allColumnEntries: Map<String, List<ColumnEntry>> = emptyMap(),
    val allFramingEntries: Map<String, List<FramingEntry>> = emptyMap(),
    val allJobNotes: Map<String, List<SerializablePath>> = emptyMap(),
    // Matches master's NotesActivity plain-text notes field (saved alongside the S-Pen
    // sketch) - previously ported as sketch-only, dropping the text half of the feature.
    val allJobTextNotes: Map<String, String> = emptyMap(),
    val allSquareEntries: Map<String, List<SquareEntry>> = emptyMap(),
    val allSurveyEntries: Map<String, List<SurveyEntry>> = emptyMap(),
    val allLandSurveyEntries: Map<String, List<LandSurveyEntry>> = emptyMap(),
    val appSettings: AppSettingsData = AppSettingsData()
)


class PersistenceManager(private val settings: Settings? = null) {
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }
    private val KEY_APP_STATE = "app_state_v1"

    fun saveState(state: AppState) {
        val s = settings ?: return
        try {
            val serialized = json.encodeToString(state)
            s.putString(KEY_APP_STATE, serialized)
        } catch (e: Exception) {
            println("Persistence Error: ${e.message}")
        }
    }

    fun loadState(): AppState? {
        val s = settings ?: return null
        return try {
            val serialized = s.getStringOrNull(KEY_APP_STATE)
            if (serialized != null) {
                json.decodeFromString<AppState>(serialized)
            } else null
        } catch (e: Exception) {
            println("Persistence Load Error: ${e.message}")
            null
        }
    }
}

// Matches master's ProjectDataManager.exportProjectPackage/importProjectPackage in
// spirit (a self-contained, re-importable single-project backup file), adapted to this
// app's single-JSON-blob AppState architecture instead of a zip of SharedPreferences keys.
object ProjectPackageIO {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(state: AppState): String = json.encodeToString(state)

    fun decode(text: String): AppState? = try {
        json.decodeFromString<AppState>(text)
    } catch (e: Exception) {
        null
    }
}

data class ToolItem(
    val title: String,
    val subLabel: String = "",
    val color: Color,
    val icon: ImageVector? = null,
    val screen: Screen? = null
)

val BlueTool = Color(0xFF0A2A66)
val OrangeTool = Color(0xFFE67E22)
val LabelRed = Color(0xFFD32F2F)
val SageGreen = Color(0xFF99A894)
val KeypadBg = Color(0xFFD1D1D1)
val GrayBtn = Color(0xFF7A7A7A)
val RedBtn = Color(0xFFD32F2F)

@Composable
@Preview
fun App() {
    val persistenceManager = remember { 
        val s = try { Settings() } catch (e: Throwable) { null }
        PersistenceManager(s)
    }
    val initialData = remember { persistenceManager.loadState() }

    var currentScreen by remember { mutableStateOf(Screen.Calculator) }
    
    // Project & Data State
    var projectList by remember { mutableStateOf(initialData?.projectList ?: listOf("Default")) }
    var currentProjectName by remember { mutableStateOf(initialData?.currentProjectName ?: "Default") }
    var allStairEntries by remember { mutableStateOf(initialData?.allStairEntries ?: mapOf()) }
    var allRafterEntries by remember { mutableStateOf(initialData?.allRafterEntries ?: mapOf()) }
    var allArcEntries by remember { mutableStateOf(initialData?.allArcEntries ?: mapOf()) }
    var allWallEntries by remember { mutableStateOf(initialData?.allWallEntries ?: mapOf()) }
    var allCrownEntries by remember { mutableStateOf(initialData?.allCrownEntries ?: mapOf()) }
    var allGazeboEntries by remember { mutableStateOf(initialData?.allGazeboEntries ?: mapOf()) }
    var allConcreteEntries by remember { mutableStateOf(initialData?.allConcreteEntries ?: mapOf()) }
    var allRoofEntries by remember { mutableStateOf(initialData?.allRoofEntries ?: mapOf()) }
    var allCoordinatePaths by remember { mutableStateOf(initialData?.allCoordinatePaths ?: mapOf()) }
    var allMasonryEntries by remember { mutableStateOf(initialData?.allMasonryEntries ?: mapOf()) }
    var allDrywallEntries by remember { mutableStateOf(initialData?.allDrywallEntries ?: mapOf()) }
    var allDeckEntries by remember { mutableStateOf(initialData?.allDeckEntries ?: mapOf()) }
    var allFloorEntries by remember { mutableStateOf(initialData?.allFloorEntries ?: mapOf()) }
    var allHandrailEntries by remember { mutableStateOf(initialData?.allHandrailEntries ?: mapOf()) }
    var allPineLineEntries by remember { mutableStateOf(initialData?.allPineLineEntries ?: mapOf()) }
    var allCircularStairEntries by remember { mutableStateOf(initialData?.allCircularStairEntries ?: mapOf()) }
    var allTrigEntries by remember { mutableStateOf(initialData?.allTrigEntries ?: mapOf()) }
    var allColumnEntries by remember { mutableStateOf(initialData?.allColumnEntries ?: mapOf()) }
    var allFramingEntries by remember { mutableStateOf(initialData?.allFramingEntries ?: mapOf()) }
    var allJobNotes by remember { mutableStateOf(initialData?.allJobNotes ?: mapOf()) }
    var allJobTextNotes by remember { mutableStateOf(initialData?.allJobTextNotes ?: mapOf()) }
    var allSquareEntries by remember { mutableStateOf(initialData?.allSquareEntries ?: mapOf()) }
    var allSurveyEntries by remember { mutableStateOf(initialData?.allSurveyEntries ?: mapOf()) }
    var allLandSurveyEntries by remember { mutableStateOf(initialData?.allLandSurveyEntries ?: mapOf()) }
    var appSettings by remember { mutableStateOf(initialData?.appSettings ?: AppSettingsData()) }

    // Applies the saved fraction precision globally - every FractionUtils.formatInches()
    // call across every calculator reads this on the next recomposition.
    LaunchedEffect(appSettings.precisionIndex) {
        FractionUtils.precisionDenominator = AppSettingsData.DENOMINATORS.getOrElse(appSettings.precisionIndex) { 16 }
    }

    // One-shot hand-off from the Coordinates map takeoff into Concrete/Roof, matching
    // master's Intent extras (IMPORT_LENGTH/IMPORT_AREA) but as in-memory Compose state.
    var concreteImportSeed by remember { mutableStateOf<ConcreteImportSeed?>(null) }
    var roofImportSeed by remember { mutableStateOf<RoofImportSeed?>(null) }

    var isMetricMode by remember { mutableStateOf(false) }

    // Custom Keyboard State
    var keyboardVisible by remember { mutableStateOf(false) }
    var keyboardValue by remember { mutableStateOf("") }
    var keyboardTarget by remember { mutableStateOf<(String) -> Unit>({}) }

    val focusManager = LocalFocusManager.current

    val openKeyboard: (String, (String) -> Unit) -> Unit = { value, target ->
        keyboardValue = value
        keyboardTarget = target
        keyboardVisible = true
    }

    // Auto-save whenever critical state changes
    LaunchedEffect(projectList, currentProjectName, allStairEntries, allRafterEntries, allArcEntries, allWallEntries, allCrownEntries, allGazeboEntries, allConcreteEntries, allRoofEntries, allCoordinatePaths, allMasonryEntries, allDrywallEntries, allDeckEntries, allFloorEntries, allHandrailEntries, allPineLineEntries, allCircularStairEntries, allTrigEntries, allColumnEntries, allFramingEntries, allJobNotes, allJobTextNotes, allSquareEntries, allSurveyEntries, allLandSurveyEntries, appSettings) {
        persistenceManager.saveState(
            AppState(projectList, currentProjectName, allStairEntries, allRafterEntries, allArcEntries, allWallEntries, allCrownEntries, allGazeboEntries, allConcreteEntries, allRoofEntries, allCoordinatePaths, allMasonryEntries, allDrywallEntries, allDeckEntries, allFloorEntries, allHandrailEntries, allPineLineEntries, allCircularStairEntries, allTrigEntries, allColumnEntries, allFramingEntries, allJobNotes, allJobTextNotes, allSquareEntries, allSurveyEntries, allLandSurveyEntries, appSettings)
        )
    }

    // Calculator Engine State
    var currentInput by remember { mutableStateOf("") }
    var historyText by remember { mutableStateOf("") }
    var firstValue by remember { mutableStateOf<DimensionValue?>(null) }
    var pendingOperation by remember { mutableStateOf("") }
    var isNewEntry by remember { mutableStateOf(true) }

    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    // Helper functions for the calculator engine
    fun getCurrentDimensionValue(): DimensionValue {
        if (currentInput.isEmpty()) return DimensionValue(0.0, 0)
        val power = when {
            currentInput.contains("cu ") -> 3
            currentInput.contains("sq ") -> 2
            currentInput.contains("'") || currentInput.contains("\"") || currentInput.contains("/") || currentInput.contains(" m") || currentInput.contains("cm") || currentInput.contains("mm") -> 1
            else -> 0
        }
        return DimensionValue(FractionUtils.parse(currentInput), power)
    }

    fun appendNumber(num: String) {
        if (isNewEntry) {
            if (historyText.contains("=")) historyText = ""
            currentInput = if (num == ".") "0." else num
            isNewEntry = false
        } else {
            if (num == "." && currentInput.contains(".")) return
            currentInput += num
        }
    }

    fun setOperation(op: String) {
        if (pendingOperation.isNotEmpty() && !isNewEntry) {
            val secondValue = getCurrentDimensionValue()
            val first = firstValue
            if (first != null) {
                val result = when (pendingOperation) {
                    "+" -> first + secondValue
                    "-" -> first - secondValue
                    "X", "*" -> first * secondValue
                    "/", "÷" -> first / secondValue
                    else -> secondValue
                }
                currentInput = result.toString()
            }
        }
        firstValue = getCurrentDimensionValue()
        pendingOperation = op
        historyText = "${firstValue.toString()} $op"
        isNewEntry = true
    }

    fun calculateResult() {
        val secondValue = getCurrentDimensionValue()
        val first = firstValue ?: return
        
        val result = when (pendingOperation) {
            "+" -> first + secondValue
            "-" -> first - secondValue
            "X", "*" -> first * secondValue
            "/", "÷" -> first / secondValue
            else -> secondValue
        }
        
        historyText = "${first.toString()} $pendingOperation ${secondValue.toString()} ="
        currentInput = result.toString()
        firstValue = null
        pendingOperation = ""
        isNewEntry = true
    }

    fun handleFeet() {
        if (isNewEntry) {
            if (historyText.contains("=")) historyText = ""
            currentInput = if (isMetricMode) "0 m" else "0'"
            isNewEntry = false
            return
        }
        if (isMetricMode) {
            when {
                currentInput.endsWith(" cu m") -> currentInput = currentInput.substring(0, currentInput.length - 5) + " m"
                currentInput.endsWith(" sq m") -> currentInput = currentInput.substring(0, currentInput.length - 5) + " cu m"
                currentInput.endsWith(" m") -> currentInput = currentInput.substring(0, currentInput.length - 2) + " sq m"
                else -> currentInput += " m"
            }
        } else {
            when {
                currentInput.endsWith(" cu ft") -> currentInput = currentInput.substring(0, currentInput.length - 6) + "'"
                currentInput.endsWith(" sq ft") -> currentInput = currentInput.substring(0, currentInput.length - 6) + " cu ft"
                currentInput.endsWith("'") -> currentInput = currentInput.substring(0, currentInput.length - 1) + " sq ft"
                else -> currentInput += "'"
            }
        }
    }

    fun handleInch() {
        if (isNewEntry) {
            if (historyText.contains("=")) historyText = ""
            currentInput = if (isMetricMode) "0 cm" else "0\""
            isNewEntry = false
            return
        }
        if (isMetricMode) {
            when {
                currentInput.endsWith(" cu cm") -> currentInput = currentInput.substring(0, currentInput.length - 6) + " cm"
                currentInput.endsWith(" sq cm") -> currentInput = currentInput.substring(0, currentInput.length - 6) + " cu cm"
                currentInput.endsWith(" cm") -> currentInput = currentInput.substring(0, currentInput.length - 3) + " sq cm"
                else -> currentInput += " cm"
            }
        } else {
            when {
                currentInput.endsWith(" cu in") -> currentInput = currentInput.substring(0, currentInput.length - 6) + "\""
                currentInput.endsWith(" sq in") -> currentInput = currentInput.substring(0, currentInput.length - 6) + " cu in"
                currentInput.endsWith("\"") -> currentInput = currentInput.substring(0, currentInput.length - 1) + " sq in"
                else -> currentInput += "\""
            }
        }
    }

    fun clearAll() {
        currentInput = ""
        historyText = ""
        firstValue = null
        pendingOperation = ""
        isNewEntry = true
    }

    MaterialTheme {
        Scaffold(
            scaffoldState = scaffoldState,
            drawerBackgroundColor = Color.Transparent,
            drawerElevation = 0.dp,
            drawerScrimColor = DrawerDefaults.scrimColor,
            topBar = {
                TopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Calculate, // Fallback, could use Res.drawable.ic_pro_calc if set up
                                contentDescription = null,
                                modifier = Modifier.size(24.dp).padding(end = 8.dp)
                            )
                            Column {
                                Text("Pro Construction Calculator", fontSize = 16.sp)
                                Text("Project: $currentProjectName", fontSize = 10.sp, color = Color.LightGray)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { scaffoldState.drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    },
                    backgroundColor = BlueTool,
                    contentColor = Color.White,
                    elevation = 0.dp
                )
            },
            drawerContent = {
                NavDrawerContent(
                    projectName = currentProjectName,
                    onToolClick = { screen ->
                        if (screen != null) currentScreen = screen
                        scope.launch { scaffoldState.drawerState.close() }
                    }
                )
            },
            bottomBar = {
                if (currentScreen == Screen.Calculator) {
                    BottomAppBar(
                        backgroundColor = BlueTool,
                        contentPadding = PaddingValues(0.dp),
                        elevation = 8.dp
                    ) {
                        UtilityBottomBar(
                            onProjectsClick = { currentScreen = Screen.ProjectList },
                            onNotesClick = { currentScreen = Screen.JobNotes },
                            onDataSheetClick = { currentScreen = Screen.JobSummary }
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                    when (currentScreen) {
                        Screen.Calculator -> ProfessionalDashboard(
                            input = currentInput,
                            history = historyText,
                            isMetricMode = isMetricMode,
                            onAction = { action ->
                                when (action) {
                                    is CalcAction.Digit -> appendNumber(action.digit)
                                    is CalcAction.Op -> setOperation(action.op)
                                    CalcAction.Equals -> calculateResult()
                                    CalcAction.Feet -> handleFeet()
                                    CalcAction.Inch -> handleInch()
                                    CalcAction.Clear -> clearAll()
                                    CalcAction.SquareRoot -> {
                                        val current = FractionUtils.parse(currentInput)
                                        if (current >= 0) {
                                            currentInput = FractionUtils.formatInches(sqrt(current))
                                            isNewEntry = false
                                        }
                                    }
                                    CalcAction.Pi -> {
                                        if (isNewEntry) {
                                            currentInput = "3.14159"
                                            isNewEntry = false
                                        } else {
                                            currentInput += "3.14159"
                                        }
                                    }
                                    CalcAction.Slash -> {
                                        if (isNewEntry) {
                                            if (historyText.contains("=")) historyText = ""
                                            currentInput = "0/"
                                            isNewEntry = false
                                        } else if (!currentInput.contains("/")) {
                                            currentInput += "/"
                                        }
                                    }
                                    CalcAction.Space -> {
                                        if (isNewEntry) {
                                            if (historyText.contains("=")) historyText = ""
                                            isNewEntry = false
                                        } else if (!currentInput.endsWith(" ") && currentInput.isNotEmpty()) {
                                            currentInput += " "
                                        }
                                    }
                                    CalcAction.Backspace -> {
                                        if (currentInput.isNotEmpty()) {
                                            currentInput = currentInput.dropLast(1)
                                        }
                                    }
                                    CalcAction.Metric -> {
                                        isMetricMode = !isMetricMode
                                        val dv = getCurrentDimensionValue()
                                        currentInput = if (isMetricMode) {
                                            FractionUtils.formatMetric(dv.value, dv.unitPower)
                                        } else {
                                            dv.toString()
                                        }
                                        isNewEntry = true
                                    }
                                    CalcAction.Convert -> {
                                        val dv = getCurrentDimensionValue()
                                        // Simple cycle: Decimal -> Fractional -> Metric
                                        currentInput = if (currentInput.contains("/")) {
                                            FractionUtils.formatMetric(dv.value, dv.unitPower)
                                        } else if (currentInput.contains("m") || currentInput.contains("cm")) {
                                            dv.value.toString() + "\""
                                        } else {
                                            dv.toString()
                                        }
                                        isNewEntry = true
                                    }
                                    is CalcAction.Navigate -> {
                                        currentScreen = action.screen
                                    }
                                }
                            }
                        )

                        Screen.ConcreteCalculator -> ConcreteCalculator(
                            entries = allConcreteEntries[currentProjectName] ?: emptyList(),
                            onUpdateEntries = { newList ->
                                allConcreteEntries = allConcreteEntries.toMutableMap().apply { put(currentProjectName, newList) }
                            },
                            onBack = { currentScreen = Screen.Calculator },
                            onFocus = openKeyboard,
                            importSeed = concreteImportSeed,
                            onImportConsumed = { concreteImportSeed = null }
                        )

                        Screen.RafterCalculator -> RafterCalculator(
                            entries = allRafterEntries[currentProjectName] ?: emptyList(),
                            onUpdateEntries = { newList ->
                                allRafterEntries = allRafterEntries.toMutableMap().apply { put(currentProjectName, newList) }
                            },
                            onBack = { currentScreen = Screen.Calculator },
                            onFocus = openKeyboard
                        )

                        Screen.ArcCalculator -> ArcCalculator(
                            entries = allArcEntries[currentProjectName] ?: emptyList(),
                            onUpdateEntries = { newList ->
                                allArcEntries = allArcEntries.toMutableMap().apply { put(currentProjectName, newList) }
                            },
                            onBack = { currentScreen = Screen.Calculator },
                            onFocus = openKeyboard
                        )

                        Screen.GableCutCalculator -> GableCutCalculator(
                            onBack = { currentScreen = Screen.Calculator },
                            onFocus = openKeyboard
                        )

                        Screen.SquareCalculator -> SquareCalculator(
                            entries = allSquareEntries[currentProjectName] ?: emptyList(),
                            onUpdateEntries = { newList ->
                                allSquareEntries = allSquareEntries.toMutableMap().apply { put(currentProjectName, newList) }
                            },
                            onBack = { currentScreen = Screen.Calculator },
                            onFocus = openKeyboard
                        )

                        Screen.GradeSlopeCalculator -> GradeSlopeCalculator(
                            entries = allSurveyEntries[currentProjectName] ?: emptyList(),
                            onUpdateEntries = { newList ->
                                allSurveyEntries = allSurveyEntries.toMutableMap().apply { put(currentProjectName, newList) }
                            },
                            onBack = { currentScreen = Screen.Calculator },
                            onFocus = openKeyboard
                        )

                        Screen.LandSurveyCalculator -> LandSurveyCalculator(
                            entries = allLandSurveyEntries[currentProjectName] ?: emptyList(),
                            onUpdateEntries = { newList ->
                                allLandSurveyEntries = allLandSurveyEntries.toMutableMap().apply { put(currentProjectName, newList) }
                            },
                            onBack = { currentScreen = Screen.Calculator },
                            onFocus = openKeyboard
                        )

                        Screen.Settings -> SettingsScreen(
                            settings = appSettings,
                            onSave = { newSettings ->
                                appSettings = newSettings
                                currentScreen = Screen.Calculator
                            },
                            onBack = { currentScreen = Screen.Calculator }
                        )

                        Screen.WallCalculator -> WallCalculator(
                            entries = allWallEntries[currentProjectName] ?: emptyList(),
                            onUpdateEntries = { newList ->
                                allWallEntries = allWallEntries.toMutableMap().apply { put(currentProjectName, newList) }
                            },
                            onBack = { currentScreen = Screen.Calculator },
                            onFocus = openKeyboard
                        )

                        Screen.FramingCalculator -> FramingCalculator(
                            entries = allFramingEntries[currentProjectName] ?: emptyList(),
                            onUpdateEntries = { newList ->
                                allFramingEntries = allFramingEntries.toMutableMap().apply { put(currentProjectName, newList) }
                            },
                            onBack = { currentScreen = Screen.Calculator },
                            onFocus = openKeyboard
                        )

                        Screen.CrownCalculator -> CrownCalculator(
                            entries = allCrownEntries[currentProjectName] ?: emptyList(),
                            onUpdateEntries = { newList ->
                                allCrownEntries = allCrownEntries.toMutableMap().apply { put(currentProjectName, newList) }
                            },
                            onBack = { currentScreen = Screen.Calculator },
                            onFocus = openKeyboard
                        )

                        Screen.GazeboCalculator -> GazeboCalculator(
                            entries = allGazeboEntries[currentProjectName] ?: emptyList(),
                            onUpdateEntries = { newList ->
                                allGazeboEntries = allGazeboEntries.toMutableMap().apply { put(currentProjectName, newList) }
                            },
                            onBack = { currentScreen = Screen.Calculator },
                            onFocus = openKeyboard
                        )

                        Screen.RoofCalculator -> RoofCalculator(
                            entries = allRoofEntries[currentProjectName] ?: emptyList(),
                            onUpdateEntries = { newList ->
                                allRoofEntries = allRoofEntries.toMutableMap().apply { put(currentProjectName, newList) }
                            },
                            onBack = { currentScreen = Screen.Calculator },
                            onFocus = openKeyboard,
                            importSeed = roofImportSeed,
                            onImportConsumed = { roofImportSeed = null }
                        )

                        Screen.CoordinateCalculator -> CoordinateCalculator(
                            paths = allCoordinatePaths[currentProjectName] ?: emptyList(),
                            onUpdatePaths = { newList ->
                                allCoordinatePaths = allCoordinatePaths.toMutableMap().apply { put(currentProjectName, newList) }
                            },
                            onBack = { currentScreen = Screen.Calculator },
                            onFocus = openKeyboard,
                            onSendToConcrete = { seed ->
                                concreteImportSeed = seed
                                currentScreen = Screen.ConcreteCalculator
                            },
                            onSendToRoof = { seed ->
                                roofImportSeed = seed
                                currentScreen = Screen.RoofCalculator
                            }
                        )

                        Screen.MasonryCalculator -> MasonryCalculator(
                            entries = allMasonryEntries[currentProjectName] ?: emptyList(),
                            onUpdateEntries = { newList ->
                                allMasonryEntries = allMasonryEntries.toMutableMap().apply { put(currentProjectName, newList) }
                            },
                            onBack = { currentScreen = Screen.Calculator },
                            onFocus = openKeyboard
                        )

                        Screen.DrywallCalculator -> DrywallCalculator(
                            entries = allDrywallEntries[currentProjectName] ?: emptyList(),
                            onUpdateEntries = { newList ->
                                allDrywallEntries = allDrywallEntries.toMutableMap().apply { put(currentProjectName, newList) }
                            },
                            onBack = { currentScreen = Screen.Calculator },
                            onFocus = openKeyboard
                        )

                        Screen.DeckCalculator -> DeckCalculator(
                            entries = allDeckEntries[currentProjectName] ?: emptyList(),
                            onUpdateEntries = { newList ->
                                allDeckEntries = allDeckEntries.toMutableMap().apply { put(currentProjectName, newList) }
                            },
                            onBack = { currentScreen = Screen.Calculator },
                            onFocus = openKeyboard
                        )

                        Screen.FloorCalculator -> FloorCalculator(
                            entries = allFloorEntries[currentProjectName] ?: emptyList(),
                            onUpdateEntries = { newList ->
                                allFloorEntries = allFloorEntries.toMutableMap().apply { put(currentProjectName, newList) }
                            },
                            onBack = { currentScreen = Screen.Calculator },
                            onFocus = openKeyboard
                        )

                        Screen.HandrailCalculator -> HandrailCalculator(
                            entries = allHandrailEntries[currentProjectName] ?: emptyList(),
                            onUpdateEntries = { newList ->
                                allHandrailEntries = allHandrailEntries.toMutableMap().apply { put(currentProjectName, newList) }
                            },
                            onBack = { currentScreen = Screen.Calculator },
                            onFocus = openKeyboard
                        )

                        Screen.PineLineCalculator -> PineLineCalculator(
                            entries = allPineLineEntries[currentProjectName] ?: emptyList(),
                            onUpdateEntries = { newList ->
                                allPineLineEntries = allPineLineEntries.toMutableMap().apply { put(currentProjectName, newList) }
                            },
                            onBack = { currentScreen = Screen.Calculator },
                            onFocus = openKeyboard
                        )

                        Screen.CircularStairCalculator -> CircularStairCalculator(
                            entries = allCircularStairEntries[currentProjectName] ?: emptyList(),
                            onUpdateEntries = { newList ->
                                allCircularStairEntries = allCircularStairEntries.toMutableMap().apply { put(currentProjectName, newList) }
                            },
                            onBack = { currentScreen = Screen.Calculator },
                            onFocus = openKeyboard
                        )

                        Screen.TrigCalculator -> TrigCalculator(
                            entries = allTrigEntries[currentProjectName] ?: emptyList(),
                            onUpdateEntries = { newList ->
                                allTrigEntries = allTrigEntries.toMutableMap().apply { put(currentProjectName, newList) }
                            },
                            onBack = { currentScreen = Screen.Calculator },
                            onFocus = openKeyboard
                        )

                        Screen.ColumnCalculator -> ColumnCalculator(
                            entries = allColumnEntries[currentProjectName] ?: emptyList(),
                            onUpdateEntries = { newList ->
                                allColumnEntries = allColumnEntries.toMutableMap().apply { put(currentProjectName, newList) }
                            },
                            onBack = { currentScreen = Screen.Calculator },
                            onFocus = openKeyboard
                        )

                        Screen.ScientificCalculator -> ScientificCalculator(
                            projectName = currentProjectName,
                            onBack = { currentScreen = Screen.Calculator },
                            onNavigate = { currentScreen = it }
                        )

                        Screen.CalculatorHelp -> CalculatorHelpScreen(
                            onBack = { currentScreen = Screen.ScientificCalculator }
                        )

                        Screen.GraphingCalculator -> GraphingCalculator(
                            projectName = currentProjectName,
                            onBack = { currentScreen = Screen.Calculator },
                            onNavigate = { currentScreen = it }
                        )

                        Screen.JobNotes -> JobNotesScreen(
                            savedPaths = allJobNotes[currentProjectName] ?: emptyList(),
                            onUpdateNotes = { newList ->
                                allJobNotes = allJobNotes.toMutableMap().apply { put(currentProjectName, newList) }
                            },
                            textNote = allJobTextNotes[currentProjectName] ?: "",
                            onUpdateTextNote = { text ->
                                allJobTextNotes = allJobTextNotes.toMutableMap().apply { put(currentProjectName, text) }
                            },
                            onBack = { currentScreen = Screen.Calculator }
                        )

                        Screen.StairCalculator -> StairCalculator(
                            entries = allStairEntries[currentProjectName] ?: emptyList(),
                            onUpdateEntries = { newList ->
                                allStairEntries = allStairEntries.toMutableMap().apply { put(currentProjectName, newList) }
                            },
                            onBack = { currentScreen = Screen.Calculator },
                            onFocus = openKeyboard
                        )

                        Screen.ProjectList -> {
                            val pdfProvider = rememberPdfExportProvider()
                            val fileDownloader = rememberFileDownloader()
                            ProjectListScreen(
                                projects = projectList,
                                onProjectSelected = { name ->
                                    currentProjectName = name
                                    currentScreen = Screen.Calculator
                                },
                                onAddProject = { name ->
                                    projectList = projectList + name
                                    currentProjectName = name
                                    currentScreen = Screen.Calculator
                                },
                                onDeleteProject = { name ->
                                    projectList = projectList.filter { it != name }
                                    if (currentProjectName == name) currentProjectName = "Default"
                                },
                                onRenameProject = { oldName, newName ->
                                    projectList = projectList.map { if (it == oldName) newName else it }
                                    if (currentProjectName == oldName) currentProjectName = newName
                                    allStairEntries = allStairEntries.renameKey(oldName, newName)
                                    allRafterEntries = allRafterEntries.renameKey(oldName, newName)
                                    allArcEntries = allArcEntries.renameKey(oldName, newName)
                                    allWallEntries = allWallEntries.renameKey(oldName, newName)
                                    allCrownEntries = allCrownEntries.renameKey(oldName, newName)
                                    allGazeboEntries = allGazeboEntries.renameKey(oldName, newName)
                                    allConcreteEntries = allConcreteEntries.renameKey(oldName, newName)
                                    allRoofEntries = allRoofEntries.renameKey(oldName, newName)
                                    allCoordinatePaths = allCoordinatePaths.renameKey(oldName, newName)
                                    allMasonryEntries = allMasonryEntries.renameKey(oldName, newName)
                                    allDrywallEntries = allDrywallEntries.renameKey(oldName, newName)
                                    allDeckEntries = allDeckEntries.renameKey(oldName, newName)
                                    allFloorEntries = allFloorEntries.renameKey(oldName, newName)
                                    allHandrailEntries = allHandrailEntries.renameKey(oldName, newName)
                                    allPineLineEntries = allPineLineEntries.renameKey(oldName, newName)
                                    allCircularStairEntries = allCircularStairEntries.renameKey(oldName, newName)
                                    allTrigEntries = allTrigEntries.renameKey(oldName, newName)
                                    allColumnEntries = allColumnEntries.renameKey(oldName, newName)
                                    allFramingEntries = allFramingEntries.renameKey(oldName, newName)
                                    allJobNotes = allJobNotes.renameKey(oldName, newName)
                                    allJobTextNotes = allJobTextNotes.renameKey(oldName, newName)
                                    allSquareEntries = allSquareEntries.renameKey(oldName, newName)
                                    allSurveyEntries = allSurveyEntries.renameKey(oldName, newName)
                                    allLandSurveyEntries = allLandSurveyEntries.renameKey(oldName, newName)
                                },
                                onPrintProject = { name ->
                                    val sections = SummaryReportGenerator.generateSections(
                                        stairEntries = allStairEntries[name] ?: emptyList(),
                                        circularStairEntries = allCircularStairEntries[name] ?: emptyList(),
                                        rafterEntries = allRafterEntries[name] ?: emptyList(),
                                        gazeboEntries = allGazeboEntries[name] ?: emptyList(),
                                        arcEntries = allArcEntries[name] ?: emptyList(),
                                        wallEntries = allWallEntries[name] ?: emptyList(),
                                        crownEntries = allCrownEntries[name] ?: emptyList(),
                                        concreteEntries = allConcreteEntries[name] ?: emptyList(),
                                        masonryEntries = allMasonryEntries[name] ?: emptyList(),
                                        drywallEntries = allDrywallEntries[name] ?: emptyList(),
                                        trigEntries = allTrigEntries[name] ?: emptyList(),
                                        columnEntries = allColumnEntries[name] ?: emptyList(),
                                        roofEntries = allRoofEntries[name] ?: emptyList(),
                                        floorEntries = allFloorEntries[name] ?: emptyList(),
                                        deckEntries = allDeckEntries[name] ?: emptyList(),
                                        handrailEntries = allHandrailEntries[name] ?: emptyList(),
                                        framingEntries = allFramingEntries[name] ?: emptyList(),
                                        pineLineEntries = allPineLineEntries[name] ?: emptyList(),
                                        coordinatePaths = allCoordinatePaths[name] ?: emptyList(),
                                        squareEntries = allSquareEntries[name] ?: emptyList(),
                                        surveyEntries = allSurveyEntries[name] ?: emptyList(),
                                        landSurveyEntries = allLandSurveyEntries[name] ?: emptyList()
                                    )
                                    pdfProvider.share(name, sections)
                                },
                                onPrintAll = {
                                    val allSections = mutableListOf<SummarySection>()
                                    projectList.forEach { name ->
                                        allSections.add(SummarySection("--- PROJECT: ${name.uppercase()} ---", emptyList(), emptyList()))
                                        allSections.addAll(SummaryReportGenerator.generateSections(
                                            stairEntries = allStairEntries[name] ?: emptyList(),
                                            circularStairEntries = allCircularStairEntries[name] ?: emptyList(),
                                            rafterEntries = allRafterEntries[name] ?: emptyList(),
                                            gazeboEntries = allGazeboEntries[name] ?: emptyList(),
                                            arcEntries = allArcEntries[name] ?: emptyList(),
                                            wallEntries = allWallEntries[name] ?: emptyList(),
                                            crownEntries = allCrownEntries[name] ?: emptyList(),
                                            concreteEntries = allConcreteEntries[name] ?: emptyList(),
                                            masonryEntries = allMasonryEntries[name] ?: emptyList(),
                                            drywallEntries = allDrywallEntries[name] ?: emptyList(),
                                            trigEntries = allTrigEntries[name] ?: emptyList(),
                                            columnEntries = allColumnEntries[name] ?: emptyList(),
                                            roofEntries = allRoofEntries[name] ?: emptyList(),
                                            floorEntries = allFloorEntries[name] ?: emptyList(),
                                            deckEntries = allDeckEntries[name] ?: emptyList(),
                                            handrailEntries = allHandrailEntries[name] ?: emptyList(),
                                            framingEntries = allFramingEntries[name] ?: emptyList(),
                                            pineLineEntries = allPineLineEntries[name] ?: emptyList(),
                                            coordinatePaths = allCoordinatePaths[name] ?: emptyList(),
                                            squareEntries = allSquareEntries[name] ?: emptyList(),
                                            surveyEntries = allSurveyEntries[name] ?: emptyList(),
                                            landSurveyEntries = allLandSurveyEntries[name] ?: emptyList()
                                        ))
                                    }
                                    pdfProvider.share("Master_Report", allSections)
                                },
                                onExportProject = { name ->
                                    val exportState = AppState(
                                        projectList = listOf(name),
                                        currentProjectName = name,
                                        allStairEntries = mapOf(name to (allStairEntries[name] ?: emptyList())),
                                        allRafterEntries = mapOf(name to (allRafterEntries[name] ?: emptyList())),
                                        allArcEntries = mapOf(name to (allArcEntries[name] ?: emptyList())),
                                        allWallEntries = mapOf(name to (allWallEntries[name] ?: emptyList())),
                                        allCrownEntries = mapOf(name to (allCrownEntries[name] ?: emptyList())),
                                        allGazeboEntries = mapOf(name to (allGazeboEntries[name] ?: emptyList())),
                                        allConcreteEntries = mapOf(name to (allConcreteEntries[name] ?: emptyList())),
                                        allRoofEntries = mapOf(name to (allRoofEntries[name] ?: emptyList())),
                                        allCoordinatePaths = mapOf(name to (allCoordinatePaths[name] ?: emptyList())),
                                        allMasonryEntries = mapOf(name to (allMasonryEntries[name] ?: emptyList())),
                                        allDrywallEntries = mapOf(name to (allDrywallEntries[name] ?: emptyList())),
                                        allDeckEntries = mapOf(name to (allDeckEntries[name] ?: emptyList())),
                                        allFloorEntries = mapOf(name to (allFloorEntries[name] ?: emptyList())),
                                        allHandrailEntries = mapOf(name to (allHandrailEntries[name] ?: emptyList())),
                                        allPineLineEntries = mapOf(name to (allPineLineEntries[name] ?: emptyList())),
                                        allCircularStairEntries = mapOf(name to (allCircularStairEntries[name] ?: emptyList())),
                                        allTrigEntries = mapOf(name to (allTrigEntries[name] ?: emptyList())),
                                        allColumnEntries = mapOf(name to (allColumnEntries[name] ?: emptyList())),
                                        allFramingEntries = mapOf(name to (allFramingEntries[name] ?: emptyList())),
                                        allJobNotes = mapOf(name to (allJobNotes[name] ?: emptyList())),
                                        allJobTextNotes = mapOf(name to (allJobTextNotes[name] ?: "")),
                                        allSquareEntries = mapOf(name to (allSquareEntries[name] ?: emptyList())),
                                        allSurveyEntries = mapOf(name to (allSurveyEntries[name] ?: emptyList())),
                                        allLandSurveyEntries = mapOf(name to (allLandSurveyEntries[name] ?: emptyList()))
                                    )
                                    val json = ProjectPackageIO.encode(exportState)
                                    fileDownloader.downloadFile("Project_${name.replace(" ", "_")}.json", json, "application/json")
                                },
                                onImportProjectText = { text ->
                                    val imported = ProjectPackageIO.decode(text)
                                    val sourceName = imported?.projectList?.firstOrNull() ?: imported?.currentProjectName

                                    if (imported == null) {
                                        "Invalid or corrupted project file"
                                    } else if (sourceName.isNullOrBlank()) {
                                        "Project file has no name"
                                    } else {
                                        var targetName = sourceName
                                        var suffix = 2
                                        while (projectList.contains(targetName)) {
                                            targetName = "$sourceName ($suffix)"
                                            suffix++
                                        }

                                        fun <T> pull(map: Map<String, List<T>>): List<T> = map[sourceName] ?: emptyList()

                                        allStairEntries = allStairEntries + (targetName to pull(imported.allStairEntries))
                                        allRafterEntries = allRafterEntries + (targetName to pull(imported.allRafterEntries))
                                        allArcEntries = allArcEntries + (targetName to pull(imported.allArcEntries))
                                        allWallEntries = allWallEntries + (targetName to pull(imported.allWallEntries))
                                        allCrownEntries = allCrownEntries + (targetName to pull(imported.allCrownEntries))
                                        allGazeboEntries = allGazeboEntries + (targetName to pull(imported.allGazeboEntries))
                                        allConcreteEntries = allConcreteEntries + (targetName to pull(imported.allConcreteEntries))
                                        allRoofEntries = allRoofEntries + (targetName to pull(imported.allRoofEntries))
                                        allCoordinatePaths = allCoordinatePaths + (targetName to pull(imported.allCoordinatePaths))
                                        allMasonryEntries = allMasonryEntries + (targetName to pull(imported.allMasonryEntries))
                                        allDrywallEntries = allDrywallEntries + (targetName to pull(imported.allDrywallEntries))
                                        allDeckEntries = allDeckEntries + (targetName to pull(imported.allDeckEntries))
                                        allFloorEntries = allFloorEntries + (targetName to pull(imported.allFloorEntries))
                                        allHandrailEntries = allHandrailEntries + (targetName to pull(imported.allHandrailEntries))
                                        allPineLineEntries = allPineLineEntries + (targetName to pull(imported.allPineLineEntries))
                                        allCircularStairEntries = allCircularStairEntries + (targetName to pull(imported.allCircularStairEntries))
                                        allTrigEntries = allTrigEntries + (targetName to pull(imported.allTrigEntries))
                                        allColumnEntries = allColumnEntries + (targetName to pull(imported.allColumnEntries))
                                        allFramingEntries = allFramingEntries + (targetName to pull(imported.allFramingEntries))
                                        allJobNotes = allJobNotes + (targetName to pull(imported.allJobNotes))
                                        allJobTextNotes = allJobTextNotes + (targetName to (imported.allJobTextNotes[sourceName] ?: ""))
                                        allSquareEntries = allSquareEntries + (targetName to pull(imported.allSquareEntries))
                                        allSurveyEntries = allSurveyEntries + (targetName to pull(imported.allSurveyEntries))
                                        allLandSurveyEntries = allLandSurveyEntries + (targetName to pull(imported.allLandSurveyEntries))

                                        projectList = projectList + targetName
                                        null
                                    }
                                },
                                onBack = { currentScreen = Screen.Calculator }
                            )
                        }

                        Screen.JobSummary -> JobSummaryScreen(
                            projectName = currentProjectName,
                            stairEntries = allStairEntries[currentProjectName] ?: emptyList(),
                            rafterEntries = allRafterEntries[currentProjectName] ?: emptyList(),
                            arcEntries = allArcEntries[currentProjectName] ?: emptyList(),
                            wallEntries = allWallEntries[currentProjectName] ?: emptyList(),
                            crownEntries = allCrownEntries[currentProjectName] ?: emptyList(),
                            gazeboEntries = allGazeboEntries[currentProjectName] ?: emptyList(),
                            concreteEntries = allConcreteEntries[currentProjectName] ?: emptyList(),
                            roofEntries = allRoofEntries[currentProjectName] ?: emptyList(),
                            coordinatePaths = allCoordinatePaths[currentProjectName] ?: emptyList(),
                            masonryEntries = allMasonryEntries[currentProjectName] ?: emptyList(),
                            drywallEntries = allDrywallEntries[currentProjectName] ?: emptyList(),
                            deckEntries = allDeckEntries[currentProjectName] ?: emptyList(),
                            floorEntries = allFloorEntries[currentProjectName] ?: emptyList(),
                            handrailEntries = allHandrailEntries[currentProjectName] ?: emptyList(),
                            pineLineEntries = allPineLineEntries[currentProjectName] ?: emptyList(),
                            framingEntries = allFramingEntries[currentProjectName] ?: emptyList(),
                            circularStairEntries = allCircularStairEntries[currentProjectName] ?: emptyList(),
                            trigEntries = allTrigEntries[currentProjectName] ?: emptyList(),
                            columnEntries = allColumnEntries[currentProjectName] ?: emptyList(),
                            squareEntries = allSquareEntries[currentProjectName] ?: emptyList(),
                            surveyEntries = allSurveyEntries[currentProjectName] ?: emptyList(),
                            landSurveyEntries = allLandSurveyEntries[currentProjectName] ?: emptyList(),
                            onBack = { currentScreen = Screen.Calculator }
                        )

                        Screen.About -> AboutScreen(
                            onBack = { currentScreen = Screen.Calculator }
                        )
                    }
                }

                // Custom Keyboard Overlay
                if (keyboardVisible) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        ConstructionKeyboard(
                            isVisible = true,
                            onValueChange = {
                                keyboardValue = it
                                keyboardTarget(it)
                            },
                            currentValue = keyboardValue,
                            onDone = {
                                keyboardVisible = false
                                focusManager.clearFocus()
                            },
                            onHide = {
                                keyboardVisible = false
                                focusManager.clearFocus()
                            }
                        )
                    }
                }
            }
        }
    }
}

sealed class CalcAction {
    data class Digit(val digit: String) : CalcAction()
    data class Op(val op: String) : CalcAction()
    object Equals : CalcAction()
    object Feet : CalcAction()
    object Inch : CalcAction()
    object Clear : CalcAction()
    object Slash : CalcAction()
    object Space : CalcAction()
    object SquareRoot : CalcAction()
    object Pi : CalcAction()
    object Backspace : CalcAction()
    object Metric : CalcAction()
    object Convert : CalcAction()
    data class Navigate(val screen: Screen) : CalcAction()
}

@Composable
fun NavDrawerContent(projectName: String, onToolClick: (Screen?) -> Unit) {
    val carpentryTools = listOf(
        ToolItem("Stair Layout", color = BlueTool, icon = Icons.Default.FormatLineSpacing, screen = Screen.StairCalculator),
        ToolItem("Circular Stairs", color = BlueTool, icon = Icons.Default.Refresh, screen = Screen.CircularStairCalculator),
        ToolItem("Rafter Cuts", color = BlueTool, icon = Icons.Default.Explore, screen = Screen.RafterCalculator),
        ToolItem("Roof Takeoff", color = BlueTool, icon = Icons.Default.MenuBook, screen = Screen.RoofCalculator),
        ToolItem("Gable Panel Cut", color = BlueTool, icon = Icons.Default.ChangeHistory, screen = Screen.GableCutCalculator),
        ToolItem("Floor / Joists", color = BlueTool, icon = Icons.Default.Today, screen = Screen.FloorCalculator),
        ToolItem("Handrails", color = BlueTool, icon = Icons.Default.SortByAlpha, screen = Screen.HandrailCalculator),
        ToolItem("Wall Framing", color = BlueTool, icon = Icons.Default.Add, screen = Screen.FramingCalculator),
        ToolItem("Wall Materials", color = BlueTool, icon = Icons.Default.Straighten, screen = Screen.WallCalculator),
        ToolItem("Deck Boards", color = BlueTool, icon = Icons.Default.History, screen = Screen.DeckCalculator),
        ToolItem("Gazebo Master", color = BlueTool, icon = Icons.Default.Directions, screen = Screen.GazeboCalculator),
        ToolItem("Crown Molding", color = BlueTool, icon = Icons.Default.Send, screen = Screen.CrownCalculator),
        ToolItem("Squaring / Diagonal", color = BlueTool, icon = Icons.Default.CropSquare, screen = Screen.SquareCalculator)
    )
    
    val concreteTools = listOf(
        ToolItem("Concrete / Rebar", color = BlueTool, icon = Icons.Default.Save, screen = Screen.ConcreteCalculator),
        ToolItem("Masonry Supply", color = BlueTool, icon = Icons.Default.Visibility, screen = Screen.MasonryCalculator)
    )
    
    val surveyTools = listOf(
        ToolItem("Site Coordinates", color = BlueTool, icon = Icons.Default.MyLocation, screen = Screen.CoordinateCalculator),
        ToolItem("Grade / Slope", color = BlueTool, icon = Icons.Default.Map, screen = Screen.GradeSlopeCalculator),
        ToolItem("Land Surveying", color = BlueTool, icon = Icons.Default.Terrain, screen = Screen.LandSurveyCalculator)
    )
    
    val mathTools = listOf(
        ToolItem("Scientific Calc", color = BlueTool, icon = Icons.Default.Calculate, screen = Screen.ScientificCalculator),
        ToolItem("Graphing Calc", color = BlueTool, icon = Icons.Default.ShowChart, screen = Screen.GraphingCalculator),
        ToolItem("Trig Solutions", color = BlueTool, icon = Icons.Default.Info, screen = Screen.TrigCalculator)
    )
    
    val drywallTools = listOf(
        ToolItem("Drywall Finish", color = BlueTool, icon = Icons.Default.Dashboard, screen = Screen.DrywallCalculator)
    )
    
    val projectTools = listOf(
        ToolItem("Job Site Notes", color = BlueTool, icon = Icons.Default.NoteAlt, screen = Screen.JobNotes),
        ToolItem("Project List", color = BlueTool, icon = Icons.Default.List, screen = Screen.ProjectList),
        ToolItem("Help & Guides", color = BlueTool, icon = Icons.Default.Info, screen = Screen.CalculatorHelp),
        ToolItem("Settings", color = BlueTool, icon = Icons.Default.Settings, screen = Screen.Settings),
        ToolItem("About", color = BlueTool, icon = Icons.Default.Info, screen = Screen.About)
    )

    Column(modifier = Modifier.width(250.dp).fillMaxHeight().shadow(8.dp).background(Color.White)) {
        Box(modifier = Modifier.fillMaxWidth().background(BlueTool).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp).padding(end = 12.dp)
                )
                Column {
                    Text("Pro Construction Calculator", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Project: $projectName", color = Color.White, fontSize = 10.sp)
                }
            }
        }
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            item { DrawerCategory("Carpentry", carpentryTools, onToolClick, initiallyExpanded = true) }
            item { DrawerCategory("Concrete & Masonry", concreteTools, onToolClick) }
            item { DrawerCategory("Survey", surveyTools, onToolClick) }
            item { DrawerCategory("Math", mathTools, onToolClick) }
            item { DrawerCategory("Drywall", drywallTools, onToolClick) }
            item { DrawerCategory("Project & Utilities", projectTools, onToolClick) }
        }
    }
}

@Composable
fun DrawerCategory(title: String, tools: List<ToolItem>, onToolClick: (Screen?) -> Unit, initiallyExpanded: Boolean = false) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    
    val icon = when (title) {
        "Carpentry" -> Icons.Default.Menu
        "Concrete & Masonry" -> Icons.Default.Business
        "Survey" -> Icons.Default.TrackChanges
        "Math" -> Icons.Default.Info
        "Drywall" -> Icons.Default.Edit
        else -> Icons.Default.Settings
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
        }
        
        if (expanded) {
            tools.forEach { tool ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onToolClick(tool.screen) }.padding(start = 32.dp, top = 8.dp, bottom = 8.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(tool.icon ?: Icons.Default.Build, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(tool.title, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        Divider()
    }
}

@Composable
fun ProfessionalDashboard(
    input: String,
    history: String,
    isMetricMode: Boolean,
    onAction: (CalcAction) -> Unit
) {
    val displayInput = input.ifEmpty { "0." }
    val fontSize = when {
        displayInput.length > 20 -> 18.sp
        displayInput.length > 15 -> 24.sp
        displayInput.length > 12 -> 30.sp
        displayInput.length > 8 -> 36.sp
        else -> 44.sp
    }

    Column(modifier = Modifier.fillMaxSize().background(KeypadBg)) {
        // Display Area (Increased Weight to 2.0)
        Card(
            modifier = Modifier.fillMaxWidth().weight(2.0f).padding(horizontal = 6.dp, vertical = 2.dp),
            backgroundColor = SageGreen,
            elevation = 0.dp,
            shape = RoundedCornerShape(2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.End
            ) {
                if (history.isNotEmpty()) {
                    Text(
                        text = history,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF333333),
                        modifier = Modifier.padding(bottom = 0.dp)
                    )
                }
                Text(
                    text = displayInput,
                    fontSize = fontSize,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111111),
                    maxLines = 1,
                    softWrap = false
                )
                // Small Spacer to ensure the text isn't flush with the bottom edge
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Keypad Area (Decreased weight to 8.0)
        Column(modifier = Modifier.fillMaxWidth().weight(8.0f).padding(1.dp)) {
            // Row 1-4: Specialty tools
            SpecialtyToolRows(onAction)
            
            // Row 5-8: Numeric and others
            NumericAndFunctionRows(isMetricMode, onAction)
            
            // Row 9: Large Operators
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                CalcButton("X", "", GrayBtn, Modifier.weight(1f)) { onAction(CalcAction.Op("X")) }
                CalcButton("-", "", GrayBtn, Modifier.weight(1.5f)) { onAction(CalcAction.Op("-")) }
                CalcButton("+", "", GrayBtn, Modifier.weight(1.5f)) { onAction(CalcAction.Op("+")) }
            }
        }
    }
}

@Composable
fun ColumnScope.SpecialtyToolRows(onAction: (CalcAction) -> Unit) {
    val toolRows = listOf(
        listOf(ToolItem("STAIR", "Headroom", BlueTool, screen = Screen.StairCalculator), ToolItem("RAFTER", "Cuts", BlueTool, screen = Screen.RafterCalculator), ToolItem("ARC", "Radius", BlueTool, screen = Screen.ArcCalculator), ToolItem("CROWN", "Miter/Bev", BlueTool, screen = Screen.CrownCalculator), ToolItem("GAZEBO", "Master", BlueTool, screen = Screen.GazeboCalculator)),
        listOf(ToolItem("CONCRETE", "Rebar", BlueTool, screen = Screen.ConcreteCalculator), ToolItem("CIR-STAIR", "Arcs", BlueTool, screen = Screen.CircularStairCalculator), ToolItem("ROOF CALC", "Order", BlueTool, screen = Screen.RoofCalculator), ToolItem("TRIG", "Angles", BlueTool, screen = Screen.TrigCalculator), ToolItem("FRAMING", "Openings", BlueTool, screen = Screen.FramingCalculator)),
        listOf(ToolItem("WALL CALC", "Studs/Ply", BlueTool, screen = Screen.WallCalculator), ToolItem("HANDRAIL", "Spindles", BlueTool, screen = Screen.HandrailCalculator), ToolItem("MASONRY", "Supply", BlueTool, screen = Screen.MasonryCalculator), ToolItem("FLOOR", "Joists", BlueTool, screen = Screen.FloorCalculator), ToolItem("DRYWALL", "Finish", BlueTool, screen = Screen.DrywallCalculator)),
        listOf(ToolItem("CIRC", "Column", BlueTool, screen = Screen.ColumnCalculator), ToolItem("DECK", "Boards", BlueTool, screen = Screen.DeckCalculator), ToolItem("GRADE", "Site", BlueTool, screen = Screen.GradeSlopeCalculator), ToolItem("BACK", "", Color(0xFFD32F2F)), ToolItem("CLEAR", "√x", RedBtn))
    )

    toolRows.forEach { row ->
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            row.forEach { tool ->
                CalcButton(tool.title, tool.subLabel, tool.color, Modifier.weight(1f)) {
                    if (tool.title == "CLEAR") {
                        onAction(CalcAction.Clear)
                    } else if (tool.title == "BACK") {
                        onAction(CalcAction.Backspace)
                    } else if (tool.subLabel == "√x") {
                        onAction(CalcAction.SquareRoot)
                    } else if (tool.screen != null) {
                        onAction(CalcAction.Navigate(tool.screen))
                    }
                }
            }
        }
    }
}

@Composable
fun ColumnScope.NumericAndFunctionRows(isMetricMode: Boolean, onAction: (CalcAction) -> Unit) {
    // Row 5
    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
        CalcButton("CONVERT", "Unit", BlueTool, Modifier.weight(1f)) { onAction(CalcAction.Convert) }
        CalcButton("7", "", GrayBtn, Modifier.weight(1f)) { onAction(CalcAction.Digit("7")) }
        CalcButton("8", "", GrayBtn, Modifier.weight(1f)) { onAction(CalcAction.Digit("8")) }
        CalcButton("9", "", GrayBtn, Modifier.weight(1f)) { onAction(CalcAction.Digit("9")) }
        CalcButton(if (isMetricMode) "METERS" else "FEET", if (isMetricMode) "m" else "'", BlueTool, Modifier.weight(1f)) { onAction(CalcAction.Feet) }
    }
    // Row 6
    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
        CalcButton("METRIC", "", Color(0xFFD32F2F), Modifier.weight(1f)) { onAction(CalcAction.Metric) }
        CalcButton("4", "", GrayBtn, Modifier.weight(1f)) { onAction(CalcAction.Digit("4")) }
        CalcButton("5", "", GrayBtn, Modifier.weight(1f)) { onAction(CalcAction.Digit("5")) }
        CalcButton("6", "", GrayBtn, Modifier.weight(1f)) { onAction(CalcAction.Digit("6")) }
        CalcButton(if (isMetricMode) "CM" else "INCH", if (isMetricMode) "cm" else "\"", BlueTool, Modifier.weight(1f)) { onAction(CalcAction.Inch) }
    }
    // Row 7
    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
        var menuExpanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            CalcButton("CALC", "Tools", Color(0xFFFF9800), Modifier.fillMaxSize()) { menuExpanded = true }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(onClick = { menuExpanded = false; onAction(CalcAction.Navigate(Screen.ScientificCalculator)) }) {
                    Text("Scientific / Graphing")
                }
                DropdownMenuItem(onClick = { menuExpanded = false; onAction(CalcAction.Navigate(Screen.CoordinateCalculator)) }) {
                    Text("Land Surveying")
                }
                DropdownMenuItem(onClick = { menuExpanded = false; onAction(CalcAction.Navigate(Screen.CalculatorHelp)) }) {
                    Text("Help & Guides")
                }
            }
        }
        CalcButton("1", "", GrayBtn, Modifier.weight(1f)) { onAction(CalcAction.Digit("1")) }
        CalcButton("2", "", GrayBtn, Modifier.weight(1f)) { onAction(CalcAction.Digit("2")) }
        CalcButton("3", "", GrayBtn, Modifier.weight(1f)) { onAction(CalcAction.Digit("3")) }
        CalcButton("/", "", GrayBtn, Modifier.weight(1f)) { onAction(CalcAction.Slash) }
    }
    // Row 8
    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
        CalcButton("SPACE", "", GrayBtn, Modifier.weight(1f)) { onAction(CalcAction.Space) }
        CalcButton("0", "", GrayBtn, Modifier.weight(1f)) { onAction(CalcAction.Digit("0")) }
        CalcButton(".", "", GrayBtn, Modifier.weight(1f)) { onAction(CalcAction.Digit(".")) }
        CalcButton("=", "", GrayBtn, Modifier.weight(1f)) { onAction(CalcAction.Equals) }
        CalcButton("÷", "", GrayBtn, Modifier.weight(1f)) { onAction(CalcAction.Op("÷")) }
    }
}

@Composable
fun CalcButton(title: String, subLabel: String, color: Color, modifier: Modifier, onClick: () -> Unit = {}) {
    Surface(
        modifier = modifier.padding(1.dp).clickable { onClick() },
        color = color,
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (subLabel.isNotEmpty()) {
                Text(
                    subLabel, 
                    color = if (color == OrangeTool) Color.White else LabelRed, 
                    fontSize = 8.sp, 
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                title, 
                color = Color.White, 
                fontSize = if (title.length > 6) 12.sp else 21.sp, // ~1.5x larger than 14sp
                fontWeight = FontWeight.Bold, 
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun UtilityBottomBar(
    onProjectsClick: () -> Unit,
    onNotesClick: () -> Unit,
    onDataSheetClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onProjectsClick() }, contentAlignment = Alignment.Center) {
            Text("PROJECTS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onNotesClick() }, contentAlignment = Alignment.Center) {
            Text("NOTES", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onDataSheetClick() }, contentAlignment = Alignment.Center) {
            Text("DATA SHEET", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

data class ConcreteImportSeed(val length: String = "", val width: String = "", val name: String = "")
data class RoofImportSeed(val bldgLen: String = "", val span: String = "1", val name: String = "")

@Composable
fun CoordinateCalculator(
    paths: List<PlottedPath>,
    onUpdatePaths: (List<PlottedPath>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit,
    onSendToConcrete: (ConcreteImportSeed) -> Unit = {},
    onSendToRoof: (RoofImportSeed) -> Unit = {}
) {
    // Auto-populate Benchmark if empty
    LaunchedEffect(paths) {
        if (paths.isEmpty()) {
            onUpdatePaths(listOf(PlottedPath(
                id = "1",
                points = listOf(CoordinatePoint(5000.0 * 12.0, 5000.0 * 12.0, 100.0 * 12.0, "Benchmark")),
                isTakeoff = false
            )))
        }
    }

    var northInput by remember { mutableStateOf("") }
    var eastInput by remember { mutableStateOf("") }
    var elevInput by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isNewPath by remember { mutableStateOf(false) }
    var isTakeoff by remember { mutableStateOf(true) }
    var closeLoop by remember { mutableStateOf(false) }

    var editingPathIdx by remember { mutableStateOf(-1) }
    var editingPtIdx by remember { mutableStateOf(-1) }

    var selectedTakeoffPath by remember { mutableStateOf(-1) }
    var showTakeoffDialog by remember { mutableStateOf(false) }
    var showManagePaths by remember { mutableStateOf(false) }
    var showMapToolsMenu by remember { mutableStateOf(false) }
    var showAngleTool by remember { mutableStateOf(false) }
    var showPointDistTool by remember { mutableStateOf(false) }
    var showLineDistTool by remember { mutableStateOf(false) }
    var showDmsPlotTool by remember { mutableStateOf(false) }
    var showPlotPointsTool by remember { mutableStateOf(false) }
    var toolResultMessage by remember { mutableStateOf<String?>(null) }
    var mapView by remember { mutableStateOf(true) }

    // Flat (pathIdx, ptIdx) list of every point, for the tool pickers below.
    val allPoints = remember(paths) {
        paths.flatMapIndexed { pIdx, path -> path.points.indices.map { ptIdx -> Triple(pIdx, ptIdx, path.points[ptIdx]) } }
    }
    fun ptLabel(pIdx: Int, ptIdx: Int, pt: CoordinatePoint) = "P${pIdx + 1}.${ptIdx + 1} ${pt.description}".trim()

    // Groups points into their existing traverse paths and closes each one that's flagged.
    fun pathAsEnginePoints(path: PlottedPath) = path.points.map { CoordinateEngine.Point(it.n, it.e, it.elev) }

    fun perimeterOf(path: PlottedPath): Double {
        val pts = pathAsEnginePoints(path)
        var p = 0.0
        for (i in 0 until pts.size - 1) {
            val chord = CoordinateEngine.dist(pts[i], pts[i + 1])
            val h = path.arcHeights.getOrElse(i) { 0.0 }
            p += if (h != 0.0) CoordinateEngine.calculateArcLength(chord, h) else chord
        }
        if (path.isClosed && pts.size > 2) {
            val chord = CoordinateEngine.dist(pts.last(), pts.first())
            val h = path.arcHeights.getOrElse(pts.size - 1) { 0.0 }
            p += if (h != 0.0) CoordinateEngine.calculateArcLength(chord, h) else chord
        }
        return p
    }

    // Matches master's showTakeoffDialog(): net area always treats the outline as closed
    // (shoelace wraps last->first regardless of the Closed toggle, which only affects the
    // reported perimeter), then subtracts any exclusion path whose start point falls inside it.
    fun netAreaOf(path: PlottedPath): Double {
        var area = CoordinateEngine.calculateNetArea(pathAsEnginePoints(path), path.arcHeights)
        val outline = pathAsEnginePoints(path)
        paths.forEach { hole ->
            if (hole !== path && hole.isExclusion) {
                val start = hole.points.firstOrNull()
                if (start != null && CoordinateEngine.isPointInPolygon(CoordinateEngine.Point(start.n, start.e), outline)) {
                    area -= CoordinateEngine.calculateNetArea(pathAsEnginePoints(hole), hole.arcHeights)
                }
            }
        }
        return area
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE))) {
        // Toolbar
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Coordinate Calculator", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = { mapView = !mapView }) {
                Text(if (mapView) "LIST VIEW" else "MAP VIEW", color = Color.White)
            }
        }

        // Input Section
        Column(modifier = Modifier.background(Color.White).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = northInput,
                    onValueChange = { northInput = it },
                    label = if (editingPtIdx != -1) "North (Abs)" else "North +/-",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(4.dp))
                ConstructionTextField(
                    value = eastInput,
                    onValueChange = { eastInput = it },
                    label = if (editingPtIdx != -1) "East (Abs)" else "East +/-",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(4.dp))
                ConstructionTextField(
                    value = elevInput,
                    onValueChange = { elevInput = it },
                    label = if (editingPtIdx != -1) "Elev (Abs)" else "Elev +/-",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            if (editingPtIdx == -1) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isNewPath, onCheckedChange = { isNewPath = it })
                    Text("Start New Path", fontSize = 12.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Type:", fontSize = 12.sp)
                    TextButton(onClick = { isTakeoff = !isTakeoff }) {
                        Text(if (isTakeoff) "TAKEOFF" else "BOUNDARY", color = if (isTakeoff) Color(0xFF6200EE) else Color.Gray, fontSize = 12.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = closeLoop, onCheckedChange = { closeLoop = it })
                    Text("Close Loop (this path)", fontSize = 12.sp)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val n = FractionUtils.parseFeet(northInput)
                        val e = FractionUtils.parseFeet(eastInput)
                        val el = FractionUtils.parseFeet(elevInput)

                        val currentPaths = paths.toMutableList()

                        if (editingPtIdx != -1 && editingPathIdx != -1) {
                            // UPDATE MODE (Absolute)
                            val path = currentPaths[editingPathIdx]
                            val pts = path.points.toMutableList()
                            pts[editingPtIdx] = CoordinatePoint(n, e, el, description)
                            currentPaths[editingPathIdx] = path.copy(points = pts)
                            editingPtIdx = -1; editingPathIdx = -1
                        } else {
                            // ADD MODE (Delta)
                            val lastPath = currentPaths.lastOrNull()
                            val lastPt = lastPath?.points?.lastOrNull() ?: CoordinatePoint(5000.0 * 12.0, 5000.0 * 12.0, 100.0 * 12.0)
                            val newPt = CoordinatePoint(lastPt.n + n, lastPt.e + e, lastPt.elev + el, description)

                            if (isNewPath || lastPath == null) {
                                currentPaths.add(PlottedPath(
                                    id = (currentPaths.size + 1).toString(),
                                    points = listOf(newPt),
                                    isTakeoff = isTakeoff,
                                    isClosed = closeLoop
                                ))
                            } else {
                                currentPaths[currentPaths.size - 1] = lastPath.copy(points = lastPath.points + newPt, isClosed = closeLoop)
                            }
                        }

                        onUpdatePaths(currentPaths)
                        northInput = ""; eastInput = ""; elevInput = ""; description = ""; isNewPath = false
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)
                ) {
                    Text(if (editingPtIdx != -1) "UPDATE POINT" else "ADD POINT", fontWeight = FontWeight.Bold)
                }

                if (editingPtIdx != -1) {
                    OutlinedButton(
                        onClick = {
                            editingPtIdx = -1; editingPathIdx = -1
                            northInput = ""; eastInput = ""; elevInput = ""; description = ""
                        },
                        modifier = Modifier.height(56.dp)
                    ) { Text("CANCEL") }
                }
            }

            if (editingPtIdx == -1) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { showManagePaths = true }) { Text("MANAGE PATHS", fontSize = 12.sp) }
                    Box {
                        TextButton(onClick = { showMapToolsMenu = true }) { Text("MAP TOOLS", fontSize = 12.sp) }
                        DropdownMenu(expanded = showMapToolsMenu, onDismissRequest = { showMapToolsMenu = false }) {
                            DropdownMenuItem(onClick = { showMapToolsMenu = false; showAngleTool = true }) { Text("Measure Angle (Corner)") }
                            DropdownMenuItem(onClick = { showMapToolsMenu = false; showPointDistTool = true }) { Text("Dist Between 2 Points") }
                            DropdownMenuItem(onClick = { showMapToolsMenu = false; showLineDistTool = true }) { Text("Dist Between 2 Lines") }
                            DropdownMenuItem(onClick = { showMapToolsMenu = false; showPlotPointsTool = true }) { Text("Plot Points on Line") }
                            DropdownMenuItem(onClick = { showMapToolsMenu = false; showDmsPlotTool = true }) { Text("Plot by D.M.S. & Dist") }
                        }
                    }
                    TextButton(onClick = { onUpdatePaths(emptyList()) }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                        Text("RESET ALL", fontSize = 12.sp)
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (mapView) {
                CoordinateMapView(paths)
                Column(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                    Button(onClick = { showTakeoffDialog = true }, colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE67E22))) {
                        Text("TAKE OFF", color = Color.White)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().background(Color.White)) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth().background(BlueTool).padding(8.dp)) {
                            TableCell("Pt #", 40.dp, isHeader = true)
                            TableCell("Desc", 80.dp, isHeader = true)
                            TableCell("North", 70.dp, isHeader = true)
                            TableCell("East", 70.dp, isHeader = true)
                            TableCell("Elev", 60.dp, isHeader = true)
                            TableCell("Act", 60.dp, isHeader = true)
                        }
                    }
                    var globalPtIndex = 0
                    paths.forEachIndexed { pIdx, path ->
                        path.points.forEachIndexed { ptIdx, pt ->
                            globalPtIndex++
                            val currentDisplayIndex = globalPtIndex
                            item {
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    TableCell(currentDisplayIndex.toString(), 40.dp)
                                    TableCell(pt.description, 80.dp)
                                    TableCell("${(pt.n / 12.0).roundToOneDecimal()}'", 70.dp)
                                    TableCell("${(pt.e / 12.0).roundToOneDecimal()}'", 70.dp)
                                    TableCell("${(pt.elev / 12.0).roundToOneDecimal()}'", 60.dp)
                                    Row(modifier = Modifier.width(60.dp)) {
                                        IconButton(onClick = {
                                            editingPathIdx = pIdx
                                            editingPtIdx = ptIdx
                                            northInput = (pt.n / 12.0).roundToOneDecimal().toString()
                                            eastInput = (pt.e / 12.0).roundToOneDecimal().toString()
                                            elevInput = (pt.elev / 12.0).roundToOneDecimal().toString()
                                            description = pt.description
                                        }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                        }
                                        IconButton(onClick = {
                                            val newList = paths.toMutableList()
                                            val p = newList[pIdx]
                                            val pts = p.points.toMutableList()
                                            pts.removeAt(ptIdx)
                                            if (pts.isEmpty()) newList.removeAt(pIdx) else newList[pIdx] = p.copy(points = pts)
                                            onUpdatePaths(newList)
                                        }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Red)
                                        }
                                    }
                                }
                                Divider()
                            }
                        }
                    }
                }
            }
        }
    }

    // --- MANAGE PATHS: closed/exclusion toggles, per-segment arc height, delete ---
    if (showManagePaths) {
        AlertDialog(
            onDismissRequest = { showManagePaths = false },
            title = { Text("Manage Paths") },
            text = {
                Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    paths.forEachIndexed { pIdx, path ->
                        Card(elevation = 1.dp, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Path ${pIdx + 1} (${path.points.size} pts, ${if (path.isTakeoff) "Takeoff" else "Boundary"})", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    IconButton(onClick = {
                                        val newList = paths.toMutableList(); newList.removeAt(pIdx); onUpdatePaths(newList)
                                    }) { Icon(Icons.Default.Delete, contentDescription = "Delete Path", tint = Color.Red) }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = path.isClosed, onCheckedChange = { checked ->
                                        val newList = paths.toMutableList(); newList[pIdx] = path.copy(isClosed = checked); onUpdatePaths(newList)
                                    })
                                    Text("Closed Loop", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Checkbox(checked = path.isExclusion, onCheckedChange = { checked ->
                                        val newList = paths.toMutableList(); newList[pIdx] = path.copy(isExclusion = checked); onUpdatePaths(newList)
                                    })
                                    Text("Exclusion (Hole)", fontSize = 12.sp)
                                }
                                if (path.points.size > 1) {
                                    Text("Arc Height per Segment (in, 0 = straight):", fontSize = 10.sp, color = Color.Gray)
                                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                        for (segIdx in 0 until path.points.size - 1) {
                                            var text by remember(path.id, segIdx) { mutableStateOf((path.arcHeights.getOrElse(segIdx) { 0.0 }).toString()) }
                                            OutlinedTextField(
                                                value = text,
                                                onValueChange = { v ->
                                                    text = v
                                                    val h = v.toDoubleOrNull() ?: 0.0
                                                    val newHeights = path.arcHeights.toMutableList()
                                                    while (newHeights.size <= segIdx) newHeights.add(0.0)
                                                    newHeights[segIdx] = h
                                                    val newList = paths.toMutableList(); newList[pIdx] = path.copy(arcHeights = newHeights); onUpdatePaths(newList)
                                                },
                                                label = { Text("Seg ${segIdx + 1}", fontSize = 10.sp) },
                                                modifier = Modifier.width(90.dp).padding(end = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showManagePaths = false }) { Text("DONE") } }
        )
    }

    // --- MEASURE ANGLE (uses adjacent points within the same path, wrapping if closed) ---
    if (showAngleTool) {
        var vertexSel by remember { mutableStateOf(-1) }
        AlertDialog(
            onDismissRequest = { showAngleTool = false },
            title = { Text("Angle Tool") },
            text = {
                Column {
                    Text("Pick the corner point:", fontSize = 12.sp)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        allPoints.forEachIndexed { i, (pIdx, ptIdx, pt) ->
                            OutlinedButton(onClick = { vertexSel = i }, modifier = Modifier.padding(2.dp)) { Text(ptLabel(pIdx, ptIdx, pt), fontSize = 10.sp) }
                        }
                    }
                    if (vertexSel != -1) {
                        val (pIdx, ptIdx, vertex) = allPoints[vertexSel]
                        val path = paths[pIdx]
                        val prev = if (ptIdx > 0) path.points[ptIdx - 1] else if (path.isClosed) path.points.last() else null
                        val next = if (ptIdx < path.points.size - 1) path.points[ptIdx + 1] else if (path.isClosed) path.points.first() else null
                        if (prev != null && next != null) {
                            val angle = CoordinateEngine.calculateAngle(
                                CoordinateEngine.Point(prev.n, prev.e), CoordinateEngine.Point(vertex.n, vertex.e), CoordinateEngine.Point(next.n, next.e)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(sprintf("Angle: %.2f°", angle), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BlueTool)
                        } else {
                            Text("Need 2 lines meeting here", fontSize = 12.sp, color = Color.Red)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAngleTool = false }) { Text("CLOSE") } }
        )
    }

    // --- DIST BETWEEN 2 POINTS ---
    if (showPointDistTool) {
        var sel1 by remember { mutableStateOf(-1) }
        var sel2 by remember { mutableStateOf(-1) }
        AlertDialog(
            onDismissRequest = { showPointDistTool = false },
            title = { Text("Point Distance") },
            text = {
                Column {
                    Text("Point 1:", fontSize = 12.sp)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        allPoints.forEachIndexed { i, (pIdx, ptIdx, pt) -> OutlinedButton(onClick = { sel1 = i }, modifier = Modifier.padding(2.dp)) { Text(ptLabel(pIdx, ptIdx, pt), fontSize = 10.sp) } }
                    }
                    Text("Point 2:", fontSize = 12.sp)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        allPoints.forEachIndexed { i, (pIdx, ptIdx, pt) -> OutlinedButton(onClick = { sel2 = i }, modifier = Modifier.padding(2.dp)) { Text(ptLabel(pIdx, ptIdx, pt), fontSize = 10.sp) } }
                    }
                    if (sel1 != -1 && sel2 != -1) {
                        val p1 = allPoints[sel1].third; val p2 = allPoints[sel2].third
                        val dist = CoordinateEngine.dist(CoordinateEngine.Point(p1.n, p1.e), CoordinateEngine.Point(p2.n, p2.e))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Direct Distance: ${FractionUtils.formatInches(dist)}", fontWeight = FontWeight.Bold, color = BlueTool)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPointDistTool = false }) { Text("CLOSE") } }
        )
    }

    // --- DIST BETWEEN 2 LINES (traverse segments, identified by their start point) ---
    if (showLineDistTool) {
        val segments = paths.flatMapIndexed { pIdx, path -> (0 until path.points.size - 1).map { i -> Triple(pIdx, i, path.points[i]) } }
        var seg1 by remember { mutableStateOf(-1) }
        var seg2 by remember { mutableStateOf(-1) }
        fun segLabel(pIdx: Int, i: Int) = "P${pIdx + 1}: Seg ${i + 1}->${i + 2}"
        AlertDialog(
            onDismissRequest = { showLineDistTool = false },
            title = { Text("Line Distance") },
            text = {
                Column {
                    Text("Line 1:", fontSize = 12.sp)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        segments.forEachIndexed { i, (pIdx, segIdx, _) -> OutlinedButton(onClick = { seg1 = i }, modifier = Modifier.padding(2.dp)) { Text(segLabel(pIdx, segIdx), fontSize = 10.sp) } }
                    }
                    Text("Line 2:", fontSize = 12.sp)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        segments.forEachIndexed { i, (pIdx, segIdx, _) -> OutlinedButton(onClick = { seg2 = i }, modifier = Modifier.padding(2.dp)) { Text(segLabel(pIdx, segIdx), fontSize = 10.sp) } }
                    }
                    if (seg1 != -1 && seg2 != -1) {
                        val (p1Idx, s1Idx, _) = segments[seg1]; val (p2Idx, s2Idx, _) = segments[seg2]
                        val a1 = paths[p1Idx].points[s1Idx]; val a2 = paths[p1Idx].points[s1Idx + 1]
                        val b1 = paths[p2Idx].points[s2Idx]; val b2 = paths[p2Idx].points[s2Idx + 1]
                        val dist = CoordinateEngine.minDistanceBetweenSegments(
                            CoordinateEngine.Point(a1.n, a1.e), CoordinateEngine.Point(a2.n, a2.e),
                            CoordinateEngine.Point(b1.n, b1.e), CoordinateEngine.Point(b2.n, b2.e)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Clear Distance: ${FractionUtils.formatInches(dist)}", fontWeight = FontWeight.Bold, color = BlueTool)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLineDistTool = false }) { Text("CLOSE") } }
        )
    }

    // --- PLOT POINTS ON LINE (divide a segment into N equal points) ---
    if (showPlotPointsTool) {
        val segments = paths.flatMapIndexed { pIdx, path -> (0 until path.points.size - 1).map { i -> Triple(pIdx, i, path.points[i]) } }
        var segSel by remember { mutableStateOf(-1) }
        var countText by remember { mutableStateOf("1") }
        fun segLabel(pIdx: Int, i: Int) = "P${pIdx + 1}: Seg ${i + 1}->${i + 2}"
        AlertDialog(
            onDismissRequest = { showPlotPointsTool = false },
            title = { Text("Plot Points on Line") },
            text = {
                Column {
                    Text("Divide line into equal segments", fontSize = 12.sp)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        segments.forEachIndexed { i, (pIdx, segIdx, _) -> OutlinedButton(onClick = { segSel = i }, modifier = Modifier.padding(2.dp)) { Text(segLabel(pIdx, segIdx), fontSize = 10.sp) } }
                    }
                    OutlinedTextField(value = countText, onValueChange = { countText = it }, label = { Text("Point Count") }, modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val count = countText.toIntOrNull() ?: 0
                    if (segSel != -1 && count > 0) {
                        val (pIdx, segIdx, _) = segments[segSel]
                        val path = paths[pIdx]
                        val p1 = path.points[segIdx]; val p2 = path.points[segIdx + 1]
                        val dN = (p2.n - p1.n) / (count + 1); val dE = (p2.e - p1.e) / (count + 1)
                        val newPts = path.points.toMutableList()
                        for (i in 1..count) newPts.add(segIdx + i, CoordinatePoint(p1.n + i * dN, p1.e + i * dE, p1.elev, "Plotted Pt"))
                        val newList = paths.toMutableList(); newList[pIdx] = path.copy(points = newPts)
                        onUpdatePaths(newList)
                    }
                    showPlotPointsTool = false
                }) { Text("PLOT") }
            },
            dismissButton = { TextButton(onClick = { showPlotPointsTool = false }) { Text("Cancel") } }
        )
    }

    // --- PLOT BY D.M.S. & DISTANCE ---
    if (showDmsPlotTool) {
        var startSel by remember { mutableStateOf(-1) }
        var deg by remember { mutableStateOf("") }
        var min by remember { mutableStateOf("") }
        var sec by remember { mutableStateOf("") }
        var distText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDmsPlotTool = false },
            title = { Text("D.M.S Plot") },
            text = {
                Column {
                    Text("0=North, 90=East", fontSize = 11.sp, color = Color.Gray)
                    Text("Start Point:", fontSize = 12.sp)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        allPoints.forEachIndexed { i, (pIdx, ptIdx, pt) -> OutlinedButton(onClick = { startSel = i }, modifier = Modifier.padding(2.dp)) { Text(ptLabel(pIdx, ptIdx, pt), fontSize = 10.sp) } }
                    }
                    Row {
                        OutlinedTextField(value = deg, onValueChange = { deg = it }, label = { Text("Deg") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = min, onValueChange = { min = it }, label = { Text("Min") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = sec, onValueChange = { sec = it }, label = { Text("Sec") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = distText, onValueChange = { distText = it }, label = { Text("Distance (ft/in)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (startSel != -1) {
                        val (pIdx, _, startPt) = allPoints[startSel]
                        val decDeg = CoordinateEngine.dmsToDecimal(deg.toDoubleOrNull() ?: 0.0, min.toDoubleOrNull() ?: 0.0, sec.toDoubleOrNull() ?: 0.0)
                        val dist = FractionUtils.parse(distText)
                        val newPt3 = CoordinateEngine.calculateCoordinate(CoordinateEngine.Point(startPt.n, startPt.e), decDeg, dist)
                        val path = paths[pIdx]
                        val newList = paths.toMutableList()
                        newList[pIdx] = path.copy(points = path.points + CoordinatePoint(newPt3.n, newPt3.e, startPt.elev, "DMS Pt"))
                        onUpdatePaths(newList)
                    }
                    showDmsPlotTool = false
                }) { Text("PLOT") }
            },
            dismissButton = { TextButton(onClick = { showDmsPlotTool = false }) { Text("Cancel") } }
        )
    }

    if (toolResultMessage != null) {
        AlertDialog(
            onDismissRequest = { toolResultMessage = null },
            title = { Text("Result") },
            text = { Text(toolResultMessage ?: "") },
            confirmButton = { TextButton(onClick = { toolResultMessage = null }) { Text("OK") } }
        )
    }

    if (showTakeoffDialog) {
        val takeoffPaths = paths.filter { it.isTakeoff }
        if (selectedTakeoffPath == -1 && takeoffPaths.isNotEmpty()) selectedTakeoffPath = paths.indexOf(takeoffPaths.first())
        val path = paths.getOrNull(selectedTakeoffPath)

        AlertDialog(
            onDismissRequest = { showTakeoffDialog = false },
            title = { Text(if (path?.isExclusion == true) "Exclusion" else "Building Plot Takeoff", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Path to take off:", fontSize = 12.sp)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        takeoffPaths.forEach { p ->
                            val idx = paths.indexOf(p)
                            OutlinedButton(onClick = { selectedTakeoffPath = idx }, modifier = Modifier.padding(2.dp)) {
                                Text("Path ${idx + 1}${if (p.isExclusion) " (Hole)" else ""}", fontSize = 10.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (path != null && path.points.size >= 2) {
                        val perim = perimeterOf(path)
                        val netArea = netAreaOf(path)
                        Text("Perimeter: ${FractionUtils.formatInches(perim)}")
                        Text("Net Area: ${(netArea / 144.0).roundToOneDecimal()} sq ft", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Select a path with at least 2 points.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        path?.let {
                            val netArea = netAreaOf(it) / 144.0
                            onSendToRoof(RoofImportSeed(bldgLen = sprintf("%.2f", netArea), span = "1", name = "Map Roof Area"))
                        }
                        showTakeoffDialog = false
                    }) { Text("Roof") }
                    TextButton(onClick = {
                        path?.let {
                            val perimFt = perimeterOf(it) / 12.0
                            onSendToConcrete(ConcreteImportSeed(length = sprintf("%.3f", perimFt), name = "Map Perimeter"))
                        }
                        showTakeoffDialog = false
                    }) { Text("Concrete") }
                }
            },
            dismissButton = {
                Row {
                    if (path != null) {
                        TextButton(onClick = {
                            val newList = paths.toMutableList()
                            newList[selectedTakeoffPath] = path.copy(isExclusion = !path.isExclusion)
                            onUpdatePaths(newList)
                        }) { Text(if (path.isExclusion) "MARK MAIN" else "MARK HOLE") }
                    }
                    TextButton(onClick = { showTakeoffDialog = false }) { Text("CLOSE") }
                }
            }
        )
    }
}

@Composable
fun JobNotesScreen(
    savedPaths: List<SerializablePath>,
    onUpdateNotes: (List<SerializablePath>) -> Unit,
    textNote: String,
    onUpdateTextNote: (String) -> Unit,
    onBack: () -> Unit
) {
    // Maintain a stable SnapshotStateList for the UI
    val paths = remember { mutableStateListOf<Pair<Path, Color>>() }
    
    // Sync local paths list with savedPaths incrementally
    LaunchedEffect(savedPaths) {
        if (savedPaths.isEmpty()) {
            paths.clear()
        } else if (paths.size < savedPaths.size) {
            // Only add the NEW paths (e.g. from sync or initial load)
            // If we are already ahead or equal (because we added locally first), do nothing
            val startIdx = paths.size
            for (i in startIdx until savedPaths.size) {
                val sp = savedPaths[i]
                val p = Path()
                if (sp.points.isNotEmpty()) {
                    p.moveTo(sp.points[0].x, sp.points[0].y)
                    sp.points.drop(1).forEach { pt -> p.lineTo(pt.x, pt.y) }
                }
                paths.add(p to Color(sp.color))
            }
        } else if (paths.size > savedPaths.size) {
            // Something was deleted or project changed
            paths.clear()
            savedPaths.forEach { sp ->
                val p = Path()
                if (sp.points.isNotEmpty()) {
                    p.moveTo(sp.points[0].x, sp.points[0].y)
                    sp.points.drop(1).forEach { pt -> p.lineTo(pt.x, pt.y) }
                }
                paths.add(p to Color(sp.color))
            }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE))) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Job Site Notes", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }

        OutlinedTextField(
            value = textNote,
            onValueChange = onUpdateTextNote,
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 100.dp),
            maxLines = 6
        )

        Text("SKETCH (S-PEN / TOUCH)", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp).border(1.dp, Color.Gray, RoundedCornerShape(8.dp))) {
            SketchCanvas(
                paths = paths,
                onRawPathAdded = { offsets ->
                    val serializable = SerializablePath(
                        points = offsets.map { SerializablePoint(it.x, it.y) },
                        color = 0xFF000000.toInt() // Solid Black
                    )
                    // Update parent state - LaunchedEffect will handle adding if needed,
                    // but SketchCanvas already added it to 'paths' list.
                    onUpdateNotes(savedPaths + serializable)
                }
            )
        }

        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
            TextButton(
                onClick = {
                    paths.clear()
                    onUpdateNotes(emptyList())
                },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
            ) {
                Text("CLEAR SKETCH")
            }
        }
    }
}




@Composable
fun CoordinateMapView(paths: List<PlottedPath>) {
    Canvas(modifier = Modifier.fillMaxSize().background(Color.White)) {
        if (paths.isEmpty()) return@Canvas
        
        val allPts = paths.flatMap { it.points }
        val minN = allPts.minOf { it.n }; val maxN = allPts.maxOf { it.n }
        val minE = allPts.minOf { it.e }; val maxE = allPts.maxOf { it.e }
        
        val rangeN = (maxN - minN).coerceAtLeast(120.0)
        val rangeE = (maxE - minE).coerceAtLeast(120.0)
        
        val scale = min(size.height / rangeN, size.width / rangeE) * 0.8f
        val centerN = (minN + maxN) / 2.0
        val centerE = (minE + maxE) / 2.0
        
        fun getCanvasX(e: Double) = (size.width / 2f + (e - centerE) * scale).toFloat()
        fun getCanvasY(n: Double) = (size.height / 2f - (n - centerN) * scale).toFloat()

        paths.forEach { path ->
            val color = if (path.isTakeoff) BlueTool else Color.Gray
            val stroke = if (path.isTakeoff) 4f else 2f
            
            val canvasPath = Path()
            if (path.points.isNotEmpty()) {
                canvasPath.moveTo(getCanvasX(path.points[0].e), getCanvasY(path.points[0].n))
                for (i in 0 until path.points.size - 1) {
                    val p1 = path.points[i]
                    val p2 = path.points[i+1]
                    val h = path.arcHeights.getOrElse(i) { 0.0 }
                    
                    if (h == 0.0) {
                        canvasPath.lineTo(getCanvasX(p2.e), getCanvasY(p2.n))
                    } else {
                        // Draw arc using quadratic bezier as approximation for visual speed
                        // Control point calculation: midpoint + perpendicular vector scaled by 2*h
                        val midX = (getCanvasX(p1.e) + getCanvasX(p2.e)) / 2f
                        val midY = (getCanvasY(p1.n) + getCanvasY(p2.n)) / 2f
                        
                        val dx = getCanvasX(p2.e) - getCanvasX(p1.e)
                        val dy = getCanvasY(p2.n) - getCanvasY(p1.n)
                        val dist = sqrt(dx*dx + dy*dy)
                        
                        val hPx = (h * scale).toFloat()
                        // Perpendicular vector (-dy, dx)
                        val nx = -dy / dist
                        val ny = dx / dist
                        
                        val ctrlX = midX + nx * hPx * 2f
                        val ctrlY = midY + ny * hPx * 2f
                        
                        canvasPath.quadraticTo(ctrlX, ctrlY, getCanvasX(p2.e), getCanvasY(p2.n))
                    }
                }
                drawPath(canvasPath, color, style = Stroke(stroke))
            }
            
            // Draw points
            path.points.forEach { pt ->
                drawCircle(color = Color.Red, radius = 6f, center = androidx.compose.ui.geometry.Offset(getCanvasX(pt.e), getCanvasY(pt.n)))
            }
        }
    }
}

@Composable
fun RoofCalculator(
    entries: List<RoofEntry>,
    onUpdateEntries: (List<RoofEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit,
    importSeed: RoofImportSeed? = null,
    onImportConsumed: () -> Unit = {}
) {
    var span by remember { mutableStateOf("") }
    var bldgLen by remember { mutableStateOf("") }
    var pitch by remember { mutableStateOf("6") }
    var oc by remember { mutableStateOf("24") }
    var overhang by remember { mutableStateOf("12") }
    var leftEndType by remember { mutableStateOf(0) } // 0=Gable, 1=Hip, 2=Valley
    var rightEndType by remember { mutableStateOf(0) }
    var isTruss by remember { mutableStateOf(false) }
    var bothSides by remember { mutableStateOf(false) }
    var sectionName by remember { mutableStateOf("") }

    var results by remember { mutableStateOf<RoofEngine.RoofResult?>(null) }
    var editingIndex by remember { mutableStateOf(-1) }

    // Matches master's RoofCalcActivity IMPORT_AREA handling: treats an imported map area
    // as building length x 1' span, letting the user correct the actual span/pitch.
    LaunchedEffect(importSeed) {
        importSeed?.let { seed ->
            bldgLen = seed.bldgLen
            span = seed.span
            sectionName = seed.name
            onImportConsumed()
        }
    }

    fun endTypeLabel(t: Int) = when (t) { 1 -> "Hip"; 2 -> "Valley"; else -> "Gable" }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Roof Takeoff", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("BUILDING & PITCH", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = span,
                    onValueChange = { span = it },
                    label = "Span/Width (ft/in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = bldgLen,
                    onValueChange = { bldgLen = it },
                    label = "Building Length (ft/in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = pitch,
                    onValueChange = { pitch = it },
                    label = "Roof Pitch (/12)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = oc,
                    onValueChange = { oc = it },
                    label = "Rafter O.C. (in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            ConstructionTextField(
                value = overhang,
                onValueChange = { overhang = it },
                label = "Overhang (in)",
                modifier = Modifier.fillMaxWidth(),
                onFocus = onFocus
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("ROOF ENDS", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Left End", fontSize = 11.sp, color = Color.Gray)
                    Row {
                        listOf(0, 1, 2).forEach { t ->
                            Button(
                                onClick = { leftEndType = t },
                                modifier = Modifier.weight(1f).height(44.dp).padding(end = if (t < 2) 2.dp else 0.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (leftEndType == t) Color(0xFF6200EE) else Color.LightGray,
                                    contentColor = if (leftEndType == t) Color.White else Color.DarkGray
                                )
                            ) { Text(endTypeLabel(t), fontSize = 11.sp) }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Right End", fontSize = 11.sp, color = Color.Gray)
                    Row {
                        listOf(0, 1, 2).forEach { t ->
                            Button(
                                onClick = { rightEndType = t },
                                modifier = Modifier.weight(1f).height(44.dp).padding(end = if (t < 2) 2.dp else 0.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (rightEndType == t) Color(0xFF6200EE) else Color.LightGray,
                                    contentColor = if (rightEndType == t) Color.White else Color.DarkGray
                                )
                            ) { Text(endTypeLabel(t), fontSize = 11.sp) }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isTruss, onCheckedChange = { isTruss = it })
                Text("Truss (not stick-framed)", fontSize = 13.sp, modifier = Modifier.padding(end = 16.dp))
                Checkbox(checked = bothSides, onCheckedChange = { bothSides = it })
                Text("Both Sides (Mirror)", fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = sectionName,
                onValueChange = { sectionName = it },
                label = { Text("Section Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val s = FractionUtils.parse(span)
                        val bl = FractionUtils.parse(bldgLen)
                        val oh = FractionUtils.parse(overhang)
                        val p = FractionUtils.parse(pitch).let { if (it == 0.0) 6.0 else it }
                        val ocVal = FractionUtils.parse(oc).let { if (it == 0.0) 24.0 else it }

                        if (s > 0 && bl > 0) {
                            val res = RoofEngine.calculate(s, bl, oh, p, ocVal, leftEndType, rightEndType, isTruss, bothSides)
                            results = res

                            val commonInfo = "$oc\" OC x ${res.commonCount}${if (bothSides) " (Mirror)" else ""} @ ${FractionUtils.formatInches(res.overallLen)}"
                            val hipInfo = if (res.totalHips > 0) "${res.totalHips} pcs @ ${FractionUtils.formatInches(res.hipLen)}" else "-"
                            val valleyInfo = if (res.totalValleys > 0) "${res.totalValleys} pcs @ ${FractionUtils.formatInches(res.hipLen)}" else "-"
                            val ridgeInfo = if (!isTruss) FractionUtils.formatInches(res.correctedRidgeLen) else "-"

                            val entry = RoofEntry(
                                id = if (editingIndex != -1) entries[editingIndex].id else (entries.size + 1).toString(),
                                name = sectionName.ifEmpty { "Roof Section" },
                                span = span,
                                bldgLen = bldgLen,
                                pitch = pitch,
                                leftEnd = leftEndType,
                                rightEnd = rightEndType,
                                commonInfo = commonInfo,
                                hipInfo = hipInfo,
                                valleyInfo = valleyInfo,
                                ridgeInfo = ridgeInfo,
                                subFascia = FractionUtils.formatInches(res.totalSubFasciaLF),
                                // Legacy shingle-coverage fields kept populated too, since
                                // JobSummary/CSV export still read them.
                                area = "${res.totalSqFt.roundToInt()} sq ft",
                                squares = "${(res.totalSqFt / 100.0 * 10).roundToInt() / 10.0}",
                                bundles = ceil(res.totalSqFt / 100.0 * 3.0).toInt().toString(),
                                sheets = res.sheetCount.toString(),
                                dimensions = "$span x $bldgLen @ $pitch"
                            )

                            if (editingIndex != -1) {
                                val newList = entries.toMutableList()
                                newList[editingIndex] = entry
                                onUpdateEntries(newList)
                                editingIndex = -1
                            } else {
                                onUpdateEntries(entries + entry)
                            }
                            sectionName = ""
                        }
                    },
                    modifier = Modifier.weight(2f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)
                ) {
                    Text(if (editingIndex != -1) "UPDATE & SAVE" else "CALCULATE TAKEOFF", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()); results = null },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFC62828)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            results?.let { res ->
                Spacer(modifier = Modifier.height(24.dp))
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("COMMON RAFTERS", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE), fontSize = 12.sp)
                        Text("${res.commonCount}${if (bothSides) " (Mirror)" else ""} @ ${FractionUtils.formatInches(res.overallLen)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                        Text("Run: ${FractionUtils.formatInches(res.run)} | Rise: ${FractionUtils.formatInches(res.rise)} | Jack Diff: ${FractionUtils.formatInches(res.commonDiff)}", fontSize = 12.sp, color = Color.Gray)

                        if (res.totalHips > 0 || res.totalValleys > 0) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text("HIP / VALLEY MEMBERS", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE), fontSize = 12.sp)
                            if (res.totalHips > 0) Text("Hips: ${res.totalHips} pcs @ ${FractionUtils.formatInches(res.hipLen)}", fontSize = 14.sp)
                            if (res.totalValleys > 0) Text("Valleys: ${res.totalValleys} pcs @ ${FractionUtils.formatInches(res.hipLen)}", fontSize = 14.sp)
                        }

                        if (!isTruss) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text("RIDGE", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE), fontSize = 12.sp)
                            Text(FractionUtils.formatInches(res.correctedRidgeLen), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Sub-Fascia", fontSize = 11.sp, color = Color.Gray)
                                Text(FractionUtils.formatInches(res.totalSubFasciaLF), fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Sheathing", fontSize = 11.sp, color = Color.Gray)
                                Text("${res.sheetCount} Sheets", fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Area", fontSize = 11.sp, color = Color.Gray)
                                Text("${res.totalSqFt.roundToInt()} sq ft", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED TAKE-OFFS", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 1.dp, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Span: ${entry.span} | Len: ${entry.bldgLen} | ${endTypeLabel(entry.leftEnd)}/${endTypeLabel(entry.rightEnd)}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    span = entry.span
                                    bldgLen = entry.bldgLen
                                    pitch = entry.pitch
                                    leftEndType = entry.leftEnd
                                    rightEndType = entry.rightEnd
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("Common: ${entry.commonInfo}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                            if (entry.hipInfo != "-") Text("Hip: ${entry.hipInfo}", fontSize = 12.sp)
                            if (entry.valleyInfo != "-") Text("Valley: ${entry.valleyInfo}", fontSize = 12.sp)
                            if (entry.ridgeInfo != "-") Text("Ridge: ${entry.ridgeInfo}", fontSize = 12.sp)
                            Text("Sub-Fascia: ${entry.subFascia} | Sheathing: ${entry.sheets} sheets", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StairCalculator(
    entries: List<StairEntry>,
    onUpdateEntries: (List<StairEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var totalRise by remember { mutableStateOf("") }
    var targetRise by remember { mutableStateOf("7.5") }
    var floorThick by remember { mutableStateOf("11.875") }
    var nosing by remember { mutableStateOf("1.0") }
    var treadWidth by remember { mutableStateOf("10.25") }
    var sectionName by remember { mutableStateOf("") }
    // Master's "Limited Run" mode: for non-conforming/tight stairs (e.g. basements) where the
    // available floor run is fixed, StairEngine.calculateLimitedRun finds the best riser count
    // to fit within it instead of solving from a target riser height.
    var limitedMode by remember { mutableStateOf(false) }
    var limitedRun by remember { mutableStateOf("") }

    var results by remember { mutableStateOf<StairEngine.StairResult?>(null) }
    var editingIndex by remember { mutableStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Stair Layout", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("STAIR DIMENSIONS", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        limitedMode = false
                        treadWidth = "10.25"
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 0.dp, bottomEnd = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (!limitedMode) Color(0xFF6200EE) else Color.LightGray,
                        contentColor = if (!limitedMode) Color.White else Color.DarkGray
                    )
                ) { Text("Standard", fontSize = 13.sp) }
                Button(
                    onClick = {
                        limitedMode = true
                        treadWidth = "11.25"
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 8.dp, bottomEnd = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (limitedMode) Color(0xFF6200EE) else Color.LightGray,
                        contentColor = if (limitedMode) Color.White else Color.DarkGray
                    )
                ) { Text("Limited Run", fontSize = 13.sp) }
            }
            Spacer(modifier = Modifier.height(8.dp))

            ConstructionTextField(
                value = totalRise,
                onValueChange = { totalRise = it },
                label = "Total Rise (ft / in)",
                modifier = Modifier.fillMaxWidth(),
                onFocus = onFocus
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (limitedMode) {
                ConstructionTextField(
                    value = limitedRun,
                    onValueChange = { limitedRun = it },
                    label = "Limited Run (in)",
                    modifier = Modifier.fillMaxWidth(),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.height(8.dp))
                ConstructionTextField(
                    value = treadWidth,
                    onValueChange = { treadWidth = it },
                    label = "Total Tread Width (Lumber size)",
                    modifier = Modifier.fillMaxWidth(),
                    onFocus = onFocus
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    ConstructionTextField(
                        value = targetRise,
                        onValueChange = { targetRise = it },
                        label = "Target Rise (in)",
                        modifier = Modifier.weight(1f),
                        onFocus = onFocus
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ConstructionTextField(
                        value = treadWidth,
                        onValueChange = { treadWidth = it },
                        label = "Tread Width (in)",
                        modifier = Modifier.weight(1f),
                        onFocus = onFocus
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = floorThick,
                    onValueChange = { floorThick = it },
                    label = "Floor Thick (in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                if (!limitedMode) {
                    Spacer(modifier = Modifier.width(8.dp))
                    ConstructionTextField(
                        value = nosing,
                        onValueChange = { nosing = it },
                        label = "Nosing (in)",
                        modifier = Modifier.weight(1f),
                        onFocus = onFocus
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = sectionName,
                onValueChange = { sectionName = it },
                label = { Text("Section Name / Location") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val tr = FractionUtils.parseFeet(totalRise)
                        val dr = pitchToDouble(targetRise, 7.5)
                        val tw = pitchToDouble(treadWidth, if (limitedMode) 11.25 else 10.25)
                        val ft = pitchToDouble(floorThick, if (limitedMode) 10.75 else 11.875)
                        val ns = pitchToDouble(nosing, 1.0)
                        val lr = pitchToDouble(limitedRun, 0.0)

                        if (tr > 0 && (!limitedMode || lr > 0)) {
                            val res = if (limitedMode) {
                                // Master favors a lower riser count in this mode - 8" target
                                // rise instead of the standard mode's 7.5".
                                StairEngine.calculateLimitedRun(
                                    totRise = tr,
                                    limitedRun = lr,
                                    totalTreadWidth = tw,
                                    desRise = 8.0,
                                    floorThick = ft
                                )
                            } else {
                                StairEngine.calculate(
                                    totRise = tr,
                                    desRise = dr,
                                    treadWidth = tw,
                                    floorThick = ft,
                                    nosing = ns
                                )
                            }
                            results = res

                            val entry = StairEntry(
                                id = if (editingIndex != -1) entries[editingIndex].id else (entries.size + 1).toString(),
                                name = sectionName.ifEmpty { "Stair Section" },
                                rise = totalRise,
                                riserCount = res.riserCount.toString(),
                                actualRise = FractionUtils.formatInches(res.actualRise),
                                treadCount = res.treadCount.toString(),
                                stringerRun = FractionUtils.formatInches(res.stringerRun),
                                totalRun = FractionUtils.formatInches(res.totalRun),
                                stringerLen = FractionUtils.formatInches(res.stringerLen),
                                angleDeg = "${res.angleDeg.roundToOneDecimal()}°",
                                treadsOut = "${res.treadsOut.roundToOneDecimal()}",
                                distOut = FractionUtils.formatInches(res.distOut),
                                headroom = "Clear"
                            )

                            if (editingIndex != -1) {
                                val newList = entries.toMutableList()
                                newList[editingIndex] = entry
                                onUpdateEntries(newList)
                                editingIndex = -1
                            } else {
                                onUpdateEntries(entries + entry)
                            }
                            sectionName = ""
                        }
                    },
                    modifier = Modifier.weight(2f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)
                ) {
                    Text(if (editingIndex != -1) "UPDATE & SAVE" else "CALCULATE & SAVE", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()); results = null },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFC62828)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            results?.let { res ->
                Spacer(modifier = Modifier.height(24.dp))
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("RISERS & TREADS", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE), fontSize = 12.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${res.riserCount} Risers @", fontSize = 16.sp)
                            Text(FractionUtils.formatInches(res.actualRise), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = BlueTool)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${res.treadCount} Treads @", fontSize = 16.sp)
                            Text(FractionUtils.formatInches(res.totalRun / res.treadCount.coerceAtLeast(1)), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = BlueTool)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("STRINGER & LAYOUT", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        StairDiagram(
                            totalRise = FractionUtils.parse(totalRise),
                            totalRun = res.totalRun,
                            riserCount = res.riserCount,
                            floorThickness = FractionUtils.parse(floorThick),
                            nosing = FractionUtils.parse(nosing),
                            treadsOut = res.treadsOut,
                            distOut = res.distOut
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Length: ${FractionUtils.formatInches(res.stringerLen)}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Angle: ${res.angleDeg.roundToOneDecimal()}°", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Distance out for Headroom:", fontSize = 12.sp, color = Color.Gray)
                        Text(FractionUtils.formatInches(res.distOut), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                    }
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED STAIRS", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 1.dp, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("Rise: ${entry.rise} | ${entry.riserCount} Risers", fontSize = 12.sp, color = Color.Gray)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    totalRise = entry.rise
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("ACT RISE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                                    Text(entry.actualRise, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("STRINGER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                                    Text(entry.stringerLen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("ANGLE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                                    Text(entry.angleDeg, fontSize = 14.sp)
                                }
                            }
                            Text("Total Run: ${entry.totalRun} | Dist Out: ${entry.distOut}", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

fun Double.roundToOneDecimal(): Double = (this * 10).roundToInt() / 10.0

// Re-keys a per-project entry map when a project is renamed (every all*Entries map in
// App() is keyed by project name, so a rename has to move data under all of them).
fun <T> Map<String, T>.renameKey(old: String, new: String): Map<String, T> {
    if (!containsKey(old)) return this
    val newMap = toMutableMap()
    newMap[new] = newMap.remove(old)!!
    return newMap
}
fun pitchToDouble(input: String, default: Double): Double = input.toDoubleOrNull() ?: FractionUtils.parse(input).takeIf { it > 0 } ?: default

@Composable
fun TableCell(text: String, width: androidx.compose.ui.unit.Dp, isHeader: Boolean = false, isBold: Boolean = false) {
    Text(
        text = text,
        modifier = Modifier.width(width).padding(horizontal = 4.dp),
        color = if (isHeader) Color.White else Color.Black,
        fontWeight = if (isHeader || isBold) FontWeight.Bold else FontWeight.Normal,
        fontSize = 13.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
fun SummaryTable(title: String, headers: List<String>, content: @Composable ColumnScope.() -> Unit) {
    Text(title, color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
    Spacer(modifier = Modifier.height(8.dp))
    Card(elevation = 2.dp, shape = RoundedCornerShape(4.dp)) {
        val scrollState = rememberScrollState()
        Column(modifier = Modifier.horizontalScroll(scrollState)) {
            Row(modifier = Modifier.background(BlueTool).padding(vertical = 8.dp)) {
                headers.forEach { header ->
                    val width = when (header) {
                        "Name" -> 120.dp
                        "Dimens", "Dimensions", "Dimension" -> 150.dp
                        "Area", "Volume", "Rebar", "Stringer", "Com Len", "Hip Len", "Radius", "Arc Len", "Plates", "Sheets", "Miter", "Bevel", "Hypot", "Angle A", "Angle B", "Surf Area", "Circumf", "Boards", "Fasteners", "Squares", "Bundles", "Joists", "Count", "Spacing", "Drop" -> 100.dp
                        else -> 80.dp
                    }
                    TableCell(header, width, isHeader = true)
                }
            }
            Column { content() }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun ProjectListScreen(
    projects: List<String>,
    onProjectSelected: (String) -> Unit,
    onAddProject: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onRenameProject: (String, String) -> Unit = { _, _ -> },
    onPrintProject: (String) -> Unit,
    onPrintAll: () -> Unit,
    onExportProject: (String) -> Unit = {},
    onImportProjectText: (String) -> String? = { "Import not supported" },
    onBack: () -> Unit
) {
    var newProjectName by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var renamingProject by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var renameError by remember { mutableStateOf<String?>(null) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    val filePicker = rememberFilePicker()

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE))) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Project Management", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("NEW JOB", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onPrintAll,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = BlueTool, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PRINT ALL", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    filePicker.pickFile("json") { text ->
                        importMessage = if (text == null) {
                            "Import cancelled or the file couldn't be read"
                        } else {
                            onImportProjectText(text) ?: "Project imported successfully"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("IMPORT PROJECT")
            }
            importMessage?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, fontSize = 12.sp, color = if (it.contains("success")) Color(0xFF2E7D32) else Color.Red)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("SELECT PROJECT", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                items(projects) { name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                            .clickable { onProjectSelected(name) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, modifier = Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        IconButton(onClick = {
                            renamingProject = name
                            renameText = name
                            renameError = null
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color(0xFFE65100))
                        }
                        IconButton(onClick = { onExportProject(name) }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = Color(0xFF2E7D32))
                        }
                        IconButton(onClick = { onPrintProject(name) }) {
                            Icon(Icons.Default.Share, contentDescription = "Print", tint = BlueTool)
                        }
                        IconButton(onClick = { onDeleteProject(name) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("New Project Name") },
            text = {
                OutlinedTextField(
                    value = newProjectName,
                    onValueChange = { newProjectName = it },
                    label = { Text("Enter name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newProjectName.isNotBlank()) {
                        onAddProject(newProjectName)
                        newProjectName = ""
                        showDialog = false
                    }
                }) { Text("CREATE") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("CANCEL") }
            }
        )
    }

    renamingProject?.let { oldName ->
        AlertDialog(
            onDismissRequest = { renamingProject = null },
            title = { Text("Rename Project") },
            text = {
                Column {
                    Text("Enter new name for '$oldName'", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it; renameError = null },
                        label = { Text("New name") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = renameError != null
                    )
                    renameError?.let { Text(it, color = Color.Red, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newName = renameText.trim()
                    when {
                        newName.isBlank() -> {}
                        newName != oldName && projects.contains(newName) -> renameError = "Project name '$newName' already exists"
                        newName == oldName -> renamingProject = null
                        else -> {
                            onRenameProject(oldName, newName)
                            renamingProject = null
                        }
                    }
                }) { Text("RENAME") }
            },
            dismissButton = {
                TextButton(onClick = { renamingProject = null }) { Text("CANCEL") }
            }
        )
    }
}

@Composable
fun JobSummaryScreen(
    projectName: String,
    stairEntries: List<StairEntry>,
    rafterEntries: List<RafterEntry>,
    arcEntries: List<ArcEntry>,
    wallEntries: List<WallEntry>,
    crownEntries: List<CrownEntry>,
    gazeboEntries: List<GazeboEntry>,
    concreteEntries: List<ConcreteEntry>,
    roofEntries: List<RoofEntry>,
    coordinatePaths: List<PlottedPath>,
    masonryEntries: List<MasonryEntry>,
    drywallEntries: List<DrywallEntry>,
    deckEntries: List<DeckEntry>,
    floorEntries: List<FloorEntry>,
    handrailEntries: List<HandrailEntry>,
    pineLineEntries: List<PineLineEntry>,
    framingEntries: List<FramingEntry> = emptyList(),
    circularStairEntries: List<CircularStairEntry> = emptyList(),
    trigEntries: List<TrigEntry> = emptyList(),
    columnEntries: List<ColumnEntry> = emptyList(),
    squareEntries: List<SquareEntry> = emptyList(),
    surveyEntries: List<SurveyEntry> = emptyList(),
    landSurveyEntries: List<LandSurveyEntry> = emptyList(),
    onBack: () -> Unit
) {
    val pdfExportProvider = rememberPdfExportProvider()

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Job Data: $projectName", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            
            val downloader = rememberFileDownloader()

            IconButton(onClick = {
                val sections = SummaryReportGenerator.generateSections(
                    stairEntries, circularStairEntries, rafterEntries, gazeboEntries,
                    arcEntries, wallEntries, crownEntries, concreteEntries,
                    masonryEntries, drywallEntries, trigEntries, columnEntries,
                    roofEntries, floorEntries, deckEntries, handrailEntries,
                    framingEntries, pineLineEntries, coordinatePaths, squareEntries, surveyEntries, landSurveyEntries
                )
                pdfExportProvider.share(projectName, sections)
            }) {
                Icon(Icons.Default.Share, contentDescription = "Export PDF", tint = Color.White)
            }

            TextButton(onClick = {
                val dxfContent = DxfExporter.generateDxf(
                    stairList = stairEntries,
                    rafterList = rafterEntries,
                    floorList = floorEntries,
                    coordList = coordinatePaths.flatMap { it.points },
                    pineLineList = pineLineEntries,
                    circStairList = circularStairEntries
                )
                downloader.downloadFile("${projectName.replace(" ", "_")}_Drawings.dxf", dxfContent, "application/dxf")
            }) {
                Text("DXF", color = Color.White, fontWeight = FontWeight.Bold)
            }

            TextButton(onClick = {
                val csvContent = buildString {
                    append("Section,Name,Quantity,Dimensions,Primary Result,Secondary Result\n")
                    stairEntries.forEach { append("Stairs,${it.name},${it.riserCount} risers,Rise: ${it.rise},Act Rise: ${it.actualRise},Stringer: ${it.stringerLen}\n") }
                    rafterEntries.forEach { append("Rafters,${it.name},1,Span: ${it.span} Pitch: ${it.pitch},Common: ${it.commonLen},Hip: ${it.hipLen}\n") }
                    concreteEntries.forEach { append("Concrete,${it.name},${it.qty},${it.dimensions},Volume: ${it.volume},Rebar: ${it.rebar}\n") }
                    wallEntries.forEach { append("Walls,${it.name},1,${it.length}x${it.height},Studs: ${it.studs},Plates: ${it.plates}\n") }
                    roofEntries.forEach { append("Roof,${it.name},1,${it.dimensions},Area: ${it.area},Squares: ${it.squares}\n") }
                    floorEntries.forEach { append("Flooring,${it.name},1,${it.length}x${it.width},Joists: ${it.joists},Sheets: ${it.sheets}\n") }
                }
                downloader.downloadFile("${projectName.replace(" ", "_")}_Takeoff.csv", csvContent, "text/csv")
            }) {
                Text("CSV", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            val isEmpty = stairEntries.isEmpty() && rafterEntries.isEmpty() && arcEntries.isEmpty() && wallEntries.isEmpty() &&
                    crownEntries.isEmpty() && gazeboEntries.isEmpty() && concreteEntries.isEmpty() && roofEntries.isEmpty() &&
                    coordinatePaths.isEmpty() && masonryEntries.isEmpty() && drywallEntries.isEmpty() &&
                    deckEntries.isEmpty() && floorEntries.isEmpty() && handrailEntries.isEmpty() && pineLineEntries.isEmpty() &&
                    circularStairEntries.isEmpty() && trigEntries.isEmpty() && columnEntries.isEmpty() && framingEntries.isEmpty()

            if (isEmpty) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No takeoff data recorded for this project.", color = Color.Gray)
                }
            } else {
                if (stairEntries.isNotEmpty()) {
                    SummaryTable("STAIR SCHEDULE", listOf("Name", "Rise", "Risers", "Act Rise", "Treads", "Run", "Stringer", "Angle")) {
                        stairEntries.forEach { entry ->
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Row {
                                    TableCell(entry.name, 120.dp, isBold = true); TableCell(entry.rise, 80.dp)
                                    TableCell(entry.riserCount, 80.dp); TableCell(entry.actualRise, 80.dp)
                                    TableCell(entry.treadCount, 80.dp); TableCell(entry.totalRun, 80.dp)
                                    TableCell(entry.stringerLen, 100.dp); TableCell(entry.angleDeg, 80.dp)
                                }
                                StairDiagram(
                                    totalRise = FractionUtils.parse(entry.rise),
                                    totalRun = FractionUtils.parse(entry.totalRun),
                                    riserCount = entry.riserCount.toIntOrNull() ?: 1,
                                    floorThickness = 11.875,
                                    nosing = 1.0,
                                    treadsOut = 0.0,
                                    distOut = FractionUtils.parse(entry.distOut)
                                )
                            }
                            Divider()
                        }
                    }
                }

                if (circularStairEntries.isNotEmpty()) {
                    SummaryTable("CIRCULAR STAIR SCHEDULE", listOf("Name", "Rise", "Inner Rad", "Tread W", "Treads", "Circle", "Out Radius", "Angle/Trd")) {
                        circularStairEntries.forEach { entry ->
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                TableCell(entry.name, 120.dp, isBold = true); TableCell(entry.totalRise, 80.dp)
                                TableCell(entry.radius, 90.dp); TableCell(entry.walkTread, 80.dp)
                                TableCell(entry.numTreads, 60.dp); TableCell(entry.circleSize, 70.dp)
                                TableCell(entry.outsideRadius, 90.dp); TableCell(entry.anglePerTread, 80.dp)
                            }
                            Divider()
                        }
                    }
                }

                if (rafterEntries.isNotEmpty()) {
                    SummaryTable("RAFTER SCHEDULE", listOf("Name", "Span", "Pitch", "Heel", "O/H", "Com Len", "Hip Len", "Jack 16")) {
                        rafterEntries.forEach { entry ->
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Row {
                                    TableCell(entry.name, 120.dp, isBold = true); TableCell(entry.span, 80.dp)
                                    TableCell(entry.pitch, 80.dp); TableCell(entry.heel, 80.dp)
                                    TableCell(entry.overhang, 80.dp); TableCell(entry.commonLen, 100.dp)
                                    TableCell(entry.hipLen, 100.dp); TableCell(entry.jack16, 80.dp)
                                }
                                RafterDetail(
                                    run = FractionUtils.parse(entry.commonRun),
                                    rise = FractionUtils.parse(entry.commonRise),
                                    pitch = FractionUtils.parse(entry.pitch),
                                    heel = FractionUtils.parse(entry.heel),
                                    lumberDepth = 7.25,
                                    overhang = FractionUtils.parse(entry.overhang).takeIf { it > 0 } ?: 12.0
                                )
                            }
                            Divider()
                        }
                    }
                }

                if (gazeboEntries.isNotEmpty()) {
                    SummaryTable("GAZEBO SCHEDULE", listOf("Name", "Sides", "Diam", "Pitch", "Area", "Com Len", "Hip Len")) {
                        gazeboEntries.forEach { entry ->
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                TableCell(entry.name, 120.dp, isBold = true); TableCell(entry.sides, 80.dp)
                                TableCell(entry.diameter, 80.dp); TableCell(entry.pitch, 80.dp)
                                TableCell(entry.area, 100.dp); TableCell(entry.commonRafterLen, 100.dp); TableCell(entry.hipLen, 100.dp)
                            }
                            Divider()
                        }
                    }
                }

                if (arcEntries.isNotEmpty()) {
                    SummaryTable("ARC SCHEDULE", listOf("Name", "Chord", "Height", "Radius", "Arc Len", "Angle")) {
                        arcEntries.forEach { entry ->
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                TableCell(entry.name, 120.dp, isBold = true); TableCell(entry.chord, 100.dp)
                                TableCell(entry.height, 100.dp); TableCell(entry.radius, 100.dp)
                                TableCell(entry.arcLength, 100.dp); TableCell(entry.arcAngle, 80.dp)
                            }
                            Divider()
                        }
                    }
                }

                if (wallEntries.isNotEmpty()) {
                    SummaryTable("WALL FRAMING SCHEDULE", listOf("Name", "Length", "Height", "Studs", "Plates", "Sheets")) {
                        wallEntries.forEach { entry ->
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                TableCell(entry.name, 120.dp, isBold = true); TableCell(entry.length, 100.dp)
                                TableCell(entry.height, 100.dp); TableCell(entry.studs, 80.dp)
                                TableCell(entry.plates, 100.dp); TableCell(entry.sheets, 100.dp)
                            }
                            Divider()
                        }
                    }
                }

                if (framingEntries.isNotEmpty()) {
                    SummaryTable("OPENINGS SCHEDULE", listOf("Name", "Qty", "Header", "Jacks", "Sill", "Lower Crip", "Upper Crip")) {
                        framingEntries.forEach { entry ->
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                TableCell(entry.name, 100.dp, isBold = true); TableCell(entry.winCount, 40.dp)
                                TableCell(entry.headerLength, 100.dp); TableCell("${entry.jackCount}@${entry.jackLength}", 100.dp)
                                TableCell(if (entry.isDoor) "-" else entry.sillLength, 80.dp)
                                TableCell(if (entry.isDoor) "-" else entry.lowerCrippleLength, 100.dp)
                                TableCell(entry.upperCrippleLength, 100.dp)
                            }
                            Divider()
                        }
                    }
                }

                if (crownEntries.isNotEmpty()) {
                    SummaryTable("CROWN MOLDING SCHEDULE", listOf("Name", "Spring", "Corner", "Miter", "Bevel")) {
                        crownEntries.forEach { entry ->
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                TableCell(entry.name, 120.dp, isBold = true); TableCell(entry.spring, 100.dp)
                                TableCell(entry.wallAngle, 80.dp); TableCell(entry.miter, 100.dp); TableCell(entry.bevel, 100.dp)
                            }
                            Divider()
                        }
                    }
                }

                if (concreteEntries.isNotEmpty()) {
                    SummaryTable("CONCRETE SCHEDULE", listOf("Name", "Qty", "Dimens", "Volume", "Rebar")) {
                        concreteEntries.forEach { entry ->
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Row {
                                    TableCell(entry.name, 120.dp, isBold = true); TableCell(entry.qty, 80.dp)
                                    TableCell(entry.dimensions, 150.dp); TableCell(entry.volume, 100.dp)
                                    TableCell(entry.rebar, 100.dp)
                                }
                                FootingCrossSection(
                                    width = 24.0,
                                    thickness = 12.0,
                                    rebarOC = 12.0
                                )
                            }
                            Divider()
                        }
                    }
                }

                if (masonryEntries.isNotEmpty()) {
                    SummaryTable("MASONRY SCHEDULE", listOf("Name", "SqFt", "Type", "Units", "Mortar", "Sand")) {
                        masonryEntries.forEach { entry ->
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                TableCell(entry.name, 120.dp, isBold = true); TableCell(entry.sqFt, 100.dp)
                                TableCell(entry.type, 70.dp); TableCell(entry.units, 70.dp)
                                TableCell(entry.mortar, 80.dp); TableCell(entry.sand, 70.dp)
                            }
                            Divider()
                        }
                    }
                }

                if (drywallEntries.isNotEmpty()) {
                    SummaryTable("DRYWALL SCHEDULE", listOf("Name", "Area", "Sheets", "Size", "Mud", "Tape")) {
                        drywallEntries.forEach { entry ->
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                TableCell(entry.name, 120.dp, isBold = true); TableCell(entry.area, 100.dp)
                                TableCell(entry.sheets, 100.dp); TableCell(entry.sheetSize, 80.dp)
                                TableCell(entry.mud, 80.dp); TableCell(entry.tape, 80.dp)
                            }
                            Divider()
                        }
                    }
                }

                if (trigEntries.isNotEmpty()) {
                    SummaryTable("TRIG SOLUTIONS SCHEDULE", listOf("Name", "Side A", "Side B", "Hypot", "Angle A", "Angle B")) {
                        trigEntries.forEach { entry ->
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                TableCell(entry.name, 120.dp, isBold = true); TableCell(entry.sideA, 80.dp)
                                TableCell(entry.sideB, 80.dp); TableCell(entry.hypotenuse, 100.dp)
                                TableCell(entry.angleA, 100.dp); TableCell(entry.angleB, 100.dp)
                            }
                            Divider()
                        }
                    }
                }

                if (columnEntries.isNotEmpty()) {
                    SummaryTable("COLUMN / CIRCLE SCHEDULE", listOf("Name", "Diam", "Height", "Volume", "Surf Area", "Circumf")) {
                        columnEntries.forEach { entry ->
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                TableCell(entry.name, 120.dp, isBold = true); TableCell(entry.diameter, 80.dp)
                                TableCell(entry.height, 80.dp); TableCell(entry.volume, 100.dp)
                                TableCell(entry.surfaceArea, 100.dp); TableCell(entry.circumference, 100.dp)
                            }
                            Divider()
                        }
                    }
                }

                if (squareEntries.isNotEmpty()) {
                    SummaryTable("SQUARING SCHEDULE", listOf("Name", "Length", "Width", "Diagonal")) {
                        squareEntries.forEach { entry ->
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                TableCell(entry.name, 120.dp, isBold = true); TableCell(entry.length, 100.dp)
                                TableCell(entry.width, 100.dp); TableCell(entry.diagonal, 100.dp)
                            }
                            Divider()
                        }
                    }
                }

                if (surveyEntries.isNotEmpty()) {
                    SummaryTable("GRADE / SLOPE SCHEDULE", listOf("Name", "Run", "Slope/Ft", "Grade %", "Total Fall")) {
                        surveyEntries.forEach { entry ->
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                TableCell(entry.name, 120.dp, isBold = true); TableCell(entry.distance, 100.dp)
                                TableCell(entry.slope, 80.dp); TableCell(entry.slopePercent, 80.dp)
                                TableCell(entry.totalFall, 100.dp)
                            }
                            Divider()
                        }
                    }
                }

                if (landSurveyEntries.isNotEmpty()) {
                    SummaryTable("LAND SURVEY SCHEDULE", listOf("Note", "Data")) {
                        landSurveyEntries.forEach { entry ->
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                TableCell(entry.note, 120.dp, isBold = true); TableCell(entry.data, 260.dp)
                            }
                            Divider()
                        }
                    }
                }

                if (roofEntries.isNotEmpty()) {
                    SummaryTable("ROOF SCHEDULE", listOf("Name", "Span", "Bldg Len", "Pitch", "Common", "Hip/Valley", "Ridge", "Sub-Fascia", "Squares")) {
                        roofEntries.forEach { entry ->
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                TableCell(entry.name, 120.dp, isBold = true); TableCell(entry.span, 80.dp)
                                TableCell(entry.bldgLen, 80.dp); TableCell(entry.pitch, 60.dp)
                                TableCell(entry.commonInfo, 100.dp)
                                TableCell(if (entry.hipInfo.isNotEmpty()) entry.hipInfo else entry.valleyInfo, 100.dp)
                                TableCell(entry.ridgeInfo, 100.dp); TableCell(entry.subFascia, 100.dp)
                                TableCell(entry.squares, 80.dp)
                            }
                            Divider()
                        }
                    }
                }

                if (floorEntries.isNotEmpty()) {
                    SummaryTable("FLOORING SCHEDULE", listOf("Name", "Length", "Width", "OC", "Joists", "Sheets")) {
                        floorEntries.forEach { entry ->
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                TableCell(entry.name, 120.dp, isBold = true); TableCell(entry.length, 100.dp)
                                TableCell(entry.width, 100.dp); TableCell(entry.spacing, 80.dp)
                                TableCell(entry.joists, 100.dp); TableCell(entry.sheets, 100.dp)
                            }
                            Divider()
                        }
                    }
                }

                if (deckEntries.isNotEmpty()) {
                    SummaryTable("DECKING SCHEDULE", listOf("Name", "Length", "Width", "Gap", "Boards", "Fasteners")) {
                        deckEntries.forEach { entry ->
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                TableCell(entry.name, 120.dp, isBold = true); TableCell(entry.length, 100.dp)
                                TableCell(entry.width, 100.dp); TableCell(entry.gap, 80.dp)
                                TableCell(entry.boards, 100.dp); TableCell(entry.fasteners, 100.dp)
                            }
                            Divider()
                        }
                    }
                }

                if (handrailEntries.isNotEmpty()) {
                    SummaryTable("HANDRAIL SCHEDULE", listOf("Name", "Length", "Count", "Spacing")) {
                        handrailEntries.forEach { entry ->
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                TableCell(entry.name, 120.dp, isBold = true); TableCell(entry.length, 100.dp)
                                TableCell(entry.spindles, 100.dp); TableCell(entry.spacing, 100.dp)
                            }
                            Divider()
                        }
                    }
                }

                if (pineLineEntries.isNotEmpty()) {
                    SummaryTable("PINE LINE SCHEDULE", listOf("Name", "Pitch", "Heel", "Wall", "O/H", "Drop")) {
                        pineLineEntries.forEach { entry ->
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                TableCell(entry.name, 120.dp, isBold = true); TableCell(entry.pitch, 80.dp)
                                TableCell(entry.heel, 80.dp); TableCell(entry.wall, 80.dp)
                                TableCell(entry.overhang, 80.dp); TableCell(entry.drop, 100.dp)
                            }
                            Divider()
                        }
                    }
                }

                if (coordinatePaths.isNotEmpty()) {
                    Text("SITE SURVEY SCHEDULE", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    coordinatePaths.forEach { path ->
                        Card(elevation = 2.dp, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth().background(if (path.isTakeoff) BlueTool else Color.Gray).padding(4.dp)) {
                                    Text(if (path.isTakeoff) "TAKEOFF: ${path.id}" else "BOUNDARY: ${path.id}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                val area = CoordinateEngine.calculateNetArea(path.points.map { CoordinateEngine.Point(it.n, it.e) }, path.arcHeights)
                                Text("Points: ${path.points.size} | Area: ${(area / 144.0).roundToOneDecimal()} sq ft", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun FramingCalculator(
    entries: List<FramingEntry>,
    onUpdateEntries: (List<FramingEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var wallHeight by remember { mutableStateOf("97 1/8") }
    var headerHeightBottom by remember { mutableStateOf("81 3/4") }
    var headerDepth by remember { mutableStateOf("9 1/4") }
    var roWidth by remember { mutableStateOf("") }
    var roHeight by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("1") }
    var winCount by remember { mutableStateOf("1") }
    var spaceBetween by remember { mutableStateOf("0") }
    var studOC by remember { mutableStateOf("16") }
    var isDoor by remember { mutableStateOf(false) }
    var isMultiWindow by remember { mutableStateOf(false) }
    var studSize by remember { mutableStateOf("2x4") }
    var sectionName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var results by remember { mutableStateOf<FramingEngine.Result?>(null) }
    var editingIndex by remember { mutableStateOf(-1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Framing: Final Test", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                backgroundColor = BlueTool
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Help */ }, backgroundColor = BlueTool) {
                Icon(Icons.Default.Info, null, tint = Color.White)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)).padding(padding).verticalScroll(rememberScrollState())) {
            Column(modifier = Modifier.padding(16.dp)) {
                
                // --- Wall Specifications ---
                Text("WALL SPECIFICATIONS", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ConstructionTextField(
                        value = wallHeight,
                        onValueChange = { wallHeight = it },
                        label = "Wall Height (in)",
                        modifier = Modifier.weight(1f),
                        onFocus = onFocus
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    val currentWH = FractionUtils.parse(wallHeight)
                    Text("Stud: ${if (currentWH > 0) FractionUtils.formatInches(currentWH - 4.5) else ""}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // --- Header Specifications ---
                Text("HEADER SPECIFICATIONS", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    ConstructionTextField(
                        value = headerHeightBottom,
                        onValueChange = { headerHeightBottom = it },
                        label = "Hdr Height (to Bottom) (in)",
                        modifier = Modifier.weight(1f),
                        onFocus = onFocus
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ConstructionTextField(
                        value = headerDepth,
                        onValueChange = { headerDepth = it },
                        label = "Hdr Depth (in)",
                        modifier = Modifier.weight(1f),
                        onFocus = onFocus
                    )
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // --- Stud Specifications ---
                Text("STUD SPECIFICATIONS", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = "Stud Size: $studSize",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { IconButton(onClick = { expanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("2x4", "2x6", "2x8").forEach { size ->
                                DropdownMenuItem(onClick = { studSize = size; expanded = false }) { Text(size) }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    ConstructionTextField(
                        value = studOC,
                        onValueChange = { studOC = it },
                        label = "Stud OC (in)",
                        modifier = Modifier.weight(1f),
                        onFocus = onFocus
                    )
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // --- Opening Dimensions ---
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("OPENING DIMENSIONS", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("Multi-Window", fontSize = 11.sp)
                    Switch(checked = isMultiWindow, onCheckedChange = { isMultiWindow = it }, modifier = Modifier.scale(0.8f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Door Mode", fontSize = 11.sp)
                    Switch(checked = isDoor, onCheckedChange = { isDoor = it }, modifier = Modifier.scale(0.8f))
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    ConstructionTextField(
                        value = roWidth,
                        onValueChange = { roWidth = it },
                        label = if (isMultiWindow) "R.O. Width (ea) (in)" else "R.O. Width (in)",
                        modifier = Modifier.weight(1.5f),
                        onFocus = onFocus
                    )
                    if (!isDoor) {
                        Spacer(modifier = Modifier.width(8.dp))
                        ConstructionTextField(
                            value = roHeight,
                            onValueChange = { roHeight = it },
                            label = "R.O. Height (in)",
                            modifier = Modifier.weight(1.5f),
                            onFocus = onFocus
                        )
                    }
                    if (isMultiWindow) {
                        Spacer(modifier = Modifier.width(8.dp))
                        ConstructionTextField(
                            value = winCount,
                            onValueChange = { winCount = it },
                            label = "Windows in Group",
                            modifier = Modifier.weight(1f),
                            onFocus = onFocus
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    ConstructionTextField(
                        value = qty,
                        onValueChange = { qty = it },
                        label = "Qty",
                        modifier = Modifier.weight(1f),
                        onFocus = onFocus
                    )
                }
                
                if (isMultiWindow) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ConstructionTextField(
                        value = spaceBetween,
                        onValueChange = { spaceBetween = it },
                        label = "Gap Between (in)",
                        modifier = Modifier.fillMaxWidth(),
                        onFocus = onFocus
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = sectionName,
                    onValueChange = { sectionName = it },
                    label = { Text("Location / Section Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Layout Notes") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            val wh = FractionUtils.parseFeet(wallHeight)
                            val hhb = FractionUtils.parse(headerHeightBottom)
                            val hd = FractionUtils.parse(headerDepth)
                            val rw = FractionUtils.parseFeet(roWidth)
                            val rh = if (isDoor) 0.0 else FractionUtils.parseFeet(roHeight)
                            val wc = winCount.toIntOrNull() ?: 1
                            val qtyVal = qty.toIntOrNull() ?: 1
                            val sb = FractionUtils.parse(spaceBetween)
                            val oc = FractionUtils.parse(studOC).takeIf { it > 0 } ?: 16.0

                            if (wh > 0 && rw > 0) {
                                val params = FramingEngine.Params(
                                    wallHeight = wh,
                                    headerHeightBottom = hhb,
                                    headerDepth = hd,
                                    roWidth = rw,
                                    roHeight = rh,
                                    winCount = if (isMultiWindow) wc else 1,
                                    spaceBetween = sb,
                                    studOC = oc,
                                    isDoor = isDoor,
                                    studSize = studSize,
                                    qty = qtyVal
                                )
                                val res = FramingEngine.calculate(params)
                                results = res

                                val entry = FramingEntry(
                                    id = if (editingIndex != -1) entries[editingIndex].id else (entries.size + 1).toString(),
                                    name = sectionName.ifEmpty { "Opening ${entries.size + 1}" },
                                    wallHeight = wallHeight,
                                    headerHeightBottom = headerHeightBottom,
                                    headerDepth = headerDepth,
                                    roWidth = roWidth,
                                    roHeight = if (isDoor) "N/A" else roHeight,
                                    qty = qty,
                                    winCount = if (isMultiWindow) winCount else "1",
                                    spaceBetween = spaceBetween,
                                    studOC = studOC,
                                    isDoor = isDoor,
                                    isMultiWindow = isMultiWindow,
                                    studSize = studSize,
                                    notes = notes,
                                    studHeight = FractionUtils.formatInches(res.studHeight),
                                    headerLength = FractionUtils.formatInches(res.headerLength),
                                    headerNominal = res.headerNominal,
                                    jackCount = res.jackCount.toString(),
                                    jackLength = FractionUtils.formatInches(res.jackLength),
                                    sillCount = res.sillCount.toString(),
                                    sillLength = FractionUtils.formatInches(res.sillLength),
                                    lowerCrippleCount = res.lowerCrippleCount.toString(),
                                    lowerCrippleLength = FractionUtils.formatInches(res.lowerCrippleLength),
                                    upperCrippleCount = res.upperCrippleCount.toString(),
                                    upperCrippleLength = FractionUtils.formatInches(res.upperCrippleLength)
                                )

                                if (editingIndex != -1) {
                                    val newList = entries.toMutableList()
                                    newList[editingIndex] = entry
                                    onUpdateEntries(newList)
                                    editingIndex = -1
                                } else {
                                    onUpdateEntries(entries + entry)
                                }
                                sectionName = ""; notes = ""; roWidth = ""; roHeight = ""; winCount = "1"; qty = "1"
                            }
                        },
                        modifier = Modifier.weight(2f).height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)
                    ) {
                        Text(if (editingIndex != -1) "UPDATE OPENING" else "ADD OPENING", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = { onUpdateEntries(emptyList()); results = null },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFD32F2F)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                    ) {
                        Text("CLEAR", fontWeight = FontWeight.Bold)
                    }
                }

                if (entries.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).background(Color.White)) {
                        Column {
                            // Header
                            Row(modifier = Modifier.background(BlueTool).padding(vertical = 12.dp, horizontal = 4.dp)) {
                                ResultHeaderCell("Name", 100.dp)
                                ResultHeaderCell("Qty", 50.dp)
                                ResultHeaderCell("Header size", 110.dp)
                                ResultHeaderCell("Jacks", 120.dp)
                                ResultHeaderCell("Sill", 110.dp)
                                ResultHeaderCell("Lower Cripples", 130.dp)
                                ResultHeaderCell("Upper cripples", 130.dp)
                            }
                            // Rows
                            entries.forEachIndexed { index, entry ->
                                Row(modifier = Modifier.background(Color.White).padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    ResultCell(entry.name, 100.dp, isBold = true)
                                    ResultCell(entry.qty, 50.dp)
                                    ResultCell("${entry.headerNominal} x ${entry.headerLength}", 110.dp)
                                    ResultCell("${entry.jackCount} - ${entry.studSize} x ${entry.jackLength}", 120.dp)
                                    ResultCell(if (entry.isDoor) "-" else "${entry.sillCount} - ${entry.studSize} x ${entry.sillLength}", 110.dp)
                                    ResultCell(if (entry.isDoor) "-" else "${entry.lowerCrippleCount} - ${entry.studSize} x ${entry.lowerCrippleLength}", 130.dp)
                                    ResultCell("${entry.upperCrippleCount} - ${entry.studSize} x ${entry.upperCrippleLength}", 130.dp)
                                    
                                    IconButton(onClick = {
                                        editingIndex = index
                                        sectionName = entry.name
                                        notes = entry.notes
                                        wallHeight = entry.wallHeight
                                        headerHeightBottom = entry.headerHeightBottom
                                        headerDepth = entry.headerDepth
                                        roWidth = entry.roWidth
                                        roHeight = if (entry.roHeight == "N/A") "" else entry.roHeight
                                        qty = entry.qty
                                        winCount = entry.winCount
                                        spaceBetween = entry.spaceBetween
                                        studOC = entry.studOC
                                        isDoor = entry.isDoor
                                        isMultiWindow = entry.isMultiWindow
                                        studSize = entry.studSize
                                    }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray) }
                                    IconButton(onClick = {
                                        val newList = entries.toMutableList()
                                        newList.removeAt(index)
                                        onUpdateEntries(newList)
                                    }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray) }
                                }
                                Divider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultHeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        modifier = Modifier.width(width).padding(4.dp),
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        textAlign = TextAlign.Start
    )
}

@Composable
fun ResultCell(text: String, width: androidx.compose.ui.unit.Dp, isBold: Boolean = false) {
    Text(
        text = text,
        modifier = Modifier.width(width).padding(4.dp),
        fontSize = 12.sp,
        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Start
    )
}

@Composable
fun ResultRow(label: String, value: String, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp)
        Text(value, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp, color = if (isBold) BlueTool else Color.Black)
    }
}
@Composable
fun WallCalculator(
    entries: List<WallEntry>,
    onUpdateEntries: (List<WallEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var length by remember { mutableStateOf("") }
    var spacing by remember { mutableStateOf("16") }
    var height by remember { mutableStateOf("8'") }
    var sectionName by remember { mutableStateOf("") }

    var results by remember { mutableStateOf<WallResults?>(null) }
    var editingIndex by remember { mutableStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Wall Framing", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("WALL DIMENSIONS", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            ConstructionTextField(
                value = length,
                onValueChange = { length = it },
                label = "Wall Length (ft / in)",
                modifier = Modifier.fillMaxWidth(),
                onFocus = onFocus
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = spacing,
                    onValueChange = { spacing = it },
                    label = "Stud O.C. (in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = "Wall Height (ft/in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = sectionName,
                onValueChange = { sectionName = it },
                label = { Text("Section Name / Location") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val l = FractionUtils.parseFeet(length) / 12.0
                        val s = spacing.toDoubleOrNull() ?: 16.0
                        val h = FractionUtils.parseFeet(height) / 12.0
                        
                        if (l > 0) {
                            val studs = WallEngine.estimateStuds(l, s)
                            val plates = WallEngine.estimatePlates(l, h)
                            val blocking = WallEngine.estimateBlocking(l, h)
                            val sheets = WallEngine.estimateSheathing(l, h)

                            val res = WallResults(
                                studs = studs.toString(),
                                plates = "${plates.roundToInt()} lin ft",
                                blocking = "${blocking.roundToInt()} lin ft",
                                sheets = sheets.toString()
                            )
                            results = res

                            val entry = WallEntry(
                                id = if (editingIndex != -1) entries[editingIndex].id else (entries.size + 1).toString(),
                                name = sectionName.ifEmpty { "Wall Section" },
                                length = length,
                                height = height,
                                spacing = "$spacing\" OC",
                                studs = res.studs,
                                plates = res.plates,
                                blocking = res.blocking,
                                sheets = res.sheets
                            )

                            if (editingIndex != -1) {
                                val newList = entries.toMutableList()
                                newList[editingIndex] = entry
                                onUpdateEntries(newList)
                                editingIndex = -1
                            } else {
                                onUpdateEntries(entries + entry)
                            }
                            sectionName = ""
                        }
                    },
                    modifier = Modifier.weight(2f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)
                ) {
                    Text(if (editingIndex != -1) "UPDATE & SAVE" else "CALCULATE & SAVE", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()); results = null },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFC62828)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            results?.let { res ->
                Spacer(modifier = Modifier.height(24.dp))
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ESTIMATED MATERIALS", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Studs:", fontSize = 16.sp)
                            Text(res.studs, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = BlueTool)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Plate Footage:", fontSize = 16.sp)
                            Text(res.plates, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GrayBtn)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Blocking Footage:", fontSize = 16.sp)
                            Text(res.blocking, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GrayBtn)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sheathing (4x8):", fontSize = 16.sp)
                            Text(res.sheets, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GrayBtn)
                        }
                    }
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED WALLS", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 1.dp, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("Length: ${entry.length} | Height: ${entry.height} | ${entry.spacing}", fontSize = 12.sp, color = Color.Gray)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    length = entry.length
                                    height = entry.height
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("${entry.studs} Studs | ${entry.plates} Plates | ${entry.blocking} Block | ${entry.sheets} Sheets", fontWeight = FontWeight.Bold, color = BlueTool)
                        }
                    }
                }
            }
        }
    }
}

data class WallResults(val studs: String, val plates: String, val blocking: String, val sheets: String)

@Composable
fun CrownCalculator(
    entries: List<CrownEntry>,
    onUpdateEntries: (List<CrownEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var springAngle by remember { mutableStateOf("38") }
    var wallAngle by remember { mutableStateOf("90") }
    var sectionName by remember { mutableStateOf("") }

    var results by remember { mutableStateOf<CrownResults?>(null) }
    var editingIndex by remember { mutableStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Crown Molding", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("MOLDING & CORNER ANGLES", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = springAngle,
                    onValueChange = { springAngle = it },
                    label = "Spring Angle (°)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = wallAngle,
                    onValueChange = { wallAngle = it },
                    label = "Wall Corner Angle (°)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Text("Tip: Enter the actual wall corner angle (e.g. 90° for a square corner)", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = sectionName,
                onValueChange = { sectionName = it },
                label = { Text("Section Name / Room") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val s = FractionUtils.parse(springAngle).takeIf { it > 0 } ?: 38.0
                        val w = FractionUtils.parse(wallAngle).takeIf { it > 0 } ?: 90.0
                        
                        val settings = CrownEngine.calculateSettings(s, w)
                        val res = CrownResults(
                            miter = "${settings.first.roundToOneDecimal()}°",
                            bevel = "${settings.second.roundToOneDecimal()}°"
                        )
                        results = res

                        val entry = CrownEntry(
                            id = if (editingIndex != -1) entries[editingIndex].id else (entries.size + 1).toString(),
                            name = sectionName.ifEmpty { "Crown Section" },
                            spring = "$s°",
                            wallAngle = "$w°",
                            miter = res.miter,
                            bevel = res.bevel
                        )

                        if (editingIndex != -1) {
                            val newList = entries.toMutableList()
                            newList[editingIndex] = entry
                            onUpdateEntries(newList)
                            editingIndex = -1
                        } else {
                            onUpdateEntries(entries + entry)
                        }
                        sectionName = ""
                    },
                    modifier = Modifier.weight(2f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)
                ) {
                    Text(if (editingIndex != -1) "UPDATE & SAVE" else "CALCULATE & SAVE", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()); results = null },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFC62828)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            results?.let { res ->
                Spacer(modifier = Modifier.height(24.dp))
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("COMPOUND SAW SETTINGS", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Miter Angle:", fontSize = 14.sp)
                                Text(res.miter, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = BlueTool)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Bevel Angle:", fontSize = 14.sp)
                                Text(res.bevel, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = BlueTool)
                            }
                        }
                    }
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED CROWN SETTINGS", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                entries.reversed().forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.name, fontWeight = FontWeight.Bold)
                            Text("Miter: ${entry.miter} | Bevel: ${entry.bevel}", fontSize = 12.sp, color = Color.Gray)
                        }
                        IconButton(onClick = {
                            editingIndex = entries.size - 1 - index
                            sectionName = entry.name
                            springAngle = entry.spring.replace("°", "")
                            wallAngle = entry.wallAngle.replace("°", "")
                        }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray) }
                        IconButton(onClick = {
                            val newList = entries.toMutableList()
                            newList.removeAt(entries.size - 1 - index)
                            onUpdateEntries(newList)
                        }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray) }
                    }
                }
            }
        }
    }
}

data class CrownResults(val miter: String, val bevel: String)

@Composable
fun GazeboCalculator(
    entries: List<GazeboEntry>,
    onUpdateEntries: (List<GazeboEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var sides by remember { mutableStateOf("8") }
    var diameter by remember { mutableStateOf("") }
    var pitch by remember { mutableStateOf("6") }
    var ridgeSpan by remember { mutableStateOf("0") }
    var overhang by remember { mutableStateOf("12") }
    var heel by remember { mutableStateOf("4") }
    var joistOC by remember { mutableStateOf("16") }
    var sectionName by remember { mutableStateOf("") }

    var results by remember { mutableStateOf<GazeboEngine.Result?>(null) }
    var diagramMode by remember { mutableStateOf(GazeboMode.LAYOUT) }
    var editingIndex by remember { mutableStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Gazebo Master", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("POLYGON DIMENSIONS (INCIRCLE)", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = sides,
                    onValueChange = { sides = it },
                    label = "Number of Sides",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = diameter,
                    onValueChange = { diameter = it },
                    label = "Total Span (ft/in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = pitch,
                    onValueChange = { pitch = it },
                    label = "Roof Pitch (/12)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = ridgeSpan,
                    onValueChange = { ridgeSpan = it },
                    label = "Ridge Span (in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = overhang,
                    onValueChange = { overhang = it },
                    label = "Overhang (in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = heel,
                    onValueChange = { heel = it },
                    label = "Heel (HAP)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = joistOC,
                    onValueChange = { joistOC = it },
                    label = "Joist O.C.",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = sectionName,
                onValueChange = { sectionName = it },
                label = { Text("Section Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val sCount = sides.toIntOrNull() ?: 8
                        val d = FractionUtils.parseFeet(diameter)
                        val p = pitch.toDoubleOrNull() ?: 0.0
                        val rs = FractionUtils.parse(ridgeSpan)
                        val oh = FractionUtils.parse(overhang)
                        val h = FractionUtils.parse(heel)
                        val oc = FractionUtils.parse(joistOC).takeIf { it > 0 } ?: 16.0
                        
                        if (d > 0 && sCount >= 3) {
                            val params = GazeboEngine.Params(d, rs, sCount, p, oh, h, oc)
                            val res = GazeboEngine.calculate(params)
                            results = res

                            val entry = GazeboEntry(
                                id = if (editingIndex != -1) entries[editingIndex].id else (entries.size + 1).toString(),
                                name = sectionName.ifEmpty { "$sCount-Sided Gazebo" },
                                sides = sCount.toString(),
                                diameter = diameter,
                                ridgeSpan = ridgeSpan,
                                pitch = pitch,
                                overhang = overhang,
                                heel = heel,
                                sideLength = FractionUtils.formatInches(res.sideLen),
                                apothem = FractionUtils.formatInches(res.apothem),
                                perimeter = FractionUtils.formatInches(res.sideLen * sCount),
                                interiorAngle = "${(180.0 * (sCount - 2.0) / sCount).roundToOneDecimal()}°",
                                miter = "${(180.0 / sCount).roundToOneDecimal()}°",
                                area = "${(0.5 * res.apothem * res.sideLen * sCount / 144.0).roundToOneDecimal()} sq ft",
                                verticalRise = FractionUtils.formatInches(res.verticalRise),
                                commonRafterLen = FractionUtils.formatInches(res.mainRafterLen),
                                commonOverall = FractionUtils.formatInches(res.overallMainLen),
                                hipLen = FractionUtils.formatInches(res.hipRafterLen),
                                hipOverall = FractionUtils.formatInches(res.overallHipLen),
                                commonPlumbSeat = run {
                                    val commonAngle = atan(p / 12.0) * (180.0 / PI)
                                    "${commonAngle.roundToOneDecimal()}° / ${(90.0 - commonAngle).roundToOneDecimal()}°"
                                },
                                hipPlumbSeat = run {
                                    val hipAngle = atan(res.hipPitch / 12.0) * (180.0 / PI)
                                    "${hipAngle.roundToOneDecimal()}° / ${(90.0 - hipAngle).roundToOneDecimal()}°"
                                },
                                ridgeOffset = "N/A",
                                joistOC = joistOC
                            )

                            if (editingIndex != -1) {
                                val newList = entries.toMutableList()
                                newList[editingIndex] = entry
                                onUpdateEntries(newList)
                                editingIndex = -1
                            } else {
                                onUpdateEntries(entries + entry)
                            }
                            sectionName = ""
                        }
                    },
                    modifier = Modifier.weight(2f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)
                ) {
                    Text(if (editingIndex != -1) "UPDATE & SAVE" else "CALCULATE & SAVE", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()); results = null },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFC62828)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            results?.let { res ->
                Spacer(modifier = Modifier.height(24.dp))
                
                // Diagram Mode Tabs
                Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    GazeboTabButton("LAYOUT", diagramMode == GazeboMode.LAYOUT) { diagramMode = GazeboMode.LAYOUT }
                    GazeboTabButton("RAFTER", diagramMode == GazeboMode.RAFTER) { diagramMode = GazeboMode.RAFTER }
                    GazeboTabButton("FLOOR", diagramMode == GazeboMode.FLOOR) { diagramMode = GazeboMode.FLOOR }
                }
                
                GazeboDiagram(
                    mode = diagramMode,
                    params = GazeboEngine.Params(
                        FractionUtils.parse(diameter), 
                        FractionUtils.parse(ridgeSpan), 
                        sides.toIntOrNull() ?: 8, 
                        pitch.toDoubleOrNull() ?: 0.0, 
                        FractionUtils.parse(overhang), 
                        FractionUtils.parse(heel), 
                        FractionUtils.parse(joistOC).takeIf { it > 0 } ?: 16.0
                    ),
                    res = res
                )

                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("LAYOUT", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Side Length", fontSize = 11.sp, color = Color.Gray)
                                Text(FractionUtils.formatInches(res.sideLen), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = BlueTool)
                            }
                            Column {
                                Text("Perimeter", fontSize = 11.sp, color = Color.Gray)
                                Text(FractionUtils.formatInches(res.sideLen * (sides.toIntOrNull() ?: 8)), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Apothem (b)", fontSize = 11.sp, color = Color.Gray)
                                Text(FractionUtils.formatInches(res.apothem), fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Radius (c)", fontSize = 11.sp, color = Color.Gray)
                                Text(FractionUtils.formatInches(res.radius), fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Ridge Len", fontSize = 11.sp, color = Color.Gray)
                                Text(FractionUtils.formatInches(res.ridgeLen), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ANGLES & CUTS", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Miter Angle:", fontSize = 11.sp, color = Color.Gray)
                                Text("${(180.0 / (sides.toIntOrNull() ?: 8)).roundToOneDecimal()}°", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = BlueTool)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hip Pitch:", fontSize = 11.sp, color = Color.Gray)
                                Text("${res.hipPitch.roundToOneDecimal()} / 12", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                // Common Rafter Card
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("COMMON RAFTER", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE), fontSize = 12.sp)
                        Text(FractionUtils.formatInches(res.mainRafterLen), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                        Text("Overall (inc Tail): ${FractionUtils.formatInches(res.overallMainLen)}", fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("HIP / CORNER RAFTER", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE), fontSize = 12.sp)
                        Text(FractionUtils.formatInches(res.hipRafterLen), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                        Text("Overall (inc Tail): ${FractionUtils.formatInches(res.overallHipLen)}", fontSize = 14.sp)
                        Text("Total Rise: ${FractionUtils.formatInches(res.verticalRise)}", fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED GAZEBOS", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 1.dp, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("${entry.sides} Sides | ${entry.diameter} Diam | ${entry.pitch}/12", fontSize = 12.sp, color = Color.Gray)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    sides = entry.sides
                                    diameter = entry.diameter
                                    pitch = entry.pitch
                                    ridgeSpan = entry.ridgeSpan
                                    overhang = entry.overhang
                                    heel = entry.heel
                                    joistOC = entry.joistOC
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("COMMON", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                                    Text("Len: ${entry.commonRafterLen}", fontSize = 12.sp)
                                    Text("O/A: ${entry.commonOverall}", fontSize = 12.sp)
                                }
                                Column {
                                    Text("HIP / CORNER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                                    Text("Len: ${entry.hipLen}", fontSize = 12.sp)
                                    Text("O/A: ${entry.hipOverall}", fontSize = 12.sp)
                                }
                            }
                            Text("Area: ${entry.area} | Perim: ${entry.perimeter}", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GazeboTabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp),
        color = if (selected) BlueTool else Color.Gray,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        fontSize = 12.sp
    )
}

@Composable
fun ArcCalculator(
    entries: List<ArcEntry>,
    onUpdateEntries: (List<ArcEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var chord by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var sectionName by remember { mutableStateOf("") }

    var results by remember { mutableStateOf<ArcResults?>(null) }
    var editingIndex by remember { mutableStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Arc / Radius", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("ARC DIMENSIONS", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            ConstructionTextField(
                value = chord,
                onValueChange = { chord = it },
                label = "Chord Length (Width - ft/in)",
                modifier = Modifier.fillMaxWidth(),
                onFocus = onFocus
            )
            Spacer(modifier = Modifier.height(8.dp))
            ConstructionTextField(
                value = height,
                onValueChange = { height = it },
                label = "Arc Height (Rise - in)",
                modifier = Modifier.fillMaxWidth(),
                onFocus = onFocus
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = sectionName,
                onValueChange = { sectionName = it },
                label = { Text("Section Name / Location") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        // Matches master's CurvesActivity exactly: bare numbers are inches
                        // for both fields (no feet auto-conversion), unlike most other
                        // "(ft/in)"-labeled fields in this app.
                        val c = FractionUtils.parse(chord)
                        val h = FractionUtils.parse(height)

                        if (c > 0 && h > 0) {
                            val r = ArcEngine.solveFromChordAndHeight(c, h)
                            val thetaRad = 2.0 * asin(c / (2.0 * r))
                            val angleDeg = thetaRad * (180.0 / PI)
                            val arcLen = ArcEngine.arcLength(r, angleDeg)

                            val res = ArcResults(
                                radius = FractionUtils.formatInches(r),
                                arcLength = FractionUtils.formatInches(arcLen),
                                arcAngle = "${angleDeg.roundToOneDecimal()}°",
                                chord = chord,
                                height = height
                            )
                            results = res

                            val entry = ArcEntry(
                                id = if (editingIndex != -1) entries[editingIndex].id else (entries.size + 1).toString(),
                                name = sectionName.ifEmpty { "Arc Section" },
                                chord = chord,
                                height = height,
                                radius = res.radius,
                                arcLength = res.arcLength,
                                arcAngle = res.arcAngle
                            )

                            if (editingIndex != -1) {
                                val newList = entries.toMutableList()
                                newList[editingIndex] = entry
                                onUpdateEntries(newList)
                                editingIndex = -1
                            } else {
                                onUpdateEntries(entries + entry)
                            }
                            sectionName = ""
                        }
                    },
                    modifier = Modifier.weight(2f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)
                ) {
                    Text(if (editingIndex != -1) "UPDATE & SAVE" else "CALCULATE & SAVE", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()); results = null },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFC62828)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            results?.let { res ->
                Spacer(modifier = Modifier.height(24.dp))
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("RADIUS", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE), fontSize = 12.sp)
                        Text(res.radius, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ARC LENGTH", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE), fontSize = 12.sp)
                                Text(res.arcLength, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GrayBtn)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ARC ANGLE", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE), fontSize = 12.sp)
                                Text(res.arcAngle, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GrayBtn)
                            }
                        }
                    }
                }
            }
            
            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED ARCS", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 1.dp, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("Chord: ${entry.chord} | Height: ${entry.height}", fontSize = 12.sp, color = Color.Gray)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    chord = entry.chord
                                    height = entry.height
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("Radius: ${entry.radius}", fontWeight = FontWeight.Bold, color = BlueTool)
                            Text("Arc: ${entry.arcLength} @ ${entry.arcAngle}", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

data class ArcResults(
    val radius: String,
    val arcLength: String,
    val arcAngle: String,
    val chord: String,
    val height: String
)

@Composable
fun SquareCalculator(
    entries: List<SquareEntry>,
    onUpdateEntries: (List<SquareEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var length by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }
    var sectionName by remember { mutableStateOf("") }

    var diagonal by remember { mutableStateOf<Double?>(null) }
    var editingIndex by remember { mutableStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Squaring / Diagonal", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("BUILDING DIMENSIONS (3-4-5 CHECK)", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = length,
                    onValueChange = { length = it },
                    label = "Length (ft/in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = width,
                    onValueChange = { width = it },
                    label = "Width (ft/in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = sectionName,
                onValueChange = { sectionName = it },
                label = { Text("Section Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        // Matches master's SquareCalcActivity exactly: bare numbers are
                        // inches (FractionUtils.parse), not auto-converted feet.
                        val l = FractionUtils.parse(length)
                        val w = FractionUtils.parse(width)

                        if (l > 0 && w > 0) {
                            val diag = sqrt(l.pow(2) + w.pow(2))
                            diagonal = diag

                            val entry = SquareEntry(
                                id = if (editingIndex != -1) entries[editingIndex].id else (entries.size + 1).toString(),
                                name = sectionName.ifEmpty { "Main Slab" },
                                length = length,
                                width = width,
                                diagonal = FractionUtils.formatInches(diag)
                            )

                            if (editingIndex != -1) {
                                val newList = entries.toMutableList()
                                newList[editingIndex] = entry
                                onUpdateEntries(newList)
                                editingIndex = -1
                            } else {
                                onUpdateEntries(entries + entry)
                            }
                            sectionName = ""
                        }
                    },
                    modifier = Modifier.weight(2f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)
                ) {
                    Text(if (editingIndex != -1) "UPDATE & SAVE" else "CALCULATE & SAVE", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()); diagonal = null },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFC62828)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            diagonal?.let { diag ->
                Spacer(modifier = Modifier.height(24.dp))
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("DIAGONAL", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE), fontSize = 12.sp)
                        Text(FractionUtils.formatInches(diag), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                        Text("If both diagonals match, the corners are square.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED SECTIONS", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 1.dp, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("${entry.length} x ${entry.width}", fontSize = 12.sp, color = Color.Gray)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    length = entry.length
                                    width = entry.width
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("Diagonal: ${entry.diagonal}", fontWeight = FontWeight.Bold, color = BlueTool)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GradeSlopeCalculator(
    entries: List<SurveyEntry>,
    onUpdateEntries: (List<SurveyEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var distance by remember { mutableStateOf("") }
    var totalFall by remember { mutableStateOf("") }
    var sectionName by remember { mutableStateOf("") }

    var results by remember { mutableStateOf<Triple<Double, Double, Double>?>(null) } // slopePerFt, percent, fallIn
    var editingIndex by remember { mutableStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Grade / Slope", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("SITE GRADE", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = distance,
                    onValueChange = { distance = it },
                    label = "Horizontal Run (ft)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = totalFall,
                    onValueChange = { totalFall = it },
                    label = "Total Vertical Fall (in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = sectionName,
                onValueChange = { sectionName = it },
                label = { Text("Point / Section Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        // Matches master's SiteSurveyActivity exactly: horizontal run is
                        // bare-number-as-feet, vertical fall is bare-number-as-inches.
                        val distIn = FractionUtils.parseFeet(distance)
                        val fallIn = FractionUtils.parse(totalFall)

                        if (distIn > 0 && fallIn != 0.0) {
                            val slopePerFt = fallIn / (distIn / 12.0)
                            val percent = (fallIn / distIn) * 100.0
                            results = Triple(slopePerFt, percent, fallIn)

                            val entry = SurveyEntry(
                                id = if (editingIndex != -1) entries[editingIndex].id else (entries.size + 1).toString(),
                                name = sectionName.ifEmpty { "Point ${entries.size + 1}" },
                                distance = FractionUtils.formatInches(distIn),
                                slope = FractionUtils.formatInches(slopePerFt),
                                slopePercent = sprintf("%.2f%%", percent),
                                totalFall = FractionUtils.formatInches(fallIn)
                            )

                            if (editingIndex != -1) {
                                val newList = entries.toMutableList()
                                newList[editingIndex] = entry
                                onUpdateEntries(newList)
                                editingIndex = -1
                            } else {
                                onUpdateEntries(entries + entry)
                            }
                            sectionName = ""
                        }
                    },
                    modifier = Modifier.weight(2f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)
                ) {
                    Text(if (editingIndex != -1) "UPDATE & SAVE" else "CALCULATE & SAVE", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()); results = null },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFC62828)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            results?.let { (slopePerFt, percent, fallIn) ->
                Spacer(modifier = Modifier.height(24.dp))
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("GRADE RESULTS", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(sprintf("%.2f%%", percent), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                        Text("Grade", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Slope per Foot", fontSize = 11.sp, color = Color.Gray)
                                Text(FractionUtils.formatInches(slopePerFt), fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total Fall", fontSize = 11.sp, color = Color.Gray)
                                Text(FractionUtils.formatInches(fallIn), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED GRADE POINTS", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 1.dp, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("Run: ${entry.distance} | Fall: ${entry.totalFall}", fontSize = 12.sp, color = Color.Gray)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    distance = entry.distance
                                    totalFall = entry.totalFall
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("Slope: ${entry.slope} | Grade: ${entry.slopePercent}", fontWeight = FontWeight.Bold, color = BlueTool)
                        }
                    }
                }
            }
        }
    }
}

// Matches master's LandSurveyActivity.decimalToDMS/formatQuadrantBearing exactly.
private fun decimalDegreesToDMS(decimal: Double): String {
    val d = floor(decimal).toInt()
    val mFloat = (decimal - d) * 60.0
    val m = floor(mFloat).toInt()
    val s = (mFloat - m) * 60.0
    return sprintf("%d° %02d' %02.0f\"", d, m, s)
}

private fun formatQuadrantBearing(azimuth: Double): String {
    return when {
        azimuth <= 90.0 -> sprintf("N %s E", decimalDegreesToDMS(azimuth))
        azimuth <= 180.0 -> sprintf("S %s E", decimalDegreesToDMS(180.0 - azimuth))
        azimuth <= 270.0 -> sprintf("S %s W", decimalDegreesToDMS(azimuth - 180.0))
        else -> sprintf("N %s W", decimalDegreesToDMS(360.0 - azimuth))
    }
}

@Composable
fun LandSurveyCalculator(
    entries: List<LandSurveyEntry>,
    onUpdateEntries: (List<LandSurveyEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var sqFt by remember { mutableStateOf("") }
    var acreageResult by remember { mutableStateOf<String?>(null) }

    var slopeDist by remember { mutableStateOf("") }
    var slopeAngle by remember { mutableStateOf("") }
    var slopeResult by remember { mutableStateOf<String?>(null) }

    var n1 by remember { mutableStateOf("") }
    var e1 by remember { mutableStateOf("") }
    var n2 by remember { mutableStateOf("") }
    var e2 by remember { mutableStateOf("") }
    var inverseResult by remember { mutableStateOf<String?>(null) }

    var showSaveDialog by remember { mutableStateOf(false) }
    var saveNote by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Land Surveying", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("ACREAGE", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            ConstructionTextField(value = sqFt, onValueChange = { sqFt = it }, label = "Area (sq ft)", modifier = Modifier.fillMaxWidth(), onFocus = onFocus)
            acreageResult?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, fontWeight = FontWeight.Bold, color = BlueTool, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("SLOPE RESOLVE", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(value = slopeDist, onValueChange = { slopeDist = it }, label = "Slope Distance (ft/in)", modifier = Modifier.weight(1f), onFocus = onFocus)
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(value = slopeAngle, onValueChange = { slopeAngle = it }, label = "Angle (°)", modifier = Modifier.weight(1f), onFocus = onFocus)
            }
            slopeResult?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, fontWeight = FontWeight.Bold, color = BlueTool, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("INVERSE (COGO)", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(value = n1, onValueChange = { n1 = it }, label = "N1", modifier = Modifier.weight(1f), onFocus = onFocus)
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(value = e1, onValueChange = { e1 = it }, label = "E1", modifier = Modifier.weight(1f), onFocus = onFocus)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(value = n2, onValueChange = { n2 = it }, label = "N2", modifier = Modifier.weight(1f), onFocus = onFocus)
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(value = e2, onValueChange = { e2 = it }, label = "E2", modifier = Modifier.weight(1f), onFocus = onFocus)
            }
            inverseResult?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, fontWeight = FontWeight.Bold, color = BlueTool, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        // 1. Acreage
                        val sq = FractionUtils.parse(sqFt)
                        if (sq > 0) acreageResult = sprintf("%.3f Acres", sq / 43560.0)

                        // 2. Slope Resolve
                        val sDist = FractionUtils.parse(slopeDist)
                        val angleDeg = slopeAngle.toDoubleOrNull() ?: 0.0
                        if (sDist > 0) {
                            val angleRad = angleDeg * (PI / 180.0)
                            val hd = sDist * cos(angleRad)
                            val vd = sDist * sin(angleRad)
                            slopeResult = "HD: ${FractionUtils.formatInches(hd)} | VD: ${FractionUtils.formatInches(vd)}"
                        }

                        // 3. Inverse
                        val nn1 = n1.toDoubleOrNull() ?: 0.0
                        val ee1 = e1.toDoubleOrNull() ?: 0.0
                        val nn2 = n2.toDoubleOrNull() ?: 0.0
                        val ee2 = e2.toDoubleOrNull() ?: 0.0
                        val dn = nn2 - nn1
                        val de = ee2 - ee1
                        if (abs(dn) > 0.0001 || abs(de) > 0.0001) {
                            val dist = sqrt(dn.pow(2) + de.pow(2))
                            val azDeg = (atan2(de, dn) * (180.0 / PI) + 360.0) % 360.0
                            inverseResult = sprintf("Dist: %.3f' | Brg: %s", dist, formatQuadrantBearing(azDeg))
                        }
                    },
                    modifier = Modifier.weight(2f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)
                ) { Text("CALCULATE") }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("SAVE") }
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED SOLUTIONS", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 1.dp, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(entry.note, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(entry.data, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Solution") },
            text = {
                OutlinedTextField(
                    value = saveNote,
                    onValueChange = { saveNote = it },
                    label = { Text("Note (e.g. Back property line)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val note = saveNote.ifBlank { "Survey Result" }
                    val data = buildString {
                        acreageResult?.let { append("Area: $it | ") }
                        slopeResult?.let { append("Slope: $it | ") }
                        inverseResult?.let { append("Inverse: $it") }
                    }.trimEnd(' ', '|')

                    if (data.isNotBlank()) {
                        onUpdateEntries(entries + LandSurveyEntry(id = (entries.size + 1).toString(), note = note, data = data))
                    }
                    saveNote = ""
                    showSaveDialog = false
                }) { Text("SAVE") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun RafterCalculator(
    entries: List<RafterEntry>,
    onUpdateEntries: (List<RafterEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var pitch by remember { mutableStateOf("6") }
    var span by remember { mutableStateOf("") }
    var ridgeThick by remember { mutableStateOf("1.5") }
    var rafterDepth by remember { mutableStateOf("7.25") }
    var heelHeight by remember { mutableStateOf("4.0") }
    var overhang by remember { mutableStateOf("12") }
    var sectionName by remember { mutableStateOf("") }

    var results by remember { mutableStateOf<RafterEngine.RafterResult?>(null) }
    var editingIndex by remember { mutableStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Rafter Cuts", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("ROOF DIMENSIONS", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = pitch,
                    onValueChange = { pitch = it },
                    label = "Pitch (/12)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = span,
                    onValueChange = { span = it },
                    label = "Total Span (ft/in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = rafterDepth,
                    onValueChange = { rafterDepth = it },
                    label = "Rafter Depth (in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = heelHeight,
                    onValueChange = { heelHeight = it },
                    label = "Heel Height (HAP)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = ridgeThick,
                    onValueChange = { ridgeThick = it },
                    label = "Ridge (in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = overhang,
                    onValueChange = { overhang = it },
                    label = "Overhang (in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = sectionName,
                onValueChange = { sectionName = it },
                label = { Text("Section Name / Location") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val p = pitchToDouble(pitch, 0.0)
                        val s = FractionUtils.parseFeet(span)
                        val r = pitchToDouble(ridgeThick, 1.5)
                        val oh = pitchToDouble(overhang, 12.0)
                        val heel = pitchToDouble(heelHeight, 4.0)
                        
                        if (s > 0) {
                            val res = RafterEngine.calculate(
                                span = s,
                                pitchVal = p,
                                ridge = r,
                                heel = heel,
                                overhang = oh
                            )
                            results = res

                            val commonAngle = res.angleDeg
                            val hipAngle = atan(res.hipPitch / 12.0) * (180.0 / PI)
                            val factor = sqrt(1.0 + (p / 12.0).pow(2))

                            val entry = RafterEntry(
                                id = if (editingIndex != -1) entries[editingIndex].id else (entries.size + 1).toString(),
                                name = sectionName.ifEmpty { "Rafter Section" },
                                span = span,
                                pitch = pitch,
                                ridge = ridgeThick,
                                depth = rafterDepth,
                                heel = FractionUtils.formatInches(res.heel),
                                overhang = overhang,
                                commonLen = FractionUtils.formatInches(res.rafterLen),
                                commonOverall = FractionUtils.formatInches(res.overall),
                                commonRun = FractionUtils.formatInches(res.run),
                                commonRise = FractionUtils.formatInches(res.rise),
                                commonOARise = FractionUtils.formatInches(res.totRise),
                                commonPlumbSeat = "${commonAngle.roundToOneDecimal()}° / ${(90.0 - commonAngle).roundToOneDecimal()}°",
                                hipLen = FractionUtils.formatInches(res.hipLen),
                                hipOverall = FractionUtils.formatInches(res.hipOverall),
                                hipRun = FractionUtils.formatInches(res.hipRun),
                                hipRise = FractionUtils.formatInches(res.hipRise),
                                hipOARise = FractionUtils.formatInches(res.hipOARise),
                                hipPitch = "${res.hipPitch.roundToOneDecimal()} / 12",
                                hipPlumbSeat = "${hipAngle.roundToOneDecimal()}° / ${(90.0 - hipAngle).roundToOneDecimal()}°",
                                jack16 = FractionUtils.formatInches(16.0 * factor),
                                jack24 = FractionUtils.formatInches(24.0 * factor)
                            )

                            if (editingIndex != -1) {
                                val newList = entries.toMutableList()
                                newList[editingIndex] = entry
                                onUpdateEntries(newList)
                                editingIndex = -1
                            } else {
                                onUpdateEntries(entries + entry)
                            }
                            sectionName = ""
                        }
                    },
                    modifier = Modifier.weight(2f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)
                ) {
                    Text(if (editingIndex != -1) "UPDATE & SAVE" else "CALCULATE & SAVE", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()); results = null },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFC62828)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            results?.let { res ->
                Spacer(modifier = Modifier.height(24.dp))
                
                // Common Rafter Detailed Card
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("COMMON RAFTER", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE), fontSize = 12.sp)
                        
                        RafterDetail(
                            run = res.run,
                            rise = res.rise,
                            pitch = res.pitch,
                            heel = res.heel,
                            lumberDepth = FractionUtils.parse(rafterDepth),
                            overhang = FractionUtils.parse(overhang)
                        )
                        
                        Text(FractionUtils.formatInches(res.rafterLen), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                        Text("Overall (inc Tail): ${FractionUtils.formatInches(res.overall)}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Run", fontSize = 11.sp, color = Color.Gray)
                                Text(FractionUtils.formatInches(res.run), fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Rise", fontSize = 11.sp, color = Color.Gray)
                                Text(FractionUtils.formatInches(res.rise), fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("O/A Rise", fontSize = 11.sp, color = Color.Gray)
                                Text(FractionUtils.formatInches(res.totRise), fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Heel (HAP)", fontSize = 11.sp, color = Color.Gray)
                                Text(FractionUtils.formatInches(res.heel), fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Pitch", fontSize = 11.sp, color = Color.Gray)
                                Text("${res.pitch} / 12", fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Plumb / Seat", fontSize = 11.sp, color = Color.Gray)
                                Text("${res.angleDeg.roundToOneDecimal()}° / ${(90.0 - res.angleDeg).roundToOneDecimal()}°", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Hip Rafter Detailed Card
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("HIP / VALLEY RAFTER", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE), fontSize = 12.sp)
                        Text(FractionUtils.formatInches(res.hipLen), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                        Text("Overall (inc Tail): ${FractionUtils.formatInches(res.hipOverall)}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Hip Run", fontSize = 11.sp, color = Color.Gray)
                                Text(FractionUtils.formatInches(res.hipRun), fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Hip Rise", fontSize = 11.sp, color = Color.Gray)
                                Text(FractionUtils.formatInches(res.hipRise), fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("O/A Rise", fontSize = 11.sp, color = Color.Gray)
                                Text(FractionUtils.formatInches(res.hipOARise), fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Hip Pitch", fontSize = 11.sp, color = Color.Gray)
                                Text("${res.hipPitch.roundToOneDecimal()} / 12", fontWeight = FontWeight.Bold)
                            }
                            val hAngle = atan(res.hipPitch / 12.0) * (180.0 / PI)
                            Column {
                                Text("Plumb / Seat", fontSize = 11.sp, color = Color.Gray)
                                Text("${hAngle.roundToOneDecimal()}° / ${(90.0 - hAngle).roundToOneDecimal()}°", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("JACK RAFTER DIFFERENCE", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        val factor = sqrt(1.0 + (pitchToDouble(pitch, 0.0) / 12.0).pow(2))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("16\" O.C. Diff:", fontSize = 14.sp)
                            Text(FractionUtils.formatInches(16.0 * factor), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("24\" O.C. Diff:", fontSize = 14.sp)
                            Text(FractionUtils.formatInches(24.0 * factor), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
            
            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED RAFTERS", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 1.dp, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("Pitch: ${entry.pitch} / 12 | Heel: ${entry.heel}", fontSize = 12.sp, color = Color.Gray)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    pitch = entry.pitch
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("COMMON", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                                    Text("Len: ${entry.commonLen}", fontSize = 12.sp)
                                    Text("O/A: ${entry.commonOverall}", fontSize = 12.sp)
                                }
                                Column {
                                    Text("HIP / VAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                                    Text("Len: ${entry.hipLen}", fontSize = 12.sp)
                                    Text("O/A: ${entry.hipOverall}", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HandrailCalculator(
    entries: List<HandrailEntry>,
    onUpdateEntries: (List<HandrailEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var length by remember { mutableStateOf("") }
    var spindleWidth by remember { mutableStateOf("1.5") }
    var maxOpening by remember { mutableStateOf("4") }
    var sectionName by remember { mutableStateOf("") }
    
    var editingIndex by remember { mutableStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Handrails (Spindles)", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("RAIL DIMENSIONS", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            ConstructionTextField(
                value = length,
                onValueChange = { length = it },
                label = "Rail Length (ft/in)",
                modifier = Modifier.fillMaxWidth(),
                onFocus = onFocus
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = spindleWidth,
                    onValueChange = { spindleWidth = it },
                    label = "Spindle Width (in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = maxOpening,
                    onValueChange = { maxOpening = it },
                    label = "Max Opening (in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = sectionName,
                onValueChange = { sectionName = it },
                label = { Text("Section Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val lIn = FractionUtils.parseFeet(length)
                        val sw = spindleWidth.toDoubleOrNull() ?: 1.5
                        val mo = maxOpening.toDoubleOrNull() ?: 4.0
                        
                        if (lIn > 0) {
                            val spindleCount = ceil((lIn - mo) / (sw + mo)).toInt()
                            val gap = (lIn - (spindleCount * sw)) / (spindleCount + 1)
                            // On-center spacing (what actually gets marked on the rail) is the
                            // gap PLUS the spindle's own width, not the gap alone.
                            val actualOC = gap + sw

                            val entry = HandrailEntry(
                                id = if (editingIndex != -1) entries[editingIndex].id else (entries.size + 1).toString(),
                                name = sectionName.ifEmpty { "Rail Section" },
                                length = length,
                                spindleWidth = spindleWidth,
                                maxOpening = maxOpening,
                                spindles = "$spindleCount Spindles",
                                spacing = "${FractionUtils.formatInches(actualOC)} O.C.",
                                gap = FractionUtils.formatInches(gap)
                            )

                            if (editingIndex != -1) {
                                val newList = entries.toMutableList()
                                newList[editingIndex] = entry
                                onUpdateEntries(newList)
                                editingIndex = -1
                            } else {
                                onUpdateEntries(entries + entry)
                            }
                            sectionName = ""
                        }
                    },
                    modifier = Modifier.weight(2f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)
                ) {
                    Text(if (editingIndex != -1) "UPDATE & SAVE" else "CALCULATE & SAVE", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()) },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFC62828)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED RAILS", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 2.dp, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("Length: ${entry.length}", fontSize = 12.sp, color = Color.Gray)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    length = entry.length
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(entry.spindles, fontSize = 14.sp)
                            Text(entry.spacing, fontSize = 14.sp)
                            if (entry.gap.isNotEmpty()) Text("Gap: ${entry.gap}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PineLineCalculator(
    entries: List<PineLineEntry>,
    onUpdateEntries: (List<PineLineEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var pitch by remember { mutableStateOf("6") }
    var heel by remember { mutableStateOf("4") }
    var wall by remember { mutableStateOf("3.5") }
    var brick by remember { mutableStateOf("0") }
    var frieze by remember { mutableStateOf("0.75") }
    var overhang by remember { mutableStateOf("12") }
    var fascia by remember { mutableStateOf("7.25") }
    var reveal by remember { mutableStateOf("1") }
    var sectionName by remember { mutableStateOf("") }

    var results by remember { mutableStateOf<PineLineEngine.Result?>(null) }
    var editingIndex by remember { mutableStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Pine Line (Grade / Slope)", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("ROOF & WALL SPECS", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = pitch,
                    onValueChange = { pitch = it },
                    label = "Pitch (/12)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = heel,
                    onValueChange = { heel = it },
                    label = "Heel Ht (HAP)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = wall,
                    onValueChange = { wall = it },
                    label = "Wall Stud (in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = brick,
                    onValueChange = { brick = it },
                    label = "Brick/Ext (in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = frieze,
                    onValueChange = { frieze = it },
                    label = "Frieze (in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = overhang,
                    onValueChange = { overhang = it },
                    label = "Overhang (in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = fascia,
                    onValueChange = { fascia = it },
                    label = "Fascia (in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = reveal,
                    onValueChange = { reveal = it },
                    label = "Reveal (in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = sectionName,
                onValueChange = { sectionName = it },
                label = { Text("Section Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val p = pitch.toDoubleOrNull() ?: 6.0
                        val h = FractionUtils.parse(heel)
                        val w = FractionUtils.parse(wall)
                        val b = FractionUtils.parse(brick)
                        val f = FractionUtils.parse(frieze)
                        val oh = FractionUtils.parse(overhang)
                        val fa = FractionUtils.parse(fascia)
                        val rv = FractionUtils.parse(reveal)
                        
                        if (h > 0) {
                            val res = PineLineEngine.calculate(p, h, w, b, f, oh, fa, rv)
                            results = res

                            val entry = PineLineEntry(
                                id = if (editingIndex != -1) entries[editingIndex].id else (entries.size + 1).toString(),
                                name = sectionName.ifEmpty { "Pine Line Section" },
                                pitch = "$pitch / 12",
                                heel = heel,
                                wall = wall,
                                brick = brick,
                                frieze = frieze,
                                overhang = overhang,
                                fascia = fascia,
                                reveal = reveal,
                                drop = FractionUtils.formatInches(res.pineLineHt),
                                rafterLength = FractionUtils.formatInches(res.rafterLenToFascia)
                            )

                            if (editingIndex != -1) {
                                val newList = entries.toMutableList()
                                newList[editingIndex] = entry
                                onUpdateEntries(newList)
                                editingIndex = -1
                            } else {
                                onUpdateEntries(entries + entry)
                            }
                            sectionName = ""
                        }
                    },
                    modifier = Modifier.weight(2f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)
                ) {
                    Text(if (editingIndex != -1) "UPDATE & SAVE" else "CALCULATE & SAVE", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()) },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFC62828)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            results?.let { res ->
                Spacer(modifier = Modifier.height(24.dp))
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("RESULTS", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Pine Line Ht (from Plate)", fontSize = 11.sp, color = Color.Gray)
                        Text(FractionUtils.formatInches(res.pineLineHt), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = BlueTool)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Rafter Length to Fascia", fontSize = 11.sp, color = Color.Gray)
                        Text(FractionUtils.formatInches(res.rafterLenToFascia), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED PINE LINES", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 2.dp, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("Pitch: ${entry.pitch}", fontSize = 12.sp, color = Color.Gray)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    pitch = entry.pitch.replace(" / 12", "")
                                    heel = entry.heel
                                    wall = entry.wall
                                    brick = entry.brick
                                    frieze = entry.frieze
                                    overhang = entry.overhang
                                    fascia = entry.fascia
                                    reveal = entry.reveal
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("Vertical Drop from Wall Plate: ${entry.drop}", fontWeight = FontWeight.Bold, color = BlueTool)
                            if (entry.rafterLength.isNotEmpty()) Text("Rafter Length to Fascia: ${entry.rafterLength}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeckCalculator(
    entries: List<DeckEntry>,
    onUpdateEntries: (List<DeckEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var length by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }
    var boardWidth by remember { mutableStateOf("5.5") }
    var gap by remember { mutableStateOf("0.125") }
    var joistOC by remember { mutableStateOf("16") }
    var runAlongWidth by remember { mutableStateOf(false) }
    var sectionName by remember { mutableStateOf("") }

    var editingIndex by remember { mutableStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Deck Boards", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("DECK DIMENSIONS", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = length,
                    onValueChange = { length = it },
                    label = "Length (ft/in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = width,
                    onValueChange = { width = it },
                    label = "Width (ft/in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = boardWidth,
                    onValueChange = { boardWidth = it },
                    label = "Board Width (in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = gap,
                    onValueChange = { gap = it },
                    label = "Gap (in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            ConstructionTextField(
                value = joistOC,
                onValueChange = { joistOC = it },
                label = "Joist O.C. (in)",
                modifier = Modifier.fillMaxWidth(),
                onFocus = onFocus
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Boards Run Along:", fontSize = 13.sp, modifier = Modifier.padding(end = 8.dp))
                Button(
                    onClick = { runAlongWidth = false },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (!runAlongWidth) Color(0xFF6200EE) else Color.LightGray,
                        contentColor = if (!runAlongWidth) Color.White else Color.DarkGray
                    )
                ) { Text("Length", fontSize = 12.sp) }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { runAlongWidth = true },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (runAlongWidth) Color(0xFF6200EE) else Color.LightGray,
                        contentColor = if (runAlongWidth) Color.White else Color.DarkGray
                    )
                ) { Text("Width", fontSize = 12.sp) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = sectionName,
                onValueChange = { sectionName = it },
                label = { Text("Section Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        // Master uses parseFeet here (bare numbers are feet, matching the "(ft/in)"
                        // labels) - the previous FractionUtils.parse would have silently treated
                        // a bare "20" as 20 inches instead of 20 feet, a ~12x error.
                        val lIn = FractionUtils.parseFeet(length)
                        val wIn = FractionUtils.parseFeet(width)
                        val bw = FractionUtils.parse(boardWidth).takeIf { it > 0 } ?: 5.5
                        val g = FractionUtils.parse(gap).takeIf { it > 0 } ?: 0.125
                        val oc = FractionUtils.parse(joistOC).takeIf { it > 0 } ?: 16.0

                        if (lIn > 0 && wIn > 0) {
                            val effectiveWidth = bw + g
                            val spanDistIn = if (runAlongWidth) wIn else lIn
                            val runDistIn = if (runAlongWidth) lIn else wIn

                            val numRows = ceil(runDistIn / effectiveWidth).toInt()
                            val totalLF = (numRows * spanDistIn) / 12.0

                            val numJoists = ceil(spanDistIn / oc).toInt() + 1
                            val fasteners = numRows * numJoists * 2

                            val entry = DeckEntry(
                                id = if (editingIndex != -1) entries[editingIndex].id else (entries.size + 1).toString(),
                                name = sectionName.ifEmpty { "Deck Section" },
                                length = length,
                                width = width,
                                boardWidth = boardWidth,
                                gap = gap,
                                boards = "$numRows Boards",
                                fasteners = "$fasteners Screws",
                                totalLF = sprintf("%.0f' LF", totalLF),
                                joistOC = joistOC,
                                runAlongWidth = runAlongWidth
                            )

                            if (editingIndex != -1) {
                                val newList = entries.toMutableList()
                                newList[editingIndex] = entry
                                onUpdateEntries(newList)
                                editingIndex = -1
                            } else {
                                onUpdateEntries(entries + entry)
                            }
                            sectionName = ""
                        }
                    },
                    modifier = Modifier.weight(2f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)
                ) {
                    Text(if (editingIndex != -1) "UPDATE & SAVE" else "CALCULATE & SAVE", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()) },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFC62828)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            if (entries.isNotEmpty()) {
                val grandLF = entries.sumOf { it.totalLF.replace("' LF", "").toDoubleOrNull() ?: 0.0 }
                Spacer(modifier = Modifier.height(24.dp))
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth(), backgroundColor = Color(0xFF0A2A66)) {
                    Text(
                        sprintf("Grand Total: %.0f' LF", grandLF),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED DECKS", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 2.dp, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("${entry.length} x ${entry.width}", fontSize = 12.sp, color = Color.Gray)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    length = entry.length
                                    width = entry.width
                                    boardWidth = entry.boardWidth
                                    gap = entry.gap
                                    joistOC = entry.joistOC
                                    runAlongWidth = entry.runAlongWidth
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(entry.boards, fontSize = 14.sp)
                            Text(entry.fasteners, fontSize = 14.sp)
                            if (entry.totalLF.isNotEmpty()) Text(entry.totalLF, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloorCalculator(
    entries: List<FloorEntry>,
    onUpdateEntries: (List<FloorEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var length by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }
    var joistSize by remember { mutableStateOf("2x10") }
    var spacing by remember { mutableStateOf("16") }
    var numBeams by remember { mutableStateOf("0") }
    var plyThick by remember { mutableStateOf("3/4\"") }
    var direction by remember { mutableStateOf("Span Length") }
    var sectionName by remember { mutableStateOf("") }
    
    var results by remember { mutableStateOf<FloorResult?>(null) }
    var editingIndex by remember { mutableStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Floor Framing & Subfloor", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("FLOOR DIMENSIONS & FRAMING SPECIFICATIONS", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = length,
                    onValueChange = { length = it },
                    label = "Length (ft/in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = width,
                    onValueChange = { width = it },
                    label = "Width (ft/in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = spacing,
                    onValueChange = { spacing = it },
                    label = "Joist O.C. (in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = numBeams,
                    onValueChange = { numBeams = it },
                    label = "Support Beams",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Joist Member & Subfloor Options
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = joistSize,
                    onValueChange = { joistSize = it },
                    label = { Text("Joist Member (2x10, TJI, LVL)") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = plyThick,
                    onValueChange = { plyThick = it },
                    label = { Text("Subfloor Plywood (3/4\")") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Span Direction Toggle Buttons
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Span Direction:", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                Button(
                    onClick = { direction = "Span Length" },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (direction == "Span Length") BlueTool else Color.LightGray,
                        contentColor = if (direction == "Span Length") Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Span Length", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { direction = "Span Width" },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (direction == "Span Width") BlueTool else Color.LightGray,
                        contentColor = if (direction == "Span Width") Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Span Width", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = sectionName,
                onValueChange = { sectionName = it },
                label = { Text("Section Name / Location") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val lIn = FractionUtils.parseFeet(length)
                        val wIn = FractionUtils.parseFeet(width)
                        val s = FractionUtils.parse(spacing).takeIf { it > 0 } ?: 16.0
                        val beams = numBeams.toIntOrNull() ?: 0

                        if (lIn > 0 && wIn > 0) {
                            val sheetsNeeded = ceil((lIn * wIn) / 4608.0).toInt()

                            val runDistIn = if (direction == "Span Length") wIn else lIn
                            val spanDistIn = if (direction == "Span Length") lIn else wIn
                            val perimeterIn = (lIn * 2.0) + (wIn * 2.0)

                            var individualJoistLenIn = spanDistIn / (beams + 1)
                            val standardFt = ceil(individualJoistLenIn / 24.0) * 2.0
                            individualJoistLenIn = standardFt * 12.0
                            // Master's lap allowance is 0.25" (a quarter inch, not 3 full inches).
                            if (beams > 0) individualJoistLenIn += 0.25

                            val joistCountPerRun = (ceil(runDistIn / s) + 1).toInt()
                            val totalJoists = joistCountPerRun * (beams + 1)

                            val res = FloorResult(
                                lengthIn = lIn,
                                widthIn = wIn,
                                joistCount = totalJoists,
                                joistCutLenIn = individualJoistLenIn,
                                rimJoistPerimeterIn = perimeterIn,
                                sheetCount = sheetsNeeded,
                                spacingIn = s,
                                beams = beams,
                                direction = direction
                            )
                            results = res

                            val entry = FloorEntry(
                                id = if (editingIndex != -1) entries[editingIndex].id else (entries.size + 1).toString(),
                                name = sectionName.ifEmpty { "Floor Section" },
                                length = length,
                                width = width,
                                spacing = "${s.toInt()}\" OC",
                                joists = "$totalJoists Joists ($joistSize @ ${FractionUtils.formatInches(individualJoistLenIn)})",
                                sheets = "$sheetsNeeded Sheets ($plyThick)",
                                rimJoist = FractionUtils.formatInches(perimeterIn)
                            )

                            if (editingIndex != -1) {
                                val newList = entries.toMutableList()
                                newList[editingIndex] = entry
                                onUpdateEntries(newList)
                                editingIndex = -1
                            } else {
                                onUpdateEntries(entries + entry)
                            }
                        }
                    },
                    modifier = Modifier.weight(2f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)
                ) {
                    Text(if (editingIndex != -1) "UPDATE TAKEOFF" else "CALCULATE & SAVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()) },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFC62828)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            results?.let { res ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth(), elevation = 4.dp, shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("FLOOR FRAMING RESULTS", fontWeight = FontWeight.Bold, color = BlueTool, fontSize = 16.sp)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Joists Required:", fontWeight = FontWeight.SemiBold)
                            Text("${res.joistCount} Joists", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cut Length / Joist:", fontWeight = FontWeight.SemiBold)
                            Text(FractionUtils.formatInches(res.joistCutLenIn), fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subfloor 4x8 Sheets:", fontWeight = FontWeight.SemiBold)
                            Text("${res.sheetCount} Sheets ($plyThick)", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Rim Joist Perimeter:", fontWeight = FontWeight.SemiBold)
                            Text(FractionUtils.formatInches(res.rimJoistPerimeterIn), fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Framing Layout Plan:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        FloorDiagram(
                            floorLength = res.lengthIn,
                            floorWidth = res.widthIn,
                            joistOC = res.spacingIn,
                            numBeams = res.beams,
                            spanDirection = res.direction
                        )
                    }
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED FLOORS", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 2.dp, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("${entry.length} x ${entry.width}", fontSize = 12.sp, color = Color.Gray)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    length = entry.length
                                    width = entry.width
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(entry.joists, fontSize = 14.sp)
                            Text(entry.sheets, fontSize = 14.sp)
                            if (entry.rimJoist.isNotEmpty()) Text("Rim Joist: ${entry.rimJoist}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

private data class FloorResult(
    val lengthIn: Double,
    val widthIn: Double,
    val joistCount: Int,
    val joistCutLenIn: Double,
    val rimJoistPerimeterIn: Double,
    val sheetCount: Int,
    val spacingIn: Double,
    val beams: Int,
    val direction: String
)

@Composable
fun MasonryCalculator(
    entries: List<MasonryEntry>,
    onUpdateEntries: (List<MasonryEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var qty by remember { mutableStateOf("1") }
    var sectionName by remember { mutableStateOf("") }
    var lengthInput by remember { mutableStateOf("") }
    var heightInput by remember { mutableStateOf("") }
    // Master app's own labels read "6.00 / sqft" for brick, but its actual formula uses 7.0 -
    // a pre-existing label/formula mismatch in the master app itself, not introduced here.
    // Matching master's real (7.0) behavior since that's what its output numbers reflect;
    // copying the label text as-is for a faithful UI match.
    var isBlock by remember { mutableStateOf(true) }

    var editingIndex by remember { mutableStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Block & Brick", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("ADD WALL SECTION", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = qty,
                    onValueChange = { qty = it },
                    label = "Qty",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = sectionName,
                    onValueChange = { sectionName = it },
                    label = { Text("Section Name") },
                    modifier = Modifier.weight(2f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = lengthInput,
                    onValueChange = { lengthInput = it },
                    label = "Length (ft)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = heightInput,
                    onValueChange = { heightInput = it },
                    label = "Height (ft)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(1f).clickable { isBlock = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isBlock, onClick = { isBlock = true })
                    Text("Standard Block (1.125 / sqft)", fontSize = 13.sp)
                }
                Row(
                    modifier = Modifier.weight(1f).clickable { isBlock = false },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = !isBlock, onClick = { isBlock = false })
                    Text("Standard Brick (6.00 / sqft)", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val lenIn = FractionUtils.parseFeet(lengthInput)
                        val heightIn = FractionUtils.parseFeet(heightInput)
                        val qtyVal = qty.toDoubleOrNull() ?: 1.0
                        if (lenIn > 0 && heightIn > 0) {
                            val areaSqFt = (lenIn * heightIn / 144.0) * qtyVal
                            val unitMultiplier = if (isBlock) 1.125 else 7.0
                            val totalUnits = ceil(areaSqFt * unitMultiplier)
                            val mortarDivider = if (isBlock) 35.0 else 135.0
                            val mortarBags = ceil(totalUnits / mortarDivider)
                            val sandYards = mortarBags / 7.0

                            val entry = MasonryEntry(
                                id = if (editingIndex != -1) entries[editingIndex].id else (entries.size + 1).toString(),
                                name = sectionName.ifEmpty { "Masonry Section" },
                                qty = qtyVal.toInt().toString(),
                                sqFt = sprintf("%.2f sq ft", areaSqFt),
                                type = if (isBlock) "Block" else "Brick",
                                units = sprintf("%.0f", totalUnits),
                                mortar = sprintf("%.0f", mortarBags),
                                sand = sprintf("%.2f", sandYards),
                                length = FractionUtils.formatInches(lenIn),
                                height = FractionUtils.formatInches(heightIn)
                            )

                            if (editingIndex != -1) {
                                val newList = entries.toMutableList()
                                newList[editingIndex] = entry
                                onUpdateEntries(newList)
                                editingIndex = -1
                            } else {
                                onUpdateEntries(entries + entry)
                            }
                            sectionName = ""; lengthInput = ""; heightInput = ""; qty = "1"
                        }
                    },
                    modifier = Modifier.weight(2f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)
                ) {
                    Text(if (editingIndex != -1) "UPDATE ITEM" else "ADD TO LIST", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()) },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFC62828)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            if (entries.isNotEmpty()) {
                val grandUnits = entries.sumOf { it.units.toDoubleOrNull() ?: 0.0 }
                val grandSqFt = entries.sumOf { it.sqFt.replace(" sq ft", "").toDoubleOrNull() ?: 0.0 }
                val grandMortar = entries.sumOf { it.mortar.toDoubleOrNull() ?: 0.0 }
                val grandSand = entries.sumOf { it.sand.toDoubleOrNull() ?: 0.0 }

                Spacer(modifier = Modifier.height(24.dp))
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth(), backgroundColor = Color(0xFF0A2A66)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(sprintf("Grand Total Units: %.0f", grandUnits), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            sprintf("Area: %.2f sq ft | Mortar: %.0f bags | Sand: %.2f yd", grandSqFt, grandMortar, grandSand),
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED MASONRY", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 2.dp, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Qty ${entry.qty} | ${entry.length} x ${entry.height} | ${entry.sqFt}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    qty = entry.qty
                                    lengthInput = entry.length
                                    heightInput = entry.height
                                    isBlock = entry.type == "Block"
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                "${entry.type}: ${entry.units} units | Mortar: ${entry.mortar} bags | Sand: ${entry.sand} yd",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlueTool
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrywallCalculator(
    entries: List<DrywallEntry>,
    onUpdateEntries: (List<DrywallEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var lengthInput by remember { mutableStateOf("") }
    var widthInput by remember { mutableStateOf("") }
    var wallHeightInput by remember { mutableStateOf("") }
    var roomMode by remember { mutableStateOf(false) }
    var sheetSize by remember { mutableStateOf("4x8") }
    var sectionName by remember { mutableStateOf("") }

    var editingIndex by remember { mutableStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Drywall Finish", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("ROOM / SECTION DIMENSIONS", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = lengthInput,
                    onValueChange = { lengthInput = it },
                    label = "Length (ft/in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = widthInput,
                    onValueChange = { widthInput = it },
                    label = "Width (ft/in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = roomMode, onCheckedChange = { roomMode = it })
                Text("Full Room (4 Walls + Ceiling)", fontSize = 12.sp)
            }
            if (roomMode) {
                Spacer(modifier = Modifier.height(8.dp))
                ConstructionTextField(
                    value = wallHeightInput,
                    onValueChange = { wallHeightInput = it },
                    label = "Wall Height (ft/in, default 8')",
                    modifier = Modifier.fillMaxWidth(),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Sheet Size:", modifier = Modifier.weight(1f))
                Row(modifier = Modifier.weight(2f), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("4x8", "4x10", "4x12").forEach { size ->
                        val selected = sheetSize == size
                        Button(
                            onClick = { sheetSize = size },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (selected) BlueTool else Color.White,
                                contentColor = if (selected) Color.White else Color.Black
                            ),
                            elevation = ButtonDefaults.elevation(0.dp),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) { Text(size) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = sectionName,
                onValueChange = { sectionName = it },
                label = { Text("Section Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val len = FractionUtils.parseFeet(lengthInput)
                        val wid = FractionUtils.parseFeet(widthInput)
                        val wallH = FractionUtils.parseFeet(wallHeightInput).takeIf { it > 0 } ?: 96.0

                        if (len > 0 && wid > 0) {
                            val areaSqIn = if (roomMode) {
                                (2.0 * len * wallH) + (2.0 * wid * wallH) + (len * wid)
                            } else {
                                len * wid
                            }
                            val area = areaSqIn / 144.0

                            val divisor = when (sheetSize) {
                                "4x8" -> 32.0
                                "4x10" -> 40.0
                                "4x12" -> 48.0
                                else -> 32.0
                            }
                            val sheets = ceil(area / divisor).toInt()
                            val mud = (area * 0.05).roundToOneDecimal()
                            val tape = ceil(area * 1.5).toInt()

                            val entry = DrywallEntry(
                                id = if (editingIndex != -1) entries[editingIndex].id else (entries.size + 1).toString(),
                                name = sectionName.ifEmpty { "Drywall Section" },
                                area = sprintf("%.0f sq ft", area),
                                sheets = "$sheets Sheets",
                                sheetSize = sheetSize,
                                mud = "$mud lbs Mud",
                                tape = "$tape ft Tape",
                                length = lengthInput,
                                width = widthInput,
                                wallHeight = wallHeightInput,
                                roomMode = roomMode
                            )

                            if (editingIndex != -1) {
                                val newList = entries.toMutableList()
                                newList[editingIndex] = entry
                                onUpdateEntries(newList)
                                editingIndex = -1
                            } else {
                                onUpdateEntries(entries + entry)
                            }
                            sectionName = ""; lengthInput = ""; widthInput = ""
                        }
                    },
                    modifier = Modifier.weight(2f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)
                ) {
                    Text(if (editingIndex != -1) "UPDATE & SAVE" else "CALCULATE & SAVE", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()) },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFC62828)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            if (entries.isNotEmpty()) {
                val totalArea = entries.sumOf { it.area.replace(" sq ft", "").toDoubleOrNull() ?: 0.0 }
                val totalSheets = entries.sumOf { it.sheets.replace(" Sheets", "").toDoubleOrNull() ?: 0.0 }
                val totalMud = entries.sumOf { it.mud.replace(" lbs Mud", "").toDoubleOrNull() ?: 0.0 }
                val totalTape = entries.sumOf { it.tape.replace(" ft Tape", "").toDoubleOrNull() ?: 0.0 }

                Spacer(modifier = Modifier.height(24.dp))
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth(), backgroundColor = BlueTool) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(sprintf("Total Area: %.0f sq ft", totalArea), color = Color.White, fontWeight = FontWeight.Bold)
                        Text(sprintf("Total Sheets: %.0f", totalSheets), color = Color.White, fontWeight = FontWeight.Bold)
                        Text(sprintf("Mud: %.0f lbs | Tape: %.0f ft", totalMud, totalTape), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED DRYWALL", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 2.dp, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("Area: ${entry.area}", fontSize = 12.sp, color = Color.Gray)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    lengthInput = entry.length
                                    widthInput = entry.width
                                    wallHeightInput = entry.wallHeight
                                    roomMode = entry.roomMode
                                    sheetSize = entry.sheetSize
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("${entry.sheets} | ${entry.mud} | ${entry.tape}", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConcreteCalculator(
    entries: List<ConcreteEntry>,
    onUpdateEntries: (List<ConcreteEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit,
    importSeed: ConcreteImportSeed? = null,
    onImportConsumed: () -> Unit = {}
) {
    var length by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }
    var thick by remember { mutableStateOf("4") }
    var rebarMode by remember { mutableStateOf(0) } // 0 = Grid, 1 = Continuous
    var rebarSpacing by remember { mutableStateOf("12") }
    var rebarMats by remember { mutableStateOf("1") }
    var barsTop by remember { mutableStateOf("0") }
    var barsBottom by remember { mutableStateOf("2") }
    var qty by remember { mutableStateOf("1") }
    var sectionName by remember { mutableStateOf("") }

    // Matches master's ConcreteActivity IMPORT_LENGTH / IMPORT_AREA handling from the
    // Coordinates map takeoff: a perimeter import fills Length only; an area import fills
    // Length=area with Width=1 (Len x 1 treats the area as a simple rectangle).
    LaunchedEffect(importSeed) {
        importSeed?.let { seed ->
            length = seed.length
            if (seed.width.isNotEmpty()) width = seed.width
            sectionName = seed.name
            onImportConsumed()
        }
    }
    
    var results by remember { mutableStateOf<ConcreteResult?>(null) }
    var editingIndex by remember { mutableStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Concrete & Rebar Master", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("SLAB & FOOTING DIMENSIONS", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = length,
                    onValueChange = { length = it },
                    label = "Length (ft / in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = width,
                    onValueChange = { width = it },
                    label = "Width (ft / in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(
                    value = thick,
                    onValueChange = { thick = it },
                    label = "Thick/Depth (in)",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(
                    value = qty,
                    onValueChange = { qty = it },
                    label = "Quantity",
                    modifier = Modifier.weight(1f),
                    onFocus = onFocus
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Rebar Reinforcement Specifications
            Text("REBAR REINFORCEMENT", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { rebarMode = 0 },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (rebarMode == 0) BlueTool else Color.LightGray,
                        contentColor = if (rebarMode == 0) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Rebar Grid (Slabs)", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { rebarMode = 1 },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (rebarMode == 1) BlueTool else Color.LightGray,
                        contentColor = if (rebarMode == 1) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Continuous Bars (Footings)", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (rebarMode == 0) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    ConstructionTextField(
                        value = rebarSpacing,
                        onValueChange = { rebarSpacing = it },
                        label = "Grid O.C. (in)",
                        modifier = Modifier.weight(1f),
                        onFocus = onFocus
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ConstructionTextField(
                        value = rebarMats,
                        onValueChange = { rebarMats = it },
                        label = "Number of Mats",
                        modifier = Modifier.weight(1f),
                        onFocus = onFocus
                    )
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    ConstructionTextField(
                        value = barsTop,
                        onValueChange = { barsTop = it },
                        label = "Top Bars",
                        modifier = Modifier.weight(1f),
                        onFocus = onFocus
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ConstructionTextField(
                        value = barsBottom,
                        onValueChange = { barsBottom = it },
                        label = "Bottom Bars",
                        modifier = Modifier.weight(1f),
                        onFocus = onFocus
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = sectionName,
                onValueChange = { sectionName = it },
                label = { Text("Section Name / Location") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val lIn = FractionUtils.parseFeet(length)
                        val wIn = FractionUtils.parseFeet(width)
                        val tIn = FractionUtils.parse(thick)
                        val q = qty.toDoubleOrNull() ?: 1.0
                        
                        if (lIn > 0 && wIn > 0 && tIn > 0) {
                            val singleCuFt = (lIn / 12.0) * (wIn / 12.0) * (tIn / 12.0)
                            val totalCuFt = singleCuFt * q
                            val totalCuYd = totalCuFt / 27.0

                            val ocIn = rebarSpacing.toDoubleOrNull() ?: 12.0
                            val mats = rebarMats.toDoubleOrNull() ?: 1.0
                            val bTop = barsTop.toIntOrNull() ?: 0
                            val bBot = barsBottom.toIntOrNull() ?: 2

                            val totalRebarFt: Double = if (rebarMode == 0) {
                                if (ocIn > 0) {
                                    val rows = (wIn / ocIn) + 1.0
                                    val cols = (lIn / ocIn) + 1.0
                                    ((rows * (lIn / 12.0)) + (cols * (wIn / 12.0))) * mats * q
                                } else 0.0
                            } else {
                                (bTop + bBot).toDouble() * (lIn / 12.0) * q
                            }

                            val res = ConcreteResult(
                                lengthIn = lIn,
                                widthIn = wIn,
                                thickIn = tIn,
                                qty = q.toInt(),
                                totalCuFt = totalCuFt,
                                totalCuYd = totalCuYd,
                                totalRebarFt = totalRebarFt,
                                rebarMode = rebarMode,
                                rebarOC = ocIn,
                                barsTop = bTop,
                                barsBottom = bBot
                            )
                            results = res

                            val entry = ConcreteEntry(
                                id = if (editingIndex != -1) entries[editingIndex].id else (entries.size + 1).toString(),
                                name = sectionName.ifEmpty { "Concrete Section" },
                                qty = q.toInt().toString(),
                                volume = "${totalCuYd.roundToOneDecimal()} cu yd (${totalCuFt.roundToOneDecimal()} cu ft)",
                                rebar = "${totalRebarFt.roundToOneDecimal()} lin ft",
                                spacing = if (rebarMode == 0) "${ocIn.toInt()}\" OC Grid" else "$bTop Top / $bBot Bot Bars",
                                dimensions = "${length} x ${width} x ${thick}\""
                            )

                            if (editingIndex != -1) {
                                val newList = entries.toMutableList()
                                newList[editingIndex] = entry
                                onUpdateEntries(newList)
                                editingIndex = -1
                            } else {
                                onUpdateEntries(entries + entry)
                            }
                            sectionName = ""
                        }
                    },
                    modifier = Modifier.weight(2f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)
                ) {
                    Text(if (editingIndex != -1) "UPDATE TAKEOFF" else "CALCULATE & SAVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()); results = null },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFC62828)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }
            
            results?.let { res ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth(), elevation = 4.dp, shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ESTIMATED CONCRETE & REBAR", fontWeight = FontWeight.Bold, color = BlueTool, fontSize = 16.sp)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        FootingCrossSection(
                            width = res.widthIn,
                            thickness = res.thickIn,
                            rebarMode = res.rebarMode,
                            rebarOC = res.rebarOC,
                            barsTop = res.barsTop,
                            barsBottom = res.barsBottom
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Concrete Volume:", fontWeight = FontWeight.SemiBold)
                            Text("${res.totalCuYd.roundToOneDecimal()} cu yd (${res.totalCuFt.roundToOneDecimal()} cu ft)", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Rebar Total Footage:", fontWeight = FontWeight.SemiBold)
                            Text("${res.totalRebarFt.roundToOneDecimal()} lin ft", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        }
                    }
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED CONCRETE SECTIONS", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 2.dp, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("Qty: ${entry.qty} | Dimens: ${entry.dimensions} | ${entry.spacing}", fontSize = 12.sp, color = Color.Gray)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    qty = entry.qty
                                    val dims = entry.dimensions.split(" x ")
                                    if (dims.size == 3) {
                                        length = dims[0]
                                        width = dims[1]
                                        thick = dims[2].replace("\"", "")
                                    }
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("Volume: ${entry.volume}", fontWeight = FontWeight.Bold, color = BlueTool)
                            Text("Rebar: ${entry.rebar}", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

private data class ConcreteResult(
    val lengthIn: Double,
    val widthIn: Double,
    val thickIn: Double,
    val qty: Int,
    val totalCuFt: Double,
    val totalCuYd: Double,
    val totalRebarFt: Double,
    val rebarMode: Int,
    val rebarOC: Double,
    val barsTop: Int,
    val barsBottom: Int
)

@Composable
fun TrigCalculator(
    entries: List<TrigEntry>,
    onUpdateEntries: (List<TrigEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var sideA by remember { mutableStateOf("") }
    var sideB by remember { mutableStateOf("") }
    var hypotenuse by remember { mutableStateOf("") }
    var angleA by remember { mutableStateOf("") }
    var sectionName by remember { mutableStateOf("") }

    var results by remember { mutableStateOf<TrigEngine.RightTriangleResult?>(null) }
    var editingIndex by remember { mutableStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Trig Solutions", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("RIGHT TRIANGLE (ENTER ANY 2)", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(value = sideA, onValueChange = { sideA = it }, label = "Side A (Rise)", modifier = Modifier.weight(1f), onFocus = onFocus)
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(value = sideB, onValueChange = { sideB = it }, label = "Side B (Run)", modifier = Modifier.weight(1f), onFocus = onFocus)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(value = hypotenuse, onValueChange = { hypotenuse = it }, label = "Hypotenuse", modifier = Modifier.weight(1f), onFocus = onFocus)
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(value = angleA, onValueChange = { angleA = it }, label = "Angle A (°)", modifier = Modifier.weight(1f), onFocus = onFocus)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = sectionName, onValueChange = { sectionName = it }, label = { Text("Section Name") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val a = FractionUtils.parse(sideA).takeIf { it > 0 }
                    val b = FractionUtils.parse(sideB).takeIf { it > 0 }
                    val c = FractionUtils.parse(hypotenuse).takeIf { it > 0 }
                    val ang = angleA.toDoubleOrNull()
                    val res = TrigEngine.solveRight(a, b, c, ang)
                    results = res
                    val entry = TrigEntry(
                        id = if (editingIndex != -1) entries[editingIndex].id else (entries.size + 1).toString(),
                        name = sectionName.ifEmpty { "Trig Section" },
                        sideA = FractionUtils.formatInches(res.sideA),
                        sideB = FractionUtils.formatInches(res.sideB),
                        hypotenuse = FractionUtils.formatInches(res.hypotenuse),
                        angleA = "${res.angleA.roundToOneDecimal()}°",
                        angleB = "${res.angleB.roundToOneDecimal()}°"
                    )

                    if (editingIndex != -1) {
                        val newList = entries.toMutableList()
                        newList[editingIndex] = entry
                        onUpdateEntries(newList)
                        editingIndex = -1
                    } else {
                        onUpdateEntries(entries + entry)
                    }
                    sectionName = ""
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text(if (editingIndex != -1) "UPDATE ITEM" else "SOLVE & SAVE") }

            results?.let { res ->
                Spacer(modifier = Modifier.height(24.dp))
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("TRIANGLE RESULTS", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Hypotenuse: ${FractionUtils.formatInches(res.hypotenuse)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Angle A", fontSize = 11.sp, color = Color.Gray)
                                Text("${res.angleA.roundToOneDecimal()}°", fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Angle B", fontSize = 11.sp, color = Color.Gray)
                                Text("${res.angleB.roundToOneDecimal()}°", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED SOLUTIONS", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 1.dp, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("A: ${entry.sideA} | B: ${entry.sideB} | Hyp: ${entry.hypotenuse}", fontSize = 12.sp, color = Color.Gray)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    sideA = entry.sideA
                                    sideB = entry.sideB
                                    hypotenuse = entry.hypotenuse
                                    angleA = entry.angleA.replace("°", "")
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("Angle A: ${entry.angleA} | Angle B: ${entry.angleB}", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColumnCalculator(
    entries: List<ColumnEntry>,
    onUpdateEntries: (List<ColumnEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var diameter by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("1") }
    var sectionName by remember { mutableStateOf("") }

    var rebarType by remember { mutableStateOf(0) } // 0 = Standard (vert + ties), 1 = Linear (top/bottom)
    var rebarVert by remember { mutableStateOf("4") }
    var rebarTies by remember { mutableStateOf("12") }
    var barsTop by remember { mutableStateOf("0") }
    var barsBottom by remember { mutableStateOf("0") }

    var results by remember { mutableStateOf<ColumnEngine.ColumnResult?>(null) }
    var resultsRebarFt by remember { mutableStateOf(0.0) }
    var editingIndex by remember { mutableStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Columns / Circles", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(value = diameter, onValueChange = { diameter = it }, label = "Diameter (in)", modifier = Modifier.weight(1f), onFocus = onFocus)
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(value = height, onValueChange = { height = it }, label = "Height (ft/in)", modifier = Modifier.weight(1f), onFocus = onFocus)
            }
            Spacer(modifier = Modifier.height(8.dp))
            ConstructionTextField(value = qty, onValueChange = { qty = it }, label = "Quantity", modifier = Modifier.fillMaxWidth(), onFocus = onFocus)
            Spacer(modifier = Modifier.height(12.dp))

            Text("REBAR REINFORCEMENT", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { rebarType = 0 },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (rebarType == 0) BlueTool else Color.LightGray,
                        contentColor = if (rebarType == 0) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Standard (Vert + Ties)", fontSize = 12.sp) }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { rebarType = 1 },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (rebarType == 1) BlueTool else Color.LightGray,
                        contentColor = if (rebarType == 1) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Linear (Top/Bottom)", fontSize = 12.sp) }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (rebarType == 0) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    ConstructionTextField(value = rebarVert, onValueChange = { rebarVert = it }, label = "Vertical Bars", modifier = Modifier.weight(1f), onFocus = onFocus)
                    Spacer(modifier = Modifier.width(8.dp))
                    ConstructionTextField(value = rebarTies, onValueChange = { rebarTies = it }, label = "Tie Spacing O.C. (in)", modifier = Modifier.weight(1f), onFocus = onFocus)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    ConstructionTextField(value = barsTop, onValueChange = { barsTop = it }, label = "Top Bars", modifier = Modifier.weight(1f), onFocus = onFocus)
                    Spacer(modifier = Modifier.width(8.dp))
                    ConstructionTextField(value = barsBottom, onValueChange = { barsBottom = it }, label = "Bottom Bars", modifier = Modifier.weight(1f), onFocus = onFocus)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = sectionName, onValueChange = { sectionName = it }, label = { Text("Section Name") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val d = FractionUtils.parse(diameter)
                        // Master's ColumnsActivity treats a bare height number as feet.
                        val h = FractionUtils.parseFeet(height)
                        val q = qty.toDoubleOrNull() ?: 1.0
                        if (d > 0 && h > 0) {
                            val res = ColumnEngine.calculate(d, h)
                            results = res

                            val vert = rebarVert.toDoubleOrNull() ?: 0.0
                            val tieOC = rebarTies.toDoubleOrNull() ?: 12.0
                            val top = barsTop.toDoubleOrNull() ?: 0.0
                            val bottom = barsBottom.toDoubleOrNull() ?: 0.0
                            val rebarFt = ColumnEngine.calculateRebar(rebarType, d, h, res.circumference, vert, tieOC, top, bottom) * q
                            resultsRebarFt = rebarFt

                            val entry = ColumnEntry(
                                id = if (editingIndex != -1) entries[editingIndex].id else (entries.size + 1).toString(),
                                name = sectionName.ifEmpty { "Column Section" },
                                diameter = diameter,
                                height = height,
                                volume = "${((res.volume / 46656.0) * q).roundToOneDecimal()} cu yd",
                                surfaceArea = "${(res.surfaceArea / 144.0).roundToOneDecimal()} sq ft",
                                circumference = FractionUtils.formatInches(res.circumference),
                                qty = q.toInt().toString(),
                                rebarFt = sprintf("%.0f", rebarFt),
                                rebarType = rebarType,
                                rebarVert = rebarVert,
                                rebarTies = rebarTies,
                                barsTop = barsTop,
                                barsBottom = barsBottom
                            )

                            if (editingIndex != -1) {
                                val newList = entries.toMutableList()
                                newList[editingIndex] = entry
                                onUpdateEntries(newList)
                                editingIndex = -1
                            } else {
                                onUpdateEntries(entries + entry)
                            }
                            sectionName = ""
                        }
                    },
                    modifier = Modifier.weight(2f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)
                ) {
                    Text(if (editingIndex != -1) "UPDATE ITEM" else "CALCULATE & SAVE", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()); results = null },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFC62828)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            results?.let { res ->
                Spacer(modifier = Modifier.height(24.dp))
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("CIRCLE / COLUMN RESULTS", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Volume: ${((res.volume / 46656.0) * (qty.toDoubleOrNull() ?: 1.0)).roundToOneDecimal()} cu yd", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                        Text("Circumference: ${FractionUtils.formatInches(res.circumference)}", fontSize = 16.sp)
                        Text("Surface Area: ${(res.surfaceArea / 144.0).roundToOneDecimal()} sq ft", fontSize = 16.sp)
                        Text(sprintf("Rebar Total: %.0f' ", resultsRebarFt), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    }
                }
            }

            if (entries.isNotEmpty()) {
                val totalVol = entries.sumOf { it.volume.replace(" cu yd", "").toDoubleOrNull() ?: 0.0 }
                val totalRebar = entries.sumOf { it.rebarFt.toDoubleOrNull() ?: 0.0 }
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED COLUMNS", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(sprintf("Total: %.4f cu yd | %.0f' Rebar", totalVol, totalRebar), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                Spacer(modifier = Modifier.height(4.dp))

                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 1.dp, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("Qty: ${entry.qty} | Diam: ${entry.diameter} | Height: ${entry.height}", fontSize = 12.sp, color = Color.Gray)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    diameter = entry.diameter
                                    height = entry.height
                                    qty = entry.qty
                                    rebarType = entry.rebarType
                                    rebarVert = entry.rebarVert
                                    rebarTies = entry.rebarTies
                                    barsTop = entry.barsTop
                                    barsBottom = entry.barsBottom
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("Volume: ${entry.volume}", fontWeight = FontWeight.Bold, color = BlueTool)
                            Text("Rebar: ${entry.rebarFt}' | Circumf: ${entry.circumference}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun CircularStairCalculator(
    entries: List<CircularStairEntry>,
    onUpdateEntries: (List<CircularStairEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var totalRise by remember { mutableStateOf("") }
    var innerRadius by remember { mutableStateOf("18") }
    var treadWidth by remember { mutableStateOf("45") }
    var numTreads by remember { mutableStateOf("8") }
    var straightTreads by remember { mutableStateOf("0") }
    var straightRun by remember { mutableStateOf("10") }
    var nosing by remember { mutableStateOf("1") }
    var circleSize by remember { mutableStateOf("2/4") }
    var sectionName by remember { mutableStateOf("") }

    var results by remember { mutableStateOf<CircularStairEngine.CircStairResult?>(null) }
    var editingIndex by remember { mutableStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Circular / Spiral Stairs", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("STAIR DIMENSIONS", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(value = totalRise, onValueChange = { totalRise = it }, label = "Total Rise (ft/in)", modifier = Modifier.weight(1f), onFocus = onFocus)
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(value = innerRadius, onValueChange = { innerRadius = it }, label = "Inner Radius (in)", modifier = Modifier.weight(1f), onFocus = onFocus)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(value = treadWidth, onValueChange = { treadWidth = it }, label = "Tread Width (in)", modifier = Modifier.weight(1f), onFocus = onFocus)
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(value = numTreads, onValueChange = { numTreads = it }, label = "Num Treads", modifier = Modifier.weight(1f), onFocus = onFocus)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(value = straightTreads, onValueChange = { straightTreads = it }, label = "Straight Treads", modifier = Modifier.weight(1f), onFocus = onFocus)
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(value = straightRun, onValueChange = { straightRun = it }, label = "Straight Run (in)", modifier = Modifier.weight(1f), onFocus = onFocus)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ConstructionTextField(value = nosing, onValueChange = { nosing = it }, label = "Nosing (in)", modifier = Modifier.weight(1f), onFocus = onFocus)
                Spacer(modifier = Modifier.width(8.dp))
                ConstructionTextField(value = circleSize, onValueChange = { circleSize = it }, label = "Circle Size (e.g. 2/4)", modifier = Modifier.weight(1f), onFocus = onFocus)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = sectionName, onValueChange = { sectionName = it }, label = { Text("Section Name") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val tr = FractionUtils.parse(totalRise)
                        val ir = FractionUtils.parse(innerRadius)
                        val tw = FractionUtils.parse(treadWidth)
                        val nt = numTreads.toDoubleOrNull() ?: 1.0
                        val st = straightTreads.toDoubleOrNull() ?: 0.0
                        var circleFactor = 0.5
                        if (circleSize.contains("/")) {
                            val parts = circleSize.split("/")
                            if (parts.size == 2) {
                                val n = parts[0].toDoubleOrNull() ?: 2.0
                                val d = parts[1].toDoubleOrNull() ?: 4.0
                                circleFactor = if (d != 0.0) n / d else 0.5
                            }
                        }
                        if (tr > 0 && ir > 0) {
                            val res = CircularStairEngine.calculate(tr, ir, tw, nt, st, circleFactor)
                            results = res
                            val entry = CircularStairEntry(
                                id = if (editingIndex != -1) entries[editingIndex].id else (entries.size + 1).toString(),
                                name = sectionName.ifEmpty { "Spiral Section" },
                                totalRise = totalRise,
                                radius = innerRadius,
                                walkTread = treadWidth,
                                numTreads = numTreads,
                                straightTreads = straightTreads,
                                straightRun = straightRun,
                                nosing = nosing,
                                circleSize = circleSize,
                                outerArcTread = FractionUtils.formatInches(res.outerArcTread),
                                innerArcTread = FractionUtils.formatInches(res.innerArcTread),
                                outsideRadius = FractionUtils.formatInches(res.outsideRadius),
                                anglePerTread = "${res.anglePerTread.roundToOneDecimal()}°"
                            )
                            if (editingIndex != -1) {
                                val newList = entries.toMutableList(); newList[editingIndex] = entry
                                onUpdateEntries(newList); editingIndex = -1
                            } else { onUpdateEntries(entries + entry) }
                            sectionName = ""
                        }
                    },
                    modifier = Modifier.weight(2f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)
                ) { Text("CALCULATE & SAVE") }
            }

            results?.let { res ->
                Spacer(modifier = Modifier.height(24.dp))
                Card(elevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("SPIRAL LAYOUT", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Outside Radius: ${FractionUtils.formatInches(res.outsideRadius)}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Angle per Tread: ${res.anglePerTread.roundToOneDecimal()}°", fontSize = 14.sp)
                        Text("Actual Rise: ${FractionUtils.formatInches(res.actualRise)}", fontSize = 14.sp)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Arc per Tread (Outer / Inner):", fontSize = 12.sp, color = Color.Gray)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Outer Arc", fontSize = 11.sp, color = Color.Gray)
                                Text(FractionUtils.formatInches(res.outerArcTread), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Inner Arc", fontSize = 11.sp, color = Color.Gray)
                                Text(FractionUtils.formatInches(res.innerArcTread), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BlueTool)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Circle Portion (Outer / Inner):", fontSize = 12.sp, color = Color.Gray)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(FractionUtils.formatInches(res.outerCircPortion), fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(FractionUtils.formatInches(res.innerCircPortion), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED SPIRAL STAIRS", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 1.dp, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("Rise: ${entry.totalRise} | ${entry.numTreads} Treads | Circle: ${entry.circleSize}", fontSize = 12.sp, color = Color.Gray)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    totalRise = entry.totalRise
                                    innerRadius = entry.radius
                                    treadWidth = entry.walkTread
                                    numTreads = entry.numTreads
                                    straightTreads = entry.straightTreads
                                    straightRun = entry.straightRun
                                    nosing = entry.nosing
                                    circleSize = entry.circleSize
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                "Outside Radius: ${entry.outsideRadius} | Angle/Tread: ${entry.anglePerTread}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlueTool
                            )
                            Text("Outer Arc: ${entry.outerArcTread} | Inner Arc: ${entry.innerArcTread}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScientificCalculator(projectName: String, onBack: () -> Unit, onNavigate: (Screen) -> Unit, initialGraphMode: Boolean = false) {
    var expression by remember { mutableStateOf("") }
    var isGraphingMode by remember { mutableStateOf(initialGraphMode) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Project: $projectName", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                backgroundColor = BlueTool
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)).padding(padding)) {
            // Display / Plot Area
            Box(modifier = Modifier.fillMaxWidth().weight(1.5f).background(Color(0xFFB9C4B1))) {
                if (isGraphingMode) {
                    GraphPlotter(expression)
                    Text(
                        text = "y =",
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 120.dp),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black.copy(alpha = 0.4f)
                    )
                }
                Text(
                    text = expression.ifEmpty { "0." },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    fontSize = if (isGraphingMode) 48.sp else 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1
                )
            }

            // Keypad
            Column(modifier = Modifier.fillMaxWidth().weight(2.5f).padding(1.dp)) {
                // Scientific Rows (5 columns)
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    SciBtn("SIN") { expression += "sin(" }
                    SciBtn("COS") { expression += "cos(" }
                    SciBtn("TAN") { expression += "tan(" }
                    SciBtn("LOG") { expression += "log(" }
                    SciBtn("LN") { expression += "ln(" }
                }
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    SciBtn("√") { expression += "sqrt(" }
                    SciBtn("^") { expression += "^" }
                    SciBtn("(") { expression += "(" }
                    SciBtn(")") { expression += ")" }
                    SciBtn("X") { expression += "x" }
                }
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    SciBtn("π") { expression += "pi" }
                    SciBtn("E") { expression += "e" }
                    SciBtn("1/X") { expression += "1/" }
                    SciBtn("ABS") { expression += "abs(" }
                    CalcButton("AC", "", Color(0xFFD32F2F), Modifier.weight(1f)) { expression = "" }
                }
                
                // Shared Numeric and Basic Operators
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    CalcButton("STANDARD", "", Color(0xFFFF9800), Modifier.weight(1f)) { onBack() }
                    NumBtn("7") { expression += "7" }
                    NumBtn("8") { expression += "8" }
                    NumBtn("9") { expression += "9" }
                    CalcButton("÷", "", Color(0xFFFF9800), Modifier.weight(1f)) { expression += "/" }
                }
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    CalcButton("HELP", "", GrayBtn, Modifier.weight(1f)) { onNavigate(Screen.CalculatorHelp) }
                    NumBtn("4") { expression += "4" }
                    NumBtn("5") { expression += "5" }
                    NumBtn("6") { expression += "6" }
                    CalcButton("x", "", Color(0xFFFF9800), Modifier.weight(1f)) { expression += "*" }
                }
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    CalcButton(if (isGraphingMode) "STANDARD" else "GRAPH", "", if (isGraphingMode) Color.DarkGray else Color(0xFFFF9800), Modifier.weight(1f)) { isGraphingMode = !isGraphingMode }
                    NumBtn("1") { expression += "1" }
                    NumBtn("2") { expression += "2" }
                    NumBtn("3") { expression += "3" }
                    CalcButton("-", "", Color(0xFFFF9800), Modifier.weight(1f)) { expression += "-" }
                }
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    CalcButton("BACK", "", Color(0xFFD32F2F), Modifier.weight(1f)) { 
                        if (expression.isNotEmpty()) expression = expression.dropLast(1)
                    }
                    NumBtn("0") { expression += "0" }
                    NumBtn(".") { expression += "." }
                    CalcButton("=", "", Color(0xFFFF9800), Modifier.weight(1f)) {
                        val res = MathEngine.evaluate(expression)
                        expression = if (res.isNaN()) "Error" else if (res % 1.0 == 0.0) res.toLong().toString() else res.toString()
                    }
                    CalcButton("+", "", Color(0xFFFF9800), Modifier.weight(1f)) { expression += "+" }
                }
            }
        }
    }
}

@Composable
fun CalculatorHelpScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Text("Calculator Guide", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("SCIENTIFIC INSTRUCTIONS", fontWeight = FontWeight.Bold, color = BlueTool, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            HelpText("• Use parentheses for nested logic: sin(pi / 2)")
            HelpText("• Power operator (^) for exponents: 2 ^ 8")
            HelpText("• Constants pi and e are built-in")
            HelpText("• Trigonometry functions use radians")

            Spacer(modifier = Modifier.height(24.dp))
            Text("GRAPHING EXAMPLES", fontWeight = FontWeight.Bold, color = BlueTool, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            EquationExample("Standard Line:", "0.5 * x + 2")
            EquationExample("Parabola:", "x ^ 2")
            EquationExample("Sine Wave:", "sin(x)")
            EquationExample("Complex Curve:", "sin(x) * x")
            EquationExample("Absolute Value:", "abs(x)")
            EquationExample("Discontinuity:", "1 / x")

            Spacer(modifier = Modifier.height(24.dp))
            Text("GRAPHING TIPS", fontWeight = FontWeight.Bold, color = BlueTool, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            HelpText("• Toggle between STANDARD and GRAPH mode using the orange button")
            HelpText("• The variable 'x' is required for any plot to appear")
            HelpText("• Use 'AC' to clear the current equation")
        }
    }
}

@Composable
fun SettingsScreen(
    settings: AppSettingsData,
    onSave: (AppSettingsData) -> Unit,
    onBack: () -> Unit
) {
    var precisionIndex by remember { mutableStateOf(settings.precisionIndex) }
    var alwaysFloating by remember { mutableStateOf(settings.alwaysFloating) }
    var immersiveMode by remember { mutableStateOf(settings.immersiveMode) }
    var precisionMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Settings", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("FRACTION PRECISION", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Controls the smallest fraction shown across every calculator (e.g. 1/16\").", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Box {
                OutlinedButton(onClick = { precisionMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(AppSettingsData.LABELS[precisionIndex])
                }
                DropdownMenu(expanded = precisionMenuExpanded, onDismissRequest = { precisionMenuExpanded = false }) {
                    AppSettingsData.LABELS.forEachIndexed { i, label ->
                        DropdownMenuItem(onClick = { precisionIndex = i; precisionMenuExpanded = false }) { Text(label) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = alwaysFloating, onCheckedChange = { alwaysFloating = it })
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text("Always Floating", fontWeight = FontWeight.Medium)
                    Text("Keep the calculator in a floating window where the platform supports it.", fontSize = 11.sp, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = immersiveMode, onCheckedChange = { immersiveMode = it })
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text("Immersive Mode", fontWeight = FontWeight.Medium)
                    Text("Hide system bars for a distraction-free view where the platform supports it.", fontSize = 11.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { onSave(AppSettingsData(precisionIndex, alwaysFloating, immersiveMode)) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)
            ) { Text("SAVE SETTINGS", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BlueTool).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Text("About Pro Calc", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Calculate,
                contentDescription = null,
                tint = BlueTool,
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Pro Construction Calculator", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BlueTool, textAlign = TextAlign.Center)
            Text("Version 2.0.0", fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "The ultimate professional tool for framing, stairs, rafters, and field takeoff. Built for builders, by builders.",
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(32.dp))
            Divider(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(24.dp))
            Text("CONTACT INFORMATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BlueTool)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Support & Inquiries:", fontSize = 14.sp, color = Color.Black)
            Text(
                "djransom1@gmail.com",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BlueTool,
                modifier = Modifier.padding(top = 4.dp).clickable {
                    uriHandler.openUri("mailto:djransom1@gmail.com?subject=Construction%20Calculator%20Support")
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
            Text("© 2026 Construction Solutions Inc.", fontSize = 12.sp, color = Color.LightGray)
        }
    }
}

@Composable
fun HelpText(text: String) {
    Text(text = text, fontSize = 14.sp, color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
fun EquationExample(label: String, equation: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Card(backgroundColor = Color(0xFFF5F5F5), elevation = 0.dp, shape = RoundedCornerShape(4.dp)) {
            Text(equation, modifier = Modifier.padding(8.dp), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = BlueTool)
        }
    }
}

@Composable
fun RowScope.SciBtn(label: String, onClick: () -> Unit) {
    CalcButton(label, "", BlueTool, Modifier.weight(1f), onClick)
}

@Composable
fun RowScope.NumBtn(label: String, onClick: () -> Unit) {
    CalcButton(label, "", Color.Gray, Modifier.weight(1f), onClick)
}

@Composable
fun GraphingCalculator(projectName: String, onBack: () -> Unit, onNavigate: (Screen) -> Unit) {
    // Matches master's toggleGraphMode(): same underlying calculator, just opened with
    // graph mode on instead of off (Scientific Calc opens in standard/off mode).
    ScientificCalculator(projectName, onBack, onNavigate, initialGraphMode = true)
}


