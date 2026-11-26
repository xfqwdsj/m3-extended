package top.ltfan.m3x.capsule

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.ltfan.m3x.compose.shape.Contour
import top.ltfan.m3x.compose.shape.ContourSdfShape

@Immutable
public open class ContinuousRoundedRectangle
public constructor(
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
    public open val continuity: Continuity = Continuity.Default,
) :
    CornerBasedShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart,
    ),
    ContourSdfShape {

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline =
        RoundedCorners(topStart, topEnd, bottomEnd, bottomStart)
            .createContinuousOutline(size, continuity, layoutDirection)

    override fun createContour(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Contour? {
        if (this::class != ContinuousRoundedRectangle::class && this !is BuiltInContourShape)
            return null
        return resolveContour(
            size = size,
            layoutDirection = layoutDirection,
            density = density,
            continuity = continuity,
            capsule = this is ContinuousCapsuleImpl,
        )
    }

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ): ContinuousRoundedRectangle {
        return ContinuousRoundedRectangle(
            topStart = topStart,
            topEnd = topEnd,
            bottomEnd = bottomEnd,
            bottomStart = bottomStart,
            continuity = continuity,
        )
    }

    public fun copy(
        topStart: CornerSize = this.topStart,
        topEnd: CornerSize = this.topEnd,
        bottomEnd: CornerSize = this.bottomEnd,
        bottomStart: CornerSize = this.bottomStart,
        continuity: Continuity = this.continuity,
    ): ContinuousRoundedRectangle {
        return ContinuousRoundedRectangle(
            topStart = topStart,
            topEnd = topEnd,
            bottomEnd = bottomEnd,
            bottomStart = bottomStart,
            continuity = continuity,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ContinuousRoundedRectangle) return false
        return continuousRoundedRectangleEquals(
            other,
            topStart,
            topEnd,
            bottomEnd,
            bottomStart,
            continuity,
        )
    }

    override fun hashCode(): Int =
        continuousRoundedRectangleHashCode(topStart, topEnd, bottomEnd, bottomStart, continuity)

    override fun toString(): String {
        return "ContinuousRoundedRectangle(topStart=$topStart, topEnd=$topEnd, bottomEnd=$bottomEnd, " +
            "bottomStart=$bottomStart, continuity=$continuity)"
    }

    override fun lerp(other: Any?, t: Float): Any? {
        var other = other
        if (other == RectangleShape || other == null) {
            other = ContinuousRectangle(continuity)
        }
        if (other is ContinuousRoundedRectangle) {
            return lerp(this, other, t)
        }
        return null
    }
}

@Stable public val ContinuousRectangle: ContinuousRoundedRectangle = ContinuousRectangleImpl()

@Suppress("FunctionName")
@Stable
public fun ContinuousRectangle(
    continuity: Continuity = Continuity.Default,
): ContinuousRoundedRectangle = ContinuousRectangleImpl(continuity)

@Immutable
private data class ContinuousRectangleImpl(
    override val continuity: Continuity = Continuity.Default,
) :
    ContinuousRoundedRectangle(
        topStart = ZeroCornerSize,
        topEnd = ZeroCornerSize,
        bottomEnd = ZeroCornerSize,
        bottomStart = ZeroCornerSize,
        continuity = continuity,
    ),
    BuiltInContourShape {

    override fun toString(): String {
        return "ContinuousRectangle(continuity=$continuity)"
    }
}

@Stable public val ContinuousCapsule: ContinuousRoundedRectangle = ContinuousCapsule()

@Suppress("FunctionName")
@Stable
public fun ContinuousCapsule(
    continuity: Continuity = Continuity.Default,
): ContinuousRoundedRectangle = ContinuousCapsuleImpl(continuity)

@Immutable
private data class ContinuousCapsuleImpl(
    override val continuity: Continuity = Continuity.Default,
) :
    ContinuousRoundedRectangle(
        topStart = FullCornerSize,
        topEnd = FullCornerSize,
        bottomEnd = FullCornerSize,
        bottomStart = FullCornerSize,
        continuity = continuity,
    ),
    BuiltInContourShape {

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline = continuity.createCapsuleOutline(size)

    override fun toString(): String {
        return "ContinuousCapsule(continuity=$continuity)"
    }
}

@Stable
public fun ContinuousRoundedRectangle(
    corner: CornerSize,
    continuity: Continuity = Continuity.Default,
): ContinuousRoundedRectangle =
    ContinuousRoundedRectangle(
        topStart = corner,
        topEnd = corner,
        bottomEnd = corner,
        bottomStart = corner,
        continuity = continuity,
    )

@Stable
public fun ContinuousRoundedRectangle(
    size: Dp,
    continuity: Continuity = Continuity.Default,
): ContinuousRoundedRectangle =
    ContinuousRoundedRectangle(
        corner = CornerSize(size),
        continuity = continuity,
    )

@Stable
public fun ContinuousRoundedRectangle(
    @FloatRange(from = 0.0) size: Float,
    continuity: Continuity = Continuity.Default,
): ContinuousRoundedRectangle =
    ContinuousRoundedRectangle(
        corner = CornerSize(size),
        continuity = continuity,
    )

@Stable
public fun ContinuousRoundedRectangle(
    @IntRange(from = 0, to = 100) percent: Int,
    continuity: Continuity = Continuity.Default,
): ContinuousRoundedRectangle =
    ContinuousRoundedRectangle(
        corner = CornerSize(percent),
        continuity = continuity,
    )

@Stable
public fun ContinuousRoundedRectangle(
    topStart: Dp = 0f.dp,
    topEnd: Dp = 0f.dp,
    bottomEnd: Dp = 0f.dp,
    bottomStart: Dp = 0f.dp,
    continuity: Continuity = Continuity.Default,
): ContinuousRoundedRectangle =
    ContinuousRoundedRectangle(
        topStart = CornerSize(topStart),
        topEnd = CornerSize(topEnd),
        bottomEnd = CornerSize(bottomEnd),
        bottomStart = CornerSize(bottomStart),
        continuity = continuity,
    )

@Stable
public fun ContinuousRoundedRectangle(
    @FloatRange(from = 0.0) topStart: Float = 0f,
    @FloatRange(from = 0.0) topEnd: Float = 0f,
    @FloatRange(from = 0.0) bottomEnd: Float = 0f,
    @FloatRange(from = 0.0) bottomStart: Float = 0f,
    continuity: Continuity = Continuity.Default,
): ContinuousRoundedRectangle =
    ContinuousRoundedRectangle(
        topStart = CornerSize(topStart),
        topEnd = CornerSize(topEnd),
        bottomEnd = CornerSize(bottomEnd),
        bottomStart = CornerSize(bottomStart),
        continuity = continuity,
    )

@Stable
public fun ContinuousRoundedRectangle(
    @IntRange(from = 0, to = 100) topStartPercent: Int = 0,
    @IntRange(from = 0, to = 100) topEndPercent: Int = 0,
    @IntRange(from = 0, to = 100) bottomEndPercent: Int = 0,
    @IntRange(from = 0, to = 100) bottomStartPercent: Int = 0,
    continuity: Continuity = Continuity.Default,
): ContinuousRoundedRectangle =
    ContinuousRoundedRectangle(
        topStart = CornerSize(topStartPercent),
        topEnd = CornerSize(topEndPercent),
        bottomEnd = CornerSize(bottomEndPercent),
        bottomStart = CornerSize(bottomStartPercent),
        continuity = continuity,
    )
