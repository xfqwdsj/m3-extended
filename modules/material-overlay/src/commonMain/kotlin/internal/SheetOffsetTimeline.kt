package top.ltfan.m3x.material.overlay.internal

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.snap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import top.ltfan.m3x.compose.animation.rewindTransition

/** The native offset animation shared by programmatic movement and predictive back. */
internal class SheetOffsetTimeline {
    val state = SeekableTransitionState(0f)
    var spec: FiniteAnimationSpec<Float> by mutableStateOf(snap())
    lateinit var transition: Transition<Float>
    lateinit var value: State<Float>

    @Composable
    fun Bind() {
        transition = rememberTransition(state, label = "SheetOffset")
        value =
            transition.animateFloat(transitionSpec = { spec }, label = "SheetOffsetValue") { it }
    }

    suspend fun prepare(offset: Float, animationSpec: FiniteAnimationSpec<Float>) {
        spec = animationSpec
        state.snapTo(offset)
    }

    suspend fun animateTo(target: Float, write: (Float) -> Unit) {
        follow(write) { state.animateTo(target) }
    }

    suspend fun seekTo(fraction: Float, target: Float, write: (Float) -> Unit) {
        state.seekTo(fraction, target)
        write(value.value)
    }

    suspend fun restore(write: (Float) -> Unit) {
        follow(write) { rewindTransition(state, transition) }
    }

    private suspend fun follow(write: (Float) -> Unit, block: suspend () -> Unit) {
        coroutineScope {
            val frames = launch { snapshotFlow { value.value }.collect { write(it) } }
            try {
                block()
                write(value.value)
            } finally {
                frames.cancel()
            }
        }
    }
}
