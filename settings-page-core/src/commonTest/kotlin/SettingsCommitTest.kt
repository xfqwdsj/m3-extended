package top.ltfan.m3x.settingspage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import top.ltfan.m3x.resource.Resource

class SettingsCommitTest {
    @Test
    fun rejectedValueDoesNotReachSetterAndPreciseSliderValueDoes() {
        val item = NumericItem()
        assertIs<SettingsCommitResult.BuiltInIssue>(SettingsCommit.tryCommit(item, 12.0))
        assertEquals(0, item.writes)
        assertEquals(SettingsCommitResult.AcceptedLocally, SettingsCommit.tryCommit(item, 0.35))
        assertEquals(1, item.writes)
        assertEquals(0.35, item.value)
        assertEquals(
            SettingsCommitResult.InvalidCandidateType,
            SettingsCommit.tryCommitDraft(item, "wrong"),
        )
        assertEquals(1, item.writes)
    }

    @Test
    fun restoringDefaultsUsesNormalAvailabilityAndValidation() {
        val switch = DefaultSwitchItem()
        assertEquals(SettingsCommitResult.AcceptedLocally, SettingsCommit.tryRestoreDefault(switch))
        assertFalse(switch.value)
        assertEquals(1, switch.writes)

        val missing = DefaultSwitchItem(defaultType = SettingsResetMode.None)
        assertEquals(SettingsCommitResult.Unavailable, SettingsCommit.tryRestoreDefault(missing))
        val hidden = DefaultSwitchItem().apply { shown = false }
        assertEquals(SettingsCommitResult.Unavailable, SettingsCommit.tryRestoreDefault(hidden))
        val disabled = DefaultSwitchItem().apply { enabled = false }
        assertEquals(SettingsCommitResult.Unavailable, SettingsCommit.tryRestoreDefault(disabled))

        val invalid = DefaultTextItem()
        assertIs<SettingsCommitResult.BuiltInIssue>(SettingsCommit.tryRestoreDefault(invalid))
        assertEquals("valid", invalid.value)
        assertEquals(0, invalid.writes)
    }

    @Test
    fun restoringSelectorAndMultiSelectorPreservesDefaultOptionPayload() {
        val selector = DefaultSelectorItem()
        val modifiedSelection = selector.value
        assertEquals(
            SettingsCommitResult.AcceptedLocally,
            SettingsCommit.tryRestoreDefault(selector),
        )
        assertEquals("default payload", selector.value.value.payload)
        assertNotSame(selector.value, modifiedSelection)

        val multi = DefaultMultiSelectorItem()
        val incoming = multi.defaultValue.single()
        assertEquals(SettingsCommitResult.AcceptedLocally, SettingsCommit.tryRestoreDefault(multi))
        assertSame(incoming, multi.value.single())
        assertEquals("default multi payload", multi.value.single().value.payload)
    }

    @Test
    fun ordinarySelectorCommitStillKeepsCurrentSameIdPayload() {
        val selector = DefaultSelectorItem()
        val current = selector.value
        val edited = value("same", Choice("edited payload"))
        assertEquals(
            SettingsCommitResult.AcceptedLocally,
            SettingsCommit.tryCommitDraft(selector, edited),
        )
        assertSame(current, selector.value)
        assertEquals("current payload", selector.value.value.payload)
    }

    private class NumericItem : SettingsItem.Item.SingleNumber<Double> {
        var writes: Int = 0
        override val id: String = "number"
        override val children: List<SettingsItem>? = null
        override val shouldShow: Boolean = true
        override val enabled: Boolean = true
        override val resources: SettingsResources =
            object : SettingsResources {
                override val label: Resource<String> = Resource.Global.Static("Number")
            }
        override val type: NumberType<Double> =
            NumberType.finiteDouble(
                RangeBasedNumberInfo.Type.Slider,
                0.0,
                1.0,
                0.1,
                true,
                SliderMode.Steps,
            )
        override var value: Double = 0.0
            set(value) {
                field = value
                writes++
            }
    }

    private class DefaultSwitchItem(
        private val defaultType: SettingsResetMode = SettingsResetMode.Menu,
    ) : SettingsItem.Item.Switch {
        var writes: Int = 0
        var shown: Boolean = true
        override var enabled: Boolean = true
        override val id: String = "default-switch"
        override val children: List<SettingsItem>? = null
        override val shouldShow: Boolean
            get() = shown

        override val resources: SettingsResources = resource("Switch")
        override val resetMode: SettingsResetMode
            get() = defaultType

        override val defaultValue: Boolean = false
        override var value: Boolean = true
            set(value) {
                field = value
                writes++
            }
    }

    private class DefaultTextItem : SettingsItem.Item.Text {
        var writes: Int = 0
        override val id: String = "default-text"
        override val children: List<SettingsItem>? = null
        override val shouldShow: Boolean = true
        override val enabled: Boolean = true
        override val resources: SettingsResources = resource("Text")
        override val resetMode: SettingsResetMode = SettingsResetMode.Menu
        override val defaultValue: String = "x"
        override val minLength: Int = 3
        override var value: String = "valid"
            set(value) {
                field = value
                writes++
            }
    }

    private data class Choice(val payload: String)

    private class DefaultSelectorItem : SettingsItem.Item.Selector<Choice> {
        override val id: String = "selector"
        override val children: List<SettingsItem>? = null
        override val shouldShow: Boolean = true
        override val enabled: Boolean = true
        override val resources: SettingsResources = resource("Selector")
        private val option = value("same", Choice("option payload"))
        override val options: List<SettingsValue<Choice>> = listOf(option)
        override val resetMode: SettingsResetMode = SettingsResetMode.Menu
        override val defaultValue: SettingsValue<Choice> = value("same", Choice("default payload"))
        override var value: SettingsValue<Choice> = value("same", Choice("current payload"))
    }

    private class DefaultMultiSelectorItem : SettingsItem.Item.MultiSelector<Choice> {
        override val id: String = "multi"
        override val children: List<SettingsItem>? = null
        override val shouldShow: Boolean = true
        override val enabled: Boolean = true
        override val resources: SettingsResources = resource("Multi")
        override val options: List<SettingsValue<Choice>> =
            listOf(value("same", Choice("option payload")))
        override val resetMode: SettingsResetMode = SettingsResetMode.Menu
        override val defaultValue: Set<SettingsValue<Choice>> =
            setOf(value("same", Choice("default multi payload")))
        override var value: Set<SettingsValue<Choice>> =
            setOf(value("same", Choice("current multi payload")))
    }

    companion object {
        private fun resource(label: String): SettingsResources =
            object : SettingsResources {
                override val label: Resource<String> = Resource.Global.Static(label)
            }

        private fun value(id: String, choice: Choice): SettingsValue<Choice> =
            object : SettingsValue<Choice> {
                override val id: String = id
                override val value: Choice = choice
                override val resources: SettingsResources = resource(id)
            }
    }
}
