package top.ltfan.m3x.compose.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.uikit.LocalUIView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIColor
import platform.UIKit.UICornerConfiguration
import platform.UIKit.UICornerRadius
import platform.UIKit.UIRectCornerBottomLeft
import platform.UIKit.UIRectCornerBottomRight
import platform.UIKit.UIRectCornerTopLeft
import platform.UIKit.UIRectCornerTopRight
import platform.UIKit.UIView
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIWindow
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable internal actual fun readPublicDisplayCornerRadii(): CornerRadii? = null

@Composable
internal actual fun readPublicWindowCornerRadii(): CornerRadii? {
    val host = currentUIViewOrNull() ?: return null
    val density = LocalDensity.current
    val state = remember(host, density) { mutableStateOf<CornerRadii?>(null) }
    DisposableEffect(host, density) {
        val tracker = WindowCornerTracker(host, density) { state.value = it }
        tracker.start()
        onDispose { tracker.dispose() }
    }
    return state.value
}

@OptIn(ExperimentalForeignApi::class)
@Composable
public actual fun rememberWindowPresentation(): WindowPresentation {
    val host = currentUIViewOrNull() ?: return WindowPresentation.Unknown
    val state = remember(host) { mutableStateOf(readWindowPresentation(host)) }
    DisposableEffect(host) {
        val observer = HostLayoutObserver { state.value = readWindowPresentation(host) }
        host.addSubview(observer)
        observer.setFrame(host.bounds)
        state.value = readWindowPresentation(host)
        onDispose { observer.removeFromSuperview() }
    }
    return state.value
}

@OptIn(InternalComposeApi::class)
@Composable
private fun currentUIViewOrNull(): UIView? {
    if (LocalInspectionMode.current) return null
    val composer = currentComposer
    return runCatching { composer.consume(LocalUIView) }.getOrNull()
}

@OptIn(ExperimentalForeignApi::class)
private fun readWindowPresentation(host: UIView): WindowPresentation {
    val hostBounds = host.bounds
    val hostSizeIsValid = hostBounds.useContents {
        size.width.isFinite() && size.height.isFinite() && size.width > 0.0 && size.height > 0.0
    }
    if (!hostSizeIsValid) {
        return WindowPresentation.Unknown
    }
    val window = host.window ?: return WindowPresentation.Unknown
    val scene = window.windowScene ?: return WindowPresentation.Unknown
    val screen = scene.screen
    val windowRect = window.convertRect(window.bounds, toCoordinateSpace = screen.coordinateSpace)
    val screenRect = screen.coordinateSpace.bounds
    if (!screen.scale.isFinite() || screen.scale <= 0.0) return WindowPresentation.Unknown
    val tolerance = 1.0 / screen.scale
    val fits = windowRect.useContents {
        val windowX = origin.x
        val windowY = origin.y
        val windowWidth = size.width
        val windowHeight = size.height
        val windowIsValid =
            listOf(windowX, windowY, windowWidth, windowHeight).all { it.isFinite() }
        screenRect.useContents {
            windowIsValid &&
                origin.x.isFinite() &&
                origin.y.isFinite() &&
                size.width.isFinite() &&
                size.height.isFinite() &&
                abs(windowX - origin.x) <= tolerance &&
                abs(windowY - origin.y) <= tolerance &&
                abs(windowWidth - size.width) <= tolerance &&
                abs(windowHeight - size.height) <= tolerance
        }
    }
    return if (fits) WindowPresentation.Fullscreen else WindowPresentation.Windowed
}

private fun Double.toDp(backingScale: Double, density: Density): Dp? {
    if (!isFinite() || this < 0.0 || !backingScale.isFinite() || backingScale <= 0.0) return null
    if (!density.density.isFinite() || density.density <= 0f) return null
    val converted = this * backingScale / density.density
    return converted.takeIf { it.isFinite() && it >= 0.0 }?.toFloat()?.takeIf { it.isFinite() }?.dp
}

@OptIn(ExperimentalForeignApi::class)
private fun supportsCornerConfiguration(): Boolean =
    NSProcessInfo.processInfo.operatingSystemVersion.useContents { majorVersion >= 26 }

private class WindowCornerTracker(
    private val host: UIView,
    private val density: Density,
    private val onCornersChanged: (CornerRadii?) -> Unit,
) {
    private var attachedWindow: UIWindow? = null
    private val observer = HostLayoutObserver(::refresh)
    private var probe: WindowCornerProbe? = null
    private var disposed = false

    @OptIn(ExperimentalForeignApi::class)
    fun start() {
        host.addSubview(observer)
        observer.setFrame(host.bounds)
        refresh()
    }

    fun dispose() {
        disposed = true
        observer.removeFromSuperview()
        probe?.removeFromSuperview()
        probe = null
        attachedWindow = null
        onCornersChanged(null)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun refresh() {
        if (disposed) return
        val window = host.window
        if (window != attachedWindow) {
            probe?.removeFromSuperview()
            attachedWindow = window
            probe =
                if (window != null && supportsCornerConfiguration()) WindowCornerProbe(::refresh)
                else null
            probe?.let {
                it.cornerConfiguration =
                    UICornerConfiguration.configurationWithRadius(
                        UICornerRadius.containerConcentricRadius(),
                    )
                window?.let { attached ->
                    it.setFrame(attached.bounds)
                    attached.addSubview(it)
                }
            }
        }
        val activeProbe = probe
        onCornersChanged(
            if (window != null && activeProbe != null) activeProbe.readCorners(window, density)
            else null,
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private class HostLayoutObserver(private val onLayout: () -> Unit) :
    UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    private var layoutUpdateScheduled = false

    init {
        userInteractionEnabled = false
        opaque = false
        backgroundColor = UIColor.clearColor
        autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        scheduleLayoutUpdate()
    }

    override fun didMoveToWindow() {
        super.didMoveToWindow()
        scheduleLayoutUpdate()
    }

    private fun scheduleLayoutUpdate() {
        if (layoutUpdateScheduled) return
        layoutUpdateScheduled = true
        dispatch_async(dispatch_get_main_queue()) {
            layoutUpdateScheduled = false
            onLayout()
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class WindowCornerProbe(private val onLayout: () -> Unit) :
    UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    private var refreshScheduled = false
    private var cachedCornerRadii: List<Double?> = List(4) { null }

    init {
        userInteractionEnabled = false
        opaque = false
        backgroundColor = UIColor.clearColor
        autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        updateCachedCornerRadii()
    }

    fun readCorners(hostWindow: UIWindow, density: Density): CornerRadii? {
        val corners = cachedCornerRadii
        if (corners.none { it != null }) return null
        val scale = hostWindow.windowScene?.screen?.scale ?: return null
        fun convert(value: Double?): Dp? = value?.toDp(scale, density)
        return CornerRadii(
            convert(corners[0]),
            convert(corners[1]),
            convert(corners[2]),
            convert(corners[3]),
        )
    }

    override fun updateProperties() {
        super.updateProperties()
        updateCachedCornerRadii()
    }

    private fun updateCachedCornerRadii() {
        val updated =
            listOf(
                    UIRectCornerTopLeft,
                    UIRectCornerTopRight,
                    UIRectCornerBottomRight,
                    UIRectCornerBottomLeft,
                )
                .map { corner ->
                    effectiveRadiusForCorner(corner).takeIf { it.isFinite() && it >= 0.0 }
                }
        if (updated != cachedCornerRadii) {
            cachedCornerRadii = updated
            scheduleRefresh()
        }
    }

    private fun scheduleRefresh() {
        if (refreshScheduled) return
        refreshScheduled = true
        dispatch_async(dispatch_get_main_queue()) {
            refreshScheduled = false
            if (window != null) onLayout()
        }
    }
}
