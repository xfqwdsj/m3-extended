package top.ltfan.m3x.overlay

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.overlay.OverlayDialog
import top.ltfan.m3x.material.overlay.OverlayDialogProperties
import top.ltfan.m3x.material.overlay.SharedTransitionAnchor

class OverlayInputLifetimeTest {
    @Test fun ordinaryDialogReleasesInputOnClose() = probe(shared = false, removeOwner = false)

    @Test fun sharedDialogReleasesInputOnClose() = probe(shared = true, removeOwner = false)

    @Test
    fun ordinaryDialogReleasesInputOnOwnerRemoval() = probe(shared = false, removeOwner = true)

    @Test fun sharedDialogReleasesInputOnOwnerRemoval() = probe(shared = true, removeOwner = true)

    private fun probe(shared: Boolean, removeOwner: Boolean) = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        var shown by mutableStateOf(true)
        var pageClicks = 0
        var dialogClicks = 0
        var dialogFocused = false
        val host = OverlayHostState()
        val motion =
            object : MotionScheme by MotionScheme.expressive() {
                override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)

                override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)

                override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(motionScheme = motion) {
                    val source = rememberBackdropLayer()
                    Box(Modifier.size(480.dp, 640.dp).testTag("scene")) {
                        BackdropHost(source) {
                            OverlayHost(host) {
                                Box(
                                    Modifier.fillMaxSize()
                                        .captureBackdrop(source)
                                        .background(Color.White)
                                        .clickable { pageClicks++ },
                                )
                                val endpoint =
                                    with(LocalSharedTransitionScope.current!!) {
                                        rememberSharedContentState("input-lifetime")
                                    }
                                if (shared)
                                    SharedTransitionAnchor(endpoint, !shown, Modifier.size(56.dp))
                                if (!removeOwner || shown) {
                                    OverlayDialog(
                                        visible = { shown },
                                        onDismissRequest = {},
                                        sharedContentState = endpoint.takeIf { shared },
                                        properties =
                                            OverlayDialogProperties(
                                                scrimColor = Color.Black.copy(alpha = 0.5f),
                                            ),
                                    ) {
                                        Column {
                                            TextField(
                                                "draft",
                                                {},
                                                Modifier.onFocusChanged {
                                                    dialogFocused = it.isFocused
                                                },
                                            )
                                            Button(onClick = { dialogClicks++ }) {
                                                Text("Dialog action")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(2000)
        runOnIdle { assertTrue(dialogFocused, "The visible dialog must own focus") }
        val point = onNodeWithText("Dialog action").fetchSemanticsNode().boundsInRoot.center
        runOnIdle { shown = false }
        mainClock.advanceTimeBy(16)
        runOnIdle {
            assertFalse(host.isModal, "Closing intent must release modal isolation immediately")
            assertFalse(dialogFocused, "Closing intent must release keyboard focus immediately")
        }
        val pixels = onNodeWithTag("scene").captureToImage().toPixelMap()
        val scrim = pixels[pixels.width - 4, 4].red
        assertTrue(scrim in 0.45f..0.6f, "Exit drawing must retain the scrim once: $scrim")
        onNodeWithTag("scene").performTouchInput { click(point) }
        runOnIdle {
            assertEquals(1, pageClicks, "The first click over the former panel must reach the page")
            assertEquals(0, dialogClicks, "Retained exit content must not receive input")
        }
        if (!removeOwner) {
            runOnIdle { shown = true }
            mainClock.advanceTimeBy(2000)
            runOnIdle {
                assertTrue(host.isModal)
                assertTrue(dialogFocused, "Reopening retained content must restore focus")
            }
            onNodeWithText("Dialog action").performTouchInput { click(center) }
            runOnIdle {
                assertEquals(1, dialogClicks)
                assertEquals(1, pageClicks)
            }
        } else {
            mainClock.advanceTimeBy(2000)
            runOnIdle { assertTrue(host.entries.isEmpty()) }
        }
    }
}
