package top.ltfan.m3x.backdrop.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import kotlin.math.ceil
import kotlin.math.floor
import top.ltfan.m3x.backdrop.BackdropGeometry
import top.ltfan.m3x.backdrop.BackdropPrimitive
import top.ltfan.m3x.backdrop.BackdropPrimitiveContext
import top.ltfan.m3x.backdrop.BackdropPrimitiveDrawContext
import top.ltfan.m3x.backdrop.BackdropPrimitiveRenderer
import top.ltfan.m3x.backdrop.BackdropRamp
import top.ltfan.m3x.backdrop.BackdropRenderResources
import top.ltfan.m3x.backdrop.recordLayerRegion

/** Retains gradient weights and branch layers independently of animated blur radii. */
internal data class LayeredBlurPrimitive(
    val radius: Float,
    val ramp: BackdropRamp,
    val levels: Int,
    val start: Offset,
    val end: Offset,
) : BackdropPrimitive<LayeredBlurPrimitive> {
    override fun bounds(geometry: BackdropGeometry): Rect = geometry.surfaceBounds

    override fun createRenderer(
        resources: BackdropRenderResources,
    ): BackdropPrimitiveRenderer<LayeredBlurPrimitive> = Renderer(resources)

    private class Branch(val weighted: GraphicsLayer, val blurred: GraphicsLayer?) {
        var brush: Brush? = null
        var radius = Float.NaN
        var outputBounds: Rect = Rect.Zero
        var blurredBounds: Rect = Rect.Zero
    }

    private class Renderer(resources: BackdropRenderResources) :
        BackdropPrimitiveRenderer<LayeredBlurPrimitive> {
        private var cached: LayeredBlurPrimitive? = null
        private var activeLevels = IntRange.EMPTY
        private var stops: List<Pair<Float, Float>> = emptyList()
        private val branches =
            resources.resourcePool<Int, Branch> { level ->
                val weighted =
                    createGraphicsLayer().apply {
                        compositingStrategy = CompositingStrategy.Offscreen
                        blendMode = BlendMode.Plus
                    }
                Branch(weighted, if (level == 0) null else createGraphicsLayer())
            }

        override fun prepare(primitive: LayeredBlurPrimitive, context: BackdropPrimitiveContext) {
            val old = cached
            if (
                old == null ||
                    old.ramp != primitive.ramp ||
                    old.levels != primitive.levels ||
                    old.start != primitive.start ||
                    old.end != primitive.end
            ) {
                stops = layeredBlurStops(primitive.ramp, primitive.levels)
                val scale = primitive.levels - 1
                val first = floor(minOf(primitive.ramp.from, primitive.ramp.to) * scale).toInt()
                val last = ceil(maxOf(primitive.ramp.from, primitive.ramp.to) * scale).toInt()
                activeLevels = first..last
                branches.retainOnly(activeLevels.toSet())
                for (level in activeLevels) {
                    branches.getOrCreate(level).brush =
                        Brush.linearGradient(
                            *layeredBlurColors(stops, level, primitive.levels).toTypedArray(),
                            start = primitive.start,
                            end = primitive.end,
                        )
                }
            }
            cached = primitive
            for (level in activeLevels) {
                val branch = branches.getOrCreate(level)
                val radius = primitive.radius * (level.toFloat() / (primitive.levels - 1))
                if (branch.radius != radius) {
                    branch.blurred?.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
                    branch.radius = radius
                }
                branch.outputBounds =
                    layeredBlurBand(
                        stops,
                        level,
                        primitive.levels,
                        primitive.start,
                        primitive.end,
                        context.surfaceBounds,
                    ) ?: Rect.Zero
                val support = gaussianSupport(radius)
                branch.blurredBounds =
                    if (branch.blurred == null) {
                        Rect.Zero
                    } else {
                        branch.outputBounds.inflate(support).intersect(context.inputBounds[0])
                    }
            }
        }

        override fun draw(
            scope: DrawScope,
            primitive: LayeredBlurPrimitive,
            context: BackdropPrimitiveDrawContext,
        ) {
            val extension = context.extension
            val input = context.input(0)
            for (level in activeLevels) {
                val branch = branches.getOrCreate(level)
                if (branch.outputBounds.isEmpty) continue
                branch.blurred?.let { layer ->
                    context.recordLayerRegion(layer, branch.blurredBounds) { input.draw(this) }
                }
                context.recordLayerRegion(branch.weighted, branch.outputBounds) {
                    val blurred = branch.blurred
                    if (blurred == null) input.draw(this) else drawLayer(blurred)
                    // Apply the weight after filtering, over the complete storage region.
                    drawRect(
                        checkNotNull(branch.brush),
                        Offset(-extension.left, -extension.top),
                        Size(
                            context.size.width + extension.left + extension.right,
                            context.size.height + extension.top + extension.bottom,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                }
                with(scope) { drawLayer(branch.weighted) }
            }
        }

        override fun refresh() {
            cached = null
        }
    }
}
