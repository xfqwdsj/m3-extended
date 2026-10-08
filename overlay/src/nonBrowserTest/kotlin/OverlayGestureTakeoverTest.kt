package top.ltfan.m3x.overlay

import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering

class OverlayGestureTakeoverTest {
    @Test fun semanticChangeDuringCancelledBackAllowsAnotherGesture() = checkTakeover(false)

    @Test fun semanticChangeDuringCompletedBackAllowsAnotherGesture() = checkTakeover(true)

    @Test
    fun sameTargetReopenDuringCompletedBackAllowsAnotherGesture() = checkTakeover(true, "first")

    @Test fun completedExitConsumesAnotherGestureUntilItSettles() = checkTakeover(true, null)

    private fun checkTakeover(completed: Boolean, nextTarget: String? = "second") =
        runInitializedComposeUiTest {
            val dispatcher = NavigationEventDispatcher()
            val input = DirectNavigationEventInput()
            dispatcher.addInput(input)
            val owner =
                object : NavigationEventDispatcherOwner {
                    override val navigationEventDispatcher = dispatcher
                }
            val state = SeekableTransitionState<String?>(null)
            var target: String? by mutableStateOf(null)
            val entry =
                OverlayHandle().apply {
                    backHandler =
                        OverlayBackHandler(enabled = { true }, onCommit = { target = null })
                }
            setContent {
                ProvideM3xMaterialRendering {
                    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                        val root = rememberTransition(state)
                        root.animateFloat(transitionSpec = { tween(10000) }) {
                            when (it) {
                                "first" -> 0f
                                "second" -> 100f
                                else -> 200f
                            }
                        }
                        OverlayTimeline(state, root, target, null, entry)
                        OverlayHostBackHandler(entry)
                    }
                }
            }
            waitForIdle()
            mainClock.autoAdvance = false
            runOnIdle { target = "first" }
            mainClock.advanceTimeBy(12000)
            runOnIdle { input.backStarted(NavigationEvent(progress = 0f)) }
            mainClock.advanceTimeBy(64)
            runOnIdle { input.backProgressed(NavigationEvent(progress = .5f)) }
            mainClock.advanceTimeBy(64)
            runOnIdle {
                assertEquals(.5f, state.fraction)
                if (completed) input.backCompleted() else input.backCancelled()
            }
            mainClock.advanceTimeBy(48)
            runOnIdle { target = nextTarget }
            mainClock.advanceTimeBy(64)
            if (nextTarget == null) {
                runOnIdle {
                    assertEquals(OverlayBackPhase.Completed, entry.backPhase)
                    input.backStarted(NavigationEvent(progress = 0f))
                    input.backProgressed(NavigationEvent(progress = 0.3f))
                }
                mainClock.advanceTimeBy(64)
                runOnIdle {
                    assertEquals(OverlayBackPhase.Completed, entry.backPhase)
                    input.backCompleted()
                    assertEquals(null, target)
                }
                mainClock.advanceTimeBy(12000)
                dispatcher.dispose()
                return@runInitializedComposeUiTest
            }
            runOnIdle {
                assertNotEquals(
                    state.currentState,
                    state.targetState,
                    "The replacement animation must still be running",
                )
                assertEquals(
                    OverlayBackPhase.Idle,
                    entry.backPhase,
                    "New semantic intent must release the old terminal gesture before its animation settles",
                )
                input.backStarted(NavigationEvent(progress = 0f))
            }
            mainClock.advanceTimeBy(64)
            runOnIdle { input.backProgressed(NavigationEvent(progress = .3f)) }
            mainClock.advanceTimeBy(64)
            runOnIdle {
                assertEquals(OverlayBackPhase.Progress, entry.backPhase)
                assertEquals(.3f, state.fraction)
                assertEquals(null, state.targetState)
                input.backCancelled()
            }
            mainClock.advanceTimeBy(12000)
            dispatcher.dispose()
        }
}
