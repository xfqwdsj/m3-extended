package top.ltfan.m3x.compose.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import platform.AppKit.NSWindow
import platform.AppKit.NSWindowDidBecomeKeyNotification
import platform.AppKit.NSWindowDidChangeOcclusionStateNotification
import platform.AppKit.NSWindowDidEnterFullScreenNotification
import platform.AppKit.NSWindowDidExitFullScreenNotification
import platform.AppKit.NSWindowStyleMaskFullScreen
import platform.AppKit.NSWindowWillCloseNotification
import platform.Foundation.NSKeyValueObservingOptionNew
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSSelectorFromString
import platform.Foundation.addObserver
import platform.Foundation.removeObserver
import platform.darwin.NSObject
import platform.objc.class_addMethod
import platform.objc.object_getClass

private val LocalCornerRadiiWindow = staticCompositionLocalOf<NSWindow?> { null }

/** Provides the native window whose physical corner radii shape the contained surfaces. */
@Composable
public fun ProvideWindowCornerRadii(window: NSWindow, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCornerRadiiWindow provides window, content = content)
}

/** Returns the window supplied by [ProvideWindowCornerRadii] in the current composition. */
@Composable public fun currentCornerRadiiWindow(): NSWindow? = LocalCornerRadiiWindow.current

@Composable internal actual fun readPublicDisplayCornerRadii(): CornerRadii? = null

@Composable internal actual fun readPublicWindowCornerRadii(): CornerRadii? = null

@Composable
public actual fun rememberWindowPresentation(): WindowPresentation {
    if (LocalInspectionMode.current) return WindowPresentation.Unknown
    return LocalCornerRadiiWindow.current?.let { window ->
        val state = remember(window) { mutableStateOf(readWindowPresentation(window)) }
        DisposableEffect(window) {
            var closed = false
            val observer =
                WindowStyleObserver(window) {
                    if (!closed) state.value = readWindowPresentation(window)
                }
            val center = NSNotificationCenter.defaultCenter
            val names =
                listOf(
                    NSWindowDidBecomeKeyNotification,
                    NSWindowDidChangeOcclusionStateNotification,
                    NSWindowDidEnterFullScreenNotification,
                    NSWindowDidExitFullScreenNotification,
                    NSWindowWillCloseNotification,
                )
            val notifications = names.map { name ->
                center.addObserverForName(name, window, NSOperationQueue.mainQueue) {
                    if (name == NSWindowWillCloseNotification) {
                        closed = true
                        state.value = WindowPresentation.Unknown
                    } else if (!closed) {
                        state.value = readWindowPresentation(window)
                    }
                }
            }
            onDispose {
                closed = true
                observer.dispose()
                notifications.forEach(center::removeObserver)
            }
        }
        state.value
    } ?: WindowPresentation.Unknown
}

private fun readWindowPresentation(window: NSWindow): WindowPresentation =
    if (!window.isVisible()) WindowPresentation.Unknown
    else if ((window.styleMask and NSWindowStyleMaskFullScreen) != 0uL)
        WindowPresentation.Fullscreen
    else WindowPresentation.Windowed

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class WindowStyleObserver(
    private val window: NSWindow,
    onChange: () -> Unit,
) : NSObject() {
    private val callback = StableRef.create(onChange)

    init {
        class_addMethod(
            object_getClass(this),
            NSSelectorFromString("observeValueForKeyPath:ofObject:change:context:"),
            staticCFunction(::styleChanged).reinterpret(),
            "v@:@@@^v",
        )
        window.addObserver(
            observer = this,
            forKeyPath = "styleMask",
            options = NSKeyValueObservingOptionNew,
            context = callback.asCPointer(),
        )
    }

    fun dispose() {
        window.removeObserver(
            observer = this,
            forKeyPath = "styleMask",
            context = callback.asCPointer(),
        )
        callback.dispose()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun styleChanged(
    self: COpaquePointer?,
    selector: COpaquePointer?,
    key: COpaquePointer?,
    obj: COpaquePointer?,
    change: COpaquePointer?,
    context: COpaquePointer?,
) {
    context?.asStableRef<() -> Unit>()?.get()?.invoke()
}
