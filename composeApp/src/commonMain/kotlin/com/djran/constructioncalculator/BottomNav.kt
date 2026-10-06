package com.djran.constructioncalculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class NavTab(val label: String, val icon: ImageVector) {
    Calculator("Calculator", Icons.Outlined.Calculate),
    Projects("Projects", Icons.Outlined.Folder),
    DataSheet("Data Sheet", Icons.Outlined.Description),
    Notes("Notes", Icons.Outlined.Draw),
    Menu("Menu", Icons.Outlined.Menu),
}

/** Which tab is lit for a screen. Tool screens belong to Calculator, where they're opened from. */
fun tabFor(screen: Screen): NavTab = when (screen) {
    Screen.ProjectList -> NavTab.Projects
    Screen.JobSummary -> NavTab.DataSheet
    Screen.JobNotes -> NavTab.Notes
    Screen.Menu, Screen.Account, Screen.CalculatorHelp, Screen.About -> NavTab.Menu
    else -> NavTab.Calculator
}

@Composable
fun BottomNav(current: NavTab, onSelect: (NavTab) -> Unit) {
    val c = LocalAppColors.current
    Column(Modifier.fillMaxWidth().background(c.nav).windowInsetsPadding(WindowInsets.navigationBars)) {
        Divider(color = c.line, thickness = 1.dp)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Row(Modifier.widthIn(max = CalcMaxWidth).fillMaxWidth()) {
                NavTab.entries.forEach { tab ->
                    val active = tab == current
                    // Sized by its content, so a larger phone font size still gets breathing room
                    Column(
                        Modifier.weight(1f)
                            .clickable(role = Role.Tab) { onSelect(tab) }
                            .semantics { selected = active }
                            .padding(top = 10.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.width(56.dp).height(30.dp).clip(RoundedCornerShape(15.dp))
                                .background(if (active) c.navActive else Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(tab.icon, contentDescription = null, tint = if (active) c.navActiveInk else c.navInk, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            tab.label, color = if (active) c.navActiveInk else c.navInk, fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold, maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
