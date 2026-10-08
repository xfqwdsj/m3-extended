package top.ltfan.m3x.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import top.ltfan.backdrop.BackdropStyle
import top.ltfan.backdrop.LocalBackdropHdrHeadroom
import top.ltfan.backdrop.effects.colorControls
import top.ltfan.backdrop.effects.lens
import top.ltfan.backdrop.effects.vibrancy
import top.ltfan.backdrop.highlight.Highlight
import top.ltfan.backdrop.highlight.HighlightColorResponse
import top.ltfan.backdrop.highlight.HighlightDirection
import top.ltfan.backdrop.highlight.HighlightStyle
import top.ltfan.backdrop.interaction.ProvideInteractionLighting
import top.ltfan.backdrop.isRenderEffectSupported
import top.ltfan.backdrop.isRuntimeShaderSupported
import top.ltfan.backdrop.shadow.InnerShadow
import top.ltfan.backdrop.shadow.Shadow
import top.ltfan.m3x.compose.ui.visual.LocalBlurEnabled
import top.ltfan.m3x.datastore.MutableStore
import top.ltfan.m3x.demo.generated.resources.Res
import top.ltfan.m3x.demo.generated.resources.demo_drag_surfaces_to_move_drag_to_resize_parameters_apply_live_to_this_preview_only
import top.ltfan.m3x.demo.generated.resources.demo_render_effects
import top.ltfan.m3x.demo.generated.resources.demo_runtime_shaders
import top.ltfan.m3x.demo.generated.resources.demo_your_glass_live
import top.ltfan.m3x.material.backdrop.theme.BackdropAdaptation
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropLighting
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropTheme
import top.ltfan.m3x.material.bundle.M3xMaterialTheme
import top.ltfan.m3x.material.theming.capsule.rememberCapsuleShapes
import top.ltfan.m3x.settingspage.SliderCommitMode
import top.ltfan.m3x.settingspage.material.CardSettings

internal fun LabParameters.material(hdrAvailable: Boolean = false): M3xBackdropMaterial =
    M3xBackdropMaterial(
        blurRadius = optics.blurRadius.dp,
        tintAlpha = optics.tintAlpha,
        dimmingAlpha = optics.dimmingAlpha,
        adaptation =
            if (optics.adaptive) BackdropAdaptation.Background else BackdropAdaptation.Fixed,
        style =
            BackdropStyle(
                effects = {
                    if (optics.vibrancy) vibrancy()
                    colorControls(
                        brightness = optics.brightness,
                        contrast = optics.contrast,
                        saturation = optics.saturation,
                    )
                    lens(
                        optics.refractionHeight.dp.toPx(),
                        optics.refractionAmount.dp.toPx(),
                        optics.depthEffect,
                        optics.chromaticAberration,
                    )
                },
                highlight = {
                    if (edge.highlight)
                        Highlight(
                            width = edge.highlightWidth.dp,
                            opacity = edge.highlightAlpha,
                            direction =
                                if (edge.highlightType == DemoHighlightType.Plain)
                                    HighlightDirection.None
                                else
                                    HighlightDirection(
                                        angle = edge.highlightAngle,
                                        strength =
                                            DemoBackdropDefaults.direction.strength.copy(
                                                from = edge.highlightStrengthFrom,
                                                to = edge.highlightStrengthTo,
                                                curve =
                                                    if (edge.customHighlightStrengthCurve)
                                                        highlightStrengthCurve(
                                                            edge.highlightFalloff,
                                                        )
                                                    else
                                                        DemoBackdropDefaults.direction.strength
                                                            .curve,
                                            ),
                                        widthScale =
                                            DemoBackdropDefaults.direction.widthScale.copy(
                                                from = edge.highlightWidthFrom,
                                                to = edge.highlightWidthTo,
                                                curve =
                                                    if (edge.customHighlightWidthCurve)
                                                        highlightStrengthCurve(
                                                            edge.highlightWidthFalloff,
                                                        )
                                                    else
                                                        DemoBackdropDefaults.direction.widthScale
                                                            .curve,
                                            ),
                                    ),
                            style =
                                if (edge.highlight)
                                    edge.highlightType.style(
                                        color =
                                            if (edge.hdrHighlight && hdrAvailable)
                                                DemoBackdropDefaults.hdrHighlightWhite
                                            else null,
                                    )
                                else HighlightStyle.None,
                            colorResponse =
                                HighlightColorResponse(
                                    amount =
                                        if (edge.followBackdropColor) edge.backdropColorAmount
                                        else 0f,
                                ),
                        )
                    else Highlight.None
                },
                shadow = {
                    if (shadows.shadow)
                        Shadow(
                            radius = shadows.shadowRadius.dp,
                            color = Color.Black.copy(alpha = shadows.shadowOpacity),
                            offset = DpOffset(0.dp, shadows.shadowOffset.dp),
                        )
                    else Shadow.None
                },
                innerShadow = {
                    if (shadows.innerShadow)
                        InnerShadow(
                            radius = shadows.innerRadius.dp,
                            color = Color.Black.copy(alpha = shadows.innerOpacity),
                        )
                    else InnerShadow.None
                },
            ),
    )

@Composable
internal fun BackdropLabPage(store: MutableStore<LabParameters>, hdrOutput: Boolean) {
    val params = store.value
    val describer = remember(store) { store.settingsDescriber() }
    val hdrAvailable =
        (hdrOutput || params.optics.hdrBackground) && LocalBackdropHdrHeadroom.current > 1.01f
    val backdrop =
        remember(params, hdrAvailable) {
            val material = params.material(hdrAvailable)
            M3xBackdropTheme(
                control = material,
                panel =
                    material.copy(
                        adaptation = BackdropAdaptation.Fixed,
                        adaptiveContentColor = false,
                    ),
                lighting =
                    M3xBackdropLighting(
                        enabled = params.lighting.lighting,
                        radius = params.lighting.lightRadius.dp,
                        color =
                            if (params.lighting.hdrInteraction && hdrAvailable)
                                demoHdrWhite(DemoBackdropDefaults.HDR_WHITE_INTENSITY)
                            else Color.White,
                        interiorAlpha = params.lighting.interiorAlpha,
                        edgeAlpha = params.lighting.edgeAlpha,
                    ),
            )
        }
    val preview: @Composable (Modifier, Boolean) -> Unit = { modifier, hdrBackground ->
        CompositionLocalProvider(LocalBlurEnabled provides true) {
            M3xMaterialTheme(
                colorScheme = MaterialTheme.colorScheme,
                shapes = rememberCapsuleShapes(),
                backdrop = backdrop,
            ) {
                ProvideInteractionLighting {
                    GlassPreview(modifier, hdrBackground = hdrBackground) {
                        MovableLabSurfaces(Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
    val intro: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                demoText(Res.string.demo_your_glass_live),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                demoText(
                    Res.string
                        .demo_drag_surfaces_to_move_drag_to_resize_parameters_apply_live_to_this_preview_only,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${demoText(Res.string.demo_render_effects)}: ${isRenderEffectSupported()} · ${demoText(Res.string.demo_runtime_shaders)}: ${isRuntimeShaderSupported()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    DemoPageHost(width = DemoWideContentWidth) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= DemoLabWideBreakpoint
            if (wide)
                Row(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.weight(1f)
                            .fillMaxHeight()
                            .padding(
                                start = DemoGutter,
                                end = 12.dp,
                                top = LocalDemoTopInset.current + 16.dp,
                                bottom = DemoBottomInset,
                            ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        intro()
                        preview(Modifier.fillMaxWidth().weight(1f), params.optics.hdrBackground)
                    }
                    CardSettings(
                        describer,
                        Modifier.weight(1f).fillMaxHeight(),
                        sliderCommitMode = SliderCommitMode.WhileDragging,
                        contentPadding = DemoContentPadding,
                    )
                }
            else
                Column(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxWidth()
                            .padding(
                                start = DemoGutter,
                                end = DemoGutter,
                                top = LocalDemoTopInset.current + 16.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        intro()
                        preview(Modifier.fillMaxWidth().height(200.dp), params.optics.hdrBackground)
                    }
                    CardSettings(
                        describer,
                        Modifier.weight(1f),
                        sliderCommitMode = SliderCommitMode.WhileDragging,
                        contentPadding =
                            PaddingValues(
                                top = 16.dp,
                                bottom = DemoBottomInset,
                                start = DemoGutter,
                                end = DemoGutter,
                            ),
                    )
                }
        }
    }
}
