package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.backdrop.BackdropFill
import top.ltfan.backdrop.BackdropRamp
import top.ltfan.backdrop.BackdropStyle
import top.ltfan.backdrop.ProvideBackdropStyle
import top.ltfan.backdrop.highlight.Highlight
import top.ltfan.backdrop.shadow.Shadow
import top.ltfan.backdrop.styledSurface
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.backdropSurface
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial

class BackdropSurfaceRenderTest {
    @Test
    fun styleBrushAlphaAndRampMatchExplicitFillWithoutBlur() = runComposeUiTest {
        val configuration =
            mutableStateOf(
                BackdropFill(
                    brush = Brush.horizontalGradient(listOf(Color.Red, Color.Blue)),
                    alpha = .5f,
                    ramp = BackdropRamp(),
                ),
            )
        val styled =
            M3xBackdropMaterial(
                style = BackdropStyle.NoEffects { fill = { configuration.value } },
            )
        setContent {
            MaterialTheme {
                val source = rememberBackdropLayer(Color.Black)
                BackdropHost(source) {
                    Box(Modifier.size(192.dp, 96.dp)) {
                        Box(Modifier.size(192.dp, 96.dp).captureBackdrop(source))
                        BackdropSurface(
                            modifier = Modifier.size(96.dp).testTag("styled"),
                            shape = RectangleShape,
                            material = styled,
                        ) {}
                        BackdropSurface(
                            modifier = Modifier.offset(x = 96.dp).size(96.dp).testTag("explicit"),
                            shape = RectangleShape,
                            material = M3xBackdropMaterial(style = BackdropStyle.NoEffects),
                            fill = configuration.value,
                        ) {}
                    }
                }
            }
        }
        for (alpha in listOf(.5f, .25f)) {
            runOnIdle { configuration.value = configuration.value.copy(alpha = alpha) }
            val first = onNodeWithTag("styled").captureToImage().toPixelMap()
            val second = onNodeWithTag("explicit").captureToImage().toPixelMap()
            for (x in listOf(12, 48, 80)) for (y in listOf(12, 48, 80)) {
                val a = first[x, y]
                val b = second[x, y]
                assertTrue(
                    abs(a.red - b.red) < .01f && abs(a.blue - b.blue) < .01f,
                    "Style and explicit fill at ($x, $y), alpha $alpha: $a vs $b",
                )
            }
            assertTrue(
                first[12, 12].red > first[12, 80].red,
                "The fill ramp remains active without blur",
            )
        }
    }

    @Test
    fun styledSurfaceInheritsBackdropStyleAndMaterialFillAppliesAlphaOnce() = runComposeUiTest {
        val fillColor = mutableStateOf(Color.Red.copy(alpha = 0.5f))
        val style = BackdropStyle.Default {
            fill = { BackdropFill(fillColor.value) }
            highlight = { Highlight.None }
            shadow = { Shadow.None }
        }
        setContent {
            MaterialTheme {
                val source = rememberBackdropLayer(Color.Black)
                BackdropHost(source) {
                    Box(Modifier.size(160.dp, 80.dp)) {
                        Box(Modifier.size(160.dp, 80.dp).captureBackdrop(source))
                        ProvideBackdropStyle(style) {
                            Box(
                                Modifier.size(80.dp)
                                    .testTag("parameterized")
                                    .backdropSurface(
                                        shape = RectangleShape,
                                        render = { styledSurface() },
                                    ),
                            )
                        }
                        BackdropSurface(
                            modifier = Modifier.offset(x = 80.dp).size(80.dp).testTag("material"),
                            shape = RectangleShape,
                            material =
                                M3xBackdropMaterial(
                                    style = style,
                                    tint = Color.Green,
                                    tintAlpha = 1f,
                                ),
                        ) {}
                    }
                }
            }
        }
        for (tag in listOf("parameterized", "material")) {
            val image = onNodeWithTag(tag).captureToImage()
            val color = image.toPixelMap()[image.width / 2, image.height / 2]
            assertTrue(color.red in 0.48f..0.52f && color.green < .01f, "$tag fill: $color")
        }
        runOnIdle { fillColor.value = Color.Transparent }
        val image = onNodeWithTag("material").captureToImage()
        val color = image.toPixelMap()[image.width / 2, image.height / 2]
        assertTrue(
            color.red < 0.01f && color.green < 0.01f,
            "Explicit transparent fill keeps the black source: $color",
        )
    }

    @Test
    fun surfaceRenderOverridesMaterialGraphAndPreservesContent() = runComposeUiTest {
        setContent {
            MaterialTheme {
                val source = rememberBackdropLayer(Color.White)
                BackdropHost(source) {
                    Box(Modifier.size(80.dp)) {
                        Box(Modifier.size(80.dp).captureBackdrop(source))
                        BackdropSurface(
                            modifier = Modifier.size(80.dp).testTag("surface"),
                            shape = RectangleShape,
                            material = M3xBackdropMaterial(render = { fill(Color.Green) }),
                            render = { fill(Color.Red) },
                        ) {
                            Box(Modifier.size(20.dp).background(Color.Blue))
                        }
                    }
                }
            }
        }
        val image = onNodeWithTag("surface").captureToImage()
        val pixels = image.toPixelMap()
        val background = pixels[image.width / 2, image.height / 2]
        val child = pixels[2, 2]
        assertTrue(background.red > .98f && background.green < .01f)
        assertTrue(child.blue > .98f && child.red < .01f)
    }
}
