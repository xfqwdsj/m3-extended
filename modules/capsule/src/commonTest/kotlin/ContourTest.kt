package top.ltfan.m3x.capsule

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import top.ltfan.m3x.capsule.continuities.G1Continuity
import top.ltfan.m3x.compose.shape.Contour
import top.ltfan.m3x.compose.shape.ContourSdfShape
import top.ltfan.m3x.compose.shape.ContourShape
import top.ltfan.m3x.compose.shape.PathSegment
import top.ltfan.m3x.compose.shape.PathSegments
import top.ltfan.m3x.compose.shape.toContour

class ContourTest {
    @Test
    fun capsuleShapesProvideSignedDistanceOnRequest() {
        val size = Size(100f, 40f)
        val density = Density(1f)
        for (shape in listOf(ContinuousCapsule, AbsoluteContinuousCapsule)) {
            val provider = assertIs<ContourSdfShape>(shape)
            val field = assertNotNull(provider.createSdf(size, LayoutDirection.Ltr, density))
            val coarseField =
                assertNotNull(
                    provider.createSdf(
                        size,
                        LayoutDirection.Ltr,
                        density,
                        flatness = .2f,
                    ),
                )
            assertTrue(field.distanceAt(50f, 20f) < 0f)
            assertTrue(coarseField.distanceAt(50f, 20f) < 0f)
            assertTrue(field.distanceAt(-2f, 20f) > 0f)
            assertEquals(0f, field.distanceAt(50f, 0f), 0.001f)
            assertNull(provider.createSdf(Size.Zero, LayoutDirection.Ltr, density))
        }
    }

    @Test
    fun customContourContinuityWorksThroughFormalShapes() {
        val custom =
            object : ContourContinuity {
                override fun createRoundedRectanglePathSegments(
                    width: Double,
                    height: Double,
                    topLeft: Double,
                    topRight: Double,
                    bottomRight: Double,
                    bottomLeft: Double,
                ): PathSegments =
                    G1Continuity.createRoundedRectanglePathSegments(
                        width,
                        height,
                        topLeft,
                        topRight,
                        bottomRight,
                        bottomLeft,
                    )

                override fun createCapsuleOutline(size: Size): Outline =
                    Outline.Rectangle(capsuleBounds)

                override fun createCapsuleContour(size: Size) = capsuleBounds.toContour()

                override fun lerp(stop: Continuity, fraction: Double): Continuity = this

                private val capsuleBounds = Rect(2f, 3f, 20f, 40f)
            }
        val size = Size(100f, 80f)
        val density = Density(1f)
        val direction = LayoutDirection.Rtl
        val rounded = ContinuousRoundedRectangle(15f, custom)
        val contour = assertNotNull(rounded.createContour(size, direction, density))
        assertTrue(contour.loops.single().any { it is PathSegment.Arc })

        for (capsule in
            listOf<ContourShape>(ContinuousCapsule(custom), AbsoluteContinuousCapsule(custom))) {
            assertEquals(
                capsule.createOutline(size, direction, density).bounds,
                assertNotNull(capsule.createContour(size, direction, density)).conservativeBounds,
            )
        }
    }

    @Test
    fun logicalAndPhysicalShapesExposeClosedStructuredContours() {
        val size = Size(120f, 80f)
        val density = Density(1f)
        val shapes =
            listOf(
                ContinuousRoundedRectangle(
                    topStart = CornerSize(6f),
                    topEnd = CornerSize(24f),
                    bottomEnd = CornerSize(12f),
                    bottomStart = CornerSize(32f),
                ),
                AbsoluteContinuousRoundedRectangle(
                    topLeft = CornerSize(6f),
                    topRight = CornerSize(24f),
                    bottomRight = CornerSize(12f),
                    bottomLeft = CornerSize(32f),
                ),
                ContinuousCapsule,
            )

        for (shape in shapes) {
            val contourShape = assertIs<ContourShape>(shape)
            val contour =
                assertNotNull(
                    contourShape.createContour(size, LayoutDirection.Ltr, density),
                )
            val loop = contour.loops.single()
            assertTrue(loop.size >= 4)
            assertTrue(
                abs(loop.first().start.x - loop.last().end.x) <= .001 &&
                    abs(loop.first().start.y - loop.last().end.y) <= .001,
            )
        }
    }

    @Test
    fun contourUsesScaledCornerPairsAndOnlyMirrorsLogicalCorners() {
        val logical = ContinuousRoundedRectangle(90f, 0f, 0f, 60f, G1Continuity)
        val physical = AbsoluteContinuousRoundedRectangle(90f, 0f, 0f, 60f, G1Continuity)
        val size = Size(100f, 100f)
        val density = Density(1f)

        fun ContourShape.arcs(direction: LayoutDirection): List<PathSegment.Arc> =
            assertNotNull(createContour(size, direction, density))
                .loops
                .single()
                .filterIsInstance<PathSegment.Arc>()

        val left = logical.arcs(LayoutDirection.Ltr)
        assertEquals(listOf(50f, 40f), left.map { it.radius.toFloat() })
        assertEquals(
            listOf(Offset(50f, 50f), Offset(40f, 60f)),
            left.map { Offset(it.center.x.toFloat(), it.center.y.toFloat()) },
        )
        assertEquals(
            listOf(Offset(50f, 50f), Offset(60f, 60f)),
            logical.arcs(LayoutDirection.Rtl).map {
                Offset(it.center.x.toFloat(), it.center.y.toFloat())
            },
        )
        assertEquals(left, physical.arcs(LayoutDirection.Ltr))
        assertEquals(left, physical.arcs(LayoutDirection.Rtl))
    }

    @Test
    fun derivedShapesRetainContoursAndCustomOutlinesUseFallback() {
        val size = Size(100f, 100f)
        val direction = LayoutDirection.Ltr
        val density = Density(1f)
        val customContinuity = object : Continuity by G1Continuity {}
        val customShape =
            object :
                ContinuousRoundedRectangle(
                    CornerSize(20f),
                    CornerSize(20f),
                    CornerSize(20f),
                    CornerSize(20f),
                ) {
                override fun createOutline(
                    size: Size,
                    topStart: Float,
                    topEnd: Float,
                    bottomEnd: Float,
                    bottomStart: Float,
                    layoutDirection: LayoutDirection,
                ): Outline = Outline.Rectangle(Rect(0f, 0f, 5f, 5f))
            }

        assertNull(customShape.createContour(size, direction, density))
        assertNull(ContinuousCapsule(customContinuity).createContour(size, direction, density))
        assertNull(
            AbsoluteContinuousRoundedRectangle(20f, customContinuity)
                .createContour(size, direction, density),
        )
        assertNotNull(
            ContinuousCapsule.concentricInset(4.dp).createContour(size, direction, density),
        )
        assertNotNull(
            lerp(AbsoluteContinuousRectangle, AbsoluteContinuousCapsule, 0.5f)
                .createContour(size, direction, density),
        )
    }

    @Test
    fun largeAsymmetricG2ShapeProvidesAClosedContour() {
        val shape = ContinuousRoundedRectangle(56.9458f, 9156.333f, 1478.3716f, 6788.165f)
        val contour =
            assertNotNull(
                shape.createContour(Size(24700f, 16000f), LayoutDirection.Ltr, Density(1f)),
            )
        val segments = contour.loops.single()
        assertTrue(segments.any { it is PathSegment.Arc })
        assertTrue(segments.any { it is PathSegment.Cubic })
    }

    @Test
    fun largeSquareCapsuleKeepsItsSingleCircleSegment() {
        val contour =
            assertNotNull(
                ContinuousCapsule.createContour(
                    Size(16000f, 16000f),
                    LayoutDirection.Ltr,
                    Density(1f),
                ),
            )
        val circle = assertIs<PathSegment.Circle>(contour.loops.single().single())
        assertEquals(2.0 * PI, circle.sweepRadians)
        assertEquals(circle.start, circle.end)
    }

    @Test
    fun contourRetainsCapsuleSegmentsByIdentity() {
        val segments =
            Continuity.Default.createRoundedRectanglePathSegments(
                100.0,
                80.0,
                10.0,
                20.0,
                30.0,
                20.0,
            )
        val contour = Contour(segments)
        val contourSegments = contour.loops.single()
        assertEquals(segments.size, contourSegments.size)
        segments.zip(contourSegments).forEach { (segment, contourSegment) ->
            assertSame(segment, contourSegment)
        }
    }
}
