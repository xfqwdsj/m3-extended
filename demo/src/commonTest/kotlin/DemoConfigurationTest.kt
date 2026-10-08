package top.ltfan.m3x.demo

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import top.ltfan.backdrop.highlight.Highlight
import top.ltfan.backdrop.highlight.HighlightColorResponse
import top.ltfan.backdrop.highlight.HighlightDirection
import top.ltfan.backdrop.highlight.HighlightStyle
import top.ltfan.m3x.compose.ui.accessibility.AccessibilityPreference
import top.ltfan.m3x.compose.ui.accessibility.AccessibilityPreferences
import top.ltfan.m3x.datastore.compose.storeHost
import top.ltfan.m3x.material.backdrop.BackdropPresets
import top.ltfan.m3x.material.backdrop.theme.BackdropAdaptation
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial
import top.ltfan.m3x.settingspage.SettingsCommit
import top.ltfan.m3x.settingspage.SettingsCommitResult
import top.ltfan.m3x.settingspage.SettingsItem

@OptIn(ExperimentalCoroutinesApi::class)
class DemoConfigurationTest {
    @Test
    fun accessibilityChoicesDefaultToSystemAndMapAndSerializeAsThreeStates() {
        assertEquals(DemoPreferences.Accessibility(), DemoPreferences().accessibility)
        val prefs =
            DemoPreferences(
                accessibility =
                    DemoPreferences.Accessibility(
                        reduceTransparency = AccessibilityMode.System,
                        increaseContrast = AccessibilityMode.Enabled,
                        reduceMotion = AccessibilityMode.Disabled,
                    ),
            )
        assertEquals(
            AccessibilityPreferences(
                AccessibilityPreference.System,
                AccessibilityPreference.Enabled,
                AccessibilityPreference.Disabled,
            ),
            prefs.themeAccessibility(),
        )
        assertSame(BackdropPresets.GlassRegular, prefs.themeBackdrop())
        for (mode in AccessibilityMode.entries) {
            assertEquals(mode, Json.decodeFromString(Json.encodeToString(mode)))
        }
    }

    @Test
    fun persistedBackgroundChoicesDecodeIntoTheTwoCurrentOptions() {
        for (value in listOf("Light", "Dark", "Text", "Theme")) {
            assertEquals(
                PreviewBackground.Theme,
                Json.decodeFromString<PreviewBackground>("\"$value\""),
            )
        }
        assertEquals(
            PreviewBackground.Artwork,
            Json.decodeFromString<PreviewBackground>("\"Artwork\""),
        )
        assertEquals(2, PreviewBackground.entries.size)
    }

    @Test
    fun hdrHighlightWhiteIsSharedByGlobalAndLabSurfacesWithoutChangingInteractionLight() {
        val preferences = DemoPreferences()
        val sdr = preferences.themeBackdrop()
        val hdr = preferences.themeBackdrop(interactionHdr = true, hdrHighlightAvailable = true)
        val expectedColor =
            demoHdrWhite(DemoBackdropDefaults.HDR_HIGHLIGHT_WHITE_INTENSITY)
                .copy(alpha = DemoBackdropDefaults.highlight.style.color.alpha)

        fun highlightColor(material: M3xBackdropMaterial) =
            (material.style.highlight() as Highlight.Config).style.color

        assertSame(BackdropPresets.GlassRegular, sdr)
        assertEquals(DemoBackdropDefaults.highlight.style.color, highlightColor(sdr.control))
        assertEquals(expectedColor, DemoBackdropDefaults.hdrHighlightWhite)
        assertEquals(expectedColor, highlightColor(hdr.control))
        assertEquals(expectedColor, highlightColor(hdr.panel))
        assertEquals(expectedColor, highlightColor(hdr.inversePanel))

        val lab =
            LabParameters(edge = LabParameters.Edge(hdrHighlight = true))
                .material(hdrAvailable = true)
        assertEquals(expectedColor, highlightColor(lab))
        assertEquals(expectedColor, highlightColor(hdr.control))

        assertEquals(2f, DemoBackdropDefaults.HDR_HIGHLIGHT_WHITE_INTENSITY)
        assertEquals(4f, DemoBackdropDefaults.HDR_WHITE_INTENSITY)
        assertEquals(demoHdrWhite(DemoBackdropDefaults.HDR_WHITE_INTENSITY), hdr.lighting.color)
        assertEquals(BackdropPresets.GlassRegular.lighting.color, sdr.lighting.color)
    }

    @Test
    fun defaultAppearancePreservesPresetAndAllEffectSwitches() {
        val prefs = DemoPreferences()
        assertSame(BackdropPresets.GlassRegular, prefs.themeBackdrop())
        assertTrue(prefs.effects.blur && prefs.effects.lens && prefs.effects.vibrancy)
        assertTrue(prefs.effects.highlight && prefs.effects.shadow && prefs.effects.innerShadow)
        assertFalse(prefs.effects.adaptation)
        assertTrue(prefs.effects.interaction)
        for ((preset, theme) in
            listOf(
                MaterialPreset.Blur to BackdropPresets.MaterialBlur,
                MaterialPreset.Clear to BackdropPresets.GlassClear,
            )) {
            assertSame(
                theme,
                prefs.copy(appearance = prefs.appearance.copy(preset = preset)).themeBackdrop(),
            )
        }
    }

    @Test
    fun labAdaptationIsFixedByDefaultAndCanBeEnabled() {
        val lab = LabParameters()
        assertEquals(BackdropAdaptation.Fixed, lab.material().adaptation)
        assertEquals(
            BackdropAdaptation.Background,
            lab.copy(optics = lab.optics.copy(adaptive = true)).material().adaptation,
        )
    }

    @Test
    fun explicitAdaptationUsesBackgroundSampling() {
        val prefs = DemoPreferences()
        val adapted = prefs.copy(effects = prefs.effects.copy(adaptation = true)).themeBackdrop()
        assertEquals(BackdropAdaptation.Background, adapted.control.adaptation)
        assertEquals(BackdropAdaptation.Background, adapted.panel.adaptation)
        assertEquals(BackdropAdaptation.Background, adapted.surface.adaptation)
        assertEquals(BackdropAdaptation.Fixed, adapted.inversePanel.adaptation)
        assertFalse(adapted.control.adaptiveContentColor)
        val adaptiveForeground =
            prefs
                .copy(effects = prefs.effects.copy(adaptation = true, adaptiveContentColor = true))
                .themeBackdrop()
        assertTrue(adaptiveForeground.control.adaptiveContentColor)
        assertTrue(adaptiveForeground.panel.adaptiveContentColor)
        assertTrue(adaptiveForeground.surface.adaptiveContentColor)
        assertFalse(adaptiveForeground.inversePanel.adaptiveContentColor)
        val gated =
            prefs.copy(effects = prefs.effects.copy(adaptiveContentColor = true)).themeBackdrop()
        assertFalse(gated.control.adaptiveContentColor)
    }

    @Test
    fun customBlurRadiiAreOptInAndOverrideThemeRoles() {
        val prefs = DemoPreferences()
        for ((preset, theme) in
            listOf(
                MaterialPreset.Regular to BackdropPresets.GlassRegular,
                MaterialPreset.Clear to BackdropPresets.GlassClear,
                MaterialPreset.Blur to BackdropPresets.MaterialBlur,
            )) {
            val selected = prefs.copy(appearance = prefs.appearance.copy(preset = preset))
            assertSame(theme, selected.themeBackdrop())
            val changedOtherSetting =
                selected.copy(effects = selected.effects.copy(interaction = false)).themeBackdrop()
            assertEquals(theme.control.blurRadius, changedOtherSetting.control.blurRadius)
            assertEquals(theme.panel.blurRadius, changedOtherSetting.panel.blurRadius)
            assertEquals(theme.surface.blurRadius, changedOtherSetting.surface.blurRadius)
            assertEquals(theme.scrollEdge.blurRadius, changedOtherSetting.scrollEdge.blurRadius)
        }
        val custom =
            prefs
                .copy(
                    effects =
                        prefs.effects.copy(
                            customBlurRadii = true,
                            controlBlurRadiusDp = 7f,
                            panelBlurRadiusDp = 21f,
                            surfaceBlurRadiusDp = 3f,
                            scrollEdgeBlurRadiusDp = 11f,
                        ),
                )
                .themeBackdrop()
        assertEquals(7.dp, custom.control.blurRadius)
        assertEquals(21.dp, custom.panel.blurRadius)
        assertEquals(3.dp, custom.surface.blurRadius)
        assertEquals(21.dp, custom.inversePanel.blurRadius)
        assertEquals(11.dp, custom.scrollEdge.blurRadius)
    }

    @Test
    fun generatedVisualSettingsPersistAndLabRemainsIndependent() = runTest {
        val disk = mutableMapOf<String, String>()
        val file = DemoStoreFile(disk::get) { key, value -> disk[key] = value }
        val host = storeHost(backgroundScope, file)
        val appearance = host.mutable(DemoPreferencesStore)
        val lab = host.mutable(LabParametersStore)
        appearance.awaitInitialized()
        lab.awaitInitialized()
        val category =
            appearance.settingsDescriber().items.first { it.id == "effects" }
                as SettingsItem.Category
        val adaptation =
            category.children.first { it.id == "effects.adaptation" } as SettingsItem.Item.Switch
        val adaptiveContentColor =
            category.children.first { it.id == "effects.adaptiveContentColor" }
                as SettingsItem.Item.Switch
        val customBlurRadii =
            category.children.first { it.id == "effects.customBlurRadii" }
                as SettingsItem.Item.Switch
        val controlBlurRadius =
            category.children.first { it.id == "effects.controlBlurRadiusDp" }
                as SettingsItem.Item.SingleNumber<*>
        val panelBlurRadius =
            category.children.first { it.id == "effects.panelBlurRadiusDp" }
                as SettingsItem.Item.SingleNumber<*>
        val surfaceBlurRadius =
            category.children.first { it.id == "effects.surfaceBlurRadiusDp" }
                as SettingsItem.Item.SingleNumber<*>
        val scrollEdgeBlurRadius =
            category.children.first { it.id == "effects.scrollEdgeBlurRadiusDp" }
                as SettingsItem.Item.SingleNumber<*>
        assertFalse(adaptiveContentColor.enabled)
        assertFalse(controlBlurRadius.enabled)
        assertFalse(panelBlurRadius.enabled)
        assertFalse(surfaceBlurRadius.enabled)
        assertFalse(scrollEdgeBlurRadius.enabled)
        SettingsCommit.tryCommit(adaptation, true)
        runCurrent()
        assertTrue(adaptiveContentColor.enabled)
        SettingsCommit.tryCommit(adaptiveContentColor, true)
        SettingsCommit.tryCommit(customBlurRadii, true)
        runCurrent()
        assertTrue(controlBlurRadius.enabled)
        assertTrue(panelBlurRadius.enabled)
        assertTrue(surfaceBlurRadius.enabled)
        assertTrue(scrollEdgeBlurRadius.enabled)
        SettingsCommit.tryCommitDraft(controlBlurRadius, 7f)
        SettingsCommit.tryCommitDraft(panelBlurRadius, 21f)
        SettingsCommit.tryCommitDraft(surfaceBlurRadius, 3f)
        SettingsCommit.tryCommitDraft(scrollEdgeBlurRadius, 11f)
        runCurrent()
        val committedTheme = appearance.value.themeBackdrop()
        assertEquals(BackdropAdaptation.Background, committedTheme.control.adaptation)
        assertTrue(committedTheme.control.adaptiveContentColor)
        assertEquals(7.dp, committedTheme.control.blurRadius)
        assertEquals(21.dp, committedTheme.panel.blurRadius)
        assertEquals(3.dp, committedTheme.surface.blurRadius)
        assertEquals(21.dp, committedTheme.inversePanel.blurRadius)
        assertEquals(11.dp, committedTheme.scrollEdge.blurRadius)
        val interaction =
            category.children.first { it.id == "effects.interaction" } as SettingsItem.Item.Switch
        SettingsCommit.tryCommit(interaction, false)
        runCurrent()
        assertFalse(appearance.value.themeBackdrop().lighting.enabled)
        assertEquals(LabParameters(), lab.value)
        assertEquals(90f, lab.value.edge.highlightAngle)
        assertEquals(
            DemoBackdropDefaults.highlight.colorResponse,
            (appearance.value.themeBackdrop().control.style.highlight() as Highlight.Config)
                .colorResponse,
        )
        assertEquals(
            DemoBackdropDefaults.highlight.colorResponse,
            (lab.value.material().style.highlight() as Highlight.Config).colorResponse,
        )
        assertTrue(appearance.value.effects.followBackdropColor)
        assertTrue(lab.value.edge.followBackdropColor)
        assertEquals(
            DemoBackdropDefaults.direction.strength.from,
            lab.value.edge.highlightStrengthFrom,
        )
        assertEquals(
            DemoBackdropDefaults.direction.strength.to,
            lab.value.edge.highlightStrengthTo,
        )
        assertEquals(
            DemoBackdropDefaults.direction.widthScale.from,
            lab.value.edge.highlightWidthFrom,
        )
        assertEquals(
            DemoBackdropDefaults.direction.widthScale.to,
            lab.value.edge.highlightWidthTo,
        )
        assertFalse(lab.value.edge.customHighlightStrengthCurve)
        assertFalse(lab.value.edge.customHighlightWidthCurve)
        val defaultDirection =
            lab.value.material().style.highlight().let { (it as Highlight.Config).direction }
        assertEquals(
            DemoBackdropDefaults.direction.strength.curve,
            (defaultDirection as HighlightDirection.Config).strength.curve,
        )
        assertEquals(
            DemoBackdropDefaults.direction.widthScale.curve,
            defaultDirection.widthScale.curve,
        )
        val edge = lab.settingsDescriber().items.first { it.id == "edge" } as SettingsItem.Category
        val followBackdropColor =
            edge.children.first { it.id == "edge.followBackdropColor" } as SettingsItem.Item.Switch
        SettingsCommit.tryCommit(followBackdropColor, true)
        runCurrent()
        lab.write(
            lab.value.copy(
                optics = lab.value.optics.copy(blurRadius = 32f),
                edge =
                    lab.value.edge.copy(
                        highlight = true,
                        followBackdropColor = true,
                        backdropColorAmount = 0.75f,
                        highlightStrengthFrom = 0.2f,
                        highlightStrengthTo = 1.4f,
                        highlightWidthFrom = 0.45f,
                        highlightWidthTo = 1.25f,
                        customHighlightStrengthCurve = true,
                        customHighlightWidthCurve = true,
                        highlightFalloff = 1.5f,
                        highlightWidthFalloff = 2f,
                    ),
            ),
        )
        runCurrent()
        host.closeAndJoin()
        val reopened = storeHost(backgroundScope, file)
        val restored = reopened.mutable(DemoPreferencesStore)
        val restoredLab = reopened.mutable(LabParametersStore)
        restored.awaitInitialized()
        restoredLab.awaitInitialized()
        assertFalse(restored.value.effects.interaction)
        val restoredTheme = restored.value.themeBackdrop()
        assertEquals(BackdropAdaptation.Background, restoredTheme.control.adaptation)
        assertTrue(restoredTheme.control.adaptiveContentColor)
        assertEquals(7.dp, restoredTheme.control.blurRadius)
        assertEquals(21.dp, restoredTheme.panel.blurRadius)
        assertEquals(3.dp, restoredTheme.surface.blurRadius)
        assertEquals(21.dp, restoredTheme.inversePanel.blurRadius)
        assertEquals(11.dp, restoredTheme.scrollEdge.blurRadius)
        assertEquals(32f, restoredLab.value.optics.blurRadius)
        assertEquals(
            SettingsCommitResult.AcceptedLocally,
            SettingsCommit.tryRestoreDefault(
                (restored.settingsDescriber().items.first { it.id == "effects" }
                        as SettingsItem.Category)
                    .children
                    .first {
                        it.id == "effects.controlBlurRadiusDp"
                    } as SettingsItem.Item.SingleNumber<*>,
            ),
        )
        runCurrent()
        assertEquals(4.dp, restored.value.themeBackdrop().control.blurRadius)
        val restoredAdaptation =
            (restored.settingsDescriber().items.first { it.id == "effects" }
                    as SettingsItem.Category)
                .children
                .first {
                    it.id == "effects.adaptation"
                } as SettingsItem.Item.Switch
        assertEquals(
            SettingsCommitResult.AcceptedLocally,
            SettingsCommit.tryRestoreDefault(restoredAdaptation),
        )
        runCurrent()
        assertEquals(BackdropAdaptation.Fixed, restored.value.themeBackdrop().control.adaptation)
        val highlight = restoredLab.value.material().style.highlight() as Highlight.Config
        assertSame(HighlightStyle.Solid, highlight.style)
        assertEquals(HighlightColorResponse(amount = 0.75f), highlight.colorResponse)
        val direction = highlight.direction as HighlightDirection.Config
        assertEquals(0.2f, direction.strength.from)
        assertEquals(1.4f, direction.strength.to)
        assertEquals(0.45f, direction.widthScale.from)
        assertEquals(1.25f, direction.widthScale.to)
        assertEquals(highlightStrengthCurve(1.5f), direction.strength.curve)
        assertEquals(highlightStrengthCurve(2f), direction.widthScale.curve)
        reopened.closeAndJoin()
    }

    @Test
    fun generatedAccessibilitySelectorCommitsPersistsAndRestoresSystem() = runTest {
        val disk = mutableMapOf<String, String>()
        val file = DemoStoreFile(disk::get) { key, value -> disk[key] = value }
        val host = storeHost(backgroundScope, file)
        val store = host.mutable(DemoPreferencesStore)
        store.awaitInitialized()
        val category =
            store.settingsDescriber().items.first { it.id == "accessibility" }
                as SettingsItem.Category
        val reduceMotion =
            category.children.first { it.id == "accessibility.reduceMotion" }
                as SettingsItem.Item.Selector<*>
        assertEquals(AccessibilityMode.System, reduceMotion.value.value)
        assertEquals(AccessibilityMode.entries, reduceMotion.options.map { it.value })
        SettingsCommit.tryCommitDraft(
            reduceMotion,
            reduceMotion.options.single { it.value == AccessibilityMode.Enabled },
        )
        runCurrent()
        assertEquals(AccessibilityMode.Enabled, store.value.accessibility.reduceMotion)
        host.closeAndJoin()

        val reopened = storeHost(backgroundScope, file)
        val restored = reopened.mutable(DemoPreferencesStore)
        restored.awaitInitialized()
        assertEquals(AccessibilityMode.Enabled, restored.value.accessibility.reduceMotion)
        val restoredItem =
            (restored.settingsDescriber().items.first { it.id == "accessibility" }
                    as SettingsItem.Category)
                .children
                .first { it.id == "accessibility.reduceMotion" } as SettingsItem.Item.Selector<*>
        assertEquals(
            SettingsCommitResult.AcceptedLocally,
            SettingsCommit.tryRestoreDefault(restoredItem),
        )
        runCurrent()
        assertEquals(AccessibilityMode.System, restored.value.accessibility.reduceMotion)
        reopened.closeAndJoin()
    }
}
