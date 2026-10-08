package top.ltfan.m3x.compose.foundation

import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.foundation.gestures.DampedDragState
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering

class DampedDragStateTest {
    @Test
    fun interruptedDragsSettleVelocityAndScale() = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        lateinit var animation: DampedDragState
        setContent {
            ProvideM3xMaterialRendering {
                val scope = rememberCoroutineScope()
                val motion = MotionScheme.expressive()
                animation = remember {
                    DampedDragState(
                        scope,
                        0f,
                        0f..2f,
                        0.001f,
                        motion.defaultSpatialSpec(),
                        motion.slowEffectsSpec(),
                        motion.slowSpatialSpec(),
                        { true },
                        1f,
                        78f / 56f,
                        {},
                        { _, _ -> },
                        { _, _, _ -> },
                    )
                }
            }
        }
        repeat(48) { iteration ->
            runOnIdle {
                animation.press()
                animation.updateValue(if (animation.value < 1f) 2f else 0f)
            }
            repeat(iteration % 12) { frame ->
                mainClock.advanceTimeByFrame()
                runOnIdle { animation.updateValue((frame % 3).toFloat()) }
            }
            runOnIdle {
                animation.animateToValue(1f)
                animation.release()
            }
            if (iteration % 2 == 0) {
                mainClock.advanceTimeByFrame()
                runOnIdle {
                    animation.press()
                    animation.animateToValue(2f)
                    animation.release()
                }
            }
            mainClock.advanceTimeBy(4000)
            assertEquals(if (iteration % 2 == 0) 2f else 1f, animation.value)
            assertEquals(1f, animation.scaleX)
            assertEquals(1f, animation.scaleY)
            assertEquals(0f, animation.pressProgress)
            assertEquals(0f, animation.velocity, "Residual velocity after interruption $iteration")
        }
    }

    @Test
    fun defaultSpatialSpecRetainsSmallReleaseRebound() = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        lateinit var animation: DampedDragState
        setContent {
            ProvideM3xMaterialRendering {
                val scope = rememberCoroutineScope()
                val motion = MotionScheme.expressive()
                animation = remember {
                    DampedDragState(
                        scope,
                        0f,
                        0f..2f,
                        0.001f,
                        motion.defaultSpatialSpec(),
                        motion.slowEffectsSpec(),
                        motion.slowSpatialSpec(),
                        { true },
                        1f,
                        78f / 56f,
                        {},
                        { _, _ -> },
                        { _, _, _ -> },
                    )
                }
            }
        }
        runOnIdle { animation.press() }
        mainClock.advanceTimeBy(2000)
        runOnIdle { animation.release() }
        val scales = mutableListOf<Float>()
        repeat(80) {
            mainClock.advanceTimeBy(16)
            scales += animation.scaleX
        }
        assertTrue(
            scales.min() < 0.997f,
            "The default spring must retain its small rebound: $scales",
        )
        val last = scales.indexOfLast { it != 1f }
        assertTrue(
            last >= 0 && abs(scales[last] - 1f) < 0.0015f,
            "The release must finish smoothly at component precision: $scales",
        )
    }
}
