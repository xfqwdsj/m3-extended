package top.ltfan.m3x.demo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import top.ltfan.m3x.datastore.FlowStore
import top.ltfan.m3x.datastore.MutableStore
import top.ltfan.m3x.datastore.ValueCellFactory
import top.ltfan.m3x.settingspage.NumberType
import top.ltfan.m3x.settingspage.SettingsCommit
import top.ltfan.m3x.settingspage.SettingsCommitResult
import top.ltfan.m3x.settingspage.SettingsItem
import top.ltfan.m3x.settingspage.SettingsItems

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureSettingsTest {
    @Test
    fun handwrittenTreeReflectsRuntimeOptionsAndBounds() {
        val tree = ManualSettingsTree()
        val mode = tree.items[0] as SettingsItem.Item.Selector<*>
        val scale = tree.items[1] as SettingsItem.Item.SingleNumber<*>
        assertEquals(3, mode.options.size)
        tree.availableModes = listOf("System", "Dark")
        tree.maximumScale = 4f
        assertEquals(2, mode.options.size)
        assertEquals(4f, (scale.type as NumberType.Finite<*>).max)
        SettingsItems.requireUniqueIds(tree.items)
        tree.availableModes = listOf("Dark", "Dark")
        assertFailsWith<IllegalArgumentException> {
            SettingsItems.requireUniqueIds(tree.items)
        }
    }

    @Test
    fun generatedModelEditsPersistsAndReloads() = runTest {
        val disk = MutableStateFlow(FeatureSettings())
        val backend =
            FlowStore(disk) { transform ->
                transform(disk.value).also { disk.value = it }
            }
        val first =
            MutableStore(backend, backgroundScope, FeatureSettings(), ValueCellFactory.Plain)
        first.awaitInitialized()
        val settings = first.settingsDescriber()
        val general = settings.items.first() as SettingsItem.Category
        val enabled =
            general.children.first { it.id == "general.enabled" } as SettingsItem.Item.Switch
        val name = general.children.first { it.id == "general.name" } as SettingsItem.Item.Text
        val mode =
            general.children.first { it.id == "general.mode" } as SettingsItem.Item.Selector<*>
        val features =
            general.children.first { it.id == "general.features" }
                as SettingsItem.Item.MultiSelector<*>
        assertEquals("Enter at least 2 characters", name.validate("x"))
        assertEquals(3, mode.options.size)
        assertEquals(mode.value, mode.value)
        assertEquals(2, features.options.size)
        @Suppress("UNCHECKED_CAST")
        val featureChoices = features as SettingsItem.Item.MultiSelector<Feature>
        featureChoices.value =
            setOf(featureChoices.options.first { it.value is Feature.Notifications })
        val channel =
            featureChoices.children!!.first { it.id == "general.features.Notifications.channel" }
                as SettingsItem.Item.Text
        channel.value = "Updates"
        val selectedBefore = featureChoices.value
        val sync = featureChoices.options.first { it.value is Feature.Sync }
        assertEquals(
            SettingsCommitResult.AcceptedLocally,
            SettingsCommit.tryCommitDraft(featureChoices, selectedBefore + sync),
        )
        assertEquals(
            "Updates",
            featureChoices.value
                .map { it.value }
                .filterIsInstance<Feature.Notifications>()
                .first()
                .channel,
        )
        enabled.value = false
        assertFalse(name.enabled)
        assertFalse(features.shouldShow)
        enabled.value = true
        name.value = "Changed profile"
        val display = settings.items.last() as SettingsItem.Category
        val scale =
            display.children.first { it.id == "display.scale" } as SettingsItem.Item.SingleNumber<*>
        @Suppress("UNCHECKED_CAST")
        assertNotNull((scale as SettingsItem.Item.SingleNumber<Float>).validate(3f))
        assertEquals(SettingsCommitResult.AcceptedLocally, SettingsCommit.tryCommit(scale, 1.05f))
        val accent =
            display.children.first { it.id == "display.accent" } as SettingsItem.Item.Custom<*>
        assertEquals("accent", accent.rendererId)
        runCurrent()
        assertTrue(disk.value.general.enabled)
        assertEquals("Changed profile", disk.value.general.name)
        assertEquals(
            "Updates",
            disk.value.general.features.filterIsInstance<Feature.Notifications>().first().channel,
        )

        val second =
            MutableStore(backend, backgroundScope, FeatureSettings(), ValueCellFactory.Plain)
        second.awaitInitialized()
        assertEquals("Changed profile", second.value.general.name)
        @Suppress("UNCHECKED_CAST")
        val restoredFeatures =
            (second.settingsDescriber().items.first() as SettingsItem.Category).children.first {
                it.id == "general.features"
            } as SettingsItem.Item.MultiSelector<Feature>
        val restoredChannel =
            restoredFeatures.children!!.first {
                it.id == "general.features.Notifications.channel"
            } as SettingsItem.Item.Text
        assertEquals(
            SettingsCommitResult.AcceptedLocally,
            SettingsCommit.tryRestoreDefault(restoredChannel),
        )
        runCurrent()
        assertEquals(
            "Primary",
            second.value.general.features
                .filterIsInstance<Feature.Notifications>()
                .single()
                .channel,
        )
        assertEquals(
            SettingsCommitResult.AcceptedLocally,
            SettingsCommit.tryRestoreDefault(restoredFeatures),
        )
        runCurrent()
        assertEquals(setOf(Feature.Sync), second.value.general.features)
    }
}
