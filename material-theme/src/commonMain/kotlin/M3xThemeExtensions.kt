package top.ltfan.m3x.material.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.ltfan.m3x.compose.ui.accessibility.AccessibilityPreferences
import top.ltfan.m3x.compose.ui.visual.DefaultEnterExitBlurRadius

/** Design-system extension values resolved at a theme position. */
@Immutable
public data class M3xThemeExtensions(
    public val shapes: M3xShapeExtensions = M3xShapeExtensions(),
    public val accessibilityPreferences: AccessibilityPreferences = AccessibilityPreferences(),
    /** Maximum foreground defocus during entrance and exit; zero retains only fading. */
    public val enterExitBlurRadius: Dp = DefaultEnterExitBlurRadius,
) {
    init {
        require(enterExitBlurRadius.value.isFinite() && enterExitBlurRadius >= 0.dp) {
            "enterExitBlurRadius must be finite and nonnegative"
        }
    }
}

/** The provided M3X values, or `null` before an extended theme is installed. */
public val LocalM3xThemeExtensions: ProvidableCompositionLocal<M3xThemeExtensions?> =
    staticCompositionLocalOf {
        null
    }

/** The M3X extension values at the current position in the composition. */
@Suppress("UnusedReceiverParameter")
public val MaterialTheme.extended: M3xThemeExtensions
    @Composable @ReadOnlyComposable get() = LocalM3xThemeExtensions.current ?: M3xThemeExtensions()
