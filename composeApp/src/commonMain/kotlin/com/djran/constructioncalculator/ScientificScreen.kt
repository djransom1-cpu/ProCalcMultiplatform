package com.djran.constructioncalculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Scientific and Graphing modes. Each key adds the same text to the expression as
 * the original screen did, and = runs MathEngine.evaluate exactly as before.
 */
@Composable
fun ScientificScreen(
    graphing: Boolean,
    projectName: String,
    onProjectClick: () -> Unit,
    cloudSync: CloudSync,
    onCloudClick: () -> Unit,
    onModeSelect: (CalcMode) -> Unit,
    onHelp: () -> Unit,
) {
    val c = LocalAppColors.current
    var expression by remember { mutableStateOf("") }
    val add: (String) -> Unit = { expression += it }

    Box(Modifier.fillMaxSize().background(c.bg), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxHeight().widthIn(max = CalcMaxWidth).fillMaxWidth()) {
            CalcTopBar(projectName, onProjectClick, cloudSync, onCloudClick)

            Column(Modifier.fillMaxWidth().weight(1f).padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(Icons.AutoMirrored.Outlined.HelpOutline, "Calculator guide", c.fn, c.fnInk, onHelp)
                    Spacer(Modifier.weight(1f))
                    ModePill(if (graphing) CalcMode.Graphing else CalcMode.Scientific, onModeSelect)
                }
                if (graphing) {
                    // The plot keeps its own colors until the drawings get their refresh
                    Box(
                        Modifier.fillMaxWidth().weight(1f).padding(top = 10.dp, bottom = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        GraphPlotter(expression)
                    }
                    Text(
                        "y = ${expression.ifEmpty { "…" }}", color = c.ink, fontSize = 28.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, softWrap = false, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth(),
                    )
                    if (!expression.contains("x")) {
                        Text(
                            "Use the x key to plot a curve.", color = c.muted, fontSize = 14.sp,
                            textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                    val text = expression.ifEmpty { "0" }
                    Text(
                        text, color = c.ink, fontWeight = FontWeight.SemiBold,
                        fontSize = when {
                            text.length <= 10 -> 52.sp
                            text.length <= 16 -> 38.sp
                            else -> 26.sp
                        },
                        maxLines = 2, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Column(
                Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Shorter keys in Graphing leave more room for the plot
                val sciRow = if (graphing) 40.dp else 46.dp
                val numRow = if (graphing) 44.dp else 52.dp
                Row(Modifier.fillMaxWidth().height(sciRow)) {
                    SciKey("sin") { add("sin(") }; SciKey("cos") { add("cos(") }; SciKey("tan") { add("tan(") }
                    SciKey("log") { add("log(") }; SciKey("ln") { add("ln(") }
                }
                Row(Modifier.fillMaxWidth().height(sciRow)) {
                    SciKey("√x", label = "Square root") { add("sqrt(") }; SciKey("^", label = "Power") { add("^") }
                    SciKey("(") { add("(") }; SciKey(")") { add(")") }; SciKey("x", label = "Variable x") { add("x") }
                }
                Row(Modifier.fillMaxWidth().height(sciRow)) {
                    SciKey("π", label = "Pi") { add("pi") }; SciKey("e") { add("e") }
                    SciKey("1/x", label = "Reciprocal") { add("1/") }; SciKey("abs", label = "Absolute value") { add("abs(") }
                    Key(c.fn, c.danger, { expression = "" }, label = "All clear") {
                        Text("AC", color = LocalContentColor.current, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Row(Modifier.fillMaxWidth().height(numRow)) {
                    SciNum("7", add); SciNum("8", add); SciNum("9", add); SciOp("÷", "/", "Divide", add)
                }
                Row(Modifier.fillMaxWidth().height(numRow)) {
                    SciNum("4", add); SciNum("5", add); SciNum("6", add); SciOp("×", "*", "Multiply", add)
                }
                Row(Modifier.fillMaxWidth().height(numRow)) {
                    SciNum("1", add); SciNum("2", add); SciNum("3", add); SciOp("−", "-", "Subtract", add)
                }
                Row(Modifier.fillMaxWidth().height(numRow)) {
                    SciNum("0", add); SciNum(".", add)
                    Key(c.fn, c.fnInk, { if (expression.isNotEmpty()) expression = expression.dropLast(1) }, label = "Backspace") {
                        Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = null, modifier = Modifier.size(26.dp))
                    }
                    SciOp("+", "+", "Add", add)
                }
                Row(Modifier.fillMaxWidth().height(numRow)) {
                    Key(c.eq, c.eqInk, {
                        val res = MathEngine.evaluate(expression)
                        expression = if (res.isNaN()) "Error" else if (res % 1.0 == 0.0) res.toLong().toString() else res.toString()
                    }, label = "Equals") {
                        Text("=", color = LocalContentColor.current, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.SciKey(text: String, label: String? = null, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Key(c.fn, c.fnInk, onClick, label = label) {
        Text(text, color = LocalContentColor.current, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RowScope.SciNum(text: String, add: (String) -> Unit) {
    val c = LocalAppColors.current
    Key(c.num, c.numInk, { add(text) }, label = if (text == ".") "Decimal point" else null) {
        Text(text, color = LocalContentColor.current, fontSize = 26.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RowScope.SciOp(symbol: String, typed: String, label: String, add: (String) -> Unit) {
    val c = LocalAppColors.current
    Key(c.op, c.opInk, { add(typed) }, label = label) {
        Text(symbol, color = LocalContentColor.current, fontSize = 30.sp)
    }
}
