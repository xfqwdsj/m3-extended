package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.material.FloatingBottomBar
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.item

/** Indicator sampling stays within the bar's effect footprint while the pointer is held. */
class FloatingBottomBarSamplingRenderingTest {
    @Test
    fun heldIndicatorDoesNotDrawRemoteSamplingEdges() = runComposeUiTest {
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    val source = rememberBackdropLayer(Color.White)
                    Box(Modifier.size(600.dp, 420.dp).background(Color.White).testTag("scene")) {
                        BackdropHost(source) {
                            Box(
                                Modifier.fillMaxSize()
                                    .captureBackdrop(source)
                                    .background(Color(0xffe9f5ec)),
                            )
                            FloatingBottomBar(
                                selectedTabIndex = 3,
                                onTabSelected = {},
                                modifier =
                                    Modifier.align(Alignment.BottomCenter)
                                        .padding(bottom = 24.dp)
                                        .testTag("bar"),
                            ) {
                                listOf("Home", "Overlay", "Lab", "Settings").forEach { label ->
                                    item {
                                        Text("●")
                                        Text(label)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        waitForIdle()
        mainClock.autoAdvance = false
        val original = onNodeWithTag("scene").captureToImage().toPixelMap()
        onNodeWithTag("bar").performMouseInput {
            moveTo(Offset(width - 40f, height / 2f))
            press()
        }
        repeat(2) {
            mainClock.advanceTimeBy(256)
            val pressed = onNodeWithTag("scene").captureToImage().toPixelMap()
            for (y in 0 until pressed.height) for (x in 0 until pressed.width) {
                // These pixels lie beyond the bar's shadow, scale and refraction footprint.
                if (y >= 260 && x >= 48) continue
                val before = original[x, y]
                val after = pressed[x, y]
                assertTrue(
                    maxOf(
                        abs(before.red - after.red),
                        abs(before.green - after.green),
                        abs(before.blue - after.blue),
                    ) < 0.002f,
                    "Pressing the indicator changed a remote source pixel at ($x, $y): $before -> $after",
                )
            }
        }
        onNodeWithTag("bar").performMouseInput { release() }
    }
}
