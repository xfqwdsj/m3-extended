package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.m3x.backdrop.BackdropBlur
import top.ltfan.m3x.backdrop.BackdropFill
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.BackdropStyle
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.highlight.Highlight
import top.ltfan.m3x.backdrop.highlight.HighlightDirection
import top.ltfan.m3x.backdrop.interaction.InteractionLighting
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.backdrop.shadow.Shadow
import top.ltfan.m3x.material.FloatingBottomBar
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.backdrop.theme.BackdropAdaptation
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial
import top.ltfan.m3x.material.backdrop.theme.ProvideBackdropMaterial
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.item

class FloatingBottomBarLensRenderingTest {
    @Test
    fun lensPreservesTheBarAndPageBehindIt() = runComposeUiTest {
        mainClock.autoAdvance = false
        val highlightEnabled = mutableStateOf(true)
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    val source = rememberBackdropLayer(Color.Magenta)
                    BackdropHost(source) {
                        Box(Modifier.size(320.dp, 160.dp).testTag("root"), Alignment.Center) {
                            Box(
                                Modifier.fillMaxSize()
                                    .captureBackdrop(source)
                                    .background(Color.Magenta),
                            )
                            ProvideBackdropMaterial(
                                MaterialSurfaceRole.Control,
                                M3xBackdropMaterial(
                                    style =
                                        BackdropStyle.Default {
                                            interactionLighting = { InteractionLighting.Config() }
                                            fill = { BackdropFill(Color.Unspecified) }
                                            blur = { BackdropBlur.Uniform(0.dp) }
                                            highlight = {
                                                if (highlightEnabled.value)
                                                    Highlight.Contour(
                                                        width = 3.dp,
                                                        blurRadius = 12.dp,
                                                        colorGain = .35f,
                                                        environmentGain = 1f,
                                                        opacity = 1f,
                                                        direction = HighlightDirection.Uniform,
                                                        color = Color.White,
                                                    )
                                                else Highlight.None
                                            }
                                            shadow = { Shadow.None }
                                        },
                                    tint = Color.Gray,
                                    tintAlpha = 1f,
                                    adaptation = BackdropAdaptation.Fixed,
                                    adaptiveContentColor = false,
                                    interactiveLighting = false,
                                ),
                            ) {
                                FloatingBottomBar(
                                    0,
                                    {},
                                    Modifier.testTag("bar"),
                                ) {
                                    repeat(3) { item { Box(Modifier.size(64.dp, 24.dp)) } }
                                }
                            }
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(500)
        val bar = onNodeWithTag("bar")
        val bounds = bar.fetchSemanticsNode().boundsInRoot
        bar.performTouchInput { down(Offset(width / 6f, height / 2f)) }
        mainClock.advanceTimeBy(1000)
        val pixels = onNodeWithTag("root").captureToImage().toPixelMap()
        val x = (bounds.left + 4f + (bounds.width - 8f) / 6f).toInt()
        val top = bounds.top.toInt()
        val pageBand = (top..top + 10).map { pixels[x, it] }
        assertTrue(
            pageBand.any { it.red > 0.9f && it.blue > 0.9f && it.green < 0.15f },
            "The real lens must preserve a page-colored band inside the selected capsule: $pageBand",
        )
        val interior = pixels[x, bounds.center.y.toInt()]
        assertTrue(
            abs(interior.red - interior.green) < 0.02f && interior.green in 0.45f..0.6f,
            "The same lens must retain the gray navigation surface: $interior",
        )
        runOnIdle { highlightEnabled.value = false }
        mainClock.advanceTimeByFrame()
        val unlit = onNodeWithTag("root").captureToImage().toPixelMap()
        var contourDelta = 0f
        // This region is inside the enlarged indicator, around the recorded tab contour.
        for (y in top + 6..top + 22) {
            for (cx in bounds.left.toInt() + 6..bounds.left.toInt() + 32) {
                val lit = pixels[cx, y]
                val dark = unlit[cx, y]
                contourDelta =
                    maxOf(
                        contourDelta,
                        abs(lit.red - dark.red),
                        abs(lit.green - dark.green),
                        abs(lit.blue - dark.blue),
                    )
            }
        }
        assertTrue(
            contourDelta > 0.02f,
            "The sampled navigation highlight must affect pixels inside the lens: $contourDelta",
        )
        bar.performTouchInput { up() }
    }
}
