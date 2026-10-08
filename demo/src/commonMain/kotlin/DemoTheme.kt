package top.ltfan.m3x.demo

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.ltfan.backdrop.BackdropRamp
import top.ltfan.backdrop.BackdropRampAnchors
import top.ltfan.backdrop.effects.lens
import top.ltfan.backdrop.effects.vibrancy
import top.ltfan.backdrop.highlight.Highlight
import top.ltfan.backdrop.highlight.HighlightColorResponse
import top.ltfan.backdrop.highlight.HighlightDirection
import top.ltfan.backdrop.shadow.InnerShadow
import top.ltfan.backdrop.shadow.Shadow
import top.ltfan.m3x.compose.ui.accessibility.AccessibilityPreference
import top.ltfan.m3x.compose.ui.accessibility.AccessibilityPreferences
import top.ltfan.m3x.material.backdrop.BackdropPresets
import top.ltfan.m3x.material.backdrop.theme.BackdropAdaptation
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropTheme

internal fun DemoPreferences.themeBackdrop(
    interactionHdr: Boolean = false,
    hdrHighlightAvailable: Boolean = false,
): M3xBackdropTheme {
    val highlightColor = if (hdrHighlightAvailable) DemoBackdropDefaults.hdrHighlightWhite else null
    val base =
        when (appearance.preset) {
            MaterialPreset.Regular -> BackdropPresets.GlassRegular
            MaterialPreset.Blur -> BackdropPresets.MaterialBlur
            MaterialPreset.Clear -> BackdropPresets.GlassClear
        }
    val backdrop =
        if (effects == DemoPreferences.Effects() && !hdrHighlightAvailable) base
        else
            base.copy(
                control =
                    base.control.copy(
                        blurRadius =
                            if (effects.customBlurRadii) effects.controlBlurRadiusDp.dp
                            else base.control.blurRadius,
                        adaptiveContentColor = effects.adaptation && effects.adaptiveContentColor,
                        style =
                            base.control.style.copy(
                                effects = {
                                    if (appearance.preset != MaterialPreset.Blur) {
                                        if (effects.vibrancy) vibrancy()
                                        if (effects.lens && shape is CornerBasedShape)
                                            lens(12.dp.toPx(), 24.dp.toPx())
                                    }
                                },
                                highlight = {
                                    effects.backdropHighlight(highlightColor)
                                },
                                shadow = {
                                    if (effects.shadow) base.control.style.shadow() else Shadow.None
                                },
                                innerShadow = {
                                    if (effects.innerShadow) base.control.style.innerShadow()
                                    else InnerShadow.None
                                },
                            ),
                        adaptation =
                            if (effects.adaptation) BackdropAdaptation.Background
                            else BackdropAdaptation.Fixed,
                    ),
                panel =
                    base.panel
                        .configuredPanel(
                            effects,
                            appearance.preset,
                            highlightColor,
                        )
                        .copy(
                            blurRadius =
                                if (effects.customBlurRadii) effects.panelBlurRadiusDp.dp
                                else base.panel.blurRadius,
                            adaptation =
                                if (effects.adaptation) BackdropAdaptation.Background
                                else BackdropAdaptation.Fixed,
                            adaptiveContentColor =
                                effects.adaptation && effects.adaptiveContentColor,
                        ),
                surface =
                    base.surface
                        .configuredSurface(effects, appearance.preset)
                        .copy(
                            blurRadius =
                                if (effects.customBlurRadii) effects.surfaceBlurRadiusDp.dp
                                else base.surface.blurRadius,
                            adaptation =
                                if (effects.adaptation) BackdropAdaptation.Background
                                else BackdropAdaptation.Fixed,
                            adaptiveContentColor =
                                effects.adaptation && effects.adaptiveContentColor,
                        ),
                inversePanel =
                    base.inversePanel
                        .configuredPanel(
                            effects,
                            appearance.preset,
                            highlightColor,
                        )
                        .copy(
                            blurRadius =
                                if (effects.customBlurRadii) effects.panelBlurRadiusDp.dp
                                else base.inversePanel.blurRadius,
                            adaptation = BackdropAdaptation.Fixed,
                            adaptiveContentColor = false,
                        ),
                lighting =
                    base.lighting.copy(
                        enabled = effects.interaction,
                        color =
                            if (interactionHdr)
                                demoHdrWhite(DemoBackdropDefaults.HDR_WHITE_INTENSITY)
                            else base.lighting.color,
                    ),
                scrollEdge =
                    base.scrollEdge.copy(
                        blurRadius =
                            if (effects.customBlurRadii) effects.scrollEdgeBlurRadiusDp.dp
                            else base.scrollEdge.blurRadius,
                        ramp =
                            BackdropRamp(
                                anchors =
                                    BackdropRampAnchors.offsets(
                                        plateau = effects.scrollEdgePlateauDp.dp,
                                        fade = effects.scrollEdgeFadeDp.dp,
                                    ),
                            ),
                    ),
            )
    return backdrop
}

internal fun DemoPreferences.themeAccessibility(): AccessibilityPreferences =
    AccessibilityPreferences(
        reduceTransparency = accessibility.reduceTransparency.toLibraryPreference(),
        increaseContrast = accessibility.increaseContrast.toLibraryPreference(),
        reduceMotion = accessibility.reduceMotion.toLibraryPreference(),
    )

private fun AccessibilityMode.toLibraryPreference(): AccessibilityPreference =
    when (this) {
        AccessibilityMode.System -> AccessibilityPreference.System
        AccessibilityMode.Enabled -> AccessibilityPreference.Enabled
        AccessibilityMode.Disabled -> AccessibilityPreference.Disabled
    }

private fun M3xBackdropMaterial.configuredSurface(
    settings: DemoPreferences.Effects,
    preset: MaterialPreset,
): M3xBackdropMaterial =
    configuredPanel(settings, preset)
        .copy(
            style =
                style.copy(
                    highlight = { Highlight.None },
                    shadow = { Shadow.None },
                    innerShadow = { InnerShadow.None },
                ),
        )

private fun M3xBackdropMaterial.configuredPanel(
    settings: DemoPreferences.Effects,
    preset: MaterialPreset,
    highlightColor: Color? = null,
): M3xBackdropMaterial =
    copy(
        style =
            style.copy(
                effects = {
                    if (preset != MaterialPreset.Blur) {
                        if (settings.vibrancy) vibrancy()
                        if (settings.lens && shape is CornerBasedShape)
                            lens(24.dp.toPx(), 24.dp.toPx(), depthEffect = true)
                    }
                },
                highlight = {
                    settings.backdropHighlight(highlightColor)
                },
                shadow = { if (settings.shadow) style.shadow() else Shadow.None },
                innerShadow = {
                    if (settings.innerShadow) style.innerShadow() else InnerShadow.None
                },
            ),
    )

private fun DemoPreferences.Effects.backdropHighlight(color: Color? = null): Highlight =
    if (!highlight) Highlight.None
    else
        Highlight(
            direction =
                if (highlightType == DemoHighlightType.Plain) HighlightDirection.None
                else HighlightDirection(),
            style = highlightType.style(color),
            colorResponse =
                if (followBackdropColor) HighlightColorResponse(amount = backdropColorAmount)
                else HighlightColorResponse(amount = 0f),
        )
