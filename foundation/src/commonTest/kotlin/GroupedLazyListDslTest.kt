package top.ltfan.m3x.compose.foundation

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class GroupedLazyListDslTest {
    @Test
    fun `groups preserve order and built results are snapshots`() {
        val first = LazyGroup { item(key = "first") {} }
        val builder =
            GroupedLazyListSpecBuilder(
                isVertical = true,
                reverse = false,
                spacing = 8.dp,
            )

        builder.group(first)
        builder.group { item(key = "second") {} }
        val snapshot = builder.build()
        builder.group { item(key = "third") {} }

        assertEquals(2, snapshot.groups.size)
        assertSame(first, snapshot.groups.first())

        val entries = LazyListGroupSpecBuilder()
        with(snapshot.groups[1]) { entries.content(1) }
        assertEquals("second", entries.build().items.single().key)
    }

    @Test
    fun `item and sticky header append in declaration order`() {
        val builder = LazyListGroupSpecBuilder()
        builder.item(key = "regular") {}
        builder.stickyHeader(key = "sticky") {}

        val items = builder.build().items
        assertEquals(listOf("regular", "sticky"), items.map { it.key })
        assertEquals(LazyItem.Item::class, items[0]::class)
        assertEquals(LazyItem.StickyHeader::class, items[1]::class)
    }
}
