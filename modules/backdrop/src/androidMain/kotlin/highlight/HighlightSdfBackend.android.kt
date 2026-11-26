package top.ltfan.m3x.backdrop.highlight

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlendMode
import android.graphics.ColorSpace
import android.graphics.HardwareBufferRenderer
import android.graphics.Paint
import android.graphics.RenderNode
import android.graphics.Shader
import android.hardware.HardwareBuffer
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RenderEffect
import java.time.Duration
import java.util.concurrent.Executor
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import top.ltfan.m3x.backdrop.BackdropImageInput
import top.ltfan.m3x.backdrop.RuntimeShader
import top.ltfan.m3x.backdrop.asAndroidRuntimeShader
import top.ltfan.m3x.backdrop.internal.BoundaryGeometry

internal actual val highlightGpuSdfAvailable: Boolean
    get() = Build.VERSION.SDK_INT >= 34

internal actual fun uploadHighlightSdf(
    layout: HighlightSdfLayout,
    pixels: IntArray,
): HighlightSdfField =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        bitmapField(
            layout,
            Bitmap.createBitmap(
                pixels,
                layout.size.width,
                layout.size.height,
                Bitmap.Config.ARGB_8888,
            ),
            usesValidityMask = true,
        )
    } else {
        error("Signed distance fields require runtime shader support")
    }

internal actual suspend fun bakeHighlightGpuSdf(
    layout: HighlightSdfLayout,
    geometry: BoundaryGeometry,
): HighlightSdfField? =
    if (Build.VERSION.SDK_INT >= 34)
        sdfGpuSlots.withPermit {
            withContext(Dispatchers.IO) { bakeHardwareField(layout, geometry) }
        }
    else null

@RequiresApi(33)
private fun bitmapField(
    layout: HighlightSdfLayout,
    bitmap: Bitmap,
    usesValidityMask: Boolean = false,
): HighlightSdfField {
    val texture =
        BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            filterMode = BitmapShader.FILTER_MODE_LINEAR
        }
    // Recorded shaders can hold the bitmap after this field is superseded.
    return object : HighlightSdfField {
        override val layout = layout
        override val usesValidityMask = usesValidityMask

        override fun bind(
            shader: RuntimeShader,
            index: Int,
            origin: Offset,
            environment: BackdropImageInput?,
        ): RenderEffect? {
            layout.bindTextureCoordinates(shader, index)
            shader.asAndroidRuntimeShader().setInputBuffer("sdfField", texture)
            return null
        }
    }
}

@RequiresApi(34)
private suspend fun bakeHardwareField(
    layout: HighlightSdfLayout,
    geometry: BoundaryGeometry,
): HighlightSdfField? {
    val width = layout.size.width
    val height = layout.size.height
    val usage = HardwareBuffer.USAGE_GPU_COLOR_OUTPUT or HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE
    if (!HardwareBuffer.isSupported(width, height, HardwareBuffer.RGBA_8888, 1, usage)) return null
    val space = ColorSpace.get(ColorSpace.Named.EXTENDED_SRGB)
    val shader = geometry.createSdfBakeShader()
    shader.setFloatUniform("sdfStep", layout.divisor.toFloat())
    shader.setFloatUniform("sdfRange", layout.range)
    val native = shader.asAndroidRuntimeShader()
    // Before API 37, RuntimeShader evaluates in the destination color space.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
        native.setWorkingColorSpace(space)
    }
    val paint =
        Paint().apply {
            this.shader = native
            blendMode = SRC
        }
    val node = RenderNode("Contour SDF")
    node.setPosition(0, 0, width, height)
    val canvas = node.beginRecording(width, height)
    try {
        canvas.drawColor(0, BlendMode.SRC)
        for (tile in layout.tiles) {
            val x = tile.offset.x
            val y = tile.offset.y
            shader.setFloatUniform(
                "origin",
                x * layout.divisor - tile.origin.x,
                y * layout.divisor - tile.origin.y,
            )
            // drawRect records an immutable native shader for the current uniforms.
            paint.shader = native
            canvas.drawRect(
                x.toFloat(),
                y.toFloat(),
                (x + tile.size.width).toFloat(),
                (y + tile.size.height).toFloat(),
                paint,
            )
        }
    } finally {
        node.endRecording()
    }
    return HardwareBuffer.create(width, height, HardwareBuffer.RGBA_8888, 1, usage).use { buffer ->
        try {
            HardwareBufferRenderer(buffer).use { renderer ->
                renderer.setContentRoot(node)
                val ready = suspendCancellableCoroutine { continuation ->
                    renderer.obtainRenderRequest().setColorSpace(space).draw(sdfFenceExecutor) {
                        result ->
                        val complete =
                            result.fence.use { fence ->
                                result.status == HardwareBufferRenderer.RenderResult.SUCCESS &&
                                    fence.await(Duration.ofSeconds(2))
                            }
                        continuation.resume(complete)
                    }
                }
                if (!ready) null
                else Bitmap.wrapHardwareBuffer(buffer, space)?.let { bitmapField(layout, it) }
            }
        } finally {
            node.discardDisplayList()
        }
    }
}

private val sdfFenceExecutor = Executor { task ->
    Dispatchers.IO.dispatch(EmptyCoroutineContext, task)
}

private val sdfGpuSlots = Semaphore(2)
