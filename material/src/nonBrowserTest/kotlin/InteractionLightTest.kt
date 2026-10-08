package top.ltfan.m3x.material

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.ltfan.backdrop.BackdropStyle
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.backdrop.BackdropSurface
import top.ltfan.m3x.material.backdrop.theme.BackdropAdaptation
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropLighting
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropTheme
import top.ltfan.m3x.material.bundle.M3xMaterialTheme
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering

class InteractionLightTest {
    private fun testMaterial() =
        M3xBackdropMaterial(
            style = BackdropStyle(effects = BackdropStyle.NoEffects),
            blurRadius = 0.dp,
            tintAlpha = 1f,
            adaptation = BackdropAdaptation.Fixed,
        )

    @Test
    fun rawPointerLightKeepsClickBehaviorAndFades() = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        var clicks by mutableIntStateOf(0)
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    val source = rememberBackdropLayer(Color.Black)
                    BackdropHost(source) {
                        Box(Modifier.size(64.dp).testTag("root")) {
                            Box(Modifier.fillMaxSize().captureBackdrop(source))
                            BackdropSurface(
                                Modifier.fillMaxSize().testTag("input").clickable { clicks++ },
                                RectangleShape,
                                material = testMaterial(),
                                containerColor = Color.Black,
                            ) {}
                        }
                    }
                }
            }
        }
        fun center(): Color = onNodeWithTag("root").captureToImage().toPixelMap()[32, 32]

        val before = center()
        onNodeWithTag("input").performTouchInput { down(0, Offset(32f, 32f)) }
        waitForIdle()
        val pressed = center()
        onNodeWithTag("input").performTouchInput { up(0) }
        mainClock.advanceTimeBy(64)
        val fading = center()
        mainClock.advanceTimeBy(240)
        val released = center()

        assertTrue(pressed.red > before.red + 0.02f)
        assertTrue(fading.red in (before.red + 0.001f)..(pressed.red - 0.001f))
        assertTrue(abs(released.red - before.red) < 0.005f)
        assertEquals(1, clicks)
    }

    @Test
    fun keyboardUsesSurfaceCenterAndKeepsClickBehavior() = runInitializedComposeUiTest {
        var clicks by mutableIntStateOf(0)
        lateinit var requester: FocusRequester
        setContent {
            ProvideM3xMaterialRendering {
                M3xMaterialTheme(backdrop = M3xBackdropTheme()) {
                    MaterialTheme {
                        val source = rememberBackdropLayer(Color.Black)
                        val interactions = remember { MutableInteractionSource() }
                        val focusRequester = remember { FocusRequester() }
                        SideEffect { requester = focusRequester }
                        BackdropHost(source) {
                            Box(Modifier.size(64.dp).testTag("root")) {
                                Box(Modifier.fillMaxSize().captureBackdrop(source))
                                BackdropSurface(
                                    Modifier.fillMaxSize()
                                        .focusRequester(focusRequester)
                                        .testTag("keyboard")
                                        .clickable(interactionSource = interactions) { clicks++ },
                                    RectangleShape,
                                    material = testMaterial(),
                                    containerColor = Color.Black,
                                    interactionSource = interactions,
                                ) {}
                            }
                        }
                    }
                }
            }
        }
        fun center(): Color = onNodeWithTag("root").captureToImage().toPixelMap()[32, 32]

        val before = center()
        runOnIdle { requester.requestFocus() }
        onNodeWithTag("keyboard").assertIsFocused()
        onNodeWithTag("keyboard").performKeyInput { keyDown(Key.Enter) }
        waitForIdle()
        val focused = center()
        onNodeWithTag("keyboard").performKeyInput { keyUp(Key.Enter) }
        waitForIdle()

        assertTrue(focused.red > before.red + 0.02f)
        assertEquals(1, clicks)
    }

    @Test
    fun interactionSourcePressUsesTransformedSurfaceCenter() = runInitializedComposeUiTest {
        val interactions = MutableInteractionSource()
        setContent {
            ProvideM3xMaterialRendering {
                M3xMaterialTheme(
                    backdrop =
                        M3xBackdropTheme(
                            lighting =
                                M3xBackdropLighting(
                                    color = Color.Red,
                                    radius = 8.dp,
                                    interiorAlpha = 1f,
                                    edgeAlpha = 0f,
                                ),
                        ),
                ) {
                    MaterialTheme {
                        val source = rememberBackdropLayer(Color.Black)
                        BackdropHost(source) {
                            Box(Modifier.size(128.dp, 64.dp).testTag("root")) {
                                Box(Modifier.fillMaxSize().captureBackdrop(source))
                                BackdropSurface(
                                    Modifier.offset(x = 8.dp)
                                        .graphicsLayer {
                                            translationX = 24.dp.toPx()
                                            transformOrigin = TransformOrigin(0f, 0f)
                                        }
                                        .size(64.dp),
                                    RectangleShape,
                                    material = testMaterial(),
                                    containerColor = Color.Black,
                                    interactionSource = interactions,
                                ) {}
                            }
                        }
                    }
                }
            }
        }
        waitForIdle()
        val press = PressInteraction.Press(Offset.Unspecified)
        assertTrue(interactions.tryEmit(press))
        waitForIdle()
        val pixels = onNodeWithTag("root").captureToImage().toPixelMap()
        val transformedCenter = pixels[64, 32]
        val untransformedCenter = pixels[32, 32]

        assertTrue(transformedCenter.red > 0.2f)
        assertTrue(transformedCenter.red > untransformedCenter.red + 0.1f)
    }

    @Test
    fun rawPointerLightUsesTransformedSurfaceCoordinates() = runInitializedComposeUiTest {
        setContent {
            ProvideM3xMaterialRendering {
                M3xMaterialTheme(
                    backdrop =
                        M3xBackdropTheme(
                            lighting =
                                M3xBackdropLighting(
                                    color = Color.Red,
                                    radius = 8.dp,
                                    interiorAlpha = 1f,
                                    edgeAlpha = 0f,
                                ),
                        ),
                ) {
                    MaterialTheme {
                        val source = rememberBackdropLayer(Color.Black)
                        BackdropHost(source) {
                            Box(Modifier.size(128.dp, 64.dp).testTag("root")) {
                                Box(Modifier.fillMaxSize().captureBackdrop(source))
                                BackdropSurface(
                                    Modifier.offset(x = 8.dp)
                                        .graphicsLayer {
                                            translationX = 24.dp.toPx()
                                            transformOrigin = TransformOrigin(0f, 0f)
                                        }
                                        .size(64.dp)
                                        .testTag("input"),
                                    RectangleShape,
                                    material = testMaterial(),
                                    containerColor = Color.Black,
                                ) {}
                            }
                        }
                    }
                }
            }
        }
        onNodeWithTag("input").performTouchInput { down(0, Offset(32f, 32f)) }
        waitForIdle()
        val pixels = onNodeWithTag("root").captureToImage().toPixelMap()
        val transformedCenter = pixels[64, 32]
        val untransformedCenter = pixels[32, 32]

        assertTrue(transformedCenter.red > 0.2f)
        assertTrue(transformedCenter.red > untransformedCenter.red + 0.1f)
    }
}
