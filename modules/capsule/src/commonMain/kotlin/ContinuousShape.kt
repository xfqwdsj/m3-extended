package top.ltfan.m3x.capsule

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.LayoutDirection

internal val FullCornerSize: CornerSize = CornerSize(50)

internal fun RoundedCorners.createContinuousOutline(
    size: Size,
    continuity: Continuity,
    layoutDirection: LayoutDirection,
): Outline {
    if (isZero) return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
    return physicalCorners(size, layoutDirection).createOutline(size, continuity)
}

internal fun continuousRoundedRectangleEquals(
    other: ContinuousRoundedRectangle?,
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
    continuity: Continuity,
): Boolean =
    other != null &&
        topStart == other.topStart &&
        topEnd == other.topEnd &&
        bottomEnd == other.bottomEnd &&
        bottomStart == other.bottomStart &&
        continuity == other.continuity

internal fun continuousRoundedRectangleEquals(
    other: AbsoluteContinuousRoundedRectangle?,
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
    continuity: Continuity,
): Boolean =
    other != null &&
        topStart == other.topStart &&
        topEnd == other.topEnd &&
        bottomEnd == other.bottomEnd &&
        bottomStart == other.bottomStart &&
        continuity == other.continuity

internal fun continuousRoundedRectangleHashCode(
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
    continuity: Continuity,
): Int {
    var result = topStart.hashCode()
    result = 31 * result + topEnd.hashCode()
    result = 31 * result + bottomEnd.hashCode()
    result = 31 * result + bottomStart.hashCode()
    result = 31 * result + continuity.hashCode()
    return result
}
