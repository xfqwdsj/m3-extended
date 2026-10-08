package top.ltfan.m3x.compose.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.LocalAwtWindow
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.window.WindowPlacement
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.beans.PropertyChangeListener

@Composable internal actual fun readPublicDisplayCornerRadii(): CornerRadii? = null

@Composable internal actual fun readPublicWindowCornerRadii(): CornerRadii? = null

@OptIn(ExperimentalComposeUiApi::class)
@Composable
public actual fun rememberWindowPresentation(): WindowPresentation {
    if (LocalInspectionMode.current) return WindowPresentation.Unknown
    val window = LocalAwtWindow.current ?: return WindowPresentation.Unknown
    val state = remember(window) { mutableStateOf(readPresentation(window)) }
    DisposableEffect(window) {
        var closed = false
        val update = {
            state.value = if (closed) WindowPresentation.Unknown else readPresentation(window)
        }
        val listener =
            object : WindowAdapter() {
                override fun windowOpened(event: WindowEvent) = update()

                override fun windowStateChanged(event: WindowEvent) = update()

                override fun windowClosing(event: WindowEvent) {
                    closed = true
                    state.value = WindowPresentation.Unknown
                }

                override fun windowClosed(event: WindowEvent) {
                    closed = true
                    state.value = WindowPresentation.Unknown
                }
            }
        val componentListener =
            object : ComponentAdapter() {
                override fun componentResized(event: ComponentEvent) = update()

                override fun componentMoved(event: ComponentEvent) = update()
            }
        val propertyListener = PropertyChangeListener { update() }
        window.addWindowListener(listener)
        window.addWindowStateListener(listener)
        window.addComponentListener(componentListener)
        window.addPropertyChangeListener(propertyListener)
        update()
        onDispose {
            window.removeWindowListener(listener)
            window.removeWindowStateListener(listener)
            window.removeComponentListener(componentListener)
            window.removePropertyChangeListener(propertyListener)
        }
    }
    return state.value
}

private fun readPresentation(window: Window): WindowPresentation {
    if (!window.isDisplayable) return WindowPresentation.Unknown
    val fullscreen = window is ComposeWindow && window.placement == WindowPlacement.Fullscreen
    return if (fullscreen) WindowPresentation.Fullscreen else WindowPresentation.Windowed
}
