package top.ltfan.m3x.settingspage.material

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import top.ltfan.m3x.compose.material3.ListItemColorsTransparent
import top.ltfan.m3x.material.Slider
import top.ltfan.m3x.resource.compose.resolve
import top.ltfan.m3x.settingspage.NumberType
import top.ltfan.m3x.settingspage.SettingsCommit
import top.ltfan.m3x.settingspage.SettingsItem
import top.ltfan.m3x.settingspage.SettingsValidation
import top.ltfan.m3x.settingspage.SliderCommitMode
import top.ltfan.m3x.settingspage.SliderMode
import top.ltfan.m3x.settingspage.compose.SettingsPresentationError
import top.ltfan.m3x.settingspage.compose.reportFailure
import top.ltfan.m3x.settingspage.sliderMappingOrNull

@Composable
internal fun SliderSettingsRow(
    item: SettingsItem.Item<*>,
    finite: NumberType.Finite<*>,
    interactive: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier,
    onError: (SettingsPresentationError) -> Unit,
    commitMode: SliderCommitMode,
    sharedText: SettingsSharedText?,
) {
    val mapping = remember(finite) { finite.sliderMappingOrNull() }
    val currentOnError by rememberUpdatedState(onError)
    val currentlyInteractive by rememberUpdatedState(interactive && item.enabled)
    LaunchedEffect(item.id, finite, interactive) {
        if (interactive && mapping == null) {
            currentOnError(
                SettingsPresentationError.InvalidConfiguration(
                    item,
                    SettingsValidation.Issue.InvalidBounds,
                ),
            )
        }
    }
    val current = item.value as Number
    var dragging by remember(item.id, finite) { mutableStateOf(false) }
    var localPosition by remember(item.id, finite) { mutableFloatStateOf(0f) }
    LaunchedEffect(currentlyInteractive) {
        if (!currentlyInteractive) {
            dragging = false
            localPosition = mapping?.positionOf(current) ?: 0f
        }
    }
    val position = if (dragging) localPosition else mapping?.positionOf(current) ?: 0f
    val preview = if (dragging) mapping?.valueAt(position) ?: current else current
    val description = item.resources.description?.resolve()
    Column(modifier) {
        ListItem(
            onClick = onOpen,
            enabled = interactive && item.enabled && finite.allowInput,
            supportingContent = {
                Column {
                    description?.let { Text(it, Modifier.settingsSharedDescription(sharedText)) }
                    Text(preview.toString())
                }
            },
            leadingContent =
                (item.resources.icon?.resolve() as? Painter)?.let { icon ->
                    { Icon(icon, contentDescription = null) }
                },
            colors = ListItemColorsTransparent,
            content = {
                Text(item.resources.label.resolve(), Modifier.settingsSharedLabel(sharedText))
            },
        )
        if (mapping != null)
            Slider(
                value = position,
                onValueChange = { next ->
                    if (!currentlyInteractive) return@Slider
                    dragging = true
                    localPosition =
                        if (mapping.bounds.sliderMode == SliderMode.Steps && mapping.steps == 0) {
                            if (next < 0.5f) 0f else 1f
                        } else next
                    if (currentlyInteractive && commitMode == SliderCommitMode.WhileDragging) {
                        val value = mapping.valueAt(localPosition)
                        if (value != item.value)
                            SettingsCommit.tryCommitDraft(item, value).reportFailure(item, onError)
                    }
                },
                onValueChangeFinished = {
                    if (dragging) {
                        if (currentlyInteractive) {
                            val next = mapping.valueAt(localPosition)
                            if (next != item.value) {
                                SettingsCommit.tryCommitDraft(item, next)
                                    .reportFailure(item, onError)
                            }
                        }
                        dragging = false
                    }
                },
                enabled = interactive && item.enabled && !mapping.isFixed,
                steps = mapping.steps,
                modifier =
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).semantics {
                        stateDescription = preview.toString()
                    },
            )
    }
}
