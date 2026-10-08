package top.ltfan.m3x.material

import androidx.compose.animation.ExitTransition
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ChipShapes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilterChipDefaults as MaterialFilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SelectableChipElevation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import top.ltfan.m3x.compose.animation.AnimatedContentWithBlur
import top.ltfan.m3x.compose.ui.visual.LocalBlurEnabled
import top.ltfan.m3x.material.theme.extended

/**
 * A themed filter chip with an animated leading slot.
 *
 * The leading slot animates between [leadingSelectedIcon] (shown when selected) and
 * [leadingPlaceholderIcon] (shown when not selected). It is absent when both are `null`.
 *
 * [LocalBlurEnabled] disables blur while preserving the leading slot's fade transition.
 *
 * @param shapes the Expressive shapes used for interaction states.
 * @param elevation the Material elevation and surface tint treatment; [ColorScheme.surface]
 *   participates in the elevation overlay.
 * @param interactionSource observes and emits [Interaction]s; `null` uses an internal source.
 */
@Composable
public fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingPlaceholderIcon: Painter? = null,
    leadingSelectedIcon: Painter? = null,
    trailing: @Composable (() -> Unit)? = null,
    shapes: ChipShapes = FilterChipDefaults.shapes(),
    colors: SelectableChipColors = MaterialFilterChipDefaults.tonalFilterChipColors(),
    elevation: SelectableChipElevation? = MaterialFilterChipDefaults.filterChipElevation(),
    border: BorderStroke? = MaterialFilterChipDefaults.filterChipBorder(enabled, selected),
    horizontalArrangement: Arrangement.Horizontal =
        MaterialFilterChipDefaults.horizontalArrangement(
            leadingPlaceholderIcon != null || leadingSelectedIcon != null,
            trailing != null,
        ),
    contentPadding: PaddingValues = MaterialFilterChipDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
) {
    val motionScheme = MaterialTheme.motionScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                modifier = textModifier,
            )
        },
        modifier = modifier,
        enabled = enabled,
        // The platform chip shows no leading slot at all when there is no icon. Provide the slot
        // when at least one icon exists. An empty slot has zero width and leaves the label in
        // place. Omitting that slot avoids composing an AnimatedContent for every chip and follows
        // the platform chip's slot contract.
        leadingIcon =
            if (leadingSelectedIcon == null && leadingPlaceholderIcon == null) {
                null
            } else {
                {
                    AnimatedContentWithBlur(
                        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                        maxRadius = MaterialTheme.extended.enterExitBlurRadius,
                        targetState = selected,
                        transitionSpec = {
                            scaleIn(
                                animationSpec = motionScheme.fastSpatialSpec(),
                                initialScale = 0.92f,
                            ) togetherWith ExitTransition.None
                        },
                    ) { isSelected ->
                        if (isSelected) {
                            if (leadingSelectedIcon == null) return@AnimatedContentWithBlur
                            Icon(leadingSelectedIcon, contentDescription = null)
                        } else {
                            if (leadingPlaceholderIcon == null) return@AnimatedContentWithBlur
                            Icon(leadingPlaceholderIcon, contentDescription = null)
                        }
                    }
                }
            },
        trailingIcon = trailing,
        shapes = shapes,
        colors = colors,
        elevation = elevation,
        border = border,
        horizontalArrangement = horizontalArrangement,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
    )
}
