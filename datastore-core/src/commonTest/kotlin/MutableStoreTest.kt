package top.ltfan.m3x.datastore

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class MutableStoreTest {
    @Test
    fun writesBeforeFirstValueAreQueuedAndNotLost() = runTest {
        assertQueuedBeforeFirstValue { it.store }
    }

    @Test
    fun writesArePersistedInOrder() = runTest {
        assertOrderedWrites { it.store }
    }

    @Test
    fun consecutiveWritesAreCoalesced() = runTest {
        assertCoalescedWrites { it.store }
    }

    @Test
    fun staleDiskValueDoesNotRevertVisibleState() = runTest {
        assertEchoGuard { it.store }
    }

    @Test
    fun flowStoreSatisfiesTheSameSemantics() = runTest {
        assertQueuedBeforeFirstValue { it.flowStore }
        assertOrderedWrites { it.flowStore }
        assertCoalescedWrites { it.flowStore }
        assertEchoGuard { it.flowStore }
    }

    @Test
    fun awaitInitializedSuspendsUntilFirstValue() = runTest {
        val backend = TestBackend<String>()
        val state =
            MutableStore(
                backend.store,
                backgroundScope,
                initialValue = "initial",
                cells = ValueCellFactory.Plain,
            )
        var ready = false
        launch {
            state.awaitInitialized()
            ready = true
        }

        runCurrent()
        assertFalse(ready)

        backend.emit("disk")
        runCurrent()
        assertTrue(ready)
    }

    @Test
    fun propertyDelegateReadsAndWritesBack() = runTest {
        val backend = TestBackend<Model>()
        val state =
            MutableStore(
                backend.store,
                backgroundScope,
                initialValue = Model("", 0),
                cells = ValueCellFactory.Plain,
            )
        backend.emit(Model("a", 1))
        runCurrent()

        var name by
            state.property(get = { it.name }, set = { model, value -> model.copy(name = value) })
        assertEquals("a", name)

        name = "b"
        assertEquals("b", name)
        assertEquals("b", state.value.name)
        assertEquals(1, state.value.count)

        runCurrent()
        assertEquals(Model("b", 1), backend.updates.last())
    }

    @Test
    fun wholeValueCanBeDelegated() = runTest {
        val backend = TestBackend<Model>()
        val state =
            MutableStore(
                backend.store,
                backgroundScope,
                initialValue = Model("", 0),
                cells = ValueCellFactory.Plain,
            )
        backend.emit(Model("a", 1))
        runCurrent()

        var model: Model by state
        assertEquals(Model("a", 1), model)

        model = Model("b", 2)
        assertEquals(Model("b", 2), model)
        assertEquals(Model("b", 2), state.data.value)

        runCurrent()
        assertEquals(Model("b", 2), backend.updates.last())
    }

    @Test
    fun atomicTransformsPreserveEveryChange() = runTest {
        val backend = TestBackend<Int>()
        val state =
            MutableStore(
                backend.store,
                backgroundScope,
                initialValue = 0,
                cells = ValueCellFactory.Plain,
            )
        backend.emit(10)
        runCurrent()
        repeat(100) { launch { state.update { it + 1 } } }
        runCurrent()
        assertEquals(110, state.value)
        assertEquals(110, backend.state.value)
    }

    @Test
    fun concurrentUpdatesPersistInTheSameOrderAsTheVisibleValue() = runTest {
        val backend = TestBackend<String>()
        val state =
            MutableStore(
                backend.store,
                backgroundScope,
                initialValue = "",
                cells = ValueCellFactory.Plain,
            )
        backend.emit("disk")
        runCurrent()
        val jobs =
            (0 until 40).map { index ->
                launch(Dispatchers.Default) { state.update { it + ".$index" } }
            }
        jobs.joinAll()
        runCurrent()
        assertEquals(state.value, backend.state.value)
        assertEquals(state.value, state.data.value)
    }

    @Test
    fun failedWriteRemainsVisibleAndCanBeRetried() = runTest {
        val backend = TestBackend<Int>()
        val state =
            MutableStore(
                backend.store,
                backgroundScope,
                initialValue = 0,
                cells = ValueCellFactory.Plain,
            )
        backend.emit(10)
        runCurrent()
        backend.failNextUpdate(IllegalStateException("temporary failure"))
        state.update { it + 1 }
        runCurrent()
        assertEquals(11, state.value)
        assertEquals(10, backend.state.value)
        assertTrue(state.status.value is StoreStatus.WriteFailed)
        state.retrySave()
        runCurrent()
        assertEquals(11, backend.state.value)
        assertEquals(StoreStatus.Ready, state.status.value)
    }

    @Test
    fun initialReadFailureIsReportedAndRejectsWrites() = runTest {
        val failing =
            object : Store<Int> {
                override val data: Flow<Int> = flow { throw IllegalStateException("unavailable") }

                override suspend fun update(transform: suspend (Int) -> Int): Int =
                    error("unreachable")
            }
        val state =
            MutableStore(failing, backgroundScope, initialValue = 0, cells = ValueCellFactory.Plain)
        runCurrent()
        assertTrue(state.status.value is StoreStatus.ReadFailed)
        assertFailsWith<IllegalStateException> { state.update { it + 1 } }
    }

    private fun TestScope.assertQueuedBeforeFirstValue(
        storeOf: (TestBackend<String>) -> Store<String>,
    ) {
        val backend = TestBackend<String>()
        val state =
            MutableStore(
                storeOf(backend),
                backgroundScope,
                initialValue = "initial",
                cells = ValueCellFactory.Plain,
            )

        state.write("a")
        state.write("b")
        runCurrent()

        // Nothing is persisted before the first value is read.
        assertEquals(emptyList(), backend.updates)
        assertEquals("b", state.value)
        assertEquals("b", state.data.value)

        backend.emit("disk")
        runCurrent()

        // The last queued write lands, and the initial disk value does not
        // revert the visible state.
        assertEquals(listOf("b"), backend.updates)
        assertEquals("b", state.value)
        assertEquals("b", state.data.value)
    }

    private fun TestScope.assertOrderedWrites(storeOf: (TestBackend<String>) -> Store<String>) {
        val backend = TestBackend<String>()
        val state =
            MutableStore(
                storeOf(backend),
                backgroundScope,
                initialValue = "initial",
                cells = ValueCellFactory.Plain,
            )
        backend.emit("disk")
        runCurrent()
        assertEquals("disk", state.data.value)

        state.write("a")
        runCurrent()
        state.write("b")
        runCurrent()

        assertEquals(listOf("a", "b"), backend.updates)
        assertEquals("b", state.data.value)
    }

    private fun TestScope.assertCoalescedWrites(storeOf: (TestBackend<String>) -> Store<String>) {
        val backend = TestBackend<String>()
        val state =
            MutableStore(
                storeOf(backend),
                backgroundScope,
                initialValue = "initial",
                cells = ValueCellFactory.Plain,
            )
        backend.emit("disk")
        runCurrent()

        repeat(100) { index -> state.write("v$index") }
        runCurrent()

        // Only the last write of the burst is persisted.
        assertEquals(listOf("v99"), backend.updates)
        assertEquals("v99", state.data.value)
    }

    private fun TestScope.assertEchoGuard(storeOf: (TestBackend<String>) -> Store<String>) {
        val backend = TestBackend<String>()
        val state =
            MutableStore(
                storeOf(backend),
                backgroundScope,
                initialValue = "initial",
                cells = ValueCellFactory.Plain,
            )
        backend.emit("disk")
        runCurrent()

        backend.block()
        state.write("new")
        assertEquals("new", state.value)

        // The disk echoes a stale value while the write is still in flight.
        backend.emit("stale")
        runCurrent()
        assertEquals("new", state.value)
        assertEquals("new", state.data.value)

        backend.release()
        runCurrent()

        assertEquals(listOf("new"), backend.updates)
        assertEquals("new", state.data.value)
    }

    data class Model(val name: String, val count: Int)
}
