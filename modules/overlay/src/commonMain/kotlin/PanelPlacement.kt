package top.ltfan.m3x.overlay

import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

/**
 * Places a panel at the position [positionProvider] calculates for [anchorBounds], relative to the
 * container of the overlay the panel lives in.
 *
 * The panel is measured with the constraints of the window and reports a zero size, so it does not
 * change the layout that holds it, like the popup of the platform. The position is calculated in
 * window coordinates and translated by the origin of the container, which is measured with
 * [overlayContainerBounds].
 *
 * A panel that shares its bounds with an element of the page is measured against the animated
 * bounds while a shared animation carries it. [stableSize] receives the size of the lookahead pass
 * and the approach pass reuses it: otherwise the two passes would calculate different positions,
 * which turns into a motion-frame-of-reference offset that moves the animated bounds.
 *
 * @param stableSize receives the size of the lookahead pass, or `null` for a panel whose
 *   measurement is not affected by a shared animation
 * @param onPlaced called with the calculated position and the size of the panel, for a panel that
 *   derives state from where it lands
 */
public fun Modifier.panelPlacement(
    bounds: OverlayBounds,
    anchorBounds: IntRect,
    positionProvider: PopupPositionProvider,
    windowSize: IntSize,
    layoutDirection: LayoutDirection,
    stableSize: MutableState<IntSize>? = null,
    onPlaced: ((position: IntOffset, size: IntSize) -> Unit)? = null,
): Modifier = layout { measurable, _ ->
    val placeable =
        measurable.measure(Constraints(maxWidth = windowSize.width, maxHeight = windowSize.height))
    val measuredSize = IntSize(placeable.width, placeable.height)
    if (stableSize != null && isLookingAhead) {
        stableSize.value = measuredSize
    }
    val panelSize =
        if (stableSize == null || isLookingAhead || stableSize.value == IntSize.Zero) {
            measuredSize
        } else {
            stableSize.value
        }
    val position =
        positionProvider.calculatePosition(
            anchorBounds = anchorBounds,
            windowSize = windowSize,
            layoutDirection = layoutDirection,
            popupContentSize = panelSize,
        )
    onPlaced?.invoke(position, panelSize)
    layout(0, 0) {
        placeable.place(
            position.x - bounds.container.left,
            position.y - bounds.container.top,
        )
    }
}
