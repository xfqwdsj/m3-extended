package top.ltfan.m3x.compose.foundation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.onConsumedWindowInsetsChanged
import androidx.compose.foundation.layout.only
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Builder for [WindowInsetsSides] used by the [only] extension.
 *
 * @property start the [WindowInsetsSides.Start] side.
 * @property top the [WindowInsetsSides.Top] side.
 * @property end the [WindowInsetsSides.End] side.
 * @property bottom the [WindowInsetsSides.Bottom] side.
 * @property vertical the [WindowInsetsSides.Vertical] sides.
 * @property horizontal the [WindowInsetsSides.Horizontal] sides.
 */
@WindowInsetsSidesBuilder.Dsl
public object WindowInsetsSidesBuilder {
    public val start: WindowInsetsSides = WindowInsetsSides.Start
    public val top: WindowInsetsSides = WindowInsetsSides.Top
    public val end: WindowInsetsSides = WindowInsetsSides.End
    public val bottom: WindowInsetsSides = WindowInsetsSides.Bottom
    public val vertical: WindowInsetsSides = WindowInsetsSides.Vertical
    public val horizontal: WindowInsetsSides = WindowInsetsSides.Horizontal

    @DslMarker public annotation class Dsl
}

/** Returns a copy of this [WindowInsets] limited to the sides selected in [block]. */
public inline fun WindowInsets.only(
    block: WindowInsetsSidesBuilder.() -> WindowInsetsSides,
): WindowInsets = this.only(WindowInsetsSidesBuilder.block())

/** Adds [padding] to each side of this [WindowInsets]. */
@Composable
public operator fun WindowInsets.plus(padding: PaddingValues): WindowInsets =
    remember(this, padding) { AddedPaddingWindowInsets(this, padding) }

@Stable
private class AddedPaddingWindowInsets(
    private val base: WindowInsets,
    private val padding: PaddingValues,
) : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int =
        base.getLeft(density, layoutDirection) +
            with(density) {
                padding.calculateLeftPadding(layoutDirection).roundToPx()
            }

    override fun getTop(density: Density): Int =
        base.getTop(density) + with(density) { padding.calculateTopPadding().roundToPx() }

    override fun getRight(density: Density, layoutDirection: LayoutDirection): Int =
        base.getRight(density, layoutDirection) +
            with(density) {
                padding.calculateRightPadding(layoutDirection).roundToPx()
            }

    override fun getBottom(density: Density): Int =
        base.getBottom(density) + with(density) { padding.calculateBottomPadding().roundToPx() }
}

/**
 * Lays out [content] with the [insets] that descendants have not consumed, exposed as
 * [PaddingValues].
 *
 * @param insets the window insets to observe
 * @param modifier the [Modifier] to be applied to the root layout
 * @param content the content that receives the unconsumed insets
 */
@Composable
public fun WindowInsetsToPaddingValuesBox(
    insets: WindowInsets,
    modifier: Modifier = Modifier,
    content: @Composable (paddingValues: PaddingValues) -> Unit,
) {
    var consumedInsets by remember { mutableStateOf(WindowInsets()) }
    Box(modifier.onConsumedWindowInsetsChanged { consumedInsets = it }) {
        content(insets.exclude(consumedInsets).asPaddingValues())
    }
}
