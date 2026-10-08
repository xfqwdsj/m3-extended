package top.ltfan.m3x.settingspage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SettingsNumbersTest {
    @Test
    fun continuousIntMappingKeepsPositionEvenWhenValuesRepeat() {
        val mapping =
            SettingsNumbers.SliderMapping(
                NumberType.finiteInt(RangeBasedNumberInfo.Type.Slider, 0, 2, 1, true),
            )
        assertEquals(0, mapping.valueAt(0.1f))
        assertEquals(0, mapping.valueAt(0.2f))
        assertEquals(1, mapping.valueAt(0.5f))
        assertEquals(2, mapping.valueAt(1f))
        assertTrue(mapping.positionOf(1) in 0.49f..0.51f)
    }

    @Test
    fun identicalNumericBoundsFollowDeclaredValueSemantics() {
        val intMapping =
            SettingsNumbers.SliderMapping(
                NumberType.finiteInt(RangeBasedNumberInfo.Type.Slider, 0, 2, 1, true),
            )
        val doubleMapping =
            SettingsNumbers.SliderMapping(
                NumberType.finiteDouble(RangeBasedNumberInfo.Type.Slider, 0.0, 2.0, 1.0, true),
            )
        assertEquals(1, intMapping.valueAt(0.25f))
        assertEquals(0.5, doubleMapping.valueAt(0.25f))
        assertEquals(null, SettingsNumbers.parse("0.5", NumberKind.Int))
        assertEquals(0.5, SettingsNumbers.parse("0.5", NumberKind.Double))
        assertTrue(
            SettingsValidation.numberIssue(
                0.5,
                NumberType.finiteInt(
                    RangeBasedNumberInfo.Type.Slider,
                    0,
                    2,
                    1,
                    true,
                ),
            ) is SettingsValidation.Issue.InvalidStep,
        )
    }

    @Test
    fun fullLongRangeMapsExactlyAtEndpointsAndStaysOrdered() {
        val mapping =
            SettingsNumbers.SliderMapping(
                NumberType.finiteLong(
                    RangeBasedNumberInfo.Type.Slider,
                    Long.MIN_VALUE,
                    Long.MAX_VALUE,
                    1L,
                    true,
                ),
            )
        assertEquals(Long.MIN_VALUE, mapping.valueAt(0f))
        assertEquals(Long.MAX_VALUE, mapping.valueAt(1f))
        val values = listOf(0f, 0.25f, 0.5f, 0.75f, 1f).map { mapping.valueAt(it) as Long }
        assertTrue(values.zipWithNext().all { (a, b) -> a < b })
    }

    @Test
    fun adjacentLargeLongValuesStayDistinctAtBoundsAndInRanges() {
        val lower = Long.MAX_VALUE - 1
        val upper = Long.MAX_VALUE
        val type = NumberType.finiteLong(RangeBasedNumberInfo.Type.Slider, lower, upper, 1L, true)
        val mapping = SettingsNumbers.SliderMapping(type)
        assertEquals(0f, mapping.positionOf(lower))
        assertEquals(1f, mapping.positionOf(upper))
        assertEquals(upper, mapping.valueAt(1f))
        assertEquals(
            SettingsValidation.Issue.RangeOrder,
            SettingsValidation.rangeIssue(upper to lower, type, type),
        )
        assertTrue(
            SettingsValidation.numberIssue(Long.MAX_VALUE - 2, type)
                is SettingsValidation.Issue.OutOfBounds,
        )
    }

    @Test
    fun floatingStepsHaveOneValuePerStop() {
        val mapping =
            SettingsNumbers.SliderMapping(
                NumberType.finiteDouble(
                    RangeBasedNumberInfo.Type.Slider,
                    0.0,
                    1.0,
                    0.1,
                    true,
                    SliderMode.Steps,
                ),
            )
        assertEquals(9, mapping.steps)
        val values = (0..10).map { mapping.valueAt(it / 10f) as Double }
        assertEquals(11, values.toSet().size)
        assertEquals(1.0, values.last())
    }

    @Test
    fun misalignedStepsAreRejectedButPreciseSliderInputIsValid() {
        assertFailsWith<IllegalArgumentException> {
            SettingsNumbers.SliderMapping(
                NumberType.finiteDouble(
                    RangeBasedNumberInfo.Type.Slider,
                    0.0,
                    1.0,
                    0.3,
                    true,
                    SliderMode.Steps,
                ),
            )
        }
        val finite =
            NumberType.finiteDouble(
                RangeBasedNumberInfo.Type.Slider,
                0.0,
                1.0,
                0.1,
                true,
                SliderMode.Steps,
            )
        assertEquals(null, SettingsValidation.numberIssue(0.35, finite))
    }

    @Test
    fun discreteStopLimitAndOffGridStepperEndpoint() {
        assertFailsWith<IllegalArgumentException> {
            SettingsNumbers.SliderMapping(
                NumberType.finiteInt(
                    RangeBasedNumberInfo.Type.Slider,
                    0,
                    4097,
                    1,
                    false,
                    SliderMode.Steps,
                ),
            )
        }
        val wheel = NumberType.finiteInt(RangeBasedNumberInfo.Type.Wheel, 0, 10, 3, false)
        assertEquals(9, SettingsNumbers.stepNumber(9, wheel, increase = true))
        assertEquals(6, SettingsNumbers.stepNumber(9, wheel, increase = false))
    }

    @Test
    fun floatStopsAreAcceptedOnlyIfDistinctOnTheTarget() {
        val upper = 1f + 0.0000002384185791f
        val result = runCatching {
            SettingsNumbers.SliderMapping(
                NumberType.finiteFloat(
                    RangeBasedNumberInfo.Type.Slider,
                    1f,
                    upper,
                    (upper - 1f) / 20f,
                    true,
                    SliderMode.Steps,
                ),
            )
        }
        result.getOrNull()?.let { mapping ->
            val values = (0..20).map { mapping.valueAt(it / 20f) }
            assertEquals(values.size, values.toSet().size)
        }
        result.exceptionOrNull()?.let { assertTrue(it is IllegalArgumentException) }
    }
}
