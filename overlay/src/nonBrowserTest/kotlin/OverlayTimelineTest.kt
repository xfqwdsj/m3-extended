package top.ltfan.m3x.overlay

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering

/** Native animation samples are the oracle for every property on the back timeline. */
class OverlayTimelineTest {
    @Test
    fun cancellationRewindsNonlinearExitWithUnequalChildDurations() = runInitializedComposeUiTest {
        val entry =
            OverlayHandle().apply {
                backHandler = OverlayBackHandler(enabled = { true }, onCommit = {})
            }
        val spatialSpec: FiniteAnimationSpec<Float> = tween(1000, easing = FastOutSlowInEasing)
        val effectsSpec: FiniteAnimationSpec<Float> = tween(600, easing = LinearEasing)
        lateinit var root: Transition<Boolean>
        setContent {
            ProvideM3xMaterialRendering {
                val transition = rememberTransition(entry.visibleState)
                root = transition
                OverlayBackTimeline(entry, transition)
                transition.animateFloat(transitionSpec = { spatialSpec }) { if (it) 0f else 200f }
                transition.animateFloat(transitionSpec = { effectsSpec }) { if (it) 0f else 12f }
            }
        }
        waitForIdle()
        mainClock.autoAdvance = false
        val spatial = TargetBasedAnimation(spatialSpec, Float.VectorConverter, 0f, 200f)
        val effects = TargetBasedAnimation(effectsSpec, Float.VectorConverter, 0f, 12f)
        fun checkNativeSample() {
            val time = (entry.visibleState.fraction * root.totalDurationNanos).toLong()
            assertEquals(spatial.getValueFromNanos(time), root.animations[0].value as Float, 0.02f)
            assertEquals(effects.getValueFromNanos(time), root.animations[1].value as Float, 0.02f)
        }
        runOnIdle {
            entry.backSession++
            entry.backPhase = OverlayBackPhase.Progress
            entry.backProgress = 0.6f
        }
        mainClock.advanceTimeBy(64)
        runOnIdle {
            checkNativeSample()
            entry.backPhase = OverlayBackPhase.Cancelled
        }
        var previousFraction = 0.6f
        repeat(12) {
            mainClock.advanceTimeByFrame()
            runOnIdle {
                assertTrue(entry.visibleState.fraction <= previousFraction)
                previousFraction = entry.visibleState.fraction
                checkNativeSample()
            }
        }
        mainClock.advanceTimeBy(1200)
        runOnIdle {
            assertEquals(OverlayBackPhase.Idle, entry.backPhase)
            assertTrue(entry.visibleState.currentState)
            assertEquals(0f, root.animations[0].value)
            assertEquals(0f, root.animations[1].value)
        }
    }
}
