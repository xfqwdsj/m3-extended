package top.ltfan.m3x.compose.shape

import androidx.compose.ui.geometry.Rect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Evaluates a point at [angleRadians] on a circle using double-precision coordinates. */
public inline fun <T> pointOnCircle(
    centerX: Double,
    centerY: Double,
    radius: Double,
    angleRadians: Double,
    point: (x: Double, y: Double) -> T,
): T = point(centerX + cos(angleRadians) * radius, centerY + sin(angleRadians) * radius)

/** Returns the axis-aligned bounds of a circle in Compose coordinates. */
public fun circleBounds(centerX: Double, centerY: Double, radius: Double): Rect =
    Rect(
        (centerX - radius).toFloat(),
        (centerY - radius).toFloat(),
        (centerX + radius).toFloat(),
        (centerY + radius).toFloat(),
    )

internal fun normalizeRadians(radians: Double): Double {
    val remainder = radians % (2.0 * PI)
    return if (remainder < 0.0) remainder + 2.0 * PI else remainder
}
