package top.ltfan.m3x.settingspage.datastore

import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import top.ltfan.m3x.datastore.MutableStore
import top.ltfan.m3x.datastore.StoreStatus
import top.ltfan.m3x.settingspage.SettingsPersistence
import top.ltfan.m3x.settingspage.SettingsPersistenceStatus

/** Maps DataStore readiness without launching a collector or changing its lifetime. */
@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
public fun StateFlow<StoreStatus>.asSettingsStatus(): StateFlow<SettingsPersistenceStatus> =
    object : StateFlow<SettingsPersistenceStatus> {
        override val value: SettingsPersistenceStatus
            get() = this@asSettingsStatus.value.settingsStatus()

        override val replayCache: List<SettingsPersistenceStatus>
            get() = this@asSettingsStatus.replayCache.map { it.settingsStatus() }

        override suspend fun collect(collector: FlowCollector<SettingsPersistenceStatus>): Nothing =
            this@asSettingsStatus.collect { collector.emit(it.settingsStatus()) }
    }

/** Adapts a store to the settings persistence protocol while preserving its retry path. */
public fun MutableStore<*>.settingsPersistence(): SettingsPersistence =
    object : SettingsPersistence {
        override val status: StateFlow<SettingsPersistenceStatus> =
            this@settingsPersistence.status.asSettingsStatus()

        override fun retrySave() {
            this@settingsPersistence.retrySave()
        }
    }

private fun StoreStatus.settingsStatus(): SettingsPersistenceStatus =
    when (this) {
        StoreStatus.Loading -> SettingsPersistenceStatus.Loading
        StoreStatus.Ready -> SettingsPersistenceStatus.Ready
        is StoreStatus.ReadFailed -> SettingsPersistenceStatus.ReadFailed(cause)
        is StoreStatus.WriteFailed -> SettingsPersistenceStatus.WriteFailed(cause)
    }
