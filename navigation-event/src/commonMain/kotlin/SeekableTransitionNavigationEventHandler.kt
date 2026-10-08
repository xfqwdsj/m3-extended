package top.ltfan.m3x.compose.navigationevent

import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationEventHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.launch

/**
 * Handles predictive back navigation gestures to update the [SeekableTransitionState].
 *
 * The gesture progress is seeked as it moves, an unused gesture animates the transition back to its
 * current state, and a completed gesture snaps to [backTarget].
 *
 * @param transitionState the [SeekableTransitionState] to update.
 * @param backTarget the target state when the gesture is completed.
 * @see NavigationEventHandler
 */
@Composable
public fun <T> SeekableTransitionNavigationEventHandler(
    transitionState: SeekableTransitionState<T>,
    backTarget: T,
) {
    val state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    val scope = rememberCoroutineScope()

    NavigationEventHandler(
        state = state,
        onBackCancelled = {
            scope.launch { transitionState.animateTo(transitionState.currentState) }
        },
        onBackCompleted = {
            scope.launch { transitionState.snapTo(backTarget) }
        },
    )

    LaunchedEffect(state, transitionState, backTarget) {
        snapshotFlow { state.transitionState }
            .collect { transition ->
                if (transition is NavigationEventTransitionState.InProgress) {
                    transitionState.seekTo(transition.latestEvent.progress, backTarget)
                }
            }
    }
}
