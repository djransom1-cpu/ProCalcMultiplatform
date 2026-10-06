package com.djran.constructioncalculator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MenuScreen(
    prefs: UiPrefs,
    onPrefsChange: (UiPrefs) -> Unit,
    onEditFavorites: () -> Unit,
    onClearHistory: () -> Unit,
    onOpen: (Screen) -> Unit,
) {
    val c = LocalAppColors.current
    Box(Modifier.fillMaxSize().background(c.bg), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.fillMaxHeight().widthIn(max = CalcMaxWidth).fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
        ) {
            Text(
                "Menu", color = c.ink, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 8.dp),
            )

            SectionLabel("Appearance")
            MenuCard {
                Column(Modifier.padding(12.dp)) {
                    Segmented(
                        options = listOf("Match phone" to Appearance.System, "Light" to Appearance.Light, "Dark" to Appearance.Dark),
                        selected = prefs.appearance,
                        onSelect = { onPrefsChange(prefs.copy(appearance = it)) },
                    )
                    Text(
                        when (prefs.appearance) {
                            Appearance.System -> "Follows your phone's light or dark setting."
                            Appearance.Light -> "Always light."
                            Appearance.Dark -> "Always dark."
                        },
                        color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                    )
                }
            }

            SectionLabel("Calculator")
            MenuCard {
                MenuRow("Starting units") {
                    Box(Modifier.size(width = 120.dp, height = 36.dp)) {
                        Segmented(
                            options = listOf("FT-IN" to false, "M" to true),
                            selected = prefs.startMetric,
                            onSelect = { onPrefsChange(prefs.copy(startMetric = it)) },
                            height = 30,
                        )
                    }
                }
                RowDivider()
                MenuRow(
                    "Favorite tools",
                    onClick = onEditFavorites,
                    value = prefs.favoriteScreens.mapNotNull { toolFor(it)?.keyLabel?.lowercase()?.replaceFirstChar { ch -> ch.uppercase() } }
                        .joinToString(", ").ifEmpty { "None" },
                )
                RowDivider()
                MenuRow("Keep calculation history") {
                    Switch(
                        checked = prefs.keepHistory,
                        onCheckedChange = { onPrefsChange(prefs.copy(keepHistory = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = c.fav,
                            checkedTrackAlpha = 1f,
                        ),
                    )
                }
                RowDivider()
                Text(
                    "Clear history", color = c.acInk, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClearHistory)
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                )
            }

            SectionLabel("Account & help")
            MenuCard {
                if (cloudSyncSupported) {
                    MenuRow("Account & sync", onClick = { onOpen(Screen.Account) })
                    RowDivider()
                }
                MenuRow("Help & guides", onClick = { onOpen(Screen.CalculatorHelp) })
                RowDivider()
                MenuRow("About ProCalc", onClick = { onOpen(Screen.About) })
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val c = LocalAppColors.current
    Text(
        text.uppercase(), color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun MenuCard(content: @Composable ColumnScope.() -> Unit) {
    val c = LocalAppColors.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.line, shape),
        content = content,
    )
}

@Composable
private fun RowDivider() {
    val c = LocalAppColors.current
    Divider(color = c.line, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun MenuRow(
    title: String,
    onClick: (() -> Unit)? = null,
    value: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth().height(54.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        if (value != null) Text(value, color = c.muted, fontSize = 15.sp)
        trailing?.invoke()
        if (onClick != null) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = c.muted, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun <T> Segmented(options: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit, height: Int = 40) {
    val c = LocalAppColors.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.fn).padding(3.dp)) {
        options.forEach { (label, value) ->
            val isSelected = value == selected
            Box(
                Modifier.weight(1f).height(height.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) c.fav else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onSelect(value) }
                    .semantics { this.selected = isSelected },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label, color = if (isSelected) c.favInk else c.fnInk, fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold, maxLines = 1,
                )
            }
        }
    }
}
