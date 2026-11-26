package top.ltfan.m3x.backdrop.shadow

import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawscope.DrawScope
import top.ltfan.m3x.backdrop.BackdropPrimitive
import top.ltfan.m3x.backdrop.BackdropPrimitiveDrawContext
import top.ltfan.m3x.backdrop.BackdropPrimitiveRenderer
import top.ltfan.m3x.backdrop.BackdropRenderResources

/** Resolves a shape and delegates its drawing to the shared shadow layer owner. */
internal abstract class ShadowPrimitiveRenderer<P : BackdropPrimitive<P>>(
    resources: BackdropRenderResources,
) : BackdropPrimitiveRenderer<P> {
    protected val renderer = ShadowRenderer(resources)

    final override fun draw(
        scope: DrawScope,
        primitive: P,
        context: BackdropPrimitiveDrawContext,
    ) {
        drawShadow(
            scope,
            context.shape.createOutline(context.size, context.layoutDirection, context.density),
            primitive,
        )
    }

    protected abstract fun drawShadow(scope: DrawScope, outline: Outline, primitive: P)

    final override fun refresh() {
        renderer.release()
    }

    final override fun release() {
        renderer.release()
    }
}
