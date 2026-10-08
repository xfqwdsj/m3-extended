package top.ltfan.m3x.settingspage

/**
 * Returns the requested slider domain, or a stepped domain for other numeric presentations, when
 * representable.
 */
public fun NumberType.Finite<*>.sliderMappingOrNull(): SettingsNumbers.SliderMapping? {
    val slider =
        if (type == RangeBasedNumberInfo.Type.Slider) this
        else
            when (kind) {
                NumberKind.Int ->
                    NumberType.finiteInt(
                        RangeBasedNumberInfo.Type.Slider,
                        min.toInt(),
                        max.toInt(),
                        step.toInt(),
                        allowInput,
                        SliderMode.Steps,
                    )

                NumberKind.Long ->
                    NumberType.finiteLong(
                        RangeBasedNumberInfo.Type.Slider,
                        min.toLong(),
                        max.toLong(),
                        step.toLong(),
                        allowInput,
                        SliderMode.Steps,
                    )

                NumberKind.Float ->
                    NumberType.finiteFloat(
                        RangeBasedNumberInfo.Type.Slider,
                        min.toFloat(),
                        max.toFloat(),
                        step.toFloat(),
                        allowInput,
                        SliderMode.Steps,
                    )

                NumberKind.Double ->
                    NumberType.finiteDouble(
                        RangeBasedNumberInfo.Type.Slider,
                        min.toDouble(),
                        max.toDouble(),
                        step.toDouble(),
                        allowInput,
                        SliderMode.Steps,
                    )
            }
    return try {
        SettingsNumbers.SliderMapping(slider)
    } catch (_: IllegalArgumentException) {
        null
    }
}
