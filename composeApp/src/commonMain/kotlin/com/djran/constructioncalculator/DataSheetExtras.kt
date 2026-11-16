package com.djran.constructioncalculator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.djran.constructioncalculator.StoryPoleMath.Layer
import com.djran.constructioncalculator.StoryPoleMath.Mode
import com.djran.constructioncalculator.StoryPoleMath.Op
import com.djran.constructioncalculator.StoryPoleMath.Pole
import kotlinx.serialization.Serializable
import kotlin.math.abs

/*
 * Story Pole and Survey Calc results saved to a project, shown on the Data Sheet and in the
 * report, the same way the Android app does it.
 */

@Serializable
data class SavedPoleLayer(val label: String = "", val amount: String = "", val op: String = "SUBTRACT")

@Serializable
data class SavedStoryPole(
    val name: String = "Pole", val mode: String = "LENGTH", val trade: String = "FRAMING",
    val startLabel: String = "Start", val start: String = "0", val endLabel: String = "Result", val target: String = "",
    val layers: List<SavedPoleLayer> = emptyList()
) {
    fun toPole() = Pole(
        name, runCatching { Mode.valueOf(mode) }.getOrDefault(Mode.LENGTH), trade, startLabel, start, endLabel, target,
        layers.map { Layer(it.label, it.amount, runCatching { Op.valueOf(it.op) }.getOrDefault(Op.SUBTRACT)) }.toMutableList()
    )

    companion object {
        fun of(p: Pole) = SavedStoryPole(p.name, p.mode.name, p.trade, p.startLabel, p.start, p.endLabel, p.target,
            p.layers.map { SavedPoleLayer(it.label, it.amount, it.op.name) })
    }
}

/** One Survey Calc result saved to a project (backsight check, check shot, resection...). */
@Serializable
data class SurveyRecordEntry(val type: String, val name: String, val time: Long, val ok: Boolean? = null, val lines: List<String> = emptyList())

object DataSheetExtras {

    /** Story pole lines, as the Android Data Sheet prints them. */
    fun poleLines(p: Pole): List<String> {
        val r = StoryPoleMath.compute(p)
        if (!r.ok) return listOf("Incomplete: " + r.errors.joinToString("; "))
        val lines = mutableListOf("${p.startLabel}: ${StoryPoleMath.fmtFtIn(r.start)} (${StoryPoleMath.fmtSecond(r.start, p.mode)})")
        p.layers.forEachIndexed { i, l ->
            val d = r.rows[i].delta ?: 0.0
            lines += "${if (d >= 0) "+" else "−"} ${StoryPoleMath.fmtIn(abs(d))}  ${l.label}${if (i == r.unknownIndex) " (solved)" else ""}  → ${StoryPoleMath.fmtFtIn(r.rows[i].value)}"
        }
        lines += "= ${p.endLabel}: ${StoryPoleMath.fmtFtIn(r.final)} (${StoryPoleMath.fmtSecond(r.final, p.mode)})"
        return lines
    }

    fun recordTitle(r: SurveyRecordEntry) = r.name + when (r.ok) { true -> " · PASS"; false -> " · FAIL"; null -> "" }

    /** Report sections for these extras (PDF and print). */
    fun sections(poles: List<SavedStoryPole>, records: List<SurveyRecordEntry>): List<SummarySection> {
        val out = mutableListOf<SummarySection>()
        poles.forEach { sp -> out += SummarySection("STORY POLE: ${sp.name}", listOf("Step"), poleLines(sp.toPole()).map { listOf(it) }) }
        records.forEach { r -> out += SummarySection("SURVEY: ${recordTitle(r)}", listOf("Result"), r.lines.map { listOf(it) }) }
        return out
    }
}

/** Data Sheet: saved story poles (with drawings) and survey results for the project. */
@Composable
fun DataSheetExtrasSections(
    poles: List<SavedStoryPole>,
    records: List<SurveyRecordEntry>,
    onRemovePole: (Int) -> Unit,
    onRemoveRecord: (Int) -> Unit,
) {
    if (poles.isNotEmpty()) {
        SummaryTable("STORY POLES", listOf("Name", "Result")) {
            poles.forEachIndexed { i, sp ->
                val p = sp.toPole()
                Column(Modifier.padding(vertical = 8.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(sp.name, fontWeight = FontWeight.Bold, color = Color(0xFF0A2A66), modifier = Modifier.weight(1f))
                        Text("remove", color = Color(0xFFB3261E), fontSize = 12.sp, modifier = Modifier.clickable { onRemovePole(i) }.padding(6.dp))
                    }
                    DataSheetExtras.poleLines(p).forEach { Text(it, fontSize = 13.sp, color = Color.Black) }
                    Spacer(Modifier.height(6.dp))
                    StoryPoleDiagram(p, Modifier.fillMaxWidth().height((110 + 52 * p.layers.size).dp))
                }
                Divider()
            }
        }
    }
    if (records.isNotEmpty()) {
        SummaryTable("SURVEY CALC RESULTS", listOf("Name", "Result")) {
            records.forEachIndexed { i, r ->
                Column(Modifier.padding(vertical = 8.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(DataSheetExtras.recordTitle(r), fontWeight = FontWeight.Bold,
                            color = when (r.ok) { true -> Color(0xFF15803D); false -> Color(0xFFB3261E); null -> Color(0xFF0A2A66) },
                            modifier = Modifier.weight(1f))
                        Text("remove", color = Color(0xFFB3261E), fontSize = 12.sp, modifier = Modifier.clickable { onRemoveRecord(i) }.padding(6.dp))
                    }
                    r.lines.forEach { Text(it, fontSize = 13.sp, color = Color.Black) }
                }
                Divider()
            }
        }
    }
}
