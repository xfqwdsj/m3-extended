package top.ltfan.m3x.backdrop.backdrops

import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import top.ltfan.m3x.backdrop.internal.recordLayer

public fun Modifier.layerBackdrop(backdrop: LayerBackdrop): Modifier =
    this then LayerBackdropElement(backdrop)

private class LayerBackdropElement(val backdrop: LayerBackdrop) :
    ModifierNodeElement<LayerBackdropNode>() {

    override fun create(): LayerBackdropNode {
        return LayerBackdropNode(backdrop)
    }

    override fun update(node: LayerBackdropNode) {
        if (node.backdrop != backdrop) {
            node.backdrop.layerCoordinates = null
            node.backdrop.sourceBounds = null
            node.backdrop.publishInvalidation()
            node.backdrop = backdrop
        }
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "layerBackdrop"
        properties["backdrop"] = backdrop
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LayerBackdropElement) return false

        if (backdrop != other.backdrop) return false

        return true
    }

    override fun hashCode(): Int {
        return backdrop.hashCode()
    }
}

private class LayerBackdropNode(var backdrop: LayerBackdrop) :
    DrawModifierNode, GlobalPositionAwareModifierNode, Modifier.Node() {
    private var publishContent = true
    private var stateObserver: ObserverHandle? = null

    override fun onAttach() {
        stateObserver = Snapshot.registerApplyObserver { changed, _ ->
            if (changed.any { !BackdropRevisionStates.contains(it) }) {
                publishContent = true
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        recordLayer(backdrop.graphicsLayer) { backdrop.onDraw(this@draw) }
        // Publishing once per state change prevents the recording from scheduling itself.
        if (!publishContent) return
        publishContent = false
        val recorded = backdrop.graphicsLayer.size
        val bounds = Rect(0f, 0f, recorded.width.toFloat(), recorded.height.toFloat())
        if (backdrop.sourceBounds != bounds) backdrop.sourceBounds = bounds
        backdrop.publishRecording()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            backdrop.layerCoordinates = coordinates
            // The coordinate object is stable while its mapped position and transform can change.
            backdrop.publishInvalidation()
        }
    }

    override fun onDetach() {
        stateObserver?.dispose()
        stateObserver = null
        publishContent = true
        backdrop.layerCoordinates = null
        backdrop.sourceBounds = null
        backdrop.publishInvalidation()
    }
}
