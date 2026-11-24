package top.ltfan.m3x.compose.ui.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIAccessibilityDarkerSystemColorsEnabled
import platform.UIKit.UIAccessibilityDarkerSystemColorsStatusDidChangeNotification
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIAccessibilityIsReduceTransparencyEnabled
import platform.UIKit.UIAccessibilityReduceMotionStatusDidChangeNotification
import platform.UIKit.UIAccessibilityReduceTransparencyStatusDidChangeNotification
import platform.UIKit.UIApplicationDidBecomeActiveNotification

@Composable
internal actual fun rememberSystemAccessibilitySettingsPlatform(): SystemAccessibilitySettings {
    val state = remember { mutableStateOf(readAccessibilitySettings()) }
    DisposableEffect(Unit) {
        var disposed = false
        val center = NSNotificationCenter.defaultCenter
        val names =
            listOf(
                UIAccessibilityDarkerSystemColorsStatusDidChangeNotification,
                UIAccessibilityReduceMotionStatusDidChangeNotification,
                UIAccessibilityReduceTransparencyStatusDidChangeNotification,
                UIApplicationDidBecomeActiveNotification,
            )
        val tokens = names.map { name ->
            center.addObserverForName(name, null, NSOperationQueue.mainQueue) {
                if (!disposed) state.value = readAccessibilitySettings()
            }
        }
        state.value = readAccessibilitySettings()
        onDispose {
            disposed = true
            tokens.forEach(center::removeObserver)
        }
    }
    return state.value
}

private fun readAccessibilitySettings() =
    SystemAccessibilitySettings(
        reduceTransparency = UIAccessibilityIsReduceTransparencyEnabled(),
        increaseContrast = UIAccessibilityDarkerSystemColorsEnabled(),
        reduceMotion = UIAccessibilityIsReduceMotionEnabled(),
    )
