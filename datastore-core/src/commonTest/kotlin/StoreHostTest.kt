package top.ltfan.m3x.datastore

import androidx.datastore.core.InterProcessCoordinator
import androidx.datastore.core.ReadScope
import androidx.datastore.core.Storage
import androidx.datastore.core.StorageConnection
import androidx.datastore.core.WriteScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class StoreHostTest {
    @Test
    fun sameNameReturnsSameInstances() = runTest {
        val host = StoreHost(backgroundScope, MemoryStoreFile(), ValueCellFactory.Plain)

        assertSame(host.dataStore(StringDefinition), host.dataStore(StringDefinition))
        assertSame(host.mutable(StringDefinition), host.mutable(StringDefinition))
    }

    @Test
    fun freshStoreReadsTheDefinitionDefault() = runTest {
        val file = MemoryStoreFile()
        val host = StoreHost(backgroundScope, file, ValueCellFactory.Plain)

        assertEquals("default", host.dataStore(StringDefinition).data.first())
        assertEquals(StringDefinition.defaultValue, file.defaults["string"])
    }

    @Test
    fun writesAndReadsThroughTheHost() = runTest {
        val host = StoreHost(backgroundScope, MemoryStoreFile(), ValueCellFactory.Plain)
        val store = host.mutable(StringDefinition)

        store.write("value")
        assertEquals("value", store.data.value)

        runCurrent()
        assertEquals("value", host.dataStore(StringDefinition).data.first())
    }

    @Test
    fun conflictingDefinitionsWithTheSameNameAreRejected() = runTest {
        val host = StoreHost(backgroundScope, MemoryStoreFile(), ValueCellFactory.Plain)
        host.dataStore(StringDefinition)
        val conflict = object : StoreDefinition<String> by StringDefinition {}
        assertFailsWith<IllegalArgumentException> { host.dataStore(conflict) }
    }

    @Test
    fun differentModelsWithTheSameNameAreRejectedByEveryEntryPoint() = runTest {
        for (mutableFirst in listOf(false, true)) {
            val file = MemoryStoreFile()
            val host = StoreHost(backgroundScope, file, ValueCellFactory.Plain)
            if (mutableFirst) host.mutable(StringDefinition) else host.dataStore(StringDefinition)
            val original = host.dataStore(StringDefinition)

            val errors =
                listOf(
                    assertFailsWith<IllegalArgumentException> {
                        host.dataStore(IntDefinitionWithSameName)
                    },
                    assertFailsWith<IllegalArgumentException> {
                        host.mutable(IntDefinitionWithSameName)
                    },
                    assertFailsWith<IllegalArgumentException> {
                        host.store(IntDefinitionWithSameName)
                    },
                )
            errors.forEach {
                assertEquals("Conflicting store definitions for 'string'", it.message)
            }
            assertSame(original, host.dataStore(StringDefinition))
            assertEquals("default", file.defaults["string"])
            assertEquals("default", original.data.first())
            original.updateData { "still usable" }
            assertEquals("still usable", original.data.first())
            host.closeAndJoin()
        }
    }

    @Test
    fun closedHostRejectsNewBindings() = runTest {
        val host = StoreHost(backgroundScope, MemoryStoreFile(), ValueCellFactory.Plain)
        val existing = host.mutable(StringDefinition)
        host.closeAndJoin()
        assertFailsWith<IllegalStateException> { host.mutable(StringDefinition) }
        assertFailsWith<IllegalStateException> { existing.write("after close") }
    }

    @Test
    fun failedReadRecoversOnlyAfterHostReplacement() = runTest {
        val file = MemoryStoreFile().apply { failReads = true }
        val failedHost = StoreHost(backgroundScope, file, ValueCellFactory.Plain)
        val failed = failedHost.mutable(StringDefinition)
        runCurrent()
        assertEquals(true, failed.status.value is StoreStatus.ReadFailed)
        assertFailsWith<IllegalStateException> { failed.write("unavailable") }

        failedHost.closeAndJoin()
        file.failReads = false
        val replacement = StoreHost(backgroundScope, file, ValueCellFactory.Plain)
        val recovered = replacement.mutable(StringDefinition)
        recovered.awaitInitialized()
        assertEquals(StoreStatus.Ready, recovered.status.value)
        assertEquals("default", recovered.value)

        recovered.write("recovered")
        runCurrent()
        assertEquals("recovered", replacement.dataStore(StringDefinition).data.first())
        assertFailsWith<IllegalStateException> { failed.write("stale") }
        replacement.closeAndJoin()
    }
}

private object StringDefinition : StoreDefinition<String> {
    override val name: String = "string"
    override val defaultValue: String = "default"
    override val codec: StoreCodec<String> =
        object : StoreCodec<String> {
            override fun encode(value: String): ByteArray = value.encodeToByteArray()

            override fun decode(bytes: ByteArray): String = bytes.decodeToString()
        }
}

private object IntDefinitionWithSameName : StoreDefinition<Int> {
    override val name: String = StringDefinition.name
    override val defaultValue: Int = 0
    override val codec: StoreCodec<Int> =
        object : StoreCodec<Int> {
            override fun encode(value: Int): ByteArray = value.toString().encodeToByteArray()

            override fun decode(bytes: ByteArray): Int = bytes.decodeToString().toInt()
        }
}

private class MemoryStoreFile : StoreFile {
    val defaults = mutableMapOf<String, Any?>()
    var failReads: Boolean = false
    private val storages = mutableMapOf<String, Storage<*>>()

    override fun <T> create(name: String, defaultValue: T, codec: StoreCodec<T>): Storage<T> {
        defaults[name] = defaultValue
        @Suppress("UNCHECKED_CAST")
        return storages.getOrPut(name) { MemoryStorage(defaultValue) { failReads } } as Storage<T>
    }
}

private class MemoryStorage<T>(initial: T, private val failReads: () -> Boolean) : Storage<T> {
    private val value = MutableStateFlow(initial)
    private val coordinator = TestCoordinator()

    override fun createConnection(): StorageConnection<T> =
        object : StorageConnection<T> {
            override val coordinator: InterProcessCoordinator
                get() = this@MemoryStorage.coordinator

            override fun close() = Unit

            override suspend fun <R> readScope(
                block: suspend ReadScope<T>.(locked: Boolean) -> R,
            ): R {
                if (failReads()) throw IllegalStateException("unavailable")
                return object : ReadScope<T> {
                        override suspend fun readData(): T = value.value

                        override fun close() = Unit
                    }
                    .block(true)
            }

            override suspend fun writeScope(block: suspend WriteScope<T>.() -> Unit) {
                object : WriteScope<T> {
                        override suspend fun readData(): T = value.value

                        override suspend fun writeData(value: T) {
                            this@MemoryStorage.value.value = value
                        }

                        override fun close() = Unit
                    }
                    .block()
            }
        }
}

private class TestCoordinator : InterProcessCoordinator {
    private val mutex = Mutex()
    private val version = MutableStateFlow(0)

    override val updateNotifications: Flow<Unit> = emptyFlow()

    override suspend fun <T> lock(block: suspend () -> T): T = mutex.withLock { block() }

    override suspend fun <T> tryLock(block: suspend (Boolean) -> T): T {
        val locked = mutex.tryLock()
        return try {
            block(locked)
        } finally {
            if (locked) mutex.unlock()
        }
    }

    override suspend fun getVersion(): Int = version.value

    override suspend fun incrementAndGetVersion(): Int = version.updateAndGet { it + 1 }
}
