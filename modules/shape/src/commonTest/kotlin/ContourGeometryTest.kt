package top.ltfan.m3x.compose.shape

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ContourGeometryTest {
    @Test
    fun signedDistanceFieldAcceptsExplicitFlatness() {
        val contour = Contour(listOf(PathSegment.Circle(Point.Zero, 10.0)))

        val field = assertNotNull(contour.toSignedDistanceField(flatness = .2f))
        assertEquals(-10f, field.distanceAt(0f, 0f), .001f)
        assertFailsWith<IllegalArgumentException> {
            contour.toSignedDistanceField(flatness = -.1f)
        }
    }

    @Test
    fun oneContourAcceptsMultipleHolesAndDisconnectedLoops() {
        val outer = PathSegment.Circle(Point.Zero, 20.0)
        val firstHole = PathSegment.Arc(Point(-8.0, 0.0), 3.0, 0.0, -2.0 * PI)
        val secondHole = PathSegment.Arc(Point(8.0, 0.0), 3.0, 0.0, -2.0 * PI)
        val island = PathSegment.Circle(Point(30.0, 0.0), 2.0)
        val contour = Contour(listOf(outer), listOf(firstHole), listOf(secondHole), listOf(island))

        assertEquals(
            listOf(outer, firstHole, secondHole, island),
            contour.loops.map { it.single() },
        )

        val field = assertNotNull(contour.toSignedDistanceField())
        assertTrue(field.distanceAt(0f, 0f) < 0f)
        assertTrue(field.distanceAt(-8f, 0f) > 0f)
        assertTrue(field.distanceAt(8f, 0f) > 0f)
        assertTrue(field.distanceAt(30f, 0f) < 0f)
        assertTrue(field.distanceAt(25f, 0f) > 0f)
    }

    @Test
    fun boundsIncludeCardinalExtremaAlongASignedSweep() {
        val arc = PathSegment.Arc(Offset.Zero, 10f, 45f, -90f)
        val contour = Contour(listOf(arc, PathSegment.Line(arc.end, arc.start)))

        assertEquals(Point(10.0, 0.0), arc.pointAt(0.5))
        val bounds = contour.conservativeBounds
        assertEquals(arc.start.x.toFloat(), bounds.left, 0.00001f)
        assertEquals(arc.end.y.toFloat(), bounds.top, 0.00001f)
        assertEquals(10f, bounds.right)
        assertEquals(arc.start.y.toFloat(), bounds.bottom, 0.00001f)
    }

    @Test
    fun largeQuarterArcsMeetAtExactCardinalPoints() {
        val radius = 8000f
        val center = Offset(radius, radius)
        val arcs = List(4) { PathSegment.Arc(center, radius, it * 90f, 90f) }

        assertEquals(
            listOf(
                Point(16000.0, 8000.0),
                Point(8000.0, 16000.0),
                Point(0.0, 8000.0),
                Point(8000.0, 0.0),
            ),
            arcs.map { it.start },
        )
        assertEquals(arcs.first().start, arcs.last().end)
        Contour(arcs)
    }

    @Test
    fun arcJoinsAllowCoordinateRoundingAndRejectGaps() {
        val arc = PathSegment.Arc(Offset(8000f, 8000f), 8000f, 90f, 90f)
        Contour(listOf(arc, PathSegment.Line(arc.end + Point(0.0015, 0.0), arc.start)))

        assertFailsWith<IllegalArgumentException> {
            Contour(listOf(arc, PathSegment.Line(arc.end + Point(0.1, 0.0), arc.start)))
        }
    }
}
