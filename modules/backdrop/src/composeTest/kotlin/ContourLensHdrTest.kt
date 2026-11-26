package top.ltfan.m3x.backdrop

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.ltfan.m3x.backdrop.effects.ContourLens
import top.ltfan.m3x.backdrop.internal.BoundaryGeometry

internal class ContourLensHdrTest {
    @Test
    fun contourRefractionPreservesExtendedPremultipliedChannels() {
        initializeTestGraphics()
        val outline =
            CutCornerShape(24.dp).createOutline(Size(64f, 64f), LayoutDirection.Ltr, Density(1f))
        val contour = ContourLens(BoundaryGeometry(outline))
        val source =
            RuntimeShader(
                "half4 main(float2 p) { return half4(1.25 + p.x / 32.0, 0.1 + p.y / 64.0, -0.1, 0.75); }",
            )
        val before = renderTestShader(source, 64, 64)
        val edge = (13 * 64 + 12) * 4
        val clear = (32 * 64 + 32) * 4
        assertTrue(before[edge] > 1.5f && before[edge + 2] < 0f, "F16 baseline was clipped")

        for (dispersion in listOf(false, true)) {
            val shader = RuntimeShader(contour.shader(dispersion))
            contour.bind(shader, 12f)
            shader.setFloatUniform("size", 64f, 64f)
            shader.setFloatUniform("refractionHeight", 12f)
            shader.setFloatUniform("refractionAmount", -12f)
            shader.setFloatUniform("depthEffect", 0f)
            shader.bindTestInput("content", source)
            val after = renderTestShader(shader, 64, 64)
            assertTrue(
                after[edge] > before[edge] + .04f,
                "Edge red was clipped or not refracted: ${after[edge]}",
            )
            assertEquals(before[edge + 2], after[edge + 2], .003f)
            assertEquals(before[edge + 3], after[edge + 3], .003f)
            for (channel in 0..3) assertEquals(
                before[clear + channel],
                after[clear + channel],
                .003f,
            )
        }
    }
}
