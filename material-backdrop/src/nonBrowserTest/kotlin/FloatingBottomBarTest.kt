package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.FloatingBottomBar
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.item
import top.ltfan.m3x.material.items

class FloatingBottomBarTest {
    @Test
    fun tabsExposeSelectionAndClick() = runInitializedComposeUiTest {
        var selected by mutableIntStateOf(0)
        var clickCount = 0
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    FloatingBottomBar(
                        selectedTabIndex = selected,
                        onTabSelected = {
                            selected = it
                            clickCount++
                        },
                    ) {
                        item { Text("First") }
                        item { Text("Second") }
                    }
                }
            }
        }

        onNodeWithText("First")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsSelected()
            .assertHasClickAction()
            .onParent()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
        onNodeWithText("First").performClick()
        waitForIdle()
        assertEquals(1, clickCount)
        onNodeWithText("Second").performClick()
        waitForIdle()
        assertEquals(1, selected)
        assertEquals(2, clickCount)
        onNodeWithText("Second").assertIsSelected()
    }

    @Test
    fun draggingHighlightToAnotherTabCallsSelectionCallback() = runInitializedComposeUiTest {
        var selected by mutableIntStateOf(0)
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    FloatingBottomBar(
                        selectedTabIndex = selected,
                        onTabSelected = { selected = it },
                    ) {
                        items(listOf("First", "Second"), key = { it }) { Text(it) }
                    }
                }
            }
        }

        onNodeWithText("First").performTouchInput {
            val start = center
            down(start)
            moveTo(start + Offset(40f, 0f), delayMillis = 100)
            moveTo(start + Offset(80f, 0f), delayMillis = 100)
            up()
        }
        waitForIdle()
        assertEquals(1, selected)
    }

    @Test
    fun pressingAnotherTabMovesIndicatorAndCallsSelectionOnce() = runInitializedComposeUiTest {
        var selected by mutableIntStateOf(0)
        var callbacks = 0
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    FloatingBottomBar(
                        selectedTabIndex = selected,
                        onTabSelected = {
                            selected = it
                            callbacks++
                        },
                    ) {
                        items(listOf("First", "Second", "Third")) { Text(it) }
                    }
                }
            }
        }

        onNodeWithText("Second").performTouchInput {
            down(center)
            up()
        }
        waitForIdle()
        assertEquals(1, selected)
        assertEquals(1, callbacks)

        selected = 0
        waitForIdle()
        onNodeWithText("Second").performTouchInput {
            val start = center
            down(start)
            moveTo(start + Offset(80f, 0f), delayMillis = 100)
            up()
        }
        waitForIdle()
        assertEquals(2, selected)
        assertEquals(2, callbacks)

        onNodeWithText("Third").performTouchInput {
            down(center)
            up()
        }
        waitForIdle()
        assertEquals(3, callbacks)
    }

    @Test
    fun draggingFromAnotherTabFollowsRtlOrder() = runInitializedComposeUiTest {
        var selected by mutableIntStateOf(0)
        var callbacks = 0
        setContent {
            ProvideM3xMaterialRendering {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MaterialTheme {
                        FloatingBottomBar(
                            selectedTabIndex = selected,
                            onTabSelected = {
                                selected = it
                                callbacks++
                            },
                        ) {
                            items(listOf("First", "Second", "Third")) { Text(it) }
                        }
                    }
                }
            }
        }

        onNodeWithText("Second").performTouchInput {
            val start = center
            down(start)
            moveTo(start + Offset(-80f, 0f), delayMillis = 100)
            up()
        }
        waitForIdle()
        assertEquals(2, selected)
        assertEquals(1, callbacks)
    }

    @Test
    fun dynamicItemsAndInvalidSelectionDoNotUseStaleCount() = runInitializedComposeUiTest {
        var count by mutableIntStateOf(2)
        var selected by mutableIntStateOf(0)
        var callbacks = 0
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    FloatingBottomBar(
                        selectedTabIndex = selected,
                        onTabSelected = {
                            selected = it
                            callbacks++
                        },
                    ) {
                        items(count) { Text("Tab $it") }
                    }
                }
            }
        }

        count = 3
        waitForIdle()
        onNodeWithText("Tab 2").performClick()
        assertEquals(2, selected)
        assertEquals(1, callbacks)

        count = 1
        waitForIdle()
        onNodeWithText("Tab 0").assertIsNotSelected()
        onNodeWithText("Tab 1").assertDoesNotExist()
        assertEquals(1, callbacks)

        onNodeWithText("Tab 0").performClick()
        assertEquals(0, selected)
        assertEquals(2, callbacks)
        onNodeWithText("Tab 0").assertIsSelected()

        onNodeWithText("Tab 0").performTouchInput {
            val start = center
            down(start)
            moveTo(start + Offset(80f, 0f), delayMillis = 100)
            up()
        }
        waitForIdle()
        assertEquals(2, callbacks)

        count = 0
        waitForIdle()
        onNodeWithText("Tab 0").assertDoesNotExist()
        assertEquals(2, callbacks)
    }

    @Test
    fun panelSwaysUnderTheFingerOnBothDragPaths() = runInitializedComposeUiTest {
        var selected by mutableIntStateOf(4)
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    Box(Modifier.size(500.dp, 200.dp).testTag("scene")) {
                        FloatingBottomBar(
                            selectedTabIndex = selected,
                            onTabSelected = { selected = it },
                            modifier = Modifier.align(Alignment.Center),
                        ) {
                            items(listOf("A", "B", "C", "D", "E")) { Text(it) }
                        }
                    }
                }
            }
        }

        fun leftEdge(): Int {
            val pixels = onNodeWithTag("scene").captureToImage().toPixelMap()
            for (x in 0 until pixels.width) {
                for (y in 0 until pixels.height) {
                    if (pixels[x, y].alpha > 0.4f) return x
                }
            }
            return pixels.width
        }

        waitForIdle()
        val restBeforeSelectedDrag = leftEdge()
        // Drag left from the selected tab: the panel sways under the finger.
        onNodeWithText("E").performTouchInput {
            down(center)
            moveTo(center + Offset(-150f, 0f), delayMillis = 100)
        }
        waitForIdle()
        val duringSelectedDrag = leftEdge()
        onNodeWithText("E").performTouchInput { up() }
        waitForIdle()

        val restBeforeUnselectedDrag = leftEdge()
        // Drag left from an unselected tab: the indicator follows the finger and the panel sways
        // the same amount.
        onNodeWithText("A").performTouchInput {
            down(center)
            moveTo(center + Offset(-150f, 0f), delayMillis = 100)
        }
        waitForIdle()
        val duringUnselectedDrag = leftEdge()
        onNodeWithText("A").performTouchInput { up() }
        waitForIdle()

        val selectedShift = duringSelectedDrag - restBeforeSelectedDrag
        val unselectedShift = duringUnselectedDrag - restBeforeUnselectedDrag
        assertTrue(selectedShift < 0, "the panel moves with a drag from the selected tab")
        assertTrue(unselectedShift < 0, "the panel moves with a drag from an unselected tab")
        assertTrue(
            abs(selectedShift - unselectedShift) <= 1,
            "both drag paths run the same movement, got $selectedShift and $unselectedShift",
        )
    }
}
