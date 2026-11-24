package top.ltfan.m3x.compose.shape

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.platform.registerSkikoComposeImplementation
import kotlin.math.sqrt
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PathDistanceTest {
    @BeforeTest
    @OptIn(InternalComposeUiApi::class)
    fun registerGraphics() {
        registerSkikoComposeImplementation()
    }

    @Test
    fun openSubpathUsesImplicitClosingEdgeAndIgnoresAnIsolatedLine() {
        val path = Path()
        assertNull(path.toSignedDistanceField())
        path.moveTo(-50f, 1f)
        path.lineTo(50f, 1f)
        assertNull(path.toSignedDistanceField())

        path.moveTo(0f, 0f)
        path.lineTo(12f, 0f)
        path.lineTo(12f, 12f)
        val field = assertNotNull(path.toSignedDistanceField())
        assertEquals(-sqrt(8f), field.distanceAt(8f, 4f), .001f)
        assertEquals(sqrt(8f), field.distanceAt(4f, 8f), .001f)
        assertEquals(0f, field.distanceAt(6f, 6f), .001f)
    }

    @Test
    fun overlappingLoopsUseTheOriginalFillRule() {
        val path =
            Path().apply {
                addRect(Rect(0f, 0f, 20f, 20f))
                addRect(Rect(10f, 0f, 30f, 20f))
            }
        val nonZero = assertNotNull(path.toSignedDistanceField())
        assertEquals(-8f, nonZero.distanceAt(15f, 8f), .001f)

        path.fillType = PathFillType.EvenOdd
        val evenOdd = assertNotNull(path.toSignedDistanceField())
        assertEquals(5f, evenOdd.distanceAt(15f, 8f), .001f)
        assertEquals(-5f, evenOdd.distanceAt(5f, 8f), .001f)
        assertEquals(PathFillType.EvenOdd, path.fillType)
        assertEquals(-8f, nonZero.distanceAt(15f, 8f), .001f)
    }

    @Test
    fun evenOddHoleRetainsItsCurvedBoundary() {
        val path =
            Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(0f, 0f, 40f, 40f))
                addOval(Rect(10f, 10f, 30f, 30f))
            }
        val field = assertNotNull(path.toSignedDistanceField())
        assertEquals(10f, field.distanceAt(20f, 20f), .05f)
        assertEquals(-5f, field.distanceAt(5f, 20f), .05f)
    }
}
