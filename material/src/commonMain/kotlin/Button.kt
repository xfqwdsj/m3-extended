package top.ltfan.m3x.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults as MaterialButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ElevatedButton as MaterialElevatedButton
import androidx.compose.material3.FilledTonalButton as MaterialFilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton as MaterialOutlinedButton
import androidx.compose.material3.TextButton as MaterialTextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import top.ltfan.m3x.material.theme.extended

/** Expressive button shapes whose default full shape follows the M3X theme. */
public object ButtonDefaults {
    /**
     * Creates button shapes using the theme's full shape by default.
     *
     * @param shape Optional resting shape; defaults to the extended theme's full shape.
     * @param pressedShape Optional pressed shape; otherwise the Material default is retained.
     */
    @Composable
    public fun shapes(shape: Shape? = null, pressedShape: Shape? = null): ButtonShapes {
        val defaults = MaterialButtonDefaults.shapes()
        val resolved = shape ?: MaterialTheme.extended.shapes.full
        return remember(defaults, resolved, pressedShape) {
            defaults.copy(shape = resolved, pressedShape = pressedShape)
        }
    }
}

/**
 * A filled expressive button whose default shapes use the M3X theme's full shape. Corner-based
 * shapes interpolate between states; other shape types switch directly.
 *
 * @param onClick Called when the button is activated.
 * @param modifier Modifier applied to the button.
 * @param enabled Whether the button accepts input and is exposed as enabled to accessibility.
 * @param shapes Shapes used for the button and its pressed state.
 * @param colors Colors used for the button's container and content.
 * @param elevation Elevation values used across interaction states.
 * @param border Optional border drawn around the button.
 * @param contentPadding Space around the button's content.
 * @param interactionSource Optional source for observing interactions; a null value uses an
 *   internal source.
 * @param content Content placed inside the button's row.
 */
@Composable
public fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: ButtonShapes = ButtonDefaults.shapes(),
    colors: ButtonColors = MaterialButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = MaterialButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues =
        MaterialButtonDefaults.contentPaddingFor(MaterialButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    MaterialButton(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * An elevated expressive button whose default shapes use the M3X theme's full shape. Corner-based
 * shapes interpolate between states; other shape types switch directly.
 *
 * @param onClick Called when the button is activated.
 * @param modifier Modifier applied to the button.
 * @param enabled Whether the button accepts input and is exposed as enabled to accessibility.
 * @param shapes Shapes used for the button and its pressed state.
 * @param colors Colors used for the button's container and content.
 * @param elevation Elevation values used across interaction states.
 * @param border Optional border drawn around the button.
 * @param contentPadding Space around the button's content.
 * @param interactionSource Optional source for observing interactions; a null value uses an
 *   internal source.
 * @param content Content placed inside the button's row.
 */
@Composable
public fun ElevatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: ButtonShapes = ButtonDefaults.shapes(),
    colors: ButtonColors = MaterialButtonDefaults.elevatedButtonColors(),
    elevation: ButtonElevation? = MaterialButtonDefaults.elevatedButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues =
        MaterialButtonDefaults.contentPaddingFor(MaterialButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    MaterialElevatedButton(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * A filled tonal expressive button whose default shapes use the M3X theme's full shape.
 * Corner-based shapes interpolate between states; other shape types switch directly.
 *
 * @param onClick Called when the button is activated.
 * @param modifier Modifier applied to the button.
 * @param enabled Whether the button accepts input and is exposed as enabled to accessibility.
 * @param shapes Shapes used for the button and its pressed state.
 * @param colors Colors used for the button's container and content.
 * @param elevation Elevation values used across interaction states.
 * @param border Optional border drawn around the button.
 * @param contentPadding Space around the button's content.
 * @param interactionSource Optional source for observing interactions; a null value uses an
 *   internal source.
 * @param content Content placed inside the button's row.
 */
@Composable
public fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: ButtonShapes = ButtonDefaults.shapes(),
    colors: ButtonColors = MaterialButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = MaterialButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues =
        MaterialButtonDefaults.contentPaddingFor(MaterialButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    MaterialFilledTonalButton(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * An outlined expressive button whose default shapes use the M3X theme's full shape. Corner-based
 * shapes interpolate between states; other shape types switch directly.
 *
 * @param onClick Called when the button is activated.
 * @param modifier Modifier applied to the button.
 * @param enabled Whether the button accepts input and is exposed as enabled to accessibility.
 * @param shapes Shapes used for the button and its pressed state.
 * @param colors Colors used for the button's container and content.
 * @param elevation Elevation values used across interaction states.
 * @param border Optional border; by default it reflects [enabled].
 * @param contentPadding Space around the button's content.
 * @param interactionSource Optional source for observing interactions; a null value uses an
 *   internal source.
 * @param content Content placed inside the button's row.
 */
@Composable
public fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: ButtonShapes = ButtonDefaults.shapes(),
    colors: ButtonColors = MaterialButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = MaterialButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues =
        MaterialButtonDefaults.contentPaddingFor(MaterialButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    MaterialOutlinedButton(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * A text expressive button whose default shapes use the M3X theme's full shape. Corner-based shapes
 * interpolate between states; other shape types switch directly.
 *
 * @param onClick Called when the button is activated.
 * @param modifier Modifier applied to the button.
 * @param enabled Whether the button accepts input and is exposed as enabled to accessibility.
 * @param shapes Shapes used for the button and its pressed state.
 * @param colors Colors used for the button's container and content.
 * @param elevation Elevation values used across interaction states.
 * @param border Optional border drawn around the button.
 * @param contentPadding Space around the button's content.
 * @param interactionSource Optional source for observing interactions; a null value uses an
 *   internal source.
 * @param content Content placed inside the button's row.
 */
@Composable
public fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: ButtonShapes = ButtonDefaults.shapes(),
    colors: ButtonColors = MaterialButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues =
        MaterialButtonDefaults.contentPaddingFor(MaterialButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    MaterialTextButton(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}
