package top.ltfan.m3x.material

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class CardGroupShapeSkiaTest {
    private val pathShape =
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density,
            ): Outline =
                Outline.Generic(
                    Path().apply {
                        moveTo(size.width / 2, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    },
                )
        }

    @Test
    fun `without fallback the custom shape is returned unchanged`() {
        val resolved = resolveFallbackCardShape(pathShape, CardGroupShapePosition.Top, null)

        assertSame(pathShape, resolved)
        assertIs<Outline.Generic>(
            resolved.createOutline(Size(40f, 20f), LayoutDirection.Ltr, Density(1f)),
        )
    }

    @Test
    fun `fallback receives the original shape and logical position`() {
        var received: Pair<Shape, CardGroupShapePosition>? = null
        val resolved =
            resolveFallbackCardShape(pathShape, CardGroupShapePosition.End) { base, position ->
                received = base to position
                RectangleShape
            }

        assertSame(RectangleShape, resolved)
        assertEquals(pathShape to CardGroupShapePosition.End, received)
    }
}
