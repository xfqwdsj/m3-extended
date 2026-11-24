package top.ltfan.m3x.compose.animation

import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Rewinds the existing exit segment, preserving every child's original animation specification. */
public suspend fun <T> rewindTransition(
    state: SeekableTransitionState<T>,
    transition: Transition<T>,
) {
    val restore = state.currentState
    val duration = (state.fraction * transition.totalDurationNanos / 1_000_000).toInt()
    coroutineScope {
        animate(state.fraction, 0f, animationSpec = tween(duration)) { value, _ ->
            launch { state.seekTo(value) }
        }
    }
    state.snapTo(restore)
}
