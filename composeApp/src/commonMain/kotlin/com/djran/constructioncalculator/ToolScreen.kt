package com.djran.constructioncalculator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The frame every construction tool sits in: the calculator's background and width (centred on
 * wide screens), and a header with a back arrow and the tool's name from the tools panel.
 * [actions] go at the right of the header. The content scrolls unless [scrollable] is false.
 */
@Composable
fun ToolScreen(
    tool: Screen,
    onBack: () -> Unit,
    scrollable: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalAppColors.current
    Surface(Modifier.fillMaxSize(), color = c.bg, contentColor = c.ink) {
        Box(contentAlignment = Alignment.TopCenter) {
            Column(Modifier.fillMaxHeight().widthIn(max = CalcMaxWidth).fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 4.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = c.ink, modifier = Modifier.size(30.dp))
                    }
                    Text(
                        toolFor(tool)?.name.orEmpty(), color = c.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                    actions()
                }
                Column(
                    Modifier.fillMaxWidth().weight(1f)
                        .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                    content = content,
                )
            }
        }
    }
}
