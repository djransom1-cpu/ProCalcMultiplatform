package com.djran.constructioncalculator

import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** One finished calculation, for the History panel. Shared across projects. */
@Serializable
data class HistoryEntry(val expression: String, val result: String, val atMillis: Long)

/** Settings from the Menu screen. Favorites hold [Screen] names. */
@Serializable
data class UiPrefs(
    val appearance: Appearance = Appearance.System,
    val startMetric: Boolean = false,
    val favorites: List<String> = listOf(Screen.StairCalculator.name, Screen.RafterCalculator.name),
    val keepHistory: Boolean = true,
    /** Decimal places of a millimetre in metric answers: 0 = 1 mm, 1 = 0.1 mm, 2 = 0.01 mm. */
    val metricPrecision: Int = 1,
) {
    val favoriteScreens: List<Screen>
        get() = favorites.mapNotNull { name -> Screen.entries.firstOrNull { it.name == name } }
}

const val MAX_FAVORITES = 2
const val MAX_HISTORY = 200

/** Saves the Menu settings and calculation history next to the app's project data. */
class UiStore(private val settings: Settings?) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun loadPrefs(): UiPrefs = try {
        settings?.getStringOrNull(KEY_PREFS)?.let { json.decodeFromString<UiPrefs>(it) } ?: UiPrefs()
    } catch (e: Exception) {
        UiPrefs()
    }

    fun savePrefs(prefs: UiPrefs) {
        settings?.putString(KEY_PREFS, json.encodeToString(prefs))
    }

    fun loadHistory(): List<HistoryEntry> = try {
        settings?.getStringOrNull(KEY_HISTORY)?.let { json.decodeFromString<List<HistoryEntry>>(it) } ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    fun saveHistory(history: List<HistoryEntry>) {
        settings?.putString(KEY_HISTORY, json.encodeToString(history))
    }

    private companion object {
        const val KEY_PREFS = "ui_prefs_v1"
        const val KEY_HISTORY = "calc_history_v1"
    }
}

private val MONTHS = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

/** Days since 1970-01-01 in the phone's time zone. */
fun localEpochDay(epochMillis: Long): Long =
    (epochMillis + localUtcOffsetMinutes(epochMillis) * 60_000L).floorDiv(86_400_000L)

/** "Today", "Yesterday", "October 3", or "October 3, 2025" for older years. */
fun historyDayLabel(epochDay: Long, today: Long): String {
    if (epochDay == today) return "Today"
    if (epochDay == today - 1) return "Yesterday"
    val (y, m, d) = civilFromDays(epochDay)
    val (thisYear, _, _) = civilFromDays(today)
    return if (y == thisYear) "${MONTHS[m - 1]} $d" else "${MONTHS[m - 1]} $d, $y"
}

/** Year, month (1-12) and day for a count of days since 1970-01-01. */
private fun civilFromDays(epochDay: Long): Triple<Int, Int, Int> {
    val z = epochDay + 719_468
    val era = z.floorDiv(146_097)
    val doe = z - era * 146_097
    val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val day = (doy - (153 * mp + 2) / 5 + 1).toInt()
    val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
    val year = (yoe + era * 400 + if (month <= 2) 1 else 0).toInt()
    return Triple(year, month, day)
}
