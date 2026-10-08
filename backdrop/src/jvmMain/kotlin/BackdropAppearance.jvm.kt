package top.ltfan.m3x.backdrop

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import java.lang.ref.Reference

internal actual suspend fun readAppearancePixels(
    layer: GraphicsLayer,
    density: Density,
    layoutDirection: LayoutDirection,
    pixels: IntArray,
) {
    val image = ImageBitmap(layer.size.width, layer.size.height)
    val canvas = Canvas(image)
    val skiaCanvas = canvas.skiaCanvas
    try {
        CanvasDrawScope().draw(density, layoutDirection, canvas, layer.size.toSize()) {
            drawLayer(layer)
        }
        image.readPixels(pixels)
    } finally {
        // Keep the destination Canvas reachable until the nested native draw completes.
        Reference.reachabilityFence(skiaCanvas)
        skiaCanvas.close()
    }
}
