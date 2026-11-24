package top.ltfan.m3x.backdrop

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import top.ltfan.m3x.backdrop.highlight.BoundaryShaderKind
import top.ltfan.m3x.backdrop.highlight.HighlightBoundaryGeometry
import top.ltfan.m3x.backdrop.highlight.HighlightTiles
import top.ltfan.m3x.backdrop.highlight.bindBoundary
import top.ltfan.m3x.backdrop.highlight.boundaryUniformCapacity
import top.ltfan.m3x.backdrop.highlight.createSdfBakeShader
import top.ltfan.m3x.backdrop.internal.convexBoundaryShaderFunctions

class AnalyticHighlightGeometryTest : PlatformGraphicsTest() {
    @Test
    fun asymmetricCircularCornersLeaveRoomForTheWholeBandAroundTheExcludedInterior() {
        val geometry =
            HighlightBoundaryGeometry(
                Outline.Rounded(
                    RoundRect(
                        0f,
                        0f,
                        100f,
                        100f,
                        CornerRadius(90f),
                        CornerRadius.Zero,
                        CornerRadius(90f),
                        CornerRadius.Zero,
                    ),
                ),
            )
        val width = 6f
        val tiles = HighlightTiles()
        tiles.prepare(IntSize(100, 100), geometry.contourFreeBounds, width)
        assertEquals(4, tiles.count)
        for (y in tiles[0].bottom until tiles[1].top) {
            for (x in tiles[2].right until tiles[3].left) {
                val distance = geometry.nearestBoundary(Offset(x + .5f, y + .5f)).signedDistance
                assertTrue(
                    distance <= -width,
                    "Excluded pixel ($x, $y) is only $distance from the contour",
                )
            }
        }
    }

    @Test
    fun rectangleAndCircularRoundedQueriesMatchGpuForInteriorExteriorAndTies() {
        val density = Density(1f)
        val direction = LayoutDirection.Ltr
        val outlines =
            listOf(
                Outline.Rectangle(Rect(3f, 5f, 43f, 35f)),
                CircleShape.createOutline(Size(44f, 44f), direction, density),
                RoundedCornerShape(8f).createOutline(Size(44f, 36f), direction, density),
                Outline.Rounded(
                    RoundRect(
                        3f,
                        5f,
                        43f,
                        35f,
                        CornerRadius(8f),
                        CornerRadius(12f),
                        CornerRadius(6f),
                        CornerRadius(10f),
                    ),
                ),
                Outline.Rounded(
                    RoundRect(
                        3f,
                        5f,
                        43f,
                        35f,
                        CornerRadius.Zero,
                        CornerRadius(12f),
                        CornerRadius.Zero,
                        CornerRadius(10f),
                    ),
                ),
            )
        val points =
            listOf(
                Offset(4f, 6f),
                Offset(15.25f, 5.5f),
                Offset(22.5f, 5.5f),
                Offset(22.5f, 20.5f),
                Offset(42f, 34f),
                Offset(43.5f, 20f),
                Offset(44f, 36f),
            )
        for (outline in outlines) {
            val boundary = HighlightBoundaryGeometry(outline)
            assertTrue(boundary.isAnalytic)
            assertEquals(0, boundary.segmentCount)
            assertEquals(boundary.shaderFunctions(1), boundary.shaderFunctions(8))
            for (point in points) assertGpuMatchesCpu(boundary, point)
        }
    }

    @Test
    fun analyticUniformUpdatesReuseOneShaderForDifferentSizesAndRadii() {
        val first =
            HighlightBoundaryGeometry(
                Outline.Rounded(RoundRect(Rect(2f, 3f, 34f, 25f), CornerRadius(7f))),
            )
        val second =
            HighlightBoundaryGeometry(
                Outline.Rounded(RoundRect(Rect(4f, 1f, 48f, 39f), CornerRadius(14f))),
            )
        val source = first.shaderFunctions(1) + probeMain()
        assertEquals(source, second.shaderFunctions(1) + probeMain())
        val shader = RuntimeShader(source)
        val point = Offset(8.25f, 7.5f)
        first.configureShader(shader)
        shader.setFloatUniform("probePoint", point.x, point.y)
        assertShaderBoundary(first, shader, point)
        second.configureShader(shader)
        assertShaderBoundary(second, shader, point)
    }

    @Test
    fun convexCutCornersUseSupportingLinesInsideAndFiniteSegmentsOutside() {
        val boundary =
            HighlightBoundaryGeometry(
                CutCornerShape(9f).createOutline(Size(44f, 36f), LayoutDirection.Rtl, Density(1f)),
            )
        assertTrue(boundary.usesConvexSupport)
        assertEquals(BoundaryShaderKind.Convex, boundary.shaderKind(0))
        val capacity = boundary.segmentCount
        val points =
            listOf(
                Offset(22f, 18f),
                Offset(7.5f, 9.25f),
                Offset(43.5f, 5.5f),
                Offset(20f, 36.75f),
            )
        for (unroll in listOf(false, true)) {
            val functions = convexBoundaryShaderFunctions(capacity, unrollSupportSearch = unroll)
            val shader = RuntimeShader(functions + probeMain())
            boundary.bindBoundary(shader, capacity, 0)
            for (point in points) {
                shader.setFloatUniform("probePoint", point.x, point.y)
                assertShaderBoundary(boundary, shader, point, capacity, functions)
            }
        }
    }

    @Test
    fun concaveContoursKeepTheGeneralSegmentPath() {
        val path =
            Path().apply {
                moveTo(0f, 0f)
                lineTo(30f, 0f)
                lineTo(30f, 10f)
                lineTo(12f, 10f)
                lineTo(12f, 30f)
                lineTo(0f, 30f)
                close()
            }
        val boundary = HighlightBoundaryGeometry(Outline.Generic(path))
        assertFalse(boundary.usesConvexSupport)
        assertEquals(BoundaryShaderKind.Segments, boundary.shaderKind(0))
    }

    @Test
    fun largeConvexPolygonGroupsHighlightQueriesAndKeepsSupportingLinesForLens() {
        val path =
            Path().apply {
                repeat(40) { index ->
                    val angle = index * 2.0 * PI / 40.0
                    val point =
                        Offset(32f + cos(angle).toFloat() * 28f, 32f + sin(angle).toFloat() * 28f)
                    if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
                close()
            }
        val boundary = HighlightBoundaryGeometry(Outline.Generic(path))
        assertTrue(boundary.usesConvexSupport)
        val capacity = boundaryUniformCapacity(boundary.segmentCount)
        val points = listOf(Offset(32f, 32f), Offset(59.5f, 32f), Offset(65f, 48f))
        for (point in points) {
            val shader = RuntimeShader(boundary.shaderFunctions(capacity) + probeMain())
            boundary.bindBoundary(shader, capacity, groupSize = 0)
            shader.setFloatUniform("probePoint", point.x, point.y)
            assertShaderBoundary(boundary, shader, point, capacity)
        }

        val groupSize = boundary.highlightSegmentGroupSize(capacity)
        assertEquals(8, groupSize)
        val functions = boundary.shaderFunctions(capacity, groupSize)
        for (point in points) {
            val shader = RuntimeShader(functions + probeMain())
            boundary.bindBoundary(shader, capacity, groupSize)
            shader.setFloatUniform("probePoint", point.x, point.y)
            assertShaderBoundary(boundary, shader, point, capacity, functions, groupSize)
        }

        val point = Offset(45f, 49f)
        val bake = boundary.createSdfBakeShader()
        bake.setFloatUniform("origin", .5f - point.x, .5f - point.y)
        bake.setFloatUniform("sdfStep", 1f)
        bake.setFloatUniform("sdfRange", 32f)
        val expected = boundary.nearestBoundary(point)
        assertEquals((128f - expected.distance / 32f * 127f) / 255f, render(bake)[0], .002f)
    }

    @Test
    fun analyticSdfBakesKeepIndependentBounds() {
        val first = HighlightBoundaryGeometry(Outline.Rectangle(Rect(0f, 0f, 20f, 20f)))
        val second = HighlightBoundaryGeometry(Outline.Rectangle(Rect(0f, 0f, 32f, 32f)))
        val point = Offset(14f, 14f)
        val firstBake = first.createSdfBakeShader()
        val secondBake = second.createSdfBakeShader()
        for ((boundary, bake) in listOf(first to firstBake, second to secondBake)) {
            bake.setFloatUniform("origin", .5f - point.x, .5f - point.y)
            bake.setFloatUniform("sdfStep", 1f)
            bake.setFloatUniform("sdfRange", 32f)
            val distance = boundary.nearestBoundary(point).distance
            assertEquals((128f - distance / 32f * 127f) / 255f, render(bake)[0], .002f)
        }
    }

    @Test
    fun invalidRoundedAndEllipticalOutlinesKeepFlattenedFallback() {
        val invalid =
            HighlightBoundaryGeometry(
                Outline.Rounded(
                    RoundRect(
                        0f,
                        0f,
                        20f,
                        20f,
                        CornerRadius(12f),
                        CornerRadius(12f),
                        CornerRadius(0f),
                        CornerRadius(0f),
                    ),
                ),
            )
        val elliptical =
            HighlightBoundaryGeometry(
                Outline.Rounded(
                    RoundRect(
                        0f,
                        0f,
                        30f,
                        20f,
                        CornerRadius(8f, 6f),
                        CornerRadius(8f, 6f),
                        CornerRadius(8f, 6f),
                        CornerRadius(8f, 6f),
                    ),
                ),
            )
        val generic =
            HighlightBoundaryGeometry(
                Outline.Generic(Path().apply { addRect(Rect(0f, 0f, 20f, 20f)) }),
            )
        assertFalse(invalid.isAnalytic)
        assertFalse(elliptical.isAnalytic)
        assertFalse(generic.isAnalytic)
        assertTrue(invalid.cpuGeometry().segmentCount > 0)
        assertTrue(elliptical.cpuGeometry().segmentCount > 0)
        assertTrue(generic.cpuGeometry().segmentCount > 0)
    }

    @Test
    fun overlappingRoundedCornerRegionsApplyEveryContainmentConstraint() {
        val boundary =
            HighlightBoundaryGeometry(
                Outline.Rounded(
                    RoundRect(
                        0f,
                        0f,
                        100f,
                        100f,
                        CornerRadius(90f),
                        CornerRadius.Zero,
                        CornerRadius(90f),
                        CornerRadius.Zero,
                    ),
                ),
            )
        val point = Offset(80f, 80f)
        assertTrue(boundary.isAnalytic)
        assertFalse(boundary.contains(point))

        val shader = RuntimeShader(boundary.shaderFunctions(1) + containsProbeMain())
        boundary.configureShader(shader)
        shader.setFloatUniform("probePoint", point.x, point.y)
        assertEquals(0f, render(shader)[0])
    }

    private fun assertGpuMatchesCpu(boundary: HighlightBoundaryGeometry, point: Offset) {
        val shader = RuntimeShader(boundary.shaderFunctions(1) + probeMain())
        boundary.configureShader(shader)
        shader.setFloatUniform("probePoint", point.x, point.y)
        assertShaderBoundary(boundary, shader, point)
    }

    private fun assertShaderBoundary(
        boundary: HighlightBoundaryGeometry,
        shader: RuntimeShader,
        point: Offset,
        capacity: Int = 1,
        functions: String = boundary.shaderFunctions(capacity),
        groupSize: Int = 0,
    ) {
        val actual = render(shader)
        val expected = boundary.nearestBoundary(point)
        assertEquals(expected.distance / 64f, actual[0], .002f)
        assertEquals((expected.outwardNormal.x + 1f) * .5f, actual[1], .002f)
        assertEquals((expected.outwardNormal.y + 1f) * .5f, actual[2], .002f)

        val positionShader = RuntimeShader(functions + positionProbeMain())
        if (boundary.isAnalytic) boundary.configureShader(positionShader)
        else boundary.bindBoundary(positionShader, capacity, groupSize)
        positionShader.setFloatUniform("probePoint", point.x, point.y)
        val position = render(positionShader)
        assertEquals(expected.position.x / 32f, position[0], .003f)
        assertEquals(expected.position.y / 32f, position[1], .003f)
    }

    private fun probeMain(): String =
        """
        uniform float2 probePoint;
        half4 main(float2 coord) {
          float2 closest;
          float3 boundary = nearestBoundaryWithPosition(probePoint, closest);
          return half4(boundary.x / 64.0, (boundary.y + 1.0) * 0.5,
            (boundary.z + 1.0) * 0.5, 1.0);
        }
        """
            .trimIndent()

    private fun containsProbeMain(): String =
        """
        uniform float2 probePoint;
        half4 main(float2 coord) {
          return half4(containsPoint(probePoint) ? 1.0 : 0.0, 0.0, 0.0, 1.0);
        }
        """
            .trimIndent()

    private fun positionProbeMain(): String =
        """
        uniform float2 probePoint;
        half4 main(float2 coord) {
          float2 closest;
          nearestBoundaryWithPosition(probePoint, closest);
          return half4(closest.x / 32.0, closest.y / 32.0, 0.0, 1.0);
        }
        """
            .trimIndent()

    private fun render(shader: RuntimeShader): FloatArray =
        renderTestShader(shader, 1, 1, premultiplied = false)
}
