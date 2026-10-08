package top.ltfan.m3x.datastore

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

/**
 * An in-memory store backend for tests: it can emit disk values at will and suspend updates until
 * released.
 */
class TestBackend<T> {
    val state = MutableStateFlow<T?>(null)

    /** All values written through [Store.update], in order. */
    val updates = mutableListOf<T>()

    private var gate: CompletableDeferred<Unit>? = null
    private var nextFailure: Throwable? = null

    fun failNextUpdate(cause: Throwable) {
        nextFailure = cause
    }

    /** Emits [value] as if it was read from the disk. */
    fun emit(value: T) {
        state.value = value
    }

    /** Makes subsequent updates suspend until [release]. */
    fun block() {
        gate = CompletableDeferred()
    }

    /** Releases the updates blocked by [block]. */
    fun release() {
        gate?.complete(Unit)
        gate = null
    }

    suspend fun update(transform: suspend (T) -> T): T {
        gate?.await()
        nextFailure?.let {
            nextFailure = null
            throw it
        }
        val next = transform(requireNotNull(state.value) { "No value emitted yet" })
        updates += next
        state.value = next
        return next
    }

    /** A plain [Store] over this backend. */
    val store: Store<T>
        get() =
            object : Store<T> {
                override val data: Flow<T> = state.filterNotNull()

                override suspend fun update(transform: suspend (T) -> T): T =
                    this@TestBackend.update(transform)
            }

    /** A [FlowStore] over this backend. */
    val flowStore: FlowStore<T>
        get() = FlowStore(state.filterNotNull()) { update(it) }
}
