package top.ltfan.m3x.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.backdrop.BackdropStyle
import top.ltfan.backdrop.highlight.Highlight
import top.ltfan.backdrop.highlight.HighlightColorResponse
import top.ltfan.backdrop.highlight.HighlightDirection
import top.ltfan.backdrop.highlight.HighlightStyle
import top.ltfan.backdrop.shadow.Shadow
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.backdropSurface
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.material.backdrop.BackdropSurface
import top.ltfan.m3x.material.backdrop.theme.BackdropAdaptation
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering

class BackdropSourceRenderingTest {
    @Test
    fun pageBoundaryPolicyChangesExteriorSampling() = runComposeUiTest {
        var tileMode by mutableStateOf(TileMode.Clamp)
        setContent {
            ProvideM3xMaterialRendering {
                val source = rememberBackdropLayer(tileMode = tileMode)
                BackdropHost(source) {
                    Box(Modifier.size(96.dp, 48.dp).background(Color.Black).testTag("root")) {
                        Box(
                            Modifier.size(32.dp, 48.dp)
                                .captureBackdrop(source)
                                .background(Color.Red),
                        )
                        Box(
                            Modifier.offset(40.dp)
                                .size(40.dp, 48.dp)
                                .backdropSurface(
                                    shape = RectangleShape,
                                    effects = {},
                                    highlight = { Highlight.None },
                                    shadow = { Shadow.None },
                                ),
                        )
                    }
                }
            }
        }

        val clamped = onNodeWithTag("root").captureToImage().toPixelMap()
        assertTrue(clamped[60, 24].red > 0.9f, "Clamp extends the red boundary texel")
        runOnIdle { tileMode = TileMode.Decal }
        val decal = onNodeWithTag("root").captureToImage().toPixelMap()
        assertTrue(decal[16, 24].red > 0.9f, "The recorded source stays red")
        assertTrue(decal[60, 24].red < 0.1f, "Decal leaves the exterior transparent")
    }

    @Test
    fun materialColorResponseSamplesTheSourceWithInteractiveLightingEnabled() = runComposeUiTest {
        var enabled by mutableStateOf(false)
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    val source = rememberBackdropLayer(Color.Red)
                    BackdropHost(source) {
                        Box(Modifier.size(96.dp).testTag("root")) {
                            Box(
                                Modifier.fillMaxSize()
                                    .background(Color.Red)
                                    .captureBackdrop(source),
                            )
                            BackdropSurface(
                                Modifier.offset(24.dp, 24.dp).size(48.dp),
                                shape = RectangleShape,
                                containerColor = Color.Black,
                                material =
                                    M3xBackdropMaterial(
                                        style =
                                            BackdropStyle(
                                                effects = {},
                                                shadow = { Shadow.None },
                                                highlight = {
                                                    Highlight(
                                                        width = 4.dp,
                                                        style =
                                                            HighlightStyle.Solid(
                                                                Color.White.copy(alpha = 0.5f),
                                                            ),
                                                        direction = HighlightDirection.None,
                                                        colorResponse =
                                                            HighlightColorResponse(
                                                                amount = if (enabled) 1f else 0f,
                                                            ),
                                                    )
                                                },
                                            ),
                                        blurRadius = 0.dp,
                                        tintAlpha = 1f,
                                        adaptation = BackdropAdaptation.Fixed,
                                        interactiveLighting = true,
                                    ),
                            ) {}
                        }
                    }
                }
            }
        }

        val plain = onNodeWithTag("root").captureToImage().toPixelMap()
        assertTrue(plain[48, 25].blue > 0.1f, "The static rim is white")
        runOnIdle { enabled = true }
        val lit = onNodeWithTag("root").captureToImage().toPixelMap()
        assertTrue(
            lit[48, 25].red > lit[48, 25].blue + 0.1f,
            "The rim follows the red source, got ${lit[48, 25]}",
        )
        assertTrue(
            lit[48, 25].blue < plain[48, 25].blue * 0.25f,
            "The rim takes the source color",
        )
        assertTrue(lit[48, 48].red < 0.1f, "The interior retains its material tint")
    }
}
