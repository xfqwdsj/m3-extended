package top.ltfan.m3x.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.ltfan.m3x.compose.foundation.plus
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.backdrop.backdropEffects
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.overlay.OverlayBottomSheet
import top.ltfan.m3x.material.overlay.OverlayBottomSheetState
import top.ltfan.m3x.material.overlay.OverlayDialog
import top.ltfan.m3x.material.overlay.OverlayDialogProperties
import top.ltfan.m3x.material.overlay.OverlayPlainTooltip
import top.ltfan.m3x.material.overlay.OverlaySnackbarHost
import top.ltfan.m3x.material.overlay.OverlayTooltipBox
import top.ltfan.m3x.material.overlay.OverlayTooltipState
import top.ltfan.m3x.material.overlay.rememberOverlayBottomSheetState
import top.ltfan.m3x.material.overlay.rememberOverlayTooltipState
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState

class OverlayComponentsTest {
    @Test
    fun modalStackExposesOnlyFrontEntryAndRestoresUnderlyingContentInOrder() =
        runInitializedComposeUiTest {
            var firstVisible by mutableStateOf(true)
            var secondVisible by mutableStateOf(true)
            val hostState = OverlayHostState()
            setContent {
                ProvideM3xMaterialRendering {
                    MaterialTheme {
                        OverlayHost(hostState) {
                            Text("Underlying page")
                            OverlayDialog(
                                onDismissRequest = { firstVisible = false },
                                properties =
                                    OverlayDialogProperties(
                                        usePlatformInsets = false,
                                        usePlatformDefaultWidth = false,
                                        animateTransition = false,
                                    ),
                                visible = { firstVisible },
                            ) {
                                Text("First dialog")
                            }
                            OverlayDialog(
                                onDismissRequest = { secondVisible = false },
                                properties =
                                    OverlayDialogProperties(
                                        usePlatformInsets = false,
                                        usePlatformDefaultWidth = false,
                                        animateTransition = false,
                                    ),
                                visible = { secondVisible },
                            ) {
                                Text("Second dialog")
                            }
                        }
                    }
                }
            }

            onNodeWithText("Second dialog").assertIsDisplayed()
            onNodeWithText("First dialog").assertDoesNotExist()
            onNodeWithText("Underlying page").assertDoesNotExist()
            runOnIdle { assertTrue(hostState.isModal) }

            runOnIdle { secondVisible = false }
            waitForIdle()
            onNodeWithText("First dialog").assertIsDisplayed()
            onNodeWithText("Underlying page").assertDoesNotExist()
            runOnIdle { assertTrue(hostState.isModal) }

            runOnIdle { firstVisible = false }
            waitForIdle()
            onNodeWithText("Underlying page").assertExists()
            runOnIdle { assertFalse(hostState.isModal) }
        }

    @Test
    fun tappingOutsideDialogDismissesItOnce() = runInitializedComposeUiTest {
        var visible by mutableStateOf(true)
        var dismissals = 0
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(OverlayHostState()) {
                        OverlayDialog(
                            onDismissRequest = {
                                dismissals++
                                visible = false
                            },
                            properties =
                                OverlayDialogProperties(
                                    usePlatformInsets = false,
                                    usePlatformDefaultWidth = false,
                                    animateTransition = false,
                                ),
                            visible = { visible },
                        ) {
                            Text("Outside-dismiss dialog")
                        }
                    }
                }
            }
        }

        onNodeWithText("Outside-dismiss dialog").assertIsDisplayed()
        onRoot().performTouchInput { click(Offset(0f, 0f)) }
        waitForIdle()
        runOnIdle { assertEquals(1, dismissals) }
        onNodeWithText("Outside-dismiss dialog").assertDoesNotExist()
    }

    @Test
    fun persistentTooltipShowsAndDismissesAndExposesPaneSemantics() = runInitializedComposeUiTest {
        lateinit var tooltipState: OverlayTooltipState
        lateinit var scope: CoroutineScope
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(OverlayHostState()) {
                        val state = rememberOverlayTooltipState(isPersistent = true)
                        tooltipState = state
                        scope = rememberCoroutineScope()
                        OverlayTooltipBox(
                            state = state,
                            tooltip = {
                                OverlayPlainTooltip { Text("Help") }
                            },
                            content = { Text("Anchor") },
                        )
                    }
                }
            }
        }

        runOnIdle { scope.launch { tooltipState.show() } }
        waitUntil { tooltipState.isVisible }
        onNodeWithText("Help").assertIsDisplayed()
        onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.PaneTitle)).assertExists()

        runOnIdle { tooltipState.dismiss() }
        waitUntil { !tooltipState.isVisible }
        onNodeWithText("Help").assertDoesNotExist()
    }

    @Test
    fun tooltipCanBeOpenedWithLongPress() = runInitializedComposeUiTest {
        lateinit var tooltipState: OverlayTooltipState
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(OverlayHostState()) {
                        val state = rememberOverlayTooltipState(isPersistent = true)
                        tooltipState = state
                        OverlayTooltipBox(
                            state = state,
                            tooltip = {
                                OverlayPlainTooltip {
                                    Text("Long-press help")
                                }
                            },
                            content = { Text("Long-press anchor") },
                        )
                    }
                }
            }
        }

        onNodeWithText("Long-press anchor").performTouchInput { longClick() }
        waitUntil { tooltipState.isVisible }
        onNodeWithText("Long-press help").assertIsDisplayed()
    }

    @Test
    fun snackbarAnnouncesAsPoliteLiveRegionAndSupportsSemanticDismiss() =
        runInitializedComposeUiTest {
            lateinit var hostState: SnackbarHostState
            lateinit var scope: CoroutineScope
            setContent {
                ProvideM3xMaterialRendering {
                    MaterialTheme {
                        OverlayHost(OverlayHostState()) {
                            val state = remember { SnackbarHostState() }
                            hostState = state
                            scope = rememberCoroutineScope()
                            OverlaySnackbarHost(state)
                        }
                    }
                }
            }

            runOnIdle {
                scope.launch {
                    hostState.showSnackbar(
                        message = "Saved",
                        actionLabel = "Undo",
                        withDismissAction = true,
                        duration = SnackbarDuration.Indefinite,
                    )
                }
            }
            onNodeWithText("Saved").assertIsDisplayed()
            onNode(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.LiveRegion,
                        LiveRegionMode.Polite,
                    ),
                )
                .assertExists()
            onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.PaneTitle)).assertExists()
            onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
                .performSemanticsAction(SemanticsActions.Dismiss)
            waitUntil { hostState.currentSnackbarData == null }
            assertNull(hostState.currentSnackbarData)
        }

    @Test
    fun bottomSheetCanShowAndHideWithoutReportingProgrammaticHideAsDismissal() =
        runInitializedComposeUiTest {
            lateinit var sheetState: OverlayBottomSheetState
            lateinit var scope: CoroutineScope
            var dismissals = 0
            setContent {
                ProvideM3xMaterialRendering {
                    MaterialTheme {
                        OverlayHost(OverlayHostState()) {
                            val state =
                                rememberOverlayBottomSheetState(skipPartiallyExpanded = true)
                            sheetState = state
                            scope = rememberCoroutineScope()
                            OverlayBottomSheet(
                                onDismissRequest = { dismissals++ },
                                sheetState = state,
                                contentWindowInsets = {
                                    WindowInsets(0)
                                },
                                effects = backdropEffects {},
                            ) {
                                Text("Sheet content")
                            }
                        }
                    }
                }
            }

            runOnIdle { scope.launch { sheetState.show() } }
            waitUntil { sheetState.isVisible }
            onNodeWithText("Sheet content").assertIsDisplayed()

            runOnIdle { scope.launch { sheetState.hide() } }
            waitUntil { !sheetState.isVisible }
            runOnIdle { assertEquals(0, dismissals) }
        }

    @Test
    fun draggingBottomSheetHandleToHiddenCallsDismissRequestOnce() = runInitializedComposeUiTest {
        lateinit var sheetState: OverlayBottomSheetState
        lateinit var scope: CoroutineScope
        var dismissals = 0
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(OverlayHostState()) {
                        val state = rememberOverlayBottomSheetState(skipPartiallyExpanded = true)
                        sheetState = state
                        scope = rememberCoroutineScope()
                        OverlayBottomSheet(
                            onDismissRequest = { dismissals++ },
                            sheetState = state,
                            dragHandle = {
                                Box(Modifier.fillMaxWidth().height(48.dp).testTag("sheet-handle"))
                            },
                            contentWindowInsets = {
                                WindowInsets(0)
                            },
                            effects = backdropEffects {},
                        ) {
                            Text("Draggable sheet")
                        }
                    }
                }
            }
        }

        runOnIdle { scope.launch { sheetState.show() } }
        waitUntil { sheetState.isVisible }
        onNodeWithTag("sheet-handle", useUnmergedTree = true).performTouchInput {
            val start = center
            down(start)
            moveTo(start + Offset(0f, 160f), delayMillis = 100)
            moveTo(start + Offset(0f, 360f), delayMillis = 100)
            up()
        }
        waitUntil { !sheetState.isVisible }
        runOnIdle { assertEquals(1, dismissals) }
    }

    @Test
    fun addedWindowInsetsReadCurrentPaddingAfterRecomposition() = runInitializedComposeUiTest {
        var bottomPadding by mutableIntStateOf(4)
        val dynamicPadding =
            object : PaddingValues {
                override fun calculateLeftPadding(layoutDirection: LayoutDirection) = 0.dp

                override fun calculateTopPadding() = 0.dp

                override fun calculateRightPadding(layoutDirection: LayoutDirection) = 0.dp

                override fun calculateBottomPadding() = bottomPadding.dp
            }
        val baseInsets = WindowInsets(bottom = 3)
        lateinit var combinedInsets: WindowInsets

        setContent {
            ProvideM3xMaterialRendering {
                combinedInsets = baseInsets + dynamicPadding
            }
        }

        val original = combinedInsets
        assertEquals(7, combinedInsets.getBottom(Density(1f)))
        runOnIdle { bottomPadding = 12 }
        waitForIdle()
        assertSame(original, combinedInsets)
        assertEquals(15, combinedInsets.getBottom(Density(1f)))
    }
}
