package top.ltfan.m3x.material

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.BackdropLayerHandle
import top.ltfan.m3x.backdrop.LocalBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.animation.BlurAnimationEnabled
import top.ltfan.m3x.compose.animation.LocalBlurAnimationEnabled
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.compose.ui.accessibility.AccessibilitySettings
import top.ltfan.m3x.compose.ui.accessibility.IncreasedContrastEnabled
import top.ltfan.m3x.compose.ui.accessibility.LocalAccessibilitySettings
import top.ltfan.m3x.compose.ui.accessibility.SpatialMotionEnabled
import top.ltfan.m3x.compose.ui.visual.BackdropBlurEnabled
import top.ltfan.m3x.compose.ui.visual.LocalBlurEnabled
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.overlay.OverlayDialog
import top.ltfan.m3x.material.theme.LocalM3xThemeExtensions
import top.ltfan.m3x.material.theme.M3xThemeExtensions
import top.ltfan.m3x.material.theme.extended
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState

class OverlayCallerValuesTest {
    @Test
    fun callerThemeAndLivePoliciesReachHostedContentWhileHostKeepsBackdropOwnership() =
        runInitializedComposeUiTest {
            val settings = mutableStateOf(AccessibilitySettings(reduceMotion = true))
            val blur = mutableStateOf(false)
            val animation = mutableStateOf(true)
            val colors = lightColorScheme(primary = Color.Red)
            val motion = MotionScheme.expressive()
            var observedPolicy = emptyList<Boolean>()
            var observedColor = Color.Unspecified
            var observedMotion: MotionScheme? = null
            var observedRadius = 0.dp
            var callerBackdrop: BackdropLayerHandle? = null
            var hostedBackdrop: BackdropLayerHandle? = null
            setContent {
                ProvideM3xMaterialRendering {
                    MaterialTheme(
                        colorScheme = lightColorScheme(primary = Color.Blue),
                        motionScheme = MotionScheme.standard(),
                    ) {
                        val page = rememberBackdropLayer()
                        BackdropHost(page) {
                            OverlayHost(remember { OverlayHostState() }) {
                                val caller = rememberBackdropLayer()
                                callerBackdrop = caller
                                CompositionLocalProvider(
                                    LocalBackdrop provides caller,
                                    LocalAccessibilitySettings provides settings.value,
                                    LocalBlurEnabled provides blur.value,
                                    LocalBlurAnimationEnabled provides animation.value,
                                    LocalM3xThemeExtensions provides
                                        M3xThemeExtensions(enterExitBlurRadius = 27.dp),
                                ) {
                                    MaterialTheme(colorScheme = colors, motionScheme = motion) {
                                        OverlayDialog(onDismissRequest = {}) {
                                            val policy =
                                                listOf(
                                                    SpatialMotionEnabled,
                                                    BlurAnimationEnabled,
                                                    BackdropBlurEnabled,
                                                    IncreasedContrastEnabled,
                                                )
                                            val primary = MaterialTheme.colorScheme.primary
                                            val scheme = MaterialTheme.motionScheme
                                            val radius = MaterialTheme.extended.enterExitBlurRadius
                                            val backdrop = LocalBackdrop.current
                                            SideEffect {
                                                observedPolicy = policy
                                                observedColor = primary
                                                observedMotion = scheme
                                                observedRadius = radius
                                                hostedBackdrop = backdrop
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            waitForIdle()
            assertEquals(Color.Red, observedColor)
            assertSame(motion, observedMotion)
            assertEquals(27.dp, observedRadius)
            assertNotNull(hostedBackdrop)
            assertNotSame(callerBackdrop, hostedBackdrop)
            for (reducedMotion in listOf(false, true)) for (transparency in
                listOf(false, true)) for (contrast in listOf(false, true)) {
                runOnIdle {
                    settings.value = AccessibilitySettings(transparency, contrast, reducedMotion)
                    blur.value = true
                    animation.value = true
                }
                waitForIdle()
                assertEquals(
                    listOf(!reducedMotion, !reducedMotion, !transparency, contrast),
                    observedPolicy,
                )
            }
            runOnIdle {
                settings.value = AccessibilitySettings()
                animation.value = false
            }
            waitForIdle()
            assertEquals(listOf(true, false, true, false), observedPolicy)
            runOnIdle {
                blur.value = false
                animation.value = true
            }
            waitForIdle()
            assertEquals(listOf(true, false, false, false), observedPolicy)
        }
}
