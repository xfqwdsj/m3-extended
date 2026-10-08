package top.ltfan.m3x.compose.ui.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import platform.AppKit.NSApplicationDidBecomeActiveNotification
import platform.AppKit.NSWorkspace
import platform.AppKit.NSWorkspaceAccessibilityDisplayOptionsDidChangeNotification
import platform.AppKit.accessibilityDisplayShouldIncreaseContrast
import platform.AppKit.accessibilityDisplayShouldReduceMotion
import platform.AppKit.accessibilityDisplayShouldReduceTransparency
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue

@Composable
internal actual fun rememberSystemAccessibilitySettingsPlatform(): SystemAccessibilitySettings {
    val workspace = NSWorkspace.sharedWorkspace
    val state = remember(workspace) { mutableStateOf(readAccessibilitySettings(workspace)) }
    DisposableEffect(workspace) {
        var disposed = false
        val center = workspace.notificationCenter
        val refresh = {
            if (!disposed) state.value = readAccessibilitySettings(workspace)
        }
        val options =
            center.addObserverForName(
                NSWorkspaceAccessibilityDisplayOptionsDidChangeNotification,
                null,
                NSOperationQueue.mainQueue,
            ) {
                refresh()
            }
        val activationCenter = NSNotificationCenter.defaultCenter
        val activation =
            activationCenter.addObserverForName(
                NSApplicationDidBecomeActiveNotification,
                null,
                NSOperationQueue.mainQueue,
            ) {
                refresh()
            }
        state.value = readAccessibilitySettings(workspace)
        onDispose {
            disposed = true
            center.removeObserver(options)
            activationCenter.removeObserver(activation)
        }
    }
    return state.value
}

private fun readAccessibilitySettings(workspace: NSWorkspace) =
    SystemAccessibilitySettings(
        reduceTransparency = workspace.accessibilityDisplayShouldReduceTransparency,
        increaseContrast = workspace.accessibilityDisplayShouldIncreaseContrast,
        reduceMotion = workspace.accessibilityDisplayShouldReduceMotion,
    )
