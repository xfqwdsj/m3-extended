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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.ltfan.m3x.backdrop.BackdropBlur
import top.ltfan.m3x.backdrop.BackdropFill
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.BackdropStyle
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.highlight.Highlight
import top.ltfan.m3x.backdrop.interaction.InteractionLight
import top.ltfan.m3x.backdrop.interaction.InteractionLighting
import top.ltfan.m3x.backdrop.interaction.interactionLightSource
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.backdrop.shadow.Shadow
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

class SharedLightRenderingTest {
    private fun material(
        highlight: Highlight,
        interactive: Boolean = true,
        lighting: InteractionLighting = InteractionLighting.Config(),
    ) =
        M3xBackdropMaterial(
            style =
                BackdropStyle.Default {
                    fill = { BackdropFill(Color.Unspecified) }
                    interactionLighting = { lighting }
                    blur = { BackdropBlur.Uniform(0.dp) }
                    this.highlight = { highlight }
                    shadow = { Shadow.None }
                },
            tintAlpha = 1f,
            adaptation = BackdropAdaptation.Fixed,
            interactiveLighting = interactive,
        )

    @Test
    fun interiorGlowCrossesComponentsAndFadesContinuously() = runComposeUiTest {
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
    fun reduceMotionReleasesSharedLightImmediately() = runComposeUiTest {
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
    fun sharedGlowKeepsSourceColorAndWindowRadiusUnderTransforms() = runComposeUiTest {
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
    fun receiverOnlySurfaceGetsSharedLight() = runComposeUiTest {
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
    fun globalAndRoleGatesDisableBothSourceAndReceiver() = runComposeUiTest {
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
                        control =
                            material(
                                Highlight.None,
                                sourceEnabled,
                                InteractionLighting.Config(1f),
                            ),
                        panel =
                            material(
                                Highlight.None,
                                receiverEnabled,
                                InteractionLighting.Config(1f),
                            ),
                        lighting =
                            M3xBackdropLighting(
                                enabled = globalEnabled,
                                color = Color.Red,
                                radius = 160.dp,
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
    fun modalOverlayIsolatesSharedInteractionLighting() = runComposeUiTest {
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
}
