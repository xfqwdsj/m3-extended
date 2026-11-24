/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.ButtonDefaults as MaterialButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ElevatedToggleButton as MaterialElevatedToggleButton
import androidx.compose.material3.ElevatedToggleButtonDefaults as MaterialElevatedToggleButtonDefaults
import androidx.compose.material3.FilledTonalToggleButton as MaterialTonalToggleButton
import androidx.compose.material3.FilledTonalToggleButtonDefaults as MaterialTonalToggleButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedToggleButton as MaterialOutlinedToggleButton
import androidx.compose.material3.OutlinedToggleButtonDefaults as MaterialOutlinedToggleButtonDefaults
import androidx.compose.material3.ToggleButton as MaterialToggleButton
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ToggleButtonDefaults as MaterialToggleButtonDefaults
import androidx.compose.material3.ToggleButtonElevation
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.ToggleButtonSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import top.ltfan.m3x.material.theme.extended

/**
 * Toggle button is a toggleable button that switches between primary and tonal colors depending on
 * [checked]'s value. It also morphs between the three shapes provided in [shapes] depending on the
 * state of the interaction with the toggle button as long as the three shapes provided are
 * [CornerBasedShape]s. If a shape in [shapes] isn't a [CornerBasedShape], then toggle button will
 * toggle between the [ToggleButtonShapes] according to user interaction.
 *
 * see [Button] for a static button that doesn't need to be toggled. see [IconToggleButton] for a
 * toggleable button where the content is specifically an [Icon].
 *
 * [ToggleButton] uses the small button design by default.
 *
 * @param checked whether the toggle button is toggled on or off.
 * @param onCheckedChange called when the toggle button is clicked.
 * @param modifier the [Modifier] to be applied to the toggle button.
 * @param buttonSize controls the height, typography, padding, and icon sizing.
 * @param icon optional leading icon sized and spaced by Material.
 * @param enabled controls the enabled state of this toggle button. When `false`, this component
 *   will not respond to user input, and it will appear visually disabled and disabled to
 *   accessibility services.
 * @param shapes the [ToggleButtonShapes] that the toggle button will morph between depending on the
 *   user's interaction with the toggle button.
 * @param colors [ToggleButtonColors] that will be used to resolve the colors used for this toggle
 *   button in different states. See [MaterialToggleButtonDefaults.colors].
 * @param elevation [ToggleButtonElevation] used to resolve the elevation for this button in
 *   different states. This controls the size of the shadow below the button. See
 *   [ToggleButtonElevation.shadowElevation]. Additionally, when the container color is
 *   [ColorScheme.surface], this controls the amount of primary color applied as an overlay.
 * @param border the border to draw around the container of this toggle button.
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this toggle button. You can use this to change the toggle button's
 *   appearance or preview the toggle button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content The content displayed on the toggle button, expected to be text, icon or image.
 */
@Composable
public fun ToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: ToggleButtonSize = MaterialToggleButtonDefaults.size,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    shapes: ToggleButtonShapes = ToggleButtonDefaults.shapesFor(buttonSize),
    colors: ToggleButtonColors = MaterialToggleButtonDefaults.colors(),
    elevation: ToggleButtonElevation? = MaterialToggleButtonDefaults.elevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues =
        MaterialToggleButtonDefaults.contentPaddingFor(buttonSize, hasStartIcon = icon != null),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
): Unit =
    MaterialToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        buttonSize = buttonSize,
        enabled = enabled,
        icon = icon,
        shapes = shapes,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )

/**
 * Toggle button is a toggleable button that switches between primary and tonal colors depending on
 * [checked]'s value. It also morphs between the three shapes provided in [shapes] depending on the
 * state of the interaction with the toggle button as long as the three shapes provided are
 * [CornerBasedShape]s. If a shape in [shapes] isn't a [CornerBasedShape], then toggle button will
 * toggle between the [ToggleButtonShapes] according to user interaction.
 *
 * Elevated toggle buttons are high-emphasis Toggle buttons. To prevent shadow creep, only use them
 * when absolutely necessary, such as when the toggle button requires visual separation from
 * patterned container.
 *
 * see [ElevatedButton] for a static button that doesn't need to be toggled.
 *
 * @param checked whether the toggle button is toggled on or off.
 * @param onCheckedChange called when the toggle button is clicked.
 * @param modifier the [Modifier] to be applied to the toggle button.
 * @param buttonSize controls the height, typography, padding, and icon sizing.
 * @param icon optional leading icon sized and spaced by Material.
 * @param enabled controls the enabled state of this toggle button. When `false`, this component
 *   will not respond to user input, and it will appear visually disabled and disabled to
 *   accessibility services.
 * @param shapes the [ToggleButtonShapes] that the toggle button will morph between depending on the
 *   user's interaction with the toggle button.
 * @param colors [ToggleButtonColors] that will be used to resolve the colors used for this toggle
 *   button in different states. See [MaterialElevatedToggleButtonDefaults.colors].
 * @param elevation [ToggleButtonElevation] used to resolve the elevation for this button in
 *   different states. This controls the size of the shadow below the button. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay.
 * @param border the border to draw around the container of this toggle button.
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this toggle button. You can use this to change the toggle button's
 *   appearance or preview the toggle button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content The content displayed on the toggle button, expected to be text, icon or image.
 */
@Composable
public fun ElevatedToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: ToggleButtonSize = MaterialToggleButtonDefaults.size,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    shapes: ToggleButtonShapes = ToggleButtonDefaults.shapesFor(buttonSize),
    colors: ToggleButtonColors = MaterialElevatedToggleButtonDefaults.colors(),
    elevation: ToggleButtonElevation? = MaterialElevatedToggleButtonDefaults.elevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues =
        MaterialToggleButtonDefaults.contentPaddingFor(buttonSize, hasStartIcon = icon != null),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
): Unit =
    MaterialElevatedToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        buttonSize = buttonSize,
        enabled = enabled,
        icon = icon,
        shapes = shapes,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )

/**
 * Toggle button is a toggleable button that switches between primary and tonal colors depending on
 * [checked]'s value. It also morphs between the three shapes provided in [shapes] depending on the
 * state of the interaction with the toggle button as long as the three shapes provided are
 * [CornerBasedShape]s. If a shape in [shapes] isn't a [CornerBasedShape], then toggle button will
 * toggle between the [ToggleButtonShapes] according to user interaction.
 *
 * tonal toggle buttons are medium-emphasis buttons that is an alternative middle ground between
 * default [ToggleButton]s (filled) and [OutlinedToggleButton]s. They can be used in contexts where
 * lower-priority button requires slightly more emphasis than an outline would give. Tonal toggle
 * buttons use the secondary color mapping.
 *
 * see [FilledTonalButton] for a static button that doesn't need to be toggled. see
 * [FilledTonalIconToggleButton] for a toggleable button where the content is specifically an
 * [Icon].
 *
 * @param checked whether the toggle button is toggled on or off.
 * @param onCheckedChange called when the toggle button is clicked.
 * @param modifier the [Modifier] to be applied to the toggle button.
 * @param buttonSize controls the height, typography, padding, and icon sizing.
 * @param icon optional leading icon sized and spaced by Material.
 * @param enabled controls the enabled state of this toggle button. When `false`, this component
 *   will not respond to user input, and it will appear visually disabled and disabled to
 *   accessibility services.
 * @param shapes the [ToggleButtonShapes] that the toggle button will morph between depending on the
 *   user's interaction with the toggle button.
 * @param colors [ToggleButtonColors] that will be used to resolve the colors used for this toggle
 *   button in different states. See [MaterialTonalToggleButtonDefaults.colors].
 * @param elevation [ToggleButtonElevation] used to resolve the elevation for this button in
 *   different states. This controls the size of the shadow below the button. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay.
 * @param border the border to draw around the container of this toggle button.
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this toggle button. You can use this to change the toggle button's
 *   appearance or preview the toggle button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content The content displayed on the toggle button, expected to be text, icon or image.
 */
@Composable
public fun FilledTonalToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: ToggleButtonSize = MaterialToggleButtonDefaults.size,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    shapes: ToggleButtonShapes = ToggleButtonDefaults.shapesFor(buttonSize),
    colors: ToggleButtonColors = MaterialTonalToggleButtonDefaults.colors(),
    elevation: ToggleButtonElevation? = MaterialTonalToggleButtonDefaults.elevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues =
        MaterialToggleButtonDefaults.contentPaddingFor(buttonSize, hasStartIcon = icon != null),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
): Unit =
    MaterialTonalToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        buttonSize = buttonSize,
        enabled = enabled,
        icon = icon,
        shapes = shapes,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )

/**
 * Toggle button is a toggleable button that switches between primary and tonal colors depending on
 * [checked]'s value. It also morphs between the three shapes provided in [shapes] depending on the
 * state of the interaction with the toggle button as long as the three shapes provided are
 * [CornerBasedShape]s. If a shape in [shapes] isn't a [CornerBasedShape], then toggle button will
 * toggle between the [ToggleButtonShapes] according to user interaction.
 *
 * Outlined toggle buttons are medium-emphasis buttons. They contain actions that are important, but
 * are not the primary action in an app. Outlined buttons pair well with [ToggleButton]s to indicate
 * an alternative, secondary action.
 *
 * see [OutlinedButton] for a static button that doesn't need to be toggled. see
 * [OutlinedIconToggleButton] for a toggleable button where the content is specifically an [Icon].
 *
 * @param checked whether the toggle button is toggled on or off.
 * @param onCheckedChange called when the toggle button is clicked.
 * @param modifier the [Modifier] to be applied to the toggle button.
 * @param buttonSize controls the height, typography, padding, and icon sizing.
 * @param icon optional leading icon sized and spaced by Material.
 * @param enabled controls the enabled state of this toggle button. When `false`, this component
 *   will not respond to user input, and it will appear visually disabled and disabled to
 *   accessibility services.
 * @param shapes the [ToggleButtonShapes] that the toggle button will morph between depending on the
 *   user's interaction with the toggle button.
 * @param colors [ToggleButtonColors] that will be used to resolve the colors used for this toggle
 *   button in different states. See [MaterialOutlinedToggleButtonDefaults.colors].
 * @param elevation [ToggleButtonElevation] used to resolve the elevation for this button in
 *   different states. This controls the size of the shadow below the button. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay.
 * @param border the border to draw around the container of this toggle button.
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this toggle button. You can use this to change the toggle button's
 *   appearance or preview the toggle button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content The content displayed on the toggle button, expected to be text, icon or image.
 */
@Composable
public fun OutlinedToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: ToggleButtonSize = MaterialToggleButtonDefaults.size,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    shapes: ToggleButtonShapes = ToggleButtonDefaults.shapesFor(buttonSize),
    colors: ToggleButtonColors = MaterialOutlinedToggleButtonDefaults.colors(),
    elevation: ToggleButtonElevation? = null,
    border: BorderStroke? =
        if (!checked) MaterialButtonDefaults.outlinedButtonBorder(enabled) else null,
    contentPadding: PaddingValues =
        MaterialToggleButtonDefaults.contentPaddingFor(buttonSize, hasStartIcon = icon != null),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
): Unit =
    MaterialOutlinedToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        buttonSize = buttonSize,
        enabled = enabled,
        icon = icon,
        shapes = shapes,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )

/** Theme-aware interaction shapes for toggle buttons. */
public object ToggleButtonDefaults {
    /**
     * Creates a [ToggleButtonShapes] that represents the default shape, pressedShape, and
     * checkedShape used in a [ToggleButton] and its variants.
     *
     * The resting shape defaults to the M3X full shape.
     *
     * @param shape the unchecked shape for [ToggleButtonShapes]
     * @param pressedShape the pressed shape for [ToggleButtonShapes]
     * @param checkedShape the checked shape for [ToggleButtonShapes]
     */
    @Composable
    public fun shapes(
        shape: Shape? = null,
        pressedShape: Shape? = null,
        checkedShape: Shape? = null,
    ): ToggleButtonShapes =
        MaterialToggleButtonDefaults.shapesFor(MaterialToggleButtonDefaults.size)
            .copy(
                shape = shape ?: MaterialTheme.extended.shapes.full,
                pressedShape = pressedShape,
                checkedShape = checkedShape,
            )

    /** Shapes for a Material size, retaining the M3X resting shape. */
    @Composable
    public fun shapesFor(buttonSize: ToggleButtonSize): ToggleButtonShapes =
        MaterialToggleButtonDefaults.shapesFor(buttonSize)
            .copy(shape = MaterialTheme.extended.shapes.full)

    /**
     * Recommended [ToggleButtonShapes] for a provided toggle button height.
     *
     * The resting shape uses the M3X full shape.
     *
     * @param buttonHeight The height of the button
     */
    @Composable
    public fun shapesFor(buttonHeight: Dp): ToggleButtonShapes =
        MaterialToggleButtonDefaults.shapesFor(buttonHeight)
            .copy(shape = MaterialTheme.extended.shapes.full)
}
