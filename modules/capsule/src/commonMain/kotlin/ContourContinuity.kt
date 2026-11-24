package top.ltfan.m3x.capsule

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import top.ltfan.m3x.compose.shape.Contour

/**
 * A [Continuity] that supplies the same boundary as its outlines as a structured contour. The
 * defaults use its rounded-rectangle path segments. Override the matching contour method when an
 * outline uses different geometry. Return null to use that outline directly.
 */
@Immutable
public interface ContourContinuity : Continuity {
    public fun createRoundedRectangleContour(
        size: Size,
        topLeft: Float,
        topRight: Float,
        bottomRight: Float,
        bottomLeft: Float,
    ): Contour? {
        val segments =
            createRoundedRectanglePathSegments(size, topLeft, topRight, bottomRight, bottomLeft)
        return if (segments.isEmpty()) null else Contour(segments)
    }

    public fun createCapsuleContour(size: Size): Contour? {
        val radius = size.capsuleRadius
        return createRoundedRectangleContour(size, radius, radius, radius, radius)
    }
}
