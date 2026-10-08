package top.ltfan.m3x.overlay

import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.overlay.OverlayAlertDialog
import top.ltfan.m3x.material.overlay.OverlayBasicAlertDialog
import top.ltfan.m3x.material.overlay.OverlayBottomSheet
import top.ltfan.m3x.material.overlay.OverlayBottomSheetState
import top.ltfan.m3x.material.overlay.OverlayBottomSheetValue
import top.ltfan.m3x.material.overlay.OverlayDialog
import top.ltfan.m3x.material.overlay.OverlayDialogProperties
import top.ltfan.m3x.material.overlay.SharedTransitionAnchor
import top.ltfan.m3x.material.overlay.rememberOverlayBottomSheetState

class OverlayScrimAnimationTest {
    @Test fun dialogScrimTracksVisibleState() = checkVisibleModes(dialogRenderer())

    @Test
    fun basicAlertDialogScrimTracksVisibleState() = checkVisibleModes(basicAlertDialogRenderer())

    @Test fun alertDialogScrimTracksVisibleState() = checkVisibleModes(alertDialogRenderer())

    @Test
    fun sharedBottomSheetScrimTracksVisibleState() =
        checkVisibleTransition(bottomSheetRenderer(), shared = true)

    @Test fun dialogScrimCompletesWhenOwnerLeaves() = checkOwnerRemovalModes(dialogRenderer())

    @Test
    fun basicAlertDialogScrimCompletesWhenOwnerLeaves() =
        checkOwnerRemovalModes(basicAlertDialogRenderer())

    @Test
    fun alertDialogScrimCompletesWhenOwnerLeaves() = checkOwnerRemovalModes(alertDialogRenderer())

    @Test
    fun sharedBottomSheetScrimCompletesWhenOwnerLeaves() =
        checkOwnerRemoval(bottomSheetRenderer(), shared = true)

    @Test
    fun ordinaryBottomSheetScrimTracksOffsetForEveryExitPath() {
        for (skipPartiallyExpanded in listOf(true, false)) {
            for (exit in SheetExit.entries) {
                checkOrdinarySheetScrimTracksOffset(skipPartiallyExpanded, exit)
            }
        }
    }

    @Test
    fun disabledDialogTransitionChangesScrimImmediately() = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        var visible by mutableStateOf(false)
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(motionScheme = TestMotionScheme) {
                    scene {
                        OverlayHost(remember { OverlayHostState() }) {
                            OverlayDialog(
                                onDismissRequest = {},
                                visible = { visible },
                                properties = dialogProperties(animateTransition = false),
                            ) {}
                        }
                    }
                }
            }
        }
        assertNear(1f, sampleRed(), 0.02f)
        runOnIdle { visible = true }
        mainClock.advanceTimeBy(48)
        val shown = sampleRed()
        assertNear(0f, shown, 0.02f)
        mainClock.advanceTimeBy(160)
        assertNear(shown, sampleRed(), 0.02f)
        runOnIdle { visible = false }
        mainClock.advanceTimeBy(48)
        assertNear(1f, sampleRed(), 0.02f)
    }

    @Test
    fun scrimSurvivesSpatialSettlementAndFadesOnEveryDismissal() {
        for (shared in listOf(false, true)) for (removeOwner in listOf(false, true)) {
            runInitializedComposeUiTest {
                mainClock.autoAdvance = false
                var shown by mutableStateOf(false)
                val motion =
                    object : MotionScheme by TestMotionScheme {
                        override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = tween(600)

                        override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = tween(600)

                        override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = tween(600)

                        override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = tween(160)

                        override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = tween(160)

                        override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = tween(160)
                    }
                setContent {
                    ProvideM3xMaterialRendering {
                        MaterialTheme(motionScheme = motion) {
                            scene {
                                OverlayHost(remember { OverlayHostState() }) {
                                    localSharedTransitionScope {
                                        val state =
                                            if (shared) rememberSharedContentState("settlement")
                                            else null
                                        if (state != null)
                                            SharedTransitionAnchor(
                                                state,
                                                !shown,
                                                Modifier.size(48.dp),
                                            )
                                        if (!removeOwner || shown) {
                                            dialogRenderer().render({ shown }, {}, state)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                repeat(2) {
                    runOnIdle { shown = true }
                    mainClock.advanceTimeBy(1600)
                    assertNear(0f, sampleRed(), 0.02f)
                    mainClock.advanceTimeBy(160)
                    assertNear(0f, sampleRed(), 0.02f)
                    runOnIdle { shown = false }
                    val frames = mutableListOf<Float>()
                    repeat(60) { frame ->
                        mainClock.advanceTimeBy(16)
                        frames += sampleRed()
                        if (frame == 0) assertNear(0f, frames.last(), 0.02f)
                    }
                    assertTrue(frames.any { it in 0.1f..0.85f }, "Scrim must fade: $frames")
                    assertNear(1f, frames.last(), 0.02f)
                }
            }
        }
    }

    private fun checkVisibleModes(render: OverlayRenderer) {
        checkVisibleTransition(render, shared = false)
        checkVisibleTransition(render, shared = true)
    }

    private fun checkOwnerRemovalModes(render: OverlayRenderer) {
        checkOwnerRemoval(render, shared = false)
        checkOwnerRemoval(render, shared = true)
    }

    private fun checkVisibleTransition(render: OverlayRenderer, shared: Boolean) =
        runInitializedComposeUiTest {
            mainClock.autoAdvance = false
            var visible by mutableStateOf(false)
            setContent {
                ProvideM3xMaterialRendering {
                    MaterialTheme(motionScheme = TestMotionScheme) {
                        scene {
                            OverlayHost(remember { OverlayHostState() }) {
                                localSharedTransitionScope {
                                    val sharedState =
                                        if (shared) rememberSharedContentState("overlay-panel")
                                        else null
                                    if (shared && sharedState != null) {
                                        SharedTransitionAnchor(
                                            sharedContentState = sharedState,
                                            visible = !visible,
                                            modifier = Modifier.size(48.dp),
                                        )
                                    }
                                    render.render({ visible }, {}, sharedState)
                                }
                            }
                        }
                    }
                }
            }

            assertNear(1f, sampleRed(), 0.02f)
            runOnIdle { visible = true }
            advanceToFirstScrimFrame()
            val earlyEnter = sampleRed()
            assertTrue(earlyEnter < 0.98f, "Scrim did not begin to appear: red=$earlyEnter")
            mainClock.advanceTimeBy(144)
            assertNear(0.5f, sampleRed(), 0.08f)
            mainClock.advanceTimeBy(208)
            val open = sampleRed()
            assertNear(0f, open, 0.02f)

            runOnIdle { visible = false }
            advanceToFirstScrimFrame()
            assertScrimExit()

            runOnIdle { visible = true }
            advanceToFirstScrimFrame()
            mainClock.advanceTimeBy(144)
            assertNear(0.5f, sampleRed(), 0.08f)
            mainClock.advanceTimeBy(208)
            assertNear(open, sampleRed(), 0.02f)
        }

    private fun checkOwnerRemoval(render: OverlayRenderer, shared: Boolean) =
        runInitializedComposeUiTest {
            mainClock.autoAdvance = false
            var attached by mutableStateOf(true)
            val hostState = OverlayHostState()
            setContent {
                ProvideM3xMaterialRendering {
                    MaterialTheme(motionScheme = TestMotionScheme) {
                        scene {
                            OverlayHost(hostState) {
                                localSharedTransitionScope {
                                    val sharedState =
                                        if (shared) rememberSharedContentState("overlay-panel")
                                        else null
                                    if (shared && sharedState != null) {
                                        SharedTransitionAnchor(
                                            sharedContentState = sharedState,
                                            visible = !attached,
                                            modifier = Modifier.size(48.dp),
                                        )
                                    }
                                    if (attached) render.render({ true }, {}, sharedState)
                                }
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(400)
            assertNear(0f, sampleRed(), 0.02f)

            runOnIdle { attached = false }
            mainClock.advanceTimeUntil(timeoutMillis = 1600) {
                hostState.entries.singleOrNull()?.visibleState?.targetState == false
            }
            val entry = hostState.entries.singleOrNull()
            assertNotNull(entry, "The hosted entry must remain until its exit completes")
            assertTrue(
                entry.visibleState.currentState != entry.visibleState.targetState,
                "Expected the hosted entry exit to be running",
            )
            advanceToFirstScrimFrame()
            assertScrimExit()
        }

    private fun checkOrdinarySheetScrimTracksOffset(
        skipPartiallyExpanded: Boolean,
        exit: SheetExit,
    ) = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        var visible by mutableStateOf(true)
        var attached by mutableStateOf(true)
        val hostState = OverlayHostState()
        lateinit var sheetState: OverlayBottomSheetState
        lateinit var scope: CoroutineScope
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(motionScheme = TestMotionScheme) {
                    scene {
                        OverlayHost(hostState) {
                            if (attached) {
                                val rememberedState =
                                    rememberOverlayBottomSheetState(
                                        skipPartiallyExpanded = skipPartiallyExpanded,
                                    )
                                sheetState = rememberedState
                                scope = rememberCoroutineScope()
                                OverlayBottomSheet(
                                    onDismissRequest = {},
                                    sheetState = rememberedState,
                                    visible =
                                        if (exit == SheetExit.VisibleFalse) ({ visible }) else null,
                                    scrimColor = Color.Black,
                                    dragHandle = null,
                                ) {
                                    Box(Modifier.fillMaxWidth().height(600.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(1000)
        runOnIdle {
            assertTrue(sheetState.isVisible)
            if (!skipPartiallyExpanded) assertTrue(sheetState.hasPartiallyExpandedState)
        }
        if (!skipPartiallyExpanded && exit == SheetExit.VisibleFalse) {
            assertNear(0f, sampleRed(), 0.02f)
            runOnIdle { scope.launch { sheetState.expand() } }
            repeat(20) {
                mainClock.advanceTimeBy(16)
                assertNear(0f, sampleRed(), 0.02f)
            }
            runOnIdle { scope.launch { sheetState.partialExpand() } }
            repeat(20) {
                mainClock.advanceTimeBy(16)
                assertNear(0f, sampleRed(), 0.02f)
            }
        }
        val hiddenOffset = sheetState.anchorOffset(OverlayBottomSheetValue.Hidden)
        val shownOffset = sheetState.requireOffset()
        assertTrue(
            hiddenOffset > shownOffset,
            "Sheet anchors are not ordered: $shownOffset..$hiddenOffset",
        )
        assertNear(0f, sampleRed(), 0.02f)

        runOnIdle {
            when (exit) {
                SheetExit.VisibleFalse -> visible = false
                SheetExit.StateHide -> scope.launch { sheetState.hide() }
                SheetExit.OwnerRemoval -> attached = false
            }
        }
        var sawIntermediateFrame = false
        for (frame in 0 until 80) {
            mainClock.advanceTimeBy(16)
            val expectedRed =
                ((sheetState.requireOffset() - shownOffset) / (hiddenOffset - shownOffset))
                    .coerceIn(0f, 1f)
            val actualRed = sampleRed()
            assertNear(expectedRed, actualRed, 0.03f)
            if (expectedRed in 0.05f..0.95f) sawIntermediateFrame = true
            if (sheetState.currentValue == OverlayBottomSheetValue.Hidden && expectedRed >= 0.98f) {
                break
            }
        }
        assertTrue(sawIntermediateFrame, "No intermediate scrim frame for $exit")
        mainClock.advanceTimeBy(400)
        assertNear(1f, sampleRed(), 0.02f)
        assertTrue(!sheetState.isVisible, "The sheet did not reach Hidden for $exit")
        if (exit == SheetExit.OwnerRemoval) {
            mainClock.advanceTimeBy(400)
            assertTrue(hostState.entries.isEmpty(), "The removed sheet entry remained hosted")
        } else {
            assertTrue(!hostState.isModal, "A hidden sheet still blocks its host")
        }
    }

    private enum class SheetExit {
        VisibleFalse,
        StateHide,
        OwnerRemoval,
    }

    private fun ComposeUiTest.advanceToFirstScrimFrame() {
        // The host first recomposes the target, then starts the transition on the next frame.
        mainClock.advanceTimeBy(32)
        mainClock.advanceTimeBy(16)
    }

    private fun ComposeUiTest.assertScrimExit() {
        val earlyExit = sampleRed()
        assertTrue(earlyExit < 0.98f, "Scrim disappeared at the start of exit")
        mainClock.advanceTimeBy(144)
        assertNear(0.5f, sampleRed(), 0.08f)
        mainClock.advanceTimeBy(208)
        assertNear(1f, sampleRed(), 0.02f)
    }

    private fun ComposeUiTest.sampleRed(): Float =
        onNodeWithTag("scene", useUnmergedTree = true).captureToImage().toPixelMap().let {
            it[it.width - 4, 4].red
        }

    private fun assertNear(expected: Float, actual: Float, tolerance: Float) {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "Expected $actual to be within $tolerance of $expected",
        )
    }

    @Composable
    private fun scene(content: @Composable () -> Unit) {
        Box(Modifier.fillMaxSize().background(Color.White).testTag("scene")) { content() }
    }

    private fun dialogProperties(animateTransition: Boolean = true) =
        OverlayDialogProperties(
            usePlatformInsets = false,
            usePlatformDefaultWidth = false,
            scrimColor = Color.Black,
            animateTransition = animateTransition,
        )

    private fun dialogRenderer() = OverlayRenderer { visible, dismiss, shared ->
        OverlayDialog(
            onDismissRequest = dismiss,
            visible = visible,
            properties = dialogProperties(),
            sharedContentState = shared,
        ) {}
    }

    private fun basicAlertDialogRenderer() = OverlayRenderer { visible, dismiss, shared ->
        OverlayBasicAlertDialog(
            onDismissRequest = dismiss,
            visible = visible,
            properties = dialogProperties(),
            sharedContentState = shared,
        ) {}
    }

    private fun alertDialogRenderer() = OverlayRenderer { visible, dismiss, shared ->
        OverlayAlertDialog(
            onDismissRequest = dismiss,
            visible = visible,
            properties = dialogProperties(),
            confirmButton = {},
            sharedContentState = shared,
        )
    }

    private fun bottomSheetRenderer() = OverlayRenderer { visible, dismiss, shared ->
        OverlayBottomSheet(
            onDismissRequest = dismiss,
            visible = visible,
            scrimColor = Color.Black,
            dragHandle = null,
            sharedContentState = shared,
        ) {
            Box(Modifier.fillMaxWidth().height(160.dp))
        }
    }

    private fun interface OverlayRenderer {
        @Composable
        fun render(
            visible: () -> Boolean,
            dismiss: () -> Unit,
            sharedContentState: SharedContentState?,
        )
    }

    private object TestMotionScheme : MotionScheme {
        override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = spatialSpec()

        override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = spatialSpec()

        override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = spatialSpec()

        override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = effectSpec()

        override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = effectSpec()

        override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = effectSpec()

        private fun <T> effectSpec(): FiniteAnimationSpec<T> =
            tween(durationMillis = 320, easing = LinearEasing)

        private fun <T> spatialSpec(): FiniteAnimationSpec<T> =
            tween(durationMillis = 80, easing = LinearEasing)
    }
}
