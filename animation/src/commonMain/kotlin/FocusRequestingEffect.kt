package top.ltfan.m3x.compose.animation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester

/**
 * Requests focus for the given [focusRequester] once the enclosing AnimatedVisibility transition
 * has become visible.
 *
 * Usage: Call this inside the content of an AnimatedVisibility so that when the enter animation
 * completes (i.e. when both `transition.currentState` and `transition.targetState` are
 * [EnterExitState.Visible]), focus is requested for the associated focusable component.
 *
 * Focus is requested once per visible cycle: the request runs when the enter completes, and runs
 * again when an exit that never left the composition returns to visible (an interrupted exit). An
 * exit in progress, and recompositions that do not change the transition state, do not request
 * focus.
 *
 * @param focusRequester the [FocusRequester] used to request focus when visible.
 */
@Composable
public fun AnimatedVisibilityScope.FocusRequestingEffect(focusRequester: FocusRequester) {
    // Both transition states key the effect so a completed entrance and an interrupted exit
    // returning to Visible each request focus once.
    LaunchedEffect(transition.currentState, transition.targetState) {
        if (
            transition.currentState == EnterExitState.Visible &&
                transition.targetState == EnterExitState.Visible
        ) {
            focusRequester.requestFocus()
        }
    }
}
