package top.ltfan.m3x.compose.shape

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.util.fastIsFinite
import kotlin.math.PI
import kotlin.math.abs

/** A double-precision segment shared by generated paths and closed contours. */
@Immutable
public sealed interface PathSegment {
    public val start: Point
    public val end: Point

    public fun drawTo(path: Path) {
        when (this) {
            is Line -> path.lineTo(end.x.toFloat(), end.y.toFloat())
            is Circle -> path.addOval(circleBounds(center.x, center.y, radius))
            is Arc -> {
                val bounds = circleBounds(center.x, center.y, radius)
                val start = (startRadians * RadiansToDegrees).toFloat()
                val sweep = (sweepRadians * RadiansToDegrees).toFloat()
                // Use two half arcs to retain a full turn when writing to Compose Path.
                if (abs(sweep) == 360f) {
                    val half = sweep * 0.5f
                    path.arcTo(bounds, start, half, forceMoveTo = false)
                    path.arcTo(bounds, start + half, half, forceMoveTo = false)
                } else {
                    path.arcTo(bounds, start, sweep, forceMoveTo = false)
                }
            }
            is Cubic ->
                path.cubicTo(
                    control1.x.toFloat(),
                    control1.y.toFloat(),
                    control2.x.toFloat(),
                    control2.y.toFloat(),
                    end.x.toFloat(),
                    end.y.toFloat(),
                )
        }
    }

    @Immutable
    public data class Line(override val start: Point, override val end: Point) : PathSegment {
        public constructor(start: Offset, end: Offset) : this(start.toPoint(), end.toPoint())
    }

    public sealed interface Circular : PathSegment {
        public val center: Point
        public val radius: Double
        public val startRadians: Double
        public val sweepRadians: Double

        override val start: Point
            get() = pointAt(0.0)

        override val end: Point
            get() = pointAt(1.0)

        /** Evaluates a point at a fraction of the sweep, where 0 is the start and 1 is the end. */
        public fun pointAt(fraction: Double): Point =
            pointAtAngle(startRadians + sweepRadians * fraction)
    }

    @Immutable
    public data class Arc(
        override val center: Point,
        override val radius: Double,
        override val startRadians: Double,
        override val sweepRadians: Double,
    ) : Circular {
        public constructor(
            center: Offset,
            radius: Float,
            startAngleDegrees: Float,
            sweepAngleDegrees: Float,
        ) : this(
            center.toPoint(),
            radius.toDouble(),
            startAngleDegrees * DegreesToRadians,
            sweepAngleDegrees * DegreesToRadians,
        )

        init {
            requireValidGeometry()
        }
    }

    @Immutable
    public data class Circle(override val center: Point, override val radius: Double) : Circular {
        override val startRadians: Double
            get() = 0.0

        override val sweepRadians: Double
            get() = 2.0 * PI

        init {
            requireValidGeometry()
        }
    }

    @Immutable
    public data class Cubic(
        override val start: Point,
        public val control1: Point,
        public val control2: Point,
        override val end: Point,
    ) : PathSegment {
        public constructor(
            start: Offset,
            control1: Offset,
            control2: Offset,
            end: Offset,
        ) : this(start.toPoint(), control1.toPoint(), control2.toPoint(), end.toPoint())

        /** Scales all four control points about the origin. */
        public operator fun times(scale: Double): Cubic =
            Cubic(start * scale, control1 * scale, control2 * scale, end * scale)
    }
}

internal fun PathSegment.Circular.pointAtAngle(radians: Double): Point {
    // Exact cardinal points keep full turns and adjacent quarter arcs joined.
    return when (val angle = normalizeRadians(radians)) {
        0.0 -> Point(center.x + radius, center.y)
        PI * 0.5 -> Point(center.x, center.y + radius)
        PI -> Point(center.x - radius, center.y)
        PI * 1.5 -> Point(center.x, center.y - radius)
        else -> pointOnCircle(center.x, center.y, radius, angle, ::Point)
    }
}

private fun PathSegment.Circular.requireValidGeometry() {
    require(
        center.x.fastIsFinite() &&
            center.y.fastIsFinite() &&
            radius.fastIsFinite() &&
            radius >= 0.0 &&
            startRadians.fastIsFinite() &&
            sweepRadians.fastIsFinite(),
    ) {
        "Arc geometry must be finite with a non-negative radius"
    }
}

private fun Offset.toPoint(): Point = Point(x.toDouble(), y.toDouble())

private const val DegreesToRadians = PI / 180.0
private const val RadiansToDegrees = 180.0 / PI
