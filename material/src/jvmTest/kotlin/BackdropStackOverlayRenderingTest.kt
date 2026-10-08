package top.ltfan.m3x.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.backdrop.highlight.Highlight
import top.ltfan.backdrop.shadow.Shadow
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.LocalBackdrop
import top.ltfan.m3x.backdrop.backdropSurface
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.overlay.MaterialOverlay
import top.ltfan.m3x.overlay.OverlayHandle
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState

/**
 * A layer of a backdrop stack stays in the chain while the stack changes.
 *
 * The layer under a surface is what the surface above it samples, so a layer that starts recording
 * late leaves the chain and the surface above it samples the page instead. Frame stepping needs the
 * desktop test runtime.
 */
class BackdropStackOverlayRenderingTest {
    @Test
    fun layerUnderANewlyOpenedOverlayStaysInTheBackdropChain() = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        var showUpper by mutableStateOf(false)
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    val page = rememberBackdropLayer(Color.Magenta)
                    val host = remember { OverlayHostState() }
                    BackdropHost(page) {
                        OverlayHost(host) {
                            Box(Modifier.size(120.dp).testTag("root")) {
                                Box(Modifier.fillMaxSize().captureBackdrop(page))
                                val lower = remember { OverlayHandle() }
                                MaterialOverlay(lower, modal = false) {
                                    val handle = LocalBackdrop.current!!
                                    Box(
                                        Modifier.fillMaxSize()
                                            .backdropSurface(
                                                handle = handle,
                                                shape = RectangleShape,
                                                effects = {},
                                                highlight = { Highlight.None },
                                                shadow = { Shadow.None },
                                                onDrawSurface = { drawRect(Color.Yellow) },
                                            ),
                                    )
                                }
                                if (showUpper) {
                                    val upper = remember { OverlayHandle() }
                                    MaterialOverlay(upper, modal = false) {
                                        val handle = LocalBackdrop.current!!
                                        Box(
                                            Modifier.fillMaxSize()
                                                .backdropSurface(
                                                    handle = handle,
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
                    }
                }
            }
        }
        mainClock.advanceTimeBy(1000)

        fun center(): Color {
            val image = onNodeWithTag("root").captureToImage()
            return image.toPixelMap()[image.width / 2, image.height / 2]
        }

        assertTrue(center().red > 0.9f, "The lower overlay samples the page: ${center()}")
        showUpper = true
        repeat(6) { frame ->
            mainClock.advanceTimeByFrame()
            val pixel = center()
            assertTrue(
                pixel.red > 0.9f && pixel.green > 0.9f && pixel.blue < 0.1f,
                "The overlay above samples the layer under it in frame $frame: $pixel",
            )
        }
    }
}
