package top.ltfan.m3x.compose.foundation

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Constraints

/**
 * Keeps the element's layout at its lookahead size. Content with shared bounds retains its final
 * layout while the bounds animate. For example, a menu panel keeps that layout as it grows out of a
 * button. The element is measured with the constraints of the lookahead pass, so it does not
 * re-flow on every animated frame.
 *
 * Shared-transition callers retain their final content layout while the enclosing bounds move.
 */
public fun Modifier.keepSizeWhenLookingAhead(): Modifier = this then KeepLookaheadLayoutElement

private data object KeepLookaheadLayoutElement : ModifierNodeElement<KeepLookaheadLayoutNode>() {
    override fun create(): KeepLookaheadLayoutNode = KeepLookaheadLayoutNode()

    override fun update(node: KeepLookaheadLayoutNode) = Unit
}

private class KeepLookaheadLayoutNode : LayoutModifierNode, Modifier.Node() {
    private var lookaheadConstraints: Constraints? = null

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        if (isLookingAhead) {
            lookaheadConstraints = constraints
        }
        val placeable = measurable.measure(lookaheadConstraints ?: constraints)
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
}
