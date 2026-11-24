package top.ltfan.m3x.settingspage.datastore

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import top.ltfan.m3x.datastore.StoreStatus
import top.ltfan.m3x.settingspage.SettingsPersistenceStatus

class SettingsPersistenceTest {
    @Test
    fun currentStatusAndReplayChangeSynchronouslyWithoutAnOwnedCollector() {
        val source = MutableStateFlow<StoreStatus>(StoreStatus.Loading)
        val status = source.asSettingsStatus()
        assertEquals(0, source.subscriptionCount.value)
        assertEquals(SettingsPersistenceStatus.Loading, status.value)
        source.value = StoreStatus.Ready
        assertEquals(SettingsPersistenceStatus.Ready, status.value)
        val failure = IllegalStateException("read")
        source.value = StoreStatus.ReadFailed(failure)
        assertSame(failure, (status.value as SettingsPersistenceStatus.ReadFailed).cause)
        assertEquals(listOf(status.value), status.replayCache)
        assertEquals(0, source.subscriptionCount.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun collectionRetainsSourceConflationFailureIdentityAndCancellationOwnership() = runTest {
        val source = MutableStateFlow<StoreStatus>(StoreStatus.Ready)
        val status = source.asSettingsStatus()
        val seen = mutableListOf<SettingsPersistenceStatus>()
        val collector =
            launch(start = CoroutineStart.UNDISPATCHED) { status.collect { seen += it } }
        assertEquals(1, source.subscriptionCount.value)
        val failure = IllegalArgumentException("write")
        source.value = StoreStatus.WriteFailed(failure)
        runCurrent()
        assertEquals(SettingsPersistenceStatus.Ready, seen.first())
        assertSame(failure, (seen.last() as SettingsPersistenceStatus.WriteFailed).cause)
        collector.cancel()
        runCurrent()
        assertEquals(0, source.subscriptionCount.value)
        source.value = StoreStatus.Ready
        assertEquals(SettingsPersistenceStatus.Ready, status.value)
        assertEquals(2, seen.size)
    }
}
