package com.djran.constructioncalculator

import com.djran.constructioncalculator.StoryPoleMath.Layer
import com.djran.constructioncalculator.StoryPoleMath.Mode
import com.djran.constructioncalculator.StoryPoleMath.Op
import com.djran.constructioncalculator.StoryPoleMath.Pole

/** Trade preset packs and built-in templates for Story Pole. Amounts are inches. */
object StoryPolePresets {

    data class Preset(val label: String, val amount: String)
    data class Group(val name: String, val presets: List<Preset>)
    data class Trade(val key: String, val name: String, val groups: List<Group>)

    private fun p(l: String, a: String) = Preset(l, a)

    val trades: List<Trade> = listOf(
        Trade("FRAMING", "Framing", listOf(
            Group("Plates & sills", listOf(p("Bottom plate", "1-1/2"), p("Double top plate", "3"), p("Single top plate", "1-1/2"),
                p("Rough sill (single)", "1-1/2"), p("Rough sill (double)", "3"), p("Sill gasket", "1/4"))),
            Group("Studs (precut)", listOf(p("Stud 92-5/8\" (8' wall)", "92-5/8"), p("Stud 104-5/8\" (9' wall)", "104-5/8"), p("Stud 116-5/8\" (10' wall)", "116-5/8"))),
            Group("Headers (2x actual)", listOf(p("Header 2x6", "5-1/2"), p("Header 2x8", "7-1/4"), p("Header 2x10", "9-1/4"), p("Header 2x12", "11-1/4"))),
            Group("Floor system", listOf(p("Subfloor 3/4\"", "3/4"), p("Subfloor 23/32\"", "23/32"), p("Subfloor 1-1/8\"", "1-1/8"),
                p("2x8 joist", "7-1/4"), p("2x10 joist", "9-1/4"), p("2x12 joist", "11-1/4"),
                p("I-joist 9-1/2\"", "9-1/2"), p("I-joist 11-7/8\"", "11-7/8"), p("I-joist 14\"", "14"), p("I-joist 16\"", "16"),
                p("LVL 9-1/4\"", "9-1/4"), p("LVL 11-7/8\"", "11-7/8"), p("LVL 14\"", "14"))),
            Group("Openings", listOf(p("Rough opening height", "82-1/2"), p("Bottom of RO above floor", "36")))
        )),
        Trade("CONCRETE", "Concrete", listOf(
            Group("Footings", listOf(p("Footing 8\"", "8"), p("Footing 10\"", "10"), p("Footing 12\"", "12"), p("Footing 16\"", "16"))),
            Group("Walls", listOf(p("Foundation wall 4'", "48"), p("Foundation wall 8'", "96"), p("Foundation wall 9'", "108"), p("Foundation wall 10'", "120"))),
            Group("Slabs & ledges", listOf(p("Slab 4\"", "4"), p("Slab 5\"", "5"), p("Slab 6\"", "6"), p("Brick ledge drop", "4"), p("Garage step-down", "4"))),
            Group("On top of wall", listOf(p("Sill plate", "1-1/2"), p("Sill gasket", "1/4"), p("Subfloor 3/4\"", "3/4"), p("2x10 joist", "9-1/4"), p("I-joist 11-7/8\"", "11-7/8")))
        )),
        Trade("SITE", "Site / Paving", listOf(
            Group("Curb & walk", listOf(p("Curb reveal (6\")", "6"), p("Curb section depth", "18"), p("Sidewalk 4\"", "4"), p("Sidewalk 6\"", "6"))),
            Group("Asphalt", listOf(p("Asphalt top course", "1-1/2"), p("Asphalt base course", "2-1/2"), p("Asphalt 3\"", "3"), p("Asphalt 4\"", "4"))),
            Group("Base & grade", listOf(p("Aggregate base 4\"", "4"), p("Aggregate base 6\"", "6"), p("Aggregate base 8\"", "8"), p("Sand sub-base 12\"", "12"), p("Topsoil 4\"", "4")))
        )),
        Trade("CABINETS", "Cabinets / Millwork", listOf(
            Group("Base", listOf(p("Finished floor", "3/4"), p("Toe kick", "4-1/2"), p("Base cabinet (w/ toe kick)", "34-1/2"), p("Countertop 1-1/2\"", "1-1/2"), p("Countertop 1-1/4\"", "1-1/4"))),
            Group("Uppers", listOf(p("Backsplash space", "18"), p("Upper cabinet 30\"", "30"), p("Upper cabinet 36\"", "36"), p("Upper cabinet 42\"", "42"), p("Light rail", "1-1/2"), p("Crown molding", "3"))),
            Group("Other", listOf(p("Shim / scribe", "1/4"), p("Filler strip", "3/4")))
        )),
        Trade("STEEL", "Steel / General", listOf(
            Group("W beams (AISC depth)", listOf(
                p("W6x9", "5.90"), p("W6x12", "6.03"), p("W8x10", "7.89"), p("W8x13", "7.99"), p("W8x15", "8.11"), p("W8x18", "8.14"),
                p("W8x21", "8.28"), p("W8x24", "7.93"), p("W8x31", "8.00"), p("W10x12", "9.87"), p("W10x15", "9.99"), p("W10x19", "10.24"),
                p("W10x22", "10.17"), p("W10x26", "10.33"), p("W10x30", "10.47"), p("W12x14", "11.91"), p("W12x16", "11.99"), p("W12x19", "12.16"),
                p("W12x22", "12.31"), p("W12x26", "12.22"), p("W14x22", "13.74"), p("W14x26", "13.91"), p("W14x30", "13.84"),
                p("W16x26", "15.69"), p("W16x31", "15.88"), p("W16x36", "15.86"), p("W16x40", "16.01"), p("W18x35", "17.70"), p("W18x40", "17.90"))),
            Group("Plates", listOf(p("Leveling plate", "1-1/2"), p("Bearing plate 3/4\"", "3/4"), p("Bearing plate 1\"", "1"), p("Nailer on beam", "1-1/2")))
        ))
    )

    fun trade(key: String) = trades.firstOrNull { it.key == key } ?: trades[0]

    data class Template(val description: String, val pole: Pole)

    private fun l(label: String, amt: String, op: Op = Op.SUBTRACT) = Layer(label, amt, op)

    /** Built-in templates. Each expected result is an acceptance test. */
    val builtIns: List<Template> = listOf(
        Template("Bench mark down through floor system & beam", Pole("Footing under steel beam", Mode.ELEVATION, "STEEL",
            "Bench mark (top of deck)", "612.73", "Top of footing", "",
            mutableListOf(l("Decking", "1"), l("Floor joist", "11-1/4"), l("2x plate", "1-1/2"), l("Steel beam W8x18", "8.14"), l("Leveling plate", "1-1/2")))),
        Template("RO height minus the bottom plate", Pole("Jack stud", Mode.LENGTH, "FRAMING",
            "Top of subfloor", "0", "Jack stud length", "",
            mutableListOf(l("Rough opening height", "82-1/2", Op.ADD), l("Bottom plate", "1-1/2")))),
        Template("Window sill height down to the bottom plate", Pole("Cripple under window", Mode.LENGTH, "FRAMING",
            "Top of subfloor", "0", "Lower cripple length", "",
            mutableListOf(l("Bottom of RO above floor", "36", Op.ADD), l("Rough sill (single)", "1-1/2"), l("Bottom plate", "1-1/2")))),
        Template("Wall height down to the top of the header", Pole("Cripple over header", Mode.LENGTH, "FRAMING",
            "Top of subfloor", "0", "Upper cripple length", "",
            mutableListOf(l("Wall height (to top of plates)", "97-1/8", Op.ADD), l("Double top plate", "3"), l("Rough opening height", "82-1/2"), l("Header 2x10", "9-1/4")))),
        Template("Finished floor to top of wall, then top of footing", Pole("Basement wall", Mode.ELEVATION, "CONCRETE",
            "First floor (top of subfloor)", "100.00", "Top of footing", "",
            mutableListOf(l("Subfloor", "3/4"), l("2x10 joist", "9-1/4"), l("Sill plate", "1-1/2"), l("Sill gasket", "1/4"), l("Foundation wall 8'", "96")))),
        Template("Top of curb down to top of base and subgrade", Pole("Curb base", Mode.ELEVATION, "SITE",
            "Top of curb (plan)", "100.50", "Subgrade",
            "", mutableListOf(l("Curb section depth", "18"), l("Aggregate base 6\"", "6")))),
        Template("Floor up through base, top and uppers", Pole("Kitchen heights", Mode.LENGTH, "CABINETS",
            "Finished floor", "0", "Top of upper cabinets", "",
            mutableListOf(l("Base cabinet (w/ toe kick)", "34-1/2", Op.ADD), l("Countertop 1-1/2\"", "1-1/2", Op.ADD), l("Backsplash space", "18", Op.ADD), l("Upper cabinet 30\"", "30", Op.ADD))))
    )

    fun blank() = Pole("New pole", Mode.LENGTH, "FRAMING", "Start", "0", "Result", "", mutableListOf(Layer()))
}
