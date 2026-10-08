package top.ltfan.m3x.material.backdrop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.compose.ui.accessibility.AccessibilityPreference
import top.ltfan.m3x.compose.ui.accessibility.AccessibilityPreferences
import top.ltfan.m3x.compose.ui.accessibility.LocalSystemAccessibilitySettingsHook
import top.ltfan.m3x.compose.ui.accessibility.SystemAccessibilitySettings
import top.ltfan.m3x.compose.ui.color.contrastRatioAgainst
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropTheme
import top.ltfan.m3x.material.bundle.M3xMaterialTheme
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering

class AccessibilityAppearanceTest {
    @Test
    fun runtimeSystemChangesReachBackdropAppearanceAndEachPreferenceCanBeOverridden() =
        runInitializedComposeUiTest {
            var system by
                mutableStateOf(
                    SystemAccessibilitySettings(
                        reduceTransparency = true,
                        increaseContrast = true,
                        reduceMotion = true,
                    ),
                )
            var appearance: MaterialBackdropAppearance? = null
            setContent {
                ProvideM3xMaterialRendering {
                    CompositionLocalProvider(
                        LocalSystemAccessibilitySettingsHook provides { system },
                    ) {
                        M3xMaterialTheme(backdrop = M3xBackdropTheme()) {
                            appearance =
                                rememberMaterialAppearance(
                                    MaterialSurfaceRole.Control,
                                    containerColor = Color.Blue,
                                    contentColor = Color.White,
                                )
                        }
                    }
                }
            }
            waitForIdle()
            assertNotNull(appearance)
            assertEquals(1f, appearance.tint.alpha)
            assertTrue(appearance.reduceMotion)

            runOnIdle {
                system =
                    SystemAccessibilitySettings(
                        reduceTransparency = false,
                        increaseContrast = true,
                        reduceMotion = false,
                    )
            }
            waitForIdle()
            assertEquals(1f, appearance.tint.alpha)
            assertTrue(appearance.contentColor.contrastRatioAgainst(appearance.tint) >= 6.999f)
            assertFalse(appearance.reduceMotion)

            runOnIdle {
                system =
                    SystemAccessibilitySettings(
                        reduceTransparency = false,
                        increaseContrast = false,
                        reduceMotion = false,
                    )
            }
            waitForIdle()
            assertEquals(
                M3xBackdropTheme().control.tintAlpha,
                appearance.tint.alpha,
                0.005f,
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
            assertEquals(1f, appearance.tint.alpha)
        }

    @Test
    fun explicitDisabledPreferenceReachesAppearanceDespiteEnabledSystemSetting() =
        runInitializedComposeUiTest {
            var appearance: MaterialBackdropAppearance? = null
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
                        M3xMaterialTheme(
                            accessibility =
                                AccessibilityPreferences(
                                    reduceTransparency = AccessibilityPreference.Disabled,
                                    increaseContrast = AccessibilityPreference.Disabled,
                                    reduceMotion = AccessibilityPreference.Disabled,
                                ),
                        ) {
                            appearance = rememberMaterialAppearance(MaterialSurfaceRole.Control)
                        }
                    }
                }
            }
            waitForIdle()
            assertEquals(
                M3xBackdropTheme().control.tintAlpha,
                appearance?.tint?.alpha ?: 0f,
                0.005f,
            )
            assertFalse(appearance?.reduceMotion ?: true)
        }
}
