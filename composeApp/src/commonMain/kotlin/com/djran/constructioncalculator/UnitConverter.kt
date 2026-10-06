package com.djran.constructioncalculator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Unit conversion factors for the converter. New for the redesign and separate from
 * the construction math: each unit is a multiple of one base unit per category.
 */
object UnitConversions {
    enum class Category(val label: String, val sameIn: String) {
        Length("Length", "Same length in"),
        Area("Area", "Same area in"),
        Volume("Volume", "Same volume in"),
        Weight("Weight", "Same weight in"),
    }

    /** [perBase] is how many base units one of this unit holds. */
    data class Unit(val name: String, val symbol: String, val perBase: Double, val feetInches: Boolean = false)

    // Base units: inch, square foot, cubic foot, pound
    val units: Map<Category, List<Unit>> = mapOf(
        Category.Length to listOf(
            Unit("Feet & inches", "", 1.0, feetInches = true),
            Unit("Inches", "in", 1.0),
            Unit("Feet", "ft", 12.0),
            Unit("Yards", "yd", 36.0),
            Unit("Miles", "mi", 63_360.0),
            Unit("Millimetres", "mm", 1.0 / 25.4),
            Unit("Centimetres", "cm", 1.0 / 2.54),
            Unit("Metres", "m", 1.0 / 0.0254),
        ),
        Category.Area to listOf(
            Unit("Square inches", "sq in", 1.0 / 144.0),
            Unit("Square feet", "sq ft", 1.0),
            Unit("Square yards", "sq yd", 9.0),
            Unit("Acres", "ac", 43_560.0),
            Unit("Square metres", "sq m", 10.763_910_416_709_722),
            Unit("Hectares", "ha", 107_639.104_167_097_22),
        ),
        Category.Volume to listOf(
            Unit("Cubic inches", "cu in", 1.0 / 1_728.0),
            Unit("Cubic feet", "cu ft", 1.0),
            Unit("Cubic yards", "cu yd", 27.0),
            Unit("US gallons", "gal", 231.0 / 1_728.0),
            Unit("Litres", "L", 0.035_314_666_721_488_59),
            Unit("Cubic metres", "cu m", 35.314_666_721_488_59),
        ),
        Category.Weight to listOf(
            Unit("Ounces", "oz", 1.0 / 16.0),
            Unit("Pounds", "lb", 1.0),
            Unit("US tons", "ton", 2_000.0),
            Unit("Grams", "g", 0.002_204_622_621_848_776),
            Unit("Kilograms", "kg", 2.204_622_621_848_776),
            Unit("Metric tonnes", "t", 2_204.622_621_848_776),
        ),
    )

    fun convert(value: Double, from: Unit, to: Unit): Double = value * from.perBase / to.perBase

    /** Feet-and-inches values use the calculator's own ft-in format; others up to 4 decimals. */
    fun format(value: Double, unit: Unit): String {
        if (unit.feetInches) return DimensionValue(value, 1).toString()
        val rounded = (value * 10_000.0).roundToLong() / 10_000.0
        val text = if (abs(rounded) >= 1e9) rounded.toString()
        else {
            val whole = rounded.toLong()
            val frac = ((abs(rounded - whole)) * 10_000.0).roundToLong()
            if (frac == 0L) whole.toString()
            else {
                val sign = if (rounded < 0 && whole == 0L) "-" else ""
                sign + whole.toString() + "." + frac.toString().padStart(4, '0').trimEnd('0')
            }
        }
        return "$text ${unit.symbol}"
    }

    /** Reads what was typed: fractions like 3 1/2 work, and ft-in for the feet-and-inches unit. */
    fun parse(text: String): Double = FractionUtils.parse(text)
}

@Composable
fun UnitConverterScreen(
    projectName: String,
    onProjectClick: () -> Unit,
    cloudSync: CloudSync,
    onCloudClick: () -> Unit,
    onModeSelect: (CalcMode) -> Unit,
    onEditValue: (String, (String) -> Unit) -> Unit,
) {
    val c = LocalAppColors.current
    var categoryName by rememberSaveable { mutableStateOf(UnitConversions.Category.Length.name) }
    val category = UnitConversions.Category.valueOf(categoryName)
    val units = UnitConversions.units.getValue(category)
    var fromIndex by rememberSaveable(categoryName) { mutableStateOf(0) }
    var toIndex by rememberSaveable(categoryName) { mutableStateOf(units.lastIndex) }
    var typed by rememberSaveable(categoryName) { mutableStateOf(if (category == UnitConversions.Category.Length) "12' 6 3/4\"" else "1") }

    val from = units[fromIndex.coerceIn(units.indices)]
    val to = units[toIndex.coerceIn(units.indices)]
    val value = UnitConversions.parse(typed)

    Box(Modifier.fillMaxSize().background(c.bg), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxHeight().widthIn(max = CalcMaxWidth).fillMaxWidth()) {
            CalcTopBar(projectName, onProjectClick, cloudSync, onCloudClick)
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    ModePill(CalcMode.Converter, onModeSelect)
                }

                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UnitConversions.Category.entries.forEach { cat ->
                        val selected = cat == category
                        Box(
                            Modifier.height(38.dp).clip(RoundedCornerShape(19.dp))
                                .background(if (selected) c.fav else c.chip)
                                .then(if (selected) Modifier else Modifier.border(1.dp, c.line, RoundedCornerShape(19.dp)))
                                .clickable(role = Role.Tab) { categoryName = cat.name }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(cat.label, color = if (selected) c.favInk else c.fnInk, fontSize = 15.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold)
                        }
                    }
                }

                Box(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ValueCard(
                            label = "From", units = units, unitIndex = fromIndex, onUnitChange = { fromIndex = it },
                            text = typed.ifEmpty { "0" }, active = true,
                            onTextClick = { onEditValue(typed) { typed = it } },
                        )
                        ValueCard(
                            label = "To", units = units, unitIndex = toIndex, onUnitChange = { toIndex = it },
                            text = UnitConversions.format(UnitConversions.convert(value, from, to), to), active = false,
                            onTextClick = null,
                        )
                    }
                    Box(
                        Modifier.align(Alignment.Center).size(46.dp).clip(CircleShape).background(c.bg).padding(3.dp)
                            .clip(CircleShape).background(c.fav)
                            .clickable(role = Role.Button) {
                                val oldFrom = fromIndex
                                typed = UnitConversions.format(UnitConversions.convert(value, from, to), to)
                                    .removeSuffix(" ${to.symbol}").trim()
                                fromIndex = toIndex
                                toIndex = oldFrom
                            }
                            .semantics { contentDescription = "Swap units" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.SwapVert, contentDescription = null, tint = c.favInk, modifier = Modifier.size(22.dp))
                    }
                }

                val shape = RoundedCornerShape(16.dp)
                Column(Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.line, shape).padding(horizontal = 16.dp)) {
                    Text(
                        category.sameIn.uppercase(), color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                    units.filter { it != from && it != to }.forEachIndexed { i, unit ->
                        if (i > 0) Divider(color = c.line)
                        Row(Modifier.fillMaxWidth().height(46.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(unit.name, color = c.muted, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            Text(UnitConversions.format(UnitConversions.convert(value, from, unit), unit), color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Text("Tap the From value to type a new one.", color = c.muted, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ValueCard(
    label: String,
    units: List<UnitConversions.Unit>,
    unitIndex: Int,
    onUnitChange: (Int) -> Unit,
    text: String,
    active: Boolean,
    onTextClick: (() -> Unit)?,
) {
    val c = LocalAppColors.current
    val shape = RoundedCornerShape(16.dp)
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().clip(shape).background(c.surface)
            .border(if (active) 2.dp else 1.dp, if (active) c.fnInk else c.line, shape)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), color = c.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, modifier = Modifier.weight(1f))
            Box {
                Row(
                    Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(c.fn)
                        .clickable(role = Role.Button, onClickLabel = "Change $label unit") { open = true }
                        .padding(start = 12.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(units[unitIndex.coerceIn(units.indices)].name, color = c.fnInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = c.fnInk, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(c.menu)) {
                    units.forEachIndexed { i, unit ->
                        DropdownMenuItem(onClick = { open = false; onUnitChange(i) }) {
                            Text(unit.name, color = c.ink, fontSize = 16.sp, fontWeight = if (i == unitIndex) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
        Text(
            text, color = if (active) c.ink else c.opInk, fontSize = 38.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, softWrap = false, textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                .then(if (onTextClick != null) Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClickLabel = "Type a value", onClick = onTextClick) else Modifier),
        )
    }
}
