package top.ltfan.m3x.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyScopeMarker
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.ltfan.m3x.compose.foundation.GroupedLazyListScope
import top.ltfan.m3x.compose.foundation.LazyItem
import top.ltfan.m3x.compose.foundation.group
import top.ltfan.m3x.compose.foundation.item

/** Registers segmented rows and an optional section heading. */
@LazyScopeMarker
public class SegmentedListGroupScope internal constructor() {
    internal var headingItem: LazyItem? = null
        private set

    internal val rows = mutableListOf<SegmentedListRow>()

    /**
     * Sets the section heading. [sticky] pins it until the next group boundary. The last call
     * supplies the heading, which is laid out independently of the segmented rows. The content
     * inherits Material's on-surface variant color and title-medium typography.
     */
    public fun header(
        key: Any? = null,
        contentType: Any? = null,
        sticky: Boolean = false,
        modifier: @Composable LazyItemScope.() -> Modifier = { Modifier },
        content: @Composable LazyItemScope.() -> Unit,
    ) {
        val heading: @Composable LazyItemScope.() -> Unit = {
            val backing =
                if (sticky) Modifier.background(MaterialTheme.colorScheme.surface) else Modifier
            val padding = ListItemDefaults.ContentPadding
            val direction = LocalLayoutDirection.current
            Box(
                modifier()
                    .fillMaxWidth()
                    .then(backing)
                    .padding(
                        start = padding.calculateStartPadding(direction),
                        top = 8.dp,
                        end = padding.calculateEndPadding(direction),
                        bottom = 8.dp,
                    )
                    .semantics(mergeDescendants = true) { heading() },
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                    LocalTextStyle provides MaterialTheme.typography.titleMedium,
                ) {
                    content()
                }
            }
        }
        headingItem =
            if (sticky) LazyItem.StickyHeader(key, contentType) { heading() }
            else LazyItem.Item(key, contentType, heading)
    }

    /**
     * Registers one lazy row with its [key] and [contentType]. Apply the modifier supplied to
     * [content] outside the row surface to place it at the group's spacing. Use the supplied shapes
     * with a Material segmented list item.
     */
    public fun item(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable LazyItemScope.(Modifier, ListItemShapes) -> Unit,
    ) {
        rows.add(SegmentedListRow(key, contentType, content))
    }

    /** Registers one lazy row per element of [items]. */
    public fun <T> items(
        items: List<T>,
        key: (T) -> Any? = { null },
        contentType: (T) -> Any? = { null },
        content: @Composable LazyItemScope.(T, Modifier, ListItemShapes) -> Unit,
    ) {
        items.forEach { value ->
            item(key(value), contentType(value)) { modifier, shapes ->
                content(value, modifier, shapes)
            }
        }
    }
}

internal class SegmentedListRow(
    val key: Any?,
    val contentType: Any?,
    val content: @Composable LazyItemScope.(Modifier, ListItemShapes) -> Unit,
)

/**
 * Adds a Material segmented section to a vertical grouped lazy list. [itemSpacing] applies between
 * rows; the outer list sets spacing between groups.
 */
public fun GroupedLazyListScope.segmentedListGroup(
    itemSpacing: Dp = ListItemDefaults.SegmentedGap,
    content: SegmentedListGroupScope.() -> Unit,
) {
    require(isVertical) { "Segmented list groups require a GroupedLazyColumn" }
    val reverse = reverse
    val section = SegmentedListGroupScope().apply(content)
    if (section.headingItem == null && section.rows.isEmpty()) return
    group {
        section.headingItem?.let { item(it) }
        section.rows.forEachIndexed { index, row ->
            val shapesIndex = if (reverse) section.rows.lastIndex - index else index
            val spacing =
                when {
                    index == 0 || itemSpacing == 0.dp -> Modifier
                    reverse -> Modifier.padding(bottom = itemSpacing)
                    else -> Modifier.padding(top = itemSpacing)
                }
            item(row.key, row.contentType) {
                row.content(
                    this,
                    spacing,
                    ListItemDefaults.segmentedShapes(shapesIndex, section.rows.size),
                )
            }
        }
    }
}
