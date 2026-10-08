package top.ltfan.m3x.material

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.animation.BlurEnterExit
import top.ltfan.m3x.compose.animation.LocalBlurAnimationEnabled
import top.ltfan.m3x.compose.animation.blurTransition
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.compose.ui.visual.LocalBlurEnabled
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering

class BlurTransitionTest {
    @Test
    fun visibilityOpticsFadeWithEitherSwitchAndWithBlurEnabled() = runInitializedComposeUiTest {
        val enabled = mutableStateOf(true)
        val master = mutableStateOf(true)
        setContent {
            ProvideM3xMaterialRendering {
                CompositionLocalProvider(
                    LocalBlurAnimationEnabled provides enabled.value,
                    LocalBlurEnabled provides master.value,
                ) {
                    Box(Modifier.size(100.dp).testTag("sample").background(Color.White)) {
                        Box(Modifier.size(100.dp).blurTransition({ 0.5f }).background(Color.Red))
                    }
                }
            }
        }
        fun green(): Float {
            val pixels = onNodeWithTag("sample").captureToImage().toPixelMap()
            return pixels[pixels.width / 2, pixels.height / 2].green
        }
        assertTrue(green() in 0.45f..0.55f, "Finite visibility blur must also fade")
        runOnIdle { enabled.value = false }
        assertTrue(green() in 0.45f..0.55f, "Animation switch must substitute fading")
        runOnIdle {
            enabled.value = true
            master.value = false
        }
        assertTrue(green() in 0.45f..0.55f, "Master switch must substitute fading")
        runOnIdle {
            master.value = true
        }
        assertTrue(green() in 0.45f..0.55f, "Visibility blur must also fade")
    }

    @Test
    fun bouncySpecsStayWithinTheOpticalRenderRange() = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        val target = mutableStateOf(0f)
        var rawProgress = 0f
        setContent {
            ProvideM3xMaterialRendering {
                val progress =
                    animateFloatAsState(
                        target.value,
                        spring(dampingRatio = 0.25f, stiffness = 120f),
                    )
                rawProgress = progress.value
                Box(Modifier.size(24.dp).background(Color.White).testTag("bounded")) {
                    Box(
                        Modifier.size(24.dp)
                            .blurTransition({ progress.value }, maxRadius = 0.dp)
                            .background(Color.Red),
                    )
                }
            }
        }
        var overshot = false
        var undershot = false
        for (value in listOf(1f, 0f)) {
            runOnIdle { target.value = value }
            repeat(48) {
                mainClock.advanceTimeByFrame()
                val pixels = onNodeWithTag("bounded").captureToImage().toPixelMap()
                val color = pixels[pixels.width / 2, pixels.height / 2]
                assertTrue(color.green in 0f..1f)
                if (rawProgress > 1f) {
                    overshot = true
                    assertTrue(color.green < 0.02f)
                }
                if (rawProgress < 0f) {
                    undershot = true
                    assertTrue(color.green > 0.98f)
                }
            }
            mainClock.advanceTimeBy(3000)
        }
        assertTrue(overshot && undershot, "The custom spec must exercise both render boundaries")
    }

    @Test
    fun sharedContentInheritsContainerOpacityExactlyOnce() = runInitializedComposeUiTest {
        val enabled = mutableStateOf(true)
        setContent {
            ProvideM3xMaterialRendering {
                CompositionLocalProvider(LocalBlurAnimationEnabled provides enabled.value) {
                    Box(Modifier.size(100.dp).testTag("sample").background(Color.White)) {
                        Box(Modifier.graphicsLayer { alpha = 0.5f }) {
                            Box(
                                Modifier.size(100.dp)
                                    .blurTransition({ 0.5f }, fade = false)
                                    .background(Color.Red),
                            )
                        }
                    }
                }
            }
        }
        for (blur in listOf(true, false)) {
            runOnIdle { enabled.value = blur }
            val pixels = onNodeWithTag("sample").captureToImage().toPixelMap()
            val color = pixels[pixels.width / 2, pixels.height / 2]
            assertTrue(
                color.green in 0.45f..0.55f,
                "Shared content must inherit one container fade, blur=$blur color=$color",
            )
        }
    }

    @Test
    fun visibilityHelperOwnsExitLifetimeWithoutCallerTransitions() = runInitializedComposeUiTest {
        val visible = mutableStateOf(true)
        mainClock.autoAdvance = false
        setContent {
            ProvideM3xMaterialRendering {
                AnimatedVisibility(
                    visible.value,
                    enter = EnterTransition.None,
                    exit = ExitTransition.None,
                ) {
                    BlurEnterExit(animationSpec = tween(300)) {
                        Box(Modifier.size(48.dp).testTag("content").background(Color.Red))
                    }
                }
            }
        }
        mainClock.advanceTimeBy(1000)
        runOnIdle { visible.value = false }
        mainClock.advanceTimeByFrame()
        onNodeWithTag("content").assertExists()
        mainClock.advanceTimeBy(2000)
        onNodeWithTag("content").assertDoesNotExist()
    }
}
