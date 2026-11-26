package top.ltfan.m3x.backdrop.highlight

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorInfo
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Matrix33
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PictureRecorder
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Shader
import org.jetbrains.skia.impl.use
import top.ltfan.m3x.backdrop.BackdropImageInput
import top.ltfan.m3x.backdrop.RuntimeShader
import top.ltfan.m3x.backdrop.asSkikoRuntimeShader
import top.ltfan.m3x.backdrop.internal.BoundaryGeometry
import top.ltfan.m3x.backdrop.internal.runtimeShaderInputFilter

internal actual val highlightGpuSdfAvailable: Boolean = true

internal actual fun uploadHighlightSdf(
    layout: HighlightSdfLayout,
    pixels: IntArray,
): HighlightSdfField {
    val bytes = ByteArray(pixels.size * 4)
    for (index in pixels.indices) {
        val pixel = pixels[index]
        val i = index * 4
        bytes[i] = (pixel ushr 16).toByte()
        bytes[i + 1] = (pixel ushr 8).toByte()
        bytes[i + 2] = pixel.toByte()
        bytes[i + 3] = (pixel ushr 24).toByte()
    }
    val info =
        ImageInfo(
            ColorInfo(ColorType.RGBA_8888, ColorAlphaType.PREMUL, null),
            layout.size.width,
            layout.size.height,
        )
    val texture =
        Image.makeRaster(info, bytes, layout.size.width * 4).use {
            it.makeShader(FilterTileMode.CLAMP, FilterTileMode.CLAMP, SamplingMode.LINEAR, null)
        }
    return object : HighlightSdfField {
        override val layout = layout
        override val usesValidityMask = true

        override fun bind(
            shader: RuntimeShader,
            index: Int,
            origin: Offset,
            environment: BackdropImageInput?,
        ): RenderEffect? {
            layout.bindTextureCoordinates(shader, index)
            shader.asSkikoRuntimeShader().child("sdfField", texture)
            return null
        }

        override fun close() = texture.close()
    }
}

internal actual suspend fun bakeHighlightGpuSdf(
    layout: HighlightSdfLayout,
    geometry: BoundaryGeometry,
): HighlightSdfField? {
    val shader = geometry.createSdfBakeShader()
    val filters = mutableListOf<ImageFilter>()
    try {
        shader.setFloatUniform("sdfStep", layout.divisor.toFloat())
        shader.setFloatUniform("sdfRange", layout.range)
        for (tile in layout.tiles) {
            shader.setFloatUniform("origin", -tile.origin.x, -tile.origin.y)
            val size = tile.size
            val rect = Rect.makeWH(size.width.toFloat(), size.height.toFloat())
            shader.asSkikoRuntimeShader().makeShader().use { bake ->
                filters += recordFieldPicture(rect, bake)
            }
        }
    } catch (failure: Throwable) {
        filters.forEach { it.close() }
        throw failure
    } finally {
        shader.asSkikoRuntimeShader().close()
    }
    return ReplayHighlightSdf(layout, filters)
}

private fun recordFieldPicture(rect: Rect, bake: Shader): ImageFilter =
    PictureRecorder().use { recorder ->
        Paint().use { paint ->
            paint.shader = bake
            paint.blendMode = BlendMode.SRC
            recorder.beginRecording(rect).drawRect(rect, paint)
        }
        recorder.finishRecordingAsPicture().use { ImageFilter.makePicture(it, rect) }
    }

private class ReplayHighlightSdf(
    override val layout: HighlightSdfLayout,
    private val pictures: List<ImageFilter>,
) : HighlightSdfField {
    private val transformed = arrayOfNulls<ImageFilter>(pictures.size)

    override fun bind(
        shader: RuntimeShader,
        index: Int,
        origin: Offset,
        environment: BackdropImageInput?,
    ): RenderEffect {
        val tile = layout.tiles[index]
        val scale = layout.divisor.toFloat()
        val field =
            transformed[index]
                ?: ImageFilter.makeMatrixTransform(
                        Matrix33.makeTranslate(origin.x + tile.origin.x, origin.y + tile.origin.y)
                            .makeConcat(Matrix33.makeScale(scale)),
                        SamplingMode.LINEAR,
                        pictures[index],
                    )
                    .also { transformed[index] = it }
        // Both children use this layer's coordinates. The field picture only contains geometry.
        shader.setFloatUniform("sdfMap", origin.x, origin.y, 1f, layout.range)
        val builder = shader.asSkikoRuntimeShader()
        if (environment == null) {
            return ImageFilter.makeRuntimeShader(builder, "sdfField", field).asComposeRenderEffect()
        }
        val output =
            ComposeRect(
                tile.output.left + origin.x,
                tile.output.top + origin.y,
                tile.output.right + origin.x,
                tile.output.bottom + origin.y,
            )
        return runtimeShaderInputFilter(environment.bounds.translate(origin), output).use { input ->
            ImageFilter.makeRuntimeShader(
                    builder,
                    arrayOf("sdfField", "environment"),
                    arrayOf(field, input),
                )
                .asComposeRenderEffect()
        }
    }

    override fun close() {
        transformed.forEach { it?.close() }
        pictures.forEach { it.close() }
    }
}
