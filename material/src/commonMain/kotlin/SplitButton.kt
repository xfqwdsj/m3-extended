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
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonDefaults as MaterialSplitButtonDefaults
import androidx.compose.material3.SplitButtonShapes
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import top.ltfan.m3x.material.theme.extended

/** Theme-aware defaults and components for split buttons. */
public object SplitButtonDefaults {
    /** Shapes for a leading button, retaining the upstream inner corners. */
    @Composable
    public fun leadingButtonShapesFor(buttonHeight: Dp): SplitButtonShapes =
        adaptSplitShapes(MaterialSplitButtonDefaults.leadingButtonShapesFor(buttonHeight), true)

    /** Shapes for a trailing button, retaining the upstream inner corners. */
    @Composable
    public fun trailingButtonShapesFor(buttonHeight: Dp): SplitButtonShapes =
        adaptSplitShapes(MaterialSplitButtonDefaults.trailingButtonShapesFor(buttonHeight), false)

    /**
     * Create a default `leading` button that has the same visual as a Filled[Button]. To create a
     * `tonal`, `outlined`, or `elevated` version, the default value of [Button] params can be
     * passed in. For example, [ElevatedButton].
     *
     * The default text style for internal [Text] components will be set to [Typography.labelLarge].
     *
     * @param onClick called when the button is clicked
     * @param modifier the [Modifier] to be applied to this button.
     * @param enabled controls the enabled state of the split button. When `false`, this component
     *   will not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param shapes the [SplitButtonShapes] that the trailing button will morph between depending
     *   on the user's interaction with the button.
     * @param colors [ButtonColors] that will be used to resolve the colors for this button in
     *   different states. See [ButtonDefaults.buttonColors].
     * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
     *   states. This controls the size of the shadow below the button. See
     *   [ButtonElevation.shadowElevation].
     * @param border the border to draw around the container of this button
     * @param contentPadding the spacing values to apply internally between the container and the
     *   content
     * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
     *   emitting [Interaction]s for this button. You can use this to change the button's appearance
     *   or preview the button in different states. Note that if `null` is provided, interactions
     *   will still happen internally.
     * @param content the content for the button.
     */
    @Composable
    public fun LeadingButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        shapes: SplitButtonShapes =
            leadingButtonShapesFor(MaterialSplitButtonDefaults.SmallContainerHeight),
        colors: ButtonColors = ButtonDefaults.buttonColors(),
        elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
        border: BorderStroke? = null,
        contentPadding: PaddingValues =
            MaterialSplitButtonDefaults.leadingButtonContentPaddingFor(
                MaterialSplitButtonDefaults.SmallContainerHeight,
            ),
        interactionSource: MutableInteractionSource? = null,
        content: @Composable RowScope.() -> Unit,
    ): Unit =
        MaterialSplitButtonDefaults.LeadingButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shapes = shapes,
            colors = colors,
            elevation = elevation,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )

    /**
     * Creates a `trailing` button that has the same visual as a [Button].
     *
     * To create a `tonal`, `outlined`, or `elevated` version, the default value of [Button] params
     * can be passed in. For example, [ElevatedButton].
     *
     * The default text style for internal [Text] components will be set to [Typography.labelLarge].
     *
     * @param onClick called when the button is clicked
     * @param modifier the [Modifier] to be applied to this button.
     * @param enabled controls the enabled state of the split button. When `false`, this component
     *   will not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param shapes the [SplitButtonShapes] that the trailing button will morph between depending
     *   on the user's interaction with the button.
     * @param colors [ButtonColors] that will be used to resolve the colors for this button in
     *   different states. See [ButtonDefaults.buttonColors].
     * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
     *   states. This controls the size of the shadow below the button. See
     *   [ButtonElevation.shadowElevation].
     * @param border the border to draw around the container of this button
     * @param contentPadding the spacing values to apply internally between the container and the
     *   content
     * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
     *   emitting [Interaction]s for this button. You can use this to change the button's appearance
     *   or preview the button in different states. Note that if `null` is provided, interactions
     *   will still happen internally.
     * @param content the content to be placed in the button
     */
    @Composable
    public fun TrailingButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        shapes: SplitButtonShapes =
            trailingButtonShapesFor(MaterialSplitButtonDefaults.SmallContainerHeight),
        colors: ButtonColors = ButtonDefaults.buttonColors(),
        elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
        border: BorderStroke? = null,
        contentPadding: PaddingValues =
            MaterialSplitButtonDefaults.trailingButtonContentPaddingFor(
                MaterialSplitButtonDefaults.SmallContainerHeight,
            ),
        interactionSource: MutableInteractionSource? = null,
        content: @Composable RowScope.() -> Unit,
    ): Unit =
        MaterialSplitButtonDefaults.TrailingButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shapes = shapes,
            colors = colors,
            elevation = elevation,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )

    /**
     * Creates a `trailing` button that has the same visual as a [Button]. When [checked] is updated
     * from `false` to `true`, the buttons corners will morph to `full` by default. Pressed shape
     * and checked shape can be customized via [shapes] param.
     *
     * To create a `tonal`, `outlined`, or `elevated` version, the default value of [Button] params
     * can be passed in. For example, [ElevatedButton].
     *
     * The default text style for internal [Text] components will be set to [Typography.labelLarge].
     *
     * @param checked indicates whether the button is checked. This will trigger the corner morphing
     *   animation to reflect the updated state.
     * @param onCheckedChange called when the button is clicked
     * @param modifier the [Modifier] to be applied to this button.
     * @param enabled controls the enabled state of the split button. When `false`, this component
     *   will not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param shapes the [SplitButtonShapes] that the trailing button will morph between depending
     *   on the user's interaction with the button.
     * @param colors [ButtonColors] that will be used to resolve the colors for this button in
     *   different states. See [ButtonDefaults.buttonColors].
     * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
     *   states. This controls the size of the shadow below the button. See
     *   [ButtonElevation.shadowElevation].
     * @param border the border to draw around the container of this button
     * @param contentPadding the spacing values to apply internally between the container and the
     *   content
     * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
     *   emitting [Interaction]s for this button. You can use this to change the button's appearance
     *   or preview the button in different states. Note that if `null` is provided, interactions
     *   will still happen internally.
     * @param content the content to be placed in the button
     */
    @Composable
    public fun TrailingButton(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        shapes: SplitButtonShapes =
            trailingButtonShapesFor(MaterialSplitButtonDefaults.SmallContainerHeight),
        colors: ButtonColors = ButtonDefaults.buttonColors(),
        elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
        border: BorderStroke? = null,
        contentPadding: PaddingValues =
            MaterialSplitButtonDefaults.trailingButtonContentPaddingFor(
                MaterialSplitButtonDefaults.SmallContainerHeight,
            ),
        interactionSource: MutableInteractionSource? = null,
        content: @Composable RowScope.() -> Unit,
    ): Unit =
        MaterialSplitButtonDefaults.TrailingButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            shapes = shapes,
            colors = colors,
            elevation = elevation,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )

    /**
     * Create a tonal `leading` button that has the same visual as a Tonal[Button]. To create a
     * `filled`, `outlined`, or `elevated` version, the default value of [Button] params can be
     * passed in. For example, [ElevatedButton].
     *
     * The default text style for internal [Text] components will be set to [Typography.labelLarge].
     *
     * @param onClick called when the button is clicked
     * @param modifier the [Modifier] to be applied to this button.
     * @param enabled controls the enabled state of the split button. When `false`, this component
     *   will not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param shapes the [SplitButtonShapes] that the trailing button will morph between depending
     *   on the user's interaction with the button.
     * @param colors [ButtonColors] that will be used to resolve the colors for this button in
     *   different states. See [ButtonDefaults.buttonColors].
     * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
     *   states. This controls the size of the shadow below the button. See
     *   [ButtonElevation.shadowElevation].
     * @param border the border to draw around the container of this button
     * @param contentPadding the spacing values to apply internally between the container and the
     *   content
     * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
     *   emitting [Interaction]s for this button. You can use this to change the button's appearance
     *   or preview the button in different states. Note that if `null` is provided, interactions
     *   will still happen internally.
     * @param content the content for the button.
     */
    @Composable
    public fun TonalLeadingButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        shapes: SplitButtonShapes =
            leadingButtonShapesFor(MaterialSplitButtonDefaults.SmallContainerHeight),
        colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
        elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
        border: BorderStroke? = null,
        contentPadding: PaddingValues =
            MaterialSplitButtonDefaults.leadingButtonContentPaddingFor(
                MaterialSplitButtonDefaults.SmallContainerHeight,
            ),
        interactionSource: MutableInteractionSource? = null,
        content: @Composable RowScope.() -> Unit,
    ): Unit =
        MaterialSplitButtonDefaults.TonalLeadingButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shapes = shapes,
            colors = colors,
            elevation = elevation,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )

    /**
     * Creates a tonal `trailing` button that has the same visual as a [FilledTonalButton]. When
     * [checked] is updated from `false` to `true`, the buttons corners will morph to `full` by
     * default. Pressed shape and checked shape can be customized via [shapes] param.
     *
     * To create a `tonal`, `outlined`, or `elevated` version, the default value of [Button] params
     * can be passed in. For example, [ElevatedButton].
     *
     * The default text style for internal [Text] components will be set to [Typography.labelLarge].
     *
     * @param checked indicates whether the button is checked. This will trigger the corner morphing
     *   animation to reflect the updated state.
     * @param onCheckedChange called when the button is clicked
     * @param modifier the [Modifier] to be applied to this button.
     * @param enabled controls the enabled state of the split button. When `false`, this component
     *   will not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param shapes the [SplitButtonShapes] that the trailing button will morph between depending
     *   on the user's interaction with the button.
     * @param colors [ButtonColors] that will be used to resolve the colors for this button in
     *   different states. See [ButtonDefaults.buttonColors].
     * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
     *   states. This controls the size of the shadow below the button. See
     *   [ButtonElevation.shadowElevation].
     * @param border the border to draw around the container of this button
     * @param contentPadding the spacing values to apply internally between the container and the
     *   content
     * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
     *   emitting [Interaction]s for this button. You can use this to change the button's appearance
     *   or preview the button in different states. Note that if `null` is provided, interactions
     *   will still happen internally.
     * @param content the content to be placed in the button
     */
    @Composable
    public fun TonalTrailingButton(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        shapes: SplitButtonShapes =
            trailingButtonShapesFor(MaterialSplitButtonDefaults.SmallContainerHeight),
        colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
        elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
        border: BorderStroke? = null,
        contentPadding: PaddingValues =
            MaterialSplitButtonDefaults.trailingButtonContentPaddingFor(
                MaterialSplitButtonDefaults.SmallContainerHeight,
            ),
        interactionSource: MutableInteractionSource? = null,
        content: @Composable RowScope.() -> Unit,
    ): Unit =
        MaterialSplitButtonDefaults.TonalTrailingButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            shapes = shapes,
            colors = colors,
            elevation = elevation,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )

    /**
     * Create an elevated `leading button` that has the same visual as an [ElevatedButton]. To
     * create a `filled`, `outlined`, or `elevated` version, the default value of [Button] params
     * can be passed in. For example, [ElevatedButton].
     *
     * The default text style for internal [Text] components will be set to [Typography.labelLarge].
     *
     * @param onClick called when the button is clicked
     * @param modifier the [Modifier] to be applied to this button.
     * @param enabled controls the enabled state of the split button. When `false`, this component
     *   will not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param shapes the [SplitButtonShapes] that the trailing button will morph between depending
     *   on the user's interaction with the button.
     * @param colors [ButtonColors] that will be used to resolve the colors for this button in
     *   different states. See [ButtonDefaults.buttonColors].
     * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
     *   states. This controls the size of the shadow below the button. See
     *   [ButtonElevation.shadowElevation].
     * @param border the border to draw around the container of this button
     * @param contentPadding the spacing values to apply internally between the container and the
     *   content
     * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
     *   emitting [Interaction]s for this button. You can use this to change the button's appearance
     *   or preview the button in different states. Note that if `null` is provided, interactions
     *   will still happen internally.
     * @param content the content for the button.
     */
    @Composable
    public fun OutlinedLeadingButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        shapes: SplitButtonShapes =
            leadingButtonShapesFor(MaterialSplitButtonDefaults.SmallContainerHeight),
        colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
        elevation: ButtonElevation? = null,
        border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
        contentPadding: PaddingValues =
            MaterialSplitButtonDefaults.leadingButtonContentPaddingFor(
                MaterialSplitButtonDefaults.SmallContainerHeight,
            ),
        interactionSource: MutableInteractionSource? = null,
        content: @Composable RowScope.() -> Unit,
    ): Unit =
        MaterialSplitButtonDefaults.OutlinedLeadingButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shapes = shapes,
            colors = colors,
            elevation = elevation,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )

    /**
     * Creates an outlined `trailing button` that has the same visual as an [OutlinedButton]. When
     * [checked] is updated from `false` to `true`, the buttons corners will morph to `full` by
     * default. Pressed shape and checked shape can be customized via [shapes] param.
     *
     * To create a `tonal`, `outlined`, or `elevated` version, the default value of [Button] params
     * can be passed in. For example, [ElevatedButton].
     *
     * The default text style for internal [Text] components will be set to [Typography.labelLarge].
     *
     * @param checked indicates whether the button is checked. This will trigger the corner morphing
     *   animation to reflect the updated state.
     * @param onCheckedChange called when the button is clicked
     * @param modifier the [Modifier] to be applied to this button.
     * @param enabled controls the enabled state of the split button. When `false`, this component
     *   will not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param shapes the [SplitButtonShapes] that the trailing button will morph between depending
     *   on the user's interaction with the button.
     * @param colors [ButtonColors] that will be used to resolve the colors for this button in
     *   different states. See [ButtonDefaults.buttonColors].
     * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
     *   states. This controls the size of the shadow below the button. See
     *   [ButtonElevation.shadowElevation].
     * @param border the border to draw around the container of this button
     * @param contentPadding the spacing values to apply internally between the container and the
     *   content
     * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
     *   emitting [Interaction]s for this button. You can use this to change the button's appearance
     *   or preview the button in different states. Note that if `null` is provided, interactions
     *   will still happen internally.
     * @param content the content to be placed in the button
     */
    @Composable
    public fun OutlinedTrailingButton(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        shapes: SplitButtonShapes =
            trailingButtonShapesFor(MaterialSplitButtonDefaults.SmallContainerHeight),
        colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
        elevation: ButtonElevation? = null,
        border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
        contentPadding: PaddingValues =
            MaterialSplitButtonDefaults.trailingButtonContentPaddingFor(
                MaterialSplitButtonDefaults.SmallContainerHeight,
            ),
        interactionSource: MutableInteractionSource? = null,
        content: @Composable RowScope.() -> Unit,
    ): Unit =
        MaterialSplitButtonDefaults.OutlinedTrailingButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            shapes = shapes,
            colors = colors,
            elevation = elevation,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )

    /**
     * Create an elevated `leading button` that has the same visual as an [ElevatedButton]. To
     * create a `filled`, `outlined`, or `elevated` version, the default value of [Button] params
     * can be passed in. For example, [ElevatedButton].
     *
     * The default text style for internal [Text] components will be set to [Typography.labelLarge].
     *
     * @param onClick called when the button is clicked
     * @param modifier the [Modifier] to be applied to this button.
     * @param enabled controls the enabled state of the split button. When `false`, this component
     *   will not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param shapes the [SplitButtonShapes] that the trailing button will morph between depending
     *   on the user's interaction with the button.
     * @param colors [ButtonColors] that will be used to resolve the colors for this button in
     *   different states. See [ButtonDefaults.buttonColors].
     * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
     *   states. This controls the size of the shadow below the button. See
     *   [ButtonElevation.shadowElevation].
     * @param border the border to draw around the container of this button
     * @param contentPadding the spacing values to apply internally between the container and the
     *   content
     * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
     *   emitting [Interaction]s for this button. You can use this to change the button's appearance
     *   or preview the button in different states. Note that if `null` is provided, interactions
     *   will still happen internally.
     * @param content the content for the button.
     */
    @Composable
    public fun ElevatedLeadingButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        shapes: SplitButtonShapes =
            leadingButtonShapesFor(MaterialSplitButtonDefaults.SmallContainerHeight),
        colors: ButtonColors = ButtonDefaults.elevatedButtonColors(),
        elevation: ButtonElevation? = ButtonDefaults.elevatedButtonElevation(),
        border: BorderStroke? = null,
        contentPadding: PaddingValues =
            MaterialSplitButtonDefaults.leadingButtonContentPaddingFor(
                MaterialSplitButtonDefaults.SmallContainerHeight,
            ),
        interactionSource: MutableInteractionSource? = null,
        content: @Composable RowScope.() -> Unit,
    ): Unit =
        MaterialSplitButtonDefaults.ElevatedLeadingButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shapes = shapes,
            colors = colors,
            elevation = elevation,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )

    /**
     * Creates an elevated `trailing button` that has the same visual as an [ElevatedButton]. When
     * [checked] is updated from `false` to `true`, the buttons corners will morph to `full` by
     * default. Pressed shape and checked shape can be customized via [shapes] param.
     *
     * To create a `tonal`, `outlined`, or `elevated` version, the default value of [Button] params
     * can be passed in. For example, [ElevatedButton].
     *
     * The default text style for internal [Text] components will be set to [Typography.labelLarge].
     *
     * @param checked indicates whether the button is checked. This will trigger the corner morphing
     *   animation to reflect the updated state.
     * @param onCheckedChange called when the button is clicked
     * @param modifier the [Modifier] to be applied to this button.
     * @param enabled controls the enabled state of the split button. When `false`, this component
     *   will not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param shapes the [SplitButtonShapes] that the trailing button will morph between depending
     *   on the user's interaction with the button.
     * @param colors [ButtonColors] that will be used to resolve the colors for this button in
     *   different states. See [ButtonDefaults.buttonColors].
     * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
     *   states. This controls the size of the shadow below the button. See
     *   [ButtonElevation.shadowElevation].
     * @param border the border to draw around the container of this button
     * @param contentPadding the spacing values to apply internally between the container and the
     *   content
     * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
     *   emitting [Interaction]s for this button. You can use this to change the button's appearance
     *   or preview the button in different states. Note that if `null` is provided, interactions
     *   will still happen internally.
     * @param content the content to be placed in the button
     */
    @Composable
    public fun ElevatedTrailingButton(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        shapes: SplitButtonShapes =
            trailingButtonShapesFor(MaterialSplitButtonDefaults.SmallContainerHeight),
        colors: ButtonColors = ButtonDefaults.elevatedButtonColors(),
        elevation: ButtonElevation? = ButtonDefaults.elevatedButtonElevation(),
        border: BorderStroke? = null,
        contentPadding: PaddingValues =
            MaterialSplitButtonDefaults.trailingButtonContentPaddingFor(
                MaterialSplitButtonDefaults.SmallContainerHeight,
            ),
        interactionSource: MutableInteractionSource? = null,
        content: @Composable RowScope.() -> Unit,
    ): Unit =
        MaterialSplitButtonDefaults.ElevatedTrailingButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            shapes = shapes,
            colors = colors,
            elevation = elevation,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )
}

@Composable
private fun adaptSplitShapes(shapes: SplitButtonShapes, leading: Boolean): SplitButtonShapes {
    val theme = MaterialTheme.extended.shapes
    val full = theme.full as? CornerBasedShape
    fun adapt(shape: Shape?): Shape? {
        if (shape == null) return null
        if (shape !is CornerBasedShape) return theme.full
        return theme.factory.logical(
            if (leading) full?.topStart ?: shape.topStart else shape.topStart,
            if (!leading) full?.topEnd ?: shape.topEnd else shape.topEnd,
            if (!leading) full?.bottomEnd ?: shape.bottomEnd else shape.bottomEnd,
            if (leading) full?.bottomStart ?: shape.bottomStart else shape.bottomStart,
        )
    }
    return SplitButtonShapes(
        shape = requireNotNull(adapt(shapes.shape)),
        pressedShape = adapt(shapes.pressedShape),
        checkedShape = shapes.checkedShape?.let { theme.full },
    )
}
