package top.ltfan.m3x.backdrop.shadow

import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import top.ltfan.m3x.backdrop.BackdropGeometry
import top.ltfan.m3x.backdrop.BackdropImageScope
import top.ltfan.m3x.backdrop.BackdropPrimitive
import top.ltfan.m3x.backdrop.BackdropPrimitiveRenderer
import top.ltfan.m3x.backdrop.BackdropRenderResources
import top.ltfan.m3x.backdrop.primitive

/** Parameters for a shape shadow image. */
@Immutable
public sealed interface Shadow {
    /** Omits the primitive and releases its rendering resources. */
    @Immutable public data object None : Shadow

    @Immutable
    public data class Config
    public constructor(
        public val radius: Dp = 24f.dp,
        public val offset: DpOffset = DpOffset(0f.dp, radius / 6f),
        public val color: Color = Color.Black.copy(alpha = 0.1f),
        @param:FloatRange(from = 0.0, to = 1.0) public val alpha: Float = 1f,
    ) : Shadow {
        init {
            requireValidShadow(radius, offset, alpha)
        }
    }

    public companion object {

        @Stable public val Default: Config = Shadow()
    }
}

@Suppress("FunctionName")
public fun Shadow(
    radius: Dp = 24f.dp,
    offset: DpOffset = DpOffset(0f.dp, radius / 6f),
    color: Color = Color.Black.copy(alpha = 0.1f),
    @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1f,
): Shadow.Config = Shadow.Config(radius, offset, color, alpha)

/** Draws an outer shape shadow at the current declaration position. */
public fun BackdropImageScope.shadow(
    config: Shadow,
    alpha: Float = 1f,
    blendMode: BlendMode = BlendMode.SrcOver,
) {
    if (config === Shadow.None) return
    primitive(ShadowPrimitive(config as Shadow.Config), alpha = alpha, blendMode = blendMode)
}

private data class ShadowPrimitive(val config: Shadow.Config) : BackdropPrimitive<ShadowPrimitive> {
    override val compositingStrategy: CompositingStrategy = CompositingStrategy.Auto

    override fun bounds(geometry: BackdropGeometry): Rect =
        outerShadowBounds(config, geometry.size, geometry.density)

    override fun createRenderer(
        resources: BackdropRenderResources,
    ): BackdropPrimitiveRenderer<ShadowPrimitive> = Renderer(resources)

    private class Renderer(resources: BackdropRenderResources) :
        ShadowPrimitiveRenderer<ShadowPrimitive>(resources) {
        override fun drawShadow(
            scope: DrawScope,
            outline: Outline,
            primitive: ShadowPrimitive,
        ) {
            renderer.draw(scope, outline, primitive.config)
        }
    }
}
