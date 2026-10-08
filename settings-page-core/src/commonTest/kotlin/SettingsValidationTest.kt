package top.ltfan.m3x.settingspage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsValidationTest {
    @Test
    fun longBoundsKeepIntegerPrecisionAcrossFullRange() {
        val bounds =
            NumberType.finiteLong(
                RangeBasedNumberInfo.Type.Wheel,
                Long.MIN_VALUE,
                Long.MAX_VALUE,
                2L,
                true,
            )
        assertNull(SettingsValidation.number(Long.MIN_VALUE, bounds))
        assertNull(SettingsValidation.number(Long.MAX_VALUE - 1L, bounds))
        assertEquals("Use increments of 2", SettingsValidation.number(Long.MAX_VALUE, bounds))
    }
}
