package top.ltfan.m3x.material

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults as MaterialButtonDefaults
import androidx.compose.material3.FilterChipDefaults as MaterialFilterChipDefaults
import androidx.compose.material3.IconButtonDefaults as MaterialIconButtonDefaults
import androidx.compose.material3.InputChipDefaults as MaterialInputChipDefaults
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.ToggleButtonDefaults as MaterialToggleButtonDefaults
import androidx.compose.material3.VerticalDragHandleDefaults as MaterialVerticalDragHandleDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.theme.LocalM3xThemeExtensions
import top.ltfan.m3x.material.theme.M3xShapeExtensions
import top.ltfan.m3x.material.theme.M3xThemeExtensions

class ThemeShapeDefaultsTest {
    @Test
    fun resolvesThemeFullAndRetainsUpstreamInteractionShapes() = runComposeUiTest {
        val first = RoundedCornerShape(11.dp)
        val second = RoundedCornerShape(17.dp)
        var full by mutableStateOf<Shape>(first)
        val resolved = mutableListOf<Shape>()
        setContent {
            ProvideM3xMaterialRendering {
                MaterialExpressiveTheme {
                    CompositionLocalProvider(
                        LocalM3xThemeExtensions provides
                            M3xThemeExtensions(shapes = M3xShapeExtensions(full = full)),
                    ) {
                        CheckButtonShapes(full)
                        resolved.clear()
                        val toggle =
                            ToggleButtonDefaults.shapesFor(MaterialButtonDefaults.MinHeight)
                        resolved += toggle.shape
                        assertEquals(
                            MaterialToggleButtonDefaults.shapesFor(MaterialButtonDefaults.MinHeight)
                                .pressedShape,
                            toggle.pressedShape,
                        )
                        assertEquals(
                            MaterialToggleButtonDefaults.shapesFor(MaterialButtonDefaults.MinHeight)
                                .checkedShape,
                            toggle.checkedShape,
                        )
                        val filter = FilterChipDefaults.shapes()
                        resolved += filter.selectedShape
                        assertEquals(MaterialFilterChipDefaults.shapes().shape, filter.shape)
                        assertEquals(
                            MaterialFilterChipDefaults.shapes().pressedShape,
                            filter.pressedShape,
                        )
                        val input = InputChipDefaults.shapes()
                        resolved += input.selectedShape
                        assertEquals(
                            MaterialInputChipDefaults.shapes().pressedShape,
                            input.pressedShape,
                        )
                        val drag = VerticalDragHandleDefaults.shapes()
                        resolved += drag.shape
                        assertEquals(
                            MaterialVerticalDragHandleDefaults.shapes().pressedShape,
                            drag.pressedShape,
                        )
                        assertEquals(
                            MaterialVerticalDragHandleDefaults.shapes().draggedShape,
                            drag.draggedShape,
                        )
                        resolved += SearchBarDefaults.inputFieldShape
                        val split =
                            SplitButtonDefaults.leadingButtonShapesFor(40.dp).shape
                                as RoundedCornerShape
                        assertEquals((full as RoundedCornerShape).topStart, split.topStart)
                        val connected =
                            ButtonGroupDefaults.connectedTrailingButtonShapes().shape
                                as RoundedCornerShape
                        assertEquals((full as RoundedCornerShape).topEnd, connected.topEnd)
                        val explicit = RoundedCornerShape(3.dp)
                        assertSame(explicit, ToggleButtonDefaults.shapes(shape = explicit).shape)
                        assertSame(
                            explicit,
                            FilterChipDefaults.shapes(selectedShape = explicit).selectedShape,
                        )
                        assertSame(
                            explicit,
                            InputChipDefaults.shapes(selectedShape = explicit).selectedShape,
                        )
                        assertSame(
                            explicit,
                            VerticalDragHandleDefaults.shapes(shape = explicit).shape,
                        )
                    }
                }
            }
        }
        waitForIdle()
        assertEquals(5, resolved.size)
        resolved.forEach { assertSame(first, it) }
        full = second
        waitForIdle()
        resolved.forEach { assertSame(second, it) }
    }

    @Composable
    private fun CheckButtonShapes(full: Shape) {
        val normal = RoundedCornerShape(7.dp)
        val pressed = RoundedCornerShape(3.dp)
        val checked = RoundedCornerShape(14.dp)

        val button = ButtonDefaults.shapes()
        assertSame(full, button.shape)
        assertEquals(MaterialButtonDefaults.shapes().pressedShape, button.pressedShape)
        val explicitButton = ButtonDefaults.shapes(normal, pressed)
        assertEquals(normal, explicitButton.shape)
        assertEquals(pressed, explicitButton.pressedShape)

        val icon = IconButtonDefaults.shapes()
        assertSame(full, icon.shape)
        assertEquals(MaterialIconButtonDefaults.shapes().pressedShape, icon.pressedShape)
        val explicitIcon = IconButtonDefaults.shapes(normal, pressed)
        assertEquals(normal, explicitIcon.shape)
        assertEquals(pressed, explicitIcon.pressedShape)

        val toggle = IconButtonDefaults.toggleableShapes()
        val upstreamToggle = MaterialIconButtonDefaults.toggleableShapes()
        assertSame(full, toggle.shape)
        assertEquals(upstreamToggle.pressedShape, toggle.pressedShape)
        assertEquals(upstreamToggle.checkedShape, toggle.checkedShape)
        val explicitToggle = IconButtonDefaults.toggleableShapes(normal, pressed, checked)
        assertEquals(normal, explicitToggle.shape)
        assertEquals(pressed, explicitToggle.pressedShape)
        assertEquals(checked, explicitToggle.checkedShape)
    }
}
