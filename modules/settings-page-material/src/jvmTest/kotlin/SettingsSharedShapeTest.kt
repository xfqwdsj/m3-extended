package top.ltfan.m3x.settingspage.material

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Interpolatable
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import top.ltfan.m3x.compose.animation.InterpolatedShape
import top.ltfan.m3x.material.theme.toMaterialShapes
import top.ltfan.m3x.material.theming.capsule.capsuleShapes

class SettingsSharedShapeTest {
    @Test
    fun asymmetricCornersInterpolateAndReverseWithProgress() {
        val progress = mutableFloatStateOf(0f)
        val shape =
            InterpolatedShape(
                RoundedCornerShape(topStart = 16.dp, topEnd = 12.dp, bottomEnd = 4.dp),
                RoundedCornerShape(24.dp),
            ) {
                progress.floatValue
            }

        fun corners(): List<Float> {
            val outline =
                shape.createOutline(Size(180f, 120f), LayoutDirection.Ltr, Density(1f))
                    as Outline.Rounded
            with(outline.roundRect) {
                return listOf(
                    topLeftCornerRadius.x,
                    topRightCornerRadius.x,
                    bottomRightCornerRadius.x,
                    bottomLeftCornerRadius.x,
                )
            }
        }

        assertEquals(listOf(16f, 12f, 4f, 0f), corners())
        progress.floatValue = 0.5f
        assertEquals(listOf(20f, 18f, 14f, 12f), corners())
        progress.floatValue = 1f
        assertEquals(listOf(24f, 24f, 24f, 24f), corners())
        progress.floatValue = 0.25f
        assertEquals(listOf(18f, 15f, 9f, 6f), corners())
    }

    @Test
    fun capsuleUsesItsNativeIntermediateContour() {
        val native = capsuleShapes().toMaterialShapes()
        val row = native.extraSmall
        val editor = native.extraLarge
        val size = Size(180f, 120f)
        val density = Density(1f)
        val halfway = Interpolatable.lerp(row, editor, 0.5f) as Shape
        val samples = (1..20).map { Offset(it.toFloat(), it.toFloat()) }

        fun coverage(outline: Outline): List<Boolean> {
            val image = ImageBitmap(size.width.toInt(), size.height.toInt())
            Canvas(image).drawPath((outline as Outline.Generic).path, Paint())
            val pixels = image.toPixelMap()
            return samples.map { pixels[it.x.toInt(), it.y.toInt()].alpha > 0.5f }
        }

        val expected = coverage(halfway.createOutline(size, LayoutDirection.Ltr, density))
        val source = coverage(row.createOutline(size, LayoutDirection.Ltr, density))
        val target = coverage(editor.createOutline(size, LayoutDirection.Ltr, density))
        val shared = InterpolatedShape(row, editor) { 0.5f }
        assertNotEquals(source, expected)
        assertNotEquals(target, expected)
        assertEquals(expected, coverage(shared.createOutline(size, LayoutDirection.Ltr, density)))
    }
}
