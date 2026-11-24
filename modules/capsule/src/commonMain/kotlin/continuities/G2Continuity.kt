package top.ltfan.m3x.capsule.continuities

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceIn
import kotlin.math.PI
import kotlin.math.min
import top.ltfan.m3x.capsule.AdvancedContinuity
import top.ltfan.m3x.capsule.Continuity
import top.ltfan.m3x.capsule.ContourContinuity
import top.ltfan.m3x.compose.shape.Contour
import top.ltfan.m3x.compose.shape.PathSegment
import top.ltfan.m3x.compose.shape.PathSegments
import top.ltfan.m3x.compose.shape.PathSegmentsBuilder
import top.ltfan.m3x.compose.shape.Point
import top.ltfan.m3x.compose.shape.buildPathSegments
import top.ltfan.m3x.compose.shape.lerp
import top.ltfan.m3x.compose.shape.pointOnCircle

@Immutable
public data class G2Continuity(
    val profile: G2ContinuityProfile = G2ContinuityProfile.RoundedRectangle,
    val capsuleProfile: G2ContinuityProfile = G2ContinuityProfile.Capsule,
) : AdvancedContinuity(), ContourContinuity {

    override fun createCapsuleContour(size: Size): Contour =
        Contour(createCapsulePathSegments(size.width.toDouble(), size.height.toDouble()))

    private fun resolveBezier(profile: G2ContinuityProfile) =
        when (profile) {
            this.profile -> this.profile.bezier
            this.capsuleProfile -> this.capsuleProfile.bezier
            else -> profile.bezier
        }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun resolveRatio(
        centerExtent: Double,
        cornerRadius: Double,
        extendedFraction: Double,
    ): Double = ((centerExtent / cornerRadius - 1.0) / extendedFraction).fastCoerceIn(0.0, 1.0)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun resolveBezierCurvatureScale(ratio: Double): Double =
        lerp(capsuleProfile.bezierCurvatureScale, profile.bezierCurvatureScale, ratio)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun resolveCapsuleGeometry(
        centerExtent: Double,
        radius: Double,
    ): CapsuleGeometry {
        val ratio = resolveRatio(centerExtent, radius, capsuleProfile.extendedFraction)
        val extFrac = capsuleProfile.extendedFraction * ratio
        val arcFrac = capsuleProfile.arcFraction
        val bezier =
            resolveBezier(
                G2ContinuityProfile(
                    extendedFraction = extFrac,
                    arcFraction = arcFrac,
                    bezierCurvatureScale = resolveBezierCurvatureScale(ratio),
                    arcCurvatureScale = 1.0,
                ),
            ) * radius
        val arcRad = PI * 0.5 * arcFrac
        val bezRad = (PI * 0.5 - arcRad) * 0.5
        return CapsuleGeometry(
            bezier = bezier,
            offset = -radius * extFrac,
            bezRad = bezRad,
            sweepRad = (bezRad + arcRad) * 2.0,
        )
    }

    override fun createStandardRoundedRectanglePathSegments(
        width: Double,
        height: Double,
        topLeft: Double,
        topRight: Double,
        bottomRight: Double,
        bottomLeft: Double,
    ): PathSegments {
        val centerX = width * 0.5
        val centerY = height * 0.5

        // mnemonics:
        // T: top, R: right, B: bottom, L: left
        // H: horizontal Bezier, V: vertical Bezier, C: arc, I: line

        // non-capsule ratios of each half corner
        // 0: full capsule, 1: safe rounded rectangle, (0, 1): progressive capsule
        val ratioTLV = resolveRatio(centerY, topLeft, profile.extendedFraction)
        val ratioTLH = resolveRatio(centerX, topLeft, profile.extendedFraction)
        val ratioTRH = resolveRatio(centerX, topRight, profile.extendedFraction)
        val ratioTRV = resolveRatio(centerY, topRight, profile.extendedFraction)
        val ratioBRV = resolveRatio(centerY, bottomRight, profile.extendedFraction)
        val ratioBRH = resolveRatio(centerX, bottomRight, profile.extendedFraction)
        val ratioBLH = resolveRatio(centerX, bottomLeft, profile.extendedFraction)
        val ratioBLV = resolveRatio(centerY, bottomLeft, profile.extendedFraction)

        // constrained non-capsule ratios of each corner
        val ratioTL = min(ratioTLV, ratioTLH)
        val ratioTR = min(ratioTRH, ratioTRV)
        val ratioBR = min(ratioBRV, ratioBRH)
        val ratioBL = min(ratioBLH, ratioBLV)

        // Bezier stuffs

        // extended fractions of each corner
        val extFracTL = lerp(capsuleProfile.extendedFraction, profile.extendedFraction, ratioTL)
        val extFracTR = lerp(capsuleProfile.extendedFraction, profile.extendedFraction, ratioTR)
        val extFracBR = lerp(capsuleProfile.extendedFraction, profile.extendedFraction, ratioBR)
        val extFracBL = lerp(capsuleProfile.extendedFraction, profile.extendedFraction, ratioBL)

        // resolved extended fractions of each half corner
        val extFracTLV = extFracTL * ratioTLV
        val extFracTLH = extFracTL * ratioTLH
        val extFracTRH = extFracTR * ratioTRH
        val extFracTRV = extFracTR * ratioTRV
        val extFracBRV = extFracBR * ratioBRV
        val extFracBRH = extFracBR * ratioBRH
        val extFracBLH = extFracBL * ratioBLH
        val extFracBLV = extFracBL * ratioBLV

        // offsets of each half corner
        val offsetTLV = -topLeft * extFracTLV
        val offsetTLH = -topLeft * extFracTLH
        val offsetTRH = -topRight * extFracTRH
        val offsetTRV = -topRight * extFracTRV
        val offsetBRV = -bottomRight * extFracBRV
        val offsetBRH = -bottomRight * extFracBRH
        val offsetBLH = -bottomLeft * extFracBLH
        val offsetBLV = -bottomLeft * extFracBLV

        // Bezier curvature scales of each half corner
        val bezKScaleTLV = resolveBezierCurvatureScale(ratioTLV)
        val bezKScaleTLH = resolveBezierCurvatureScale(ratioTLH)
        val bezKScaleTRH = resolveBezierCurvatureScale(ratioTRH)
        val bezKScaleTRV = resolveBezierCurvatureScale(ratioTRV)
        val bezKScaleBRV = resolveBezierCurvatureScale(ratioBRV)
        val bezKScaleBRH = resolveBezierCurvatureScale(ratioBRH)
        val bezKScaleBLH = resolveBezierCurvatureScale(ratioBLH)
        val bezKScaleBLV = resolveBezierCurvatureScale(ratioBLV)

        // arc stuffs

        // arc fractions of each corner
        val arcFracTL = lerp(capsuleProfile.arcFraction, profile.arcFraction, ratioTL)
        val arcFracTR = lerp(capsuleProfile.arcFraction, profile.arcFraction, ratioTR)
        val arcFracBR = lerp(capsuleProfile.arcFraction, profile.arcFraction, ratioBR)
        val arcFracBL = lerp(capsuleProfile.arcFraction, profile.arcFraction, ratioBL)

        // arc curvature scales of each corner
        val arcKScaleTL = 1.0 + (profile.arcCurvatureScale - 1.0) * ratioTL
        val arcKScaleTR = 1.0 + (profile.arcCurvatureScale - 1.0) * ratioTR
        val arcKScaleBR = 1.0 + (profile.arcCurvatureScale - 1.0) * ratioBR
        val arcKScaleBL = 1.0 + (profile.arcCurvatureScale - 1.0) * ratioBL

        // base Beziers of each half corner
        val bezierTLV =
            resolveBezier(G2ContinuityProfile(extFracTLV, arcFracTL, bezKScaleTLV, arcKScaleTL))
        val bezierTLH =
            resolveBezier(G2ContinuityProfile(extFracTLH, arcFracTL, bezKScaleTLH, arcKScaleTL))
        val bezierTRH =
            resolveBezier(G2ContinuityProfile(extFracTRH, arcFracTR, bezKScaleTRH, arcKScaleTR))
        val bezierTRV =
            resolveBezier(G2ContinuityProfile(extFracTRV, arcFracTR, bezKScaleTRV, arcKScaleTR))
        val bezierBRV =
            resolveBezier(G2ContinuityProfile(extFracBRV, arcFracBR, bezKScaleBRV, arcKScaleBR))
        val bezierBRH =
            resolveBezier(G2ContinuityProfile(extFracBRH, arcFracBR, bezKScaleBRH, arcKScaleBR))
        val bezierBLH =
            resolveBezier(G2ContinuityProfile(extFracBLH, arcFracBL, bezKScaleBLH, arcKScaleBL))
        val bezierBLV =
            resolveBezier(G2ContinuityProfile(extFracBLV, arcFracBL, bezKScaleBLV, arcKScaleBL))

        return buildPathSegments {
            var x = 0.0
            var y = topLeft
            moveTo(x, y - offsetTLV)

            // TL
            if (topLeft > 0.0) {
                // TLV
                with(bezierTLV) {
                    cubicTo(
                        x + control1.y * topLeft,
                        y - control1.x * topLeft,
                        x + control2.y * topLeft,
                        y - control2.x * topLeft,
                        x + end.y * topLeft,
                        y - end.x * topLeft,
                    )
                }

                // TLC
                arcToWithScaledRadius(
                    center = Point(topLeft, topLeft),
                    radius = topLeft,
                    radiusScale = 1.0 / arcKScaleTL,
                    startAngle = PI + PI * 0.5 * (1.0 - arcFracTL) * 0.5,
                    sweepAngle = PI * 0.5 * arcFracTL,
                )

                // TLH
                x = topLeft
                y = 0.0
                with(bezierTLH) {
                    cubicTo(
                        x - control2.x * topLeft,
                        y + control2.y * topLeft,
                        x - control1.x * topLeft,
                        y + control1.y * topLeft,
                        x - (start.x * topLeft).fastCoerceAtLeast(offsetTLH),
                        y + start.y * topLeft,
                    )
                }
            }

            // TI
            x = width - topRight
            y = 0.0
            lineTo(x + offsetTRH, y)

            // TR
            if (topRight > 0.0) {
                // TRH
                with(bezierTRH) {
                    cubicTo(
                        x + control1.x * topRight,
                        y + control1.y * topRight,
                        x + control2.x * topRight,
                        y + control2.y * topRight,
                        x + end.x * topRight,
                        y + end.y * topRight,
                    )
                }

                // TRC
                arcToWithScaledRadius(
                    center = Point(width - topRight, topRight),
                    radius = topRight,
                    radiusScale = 1.0 / arcKScaleTR,
                    startAngle = -PI * 0.5 + PI * 0.5 * (1.0 - arcFracTR) * 0.5,
                    sweepAngle = PI * 0.5 * arcFracTR,
                )

                // TRV
                x = width
                y = topRight
                with(bezierTRV) {
                    cubicTo(
                        x - control2.y * topRight,
                        y - control2.x * topRight,
                        x - control1.y * topRight,
                        y - control1.x * topRight,
                        x - start.y * topRight,
                        y - (start.x * topRight).fastCoerceAtLeast(offsetTRV),
                    )
                }
            }

            // RI
            x = width
            y = height - bottomRight
            lineTo(x, y + offsetBRV)

            // BR
            if (bottomRight > 0.0) {
                // BRV
                with(bezierBRV) {
                    cubicTo(
                        x - control1.y * bottomRight,
                        y + control1.x * bottomRight,
                        x - control2.y * bottomRight,
                        y + control2.x * bottomRight,
                        x - end.y * bottomRight,
                        y + end.x * bottomRight,
                    )
                }

                // BRC
                arcToWithScaledRadius(
                    center = Point(width - bottomRight, height - bottomRight),
                    radius = bottomRight,
                    radiusScale = 1.0 / arcKScaleBR,
                    startAngle = 0.0 + PI * 0.5 * (1.0 - arcFracBR) * 0.5,
                    sweepAngle = PI * 0.5 * arcFracBR,
                )

                // BRH
                x = width - bottomRight
                y = height
                with(bezierBRH) {
                    cubicTo(
                        x + control2.x * bottomRight,
                        y - control2.y * bottomRight,
                        x + control1.x * bottomRight,
                        y - control1.y * bottomRight,
                        x + (start.x * bottomRight).fastCoerceAtLeast(offsetBRH),
                        y - start.y * bottomRight,
                    )
                }
            }

            // BI
            x = bottomLeft
            y = height
            lineTo(x - offsetBLH, y)

            // BL
            if (bottomLeft > 0.0) {
                // BLH
                with(bezierBLH) {
                    cubicTo(
                        x - control1.x * bottomLeft,
                        y - control1.y * bottomLeft,
                        x - control2.x * bottomLeft,
                        y - control2.y * bottomLeft,
                        x - end.x * bottomLeft,
                        y - end.y * bottomLeft,
                    )
                }

                // BLC
                arcToWithScaledRadius(
                    center = Point(bottomLeft, height - bottomLeft),
                    radius = bottomLeft,
                    radiusScale = 1.0 / arcKScaleBL,
                    startAngle = PI * 0.5 + PI * 0.5 * (1.0 - arcFracBL) * 0.5,
                    sweepAngle = PI * 0.5 * arcFracBL,
                )

                // BLV
                x = 0.0
                y = height - bottomLeft
                with(bezierBLV) {
                    cubicTo(
                        x + control2.y * bottomLeft,
                        y + control2.x * bottomLeft,
                        x + control1.y * bottomLeft,
                        y + control1.x * bottomLeft,
                        x + start.y * bottomLeft,
                        y + (start.x * bottomLeft).fastCoerceAtLeast(offsetBLV),
                    )
                }
            }

            // LI
            close()
        }
    }

    override fun createHorizontalCapsulePathSegments(width: Double, height: Double): PathSegments {
        val radius = height * 0.5

        val (bezierH, offsetH, bezRad, sweepRad) = resolveCapsuleGeometry(width * 0.5, radius)

        return buildPathSegments {
            var x = 0.0
            var y = radius
            moveTo(x, y)

            // LC
            arcTo(
                center = Point(radius, radius),
                radius = radius,
                startAngle = PI * 0.5 + bezRad,
                sweepAngle = sweepRad,
            )

            // TLH
            x = radius
            y = 0.0
            with(bezierH) {
                cubicTo(
                    x - control2.x,
                    y + control2.y,
                    x - control1.x,
                    y + control1.y,
                    x - start.x.fastCoerceAtLeast(offsetH),
                    y + start.y,
                )
            }

            // TI
            x = width - radius
            y = 0.0
            lineTo(x + offsetH, y)

            // TRH
            with(bezierH) {
                cubicTo(
                    x + control1.x,
                    y + control1.y,
                    x + control2.x,
                    y + control2.y,
                    x + end.x,
                    y + end.y,
                )
            }

            // RC
            arcTo(
                center = Point(width - radius, radius),
                radius = radius,
                startAngle = -(PI * 0.5 - bezRad),
                sweepAngle = sweepRad,
            )

            // BRH
            x = width - radius
            y = height
            with(bezierH) {
                cubicTo(
                    x + control2.x,
                    y - control2.y,
                    x + control1.x,
                    y - control1.y,
                    x + start.x.fastCoerceAtLeast(offsetH),
                    y - start.y,
                )
            }

            // BI
            x = radius
            y = height
            lineTo(x - offsetH, y)

            // BLH
            with(bezierH) {
                cubicTo(
                    x - control1.x,
                    y - control1.y,
                    x - control2.x,
                    y - control2.y,
                    x - end.x,
                    y - end.y,
                )
            }
        }
    }

    override fun createVerticalCapsulePathSegments(width: Double, height: Double): PathSegments {
        val radius = width * 0.5

        val (bezierV, offsetV, bezRad, sweepRad) = resolveCapsuleGeometry(height * 0.5, radius)

        return buildPathSegments {
            var x = 0.0
            var y = radius
            moveTo(x, y - offsetV)

            // TLV
            with(bezierV) {
                cubicTo(
                    x + control1.y,
                    y - control1.x,
                    x + control2.y,
                    y - control2.x,
                    x + end.y,
                    y - end.x,
                )
            }

            // TC
            arcTo(
                center = Point(radius, radius),
                radius = radius,
                startAngle = -(PI - bezRad),
                sweepAngle = sweepRad,
            )

            // TRV
            x = width
            y = radius
            with(bezierV) {
                cubicTo(
                    x - control2.y,
                    y - control2.x,
                    x - control1.y,
                    y - control1.x,
                    x - start.y,
                    y - start.x.fastCoerceAtLeast(offsetV),
                )
            }

            // RI
            x = width
            y = height - radius
            lineTo(x, y + offsetV)

            // BRV
            with(bezierV) {
                cubicTo(
                    x - control1.y,
                    y + control1.x,
                    x - control2.y,
                    y + control2.x,
                    x - end.y,
                    y + end.x,
                )
            }

            // BC
            arcTo(
                center = Point(width - radius, height - radius),
                radius = radius,
                startAngle = bezRad,
                sweepAngle = sweepRad,
            )

            // BLV
            x = 0.0
            y = height - radius
            with(bezierV) {
                cubicTo(
                    x + control2.y,
                    y + control2.x,
                    x + control1.y,
                    y + control1.x,
                    x + start.y,
                    y + start.x.fastCoerceAtLeast(offsetV),
                )
            }

            // LI
            close()
        }
    }

    override fun lerp(stop: Continuity, fraction: Double): Continuity {
        return when (stop) {
            is G2Continuity ->
                G2Continuity(
                    profile = lerp(this.profile, stop.profile, fraction),
                    capsuleProfile = lerp(this.capsuleProfile, stop.capsuleProfile, fraction),
                )

            else -> stop.lerp(this, 1f - fraction)
        }
    }
}

private data class CapsuleGeometry(
    val bezier: PathSegment.Cubic,
    val offset: Double,
    val bezRad: Double,
    val sweepRad: Double,
)

private fun PathSegmentsBuilder.arcToWithScaledRadius(
    center: Point,
    radius: Double,
    radiusScale: Double,
    startAngle: Double,
    sweepAngle: Double,
) {
    val centerAngle = startAngle + sweepAngle * 0.5
    return arcTo(
        center =
            pointOnCircle(center.x, center.y, radius * (1.0 - radiusScale), centerAngle, ::Point),
        radius = radius * radiusScale,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
    )
}
