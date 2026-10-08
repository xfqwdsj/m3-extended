package top.ltfan.m3x.settingspage.processor

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import top.ltfan.m3x.settingspage.NumberKind

class FiniteBoundsTest {
    @Test
    fun adjacentLongBoundsAreOrderedWithoutDoubleRounding() {
        val lower = 9_007_199_254_740_992L
        val upper = lower + 1L
        assertTrue(validFiniteBounds(lower, upper, 1L, NumberKind.Long))
        assertFalse(validFiniteBounds(upper, lower, 1L, NumberKind.Long))
        assertFalse(validFiniteBounds(Long.MAX_VALUE, Long.MAX_VALUE - 1L, 1L, NumberKind.Long))
        assertTrue(validFiniteBounds(Long.MIN_VALUE, Long.MAX_VALUE, 1L, NumberKind.Long))
    }

    @Test
    fun invalidStepAndFloatingBoundsAreRejected() {
        assertFalse(validFiniteBounds(0, 10, 0, NumberKind.Int))
        assertFalse(validFiniteBounds(Double.NaN, 1.0, 0.1, NumberKind.Double))
        assertFalse(validFiniteBounds(1.0, 0.0, 0.1, NumberKind.Double))
    }
}
