package top.ltfan.m3x.demo

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import top.ltfan.m3x.demo.generated.resources.Res
import top.ltfan.m3x.demo.generated.resources.demo_dark
import top.ltfan.m3x.demo.generated.resources.demo_light
import top.ltfan.m3x.demo.generated.resources.demo_runtime_mode
import top.ltfan.m3x.demo.generated.resources.demo_runtime_scale
import top.ltfan.m3x.demo.generated.resources.demo_system
import top.ltfan.m3x.resource.Resource
import top.ltfan.m3x.settingspage.AbstractSettingsValue
import top.ltfan.m3x.settingspage.NumberType
import top.ltfan.m3x.settingspage.RangeBasedNumberInfo
import top.ltfan.m3x.settingspage.SettingsItem
import top.ltfan.m3x.settingspage.SettingsResources
import top.ltfan.m3x.settingspage.SettingsTree
import top.ltfan.m3x.settingspage.SettingsValue
import top.ltfan.m3x.settingspage.SliderMode

/** Demonstrates runtime options and bounds that generated items intentionally snapshot. */
@Stable
class ManualSettingsTree : SettingsTree {
    var availableModes: List<String> by mutableStateOf(listOf("System", "Light", "Dark"))
    var maximumScale: Float by mutableFloatStateOf(2f)
    var selectedMode: String by mutableStateOf("System")
    var scale: Float by mutableFloatStateOf(1f)

    override val items: List<SettingsItem> = listOf(ModeItem(), ScaleItem())

    /** Cycles the runtime option list that the generated selector captured when it was built. */
    fun toggleModes() {
        availableModes =
            if (availableModes.size == 3) listOf("System", "Dark")
            else listOf("System", "Light", "Dark")
    }

    /** Cycles the runtime bound that the generated slider captured when it was built. */
    fun toggleScaleBounds() {
        maximumScale = if (maximumScale == 2f) 4f else 2f
        scale = scale.coerceAtMost(maximumScale)
    }

    private inner class ModeItem : SettingsItem.Item.Selector<String> {
        override val id: String = "manual.mode"
        override val resources: SettingsResources = label("Runtime mode")
        override val children: List<SettingsItem>?
            get() = null

        override val shouldShow: Boolean
            get() = true

        override val enabled: Boolean
            get() = true

        override val options: List<SettingsValue<String>>
            get() = availableModes.map(::ModeValue)

        override var value: SettingsValue<String>
            get() = ModeValue(selectedMode)
            set(value) {
                selectedMode = value.value
            }
    }

    private inner class ModeValue(override val value: String) : AbstractSettingsValue<String>() {
        override val id: String = "manual.mode.$value"
        override val resources: SettingsResources = label(value)
    }

    private inner class ScaleItem : SettingsItem.Item.SingleNumber<Float> {
        override val id: String = "manual.scale"
        override val resources: SettingsResources = label("Runtime scale")
        override val children: List<SettingsItem>?
            get() = null

        override val shouldShow: Boolean
            get() = true

        override val enabled: Boolean
            get() = true

        override val type: NumberType<Float>
            get() =
                NumberType.finiteFloat(
                    RangeBasedNumberInfo.Type.Slider,
                    0f,
                    maximumScale,
                    0.1f,
                    true,
                    SliderMode.Continuous,
                )

        override var value: Float
            get() = scale
            set(value) {
                scale = value
            }
    }

    private fun label(text: String): SettingsResources =
        object : SettingsResources {
            override val label: Resource<String> =
                when (text) {
                    "Runtime mode" -> demoResource(Res.string.demo_runtime_mode)
                    "Runtime scale" -> demoResource(Res.string.demo_runtime_scale)
                    "System" -> demoResource(Res.string.demo_system)
                    "Light" -> demoResource(Res.string.demo_light)
                    "Dark" -> demoResource(Res.string.demo_dark)
                    else -> Resource.Global.Static(text)
                }
        }
}
