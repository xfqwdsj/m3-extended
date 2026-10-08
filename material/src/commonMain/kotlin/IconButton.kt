package top.ltfan.m3x.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.FilledIconButton as MaterialFilledIconButton
import androidx.compose.material3.FilledIconToggleButton as MaterialFilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton as MaterialFilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton as MaterialFilledTonalIconToggleButton
import androidx.compose.material3.IconButton as MaterialIconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults as MaterialIconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.IconToggleButton as MaterialIconToggleButton
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.IconToggleButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton as MaterialOutlinedIconButton
import androidx.compose.material3.OutlinedIconToggleButton as MaterialOutlinedIconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import top.ltfan.m3x.material.theme.extended

/** Theme-aware factories and wrappers for expressive icon buttons. */
public object IconButtonDefaults {
    /**
     * Returns icon button shapes with the M3X full shape as the resting shape.
     *
     * Corner-based shapes interpolate between states; other shape types switch directly.
     *
     * @param shape Optional resting shape; defaults to the extended theme's full shape.
     */
    @Composable
    public fun staticShapes(shape: Shape? = null): IconButtonShapes {
        val defaults = MaterialIconButtonDefaults.shapes()
        val resolved = shape ?: MaterialTheme.extended.shapes.full
        return remember(defaults, resolved) {
            defaults.copy(shape = resolved, pressedShape = resolved)
        }
    }

    /**
     * Returns icon button shapes with the M3X full shape as the resting shape.
     *
     * Corner-based shapes interpolate between states; other shape types switch directly.
     *
     * @param shape Optional resting shape; defaults to the extended theme's full shape.
     * @param pressedShape Optional pressed shape; otherwise Material's pressed shape is retained.
     */
    @Composable
    public fun shapes(shape: Shape? = null, pressedShape: Shape? = null): IconButtonShapes {
        val defaults = MaterialIconButtonDefaults.shapes()
        val resolved = shape ?: MaterialTheme.extended.shapes.full
        return remember(defaults, resolved, pressedShape) {
            defaults.copy(shape = resolved, pressedShape = pressedShape)
        }
    }

    /**
     * Returns toggle button shapes with M3X full shape at rest and Material interaction shapes.
     *
     * Corner-based shapes interpolate between states; other shape types switch directly.
     *
     * @param shape Optional unchecked shape; defaults to the extended theme's full shape.
     * @param pressedShape Optional pressed shape; otherwise Material's pressed shape is retained.
     * @param checkedShape Optional checked shape; otherwise Material's checked shape is retained.
     */
    @Composable
    public fun toggleableShapes(
        shape: Shape? = null,
        pressedShape: Shape? = null,
        checkedShape: Shape? = null,
    ): IconToggleButtonShapes {
        val defaults = MaterialIconButtonDefaults.toggleableShapes()
        val resolved = shape ?: MaterialTheme.extended.shapes.full
        return remember(defaults, resolved, pressedShape, checkedShape) {
            defaults.copy(
                shape = resolved,
                pressedShape = pressedShape,
                checkedShape = checkedShape,
            )
        }
    }
}

/**
 * An expressive icon button with a theme-aware resting shape. Corner-based shapes interpolate;
 * other shape types switch directly.
 *
 * @param onClick Action called when activated.
 * @param modifier Modifier applied to the button.
 * @param enabled Whether the button accepts input and is exposed as enabled to accessibility.
 * @param shapes Shapes used at rest and while pressed.
 * @param colors Colors used for its container and icon.
 * @param interactionSource Optional source for observing interactions; a null value uses an
 *   internal source.
 * @param content Icon content displayed by the button.
 */
@Composable
public fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shapes: IconButtonShapes = IconButtonDefaults.shapes(),
    enabled: Boolean = true,
    colors: IconButtonColors = MaterialIconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
): Unit = MaterialIconButton(onClick, shapes, modifier, enabled, colors, interactionSource, content)

/**
 * A filled expressive icon button with a theme-aware resting shape. Corner-based shapes
 * interpolate; other shape types switch directly.
 *
 * @param onClick Action called when activated.
 * @param modifier Modifier applied to the button.
 * @param enabled Whether the button accepts input and is exposed as enabled to accessibility.
 * @param shapes Shapes used at rest and while pressed.
 * @param colors Colors used for its container and icon.
 * @param interactionSource Optional source for observing interactions; a null value uses an
 *   internal source.
 * @param content Icon content displayed by the button.
 */
@Composable
public fun FilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shapes: IconButtonShapes = IconButtonDefaults.shapes(),
    enabled: Boolean = true,
    colors: IconButtonColors = MaterialIconButtonDefaults.filledIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
): Unit =
    MaterialFilledIconButton(onClick, shapes, modifier, enabled, colors, interactionSource, content)

/**
 * A filled tonal expressive icon button with a theme-aware resting shape. Corner-based shapes
 * interpolate; other shape types switch directly.
 *
 * @param onClick Action called when activated.
 * @param modifier Modifier applied to the button.
 * @param enabled Whether the button accepts input and is exposed as enabled to accessibility.
 * @param shapes Shapes used at rest and while pressed.
 * @param colors Colors used for its container and icon.
 * @param interactionSource Optional source for observing interactions; a null value uses an
 *   internal source.
 * @param content Icon content displayed by the button.
 */
@Composable
public fun FilledTonalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shapes: IconButtonShapes = IconButtonDefaults.shapes(),
    enabled: Boolean = true,
    colors: IconButtonColors = MaterialIconButtonDefaults.filledTonalIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
): Unit =
    MaterialFilledTonalIconButton(
        onClick,
        shapes,
        modifier,
        enabled,
        colors,
        interactionSource,
        content,
    )

/**
 * An outlined expressive icon button with a theme-aware resting shape. Corner-based shapes
 * interpolate; other shape types switch directly.
 *
 * @param onClick Action called when activated.
 * @param modifier Modifier applied to the button.
 * @param enabled Whether the button accepts input and is exposed as enabled to accessibility.
 * @param shapes Shapes used at rest and while pressed.
 * @param colors Colors used for its container and icon.
 * @param border Border drawn around the button; its default follows [enabled].
 * @param interactionSource Optional source for observing interactions; a null value uses an
 *   internal source.
 * @param content Icon content displayed by the button.
 */
@Composable
public fun OutlinedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shapes: IconButtonShapes = IconButtonDefaults.shapes(),
    enabled: Boolean = true,
    colors: IconButtonColors = MaterialIconButtonDefaults.outlinedIconButtonColors(),
    border: BorderStroke? = MaterialIconButtonDefaults.outlinedIconButtonBorder(enabled),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
): Unit =
    MaterialOutlinedIconButton(
        onClick,
        shapes,
        modifier,
        enabled,
        colors,
        border,
        interactionSource,
        content,
    )

/**
 * An expressive icon toggle button with a theme-aware unchecked shape. Corner-based shapes
 * interpolate; other shape types switch directly.
 *
 * @param checked Whether the button is selected.
 * @param onCheckedChange Called when selection changes.
 * @param modifier Modifier applied to the button.
 * @param enabled Whether the button accepts input and is exposed as enabled to accessibility.
 * @param shapes Shapes used at rest, while pressed, and when selected.
 * @param colors Colors used for its container and icon.
 * @param interactionSource Optional source for observing interactions; a null value uses an
 *   internal source.
 * @param content Icon content displayed by the button.
 */
@Composable
public fun IconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    shapes: IconToggleButtonShapes = IconButtonDefaults.toggleableShapes(),
    enabled: Boolean = true,
    colors: IconToggleButtonColors = MaterialIconButtonDefaults.iconToggleButtonVibrantColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
): Unit =
    MaterialIconToggleButton(
        checked,
        onCheckedChange,
        shapes,
        modifier,
        enabled,
        colors,
        interactionSource,
        content,
    )

/**
 * A filled expressive icon toggle button with a theme-aware unchecked shape. Corner-based shapes
 * interpolate; other shape types switch directly.
 *
 * @param checked Whether the button is selected.
 * @param onCheckedChange Called when selection changes.
 * @param modifier Modifier applied to the button.
 * @param enabled Whether the button accepts input and is exposed as enabled to accessibility.
 * @param shapes Shapes used at rest, while pressed, and when selected.
 * @param colors Colors used for its container and icon.
 * @param interactionSource Optional source for observing interactions; a null value uses an
 *   internal source.
 * @param content Icon content displayed by the button.
 */
@Composable
public fun FilledIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    shapes: IconToggleButtonShapes = IconButtonDefaults.toggleableShapes(),
    enabled: Boolean = true,
    colors: IconToggleButtonColors = MaterialIconButtonDefaults.filledIconToggleButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
): Unit =
    MaterialFilledIconToggleButton(
        checked,
        onCheckedChange,
        shapes,
        modifier,
        enabled,
        colors,
        interactionSource,
        content,
    )

/**
 * A filled tonal expressive icon toggle button with a theme-aware unchecked shape. Corner-based
 * shapes interpolate; other shape types switch directly.
 *
 * @param checked Whether the button is selected.
 * @param onCheckedChange Called when selection changes.
 * @param modifier Modifier applied to the button.
 * @param enabled Whether the button accepts input and is exposed as enabled to accessibility.
 * @param shapes Shapes used at rest, while pressed, and when selected.
 * @param colors Colors used for its container and icon.
 * @param interactionSource Optional source for observing interactions; a null value uses an
 *   internal source.
 * @param content Icon content displayed by the button.
 */
@Composable
public fun FilledTonalIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    shapes: IconToggleButtonShapes = IconButtonDefaults.toggleableShapes(),
    enabled: Boolean = true,
    colors: IconToggleButtonColors = MaterialIconButtonDefaults.filledTonalIconToggleButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
): Unit =
    MaterialFilledTonalIconToggleButton(
        checked,
        onCheckedChange,
        shapes,
        modifier,
        enabled,
        colors,
        interactionSource,
        content,
    )

/**
 * An outlined expressive icon toggle button with a theme-aware unchecked shape. Corner-based shapes
 * interpolate; other shape types switch directly.
 *
 * @param checked Whether the button is selected.
 * @param onCheckedChange Called when selection changes.
 * @param modifier Modifier applied to the button.
 * @param enabled Whether the button accepts input and is exposed as enabled to accessibility.
 * @param shapes Shapes used at rest, while pressed, and when selected.
 * @param colors Colors used for its container and icon.
 * @param border Border drawn around the button; its default depends on [enabled] and [checked].
 * @param interactionSource Optional source for observing interactions; a null value uses an
 *   internal source.
 * @param content Icon content displayed by the button.
 */
@Composable
public fun OutlinedIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    shapes: IconToggleButtonShapes = IconButtonDefaults.toggleableShapes(),
    enabled: Boolean = true,
    colors: IconToggleButtonColors =
        MaterialIconButtonDefaults.outlinedIconToggleButtonVibrantColors(),
    border: BorderStroke? =
        MaterialIconButtonDefaults.outlinedIconToggleButtonVibrantBorder(enabled, checked),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
): Unit =
    MaterialOutlinedIconToggleButton(
        checked,
        onCheckedChange,
        shapes,
        modifier,
        enabled,
        colors,
        border,
        interactionSource,
        content,
    )
