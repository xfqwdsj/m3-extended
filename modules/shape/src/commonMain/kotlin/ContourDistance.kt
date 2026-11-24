package top.ltfan.m3x.compose.shape

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathIterator
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastIsFinite
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Prepares distance to the supplied boundary segments, using the contour's non-zero winding rule
 * for the sign. The loops must trace the filled boundary.
 *
 * @param flatness maximum curve-to-segment deviation in local pixels.
 * @return null if the boundary has no measurable segments.
 */
public fun Contour.toSignedDistanceField(flatness: Float = Flatness): SignedDistanceField? {
    require(flatness.isFinite() && flatness >= 0f) {
        "flatness must be finite and non-negative"
    }
    val circle = loops.singleOrNull()?.singleOrNull() as? PathSegment.Circle
    if (circle != null && circle.radius > 0.0) {
        return CircleDistance(
            circle.center.x.toFloat(),
            circle.center.y.toFloat(),
            circle.radius.toFloat(),
        )
    }
    val builder = DistanceBuilder(flatness)
    loops.fastForEach { loop ->
        loop.fastForEach(builder::append)
        builder.closeLoop()
    }
    return builder.build()
}

/**
 * Prepares distance to the boundary of this path's filled region. Open subpaths use the same
 * implicit closure as filling.
 *
 * @param flatness maximum curve-to-segment deviation in local pixels.
 * @return null if the fill is empty or its boundary cannot be extracted.
 */
public fun Path.toSignedDistanceField(flatness: Float = Flatness): SignedDistanceField? {
    require(flatness.isFinite() && flatness >= 0f) {
        "flatness must be finite and non-negative"
    }
    val boundary = Path()
    if (!boundary.op(this, Path(), PathOperation.Union) || boundary.isEmpty) return null
    val builder = DistanceBuilder(flatness)
    if (!builder.appendBoundary(boundary)) return null
    return builder.build(evenOdd = boundary.fillType == PathFillType.EvenOdd)
}

internal const val Flatness = .05f

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

private class DistanceBuilder(
    private val flatness: Float,
) {
    private val flatnessSquared = flatness * flatness
    private var edges = FloatArray(16 * EdgeStride)
    private var used = 0
    private var first: Point? = null
    private var last: Point? = null

    fun appendBoundary(path: Path): Boolean {
        val iterator = PathIterator(path, PathIterator.ConicEvaluation.AsConic)
        val points = FloatArray(8)
        var startX = 0f
        var startY = 0f
        var endX = 0f
        var endY = 0f
        var open = false

        fun closeBoundary() {
            if (open) line(endX, endY, startX, startY)
            open = false
        }

        while (iterator.hasNext()) {
            when (val type = iterator.next(points)) {
                Move -> {
                    closeBoundary()
                    startX = points[0]
                    startY = points[1]
                    endX = startX
                    endY = startY
                    open = true
                }
                Line -> {
                    endX = points[2]
                    endY = points[3]
                    line(points[0], points[1], endX, endY)
                }
                Quadratic,
                Conic -> {
                    endX = points[4]
                    endY = points[5]
                    val weight = if (type == Conic) points[6] else 1f
                    if (!weight.fastIsFinite() || weight <= 0f) return false
                    conic(points[0], points[1], points[2], points[3], endX, endY, weight, 0)
                }
                Cubic -> {
                    endX = points[6]
                    endY = points[7]
                    cubic(
                        points[0],
                        points[1],
                        points[2],
                        points[3],
                        points[4],
                        points[5],
                        endX,
                        endY,
                        0,
                    )
                }
                Close -> closeBoundary()
                Done -> break
            }
        }
        closeBoundary()
        return true
    }

    fun append(segment: PathSegment) {
        val start = segment.start
        val end = segment.end
        if (first == null) first = start
        last = end
        when (segment) {
            is Line ->
                line(
                    start.x.toFloat(),
                    start.y.toFloat(),
                    end.x.toFloat(),
                    end.y.toFloat(),
                )
            is Circular -> arc(segment, start, end)
            is Cubic ->
                cubic(
                    start.x.toFloat(),
                    start.y.toFloat(),
                    segment.control1.x.toFloat(),
                    segment.control1.y.toFloat(),
                    segment.control2.x.toFloat(),
                    segment.control2.y.toFloat(),
                    end.x.toFloat(),
                    end.y.toFloat(),
                    0,
                )
        }
    }

    fun closeLoop() {
        val start = first ?: return
        val end = checkNotNull(last)
        line(end.x.toFloat(), end.y.toFloat(), start.x.toFloat(), start.y.toFloat())
        first = null
        last = null
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

    private fun cubic(
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
        cx: Float,
        cy: Float,
        dx: Float,
        dy: Float,
        depth: Int,
    ) {
        if (
            depth == 18 ||
                (chordDistanceSquared(bx, by, ax, ay, dx, dy) <= flatnessSquared &&
                    chordDistanceSquared(cx, cy, ax, ay, dx, dy) <= flatnessSquared)
        ) {
            line(ax, ay, dx, dy)
            return
        }
        val abX = (ax + bx) * .5f
        val abY = (ay + by) * .5f
        val bcX = (bx + cx) * .5f
        val bcY = (by + cy) * .5f
        val cdX = (cx + dx) * .5f
        val cdY = (cy + dy) * .5f
        val leftX = (abX + bcX) * .5f
        val leftY = (abY + bcY) * .5f
        val rightX = (bcX + cdX) * .5f
        val rightY = (bcY + cdY) * .5f
        val middleX = (leftX + rightX) * .5f
        val middleY = (leftY + rightY) * .5f
        cubic(ax, ay, abX, abY, leftX, leftY, middleX, middleY, depth + 1)
        cubic(middleX, middleY, rightX, rightY, cdX, cdY, dx, dy, depth + 1)
    }

    private fun conic(
        ax: Float,
        ay: Float,
        cx: Float,
        cy: Float,
        bx: Float,
        by: Float,
        weight: Float,
        depth: Int,
    ) {
        if (depth == 18 || chordDistanceSquared(cx, cy, ax, ay, bx, by) <= flatnessSquared) {
            line(ax, ay, bx, by)
            return
        }
        // Subdivide in homogeneous coordinates, then normalize each child's endpoint weights.
        val inverse = 1f / (1f + weight)
        val leftX = (ax + weight * cx) * inverse
        val leftY = (ay + weight * cy) * inverse
        val rightX = (weight * cx + bx) * inverse
        val rightY = (weight * cy + by) * inverse
        val midX = (leftX + rightX) * .5f
        val midY = (leftY + rightY) * .5f
        val childWeight = sqrt((1f + weight) * .5f)
        conic(ax, ay, leftX, leftY, midX, midY, childWeight, depth + 1)
        conic(midX, midY, rightX, rightY, bx, by, childWeight, depth + 1)
    }

    private fun arc(segment: PathSegment.Circular, start: Point, end: Point) {
        if (segment.radius <= 0.0 || segment.sweepRadians == 0.0) return
        val step =
            if (segment.radius <= flatness) PI * 0.5
            else 2.0 * acos((1.0 - flatness.toDouble() / segment.radius).fastCoerceIn(-1.0, 1.0))
        val count = ceil(abs(segment.sweepRadians) / step).toInt().fastCoerceAtLeast(1)
        var previous = start
        for (index in 1..count) {
            val next = if (index == count) end else segment.pointAt(index.toDouble() / count)
            line(previous.x.toFloat(), previous.y.toFloat(), next.x.toFloat(), next.y.toFloat())
            previous = next
        }
    }

    fun build(evenOdd: Boolean = false): SignedDistanceField? =
        if (used == 0) null else PolygonDistance(edges.copyOf(used), evenOdd)
}

private fun chordDistanceSquared(
    px: Float,
    py: Float,
    ax: Float,
    ay: Float,
    bx: Float,
    by: Float,
): Float {
    val dx = bx - ax
    val dy = by - ay
    val rx = px - ax
    val ry = py - ay
    val lengthSquared = dx * dx + dy * dy
    val t =
        if (lengthSquared == 0f) 0f else ((rx * dx + ry * dy) / lengthSquared).fastCoerceIn(0f, 1f)
    val ex = rx - t * dx
    val ey = ry - t * dy
    return ex * ex + ey * ey
}
