package top.ltfan.m3x.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.backdrop.highlight.Highlight
import top.ltfan.backdrop.shadow.Shadow
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.backdropSurface
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering

class BackdropRenderingTest {
    @Test
    fun changingBackgroundUpdatesSampledPixelsWithoutReplacingLayer() = runComposeUiTest {
        var background by mutableStateOf(Color.Red)
        setContent {
            ProvideM3xMaterialRendering {
                val layer = rememberBackdropLayer(background)
                BackdropHost(layer) {
                    Box(Modifier.size(80.dp)) {
                        Box(Modifier.fillMaxSize().captureBackdrop(layer))
                        Box(
                            Modifier.fillMaxSize()
                                .backdropSurface(
                                    shape = RectangleShape,
                                    effects = {},
                                    highlight = { Highlight.None },
                                    shadow = { Shadow.None },
                                )
                                .testTag("sample"),
                        )
                    }
                }
            }
        }

        fun sampleCenter(): Color {
            val image = onNodeWithTag("sample").captureToImage()
            return image.toPixelMap()[image.width / 2, image.height / 2]
        }

        val red = sampleCenter()
        assertTrue(red.red > 0.8f && red.blue < 0.2f, "Expected red backdrop, got $red")

        runOnIdle { background = Color.Blue }
        waitForIdle()
        val blue = sampleCenter()
        assertTrue(blue.blue > 0.8f && blue.red < 0.2f, "Expected blue backdrop, got $blue")
    }
}
