package com.djran.constructioncalculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** What fills the space under the display: keys, the tools panel, or nothing (history is open). */
enum class CalcPanel { Keypad, Tools, History }

/** The calculator types in the switcher at the top of the display. */
enum class CalcMode(val label: String, val screen: Screen, val icon: ImageVector) {
    Construction("Construction", Screen.Calculator, Icons.Outlined.Straighten),
    Scientific("Scientific", Screen.ScientificCalculator, Icons.Outlined.Functions),
    Graphing("Graphing", Screen.GraphingCalculator, Icons.AutoMirrored.Outlined.ShowChart),
    Converter("Unit converter", Screen.UnitConverter, Icons.Outlined.SwapHoriz),
}

/** Widest the calculator gets on tablets and desktop browsers. */
val CalcMaxWidth = 520.dp

/** Spacing around the keypad's six key rows. Short screens (iPhone SE, laptop browsers) get the tight set. */
private class KeypadSpacing(val handle: Dp, val bottom: Dp, val gap: Dp) {
    /** Everything in the keypad area that isn't a key: tools handle, padding and the gaps between rows. */
    val chrome: Dp get() = handle + 4.dp + bottom + gap * 5
}
private val RegularSpacing = KeypadSpacing(handle = 48.dp, bottom = 14.dp, gap = 8.dp)
private val ShortSpacing = KeypadSpacing(handle = 40.dp, bottom = 8.dp, gap = 6.dp)

@Composable
fun CalculatorScreen(
    projectName: String,
    onProjectClick: () -> Unit,
    cloudSync: CloudSync,
    onCloudClick: () -> Unit,
    input: String,
    expression: String,
    isMetric: Boolean,
    panel: CalcPanel,
    onPanelChange: (CalcPanel) -> Unit,
    favorites: List<Screen>,
    editingFavorites: Boolean,
    onEditFavoritesChange: (Boolean) -> Unit,
    onToggleFavorite: (Screen) -> Unit,
    history: List<HistoryEntry>,
    historyOn: Boolean,
    onUseHistory: (HistoryEntry) -> Unit,
    onClearHistory: () -> Unit,
    onModeSelect: (CalcMode) -> Unit,
    onOpenTool: (Screen) -> Unit,
    onAction: (CalcAction) -> Unit,
) {
    val c = LocalAppColors.current
    Box(Modifier.fillMaxSize().background(c.bg), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxHeight().widthIn(max = CalcMaxWidth).fillMaxWidth()) {
            CalcTopBar(projectName, onProjectClick, cloudSync, onCloudClick)

            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                // Keys shrink on short screens so the whole display, FT-IN/M switch included,
                // always fits. The display needs about 200dp, or 184dp with the smaller result.
                val short = maxHeight < 580.dp
                val spacing = if (short) ShortSpacing else RegularSpacing
                val displayHeight = if (short) 184.dp else 202.dp
                val keyHeight = ((maxHeight - displayHeight - spacing.chrome) / 6).coerceIn(36.dp, 60.dp)
                Column(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxWidth()
                            .then(if (panel == CalcPanel.Tools) Modifier else Modifier.weight(1f))
                            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 10.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            RoundIconButton(
                                icon = Icons.Outlined.History,
                                label = if (panel == CalcPanel.History) "Close history" else "Calculation history",
                                background = if (panel == CalcPanel.History) c.fav else c.fn,
                                tint = if (panel == CalcPanel.History) c.favInk else c.fnInk,
                                onClick = { onPanelChange(if (panel == CalcPanel.History) CalcPanel.Keypad else CalcPanel.History) },
                            )
                            Spacer(Modifier.weight(1f))
                            ModePill(CalcMode.Construction, onModeSelect)
                        }
                        when (panel) {
                            CalcPanel.History -> HistoryList(
                                history = history,
                                historyOn = historyOn,
                                onUse = onUseHistory,
                                onClear = onClearHistory,
                                onClose = { onPanelChange(CalcPanel.Keypad) },
                                modifier = Modifier.weight(1f),
                            )
                            CalcPanel.Tools -> {
                                Spacer(Modifier.height(8.dp))
                                ExpressionLine(expression, 18)
                                ResultLine(input, compact = true)
                            }
                            CalcPanel.Keypad -> {
                                Spacer(Modifier.weight(1f))
                                ExpressionLine(expression, 20)
                                ResultLine(input, compact = false, maxSize = if (short) 44 else 56)
                                Spacer(Modifier.height(if (short) 6.dp else 10.dp))
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    UnitsToggle(isMetric, onToggle = { onAction(CalcAction.Metric) })
                                    Spacer(Modifier.weight(1f))
                                    ChipButton(Icons.Outlined.SwapHoriz, "Convert", onClick = { onAction(CalcAction.Convert) })
                                }
                            }
                        }
                    }

                    when (panel) {
                        CalcPanel.Keypad -> {
                            ToolsHandle(expanded = false, height = spacing.handle, onClick = { onPanelChange(CalcPanel.Tools) })
                            if (isMetric) {
                                MetricKeypad(keyHeight, spacing, favorites, onOpenTool, onAddFavorite = {
                                    onEditFavoritesChange(true); onPanelChange(CalcPanel.Tools)
                                }, onAction = onAction)
                            } else {
                                ImperialKeypad(keyHeight, spacing, favorites, onOpenTool, onAddFavorite = {
                                    onEditFavoritesChange(true); onPanelChange(CalcPanel.Tools)
                                }, onAction = onAction)
                            }
                        }
                        CalcPanel.Tools -> ToolsPanel(
                            favorites = favorites,
                            editing = editingFavorites,
                            onEditingChange = onEditFavoritesChange,
                            onToggleFavorite = onToggleFavorite,
                            onOpenTool = onOpenTool,
                            onCollapse = { onEditFavoritesChange(false); onPanelChange(CalcPanel.Keypad) },
                            modifier = Modifier.weight(1f),
                        )
                        CalcPanel.History -> Unit
                    }
                }
            }
        }
    }
}

// ---------- Top bar and mode switcher (shared with Scientific and Unit converter) ----------

@Composable
fun CalcTopBar(projectName: String, onProjectClick: () -> Unit, cloudSync: CloudSync, onCloudClick: () -> Unit) {
    val c = LocalAppColors.current
    // One line ("Project  Default"), so it never clips, even with a larger phone font size
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 6.dp, end = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f, fill = false).clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClickLabel = "Switch project", onClick = onProjectClick)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Folder, contentDescription = null, tint = c.ink, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Project", color = c.muted, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Spacer(Modifier.width(6.dp))
            Text(
                projectName, color = c.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
            )
            Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = c.ink, modifier = Modifier.size(20.dp))
        }
        if (cloudSyncSupported) {
            val (icon, label) = when {
                !cloudSync.isSignedIn -> Icons.Outlined.CloudOff to "Account and sync: signed out"
                cloudSync.isBusy -> Icons.Outlined.CloudSync to "Account and sync: syncing"
                else -> Icons.Outlined.CloudDone to "Account and sync: synced"
            }
            RoundIconButton(icon, label, Color.Transparent, c.muted, onCloudClick)
        }
    }
}

@Composable
fun ModePill(current: CalcMode, onSelect: (CalcMode) -> Unit) {
    val c = LocalAppColors.current
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.height(40.dp).clip(RoundedCornerShape(20.dp))
                .background(c.chip).border(1.dp, c.line, RoundedCornerShape(20.dp))
                .clickable(role = Role.Button, onClickLabel = "Switch calculator type") { open = true }
                .padding(start = 10.dp, end = 10.dp)
                .semantics { contentDescription = "Calculator type: ${current.label}" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(current.icon, contentDescription = null, tint = c.ink, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(current.label, color = c.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = c.ink, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.background(c.menu).widthIn(min = 230.dp),
        ) {
            CalcMode.entries.forEach { mode ->
                DropdownMenuItem(onClick = { open = false; if (mode != current) onSelect(mode) }) {
                    Icon(mode.icon, contentDescription = null, tint = c.ink, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        mode.label, color = c.ink, fontSize = 16.sp,
                        fontWeight = if (mode == current) FontWeight.SemiBold else FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    if (mode == current) {
                        Icon(Icons.Outlined.Check, contentDescription = "Selected", tint = c.ink, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ---------- Display pieces ----------

/** Shows ×, − and ÷ in place of the X, - and / the calculator stores. */
fun prettyExpression(text: String): String =
    text.split(" ").joinToString(" ") { token ->
        when (token) {
            "X", "*" -> "×"
            "-" -> "−"
            "/" -> "÷"
            else -> token
        }
    }

@Composable
private fun ExpressionLine(expression: String, sizeSp: Int) {
    val c = LocalAppColors.current
    Text(
        prettyExpression(expression),
        color = c.muted, fontSize = sizeSp.sp, maxLines = 1, softWrap = false,
        textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().height((sizeSp + 6).dp),
    )
}

@Composable
private fun ResultLine(input: String, compact: Boolean, maxSize: Int = 56) {
    val c = LocalAppColors.current
    // Display only: 10'4 reads as 10' 4 (what's typed is unchanged)
    val text = input.ifEmpty { "0" }.replace(Regex("'(?=\\d)"), "' ")
    val size = when {
        compact -> if (text.length > 14) 30 else 40
        text.length <= 8 -> 56
        text.length <= 11 -> 46
        text.length <= 14 -> 38
        text.length <= 18 -> 30
        else -> 24
    }.coerceAtMost(maxSize)
    Text(
        text, color = c.ink, fontSize = size.sp, fontWeight = FontWeight.SemiBold,
        maxLines = 1, softWrap = false, textAlign = TextAlign.End,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun UnitsToggle(isMetric: Boolean, onToggle: () -> Unit) {
    val c = LocalAppColors.current
    Row(Modifier.clip(RoundedCornerShape(18.dp)).background(c.fn).padding(3.dp)) {
        listOf("FT-IN" to false, "M" to true).forEach { (label, metric) ->
            val selected = metric == isMetric
            Box(
                Modifier.height(32.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (selected) c.fav else Color.Transparent)
                    .clickable(role = Role.Button, onClickLabel = if (metric) "Use metric" else "Use feet and inches") {
                        if (!selected) onToggle()
                    }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label, color = if (selected) c.favInk else c.fnInk,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
                )
            }
        }
    }
}

@Composable
fun ChipButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier.height(38.dp).clip(RoundedCornerShape(19.dp))
            .background(c.chip).border(1.dp, c.line, RoundedCornerShape(19.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = c.ink, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun RoundIconButton(icon: ImageVector, label: String, background: Color, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(background)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

// ---------- Keypads ----------

/** One calculator key. */
@Composable
fun RowScope.Key(
    background: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    weight: Float = 1f,
    label: String? = null,
    content: @Composable () -> Unit,
) {
    // Gaps come from padding inside each cell, so a 2-wide key lines up with the columns above
    Box(
        modifier.weight(weight).fillMaxHeight().padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(14.dp)).background(background)
            .clickable(role = Role.Button, onClick = onClick)
            .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
    }
}

@Composable
fun KeyText(text: String, size: Int, weight: FontWeight = FontWeight.Medium) {
    Text(text, color = LocalContentColor.current, fontSize = size.sp, fontWeight = weight, maxLines = 1)
}

@Composable
private fun KeyRow(height: Dp, content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().height(height), content = content)
}

@Composable
private fun KeypadColumn(spacing: KeypadSpacing, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = spacing.bottom),
        verticalArrangement = Arrangement.spacedBy(spacing.gap),
        content = content,
    )
}

@Composable
private fun RowScope.NumKey(digit: String, onAction: (CalcAction) -> Unit) {
    val c = LocalAppColors.current
    Key(c.num, c.numInk, { onAction(CalcAction.Digit(digit)) }) { KeyText(digit, 28) }
}

@Composable
private fun RowScope.OpKey(symbol: String, op: String, label: String, onAction: (CalcAction) -> Unit) {
    val c = LocalAppColors.current
    Key(c.op, c.opInk, { onAction(CalcAction.Op(op)) }, label = label) { KeyText(symbol, 32) }
}

@Composable
private fun RowScope.TopRow(
    favorites: List<Screen>,
    onOpenTool: (Screen) -> Unit,
    onAddFavorite: () -> Unit,
    onAction: (CalcAction) -> Unit,
) {
    val c = LocalAppColors.current
    Key(c.fn, c.danger, { onAction(CalcAction.Clear) }, label = "All clear") { KeyText("AC", 22, FontWeight.Bold) }
    Key(c.fn, c.fnInk, { onAction(CalcAction.Backspace) }, label = "Backspace") {
        Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = null, modifier = Modifier.size(28.dp))
    }
    for (i in 0 until MAX_FAVORITES) {
        val tool = favorites.getOrNull(i)?.let { toolFor(it) }
        if (tool != null) {
            Key(c.fav, c.favInk, { onOpenTool(tool.screen) }, label = "Favorite: ${tool.name}") {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(tool.icon, contentDescription = null, tint = c.favInk, modifier = Modifier.size(18.dp))
                    Text(
                        tool.keyLabel, color = c.favInk, fontSize = 15.sp, lineHeight = 17.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, maxLines = 1,
                    )
                }
            }
        } else {
            Key(c.fn, c.fnInk, onAddFavorite, label = "Add a favorite tool") {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun ImperialKeypad(
    keyHeight: Dp,
    spacing: KeypadSpacing,
    favorites: List<Screen>,
    onOpenTool: (Screen) -> Unit,
    onAddFavorite: () -> Unit,
    onAction: (CalcAction) -> Unit,
) {
    val c = LocalAppColors.current
    KeypadColumn(spacing) {
        KeyRow(keyHeight) { TopRow(favorites, onOpenTool, onAddFavorite, onAction) }
        KeyRow(keyHeight) {
            Key(c.fn, c.fnInk, { onAction(CalcAction.Feet) }, label = "Feet") { KeyText("ft '", 22, FontWeight.SemiBold) }
            Key(c.fn, c.fnInk, { onAction(CalcAction.Inch) }, label = "Inches") { KeyText("in \"", 22, FontWeight.SemiBold) }
            Key(c.num, c.numInk, { onAction(CalcAction.Digit(".")) }, label = "Decimal point") { KeyText(".", 32, FontWeight.SemiBold) }
            OpKey("÷", "÷", "Divide", onAction)
        }
        KeyRow(keyHeight) { NumKey("7", onAction); NumKey("8", onAction); NumKey("9", onAction); OpKey("×", "X", "Multiply", onAction) }
        KeyRow(keyHeight) { NumKey("4", onAction); NumKey("5", onAction); NumKey("6", onAction); OpKey("−", "-", "Subtract", onAction) }
        KeyRow(keyHeight) { NumKey("1", onAction); NumKey("2", onAction); NumKey("3", onAction); OpKey("+", "+", "Add", onAction) }
        KeyRow(keyHeight) {
            NumKey("0", onAction)
            Key(c.fn, c.fnInk, { onAction(CalcAction.Space) }, label = "Space, between whole inches and a fraction") {
                Text("space", color = c.fnInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
            }
            Key(c.fn, c.fnInk, { onAction(CalcAction.Slash) }, label = "Fraction bar") {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("/", color = c.fnInk, fontSize = 26.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold)
                    // The caption only fits once keys are at least 40dp tall
                    if (keyHeight >= 40.dp) {
                        Text("frac", color = c.fnInk, fontSize = 11.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Key(c.eq, c.eqInk, { onAction(CalcAction.Equals) }, label = "Equals") { KeyText("=", 34, FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun MetricKeypad(
    keyHeight: Dp,
    spacing: KeypadSpacing,
    favorites: List<Screen>,
    onOpenTool: (Screen) -> Unit,
    onAddFavorite: () -> Unit,
    onAction: (CalcAction) -> Unit,
) {
    val c = LocalAppColors.current
    KeypadColumn(spacing) {
        KeyRow(keyHeight) { TopRow(favorites, onOpenTool, onAddFavorite, onAction) }
        KeyRow(keyHeight) {
            Key(c.fn, c.fnInk, { onAction(CalcAction.Feet) }, label = "Meters") { KeyText("m", 22, FontWeight.SemiBold) }
            Key(c.fn, c.fnInk, { onAction(CalcAction.Inch) }, label = "Centimeters") { KeyText("cm", 22, FontWeight.SemiBold) }
            Key(c.fn, c.fnInk, { onAction(CalcAction.Millimeter) }, label = "Millimeters") { KeyText("mm", 22, FontWeight.SemiBold) }
            OpKey("÷", "÷", "Divide", onAction)
        }
        KeyRow(keyHeight) { NumKey("7", onAction); NumKey("8", onAction); NumKey("9", onAction); OpKey("×", "X", "Multiply", onAction) }
        KeyRow(keyHeight) { NumKey("4", onAction); NumKey("5", onAction); NumKey("6", onAction); OpKey("−", "-", "Subtract", onAction) }
        KeyRow(keyHeight) { NumKey("1", onAction); NumKey("2", onAction); NumKey("3", onAction); OpKey("+", "+", "Add", onAction) }
        KeyRow(keyHeight) {
            // The wide 0 spans two columns plus the gap between them
            Key(c.num, c.numInk, { onAction(CalcAction.Digit("0")) }, weight = 2f) { KeyText("0", 28) }
            Key(c.num, c.numInk, { onAction(CalcAction.Digit(".")) }, label = "Decimal point") { KeyText(".", 32, FontWeight.SemiBold) }
            Key(c.eq, c.eqInk, { onAction(CalcAction.Equals) }, label = "Equals") { KeyText("=", 34, FontWeight.SemiBold) }
        }
    }
}

// ---------- Construction tools panel ----------

@Composable
private fun ToolsHandle(expanded: Boolean, height: Dp = 48.dp, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Box(Modifier.fillMaxWidth().height(height).padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(
            Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                .background(if (expanded) c.fav else c.surface)
                .then(if (expanded) Modifier else Modifier.border(1.dp, c.line, RoundedCornerShape(12.dp)))
                .clickable(role = Role.Button, onClickLabel = if (expanded) "Hide tools" else "Show tools", onClick = onClick),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val ink = if (expanded) c.favInk else c.ink
            Icon(Icons.Outlined.GridView, contentDescription = null, tint = ink, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Construction tools", color = ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(6.dp))
            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null, tint = ink, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ToolsPanel(
    favorites: List<Screen>,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    onToggleFavorite: (Screen) -> Unit,
    onOpenTool: (Screen) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalAppColors.current
    Column(modifier.fillMaxWidth()) {
        ToolsHandle(expanded = true, onClick = onCollapse)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToolCatalog.groupBy { it.group }.entries.forEachIndexed { index, (group, tools) ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = if (index == 0) 8.dp else 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            group.uppercase(), color = c.muted, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.weight(1f),
                        )
                        if (index == 0) {
                            Text(
                                if (editing) "Done" else "Edit favorites",
                                color = c.fnInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .clickable(role = Role.Button) { onEditingChange(!editing) }
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
                if (index == 0 && editing) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "Tap tools to choose your two favorites for the keypad.",
                            color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
                items(tools, key = { it.screen.name }) { tool ->
                    ToolTile(
                        tool = tool,
                        favorite = tool.screen in favorites,
                        editing = editing,
                        onClick = { if (editing) onToggleFavorite(tool.screen) else onOpenTool(tool.screen) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolTile(tool: ToolInfo, favorite: Boolean, editing: Boolean, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.fillMaxWidth().height(70.dp).clip(shape)
            .background(if (editing && favorite) c.fn else c.surface)
            .border(BorderStroke(if (editing && favorite) 2.dp else 1.dp, if (editing && favorite) c.fnInk else c.line), shape)
            .clickable(
                role = Role.Button,
                onClickLabel = if (editing) (if (favorite) "Remove from favorites" else "Add to favorites") else "Open",
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(tool.icon, contentDescription = null, tint = c.fnInk, modifier = Modifier.size(22.dp))
            Text(tool.name, color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 15.sp, maxLines = 2)
        }
        if (favorite || editing) {
            Icon(
                if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = if (favorite) "Favorite" else null,
                tint = if (favorite) c.star else c.muted,
                modifier = Modifier.align(Alignment.TopEnd).size(if (editing) 18.dp else 13.dp),
            )
        }
    }
}

// ---------- History ----------

@Composable
private fun HistoryList(
    history: List<HistoryEntry>,
    historyOn: Boolean,
    onUse: (HistoryEntry) -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalAppColors.current
    Column(modifier.fillMaxWidth()) {
        if (history.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    if (historyOn) "No calculations yet. Answers show up here after you press =."
                    else "History is turned off. You can turn it on in Menu.",
                    color = c.muted, fontSize = 16.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        } else {
            val today = localEpochDay(currentTimeMillis())
            // Newest first, each day's entries followed by its label; drawn bottom-up
            val rows = remember(history, today) {
                buildList<Any> {
                    history.asReversed().groupBy { localEpochDay(it.atMillis) }.forEach { (day, entries) ->
                        addAll(entries)
                        add(historyDayLabel(day, today))
                    }
                }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), reverseLayout = true) {
                items(rows) { row ->
                    when (row) {
                        is HistoryEntry -> Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .clickable(role = Role.Button, onClickLabel = "Use this answer") { onUse(row) }
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.End,
                        ) {
                            Text(prettyExpression(row.expression), color = c.muted, fontSize = 17.sp, textAlign = TextAlign.End)
                            Text(row.result, color = c.ink, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
                        }
                        is String -> Text(
                            row.uppercase(), color = c.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp,
                            modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp),
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (history.isNotEmpty()) {
                Text(
                    "Clear history", color = c.danger, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onClear)
                        .padding(horizontal = 4.dp, vertical = 12.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            RoundIconButton(Icons.Outlined.ExpandLess, "Close history", c.fn, c.fnInk, onClose)
        }
    }
}
