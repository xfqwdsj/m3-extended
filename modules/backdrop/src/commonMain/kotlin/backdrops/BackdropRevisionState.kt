package top.ltfan.m3x.backdrop.backdrops

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.util.fastAny

/** Invalidates surfaces that sample a recorded layer. */
internal class BackdropRevisionState : State<Long> {
    private val state = mutableLongStateOf(0L)

    override val value: Long
        get() = state.longValue

    init {
        BackdropRevisionStates.register(state)
    }

    fun increment() {
        state.longValue++
    }

    fun dispose() {
        BackdropRevisionStates.unregister(state)
    }
}

/**
 * Distinguishes layer redraw notifications from application state that can change captured content.
 * A capture node ignores these notifications when deciding whether to publish again.
 */
internal object BackdropRevisionStates {
    private val states = mutableListOf<Any>()

    fun register(state: Any) {
        states += state
    }

    fun unregister(state: Any) {
        states.removeAll { it === state }
    }

    fun contains(state: Any): Boolean = states.fastAny { it === state }
}
