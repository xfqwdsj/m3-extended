package top.ltfan.m3x.backdrop.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathIterator
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastIsFinite
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.sqrt
import top.ltfan.m3x.backdrop.highlight.HighlightFlatness
import top.ltfan.m3x.compose.shape.Contour
import top.ltfan.m3x.compose.shape.PathSegment

private typealias AppendSegment = (Float, Float, Float, Float) -> Unit

private const val CERTIFICATE_GROUP_SIZE = 8
private const val CERTIFICATE_INDEX_MINIMUM_SEGMENTS = 32

/**
 * A line approximation of a filled path boundary. Subdivision uses [HighlightFlatness] as its
 * tolerance in the contour shader's local pixel coordinates.
 */
internal class FlattenedBoundaryGeometry private constructor(source: BoundarySource) {
    private sealed interface BoundarySource {
        val bounds: Rect

        fun flatten(add: AppendSegment)
    }

    private class PathSource(private val path: Path) : BoundarySource {
        override val bounds: Rect
            get() = path.getBounds()

        override fun flatten(add: AppendSegment) = flattenPath(path, add)
    }

    private class ContourSource(private val contour: Contour) : BoundarySource {
        override val bounds: Rect
            get() = contour.conservativeBounds

        override fun flatten(add: AppendSegment) = flattenContour(contour, add)
    }

    internal val bounds: Rect = source.bounds

    /** Four floats per segment: start x/y followed by end x/y. */
    internal val segments: FloatArray

    internal val segmentCount: Int
        get() = segments.size / 4

    /**
     * Normalized supporting lines for a single convex closed contour, or `null` when the flattened
     * boundary cannot prove that model. Four floats hold the outward normal x/y and a line point
     * x/y. Interior distance is then the largest negative half-plane distance.
     */
    internal val convexSupportLines: FloatArray? by
        lazy(LazyThreadSafetyMode.NONE) {
            if (!formsSingleClosedConvexContour()) return@lazy null
            val signs = normalSigns
            if (signs.any { it == 0f }) return@lazy null
            FloatArray(segmentCount * 4) { index ->
                val segment = index / 4
                val component = index % 4
                val offset = segment * 4
                when (component) {
                    0,
                    1 -> {
                        val dx = segments[offset + 2] - segments[offset]
                        val dy = segments[offset + 3] - segments[offset + 1]
                        val length = sqrt(dx * dx + dy * dy)
                        (if (component == 0) dy else -dx) / length * signs[segment]
                    }
                    2 -> segments[offset]
                    else -> segments[offset + 1]
                }
            }
        }

    internal val coordinateScale: Float =
        maxOf(1f, abs(bounds.left), abs(bounds.top), abs(bounds.right), abs(bounds.bottom))

    private fun certificateGroupBounds(): FloatArray? =
        if (segmentCount < CERTIFICATE_INDEX_MINIMUM_SEGMENTS) null
        else
            FloatArray((segmentCount + CERTIFICATE_GROUP_SIZE - 1) / CERTIFICATE_GROUP_SIZE * 4) {
                index ->
                val first = index / 4 * CERTIFICATE_GROUP_SIZE
                val component = index % 4
                var value = if (component < 2) Float.POSITIVE_INFINITY else Float.NEGATIVE_INFINITY
                for (segment in first until minOf(first + CERTIFICATE_GROUP_SIZE, segmentCount)) {
                    val offset = segment * 4
                    val a = segments[offset + component % 2]
                    val b = segments[offset + 2 + component % 2]
                    value = if (component < 2) minOf(value, a, b) else maxOf(value, a, b)
                }
                value
            }

    /** A nonzero value certifies the segment's outward normal for its full length. */
    internal val normalSigns: FloatArray by
        lazy(LazyThreadSafetyMode.NONE) {
            val signs = FloatArray(segmentCount)
            // Build the spatial index while producing the retained certificate.
            val groups = certificateGroupBounds()
            val tolerance = 9.5367432e-7 * coordinateScale.toDouble()
            val probe = maxOf(0.0001, tolerance * 4.0)
            val distanceLimit = tolerance * 2.0
            val distanceLimitSquared = distanceLimit * distanceLimit
            for (index in 0 until segmentCount) {
                val offset = index * 4
                val ax = segments[offset].toDouble()
                val ay = segments[offset + 1].toDouble()
                val bx = segments[offset + 2].toDouble()
                val by = segments[offset + 3].toDouble()
                val dx = bx - ax
                val dy = by - ay
                val length = sqrt(dx * dx + dy * dy)
                if (length <= 0.0) continue
                val normalX = dy / length
                val normalY = -dx / length
                val shiftedAx = ax + normalX * probe
                val shiftedAy = ay + normalY * probe
                val shiftedBx = bx + normalX * probe
                val shiftedBy = by + normalY * probe
                val minX = minOf(shiftedAx, shiftedBx) - distanceLimit
                val minY = minOf(shiftedAy, shiftedBy) - distanceLimit
                val maxX = maxOf(shiftedAx, shiftedBx) + distanceLimit
                val maxY = maxOf(shiftedAy, shiftedBy) + distanceLimit
                val groupCount = if (groups == null) 1 else groups.size / 4
                val touchesBoundary =
                    (0 until groupCount).any { group ->
                        val bound = group * 4
                        if (
                            groups != null &&
                                (groups[bound].toDouble() > maxX ||
                                    groups[bound + 2].toDouble() < minX ||
                                    groups[bound + 1].toDouble() > maxY ||
                                    groups[bound + 3].toDouble() < minY)
                        ) {
                            return@any false
                        }
                        val first = if (groups == null) 0 else group * CERTIFICATE_GROUP_SIZE
                        val last =
                            if (groups == null) segmentCount
                            else minOf(first + CERTIFICATE_GROUP_SIZE, segmentCount)
                        (first until last).any { otherIndex ->
                            val otherOffset = otherIndex * 4
                            val cx = segments[otherOffset].toDouble()
                            val cy = segments[otherOffset + 1].toDouble()
                            val ex = segments[otherOffset + 2].toDouble()
                            val ey = segments[otherOffset + 3].toDouble()
                            if (
                                minOf(cx, ex) > maxX ||
                                    maxOf(cx, ex) < minX ||
                                    minOf(cy, ey) > maxY ||
                                    maxOf(cy, ey) < minY
                            ) {
                                return@any false
                            }
                            distanceSquaredBetweenSegments(
                                shiftedAx,
                                shiftedAy,
                                shiftedBx,
                                shiftedBy,
                                cx,
                                cy,
                                ex,
                                ey,
                            ) <= distanceLimitSquared
                        }
                    }
                if (!touchesBoundary) {
                    val midpointX = (shiftedAx + shiftedBx) * 0.5
                    val midpointY = (shiftedAy + shiftedBy) * 0.5
                    signs[index] = if (contains(midpointX, midpointY, groups)) -1f else 1f
                }
            }
            signs
        }

    internal fun contains(point: Offset): Boolean {
        var crossings = 0
        for (index in segments.indices step 4) {
            val x0 = segments[index]
            val y0 = segments[index + 1]
            val x1 = segments[index + 2]
            val y1 = segments[index + 3]
            if ((y0 > point.y) != (y1 > point.y)) {
                val crossingX = x0 + (point.y - y0) * (x1 - x0) / (y1 - y0)
                if (crossingX > point.x) {
                    crossings++
                }
            }
        }
        return crossings % 2 != 0
    }

    internal fun paddedConvexSupportLines(capacity: Int): FloatArray {
        require(capacity >= segmentCount)
        return FloatArray(capacity * 4).also { requireNotNull(convexSupportLines).copyInto(it) }
    }

    private fun contains(x: Double, y: Double, groups: FloatArray?): Boolean {
        var crossings = 0
        if (groups == null) {
            for (index in segments.indices step 4) {
                val x0 = segments[index].toDouble()
                val y0 = segments[index + 1].toDouble()
                val x1 = segments[index + 2].toDouble()
                val y1 = segments[index + 3].toDouble()
                if ((y0 > y) != (y1 > y)) {
                    val crossingX = x0 + (y - y0) * (x1 - x0) / (y1 - y0)
                    if (crossingX > x) crossings++
                }
            }
            return crossings % 2 != 0
        }
        for (group in 0 until groups.size / 4) {
            val bound = group * 4
            if (groups[bound + 1].toDouble() > y || groups[bound + 3].toDouble() <= y) continue
            val first = group * CERTIFICATE_GROUP_SIZE
            for (segment in first until minOf(first + CERTIFICATE_GROUP_SIZE, segmentCount)) {
                val index = segment * 4
                val x0 = segments[index].toDouble()
                val y0 = segments[index + 1].toDouble()
                val x1 = segments[index + 2].toDouble()
                val y1 = segments[index + 3].toDouble()
                if ((y0 > y) != (y1 > y)) {
                    val crossingX = x0 + (y - y0) * (x1 - x0) / (y1 - y0)
                    if (crossingX > x) crossings++
                }
            }
        }
        return crossings % 2 != 0
    }

    internal fun nearestBoundary(point: Offset): Boundary {
        convexSupportLines?.let {
            return nearestConvexBoundary(point, it)
        }
        var bestDistance = Float.POSITIVE_INFINITY
        var bestNormal = Offset.Zero
        var bestPosition = Offset.Zero
        val coordinateScale = maxOf(coordinateScale, abs(point.x), abs(point.y))
        val tolerance = (8f * Float.fromBits(0x34000000)) * coordinateScale
        for (index in segments.indices step 4) {
            val start = Offset(segments[index], segments[index + 1])
            val end = Offset(segments[index + 2], segments[index + 3])
            val edge = end - start
            val lengthSquared = edge.x * edge.x + edge.y * edge.y
            if (lengthSquared == 0f) continue
            val relative = point - start
            val amount =
                ((relative.x * edge.x + relative.y * edge.y) / lengthSquared).fastCoerceIn(0f, 1f)
            val closest = start + edge * amount
            val delta = point - closest
            val distanceSquared = delta.x * delta.x + delta.y * delta.y
            val distance = sqrt(distanceSquared)
            if (distance < bestDistance - tolerance) {
                bestDistance = distance
                bestNormal = Offset(edge.y, -edge.x) / sqrt(lengthSquared)
                bestPosition = closest
            }
        }
        val probeDistance = maxOf(0.0001f, tolerance * 4f)
        if (contains(bestPosition + bestNormal * probeDistance)) bestNormal = -bestNormal
        return Boundary(
            bestDistance,
            bestNormal,
            bestPosition,
            if (contains(point)) -bestDistance else bestDistance,
        )
    }

    internal data class Boundary(
        val distance: Float,
        val outwardNormal: Offset,
        val position: Offset,
        val signedDistance: Float,
    )

    private fun formsSingleClosedConvexContour(): Boolean {
        if (segmentCount < 3) return false
        val tolerance = 9.5367432e-7 * coordinateScale
        var orientation = 0f
        var previousDx = 0f
        var previousDy = 0f
        for (segment in 0 until segmentCount) {
            val offset = segment * 4
            val nextOffset = ((segment + 1) % segmentCount) * 4
            if (
                abs(segments[offset + 2] - segments[nextOffset]) > tolerance ||
                    abs(segments[offset + 3] - segments[nextOffset + 1]) > tolerance
            ) {
                return false
            }
            val dx = segments[offset + 2] - segments[offset]
            val dy = segments[offset + 3] - segments[offset + 1]
            if (segment > 0) {
                val cross = previousDx * dy - previousDy * dx
                val scale = maxOf(1f, abs(previousDx), abs(previousDy), abs(dx), abs(dy))
                if (abs(cross) > tolerance * scale) {
                    val sign = if (cross > 0f) 1f else -1f
                    if (orientation != 0f && orientation != sign) return false
                    orientation = sign
                }
            }
            previousDx = dx
            previousDy = dy
        }
        val firstDx = segments[2] - segments[0]
        val firstDy = segments[3] - segments[1]
        val closingCross = previousDx * firstDy - previousDy * firstDx
        val closingScale = maxOf(1f, abs(previousDx), abs(previousDy), abs(firstDx), abs(firstDy))
        if (abs(closingCross) > tolerance * closingScale) {
            val sign = if (closingCross > 0f) 1f else -1f
            if (orientation != 0f && orientation != sign) return false
            orientation = sign
        }
        return orientation != 0f
    }

    private fun nearestConvexBoundary(point: Offset, lines: FloatArray): Boundary {
        val coordinateScale = maxOf(coordinateScale, abs(point.x), abs(point.y))
        val tolerance = (8f * Float.fromBits(0x34000000)) * coordinateScale
        var nearestDistance = Float.POSITIVE_INFINITY
        var nearestNormal = Offset.Zero
        var inside = true
        for (index in lines.indices step 4) {
            val nx = lines[index]
            val ny = lines[index + 1]
            val signed = (point.x - lines[index + 2]) * nx + (point.y - lines[index + 3]) * ny
            if (signed > tolerance) inside = false
            val distance = -signed
            if (distance < nearestDistance - tolerance) {
                nearestDistance = distance
                nearestNormal = Offset(nx, ny)
            }
        }
        if (!inside) return nearestFiniteSegmentBoundary(point)
        val distance = maxOf(0f, nearestDistance)
        return Boundary(
            distance = distance,
            outwardNormal = nearestNormal,
            position = point + nearestNormal * distance,
            signedDistance = -distance,
        )
    }

    private fun nearestFiniteSegmentBoundary(point: Offset): Boundary {
        var bestDistance = Float.POSITIVE_INFINITY
        var bestNormal = Offset.Zero
        var bestPosition = Offset.Zero
        val coordinateScale = maxOf(coordinateScale, abs(point.x), abs(point.y))
        val tolerance = (8f * Float.fromBits(0x34000000)) * coordinateScale
        for (index in segments.indices step 4) {
            val start = Offset(segments[index], segments[index + 1])
            val end = Offset(segments[index + 2], segments[index + 3])
            val edge = end - start
            val lengthSquared = edge.x * edge.x + edge.y * edge.y
            if (lengthSquared == 0f) continue
            val relative = point - start
            val amount =
                ((relative.x * edge.x + relative.y * edge.y) / lengthSquared).fastCoerceIn(0f, 1f)
            val closest = start + edge * amount
            val delta = point - closest
            val distanceSquared = delta.x * delta.x + delta.y * delta.y
            val distance = sqrt(distanceSquared)
            if (distance < bestDistance - tolerance) {
                bestDistance = distance
                bestNormal = Offset(edge.y, -edge.x) / sqrt(lengthSquared)
                bestPosition = closest
            }
        }
        val probeDistance = maxOf(0.0001f, tolerance * 4f)
        if (contains(bestPosition + bestNormal * probeDistance)) bestNormal = -bestNormal
        return Boundary(bestDistance, bestNormal, bestPosition, bestDistance)
    }

    init {
        // Subdivision appends primitive coordinates without allocating boxed Float values.
        var coordinates = FloatArray(64)
        var coordinateCount = 0
        require(
            bounds.left.fastIsFinite() &&
                bounds.top.fastIsFinite() &&
                bounds.right.fastIsFinite() &&
                bounds.bottom.fastIsFinite(),
        )
        fun add(x0: Float, y0: Float, x1: Float, y1: Float) {
            require(
                x0.fastIsFinite() && y0.fastIsFinite() && x1.fastIsFinite() && y1.fastIsFinite(),
            )
            if (x0 != x1 || y0 != y1) {
                if (coordinateCount + 4 > coordinates.size) {
                    coordinates = coordinates.copyOf(coordinates.size * 2)
                }
                coordinates[coordinateCount] = x0
                coordinates[coordinateCount + 1] = y0
                coordinates[coordinateCount + 2] = x1
                coordinates[coordinateCount + 3] = y1
                coordinateCount += 4
            }
        }

        source.flatten(::add)
        segments = coordinates.copyOf(coordinateCount)
    }

    internal companion object {
        internal fun fromPath(path: Path): FlattenedBoundaryGeometry {
            val normalized = Path()
            return FlattenedBoundaryGeometry(
                PathSource(
                    if (normalized.op(path, Path(), PathOperation.Union)) normalized else path,
                ),
            )
        }

        internal fun fromContour(contour: Contour): FlattenedBoundaryGeometry =
            FlattenedBoundaryGeometry(ContourSource(contour))

        private fun flattenPath(path: Path, add: AppendSegment) {
            val iterator = PathIterator(path, PathIterator.ConicEvaluation.AsConic)
            val points = FloatArray(8)
            var contourX = 0f
            var contourY = 0f
            var currentX = 0f
            var currentY = 0f
            var hasContour = false

            fun closeContour() {
                if (hasContour) add(currentX, currentY, contourX, contourY)
                hasContour = false
            }

            while (iterator.hasNext()) {
                when (iterator.next(points)) {
                    Move -> {
                        closeContour()
                        contourX = points[0]
                        contourY = points[1]
                        currentX = contourX
                        currentY = contourY
                        hasContour = true
                    }
                    Line -> {
                        if (!hasContour) {
                            contourX = points[0]
                            contourY = points[1]
                            currentX = contourX
                            currentY = contourY
                            hasContour = true
                        }
                        add(points[0], points[1], points[2], points[3])
                        currentX = points[2]
                        currentY = points[3]
                    }
                    Quadratic -> {
                        if (!hasContour) {
                            contourX = points[0]
                            contourY = points[1]
                            currentX = contourX
                            currentY = contourY
                            hasContour = true
                        }
                        flattenQuadratic(
                            points[0],
                            points[1],
                            points[2],
                            points[3],
                            points[4],
                            points[5],
                            add,
                        )
                        currentX = points[4]
                        currentY = points[5]
                    }
                    Cubic -> {
                        if (!hasContour) {
                            contourX = points[0]
                            contourY = points[1]
                            currentX = contourX
                            currentY = contourY
                            hasContour = true
                        }
                        flattenCubic(
                            points[0],
                            points[1],
                            points[2],
                            points[3],
                            points[4],
                            points[5],
                            points[6],
                            points[7],
                            add,
                        )
                        currentX = points[6]
                        currentY = points[7]
                    }
                    Close -> {
                        closeContour()
                        currentX = contourX
                        currentY = contourY
                    }
                    Conic -> {
                        if (!hasContour) {
                            contourX = points[0]
                            contourY = points[1]
                            currentX = contourX
                            currentY = contourY
                            hasContour = true
                        }
                        flattenConic(
                            points[0],
                            points[1],
                            points[2],
                            points[3],
                            points[4],
                            points[5],
                            points[6],
                            add,
                        )
                        currentX = points[4]
                        currentY = points[5]
                    }
                    Done -> break
                }
            }
            closeContour()
        }

        private fun flattenContour(contour: Contour, add: AppendSegment) {
            for (loop in contour.loops) {
                for (segment in loop) {
                    when (segment) {
                        is Line ->
                            add(
                                segment.start.x.toFloat(),
                                segment.start.y.toFloat(),
                                segment.end.x.toFloat(),
                                segment.end.y.toFloat(),
                            )
                        is Circular -> flattenArc(segment, add)
                        is Cubic ->
                            flattenCubic(
                                segment.start.x.toFloat(),
                                segment.start.y.toFloat(),
                                segment.control1.x.toFloat(),
                                segment.control1.y.toFloat(),
                                segment.control2.x.toFloat(),
                                segment.control2.y.toFloat(),
                                segment.end.x.toFloat(),
                                segment.end.y.toFloat(),
                                add,
                            )
                    }
                }
                val first = loop.first().start
                val last = loop.last().end
                add(last.x.toFloat(), last.y.toFloat(), first.x.toFloat(), first.y.toFloat())
            }
        }

        private fun flattenQuadratic(
            x0: Float,
            y0: Float,
            cx: Float,
            cy: Float,
            x1: Float,
            y1: Float,
            add: AppendSegment,
        ) {
            val stack = ArrayList<FloatArray>()
            stack.add(floatArrayOf(x0, y0, cx, cy, x1, y1))
            while (stack.isNotEmpty()) {
                val c = stack.removeAt(stack.lastIndex)
                if (distanceToLine(c[2], c[3], c[0], c[1], c[4], c[5]) <= HighlightFlatness) {
                    add(c[0], c[1], c[4], c[5])
                } else {
                    val ax = (c[0] + c[2]) * 0.5f
                    val ay = (c[1] + c[3]) * 0.5f
                    val bx = (c[2] + c[4]) * 0.5f
                    val by = (c[3] + c[5]) * 0.5f
                    val mx = (ax + bx) * 0.5f
                    val my = (ay + by) * 0.5f
                    val left = floatArrayOf(c[0], c[1], ax, ay, mx, my)
                    val right = floatArrayOf(mx, my, bx, by, c[4], c[5])
                    if (left.contentEquals(c) || right.contentEquals(c)) {
                        add(c[0], c[1], c[4], c[5])
                    } else {
                        stack.add(right)
                        stack.add(left)
                    }
                }
            }
        }

        private fun flattenCubic(
            x0: Float,
            y0: Float,
            c1x: Float,
            c1y: Float,
            c2x: Float,
            c2y: Float,
            x1: Float,
            y1: Float,
            add: AppendSegment,
        ) {
            val stack = ArrayList<FloatArray>()
            stack.add(floatArrayOf(x0, y0, c1x, c1y, c2x, c2y, x1, y1))
            while (stack.isNotEmpty()) {
                val c = stack.removeAt(stack.lastIndex)
                if (
                    maxOf(
                        distanceToLine(c[2], c[3], c[0], c[1], c[6], c[7]),
                        distanceToLine(c[4], c[5], c[0], c[1], c[6], c[7]),
                    ) <= HighlightFlatness
                ) {
                    add(c[0], c[1], c[6], c[7])
                } else {
                    val aX = (c[0] + c[2]) * 0.5f
                    val aY = (c[1] + c[3]) * 0.5f
                    val bX = (c[2] + c[4]) * 0.5f
                    val bY = (c[3] + c[5]) * 0.5f
                    val dX = (c[4] + c[6]) * 0.5f
                    val dY = (c[5] + c[7]) * 0.5f
                    val eX = (aX + bX) * 0.5f
                    val eY = (aY + bY) * 0.5f
                    val fX = (bX + dX) * 0.5f
                    val fY = (bY + dY) * 0.5f
                    val mX = (eX + fX) * 0.5f
                    val mY = (eY + fY) * 0.5f
                    val left = floatArrayOf(c[0], c[1], aX, aY, eX, eY, mX, mY)
                    val right = floatArrayOf(mX, mY, fX, fY, dX, dY, c[6], c[7])
                    if (left.contentEquals(c) || right.contentEquals(c)) {
                        add(c[0], c[1], c[6], c[7])
                    } else {
                        stack.add(right)
                        stack.add(left)
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
            add: AppendSegment,
        ) {
            require(weight.fastIsFinite() && weight > 0f) {
                "Conic weight must be finite and positive"
            }
            val stack = ArrayList<FloatArray>()
            stack.add(floatArrayOf(x0, y0, cx, cy, x1, y1, weight))
            while (stack.isNotEmpty()) {
                val c = stack.removeAt(stack.lastIndex)
                if (distanceToLine(c[2], c[3], c[0], c[1], c[4], c[5]) <= HighlightFlatness) {
                    add(c[0], c[1], c[4], c[5])
                } else {
                    val w = c[6]
                    val midX = ((c[0] + 2f * w * c[2] + c[4]) / (2f + 2f * w))
                    val midY = ((c[1] + 2f * w * c[3] + c[5]) / (2f + 2f * w))
                    val leftControlX = (c[0] + w * c[2]) / (1f + w)
                    val leftControlY = (c[1] + w * c[3]) / (1f + w)
                    val rightControlX = (w * c[2] + c[4]) / (1f + w)
                    val rightControlY = (w * c[3] + c[5]) / (1f + w)
                    val childWeight = sqrt(((1.0 + w.toDouble()) * 0.5)).toFloat()
                    val left =
                        floatArrayOf(
                            c[0],
                            c[1],
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
                            c[4],
                            c[5],
                            childWeight,
                        )
                    if (left.contentEquals(c) || right.contentEquals(c)) {
                        add(c[0], c[1], c[4], c[5])
                    } else {
                        stack.add(right)
                        stack.add(left)
                    }
                }
            }
        }

        private fun flattenArc(arc: PathSegment.Circular, add: AppendSegment) {
            val radius = arc.radius
            val radians = arc.sweepRadians
            if (radius <= 0.0 || radians == 0.0) return
            val maximumStep =
                if (radius <= HighlightFlatness) {
                    PI * .5
                } else {
                    2.0 *
                        acos(
                            (1.0 - HighlightFlatness.toDouble() / radius).fastCoerceIn(
                                -1.0,
                                1.0,
                            ),
                        )
                }
            val count = ceil(abs(radians) / maximumStep).toInt().fastCoerceAtLeast(1)
            var previous = arc.start
            for (index in 1..count) {
                val next = arc.pointAt(index.toDouble() / count)
                add(previous.x.toFloat(), previous.y.toFloat(), next.x.toFloat(), next.y.toFloat())
                previous = next
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
    }
}

private fun distanceSquaredBetweenSegments(
    ax: Double,
    ay: Double,
    bx: Double,
    by: Double,
    cx: Double,
    cy: Double,
    dx: Double,
    dy: Double,
): Double {
    val abx = bx - ax
    val aby = by - ay
    val cdx = dx - cx
    val cdy = dy - cy
    val acx = cx - ax
    val acy = cy - ay
    val adx = dx - ax
    val ady = dy - ay
    val cax = ax - cx
    val cay = ay - cy
    val cbx = bx - cx
    val cby = by - cy
    val o1 = cross(abx, aby, acx, acy)
    val o2 = cross(abx, aby, adx, ady)
    val o3 = cross(cdx, cdy, cax, cay)
    val o4 = cross(cdx, cdy, cbx, cby)
    val firstStraddles = (o1 > 0.0 && o2 < 0.0) || (o2 > 0.0 && o1 < 0.0)
    val secondStraddles = (o3 > 0.0 && o4 < 0.0) || (o4 > 0.0 && o3 < 0.0)
    if (firstStraddles && secondStraddles) return 0.0
    var minimum = pointSegmentDistanceSquared(ax, ay, cx, cy, dx, dy)
    val distance = pointSegmentDistanceSquared(bx, by, cx, cy, dx, dy)
    if (distance < minimum) minimum = distance
    val reverseDistance = pointSegmentDistanceSquared(cx, cy, ax, ay, bx, by)
    if (reverseDistance < minimum) minimum = reverseDistance
    val otherReverseDistance = pointSegmentDistanceSquared(dx, dy, ax, ay, bx, by)
    if (otherReverseDistance < minimum) minimum = otherReverseDistance
    return minimum
}

private fun pointSegmentDistanceSquared(
    px: Double,
    py: Double,
    ax: Double,
    ay: Double,
    bx: Double,
    by: Double,
): Double {
    val dx = bx - ax
    val dy = by - ay
    val denominator = dx * dx + dy * dy
    if (denominator <= 0.0) {
        val x = px - ax
        val y = py - ay
        return x * x + y * y
    }
    val amount = (((px - ax) * dx + (py - ay) * dy) / denominator).fastCoerceIn(0.0, 1.0)
    val x = px - (ax + amount * dx)
    val y = py - (ay + amount * dy)
    return x * x + y * y
}

private fun cross(ax: Double, ay: Double, bx: Double, by: Double): Double = ax * by - ay * bx
