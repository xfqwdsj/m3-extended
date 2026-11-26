package top.ltfan.m3x.compose.shape

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.registerSkikoComposeImplementation
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PathFlatteningTest {
    @BeforeTest
    @OptIn(InternalComposeUiApi::class)
    fun registerGraphics() {
        registerSkikoComposeImplementation()
    }

    @Test
    fun flattenedPathClosesOpenSubpathsAndPreservesDistanceSign() {
        val path =
            Path().apply {
                moveTo(0f, 0f)
                lineTo(10f, 0f)
                lineTo(10f, 10f)
                lineTo(0f, 10f)
            }

        val contour = assertNotNull(path.toFlattenedContour())
        assertEquals(1, contour.loops.size)
        assertTrue(contour.loops.single().all { it is PathSegment.Line })

        val field = assertNotNull(path.toSignedDistanceField())
        assertTrue(field.distanceAt(5f, 5f) < 0f)
        assertTrue(field.distanceAt(15f, 5f) > 0f)
    }

    @Test
    fun flattenedPathCurvesHonorRequestedFlatness() {
        val path =
            Path().apply {
                moveTo(0f, 0f)
                cubicTo(0f, 10f, 10f, 10f, 10f, 0f)
            }

        val coarse = assertNotNull(path.toFlattenedContour(flatness = 1f))
        val fine = assertNotNull(path.toFlattenedContour(flatness = .01f))
        assertTrue(fine.loops.single().size > coarse.loops.single().size)
        assertFailsWith<IllegalArgumentException> { path.toFlattenedContour(flatness = -1f) }
    }
}
