package com.djran.constructioncalculator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewQuilt
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.ChangeHistory
import androidx.compose.material.icons.outlined.Cottage
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.Deck
import androidx.compose.material.icons.outlined.Fence
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Foundation
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Roofing
import androidx.compose.material.icons.outlined.RoundedCorner
import androidx.compose.material.icons.outlined.Stairs
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.ui.graphics.vector.ImageVector

/** A construction tool as it appears in the tools panel and on a favorite key. */
data class ToolInfo(
    val screen: Screen,
    val name: String,
    val keyLabel: String,
    val group: String,
    val icon: ImageVector,
)

const val GROUP_CARPENTRY = "Carpentry"
const val GROUP_CONCRETE = "Concrete & Masonry"
const val GROUP_SITE = "Site, Finish & Math"

val ToolCatalog = listOf(
    ToolInfo(Screen.StairCalculator, "Stair Layout", "STAIR", GROUP_CARPENTRY, Icons.Outlined.Stairs),
    ToolInfo(Screen.RafterCalculator, "Rafter Cuts", "RAFTER", GROUP_CARPENTRY, Icons.Outlined.ChangeHistory),
    ToolInfo(Screen.RoofCalculator, "Roof Takeoff", "ROOF", GROUP_CARPENTRY, Icons.Outlined.Roofing),
    ToolInfo(Screen.FloorCalculator, "Floor / Joists", "FLOOR", GROUP_CARPENTRY, Icons.Outlined.ViewWeek),
    ToolInfo(Screen.FramingCalculator, "Wall Framing", "FRAMING", GROUP_CARPENTRY, Icons.Outlined.ViewColumn),
    ToolInfo(Screen.WallCalculator, "Wall Materials", "WALL", GROUP_CARPENTRY, Icons.Outlined.CropSquare),
    ToolInfo(Screen.HandrailCalculator, "Handrails", "HANDRAIL", GROUP_CARPENTRY, Icons.Outlined.Fence),
    ToolInfo(Screen.DeckCalculator, "Deck Boards", "DECK", GROUP_CARPENTRY, Icons.Outlined.Deck),
    ToolInfo(Screen.CircularStairCalculator, "Circular Stairs", "SPIRAL", GROUP_CARPENTRY, Icons.Outlined.Autorenew),
    ToolInfo(Screen.GazeboCalculator, "Gazebo", "GAZEBO", GROUP_CARPENTRY, Icons.Outlined.Cottage),
    ToolInfo(Screen.CrownCalculator, "Crown Molding", "CROWN", GROUP_CARPENTRY, Icons.Outlined.Waves),
    ToolInfo(Screen.ArcCalculator, "Arc / Radius", "ARC", GROUP_CARPENTRY, Icons.Outlined.RoundedCorner),
    ToolInfo(Screen.ConcreteCalculator, "Concrete / Rebar", "CONCRETE", GROUP_CONCRETE, Icons.Outlined.Foundation),
    ToolInfo(Screen.MasonryCalculator, "Masonry Supply", "MASONRY", GROUP_CONCRETE, Icons.AutoMirrored.Outlined.ViewQuilt),
    ToolInfo(Screen.ColumnCalculator, "Round Column", "COLUMN", GROUP_CONCRETE, Icons.Outlined.RadioButtonUnchecked),
    ToolInfo(Screen.PineLineCalculator, "Grade / Slope", "GRADE", GROUP_SITE, Icons.Outlined.Landscape),
    ToolInfo(Screen.CoordinateCalculator, "Site Coordinates", "SITE", GROUP_SITE, Icons.Outlined.MyLocation),
    ToolInfo(Screen.DrywallCalculator, "Drywall Finish", "DRYWALL", GROUP_SITE, Icons.Outlined.Layers),
    ToolInfo(Screen.TrigCalculator, "Trig Solver", "TRIG", GROUP_SITE, Icons.Outlined.Architecture),
    ToolInfo(Screen.LayoutMarks, "Layout Marks", "MARKS", GROUP_SITE, Icons.Outlined.FormatListNumbered),
)

fun toolFor(screen: Screen): ToolInfo? = ToolCatalog.firstOrNull { it.screen == screen }
