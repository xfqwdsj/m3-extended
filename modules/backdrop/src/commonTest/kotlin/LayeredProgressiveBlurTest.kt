package top.ltfan.m3x.backdrop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import top.ltfan.m3x.backdrop.effects.layeredBlurBand
import top.ltfan.m3x.backdrop.effects.layeredBlurStops
import top.ltfan.m3x.backdrop.effects.layeredBlurWeight
import top.ltfan.m3x.backdrop.effects.layeredProgressiveBlur
import top.ltfan.m3x.backdrop.effects.layeredProgressiveBlurImpl
import top.ltfan.m3x.backdrop.effects.progressiveBlurImpl

class LayeredProgressiveBlurTest {
    @Test
    fun levelBandsContainOnlyPositiveWeightIntervals() {
        val surface = Rect(0f, 0f, 100f, 100f)
        val top = Offset(0f, 100f) to Offset(0f, 0f)
        for (reverse in listOf(false, true)) {
            val ramp =
                BackdropRamp(
                    from = if (reverse) 1f else 0f,
                    to = if (reverse) 0f else 1f,
                    curve = BackdropCurve.Linear,
                )
            val first =
                requireNotNull(
                    layeredBlurBand(
                        ramp,
                        if (reverse) 15 else 0,
                        16,
                        top.first,
                        top.second,
                        surface,
                    ),
                )
            val middle =
                requireNotNull(layeredBlurBand(ramp, 8, 16, top.first, top.second, surface))
            val last =
                requireNotNull(
                    layeredBlurBand(
                        ramp,
                        if (reverse) 0 else 15,
                        16,
                        top.first,
                        top.second,
                        surface,
                    ),
                )
            assertTrue(first.top > 90f && first.bottom == 100f, "first=$first reverse=$reverse")
            assertTrue(
                middle.top > 30f && middle.bottom < 70f && middle.height < surface.height / 2f,
                "middle=$middle reverse=$reverse",
            )
            assertTrue(last.top == 0f && last.bottom < 10f, "last=$last reverse=$reverse")
        }

        val constant = BackdropRamp(from = .5f, to = .5f)
        assertEquals(surface, layeredBlurBand(constant, 3, 8, top.first, top.second, surface))
        assertEquals(surface, layeredBlurBand(constant, 4, 8, top.first, top.second, surface))
        assertEquals(null, layeredBlurBand(constant, 2, 8, top.first, top.second, surface))
    }

    @Test
    fun sampledWeightsKeepOnlyAdjacentLevelsAndPartitionUnity() {
        for (levels in listOf(2, 4, 8, 16)) for (reverse in listOf(false, true)) {
            val ramp =
                BackdropRamp(
                    from = if (reverse) 1f else 0f,
                    to = if (reverse) 0f else 1f,
                    stops = 2,
                )
            val stops = layeredBlurStops(ramp, levels)
            for ((a, b) in stops.zipWithNext()) for (fraction in listOf(0f, .1f, .5f, .9f, 1f)) {
                val weights =
                    List(levels) { level ->
                        layeredBlurWeight(a.second, level, levels) * (1 - fraction) +
                            layeredBlurWeight(b.second, level, levels) * fraction
                    }
                assertTrue(abs(weights.sum() - 1f) < .00001f)
                val active = weights.indices.filter { weights[it] > .00001f }
                assertTrue(
                    active.size <= 2 && (active.size < 2 || active.last() - active.first() == 1),
                )
            }
        }
    }

    @Test
    fun zeroRadiusPreservesCurrentWithoutAddingGraphNodes() {
        val scope = scope()
        scope.fill(Color.Red)
        val input = scope.current
        val count = scope.graph.size
        scope.layeredProgressiveBlur(0f, BackdropEdge.Top)
        assertEquals(input, scope.current)
        assertEquals(count, scope.graph.size)
    }

    @Test
    fun invalidParametersAreRejected() {
        for (radius in listOf(-1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> {
                scope().layeredProgressiveBlur(radius, BackdropEdge.Top)
            }
        }
        assertFailsWith<IllegalArgumentException> {
            scope().layeredProgressiveBlur(8f, BackdropEdge.Top, levels = 1)
        }
        assertFailsWith<IllegalArgumentException> {
            scope().layeredProgressiveBlur(8f, BackdropEdge.Top, BackdropRamp(from = -1f))
        }
    }

    @Test
    fun unavailableAlgorithmsPreserveInputWithoutCoverageOrStages() {
        val ramp = BackdropRamp(anchors = BackdropRampAnchors.offsets(16.dp, 32.dp))
        val shader =
            object : BackdropEffectScopeImpl() {
                override val shape = RectangleShape
            }
        shader.size = Size(80f, 80f)
        shader.resolveEffects {
            progressiveBlurImpl(16f, BackdropEdge.Top, ramp, supported = false)
        }
        assertEquals(BackdropInsets.None, shader.cover)
        assertEquals(BackdropExtension.None, shader.extension)
        assertTrue(shader.passes.isEmpty())
        val layered = scope()
        val original = layered.current
        val count = layered.graph.size
        layered.layeredProgressiveBlurImpl(16f, BackdropEdge.Top, ramp, 8, supported = false)
        assertEquals(original, layered.current)
        assertEquals(count, layered.graph.size)
    }

    private fun scope() =
        BackdropRenderScope(1f, 1f, Size(80f, 80f), RectangleShape, LayoutDirection.Ltr)
}
