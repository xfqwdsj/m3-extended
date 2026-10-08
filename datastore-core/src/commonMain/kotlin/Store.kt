package top.ltfan.m3x.datastore

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow

/** A small store contract consumed by [MutableStore]. */
public interface Store<T> {
    /** The current value and all subsequent values of this store. */
    public val data: Flow<T>

    /**
     * Updates the stored value atomically.
     *
     * @param transform takes the current value and returns the new value.
     * @return the new value.
     */
    public suspend fun update(transform: suspend (T) -> T): T
}

/** A [Store] backed by an [DataStore]. */
public class DataStoreStore<T>(private val delegate: DataStore<T>) : Store<T> {
    override val data: Flow<T>
        get() = delegate.data

    override suspend fun update(transform: suspend (T) -> T): T = delegate.updateData(transform)
}

/**
 * A [Store] backed by a [Flow] and a writer.
 *
 * Useful for controlled integration points and tests. The supported persistence foundation of this
 * library remains AndroidX DataStore.
 *
 * Example:
 * ```
 * val store: Store<UiSettingsModel> = FlowStore(
 *     data = dao.settingsFlow,
 *     writer = { dao.save(it) },
 * )
 * ```
 */
public class FlowStore<T>(
    override val data: Flow<T>,
    private val writer: suspend (transform: suspend (T) -> T) -> T,
) : Store<T> {
    override suspend fun update(transform: suspend (T) -> T): T = writer(transform)
}
