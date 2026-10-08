package top.ltfan.m3x.material

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.ltfan.m3x.compose.foundation.GroupedLazyColumn
import top.ltfan.m3x.compose.foundation.group
import top.ltfan.m3x.compose.foundation.items
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering

class GroupedLazyListScrollTest {
    @Test
    fun stableItemRemainsAtViewportAfterItemsAreInsertedBeforeItsGroup() =
        runInitializedComposeUiTest {
            var firstGroup by mutableStateOf((0 until 24).map { "first-$it" })
            val secondGroup = (0 until 12).map { "second-$it" }
            lateinit var listState: LazyListState
            lateinit var scope: CoroutineScope
            setContent {
                ProvideM3xMaterialRendering {
                    listState = rememberLazyListState()
                    scope = rememberCoroutineScope()
                    GroupedLazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        spacing = 8.dp,
                    ) {
                        group { items(firstGroup, key = { it }) { Text(it) } }
                        group { items(secondGroup, key = { it }) { Text(it) } }
                    }
                }
            }

            val target = "second-4"
            runOnIdle { scope.launch { listState.scrollToItem(24 + 1 + 4) } }
            waitForIdle()
            assertTrue(listState.layoutInfo.visibleItemsInfo.any { it.key == target })
            val anchor =
                listState.layoutInfo.visibleItemsInfo
                    .first {
                        it.index == listState.firstVisibleItemIndex
                    }
                    .key
            assertTrue(
                anchor in secondGroup,
                "Expected an item in the second group to anchor the viewport, got $anchor",
            )
            val anchorIndex = listState.firstVisibleItemIndex
            val anchorOffset = listState.firstVisibleItemScrollOffset

            runOnIdle { firstGroup = listOf("inserted") + firstGroup }
            waitForIdle()
            assertEquals(
                anchor,
                listState.layoutInfo.visibleItemsInfo
                    .firstOrNull {
                        it.index == listState.firstVisibleItemIndex
                    }
                    ?.key,
            )
            assertEquals(anchorIndex + 1, listState.firstVisibleItemIndex)
            assertEquals(anchorOffset, listState.firstVisibleItemScrollOffset)
        }
}
