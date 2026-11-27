package top.ltfan.m3x.settingspage.material

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import top.ltfan.m3x.resource.Resource
import top.ltfan.m3x.resource.compose.composeResource
import top.ltfan.m3x.settingspage.NumberType
import top.ltfan.m3x.settingspage.SettingsItem
import top.ltfan.m3x.settingspage.SettingsResetMode
import top.ltfan.m3x.settingspage.SettingsResources
import top.ltfan.m3x.settingspage.SettingsValidation
import top.ltfan.m3x.settingspage.SettingsValue

internal object SettingsPageFixtures {
    internal class RuntimeBoundsSliderItem : SettingsItem.Item.SingleNumber<Float> {
        var maximumScale: Float by mutableFloatStateOf(2f)
        private val state = mutableFloatStateOf(1f)

        override var value: Float
            get() = state.floatValue
            set(value) {
                state.floatValue = value
            }

        override val type: NumberType<Float>
            get() =
                NumberType.finiteFloat(
                    Slider,
                    0f,
                    maximumScale,
                    0.1f,
                    true,
                    Continuous,
                )

        override val id: String = "runtime-scale"
        override val children: List<SettingsItem>? = null
        override val shouldShow: Boolean = true
        override val enabled: Boolean = true
        override val resources: SettingsResources =
            object : SettingsResources {
                override val label: Resource<String> = Resource.Global.Static("Runtime scale")
            }
    }

    internal class SliderItem(
        override val type: NumberType<Int> =
            NumberType.finiteInt(
                Slider,
                0,
                10,
                1,
                false,
            ),
        override val defaultValue: Int? = null,
        override val resetMode: SettingsResetMode = SettingsResetMode.None,
    ) : SettingsItem.Item.SingleNumber<Int> {
        private val state = mutableIntStateOf(5)
        var writes: Int = 0
            private set

        override var value: Int
            get() = state.intValue
            set(value) {
                state.intValue = value
                writes++
            }

        override val id: String = "level"
        override val children: List<SettingsItem>? = null
        override val shouldShow: Boolean = true
        override val enabled: Boolean = true
        override val resources: SettingsResources =
            object : SettingsResources {
                override val label: Resource<String> = Resource.Global.Static("Level")
            }
    }

    internal class SwitchItem : SettingsItem.Item.Switch {
        private val state = mutableStateOf(false)
        var writes = 0
            private set

        override var value: Boolean
            get() = state.value
            set(value) {
                state.value = value
                writes++
            }

        override val id = "switch"
        override val children: List<SettingsItem>? = null
        override val shouldShow = true
        override val enabled = true
        override val resources =
            object : SettingsResources {
                override val label: Resource<String> = Resource.Global.Static("Switch")
                override val description: Resource<String> = Resource.Global.Static("Off")
            }
    }

    internal class DefaultSwitchItem(
        initialValue: Boolean = true,
        override val enabled: Boolean = true,
        override val resetMode: SettingsResetMode = Menu,
        private val labelText: String = "Default switch",
        override val id: String = "default-switch",
    ) : SettingsItem.Item.Switch {
        private var storedValue = initialValue
        override var value: Boolean
            get() = storedValue
            set(value) {
                storedValue = value
                writes++
            }

        var writes = 0
        override val defaultValue = false
        override val children: List<SettingsItem>? = null
        override val shouldShow = true
        override val resources =
            object : SettingsResources {
                override val label: Resource<String> = Resource.Global.Static(labelText)
            }
    }

    internal class RangeItem : SettingsItem.Item.NumberRange<Int> {
        override val id = "range"
        override var value = 1 to 9
        override val startType = NumberType.inputInt()
        override val endType = NumberType.inputInt()
        override val children: List<SettingsItem>? = null
        override val shouldShow = true
        override val enabled = true
        override val resources =
            object : SettingsResources {
                override val label: Resource<String> = Resource.Global.Static("Range")
            }
    }

    internal class RangeSliderItem : SettingsItem.Item.NumberRange<Int> {
        private val type = NumberType.finiteInt(Slider, 0, 10, 1, false)
        private val state = mutableStateOf(2 to 8)
        var writes: Int = 0
            private set

        override val id = "range-slider"
        override var value: Pair<Int, Int>
            get() = state.value
            set(value) {
                state.value = value
                writes++
            }

        override val startType = type
        override val endType = type
        override val children: List<SettingsItem>? = null
        override val shouldShow = true
        override val enabled = true
        override val resources =
            object : SettingsResources {
                override val label: Resource<String> = Resource.Global.Static("Range slider")
            }
    }

    internal class SelectorItem : SettingsItem.Item.Selector<Int> {
        override val id = "selector"
        override val options = listOf(option("one", "One", 1), option("two", "Two", 2))
        override var value: SettingsValue<Int> = options.first()
        override val children: List<SettingsItem>? = null
        override val shouldShow = true
        override val enabled = true
        override val resources =
            object : SettingsResources {
                override val label: Resource<String> = Resource.Global.Static("Choice")
            }
    }

    internal class MultiSelectorItem : SettingsItem.Item.MultiSelector<Int> {
        override val id = "multi-selector"
        override val options = listOf(option("one", "One", 1), option("two", "Two", 2))
        override var value = setOf(options.first())
        override val children: List<SettingsItem>? = null
        override val shouldShow = true
        override val enabled = true
        override val resources =
            object : SettingsResources {
                override val label: Resource<String> = Resource.Global.Static("Choices")
            }
    }

    private fun option(optionId: String, label: String, number: Int): SettingsValue<Int> =
        object : SettingsValue<Int> {
            override val id = optionId
            override val value = number
            override val resources =
                object : SettingsResources {
                    override val label: Resource<String> = Resource.Global.Static(label)
                }
        }

    internal class TextItem(override val id: String = "name", label: String = "Name") :
        SettingsItem.Item.Text {
        private val state = mutableStateOf("before")
        override var value: String
            get() = state.value
            set(value) {
                state.value = value
            }

        override val minLength: Int = 2

        override fun validate(candidate: String): String? =
            SettingsValidation.text(candidate, minLength, maxLength)

        override val children: List<SettingsItem>? = null
        override val shouldShow: Boolean = true
        override val enabled: Boolean = true
        override val resources: SettingsResources =
            object : SettingsResources {
                override val label: Resource<String> = composeResource { label }
            }
    }

    internal class ConditionalTextItem(
        override val id: String = "conditional",
        initiallyShown: Boolean = true,
    ) : SettingsItem.Item.Text {
        private val visibility = mutableStateOf(initiallyShown)
        var shown: Boolean
            get() = visibility.value
            set(value) {
                visibility.value = value
            }

        override var value: String = "value"
        override val minLength: Int = 0
        override val maxLength: Int = 20

        override fun validate(candidate: String): String? = null

        override val children: List<SettingsItem>? = null
        override val shouldShow: Boolean
            get() = visibility.value

        override val enabled: Boolean = true
        override val resources: SettingsResources =
            object : SettingsResources {
                override val label: Resource<String> = Resource.Global.Static("Conditional row")
            }
    }

    internal class ConditionalCategory(
        item: SettingsItem.Item<*>,
        initiallyShown: Boolean = true,
    ) : SettingsItem.Category {
        private val visibility = mutableStateOf(initiallyShown)
        var shown: Boolean
            get() = visibility.value
            set(value) {
                visibility.value = value
            }

        override val id = "conditional-category"
        override val children: List<SettingsItem> = listOf(item)
        override val shouldShow: Boolean
            get() = visibility.value

        override val enabled: Boolean = true
        override val resources: SettingsResources =
            object : SettingsResources {
                override val label: Resource<String> =
                    Resource.Global.Static("Conditional category")
            }
    }
}
