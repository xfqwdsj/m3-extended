package top.ltfan.m3x.demo

import top.ltfan.backdrop.highlight.Highlight
import top.ltfan.backdrop.highlight.HighlightDirection
import top.ltfan.m3x.material.backdrop.BackdropPresets

/** Shared sources for the demo's adjustable Backdrop defaults. */
internal object DemoBackdropDefaults {
    val highlight = Highlight.Default
    val direction: HighlightDirection.Config = highlight.direction as HighlightDirection.Config

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
    const val EDGE_ALPHA = 0.35f

    val controlBlurRadius = BackdropPresets.GlassRegular.control.blurRadius.value
    val panelBlurRadius = BackdropPresets.GlassRegular.panel.blurRadius.value
    val surfaceBlurRadius = BackdropPresets.GlassRegular.surface.blurRadius.value
    val scrollEdgeBlurRadius = BackdropPresets.GlassRegular.scrollEdge.blurRadius.value
    const val SCROLL_EDGE_PLATEAU = 24.0f
    const val SCROLL_EDGE_FADE = 24.0f
    const val HDR_HEADROOM = 4.0f
    const val HDR_REFRESH_INTERVAL_MS = 200.0f
    const val HDR_WHITE_INTENSITY = 4.0f
    const val HDR_HIGHLIGHT_WHITE_INTENSITY = 2.0f

    val hdrHighlightWhite =
        demoHdrWhite(HDR_HIGHLIGHT_WHITE_INTENSITY).copy(alpha = highlight.style.color.alpha)
}
