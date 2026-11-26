package top.ltfan.m3x.compose.shape

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastForEach
import kotlin.math.sqrt

/**
 * Prepares distance to the supplied boundary segments, using the contour's non-zero winding rule
 * for the sign. The loops must trace the filled boundary.
 *
 * @param flatness maximum curve-to-segment deviation in local pixels.
 * @return null if the boundary has no measurable segments.
 * @throws IllegalArgumentException if [flatness] is negative or non-finite, or the requested
 *   tolerance exceeds the supported subdivision budget.
 */
public fun Contour.toSignedDistanceField(flatness: Float = Flatness): SignedDistanceField? {
    val circle = loops.singleOrNull()?.singleOrNull() as? PathSegment.Circle
    if (circle != null && circle.radius > 0.0) {
        requireFlatness(flatness)
        return CircleDistance(
            circle.center.x.toFloat(),
            circle.center.y.toFloat(),
            circle.radius.toFloat(),
        )
    }
    val boundary = toFlattenedContour(flatness) ?: return null
    return boundary.distanceField(evenOdd = false)
}

/**
 * Prepares distance to the boundary of this path's filled region. Open subpaths use the same
 * implicit closure as filling.
 *
 * @param flatness maximum curve-to-segment deviation in local pixels.
 * @return null if the fill is empty or its boundary cannot be extracted.
 * @throws IllegalArgumentException if [flatness] is negative or non-finite, or the requested
 *   tolerance exceeds the supported subdivision budget.
 */
public fun Path.toSignedDistanceField(flatness: Float = Flatness): SignedDistanceField? {
    val boundary = flattenBoundary(flatness)
    return boundary.contour?.distanceField(boundary.evenOdd)
}

internal const val Flatness = .05f

private fun requireFlatness(flatness: Float) {
    require(flatness.isFinite() && flatness >= 0f) {
        "flatness must be finite and non-negative"
    }
}

private fun Contour.distanceField(evenOdd: Boolean): SignedDistanceField? {
    val builder = DistanceBuilder()
    loops.fastForEach { loop -> loop.fastForEach(builder::append) }
    return builder.build(evenOdd)
}

private class CircleDistance(
    private val x: Float,
    private val y: Float,
    private val radius: Float,
) : SignedDistanceField {
    override fun distanceAt(x: Float, y: Float): Float {
        val dx = x - this.x
        val dy = y - this.y
        return sqrt(dx * dx + dy * dy) - radius
    }
}

private class PolygonDistance(
    private val edges: FloatArray,
    private val evenOdd: Boolean,
) : SignedDistanceField {
    override fun distanceAt(x: Float, y: Float): Float {
        var bestSquared = Float.POSITIVE_INFINITY
        var winding = 0
        for (index in edges.indices step EdgeStride) {
            val ax = edges[index]
            val ay = edges[index + 1]
            val dx = edges[index + 2]
            val dy = edges[index + 3]
            val by = edges[index + 4]
            val relX = x - ax
            val relY = y - ay
            val t = ((relX * dx + relY * dy) * edges[index + 5]).fastCoerceIn(0f, 1f)
            val distanceX = relX - dx * t
            val distanceY = relY - dy * t
            bestSquared = minOf(bestSquared, distanceX * distanceX + distanceY * distanceY)
            if ((ay > y) != (by > y)) {
                val cross = dx * relY - dy * relX
                if (dy > 0f && cross > 0f) winding++
                if (dy < 0f && cross < 0f) winding--
            }
        }
        val distance = sqrt(bestSquared)
        val inside = if (evenOdd) winding % 2 != 0 else winding != 0
        return if (inside) -distance else distance
    }
}

private const val EdgeStride = 6

private class DistanceBuilder {
    private var edges = FloatArray(16 * EdgeStride)
    private var used = 0

    fun append(segment: PathSegment) {
        if (segment !is PathSegment.Line) return
        line(
            segment.start.x.toFloat(),
            segment.start.y.toFloat(),
            segment.end.x.toFloat(),
            segment.end.y.toFloat(),
        )
    }

    private fun line(ax: Float, ay: Float, bx: Float, by: Float) {
        val dx = bx - ax
        val dy = by - ay
        val squaredLength = dx * dx + dy * dy
        if (squaredLength == 0f) return
        if (used == edges.size) edges = edges.copyOf(edges.size * 2)
        edges[used++] = ax
        edges[used++] = ay
        edges[used++] = dx
        edges[used++] = dy
        edges[used++] = by
        edges[used++] = 1f / squaredLength
    }

    fun build(evenOdd: Boolean): SignedDistanceField? =
        if (used == 0) null else PolygonDistance(edges.copyOf(used), evenOdd)
}
