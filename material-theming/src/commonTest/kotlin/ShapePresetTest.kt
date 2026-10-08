package top.ltfan.m3x.material.theming

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.compose.ui.layout.CornerRadii
import top.ltfan.m3x.compose.ui.layout.WindowPresentation
import top.ltfan.m3x.material.theme.ExpressiveCornerRadii
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

    @Test
    fun roundedFactoryKeepsLogicalFiniteRolesAndPhysicalCornerRadii() =
        runInitializedComposeUiTest {
            val radii =
                adaptiveCornerRadii(
                    CornerRadii(8.dp, 16.dp, 24.dp, 32.dp),
                    null,
                    WindowPresentation.Fullscreen,
                    isMobile = true,
                )
            val shapes = roundedShapes(radii)
            assertIs<RoundedCornerShape>(shapes.extraSmall)
            assertEquals(radii, shapes.cornerRadii)

            val density = Density(1f)
            val size = Size(200f, 200f)
            // TODO: Restore conic path hit assertions after
            // https://youtrack.jetbrains.com/issue/CMP-10965 is resolved.
            // val capsuleScreen = capsuleShapes(radii).container
            // val capsuleLtr =
            //     capsuleScreen.createOutline(size, LayoutDirection.Ltr, density) as
            // Outline.Generic
            // val capsuleRtl =
            //     capsuleScreen.createOutline(size, LayoutDirection.Rtl, density) as
            // Outline.Generic
            // val ltrHits = PathHitTester(capsuleLtr.path, tolerance = 0.5f)
            // val rtlHits = PathHitTester(capsuleRtl.path, tolerance = 0.5f)
            // for (x in 0..200 step 4) {
            //     for (y in 0..200 step 4) {
            //         assertEquals(
            //             Offset(x.toFloat(), y.toFloat()) in ltrHits,
            //             Offset(x.toFloat(), y.toFloat()) in rtlHits,
            //         )
            //     }
            // }
            // assertTrue(Offset(7f, 1f) in ltrHits)
            // assertFalse(Offset(192f, 1f) in ltrHits)

            val ltr = shapes.container.createOutline(size, LayoutDirection.Ltr, density)
            val rtl = shapes.container.createOutline(size, LayoutDirection.Rtl, density)
            assertIs<Outline.Rounded>(ltr)
            assertEquals(ltr, rtl)

            val logicalCopy =
                (shapes.medium as RoundedCornerShape).copy(bottomStart = CornerSize(2.dp))
            val logicalLtr = logicalCopy.createOutline(size, LayoutDirection.Ltr, density)
            val logicalRtl = logicalCopy.createOutline(size, LayoutDirection.Rtl, density)
            assertNotEquals(logicalLtr, logicalRtl)
        }

    @Test
    fun fullShapeAdaptsToItsDrawnSizeAndCustomRadiiArePreserved() = runInitializedComposeUiTest {
        val shapes =
            capsuleShapes(ExpressiveCornerRadii.copy(extraSmall = 3.dp, containerRadius = 41.dp))
        assertEquals(3.dp, shapes.cornerRadii?.extraSmall)
        assertEquals(41.dp, shapes.cornerRadii?.containerRadius)
        val density = Density(1f)
        // TODO: Restore conic path hit assertions after
        // https://youtrack.jetbrains.com/issue/CMP-10965 is resolved.
        // val shortOutline =
        //     shapes.full.createOutline(Size(120f, 40f), LayoutDirection.Ltr, density)
        //         as Outline.Generic
        // val tallOutline =
        //     shapes.full.createOutline(Size(120f, 80f), LayoutDirection.Ltr, density)
        //         as Outline.Generic
        // val shortHits = PathHitTester(shortOutline.path, tolerance = 0.5f)
        // val tallHits = PathHitTester(tallOutline.path, tolerance = 0.5f)
        // assertFalse(Offset(60f, 60f) in shortHits)
        // assertTrue(Offset(60f, 60f) in tallHits)
        val fullShape = shapes.full as CornerBasedShape
        assertEquals(20f, fullShape.topStart.toPx(Size(120f, 40f), density))
        assertEquals(40f, fullShape.topStart.toPx(Size(120f, 80f), density))

        val finiteShape = roundedShapes(ExpressiveCornerRadii).extraExtraLarge
        val narrow = finiteShape.createOutline(Size(120f, 40f), LayoutDirection.Ltr, density)
        val square = finiteShape.createOutline(Size(120f, 120f), LayoutDirection.Ltr, density)
        assertEquals(20f, (narrow as Outline.Rounded).roundRect.topLeftCornerRadius.x)
        assertEquals(48f, (square as Outline.Rounded).roundRect.topLeftCornerRadius.x)
    }
}
