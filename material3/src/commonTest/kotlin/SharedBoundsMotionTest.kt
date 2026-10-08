package top.ltfan.m3x.compose.material3

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.MotionScheme
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.animation.core.withDefaultVisibilityThreshold

class SharedBoundsMotionTest {
    @Test
    fun typedGeometryThresholdsPreserveEveryExpressiveSpringParameter() {
        val scheme = MotionScheme.expressive()
        fun <T> check(original: FiniteAnimationSpec<T>, threshold: T) {
            val spring = original as SpringSpec
            val aligned = original.withDefaultVisibilityThreshold(threshold) as SpringSpec
            assertEquals(spring.stiffness, aligned.stiffness)
            assertEquals(spring.dampingRatio, aligned.dampingRatio)
            assertEquals(threshold, aligned.visibilityThreshold)
            assertSame(aligned, aligned.withDefaultVisibilityThreshold(threshold))
        }
        check(scheme.fastSpatialSpec(), Dp.VisibilityThreshold)
        check(scheme.fastSpatialSpec(), IntSize.VisibilityThreshold)
        check(scheme.defaultSpatialSpec(), 1f)
        check(scheme.fastEffectsSpec(), 1f)
        check(scheme.slowSpatialSpec(), Rect.VisibilityThreshold)
        val timed = tween<Float>(450)
        assertSame(timed, timed.withDefaultVisibilityThreshold(1f))
    }

    @Test
    fun nativeRectangleThresholdKeepsTheOriginalSpringTrajectory() {
        val scheme = MotionScheme.expressive()
        val original =
            TargetBasedAnimation(
                scheme.slowSpatialSpec(),
                Rect.VectorConverter,
                Rect(0f, 500f, 400f, 700f),
                Rect(0f, 0f, 400f, 200f),
            )
        val spec = scheme.sharedBoundsSpec()
        val aligned =
            TargetBasedAnimation(
                spec,
                Rect.VectorConverter,
                Rect(0f, 500f, 400f, 700f),
                Rect(0f, 0f, 400f, 200f),
            )
        assertEquals(Rect.VisibilityThreshold, (spec as SpringSpec).visibilityThreshold)
        assertTrue(aligned.durationNanos < original.durationNanos)
        for (time in 0 until aligned.durationNanos step 1_000_000) {
            assertEquals(original.getValueFromNanos(time), aligned.getValueFromNanos(time))
            assertEquals(
                original.getVelocityVectorFromNanos(time),
                aligned.getVelocityVectorFromNanos(time),
            )
        }
        assertEquals(aligned.targetValue, aligned.getValueFromNanos(aligned.durationNanos))
    }

    @Test
    fun callerOwnedThresholdAndDurationBasedSpecificationsRemainAuthoritative() {
        val threshold = Rect(2f, 2f, 2f, 2f)
        val customSpring = spring(visibilityThreshold = threshold)
        val customTween = tween<Rect>(450)
        for (custom in listOf(customSpring, customTween)) {
            val scheme =
                object : MotionScheme by MotionScheme.expressive() {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> =
                        custom as FiniteAnimationSpec<T>
                }
            assertSame(custom, scheme.sharedBoundsSpec())
        }
    }
}
