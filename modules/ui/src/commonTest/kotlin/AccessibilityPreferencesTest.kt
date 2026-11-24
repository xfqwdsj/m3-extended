package top.ltfan.m3x.compose.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import top.ltfan.m3x.compose.ui.accessibility.AccessibilityPreference
import top.ltfan.m3x.compose.ui.accessibility.AccessibilityPreferences
import top.ltfan.m3x.compose.ui.accessibility.AccessibilitySettings
import top.ltfan.m3x.compose.ui.accessibility.SystemAccessibilitySettings
import top.ltfan.m3x.compose.ui.accessibility.resolveAccessibility

class AccessibilityPreferencesTest {
    @Test
    fun preferencesResolveEverySystemValue() {
        for (preference in AccessibilityPreference.entries) {
            for (systemValue in listOf(null, false, true)) {
                val preferences = AccessibilityPreferences(preference, preference, preference)
                val expected =
                    preference == AccessibilityPreference.Enabled ||
                        preference == AccessibilityPreference.System && systemValue == true
                assertEquals(
                    AccessibilitySettings(expected, expected, expected),
                    preferences.resolveAccessibility(
                        SystemAccessibilitySettings(systemValue, systemValue, systemValue),
                    ),
                )
            }
        }
    }
}
