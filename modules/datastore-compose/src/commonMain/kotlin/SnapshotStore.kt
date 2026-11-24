package top.ltfan.m3x.datastore.compose

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import top.ltfan.m3x.datastore.MutableStore
import top.ltfan.m3x.datastore.Store
import top.ltfan.m3x.datastore.StoreFile
import top.ltfan.m3x.datastore.StoreHost
import top.ltfan.m3x.datastore.ValueCell
import top.ltfan.m3x.datastore.ValueCellFactory

/** A cell factory whose reads subscribe a Compose composition to changes. */
public val ValueCellFactory.Companion.Snapshot: ValueCellFactory
    get() = SnapshotValueCellFactory

private object SnapshotValueCellFactory : ValueCellFactory {
    override fun <T> create(initial: T): ValueCell<T> = SnapshotValueCell(initial)
}

private class SnapshotValueCell<T>(initial: T) : ValueCell<T> {
    private val state = mutableStateOf(initial)
    override var value: T
        get() = state.value
        set(value) {
            state.value = value
        }
}

/** Creates a store whose [MutableStore.value] is observable by Compose. */
public fun <T> mutableStateStore(
    store: Store<T>,
    scope: CoroutineScope,
    initialValue: T,
): MutableStore<T> = MutableStore(store, scope, initialValue, ValueCellFactory.Snapshot)

/** Creates a host that supplies Compose observable stores. */
public fun storeHost(scope: CoroutineScope, file: StoreFile): StoreHost =
    StoreHost(scope, file, ValueCellFactory.Snapshot)
