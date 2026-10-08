package top.ltfan.m3x.material

import androidx.compose.ui.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.foundation.LazyItem

class CardLazyGroupDslTest {
    @Test
    fun `latest header is first and item order is preserved`() {
        val builder =
            CardLazyGroupSpecBuilder(
                useStickyHeader = true,
                isVertical = true,
                reverse = false,
            )
        builder.item(key = "first") {}
        builder.header(text = "old", key = "old")
        builder.stickyHeader(key = "sticky", modifier = Modifier) {}
        builder.header(text = "new", key = "new")

        val entries = builder.build().orderedItems().map { it.item }
        assertEquals(listOf("new", "first", "sticky"), entries.map { it.key })
        assertTrue(entries[0] is LazyItem.Item)
        assertTrue(entries[2] is LazyItem.StickyHeader)
    }
}
