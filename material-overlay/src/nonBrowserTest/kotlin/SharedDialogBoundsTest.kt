package top.ltfan.m3x.material.overlay

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.backdrop.backdropEffects
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState
import top.ltfan.m3x.overlay.localSharedTransitionScope

/**
 * The shared bounds of a dialog must move the panel from the bounds of its anchor and back, without
 * a frame that shows the panel at the other end of the animation.
 */
class SharedDialogBoundsTest {
    @Test
    fun sharedDialogEntersAndLeavesFromItsAnchor() = runInitializedComposeUiTest {
        var visible by mutableStateOf(false)
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(motionScheme = TestMotion) {
                    Box(Modifier.fillMaxSize().background(Color.Black).testTag("scene")) {
                        OverlayHost(remember { OverlayHostState() }) {
                            localSharedTransitionScope {
                                val shared = rememberSharedContentState("panel")
                                SharedTransitionAnchor(shared, !visible) {
                                    Box(Modifier.requiredSize(48.dp).background(Color.Blue))
                                }
                                OverlayDialog(
                                    onDismissRequest = { visible = false },
                                    visible = { visible },
                                    sharedContentState = shared,
                                    properties =
                                        OverlayDialogProperties(
                                            usePlatformInsets = false,
                                            usePlatformDefaultWidth = false,
                                            scrimColor = Color.Transparent,
                                        ),
                                    effects = backdropEffects {},
                                ) {
                                    Box(Modifier.size(200.dp).background(Color.Red))
                                }
                            }
                        }
                    }
                }
            }
        }
        waitForIdle()
        mainClock.autoAdvance = false
        val anchor = colorBounds(blue = true)
        // 1. open to the end, then close.
        runOnIdle { visible = true }
        val settledOpen =
            List(24) {
                mainClock.advanceTimeBy(16)
                redBounds()
            }
        runOnIdle { visible = false }
        val settledClose =
            List(24) {
                mainClock.advanceTimeBy(16)
                redBounds()
            }
        mainClock.advanceTimeBy(2000)

        // 2. reverse while the panel is still opening.
        runOnIdle { visible = true }
        val interruptedOpen =
            List(6) {
                mainClock.advanceTimeBy(16)
                redBounds()
            }
        runOnIdle { visible = false }
        val interruptedClose =
            List(24) {
                mainClock.advanceTimeBy(16)
                redBounds()
            }
        mainClock.advanceTimeBy(2000)

        // 3. open, reverse while opening, then reopen while it is still closing.
        runOnIdle { visible = true }
        val firstOpen =
            List(6) {
                mainClock.advanceTimeBy(16)
                redBounds()
            }
        runOnIdle { visible = false }
        val firstClose =
            List(6) {
                mainClock.advanceTimeBy(16)
                redBounds()
            }
        runOnIdle { visible = true }
        val secondOpen =
            List(24) {
                mainClock.advanceTimeBy(16)
                redBounds()
            }

        assertTrue(anchor != null, "The anchor must be drawn")
        // The panel never covers more than its own bounds: a frame that covers the window is the
        // bounds animation reading a container rect instead of the panel rect.
        val panel = settledOpen.last()!!
        for ((name, frames) in
            listOf(
                "open" to settledOpen,
                "close" to settledClose,
                "interrupted open" to interruptedOpen,
                "interrupted close" to interruptedClose,
                "reversal open" to firstOpen,
                "reversal close" to firstClose,
                "reversal open again" to secondOpen,
            )) {
            assertTrue(
                frames.any { it != null },
                "The $name animation must draw the panel before it becomes transparent: $frames",
            )
            assertTrue(
                frames.filterNotNull().all {
                    it.width <= panel.width + 80 && it.height <= panel.height + 80
                },
                "The $name animation must stay inside the panel bounds: $frames",
            )
        }
        // An animation that is interrupted continues from the frame it reached.
        assertTrue(
            interruptedClose.first()!!.width < panel.width - 8,
            "The interrupted close must start where the open left it: ${interruptedClose.first()} panel=$panel",
        )
        assertTrue(
            settledOpen.last()!!.width > anchor.width,
            "The open panel must be larger than its anchor: ${settledOpen.last()} anchor=$anchor",
        )
        assertEquals(
            null,
            settledClose.last(),
            "The closed panel must release its foreground pixels: ${settledClose.last()}",
        )
    }

    private fun ComposeUiTest.redBounds(): IntRect? = colorBounds(false)

    private fun ComposeUiTest.colorBounds(blue: Boolean): IntRect? {
        val pixels = onNodeWithTag("scene", useUnmergedTree = true).captureToImage().toPixelMap()
        var left = Int.MAX_VALUE
        var top = Int.MAX_VALUE
        var right = Int.MIN_VALUE
        var bottom = Int.MIN_VALUE
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                val color = pixels[x, y]
                if ((if (blue) color.blue else color.red) > 0.02f) {
                    if (x < left) left = x
                    if (y < top) top = y
                    if (x > right) right = x
                    if (y > bottom) bottom = y
                }
            }
        }
        if (right < left) return null
        return IntRect(left, top, right + 1, bottom + 1)
    }

    private object TestMotion : MotionScheme {
        override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = spec()

        override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = spec()

        override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = spec()

        override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = spec()

        override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = spec()

        override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = spec()

        private fun <T> spec(): FiniteAnimationSpec<T> = tween(320)
    }
}
