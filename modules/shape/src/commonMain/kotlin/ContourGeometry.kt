package top.ltfan.m3x.compose.shape

import androidx.compose.ui.geometry.Rect

/** Expresses this rectangle as a single closed contour. */
public fun Rect.toContour(): Contour =
    Contour(
        listOf(
            PathSegment.Line(topLeft, topRight),
            PathSegment.Line(topRight, bottomRight),
            PathSegment.Line(bottomRight, bottomLeft),
            PathSegment.Line(bottomLeft, topLeft),
        ),
    )
