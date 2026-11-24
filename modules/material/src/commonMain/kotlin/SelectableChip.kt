/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.ChipColors
import androidx.compose.material3.ChipElevation
import androidx.compose.material3.ChipShapes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ElevatedFilterChip as MaterialElevatedFilterChip
import androidx.compose.material3.FilterChip as MaterialFilterChip
import androidx.compose.material3.FilterChipDefaults as MaterialFilterChipDefaults
import androidx.compose.material3.InputChip as MaterialInputChip
import androidx.compose.material3.InputChipDefaults as MaterialInputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SelectableChipElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import top.ltfan.m3x.material.theme.extended

/**
 * [Material Design filter chip](https://m3.material.io/components/chips/overview)
 *
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts.
 *
 * Filter chips use tags or descriptive words to filter content. They can be a good alternative to
 * toggle buttons or checkboxes. It also morphs between the shapes provided in [shapes] depending on
 * the state of the chip as long as the shapes provided are [CornerBasedShape]s. If a shape in
 * [shapes] isn't a [CornerBasedShape], then the chip will change between the [ChipShapes] according
 * to user interaction.
 *
 * This filter chip is applied with a flat style. If you want an elevated style, use the
 * [ElevatedFilterChip].
 *
 * Tapping on a filter chip toggles its selection state. A selection state [leadingIcon] can be
 * provided (e.g. a checkmark) to be appended at the starting edge of the chip's label.
 *
 * @param selected whether this chip is selected
 * @param onClick called when this chip is clicked
 * @param label text label for this chip
 * @param shapes the [ChipShapes] that this chip with morph between depending on the chip's selected
 *   state.
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param leadingIcon optional icon at the start of the chip, preceding the [label] text. When
 *   [selected] is true, this icon may visually indicate that the chip is selected (for example, via
 *   a checkmark icon).
 * @param trailingIcon optional icon at the end of the chip
 * @param colors [SelectableChipColors] that will be used to resolve the colors used for this chip
 *   in different states. See [MaterialFilterChipDefaults.filterChipColors].
 * @param elevation [SelectableChipElevation] used to resolve the elevation for this chip in
 *   different states. This controls the size of the shadow below the chip. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See [MaterialFilterChipDefaults.filterChipElevation].
 * @param border the border to draw around the container of this chip. Pass `null` for no border.
 *   See [MaterialFilterChipDefaults.filterChipBorder].
 * @param horizontalArrangement the horizontal arrangement of the chip's children. If there aren't
 *   icons, then the horizontal padding between the [label] and the [border] will be the sum of
 *   [contentPadding] and the spacing in this [horizontalArrangement].
 * @param contentPadding the padding around the content of this chip, including the [leadingIcon],
 *   [label], and [trailingIcon].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this chip. You can use this to change the chip's appearance or
 *   preview the chip in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@Composable
public fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    shapes: ChipShapes = FilterChipDefaults.shapes(),
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    colors: SelectableChipColors = MaterialFilterChipDefaults.tonalFilterChipColors(),
    elevation: SelectableChipElevation? = MaterialFilterChipDefaults.filterChipElevation(),
    border: BorderStroke? = MaterialFilterChipDefaults.filterChipBorder(enabled, selected),
    horizontalArrangement: Arrangement.Horizontal =
        MaterialFilterChipDefaults.horizontalArrangement(leadingIcon != null, trailingIcon != null),
    contentPadding: PaddingValues = MaterialFilterChipDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
): Unit =
    MaterialFilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        shapes = shapes,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        colors = colors,
        elevation = elevation,
        border = border,
        horizontalArrangement = horizontalArrangement,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
    )

/**
 * [Material Design elevated filter chip](https://m3.material.io/components/chips/overview)
 *
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts.
 *
 * Filter chips use tags or descriptive words to filter content. They can be a good alternative to
 * toggle buttons or checkboxes. It also morphs between the shapes provided in [shapes] depending on
 * the state of the chip as long as the shapes provided are [CornerBasedShape]s. If a shape in
 * [shapes] isn't a [CornerBasedShape], then the chip will change between the [ChipShapes] according
 * to user interaction.
 *
 * This filter chip is applied with an elevated style. If you want a flat style, use the
 * [FilterChip].
 *
 * Tapping on a filter chip toggles its selection state. A selection state [leadingIcon] can be
 * provided (e.g. a checkmark) to be appended at the starting edge of the chip's label.
 *
 * @param selected whether this chip is selected
 * @param onClick called when this chip is clicked
 * @param label text label for this chip
 * @param shapes the [ChipShapes] that this chip with morph between depending on the chip's selected
 *   state.
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param leadingIcon optional icon at the start of the chip, preceding the [label] text. When
 *   [selected] is true, this icon may visually indicate that the chip is selected (for example, via
 *   a checkmark icon).
 * @param trailingIcon optional icon at the end of the chip
 * @param colors [SelectableChipColors] that will be used to resolve the colors used for this chip
 *   in different states. See [MaterialFilterChipDefaults.elevatedFilterChipColors].
 * @param elevation [SelectableChipElevation] used to resolve the elevation for this chip in
 *   different states. This controls the size of the shadow below the chip. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See [MaterialFilterChipDefaults.filterChipElevation].
 * @param border the border to draw around the container of this chip. Pass `null` for no border.
 *   See [MaterialFilterChipDefaults.filterChipBorder].
 * @param horizontalArrangement the horizontal arrangement of the chip's children. If there aren't
 *   icons, then the horizontal padding between the [label] and the [border] will be the sum of
 *   [contentPadding] and the spacing in this [horizontalArrangement].
 * @param contentPadding the padding around the content of this chip, including the [leadingIcon],
 *   [label], and [trailingIcon].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this chip. You can use this to change the chip's appearance or
 *   preview the chip in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@Composable
public fun ElevatedFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    shapes: ChipShapes = FilterChipDefaults.shapes(),
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    colors: SelectableChipColors = MaterialFilterChipDefaults.tonalElevatedFilterChipColors(),
    elevation: SelectableChipElevation? = MaterialFilterChipDefaults.elevatedFilterChipElevation(),
    border: BorderStroke? = null,
    horizontalArrangement: Arrangement.Horizontal =
        MaterialFilterChipDefaults.horizontalArrangement(leadingIcon != null, trailingIcon != null),
    contentPadding: PaddingValues = MaterialFilterChipDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
): Unit =
    MaterialElevatedFilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        shapes = shapes,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        colors = colors,
        elevation = elevation,
        border = border,
        horizontalArrangement = horizontalArrangement,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
    )

/**
 * [Material Design input chip](https://m3.material.io/components/chips/overview)
 *
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts.
 *
 * Input chips represent discrete pieces of information entered by a user. It also morphs between
 * the shapes provided in [shapes] depending on the state of the chip as long as the shapes provided
 * are [CornerBasedShape]s. If a shape in [shapes] isn't a [CornerBasedShape], then the chip will
 * change between the [ChipShapes] according to user interaction.
 *
 * An Input Chip can have a leading icon or an avatar at its start. In case both are provided, the
 * avatar will take precedence and will be displayed.
 *
 * @param selected whether this chip is selected
 * @param onClick called when this chip is clicked
 * @param label text label for this chip
 * @param shapes the [ChipShapes] that this chip with morph between depending on the chip's selected
 *   state.
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param leadingIcon optional icon at the start of the chip, preceding the [label] text
 * @param avatar optional avatar at the start of the chip, preceding the [label] text
 * @param trailingIcon optional icon at the end of the chip
 * @param colors [ChipColors] that will be used to resolve the colors used for this chip in
 *   different states. See [MaterialInputChipDefaults.inputChipColors].
 * @param elevation [ChipElevation] used to resolve the elevation for this chip in different states.
 *   This controls the size of the shadow below the chip. Additionally, when the container color is
 *   [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 *   [MaterialInputChipDefaults.inputChipElevation].
 * @param border the border to draw around the container of this chip. Pass `null` for no border.
 *   See [MaterialInputChipDefaults.inputChipBorder].
 * @param horizontalArrangement the horizontal arrangement of the chip's children. If there aren't
 *   any icons, then the horizontal padding between the [label] and the [border] will be the sum of
 *   [contentPadding] and the spacing in this [horizontalArrangement].
 * @param contentPadding the padding around the content of this chip, including the [leadingIcon],
 *   [avatar], [label], and [trailingIcon].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this chip. You can use this to change the chip's appearance or
 *   preview the chip in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@Composable
public fun InputChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    shapes: ChipShapes = InputChipDefaults.shapes(),
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    avatar: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    colors: SelectableChipColors = MaterialInputChipDefaults.tonalInputChipColors(),
    elevation: SelectableChipElevation? = MaterialInputChipDefaults.inputChipElevation(),
    border: BorderStroke? = MaterialInputChipDefaults.inputChipBorder(enabled, selected),
    horizontalArrangement: Arrangement.Horizontal =
        MaterialInputChipDefaults.horizontalArrangement(
            avatar != null,
            leadingIcon != null,
            trailingIcon != null,
        ),
    contentPadding: PaddingValues =
        MaterialInputChipDefaults.contentPadding(
            avatar != null,
            leadingIcon != null,
            trailingIcon != null,
        ),
    interactionSource: MutableInteractionSource? = null,
): Unit =
    MaterialInputChip(
        selected = selected,
        onClick = onClick,
        label = label,
        shapes = shapes,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        avatar = avatar,
        trailingIcon = trailingIcon,
        colors = colors,
        elevation = elevation,
        border = border,
        horizontalArrangement = horizontalArrangement,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
    )

/** Theme-aware interaction shapes for filter chips. */
public object FilterChipDefaults {
    /**
     * Creates a [ChipShapes] that represents the default shape, pressed shape, and selected shape
     * used in a [FilterChip] and its variants.
     *
     * The selected shape defaults to the M3X full shape.
     *
     * @param shape the unchecked shape for [ChipShapes]
     * @param selectedShape the selected shape for [ChipShapes]
     * @param pressedShape the pressed shape for [ChipShapes]
     */
    @Composable
    public fun shapes(
        shape: Shape? = null,
        selectedShape: Shape? = null,
        pressedShape: Shape? = null,
    ): ChipShapes =
        MaterialFilterChipDefaults.shapes(
            shape = shape,
            selectedShape = selectedShape ?: MaterialTheme.extended.shapes.full,
            pressedShape = pressedShape,
        )
}

/** Theme-aware interaction shapes for input chips. */
public object InputChipDefaults {
    /**
     * Creates a [ChipShapes] that represents the default shape, pressed shape, and selected shape
     * used in an [InputChip] and its variants.
     *
     * The selected shape defaults to the M3X full shape.
     *
     * @param shape the unchecked shape for [ChipShapes]
     * @param selectedShape the selected shape for [ChipShapes]
     * @param pressedShape the pressed shape for [ChipShapes]
     */
    @Composable
    public fun shapes(
        shape: Shape? = null,
        selectedShape: Shape? = null,
        pressedShape: Shape? = null,
    ): ChipShapes =
        MaterialInputChipDefaults.shapes(
            shape = shape,
            selectedShape = selectedShape ?: MaterialTheme.extended.shapes.full,
            pressedShape = pressedShape,
        )
}
