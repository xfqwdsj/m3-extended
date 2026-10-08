package top.ltfan.m3x.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.theme.LocalM3xThemeExtensions
import top.ltfan.m3x.material.theme.M3xShapeExtensions
import top.ltfan.m3x.material.theme.M3xThemeExtensions

class ThemeShapeSlotIntegrationTest {
    private class RecordedShape : Shape {
        var draws = 0

        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density,
        ): Outline {
            draws++
            return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        }
    }

    @Test
    fun defaultSlotsConsumeTheThemeFullShape() = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        var full by mutableStateOf(RecordedShape())
        var component by mutableIntStateOf(0)
        setContent {
            ProvideM3xMaterialRendering {
                MaterialExpressiveTheme {
                    CompositionLocalProvider(
                        LocalM3xThemeExtensions provides
                            M3xThemeExtensions(shapes = M3xShapeExtensions(full = full)),
                    ) {
                        Box(Modifier.size(400.dp, 480.dp).testTag("scene")) {
                            key(component) {
                                when (component) {
                                    0 -> ToggleButton(false, {}) { Text("Toggle") }
                                    1 -> FilterChip(true, {}, label = { Text("Filter") })
                                    2 -> ElevatedFilterChip(true, {}, label = { Text("Elevated") })
                                    3 -> InputChip(true, {}, label = { Text("Input") })
                                    4 -> Badge { Text("123") }
                                    5 -> VerticalDragHandle()
                                    6 -> HorizontalFloatingToolbar(true) { Text("Toolbar") }
                                    7 -> VerticalFloatingToolbar(true) { Text("Toolbar") }
                                    8 -> ContainedLoadingIndicator(progress = { 0.5f })
                                    9 ->
                                        SearchBarDefaults.InputField(
                                            textFieldState = rememberTextFieldState(),
                                            searchBarState = rememberSearchBarState(),
                                            onSearch = {},
                                        )
                                    10 ->
                                        ButtonGroup(overflowIndicator = {}) {
                                            clickableItem({}, "Button")
                                        }
                                }
                            }
                        }
                    }
                }
            }
        }
        for (index in 0..10) {
            runOnIdle {
                full = RecordedShape()
                component = index
            }
            mainClock.advanceTimeBy(1000)
            repeat(4) {
                mainClock.advanceTimeByFrame()
                onNodeWithTag("scene").captureToImage()
            }
            assertTrue(full.draws > 0, "Component $index must consume the provided full shape")
        }
    }
}
