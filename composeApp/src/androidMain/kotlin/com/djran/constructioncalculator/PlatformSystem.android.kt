package com.djran.constructioncalculator

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import java.util.TimeZone

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun localUtcOffsetMinutes(epochMillis: Long): Int =
    TimeZone.getDefault().getOffset(epochMillis) / 60_000

@Composable
actual fun SystemBarsAppearance(darkBackground: Boolean, followSystem: Boolean) {
    val view = LocalView.current
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkBackground
            isAppearanceLightNavigationBars = !darkBackground
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
actual fun BackGesture(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled, onBack)
