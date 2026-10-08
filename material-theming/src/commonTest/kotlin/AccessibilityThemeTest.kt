package top.ltfan.m3x.material.theming

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.compose.ui.accessibility.AccessibilityPreference
import top.ltfan.m3x.compose.ui.accessibility.AccessibilityPreferences
import top.ltfan.m3x.compose.ui.accessibility.AccessibilitySettings
import top.ltfan.m3x.compose.ui.accessibility.LocalAccessibilitySettings
import top.ltfan.m3x.compose.ui.accessibility.LocalSystemAccessibilitySettingsHook
import top.ltfan.m3x.compose.ui.accessibility.ProvideAccessibility
import top.ltfan.m3x.compose.ui.accessibility.SystemAccessibilitySettings
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.backdrop.BackdropPresets
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropTheme
import top.ltfan.m3x.material.backdrop.theme.ProvideBackdropMaterial
import top.ltfan.m3x.material.backdrop.theme.backdrop
import top.ltfan.m3x.material.bundle.M3xMaterialTheme
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.theme.M3xThemeExtensions
import top.ltfan.m3x.material.theme.extended

class AccessibilityThemeTest {
    @Test
    fun runtimePoliciesOnlyInvalidateStaticThemeWhenNativeColorsChange() =
        runInitializedComposeUiTest {
            var system by
                mutableStateOf(
                    SystemAccessibilitySettings(
                        reduceTransparency = false,
                        increaseContrast = false,
                        reduceMotion = false,
                    ),
                )
            var themeReaderCount = 0
            var unrelatedReaderCount = 0
            var settingsReaderCount = 0
            var observed = AccessibilitySettings()
            setContent {
                ProvideM3xMaterialRendering {
                    CompositionLocalProvider(
                        LocalSystemAccessibilitySettingsHook provides { system },
                    ) {
                        M3xMaterialTheme(
                            accessibility =
                                AccessibilityPreferences(
                                    reduceTransparency = AccessibilityPreference.System,
                                    increaseContrast = AccessibilityPreference.System,
                                    reduceMotion = AccessibilityPreference.System,
                                ),
                        ) {
                            NonSkippableThemeReader { themeReaderCount++ }
                            NonSkippableComposableProbe { unrelatedReaderCount++ }
                            NonSkippableSettingsReader { settings ->
                                settingsReaderCount++
                                observed = settings
                            }
                        }
                    }
                }
            }
            waitForIdle()
            val initialSettingsCount = settingsReaderCount
            assertEquals(
                AccessibilitySettings(
                    reduceTransparency = false,
                    increaseContrast = false,
                    reduceMotion = false,
                ),
                observed,
            )

            runOnIdle {
                system =
                    SystemAccessibilitySettings(
                        reduceTransparency = false,
                        increaseContrast = true,
                        reduceMotion = false,
                    )
            }
            waitForIdle()
            // Native color-scheme replacement invalidates its Material subtree.
            val initialThemeCount = themeReaderCount
            val initialUnrelatedCount = unrelatedReaderCount
            assertEquals(initialSettingsCount + 1, settingsReaderCount)
            assertEquals(
                AccessibilitySettings(
                    reduceTransparency = false,
                    increaseContrast = true,
                    reduceMotion = false,
                ),
                observed,
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
            assertEquals(initialThemeCount, themeReaderCount)
            assertEquals(initialUnrelatedCount, unrelatedReaderCount)
            assertEquals(initialSettingsCount + 2, settingsReaderCount)
            assertEquals(
                AccessibilitySettings(
                    reduceTransparency = true,
                    increaseContrast = true,
                    reduceMotion = false,
                ),
                observed,
            )

            runOnIdle {
                system =
                    SystemAccessibilitySettings(
                        reduceTransparency = true,
                        increaseContrast = true,
                        reduceMotion = true,
                    )
            }
            waitForIdle()
            assertEquals(initialThemeCount, themeReaderCount)
            assertEquals(initialUnrelatedCount, unrelatedReaderCount)
            assertEquals(initialSettingsCount + 3, settingsReaderCount)
            assertEquals(
                AccessibilitySettings(
                    reduceTransparency = true,
                    increaseContrast = true,
                    reduceMotion = true,
                ),
                observed,
            )
        }

    @Test
    fun nearestGenericProviderSuppliesNestedThemePreferences() = runInitializedComposeUiTest {
        val themePreferences =
            AccessibilityPreferences(
                AccessibilityPreference.Enabled,
                AccessibilityPreference.Enabled,
                AccessibilityPreference.Enabled,
            )
        val scopedPreferences =
            AccessibilityPreferences(
                AccessibilityPreference.Disabled,
                AccessibilityPreference.Disabled,
                AccessibilityPreference.Disabled,
            )
        var nested: M3xThemeExtensions? = null
        var nestedBackdrop: M3xBackdropTheme? = null
        var resolved: AccessibilitySettings? = null
        setContent {
            ProvideM3xMaterialRendering {
                CompositionLocalProvider(
                    LocalSystemAccessibilitySettingsHook provides
                        {
                            SystemAccessibilitySettings(
                                reduceTransparency = true,
                                increaseContrast = true,
                                reduceMotion = true,
                            )
                        },
                ) {
                    M3xMaterialTheme(accessibility = themePreferences) {
                        ProvideAccessibility(scopedPreferences) {
                            M3xMaterialTheme {
                                nested = MaterialTheme.extended
                                nestedBackdrop = MaterialTheme.extended.backdrop
                                resolved = LocalAccessibilitySettings.current
                            }
                        }
                    }
                }
            }
        }
        waitForIdle()
        assertEquals(scopedPreferences, nested?.accessibilityPreferences)
        assertEquals(
            AccessibilitySettings(
                reduceTransparency = false,
                increaseContrast = false,
                reduceMotion = false,
            ),
            resolved,
        )
    }

    @Test
    fun outerGenericProviderSuppliesThemeDefaults() = runInitializedComposeUiTest {
        val preferences =
            AccessibilityPreferences(
                AccessibilityPreference.Disabled,
                AccessibilityPreference.Enabled,
                AccessibilityPreference.Disabled,
            )
        var theme: M3xThemeExtensions? = null
        var resolved: AccessibilitySettings? = null
        setContent {
            ProvideM3xMaterialRendering {
                CompositionLocalProvider(
                    LocalSystemAccessibilitySettingsHook provides
                        {
                            SystemAccessibilitySettings(
                                reduceTransparency = true,
                                increaseContrast = false,
                                reduceMotion = true,
                            )
                        },
                ) {
                    ProvideAccessibility(preferences) {
                        M3xMaterialTheme {
                            theme = MaterialTheme.extended
                            resolved = LocalAccessibilitySettings.current
                        }
                    }
                }
            }
        }
        waitForIdle()
        assertEquals(preferences, theme?.accessibilityPreferences)
        assertEquals(
            AccessibilitySettings(
                reduceTransparency = false,
                increaseContrast = true,
                reduceMotion = false,
            ),
            resolved,
        )
    }

    @Test
    fun preferencesRemainIndependentWhenBackdropChangesAndCanBeOverriddenInSubtrees() =
        runInitializedComposeUiTest {
            var system by
                mutableStateOf(
                    SystemAccessibilitySettings(
                        reduceTransparency = true,
                        increaseContrast = false,
                        reduceMotion = true,
                    ),
                )
            var preset by mutableStateOf(BackdropPresets.GlassRegular)
            val rootPreferences =
                AccessibilityPreferences(
                    AccessibilityPreference.Disabled,
                    AccessibilityPreference.Enabled,
                    AccessibilityPreference.System,
                )
            val overridePreferences =
                AccessibilityPreferences(
                    AccessibilityPreference.System,
                    AccessibilityPreference.Disabled,
                    AccessibilityPreference.Enabled,
                )
            var root: M3xThemeExtensions? = null
            var rootBackdrop: M3xBackdropTheme? = null
            var nested: M3xThemeExtensions? = null
            var nestedBackdrop: M3xBackdropTheme? = null
            var nestedOverride: M3xThemeExtensions? = null
            var nestedOverrideBackdrop: M3xBackdropTheme? = null
            var roleOverride: M3xThemeExtensions? = null
            var roleOverrideBackdrop: M3xBackdropTheme? = null
            var roleOverrideSettings = AccessibilitySettings()
            var accessibilityOnly: M3xThemeExtensions? = null
            var runtimeSettings = AccessibilitySettings()
            setContent {
                ProvideM3xMaterialRendering {
                    CompositionLocalProvider(
                        LocalSystemAccessibilitySettingsHook provides { system },
                    ) {
                        M3xMaterialTheme(backdrop = preset, accessibility = rootPreferences) {
                            root = MaterialTheme.extended
                            rootBackdrop = MaterialTheme.extended.backdrop
                            runtimeSettings = LocalAccessibilitySettings.current
                            val changedBackdrop =
                                preset.copy(control = preset.control.copy(tint = Color.Red))
                            M3xMaterialTheme(backdrop = changedBackdrop) {
                                nested = MaterialTheme.extended
                                nestedBackdrop = MaterialTheme.extended.backdrop
                            }
                            M3xMaterialTheme(
                                backdrop = changedBackdrop,
                                accessibility = overridePreferences,
                            ) {
                                nestedOverride = MaterialTheme.extended
                                nestedOverrideBackdrop = MaterialTheme.extended.backdrop
                            }
                            ProvideBackdropMaterial(
                                MaterialSurfaceRole.Control,
                                changedBackdrop.control.copy(tint = Color.Blue),
                            ) {
                                roleOverride = MaterialTheme.extended
                                roleOverrideBackdrop = MaterialTheme.extended.backdrop
                                roleOverrideSettings = LocalAccessibilitySettings.current
                            }
                            M3xMaterialTheme(accessibility = overridePreferences) {
                                accessibilityOnly = MaterialTheme.extended
                            }
                        }
                    }
                }
            }
            waitForIdle()
            assertEquals(rootPreferences, root?.accessibilityPreferences)
            assertEquals(preset, rootBackdrop)
            assertEquals(rootPreferences, nested?.accessibilityPreferences)
            assertEquals(overridePreferences, nestedOverride?.accessibilityPreferences)
            assertEquals(overridePreferences, accessibilityOnly?.accessibilityPreferences)
            assertEquals(Color.Red, nestedBackdrop?.control?.tint)
            assertEquals(Color.Red, nestedOverrideBackdrop?.control?.tint)
            assertEquals(Color.Blue, roleOverrideBackdrop?.control?.tint)
            assertEquals(rootPreferences, roleOverride?.accessibilityPreferences)
            assertEquals(
                AccessibilitySettings(
                    reduceTransparency = false,
                    increaseContrast = true,
                    reduceMotion = true,
                ),
                roleOverrideSettings,
            )
            assertEquals(
                AccessibilitySettings(
                    reduceTransparency = false,
                    increaseContrast = true,
                    reduceMotion = true,
                ),
                runtimeSettings,
            )

            runOnIdle { preset = BackdropPresets.GlassClear }
            waitForIdle()
            assertEquals(BackdropPresets.GlassClear, rootBackdrop)
            assertEquals(rootPreferences, nested?.accessibilityPreferences)
            assertEquals(
                BackdropPresets.GlassClear.control.copy(tint = Color.Red),
                nestedBackdrop?.control,
            )

            runOnIdle {
                system =
                    SystemAccessibilitySettings(
                        reduceTransparency = false,
                        increaseContrast = true,
                        reduceMotion = false,
                    )
            }
            waitForIdle()
            assertEquals(
                AccessibilitySettings(
                    reduceTransparency = false,
                    increaseContrast = true,
                    reduceMotion = false,
                ),
                runtimeSettings,
            )
            assertEquals(
                AccessibilitySettings(
                    reduceTransparency = false,
                    increaseContrast = true,
                    reduceMotion = false,
                ),
                roleOverrideSettings,
            )
            assertEquals(rootPreferences, root?.accessibilityPreferences)
            assertEquals(rootPreferences, nested?.accessibilityPreferences)
        }

    @Composable
    @NonSkippableComposable
    private fun NonSkippableThemeReader(onRead: () -> Unit) {
        MaterialTheme.extended
        SideEffect(onRead)
    }

    @Composable
    @NonSkippableComposable
    private fun NonSkippableComposableProbe(onRead: () -> Unit) {
        SideEffect(onRead)
    }

    @Composable
    @NonSkippableComposable
    private fun NonSkippableSettingsReader(onRead: (AccessibilitySettings) -> Unit) {
        val settings = LocalAccessibilitySettings.current
        SideEffect { onRead(settings) }
    }
}
