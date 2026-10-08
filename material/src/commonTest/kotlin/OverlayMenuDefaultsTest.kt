package top.ltfan.m3x.material

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Shapes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.M3xMaterialTheme
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.overlay.OverlayMenuDefaults
import top.ltfan.m3x.material.theme.M3xShapeFactory
import top.ltfan.m3x.material.theme.RoundedM3xShapeFactory
import top.ltfan.m3x.material.theme.extended
import top.ltfan.m3x.material.theming.capsule.CapsuleM3xShapeFactory

class OverlayMenuDefaultsTest {
    @Test
    fun itemAndGroupShapesUseCurrentFactoryAndNativeRoleCorners() = runInitializedComposeUiTest {
        val medium = RoundedCornerShape(11.dp, 12.dp, 13.dp, 14.dp)
        val small = RoundedCornerShape(21.dp, 22.dp, 23.dp, 24.dp)
        val native =
            Shapes(
                extraSmall = RoundedCornerShape(1.dp),
                small = small,
                medium = medium,
                large = RoundedCornerShape(3.dp),
                extraLarge = RoundedCornerShape(4.dp),
                largeIncreased = RoundedCornerShape(5.dp),
                extraLargeIncreased = RoundedCornerShape(6.dp),
                extraExtraLarge = RoundedCornerShape(7.dp),
            )
        var selectedFactory by mutableStateOf<M3xShapeFactory>(CapsuleM3xShapeFactory)
        lateinit var leading: CornerBasedShape
        lateinit var trailing: CornerBasedShape
        lateinit var singleton: Any
        lateinit var middle: Any
        lateinit var expectedMiddle: Any
        lateinit var leadingGroup: CornerBasedShape
        lateinit var trailingGroup: CornerBasedShape
        lateinit var middleGroup: Any
        lateinit var expectedMiddleGroup: Any
        lateinit var inactive: CornerBasedShape
        lateinit var factory: M3xShapeFactory

        setContent {
            ProvideM3xMaterialRendering {
                M3xMaterialTheme(shapeFactory = selectedFactory) {
                    MaterialTheme(shapes = native) {
                        factory = MaterialTheme.extended.shapes.factory
                        leading = OverlayMenuDefaults.itemShape(0, 3) as CornerBasedShape
                        middle = OverlayMenuDefaults.itemShape(1, 3)
                        expectedMiddle = MenuDefaults.middleItemShape
                        trailing = OverlayMenuDefaults.itemShape(2, 3) as CornerBasedShape
                        singleton = OverlayMenuDefaults.itemShape(0, 1)
                        val first = OverlayMenuDefaults.groupShape(0, 3)
                        val center = OverlayMenuDefaults.groupShape(1, 3)
                        val last = OverlayMenuDefaults.groupShape(2, 3)
                        leadingGroup = first.shape as CornerBasedShape
                        trailingGroup = last.shape as CornerBasedShape
                        middleGroup = center.shape
                        expectedMiddleGroup = MenuDefaults.middleGroupShape
                        inactive = first.inactiveShape as CornerBasedShape
                    }
                }
            }
        }
        waitForIdle()

        assertSame(CapsuleM3xShapeFactory, factory)
        assertEquals(
            factory.logical(medium.topStart, medium.topEnd, small.bottomEnd, small.bottomStart),
            leading,
        )
        assertEquals(
            factory.logical(small.topStart, small.topEnd, medium.bottomEnd, medium.bottomStart),
            trailing,
        )
        assertEquals(medium.topStart, leading.topStart)
        assertEquals(medium.topEnd, leading.topEnd)
        assertEquals(small.bottomEnd, leading.bottomEnd)
        assertEquals(small.bottomStart, leading.bottomStart)
        assertEquals(small.topStart, trailing.topStart)
        assertEquals(small.topEnd, trailing.topEnd)
        assertEquals(medium.bottomEnd, trailing.bottomEnd)
        assertEquals(medium.bottomStart, trailing.bottomStart)
        assertEquals(native.medium, singleton)
        assertEquals(expectedMiddle, middle)
        assertNotEquals(
            leading.createOutline(Size(120f, 80f), LayoutDirection.Ltr, Density(1f)),
            leading.createOutline(Size(120f, 80f), LayoutDirection.Rtl, Density(1f)),
        )
        assertEquals(leading, leadingGroup)
        assertEquals(trailing, trailingGroup)
        assertEquals(expectedMiddleGroup, middleGroup)
        assertEquals(native.small, inactive)

        runOnIdle { selectedFactory = RoundedM3xShapeFactory }
        waitForIdle()
        assertSame(RoundedM3xShapeFactory, factory)
        assertEquals(
            RoundedM3xShapeFactory.logical(
                medium.topStart,
                medium.topEnd,
                small.bottomEnd,
                small.bottomStart,
            ),
            leading,
        )
    }
}
