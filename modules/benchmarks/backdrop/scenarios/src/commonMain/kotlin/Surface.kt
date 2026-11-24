package top.ltfan.m3x.backdrop.benchmark.scenarios

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.CompositingStrategy
import top.ltfan.m3x.backdrop.Backdrop
import top.ltfan.m3x.backdrop.BackdropEffectScope
import top.ltfan.m3x.backdrop.BackdropRoom
import top.ltfan.m3x.backdrop.drawBackdrop
import top.ltfan.m3x.backdrop.effects.blur
import top.ltfan.m3x.backdrop.effects.lens
import top.ltfan.m3x.backdrop.effects.vibrancy
import top.ltfan.m3x.backdrop.highlight.Highlight
import top.ltfan.m3x.backdrop.highlight.highlight
import top.ltfan.m3x.backdrop.shadow.Shadow
import top.ltfan.m3x.backdrop.shadow.shadow

private val contour = Highlight()

/** Draws shared optics and contour lighting with independent background opacity. */
internal fun Modifier.benchmarkSurface(
    source: Backdrop,
    shape: () -> Shape,
    phase: () -> Float,
    backgroundAlpha: () -> Float = { 1f },
): Modifier =
    drawBackdrop(source, shape, backgroundAlpha = backgroundAlpha) {
        val optics = image {
            source()
            effects { scenarioOptics(phase) }
        }
        val material = image {
            replace(optics)
            clipSurface()
        }
        val environment = image { replace(optics) }
        group {
            shadow(Shadow.Default)
            draw(material)
            paintSurface(CompositingStrategy.Auto) { room -> scenarioTint(room) }
            highlight(contour, environment = environment)
        }
    }

private fun BackdropEffectScope.scenarioOptics(
    phase: () -> Float,
) {
    val radius = (8f + phase().coerceIn(0f, 1f) * 20f) * density
    blur(radius)
    vibrancy()
    if (shape is CornerBasedShape) {
        lens(24f * density, 18f * density, depthEffect = true)
    }
}

private fun DrawScope.scenarioTint(room: BackdropRoom) {
    drawRect(
        Color.White.copy(alpha = .18f),
        room.extension.originInNode,
        Size(
            room.size.width + room.extension.left + room.extension.right,
            room.size.height + room.extension.top + room.extension.bottom,
        ),
    )
}
