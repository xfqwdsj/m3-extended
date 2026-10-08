package top.ltfan.m3x.material

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.animation.LocalBlurAnimationEnabled
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.overlay.OverlayDialog
import top.ltfan.m3x.material.overlay.OverlayDialogProperties
import top.ltfan.m3x.material.overlay.SharedTransitionAnchor
import top.ltfan.m3x.overlay.LocalSharedTransitionScope
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState

class SharedMotionSchemeTest {
    @Test
    fun sharedContainerUsesSlowGeometryAndSingleSlowCrossfade() = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        var shown by mutableStateOf(false)
        val motion =
            object : MotionScheme by MotionScheme.expressive() {
                override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = tween(10)

                override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = tween(10)

                override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)

                override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = tween(10)

                override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = tween(10)

                override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(motionScheme = motion) {
                    CompositionLocalProvider(LocalBlurAnimationEnabled provides false) {
                        Box(
                            Modifier.size(480.dp, 640.dp).background(Color.White).testTag("scene"),
                        ) {
                            OverlayHost(remember { OverlayHostState() }) {
                                val shared =
                                    with(LocalSharedTransitionScope.current!!) {
                                        rememberSharedContentState("slow-container")
                                    }
                                SharedTransitionAnchor(
                                    shared,
                                    !shown,
                                    Modifier.size(56.dp).testTag("anchor"),
                                )
                                OverlayDialog(
                                    onDismissRequest = { shown = false },
                                    visible = { shown },
                                    sharedContentState = shared,
                                    containerColor = Color.Red,
                                    properties =
                                        OverlayDialogProperties(scrimColor = Color.Transparent),
                                ) {
                                    Spacer(Modifier.size(240.dp, 200.dp).testTag("panel"))
                                }
                            }
                        }
                    }
                }
            }
        }
        fun appearance(): Pair<Float, Float> {
            val pixels = onNodeWithTag("scene").captureToImage().toPixelMap()
            var result = 0f
            for (y in 0 until pixels.height step 4) for (x in 0 until pixels.width step 4) {
                val color = pixels[x, y]
                result = maxOf(result, color.red - (color.green + color.blue) / 2f)
            }
            var left = pixels.width
            var right = -1
            for (y in 0 until pixels.height step 4) for (x in 0 until pixels.width step 4) {
                val color = pixels[x, y]
                if (color.red - (color.green + color.blue) / 2f > result * 0.5f) {
                    left = minOf(left, x)
                    right = maxOf(right, x)
                }
            }
            return (right - left + 4).toFloat() to result
        }
        mainClock.advanceTimeBy(1200)
        val anchorWidth = onNodeWithTag("anchor").fetchSemanticsNode().boundsInRoot.width
        repeat(2) {
            runOnIdle { shown = true }
            mainClock.advanceTimeBy(160)
            val (enteringWidth, enteringOpacity) = appearance()
            mainClock.advanceTimeBy(1600)
            val (fullWidth, fullOpacity) = appearance()
            val geometry = (enteringWidth - anchorWidth) / (fullWidth - anchorWidth)
            assertTrue(
                geometry in 0.02f..0.35f,
                "Shared geometry must use the slow spec: $geometry",
            )
            assertTrue(
                enteringOpacity / fullOpacity in 0.07f..0.35f,
                "Shared crossfade must use the slow effects spec exactly once: ${enteringOpacity / fullOpacity}",
            )
            runOnIdle { shown = false }
            mainClock.advanceTimeBy(160)
            val (exitingWidth, exitingOpacity) = appearance()
            val remaining = (exitingWidth - anchorWidth) / (fullWidth - anchorWidth)
            assertTrue(remaining in 0.65f..1f, "Shared exit must use the slow spec: $remaining")
            assertTrue(
                exitingOpacity / fullOpacity in 0.6f..0.98f,
                "Shared exit must keep its slow crossfade: $exitingOpacity",
            )
            mainClock.advanceTimeBy(1600)
        }
    }
}
