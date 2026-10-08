package top.ltfan.m3x.settingspage

/** Local persistence readiness and failures observed by settings renderers. */
public sealed interface SettingsPersistenceStatus {
    public data object Loading : SettingsPersistenceStatus

    public data object Ready : SettingsPersistenceStatus

    public data class ReadFailed(val cause: Throwable) : SettingsPersistenceStatus

    public data class WriteFailed(val cause: Throwable) : SettingsPersistenceStatus
}
