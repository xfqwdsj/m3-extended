package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import top.ltfan.backdrop.BackdropEffectScope
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial
import top.ltfan.m3x.material.theme.extended

/**
 * A shared capsule material for a top app bar's navigation icon or actions. Use an M3X Backdrop top
 * app bar to allow its shadow to extend beyond the row; Material TopAppBar clips its controls to
 * the row bounds. Children keep their own click, focus, hover, and accessibility behavior.
 */
@Composable
public fun BackdropTopAppBarControl(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.extended.shapes.full,
    material: M3xBackdropMaterial? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    effects: (BackdropEffectScope.() -> Unit)? = null,
    interactionSource: InteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val role = LocalBackdropTopAppBarControlRole.current
    BackdropSurface(
        modifier =
            modifier.run {
                when (role) {
                    BackdropTopAppBarControlRole.Navigation -> padding(start = 4.dp, end = 2.dp)
                    BackdropTopAppBarControlRole.Actions -> padding(start = 2.dp, end = 4.dp)
                    null -> this
                }
            },
        shape = shape,
        role = MaterialSurfaceRole.Control,
        material = material,
        containerColor = containerColor,
        contentColor = contentColor,
        effects = effects,
        interactionSource = interactionSource,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

internal val LocalBackdropTopAppBarControlRole =
    staticCompositionLocalOf<BackdropTopAppBarControlRole?> { null }

internal enum class BackdropTopAppBarControlRole {
    Navigation,
    Actions,
}
