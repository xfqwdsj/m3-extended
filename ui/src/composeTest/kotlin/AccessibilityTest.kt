package top.ltfan.m3x.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.compose.ui.accessibility.AccessibilityPreference
import top.ltfan.m3x.compose.ui.accessibility.AccessibilityPreferences
import top.ltfan.m3x.compose.ui.accessibility.AccessibilitySettings
import top.ltfan.m3x.compose.ui.accessibility.LocalAccessibilityPreferences
import top.ltfan.m3x.compose.ui.accessibility.LocalAccessibilitySettings
import top.ltfan.m3x.compose.ui.accessibility.LocalSystemAccessibilitySettingsHook
import top.ltfan.m3x.compose.ui.accessibility.ProvideAccessibility
import top.ltfan.m3x.compose.ui.accessibility.SystemAccessibilitySettings

class AccessibilityTest {
    @Test
    fun nestedProvidersReuseOneLiveSystemReaderAndOverrideOnlyTheirSubtree() =
        runInitializedComposeUiTest {
            var system by
                mutableStateOf(
                    SystemAccessibilitySettings(
                        reduceTransparency = true,
                        increaseContrast = false,
                        reduceMotion = true,
                    ),
                )
            var readerCalls = 0
            var rootSettings = AccessibilitySettings()
            var inheritedSettings = AccessibilitySettings()
            var overrideSettings = AccessibilitySettings()
            var disabledSettings =
                AccessibilitySettings(
                    reduceTransparency = true,
                    increaseContrast = true,
                    reduceMotion = true,
                )
            var disabledReaderCount = 0
            var rootPreferences = AccessibilityPreferences()
            setContent {
                CompositionLocalProvider(
                    LocalSystemAccessibilitySettingsHook provides
                        {
                            readerCalls++
                            system
                        },
                ) {
                    ProvideAccessibility {
                        NonSkippableSettingsReader { rootSettings = it }
                        NonSkippablePreferencesReader { rootPreferences = it }
                        ProvideAccessibility {
                            NonSkippableSettingsReader { inheritedSettings = it }
                        }
                        ProvideAccessibility(
                            AccessibilityPreferences(
                                AccessibilityPreference.Disabled,
                                AccessibilityPreference.Enabled,
                                AccessibilityPreference.System,
                            ),
                        ) {
                            NonSkippableSettingsReader { overrideSettings = it }
                        }
                        ProvideAccessibility(
                            AccessibilityPreferences(
                                AccessibilityPreference.Disabled,
                                AccessibilityPreference.Disabled,
                                AccessibilityPreference.Disabled,
                            ),
                        ) {
                            NonSkippableSettingsReader { settings ->
                                disabledReaderCount++
                                disabledSettings = settings
                            }
                        }
                    }
                }
            }
            waitForIdle()
            assertEquals(1, readerCalls)
            assertEquals(
                AccessibilitySettings(
                    reduceTransparency = true,
                    increaseContrast = false,
                    reduceMotion = true,
                ),
                rootSettings,
            )
            assertEquals(rootSettings, inheritedSettings)
            assertEquals(AccessibilityPreferences(), rootPreferences)
            assertEquals(
                AccessibilitySettings(
                    reduceTransparency = false,
                    increaseContrast = true,
                    reduceMotion = true,
                ),
                overrideSettings,
            )
            assertEquals(
                AccessibilitySettings(
                    reduceTransparency = false,
                    increaseContrast = false,
                    reduceMotion = false,
                ),
                disabledSettings,
            )
            val initialDisabledReaderCount = disabledReaderCount

            runOnIdle {
                system =
                    SystemAccessibilitySettings(
                        reduceTransparency = false,
                        increaseContrast = true,
                        reduceMotion = true,
                    )
            }
            waitForIdle()
            assertTrue(readerCalls >= 2)
            assertEquals(
                AccessibilitySettings(
                    reduceTransparency = false,
                    increaseContrast = true,
                    reduceMotion = true,
                ),
                rootSettings,
            )
            assertEquals(rootSettings, inheritedSettings)
            assertEquals(
                AccessibilitySettings(
                    reduceTransparency = false,
                    increaseContrast = true,
                    reduceMotion = true,
                ),
                overrideSettings,
            )
            assertEquals(initialDisabledReaderCount, disabledReaderCount)
            assertEquals(
                AccessibilitySettings(
                    reduceTransparency = false,
                    increaseContrast = false,
                    reduceMotion = false,
                ),
                disabledSettings,
            )

            runOnIdle {
                system =
                    SystemAccessibilitySettings(
                        reduceTransparency = true,
                        increaseContrast = true,
                        reduceMotion = false,
                    )
            }
            waitForIdle()
            assertEquals(
                AccessibilitySettings(
                    reduceTransparency = true,
                    increaseContrast = true,
                    reduceMotion = false,
                ),
                rootSettings,
            )
            assertEquals(
                AccessibilitySettings(
                    reduceTransparency = false,
                    increaseContrast = true,
                    reduceMotion = false,
                ),
                overrideSettings,
            )
            assertEquals(initialDisabledReaderCount, disabledReaderCount)
            assertEquals(
                AccessibilitySettings(
                    reduceTransparency = false,
                    increaseContrast = false,
                    reduceMotion = false,
                ),
                disabledSettings,
            )
        }

    @Test
    fun unchangedDisabledResolutionDoesNotRecomposeItsReader() = runInitializedComposeUiTest {
        var system by
            mutableStateOf(
                SystemAccessibilitySettings(
                    reduceTransparency = false,
                    increaseContrast = false,
                    reduceMotion = false,
                ),
            )
        var readCount = 0
        var observed =
            AccessibilitySettings(
                reduceTransparency = true,
                increaseContrast = true,
                reduceMotion = true,
            )
        val disabled =
            AccessibilityPreferences(
                AccessibilityPreference.Disabled,
                AccessibilityPreference.Disabled,
                AccessibilityPreference.Disabled,
            )
        setContent {
            CompositionLocalProvider(LocalSystemAccessibilitySettingsHook provides { system }) {
                ProvideAccessibility(disabled) {
                    NonSkippableSettingsReader {
                        readCount++
                        observed = it
                    }
                }
            }
        }
        waitForIdle()
        assertEquals(
            AccessibilitySettings(
                reduceTransparency = false,
                increaseContrast = false,
                reduceMotion = false,
            ),
            observed,
        )
        val initialReadCount = readCount

        runOnIdle {
            system =
                SystemAccessibilitySettings(
                    reduceTransparency = true,
                    increaseContrast = true,
                    reduceMotion = true,
                )
        }
        waitForIdle()
        assertEquals(initialReadCount, readCount)
        assertEquals(
            AccessibilitySettings(
                reduceTransparency = false,
                increaseContrast = false,
                reduceMotion = false,
            ),
            observed,
        )
    }

    @Composable
    @NonSkippableComposable
    private fun NonSkippableSettingsReader(onRead: (AccessibilitySettings) -> Unit) {
        val settings = LocalAccessibilitySettings.current
        SideEffect { onRead(settings) }
    }

    @Composable
    @NonSkippableComposable
    private fun NonSkippablePreferencesReader(onRead: (AccessibilityPreferences) -> Unit) {
        val preferences = LocalAccessibilityPreferences.current
        SideEffect { onRead(preferences) }
    }
}
