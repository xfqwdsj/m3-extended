package top.ltfan.m3x.compose.animation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Interpolatable
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Resolves the outline with Compose shape interpolation at the current progress. The shape clamps
 * progress to the range between the start and end shapes.
 */
public class InterpolatedShape
internal constructor(
    private val start: () -> Shape,
    private val end: () -> Shape,
    private val progress: () -> Float,
) : Shape {
    public constructor(
        start: Shape,
        end: Shape,
        progress: () -> Float,
    ) : this({ start }, { end }, progress)

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val shape = Interpolatable.lerp(start(), end(), progress().coerceIn(0f, 1f)) as Shape
        return shape.createOutline(size, layoutDirection, density)
    }
}

/**
 * Remembers one shape for the transition. States that satisfy [isExpanded] target [end]. Other
 * states target [start].
 */
@Composable
public fun <T> Transition<T>.rememberInterpolatedShape(
    start: Shape,
    end: Shape,
    animationSpec: FiniteAnimationSpec<Float>,
    isExpanded: (T) -> Boolean,
): Shape {
    val currentStart = rememberUpdatedState(start)
    val currentEnd = rememberUpdatedState(end)
    val progress =
        animateFloat(transitionSpec = { animationSpec }, label = "ShapeProgress") {
            if (isExpanded(it)) 1f else 0f
        }
    return remember(progress) {
        InterpolatedShape({ currentStart.value }, { currentEnd.value }) { progress.value }
    }
}
