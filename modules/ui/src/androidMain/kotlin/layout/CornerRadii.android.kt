package top.ltfan.m3x.compose.ui.layout

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.RoundedCorner
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import kotlin.math.abs

@Composable
internal actual fun readPublicDisplayCornerRadii(): CornerRadii? {
    if (LocalInspectionMode.current) return null
    val view = LocalView.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val corners =
        remember(view, density, configuration) {
            mutableStateOf(readCornerRadii(view, density))
        }

    fun refresh() {
        corners.value = readCornerRadii(view, density)
    }
    DisposableEffect(view, density, configuration) {
        val mainHandler = Handler(Looper.getMainLooper())
        val context = view.context
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val displayListener =
            object : DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) = refresh()

                override fun onDisplayRemoved(displayId: Int) = refresh()

                override fun onDisplayChanged(displayId: Int) = refresh()
            }
        val attachListener =
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) = refresh()

                override fun onViewDetachedFromWindow(v: View) {
                    corners.value = null
                }
            }
        val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> refresh() }
        view.addOnAttachStateChangeListener(attachListener)
        view.addOnLayoutChangeListener(layoutListener)
        displayManager?.registerDisplayListener(displayListener, mainHandler)
        refresh()
        onDispose {
            view.removeOnAttachStateChangeListener(attachListener)
            view.removeOnLayoutChangeListener(layoutListener)
            displayManager?.unregisterDisplayListener(displayListener)
        }
    }
    return corners.value
}

private fun readCornerRadii(view: View, density: Density): CornerRadii? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val display = view.display ?: return null
    fun radius(position: Int): Dp? {
        val px = display.getRoundedCorner(position)?.radius ?: return null
        if (px < 0) return null
        return with(density) { px.toDp() }.takeIf { it.value.isFinite() && it.value >= 0f }
    }

    val result =
        CornerRadii(
            topLeft = radius(RoundedCorner.POSITION_TOP_LEFT),
            topRight = radius(RoundedCorner.POSITION_TOP_RIGHT),
            bottomRight = radius(RoundedCorner.POSITION_BOTTOM_RIGHT),
            bottomLeft = radius(RoundedCorner.POSITION_BOTTOM_LEFT),
        )
    return result.takeIf {
        it.topLeft != null || it.topRight != null || it.bottomRight != null || it.bottomLeft != null
    }
}

@Composable internal actual fun readPublicWindowCornerRadii(): CornerRadii? = null

@Composable
public actual fun rememberWindowPresentation(): WindowPresentation {
    if (LocalInspectionMode.current) return WindowPresentation.Unknown
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val state = remember(view, configuration) { mutableStateOf(readWindowPresentation(view)) }
    DisposableEffect(view, configuration) {
        val root = view.rootView
        val refresh = { state.value = readWindowPresentation(view) }
        val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> refresh() }
        val attachListener =
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) = refresh()

                override fun onViewDetachedFromWindow(v: View) {
                    state.value = WindowPresentation.Unknown
                }
            }
        root.addOnLayoutChangeListener(layoutListener)
        view.addOnAttachStateChangeListener(attachListener)
        refresh()
        onDispose {
            root.removeOnLayoutChangeListener(layoutListener)
            view.removeOnAttachStateChangeListener(attachListener)
        }
    }
    return state.value
}

private fun readWindowPresentation(view: View): WindowPresentation {
    if (!view.isAttachedToWindow || view.width <= 0 || view.height <= 0) {
        return WindowPresentation.Unknown
    }
    val activity = view.context.findActivity()
    if (
        (Build.VERSION.SDK_INT >= 24 && activity?.isInMultiWindowMode == true) ||
            (Build.VERSION.SDK_INT >= 26 && activity?.isInPictureInPictureMode == true)
    ) {
        return WindowPresentation.Windowed
    }
    val display = view.display ?: return WindowPresentation.Unknown
    val root = view.rootView
    if (root.width <= 0 || root.height <= 0) return WindowPresentation.Unknown
    val location = IntArray(2)
    root.getLocationOnScreen(location)
    val size = Point()
    @Suppress("DEPRECATION") display.getRealSize(size)
    if (size.x <= 0 || size.y <= 0) return WindowPresentation.Unknown
    val tolerance = 1
    return if (
        abs(location[0]) <= tolerance &&
            abs(location[1]) <= tolerance &&
            abs(root.width - size.x) <= tolerance &&
            abs(root.height - size.y) <= tolerance
    )
        WindowPresentation.Fullscreen
    else WindowPresentation.Windowed
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
