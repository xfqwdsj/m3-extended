package top.ltfan.m3x.compose.shape

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastIsFinite
import kotlin.math.PI
import kotlin.math.abs

/** One or more ordered closed loops, filled using the non-zero winding rule. */
@Immutable
public class Contour(vararg loops: PathSegments) {
    public val loops: List<PathSegments> = loops.map { it.toList() }

    /** A conservative bounding box, including the control points of cubic segments. */
    public val conservativeBounds: Rect by
        lazy(LazyThreadSafetyMode.PUBLICATION) { calculateConservativeBounds(this.loops) }

    init {
        require(this.loops.isNotEmpty()) { "A contour must contain at least one closed loop" }
        for (loop in this.loops) {
            require(loop.isNotEmpty()) { "A contour loop must contain at least one segment" }
            for (index in loop.indices) {
                require(loop[index].joins(loop[(index + 1) % loop.size])) {
                    "Contour segments must form one ordered closed path"
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is Contour && loops == other.loops)

    override fun hashCode(): Int = loops.hashCode()

    override fun toString(): String = "Contour(loops=$loops)"
}

/**
 * A Compose [Shape] that can provide structured boundary geometry for optimized rendering. A
 * non-null [createContour] result describes the same filled region as the outline for the same
 * size, layout direction, and density. Return null to use the outline when that geometry is
 * unavailable.
 */
public interface ContourShape : Shape {
    public fun createContour(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Contour?
}

/** Converts [this] contour to a Compose path using non-zero fill for every loop. */
public fun Contour.toPath(): Path =
    Path().apply {
        for (loop in loops) {
            addSegments(loop)
            close()
        }
    }

private fun PathSegment.joins(next: PathSegment): Boolean {
    val previousEnd = end
    val start = next.start
    // The absolute floor and relative tolerance account for float inputs from Compose.
    val arcRadius =
        maxOf(
            (this as? PathSegment.Circular)?.radius ?: 0.0,
            (next as? PathSegment.Circular)?.radius ?: 0.0,
        )
    val scale = maxOf(abs(previousEnd.x), abs(previousEnd.y), abs(start.x), abs(start.y), arcRadius)
    if (!scale.fastIsFinite()) return false
    val tolerance = maxOf(0.001, scale * RelativeJoinTolerance)
    return abs(previousEnd.x - start.x) <= tolerance && abs(previousEnd.y - start.y) <= tolerance
}

private const val RelativeJoinTolerance = 4f * 1.1920929e-7f

private fun calculateConservativeBounds(loops: List<PathSegments>): Rect {
    var left = Double.POSITIVE_INFINITY
    var top = Double.POSITIVE_INFINITY
    var right = Double.NEGATIVE_INFINITY
    var bottom = Double.NEGATIVE_INFINITY

    fun include(point: Point) {
        left = minOf(left, point.x)
        top = minOf(top, point.y)
        right = maxOf(right, point.x)
        bottom = maxOf(bottom, point.y)
    }

    for (loop in loops) {
        for (segment in loop) {
            include(segment.start)
            include(segment.end)
            when (segment) {
                is Line -> Unit
                is Circular -> {
                    for (angle in ArcCardinalAngles) {
                        if (segment.containsAngle(angle)) {
                            include(segment.pointAtAngle(angle))
                        }
                    }
                }
                is Cubic -> {
                    include(segment.control1)
                    include(segment.control2)
                }
            }
        }
    }
    return if (left.fastIsFinite())
        Rect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
    else Rect.Zero
}

private fun PathSegment.Circular.containsAngle(angle: Double): Boolean {
    if (abs(sweepRadians) >= 2.0 * PI) return true
    val offset =
        if (sweepRadians >= 0f) {
            normalizeRadians(angle - startRadians)
        } else {
            normalizeRadians(startRadians - angle)
        }
    return offset <= abs(sweepRadians) + PI / 180_000.0
}

private val ArcCardinalAngles = doubleArrayOf(0.0, PI * 0.5, PI, PI * 1.5)
