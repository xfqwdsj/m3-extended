package top.ltfan.m3x.material

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.compose.ui.accessibility.AccessibilitySettings
import top.ltfan.m3x.compose.ui.accessibility.LocalAccessibilitySettings
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.overlay.OverlayBottomSheet
import top.ltfan.m3x.material.overlay.rememberOverlayBottomSheetState
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState

class ReducedMotionSheetTest {
    @Test
    fun programmaticDismissalFadesAtItsShownPositionAndCanReopen() = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        val shown = mutableStateOf(false)
        val motion =
            object : MotionScheme by MotionScheme.standard() {
                override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> =
                    tween(400, easing = LinearEasing)

                override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> =
                    tween(400, easing = LinearEasing)
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(motionScheme = motion) {
                    CompositionLocalProvider(
                        LocalAccessibilitySettings provides
                            AccessibilitySettings(reduceMotion = true),
                    ) {
                        Box(
                            Modifier.size(400.dp, 600.dp).background(Color.White).testTag("scene"),
                        ) {
                            OverlayHost(remember { OverlayHostState() }) {
                                OverlayBottomSheet(
                                    visible = { shown.value },
                                    onDismissRequest = { shown.value = false },
                                    sheetState =
                                        rememberOverlayBottomSheetState(
                                            skipPartiallyExpanded = true,
                                        ),
                                    sheetMaxWidth = 240.dp,
                                    shape = RectangleShape,
                                    containerColor = Color.Red,
                                    scrimColor = Color.Transparent,
                                    dragHandle = null,
                                    contentWindowInsets = { WindowInsets(0) },
                                ) {
                                    Spacer(Modifier.size(240.dp, 120.dp).testTag("sheet"))
                                }
                            }
                        }
                    }
                }
            }
        }
        repeat(2) {
            runOnIdle { shown.value = true }
            mainClock.advanceTimeBy(1000)
            val fullBounds =
                onNodeWithTag("sheet", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            runOnIdle { shown.value = false }
            mainClock.advanceTimeBy(160)
            val bounds =
                onNodeWithTag("sheet", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertEquals(fullBounds, bounds)
            val pixels = onNodeWithTag("scene").captureToImage().toPixelMap()
            val sample = pixels[bounds.center.x.toInt(), bounds.center.y.toInt()]
            assertTrue(
                sample.green in 0.1f..0.9f,
                "The sheet must fade rather than disappear: $sample",
            )
            mainClock.advanceTimeBy(1000)
            onNodeWithTag("sheet", useUnmergedTree = true).assertDoesNotExist()
        }
    }
}
