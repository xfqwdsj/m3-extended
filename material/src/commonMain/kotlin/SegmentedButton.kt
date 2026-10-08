/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package top.ltfan.m3x.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.MultiChoiceSegmentedButtonRowScope
import androidx.compose.material3.SegmentedButton as MaterialSegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults as MaterialSegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import top.ltfan.m3x.material.theme.extended

/**
 * [Material Design segmented Button](https://m3.material.io/components/segmented-buttons/overview)
 *
 * Segmented buttons help people select options, switch views, or sort elements.
 *
 * A default Toggleable Segmented Button. Also known as Outlined Segmented Button. See [toggleable].
 *
 * Toggleable segmented buttons should be used for cases where the selection is not mutually
 * exclusive.
 *
 * This should typically be used inside of a [MultiChoiceSegmentedButtonRow]
 *
 * @param checked whether this button is checked
 * @param onCheckedChange called when the button requests a change to its checked state
 * @param shape the shape for this button
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors colors for this button in different states
 * @param border the border for this button
 * @param contentPadding spacing between the container and its content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param icon the icon slot for this button. With no unchecked icon, the content shifts to show the
 *   checked icon. When both states have icons, the icons crossfade.
 * @param label content to be rendered inside this button
 */
@Composable
public fun MultiChoiceSegmentedButtonRowScope.SegmentedButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SegmentedButtonColors = MaterialSegmentedButtonDefaults.colors(),
    border: BorderStroke =
        MaterialSegmentedButtonDefaults.borderStroke(colors.segmentedBorderColor(enabled, checked)),
    contentPadding: PaddingValues = MaterialSegmentedButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    icon: @Composable () -> Unit = { MaterialSegmentedButtonDefaults.Icon(checked) },
    label: @Composable () -> Unit,
): Unit =
    MaterialSegmentedButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shape = shape,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        icon = icon,
        label = label,
    )

/**
 * [Material Design segmented button](https://m3.material.io/components/segmented-buttons/overview)
 *
 * Segmented buttons help people select options, switch views, or sort elements.
 *
 * An outlined selectable segmented button. See [selectable].
 *
 * Selectable segmented buttons should be used for cases where the selection is mutually exclusive,
 * when only one button can be selected at a time.
 *
 * This should typically be used inside of a [SingleChoiceSegmentedButtonRow]
 *
 * @param selected whether this button is selected
 * @param onClick called when the user requests selection of this button
 * @param shape the shape for this button
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors colors for this button in different states
 * @param border the border for this button
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param icon the icon slot for this button. With no unchecked icon, the content shifts to show the
 *   checked icon. When both states have icons, the icons crossfade.
 * @param label content to be rendered inside this button
 */
@Composable
public fun SingleChoiceSegmentedButtonRowScope.SegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SegmentedButtonColors = MaterialSegmentedButtonDefaults.colors(),
    border: BorderStroke =
        MaterialSegmentedButtonDefaults.borderStroke(
            colors.segmentedBorderColor(enabled, selected),
        ),
    contentPadding: PaddingValues = MaterialSegmentedButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    icon: @Composable () -> Unit = { MaterialSegmentedButtonDefaults.Icon(selected) },
    label: @Composable () -> Unit,
): Unit =
    MaterialSegmentedButton(
        selected = selected,
        onClick = onClick,
        shape = shape,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        icon = icon,
        label = label,
    )

private fun SegmentedButtonColors.segmentedBorderColor(enabled: Boolean, active: Boolean): Color =
    if (enabled) {
        if (active) activeBorderColor else inactiveBorderColor
    } else {
        if (active) disabledActiveBorderColor else disabledInactiveBorderColor
    }

/** Theme-aware shapes for segmented button rows. */
public object SegmentedButtonDefaults {
    /** Corner-based full shape used to derive the row endpoints. */
    public val baseShape: CornerBasedShape
        @Composable
        get() =
            MaterialTheme.extended.shapes.full as? CornerBasedShape
                ?: MaterialTheme.extended.shapes.factory.logical(
                    CornerSize(100),
                    CornerSize(100),
                    CornerSize(100),
                    CornerSize(100),
                )

    /**
     * A shape constructor that the button in [index] should have when there are [count] buttons in
     * the container.
     *
     * The default base shape resolves through the M3X shape theme.
     *
     * @param index the index for this button in the row
     * @param count the count of buttons in this row
     * @param baseShape the [CornerBasedShape] the base shape that should be used in buttons that
     *   are not in the start or the end.
     */
    @Composable
    public fun itemShape(
        index: Int,
        count: Int,
        baseShape: CornerBasedShape = this.baseShape,
    ): Shape = MaterialSegmentedButtonDefaults.itemShape(index, count, baseShape)
}
