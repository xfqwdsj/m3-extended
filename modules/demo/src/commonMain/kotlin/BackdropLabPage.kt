package top.ltfan.m3x.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import top.ltfan.m3x.backdrop.BackdropBlur
import top.ltfan.m3x.backdrop.BackdropColorControls
import top.ltfan.m3x.backdrop.BackdropFill
import top.ltfan.m3x.backdrop.BackdropLens
import top.ltfan.m3x.backdrop.BackdropStyle
import top.ltfan.m3x.backdrop.LocalBackdropHdrHeadroom
import top.ltfan.m3x.backdrop.highlight.Highlight
import top.ltfan.m3x.backdrop.highlight.HighlightDirection
import top.ltfan.m3x.backdrop.interaction.InteractionLighting
import top.ltfan.m3x.backdrop.interaction.ProvideInteractionLighting
import top.ltfan.m3x.backdrop.isRenderEffectSupported
import top.ltfan.m3x.backdrop.isRuntimeShaderSupported
import top.ltfan.m3x.backdrop.shadow.InnerShadow
import top.ltfan.m3x.backdrop.shadow.Shadow
import top.ltfan.m3x.compose.ui.visual.LocalBlurEnabled
import top.ltfan.m3x.datastore.MutableStore
import top.ltfan.m3x.demo.generated.resources.MR
import top.ltfan.m3x.demo.generated.resources.demo_lab_limited_effects
import top.ltfan.m3x.demo.generated.resources.demo_lab_scroll_hint
import top.ltfan.m3x.demo.generated.resources.demo_lab_surface_hint
import top.ltfan.m3x.demo.generated.resources.demo_material_surfaces
import top.ltfan.m3x.demo.generated.resources.demo_scroll_edges
import top.ltfan.m3x.material.Card
import top.ltfan.m3x.material.ProvidePlainMaterialRendering
import top.ltfan.m3x.material.backdrop.theme.BackdropAdaptation
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropLighting
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropTheme
import top.ltfan.m3x.material.bundle.M3xMaterialTheme
import top.ltfan.m3x.material.theme.M3xShapes
import top.ltfan.m3x.material.theming.M3xBaseTheme
import top.ltfan.m3x.material.theming.capsule.rememberCapsuleShapes
import top.ltfan.m3x.settingspage.SliderCommitMode
import top.ltfan.m3x.settingspage.material.SettingsPage

internal fun LabParameters.material(hdrAvailable: Boolean = false): M3xBackdropMaterial =
    M3xBackdropMaterial(
        tintAlpha = optics.tintAlpha,
        dimmingAlpha = optics.dimmingAlpha,
        adaptation =
            if (optics.adaptive) BackdropAdaptation.Background else BackdropAdaptation.Fixed,
        style =
            BackdropStyle.Default {
                vibrancy = { if (optics.vibrancy) 1.5f else 1f }
                colorControls = {
                    BackdropColorControls(optics.brightness, optics.contrast, optics.saturation)
                }
                lens = {
                    BackdropLens(
                        optics.refractionHeight.dp,
                        optics.refractionAmount.dp,
                        optics.depthEffect,
                        optics.chromaticAberration,
                    )
                }

                fill = { BackdropFill(Color.Unspecified) }
                interactionLighting = { InteractionLighting.Config(lighting.interiorAlpha) }
                blur = { BackdropBlur.Uniform(optics.blurRadius.dp) }
                highlight = {
                    if (!edge.highlight) Highlight.None
                    else
                        when (edge.highlightType) {
                            DemoHighlightType.Basic ->
                                Highlight.Basic(
                                    width = edge.highlightWidth.dp,
                                    colorGain =
                                        if (edge.hdrHighlight && hdrAvailable)
                                            DemoBackdropDefaults.HDR_HIGHLIGHT_COLOR_GAIN
                                        else edge.highlightColorGain,
                                    opacity = edge.highlightAlpha,
                                )
                            DemoHighlightType.Contour ->
                                Highlight(
                                    width = edge.highlightWidth.dp,
                                    blurRadius = edge.highlightBlurRadius.dp,
                                    colorGain =
                                        if (edge.hdrHighlight && hdrAvailable)
                                            DemoBackdropDefaults.HDR_HIGHLIGHT_COLOR_GAIN
                                        else edge.highlightColorGain,
                                    environmentGain = edge.highlightEnvironmentGain,
                                    opacity = edge.highlightAlpha,
                                    direction =
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
                                                            DemoBackdropDefaults.direction
                                                                .widthScale
                                                                .curve,
                                                ),
                                        ),
                                )
                        }
                }
                shadow = {
                    if (shadows.shadow)
                        Shadow(
                            radius = shadows.shadowRadius.dp,
                            color = Color.Black.copy(alpha = shadows.shadowOpacity),
                            offset = DpOffset(0.dp, shadows.shadowOffset.dp),
                        )
                    else Shadow.None
                }
                innerShadow = {
                    if (shadows.innerShadow)
                        InnerShadow(
                            radius = shadows.innerRadius.dp,
                            color = Color.Black.copy(alpha = shadows.innerOpacity),
                        )
                    else InnerShadow.None
                }
            },
    )

@Composable
internal fun BackdropLabPage(store: MutableStore<LabParameters>, hdrOutput: Boolean) {
    var showScrollEdges by rememberSaveable { mutableStateOf(false) }
    val params = store.value
    val describer = remember(store) { store.settingsDescriber() }
    val hdrAvailable =
        (hdrOutput || params.optics.hdrBackground) && LocalBackdropHdrHeadroom.current > 1.01f
    val backdrop =
        remember(params, hdrAvailable) {
            // Only the material under test and its interaction light are explicit; every other
            // role keeps the M3X default theme, including the scroll edge's ramp.
            M3xBackdropTheme(
                control = params.material(hdrAvailable),
                lighting =
                    M3xBackdropLighting(
                        enabled = params.lighting.lighting,
                        radius = params.lighting.lightRadius.dp,
                        color =
                            if (params.lighting.hdrInteraction && hdrAvailable)
                                demoHdrWhite(DemoBackdropDefaults.HDR_WHITE_INTENSITY)
                            else Color.White,
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
                    if (showScrollEdges) ScrollEdgeLabPreview(modifier)
                    else
                        GlassPreview(
                            modifier,
                            hdrBackground = hdrBackground,
                            backgroundContent = { LabPreviewSamples(Modifier.padding(16.dp)) },
                        ) {
                            MovableLabSurfaces(Modifier.fillMaxSize())
                        }
                }
            }
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
                        LabPreviewPanel(
                            showScrollEdges,
                            { showScrollEdges = it },
                            Modifier.fillMaxWidth().weight(1f),
                        ) {
                            preview(Modifier.fillMaxSize(), params.optics.hdrBackground)
                        }
                    }
                    SettingsPage(
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
                        LabPreviewPanel(
                            showScrollEdges,
                            { showScrollEdges = it },
                            Modifier.fillMaxWidth().height(300.dp),
                        ) {
                            preview(Modifier.fillMaxSize(), params.optics.hdrBackground)
                        }
                    }
                    SettingsPage(
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

/** Keeps preview navigation and scene-specific guidance within one bounded workspace. */
@Composable
internal fun LabPreviewPanel(
    scrollEdges: Boolean,
    onScrollEdgesChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    M3xBaseTheme(
        colorScheme = if (dark) darkColorScheme() else expressiveLightColorScheme(),
        shapes = remember { M3xShapes() },
        typography = remember { Typography() },
        motionScheme = remember { MotionScheme.expressive() },
        enterExitBlurRadius = 0.dp,
    ) {
        ProvidePlainMaterialRendering {
            Card(
                modifier = modifier.testTag("lab-preview"),
                backdrop = false,
            ) {
                SecondaryTabRow(
                    selectedTabIndex = if (scrollEdges) 1 else 0,
                    containerColor = Color.Transparent,
                    divider = {},
                ) {
                    listOf(MR.strings.demo_material_surfaces, MR.strings.demo_scroll_edges)
                        .forEachIndexed { index, label ->
                            Tab(
                                selected = scrollEdges == (index == 1),
                                onClick = { onScrollEdgesChange(index == 1) },
                                text = { Text(demoText(label)) },
                            )
                        }
                }
                Box(Modifier.fillMaxWidth().weight(1f)) { preview() }
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        demoText(
                            if (scrollEdges) MR.strings.demo_lab_scroll_hint
                            else MR.strings.demo_lab_surface_hint,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!isRenderEffectSupported() || !isRuntimeShaderSupported())
                        Text(
                            demoText(MR.strings.demo_lab_limited_effects),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                }
            }
        }
    }
}

/** Color boundaries and type specimens reveal blur and refraction without decorative copy. */
@Composable
internal fun LabPreviewSamples(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Aa 0123", style = MaterialTheme.typography.displayMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.onSurface,
                )
                .forEach { color ->
                    Box(
                        Modifier.weight(1f)
                            .height(32.dp)
                            .background(color, MaterialTheme.shapes.small),
                    )
                }
        }
        Text("Aa Bb Cc", style = MaterialTheme.typography.headlineSmall)
        Text("0123456789", style = MaterialTheme.typography.bodyLarge)
    }
}
