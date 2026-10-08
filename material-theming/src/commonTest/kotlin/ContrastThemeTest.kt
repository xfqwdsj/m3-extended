package top.ltfan.m3x.material.theming

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.material3.withIncreasedContrast
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.compose.ui.accessibility.AccessibilityPreference
import top.ltfan.m3x.compose.ui.accessibility.AccessibilityPreferences
import top.ltfan.m3x.compose.ui.accessibility.LocalSystemAccessibilitySettingsHook
import top.ltfan.m3x.compose.ui.accessibility.SystemAccessibilitySettings
import top.ltfan.m3x.compose.ui.color.contrastRatioAgainst
import top.ltfan.m3x.material.bundle.M3xMaterialTheme
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering

class ContrastThemeTest {
    @Test
    fun nestedThemesRetainBaseColorsAcrossRuntimeContrastAndExplicitOverrides() =
        runInitializedComposeUiTest {
            val contrast = mutableStateOf(false)
            val base = lightColorScheme()
            val explicit = lightColorScheme(primary = Color.Magenta)
            val nativeOverride = lightColorScheme(primary = Color.Green)
            var nativePrimary = Color.Unspecified
            var rootPrimary = Color.Unspecified
            var nestedPrimary = Color.Unspecified
            var disabledPrimary = Color.Unspecified
            var suppliedPrimary = Color.Unspecified
            var rootForeground = Color.Unspecified
            var rootSurface = Color.Unspecified
            setContent {
                ProvideM3xMaterialRendering {
                    CompositionLocalProvider(
                        LocalSystemAccessibilitySettingsHook provides
                            {
                                SystemAccessibilitySettings(increaseContrast = contrast.value)
                            },
                    ) {
                        M3xMaterialTheme(colorScheme = base) {
                            rootPrimary = MaterialTheme.colorScheme.primary
                            rootForeground = MaterialTheme.colorScheme.onSurface
                            rootSurface = MaterialTheme.colorScheme.surface
                            M3xMaterialTheme { nestedPrimary = MaterialTheme.colorScheme.primary }
                            MaterialTheme(colorScheme = nativeOverride) {
                                M3xMaterialTheme {
                                    nativePrimary = MaterialTheme.colorScheme.primary
                                }
                            }
                            M3xMaterialTheme(
                                accessibility =
                                    AccessibilityPreferences(
                                        increaseContrast = AccessibilityPreference.Disabled,
                                    ),
                            ) {
                                disabledPrimary = MaterialTheme.colorScheme.primary
                            }
                            M3xMaterialTheme(increasedContrastColorScheme = explicit) {
                                suppliedPrimary = MaterialTheme.colorScheme.primary
                            }
                        }
                    }
                }
            }
            waitForIdle()
            assertEquals(base.primary, rootPrimary)
            runOnIdle { contrast.value = true }
            waitForIdle()
            assertEquals(rootPrimary, nestedPrimary)
            assertEquals(base.primary, disabledPrimary)
            assertEquals(explicit.primary, suppliedPrimary)
            assertEquals(nativeOverride.withIncreasedContrast().primary, nativePrimary)
            assertTrue(rootForeground.contrastRatioAgainst(rootSurface) >= 7f)
            runOnIdle { contrast.value = false }
            waitForIdle()
            assertEquals(base.primary, rootPrimary)
            assertEquals(base.primary, nestedPrimary)
            assertEquals(base.primary, suppliedPrimary)
            assertEquals(nativeOverride.primary, nativePrimary)
        }
}
