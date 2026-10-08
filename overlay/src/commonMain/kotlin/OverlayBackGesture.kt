package top.ltfan.m3x.overlay

import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import top.ltfan.m3x.compose.animation.rewindTransition

/** The back event currently owning a presentation's timeline. */
internal enum class OverlayBackPhase {
    Idle,
    Progress,
    Cancelled,
    Completed,
}

/** Drives an entry without a caller-owned animation root. */
@Composable
internal fun OverlayBackTimeline(entry: OverlayHandle, transition: Transition<Boolean>) {
    OverlayTimeline(entry.visibleState, transition, entry.visibleIntent, false, entry)
}

/** One writer for both semantic targets and speculative back targets. */
@Composable
internal fun <T> OverlayTimeline(
    state: SeekableTransitionState<T>,
    transition: Transition<T>,
    target: T,
    backTarget: T,
    entry: OverlayHandle?,
) {
    val currentTarget by rememberUpdatedState(target)
    val currentBackTarget by rememberUpdatedState(backTarget)
    LaunchedEffect(state, transition, entry) {
        var previousTarget = currentTarget
        try {
            snapshotFlow { Triple(currentTarget, entry?.backPhase, entry?.backSession) }
                .collectLatest { (intent, phase, _) ->
                    // An owner request supersedes a speculative gesture, including an exit settle.
                    val changed = previousTarget != intent
                    previousTarget = intent
                    if (
                        changed &&
                            phase != OverlayBackPhase.Idle &&
                            (phase != OverlayBackPhase.Completed || intent != currentBackTarget)
                    ) {
                        entry?.resolveBackGesture()
                    }
                    when {
                        phase == OverlayBackPhase.Progress &&
                            !changed &&
                            entry?.backHandler?.previewTarget?.invoke() == false -> {
                            snapshotFlow { entry.backProgress }
                                .filterNotNull()
                                .collectLatest {
                                    state.seekTo(it.coerceIn(0f, 1f), currentBackTarget)
                                }
                        }
                        (phase == OverlayBackPhase.Cancelled ||
                            phase == OverlayBackPhase.Completed && intent != currentBackTarget) &&
                            !changed -> {
                            if (state.currentState == intent) rewindTransition(state, transition)
                            else state.animateTo(intent)
                            entry?.resolveBackGesture()
                        }
                        else -> {
                            // animateTo also resumes a canceled animation whose target already
                            // matches.
                            state.animateTo(intent)
                            if (
                                phase == OverlayBackPhase.Completed ||
                                    phase == OverlayBackPhase.Cancelled
                            ) {
                                entry?.resolveBackGesture()
                            }
                        }
                    }
                }
        } finally {
            entry?.resolveBackGesture()
        }
    }
}

/** Releases speculative control so the latest semantic intent drives the root again. */
internal fun OverlayHandle.resolveBackGesture() {
    backProgress = null
    backPhase = OverlayBackPhase.Idle
}
