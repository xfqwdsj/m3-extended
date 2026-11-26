package top.ltfan.m3x.backdrop

import top.ltfan.m3x.backdrop.internal.FailureCollector

/**
 * Retains renderer state by keys with immutable equality and hash behavior. Each entry owns a
 * separate layer owner and shader namespace. Parameter changes update the retained value; callers
 * select active entries with [retainOnly]. Access belongs to the renderer's lifecycle.
 *
 * Display refresh disposes values and releases their layers, retaining shader caches for lazy
 * recreation. Removal and owner release dispose entries and their shader caches. Values borrowed
 * from the pool remain valid until removal, clearing, display refresh or owner release.
 */
public class BackdropResourcePool<K : Any, V : Any>
internal constructor(
    private val resources: () -> PrimitiveResources,
    private val create: BackdropRenderResources.(K) -> V,
    private val dispose: (V) -> Unit,
) {
    private class Entry<V>(val resources: PrimitiveResources, var value: V?)

    private val entries = mutableMapOf<K, Entry<V>>()
    private var closed = false
    private var changing = false

    /** Number of retained keys, including entries awaiting recreation after display refresh. */
    public val size: Int
        get() = entries.size

    /**
     * Returns the retained value or creates it using this key's resource owner. Resources allocated
     * through that owner are cleaned up if construction throws. The factory owns cleanup of other
     * partially constructed resources. Callbacks operate on entry resources; pool mutations are
     * guarded for the duration of each callback.
     */
    public fun getOrCreate(key: K): V {
        checkAccess()
        val entry = entries[key] ?: Entry<V>(resources(), null).also { entries[key] = it }
        entry.value?.let {
            return it
        }
        changing = true
        try {
            return create(entry.resources, key).also { entry.value = it }
        } catch (failure: Throwable) {
            entries.remove(key)
            try {
                entry.resources.close()
            } catch (cleanup: Throwable) {
                failure.addSuppressed(cleanup)
            }
            throw failure
        } finally {
            changing = false
        }
    }

    /** Releases entries absent from [keys], retaining existing values for the remaining keys. */
    public fun retainOnly(keys: Set<K>) {
        checkAccess()
        changing = true
        val failures = FailureCollector()
        try {
            val iterator = entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.key in keys) continue
                val value = entry.value
                iterator.remove()
                failures.run { release(value, refresh = false) }
            }
        } finally {
            changing = false
        }
        failures.throwIfFailed()
    }

    /** Disposes all retained values and releases their layers and shader caches. */
    public fun clear(): Unit = retainOnly(emptySet())

    internal fun refresh() {
        checkAccess()
        changing = true
        val failures = FailureCollector()
        try {
            for (entry in entries.values) {
                failures.run { release(entry, refresh = true) }
            }
        } finally {
            changing = false
        }
        failures.throwIfFailed()
    }

    internal fun close() {
        if (closed) return
        try {
            clear()
        } finally {
            closed = true
        }
    }

    private fun release(entry: Entry<V>, refresh: Boolean) {
        val value = entry.value
        entry.value = null
        val failures = FailureCollector()
        if (value != null) failures.run { dispose(value) }
        failures.run {
            if (refresh) entry.resources.releaseLayers() else entry.resources.close()
        }
        failures.throwIfFailed()
    }

    private fun checkAccess() {
        check(!closed) { "The resource pool's owner has been released" }
        check(!changing) { "Resource pool callbacks cannot reenter their pool" }
    }
}
