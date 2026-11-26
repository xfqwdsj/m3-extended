package top.ltfan.m3x.backdrop.highlight

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import top.ltfan.m3x.backdrop.BackdropImageInput
import top.ltfan.m3x.backdrop.BuiltinRuntimeShader
import top.ltfan.m3x.backdrop.RuntimeShader
import top.ltfan.m3x.backdrop.internal.BoundaryGeometry
import top.ltfan.m3x.backdrop.internal.analyticBoundaryShaderFunctions
import top.ltfan.m3x.backdrop.internal.bindBoundary
import top.ltfan.m3x.backdrop.internal.shaderUniformCapacity

internal class HighlightSdfLayout(outputs: List<IntRect>, val divisor: Int, maximumWidth: Float) {
    // The extra texel on each side supplies the bilinear footprint at tile and outline edges.
    internal class Tile(val output: IntRect, val offset: IntOffset, divisor: Int) {
        val size = IntSize((output.width - 1) / divisor + 3, (output.height - 1) / divisor + 3)
        val origin = Offset((output.left - divisor).toFloat(), (output.top - divisor).toFloat())
    }

    val range = maximumWidth + 2f * divisor
    val tiles: List<Tile>
    val size: IntSize
    val bandTexels: Long

    init {
        val packing = mutableListOf<Tile>()
        val width = (outputs.maxOf { it.width } - 1) / divisor + 3
        var x = 0
        var y = 0
        var rowHeight = 0
        var texels = 0L
        for (output in outputs) {
            val tileWidth = (output.width - 1) / divisor + 3
            if (x > width - tileWidth) {
                x = 0
                y += rowHeight
                rowHeight = 0
            }
            val tile = Tile(output, IntOffset(x, y), divisor)
            packing += tile
            x += tileWidth
            rowHeight = maxOf(rowHeight, tile.size.height)
            texels += tileWidth.toLong() * tile.size.height
        }
        tiles = packing
        size = IntSize(width, y + rowHeight)
        bandTexels = texels
    }

    val fitsBudget: Boolean
        get() =
            size.width in 1..4096 &&
                size.height in 1..4096 &&
                size.width.toLong() * size.height <= 1_048_576L
}

/** One immutable field for all the tiles in a presentation. */
internal interface HighlightSdfField : AutoCloseable {
    val layout: HighlightSdfLayout

    /** Whether empty texels use alpha as an invalid sample marker. */
    val usesValidityMask: Boolean
        get() = false

    /** Binds the field and optionally constructs the effect that produces it at replay time. */
    fun bind(
        shader: RuntimeShader,
        index: Int,
        origin: Offset,
        environment: BackdropImageInput?,
    ): RenderEffect?

    override fun close() {}
}

internal fun HighlightSdfLayout.bindTextureCoordinates(shader: RuntimeShader, index: Int) {
    val tile = tiles[index]
    shader.setFloatUniform(
        "sdfMap",
        tile.offset.x - tile.origin.x / divisor,
        tile.offset.y - tile.origin.y / divisor,
        1f / divisor,
        range,
    )
}

internal expect val highlightGpuSdfAvailable: Boolean

internal expect fun uploadHighlightSdf(
    layout: HighlightSdfLayout,
    pixels: IntArray,
): HighlightSdfField

internal expect suspend fun bakeHighlightGpuSdf(
    layout: HighlightSdfLayout,
    geometry: BoundaryGeometry,
): HighlightSdfField?

internal suspend fun bakeHighlightCpuSdf(
    layout: HighlightSdfLayout,
    geometry: BoundaryGeometry,
    cullFarSamples: Boolean = true,
): IntArray {
    val pixels = IntArray(layout.size.width * layout.size.height)
    val coroutine = currentCoroutineContext()
    val segments =
        if (cullFarSamples && !geometry.isAnalytic) geometry.cpuGeometry().segments else null
    for (tile in layout.tiles) {
        val candidates = segments?.let {
            candidateTexels(tile, layout.divisor, layout.range, geometry.coordinateScale, it)
        }
        for (y in 0 until tile.size.height) {
            coroutine.ensureActive()
            val py = tile.origin.y + (y + .5f) * layout.divisor
            var i = (y + tile.offset.y) * layout.size.width + tile.offset.x
            for (x in 0 until tile.size.width) {
                if (candidates != null && candidates[y * (tile.size.width + 1) + x] == 0) {
                    i++
                    continue
                }
                val px = tile.origin.x + (x + .5f) * layout.divisor
                val sample = geometry.nearestBoundary(Offset(px, py))
                val r = encodeSdfChannel(sample.signedDistance / layout.range)
                val g = encodeSdfChannel(sample.outwardNormal.x)
                val b = encodeSdfChannel(sample.outwardNormal.y)
                pixels[i++] = 0xff000000.toInt() or (r shl 16) or (g shl 8) or b
            }
        }
    }
    return pixels
}

private fun encodeSdfChannel(value: Float): Int =
    (128f + value.coerceIn(-1f, 1f) * 127f).roundToInt()

internal fun BoundaryGeometry.createSdfBakeShader(): RuntimeShader {
    if (isAnalytic) return AnalyticSdfBakeShader.create().also { configureShader(it) }

    val capacity = shaderUniformCapacity(segmentCount)
    val groupSize = segmentGroupSize(capacity)
    val functions = shaderFunctions(capacity, groupSize)
    return RuntimeShader(
            sdfBakeSource(functions, "nearestBoundaryWithPosition(p, sdfRange, closest)"),
        )
        .also { bindBoundary(it, capacity, groupSize) }
}

private val AnalyticSdfBakeShader by lazy {
    BuiltinRuntimeShader(
        sdfBakeSource(analyticBoundaryShaderFunctions(), "nearestBoundaryWithPosition(p, closest)"),
    )
}

// Expanded segment AABBs cover the entire distance range, including the interpolation footprint.
private fun candidateTexels(
    tile: HighlightSdfLayout.Tile,
    divisor: Int,
    range: Float,
    coordinateScale: Float,
    segments: FloatArray,
): IntArray {
    val width = tile.size.width
    val height = tile.size.height
    val stride = width + 1
    val counts = IntArray(stride * (height + 1))
    val margin = range + maxOf(.001f, coordinateScale * .000002f)
    for (i in segments.indices step 4) {
        val x1 = segments[i]
        val y1 = segments[i + 1]
        val x2 = segments[i + 2]
        val y2 = segments[i + 3]
        val l =
            ceil((minOf(x1, x2) - margin - tile.origin.x) / divisor - .5f)
                .toInt()
                .coerceIn(0, width)
        val r =
            (floor((maxOf(x1, x2) + margin - tile.origin.x) / divisor - .5f) + 1f)
                .toInt()
                .coerceIn(
                    0,
                    width,
                )
        val t =
            ceil((minOf(y1, y2) - margin - tile.origin.y) / divisor - .5f)
                .toInt()
                .coerceIn(0, height)
        val b =
            (floor((maxOf(y1, y2) + margin - tile.origin.y) / divisor - .5f) + 1f)
                .toInt()
                .coerceIn(
                    0,
                    height,
                )
        if (l >= r || t >= b) continue
        counts[t * stride + l]++
        counts[t * stride + r]--
        counts[b * stride + l]--
        counts[b * stride + r]++
    }
    for (y in 0 until height) {
        var row = 0
        for (x in 0 until width) {
            val i = y * stride + x
            row += counts[i]
            counts[i] = row + if (y == 0) 0 else counts[i - stride]
        }
    }
    return counts
}

private fun sdfBakeSource(functions: String, nearest: String): String =
    """
$functions
uniform float sdfStep;
uniform float sdfRange;

half4 main(float2 coord) {
  float2 p = coord * sdfStep - origin;
  float2 closest;
  float3 sample = $nearest;
  float distance = min(sample.x, sdfRange);
  if (containsPoint(p)) distance = -distance;
  float3 value = float3(distance / sdfRange, sample.yz);
  return half4((128.0 + clamp(value, -1.0, 1.0) * 127.0) / 255.0, 1.0);
}
"""
