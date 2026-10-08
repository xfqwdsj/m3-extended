package top.ltfan.m3x.material.backdrop

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.compose.ui.accessibility.AccessibilitySettings
import top.ltfan.m3x.compose.ui.accessibility.LocalAccessibilitySettings
import top.ltfan.m3x.material.FloatingBottomBar
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.items

class FloatingBottomBarMotionTest {
    @Test fun releaseKeepsSpatialReboundAfterOpticalFade() = probe(false)

    @Test fun reducedMotionOmitsDecorativeRebound() = probe(true)

    @Test fun reversingDragReturnsToRestingShape() = probe(false, reverseDrag = true)

    private fun probe(reduceMotion: Boolean, reverseDrag: Boolean = false) =
        runInitializedComposeUiTest {
            val motion =
                object : MotionScheme by MotionScheme.expressive() {
                    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = tween(100)

                    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = tween(100)

                    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = tween(10)

                    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> =
                        spring(dampingRatio = 0.3f, stiffness = 100f)
                }
            setContent {
                ProvideM3xMaterialRendering {
                    CompositionLocalProvider(
                        LocalAccessibilitySettings provides
                            AccessibilitySettings(reduceMotion = reduceMotion),
                    ) {
                        MaterialTheme(
                            colorScheme =
                                lightColorScheme(surface = Color.White, onSurface = Color.Black),
                            motionScheme = motion,
                        ) {
                            val source = rememberBackdropLayer()
                            Box(Modifier.size(500.dp, 200.dp).testTag("scene")) {
                                Box(
                                    Modifier.fillMaxSize()
                                        .captureBackdrop(source)
                                        .background(Color.White),
                                )
                                BackdropHost(source) {
                                    FloatingBottomBar(
                                        selectedTabIndex = 0,
                                        onTabSelected = {},
                                        modifier = Modifier.align(Alignment.Center),
                                    ) {
                                        items(listOf("A", "B", "C")) {
                                            Text(it, color = Color.Transparent)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            mainClock.autoAdvance = false
            mainClock.advanceTimeBy(4000)
            val x = onNodeWithText("A").fetchSemanticsNode().boundsInRoot.center.x.toInt()
            fun height(): Int {
                val pixels = onNodeWithTag("scene").captureToImage().toPixelMap()
                val ys =
                    (0 until pixels.height).filter {
                        val c = pixels[x, it]
                        c.red < 0.96f && c.green < 0.96f && c.blue < 0.96f
                    }
                return if (ys.isEmpty()) 0 else ys.last() - ys.first() + 1
            }
            val resting = height()
            onNodeWithText("A").performTouchInput { down(center) }
            if (reverseDrag) {
                mainClock.advanceTimeBy(48)
                onNodeWithText("A").performTouchInput { moveBy(Offset(30f, 0f)) }
                onNodeWithText("A").performTouchInput { moveBy(Offset(-15f, 0f)) }
                mainClock.advanceTimeBy(16)
            } else mainClock.advanceTimeBy(4000)
            onNodeWithText("A").performTouchInput { up() }
            if (reverseDrag) {
                mainClock.advanceTimeBy(4000)
                val settled = height()
                assertTrue(
                    abs(settled - resting) <= 1,
                    "Residual shape: rest=$resting settled=$settled",
                )
                return@runInitializedComposeUiTest
            }
            val frames = mutableListOf<Int>()
            repeat(50) {
                mainClock.advanceTimeBy(16)
                if (it >= 10) frames += height()
            }
            assertTrue(resting > 30)
            if (reduceMotion)
                assertTrue(
                    frames.all { abs(it - resting) <= 1 },
                    "Reduced motion must keep geometry still: $frames",
                )
            else
                assertTrue(
                    frames.min() < resting - 3 && frames.max() > resting + 1,
                    "Scale rebound must outlive optical fade: rest=$resting frames=$frames",
                )
            mainClock.advanceTimeBy(4000)
            assertTrue(abs(height() - resting) <= 1)
        }
}
