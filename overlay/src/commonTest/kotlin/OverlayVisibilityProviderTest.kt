package top.ltfan.m3x.overlay

import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.overlay.OverlayDialog
import top.ltfan.m3x.material.overlay.OverlayDialogProperties

class OverlayVisibilityProviderTest {
    @Test fun removingTheNavigationRootReleasesItsHostedContent() = checkOwnerRemoval(true)

    @Test fun removingThePanelClosesItsSurvivingNavigationRoot() = checkOwnerRemoval(false)

    private fun checkOwnerRemoval(removeRoot: Boolean) = runInitializedComposeUiTest {
        var mounted by mutableStateOf(true)
        val host = OverlayHostState()
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(host) {
                        if (mounted || !removeRoot) {
                            val state = remember { SeekableTransitionState(false) }
                            val root = rememberTransition(state)
                            val navigation =
                                rememberOverlayNavigationTransition(
                                    state,
                                    root,
                                    targetState = true,
                                    backTarget = false,
                                )
                            if (mounted)
                                OverlayDialog(
                                    visible = { true },
                                    onDismissRequest = {},
                                    navigationTransition = navigation,
                                ) {
                                    Text("Owned dialog")
                                }
                        }
                    }
                }
            }
        }
        onNodeWithText("Owned dialog").assertIsDisplayed()
        mainClock.autoAdvance = false
        runOnIdle { mounted = false }
        mainClock.advanceTimeBy(64)
        if (!removeRoot) {
            onNodeWithText("Owned dialog", useUnmergedTree = true).assertExists()
            runOnIdle { assertTrue(host.entries.isNotEmpty()) }
        } else {
            onNodeWithText("Owned dialog", useUnmergedTree = true).assertDoesNotExist()
            runOnIdle { assertTrue(host.entries.isEmpty()) }
        }
        mainClock.advanceTimeBy(2400)
        onNodeWithText("Owned dialog", useUnmergedTree = true).assertDoesNotExist()
        runOnIdle { assertTrue(host.entries.isEmpty()) }
        runOnIdle { mounted = true }
        mainClock.advanceTimeBy(2400)
        onNodeWithText("Owned dialog").assertIsDisplayed()
    }

    @Test
    fun replacingAHiddenOwnersVisibilityStateSubscribesToTheNewState() =
        runInitializedComposeUiTest {
            var source by mutableStateOf(mutableStateOf(false))
            val host = OverlayHostState()
            setContent {
                ProvideM3xMaterialRendering {
                    MaterialTheme {
                        OverlayHost(host) {
                            val visible = source
                            OverlayDialog(
                                onDismissRequest = { visible.value = false },
                                visible = { visible.value },
                                properties = OverlayDialogProperties(animateTransition = false),
                            ) {
                                Text("Replaceable dialog")
                            }
                        }
                    }
                }
            }
            onNodeWithText("Replaceable dialog").assertDoesNotExist()
            runOnIdle { source = mutableStateOf(false) }
            waitForIdle()
            repeat(2) {
                runOnIdle { source.value = true }
                onNodeWithText("Replaceable dialog").assertIsDisplayed()
                runOnIdle { source.value = false }
                onNodeWithText("Replaceable dialog").assertDoesNotExist()
            }
        }
}
