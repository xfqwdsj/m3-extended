package top.ltfan.m3x.compose.shape

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/** A reusable signed distance function in the shape's local pixel coordinates. */
public fun interface SignedDistanceField {
    /** Returns distance in pixels, with negative values inside and positive values outside. */
    public fun distanceAt(x: Float, y: Float): Float
}

/** A shape that can prepare a signed distance field when requested. */
public interface SdfShape : Shape {
    /**
     * Returns null when a field is unavailable for the resolved shape.
     *
     * @param flatness maximum curve-to-segment deviation in local pixels.
     */
    public fun createSdf(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
        flatness: Float = Flatness,
    ): SignedDistanceField?
}

/** A shape that can prepare a distance field from its structured boundary. */
public interface ContourSdfShape : ContourShape, SdfShape {
    override fun createSdf(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
        flatness: Float,
    ): SignedDistanceField? =
        createContour(size, layoutDirection, density)?.toSignedDistanceField(flatness)
}
