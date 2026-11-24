package top.ltfan.m3x.backdrop.internal

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.abs
import kotlin.math.sqrt
import top.ltfan.m3x.backdrop.BackdropSampling

private const val BILINEAR_TEXEL_RADIUS = 0.5f

/** Returns a conservative analytic input envelope, including the bilinear texel footprint. */
internal fun lensSampling(
    size: Size,
    cornerRadii: FloatArray,
    height: Float,
    amount: Float,
    dispersion: Boolean,
): BackdropSampling = BackdropSampling { output ->
    val halfWidth = size.width * 0.5f
    val halfHeight = size.height * 0.5f
    if (halfWidth <= 0f || halfHeight <= 0f) {
        return@BackdropSampling output
    }

    val maximumSdf = maximumInteriorSdf(output, halfWidth, halfHeight, cornerRadii)
    if (maximumSdf != null) {
        // Each quadrant's rounded-rectangle SDF is convex, so its maximum is bounded by the
        // values at the corners of the output rectangle clipped to that quadrant.
        if (maximumSdf <= -height) return@BackdropSampling output
        val t = 1f + maximumSdf / height
        val activeAmount = amount * (1f - sqrt(1f - t * t))
        return@BackdropSampling bounds(output, size, activeAmount, dispersion)
    }

    bounds(output, size, amount, dispersion)
}

/** Contour normals may face any direction, including towards a hole in the surface. */
internal fun contourLensSampling(
    size: Size,
    contourBounds: Rect,
    height: Float,
    amount: Float,
    dispersion: Boolean,
): BackdropSampling = BackdropSampling { output ->
    val active = output.intersect(contourBounds.inflate(height))
    if (active.isEmpty) return@BackdropSampling output
    val halfWidth = size.width / 2f
    val halfHeight = size.height / 2f
    val x = maxOf(abs(active.left - halfWidth), abs(active.right - halfWidth)) / halfWidth
    val y = maxOf(abs(active.top - halfHeight), abs(active.bottom - halfHeight)) / halfHeight
    val support = amount * (if (dispersion) 1f + x * y else 1f) + BILINEAR_TEXEL_RADIUS
    val sampled = active.inflate(support)
    Rect(
        minOf(output.left, sampled.left),
        minOf(output.top, sampled.top),
        maxOf(output.right, sampled.right),
        maxOf(output.bottom, sampled.bottom),
    )
}

private fun bounds(output: Rect, size: Size, amount: Float, dispersion: Boolean): Rect {
    if (amount <= 0f) return output

    val halfWidth = size.width * 0.5f
    val halfHeight = size.height * 0.5f
    val centeredLeft = output.left - halfWidth
    val centeredRight = output.right - halfWidth
    val centeredTop = output.top - halfHeight
    val centeredBottom = output.bottom - halfHeight
    val maxX = maxOf(abs(centeredLeft), abs(centeredRight)) / halfWidth
    val maxY = maxOf(abs(centeredTop), abs(centeredBottom)) / halfHeight
    val maxIntensity = if (dispersion) maxX * maxY else 0f
    val inward = amount * (1f + maxIntensity)
    val outward = amount * maxOf(0f, maxIntensity - 1f)

    val (leftOutset, rightOutset) = axisOutsets(centeredLeft, centeredRight, inward, outward)
    val (topOutset, bottomOutset) = axisOutsets(centeredTop, centeredBottom, inward, outward)
    return Rect(
        output.left - leftOutset - BILINEAR_TEXEL_RADIUS,
        output.top - topOutset - BILINEAR_TEXEL_RADIUS,
        output.right + rightOutset + BILINEAR_TEXEL_RADIUS,
        output.bottom + bottomOutset + BILINEAR_TEXEL_RADIUS,
    )
}

internal fun maximumInteriorSdf(
    output: Rect,
    halfWidth: Float,
    halfHeight: Float,
    radii: FloatArray,
): Float? {
    var maximum = Float.NEGATIVE_INFINITY
    for (rightHalf in 0 until 2) {
        val rightSide = rightHalf == 1
        val left = if (rightSide) maxOf(output.left, halfWidth) else output.left
        val right = if (rightSide) output.right else minOf(output.right, halfWidth)
        if (left > right) continue
        for (bottomHalf in 0 until 2) {
            val bottomSide = bottomHalf == 1
            val top = if (bottomSide) maxOf(output.top, halfHeight) else output.top
            val bottom = if (bottomSide) output.bottom else minOf(output.bottom, halfHeight)
            if (top > bottom) continue
            val radiusIndex =
                when {
                    rightSide && bottomSide -> 2
                    rightSide -> 1
                    bottomSide -> 3
                    else -> 0
                }
            val radius = radii[radiusIndex]
            val topLeft = roundedRectSdf(left, top, halfWidth, halfHeight, radius)
            val topRight = roundedRectSdf(right, top, halfWidth, halfHeight, radius)
            val bottomLeft = roundedRectSdf(left, bottom, halfWidth, halfHeight, radius)
            val bottomRight = roundedRectSdf(right, bottom, halfWidth, halfHeight, radius)
            if (topLeft > 0f || topRight > 0f || bottomLeft > 0f || bottomRight > 0f) return null
            maximum =
                maxOf(maximum, maxOf(maxOf(topLeft, topRight), maxOf(bottomLeft, bottomRight)))
        }
    }
    return if (maximum.isFinite()) maximum else null
}

private fun roundedRectSdf(
    x: Float,
    y: Float,
    halfWidth: Float,
    halfHeight: Float,
    radius: Float,
): Float {
    val centeredX = x - halfWidth
    val centeredY = y - halfHeight
    val cornerX = abs(centeredX) - (halfWidth - radius)
    val cornerY = abs(centeredY) - (halfHeight - radius)
    val outsideX = maxOf(cornerX, 0f)
    val outsideY = maxOf(cornerY, 0f)
    return sqrt(outsideX * outsideX + outsideY * outsideY) - radius +
        minOf(maxOf(cornerX, cornerY), 0f)
}

private fun axisOutsets(min: Float, max: Float, inward: Float, outward: Float): Pair<Float, Float> =
    when {
        min >= 0f -> inward to outward
        max <= 0f -> outward to inward
        else -> maxOf(outward, inward + min, 0f) to maxOf(outward, inward - max, 0f)
    }
