package top.ltfan.m3x.settingspage.material

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import top.ltfan.m3x.compose.material3.ListItemColorsTransparent
import top.ltfan.m3x.material.RangeSlider
import top.ltfan.m3x.material.Slider
import top.ltfan.m3x.resource.compose.resolve
import top.ltfan.m3x.settingspage.NumberType
import top.ltfan.m3x.settingspage.RangeBasedNumberInfo
import top.ltfan.m3x.settingspage.SettingsCommit
import top.ltfan.m3x.settingspage.SettingsItem
import top.ltfan.m3x.settingspage.SettingsNumbers
import top.ltfan.m3x.settingspage.SettingsValidation
import top.ltfan.m3x.settingspage.SliderCommitMode
import top.ltfan.m3x.settingspage.SliderMode
import top.ltfan.m3x.settingspage.compose.SettingsPresentationError
import top.ltfan.m3x.settingspage.compose.reportFailure
import top.ltfan.m3x.settingspage.sliderMappingOrNull

@Composable
internal fun RangeSliderSettingsRow(
    item: SettingsItem.Item.NumberRange<*>,
    labels: CardSettingsLabels,
    interactive: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier,
    onError: (SettingsPresentationError) -> Unit,
    commitMode: SliderCommitMode,
    sharedText: SettingsSharedText?,
) {
    val startType = item.startType as? NumberType.Finite<*>
    val endType = item.endType as? NumberType.Finite<*>
    val startMapping =
        remember(startType) {
            startType?.sliderMappingOrNull()
        }
    val endMapping =
        remember(endType) {
            endType?.sliderMappingOrNull()
        }
    val startSlider = startMapping != null || startType?.type == RangeBasedNumberInfo.Type.Slider
    val endSlider = endMapping != null || endType?.type == RangeBasedNumberInfo.Type.Slider
    val original = item.value
    val start: Number = original.first
    val end: Number = original.second
    val validOrder = SettingsNumbers.compare(start, end, item.startType.kind) <= 0
    var startDragging by remember(item.id, startType) { mutableStateOf(false) }
    var endDragging by remember(item.id, endType) { mutableStateOf(false) }
    var localStart by remember(item.id, startType) { mutableFloatStateOf(0f) }
    var localEnd by remember(item.id, endType) { mutableFloatStateOf(0f) }
    val currentlyInteractive by rememberUpdatedState(interactive && item.enabled)
    LaunchedEffect(currentlyInteractive) {
        if (!currentlyInteractive) {
            startDragging = false
            endDragging = false
            localStart = startMapping?.positionOf(start) ?: 0f
            localEnd = endMapping?.positionOf(end) ?: 1f
        }
    }
    val startPosition = if (startDragging) localStart else startMapping?.positionOf(start) ?: 0f
    val endPosition = if (endDragging) localEnd else endMapping?.positionOf(end) ?: 1f
    val previewStart = if (startDragging) startMapping?.valueAt(startPosition) ?: start else start
    val previewEnd = if (endDragging) endMapping?.valueAt(endPosition) ?: end else end
    val canOpen =
        (startType?.allowInput != false || !startSlider) ||
            (endType?.allowInput != false || !endSlider)
    val currentOnError by rememberUpdatedState(onError)
    LaunchedEffect(item.id, startType, endType, validOrder, interactive) {
        if (interactive) {
            if (startSlider && startMapping == null || endSlider && endMapping == null) {
                currentOnError(
                    SettingsPresentationError.InvalidConfiguration(
                        item,
                        SettingsValidation.Issue.InvalidBounds,
                    ),
                )
            }
            if (!validOrder) {
                currentOnError(
                    SettingsPresentationError.InvalidConfiguration(
                        item,
                        SettingsValidation.Issue.RangeOrder,
                    ),
                )
            }
        }
    }

    fun commit(a: Number, b: Number) {
        if (!currentlyInteractive) return
        if (a == item.value.first && b == item.value.second) return
        SettingsCommit.tryCommitDraft(item, a to b).reportFailure(item, onError)
    }

    Column(modifier) {
        ListItem(
            onClick = onOpen,
            enabled = interactive && item.enabled && canOpen,
            supportingContent = {
                Column {
                    item.resources.description?.resolve()?.let {
                        Text(it, Modifier.settingsSharedDescription(sharedText))
                    }
                    Text("$previewStart – $previewEnd")
                }
            },
            colors = ListItemColorsTransparent,
            content = {
                Text(item.resources.label.resolve(), Modifier.settingsSharedLabel(sharedText))
            },
        )
        if (validOrder && startMapping != null && endMapping != null && startType == endType) {
            RangeSlider(
                value = startPosition..endPosition,
                onValueChange = { next ->
                    if (!currentlyInteractive) return@RangeSlider
                    startDragging = true
                    endDragging = true
                    localStart = snapTwoStop(next.start, startMapping)
                    localEnd = snapTwoStop(next.endInclusive, endMapping)
                    if (commitMode == SliderCommitMode.WhileDragging)
                        commit(startMapping.valueAt(localStart), endMapping.valueAt(localEnd))
                },
                onValueChangeFinished = {
                    if (startDragging || endDragging) {
                        commit(startMapping.valueAt(localStart), endMapping.valueAt(localEnd))
                        startDragging = false
                        endDragging = false
                    }
                },
                enabled = interactive && item.enabled && !startMapping.isFixed,
                steps = startMapping.steps,
                modifier =
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).semantics {
                        stateDescription = "$previewStart – $previewEnd"
                    },
            )
        } else {
            if (startMapping != null) {
                Text(labels.from.resolve(), modifier = Modifier.padding(horizontal = 16.dp))
                Slider(
                    value = startPosition,
                    onValueChange = {
                        if (!currentlyInteractive) return@Slider
                        startDragging = true
                        localStart = snapTwoStop(it, startMapping)
                        if (commitMode == SliderCommitMode.WhileDragging)
                            commit(startMapping.valueAt(localStart), item.value.second)
                    },
                    onValueChangeFinished = {
                        if (startDragging) {
                            commit(startMapping.valueAt(localStart), end)
                            startDragging = false
                        }
                    },
                    enabled = interactive && item.enabled && !startMapping.isFixed,
                    steps = startMapping.steps,
                    modifier =
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp).semantics {
                            stateDescription = previewStart.toString()
                        },
                )
            }
            if (endMapping != null) {
                Text(labels.to.resolve(), modifier = Modifier.padding(horizontal = 16.dp))
                Slider(
                    value = endPosition,
                    onValueChange = {
                        if (!currentlyInteractive) return@Slider
                        endDragging = true
                        localEnd = snapTwoStop(it, endMapping)
                        if (commitMode == SliderCommitMode.WhileDragging)
                            commit(item.value.first, endMapping.valueAt(localEnd))
                    },
                    onValueChangeFinished = {
                        if (endDragging) {
                            commit(start, endMapping.valueAt(localEnd))
                            endDragging = false
                        }
                    },
                    enabled = interactive && item.enabled && !endMapping.isFixed,
                    steps = endMapping.steps,
                    modifier =
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp).semantics {
                            stateDescription = previewEnd.toString()
                        },
                )
            }
        }
    }
}

private fun snapTwoStop(
    position: Float,
    mapping: SettingsNumbers.SliderMapping,
): Float =
    if (mapping.bounds.sliderMode == SliderMode.Steps && mapping.steps == 0) {
        if (position < 0.5f) 0f else 1f
    } else position
