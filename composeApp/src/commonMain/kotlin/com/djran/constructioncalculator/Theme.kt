package com.djran.constructioncalculator

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import procalcmultiplatform.composeapp.generated.resources.Res
import procalcmultiplatform.composeapp.generated.resources.barlow_bold
import procalcmultiplatform.composeapp.generated.resources.barlow_medium
import procalcmultiplatform.composeapp.generated.resources.barlow_regular
import procalcmultiplatform.composeapp.generated.resources.barlow_semibold

/** Brand colours. The light theme is built from these; screens use [AppColors]. */
object Palette {
    val Navy = Color(0xFF0A2A66)
    val Ink = Color(0xFF13213D)
    val Muted = Color(0xFF5B6578)
    val Bg = Color(0xFFF3F4F7)
    val Surface = Color.White
    val Line = Color(0xFFDCE1EA)
    val FieldLine = Color(0xFFCBD3E1)
    val NavyTint = Color(0xFFE2E7F0)
    val Orange = Color(0xFFC85F0C)
    val OrangeTint = Color(0xFFFBE6D4)
    val OrangeInk = Color(0xFFA84B05)
    val Red = Color(0xFFB3261E)
    val Star = Color(0xFFE07B1F)
}

/** Colours for the screens that follow light/dark. Read them with [AppTheme.colors]. */
@Immutable
data class AppColors(
    val isDark: Boolean,
    val bg: Color,
    val ink: Color,
    val muted: Color,
    val surface: Color,
    val line: Color,
    /** Headings, links and highlighted results. */
    val accent: Color,
    /** Clearing and deleting. */
    val danger: Color,
    val success: Color,
    val num: Color,
    val numInk: Color,
    val fn: Color,
    val fnInk: Color,
    val op: Color,
    val opInk: Color,
    val eq: Color,
    val eqInk: Color,
    val fav: Color,
    val favInk: Color,
    val star: Color,
    val chip: Color,
    val menu: Color,
    val nav: Color,
    val navInk: Color,
    val navActive: Color,
    val navActiveInk: Color,
)

val LightAppColors = AppColors(
    isDark = false,
    bg = Palette.Bg, ink = Palette.Ink, muted = Palette.Muted, surface = Color.White, line = Palette.Line,
    accent = Palette.Navy, danger = Palette.Red, success = Color(0xFF2E7D32),
    num = Color.White, numInk = Palette.Ink,
    fn = Palette.NavyTint, fnInk = Palette.Navy,
    op = Palette.OrangeTint, opInk = Palette.OrangeInk, eq = Palette.Orange, eqInk = Color.White,
    fav = Palette.Navy, favInk = Color.White, star = Palette.Star,
    chip = Color.White, menu = Color.White,
    nav = Color.White, navInk = Palette.Muted, navActive = Palette.NavyTint, navActiveInk = Palette.Navy,
)

val DarkAppColors = AppColors(
    isDark = true,
    bg = Color(0xFF0B111C), ink = Color(0xFFF2F4F8), muted = Color(0xFF9AA6BA), surface = Color(0xFF151D2B), line = Color(0xFF253047),
    accent = Color(0xFF9DB8F2), danger = Color(0xFFFF8A80), success = Color(0xFF81C995),
    num = Color(0xFF1D2738), numInk = Color(0xFFF2F4F8),
    fn = Color(0xFF2A3754), fnInk = Color(0xFFDCE4F5),
    op = Color(0xFF3A2614), opInk = Color(0xFFFFB066), eq = Color(0xFFE8822A), eqInk = Color(0xFF1A0E03),
    fav = Color(0xFF22396B), favInk = Color.White, star = Color(0xFFF2A65A),
    chip = Color(0xFF1D2738), menu = Color(0xFF1D2738),
    nav = Color(0xFF121A27), navInk = Color(0xFF9AA6BA), navActive = Color(0xFF22396B), navActiveInk = Color.White,
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/** The current theme, like MaterialTheme: `AppTheme.colors.accent`. */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
}

enum class Appearance { System, Light, Dark }

@Composable
fun barlowFamily(): FontFamily = FontFamily(
    Font(Res.font.barlow_regular, FontWeight.Normal),
    Font(Res.font.barlow_medium, FontWeight.Medium),
    Font(Res.font.barlow_semibold, FontWeight.SemiBold),
    Font(Res.font.barlow_bold, FontWeight.Bold),
)

/** App theme: Barlow everywhere, navy as the primary colour, orange for main actions. */
@Composable
fun ProCalcTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) DarkAppColors else LightAppColors
    val materialColors = if (dark) {
        darkColors(
            primary = colors.accent,
            primaryVariant = Color(0xFF22396B),
            secondary = Color(0xFFE8822A),
            background = colors.bg,
            surface = colors.surface,
            onPrimary = Color(0xFF0B111C),
            onSecondary = Color(0xFF1A0E03),
            onBackground = colors.ink,
            onSurface = colors.ink,
        )
    } else {
        lightColors(
            primary = Palette.Navy,
            primaryVariant = Palette.Navy,
            secondary = Palette.Orange,
            background = Palette.Bg,
            surface = Color.White,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Palette.Ink,
            onSurface = Palette.Ink,
        )
    }
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colors = materialColors,
            // Buttons read like the calculator's keys: no all-caps letter spacing
            typography = Typography(
                defaultFontFamily = barlowFamily(),
                button = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 0.4.sp),
            ),
            // Rounded fields, buttons and cards, like the keypad and Menu cards
            shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp)),
            content = content,
        )
    }
}
