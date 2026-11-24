package top.ltfan.m3x.settingspage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import top.ltfan.m3x.settingspage.RangeBasedNumberInfo.Type

class NumberSliderMappingTest {
    @Test
    fun selectorAndWheelFallbackPreserveLongPrecisionAndStep() {
        for (type in listOf(Type.Selector, Type.Wheel)) {
            val original =
                NumberType.finiteLong(type, Long.MAX_VALUE - 10, Long.MAX_VALUE, 2, false)
            val mapping = assertNotNull(original.sliderMappingOrNull())
            assertEquals(SliderMode.Steps, mapping.bounds.sliderMode)
            assertEquals(4, mapping.steps)
            assertEquals(Long.MAX_VALUE - 6, mapping.valueAt(0.4f))
            assertEquals(type, original.type)
        }
    }

    @Test
    fun nonDivisibleDiscreteRangeCannotUseSlider() {
        assertNull(NumberType.finiteInt(Type.Wheel, 0, 10, 3, false).sliderMappingOrNull())
    }

    @Test
    fun excessiveDiscreteRangeCannotUseSlider() {
        assertNull(NumberType.finiteInt(Type.Selector, 0, 4097, 1, false).sliderMappingOrNull())
    }

    @Test
    fun indistinguishableFloatStopsCannotUseSlider() {
        assertNull(
            NumberType.finiteFloat(Type.Wheel, 1f, 1.000001f, 0.00000001f, false)
                .sliderMappingOrNull(),
        )
    }

    @Test
    fun explicitSliderRetainsContinuousMode() {
        val mapping =
            assertNotNull(
                NumberType.finiteDouble(Type.Slider, 0.0, 10.0, 3.0, true).sliderMappingOrNull(),
            )
        assertEquals(SliderMode.Continuous, mapping.bounds.sliderMode)
        assertEquals(0, mapping.steps)
    }
}
