package top.ltfan.m3x.backdrop.highlight

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.skiaImageFilter
import org.jetbrains.skia.IRect
import org.jetbrains.skia.ImageFilter
import top.ltfan.m3x.backdrop.BackdropImageInput
import top.ltfan.m3x.backdrop.RuntimeShader

internal actual val supportsMergedHighlightTiles: Boolean = true

internal actual fun mergeHighlightTiles(
    environment: BackdropImageInput,
    shader: RuntimeShader,
    tiles: HighlightTiles,
    field: HighlightSdfField?,
): RenderEffect? {
    if (field != null) return mergeSdfHighlightTiles(environment, shader, field)
    shader.setFloatUniform("origin", 0f, 0f)
    val effect = environment.runtimeShaderEffect(shader, "environment").skiaImageFilter
    val branches = arrayOfNulls<ImageFilter>(tiles.count)
    try {
        for (index in branches.indices) {
            val tile = tiles[index]
            val crop = IRect.makeLTRB(tile.left, tile.top, tile.right, tile.bottom)
            branches[index] = ImageFilter.makeOffset(0f, 0f, effect, crop)
        }
        return ImageFilter.makeMerge(branches, null).asComposeRenderEffect()
    } finally {
        branches.forEach { it?.close() }
        effect.close()
    }
}

private fun mergeSdfHighlightTiles(
    environment: BackdropImageInput,
    shader: RuntimeShader,
    field: HighlightSdfField,
): RenderEffect {
    shader.setFloatUniform("origin", 0f, 0f)
    val branches = arrayOfNulls<ImageFilter>(field.layout.tiles.size)
    try {
        for ((index, tile) in field.layout.tiles.withIndex()) {
            val output = tile.output
            val crop = IRect.makeLTRB(output.left, output.top, output.right, output.bottom)
            val effect =
                (field.bind(shader, index, Offset.Zero, environment)
                        ?: environment.runtimeShaderEffect(shader, "environment"))
                    .skiaImageFilter
            try {
                branches[index] = ImageFilter.makeOffset(0f, 0f, effect, crop)
            } finally {
                effect.close()
            }
        }
        return ImageFilter.makeMerge(branches, null).asComposeRenderEffect()
    } finally {
        branches.forEach { it?.close() }
    }
}
