package top.ltfan.m3x.backdrop.internal

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toIntSize

context(node: DelegatableNode)
internal fun DrawScope.recordLayer(
    layer: GraphicsLayer,
    size: IntSize = this.size.toIntSize(),
    block: DrawScope.() -> Unit,
) {
    val density = node.requireDensity()
    layer.record(size) {
        val prevDensity = drawContext.density
        drawContext.density = density
        try {
            this.block()
        } finally {
            drawContext.density = prevDensity
        }
    }
}

/**
 * Records [storageSize] at [topLeft] while the draw block observes [surfaceSize] and the supplied
 * storage translation.
 */
@PublishedApi
internal inline fun GraphicsLayer.recordSurfaceLayer(
    density: Density,
    layoutDirection: LayoutDirection,
    surfaceSize: Size,
    topLeft: IntOffset,
    storageSize: IntSize,
    translationX: Float,
    translationY: Float,
    crossinline block: DrawScope.() -> Unit,
) {
    this.topLeft = topLeft
    record(density, layoutDirection, storageSize) {
        val previous = drawContext.size
        drawContext.size = surfaceSize
        try {
            translate(translationX, translationY) { block() }
        } finally {
            drawContext.size = previous
        }
    }
}
