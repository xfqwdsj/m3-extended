package top.ltfan.m3x.material.overlay

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Interpolatable
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import top.ltfan.m3x.compose.animation.core.withDefaultVisibilityThreshold
import top.ltfan.m3x.compose.ui.accessibility.SpatialMotionEnabled
import top.ltfan.m3x.overlay.LocalOverlayAnimatedVisibilityScope

@Composable
internal fun rememberMenuTransitionShape(collapsed: Shape, expanded: Shape, size: Size): Shape {
    if (collapsed == expanded || !SpatialMotionEnabled) return expanded
    val transition = LocalOverlayAnimatedVisibilityScope.current?.transition ?: return expanded
    val motionScheme = MaterialTheme.motionScheme
    val density = LocalDensity.current
    val threshold =
        remember(collapsed, expanded, size, density) {
            menuCornerProgressThreshold(collapsed, expanded, size, density)
        }
    val spec =
        remember(motionScheme, threshold) {
            motionScheme.slowEffectsSpec<Float>().withDefaultVisibilityThreshold(threshold)
        }
    val progress =
        transition.animateFloat(transitionSpec = { spec }, label = "MenuCornerProgress") {
            if (it == EnterExitState.Visible) 1f else 0f
        }
    return remember(collapsed, expanded, progress) {
        MenuTransitionShape(collapsed, expanded) { progress.value }
    }
}

internal class MenuTransitionShape(
    private val collapsed: Shape,
    private val expanded: Shape,
    private val progress: () -> Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val shape = Interpolatable.lerp(collapsed, expanded, progress().coerceIn(0f, 1f)) as Shape
        return shape.createOutline(size, layoutDirection, density)
    }
}

/** Converts a one-pixel corner tolerance to progress units once the panel is measured. */
internal fun menuCornerProgressThreshold(
    collapsed: Shape,
    expanded: Shape,
    size: Size,
    density: Density,
): Float {
    if (
        size.minDimension <= 0f || collapsed !is CornerBasedShape || expanded !is CornerBasedShape
    ) {
        return 0.01f
    }
    val distance =
        maxOf(
            abs(expanded.topStart.toPx(size, density) - collapsed.topStart.toPx(size, density)),
            abs(expanded.topEnd.toPx(size, density) - collapsed.topEnd.toPx(size, density)),
            abs(expanded.bottomEnd.toPx(size, density) - collapsed.bottomEnd.toPx(size, density)),
            abs(
                expanded.bottomStart.toPx(size, density) -
                    collapsed.bottomStart.toPx(size, density),
            ),
        )
    return if (distance > 0f) (1f / distance).coerceAtMost(1f) else 0.01f
}
