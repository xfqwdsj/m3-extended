package top.ltfan.m3x.material.theme

import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Creates shapes from logical or physical corner sizes. */
@Immutable
public interface M3xShapeFactory {
    /** Creates a shape whose corners follow layout direction. */
    public fun logical(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ): CornerBasedShape

    /** Creates a shape whose corners stay at physical positions. */
    public fun physical(
        topLeft: CornerSize,
        topRight: CornerSize,
        bottomRight: CornerSize,
        bottomLeft: CornerSize,
    ): CornerBasedShape

    /** Shape with equal corners sized relative to its bounds. */
    public val full: Shape

    /** Shape with no rounded corners. */
    public val none: Shape
}

/** Standard rounded-corner implementation for M3X shape roles. */
public object RoundedM3xShapeFactory : M3xShapeFactory {
    override fun logical(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ): CornerBasedShape = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)

    override fun physical(
        topLeft: CornerSize,
        topRight: CornerSize,
        bottomRight: CornerSize,
        bottomLeft: CornerSize,
    ): CornerBasedShape = AbsoluteRoundedCornerShape(topLeft, topRight, bottomRight, bottomLeft)

    override val full: Shape = CircleShape
    override val none: Shape = RectangleShape
}

private val DefaultRadii = M3xCornerRadii()

private fun M3xShapeFactory.logical(radius: Dp): CornerBasedShape =
    logical(CornerSize(radius), CornerSize(radius), CornerSize(radius), CornerSize(radius))

private fun M3xShapeFactory.physical(
    topLeft: Dp,
    topRight: Dp,
    bottomRight: Dp,
    bottomLeft: Dp,
): CornerBasedShape =
    physical(
        CornerSize(topLeft),
        CornerSize(topRight),
        CornerSize(bottomRight),
        CornerSize(bottomLeft),
    )

/** The unified M3X shape roles, including size-relative and physical container-corner shapes. */
@Immutable
public data class M3xShapes(
    public val cornerRadii: M3xCornerRadii? = null,
    public val factory: M3xShapeFactory = RoundedM3xShapeFactory,
    public val extraSmall: CornerBasedShape =
        factory.logical((cornerRadii ?: DefaultRadii).extraSmall),
    public val small: CornerBasedShape = factory.logical((cornerRadii ?: DefaultRadii).small),
    public val medium: CornerBasedShape = factory.logical((cornerRadii ?: DefaultRadii).medium),
    public val large: CornerBasedShape = factory.logical((cornerRadii ?: DefaultRadii).large),
    public val extraLarge: CornerBasedShape =
        factory.logical((cornerRadii ?: DefaultRadii).extraLarge),
    public val largeIncreased: CornerBasedShape =
        factory.logical((cornerRadii ?: DefaultRadii).largeIncreased),
    public val extraLargeIncreased: CornerBasedShape =
        factory.logical((cornerRadii ?: DefaultRadii).extraLargeIncreased),
    public val extraExtraLarge: CornerBasedShape =
        factory.logical((cornerRadii ?: DefaultRadii).extraExtraLarge),
    public val full: Shape = factory.full,
    public val none: Shape = factory.none,
    public val container: CornerBasedShape =
        (cornerRadii ?: DefaultRadii).resolvedContainerCorners.let { corners ->
            factory.physical(
                requireNotNull(corners.topLeft),
                requireNotNull(corners.topRight),
                requireNotNull(corners.bottomRight),
                requireNotNull(corners.bottomLeft),
            )
        },
)

/** Converts the eight configurable M3X roles to Material 3 shapes. */
public fun M3xShapes.toMaterialShapes(): Shapes =
    Shapes(
        extraSmall = extraSmall,
        small = small,
        medium = medium,
        large = large,
        extraLarge = extraLarge,
        largeIncreased = largeIncreased,
        extraLargeIncreased = extraLargeIncreased,
        extraExtraLarge = extraExtraLarge,
    )

/** Shape roles and radius metadata outside Material 3's configurable shapes. */
@Immutable
public data class M3xShapeExtensions(
    public val factory: M3xShapeFactory = RoundedM3xShapeFactory,
    public val full: Shape = factory.full,
    public val none: Shape = factory.none,
    public val container: CornerBasedShape =
        factory.physical(
            CornerSize(32.dp),
            CornerSize(32.dp),
            CornerSize(32.dp),
            CornerSize(32.dp),
        ),
    public val cornerRadii: M3xCornerRadii? = null,
)

/** Extracts the M3X extension shapes from a complete preset. */
public fun M3xShapes.toShapeExtensions(): M3xShapeExtensions =
    M3xShapeExtensions(
        factory = factory,
        full = full,
        none = none,
        container = container,
        cornerRadii = cornerRadii,
    )

/** Combines these M3X extension shapes with the native Material 3 shape roles. */
public fun M3xShapeExtensions.toM3xShapes(shapes: Shapes): M3xShapes =
    M3xShapes(
        factory = factory,
        cornerRadii = cornerRadii,
        extraSmall = shapes.extraSmall,
        small = shapes.small,
        medium = shapes.medium,
        large = shapes.large,
        extraLarge = shapes.extraLarge,
        largeIncreased = shapes.largeIncreased,
        extraLargeIncreased = shapes.extraLargeIncreased,
        extraExtraLarge = shapes.extraExtraLarge,
        full = full,
        none = none,
        container = container,
    )
