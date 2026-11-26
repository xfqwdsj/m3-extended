package top.ltfan.m3x.material

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import top.ltfan.m3x.material.theme.extended

/** Applies the M3X full resting shape to an upstream two-state shape value. */
@Composable
internal fun <T> rememberThemedShapes(
    defaults: T,
    shape: Shape?,
    pressedShape: Shape?,
    copy: (shape: Shape, pressedShape: Shape?) -> T,
): T {
    val resolved = resolvedFullShape(shape)
    return remember(defaults, resolved, pressedShape) { copy(resolved, pressedShape) }
}

@Composable
internal fun resolvedFullShape(shape: Shape?): Shape = shape ?: MaterialTheme.extended.shapes.full
