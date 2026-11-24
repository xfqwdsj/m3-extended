package top.ltfan.m3x.demo

import androidx.compose.runtime.Composable
import androidx.datastore.core.InterProcessCoordinator
import androidx.datastore.core.ReadScope
import androidx.datastore.core.Storage
import androidx.datastore.core.StorageConnection
import androidx.datastore.core.WriteScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import top.ltfan.m3x.datastore.StoreCodec
import top.ltfan.m3x.datastore.StoreFile
import top.ltfan.m3x.datastore.serialization.jsonStoreCodec

object DemoJsonCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun <T> create(serializer: KSerializer<T>): StoreCodec<T> = jsonStoreCodec(serializer, json)
}

@Composable internal expect fun rememberDemoStoreFile(): StoreFile

/**
 * The platform key-value store supplies bytes; AndroidX DataStore owns updates and coordination.
 */
internal class DemoStoreFile(
    private val read: (String) -> String?,
    private val write: (String, String) -> Unit,
) : StoreFile {
    override fun <T> create(name: String, defaultValue: T, codec: StoreCodec<T>): Storage<T> =
        object : Storage<T> {
            private val sharedCoordinator = DemoCoordinator()

            override fun createConnection(): StorageConnection<T> =
                object : StorageConnection<T> {
                    override val coordinator: InterProcessCoordinator = sharedCoordinator

                    override fun close() = Unit

                    override suspend fun <R> readScope(
                        block: suspend ReadScope<T>.(Boolean) -> R,
                    ): R =
                        object : ReadScope<T> {
                                override suspend fun readData(): T =
                                    read(name)?.let { codec.decode(it.encodeToByteArray()) }
                                        ?: defaultValue

                                override fun close() = Unit
                            }
                            .block(true)

                    override suspend fun writeScope(block: suspend WriteScope<T>.() -> Unit) {
                        object : WriteScope<T> {
                                override suspend fun readData(): T =
                                    read(name)?.let { codec.decode(it.encodeToByteArray()) }
                                        ?: defaultValue

                                override suspend fun writeData(value: T) {
                                    write(name, codec.encode(value).decodeToString())
                                }

                                override fun close() = Unit
                            }
                            .block()
                    }
                }
        }
}

private class DemoCoordinator : InterProcessCoordinator {
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
