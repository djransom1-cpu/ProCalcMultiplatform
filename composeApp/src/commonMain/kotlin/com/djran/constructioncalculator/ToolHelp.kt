package com.djran.constructioncalculator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp

/** The same step-by-step help as each tool's ? button in the Android app (HelpManager). */
object ToolHelp {
    fun text(screen: Screen): String? = when (screen) {
        Screen.GazeboCalculator -> "A. Name Section if you want\n" +
                "B. Total Span in feet in inches\n" +
                "C. Ridge span is an important Number, to get rafters to fit on each side I recommend no less than 6 inches. The results are labeled for both the Gazebo layout and the Ridge, identified by the letter for each segment.\n" +
                "D. Enter number of sides preset at 8\n" +
                "E. Enter pitch of common rafter ( Hip is automatically figured)\n" +
                "F. Enter overhang in inches\n" +
                "G. Heel height is needed for overall ridge height\n" +
                "H. Enter spacing for layouts, Preset at 16 o/c"
        Screen.CoordinateCalculator -> "1. Log points using North/East deltas.\n2. Tap a closed path for area takeoff."
        Screen.StairCalculator -> "A. Enter O/A height in inches ( Figure for finished floor)\n" +
                "B. Floor thickness in inches ( When opening size is desired)\n" +
                "C. Tread width (includes Nosing)\n" +
                "D. Nosing standard 1” but it is changeable\n" +
                "E. Name the Section if Desired"
        Screen.CircularStairCalculator -> "A. Name Stairs\n" +
                "B. Total rise in inches ( Include Finish floor height)\n" +
                "C. Inner radius in Inches (the narrow side)\n" +
                "D. Width of the stair (length of treads) in Inches\n" +
                "E. Total number of treads, straight and curved. Risers are always treads + 1, so the step rise is Total Rise / (treads + 1)\n" +
                "F. If you have a section of straight tread before the start of curve enter here, it will automatically deduct from total number of treads in curve\n" +
                "G. Straight tread run if you have any, run not full tread width\n" +
                "H. Enter nosing 1” is a standard\n" +
                "I. Walk-line tread depth: the tread measured 12” out from the narrow side. Michigan code minimum is 10” (winder rule), 11” is better and needs no nosing. The app works out how much of the circle the stair needs from this, so you no longer enter 2/4 etc.\n" +
                "J. Optional: length and width of the space you have. The app checks the stair footprint fits either way around.\n\n" +
                "Results show the step rise, layout angles, the best tread count for a 7 1/2” rise, a code check (rise 6 1/4” to 8 1/4” per Michigan code, 10” walk-line tread, 6” min at the narrow end, 36” width, nosing, 9” straight treads, headroom if it wraps over itself) and a material list. Check the code your inspector has adopted."
        Screen.RoofCalculator -> "A. Name section if have multiple roofs to calculate\n" +
                "B. Span of building in feet and Inches\n" +
                "C. Building Length at wall of the section of roof you are calculating\n" +
                "D. Enter Roof Pitch\n" +
                "E. Enter Overhang in Inches\n" +
                "F. Rafter spacing O/C\n" +
                "G. Enter Rafter lumber size\n" +
                "H. Enter truss for Truss roof, if Roof is same on both sides check Equal both Sides and it will calulate Rafters and Sheathing for both sides\n" +
                "I. Tap the appropriate box for Gable, Hip or Valley"
        Screen.RafterCalculator -> "A. Enter total span in feet and inches ( App will figure run- Ridge thickness)\n" +
                "B. Enter Ridge thickness if necessary\n" +
                "C. Enter Roof pitch\n" +
                "D. Name Section\n" +
                "E. Change Rafter Thickness if needed\n" +
                "F. Enter overhang inches\n" +
                "G. Enter Heel height"
        Screen.PineLineCalculator -> "1. Anchors trim to a level reference line.\n2. Use results to set frieze board heights."
        Screen.ConcreteCalculator -> "A. Qty is for if you have more than one side equal length and size\n" +
                "B. Name if you want to do Salb or Footings and have multiple slabs and footings\n" +
                "C. Length can be of total perimeter or length of each side if footings are different sizes\n" +
                "D. Width in Feet\n" +
                "E. Height in iches\n" +
                "F. Rebar type Grid (Mat or Continuous top and bottom)\n" +
                "G. Enter on center for grid and how many mats\n" +
                "H. On Continuous enter how many bars top and bottom\n" +
                "I. Footing cross section will be produced to scale"
        Screen.ColumnCalculator -> "A. If you have multiple columns to figure enter qty\n" +
                "B. Name if multiple columns\n" +
                "C. Enter Diameter in Inches\n" +
                "D. Height in Feet\n" +
                "E. Rebar For vertical with ties or only verticals\n" +
                "F. Enter number of Verticals\n" +
                "G. Ties O/C"
        Screen.WallCalculator -> "This is to determine how many Plates, studs and Sheathing are needed for a wall section, when planning out your day. Or for takeoffs. Also any walls over 10’ and extra Plate is figured for blocking at every 8’ after.\n" +
                "A. Enter wall Height and length\n" +
                "B. O/C spacing"
        Screen.FramingCalculator -> "This will give you your Header length, Trimmers, sills and cripple sizes and based on O/C with give # of Cripples needed, Also openings over 60” extra Trimmer is figured automatically.\n" +
                "A. Enter wall height in inches if wall is different than\n" +
                "B. Stud height is automatically calculated\n" +
                "C. Enter Header Height in Inches\n" +
                "D. Header size (if you normally put a 2x on top and bottom of Header include this)\n" +
                "E. Wall type 2x4, 2x6, etc.\n" +
                "F. On O/C framing\n" +
                "G. You have options for a multiple window opening under same header or for a door opening (If Multiple window is used, extra inputs come up for space between windows and how many openings under same header)\n" +
                "H. Enter qty of windows, if you have many window opening the same size, enter here to save from entering multiple windows that are the same size\n" +
                "I. Window height in inches\n" +
                "J. You can name window opening to keep track of where they go\n" +
                "K. If you have special notes"
        Screen.CrownCalculator -> "1. Enter wall and spring angles.\n2. Provides miter and bevel saw settings."
        Screen.ArcCalculator -> "1. Enter chord and height.\n2. Solves for radius and arc length."
        Screen.DeckCalculator -> "A. Name Section if you wish\n" +
                "B. Length of Deck in Feet\n" +
                "C. Width of Deck in Feet\n" +
                "D. Board width in Inches\n" +
                "E. Gap spacing\n" +
                "F. Joist on Center\n" +
                "G. Which way joist are going"
        Screen.DrywallCalculator -> "A. Name Wall section\n" +
                "B. Length of Wall Height of wall\n" +
                "C. If you check 4 walls and ceiling it will calculate all 4 walls and the ceiling, another input is added for wall height"
        Screen.FloorCalculator -> "A. Name floor section if you wish\n" +
                "B. Length of Floor in Feet\n" +
                "C. Width of Floor in Feet\n" +
                "D. Size of joist\n" +
                "E. O/C spacing\n" +
                "F. If Floor section has beams for support, enter how many and it will determine joist length\n" +
                "G. Plywood thickness\n" +
                "H. Choose Which direction joist are going\n" +
                "I. Will produce a drawing to scale for Autocad"
        Screen.HandrailCalculator -> "A. Enter Rail length\n" +
                "B. Spindle width\n" +
                "C. Desired gap spacing standard 4”\n" +
                "D. Name section\n" +
                "E. Returns Rail length and Number of Spindles and Spacing"
        Screen.MasonryCalculator -> "A. If you have multiple walls the same length you can enter that here\n" +
                "B. Name wall section if you desire\n" +
                "C. Length of wall or if wall height is all the same enter total perimeter in feet\n" +
                "D. Wall height in feet\n" +
                "E. Choice of Block or standard brick size\n" +
                "F. Results in How many units, Bags of Mortar and Sand required"
        Screen.GradeSlope -> "1. Enter distance and fall.\n2. Provides exact slope and percentage."
        Screen.SquareCalculator -> "1. Enter building length and width.\n2. Use the diagonal for perfect squaring."
        Screen.TrigCalculator -> "1. Solve any side of a triangle using trig functions."
        else -> extra(screen)
    }

    private fun extra(screen: Screen): String? = when (screen) {
        Screen.StoryPole -> "A. Pick a preset or start a new pole\nB. Enter the known height or elevation you start from\nC. Add layers: + adds, - subtracts, ? is the one unknown to solve\nD. Feet-inches work everywhere (6' 9 1/2)\nE. Save to the Data Sheet for the project"
        Screen.SurveyCalc -> "A. Setup: station coordinates and the backsight\nB. BS Check and Check Shot compare what the gun reads with what it should\nC. Resection: 2 points direct, 3 to 5 points least squares\nD. DMS math adds and subtracts angles in degrees-minutes-seconds\nE. Save any result to the Data Sheet"
        Screen.LandSurvey -> "A. Acreage: square feet to acres\nB. Slope distance and vertical angle to horizontal and vertical distance\nC. Point to point: distance and bearing between two North/East points\nD. Save to Project keeps the solution on the Data Sheet"
        Screen.GableCut, Screen.GableStuds, Screen.LayoutMarks -> null
        else -> null
    }
}

@Composable
fun ToolHelpDialog(screen: Screen, onDismiss: () -> Unit) {
    val t = ToolHelp.text(screen) ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(toolFor(screen)?.name ?: "Help") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(t, fontSize = 15.sp) } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("GOT IT") } }
    )
}
