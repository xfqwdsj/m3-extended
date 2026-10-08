package top.ltfan.m3x.material

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButtonDefaults as MaterialIconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.IconToggleButtonShapes
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.theme.LocalM3xThemeExtensions
import top.ltfan.m3x.material.theme.M3xShapeExtensions
import top.ltfan.m3x.material.theme.M3xThemeExtensions

class IconButtonDefaultsTest {
    @Test
    fun usesThemeFullShapeAndPreservesUpstreamInteractionShapes() = runInitializedComposeUiTest {
        val firstFull = RoundedCornerShape(11.dp)
        val secondFull = RoundedCornerShape(19.dp)
        var themeFull by mutableStateOf<Shape>(firstFull)
        var resolved: IconButtonShapes? = null
        var resolvedToggle: IconToggleButtonShapes? = null
        var upstream: IconButtonShapes? = null
        var upstreamToggle: IconToggleButtonShapes? = null
        var explicit: IconButtonShapes? = null
        var explicitToggle: IconToggleButtonShapes? = null
        val explicitShape = RoundedCornerShape(5.dp)
        val explicitPressed = RoundedCornerShape(8.dp)
        val explicitChecked = RoundedCornerShape(14.dp)
        setContent {
            ProvideM3xMaterialRendering {
                MaterialExpressiveTheme {
                    CompositionLocalProvider(
                        LocalM3xThemeExtensions provides
                            M3xThemeExtensions(shapes = M3xShapeExtensions(full = themeFull)),
                    ) {
                        upstream = MaterialIconButtonDefaults.shapes()
                        upstreamToggle = MaterialIconButtonDefaults.toggleableShapes()
                        resolved = IconButtonDefaults.shapes()
                        resolvedToggle = IconButtonDefaults.toggleableShapes()
                        explicit = IconButtonDefaults.shapes(explicitShape, explicitPressed)
                        explicitToggle =
                            IconButtonDefaults.toggleableShapes(
                                shape = explicitShape,
                                pressedShape = explicitPressed,
                                checkedShape = explicitChecked,
                            )
                    }
                }
            }
        }
        waitForIdle()
        assertNotEquals(firstFull, upstream?.shape)
        assertEquals(firstFull, resolved?.shape)
        assertEquals(upstream?.pressedShape, resolved?.pressedShape)
        assertEquals(firstFull, resolvedToggle?.shape)
        assertEquals(upstreamToggle?.pressedShape, resolvedToggle?.pressedShape)
        assertEquals(upstreamToggle?.checkedShape, resolvedToggle?.checkedShape)
        assertEquals(explicitShape, explicit?.shape)
        assertEquals(explicitPressed, explicit?.pressedShape)
        assertEquals(explicitShape, explicitToggle?.shape)
        assertEquals(explicitPressed, explicitToggle?.pressedShape)
        assertEquals(explicitChecked, explicitToggle?.checkedShape)

        themeFull = secondFull
        waitForIdle()
        assertEquals(secondFull, resolved?.shape)
        assertEquals(secondFull, resolvedToggle?.shape)
        assertEquals(upstream?.pressedShape, resolved?.pressedShape)
        assertEquals(upstreamToggle?.pressedShape, resolvedToggle?.pressedShape)
        assertEquals(upstreamToggle?.checkedShape, resolvedToggle?.checkedShape)
    }
}
