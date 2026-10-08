package top.ltfan.m3x.material.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import top.ltfan.m3x.material.MaterialSurfaceEdge
import top.ltfan.m3x.material.MaterialSurfaceEffects
import top.ltfan.m3x.material.MaterialSurfaceLayer
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.materialSurface
import top.ltfan.m3x.material.rememberSurfaceAppearance
import top.ltfan.m3x.overlay.LocalSharedTransitionScope

/**
 * A renderer-selected panel surface with shared-transition-aware content measurement.
 *
 * During a shared transition, drawing spans the animated panel bounds. Lookahead reports the
 * content size, and ordinary layout follows the content when no shared transition is active.
 *
 * A specified [containerColor] enables surface drawing. With [Color.Unspecified], the content owns
 * its container. [exportedBackdrop] wraps the content with drawing enabled on the same condition;
 * the selected renderer determines how its layer participates in nested rendering.
 *
 * @param shape the shape of the panel
 * @param containerColor the panel color, or [Color.Unspecified] for content-owned backing
 * @param exportedBackdrop the renderer-owned layer wrapping the panel content
 * @param modifier the modifier applied to the surface
 * @param contentAlignment the alignment of the content inside the surface
 * @param effects optional renderer-specific effects
 * @param contentColor the supplied foreground color, resolved by the selected renderer
 * @param excludedEdges physical edges excluded from the renderer's surface treatment
 * @param content the content of the panel
 */
@Composable
public fun MaterialPanelSurface(
    shape: Shape,
    containerColor: Color,
    exportedBackdrop: MaterialSurfaceLayer,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    effects: MaterialSurfaceEffects? = null,
    contentColor: Color = LocalContentColor.current,
    excludedEdges: Set<MaterialSurfaceEdge> = emptySet(),
    content: @Composable () -> Unit,
) {
    val drawsBackdrop = containerColor.isSpecified
    val appearance =
        if (drawsBackdrop)
            rememberSurfaceAppearance(
                MaterialSurfaceRole.Panel,
                containerColor = containerColor,
                contentColor = contentColor,
                effects = effects,
                shape = shape,
                excludedEdges = excludedEdges,
            )
        else null
    Box(
        modifier =
            modifier
                .panelSurfaceFill()
                .then(
                    if (appearance != null)
                        Modifier.materialSurface(
                            appearance = appearance,
                            shape = shape,
                            exportedLayer = exportedBackdrop,
                        )
                    else Modifier,
                ),
        contentAlignment = contentAlignment,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides (appearance?.contentColor ?: contentColor),
        ) {
            exportedBackdrop.Content(enabled = drawsBackdrop, content = content)
        }
    }
}

/**
 * While a shared animation runs, the surface spans the panel's animated bounds. During lookahead,
 * it keeps the content size. This keeps the size reported to the parent stable while the bounds
 * animate.
 */
@Composable
private fun Modifier.panelSurfaceFill(): Modifier =
    this then
        BackdropPanelFillElement(LocalSharedTransitionScope.current?.isTransitionActive == true)

private data class BackdropPanelFillElement(val fill: Boolean) :
    ModifierNodeElement<BackdropPanelFillNode>() {
    override fun create(): BackdropPanelFillNode = BackdropPanelFillNode(fill)

    override fun update(node: BackdropPanelFillNode) {
        node.fill = fill
    }

    override fun InspectorInfo.inspectableProperties() {
        debugInspectorInfo { properties["fill"] = fill }
    }
}

private class BackdropPanelFillNode(var fill: Boolean) : LayoutModifierNode, Modifier.Node() {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable =
            if (isLookingAhead || !fill) {
                measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
            } else {
                measurable.measure(constraints)
            }
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
}
