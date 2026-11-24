package top.ltfan.m3x.backdrop.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil
import top.ltfan.m3x.backdrop.BackdropGeometry
import top.ltfan.m3x.backdrop.BackdropImageScope
import top.ltfan.m3x.backdrop.BackdropPrimitive
import top.ltfan.m3x.backdrop.BackdropPrimitiveContext
import top.ltfan.m3x.backdrop.BackdropPrimitiveDrawContext
import top.ltfan.m3x.backdrop.BackdropPrimitiveInput
import top.ltfan.m3x.backdrop.BackdropPrimitiveRenderer
import top.ltfan.m3x.backdrop.BackdropRamp
import top.ltfan.m3x.backdrop.BackdropRenderResources
import top.ltfan.m3x.backdrop.BackdropSampling
import top.ltfan.m3x.backdrop.PrimitiveResources
import top.ltfan.m3x.backdrop.RenderImageNode
import top.ltfan.m3x.backdrop.RuntimeShader
import top.ltfan.m3x.backdrop.internal.chain
import top.ltfan.m3x.backdrop.internal.createRuntimeShaderEffect
import top.ltfan.m3x.backdrop.shaderControls

private const val DownsampleScale = .25f

/** Resampling needs a large blur footprint and a ramp that stays soft across the receiver. */
internal fun minimumAreaForDownsampledBlur(radius: Float, ramp: BackdropRamp): Float {
    val minimumRadius = radius * minOf(ramp.from, ramp.to)
    val maximumRadius = radius * maxOf(ramp.from, ramp.to)
    return when {
        minimumRadius < 16f -> Float.POSITIVE_INFINITY
        maximumRadius >= 160f -> 144_000f
        maximumRadius >= 128f -> 200_000f
        else -> Float.POSITIVE_INFINITY
    }
}

internal fun BackdropImageScope.downsampledProgressiveBlur(
    radius: Float,
    ramp: BackdropRamp,
    geometry: ProgressiveBlurGeometry,
) {
    val vertical = geometry.side.isVertical
    val anchorStart = if (vertical) geometry.start.y else geometry.start.x
    val anchorEnd = if (vertical) geometry.end.y else geometry.end.x
    val horizontalSampling =
        progressiveBlurSampling(false, vertical, radius, ramp, anchorStart, anchorEnd)
    val verticalSampling =
        progressiveBlurSampling(true, vertical, radius, ramp, anchorStart, anchorEnd)
    val sampling = BackdropSampling { output ->
        verticalSampling.requiredInput(horizontalSampling.requiredInput(output))
    }
    effects { cover(geometry.coverage) }
    val original = current
    val primitive =
        DownsampledProgressiveBlurPrimitive(
            radius = radius,
            vertical = vertical,
            ramp = ramp,
            anchorStart = anchorStart,
            anchorEnd = anchorEnd,
        )
    drawPrimitive(
        RenderImageNode.Primitive(
            primitive,
            listOf(BackdropPrimitiveInput(original, sampling)),
        ),
        1f,
        BlendMode.Src,
    )
}

private class DownsampledProgressiveBlurPrimitive(
    val radius: Float,
    val vertical: Boolean,
    val ramp: BackdropRamp,
    val anchorStart: Float,
    val anchorEnd: Float,
) : BackdropPrimitive<DownsampledProgressiveBlurPrimitive> {
    override fun bounds(geometry: BackdropGeometry): Rect = geometry.surfaceBounds

    override fun createRenderer(
        resources: BackdropRenderResources,
    ): BackdropPrimitiveRenderer<DownsampledProgressiveBlurPrimitive> =
        Renderer(resources as PrimitiveResources)

    private class Renderer(private val resources: PrimitiveResources) :
        BackdropPrimitiveRenderer<DownsampledProgressiveBlurPrimitive> {
        private var layer: GraphicsLayer? = null
        private lateinit var verticalShader: RuntimeShader
        private lateinit var horizontalShader: RuntimeShader

        override fun prepare(
            primitive: DownsampledProgressiveBlurPrimitive,
            context: BackdropPrimitiveContext,
        ) {
            if (layer == null) layer = resources.createGraphicsLayer()
            verticalShader =
                resources.obtainRuntimeShader(
                    "ProgressiveBlurDownsampleVertical",
                    ProgressiveBlurVerticalShader,
                )
            horizontalShader =
                resources.obtainRuntimeShader(
                    "ProgressiveBlurDownsampleHorizontal",
                    ProgressiveBlurHorizontalShader,
                )
        }

        override fun draw(
            scope: DrawScope,
            primitive: DownsampledProgressiveBlurPrimitive,
            context: BackdropPrimitiveDrawContext,
        ) {
            val source = context.inputBounds[0]
            if (source.isEmpty) return
            val output = checkNotNull(layer)
            val scaledWidth = ceil(source.width * DownsampleScale).toInt()
            val scaledHeight = ceil(source.height * DownsampleScale).toInt()
            if (scaledWidth <= 0 || scaledHeight <= 0) return
            val axisStart = if (primitive.vertical) source.top else source.left
            val scaledRadius = primitive.radius * DownsampleScale
            fun configure(shader: RuntimeShader) {
                shader.setFloatUniform(
                    "anchors",
                    (primitive.anchorStart - axisStart) * DownsampleScale,
                    (primitive.anchorEnd - axisStart) * DownsampleScale,
                )
                shader.setFloatUniform("blurRadius", scaledRadius)
                shader.setFloatUniform("axis", if (primitive.vertical) 1f else 0f)
                shader.setFloatUniform("curve", primitive.ramp.curve.shaderControls())
                shader.setFloatUniform("intensities", primitive.ramp.from, primitive.ramp.to)
            }
            configure(verticalShader)
            configure(horizontalShader)
            output.topLeft = IntOffset.Zero
            output.renderEffect =
                createRuntimeShaderEffect(verticalShader, "content").let { verticalEffect ->
                    createRuntimeShaderEffect(horizontalShader, "content").let { horizontalEffect ->
                        verticalEffect.chain(horizontalEffect)
                    }
                }
            output.record(
                context.density,
                context.layoutDirection,
                IntSize(scaledWidth, scaledHeight),
            ) {
                scale(DownsampleScale, DownsampleScale, Offset.Zero) {
                    translate(-source.left, -source.top) { context.input(0).draw(this) }
                }
            }
            with(scope) {
                translate(source.left, source.top) {
                    scale(1f / DownsampleScale, 1f / DownsampleScale, Offset.Zero) {
                        drawLayer(output)
                    }
                }
            }
        }

        override fun refresh() = release()

        override fun release() {
            layer?.let(resources::releaseGraphicsLayer)
            layer = null
        }
    }
}
