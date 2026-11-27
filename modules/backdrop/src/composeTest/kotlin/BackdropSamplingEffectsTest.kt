package top.ltfan.m3x.backdrop

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import kotlin.test.Test
import kotlin.test.assertEquals

class BackdropSamplingEffectsTest : PlatformGraphicsTest() {
    @Test
    fun translatedAsymmetricSamplingUnionsEveryIntermediateRegion() {
        val scope = samplingTestScope(Size(10f, 10f))
        val stageBounds = mutableListOf<Rect>()

        scope.resolveEffects {
            addEffect(BackdropSampling.translated(20f, -5f)) {
                stageBounds += scope.samplingBounds
                blurEffect()
            }
            addEffect(BackdropSampling.outsets(1f, 2f, 3f, 4f)) {
                stageBounds += scope.samplingBounds
                blurEffect()
            }
        }

        assertEquals(Rect(19f, -7f, 33f, 9f), stageBounds[0])
        assertEquals(Rect(-1f, -2f, 13f, 14f), stageBounds[1])
        assertEquals(BackdropExtension(1f, 7f, 23f, 4f), scope.extension)
    }

    @Test
    fun fractionalIntermediateTexelsContributeToEarlierSampling() {
        val scope = samplingTestScope(Size(10f, 10f))
        val stageBounds = mutableListOf<Rect>()
        scope.resolveEffects {
            addEffect(
                sampling = { output ->
                    Rect(output.left * 10f, output.top, output.right * 10f, output.bottom)
                },
            ) {
                stageBounds += scope.samplingBounds
                blurEffect()
            }
            addEffect(BackdropSampling.translated(.25f, -.25f)) {
                stageBounds += scope.samplingBounds
                blurEffect()
            }
        }
        assertEquals(Rect(0f, -1f, 110f, 10f), stageBounds[0])
        assertEquals(Rect(0f, -1f, 11f, 10f), stageBounds[1])
        assertEquals(BackdropExtension(0f, 1f, 100f, 0f), scope.extension)
    }

    private fun blurEffect() = BlurEffect(1f, 1f)
}
