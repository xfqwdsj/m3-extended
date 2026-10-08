package top.ltfan.m3x.material

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.theme.LocalM3xThemeExtensions
import top.ltfan.m3x.material.theme.M3xShapeExtensions
import top.ltfan.m3x.material.theme.M3xThemeExtensions

class ThemeShapeRenderingTest {
    private class RecordedShape : Shape {
        val sizes = mutableListOf<Size>()

        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density,
        ): Outline {
            sizes += size
            return Outline.Rectangle(Rect(size = size, offset = Offset.Zero))
        }
    }

    @Test
    fun switchRetainsToggleSemanticsAndUsesBothThemeShapes() = runInitializedComposeUiTest {
        val track = RecordedShape()
        val thumb = RecordedShape()
        var checked by mutableStateOf(false)
        setContent {
            ProvideM3xMaterialRendering {
                MaterialExpressiveTheme {
                    Switch(
                        checked,
                        { checked = it },
                        Modifier.testTag("switch"),
                        trackShape = track,
                        thumbShape = thumb,
                    )
                }
            }
        }
        onNodeWithTag("switch").captureToImage()
        onNodeWithTag("switch").assertHasClickAction().assertIsOff().performClick()
        mainClock.advanceTimeBy(1000)
        onNodeWithTag("switch").captureToImage()
        onNodeWithTag("switch").assertIsOn()
        assertTrue(track.sizes.any { it.width > it.height })
        assertTrue(thumb.sizes.any { it.width == it.height })
    }

    @Test
    fun defaultSwitchAndBadgeRenderTheProvidedFullShape() = runInitializedComposeUiTest {
        val full = RecordedShape()
        setContent {
            ProvideM3xMaterialRendering {
                MaterialExpressiveTheme {
                    CompositionLocalProvider(
                        LocalM3xThemeExtensions provides
                            M3xThemeExtensions(shapes = M3xShapeExtensions(full = full)),
                    ) {
                        Column(Modifier.testTag("scene")) {
                            Switch(true, null)
                            Badge { Text("123") }
                        }
                    }
                }
            }
        }
        onNodeWithTag("scene").captureToImage()
        assertTrue(full.sizes.any { it.width == 52f && it.height == 32f })
        assertTrue(full.sizes.any { it.width == 24f && it.height == 24f })
        assertTrue(full.sizes.any { it.width > it.height && it.height == 16f })
    }

    @Test
    fun sliderThumbPreservesPressSizingAndThemeShape() = runInitializedComposeUiTest {
        val full = RecordedShape()
        val source = MutableInteractionSource()
        setContent {
            ProvideM3xMaterialRendering {
                MaterialExpressiveTheme {
                    SliderDefaults.Thumb(
                        source,
                        Modifier.testTag("thumb"),
                        thumbSize = DpSize(8.dp, 44.dp),
                        shape = full,
                    )
                }
            }
        }
        onNodeWithTag("thumb").captureToImage()
        assertTrue(full.sizes.any { it.width == 8f && it.height == 44f })
        val press = PressInteraction.Press(Offset.Zero)
        runOnIdle { assertTrue(source.tryEmit(press)) }
        waitForIdle()
        onNodeWithTag("thumb").captureToImage()
        assertTrue(full.sizes.any { it.width == 4f && it.height == 44f })
        runOnIdle { assertTrue(source.tryEmit(PressInteraction.Cancel(press))) }
        waitForIdle()
        onNodeWithTag("thumb").captureToImage()
        assertEquals(Size(8f, 44f), full.sizes.last())
    }
}
