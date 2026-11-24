package top.ltfan.m3x.capsule

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceIn
import top.ltfan.m3x.compose.shape.Contour
import top.ltfan.m3x.compose.shape.toContour

/** Identifies Capsule's internal subclasses with a matching outline and contour. */
internal sealed interface BuiltInContourShape

internal fun CornerBasedShape.resolveContour(
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density,
    continuity: Continuity,
    capsule: Boolean,
): Contour? {
    if (size.width <= 0f || size.height <= 0f) return null
    if (continuity !is ContourContinuity) return null
    if (capsule) return continuity.createCapsuleContour(size)

    val corners =
        RoundedCorners(
                topStart.toPx(size, density),
                topEnd.toPx(size, density),
                bottomEnd.toPx(size, density),
                bottomStart.toPx(size, density),
            )
            .scaledToFit(size.minDimension)
    if (corners.isZero) return Rect(0f, 0f, size.width, size.height).toContour()

    val (left, right, bottomRight, bottomLeft) = corners.physicalCorners(size, layoutDirection)
    return continuity.createRoundedRectangleContour(size, left, right, bottomRight, bottomLeft)
}

/** Corners in perimeter order, starting at the top start corner. */
internal data class RoundedCorners(
    val topStart: Float,
    val topEnd: Float,
    val bottomEnd: Float,
    val bottomStart: Float,
) {
    val isZero: Boolean
        get() = topStart + topEnd + bottomEnd + bottomStart == 0f

    // CornerBasedShape resolves these pairs before calling the outline overload.
    fun scaledToFit(minDimension: Float): RoundedCorners {
        val startSum = topStart + bottomStart
        val endSum = topEnd + bottomEnd
        val startScale = if (startSum > minDimension) minDimension / startSum else 1f
        val endScale = if (endSum > minDimension) minDimension / endSum else 1f
        return RoundedCorners(
            topStart * startScale,
            topEnd * endScale,
            bottomEnd * endScale,
            bottomStart * startScale,
        )
    }

    fun physicalCorners(size: Size, layoutDirection: LayoutDirection): PhysicalCorners {
        val maximum = minOf(size.width, size.height) * 0.5f
        val startTop = topStart.fastCoerceIn(0f, maximum)
        val endTop = topEnd.fastCoerceIn(0f, maximum)
        val endBottom = bottomEnd.fastCoerceIn(0f, maximum)
        val startBottom = bottomStart.fastCoerceIn(0f, maximum)
        return if (layoutDirection == LayoutDirection.Ltr) {
            PhysicalCorners(startTop, endTop, endBottom, startBottom)
        } else {
            PhysicalCorners(endTop, startTop, startBottom, endBottom)
        }
    }
}

internal data class PhysicalCorners(
    val topLeft: Float,
    val topRight: Float,
    val bottomRight: Float,
    val bottomLeft: Float,
) {
    fun createOutline(size: Size, continuity: Continuity): Outline =
        continuity.createRoundedRectangleOutline(size, topLeft, topRight, bottomRight, bottomLeft)
}
