package top.ltfan.m3x.backdrop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.ltfan.m3x.backdrop.backdrops.rememberCanvasBackdrop
import top.ltfan.m3x.backdrop.effects.lens
import top.ltfan.m3x.backdrop.internal.setRuntimeShader

class ContourLensLayerHdrTest {
    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun cutCornerRefractionPreservesCoverageInTheRecordedF16Layer() = runRenderTest {
        val size = 64
        val mode = mutableIntStateOf(0)
        val sourceShader =
            RuntimeShader(
                """
                half4 main(float2 p) {
                    return half4(2.0, p.x < 16.0 ? 0.125 : 0.75, 0.1, 1.0) * 0.75;
                }
                """
                    .trimIndent(),
            )
        val direct = renderTestShader(sourceShader, size, size)
        val edge = (13 * size + 12) * 4
        val center = (32 * size + 12) * 4
        assertTrue(direct[edge] > 1.4f, "F16 target red=${direct[edge]}")

        val style = BackdropStyle.NoEffects {
            lens = { BackdropLens(12.dp, 12.dp) }
        }

        setContent {
            val source = rememberCanvasBackdrop {
                val paint = Paint()
                paint.setRuntimeShader(sourceShader)
                drawContext.canvas.drawRect(samplingBounds, paint)
            }
            Box(
                Modifier.size(size.dp).drawBackdrop(
                    source,
                    {
                        if (mode.intValue == 1) RoundedCornerShape(24.dp) else CutCornerShape(24.dp)
                    },
                ) {
                    when (mode.intValue) {
                        0,
                        1 -> effects { lens(12f, 12f) }
                        3 -> styledSurface(style)
                    }
                },
            )
        }

        val refracted = runOnIdle { captureRenderTestContentF16() }
        runOnIdle { mode.intValue = 1 }
        val analytic = runOnIdle { captureRenderTestContentF16() }
        runOnIdle { mode.intValue = 2 }
        val noLens = runOnIdle { captureRenderTestContentF16() }
        runOnIdle { mode.intValue = 3 }
        val styled = runOnIdle { captureRenderTestContentF16() }
        for (pixel in listOf(edge, center)) {
            for (channel in 0..3) {
                assertEquals(refracted[pixel + channel], styled[pixel + channel], .01f)
            }
        }
        // Green locates the source sample independently of the intermediate layer's color range.
        assertTrue(
            refracted[edge + 1] > .4f,
            "Cut-corner sample: ${refracted.slice(edge..edge + 3)}",
        )
        assertEquals(noLens[edge + 1], analytic[edge + 1], .025f)
        assertEquals(direct[center + 1], refracted[center + 1], .025f)
        assertEquals(.75f, refracted[edge + 3], .01f)
        assertEquals(.75f, refracted[center + 3], .01f)
        assertEquals(analytic[center], refracted[center], .01f)
        println(
            "CONTOUR_LENS_F16 red direct=${direct[center]} noLens=${noLens[center]} " +
                "analytic=${analytic[center]} contour=${refracted[center]} styled=${styled[center]}, " +
                "contour edge RGBA=${refracted.slice(edge..edge + 3)}",
        )
    }
}
