package top.ltfan.m3x.datastore

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

/**
 * An optimistic two-way state over a [Store].
 *
 * Three semantics:
 * 1. Writes made before the first value of [Store.data] is read wait in the queue;
 * 2. pending transforms are composed in order and persisted against the latest backend value;
 * 3. disk emissions cannot replace a value while local transforms remain pending, including writes
 *    issued before the first disk value.
 *
 * [cells] supplies the read observation mechanism. A plain cell has no UI subscription; a Compose
 * cell subscribes reads to snapshot invalidations. [data] exposes the visible value as a
 * [StateFlow].
 */
public open class MutableStore<T>(
    /** The backing store. */
    public val store: Store<T>,
    scope: CoroutineScope,
    initialValue: T,
    cells: ValueCellFactory,
) : ReadWriteProperty<Any?, T> {
    private class WriteRequest<T>(val transform: (T) -> T)

    private class PendingState<T>(val value: T, val pending: List<WriteRequest<T>>)

    private val visible = cells.create(initialValue)
    private val owner = scope.coroutineContext[Job]
    private val mutableData = MutableStateFlow(initialValue)
    private val latest = MutableStateFlow(PendingState(initialValue, emptyList<WriteRequest<T>>()))
    private val writes = Channel<Unit>(Channel.CONFLATED)
    private val retries = Channel<Unit>(Channel.CONFLATED)
    private val initialized = CompletableDeferred<Unit>()
    private val mutableStatus = MutableStateFlow<StoreStatus>(StoreStatus.Loading)

    /** The observable state, equal to [value]. */
    public val data: StateFlow<T> = mutableData.asStateFlow()
    public val status: StateFlow<StoreStatus> = mutableStatus.asStateFlow()

    /** The current value. Writing it is equivalent to [write]. */
    public var value: T
        get() {
            visible.value // Subscribe observing readers to invalidations.
            return latest.value.value
        }
        set(value) {
            write(value)
        }

    init {
        scope.launch {
            try {
                store.data.collect { diskValue ->
                    onDiskValue(diskValue)
                    if (initialized.complete(Unit)) mutableStatus.value = StoreStatus.Ready
                }
            } catch (failure: Throwable) {
                if (failure is CancellationException) throw failure
                mutableStatus.value = StoreStatus.ReadFailed(failure)
                initialized.completeExceptionally(failure)
            }
        }
        scope.launch {
            try {
                initialized.await()
            } catch (failure: Throwable) {
                if (failure is CancellationException) throw failure
                return@launch
            }
            while (true) {
                writes.receive()
                while (latest.value.pending.isNotEmpty()) {
                    val batch = latest.value.pending
                    var saved = false
                    while (!saved) {
                        try {
                            val persisted = store.update { current ->
                                batch.fold(current) { value, request -> request.transform(value) }
                            }
                            latest.updateAndGet { current ->
                                val remaining = current.pending.drop(batch.size)
                                val rebased =
                                    remaining.fold(persisted) { value, request ->
                                        request.transform(value)
                                    }
                                PendingState(rebased, remaining)
                            }
                            refreshVisibleState()
                            mutableStatus.value = StoreStatus.Ready
                            saved = true
                        } catch (failure: Throwable) {
                            if (failure is CancellationException) throw failure
                            mutableStatus.value = StoreStatus.WriteFailed(failure)
                            retries.receive()
                        }
                    }
                }
            }
        }
    }

    /**
     * Suspends until the first value of [store] has been read.
     *
     * Useful to keep a first-frame placeholder (e.g. a splash screen) visible until [value] is
     * backed by the persisted value instead of `initialValue`.
     */
    public suspend fun awaitInitialized() {
        initialized.await()
    }

    /** Immediately updates visible state and queues an atomic backend transform. */
    public fun write(value: T): T = update { value }

    /**
     * Applies a pure transformation and queues it for persistence. [transform] may run repeatedly
     * during CAS retries, disk writes and rebasing.
     */
    public fun update(transform: (T) -> T): T {
        check(owner?.isActive != false) { "Cannot update a closed store" }
        check(mutableStatus.value !is StoreStatus.ReadFailed) {
            "Cannot update a store whose initial read failed"
        }
        // The transform may be evaluated more than once when callers race.
        val updated = latest.updateAndGet {
            PendingState(transform(it.value), it.pending + WriteRequest(transform))
        }
        refreshVisibleState()
        writes.trySend(Unit)
        return updated.value
    }

    /** Retries the latest local value after a write failure. */
    public fun retrySave() {
        if (mutableStatus.value is StoreStatus.WriteFailed) retries.trySend(Unit)
    }

    /**
     * Returns a [ReadWriteProperty] that reads `get(value)` and writes back `set(value, newValue)`.
     * An escape hatch for fields outside the generated delegates.
     */
    public fun <R> property(get: (T) -> R, set: (T, R) -> T): ReadWriteProperty<Any?, R> =
        object : ReadWriteProperty<Any?, R> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): R = get(value)

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: R) {
                update { current -> set(current, value) }
            }
        }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        write(value)
    }

    private fun onDiskValue(diskValue: T) {
        latest.updateAndGet {
            if (it.pending.isNotEmpty()) it else PendingState(diskValue, it.pending)
        }
        refreshVisibleState()
    }

    private fun refreshVisibleState() {
        visible.value = latest.value.value
        mutableData.updateAndGet { latest.value.value }
    }
}
