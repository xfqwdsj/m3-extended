package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue
import top.ltfan.backdrop.highlight.Highlight
import top.ltfan.backdrop.shadow.Shadow
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.BackdropLayerHandle
import top.ltfan.m3x.backdrop.backdropSurface
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.captureStackBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.backdrop.rememberStackedBackdropLayer
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering

/**
 * A layer of a backdrop stack stays in the chain while the stack changes.
 *
 * The layer under a surface is what the surface above it samples, so a layer that is replaced or
 * starts recording late leaves the chain and the surface above it samples the page instead.
 */
class BackdropStackRenderingTest {
    @Test
    fun stackedLayerHandleAndSamplingSurviveABelowChange() = runInitializedComposeUiTest {
        var addExtra by mutableStateOf(false)
        var lowerHandle: BackdropLayerHandle? = null
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    val page = rememberBackdropLayer(Color.Red)
                    BackdropHost(page) {
                        Box(Modifier.size(120.dp)) {
                            Box(Modifier.fillMaxSize().captureBackdrop(page))
                            val extra = rememberStackedBackdropLayer(emptyList())
                            val lower =
                                rememberStackedBackdropLayer(
                                    if (addExtra) listOf(page, extra) else listOf(page),
                                )
                            SideEffect { lowerHandle = lower }
                            Box(Modifier.fillMaxSize().captureStackBackdrop(lower)) {
                                Box(Modifier.fillMaxSize().background(Color.Green))
                            }
                            val upper = rememberStackedBackdropLayer(listOf(page, lower))
                            Box(Modifier.fillMaxSize().captureStackBackdrop(upper)) {
                                Box(
                                    Modifier.fillMaxSize()
                                        .backdropSurface(
                                            handle = upper,
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

        waitUntil { lowerHandle?.isSampling == true }
        val original = lowerHandle!!
        runOnIdle { addExtra = true }
        waitForIdle()
        assertSame(original, lowerHandle, "The handle survives a change of the layers under it")
        assertTrue(original.isSampling, "The layer keeps the sampling of the stack")
    }

    @Test
    fun stackedLayerStaysInTheBackdropChainAcrossABelowChange() = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        var addExtra by mutableStateOf(false)
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    val page = rememberBackdropLayer(Color.Red)
                    BackdropHost(page) {
                        Box(Modifier.size(120.dp).testTag("root")) {
                            Box(Modifier.fillMaxSize().captureBackdrop(page))
                            val extra = rememberStackedBackdropLayer(emptyList())
                            val lower =
                                rememberStackedBackdropLayer(
                                    if (addExtra) listOf(page, extra) else listOf(page),
                                )
                            Box(Modifier.fillMaxSize().captureStackBackdrop(lower)) {
                                Box(Modifier.fillMaxSize().background(Color.Green))
                            }
                            val upper = rememberStackedBackdropLayer(listOf(page, lower))
                            Box(Modifier.fillMaxSize().captureStackBackdrop(upper)) {
                                Box(
                                    Modifier.fillMaxSize()
                                        .backdropSurface(
                                            handle = upper,
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
        mainClock.advanceTimeBy(1000)

        fun center(): Color {
            val image = onNodeWithTag("root").captureToImage()
            return image.toPixelMap()[image.width / 2, image.height / 2]
        }

        assertTrue(center().green > 0.9f, "The stacked layer is sampled at rest: ${center()}")
        addExtra = true
        mainClock.advanceTimeByFrame()
        val afterChange = center()
        assertTrue(
            afterChange.green > 0.9f && afterChange.red < 0.1f,
            "The stacked layer is still sampled in the frame the layers change: $afterChange",
        )
        mainClock.advanceTimeBy(500)
        val settled = center()
        assertTrue(
            settled.green > 0.9f && settled.red < 0.1f,
            "The stacked layer is sampled after the change: $settled",
        )
    }
}
