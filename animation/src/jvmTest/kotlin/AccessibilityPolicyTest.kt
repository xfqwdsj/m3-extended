package top.ltfan.m3x.compose.animation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import top.ltfan.m3x.compose.ui.accessibility.AccessibilitySettings
import top.ltfan.m3x.compose.ui.accessibility.LocalAccessibilitySettings
import top.ltfan.m3x.compose.ui.accessibility.SpatialMotionEnabled
import top.ltfan.m3x.compose.ui.visual.BackdropBlurEnabled
import top.ltfan.m3x.compose.ui.visual.LocalBlurEnabled

class AccessibilityPolicyTest {
    @Test
    fun accessibilityConstrainsOrdinarySwitchesForEveryCombination() = runComposeUiTest {
        val settings = mutableStateOf(AccessibilitySettings())
        val master = mutableStateOf(true)
        val motion = mutableStateOf(true)
        var actual = emptyList<Boolean>()
        setContent {
            CompositionLocalProvider(
                LocalAccessibilitySettings provides settings.value,
                LocalBlurEnabled provides master.value,
                LocalBlurAnimationEnabled provides motion.value,
            ) {
                val values =
                    listOf(
                        BackdropBlurEnabled,
                        BlurAnimationEnabled,
                        SpatialMotionEnabled,
                        BackdropBlurAnimationEnabled,
                    )
                SideEffect { actual = values }
            }
        }
        for (transparency in listOf(false, true)) for (contrast in
            listOf(false, true)) for (reducedMotion in listOf(false, true)) for (blur in
            listOf(false, true)) for (animated in listOf(false, true)) {
            runOnIdle {
                settings.value = AccessibilitySettings(transparency, contrast, reducedMotion)
                master.value = blur
                motion.value = animated
            }
            waitForIdle()
            assertEquals(
                listOf(
                    blur && !transparency,
                    blur && animated && !reducedMotion,
                    !reducedMotion,
                    blur && animated && !reducedMotion && !transparency,
                ),
                actual,
            )
        }
    }
}
