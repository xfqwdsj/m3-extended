package top.ltfan.m3x.backdrop

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import top.ltfan.m3x.backdrop.highlight.Highlight
import top.ltfan.m3x.backdrop.highlight.HighlightAngularCut

class HighlightConfigurationTest {
    @Test
    fun basicAndContourHaveIndependentWidthDefaults() {
        val basic = Highlight.Basic()
        val contour = Highlight.Contour()
        assertNotEquals(basic.width, contour.width)
        assertEquals(contour.width, Highlight().width)
        assertEquals(contour.width, Highlight {}.width)
        val custom = Highlight.Basic(width = 3.dp)
        assertEquals(custom.width, custom.copy(opacity = .5f).width)
    }

    @Test
    fun exclusionsValidateAngularRangesAndFeather() {
        assertEquals(-180f, HighlightAngularCut(-180f, 180f, 0f).angle)
        assertFailsWith<IllegalArgumentException> { HighlightAngularCut(Float.NaN) }
        assertFailsWith<IllegalArgumentException> { HighlightAngularCut(0f, 180.1f) }
        assertFailsWith<IllegalArgumentException> { HighlightAngularCut(0f, 60f, 121f) }
        assertFailsWith<IllegalArgumentException> {
            HighlightAngularCut(0f, 0f, Float.POSITIVE_INFINITY)
        }
    }

    @Test
    fun highlightsRejectInvalidDistancesLightOpacityAndColor() {
        assertFailsWith<IllegalArgumentException> { Highlight.Basic(width = Float.NaN.dp) }
        assertFailsWith<IllegalArgumentException> { Highlight(width = Float.NaN.dp) }
        assertFailsWith<IllegalArgumentException> { Highlight(width = (-1).dp) }
        assertFailsWith<IllegalArgumentException> { Highlight(blurRadius = Float.NaN.dp) }
        assertFailsWith<IllegalArgumentException> { Highlight(blurRadius = (-1).dp) }
        assertFailsWith<IllegalArgumentException> { Highlight(colorGain = Float.NaN) }
        assertFailsWith<IllegalArgumentException> { Highlight(colorGain = -0.1f) }
        assertFailsWith<IllegalArgumentException> {
            Highlight(environmentGain = Float.POSITIVE_INFINITY)
        }
        assertFailsWith<IllegalArgumentException> { Highlight(environmentGain = -0.1f) }
        assertFailsWith<IllegalArgumentException> { Highlight(opacity = Float.NaN) }
        assertFailsWith<IllegalArgumentException> { Highlight(opacity = 1.01f) }
        assertFailsWith<IllegalArgumentException> { Highlight(color = Color.Unspecified) }
    }

    @Test
    fun contourBuilderAndCopyRetainExplicitWidthAndColor() {
        val customWidth = 3.dp
        val fixedColor = Color.Red.copy(alpha = .25f)
        val contour = Highlight {
            width = customWidth
            color = fixedColor
        }
        assertEquals(customWidth, contour.width)
        assertEquals(fixedColor, contour.color)
        assertEquals(
            Highlight(width = customWidth, color = fixedColor, opacity = .5f),
            contour.copy(opacity = .5f),
        )
    }
}
