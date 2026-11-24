package top.ltfan.m3x.backdrop

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BackdropSamplingTest {
    @Test
    fun samplerMustReturnFiniteNonInvertedBounds() {
        val scope = samplingTestScope(Size(10f, 10f))

        assertFailsWith<IllegalArgumentException> {
            scope.resolveEffects {
                addEffect(sampling = { Rect(Float.NaN, 0f, 1f, 1f) }) { blurEffect() }
            }
        }
        assertFailsWith<IllegalArgumentException> {
            scope.resolveEffects {
                addEffect(sampling = { Rect(2f, 0f, 1f, 1f) }) { blurEffect() }
            }
        }
    }

    @Test
    fun coverageRequestsMergePerSideAndAddToCallerCoverage() {
        val scope = samplingTestScope(Size(10f, 10f))

        scope.resolveEffects(
            effects = {
                cover(BackdropInsets(left = 3.dp, top = 1.dp, right = 2.dp, bottom = 5.dp))
                cover(BackdropInsets(left = 1.dp, top = 4.dp, right = 4.dp, bottom = 2.dp))
            },
            requested = BackdropInsets.all(1.dp),
        )

        assertEquals(
            BackdropInsets(left = 3.dp, top = 4.dp, right = 4.dp, bottom = 5.dp),
            scope.cover,
        )
        assertEquals(BackdropExtension(4f, 5f, 5f, 6f), scope.extension)
    }

    @Test
    fun effectsRunOnceAndDeferredFactoriesSeeResolvedExtensionAndStageInput() {
        val scope = samplingTestScope(Size(10f, 8f))
        var producerCalls = 0
        var factoryCalls = 0
        var factoryExtension: BackdropExtension? = null
        var factoryBounds: Rect? = null

        scope.resolveEffects {
            producerCalls++
            addEffect(BackdropSampling.outsets(2f, 3f, 4f, 5f)) {
                factoryCalls++
                factoryExtension = extension
                factoryBounds = scope.samplingBounds
                blurEffect()
            }
        }

        assertEquals(1, producerCalls)
        assertEquals(1, factoryCalls)
        assertEquals(BackdropExtension(2f, 3f, 4f, 5f), factoryExtension)
        assertEquals(Rect(-2f, -3f, 14f, 13f), factoryBounds)
    }

    @Test
    fun unspecifiedSizeStillAllowsPointwiseEffects() {
        val scope = samplingTestScope(Size.Unspecified)
        var factoryRan = false

        scope.resolveEffects {
            addEffect(BackdropSampling.Identity) {
                factoryRan = true
                blurEffect()
            }
        }

        assertEquals(true, factoryRan)
        assertEquals(Rect.Zero, scope.samplingBounds)
        assertEquals(BackdropExtension.None, scope.extension)
    }

    private fun blurEffect() = BlurEffect(1f, 1f)
}

internal fun samplingTestScope(size: Size): BackdropEffectScopeImpl =
    object : BackdropEffectScopeImpl() {
            override val shape: Shape = RectangleShape
        }
        .apply { this.size = size }
