package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.ltfan.backdrop.BackdropStyle
import top.ltfan.backdrop.RuntimeShader
import top.ltfan.backdrop.highlight.Highlight
import top.ltfan.backdrop.highlight.HighlightDirection
import top.ltfan.backdrop.highlight.HighlightEmission
import top.ltfan.backdrop.highlight.HighlightSample
import top.ltfan.backdrop.highlight.HighlightStyle
import top.ltfan.backdrop.interaction.InteractionLight
import top.ltfan.backdrop.interaction.interactionLightSource
import top.ltfan.backdrop.shadow.Shadow
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.compose.ui.accessibility.LocalSystemAccessibilitySettingsHook
import top.ltfan.m3x.compose.ui.accessibility.SystemAccessibilitySettings
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.backdrop.theme.BackdropAdaptation
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropLighting
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropTheme
import top.ltfan.m3x.material.bundle.M3xMaterialTheme
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.overlay.MaterialOverlay
import top.ltfan.m3x.overlay.OverlayHandle
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState

@Stable
private class DynamicShaderHighlightStyle : HighlightStyle {
    var green by mutableStateOf(false)
    var level by mutableStateOf(.25f)

    override val color: Color = Color.White.copy(alpha = .75f)

    override val shaderSource: String
        get() {
            val rgb =
                if (green) "half4(0.0, dynamicLevel, 0.0, 0.75)"
                else "half4(dynamicLevel, 0.0, 0.0, 0.75)"
            return "uniform float dynamicLevel; half4 shadeHighlight(float2 position, float2 outwardNormal, float projection, half4 color) { return $rgb; } float highlightStrength(float2 position, float2 outwardNormal, float projection) { return 1.0; }"
        }

    override fun shade(sample: HighlightSample): HighlightEmission =
        HighlightEmission(if (green) Color(0f, level, 0f, .75f) else Color(level, 0f, 0f, .75f))

    override fun configureShader(shader: RuntimeShader) {
        shader.setFloatUniform("dynamicLevel", level)
    }
}

private fun assertImagesClose(first: PixelMap, second: PixelMap) {
    assertTrue(first.width == second.width && first.height == second.height)
    for (y in 0 until first.height) for (x in 0 until first.width) {
        val a = first[x, y]
        val b = second[x, y]
        assertTrue(
            abs(a.red - b.red) < .005f &&
                abs(a.green - b.green) < .005f &&
                abs(a.blue - b.blue) < .005f,
            "CPU and GPU pixels agree at ($x, $y): $a vs $b",
        )
    }
}

class SharedLightRenderingTest {
    private fun material(highlight: Highlight, interactive: Boolean = true) =
        M3xBackdropMaterial(
            style =
                BackdropStyle(
                    effects = BackdropStyle.NoEffects,
                    highlight = { highlight },
                    shadow = { Shadow.None },
                ),
            blurRadius = 0.dp,
            tintAlpha = 1f,
            adaptation = BackdropAdaptation.Fixed,
            interactiveLighting = interactive,
        )

    @Test
    fun interiorGlowCrossesComponentsAndFadesContinuously() = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        val interactions = MutableInteractionSource()
        val press = PressInteraction.Press(Offset(60f, 32f))
        lateinit var scope: CoroutineScope
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    val source = rememberBackdropLayer(Color.Black)
                    val coroutineScope = rememberCoroutineScope()
                    SideEffect { scope = coroutineScope }
                    BackdropHost(source) {
                        Box(Modifier.size(160.dp, 80.dp).background(Color.Black)) {
                            Box(Modifier.fillMaxSize().captureBackdrop(source))
                            BackdropSurface(
                                Modifier.size(64.dp),
                                RectangleShape,
                                material = material(Highlight.None),
                                containerColor = Color.Black,
                                interactionSource = interactions,
                            ) {}
                            BackdropSurface(
                                Modifier.offset(x = 76.dp).size(64.dp).testTag("target"),
                                RectangleShape,
                                material = material(Highlight.None),
                                containerColor = Color.Black,
                            ) {}
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(32)
        fun brightness(): Float {
            val pixels = onNodeWithTag("target").captureToImage().toPixelMap()
            return pixels[pixels.width / 2, pixels.height / 2].red
        }
        val before = brightness()
        runOnIdle { scope.launch { interactions.emit(press) } }
        mainClock.advanceTimeBy(32)
        val pressed = brightness()
        runOnIdle { scope.launch { interactions.emit(PressInteraction.Release(press)) } }
        mainClock.advanceTimeBy(64)
        val fading = brightness()
        mainClock.advanceTimeBy(240)
        val released = brightness()
        assertTrue(pressed > before + 0.02f)
        assertTrue(fading in (before + 0.001f)..(pressed - 0.001f))
        assertTrue(abs(released - before) < 0.005f)
    }

    @Test
    fun reduceMotionReleasesSharedLightImmediately() = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        val interactions = MutableInteractionSource()
        val press = PressInteraction.Press(Offset(60f, 32f))
        lateinit var scope: CoroutineScope
        setContent {
            ProvideM3xMaterialRendering {
                CompositionLocalProvider(
                    LocalSystemAccessibilitySettingsHook provides
                        {
                            SystemAccessibilitySettings(reduceMotion = true)
                        },
                ) {
                    M3xMaterialTheme(backdrop = M3xBackdropTheme()) {
                        val source = rememberBackdropLayer(Color.Black)
                        val coroutineScope = rememberCoroutineScope()
                        SideEffect { scope = coroutineScope }
                        BackdropHost(source) {
                            Box(Modifier.size(160.dp, 80.dp).background(Color.Black)) {
                                Box(Modifier.fillMaxSize().captureBackdrop(source))
                                BackdropSurface(
                                    Modifier.size(64.dp),
                                    RectangleShape,
                                    material = material(Highlight.None),
                                    containerColor = Color.Black,
                                    interactionSource = interactions,
                                ) {}
                                BackdropSurface(
                                    Modifier.offset(x = 76.dp).size(64.dp).testTag("target"),
                                    RectangleShape,
                                    material = material(Highlight.None),
                                    containerColor = Color.Black,
                                ) {}
                            }
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(32)
        fun brightness(): Float {
            val pixels = onNodeWithTag("target").captureToImage().toPixelMap()
            return pixels[pixels.width / 2, pixels.height / 2].red
        }
        val before = brightness()
        runOnIdle { scope.launch { interactions.emit(press) } }
        mainClock.advanceTimeBy(32)
        val pressed = brightness()
        runOnIdle { scope.launch { interactions.emit(PressInteraction.Release(press)) } }
        mainClock.advanceTimeBy(16)
        val released = brightness()

        assertTrue(pressed > before + 0.02f)
        assertTrue(abs(released - before) < 0.005f)
    }

    @Test
    fun sharedGlowKeepsSourceColorAndWindowRadiusUnderTransforms() = runInitializedComposeUiTest {
        val interactions = MutableInteractionSource()
        lateinit var scope: CoroutineScope
        var transformed by mutableStateOf(false)
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    val backdrop = rememberBackdropLayer(Color.Black)
                    val coroutineScope = rememberCoroutineScope()
                    SideEffect { scope = coroutineScope }
                    BackdropHost(backdrop) {
                        Box(Modifier.size(256.dp, 160.dp).background(Color.Black).testTag("root")) {
                            Box(Modifier.fillMaxSize().captureBackdrop(backdrop))
                            Box(
                                Modifier.size(64.dp).interactionLightSource(interactions) {
                                    InteractionLight.Config(color = Color.Red)
                                },
                            )
                            BackdropSurface(
                                Modifier.offset(x = 76.dp).size(64.dp).graphicsLayer {
                                    transformOrigin = TransformOrigin(0f, 0f)
                                    scaleX = if (transformed) 2f else 1f
                                    rotationZ = if (transformed) 30f else 0f
                                },
                                RectangleShape,
                                material = material(Highlight.None),
                                containerColor = Color.Black,
                            ) {}
                        }
                    }
                }
            }
        }
        runOnIdle { scope.launch { interactions.emit(PressInteraction.Press(Offset(32f, 32f))) } }
        fun pixel(): Color = onNodeWithTag("root").captureToImage().toPixelMap()[108, 32]
        val original = pixel()
        runOnIdle { transformed = true }
        val transformedPixel = pixel()
        assertTrue(original.red > 0.03f && original.blue < 0.005f)
        assertTrue(abs(original.red - transformedPixel.red) < 0.01f)
        assertTrue(transformedPixel.blue < 0.005f)
    }

    @Test
    fun receiverOnlySurfaceGetsSharedLight() = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        val interactions = MutableInteractionSource()
        val press = PressInteraction.Press(Offset(60f, 32f))
        lateinit var scope: CoroutineScope
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    val source = rememberBackdropLayer(Color.Black)
                    val coroutineScope = rememberCoroutineScope()
                    SideEffect { scope = coroutineScope }
                    BackdropHost(source) {
                        Box(Modifier.size(160.dp, 80.dp).background(Color.Black)) {
                            Box(Modifier.fillMaxSize().captureBackdrop(source))
                            BackdropSurface(
                                Modifier.size(64.dp),
                                RectangleShape,
                                material = material(Highlight.None),
                                containerColor = Color.Black,
                                interactionSource = interactions,
                            ) {}
                            val receiver =
                                rememberMaterialAppearance(
                                    role = MaterialSurfaceRole.Panel,
                                    material = material(Highlight.None),
                                    containerColor = Color.Black,
                                )
                            Box(
                                Modifier.offset(x = 76.dp)
                                    .size(64.dp)
                                    .testTag("target")
                                    .materialBackdropSurface(
                                        receiver,
                                        RectangleShape,
                                        observeInput = false,
                                    ),
                            )
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(32)
        fun brightness(): Float {
            val pixels = onNodeWithTag("target").captureToImage().toPixelMap()
            return pixels[pixels.width / 2, pixels.height / 2].red
        }
        val before = brightness()
        runOnIdle { scope.launch { interactions.emit(press) } }
        mainClock.advanceTimeBy(32)
        val after = brightness()
        assertTrue(after > before + 0.02f)
    }

    @Test
    fun globalAndRoleGatesDisableBothSourceAndReceiver() = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        val interactions = MutableInteractionSource()
        lateinit var scope: CoroutineScope
        var sourceEnabled by mutableStateOf(true)
        var receiverEnabled by mutableStateOf(true)
        var globalEnabled by mutableStateOf(true)

        setContent {
            ProvideM3xMaterialRendering {
                val backdrop =
                    M3xBackdropTheme(
                        control = material(Highlight.None, sourceEnabled),
                        panel = material(Highlight.None, receiverEnabled),
                        lighting =
                            M3xBackdropLighting(
                                enabled = globalEnabled,
                                color = Color.Red,
                                radius = 160.dp,
                                interiorAlpha = 1f,
                                edgeAlpha = 0f,
                            ),
                    )
                M3xMaterialTheme(backdrop = backdrop) {
                    val source = rememberBackdropLayer(Color.Black)
                    val coroutineScope = rememberCoroutineScope()
                    SideEffect { scope = coroutineScope }
                    BackdropHost(source) {
                        Box(Modifier.size(160.dp, 80.dp).background(Color.Black)) {
                            Box(Modifier.fillMaxSize().captureBackdrop(source))
                            BackdropSurface(
                                Modifier.size(64.dp),
                                RectangleShape,
                                role = MaterialSurfaceRole.Control,
                                containerColor = Color.Black,
                                interactionSource = interactions,
                            ) {}
                            BackdropSurface(
                                Modifier.offset(x = 76.dp).size(64.dp).testTag("target"),
                                RectangleShape,
                                role = MaterialSurfaceRole.Panel,
                                containerColor = Color.Black,
                            ) {}
                        }
                    }
                }
            }
        }

        fun brightness(): Float {
            val pixels = onNodeWithTag("target").captureToImage().toPixelMap()
            return pixels[pixels.width / 2, pixels.height / 2].red
        }
        fun pressAndRelease(): Float {
            val press = PressInteraction.Press(Offset(60f, 32f))
            runOnIdle { scope.launch { interactions.emit(press) } }
            mainClock.advanceTimeBy(32)
            waitForIdle()
            val value = brightness()
            runOnIdle { scope.launch { interactions.emit(PressInteraction.Release(press)) } }
            mainClock.advanceTimeBy(240)
            waitForIdle()
            return value
        }

        val active = pressAndRelease()
        runOnIdle { sourceEnabled = false }
        val sourceRoleDisabled = pressAndRelease()
        runOnIdle {
            sourceEnabled = true
            receiverEnabled = false
        }
        val receiverRoleDisabled = pressAndRelease()
        runOnIdle {
            receiverEnabled = true
            globalEnabled = false
        }
        val globallyDisabled = pressAndRelease()

        assertTrue(active > 0.02f)
        assertTrue(sourceRoleDisabled < active * 0.1f)
        assertTrue(receiverRoleDisabled < active * 0.1f)
        assertTrue(globallyDisabled < active * 0.1f)
    }

    @Test
    fun modalOverlayIsolatesSharedInteractionLighting() = runInitializedComposeUiTest {
        val interactions = MutableInteractionSource()
        var modal by mutableStateOf(false)
        lateinit var scope: CoroutineScope
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    val page = rememberBackdropLayer(Color.Black)
                    val host = remember { OverlayHostState() }
                    val entry = remember { OverlayHandle() }
                    val coroutineScope = rememberCoroutineScope()
                    SideEffect { scope = coroutineScope }
                    BackdropHost(page) {
                        OverlayHost(host) {
                            Box(Modifier.size(160.dp, 80.dp).background(Color.Black)) {
                                Box(Modifier.fillMaxSize().captureBackdrop(page))
                                BackdropSurface(
                                    Modifier.size(64.dp),
                                    RectangleShape,
                                    material = material(Highlight.None),
                                    containerColor = Color.Black,
                                    interactionSource = interactions,
                                ) {}
                            }
                            MaterialOverlay(entry, modal = modal) {
                                BackdropSurface(
                                    Modifier.offset(x = 76.dp).size(64.dp).testTag("target"),
                                    RectangleShape,
                                    material = material(Highlight.None),
                                    containerColor = Color.Black,
                                ) {}
                            }
                        }
                    }
                }
            }
        }
        runOnIdle { scope.launch { interactions.emit(PressInteraction.Press(Offset(60f, 32f))) } }
        waitForIdle()
        fun brightness(): Float {
            val pixels = onNodeWithTag("target").captureToImage().toPixelMap()
            return pixels[pixels.width / 2, pixels.height / 2].red
        }
        val nonmodal = brightness()
        runOnIdle { modal = true }
        waitForIdle()
        val modalValue = brightness()

        assertTrue(nonmodal > 0.02f)
        assertTrue(modalValue < nonmodal * 0.1f)
    }

    @Test
    fun customHighlightShaderTracksDynamicSourceAndUniformsWithCpuParity() =
        runInitializedComposeUiTest {
            val style = DynamicShaderHighlightStyle()
            var lighting by mutableStateOf(false)
            setContent {
                ProvideM3xMaterialRendering {
                    MaterialTheme {
                        val source = rememberBackdropLayer(Color.Black)
                        BackdropHost(source) {
                            Box(Modifier.size(64.dp).background(Color.Black)) {
                                Box(Modifier.fillMaxSize().captureBackdrop(source))
                                BackdropSurface(
                                    Modifier.size(64.dp).testTag("target"),
                                    RectangleShape,
                                    material =
                                        material(
                                            Highlight(
                                                width = 2.dp,
                                                direction = HighlightDirection.None,
                                                style = style,
                                            ),
                                            lighting,
                                        ),
                                    containerColor = Color.Black,
                                ) {}
                            }
                        }
                    }
                }
            }

            fun pixels() = onNodeWithTag("target").captureToImage().toPixelMap()
            val cpuRed = pixels()
            runOnIdle { lighting = true }
            val gpuRed = pixels()
            assertImagesClose(cpuRed, gpuRed)
            assertTrue(gpuRed[0, gpuRed.height / 2].red > gpuRed[0, gpuRed.height / 2].green)

            style.level = .8f
            waitForIdle()
            runOnIdle { lighting = false }
            val cpuBrighterRed = pixels()
            runOnIdle { lighting = true }
            val gpuBrighterRed = pixels()
            assertImagesClose(cpuBrighterRed, gpuBrighterRed)
            assertTrue(
                gpuBrighterRed[0, gpuBrighterRed.height / 2].red >
                    gpuRed[0, gpuRed.height / 2].red + .2f,
                "base shader uniforms are applied on each draw",
            )

            runOnIdle {
                lighting = false
                style.green = true
            }
            val cpuGreen = pixels()
            runOnIdle { lighting = true }
            val gpuGreen = pixels()
            assertImagesClose(cpuGreen, gpuGreen)
            assertTrue(
                gpuGreen[0, gpuGreen.height / 2].green > gpuGreen[0, gpuGreen.height / 2].red,
            )
        }

    @Test
    fun zeroIncidentLightPreservesBuiltInAndCustomHighlightPixels() = runInitializedComposeUiTest {
        val custom =
            object : HighlightStyle {
                override val color: Color = Color.Yellow.copy(alpha = 0.4f)
                override val shaderSource: String =
                    "half4 shadeHighlight(float2 position, float2 outwardNormal, float projection, half4 color) { return half4(0.0, 0.0, 0.75, 0.75); } float highlightStrength(float2 position, float2 outwardNormal, float projection) { return 1.0; }"

                override fun shade(sample: HighlightSample): HighlightEmission =
                    HighlightEmission(Color(0f, 0f, 0.75f, 0.75f))
            }
        var style by mutableStateOf<HighlightStyle>(HighlightStyle.TwoTone)
        var lighting by mutableStateOf(false)
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    val source = rememberBackdropLayer(Color.Black)
                    BackdropHost(source) {
                        Box(Modifier.size(64.dp).background(Color.Black)) {
                            Box(Modifier.fillMaxSize().captureBackdrop(source))
                            BackdropSurface(
                                Modifier.size(64.dp).testTag("target"),
                                RectangleShape,
                                material =
                                    material(Highlight(width = 2.dp, style = style), lighting),
                                containerColor = Color.Black,
                            ) {}
                        }
                    }
                }
            }
        }
        for (candidate in listOf(HighlightStyle.TwoTone, HighlightStyle.Solid, custom)) {
            runOnIdle {
                style = candidate
                lighting = false
            }
            val before = onNodeWithTag("target").captureToImage().toPixelMap()
            runOnIdle { lighting = true }
            val after = onNodeWithTag("target").captureToImage().toPixelMap()
            var error = 0f
            for (y in 0 until before.height) for (x in 0 until before.width) {
                val a = before[x, y]
                val b = after[x, y]
                error =
                    maxOf(
                        error,
                        abs(a.red - b.red),
                        abs(a.green - b.green),
                        abs(a.blue - b.blue),
                    )
            }
            assertTrue(error < 0.005f)
        }
    }
}
