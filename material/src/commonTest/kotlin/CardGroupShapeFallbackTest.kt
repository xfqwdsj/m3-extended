package top.ltfan.m3x.material

import kotlin.test.Test
import kotlin.test.assertEquals

class CardGroupShapeFallbackTest {
    @Test
    fun `outer positions account for orientation and reverse layout`() {
        assertEquals(CardGroupShapePosition.Top, fallbackCardPosition(true, false, false))
        assertEquals(CardGroupShapePosition.Bottom, fallbackCardPosition(true, false, true))
        assertEquals(CardGroupShapePosition.Bottom, fallbackCardPosition(true, true, false))
        assertEquals(CardGroupShapePosition.Top, fallbackCardPosition(true, true, true))
        assertEquals(CardGroupShapePosition.Start, fallbackCardPosition(false, false, false))
        assertEquals(CardGroupShapePosition.End, fallbackCardPosition(false, false, true))
        assertEquals(CardGroupShapePosition.End, fallbackCardPosition(false, true, false))
        assertEquals(CardGroupShapePosition.Start, fallbackCardPosition(false, true, true))
    }
}
