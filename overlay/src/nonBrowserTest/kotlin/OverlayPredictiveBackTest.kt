package top.ltfan.m3x.overlay

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.backdrop.backdropEffects
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.overlay.BottomSheetSharedAnchor
import top.ltfan.m3x.material.overlay.OverlayBottomSheet
import top.ltfan.m3x.material.overlay.OverlayBottomSheetState
import top.ltfan.m3x.material.overlay.OverlayBottomSheetValue
import top.ltfan.m3x.material.overlay.OverlayDialog
import top.ltfan.m3x.material.overlay.OverlayDialogProperties
import top.ltfan.m3x.material.overlay.OverlayMenu
import top.ltfan.m3x.material.overlay.OverlayTooltipBox
import top.ltfan.m3x.material.overlay.SharedTransitionAnchor
import top.ltfan.m3x.material.overlay.rememberOverlayBottomSheetState
import top.ltfan.m3x.material.overlay.rememberOverlayTooltipState

class OverlayPredictiveBackTest {
    @Test fun dialogSeeksItsExitAndRestoresOnCancel() = checkExit(Panel.Dialog, false)

    @Test fun sharedDialogSeeksItsExitAndRestoresOnCancel() = checkExit(Panel.Dialog, true)

    @Test fun menuSeeksItsExitAndRestoresOnCancel() = checkExit(Panel.Menu, false)

    @Test fun sharedMenuSeeksItsExitAndRestoresOnCancel() = checkExit(Panel.Menu, true)

    @Test fun sheetSeeksItsExitAndRestoresOnCancel() = checkExit(Panel.Sheet, false)

    @Test fun sharedSheetSeeksItsExitAndRestoresOnCancel() = checkExit(Panel.Sheet, true)

    @Test fun focusableTooltipSeeksItsExitAndRestoresOnCancel() = checkExit(Panel.Tooltip, false)

    @Test
    fun sharedFocusableTooltipSeeksItsExitAndRestoresOnCancel() = checkExit(Panel.Tooltip, true)

    private fun checkExit(panel: Panel, shared: Boolean) = runInitializedComposeUiTest {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val owner =
            object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher = dispatcher
            }
        val host = OverlayHostState()
        var visible by mutableStateOf(true)
        var dismissals = 0
        lateinit var sheet: OverlayBottomSheetState
        val dismiss = {
            dismissals++
            visible = false
        }
        setContent {
            ProvideM3xMaterialRendering {
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                    MaterialTheme(motionScheme = TestMotion) {
                        Box(Modifier.fillMaxSize().background(Color.White).testTag("scene")) {
                            OverlayHost(host) {
                                localSharedTransitionScope {
                                    val sharedState =
                                        rememberSharedContentState("panel").takeIf { shared }
                                    if (sharedState != null && panel != Panel.Sheet) {
                                        SharedTransitionAnchor(sharedState, !visible) {
                                            Box(Modifier.requiredSize(40.dp).background(Color.Blue))
                                        }
                                    }
                                    when (panel) {
                                        Panel.Dialog ->
                                            OverlayDialog(
                                                onDismissRequest = dismiss,
                                                visible = { visible },
                                                sharedContentState = sharedState,
                                                properties =
                                                    OverlayDialogProperties(
                                                        usePlatformInsets = false,
                                                        scrimColor = Color.Black,
                                                    ),
                                                effects = backdropEffects {},
                                            ) {
                                                Box(Modifier.size(200.dp).background(Color.Red))
                                            }
                                        Panel.Menu ->
                                            OverlayMenu(
                                                expanded = { visible },
                                                onDismissRequest = dismiss,
                                                sharedContentState = sharedState,
                                                effects = backdropEffects {},
                                            ) {
                                                Box(Modifier.size(200.dp).background(Color.Red))
                                            }
                                        Panel.Tooltip -> {
                                            val tooltip =
                                                rememberOverlayTooltipState(isPersistent = true)
                                            LaunchedEffect(visible) {
                                                if (visible) tooltip.show() else tooltip.dismiss()
                                            }
                                            OverlayTooltipBox(
                                                tooltip = {
                                                    Box(Modifier.size(200.dp).background(Color.Red))
                                                },
                                                state = tooltip,
                                                focusable = true,
                                                enableUserInput = false,
                                                sharedContentState = sharedState,
                                                onDismissRequest = {
                                                    tooltip.dismiss()
                                                    dismiss()
                                                },
                                            ) {
                                                Box(Modifier.requiredSize(40.dp))
                                            }
                                        }
                                        Panel.Sheet -> {
                                            sheet =
                                                rememberOverlayBottomSheetState(
                                                    skipPartiallyExpanded = true,
                                                )
                                            if (sharedState != null)
                                                BottomSheetSharedAnchor(sharedState, sheet) {
                                                    Box(
                                                        Modifier.requiredSize(40.dp)
                                                            .background(Color.Blue),
                                                    )
                                                }
                                            OverlayBottomSheet(
                                                onDismissRequest = dismiss,
                                                visible = { visible },
                                                sheetState = sheet,
                                                sharedContentState = sharedState,
                                                contentWindowInsets = { WindowInsets(0) },
                                                scrimColor = Color.Black,
                                                dragHandle = null,
                                                effects = backdropEffects {},
                                            ) {
                                                Box(Modifier.size(240.dp).background(Color.Red))
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
        waitForIdle()
        mainClock.autoAdvance = false
        val shown = onNodeWithTag("scene").captureToImage().toPixelMap()
        runOnIdle { visible = false }
        mainClock.advanceTimeBy(528)
        val ordinaryExit = onNodeWithTag("scene").captureToImage().toPixelMap()
        runOnIdle { visible = true }
        mainClock.advanceTimeBy(2000)
        assertTrue(
            difference(shown, onNodeWithTag("scene").captureToImage().toPixelMap()) < 0.005f,
            "Ordinary reopening must restore the original panel before any gesture: $panel shared=$shared",
        )
        runOnIdle { input.backStarted(NavigationEvent(progress = 0f)) }
        mainClock.advanceTimeBy(64)
        assertTrue(
            difference(shown, onNodeWithTag("scene").captureToImage().toPixelMap()) < 0.005f,
            "Starting a zero-progress Back gesture must preserve the displayed frame",
        )
        runOnIdle { input.backProgressed(NavigationEvent(progress = 0.5f)) }
        mainClock.advanceTimeBy(64)
        val preview = onNodeWithTag("scene").captureToImage().toPixelMap()
        assertTrue(
            difference(ordinaryExit, preview) < 0.04f,
            "Preview must match ordinary exit pixels: $panel shared=$shared difference=${difference(ordinaryExit, preview)}",
        )
        assertTrue(
            difference(shown, preview) > 0.005f,
            "Expected visible exit preview: $panel shared=$shared",
        )
        runOnIdle {
            assertEquals(0, dismissals)
            assertTrue(visible)
        }
        runOnIdle { input.backProgressed(NavigationEvent(progress = 0.5f)) }
        mainClock.advanceTimeBy(64)
        val atCancel = onNodeWithTag("scene").captureToImage().toPixelMap()
        runOnIdle { input.backCancelled() }
        mainClock.advanceTimeBy(32)
        // A canceled gesture hands the timeline back to the ordinary animation without moving the
        // frame it had reached: the first frame after the cancellation continues the preview.
        assertTrue(
            difference(atCancel, onNodeWithTag("scene").captureToImage().toPixelMap()) < 0.05f,
            "Cancellation must continue the preview frame: $panel shared=$shared",
        )
        mainClock.advanceTimeBy(1408)
        val restored = onNodeWithTag("scene").captureToImage().toPixelMap()
        assertTrue(
            difference(shown, restored) < 0.005f,
            "Cancellation must restore pixels: $panel shared=$shared",
        )
        runOnIdle { assertEquals(0, dismissals) }
        runOnIdle { input.backStarted(NavigationEvent(progress = 0f)) }
        mainClock.advanceTimeBy(64)
        runOnIdle { input.backProgressed(NavigationEvent(progress = 0.5f)) }
        mainClock.advanceTimeBy(64)
        assertTrue(
            difference(preview, onNodeWithTag("scene").captureToImage().toPixelMap()) < 0.005f,
            "Repeating a cancelled gesture must sample the same exit: $panel shared=$shared",
        )
        runOnIdle { input.backCompleted() }
        mainClock.advanceTimeBy(32)
        val committed = onNodeWithTag("scene").captureToImage().toPixelMap()
        assertTrue(
            difference(preview, committed) < 0.04f,
            "Completion must continue the preview: $panel shared=$shared",
        )
        mainClock.advanceTimeBy(1600)
        runOnIdle {
            assertEquals(1, dismissals)
            assertTrue(!visible)
        }
        runOnIdle { visible = true }
        mainClock.advanceTimeBy(1600)
        val reopened = onNodeWithTag("scene").captureToImage().toPixelMap()
        assertTrue(difference(shown, reopened) < 0.005f)
        dispatcher.dispose()
    }

    @Test fun entryKeepsOneTimelineThroughAGesture() = checkTimelineIdentity(cancel = true)

    @Test fun completedGestureKeepsTheEntryTimeline() = checkTimelineIdentity(cancel = false)

    private fun checkTimelineIdentity(cancel: Boolean) = runInitializedComposeUiTest {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val owner =
            object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher = dispatcher
            }
        val host = OverlayHostState()
        var visible by mutableStateOf(true)
        var dismissals = 0
        setContent {
            ProvideM3xMaterialRendering {
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                    MaterialTheme(motionScheme = TestMotion) {
                        Box(Modifier.fillMaxSize().background(Color.White).testTag("scene")) {
                            OverlayHost(host) {
                                localSharedTransitionScope {
                                    OverlayDialog(
                                        onDismissRequest = {
                                            dismissals++
                                            visible = false
                                        },
                                        visible = { visible },
                                        effects = backdropEffects {},
                                    ) {
                                        Box(Modifier.size(200.dp).background(Color.Red))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        waitForIdle()
        mainClock.autoAdvance = false
        val entry = host.entries.single()
        val timeline = entry.visibleState
        runOnIdle { input.backStarted(NavigationEvent(progress = 0f)) }
        mainClock.advanceTimeBy(64)
        runOnIdle { input.backProgressed(NavigationEvent(progress = 0.5f)) }
        mainClock.advanceTimeBy(64)
        runOnIdle {
            assertSame(
                timeline,
                entry.visibleState,
                "A running Back gesture must not replace the entry timeline",
            )
        }
        if (cancel) {
            runOnIdle { input.backCancelled() }
            mainClock.advanceTimeBy(48)
            val rewindingFraction = entry.visibleState.fraction
            runOnIdle {
                assertEquals(OverlayBackPhase.Cancelled, entry.backPhase)
                input.backStarted(NavigationEvent(progress = 0f))
                input.backProgressed(NavigationEvent(progress = 0.9f))
            }
            mainClock.advanceTimeBy(48)
            runOnIdle {
                assertEquals(OverlayBackPhase.Cancelled, entry.backPhase)
                assertTrue(entry.visibleState.fraction <= rewindingFraction)
            }
            mainClock.advanceTimeBy(2000)
            // Even if the ignored sequence ends after the rewind, it cannot dismiss the entry.
            runOnIdle { input.backCompleted() }
            runOnIdle {
                assertSame(
                    timeline,
                    entry.visibleState,
                    "Cancelling a Back gesture must keep the entry timeline",
                )
                assertEquals(OverlayBackPhase.Idle, entry.backPhase)
                assertSame(entry, host.entries.single())
                assertEquals(0, dismissals)
            }
        } else {
            runOnIdle { input.backCompleted() }
            mainClock.advanceTimeBy(2000)
            runOnIdle {
                assertSame(
                    timeline,
                    entry.visibleState,
                    "Completing a Back gesture must keep the entry timeline",
                )
                assertEquals(OverlayBackPhase.Idle, entry.backPhase)
                assertEquals(1, dismissals)
            }
        }
        dispatcher.dispose()
    }

    @Test
    fun repeatedKeyedReopenLeavesOneEntry() = runInitializedComposeUiTest {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val owner =
            object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher = dispatcher
            }
        val host = OverlayHostState()
        var active by mutableStateOf<String?>("a")
        setContent {
            ProvideM3xMaterialRendering {
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                    MaterialTheme(motionScheme = TestMotion) {
                        Box(Modifier.fillMaxSize().background(Color.White).testTag("scene")) {
                            OverlayHost(host) {
                                localSharedTransitionScope {
                                    val current = active
                                    if (current != null)
                                        key(current) {
                                            OverlayDialog(
                                                onDismissRequest = { active = null },
                                                visible = { true },
                                                effects = backdropEffects {},
                                            ) {
                                                Box(Modifier.size(200.dp).background(Color.Red))
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
        mainClock.autoAdvance = false
        repeat(5) { iteration ->
            runOnIdle { input.backStarted(NavigationEvent(progress = 0f)) }
            mainClock.advanceTimeBy(64)
            runOnIdle { input.backProgressed(NavigationEvent(progress = 0.5f)) }
            mainClock.advanceTimeBy(64)
            runOnIdle { input.backCompleted() }
            mainClock.advanceTimeBy(16)
            runOnIdle { active = "a" }
            mainClock.advanceTimeBy(2000)
            runOnIdle {
                assertEquals(
                    1,
                    host.entries.size,
                    "Reopen $iteration left a residual overlay entry",
                )
                assertEquals(OverlayBackPhase.Idle, host.entries.single().backPhase)
            }
        }
        dispatcher.dispose()
    }

    @Test
    fun ownerLeavingMidGestureStillRemovesTheEntry() = runInitializedComposeUiTest {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val owner =
            object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher = dispatcher
            }
        val host = OverlayHostState()
        var visible by mutableStateOf(true)
        var composed by mutableStateOf(true)
        setContent {
            ProvideM3xMaterialRendering {
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                    MaterialTheme(motionScheme = TestMotion) {
                        Box(Modifier.fillMaxSize().background(Color.White).testTag("scene")) {
                            OverlayHost(host) {
                                localSharedTransitionScope {
                                    if (composed)
                                        OverlayDialog(
                                            onDismissRequest = {},
                                            visible = { visible },
                                            effects = backdropEffects {},
                                        ) {
                                            Box(Modifier.size(200.dp).background(Color.Red))
                                        }
                                }
                            }
                        }
                    }
                }
            }
        }
        waitForIdle()
        mainClock.autoAdvance = false
        runOnIdle { input.backStarted(NavigationEvent(progress = 0f)) }
        mainClock.advanceTimeBy(64)
        runOnIdle { input.backProgressed(NavigationEvent(progress = 0.6f)) }
        mainClock.advanceTimeBy(64)
        runOnIdle {
            assertEquals(1, host.entries.size)
            visible = false
            composed = false
        }
        mainClock.advanceTimeBy(2000)
        runOnIdle {
            assertTrue(
                host.entries.isEmpty(),
                "An owner that leaves mid-gesture must not strand its entry: ${host.entries.size}",
            )
        }
        dispatcher.dispose()
    }

    @Test fun sheetBackCollapsesBeforeDismissing() = checkSheetAction(shared = false, veto = false)

    @Test
    fun sharedSheetBackCollapsesBeforeDismissing() = checkSheetAction(shared = true, veto = false)

    @Test fun sheetVetoRestoresPreview() = checkSheetAction(shared = false, veto = true)

    @Test fun sharedSheetVetoRestoresPreview() = checkSheetAction(shared = true, veto = true)

    private fun checkSheetAction(shared: Boolean, veto: Boolean) = runInitializedComposeUiTest {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val owner =
            object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher = dispatcher
            }
        lateinit var sheet: OverlayBottomSheetState
        lateinit var scope: CoroutineScope
        var dismissals = 0
        setContent {
            ProvideM3xMaterialRendering {
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                    MaterialTheme(motionScheme = TestMotion) {
                        OverlayHost(remember { OverlayHostState() }) {
                            localSharedTransitionScope {
                                sheet =
                                    rememberOverlayBottomSheetState(
                                        skipPartiallyExpanded = veto,
                                        confirmValueChange = {
                                            !veto || it != OverlayBottomSheetValue.Hidden
                                        },
                                    )
                                scope = rememberCoroutineScope()
                                val endpoint = rememberSharedContentState("sheet").takeIf { shared }
                                if (endpoint != null)
                                    BottomSheetSharedAnchor(endpoint, sheet) {
                                        Box(Modifier.requiredSize(40.dp))
                                    }
                                OverlayBottomSheet(
                                    onDismissRequest = { dismissals++ },
                                    sheetState = sheet,
                                    sharedContentState = endpoint,
                                    effects = backdropEffects {},
                                    dragHandle = null,
                                    contentWindowInsets = { WindowInsets(0) },
                                ) {
                                    Box(Modifier.fillMaxSize().background(Color.Red))
                                }
                            }
                        }
                    }
                }
            }
        }
        waitForIdle()
        runOnIdle { scope.launch { sheet.expand() } }
        waitForIdle()
        mainClock.autoAdvance = false
        runOnIdle { input.backStarted(NavigationEvent()) }
        mainClock.advanceTimeBy(64)
        runOnIdle { input.backProgressed(NavigationEvent(progress = 0.6f)) }
        mainClock.advanceTimeBy(64)
        runOnIdle {
            assertEquals(OverlayBottomSheetValue.Expanded, sheet.currentValue)
            assertEquals(0, dismissals)
        }
        runOnIdle { input.backCompleted() }
        mainClock.advanceTimeBy(2000)
        runOnIdle {
            assertEquals(
                if (veto) OverlayBottomSheetValue.Expanded
                else OverlayBottomSheetValue.PartiallyExpanded,
                sheet.currentValue,
            )
            assertEquals(0, dismissals)
            assertEquals(false, sheet.isBackGestureInProgress)
        }
        dispatcher.dispose()
    }

    @Test fun aNewOverlayDoesNotStealAnInProgressGesture() = checkGestureOwner(OwnerExit.None)

    @Test fun aHiddenOwnerDoesNotReceiveGestureCompletion() = checkGestureOwner(OwnerExit.Hidden)

    @Test fun aRemovedOwnerDoesNotReceiveGestureCompletion() = checkGestureOwner(OwnerExit.Removed)

    private fun checkGestureOwner(exit: OwnerExit) = runInitializedComposeUiTest {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val owner =
            object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher = dispatcher
            }
        var menuVisible by mutableStateOf(false)
        var dialogVisible by mutableStateOf(true)
        var dialogComposed by mutableStateOf(true)
        var dialogDismissals = 0
        var menuDismissals = 0
        setContent {
            ProvideM3xMaterialRendering {
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                    MaterialTheme {
                        OverlayHost(remember { OverlayHostState() }) {
                            if (dialogComposed)
                                OverlayDialog(
                                    onDismissRequest = { dialogDismissals++ },
                                    visible = { dialogVisible },
                                ) {
                                    Box(Modifier.size(100.dp))
                                }
                            OverlayMenu(
                                expanded = { menuVisible },
                                onDismissRequest = { menuDismissals++ },
                            ) {
                                Box(Modifier.size(100.dp))
                            }
                        }
                    }
                }
            }
        }
        waitForIdle()
        runOnIdle { input.backStarted(NavigationEvent()) }
        runOnIdle { input.backProgressed(NavigationEvent(progress = 0.5f)) }
        runOnIdle {
            if (exit == OwnerExit.Hidden) dialogVisible = false
            if (exit == OwnerExit.Removed) dialogComposed = false
            menuVisible = true
        }
        waitForIdle()
        runOnIdle { input.backCompleted() }
        waitForIdle()
        runOnIdle {
            assertEquals(if (exit == OwnerExit.None) 1 else 0, dialogDismissals)
            assertEquals(0, menuDismissals)
        }
        runOnIdle { input.backCompleted() }
        runOnIdle { assertEquals(1, menuDismissals) }
        dispatcher.dispose()
    }

    private fun difference(first: PixelMap, second: PixelMap): Float {
        var total = 0f
        var count = 0
        for (y in 0 until first.height step 4) for (x in 0 until first.width step 4) {
            val a = first[x, y]
            val b = second[x, y]
            total += abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue)
            count += 3
        }
        return total / count
    }

    private enum class OwnerExit {
        None,
        Hidden,
        Removed,
    }

    private enum class Panel {
        Dialog,
        Menu,
        Sheet,
        Tooltip,
    }

    private object TestMotion : MotionScheme {
        override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = spec()

        override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = spec()

        override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = spec()

        override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = spec()

        override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = spec()

        override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = spec()

        private fun <T> spec(): FiniteAnimationSpec<T> = tween(1000, easing = LinearEasing)
    }
}
