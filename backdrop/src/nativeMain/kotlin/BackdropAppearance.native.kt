package top.ltfan.m3x.backdrop

import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

internal actual suspend fun readAppearancePixels(
    layer: GraphicsLayer,
    density: Density,
    layoutDirection: LayoutDirection,
    pixels: IntArray,
) {
    layer.toImageBitmap().readPixels(pixels)
}
