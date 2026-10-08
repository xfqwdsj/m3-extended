package top.ltfan.m3x.material.overlay

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults as MaterialIconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.ltfan.m3x.material.IconButton
import top.ltfan.m3x.material.IconButtonDefaults

/**
 * High-level composable that implements an [IconButton] with a tooltip.
 *
 * If [contentDescription] is `null`, no tooltip will be shown.
 *
 * When calculating the position of the tooltip, it will first try to show it in the
 * [preferredTooltipPosition], but if there is not enough space, it will try to show it in the
 * opposite position.
 *
 * @param painter the icon.
 * @param contentDescription text used by accessibility services to describe what this icon does. If
 *   `null`, no tooltip will be shown.
 * @param modifier the [Modifier] to be applied to this layout node.
 * @param buttonEnabled controls the enabled state of the icon button. When `false`, the component
 *   will not respond to user input, and it will appear visually disabled and disabled to
 *   accessibility services. A disabled button does not show a tooltip.
 * @param buttonColors [IconButtonColors] that will be used to resolve the colors used for the icon
 *   button in different states. See [MaterialIconButtonDefaults.iconButtonColors].
 * @param buttonInteractionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for the icon button. You can use this to change the icon button's
 *   appearance or preview the icon button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param preferredTooltipPosition the preferred position of the tooltip relative to the icon
 *   button.
 * @param tooltipSpacing the spacing between the icon button and the tooltip.
 * @param buttonShapes the resting and pressed shapes animated by the icon button.
 * @param onClick called when the user clicks the button.
 */
@Composable
public fun IconButtonWithTooltip(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    buttonEnabled: Boolean = true,
    buttonColors: IconButtonColors = MaterialIconButtonDefaults.iconButtonColors(),
    buttonInteractionSource: MutableInteractionSource? = null,
    preferredTooltipPosition: TooltipPosition = TooltipPosition.Bottom,
    tooltipSpacing: Dp = 8.dp,
    buttonShapes: IconButtonShapes = IconButtonDefaults.shapes(),
    onClick: () -> Unit,
) {
    OverlayTooltipBox(
        positionProvider =
            rememberOverlayTooltipPositionProvider(
                positioning =
                    when (preferredTooltipPosition) {
                        TooltipPosition.Top -> OverlayTooltipAnchorPosition.Above
                        TooltipPosition.Bottom -> OverlayTooltipAnchorPosition.Below
                    },
                spacingBetweenTooltipAndAnchor = tooltipSpacing,
            ),
        tooltip = {
            contentDescription?.let { OverlayPlainTooltip { Text(it) } }
        },
        state = rememberOverlayTooltipState(),
        modifier = modifier,
        // A plain tooltip has no action and never takes the focus: it is announced to accessibility
        // services without moving the focus away from the button.
        // A disabled button does not show a tooltip: the caller that wants to explain why a control
        // is disabled provides that explanation itself instead of it appearing as user input.
        enableUserInput = buttonEnabled && contentDescription != null,
    ) {
        IconButton(
            onClick = onClick,
            shapes = buttonShapes,
            enabled = buttonEnabled,
            colors = buttonColors,
            interactionSource = buttonInteractionSource,
            content = {
                Icon(painter, contentDescription)
            },
        )
    }
}

public enum class TooltipPosition {
    Top,
    Bottom,
}
