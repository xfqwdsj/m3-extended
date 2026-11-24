package top.ltfan.m3x.demo

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.ltfan.m3x.backdrop.BackdropBlur
import top.ltfan.m3x.backdrop.highlight.Highlight
import top.ltfan.m3x.backdrop.highlight.HighlightDirection
import top.ltfan.m3x.material.backdrop.BackdropPresets

/** Shared sources for the demo's adjustable Backdrop defaults. */
internal object DemoBackdropDefaults {
    val highlight = Highlight.Default
    val direction: HighlightDirection.Incidence =
        highlight.direction as HighlightDirection.Incidence

    const val BLUR_RADIUS = 4.0f
    const val REFRACTION_HEIGHT = 12.0f
    const val REFRACTION_AMOUNT = 24.0f
    const val TINT_ALPHA = 0.4f
    const val DIMMING_ALPHA = 0.0f
    const val BRIGHTNESS = 0.0f
    const val CONTRAST = 1.0f
    const val SATURATION = 1.0f

    const val HIGHLIGHT_FALLOFF = 1.0f
    const val HIGHLIGHT_WIDTH_FALLOFF = 3.0f

    const val SHADOW_RADIUS = 16.0f
    const val SHADOW_OPACITY = 0.1f
    const val SHADOW_OFFSET = 2.6666667f
    const val INNER_RADIUS = 24.0f
    const val INNER_OPACITY = 0.15f

    const val LIGHT_RADIUS = 160.0f
    const val INTERIOR_ALPHA = 0.15f

    val controlBlurRadius = BackdropPresets.GlassRegular.control.style.blur!!.invoke().radius.value
    val panelBlurRadius = BackdropPresets.GlassRegular.panel.style.blur!!.invoke().radius.value
    val surfaceBlurRadius = BackdropPresets.GlassRegular.surface.style.blur!!.invoke().radius.value
    val scrollEdgeBlurRadius =
        BackdropPresets.GlassRegular.scrollEdge.style.blur!!.invoke().radius.value
    const val SCROLL_EDGE_PLATEAU = 24.0f
    const val SCROLL_EDGE_FADE = 24.0f
    const val HDR_HEADROOM = 4.0f
    const val HDR_REFRESH_INTERVAL_MS = 200.0f
    const val HDR_WHITE_INTENSITY = 4.0f
    const val HDR_HIGHLIGHT_COLOR_GAIN = .06f
}

internal val BackdropBlur.radius: Dp
    get() =
        when (this) {
            BackdropBlur.None -> 0.dp
            is BackdropBlur.Uniform -> radius
            is BackdropBlur.Progressive -> radius
            is BackdropBlur.LayeredProgressive -> radius
        }
