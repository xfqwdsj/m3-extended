package top.ltfan.m3x.settingspage.compose

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import top.ltfan.m3x.settingspage.SettingsCommit
import top.ltfan.m3x.settingspage.SettingsCommitResult
import top.ltfan.m3x.settingspage.SettingsItem

/** A renderer-independent, Compose observable draft for one settings item. */
@Stable
public class SettingsEditSession {
    public var activeItem: SettingsItem.Item<*>? by mutableStateOf(null)
        private set

    public var draft: Any? by mutableStateOf(null)
        private set

    public var error: SettingsEditError? by mutableStateOf(null)
        private set

    public fun open(item: SettingsItem.Item<*>) {
        if (!item.enabled || !item.shouldShow) return
        activeItem = item
        draft = item.value
        error = null
    }

    public fun changeDraft(value: Any?) {
        draft = value
        error = null
    }

    public fun invalidInput() {
        error = SettingsEditError.InvalidInput
    }

    public fun dismiss() {
        activeItem = null
        draft = null
        error = null
    }

    /** Returns true when the value was accepted into the local write queue. */
    public fun confirm(): Boolean {
        val item = activeItem ?: return false
        if (error != null) return false
        val result = SettingsCommit.tryCommitDraft(item, draft)
        if (result == SettingsCommitResult.AcceptedLocally) return true
        error = SettingsEditError.Commit(result)
        return false
    }
}

@Immutable
public sealed interface SettingsEditError {
    public data object InvalidInput : SettingsEditError

    public data class Commit(val result: SettingsCommitResult) : SettingsEditError
}
