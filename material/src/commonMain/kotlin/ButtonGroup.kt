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

import androidx.annotation.FloatRange
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.ButtonDefaults as MaterialButtonDefaults
import androidx.compose.material3.ButtonGroup as MaterialButtonGroup
import androidx.compose.material3.ButtonGroupDefaults as MaterialButtonGroupDefaults
import androidx.compose.material3.ButtonGroupMenuState
import androidx.compose.material3.ButtonGroupScope as MaterialButtonGroupScope
import androidx.compose.material3.DropdownMenuItem as MaterialDropdownMenuItem
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import top.ltfan.m3x.material.theme.extended

/**
 * Arranges buttons horizontally and moves overflowing items into a dropdown menu.
 *
 * Children using [ButtonGroupScope.animateWidth] expand when pressed and compress their neighbors.
 * Its compression limit bounds how much width each child can give up. Use
 * [ButtonGroupScope.clickableItem] for standard items, or [ButtonGroupScope.customItem] to supply
 * custom content.
 *
 * @param overflowIndicator composable that is displayed at the end of the button group if it needs
 *   to overflow. It receives a [ButtonGroupMenuState].
 * @param modifier the [Modifier] to be applied to the button group.
 * @param expandedRatio the percentage, represented by a float, of the width of the interacted child
 *   element that will be used to expand the interacted child element as well as compress the
 *   neighboring children. By Default, standard button group will expand the interacted child
 *   element by [MaterialButtonGroupDefaults.ExpandedRatio] of its width and this will be propagated
 *   to its neighbors. If 0f is passed into this slot, then the interacted child element will not
 *   expand at all and the neighboring elements will not compress. If 1f is passed into this slot,
 *   then the interacted child element will expand to 200% of its default width when pressed.
 * @param horizontalArrangement The horizontal arrangement of the button group's children.
 * @param verticalAlignment The vertical alignment of the button group's children.
 * @param content the content displayed in the button group, expected to use a composable that is
 *   tagged with [ButtonGroupScope.animateWidth].
 */
@Composable
public fun ButtonGroup(
    overflowIndicator: @Composable (ButtonGroupMenuState) -> Unit,
    modifier: Modifier = Modifier,
    @FloatRange(0.0) expandedRatio: Float = MaterialButtonGroupDefaults.ExpandedRatio,
    horizontalArrangement: Arrangement.Horizontal =
        MaterialButtonGroupDefaults.HorizontalArrangement,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: ButtonGroupScope.() -> Unit,
): Unit =
    MaterialButtonGroup(
        overflowIndicator = overflowIndicator,
        modifier = modifier,
        expandedRatio = expandedRatio,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        content = { ButtonGroupScope(this).content() },
    )

/** Theme-aware content scope for a button group. */
public class ButtonGroupScope internal constructor(private val delegate: MaterialButtonGroupScope) {
    public fun Modifier.weight(weight: Float): Modifier = with(delegate) { weight(weight) }

    public fun Modifier.align(alignment: Alignment.Vertical): Modifier =
        with(delegate) { align(alignment) }

    public fun Modifier.animateWidth(interactionSource: InteractionSource): Modifier =
        with(delegate) { animateWidth(interactionSource) }

    public fun Modifier.animateWidth(
        interactionSource: InteractionSource,
        compressionLimit: Dp,
    ): Modifier = with(delegate) { animateWidth(interactionSource, compressionLimit) }

    public fun customItem(
        buttonGroupContent: @Composable () -> Unit,
        menuContent: @Composable (ButtonGroupMenuState) -> Unit,
    ): Unit = delegate.customItem(buttonGroupContent, menuContent)

    public fun clickableItem(
        onClick: () -> Unit,
        label: String,
        icon: (@Composable () -> Unit)? = null,
        weight: Float = Float.NaN,
        enabled: Boolean = true,
    ) {
        addItem(null, onClick, label, icon, weight, enabled)
    }

    public fun toggleableItem(
        checked: Boolean,
        label: String,
        onCheckedChange: (Boolean) -> Unit,
        icon: (@Composable () -> Unit)? = null,
        weight: Float = Float.NaN,
        enabled: Boolean = true,
    ) {
        addItem(checked, { onCheckedChange(!checked) }, label, icon, weight, enabled)
    }

    private fun addItem(
        checked: Boolean?,
        onClick: () -> Unit,
        label: String,
        icon: (@Composable () -> Unit)? = null,
        weight: Float = Float.NaN,
        enabled: Boolean = true,
    ) {
        delegate.customItem(
            buttonGroupContent = {
                val source = remember { MutableInteractionSource() }
                val padding =
                    if (icon != null) MaterialButtonDefaults.ButtonWithIconContentPadding
                    else MaterialButtonDefaults.ContentPadding
                val modifier =
                    with(delegate) {
                        Modifier.animateWidth(
                                source,
                                padding.calculateLeftPadding(LayoutDirection.Ltr) +
                                    padding.calculateRightPadding(LayoutDirection.Ltr),
                            )
                            .then(
                                if (weight.isNaN()) Modifier
                                else Modifier.weight(weight.coerceAtMost(Float.MAX_VALUE)),
                            )
                    }
                val content: @Composable RowScope.() -> Unit = {
                    icon?.let {
                        it()
                        Spacer(Modifier.size(MaterialButtonDefaults.IconSpacing))
                    }
                    Text(label, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
                }
                if (checked == null)
                    Button(
                        onClick,
                        modifier,
                        enabled,
                        contentPadding = padding,
                        interactionSource = source,
                        content = content,
                    )
                else
                    ToggleButton(
                        checked,
                        { onClick() },
                        modifier = modifier,
                        enabled = enabled,
                        contentPadding = padding,
                        interactionSource = source,
                        content = content,
                    )
            },
            menuContent = { state ->
                MaterialDropdownMenuItem(
                    text = { Text(label) },
                    leadingIcon = icon,
                    enabled = enabled,
                    onClick = {
                        onClick()
                        state.dismiss()
                    },
                )
            },
        )
    }
}

/** Theme-aware shapes and overflow controls for expressive button groups. */
public object ButtonGroupDefaults {
    /** Connected leading shapes with the upstream inner corners. */
    @Composable
    public fun connectedLeadingButtonShapes(
        shape: Shape? = null,
        pressedShape: Shape? = null,
        checkedShape: Shape? = null,
    ): ToggleButtonShapes {
        val upstream = MaterialButtonGroupDefaults.connectedLeadingButtonShapes()
        return upstream.copy(
            shape = shape ?: themedConnectedShape(upstream.shape, true),
            pressedShape = pressedShape ?: themedConnectedShape(upstream.pressedShape, true),
            checkedShape = checkedShape ?: MaterialTheme.extended.shapes.full,
        )
    }

    /** Connected middle shapes with the upstream interaction states. */
    @Composable
    public fun connectedMiddleButtonShapes(
        shape: Shape? = null,
        pressedShape: Shape? = null,
        checkedShape: Shape? = null,
    ): ToggleButtonShapes {
        val upstream = MaterialButtonGroupDefaults.connectedMiddleButtonShapes()
        return upstream.copy(
            shape = shape ?: themedConnectedShape(upstream.shape, null),
            pressedShape = pressedShape ?: themedConnectedShape(upstream.pressedShape, null),
            checkedShape = checkedShape ?: MaterialTheme.extended.shapes.full,
        )
    }

    /** Connected trailing shapes with the upstream inner corners. */
    @Composable
    public fun connectedTrailingButtonShapes(
        shape: Shape? = null,
        pressedShape: Shape? = null,
        checkedShape: Shape? = null,
    ): ToggleButtonShapes {
        val upstream = MaterialButtonGroupDefaults.connectedTrailingButtonShapes()
        return upstream.copy(
            shape = shape ?: themedConnectedShape(upstream.shape, false),
            pressedShape = pressedShape ?: themedConnectedShape(upstream.pressedShape, false),
            checkedShape = checkedShape ?: MaterialTheme.extended.shapes.full,
        )
    }

    /**
     * Default overflow indicator for [ButtonGroup]. It uses a [FilledIconButton]. When clicked it
     * will open the menu associated with the provided [ButtonGroupMenuState].
     *
     * @param menuState the [ButtonGroupMenuState] used to show or dismiss the overflow menu.
     * @param modifier [Modifier] to be applied to the overflow indicator
     * @param enabled controls the enabled state of this icon button. When `false`, this component
     *   will not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param shape defines the shape of this icon button's container
     * @param colors [IconButtonColors] that will be used to resolve the colors used for this icon
     *   button in different states. See [IconButtonDefaults.filledIconButtonColors].
     * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
     *   emitting [Interaction]s for this icon button. You can use this to change the icon button's
     *   appearance or preview the icon button in different states. Note that if `null` is provided,
     *   interactions will still happen internally.
     */
    @Composable
    public fun OverflowIndicator(
        menuState: ButtonGroupMenuState,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        shape: Shape = MaterialTheme.extended.shapes.full,
        colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
        interactionSource: MutableInteractionSource? = null,
    ): Unit =
        MaterialButtonGroupDefaults.OverflowIndicator(
            menuState = menuState,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = colors,
            interactionSource = interactionSource,
        )
}

@Composable
private fun themedConnectedShape(shape: Shape, leading: Boolean?): Shape {
    val theme = MaterialTheme.extended.shapes
    val full = theme.full as? CornerBasedShape
    return if (shape is CornerBasedShape)
        theme.factory.logical(
            if (leading == true) full?.topStart ?: shape.topStart else shape.topStart,
            if (leading == false) full?.topEnd ?: shape.topEnd else shape.topEnd,
            if (leading == false) full?.bottomEnd ?: shape.bottomEnd else shape.bottomEnd,
            if (leading == true) full?.bottomStart ?: shape.bottomStart else shape.bottomStart,
        )
    else theme.full
}
