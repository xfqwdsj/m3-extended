package top.ltfan.m3x.material

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.foundation.GroupedLazyColumn

class SegmentedListGroupTest {
    @Test
    fun headingDoesNotAffectRowShapesInReverseLayout() = runComposeUiTest {
        val actual = mutableMapOf<String, ListItemShapes>()
        lateinit var expected: Map<String, ListItemShapes>
        setContent {
            MaterialTheme {
                expected =
                    mapOf(
                        "first" to ListItemDefaults.segmentedShapes(1, 2),
                        "second" to ListItemDefaults.segmentedShapes(0, 2),
                        "single" to ListItemDefaults.segmentedShapes(0, 1),
                    )
                GroupedLazyColumn(Modifier.height(320.dp), reverseLayout = true) {
                    segmentedListGroup {
                        header(key = "heading") { Text("Heading") }
                        items(listOf("first", "second"), key = { it }) { label, modifier, shapes ->
                            SideEffect { actual[label] = shapes }
                            SegmentedListItem(shapes = shapes, modifier = modifier) { Text(label) }
                        }
                    }
                    segmentedListGroup {
                        item(key = "single") { modifier, shapes ->
                            SideEffect { actual["single"] = shapes }
                            SegmentedListItem(shapes = shapes, modifier = modifier) {
                                Text("Single")
                            }
                        }
                    }
                }
            }
        }
        onNodeWithText("Heading").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        runOnIdle { assertEquals(expected, actual) }
    }

    @Test
    fun stickyHeadingUnpinsAtAnUntitledGroup() = runComposeUiTest {
        var visibleKeys: () -> List<Any> = { emptyList() }
        setContent {
            MaterialTheme {
                val state = rememberLazyListState()
                SideEffect { visibleKeys = { state.layoutInfo.visibleItemsInfo.map { it.key } } }
                GroupedLazyColumn(Modifier.height(160.dp).testTag("list"), state = state) {
                    segmentedListGroup {
                        header(key = "heading", sticky = true) { Text("Heading") }
                        items((1..6).toList(), key = { it }) { number, modifier, shapes ->
                            SegmentedListItem(shapes = shapes, modifier = modifier) {
                                Text("$number")
                            }
                        }
                    }
                    segmentedListGroup {
                        items((7..16).toList(), key = { it }) { number, modifier, shapes ->
                            SegmentedListItem(shapes = shapes, modifier = modifier) {
                                Text("$number")
                            }
                        }
                    }
                }
            }
        }
        onNodeWithTag("list").performScrollToIndex(3)
        runOnIdle { assertTrue("heading" in visibleKeys()) }
        onNodeWithTag("list").performScrollToIndex(10)
        runOnIdle { assertFalse("heading" in visibleKeys()) }
    }
}
