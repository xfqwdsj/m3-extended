package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import top.ltfan.backdrop.BackdropBlur
import top.ltfan.backdrop.BackdropEdge
import top.ltfan.backdrop.BackdropFill
import top.ltfan.backdrop.BackdropRamp
import top.ltfan.backdrop.BackdropRampAnchors
import top.ltfan.backdrop.BackdropStyle
import top.ltfan.backdrop.backdrops.rememberCanvasBackdrop
import top.ltfan.backdrop.drawBackdrop
import top.ltfan.backdrop.highlight.Highlight
import top.ltfan.backdrop.shadow.Shadow
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.LocalBackdrop
import top.ltfan.m3x.backdrop.LocalProgressiveBlurMode
import top.ltfan.m3x.backdrop.ProgressiveBlurMode
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.progressiveBlur
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.ui.accessibility.AccessibilitySettings
import top.ltfan.m3x.compose.ui.accessibility.LocalAccessibilitySettings
import top.ltfan.m3x.compose.ui.visual.LocalBlurEnabled
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial

class PublicBackdropSurfaceTest {
    @Test
    fun spacerAndBoxUseTheSameMaterialAndModifierPreservesContent() = runComposeUiTest {
        val material = M3xBackdropMaterial(tint = Color.Green, fallbackAlpha = 1f)
        setContent {
            MaterialTheme {
                Row {
                    BackdropSurface(Modifier.size(64.dp).testTag("spacer"), material = material)
                    BackdropSurface(Modifier.size(64.dp).testTag("box"), material = material) {}
                    val appearance = rememberBackdropSurfaceAppearance(material = material)
                    Box(
                        Modifier.size(64.dp)
                            .testTag("modifier")
                            .materialBackdropSurface(appearance),
                    ) {
                        Box(Modifier.size(12.dp).background(Color.White))
                    }
                }
            }
        }
        for (tag in listOf("spacer", "box", "modifier")) {
            val pixels = onNodeWithTag(tag).captureToImage().toPixelMap()
            assertEquals(Color.Green, pixels[32, 32])
            if (tag == "modifier") assertEquals(Color.White, pixels[4, 4])
        }
    }

    @Test
    fun unsupportedFullHasNoBackgroundWhileDisableAndCustomRenderKeepTheirContracts() =
        runComposeUiTest {
            val blur = mutableStateOf(true)
            val custom = mutableStateOf(false)
            val reducedTransparency = mutableStateOf(false)
            lateinit var appearance: BackdropSurfaceAppearance
            setContent {
                MaterialTheme {
                    val source = rememberBackdropLayer(Color.Red)
                    CompositionLocalProvider(
                        LocalAccessibilitySettings provides
                            AccessibilitySettings(reduceTransparency = reducedTransparency.value),
                    ) {
                        BackdropHost(source) {
                            appearance =
                                rememberMaterialAppearance(
                                    role = MaterialSurfaceRole.ScrollEdge,
                                    material =
                                        M3xBackdropMaterial(tint = Color.Green, tintAlpha = 1f),
                                    blurEnabled = blur.value,
                                    progressiveBlurMode = ProgressiveBlurMode.Full,
                                    shaderSupported = false,
                                )
                            Box(Modifier.size(64.dp).background(Color.Blue).testTag("scene")) {
                                Box(
                                    Modifier.size(64.dp)
                                        .materialBackdropSurface(
                                            appearance,
                                            render =
                                                if (custom.value) ({ fill(Color.Yellow) })
                                                else null,
                                        ),
                                ) {
                                    Box(Modifier.size(12.dp).background(Color.White))
                                }
                            }
                        }
                    }
                }
            }
            fun center() = onNodeWithTag("scene").captureToImage().toPixelMap()[32, 32]
            assertFalse(appearance.drawsBackground)
            assertEquals(Color.Blue, center())
            assertEquals(Color.White, onNodeWithTag("scene").captureToImage().toPixelMap()[4, 4])
            runOnIdle { custom.value = true }
            assertEquals(Color.Yellow, center())
            runOnIdle {
                custom.value = false
                blur.value = false
            }
            waitForIdle()
            assertTrue(appearance.drawsBackground)
            assertEquals(Color.Green, center())
            runOnIdle {
                blur.value = true
                reducedTransparency.value = true
            }
            waitForIdle()
            assertTrue(appearance.drawsBackground)
            assertEquals(Color.Green, center())
        }

    @Test
    fun disabledBarPreservesTheOuterSourceAndStaticBarHasNoExtendedFill() = runComposeUiTest {
        val enabled = mutableStateOf(false)
        setContent {
            MaterialTheme {
                val source = rememberBackdropLayer(Color.Black)
                BackdropHost(source) {
                    Box(Modifier.size(200.dp, 120.dp).background(Color.Black).testTag("scene")) {
                        BackdropTopAppBar(
                            title = {
                                assertTrue(LocalBackdrop.current === source || enabled.value)
                            },
                            windowInsets = WindowInsets(0, 0, 0, 0),
                            backdropEnabled = enabled.value,
                        )
                    }
                }
            }
        }
        var pixels = onNodeWithTag("scene").captureToImage().toPixelMap()
        assertEquals(Color.Black, pixels[100, 32])
        runOnIdle { enabled.value = true }
        pixels = onNodeWithTag("scene").captureToImage().toPixelMap()
        assertTrue(pixels[100, 32].red > .1f)
        assertEquals(Color.Black, pixels[100, 68])
    }

    @Test
    fun uniformMaterialsAlsoApplyPresenceToTheirBlurRadius() = runComposeUiTest {
        val presence = mutableStateOf(0f)
        setContent {
            MaterialTheme {
                val page = rememberBackdropLayer()
                BackdropHost(page) {
                    Box(Modifier.size(80.dp)) {
                        Canvas(Modifier.size(80.dp).captureBackdrop(page)) {
                            for (x in 0 until size.width.toInt() step 4) {
                                drawRect(
                                    if (x % 8 == 0) Color.White else Color.Black,
                                    Offset(x.toFloat(), 0f),
                                    Size(4f, size.height),
                                )
                            }
                        }
                        BackdropSurface(
                            Modifier.size(80.dp).testTag("uniform"),
                            shape = RectangleShape,
                            presence = { presence.value },
                            material =
                                M3xBackdropMaterial(
                                    style =
                                        BackdropStyle.Default {
                                            blur = { BackdropBlur.Uniform(16.dp) }
                                            highlight = { Highlight.None }
                                            shadow = { Shadow.None }
                                        },
                                    tintAlpha = 0f,
                                ),
                        )
                    }
                }
            }
        }
        var pixels = onNodeWithTag("uniform").captureToImage().toPixelMap()
        val sharp = abs(pixels[40, 40].red - pixels[44, 40].red)
        assertTrue(sharp > .9f)
        runOnIdle { presence.value = 1f }
        pixels = onNodeWithTag("uniform").captureToImage().toPixelMap()
        assertTrue(abs(pixels[40, 40].red - pixels[44, 40].red) < .1f)
    }

    @Test
    fun staticBarsKeepExplicitFillWithoutProgressiveExtension() {
        for (variant in 0..3) for (sourced in listOf(false, true)) runComposeUiTest {
            setContent {
                MaterialTheme {
                    val source = rememberBackdropLayer(Color.Black)
                    CompositionLocalProvider(LocalBackdrop provides if (sourced) source else null) {
                        Box(Modifier.size(320.dp).background(Color.Black).testTag("scene")) {
                            val fill = BackdropFill(brush = SolidColor(Color.Red))
                            when (variant) {
                                0 ->
                                    BackdropTopAppBar(
                                        {},
                                        Modifier.testTag("bar"),
                                        windowInsets = WindowInsets(0, 0, 0, 0),
                                        fill = fill,
                                    )
                                1 ->
                                    BackdropTopAppBar(
                                        {},
                                        Modifier.testTag("bar"),
                                        windowInsets = WindowInsets(0, 0, 0, 0),
                                        fill = fill,
                                        titleHorizontalAlignment = Alignment.CenterHorizontally,
                                    )
                                2 ->
                                    BackdropMediumFlexibleTopAppBar(
                                        {},
                                        Modifier.testTag("bar"),
                                        windowInsets = WindowInsets(0, 0, 0, 0),
                                        fill = fill,
                                    )
                                else ->
                                    BackdropLargeFlexibleTopAppBar(
                                        {},
                                        Modifier.testTag("bar"),
                                        windowInsets = WindowInsets(0, 0, 0, 0),
                                        fill = fill,
                                    )
                            }
                        }
                    }
                }
            }
            val pixels = onNodeWithTag("scene").captureToImage().toPixelMap()
            val bottom = onNodeWithTag("bar").fetchSemanticsNode().boundsInRoot.bottom.toInt()
            val inside = pixels[pixels.width / 2, bottom - 1]
            assertTrue(
                inside.red > .5f && inside.green < .01f && inside.blue < .01f,
                "Static variant=$variant source=$sourced must preserve its explicit red fill: $inside",
            )
            assertEquals(Color.Black, pixels[pixels.width / 2, bottom])
            assertEquals(Color.Black, pixels[pixels.width / 2, bottom + 12])
        }
    }

    @Test
    fun alphaMasksTheBlurredImageWhileLayeredPreservesOpaqueCoverage() = runComposeUiTest {
        val mode = mutableStateOf<ProgressiveBlurMode>(ProgressiveBlurMode.Alpha)
        setContent {
            val source = rememberCanvasBackdrop {
                drawRect(Color.White, samplingBounds.topLeft, samplingBounds.size)
            }
            Box(
                Modifier.size(80.dp)
                    .testTag("image")
                    .drawBackdrop(
                        source,
                        { RectangleShape },
                        render = {
                            progressiveBlur(
                                8f,
                                BackdropEdge.Top,
                                BackdropRamp(),
                                mode.value,
                            )
                        },
                    ),
            )
        }
        var pixels = onNodeWithTag("image").captureToImage().toPixelMap()
        assertTrue(pixels[40, 5].alpha > pixels[40, 74].alpha + .5f)
        runOnIdle { mode.value = ProgressiveBlurMode.Layered(4) }
        pixels = onNodeWithTag("image").captureToImage().toPixelMap()
        assertTrue(pixels[40, 5].alpha > .98f && pixels[40, 74].alpha > .98f)
    }

    @Test
    fun alphaRampKeepsItsExtendedCoordinatesInBothReadingDirections() = runComposeUiTest {
        val direction = mutableStateOf(LayoutDirection.Ltr)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction.value) {
                val source = rememberCanvasBackdrop {
                    drawRect(Color.Red, samplingBounds.topLeft, samplingBounds.size)
                }
                Box(Modifier.size(160.dp).testTag("extended")) {
                    Box(
                        Modifier.offset(40.dp, 40.dp)
                            .size(80.dp)
                            .drawBackdrop(
                                source,
                                { RectangleShape },
                                render = {
                                    progressiveBlur(
                                        8f,
                                        BackdropEdge.Start,
                                        BackdropRamp(
                                            anchors = BackdropRampAnchors.offsets(16.dp, 24.dp),
                                        ),
                                        ProgressiveBlurMode.Alpha,
                                    )
                                },
                            ),
                    )
                }
            }
        }
        var pixels = onNodeWithTag("extended").captureToImage().toPixelMap()
        assertTrue(pixels[129, 80].alpha in 0.2f..0.6f)
        assertTrue(pixels[146, 80].alpha < .01f)
        runOnIdle { direction.value = LayoutDirection.Rtl }
        pixels = onNodeWithTag("extended").captureToImage().toPixelMap()
        assertTrue(pixels[30, 80].alpha in 0.2f..0.6f)
        assertTrue(pixels[13, 80].alpha < .01f)
    }

    @Test
    fun explicitModeOverridesLocalAndMasterSwitchStillWins() = runComposeUiTest {
        lateinit var inherited: BackdropSurfaceAppearance
        lateinit var explicit: BackdropSurfaceAppearance
        setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalProgressiveBlurMode provides ProgressiveBlurMode.Alpha,
                    LocalBlurEnabled provides false,
                ) {
                    inherited =
                        rememberBackdropSurfaceAppearance(role = MaterialSurfaceRole.ScrollEdge)
                    explicit =
                        rememberBackdropSurfaceAppearance(
                            role = MaterialSurfaceRole.ScrollEdge,
                            progressiveBlurMode = ProgressiveBlurMode.Full,
                        )
                }
            }
        }
        assertEquals(ProgressiveBlurMode.Alpha, inherited.progressiveBlurMode)
        assertEquals(ProgressiveBlurMode.Full, explicit.progressiveBlurMode)
        assertFalse(inherited.blurEnabled)
        assertFalse(explicit.blurEnabled)
        assertTrue(explicit.drawsBackground)
    }
}
