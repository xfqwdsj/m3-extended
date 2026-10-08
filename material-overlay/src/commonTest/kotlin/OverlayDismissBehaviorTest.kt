package top.ltfan.m3x.material.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog as PlatformDialog
import androidx.compose.ui.window.DialogProperties as PlatformDialogProperties
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.Button
import top.ltfan.m3x.material.backdrop.backdropEffects
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState

class OverlayDismissBehaviorTest {
    @Test
    fun dialogButtonClickSurvivesTouchMovement() = runInitializedComposeUiTest {
        var clicks = 0
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(remember { OverlayHostState() }) {
                        OverlayDialog(
                            onDismissRequest = {},
                            properties =
                                OverlayDialogProperties(
                                    usePlatformInsets = false,
                                    usePlatformDefaultWidth = false,
                                    animateTransition = false,
                                ),
                        ) {
                            Button(
                                onClick = { clicks++ },
                                modifier = Modifier.testTag("dialog-action"),
                            ) {
                                Text("Action")
                            }
                        }
                    }
                }
            }
        }

        onNodeWithTag("dialog-action").performTouchInput { click() }
        runOnIdle { assertEquals(1, clicks) }

        onNodeWithTag("dialog-action").performTouchInput {
            val center = center
            down(center)
            moveTo(center + Offset(1f, 0f), delayMillis = 0)
            up()
        }
        runOnIdle { assertEquals(2, clicks) }
    }

    @Test
    fun dialogButtonClickSurvivesMouseMovement() = runInitializedComposeUiTest {
        var clicks = 0
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(remember { OverlayHostState() }) {
                        OverlayDialog(
                            onDismissRequest = {},
                            properties =
                                OverlayDialogProperties(
                                    usePlatformInsets = false,
                                    usePlatformDefaultWidth = false,
                                    animateTransition = false,
                                ),
                        ) {
                            Button(
                                onClick = { clicks++ },
                                modifier = Modifier.testTag("dialog-action"),
                            ) {
                                Text("Action")
                            }
                        }
                    }
                }
            }
        }

        onNodeWithTag("dialog-action").performMouseInput { click() }
        runOnIdle { assertEquals(1, clicks) }

        onNodeWithTag("dialog-action").performMouseInput {
            val center = center
            moveTo(center)
            press()
            moveTo(center + Offset(1f, 0f), delayMillis = 0)
            release()
        }
        runOnIdle { assertEquals(2, clicks) }
    }

    @Test
    fun platformDialogButtonClickSurvivesTouchAndMouseMovement() = runInitializedComposeUiTest {
        var clicks = 0
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    PlatformDialog(
                        onDismissRequest = {},
                        properties = PlatformDialogProperties(usePlatformDefaultWidth = false),
                    ) {
                        Button(
                            onClick = { clicks++ },
                            modifier = Modifier.testTag("platform-dialog-action"),
                        ) {
                            Text("Action")
                        }
                    }
                }
            }
        }

        onNodeWithTag("platform-dialog-action").performTouchInput {
            val center = center
            down(center)
            moveTo(center + Offset(1f, 0f), delayMillis = 0)
            up()
        }
        runOnIdle { assertEquals(1, clicks) }

        onNodeWithTag("platform-dialog-action").performMouseInput {
            val center = center
            moveTo(center)
            press()
            moveTo(center + Offset(1f, 0f), delayMillis = 0)
            release()
        }
        runOnIdle { assertEquals(2, clicks) }
    }

    @Test
    fun dialogIgnoresSecondaryButtonAndDismissesOnPrimaryRelease() = runInitializedComposeUiTest {
        var dismissals = 0
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(remember { OverlayHostState() }) {
                        OverlayDialog(
                            onDismissRequest = { dismissals++ },
                            properties =
                                OverlayDialogProperties(
                                    usePlatformInsets = false,
                                    usePlatformDefaultWidth = false,
                                    animateTransition = false,
                                ),
                        ) {
                            Box(Modifier.size(120.dp).testTag("dialog-panel"))
                        }
                    }
                }
            }
        }

        onRoot().performMouseInput { rightClick(Offset(1f, 1f)) }
        runOnIdle { assertEquals(0, dismissals) }

        onRoot().performMouseInput {
            moveTo(Offset(1f, 1f))
            press()
        }
        runOnIdle { assertEquals(0, dismissals) }
        onRoot().performMouseInput { release() }
        runOnIdle { assertEquals(1, dismissals) }
    }

    @Test
    fun dialogTouchOutsideMustEndOutsideAndCancelledTouchDoesNotDismiss() =
        runInitializedComposeUiTest {
            var dismissals = 0
            setContent {
                ProvideM3xMaterialRendering {
                    MaterialTheme {
                        OverlayHost(remember { OverlayHostState() }) {
                            OverlayDialog(
                                onDismissRequest = { dismissals++ },
                                properties =
                                    OverlayDialogProperties(
                                        usePlatformInsets = false,
                                        usePlatformDefaultWidth = false,
                                        animateTransition = false,
                                    ),
                            ) {
                                Box(Modifier.size(120.dp).testTag("dialog-panel"))
                            }
                        }
                    }
                }
            }

            val panelCenter = onNodeWithTag("dialog-panel").fetchSemanticsNode().boundsInRoot.center
            onRoot().performTouchInput {
                down(Offset(1f, 1f))
                moveTo(panelCenter)
                up()
            }
            runOnIdle { assertEquals(0, dismissals) }
            onRoot().performTouchInput {
                down(Offset(1f, 1f))
                cancel()
            }
            runOnIdle { assertEquals(0, dismissals) }
            onRoot().performTouchInput { click(Offset(1f, 1f)) }
            runOnIdle { assertEquals(1, dismissals) }
        }

    @Test
    fun dialogDismissSemanticsFollowItsProperties() = runInitializedComposeUiTest {
        var allowDismiss by mutableStateOf(true)
        var dismissals = 0
        var dismissLabel = ""
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    dismissLabel = OverlayStrings.dismissDialog()
                    OverlayHost(remember { OverlayHostState() }) {
                        OverlayDialog(
                            onDismissRequest = { dismissals++ },
                            properties =
                                OverlayDialogProperties(
                                    dismissOnBackPress = allowDismiss,
                                    dismissOnClickOutside = allowDismiss,
                                    animateTransition = false,
                                ),
                        ) {
                            Text("Dialog content")
                        }
                    }
                }
            }
        }

        val dismissNode = onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
        assertEquals(
            dismissLabel,
            dismissNode.fetchSemanticsNode().config[SemanticsActions.Dismiss].label,
        )
        dismissNode.performSemanticsAction(SemanticsActions.Dismiss)
        runOnIdle { assertEquals(1, dismissals) }

        runOnIdle { allowDismiss = false }
        onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)).assertDoesNotExist()
    }

    @Test
    fun menuAcceptsAnyButtonOnPressAndExposesDismissAction() = runInitializedComposeUiTest {
        var dismissals = 0
        var dismissLabel = ""
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    dismissLabel = OverlayStrings.dismissMenu()
                    OverlayHost(remember { OverlayHostState() }) {
                        Box(Modifier.fillMaxSize()) {
                            Box(Modifier.size(48.dp).align(Alignment.Center)) {
                                OverlayMenu(
                                    expanded = { true },
                                    onDismissRequest = { dismissals++ },
                                ) {
                                    Text("Menu item")
                                }
                            }
                        }
                    }
                }
            }
        }

        onRoot().performMouseInput {
            moveTo(Offset(1f, 1f))
            press(MouseButton.Secondary)
        }
        runOnIdle { assertEquals(1, dismissals) }
        onRoot().performMouseInput { release(MouseButton.Secondary) }
        runOnIdle { assertEquals(1, dismissals) }
        onRoot().performMouseInput { click(Offset(1f, 1f)) }
        runOnIdle { assertEquals(2, dismissals) }

        val dismissNode = onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
        assertEquals(
            dismissLabel,
            dismissNode.fetchSemanticsNode().config[SemanticsActions.Dismiss].label,
        )
        dismissNode.performSemanticsAction(SemanticsActions.Dismiss)
        runOnIdle { assertEquals(3, dismissals) }
    }

    @Test
    fun menuWithBothDismissRoutesDisabledHasNoDismissAction() = runInitializedComposeUiTest {
        var dismissals = 0
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(remember { OverlayHostState() }) {
                        Box(Modifier.fillMaxSize()) {
                            Box(Modifier.size(48.dp).align(Alignment.Center)) {
                                OverlayMenu(
                                    expanded = { true },
                                    onDismissRequest = { dismissals++ },
                                    dismissOnClickOutside = false,
                                    dismissOnBackPress = false,
                                ) {
                                    Text("Menu item")
                                }
                            }
                        }
                    }
                }
            }
        }
        onRoot().performMouseInput { rightClick(Offset(1f, 1f)) }
        runOnIdle { assertEquals(0, dismissals) }
        onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)).assertDoesNotExist()
    }

    @Test
    fun menuTouchOutsideDismissesOnPressOnlyOnce() = runInitializedComposeUiTest {
        var dismissals = 0
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(remember { OverlayHostState() }) {
                        Box(Modifier.fillMaxSize()) {
                            Box(Modifier.size(48.dp).align(Alignment.Center)) {
                                OverlayMenu(
                                    expanded = { true },
                                    onDismissRequest = { dismissals++ },
                                ) {
                                    Text("Menu item")
                                }
                            }
                        }
                    }
                }
            }
        }
        onRoot().performTouchInput { down(Offset(1f, 1f)) }
        runOnIdle { assertEquals(1, dismissals) }
        onRoot().performTouchInput { up() }
        runOnIdle { assertEquals(1, dismissals) }
    }

    @Test
    fun backReachesTopmostOverlayOnce() = runInitializedComposeUiTest {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val owner =
            object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher = dispatcher
            }
        var dialogDismissals = 0
        var menuDismissals = 0
        var menuExpanded by mutableStateOf(true)
        setContent {
            ProvideM3xMaterialRendering {
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                    MaterialTheme {
                        OverlayHost(remember { OverlayHostState() }) {
                            OverlayDialog(
                                onDismissRequest = { dialogDismissals++ },
                                properties = OverlayDialogProperties(animateTransition = false),
                            ) {
                                Text("Dialog content")
                            }
                            Box(Modifier.fillMaxSize()) {
                                Box(Modifier.size(48.dp).align(Alignment.Center)) {
                                    OverlayMenu(
                                        expanded = { menuExpanded },
                                        onDismissRequest = {
                                            menuDismissals++
                                            menuExpanded = false
                                        },
                                    ) {
                                        Text("Menu item")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        waitForIdle()
        runOnIdle { input.backCompleted() }
        runOnIdle {
            assertEquals(1, menuDismissals)
            assertEquals(0, dialogDismissals)
        }
        waitForIdle()
        runOnIdle { input.backCompleted() }
        runOnIdle { assertEquals(1, dialogDismissals) }
    }

    @Test
    fun sheetOutsidePolicyBlocksUnderlyingInputAndItsDismissActions() =
        runInitializedComposeUiTest {
            lateinit var sheetState: OverlayBottomSheetState
            var pageClicks = 0
            var dismissals = 0
            var closeSheetLabel = ""
            setContent {
                ProvideM3xMaterialRendering {
                    MaterialTheme {
                        closeSheetLabel = OverlayStrings.closeSheet()
                        OverlayHost(remember { OverlayHostState() }) {
                            Box(Modifier.fillMaxSize().clickable { pageClicks++ })
                            val state =
                                rememberOverlayBottomSheetState(skipPartiallyExpanded = true)
                            sheetState = state
                            OverlayBottomSheet(
                                onDismissRequest = { dismissals++ },
                                sheetState = state,
                                sheetGesturesEnabled = false,
                                properties =
                                    OverlayBottomSheetProperties(
                                        shouldDismissOnBackPress = false,
                                        shouldDismissOnClickOutside = false,
                                    ),
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

            waitUntil { sheetState.isVisible }
            onRoot().performTouchInput { click(Offset(1f, 1f)) }
            onRoot().performMouseInput { rightClick(Offset(1f, 1f)) }
            runOnIdle {
                assertTrue(sheetState.isVisible)
                assertEquals(0, dismissals)
                assertEquals(0, pageClicks)
            }
            onNodeWithContentDescription(closeSheetLabel).assertDoesNotExist()
            onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)).assertDoesNotExist()
        }

    @Test
    fun sheetScrimSemanticsDismissAfterHideCompletes() = runInitializedComposeUiTest {
        lateinit var sheetState: OverlayBottomSheetState
        var dismissals = 0
        var closeSheetLabel = ""
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    closeSheetLabel = OverlayStrings.closeSheet()
                    OverlayHost(remember { OverlayHostState() }) {
                        val state = rememberOverlayBottomSheetState(skipPartiallyExpanded = true)
                        sheetState = state
                        OverlayBottomSheet(
                            onDismissRequest = { dismissals++ },
                            sheetState = state,
                            sheetGesturesEnabled = false,
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

        waitUntil { sheetState.isVisible }
        onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)).assertExists()
        val scrim = onNodeWithContentDescription(closeSheetLabel)
        scrim.performSemanticsAction(SemanticsActions.OnClick)
        waitUntil { !sheetState.isVisible }
        runOnIdle { assertEquals(1, dismissals) }
    }

    @Test
    fun sheetOnlyAcceptsPrimaryMouseReleaseOutside() = runInitializedComposeUiTest {
        lateinit var sheetState: OverlayBottomSheetState
        var dismissals = 0
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(remember { OverlayHostState() }) {
                        val state = rememberOverlayBottomSheetState(skipPartiallyExpanded = true)
                        sheetState = state
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

        waitUntil { sheetState.isVisible }
        onRoot().performMouseInput { rightClick(Offset(1f, 1f)) }
        runOnIdle {
            assertEquals(0, dismissals)
            assertTrue(sheetState.isVisible)
        }
        onRoot().performMouseInput {
            moveTo(Offset(1f, 1f))
            press()
        }
        runOnIdle { assertEquals(0, dismissals) }
        onRoot().performMouseInput { release() }
        waitUntil { !sheetState.isVisible }
        runOnIdle { assertEquals(1, dismissals) }
    }

    @Test
    fun sheetTouchDismissesOnReleaseAndBackUsesTheSameHidePath() = runInitializedComposeUiTest {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val owner =
            object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher = dispatcher
            }
        lateinit var sheetState: OverlayBottomSheetState
        lateinit var scope: CoroutineScope
        var dismissals = 0
        setContent {
            ProvideM3xMaterialRendering {
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                    MaterialTheme {
                        OverlayHost(remember { OverlayHostState() }) {
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
        }

        waitUntil { sheetState.isVisible }
        onRoot().performTouchInput { down(Offset(1f, 1f)) }
        runOnIdle { assertEquals(0, dismissals) }
        onRoot().performTouchInput { up() }
        waitUntil { !sheetState.isVisible }
        runOnIdle { assertEquals(1, dismissals) }

        runOnIdle { scope.launch { sheetState.show() } }
        waitUntil { sheetState.isVisible }
        runOnIdle { input.backCompleted() }
        waitUntil { !sheetState.isVisible }
        runOnIdle { assertEquals(2, dismissals) }
    }

    @Test
    fun focusableTooltipDismissActionRestoresAnchorFocus() = runInitializedComposeUiTest {
        lateinit var tooltipState: OverlayTooltipState
        lateinit var scope: CoroutineScope
        var dismissals = 0
        var dismissLabel = ""
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    dismissLabel = OverlayStrings.dismissTooltip()
                    OverlayHost(remember { OverlayHostState() }) {
                        val state = rememberOverlayTooltipState(isPersistent = true)
                        tooltipState = state
                        scope = rememberCoroutineScope()
                        OverlayTooltipBox(
                            tooltip = { Text("Tooltip body") },
                            state = state,
                            focusable = true,
                            onDismissRequest = {
                                dismissals++
                                state.dismiss()
                            },
                        ) {
                            Box(Modifier.testTag("tooltip-anchor").focusable()) {
                                Text("Tooltip anchor")
                            }
                        }
                    }
                }
            }
        }

        runOnIdle { scope.launch { tooltipState.show() } }
        waitUntil { tooltipState.isVisible }
        val dismissNode = onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
        assertEquals(
            dismissLabel,
            dismissNode.fetchSemanticsNode().config[SemanticsActions.Dismiss].label,
        )
        dismissNode.performSemanticsAction(SemanticsActions.Dismiss)
        waitUntil { !tooltipState.isVisible }
        waitForIdle()
        runOnIdle { assertEquals(1, dismissals) }
        onNodeWithTag("tooltip-anchor").assertIsFocused()
    }

    @Test
    fun focusableTooltipAcceptsSecondaryMousePressAndBack() = runInitializedComposeUiTest {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val owner =
            object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher = dispatcher
            }
        lateinit var tooltipState: OverlayTooltipState
        lateinit var scope: CoroutineScope
        var dismissals = 0
        setContent {
            ProvideM3xMaterialRendering {
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                    MaterialTheme {
                        OverlayHost(remember { OverlayHostState() }) {
                            Box(Modifier.fillMaxSize()) {
                                val state = rememberOverlayTooltipState(isPersistent = true)
                                tooltipState = state
                                scope = rememberCoroutineScope()
                                Box(Modifier.align(Alignment.Center)) {
                                    OverlayTooltipBox(
                                        tooltip = { Text("Tooltip body") },
                                        state = state,
                                        focusable = true,
                                        onDismissRequest = {
                                            dismissals++
                                            state.dismiss()
                                        },
                                    ) {
                                        Text("Tooltip anchor")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        runOnIdle { scope.launch { tooltipState.show() } }
        waitUntil { tooltipState.isVisible }
        onRoot().performMouseInput {
            moveTo(Offset(1f, 1f))
            press(MouseButton.Secondary)
        }
        runOnIdle { assertEquals(1, dismissals) }
        onRoot().performMouseInput { release(MouseButton.Secondary) }
        runOnIdle { assertEquals(1, dismissals) }

        runOnIdle { scope.launch { tooltipState.show() } }
        waitUntil { tooltipState.isVisible }
        runOnIdle { input.backCompleted() }
        waitUntil { !tooltipState.isVisible }
        runOnIdle { assertEquals(2, dismissals) }
    }

    @Test
    fun plainTooltipDoesNotExposeDismissAction() = runInitializedComposeUiTest {
        lateinit var tooltipState: OverlayTooltipState
        lateinit var scope: CoroutineScope
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(remember { OverlayHostState() }) {
                        val state = rememberOverlayTooltipState(isPersistent = true)
                        tooltipState = state
                        scope = rememberCoroutineScope()
                        OverlayTooltipBox(
                            tooltip = { Text("Tooltip body") },
                            state = state,
                            focusable = false,
                        ) {
                            Text("Tooltip anchor")
                        }
                    }
                }
            }
        }
        runOnIdle { scope.launch { tooltipState.show() } }
        waitUntil { tooltipState.isVisible }
        onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)).assertDoesNotExist()
    }

    @Test
    fun focusableTooltipTouchOutsideDismissesOnPressOnlyOnce() = runInitializedComposeUiTest {
        lateinit var tooltipState: OverlayTooltipState
        lateinit var scope: CoroutineScope
        var dismissals = 0
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(remember { OverlayHostState() }) {
                        Box(Modifier.fillMaxSize()) {
                            val state = rememberOverlayTooltipState(isPersistent = true)
                            tooltipState = state
                            scope = rememberCoroutineScope()
                            Box(Modifier.align(Alignment.Center)) {
                                OverlayTooltipBox(
                                    tooltip = { Text("Tooltip body") },
                                    state = state,
                                    focusable = true,
                                    onDismissRequest = {
                                        dismissals++
                                        state.dismiss()
                                    },
                                ) {
                                    Text("Tooltip anchor")
                                }
                            }
                        }
                    }
                }
            }
        }
        runOnIdle { scope.launch { tooltipState.show() } }
        waitUntil { tooltipState.isVisible }
        onRoot().performTouchInput { down(Offset(1f, 1f)) }
        runOnIdle { assertEquals(1, dismissals) }
        onRoot().performTouchInput { up() }
        runOnIdle { assertEquals(1, dismissals) }
    }
}
