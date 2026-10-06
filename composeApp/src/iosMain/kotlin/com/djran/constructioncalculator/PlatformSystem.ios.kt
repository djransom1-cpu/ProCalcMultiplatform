package com.djran.constructioncalculator

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.runtime.SideEffect
import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceStyle

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun localUtcOffsetMinutes(epochMillis: Long): Int {
    val date = NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0)
    return (NSTimeZone.localTimeZone.secondsFromGMTForDate(date) / 60).toInt()
}

@Composable
actual fun SystemBarsAppearance(darkBackground: Boolean, followSystem: Boolean) {
    // iOS colours the status bar from the window's light/dark style. Only override
    // it when the user picked Light or Dark in Menu, so "Match phone" keeps working.
    SideEffect {
        val style = when {
            followSystem -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
            darkBackground -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
            else -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
        }
        UIApplication.sharedApplication.keyWindow?.overrideUserInterfaceStyle = style
    }
}

// Compose turns the swipe in from the left edge into a back event. BackHandler is deprecated
// for NavigationEventHandler, which needs the extra navigationevent-compose library.
@Suppress("DEPRECATION")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun BackGesture(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled, onBack)
