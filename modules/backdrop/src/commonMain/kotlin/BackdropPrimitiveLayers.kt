package top.ltfan.m3x.backdrop

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil
import kotlin.math.floor
import top.ltfan.m3x.backdrop.internal.hasFiniteEdges
import top.ltfan.m3x.backdrop.internal.isOrdered
import top.ltfan.m3x.backdrop.internal.recordSurfaceLayer

/**
 * Records an owned [layer] in surface-local coordinates using the resolved graph storage. The
 * layer's placement and recording origin include [BackdropPrimitiveContext.extension]; draw-scope
 * and brush sizes remain the surface size. Draw explicitly across the extension when the operation
 * needs those pixels. Configure layer effects, alpha and blending separately.
 *
 * Call during primitive drawing. Borrowed inputs stay valid only for that invocation. This helper
 * records commands and leaves sampling declarations and layer ownership with their existing owners.
 */
public inline fun BackdropPrimitiveDrawContext.recordLayer(
    layer: GraphicsLayer,
    crossinline block: DrawScope.() -> Unit,
) {
    checkActivePrimitiveDraw()
    val surfaceSize = size
    val room = extension
    val expanded = room.expand(surfaceSize)
    val width = ceil(expanded.width)
    val height = ceil(expanded.height)
    require(
        width.isFinite() &&
            height.isFinite() &&
            width >= 0f &&
            height >= 0f &&
            width.toDouble() <= Int.MAX_VALUE &&
            height.toDouble() <= Int.MAX_VALUE,
    ) {
        "Primitive layer storage must fit non-negative pixel dimensions"
    }
    layer.recordSurfaceLayer(
        density,
        layoutDirection,
        surfaceSize,
        IntOffset(-room.left.toInt(), -room.top.toInt()),
        IntSize(width.toInt(), height.toInt()),
        room.left,
        room.top,
        block,
    )
}

/**
 * Records only [region] from this primitive's surface-local drawing space. The caller owns the
 * layer's effects and blending. Commands outside [region] are clipped by the native layer storage.
 */
internal inline fun BackdropPrimitiveDrawContext.recordLayerRegion(
    layer: GraphicsLayer,
    region: Rect,
    crossinline block: DrawScope.() -> Unit,
) {
    checkActivePrimitiveDraw()
    require(
        region.hasFiniteEdges && region.isOrdered,
    ) {
        "Primitive layer region must be finite and non-inverted"
    }
    val left = floor(region.left)
    val top = floor(region.top)
    val right = ceil(region.right)
    val bottom = ceil(region.bottom)
    val width = right - left
    val height = bottom - top
    require(
        width >= 0f &&
            height >= 0f &&
            width.toDouble() <= Int.MAX_VALUE &&
            height.toDouble() <= Int.MAX_VALUE,
    ) {
        "Primitive layer region must fit non-negative pixel dimensions"
    }
    val surfaceSize = size
    layer.recordSurfaceLayer(
        density,
        layoutDirection,
        surfaceSize,
        IntOffset(left.toInt(), top.toInt()),
        IntSize(width.toInt(), height.toInt()),
        -left,
        -top,
        block,
    )
}

@PublishedApi
internal fun BackdropPrimitiveDrawContext.checkActivePrimitiveDraw() {
    check((this as PrimitiveContext).drawing) {
        "Primitive layers can be recorded only during drawing"
    }
}
