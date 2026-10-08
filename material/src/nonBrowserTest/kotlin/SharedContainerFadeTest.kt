package top.ltfan.m3x.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.animation.LocalBlurAnimationEnabled
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.compose.ui.accessibility.AccessibilitySettings
import top.ltfan.m3x.compose.ui.accessibility.LocalAccessibilitySettings
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.overlay.MenuFloatingActionButton
import top.ltfan.m3x.material.overlay.OverlayDialog
import top.ltfan.m3x.material.overlay.OverlayDialogProperties
import top.ltfan.m3x.material.overlay.SharedTransitionAnchor
import top.ltfan.m3x.overlay.LocalSharedTransitionScope
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState

class SharedContainerFadeTest {
    @Test fun sharedDialogContainerFadesWithBlur() = probe(fab = false, blurEnabled = true)

    @Test fun sharedDialogContainerFadesWithoutBlur() = probe(fab = false, blurEnabled = false)

    @Test fun sharedFabMenuContainerFadesWithBlur() = probe(fab = true, blurEnabled = true)

    @Test fun sharedFabMenuContainerFadesWithoutBlur() = probe(fab = true, blurEnabled = false)

    @Test
    fun reducedMotionSharedDialogUsesContainerFades() =
        probe(fab = false, blurEnabled = true, reduceMotion = true)

    @Test
    fun reducedMotionFabMenuUsesContainerFades() =
        probe(fab = true, blurEnabled = true, reduceMotion = true)

    private fun probe(fab: Boolean, blurEnabled: Boolean, reduceMotion: Boolean = false) =
        runInitializedComposeUiTest {
            mainClock.autoAdvance = false
            var shown by mutableStateOf(false)
            setContent {
                ProvideM3xMaterialRendering {
                    CompositionLocalProvider(
                        LocalBlurAnimationEnabled provides blurEnabled,
                        LocalAccessibilitySettings provides
                            AccessibilitySettings(reduceMotion = reduceMotion),
                    ) {
                        MaterialTheme {
                            Box(
                                Modifier.size(480.dp, 640.dp)
                                    .background(Color.White)
                                    .testTag("scene"),
                            ) {
                                OverlayHost(remember { OverlayHostState() }) {
                                    if (fab) {
                                        Box(Modifier.fillMaxSize().padding(48.dp)) {
                                            MenuFloatingActionButton(
                                                expanded = { shown },
                                                onExpandedChange = { shown = it },
                                                modifier =
                                                    Modifier.align(Alignment.BottomEnd)
                                                        .testTag("anchor"),
                                                containerColor = Color.Red,
                                                buttonContent = {},
                                                menuContent = {
                                                    Spacer(
                                                        Modifier.size(240.dp, 200.dp)
                                                            .testTag("panel"),
                                                    )
                                                },
                                            )
                                        }
                                    } else {
                                        val shared =
                                            with(LocalSharedTransitionScope.current!!) {
                                                rememberSharedContentState("dialog")
                                            }
                                        SharedTransitionAnchor(
                                            shared,
                                            !shown,
                                            Modifier.size(56.dp).testTag("anchor"),
                                        ) {}
                                        OverlayDialog(
                                            visible = { shown },
                                            onDismissRequest = { shown = false },
                                            sharedContentState = shared,
                                            containerColor = Color.Red,
                                            properties =
                                                OverlayDialogProperties(
                                                    scrimColor = Color.Transparent,
                                                ),
                                        ) {
                                            Spacer(Modifier.size(240.dp, 200.dp).testTag("panel"))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(1000)
            val anchor = onNodeWithTag("anchor").fetchSemanticsNode().boundsInRoot.inflate(4f)
            fun opacity(): Float {
                val pixels = onNodeWithTag("scene").captureToImage().toPixelMap()
                var maximum = 0f
                for (y in 0 until pixels.height step 4) for (x in 0 until pixels.width step 4) {
                    if (fab && anchor.contains(Offset(x.toFloat(), y.toFloat()))) continue
                    val c = pixels[x, y]
                    maximum = maxOf(maximum, c.red - (c.green + c.blue) / 2f)
                }
                return maximum
            }
            repeat(2) {
                runOnIdle { shown = true }
                val entering = mutableListOf<Float>()
                repeat(40) {
                    mainClock.advanceTimeBy(16)
                    entering += opacity()
                }
                mainClock.advanceTimeBy(1000)
                val full = opacity()
                assertTrue(full > 0.3f, "The container must be visible at rest: $full")
                assertTrue(
                    entering.any { it / full in 0.1f..0.85f },
                    "Container must fade in: $entering",
                )
                runOnIdle { shown = false }
                val exiting = mutableListOf<Float>()
                val fullBounds =
                    onNodeWithTag("panel", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                repeat(40) {
                    mainClock.advanceTimeBy(16)
                    val fraction = opacity() / full
                    exiting += fraction
                    if (reduceMotion && fraction > 0.05f) {
                        val bounds =
                            onNodeWithTag("panel", useUnmergedTree = true)
                                .fetchSemanticsNode()
                                .boundsInRoot
                        assertTrue(
                            abs(bounds.width - fullBounds.width) < 1f,
                            "Reduced motion must keep container width: $bounds",
                        )
                        assertTrue(
                            abs(bounds.height - fullBounds.height) < 1f,
                            "Reduced motion must keep container height: $bounds",
                        )
                    }
                }
                assertTrue(exiting.any { it in 0.1f..0.85f }, "Container must fade out: $exiting")
                assertTrue(exiting.last() < 0.03f, "Container must finish fading: $exiting")
                mainClock.advanceTimeBy(1000)
            }
        }
}
