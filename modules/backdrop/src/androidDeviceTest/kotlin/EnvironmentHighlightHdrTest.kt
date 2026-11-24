package top.ltfan.m3x.backdrop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.ltfan.m3x.backdrop.backdrops.rememberCanvasBackdrop
import top.ltfan.m3x.backdrop.highlight.Highlight
import top.ltfan.m3x.backdrop.highlight.HighlightDirection
import top.ltfan.m3x.backdrop.highlight.highlight
import top.ltfan.m3x.backdrop.internal.setRuntimeShader

class EnvironmentHighlightHdrTest {
    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun blurredEnvironmentRetainsColorAndCoverageOnAnF16Target() = runRenderTest {
        val sourceAlpha = mutableFloatStateOf(.5f)
        val size = 64
        val sourceShader =
            RuntimeShader(
                """
                uniform float opacity;
                half4 main(float2 p) { return half4(2.0, 0.3, 0.1, 1.0) * opacity; }
                """
                    .trimIndent(),
            )
        sourceShader.setFloatUniform("opacity", 1f)
        val direct = renderTestShader(sourceShader, 1, 1)
        assertTrue(direct[0] > 1.5f, "F16 target red=${direct[0]}")

        setContent {
            val source = rememberCanvasBackdrop {
                val paint = Paint()
                sourceShader.setFloatUniform("opacity", sourceAlpha.floatValue)
                paint.setRuntimeShader(sourceShader)
                drawContext.canvas.drawRect(samplingBounds, paint)
            }
            Box(
                Modifier.size(size.dp).drawBackdrop(source, { RectangleShape }) {
                    val environment = image { source() }
                    replace(image {})
                    highlight(
                        Highlight(
                            width = 8.dp,
                            blurRadius = 4.dp,
                            colorGain = 0f,
                            environmentGain = 1f,
                            direction = HighlightDirection.Uniform,
                        ),
                        environment = environment,
                        blendMode = BlendMode.SrcOver,
                    )
                },
            )
        }

        val translucent = runOnIdle { captureRenderTestContentF16() }
        runOnIdle { sourceAlpha.floatValue = 1f }
        val opaque = runOnIdle { captureRenderTestContentF16() }

        val rim = (32 * size + 1) * 4
        val falloff = (32 * size + 4) * 4
        val interior = (32 * size + 32) * 4
        // The green channel stays in range when a HWUI intermediate clamps the red channel.
        assertTrue(
            translucent[rim] > translucent[rim + 1] && opaque[rim] > opaque[rim + 1],
            "Rim premultiplied RGBA: ${translucent.slice(rim..rim + 3)} to ${opaque.slice(rim..rim + 3)}",
        )
        assertEquals(opaque[rim + 1], translucent[rim + 1], .05f)
        assertEquals(opaque[rim + 3], translucent[rim + 3], .01f)
        val falloffAlpha = translucent[falloff + 3]
        assertTrue(falloffAlpha > 0f && falloffAlpha < translucent[rim + 3])
        assertEquals(opaque[falloff + 3], falloffAlpha, .01f)
        assertEquals(0f, translucent[interior + 3])
    }
}
