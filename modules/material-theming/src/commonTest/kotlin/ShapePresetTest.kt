package top.ltfan.m3x.material.theming

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import top.ltfan.m3x.capsule.ContinuousCapsule
import top.ltfan.m3x.capsule.ContinuousRoundedRectangle
import top.ltfan.m3x.compose.shape.ContourShape
import top.ltfan.m3x.compose.ui.layout.CornerRadii
import top.ltfan.m3x.compose.ui.layout.WindowPresentation
import top.ltfan.m3x.material.theme.M3xCornerRadii
import top.ltfan.m3x.material.theme.adaptiveCornerRadii
import top.ltfan.m3x.material.theme.toM3xShapes
import top.ltfan.m3x.material.theme.toMaterialShapes
import top.ltfan.m3x.material.theme.toShapeExtensions
import top.ltfan.m3x.material.theming.capsule.CapsuleM3xShapeFactory
import top.ltfan.m3x.material.theming.capsule.capsuleShapes

class ShapePresetTest {
    @Test
    fun shapeFactoryCreatesRolesAndSurvivesPresetConversions() {
        val radii = M3xCornerRadii(medium = 23.dp, containerRadius = 37.dp)
        val shapes = capsuleShapes(radii)
        assertSame(CapsuleM3xShapeFactory, shapes.factory)
        assertIs<ContinuousRoundedRectangle>(shapes.medium)
        assertEquals(radii, shapes.cornerRadii)
        assertSame(shapes.factory, shapes.toShapeExtensions().factory)
        assertSame(
            shapes.factory,
            shapes.toShapeExtensions().toM3xShapes(shapes.toMaterialShapes()).factory,
        )

        val roleOverride = RoundedCornerShape(9.dp)
        val overridden = shapes.copy(medium = roleOverride)
        assertSame(roleOverride, overridden.medium)
        assertSame(shapes.factory, overridden.factory)
        assertIs<ContinuousRoundedRectangle>(overridden.small)
    }

    @Test
    fun capsuleFactoryUsesContinuousShapesAndRetainsRadiusMetadata() {
        val radii =
            adaptiveCornerRadii(
                CornerRadii(12.dp, 24.dp, 48.dp, 32.dp),
                null,
                WindowPresentation.Fullscreen,
                isMobile = true,
            )
        val shapes = capsuleShapes(radii)
        listOf(
                shapes.extraSmall,
                shapes.small,
                shapes.medium,
                shapes.large,
                shapes.extraLarge,
                shapes.largeIncreased,
                shapes.extraLargeIncreased,
                shapes.extraExtraLarge,
            )
            .forEach { assertIs<ContinuousRoundedRectangle>(it) }
        assertEquals(ContinuousCapsule, shapes.full)
        for (shape in listOf(shapes.medium, shapes.full)) {
            val contourShape = assertIs<ContourShape>(shape)
            assertNotNull(
                contourShape.createContour(Size(100f, 80f), LayoutDirection.Ltr, Density(1f)),
            )
        }
        assertEquals(radii, shapes.cornerRadii)
        assertEquals(32.dp, radii.resolvedContainerCorners.bottomLeft)

        val logicalCopy =
            shapes.medium.copy(
                bottomStart = shapes.small.bottomStart,
                bottomEnd = shapes.small.bottomEnd,
            )
        assertIs<ContinuousRoundedRectangle>(logicalCopy)
        assertEquals(shapes.small.bottomStart, logicalCopy.bottomStart)
    }
}
