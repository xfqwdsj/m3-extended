package top.ltfan.m3x.material

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults as MaterialButtonDefaults
import androidx.compose.material3.ButtonShapes
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

class ButtonDefaultsTest {
    @Test
    fun usesThemeFullShapeAndPreservesDefaultPressedShape() = runInitializedComposeUiTest {
        val themeFull = RoundedCornerShape(11.dp)
        val nextThemeFull = RoundedCornerShape(15.dp)
        var currentThemeFull by mutableStateOf<Shape>(themeFull)
        var resolved: ButtonShapes? = null
        var explicit: ButtonShapes? = null
        var upstreamPressed: Shape? = null
        var upstreamShape: Shape? = null
        val explicitNormal = RoundedCornerShape(7.dp)
        val explicitPressed = RoundedCornerShape(3.dp)
        setContent {
            ProvideM3xMaterialRendering {
                MaterialExpressiveTheme {
                    CompositionLocalProvider(
                        LocalM3xThemeExtensions provides
                            M3xThemeExtensions(
                                shapes = M3xShapeExtensions(full = currentThemeFull),
                            ),
                    ) {
                        upstreamPressed = MaterialButtonDefaults.shapes().pressedShape
                        upstreamShape = MaterialButtonDefaults.shapes().shape
                        resolved = ButtonDefaults.shapes()
                        explicit =
                            ButtonDefaults.shapes(
                                shape = explicitNormal,
                                pressedShape = explicitPressed,
                            )
                    }
                }
            }
        }
        waitForIdle()
        assertNotEquals(themeFull, upstreamShape)
        assertEquals(themeFull, resolved?.shape)
        assertEquals(upstreamPressed, resolved?.pressedShape)
        assertEquals(explicitNormal, explicit?.shape)
        assertEquals(explicitPressed, explicit?.pressedShape)

        currentThemeFull = nextThemeFull
        waitForIdle()
        assertEquals(nextThemeFull, resolved?.shape)
    }
}
