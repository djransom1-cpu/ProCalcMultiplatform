package com.djran.constructioncalculator

import androidx.compose.runtime.Composable

/** Milliseconds since 1970 (UTC). */
expect fun currentTimeMillis(): Long

/** The device's offset from UTC at the given moment, in minutes (e.g. -240 for EDT). */
expect fun localUtcOffsetMinutes(epochMillis: Long): Int

/**
 * Makes the clock and battery icons readable over the app's top strip.
 * [followSystem] leaves the phone's own light/dark choice in charge.
 */
@Composable
expect fun SystemBarsAppearance(darkBackground: Boolean, followSystem: Boolean)

/**
 * Runs [onBack] on the platform's own back gesture while [enabled]: Android's back swipe or
 * button, the iPhone's swipe in from the left edge, and the browser's back button or swipe
 * on the web.
 */
@Composable
expect fun BackGesture(enabled: Boolean, onBack: () -> Unit)
