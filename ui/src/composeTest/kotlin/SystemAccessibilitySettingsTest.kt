package top.ltfan.m3x.compose.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import kotlin.test.Test
import kotlin.test.assertEquals
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.compose.ui.accessibility.LocalSystemAccessibilitySettingsHook
import top.ltfan.m3x.compose.ui.accessibility.SystemAccessibilitySettings
import top.ltfan.m3x.compose.ui.accessibility.rememberSystemAccessibilitySettings

class SystemAccessibilitySettingsTest {
    @Test
    fun hookReportsUnknownAndRecomposesWhenItsSourceChanges() = runInitializedComposeUiTest {
        var value by mutableStateOf(SystemAccessibilitySettings())
        var useHook by mutableStateOf(true)
        var observed = SystemAccessibilitySettings(reduceMotion = false)
        setContent {
            CompositionLocalProvider(
                LocalInspectionMode provides !useHook,
                LocalSystemAccessibilitySettingsHook provides if (useHook) ({ value }) else null,
            ) {
                observed = rememberSystemAccessibilitySettings()
            }
        }
        waitForIdle()
        assertEquals(SystemAccessibilitySettings(), observed)
        runOnIdle {
            value =
                SystemAccessibilitySettings(
                    reduceTransparency = true,
                    increaseContrast = false,
                    reduceMotion = true,
                )
        }
        waitForIdle()
        assertEquals(
            SystemAccessibilitySettings(
                reduceTransparency = true,
                increaseContrast = false,
                reduceMotion = true,
            ),
            observed,
        )
        runOnIdle { useHook = false }
        waitForIdle()
        assertEquals(SystemAccessibilitySettings(), observed)
    }
}
