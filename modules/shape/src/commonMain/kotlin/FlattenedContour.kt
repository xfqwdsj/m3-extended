package top.ltfan.m3x.compose.shape

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathIterator
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastIsFinite
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Converts every curved segment to lines within [flatness] pixels of the source curve. The returned
 * contour keeps the source loop order and winding.
 *
 * @throws IllegalArgumentException if [flatness] is negative or non-finite, or the requested
 *   tolerance exceeds the supported subdivision budget.
 */
public fun Contour.toFlattenedContour(flatness: Float = Flatness): Contour? {
    requireValidFlatness(flatness)
    val budget = FlatteningBudget()
    val flattened = loops.mapNotNull { loop -> flattenLoop(loop, flatness, budget) }
    return flattened.takeIf { it.isNotEmpty() }?.let { Contour(*it.toTypedArray()) }
}

/**
 * Converts this path's filled boundary to ordered line contours. The fill boundary is normalized
 * before subdivision, and implicit path closure follows the same rule as filling.
 *
 * @throws IllegalArgumentException if [flatness] is negative or non-finite, or the requested
 *   tolerance exceeds the supported subdivision budget.
 */
public fun Path.toFlattenedContour(flatness: Float = Flatness): Contour? =
    flattenBoundary(flatness).contour

internal data class FlattenedPathBoundary(
    val contour: Contour?,
    val evenOdd: Boolean,
)

internal fun Path.flattenBoundary(flatness: Float = Flatness): FlattenedPathBoundary {
    requireValidFlatness(flatness)
    val normalized = Path()
    val boundary = if (normalized.op(this, Path(), PathOperation.Union)) normalized else this
    return FlattenedPathBoundary(
        contour = flattenPath(boundary, flatness, FlatteningBudget()),
        evenOdd = boundary.fillType == PathFillType.EvenOdd,
    )
}

private typealias AppendLine = (Float, Float, Float, Float) -> Unit

private fun requireValidFlatness(flatness: Float) {
    require(flatness.fastIsFinite() && flatness >= 0f) {
        "flatness must be finite and non-negative"
    }
}

private fun flattenLoop(
    loop: PathSegments,
    flatness: Float,
    budget: FlatteningBudget,
): PathSegments? {
    val lines = mutableListOf<PathSegment>()

    fun add(x0: Float, y0: Float, x1: Float, y1: Float) {
        require(
            x0.fastIsFinite() && y0.fastIsFinite() && x1.fastIsFinite() && y1.fastIsFinite(),
        ) {
            "Flattened contour coordinates must be finite"
        }
        budget.reserveSegment()
        lines +=
            PathSegment.Line(
                Point(x0.toDouble(), y0.toDouble()),
                Point(x1.toDouble(), y1.toDouble()),
            )
    }

    for (segment in loop) {
        when (segment) {
            is PathSegment.Line ->
                add(
                    segment.start.x.toFloat(),
                    segment.start.y.toFloat(),
                    segment.end.x.toFloat(),
                    segment.end.y.toFloat(),
                )
            is PathSegment.Circular -> flattenCircular(segment, flatness, budget, ::add)
            is PathSegment.Cubic ->
                flattenCubic(
                    segment.start.x.toFloat(),
                    segment.start.y.toFloat(),
                    segment.control1.x.toFloat(),
                    segment.control1.y.toFloat(),
                    segment.control2.x.toFloat(),
                    segment.control2.y.toFloat(),
                    segment.end.x.toFloat(),
                    segment.end.y.toFloat(),
                    flatness,
                    ::add,
                )
        }
    }

    val first = loop.first().start
    val last = loop.last().end
    if (last != first) {
        add(last.x.toFloat(), last.y.toFloat(), first.x.toFloat(), first.y.toFloat())
    }

    return lines.takeIf { it.hasMeasurableSegment() }
}

private fun flattenPath(path: Path, flatness: Float, budget: FlatteningBudget): Contour? {
    val iterator = PathIterator(path, PathIterator.ConicEvaluation.AsConic)
    val points = FloatArray(8)
    val loops = mutableListOf<PathSegments>()
    var current = mutableListOf<PathSegment>()
    var startX = 0f
    var startY = 0f
    var currentX = 0f
    var currentY = 0f
    var open = false

    fun add(x0: Float, y0: Float, x1: Float, y1: Float) {
        require(
            x0.fastIsFinite() && y0.fastIsFinite() && x1.fastIsFinite() && y1.fastIsFinite(),
        ) {
            "Flattened path coordinates must be finite"
        }
        budget.reserveSegment()
        current +=
            PathSegment.Line(
                Point(x0.toDouble(), y0.toDouble()),
                Point(x1.toDouble(), y1.toDouble()),
            )
    }

    fun closeContour() {
        if (open && (currentX != startX || currentY != startY)) {
            add(currentX, currentY, startX, startY)
        }
        if (current.hasMeasurableSegment()) loops += current.toList()
        current = mutableListOf()
        open = false
    }

    fun startContour(x: Float, y: Float) {
        closeContour()
        startX = x
        startY = y
        currentX = x
        currentY = y
        open = true
    }

    while (iterator.hasNext()) {
        when (val type = iterator.next(points)) {
            Move -> startContour(points[0], points[1])
            Line -> {
                if (!open) startContour(points[0], points[1])
                add(points[0], points[1], points[2], points[3])
                currentX = points[2]
                currentY = points[3]
            }
            Quadratic,
            Conic -> {
                if (!open) startContour(points[0], points[1])
                val weight = if (type == Conic) points[6] else 1f
                flattenConic(
                    points[0],
                    points[1],
                    points[2],
                    points[3],
                    points[4],
                    points[5],
                    weight,
                    flatness,
                    ::add,
                )
                currentX = points[4]
                currentY = points[5]
            }
            Cubic -> {
                if (!open) startContour(points[0], points[1])
                flattenCubic(
                    points[0],
                    points[1],
                    points[2],
                    points[3],
                    points[4],
                    points[5],
                    points[6],
                    points[7],
                    flatness,
                    ::add,
                )
                currentX = points[6]
                currentY = points[7]
            }
            Close -> {
                closeContour()
                currentX = startX
                currentY = startY
            }
            Done -> break
        }
    }
    closeContour()
    return loops.takeIf { it.isNotEmpty() }?.let { Contour(*it.toTypedArray()) }
}

private fun flattenCircular(
    arc: PathSegment.Circular,
    flatness: Float,
    budget: FlatteningBudget,
    add: AppendLine,
) {
    val radius = arc.radius
    val radians = arc.sweepRadians
    if (radius <= 0.0 || radians == 0.0) {
        add(arc.start.x.toFloat(), arc.start.y.toFloat(), arc.end.x.toFloat(), arc.end.y.toFloat())
        return
    }
    val maximumStep =
        if (radius <= flatness) {
            PI * .5
        } else {
            2.0 * acos((1.0 - flatness.toDouble() / radius).fastCoerceIn(-1.0, 1.0))
        }
    require(maximumStep.fastIsFinite() && maximumStep > 0.0) {
        "flatness requires more than $MaxFlattenedSegments segments"
    }
    val requiredSegments = ceil(abs(radians) / maximumStep).coerceAtLeast(1.0)
    budget.requireCapacity(requiredSegments)
    val count = requiredSegments.toInt()
    var previous = arc.start
    for (index in 1..count) {
        val next = if (index == count) arc.end else arc.pointAt(index.toDouble() / count)
        add(previous.x.toFloat(), previous.y.toFloat(), next.x.toFloat(), next.y.toFloat())
        previous = next
    }
}

private class FlatteningBudget {
    private var remaining = MaxFlattenedSegments

    fun reserveSegment() {
        require(remaining > 0) { "Flattening requires more than $MaxFlattenedSegments segments" }
        remaining--
    }

    fun requireCapacity(segments: Double) {
        require(segments.fastIsFinite() && segments <= remaining.toDouble()) {
            "Flattening requires more than $MaxFlattenedSegments segments"
        }
    }
}

private fun PathSegments.hasMeasurableSegment(): Boolean = any { it.start != it.end }

private const val MaxFlattenedSegments = 65_536

private fun flattenCubic(
    x0: Float,
    y0: Float,
    c1x: Float,
    c1y: Float,
    c2x: Float,
    c2y: Float,
    x1: Float,
    y1: Float,
    flatness: Float,
    add: AppendLine,
) {
    val stack = ArrayList<FloatArray>()
    stack += floatArrayOf(x0, y0, c1x, c1y, c2x, c2y, x1, y1)
    while (stack.isNotEmpty()) {
        val curve = stack.removeAt(stack.lastIndex)
        if (
            maxOf(
                distanceToLine(curve[2], curve[3], curve[0], curve[1], curve[6], curve[7]),
                distanceToLine(curve[4], curve[5], curve[0], curve[1], curve[6], curve[7]),
            ) <= flatness
        ) {
            add(curve[0], curve[1], curve[6], curve[7])
        } else {
            val ax = (curve[0] + curve[2]) * .5f
            val ay = (curve[1] + curve[3]) * .5f
            val bx = (curve[2] + curve[4]) * .5f
            val by = (curve[3] + curve[5]) * .5f
            val dx = (curve[4] + curve[6]) * .5f
            val dy = (curve[5] + curve[7]) * .5f
            val ex = (ax + bx) * .5f
            val ey = (ay + by) * .5f
            val fx = (bx + dx) * .5f
            val fy = (by + dy) * .5f
            val mx = (ex + fx) * .5f
            val my = (ey + fy) * .5f
            val left = floatArrayOf(curve[0], curve[1], ax, ay, ex, ey, mx, my)
            val right = floatArrayOf(mx, my, fx, fy, dx, dy, curve[6], curve[7])
            if (left.contentEquals(curve) || right.contentEquals(curve)) {
                add(curve[0], curve[1], curve[6], curve[7])
            } else {
                stack += right
                stack += left
            }
        }
    }
}

private fun flattenConic(
    x0: Float,
    y0: Float,
    cx: Float,
    cy: Float,
    x1: Float,
    y1: Float,
    weight: Float,
    flatness: Float,
    add: AppendLine,
) {
    require(weight.fastIsFinite() && weight > 0f) {
        "Conic weight must be finite and positive"
    }
    val stack = ArrayList<FloatArray>()
    stack += floatArrayOf(x0, y0, cx, cy, x1, y1, weight)
    while (stack.isNotEmpty()) {
        val conic = stack.removeAt(stack.lastIndex)
        if (
            distanceToLine(conic[2], conic[3], conic[0], conic[1], conic[4], conic[5]) <= flatness
        ) {
            add(conic[0], conic[1], conic[4], conic[5])
        } else {
            val w = conic[6]
            val midX = (conic[0] + 2f * w * conic[2] + conic[4]) / (2f + 2f * w)
            val midY = (conic[1] + 2f * w * conic[3] + conic[5]) / (2f + 2f * w)
            val leftControlX = (conic[0] + w * conic[2]) / (1f + w)
            val leftControlY = (conic[1] + w * conic[3]) / (1f + w)
            val rightControlX = (w * conic[2] + conic[4]) / (1f + w)
            val rightControlY = (w * conic[3] + conic[5]) / (1f + w)
            val childWeight = sqrt((1.0 + w.toDouble()) * .5).toFloat()
            val left =
                floatArrayOf(
                    conic[0],
                    conic[1],
                    leftControlX,
                    leftControlY,
                    midX,
                    midY,
                    childWeight,
                )
            val right =
                floatArrayOf(
                    midX,
                    midY,
                    rightControlX,
                    rightControlY,
                    conic[4],
                    conic[5],
                    childWeight,
                )
            if (left.contentEquals(conic) || right.contentEquals(conic)) {
                add(conic[0], conic[1], conic[4], conic[5])
            } else {
                stack += right
                stack += left
            }
        }
    }
}

private fun distanceToLine(
    px: Float,
    py: Float,
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
): Float {
    val dx = x1 - x0
    val dy = y1 - y0
    val denominator = dx * dx + dy * dy
    if (denominator == 0f) return hypot(px - x0, py - y0)
    val projection = (((px - x0) * dx + (py - y0) * dy) / denominator).fastCoerceIn(0f, 1f)
    return hypot(px - (x0 + projection * dx), py - (y0 + projection * dy))
}
