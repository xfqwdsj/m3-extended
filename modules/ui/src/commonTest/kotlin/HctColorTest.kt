package top.ltfan.m3x.compose.ui

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.ui.color.Contrast
import top.ltfan.m3x.compose.ui.color.ViewingConditions
import top.ltfan.m3x.compose.ui.color.toColor
import top.ltfan.m3x.compose.ui.color.toHct

class HctColorTest {
    @Test
    fun composeColorsRoundTripAndExposeMaterialToneAdjustment() {
        val blue = Color.Blue.toHct()
        assertEquals(Color.Blue, blue.toColor())
        assertEquals(Color.Blue, blue.inViewingConditions(ViewingConditions.DEFAULT).toColor())
        blue.setTone(90.0)
        assertEquals(90.035, blue.tone, 0.01)
        assertEquals(282.239, blue.hue, 0.01)
        assertEquals(19.144, blue.chroma, 0.01)
        assertEquals(blue.toColor(), blue.toColor().toHct().toColor())
        val translucent = Color(0x804488cc)
        assertEquals(translucent, translucent.toHct().toColor())
    }

    @Test
    fun toneContrastReportsGamutBoundaries() {
        assertEquals(21.0, Contrast.ratioOfTones(0.0, 100.0), 0.00001)
        assertEquals(1.0, Contrast.ratioOfTones(50.0, 50.0), 0.00001)
        assertNull(Contrast.lighter(100.0, 7.0))
        assertNull(Contrast.darker(0.0, 7.0))
        val lighter = Contrast.lighter(20.0, 7.0)!!
        val darker = Contrast.darker(90.0, 7.0)!!
        assertTrue(Contrast.ratioOfTones(20.0, lighter) >= 7.0)
        assertTrue(Contrast.ratioOfTones(90.0, darker) >= 7.0)
    }
}
