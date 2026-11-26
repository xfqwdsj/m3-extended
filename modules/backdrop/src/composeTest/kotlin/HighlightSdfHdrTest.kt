package top.ltfan.m3x.backdrop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.IntRect
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import top.ltfan.m3x.backdrop.highlight.HighlightSdfField
import top.ltfan.m3x.backdrop.highlight.HighlightSdfLayout
import top.ltfan.m3x.backdrop.highlight.bakeHighlightCpuSdf
import top.ltfan.m3x.backdrop.highlight.bakeHighlightGpuSdf
import top.ltfan.m3x.backdrop.highlight.environmentHighlightTileShader
import top.ltfan.m3x.backdrop.highlight.uploadHighlightSdf
import top.ltfan.m3x.backdrop.internal.BoundaryGeometry

internal class HighlightSdfHdrTest {
    @Test
    fun textureSamplePreservesRadianceInTheExistingF16ShaderHarness() = runBlocking {
        initializeTestGraphics()
        val geometry = BoundaryGeometry(Outline.Rectangle(Rect(0f, 0f, 40f, 32f)))
        val layout = HighlightSdfLayout(listOf(IntRect(0, 0, 40, 32)), 2, 8f)
        uploadHighlightSdf(layout, bakeHighlightCpuSdf(layout, geometry)).use {
            assertExtendedRange(geometry, it)
        }
        if (testHardwareHighlightSdf) {
            requireNotNull(bakeHighlightGpuSdf(layout, geometry)).use {
                assertExtendedRange(geometry, it)
            }
        }
    }

    private fun assertExtendedRange(geometry: BoundaryGeometry, field: HighlightSdfField) {
        val shader =
            RuntimeShader(
                environmentHighlightTileShader(
                    boundary = geometry,
                    sharedDirectionCurve = true,
                    useSdf = true,
                    usesSdfValidityMask = field.usesValidityMask,
                ),
            )
        shader.setFloatUniform("origin", 0f, 0f)
        shader.setFloatUniform("tileBounds", 0f, 0f, 40f, 32f)
        shader.setFloatUniform("width", 8f)
        shader.setFloatUniform("colorGain", 4f)
        shader.setColorUniform("fixedColor", Color.White)
        shader.setFloatUniform("environmentGain", 0f)
        shader.setFloatUniform("directionVector", 1f, 0f)
        shader.setFloatUniform("directionEnabled", 0f)
        shader.setFloatUniform("directionMode", 0f)
        shader.setFloatUniform("directionStrengthRange", 1f, 1f)
        shader.setFloatUniform("directionStrengthCurve", 0f, 0f, 1f, 1f)
        shader.setFloatUniform("directionWidthRange", 1f, 1f)
        check(field.bind(shader, 0, Offset.Zero, null) == null)
        val pixels = renderTestShader(shader, 40, 32, premultiplied = true)
        assertTrue(pixels[(16 * 40 + 1) * 4] > 1.5f)
        assertTrue(pixels[(16 * 40 + 20) * 4 + 3] < .01f)
    }
}

internal expect val testHardwareHighlightSdf: Boolean
