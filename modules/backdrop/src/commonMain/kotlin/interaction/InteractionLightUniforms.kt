package top.ltfan.m3x.backdrop.interaction

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.util.fastIsFinite
import kotlin.math.abs
import kotlin.math.sqrt
import top.ltfan.m3x.backdrop.BackdropCurve
import top.ltfan.m3x.backdrop.RuntimeShader

/**
 * Reusable source arrays and the local-to-window homography shared by interior and contour shaders.
 */
internal class InteractionLightUniforms(val capacity: Int) {
    val shaderCapacity: Int
        get() = (maxOf(4, count) - 1).takeHighestOneBit() * 2

    val rowX = FloatArray(4)
    val rowY = FloatArray(4)
    val rowW = FloatArray(4)
    val positions = FloatArray(capacity * 4)
    val colors = FloatArray(capacity * 4)
    val parameters = FloatArray(capacity * 4)
    val curves = FloatArray(capacity * 4)
    var count: Int = 0
        private set

    var mayCoverReceiver: Boolean = true
        private set

    var coverageBounds: Rect = Rect.Zero
        private set

    fun clear() {
        count = 0
        mayCoverReceiver = false
        coverageBounds = Rect.Zero
    }

    fun prepare(
        entries: List<InteractionLightEntry>,
        coordinates: LayoutCoordinates,
        size: Size,
    ): Boolean {
        count = 0
        val width = size.width
        val height = size.height
        if (!(width > 0f && height > 0f)) return false
        val a = coordinates.localToWindow(Offset.Zero)
        val b = coordinates.localToWindow(Offset(width, 0f))
        val d = coordinates.localToWindow(Offset(0f, height))
        val e = coordinates.localToWindow(Offset(width, height))
        if (
            !a.x.fastIsFinite() ||
                !a.y.fastIsFinite() ||
                !b.x.fastIsFinite() ||
                !b.y.fastIsFinite() ||
                !d.x.fastIsFinite() ||
                !d.y.fastIsFinite() ||
                !e.x.fastIsFinite() ||
                !e.y.fastIsFinite()
        )
            return false
        val dx1 = b.x - e.x
        val dx2 = d.x - e.x
        val dx3 = a.x - b.x - d.x + e.x
        val dy1 = b.y - e.y
        val dy2 = d.y - e.y
        val dy3 = a.y - b.y - d.y + e.y
        val denominator = dx1 * dy2 - dx2 * dy1
        if (!denominator.fastIsFinite() || abs(denominator) <= 1e-8f) return false
        val g = (dx3 * dy2 - dx2 * dy3) / denominator
        val k = (dx1 * dy3 - dx3 * dy1) / denominator
        rowX[0] = (b.x - a.x + g * b.x) / width
        rowX[1] = (d.x - a.x + k * d.x) / height
        rowX[2] = a.x
        rowY[0] = (b.y - a.y + g * b.y) / width
        rowY[1] = (d.y - a.y + k * d.y) / height
        rowY[2] = a.y
        rowW[0] = g / width
        rowW[1] = k / height
        rowW[2] = 1f
        rowX[3] = 0f
        rowY[3] = 0f
        rowW[3] = 0f
        if (
            !rowX[0].fastIsFinite() ||
                !rowX[1].fastIsFinite() ||
                !rowX[2].fastIsFinite() ||
                !rowY[0].fastIsFinite() ||
                !rowY[1].fastIsFinite() ||
                !rowY[2].fastIsFinite() ||
                !rowW[0].fastIsFinite() ||
                !rowW[1].fastIsFinite() ||
                !rowW[2].fastIsFinite()
        )
            return false
        // Affine maps attain their coordinate extrema at the receiver corners. Perspective
        // receivers retain the general field path because an interior pole can exceed them.
        val minX = minOf(minOf(a.x, b.x), minOf(d.x, e.x))
        val maxX = maxOf(maxOf(a.x, b.x), maxOf(d.x, e.x))
        val minY = minOf(minOf(a.y, b.y), minOf(d.y, e.y))
        val maxY = maxOf(maxOf(a.y, b.y), maxOf(d.y, e.y))
        val scale = maxOf(1f, maxOf(abs(minX), abs(maxX)), maxOf(abs(minY), abs(maxY)))
        val determinant = rowX[0] * rowY[1] - rowX[1] * rowY[0]
        val affine = g == 0f && k == 0f && abs(determinant) >= 1e-8f
        mayCoverReceiver = !affine
        coverageBounds = if (affine) Rect.Zero else Rect(0f, 0f, width, height)
        val inverseXx = if (affine) rowY[1] / determinant else 0f
        val inverseXy = if (affine) -rowX[1] / determinant else 0f
        val inverseYx = if (affine) -rowY[0] / determinant else 0f
        val inverseYy = if (affine) rowX[0] / determinant else 0f
        val radiusScaleX = sqrt(inverseXx * inverseXx + inverseXy * inverseXy)
        val radiusScaleY = sqrt(inverseYx * inverseYx + inverseYy * inverseYy)
        val receiver = Rect(0f, 0f, width, height)
        var finiteCount = 0
        // The shader reads the packed prefix defined by the active light count.
        for (entry in entries) {
            if (!entry.isFiniteForRendering()) continue
            require(++finiteCount <= capacity) {
                "Interaction shader capacity must cover every finite source"
            }
            if (entry.radiusPx <= 0f) continue
            if (affine) {
                // Include rounding in the uploaded homography and shader coordinate arithmetic.
                val guard =
                    maxOf(scale, maxOf(abs(entry.x), abs(entry.y)), entry.radiusPx) * 0.00001f
                val overlaps =
                    entry.x + entry.radiusPx + guard >= minX &&
                        entry.x - entry.radiusPx - guard <= maxX &&
                        entry.y + entry.radiusPx + guard >= minY &&
                        entry.y - entry.radiusPx - guard <= maxY
                if (!overlaps) continue
                mayCoverReceiver = true
                val dx = entry.x - rowX[2]
                val dy = entry.y - rowY[2]
                val localX = inverseXx * dx + inverseXy * dy
                val localY = inverseYx * dx + inverseYy * dy
                val extentX = (entry.radiusPx + guard) * radiusScaleX + 1f
                val extentY = (entry.radiusPx + guard) * radiusScaleY + 1f
                val lightBounds =
                    Rect(localX - extentX, localY - extentY, localX + extentX, localY + extentY)
                        .intersect(receiver)
                if (!lightBounds.isEmpty) {
                    coverageBounds =
                        coverageBounds.takeUnless { it.isEmpty }?.expandToInclude(lightBounds)
                            ?: lightBounds
                }
            }
            val offset = count++ * 4
            positions[offset] = entry.x
            positions[offset + 1] = entry.y
            val linear = entry.linearColor
            colors[offset] = linear.red
            colors[offset + 1] = linear.green
            colors[offset + 2] = linear.blue
            colors[offset + 3] = linear.alpha
            parameters[offset] = entry.radiusPx
            parameters[offset + 1] = entry.intensity
            when (val curve = entry.falloff) {
                BackdropCurve.Linear -> {
                    curves[offset] = 0f
                    curves[offset + 1] = 0f
                    curves[offset + 2] = 1f
                    curves[offset + 3] = 1f
                }
                is BackdropCurve.CubicBezier -> {
                    curves[offset] = curve.x1
                    curves[offset + 1] = curve.y1
                    curves[offset + 2] = curve.x2
                    curves[offset + 3] = curve.y2
                }
            }
        }
        if (affine && coverageBounds.isEmpty) mayCoverReceiver = false
        return true
    }

    fun upload(shader: RuntimeShader, packet: InteractionLightUniforms) {
        if (packet !== this) {
            val end = count * 4
            positions.copyInto(packet.positions, endIndex = end)
            colors.copyInto(packet.colors, endIndex = end)
            parameters.copyInto(packet.parameters, endIndex = end)
            curves.copyInto(packet.curves, endIndex = end)
        }
        shader.setFloatUniform("interactionRowX", rowX)
        shader.setFloatUniform("interactionRowY", rowY)
        shader.setFloatUniform("interactionRowW", rowW)
        shader.setFloatUniform("interactionPositions", packet.positions)
        shader.setFloatUniform("interactionColors", packet.colors)
        shader.setFloatUniform("interactionParameters", packet.parameters)
        shader.setFloatUniform("interactionCurves", packet.curves)
        shader.setIntUniform("interactionLightCount", count)
    }
}

private fun Rect.expandToInclude(other: Rect): Rect =
    Rect(
        minOf(left, other.left),
        minOf(top, other.top),
        maxOf(right, other.right),
        maxOf(bottom, other.bottom),
    )
