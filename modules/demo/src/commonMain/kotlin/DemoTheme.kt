package top.ltfan.m3x.demo

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.ltfan.m3x.backdrop.BackdropBlur
import top.ltfan.m3x.backdrop.BackdropLens
import top.ltfan.m3x.backdrop.BackdropRamp
import top.ltfan.m3x.backdrop.BackdropRampAnchors
import top.ltfan.m3x.backdrop.highlight.Highlight
import top.ltfan.m3x.backdrop.highlight.HighlightDirection
import top.ltfan.m3x.backdrop.shadow.InnerShadow
import top.ltfan.m3x.backdrop.shadow.Shadow
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
    val base =
        when (appearance.preset) {
            MaterialPreset.Regular -> BackdropPresets.GlassRegular
            MaterialPreset.Blur -> BackdropPresets.MaterialBlur
            MaterialPreset.Clear -> BackdropPresets.GlassClear
        }
    val backdrop =
        if (effects == DemoPreferences.Effects() && !interactionHdr && !hdrHighlightAvailable) base
        else
            base.copy(
                control =
                    base.control.copy(
                        style =
                            base.control.style {
                                vibrancy = {
                                    if (
                                        effects.vibrancy && appearance.preset != MaterialPreset.Blur
                                    )
                                        1.5f
                                    else 1f
                                }
                                lens = {
                                    if (effects.lens && appearance.preset != MaterialPreset.Blur)
                                        BackdropLens(12.dp, 24.dp)
                                    else BackdropLens.None
                                }

                                blur = {
                                    BackdropBlur.Uniform(
                                        if (effects.customBlurRadii) effects.controlBlurRadiusDp.dp
                                        else base.control.style.blur!!.invoke().radius,
                                    )
                                }
                                highlight = {
                                    effects.backdropHighlight(hdrHighlightAvailable)
                                }
                                shadow = {
                                    if (effects.shadow) base.control.style.shadow!!.invoke()
                                    else Shadow.None
                                }
                                innerShadow = {
                                    if (effects.innerShadow)
                                        base.control.style.innerShadow!!.invoke()
                                    else InnerShadow.None
                                }
                            },
                        adaptiveContentColor = effects.adaptation && effects.adaptiveContentColor,
                        adaptation =
                            if (effects.adaptation) BackdropAdaptation.Background
                            else BackdropAdaptation.Fixed,
                    ),
                panel =
                    base.panel
                        .configuredPanel(
                            effects,
                            appearance.preset,
                            hdrHighlightAvailable,
                        )
                        .withBlur(
                            effects.customBlurRadii,
                            effects.panelBlurRadiusDp.dp,
                        )
                        .copy(
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
                            hdrHighlightAvailable,
                        )
                        .withBlur(
                            effects.customBlurRadii,
                            effects.panelBlurRadiusDp.dp,
                        )
                        .copy(
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
                        style =
                            base.scrollEdge.style {
                                blur = {
                                    BackdropBlur.Uniform(
                                        if (effects.customBlurRadii)
                                            effects.scrollEdgeBlurRadiusDp.dp
                                        else base.scrollEdge.style.blur!!.invoke().radius,
                                    )
                                }
                            },
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

private fun M3xBackdropMaterial.withBlur(
    customBlur: Boolean,
    radius: Dp,
): M3xBackdropMaterial =
    copy(
        style =
            style {
                blur = {
                    BackdropBlur.Uniform(
                        if (customBlur) radius else style.blur!!.invoke().radius,
                    )
                }
            },
    )

private fun M3xBackdropMaterial.configuredPanel(
    settings: DemoPreferences.Effects,
    preset: MaterialPreset,
    hdrHighlightAvailable: Boolean,
): M3xBackdropMaterial =
    copy(
        style =
            style {
                vibrancy = { if (settings.vibrancy && preset != MaterialPreset.Blur) 1.5f else 1f }
                lens = {
                    if (settings.lens && preset != MaterialPreset.Blur)
                        BackdropLens(24.dp, 24.dp, depthEffect = true)
                    else BackdropLens.None
                }

                highlight = {
                    settings.backdropHighlight(hdrHighlightAvailable)
                }
                shadow = { if (settings.shadow) style.shadow!!.invoke() else Shadow.None }
                innerShadow = {
                    if (settings.innerShadow) style.innerShadow!!.invoke() else InnerShadow.None
                }
            },
    )

private fun DemoPreferences.Effects.backdropHighlight(hdrHighlightAvailable: Boolean): Highlight =
    if (!highlight) Highlight.None
    else {
        val colorGain =
            if (hdrHighlightAvailable) DemoBackdropDefaults.HDR_HIGHLIGHT_COLOR_GAIN
            else Highlight.Default.colorGain
        when (highlightType) {
            DemoHighlightType.Basic -> Highlight.Basic(colorGain = colorGain)
            DemoHighlightType.Contour ->
                Highlight(
                    colorGain = colorGain,
                    direction = HighlightDirection(),
                )
        }
    }
