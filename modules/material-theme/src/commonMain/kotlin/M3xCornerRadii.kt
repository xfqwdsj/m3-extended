package top.ltfan.m3x.material.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.ltfan.m3x.compose.ui.layout.CornerRadii
import top.ltfan.m3x.compose.ui.layout.WindowPresentation
import top.ltfan.m3x.compose.ui.layout.rememberDisplayCornerRadii
import top.ltfan.m3x.compose.ui.layout.rememberWindowCornerRadii
import top.ltfan.m3x.compose.ui.layout.rememberWindowPresentation

/** Selects which physical corner measurements shape container surfaces. */
public enum class CornerRadiiPolicy {
    /** Selects window corners except when a mobile app occupies the display. */
    Auto,
    /** Selects display corners. */
    Display,
    /** Selects window corners. */
    Window,
}

/** Identifies the measurements used to derive [M3xCornerRadii.containerRadius]. */
public enum class CornerRadiiSource {
    Display,
    Window,
    Fallback,
}

/** Radius values and source measurements used by the M3X shape scale. */
@Immutable
public data class M3xCornerRadii(
    public val extraSmall: Dp = 4.dp,
    public val small: Dp = 8.dp,
    public val medium: Dp = 12.dp,
    public val large: Dp = 16.dp,
    public val extraLarge: Dp = 28.dp,
    public val largeIncreased: Dp = 20.dp,
    public val extraLargeIncreased: Dp = 32.dp,
    public val extraExtraLarge: Dp = 48.dp,
    public val displayCorners: CornerRadii? = null,
    public val windowCorners: CornerRadii? = null,
    public val policy: CornerRadiiPolicy = CornerRadiiPolicy.Auto,
    public val windowPresentation: WindowPresentation = WindowPresentation.Unknown,
    public val containerRadius: Dp = 32.dp,
    private val mobilePlatform: Boolean = isMobilePlatform(),
) {
    private val selectedCorners: CornerRadii?
        get() =
            when (policy) {
                CornerRadiiPolicy.Display -> displayCorners
                CornerRadiiPolicy.Window -> windowCorners
                CornerRadiiPolicy.Auto ->
                    if (mobilePlatform && windowPresentation == WindowPresentation.Fullscreen) {
                        displayCorners
                    } else {
                        windowCorners
                    }
            }

    /**
     * Measurement source selected by [policy], or [CornerRadiiSource.Fallback] when unavailable.
     */
    public val source: CornerRadiiSource
        get() =
            when {
                selectedCorners.usableValues().isEmpty() -> CornerRadiiSource.Fallback
                policy == CornerRadiiPolicy.Display ||
                    (policy == CornerRadiiPolicy.Auto &&
                        mobilePlatform &&
                        windowPresentation == WindowPresentation.Fullscreen) ->
                    CornerRadiiSource.Display
                else -> CornerRadiiSource.Window
            }

    /** Physical corner values used by the container shape. */
    public val resolvedContainerCorners: CornerRadii
        get() {
            val raw = selectedCorners
            val known = raw.usableValues()
            if (known.isEmpty()) {
                return CornerRadii(
                    containerRadius,
                    containerRadius,
                    containerRadius,
                    containerRadius,
                )
            }
            val largest = known.maxBy { it.value }
            fun resolve(value: Dp?): Dp = value?.takeIf { it.isUsableRadius() } ?: largest
            return CornerRadii(
                topLeft = resolve(raw?.topLeft),
                topRight = resolve(raw?.topRight),
                bottomRight = resolve(raw?.bottomRight),
                bottomLeft = resolve(raw?.bottomLeft),
            )
        }

    /** Radius used for shapes without rounded corners. */
    public val none: Dp
        get() = 0.dp
}

/** Fixed baseline values for the expressive M3X theme. */
public val ExpressiveCornerRadii: M3xCornerRadii = M3xCornerRadii()

/** Resolves display and window measurements into a container radius scale. */
public fun adaptiveCornerRadii(
    displayCorners: CornerRadii?,
    windowCorners: CornerRadii?,
    windowPresentation: WindowPresentation,
    policy: CornerRadiiPolicy = CornerRadiiPolicy.Auto,
    isMobile: Boolean = isMobilePlatform(),
    fallbackContainerRadius: Dp = 32.dp,
): M3xCornerRadii {
    require(fallbackContainerRadius.value.isFinite() && fallbackContainerRadius.value > 0f) {
        "fallbackContainerRadius must be finite and positive."
    }
    val selection =
        when (policy) {
            CornerRadiiPolicy.Display -> CornerRadiiSource.Display to displayCorners
            CornerRadiiPolicy.Window -> CornerRadiiSource.Window to windowCorners
            CornerRadiiPolicy.Auto ->
                if (isMobile && windowPresentation == WindowPresentation.Fullscreen) {
                    CornerRadiiSource.Display to displayCorners
                } else {
                    CornerRadiiSource.Window to windowCorners
                }
        }
    val raw = selection.second
    val known = raw.usableValues()
    val usable = known.isNotEmpty()
    val radius = if (!usable) fallbackContainerRadius else known.maxOf { it.value }.dp
    val scale = (radius.value / 32f).coerceIn(0.75f, 1.5f)
    return M3xCornerRadii(
        extraSmall = 4.dp * scale,
        small = 8.dp * scale,
        medium = 12.dp * scale,
        large = 16.dp * scale,
        extraLarge = 28.dp * scale,
        largeIncreased = 20.dp * scale,
        extraLargeIncreased = 32.dp * scale,
        extraExtraLarge = 48.dp * scale,
        displayCorners = displayCorners,
        windowCorners = windowCorners,
        policy = policy,
        windowPresentation = windowPresentation,
        containerRadius = radius,
        mobilePlatform = isMobile,
    )
}

/** Remembers shape radii adapted to the selected display or window measurements. */
@Composable
public fun rememberAdaptiveCornerRadii(
    policy: CornerRadiiPolicy = CornerRadiiPolicy.Auto,
    fallbackContainerRadius: Dp = 32.dp,
): M3xCornerRadii {
    val display = rememberDisplayCornerRadii()
    val window = rememberWindowCornerRadii()
    val presentation = rememberWindowPresentation()
    return remember(display, window, presentation, policy, fallbackContainerRadius) {
        adaptiveCornerRadii(
            display,
            window,
            presentation,
            policy,
            isMobilePlatform(),
            fallbackContainerRadius,
        )
    }
}

private fun CornerRadii?.usableValues(): List<Dp> =
    listOfNotNull(this?.topLeft, this?.topRight, this?.bottomRight, this?.bottomLeft).filter {
        it.isUsableRadius()
    }

private fun Dp?.isUsableRadius(): Boolean = this != null && value.isFinite() && value >= 0f

internal expect fun isMobilePlatform(): Boolean
