package top.ltfan.m3x.settingspage.compose

import top.ltfan.m3x.settingspage.SettingsCommitResult
import top.ltfan.m3x.settingspage.SettingsItem
import top.ltfan.m3x.settingspage.SettingsSource
import top.ltfan.m3x.settingspage.SettingsValidation

/** Structured failures for caller-owned presentation and recovery. */
public sealed interface SettingsPresentationError {
    /** Recreate the source's persistence host to retry an initial read. */
    public data class ReadFailed(val source: SettingsSource, val cause: Throwable) :
        SettingsPresentationError

    /** The caller can retry with `source.persistence?.retrySave()`. */
    public data class WriteFailed(val source: SettingsSource, val cause: Throwable) :
        SettingsPresentationError

    public data class EditFailed(val item: SettingsItem.Item<*>, val reason: SettingsEditError) :
        SettingsPresentationError

    public data class InvalidConfiguration(
        val item: SettingsItem.Item<*>,
        val issue: SettingsValidation.Issue,
    ) : SettingsPresentationError

    public data class MissingCustomEditor(val item: SettingsItem.Item.Custom<*>) :
        SettingsPresentationError
}

public inline fun SettingsCommitResult.reportFailure(
    item: SettingsItem.Item<*>,
    onError: (SettingsPresentationError) -> Unit,
) {
    if (this != SettingsCommitResult.AcceptedLocally) {
        onError(SettingsPresentationError.EditFailed(item, SettingsEditError.Commit(this)))
    }
}
