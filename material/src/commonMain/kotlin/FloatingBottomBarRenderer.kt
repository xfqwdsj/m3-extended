package top.ltfan.m3x.material

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import top.ltfan.m3x.compose.foundation.gestures.DampedDragState

/** Surface hooks for a bar whose gesture, selection and layout remain component-owned. */
public interface FloatingBottomBarVisuals {
    public val hasSource: Boolean
    public val hasIndicatorSource: Boolean
    public val containerColor: Color
    public val contentColor: Color

    @Composable public fun Modifier.interactionModifier(): Modifier

    @Composable public fun Modifier.barModifier(spatialPressFraction: () -> Float): Modifier

    @Composable
    public fun Modifier.indicatorModifier(
        dampedDragAnimation: DampedDragState,
        pressFraction: () -> Float,
        indicatorEffects: MaterialSurfaceEffects?,
    ): Modifier

    @Composable
    public fun TabCapture(
        panelOffset: () -> Float,
        spatialPressFraction: () -> Float,
        pressFraction: () -> Float,
        tabContent: @Composable RowScope.() -> Unit,
    )
}

/** Selects floating-bar optics independently of its interaction implementation. */
public fun interface FloatingBottomBarRenderer {
    @Composable public fun appearance(shape: Shape): FloatingBottomBarVisuals
}

/** Ordinary floating-bar rendering using the caller's Material colors. */
public val PlainFloatingBottomBarRenderer: FloatingBottomBarRenderer =
    FloatingBottomBarRenderer { shape ->
        val colors = MaterialTheme.colorScheme
        object : FloatingBottomBarVisuals {
            override val hasSource: Boolean = false
            override val hasIndicatorSource: Boolean = false
            override val containerColor: Color = colors.surface
            override val contentColor: Color = colors.onSurface

            @Composable override fun Modifier.interactionModifier(): Modifier = this

            @Composable
            override fun Modifier.barModifier(spatialPressFraction: () -> Float): Modifier = this

            @Composable
            override fun Modifier.indicatorModifier(
                dampedDragAnimation: DampedDragState,
                pressFraction: () -> Float,
                indicatorEffects: MaterialSurfaceEffects?,
            ): Modifier =
                background(colors.surfaceContainerHighest, shape)
                    .border(1.dp, colors.outline, shape)

            @Composable
            override fun TabCapture(
                panelOffset: () -> Float,
                spatialPressFraction: () -> Float,
                pressFraction: () -> Float,
                tabContent: @Composable RowScope.() -> Unit,
            ) = Unit
        }
    }

/** The renderer selected for ordinary floating bars. */
public val LocalFloatingBottomBarRenderer: ProvidableCompositionLocal<FloatingBottomBarRenderer> =
    staticCompositionLocalOf {
        PlainFloatingBottomBarRenderer
    }
