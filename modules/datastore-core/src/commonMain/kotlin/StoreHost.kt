package top.ltfan.m3x.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The single entry point of a set of stores.
 *
 * Each unique [StoreDefinition.name] maps to exactly one [DataStore], so a single active
 * `DataStore` owns each file; repeated calls for the same definition return the same instance.
 * Reuse the same [StoreDefinition] instance for a name: a different definition with that name is
 * rejected with [IllegalArgumentException], even if its model type is the same.
 *
 * Creation is lazy and synchronized per name, so even concurrent calls share one `DataStore` /
 * [MutableStore] per [StoreDefinition.name].
 */
public class StoreHost(
    private val scope: CoroutineScope,
    private val file: StoreFile,
    private val cells: ValueCellFactory,
) {
    private val hostScope =
        CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))
    private val closed = MutableStateFlow(false)
    private val definitions = MutableStateFlow<Map<String, StoreDefinition<*>>>(emptyMap())
    private val dataStores = MutableStateFlow<Map<String, Lazy<DataStore<*>>>>(emptyMap())
    private val states = MutableStateFlow<Map<String, Lazy<MutableStore<*>>>>(emptyMap())

    /** Cancels all stores. Create a new host after an unrecoverable read failure. */
    public fun close() {
        if (closed.compareAndSet(expect = false, update = true)) hostScope.cancel()
    }

    /**
     * Waits for all owned stores to release their resources before a replacement host is created.
     */
    public suspend fun closeAndJoin() {
        close()
        hostScope.coroutineContext[Job]?.join()
    }

    private fun register(definition: StoreDefinition<*>) {
        check(!closed.value) { "StoreHost is closed" }
        while (true) {
            check(!closed.value) { "StoreHost is closed" }
            val current = definitions.value
            val existing = current[definition.name]
            if (existing != null) {
                require(existing === definition) {
                    "Conflicting store definitions for '${definition.name}'"
                }
                return
            }
            if (definitions.compareAndSet(current, current + (definition.name to definition)))
                return
        }
    }

    /** Returns the [Store] for [definition], creating it if necessary. */
    public fun <T : Any> store(definition: StoreDefinition<T>): Store<T> =
        DataStoreStore(dataStore(definition))

    /** Returns the [DataStore] for [definition], creating it if necessary. */
    public fun <T : Any> dataStore(definition: StoreDefinition<T>): DataStore<T> {
        register(definition)
        val key = definition.name
        while (true) {
            check(!closed.value) { "StoreHost is closed" }
            val current = dataStores.value
            val existing = current[key]
            if (existing != null) {
                @Suppress("UNCHECKED_CAST")
                return existing.value as DataStore<T>
            }
            val cell =
                lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
                    check(!closed.value) { "StoreHost is closed" }
                    DataStoreFactory.create(
                        storage =
                            file.create(definition.name, definition.defaultValue, definition.codec),
                        scope = hostScope,
                    )
                }
            if (dataStores.compareAndSet(current, current + (key to cell))) {
                return cell.value
            }
        }
    }

    /** Returns the [MutableStore] for [definition], creating it if necessary. */
    public fun <T : Any> mutable(definition: StoreDefinition<T>): MutableStore<T> {
        register(definition)
        val key = definition.name
        while (true) {
            check(!closed.value) { "StoreHost is closed" }
            val current = states.value
            val existing = current[key]
            if (existing != null) {
                @Suppress("UNCHECKED_CAST")
                return existing.value as MutableStore<T>
            }
            val cell =
                lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
                    check(!closed.value) { "StoreHost is closed" }
                    MutableStore(
                        store = DataStoreStore(dataStore(definition)),
                        scope = hostScope,
                        initialValue = definition.defaultValue,
                        cells = cells,
                    )
                }
            if (states.compareAndSet(current, current + (key to cell))) {
                return cell.value
            }
        }
    }
}
