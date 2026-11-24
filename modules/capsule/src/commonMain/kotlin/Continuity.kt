package top.ltfan.m3x.capsule

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import top.ltfan.m3x.capsule.continuities.G2Continuity
import top.ltfan.m3x.compose.shape.PathSegments
import top.ltfan.m3x.compose.shape.toPath

@Immutable
public interface Continuity {

    public fun createRoundedRectanglePathSegments(
        width: Double,
        height: Double,
        topLeft: Double,
        topRight: Double,
        bottomRight: Double,
        bottomLeft: Double,
    ): PathSegments

    public fun createRoundedRectangleOutline(
        size: Size,
        topLeft: Float,
        topRight: Float,
        bottomRight: Float,
        bottomLeft: Float,
    ): Outline {
        val path =
            createRoundedRectanglePathSegments(
                    size,
                    topLeft,
                    topRight,
                    bottomRight,
                    bottomLeft,
                )
                .toPath()
        return Outline.Generic(path)
    }

    public fun createCapsuleOutline(size: Size): Outline {
        val radius = size.capsuleRadius
        return createRoundedRectangleOutline(
            size = size,
            topLeft = radius,
            topRight = radius,
            bottomRight = radius,
            bottomLeft = radius,
        )
    }

    public fun lerp(stop: Continuity, fraction: Double): Continuity

    public companion object {

        @Stable public val Default: Continuity = G2Continuity()
    }
}

internal fun Continuity.createRoundedRectanglePathSegments(
    size: Size,
    topLeft: Float,
    topRight: Float,
    bottomRight: Float,
    bottomLeft: Float,
): PathSegments =
    createRoundedRectanglePathSegments(
        size.width.toDouble(),
        size.height.toDouble(),
        topLeft.toDouble(),
        topRight.toDouble(),
        bottomRight.toDouble(),
        bottomLeft.toDouble(),
    )

internal val Size.capsuleRadius: Float
    get() = minDimension * 0.5f
