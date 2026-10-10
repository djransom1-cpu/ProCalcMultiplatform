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
    Account,
    About,
    UnitConverter,
    Menu
}

/** Screens reached from the calculator-type switcher. */
val CalcModeScreens = setOf(Screen.Calculator, Screen.ScientificCalculator, Screen.GraphingCalculator, Screen.UnitConverter)

/** Screens that follow the light/dark setting. */
val ShellScreens = CalcModeScreens + Screen.Menu

/** Screens opened from somewhere else, that back (the arrow or a swipe) returns from. */
val SubScreens = ToolCatalog.map { it.screen }.toSet() + setOf(Screen.CalculatorHelp, Screen.About, Screen.Account)

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
    val jack24: String,
    /** Shed roof: [span] holds the rafter run. Missing in older saves, which read as gable. */
    val isShed: Boolean = false
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
    val ridgeOffset: String
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
    val tape: String
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
    val fasteners: String
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
    val drop: String
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
    val arcHeights: List<Double> = emptyList()
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
    val circumference: String
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
        coordinatePaths: List<PlottedPath>
    ): List<SummarySection> {
        val sections = mutableListOf<SummarySection>()

        if (stairEntries.isNotEmpty()) {
            sections.add(SummarySection("STAIR SCHEDULE", listOf("Name", "Rise", "Risers", "Act Rise", "Treads", "Run", "Stringer", "Angle"), stairEntries.map { listOf(it.name, it.rise, it.riserCount, it.actualRise, it.treadCount, it.totalRun, it.stringerLen, it.angleDeg) }))
        }
        if (circularStairEntries.isNotEmpty()) {
            sections.add(SummarySection("CIRCULAR STAIR SCHEDULE", listOf("Name", "Rise", "Inner Rad", "Tread W", "Treads", "Circle", "Out Radius", "Angle/Trd"), circularStairEntries.map { listOf(it.name, it.totalRise, it.radius, it.walkTread, it.numTreads, it.circleSize, it.outsideRadius, it.anglePerTread) }))
        }
        if (rafterEntries.isNotEmpty()) {
            sections.add(SummarySection("RAFTER SCHEDULE", listOf("Name", "Span", "Pitch", "Heel", "O/H", "Com Len", "Hip Len", "Jack 16"), rafterEntries.map { listOf(it.name, if (it.isShed) "${it.span} (shed run)" else it.span, it.pitch, it.heel, it.overhang, it.commonLen, it.hipLen, it.jack16) }))
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
            sections.add(SummarySection("ROOF SCHEDULE", listOf("Name", "Area", "Squares", "Bundles", "Sheets", "Pitch"), roofEntries.map { listOf(it.name, it.area, it.squares, it.bundles, it.sheets, it.dimensions) }))
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
    val cloudLinks: Map<String, CloudLink> = emptyMap()
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

val BlueTool = Color(0xFF0A2A66)
val LabelRed = Color(0xFFD32F2F)
val GrayBtn = Color(0xFF7A7A7A)

@Composable
@Preview
fun App() {
    val appSettings = remember { try { Settings() } catch (e: Throwable) { null } }
    val persistenceManager = remember { PersistenceManager(appSettings) }
    val cloudSync = remember { CloudSync() }
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
    var cloudLinks by remember { mutableStateOf(initialData?.cloudLinks ?: mapOf()) }

    // Menu settings, calculation history and calculator panels
    val uiStore = remember { UiStore(appSettings) }
    var uiPrefs by remember { mutableStateOf(uiStore.loadPrefs()) }
    var calcHistory by remember { mutableStateOf(uiStore.loadHistory()) }
    var calcPanel by remember { mutableStateOf(CalcPanel.Keypad) }
    var editingFavorites by remember { mutableStateOf(false) }
    var lastCalcScreen by remember { mutableStateOf(Screen.Calculator) }
    var backTo by remember { mutableStateOf(Screen.Calculator) }

    var isMetricMode by remember { mutableStateOf(uiPrefs.startMetric) }

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

    fun hideKeyboard() {
        keyboardVisible = false
        focusManager.clearFocus()
    }

    fun currentAppState() = AppState(projectList, currentProjectName, allStairEntries, allRafterEntries, allArcEntries, allWallEntries, allCrownEntries, allGazeboEntries, allConcreteEntries, allRoofEntries, allCoordinatePaths, allMasonryEntries, allDrywallEntries, allDeckEntries, allFloorEntries, allHandrailEntries, allPineLineEntries, allCircularStairEntries, allTrigEntries, allColumnEntries, allFramingEntries, allJobNotes, cloudLinks)

    fun applyAppState(state: AppState) {
        projectList = state.projectList
        currentProjectName = state.currentProjectName
        allStairEntries = state.allStairEntries
        allRafterEntries = state.allRafterEntries
        allArcEntries = state.allArcEntries
        allWallEntries = state.allWallEntries
        allCrownEntries = state.allCrownEntries
        allGazeboEntries = state.allGazeboEntries
        allConcreteEntries = state.allConcreteEntries
        allRoofEntries = state.allRoofEntries
        allCoordinatePaths = state.allCoordinatePaths
        allMasonryEntries = state.allMasonryEntries
        allDrywallEntries = state.allDrywallEntries
        allDeckEntries = state.allDeckEntries
        allFloorEntries = state.allFloorEntries
        allHandrailEntries = state.allHandrailEntries
        allPineLineEntries = state.allPineLineEntries
        allCircularStairEntries = state.allCircularStairEntries
        allTrigEntries = state.allTrigEntries
        allColumnEntries = state.allColumnEntries
        allFramingEntries = state.allFramingEntries
        allJobNotes = state.allJobNotes
        cloudLinks = state.cloudLinks
    }

    // Auto-save whenever critical state changes
    LaunchedEffect(projectList, currentProjectName, allStairEntries, allRafterEntries, allArcEntries, allWallEntries, allCrownEntries, allGazeboEntries, allConcreteEntries, allRoofEntries, allCoordinatePaths, allMasonryEntries, allDrywallEntries, allDeckEntries, allFloorEntries, allHandrailEntries, allPineLineEntries, allCircularStairEntries, allTrigEntries, allColumnEntries, allFramingEntries, allJobNotes, cloudLinks) {
        persistenceManager.saveState(currentAppState())
    }

    // Firebase keeps the Google sign-in between visits
    LaunchedEffect(Unit) { cloudSync.restoreSession() }

    // Calculator Engine State
    var currentInput by remember { mutableStateOf("") }
    var historyText by remember { mutableStateOf("") }
    var firstValue by remember { mutableStateOf<DimensionValue?>(null) }
    var pendingOperation by remember { mutableStateOf("") }
    var isNewEntry by remember { mutableStateOf(true) }

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

    // In metric mode, show lengths, areas and volumes in metric; everything else
    // (and all of imperial mode) is shown exactly as before
    fun formatResult(value: DimensionValue): String =
        if (isMetricMode && value.unitPower in 1..3) FractionUtils.formatMetric(value.value, value.unitPower, uiPrefs.metricPrecision)
        else value.toString()

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
                currentInput = formatResult(result)
            }
        }
        val first = getCurrentDimensionValue()
        firstValue = first
        pendingOperation = op
        historyText = "${formatResult(first)} $op"
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
        
        historyText = "${formatResult(first)} $pendingOperation ${formatResult(secondValue)} ="
        currentInput = formatResult(result)
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

    // mm key on the metric keypad. FractionUtils.parse already reads "mm"; the key
    // only adds the unit after a typed number
    fun handleMillimeter() {
        if (isNewEntry) {
            if (historyText.contains("=")) historyText = ""
            currentInput = "0 mm"
            isNewEntry = false
            return
        }
        val last = currentInput.lastOrNull()
        if (last != null && (last.isDigit() || last == '.')) currentInput += " mm"
    }

    fun clearAll() {
        currentInput = ""
        historyText = ""
        firstValue = null
        pendingOperation = ""
        isNewEntry = true
    }

    // History only reads the finished answer after calculateResult runs
    fun recordHistory() {
        if (!uiPrefs.keepHistory) return
        val entry = HistoryEntry(historyText.removeSuffix("=").trim(), currentInput, currentTimeMillis())
        calcHistory = (calcHistory + entry).takeLast(MAX_HISTORY)
        uiStore.saveHistory(calcHistory)
    }

    fun clearHistory() {
        calcHistory = emptyList()
        uiStore.saveHistory(calcHistory)
    }

    // Tapping a past answer puts it on the display, like typing it in
    fun useHistoryEntry(entry: HistoryEntry) {
        currentInput = entry.result
        if (pendingOperation.isEmpty()) historyText = ""
        isNewEntry = true
        calcPanel = CalcPanel.Keypad
    }

    fun updatePrefs(prefs: UiPrefs) {
        uiPrefs = prefs
        uiStore.savePrefs(prefs)
    }

    fun toggleFavorite(screen: Screen) {
        val favorites = uiPrefs.favorites.toMutableList()
        if (!favorites.remove(screen.name)) {
            favorites.add(screen.name)
            while (favorites.size > MAX_FAVORITES) favorites.removeAt(0)
        }
        updatePrefs(uiPrefs.copy(favorites = favorites))
    }

    // Leaving a screen (back arrow, a tab, the calculator type menu) closes its keypad
    fun goTo(screen: Screen) {
        if (screen in CalcModeScreens) lastCalcScreen = screen
        currentScreen = screen
        hideKeyboard()
    }

    fun selectTab(tab: NavTab) {
        when (tab) {
            NavTab.Calculator -> goTo(lastCalcScreen)
            NavTab.Projects -> goTo(Screen.ProjectList)
            NavTab.DataSheet -> goTo(Screen.JobSummary)
            NavTab.Notes -> goTo(Screen.JobNotes)
            NavTab.Menu -> goTo(Screen.Menu)
        }
    }

    // Opens a tool, help or account screen; back returns to wherever it was opened from
    fun open(screen: Screen) {
        backTo = currentScreen
        goTo(screen)
    }

    fun goBack() = goTo(backTo)

    fun openAccount() = open(Screen.Account)

    fun handleAction(action: CalcAction) {
        when (action) {
            is CalcAction.Digit -> appendNumber(action.digit)
            is CalcAction.Op -> setOperation(action.op)
            CalcAction.Equals -> {
                val hadOperation = firstValue != null
                calculateResult()
                if (hadOperation) recordHistory()
            }
            CalcAction.Feet -> handleFeet()
            CalcAction.Inch -> handleInch()
            CalcAction.Millimeter -> handleMillimeter()
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
                    FractionUtils.formatMetric(dv.value, dv.unitPower, uiPrefs.metricPrecision)
                } else {
                    dv.toString()
                }
                isNewEntry = true
            }
            CalcAction.Convert -> {
                val dv = getCurrentDimensionValue()
                // Simple cycle: Decimal -> Fractional -> Metric
                currentInput = if (currentInput.contains("/")) {
                    FractionUtils.formatMetric(dv.value, dv.unitPower, uiPrefs.metricPrecision)
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

    val darkActive = when (uiPrefs.appearance) {
        Appearance.System -> isSystemInDarkTheme()
        Appearance.Light -> false
        Appearance.Dark -> true
    }
    // The calculator screens, Menu and the tools follow light/dark; projects, data sheet,
    // notes, help and account stay light until they get the same treatment
    val lightScreen = currentScreen !in ShellScreens && toolFor(currentScreen) == null
    SystemBarsAppearance(darkBackground = darkActive, followSystem = uiPrefs.appearance == Appearance.System)

    // Back swipe or button: closes the tool keyboard first, then the tool, help or account screen
    BackGesture(enabled = currentScreen in SubScreens, onBack = ::goBack)
    BackGesture(enabled = keyboardVisible, onBack = ::hideKeyboard)

    ProCalcTheme(dark = darkActive) {
        Column(modifier = Modifier.fillMaxSize().background(LocalAppColors.current.bg)) {
            Spacer(modifier = Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars))
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                ProCalcTheme(dark = darkActive && !lightScreen) {
                    Box(modifier = Modifier.fillMaxSize().background(LocalAppColors.current.bg)) {
                        when (currentScreen) {
                            Screen.Calculator -> CalculatorScreen(
                                projectName = currentProjectName,
                                onProjectClick = { goTo(Screen.ProjectList) },
                                cloudSync = cloudSync,
                                onCloudClick = ::openAccount,
                                input = currentInput,
                                expression = historyText,
                                isMetric = isMetricMode,
                                panel = calcPanel,
                                onPanelChange = { calcPanel = it },
                                favorites = uiPrefs.favoriteScreens,
                                editingFavorites = editingFavorites,
                                onEditFavoritesChange = { editingFavorites = it },
                                onToggleFavorite = ::toggleFavorite,
                                history = calcHistory,
                                historyOn = uiPrefs.keepHistory,
                                onUseHistory = ::useHistoryEntry,
                                onClearHistory = ::clearHistory,
                                onModeSelect = { goTo(it.screen) },
                                onOpenTool = ::open,
                                onAction = ::handleAction,
                            )

                            Screen.ScientificCalculator, Screen.GraphingCalculator -> ScientificScreen(
                                graphing = currentScreen == Screen.GraphingCalculator,
                                projectName = currentProjectName,
                                onProjectClick = { goTo(Screen.ProjectList) },
                                cloudSync = cloudSync,
                                onCloudClick = ::openAccount,
                                onModeSelect = { goTo(it.screen) },
                                onHelp = { open(Screen.CalculatorHelp) },
                            )

                            Screen.UnitConverter -> UnitConverterScreen(
                                projectName = currentProjectName,
                                onProjectClick = { goTo(Screen.ProjectList) },
                                cloudSync = cloudSync,
                                onCloudClick = ::openAccount,
                                onModeSelect = { goTo(it.screen) },
                                onEditValue = openKeyboard,
                            )

                            Screen.Menu -> MenuScreen(
                                prefs = uiPrefs,
                                onPrefsChange = ::updatePrefs,
                                onEditFavorites = {
                                    editingFavorites = true
                                    calcPanel = CalcPanel.Tools
                                    goTo(Screen.Calculator)
                                },
                                onClearHistory = ::clearHistory,
                                onOpen = ::open,
                            )
                            Screen.ConcreteCalculator -> ConcreteCalculator(
                                entries = allConcreteEntries[currentProjectName] ?: emptyList(),
                                onUpdateEntries = { newList ->
                                    allConcreteEntries = allConcreteEntries.toMutableMap().apply { put(currentProjectName, newList) }
                                },
                                onBack = ::goBack,
                                onFocus = openKeyboard
                            )

                            Screen.RafterCalculator -> RafterCalculator(
                                entries = allRafterEntries[currentProjectName] ?: emptyList(),
                                onUpdateEntries = { newList ->
                                    allRafterEntries = allRafterEntries.toMutableMap().apply { put(currentProjectName, newList) }
                                },
                                onBack = ::goBack,
                                onFocus = openKeyboard
                            )

                            Screen.ArcCalculator -> ArcCalculator(
                                entries = allArcEntries[currentProjectName] ?: emptyList(),
                                onUpdateEntries = { newList ->
                                    allArcEntries = allArcEntries.toMutableMap().apply { put(currentProjectName, newList) }
                                },
                                onBack = ::goBack,
                                onFocus = openKeyboard
                            )

                            Screen.WallCalculator -> WallCalculator(
                                entries = allWallEntries[currentProjectName] ?: emptyList(),
                                onUpdateEntries = { newList ->
                                    allWallEntries = allWallEntries.toMutableMap().apply { put(currentProjectName, newList) }
                                },
                                onBack = ::goBack,
                                onFocus = openKeyboard
                            )

                            Screen.FramingCalculator -> FramingCalculator(
                                entries = allFramingEntries[currentProjectName] ?: emptyList(),
                                onUpdateEntries = { newList ->
                                    allFramingEntries = allFramingEntries.toMutableMap().apply { put(currentProjectName, newList) }
                                },
                                onBack = ::goBack,
                                onFocus = openKeyboard
                            )

                            Screen.CrownCalculator -> CrownCalculator(
                                entries = allCrownEntries[currentProjectName] ?: emptyList(),
                                onUpdateEntries = { newList ->
                                    allCrownEntries = allCrownEntries.toMutableMap().apply { put(currentProjectName, newList) }
                                },
                                onBack = ::goBack,
                                onFocus = openKeyboard
                            )

                            Screen.GazeboCalculator -> GazeboCalculator(
                                entries = allGazeboEntries[currentProjectName] ?: emptyList(),
                                onUpdateEntries = { newList ->
                                    allGazeboEntries = allGazeboEntries.toMutableMap().apply { put(currentProjectName, newList) }
                                },
                                onBack = ::goBack,
                                onFocus = openKeyboard
                            )

                            Screen.RoofCalculator -> RoofCalculator(
                                entries = allRoofEntries[currentProjectName] ?: emptyList(),
                                onUpdateEntries = { newList ->
                                    allRoofEntries = allRoofEntries.toMutableMap().apply { put(currentProjectName, newList) }
                                },
                                onBack = ::goBack,
                                onFocus = openKeyboard
                            )

                            Screen.CoordinateCalculator -> CoordinateCalculator(
                                paths = allCoordinatePaths[currentProjectName] ?: emptyList(),
                                onUpdatePaths = { newList ->
                                    allCoordinatePaths = allCoordinatePaths.toMutableMap().apply { put(currentProjectName, newList) }
                                },
                                onBack = ::goBack,
                                onFocus = openKeyboard
                            )

                            Screen.MasonryCalculator -> MasonryCalculator(
                                entries = allMasonryEntries[currentProjectName] ?: emptyList(),
                                onUpdateEntries = { newList ->
                                    allMasonryEntries = allMasonryEntries.toMutableMap().apply { put(currentProjectName, newList) }
                                },
                                onBack = ::goBack,
                                onFocus = openKeyboard
                            )

                            Screen.DrywallCalculator -> DrywallCalculator(
                                entries = allDrywallEntries[currentProjectName] ?: emptyList(),
                                onUpdateEntries = { newList ->
                                    allDrywallEntries = allDrywallEntries.toMutableMap().apply { put(currentProjectName, newList) }
                                },
                                onBack = ::goBack,
                                onFocus = openKeyboard
                            )

                            Screen.DeckCalculator -> DeckCalculator(
                                entries = allDeckEntries[currentProjectName] ?: emptyList(),
                                onUpdateEntries = { newList ->
                                    allDeckEntries = allDeckEntries.toMutableMap().apply { put(currentProjectName, newList) }
                                },
                                onBack = ::goBack,
                                onFocus = openKeyboard
                            )

                            Screen.FloorCalculator -> FloorCalculator(
                                entries = allFloorEntries[currentProjectName] ?: emptyList(),
                                onUpdateEntries = { newList ->
                                    allFloorEntries = allFloorEntries.toMutableMap().apply { put(currentProjectName, newList) }
                                },
                                onBack = ::goBack,
                                onFocus = openKeyboard
                            )

                            Screen.HandrailCalculator -> HandrailCalculator(
                                entries = allHandrailEntries[currentProjectName] ?: emptyList(),
                                onUpdateEntries = { newList ->
                                    allHandrailEntries = allHandrailEntries.toMutableMap().apply { put(currentProjectName, newList) }
                                },
                                onBack = ::goBack,
                                onFocus = openKeyboard
                            )

                            Screen.PineLineCalculator -> PineLineCalculator(
                                entries = allPineLineEntries[currentProjectName] ?: emptyList(),
                                onUpdateEntries = { newList ->
                                    allPineLineEntries = allPineLineEntries.toMutableMap().apply { put(currentProjectName, newList) }
                                },
                                onBack = ::goBack,
                                onFocus = openKeyboard
                            )

                            Screen.CircularStairCalculator -> CircularStairCalculator(
                                entries = allCircularStairEntries[currentProjectName] ?: emptyList(),
                                onUpdateEntries = { newList ->
                                    allCircularStairEntries = allCircularStairEntries.toMutableMap().apply { put(currentProjectName, newList) }
                                },
                                onBack = ::goBack,
                                onFocus = openKeyboard
                            )

                            Screen.TrigCalculator -> TrigCalculator(
                                entries = allTrigEntries[currentProjectName] ?: emptyList(),
                                onUpdateEntries = { newList ->
                                    allTrigEntries = allTrigEntries.toMutableMap().apply { put(currentProjectName, newList) }
                                },
                                onBack = ::goBack,
                                onFocus = openKeyboard
                            )

                            Screen.ColumnCalculator -> ColumnCalculator(
                                entries = allColumnEntries[currentProjectName] ?: emptyList(),
                                onUpdateEntries = { newList ->
                                    allColumnEntries = allColumnEntries.toMutableMap().apply { put(currentProjectName, newList) }
                                },
                                onBack = ::goBack,
                                onFocus = openKeyboard
                            )


                            Screen.CalculatorHelp -> CalculatorHelpScreen(
                                onBack = ::goBack
                            )


                            Screen.JobNotes -> JobNotesScreen(
                                savedPaths = allJobNotes[currentProjectName] ?: emptyList(),
                                onUpdateNotes = { newList ->
                                    allJobNotes = allJobNotes.toMutableMap().apply { put(currentProjectName, newList) }
                                },
                                onBack = { currentScreen = Screen.Calculator }
                            )

                            Screen.StairCalculator -> StairCalculator(
                                entries = allStairEntries[currentProjectName] ?: emptyList(),
                                onUpdateEntries = { newList ->
                                    allStairEntries = allStairEntries.toMutableMap().apply { put(currentProjectName, newList) }
                                },
                                onBack = ::goBack,
                                onFocus = openKeyboard
                            )

                            Screen.ProjectList -> {
                                val pdfProvider = rememberPdfExportProvider()
                                // Like the app's project list: pick up new and changed cloud projects on open
                                LaunchedEffect(cloudSync.user) {
                                    cloudSync.mergeCloudProjects(::currentAppState, ::applyAppState)
                                }
                                ProjectListScreen(
                                    projects = projectList,
                                    cloudSync = cloudSync,
                                    cloudLinks = cloudLinks,
                                    onCloudSync = { name ->
                                        if (cloudSync.isSignedIn) {
                                            scope.launch { cloudSync.syncProject(name, ::currentAppState, ::applyAppState) }
                                        } else {
                                            currentScreen = Screen.Account
                                        }
                                    },
                                    onJoinByCode = { code ->
                                        scope.launch { cloudSync.joinByCode(code, ::currentAppState, ::applyAppState) }
                                    },
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
                                        cloudLinks = cloudLinks - name
                                        if (currentProjectName == name) currentProjectName = "Default"
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
                                            coordinatePaths = allCoordinatePaths[name] ?: emptyList()
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
                                                coordinatePaths = allCoordinatePaths[name] ?: emptyList()
                                            ))
                                        }
                                        pdfProvider.share("Master_Report", allSections)
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
                                onBack = { currentScreen = Screen.Calculator }
                            )

                            Screen.About -> AboutScreen(
                                onBack = ::goBack
                            )

                            Screen.Account -> AccountScreen(
                                cloudSync = cloudSync,
                                onOpenProjects = { currentScreen = Screen.ProjectList },
                                onBack = ::goBack
                            )
                        }
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
                            onDone = ::hideKeyboard,
                            onHide = ::hideKeyboard
                        )
                    }
                }
            }
            BottomNav(current = tabFor(currentScreen), onSelect = ::selectTab)
        }
    }
}

sealed class CalcAction {
    data class Digit(val digit: String) : CalcAction()
    data class Op(val op: String) : CalcAction()
    object Equals : CalcAction()
    object Feet : CalcAction()
    object Inch : CalcAction()
    object Millimeter : CalcAction()
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
fun CoordinateCalculator(
    paths: List<PlottedPath>,
    onUpdatePaths: (List<PlottedPath>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
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

    var editingPathIdx by remember { mutableStateOf(-1) }
    var editingPtIdx by remember { mutableStateOf(-1) }

    var showTakeoffDialog by remember { mutableStateOf(false) }
    var mapView by remember { mutableStateOf(true) }

    // The map or list below fills the rest of the screen, so the page itself doesn't scroll
    ToolScreen(
        Screen.CoordinateCalculator, onBack, scrollable = false,
        actions = {
            TextButton(onClick = { mapView = !mapView }) {
                Text(if (mapView) "LIST VIEW" else "MAP VIEW", color = AppTheme.colors.accent, fontWeight = FontWeight.SemiBold)
            }
        },
    ) {

        // Input Section
        Column(modifier = Modifier.background(AppTheme.colors.surface).padding(16.dp)) {
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
                        Text(if (isTakeoff) "TAKEOFF" else "BOUNDARY", color = if (isTakeoff) AppTheme.colors.accent else AppTheme.colors.muted, fontSize = 12.sp)
                    }
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
                                    isTakeoff = isTakeoff
                                ))
                            } else {
                                currentPaths[currentPaths.size - 1] = lastPath.copy(points = lastPath.points + newPt)
                            }
                        }
                        
                        onUpdatePaths(currentPaths)
                        northInput = ""; eastInput = ""; elevInput = ""; description = ""; isNewPath = false
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)
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
                    TextButton(onClick = { /* Manage */ }) { Text("MANAGE LINES", fontSize = 12.sp) }
                    TextButton(onClick = { onUpdatePaths(emptyList()) }, colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.danger)) {
                        Text("RESET ALL", fontSize = 12.sp)
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (mapView) {
                CoordinateMapView(paths)
                Column(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                    Button(onClick = { showTakeoffDialog = true }, colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq)) {
                        Text("TAKE OFF", color = Color.White)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().background(AppTheme.colors.surface)) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth().background(AppTheme.colors.bg).padding(horizontal = 4.dp, vertical = 6.dp)) {
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
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = AppTheme.colors.muted)
                                        }
                                        IconButton(onClick = {
                                            val newList = paths.toMutableList()
                                            val p = newList[pIdx]
                                            val pts = p.points.toMutableList()
                                            pts.removeAt(ptIdx)
                                            if (pts.isEmpty()) newList.removeAt(pIdx) else newList[pIdx] = p.copy(points = pts)
                                            onUpdatePaths(newList)
                                        }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = AppTheme.colors.danger)
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

    if (showTakeoffDialog) {
        val takeoffPaths = paths.filter { it.isTakeoff }
        val netArea = takeoffPaths.sumOf { path ->
            CoordinateEngine.calculateNetArea(path.points.map { CoordinateEngine.Point(it.n, it.e) }, path.arcHeights)
        }
        val currentPerim = paths.lastOrNull()?.let { path ->
            var p = 0.0
            for (i in 0 until path.points.size - 1) {
                p += CoordinateEngine.dist(CoordinateEngine.Point(path.points[i].n, path.points[i].e), CoordinateEngine.Point(path.points[i+1].n, path.points[i+1].e))
            }
            p
        } ?: 0.0

        AlertDialog(
            onDismissRequest = { showTakeoffDialog = false },
            title = { Text("Building Plot Takeoff", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Summary Results:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Current Path Perimeter: ${FractionUtils.formatInches(currentPerim)}")
                    Text("Total Net Area: ${(netArea / 144.0).roundToOneDecimal()} sq ft", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(onClick = { showTakeoffDialog = false }) { Text("CLOSE") }
            }
        )
    }
}

@Composable
fun JobNotesScreen(
    savedPaths: List<SerializablePath>,
    onUpdateNotes: (List<SerializablePath>) -> Unit,
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
    
    Column(modifier = Modifier.fillMaxSize().background(Palette.Bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Palette.Bg).padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Palette.Navy, modifier = Modifier.size(30.dp)) }
            Text("Job Site Notes", color = Palette.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        
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
        
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { /* Export? */ }) {
                Text("SAVE SKETCH")
            }
            TextButton(
                onClick = { 
                    paths.clear()
                    onUpdateNotes(emptyList()) 
                }, 
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
            ) {
                Text("CLEAR")
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
    onFocus: (String, (String) -> Unit) -> Unit
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

    fun endTypeLabel(t: Int) = when (t) { 1 -> "Hip"; 2 -> "Valley"; else -> "Gable" }

    ToolScreen(Screen.RoofCalculator, onBack) {

        Column(modifier = Modifier.padding(16.dp)) {
            Text("BUILDING & PITCH", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
            Text("ROOF ENDS", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Left End", fontSize = 11.sp, color = AppTheme.colors.muted)
                    Row {
                        listOf(0, 1, 2).forEach { t ->
                            Button(
                                onClick = { leftEndType = t },
                                modifier = Modifier.weight(1f).height(44.dp).padding(end = if (t < 2) 2.dp else 0.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (leftEndType == t) AppTheme.colors.fav else AppTheme.colors.fn,
                                    contentColor = if (leftEndType == t) AppTheme.colors.favInk else AppTheme.colors.fnInk
                                )
                            ) { Text(endTypeLabel(t), fontSize = 11.sp) }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Right End", fontSize = 11.sp, color = AppTheme.colors.muted)
                    Row {
                        listOf(0, 1, 2).forEach { t ->
                            Button(
                                onClick = { rightEndType = t },
                                modifier = Modifier.weight(1f).height(44.dp).padding(end = if (t < 2) 2.dp else 0.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (rightEndType == t) AppTheme.colors.fav else AppTheme.colors.fn,
                                    contentColor = if (rightEndType == t) AppTheme.colors.favInk else AppTheme.colors.fnInk
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
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)
                ) {
                    Text(if (editingIndex != -1) "UPDATE & SAVE" else "CALCULATE TAKEOFF", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()); results = null },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppTheme.colors.danger),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.danger)
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            results?.let { res ->
                Spacer(modifier = Modifier.height(24.dp))
                Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("COMMON RAFTERS", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 12.sp)
                        Text("${res.commonCount}${if (bothSides) " (Mirror)" else ""} @ ${FractionUtils.formatInches(res.overallLen)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
                        Text("Run: ${FractionUtils.formatInches(res.run)} | Rise: ${FractionUtils.formatInches(res.rise)} | Jack Diff: ${FractionUtils.formatInches(res.commonDiff)}", fontSize = 12.sp, color = AppTheme.colors.muted)

                        if (res.totalHips > 0 || res.totalValleys > 0) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text("HIP / VALLEY MEMBERS", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 12.sp)
                            if (res.totalHips > 0) Text("Hips: ${res.totalHips} pcs @ ${FractionUtils.formatInches(res.hipLen)}", fontSize = 14.sp)
                            if (res.totalValleys > 0) Text("Valleys: ${res.totalValleys} pcs @ ${FractionUtils.formatInches(res.hipLen)}", fontSize = 14.sp)
                        }

                        if (!isTruss) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text("RIDGE", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 12.sp)
                            Text(FractionUtils.formatInches(res.correctedRidgeLen), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Sub-Fascia", fontSize = 11.sp, color = AppTheme.colors.muted)
                                Text(FractionUtils.formatInches(res.totalSubFasciaLF), fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Sheathing", fontSize = 11.sp, color = AppTheme.colors.muted)
                                Text("${res.sheetCount} Sheets", fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Area", fontSize = 11.sp, color = AppTheme.colors.muted)
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
                Text("SAVED TAKE-OFFS", color = AppTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Span: ${entry.span} | Len: ${entry.bldgLen} | ${endTypeLabel(entry.leftEnd)}/${endTypeLabel(entry.rightEnd)}",
                                        fontSize = 12.sp,
                                        color = AppTheme.colors.muted
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
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTheme.colors.muted) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppTheme.colors.muted) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("Common: ${entry.commonInfo}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
                            if (entry.hipInfo != "-") Text("Hip: ${entry.hipInfo}", fontSize = 12.sp)
                            if (entry.valleyInfo != "-") Text("Valley: ${entry.valleyInfo}", fontSize = 12.sp)
                            if (entry.ridgeInfo != "-") Text("Ridge: ${entry.ridgeInfo}", fontSize = 12.sp)
                            Text("Sub-Fascia: ${entry.subFascia} | Sheathing: ${entry.sheets} sheets", fontSize = 12.sp, color = AppTheme.colors.muted)
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

    ToolScreen(Screen.StairCalculator, onBack) {

        Column(modifier = Modifier.padding(16.dp)) {
            Text("STAIR DIMENSIONS", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                        backgroundColor = if (!limitedMode) AppTheme.colors.fav else AppTheme.colors.fn,
                        contentColor = if (!limitedMode) AppTheme.colors.favInk else AppTheme.colors.fnInk
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
                        backgroundColor = if (limitedMode) AppTheme.colors.fav else AppTheme.colors.fn,
                        contentColor = if (limitedMode) AppTheme.colors.favInk else AppTheme.colors.fnInk
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
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)
                ) {
                    Text(if (editingIndex != -1) "UPDATE & SAVE" else "CALCULATE & SAVE", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()); results = null },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppTheme.colors.danger),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.danger)
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            results?.let { res ->
                Spacer(modifier = Modifier.height(24.dp))
                Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("RISERS & TREADS", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 12.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${res.riserCount} Risers @", fontSize = 16.sp)
                            Text(FractionUtils.formatInches(res.actualRise), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = AppTheme.colors.accent)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${res.treadCount} Treads @", fontSize = 16.sp)
                            Text(FractionUtils.formatInches(res.totalRun / res.treadCount.coerceAtLeast(1)), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = AppTheme.colors.accent)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("STRINGER & LAYOUT", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 12.sp)
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
                        Text("Angle: ${res.angleDeg.roundToOneDecimal()}°", fontSize = 14.sp, color = AppTheme.colors.muted)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Distance out for Headroom:", fontSize = 12.sp, color = AppTheme.colors.muted)
                        Text(FractionUtils.formatInches(res.distOut), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
                    }
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED STAIRS", color = AppTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("Rise: ${entry.rise} | ${entry.riserCount} Risers", fontSize = 12.sp, color = AppTheme.colors.muted)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    totalRise = entry.rise
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTheme.colors.muted) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppTheme.colors.muted) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("ACT RISE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
                                    Text(entry.actualRise, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("STRINGER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
                                    Text(entry.stringerLen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("ANGLE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
                                    Text(entry.angleDeg, fontSize = 14.sp)
                                }
                            }
                            Text("Total Run: ${entry.totalRun} | Dist Out: ${entry.distOut}", fontSize = 12.sp, color = AppTheme.colors.muted, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

fun Double.roundToOneDecimal(): Double = (this * 10).roundToInt() / 10.0
fun pitchToDouble(input: String, default: Double): Double = input.toDoubleOrNull() ?: FractionUtils.parse(input).takeIf { it > 0 } ?: default

@Composable
fun TableCell(text: String, width: androidx.compose.ui.unit.Dp, isHeader: Boolean = false, isBold: Boolean = false) {
    Text(
        text = text,
        modifier = Modifier.width(width).padding(horizontal = 4.dp),
        color = if (isHeader) AppTheme.colors.favInk else AppTheme.colors.fnInk,
        fontWeight = if (isHeader || isBold) FontWeight.Bold else FontWeight.Normal,
        fontSize = 13.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
fun SummaryTable(title: String, headers: List<String>, content: @Composable ColumnScope.() -> Unit) {
    Text(title, color = Palette.Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
    onBack: () -> Unit
) {
    val pdfExportProvider = rememberPdfExportProvider()

    Column(modifier = Modifier.fillMaxSize().background(Palette.Bg).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Palette.Bg).padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Palette.Navy, modifier = Modifier.size(30.dp)) }
            Text("Data Sheet: $projectName", color = Palette.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            
            val downloader = rememberFileDownloader()

            IconButton(onClick = {
                val sections = SummaryReportGenerator.generateSections(
                    stairEntries, circularStairEntries, rafterEntries, gazeboEntries,
                    arcEntries, wallEntries, crownEntries, concreteEntries,
                    masonryEntries, drywallEntries, trigEntries, columnEntries,
                    roofEntries, floorEntries, deckEntries, handrailEntries,
                    framingEntries, pineLineEntries, coordinatePaths
                )
                pdfExportProvider.share(projectName, sections)
            }) {
                Icon(Icons.Default.Share, contentDescription = "Export PDF", tint = Palette.Navy)
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
                Text("DXF", color = Palette.Navy, fontWeight = FontWeight.Bold)
            }

            TextButton(onClick = {
                val csvContent = buildString {
                    append("Section,Name,Quantity,Dimensions,Primary Result,Secondary Result\n")
                    stairEntries.forEach { append("Stairs,${it.name},${it.riserCount} risers,Rise: ${it.rise},Act Rise: ${it.actualRise},Stringer: ${it.stringerLen}\n") }
                    rafterEntries.forEach { append("Rafters,${it.name},1,${if (it.isShed) "Shed run" else "Span"}: ${it.span} Pitch: ${it.pitch},Common: ${it.commonLen},Hip: ${it.hipLen}\n") }
                    concreteEntries.forEach { append("Concrete,${it.name},${it.qty},${it.dimensions},Volume: ${it.volume},Rebar: ${it.rebar}\n") }
                    wallEntries.forEach { append("Walls,${it.name},1,${it.length}x${it.height},Studs: ${it.studs},Plates: ${it.plates}\n") }
                    roofEntries.forEach { append("Roof,${it.name},1,${it.dimensions},Area: ${it.area},Squares: ${it.squares}\n") }
                    floorEntries.forEach { append("Flooring,${it.name},1,${it.length}x${it.width},Joists: ${it.joists},Sheets: ${it.sheets}\n") }
                }
                downloader.downloadFile("${projectName.replace(" ", "_")}_Takeoff.csv", csvContent, "text/csv")
            }) {
                Text("CSV", color = Palette.Navy, fontWeight = FontWeight.Bold)
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
                                    TableCell(entry.name, 120.dp, isBold = true); TableCell(if (entry.isShed) "${entry.span} shed" else entry.span, 80.dp)
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

                if (roofEntries.isNotEmpty()) {
                    SummaryTable("ROOF SCHEDULE", listOf("Name", "Area", "Squares", "Bundles", "Sheets", "Pitch")) {
                        roofEntries.forEach { entry ->
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                TableCell(entry.name, 120.dp, isBold = true); TableCell(entry.area, 100.dp)
                                TableCell(entry.squares, 100.dp); TableCell(entry.bundles, 100.dp)
                                TableCell(entry.sheets, 100.dp); TableCell(entry.dimensions, 80.dp)
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

                if (roofEntries.isNotEmpty()) {
                    SummaryTable("ROOF SCHEDULE", listOf("Name", "Area", "Squares", "Bundles", "Sheets", "Pitch")) {
                        roofEntries.forEach { entry ->
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                TableCell(entry.name, 120.dp, isBold = true); TableCell(entry.area, 100.dp)
                                TableCell(entry.squares, 80.dp); TableCell(entry.bundles, 80.dp)
                                TableCell(entry.sheets, 80.dp); TableCell(entry.dimensions, 120.dp)
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
                    Text("SITE SURVEY SCHEDULE", color = Palette.Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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

    ToolScreen(Screen.FramingCalculator, onBack) {
        Column(modifier = Modifier.padding(16.dp)) {
            
            // --- Wall Specifications ---
            Text("WALL SPECIFICATIONS", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
            Text("HEADER SPECIFICATIONS", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
            Text("STUD SPECIFICATIONS", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
                Text("OPENING DIMENSIONS", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
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
                        val wh = FractionUtils.parse(wallHeight)
                        val hhb = FractionUtils.parse(headerHeightBottom)
                        val hd = FractionUtils.parse(headerDepth)
                        val rw = FractionUtils.parse(roWidth)
                        val rh = if (isDoor) 0.0 else FractionUtils.parse(roHeight)
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
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)
                ) {
                    Text(if (editingIndex != -1) "UPDATE OPENING" else "ADD OPENING", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()); results = null },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppTheme.colors.danger),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.danger)
                ) {
                    Text("CLEAR", fontWeight = FontWeight.Bold)
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).background(AppTheme.colors.surface)) {
                    Column {
                        // Header
                        Row(modifier = Modifier.background(AppTheme.colors.fav).padding(vertical = 12.dp, horizontal = 4.dp)) {
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
                            Row(modifier = Modifier.background(AppTheme.colors.surface).padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
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
                                }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTheme.colors.muted) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(index)
                                    onUpdateEntries(newList)
                                }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppTheme.colors.muted) }
                            }
                            Divider()
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
        Text(value, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp, color = if (isBold) AppTheme.colors.accent else AppTheme.colors.ink)
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

    ToolScreen(Screen.WallCalculator, onBack) {

        Column(modifier = Modifier.padding(16.dp)) {
            Text("WALL DIMENSIONS", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)
                ) {
                    Text(if (editingIndex != -1) "UPDATE & SAVE" else "CALCULATE & SAVE", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()); results = null },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppTheme.colors.danger),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.danger)
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            results?.let { res ->
                Spacer(modifier = Modifier.height(24.dp))
                Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ESTIMATED MATERIALS", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Studs:", fontSize = 16.sp)
                            Text(res.studs, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = AppTheme.colors.accent)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Plate Footage:", fontSize = 16.sp)
                            Text(res.plates, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppTheme.colors.ink)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Blocking Footage:", fontSize = 16.sp)
                            Text(res.blocking, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppTheme.colors.ink)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sheathing (4x8):", fontSize = 16.sp)
                            Text(res.sheets, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppTheme.colors.ink)
                        }
                    }
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED WALLS", color = AppTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("Length: ${entry.length} | Height: ${entry.height} | ${entry.spacing}", fontSize = 12.sp, color = AppTheme.colors.muted)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    length = entry.length
                                    height = entry.height
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTheme.colors.muted) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppTheme.colors.muted) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("${entry.studs} Studs | ${entry.plates} Plates | ${entry.blocking} Block | ${entry.sheets} Sheets", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
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

    ToolScreen(Screen.CrownCalculator, onBack) {

        Column(modifier = Modifier.padding(16.dp)) {
            Text("MOLDING & CORNER ANGLES", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
            Text("Tip: Enter 135° corner OR 45° turn", fontSize = 10.sp, color = AppTheme.colors.muted, modifier = Modifier.padding(top = 4.dp))
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
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)
                ) {
                    Text(if (editingIndex != -1) "UPDATE & SAVE" else "CALCULATE & SAVE", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()); results = null },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppTheme.colors.danger),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.danger)
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            results?.let { res ->
                Spacer(modifier = Modifier.height(24.dp))
                Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("COMPOUND SAW SETTINGS", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Miter Angle:", fontSize = 14.sp)
                                Text(res.miter, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = AppTheme.colors.accent)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Bevel Angle:", fontSize = 14.sp)
                                Text(res.bevel, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = AppTheme.colors.accent)
                            }
                        }
                    }
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED CROWN SETTINGS", color = AppTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                entries.reversed().forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.name, fontWeight = FontWeight.Bold)
                            Text("Miter: ${entry.miter} | Bevel: ${entry.bevel}", fontSize = 12.sp, color = AppTheme.colors.muted)
                        }
                        IconButton(onClick = {
                            editingIndex = entries.size - 1 - index
                            sectionName = entry.name
                            springAngle = entry.spring.replace("°", "")
                            wallAngle = entry.wallAngle.replace("°", "")
                        }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTheme.colors.muted) }
                        IconButton(onClick = {
                            val newList = entries.toMutableList()
                            newList.removeAt(entries.size - 1 - index)
                            onUpdateEntries(newList)
                        }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppTheme.colors.muted) }
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

    ToolScreen(Screen.GazeboCalculator, onBack) {

        Column(modifier = Modifier.padding(16.dp)) {
            Text("POLYGON DIMENSIONS (INCIRCLE)", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                                commonPlumbSeat = "${atan(p/12.0)*(180.0/PI).roundToOneDecimal()}° / ${(90.0-atan(p/12.0)*(180.0/PI)).roundToOneDecimal()}°",
                                hipPlumbSeat = "${atan(res.hipPitch/12.0)*(180.0/PI).roundToOneDecimal()}° / ${(90.0-atan(res.hipPitch/12.0)*(180.0/PI)).roundToOneDecimal()}°",
                                ridgeOffset = "N/A"
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
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)
                ) {
                    Text(if (editingIndex != -1) "UPDATE & SAVE" else "CALCULATE & SAVE", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()); results = null },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppTheme.colors.danger),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.danger)
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            results?.let { res ->
                Spacer(modifier = Modifier.height(24.dp))
                
                // Diagram Mode Tabs
                Row(modifier = Modifier.fillMaxWidth().background(AppTheme.colors.surface).padding(4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
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

                Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("LAYOUT", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Side Length", fontSize = 11.sp, color = AppTheme.colors.muted)
                                Text(FractionUtils.formatInches(res.sideLen), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = AppTheme.colors.accent)
                            }
                            Column {
                                Text("Perimeter", fontSize = 11.sp, color = AppTheme.colors.muted)
                                Text(FractionUtils.formatInches(res.sideLen * (sides.toIntOrNull() ?: 8)), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Apothem (b)", fontSize = 11.sp, color = AppTheme.colors.muted)
                                Text(FractionUtils.formatInches(res.apothem), fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Radius (c)", fontSize = 11.sp, color = AppTheme.colors.muted)
                                Text(FractionUtils.formatInches(res.radius), fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Ridge Len", fontSize = 11.sp, color = AppTheme.colors.muted)
                                Text(FractionUtils.formatInches(res.ridgeLen), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ANGLES & CUTS", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Miter Angle:", fontSize = 11.sp, color = AppTheme.colors.muted)
                                Text("${(180.0 / (sides.toIntOrNull() ?: 8)).roundToOneDecimal()}°", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = AppTheme.colors.accent)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hip Pitch:", fontSize = 11.sp, color = AppTheme.colors.muted)
                                Text("${res.hipPitch.roundToOneDecimal()} / 12", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                // Common Rafter Card
                Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("COMMON RAFTER", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 12.sp)
                        Text(FractionUtils.formatInches(res.mainRafterLen), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
                        Text("Overall (inc Tail): ${FractionUtils.formatInches(res.overallMainLen)}", fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("HIP / CORNER RAFTER", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 12.sp)
                        Text(FractionUtils.formatInches(res.hipRafterLen), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
                        Text("Overall (inc Tail): ${FractionUtils.formatInches(res.overallHipLen)}", fontSize = 14.sp)
                        Text("Total Rise: ${FractionUtils.formatInches(res.verticalRise)}", fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED GAZEBOS", color = AppTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("${entry.sides} Sides | ${entry.diameter} Diam | ${entry.pitch}/12", fontSize = 12.sp, color = AppTheme.colors.muted)
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
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTheme.colors.muted) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppTheme.colors.muted) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("COMMON", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
                                    Text("Len: ${entry.commonRafterLen}", fontSize = 12.sp)
                                    Text("O/A: ${entry.commonOverall}", fontSize = 12.sp)
                                }
                                Column {
                                    Text("HIP / CORNER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
                                    Text("Len: ${entry.hipLen}", fontSize = 12.sp)
                                    Text("O/A: ${entry.hipOverall}", fontSize = 12.sp)
                                }
                            }
                            Text("Area: ${entry.area} | Perim: ${entry.perimeter}", fontSize = 12.sp, color = AppTheme.colors.muted, modifier = Modifier.padding(top = 4.dp))
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
        color = if (selected) AppTheme.colors.accent else AppTheme.colors.muted,
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

    ToolScreen(Screen.ArcCalculator, onBack) {

        Column(modifier = Modifier.padding(16.dp)) {
            Text("ARC DIMENSIONS", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                        val c = FractionUtils.parseFeet(chord)
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
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)
                ) {
                    Text(if (editingIndex != -1) "UPDATE & SAVE" else "CALCULATE & SAVE", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()); results = null },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppTheme.colors.danger),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.danger)
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            results?.let { res ->
                Spacer(modifier = Modifier.height(24.dp))
                Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("RADIUS", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 12.sp)
                        Text(res.radius, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ARC LENGTH", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 12.sp)
                                Text(res.arcLength, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.ink)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ARC ANGLE", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 12.sp)
                                Text(res.arcAngle, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.ink)
                            }
                        }
                    }
                }
            }
            
            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED ARCS", color = AppTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("Chord: ${entry.chord} | Height: ${entry.height}", fontSize = 12.sp, color = AppTheme.colors.muted)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    chord = entry.chord
                                    height = entry.height
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTheme.colors.muted) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppTheme.colors.muted) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("Radius: ${entry.radius}", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
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
fun RafterCalculator(
    entries: List<RafterEntry>,
    onUpdateEntries: (List<RafterEntry>) -> Unit,
    onBack: () -> Unit,
    onFocus: (String, (String) -> Unit) -> Unit
) {
    var pitch by remember { mutableStateOf("6") }
    var span by remember { mutableStateOf("") }
    var isShed by remember { mutableStateOf(false) }
    var ridgeThick by remember { mutableStateOf("1.5") }
    var rafterDepth by remember { mutableStateOf("7.25") }
    var heelHeight by remember { mutableStateOf("4.0") }
    var overhang by remember { mutableStateOf("12") }
    var sectionName by remember { mutableStateOf("") }

    var results by remember { mutableStateOf<RafterEngine.RafterResult?>(null) }
    var editingIndex by remember { mutableStateOf(-1) }

    ToolScreen(Screen.RafterCalculator, onBack) {

        Column(modifier = Modifier.padding(16.dp)) {
            Text("ROOF DIMENSIONS", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // Gable: enter the building span. Shed: enter the run; the ledger/beam is taken off it.
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                listOf(false to "Gable (span)", true to "Shed (run + ledger)").forEach { (shed, label) ->
                    Row(
                        modifier = Modifier.weight(1f).clickable { isShed = shed; results = null },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isShed == shed,
                            onClick = { isShed = shed; results = null },
                            colors = RadioButtonDefaults.colors(selectedColor = AppTheme.colors.accent)
                        )
                        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

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
                    label = if (isShed) "Rafter Run (ft/in)" else "Total Span (ft/in)",
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
                // Shed: the same box is the ledger/beam at the high end (taken off the run in full)
                ConstructionTextField(
                    value = ridgeThick,
                    onValueChange = { ridgeThick = it },
                    label = if (isShed) "Ledger / Beam (in)" else "Ridge (in)",
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
                                overhang = oh,
                                isShed = isShed
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
                                // A shed roof has no hips, valleys or jacks
                                hipLen = if (isShed) "-" else FractionUtils.formatInches(res.hipLen),
                                hipOverall = if (isShed) "-" else FractionUtils.formatInches(res.hipOverall),
                                hipRun = if (isShed) "-" else FractionUtils.formatInches(res.hipRun),
                                hipRise = if (isShed) "-" else FractionUtils.formatInches(res.hipRise),
                                hipOARise = if (isShed) "-" else FractionUtils.formatInches(res.hipOARise),
                                hipPitch = if (isShed) "-" else "${res.hipPitch.roundToOneDecimal()} / 12",
                                hipPlumbSeat = if (isShed) "-" else "${hipAngle.roundToOneDecimal()}° / ${(90.0 - hipAngle).roundToOneDecimal()}°",
                                jack16 = if (isShed) "-" else FractionUtils.formatInches(16.0 * factor),
                                jack24 = if (isShed) "-" else FractionUtils.formatInches(24.0 * factor),
                                isShed = isShed
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
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)
                ) {
                    Text(if (editingIndex != -1) "UPDATE & SAVE" else "CALCULATE & SAVE", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()); results = null },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppTheme.colors.danger),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.danger)
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            results?.let { res ->
                Spacer(modifier = Modifier.height(24.dp))
                
                // Common Rafter Detailed Card
                Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("COMMON RAFTER", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 12.sp)
                        
                        RafterDetail(
                            run = res.run,
                            rise = res.rise,
                            pitch = res.pitch,
                            heel = res.heel,
                            lumberDepth = FractionUtils.parse(rafterDepth),
                            overhang = FractionUtils.parse(overhang)
                        )
                        
                        Text(FractionUtils.formatInches(res.rafterLen), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
                        Text("Overall (inc Tail): ${FractionUtils.formatInches(res.overall)}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Run", fontSize = 11.sp, color = AppTheme.colors.muted)
                                Text(FractionUtils.formatInches(res.run), fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Rise", fontSize = 11.sp, color = AppTheme.colors.muted)
                                Text(FractionUtils.formatInches(res.rise), fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("O/A Rise", fontSize = 11.sp, color = AppTheme.colors.muted)
                                Text(FractionUtils.formatInches(res.totRise), fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Heel (HAP)", fontSize = 11.sp, color = AppTheme.colors.muted)
                                Text(FractionUtils.formatInches(res.heel), fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Pitch", fontSize = 11.sp, color = AppTheme.colors.muted)
                                Text("${res.pitch} / 12", fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Plumb / Seat", fontSize = 11.sp, color = AppTheme.colors.muted)
                                Text("${res.angleDeg.roundToOneDecimal()}° / ${(90.0 - res.angleDeg).roundToOneDecimal()}°", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Hip/valley and jack rafter cards: gable only (a shed roof has neither)
                if (!isShed) {
                // Hip Rafter Detailed Card
                Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("HIP / VALLEY RAFTER", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 12.sp)
                        Text(FractionUtils.formatInches(res.hipLen), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
                        Text("Overall (inc Tail): ${FractionUtils.formatInches(res.hipOverall)}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Hip Run", fontSize = 11.sp, color = AppTheme.colors.muted)
                                Text(FractionUtils.formatInches(res.hipRun), fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Hip Rise", fontSize = 11.sp, color = AppTheme.colors.muted)
                                Text(FractionUtils.formatInches(res.hipRise), fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("O/A Rise", fontSize = 11.sp, color = AppTheme.colors.muted)
                                Text(FractionUtils.formatInches(res.hipOARise), fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Hip Pitch", fontSize = 11.sp, color = AppTheme.colors.muted)
                                Text("${res.hipPitch.roundToOneDecimal()} / 12", fontWeight = FontWeight.Bold)
                            }
                            val hAngle = atan(res.hipPitch / 12.0) * (180.0 / PI)
                            Column {
                                Text("Plumb / Seat", fontSize = 11.sp, color = AppTheme.colors.muted)
                                Text("${hAngle.roundToOneDecimal()}° / ${(90.0 - hAngle).roundToOneDecimal()}°", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("JACK RAFTER DIFFERENCE", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 12.sp)
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
            }
            
            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED RAFTERS", color = AppTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        (if (entry.isShed) "Shed run: ${entry.span} | Ledger: ${entry.ridge} | " else "") + "Pitch: ${entry.pitch} / 12 | Heel: ${entry.heel}",
                                        fontSize = 12.sp, color = AppTheme.colors.muted
                                    )
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    pitch = entry.pitch
                                    span = entry.span
                                    isShed = entry.isShed
                                    ridgeThick = entry.ridge
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTheme.colors.muted) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppTheme.colors.muted) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("COMMON", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
                                    Text("Len: ${entry.commonLen}", fontSize = 12.sp)
                                    Text("O/A: ${entry.commonOverall}", fontSize = 12.sp)
                                }
                                if (!entry.isShed) {
                                    Column {
                                        Text("HIP / VAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
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

    ToolScreen(Screen.HandrailCalculator, onBack) {

        Column(modifier = Modifier.padding(16.dp)) {
            Text("RAIL DIMENSIONS", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)
                ) {
                    Text(if (editingIndex != -1) "UPDATE & SAVE" else "CALCULATE & SAVE", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()) },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppTheme.colors.danger),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.danger)
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED RAILS", color = AppTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("Length: ${entry.length}", fontSize = 12.sp, color = AppTheme.colors.muted)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    length = entry.length
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTheme.colors.muted) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppTheme.colors.muted) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(entry.spindles, fontSize = 14.sp)
                            Text(entry.spacing, fontSize = 14.sp)
                            if (entry.gap.isNotEmpty()) Text("Gap: ${entry.gap}", fontSize = 12.sp, color = AppTheme.colors.muted)
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
    
    var editingIndex by remember { mutableStateOf(-1) }

    ToolScreen(Screen.PineLineCalculator, onBack) {

        Column(modifier = Modifier.padding(16.dp)) {
            Text("ROOF & WALL SPECS", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                            val pineLineHt = PineLineEngine.calculate(p, h, w, b, f, oh, fa, rv)

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
                                drop = FractionUtils.formatInches(pineLineHt)
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
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)
                ) {
                    Text(if (editingIndex != -1) "UPDATE & SAVE" else "CALCULATE & SAVE", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()) },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppTheme.colors.danger),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.danger)
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED PINE LINES", color = AppTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("Pitch: ${entry.pitch}", fontSize = 12.sp, color = AppTheme.colors.muted)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTheme.colors.muted) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppTheme.colors.muted) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("Vertical Drop from Wall Plate: ${entry.drop}", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
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
    var sectionName by remember { mutableStateOf("") }
    
    var editingIndex by remember { mutableStateOf(-1) }

    ToolScreen(Screen.DeckCalculator, onBack) {

        Column(modifier = Modifier.padding(16.dp)) {
            Text("DECK DIMENSIONS", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                        val lIn = FractionUtils.parse(length)
                        val wIn = FractionUtils.parse(width)
                        val bw = boardWidth.toDoubleOrNull() ?: 5.5
                        val g = gap.toDoubleOrNull() ?: 0.125
                        
                        if (lIn > 0 && wIn > 0) {
                            val boardCount = ceil(wIn / (bw + g)).toInt()
                            // Assuming 16" OC joists for fastener count
                            val joistCount = ceil(lIn / 16.0).toInt() + 1
                            val fasteners = boardCount * joistCount * 2

                            val entry = DeckEntry(
                                id = if (editingIndex != -1) entries[editingIndex].id else (entries.size + 1).toString(),
                                name = sectionName.ifEmpty { "Deck Section" },
                                length = length,
                                width = width,
                                boardWidth = boardWidth,
                                gap = gap,
                                boards = "$boardCount Boards (${FractionUtils.formatInches(lIn)})",
                                fasteners = "$fasteners Screws"
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
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)
                ) {
                    Text(if (editingIndex != -1) "UPDATE & SAVE" else "CALCULATE & SAVE", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()) },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppTheme.colors.danger),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.danger)
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED DECKS", color = AppTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("${entry.length} x ${entry.width}", fontSize = 12.sp, color = AppTheme.colors.muted)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    length = entry.length
                                    width = entry.width
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTheme.colors.muted) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppTheme.colors.muted) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(entry.boards, fontSize = 14.sp)
                            Text(entry.fasteners, fontSize = 14.sp)
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

    ToolScreen(Screen.FloorCalculator, onBack) {

        Column(modifier = Modifier.padding(16.dp)) {
            Text("FLOOR DIMENSIONS & FRAMING SPECIFICATIONS", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                        backgroundColor = if (direction == "Span Length") AppTheme.colors.fav else AppTheme.colors.fn,
                        contentColor = if (direction == "Span Length") AppTheme.colors.favInk else AppTheme.colors.fnInk
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Span Length", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { direction = "Span Width" },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (direction == "Span Width") AppTheme.colors.fav else AppTheme.colors.fn,
                        contentColor = if (direction == "Span Width") AppTheme.colors.favInk else AppTheme.colors.fnInk
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
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)
                ) {
                    Text(if (editingIndex != -1) "UPDATE TAKEOFF" else "CALCULATE & SAVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()) },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppTheme.colors.danger),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.danger)
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }

            results?.let { res ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth(), elevation = 4.dp, shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("FLOOR FRAMING RESULTS", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 16.sp)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Joists Required:", fontWeight = FontWeight.SemiBold)
                            Text("${res.joistCount} Joists", fontWeight = FontWeight.Bold, color = AppTheme.colors.success)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cut Length / Joist:", fontWeight = FontWeight.SemiBold)
                            Text(FractionUtils.formatInches(res.joistCutLenIn), fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subfloor 4x8 Sheets:", fontWeight = FontWeight.SemiBold)
                            Text("${res.sheetCount} Sheets ($plyThick)", fontWeight = FontWeight.Bold, color = AppTheme.colors.danger)
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
                Text("SAVED FLOORS", color = AppTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("${entry.length} x ${entry.width}", fontSize = 12.sp, color = AppTheme.colors.muted)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    length = entry.length
                                    width = entry.width
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTheme.colors.muted) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppTheme.colors.muted) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(entry.joists, fontSize = 14.sp)
                            Text(entry.sheets, fontSize = 14.sp)
                            if (entry.rimJoist.isNotEmpty()) Text("Rim Joist: ${entry.rimJoist}", fontSize = 12.sp, color = AppTheme.colors.muted)
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

    ToolScreen(Screen.MasonryCalculator, onBack) {

        Column(modifier = Modifier.padding(16.dp)) {
            Text("ADD WALL SECTION", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                                sqFt = "${areaSqFt.toFixed(2)} sq ft",
                                type = if (isBlock) "Block" else "Brick",
                                units = totalUnits.toFixed(0),
                                mortar = mortarBags.toFixed(0),
                                sand = sandYards.toFixed(2),
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
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)
                ) {
                    Text(if (editingIndex != -1) "UPDATE ITEM" else "ADD TO LIST", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()) },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppTheme.colors.danger),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.danger)
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
                Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.fillMaxWidth(), backgroundColor = AppTheme.colors.fav) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Grand Total Units: ${grandUnits.toFixed(0)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "Area: ${grandSqFt.toFixed(2)} sq ft | Mortar: ${grandMortar.toFixed(0)} bags | Sand: ${grandSand.toFixed(2)} yd",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED MASONRY", color = AppTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Qty ${entry.qty} | ${entry.length} x ${entry.height} | ${entry.sqFt}",
                                        fontSize = 12.sp,
                                        color = AppTheme.colors.muted
                                    )
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    qty = entry.qty
                                    lengthInput = entry.length
                                    heightInput = entry.height
                                    isBlock = entry.type == "Block"
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTheme.colors.muted) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppTheme.colors.muted) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                "${entry.type}: ${entry.units} units | Mortar: ${entry.mortar} bags | Sand: ${entry.sand} yd",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.accent
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
    var areaInput by remember { mutableStateOf("") }
    var sheetSize by remember { mutableStateOf("4x8") }
    var sectionName by remember { mutableStateOf("") }
    
    var editingIndex by remember { mutableStateOf(-1) }

    ToolScreen(Screen.DrywallCalculator, onBack) {

        Column(modifier = Modifier.padding(16.dp)) {
            Text("FINISH AREA & MATERIAL", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            ConstructionTextField(
                value = areaInput,
                onValueChange = { areaInput = it },
                label = "Total Area (sq ft)",
                modifier = Modifier.fillMaxWidth(),
                onFocus = onFocus
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Sheet Size:", modifier = Modifier.weight(1f))
                Row(modifier = Modifier.weight(2f), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("4x8", "4x10", "4x12").forEach { size ->
                        val selected = sheetSize == size
                        Button(
                            onClick = { sheetSize = size },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (selected) AppTheme.colors.fav else AppTheme.colors.fn,
                                contentColor = if (selected) AppTheme.colors.favInk else AppTheme.colors.fnInk
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
                        val area = areaInput.toDoubleOrNull() ?: 0.0
                        if (area > 0) {
                            val divisor = when(sheetSize) {
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
                                area = "$area sq ft",
                                sheets = "$sheets Sheets",
                                sheetSize = sheetSize,
                                mud = "$mud lbs Mud",
                                tape = "$tape ft Tape"
                            )

                            if (editingIndex != -1) {
                                val newList = entries.toMutableList()
                                newList[editingIndex] = entry
                                onUpdateEntries(newList)
                                editingIndex = -1
                            } else {
                                onUpdateEntries(entries + entry)
                            }
                            sectionName = ""; areaInput = ""
                        }
                    },
                    modifier = Modifier.weight(2f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)
                ) {
                    Text(if (editingIndex != -1) "UPDATE & SAVE" else "CALCULATE & SAVE", textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()) },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppTheme.colors.danger),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.danger)
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }


            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED DRYWALL", color = AppTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("Area: ${entry.area}", fontSize = 12.sp, color = AppTheme.colors.muted)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    areaInput = entry.area.replace(" sq ft", "")
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTheme.colors.muted) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppTheme.colors.muted) }
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
    onFocus: (String, (String) -> Unit) -> Unit
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
    
    var results by remember { mutableStateOf<ConcreteResult?>(null) }
    var editingIndex by remember { mutableStateOf(-1) }

    ToolScreen(Screen.ConcreteCalculator, onBack) {

        Column(modifier = Modifier.padding(16.dp)) {
            Text("SLAB & FOOTING DIMENSIONS", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
            Text("REBAR REINFORCEMENT", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { rebarMode = 0 },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (rebarMode == 0) AppTheme.colors.fav else AppTheme.colors.fn,
                        contentColor = if (rebarMode == 0) AppTheme.colors.favInk else AppTheme.colors.fnInk
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Rebar Grid (Slabs)", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { rebarMode = 1 },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (rebarMode == 1) AppTheme.colors.fav else AppTheme.colors.fn,
                        contentColor = if (rebarMode == 1) AppTheme.colors.favInk else AppTheme.colors.fnInk
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
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)
                ) {
                    Text(if (editingIndex != -1) "UPDATE TAKEOFF" else "CALCULATE & SAVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onUpdateEntries(emptyList()); results = null },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppTheme.colors.danger),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.danger)
                ) {
                    Text("CLEAR LIST", textAlign = TextAlign.Center)
                }
            }
            
            results?.let { res ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth(), elevation = 4.dp, shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ESTIMATED CONCRETE & REBAR", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 16.sp)
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
                            Text("${res.totalCuYd.roundToOneDecimal()} cu yd (${res.totalCuFt.roundToOneDecimal()} cu ft)", fontWeight = FontWeight.Bold, color = AppTheme.colors.success)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Rebar Total Footage:", fontWeight = FontWeight.SemiBold)
                            Text("${res.totalRebarFt.roundToOneDecimal()} lin ft", fontWeight = FontWeight.Bold, color = AppTheme.colors.danger)
                        }
                    }
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("SAVED CONCRETE SECTIONS", color = AppTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("Qty: ${entry.qty} | Dimens: ${entry.dimensions} | ${entry.spacing}", fontSize = 12.sp, color = AppTheme.colors.muted)
                                }
                                IconButton(onClick = {
                                    editingIndex = entries.size - 1 - index
                                    sectionName = entry.name
                                    val dims = entry.dimensions.split(" x ")
                                    if (dims.size == 3) {
                                        length = dims[0]
                                        width = dims[1]
                                        thick = dims[2].replace("\"", "")
                                    }
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTheme.colors.muted) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppTheme.colors.muted) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("Volume: ${entry.volume}", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
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

    ToolScreen(Screen.TrigCalculator, onBack) {

        Column(modifier = Modifier.padding(16.dp)) {
            Text("RIGHT TRIANGLE (ENTER ANY 2)", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                    val a = sideA.toDoubleOrNull(); val b = sideB.toDoubleOrNull()
                    val c = hypotenuse.toDoubleOrNull(); val ang = angleA.toDoubleOrNull()
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
                    onUpdateEntries(entries + entry); sectionName = ""
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("SOLVE & SAVE") }

            results?.let { res ->
                Spacer(modifier = Modifier.height(24.dp))
                Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("TRIANGLE RESULTS", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Hypotenuse: ${FractionUtils.formatInches(res.hypotenuse)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Angle A", fontSize = 11.sp, color = AppTheme.colors.muted)
                                Text("${res.angleA.roundToOneDecimal()}°", fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Angle B", fontSize = 11.sp, color = AppTheme.colors.muted)
                                Text("${res.angleB.roundToOneDecimal()}°", fontWeight = FontWeight.Bold)
                            }
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
    var sectionName by remember { mutableStateOf("") }

    var results by remember { mutableStateOf<ColumnEngine.ColumnResult?>(null) }

    ToolScreen(Screen.ColumnCalculator, onBack) {

        Column(modifier = Modifier.padding(16.dp)) {
            ConstructionTextField(value = diameter, onValueChange = { diameter = it }, label = "Diameter (ft/in)", modifier = Modifier.fillMaxWidth(), onFocus = onFocus)
            Spacer(modifier = Modifier.height(8.dp))
            ConstructionTextField(value = height, onValueChange = { height = it }, label = "Height / Length (ft/in)", modifier = Modifier.fillMaxWidth(), onFocus = onFocus)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = sectionName, onValueChange = { sectionName = it }, label = { Text("Section Name") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val d = FractionUtils.parse(diameter); val h = FractionUtils.parse(height)
                    if (d > 0) {
                        val res = ColumnEngine.calculate(d, h)
                        results = res
                        val entry = ColumnEntry(
                            id = (entries.size + 1).toString(),
                            name = sectionName.ifEmpty { "Column Section" },
                            diameter = diameter,
                            height = height,
                            volume = "${(res.volume / 46656.0).roundToOneDecimal()} cu yd",
                            surfaceArea = "${(res.surfaceArea / 144.0).roundToOneDecimal()} sq ft",
                            circumference = FractionUtils.formatInches(res.circumference)
                        )
                        onUpdateEntries(entries + entry); sectionName = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text("CALCULATE & SAVE") }

            results?.let { res ->
                Spacer(modifier = Modifier.height(24.dp))
                Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("CIRCLE / COLUMN RESULTS", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Volume: ${(res.volume / 46656.0).roundToOneDecimal()} cu yd", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
                        Text("Circumference: ${FractionUtils.formatInches(res.circumference)}", fontSize = 16.sp)
                        Text("Surface Area: ${(res.surfaceArea / 144.0).roundToOneDecimal()} sq ft", fontSize = 16.sp)
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

    ToolScreen(Screen.CircularStairCalculator, onBack) {

        Column(modifier = Modifier.padding(16.dp)) {
            Text("STAIR DIMENSIONS", color = AppTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.colors.eq, contentColor = AppTheme.colors.eqInk)
                ) { Text("CALCULATE & SAVE") }
            }

            results?.let { res ->
                Spacer(modifier = Modifier.height(24.dp))
                Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("SPIRAL LAYOUT", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Outside Radius: ${FractionUtils.formatInches(res.outsideRadius)}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Angle per Tread: ${res.anglePerTread.roundToOneDecimal()}°", fontSize = 14.sp)
                        Text("Actual Rise: ${FractionUtils.formatInches(res.actualRise)}", fontSize = 14.sp)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Arc per Tread (Outer / Inner):", fontSize = 12.sp, color = AppTheme.colors.muted)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Outer Arc", fontSize = 11.sp, color = AppTheme.colors.muted)
                                Text(FractionUtils.formatInches(res.outerArcTread), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Inner Arc", fontSize = 11.sp, color = AppTheme.colors.muted)
                                Text(FractionUtils.formatInches(res.innerArcTread), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Circle Portion (Outer / Inner):", fontSize = 12.sp, color = AppTheme.colors.muted)
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
                Text("SAVED SPIRAL STAIRS", color = AppTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                entries.reversed().forEachIndexed { index, entry ->
                    Card(elevation = 0.dp, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppTheme.colors.line), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, fontWeight = FontWeight.Bold)
                                    Text("Rise: ${entry.totalRise} | ${entry.numTreads} Treads | Circle: ${entry.circleSize}", fontSize = 12.sp, color = AppTheme.colors.muted)
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
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTheme.colors.muted) }
                                IconButton(onClick = {
                                    val newList = entries.toMutableList()
                                    newList.removeAt(entries.size - 1 - index)
                                    onUpdateEntries(newList)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppTheme.colors.muted) }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                "Outside Radius: ${entry.outsideRadius} | Angle/Tread: ${entry.anglePerTread}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.accent
                            )
                            Text("Outer Arc: ${entry.outerArcTread} | Inner Arc: ${entry.innerArcTread}", fontSize = 12.sp, color = AppTheme.colors.muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorHelpScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Palette.Bg).padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Palette.Navy, modifier = Modifier.size(30.dp)) }
            Text("Calculator Guide", color = Palette.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
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
            HelpText("• Switch between Scientific and Graphing with the calculator type button at the top")
            HelpText("• The variable 'x' is required for any plot to appear")
            HelpText("• Use 'AC' to clear the current equation")
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Palette.Bg).padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Palette.Navy, modifier = Modifier.size(30.dp)) }
            Text("About Pro Calc", color = Palette.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
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
            
            Spacer(modifier = Modifier.weight(1f))
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
