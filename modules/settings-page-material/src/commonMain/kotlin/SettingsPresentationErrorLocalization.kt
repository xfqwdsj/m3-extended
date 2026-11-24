package top.ltfan.m3x.settingspage.material

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.ltfan.m3x.resource.compose.resolve
import top.ltfan.m3x.settingspage.SettingsCommitResult
import top.ltfan.m3x.settingspage.compose.SettingsEditError
import top.ltfan.m3x.settingspage.compose.SettingsPresentationError

/**
 * Optional localized text for a caller's own UI. Persistence exception messages are deliberately
 * excluded; the original cause remains available for diagnostics. Store the callback value in
 * observable state and call this function in composition.
 */
@Composable
public fun SettingsPresentationError.localizedMessage(
    labels: CardSettingsLabels = remember { CardSettingsLabels() },
): String =
    when (this) {
        is SettingsPresentationError.ReadFailed -> labels.readFailed.resolve()
        is SettingsPresentationError.WriteFailed -> labels.writeFailed.resolve()
        is SettingsPresentationError.InvalidConfiguration -> issue.localizedMessage()
        is SettingsPresentationError.MissingCustomEditor ->
            labels.customEditorMissing(item.rendererId).resolve()
        is SettingsPresentationError.EditFailed ->
            when (val error = reason) {
                SettingsEditError.InvalidInput -> labels.invalidNumber.resolve()
                is SettingsEditError.Commit ->
                    when (val result = error.result) {
                        SettingsCommitResult.AcceptedLocally -> ""
                        SettingsCommitResult.Unavailable -> labels.unavailable.resolve()
                        SettingsCommitResult.InvalidCandidateType -> labels.invalidNumber.resolve()
                        is SettingsCommitResult.BuiltInIssue -> result.issue.localizedMessage()
                        is SettingsCommitResult.CustomIssue -> result.message
                    }
            }
    }
