package top.ltfan.m3x.compose.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp

/** Physical corner radii reported by a display or window. */
@Immutable
public data class CornerRadii(
    public val topLeft: Dp? = null,
    public val topRight: Dp? = null,
    public val bottomRight: Dp? = null,
    public val bottomLeft: Dp? = null,
)

/** Indicates whether content occupies the complete display or a windowed area. */
public enum class WindowPresentation {
    Fullscreen,
    Windowed,
    Unknown,
}

/** A composable reader for the physical corner radii of a display or window. */
public typealias CornerRadiiHook = @Composable () -> CornerRadii?

/**
 * Overrides the display-corner reader in the current composition. A null hook uses the public
 * platform reader; a hook that returns null reports unknown radii.
 */
public val LocalDisplayCornerRadiiHook: ProvidableCompositionLocal<CornerRadiiHook?> =
    staticCompositionLocalOf {
        null
    }

/**
 * Overrides the window-corner reader in the current composition. A null hook uses the public
 * platform reader; a hook that returns null reports unknown radii.
 */
public val LocalWindowCornerRadiiHook: ProvidableCompositionLocal<CornerRadiiHook?> =
    staticCompositionLocalOf {
        null
    }

/** Reads physical display-corner radii when the platform exposes them. */
@Composable
public fun rememberDisplayCornerRadii(): CornerRadii? {
    val hook = LocalDisplayCornerRadiiHook.current
    return if (hook == null) readPublicDisplayCornerRadii() else hook()
}

/** Reads physical window-corner radii when the platform exposes them. */
@Composable
public fun rememberWindowCornerRadii(): CornerRadii? {
    val hook = LocalWindowCornerRadiiHook.current
    return if (hook == null) readPublicWindowCornerRadii() else hook()
}

@Composable internal expect fun readPublicDisplayCornerRadii(): CornerRadii?

@Composable internal expect fun readPublicWindowCornerRadii(): CornerRadii?

/** Reports the presentation state used to select display or window corner radii. */
@Composable public expect fun rememberWindowPresentation(): WindowPresentation
