package top.ltfan.m3x.backdrop

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.ColorSpace
import android.graphics.HardwareBufferRenderer
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RecordingCanvas
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.test.AndroidComposeUiTest
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assume.assumeTrue

internal actual fun initializeTestGraphics() {
    assumeTrue(
        "Floating-point graphics contracts require API 34 or newer",
        Build.VERSION.SDK_INT >= 34,
    )
}

@RequiresApi(33)
internal actual fun RuntimeShader.bindTestInput(name: String, input: RuntimeShader) {
    asAndroidRuntimeShader().setInputShader(name, input.asAndroidRuntimeShader())
}

@RequiresApi(34)
internal actual fun renderTestShader(
    shader: RuntimeShader,
    width: Int,
    height: Int,
    premultiplied: Boolean,
    tiles: List<IntRect>?,
): FloatArray =
    renderAndroidF16(width, height, premultiplied) {
        val paint =
            Paint().apply {
                this.shader = shader.asAndroidRuntimeShader()
                blendMode = BlendMode.SRC
            }
        if (tiles != null) {
            val clip =
                Path().apply {
                    for ((left, top, right, bottom) in tiles) addRect(
                        left.toFloat(),
                        top.toFloat(),
                        right.toFloat(),
                        bottom.toFloat(),
                        Path.Direction.CW,
                    )
                }
            clipPath(clip)
        }
        drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

@RequiresApi(34)
internal fun captureRenderTestContentF16(): FloatArray {
    val layer = checkNotNull(captureLayer) { "Test content has not been recorded" }
    val size = layer.size
    return renderAndroidF16(size.width, size.height, premultiplied = true) {
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(this), size.toSize()) {
            drawLayer(layer)
        }
    }
}

@RequiresApi(34)
private fun renderAndroidF16(
    width: Int,
    height: Int,
    premultiplied: Boolean,
    draw: RecordingCanvas.() -> Unit,
): FloatArray {
    val buffer =
        HardwareBuffer.create(
            width,
            height,
            HardwareBuffer.RGBA_FP16,
            1,
            HardwareBuffer.USAGE_GPU_COLOR_OUTPUT or HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE,
        )
    val node = RenderNode("BackdropF16Contract")
    val renderer = HardwareBufferRenderer(buffer)
    try {
        node.setPosition(0, 0, width, height)
        val canvas = node.beginRecording(width, height)
        canvas.drawColor(0, BlendMode.SRC)
        canvas.draw()
        node.endRecording()
        renderer.setContentRoot(node)
        val complete = CountDownLatch(1)
        var result: HardwareBufferRenderer.RenderResult? = null
        val colorSpace = ColorSpace.get(ColorSpace.Named.EXTENDED_SRGB)
        renderer.obtainRenderRequest().setColorSpace(colorSpace).draw({ it.run() }) {
            result = it
            complete.countDown()
        }
        check(complete.await(10, TimeUnit.SECONDS)) { "Shader probe timed out" }
        val rendered = checkNotNull(result)
        check(rendered.status == HardwareBufferRenderer.RenderResult.SUCCESS) {
            "Shader probe failed: ${rendered.status}"
        }
        rendered.fence.use { check(it.awaitForever()) { "Shader probe fence failed" } }
        val hardware = checkNotNull(Bitmap.wrapHardwareBuffer(buffer, colorSpace))
        try {
            val bitmap = checkNotNull(hardware.copy(Bitmap.Config.RGBA_F16, false))
            try {
                val bytes =
                    ByteBuffer.allocateDirect(bitmap.byteCount).order(ByteOrder.nativeOrder())
                bitmap.copyPixelsToBuffer(bytes)
                val pixels = FloatArray(width * height * 4)
                for (y in 0 until height) for (x in 0 until width) {
                    val base = y * bitmap.rowBytes + x * 8
                    val output = (y * width + x) * 4
                    for (channel in 0..3) pixels[output + channel] =
                        testHalfToFloat(bytes.getShort(base + channel * 2).toInt() and 0xffff)
                    if (!premultiplied && pixels[output + 3] > 0f) {
                        for (channel in 0..2) pixels[output + channel] /= pixels[output + 3]
                    }
                }
                return pixels
            } finally {
                bitmap.recycle()
            }
        } finally {
            hardware.recycle()
        }
    } finally {
        renderer.close()
        node.discardDisplayList()
        buffer.close()
    }
}

// Keep the test host fullscreen across the supported Android window API generations.
@Suppress("DEPRECATION")
internal actual fun prepareRenderTestWindow(test: ComposeUiTest) {
    captureLayer = null
    val activity = checkNotNull((test as AndroidComposeUiTest<*>).activity)
    test.runOnUiThread {
        if (Build.VERSION.SDK_INT >= 30) {
            activity.window.setDecorFitsSystemWindows(false)
            activity.window.insetsController?.hide(WindowInsets.Type.systemBars())
        } else {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }
}

// Compose UI tests own one host at a time; its recorded output retains alpha before window
// blending.
@Volatile private var captureLayer: GraphicsLayer? = null

@Composable
internal actual fun RenderTestContent(content: @Composable () -> Unit) {
    val layer = rememberGraphicsLayer()
    Box(
        Modifier.drawWithContent {
            layer.record { this@drawWithContent.drawContent() }
            captureLayer = layer
            drawLayer(layer)
        },
    ) {
        content()
    }
}
