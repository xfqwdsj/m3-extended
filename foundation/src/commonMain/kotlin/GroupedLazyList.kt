package top.ltfan.m3x.compose.foundation

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyScopeMarker
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.ltfan.dslutilities.DslBuilder
import top.ltfan.dslutilities.DslList

/**
 * The vertically scrolling grouped list that only composes and lays out the currently visible
 * items. The [content] block defines a DSL which allows you to emit groups of items; each group can
 * contain single or multiple items, sticky headers, etc.
 *
 * @param modifier the modifier to apply to this layout.
 * @param state the state object to be used to control or observe the list's state.
 * @param contentPadding a padding around the whole content.
 * @param reverseLayout reverse the direction of scrolling and layout.
 * @param spacing the spacing to be applied between groups.
 * @param flingBehavior logic describing fling behavior.
 * @param userScrollEnabled whether scrolling via user gestures or accessibility actions is allowed.
 * @param overscrollEffect the [OverscrollEffect] used to render overscroll.
 * @param content a block which describes the grouped content.
 */
@Composable
public fun GroupedLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    spacing: Dp = 0.dp,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    content: GroupedLazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        overscrollEffect = overscrollEffect,
    ) {
        val groups =
            buildGroups(
                isVertical = true,
                reverse = reverseLayout,
                spacing = spacing,
                content = content,
            )
        buildGroups(groups, isVertical = true, spacing = spacing)
    }
}

/**
 * The horizontally scrolling grouped list that only composes and lays out the currently visible
 * items. See [GroupedLazyColumn] for the DSL.
 */
@Composable
public fun GroupedLazyRow(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    spacing: Dp = 0.dp,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    content: GroupedLazyListScope.() -> Unit,
) {
    LazyRow(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        overscrollEffect = overscrollEffect,
    ) {
        val groups =
            buildGroups(
                isVertical = false,
                reverse = reverseLayout,
                spacing = spacing,
                content = content,
            )
        buildGroups(groups, isVertical = false, spacing = spacing)
    }
}

/**
 * Scope for building grouped lazy lists (vertical or horizontal).
 *
 * Provides DSL methods to add groups, items, and headers. Each group can contain one or more items,
 * and optional sticky headers.
 */
@LazyScopeMarker
public sealed interface GroupedLazyListScope {
    /** Whether the list is vertical (`true`) or horizontal (`false`). */
    public val isVertical: Boolean

    /** Whether the layout is reversed. */
    public val reverse: Boolean

    /** The spacing between groups. */
    public val spacing: Dp
}

/** Adds a [LazyGroup] to the list. */
public fun GroupedLazyListScope.group(group: LazyGroup) {
    (this as GroupedLazyListSpec).addGroup(group)
}

/** Adds a group using a DSL block. */
public inline fun GroupedLazyListScope.group(crossinline content: LazyListGroupScope.() -> Unit) {
    group(LazyGroup { content() })
}

/** Adds [count] groups, each configured by [content] with its index. */
public inline fun GroupedLazyListScope.groups(
    count: Int,
    crossinline content: LazyListGroupScope.(index: Int) -> Unit,
) {
    for (i in 0 until count) {
        group { content(i) }
    }
}

/** Adds one group per element of [items], configured by [content]. */
public inline fun <T> GroupedLazyListScope.groups(
    items: List<T>,
    crossinline content: LazyListGroupScope.(T) -> Unit,
) {
    groups(count = items.size, content = { index -> content(items[index]) })
}

/** Adds a single item as a group. */
public fun GroupedLazyListScope.item(
    key: Any? = null,
    contentType: Any? = null,
    content: @Composable LazyItemScope.() -> Unit,
) {
    group { item(key, contentType, content) }
}

/** Adds [count] items as a group. */
public inline fun GroupedLazyListScope.items(
    count: Int,
    crossinline key: (index: Int) -> Any? = { null },
    crossinline contentType: (index: Int) -> Any? = { null },
    crossinline itemContent: @Composable LazyItemScope.(index: Int) -> Unit,
) {
    group { items(count, key, contentType, itemContent) }
}

/** Adds one item per element of [items] as a group. */
public inline fun <T> GroupedLazyListScope.items(
    items: List<T>,
    crossinline key: (item: T) -> Any? = { null },
    crossinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable LazyItemScope.(T) -> Unit,
) {
    group { items(items, key, contentType, itemContent) }
}

/** Scope for building a group of lazy list items and sticky headers. */
@LazyScopeMarker public sealed interface LazyListGroupScope

/** Adds a [LazyItem] to the group. */
public fun LazyListGroupScope.item(item: LazyItem) {
    (this as LazyListGroupSpec).addItem(item)
}

/** Adds a single item to the group. */
public fun LazyListGroupScope.item(
    key: Any? = null,
    contentType: Any? = null,
    content: @Composable LazyItemScope.() -> Unit,
) {
    item(LazyItem.Item(key, contentType, content))
}

/** Adds [count] items to the group. */
public inline fun LazyListGroupScope.items(
    count: Int,
    key: (index: Int) -> Any? = { null },
    contentType: (index: Int) -> Any? = { null },
    crossinline content: @Composable LazyItemScope.(index: Int) -> Unit,
) {
    for (i in 0 until count) {
        item(key(i), contentType(i)) {
            content(i)
        }
    }
}

/** Adds one item per element of [items] to the group. */
public inline fun <T> LazyListGroupScope.items(
    items: List<T>,
    key: (item: T) -> Any? = { null },
    contentType: (item: T) -> Any? = { null },
    crossinline content: @Composable LazyItemScope.(T) -> Unit,
) {
    items(
        count = items.size,
        key = { key(items[it]) },
        contentType = { contentType(items[it]) },
        content = { content(items[it]) },
    )
}

/** Adds a sticky header to the group. */
public fun LazyListGroupScope.stickyHeader(
    key: Any? = null,
    contentType: Any? = null,
    content: @Composable LazyItemScope.(globalIndex: Int) -> Unit,
) {
    item(LazyItem.StickyHeader(key, contentType, content))
}

/** An item in a grouped lazy list. */
public sealed class LazyItem {
    public abstract val key: Any?
    public abstract val contentType: Any?

    /** A regular item in the lazy list. */
    public data class Item(
        override val key: Any? = null,
        override val contentType: Any? = null,
        val content: @Composable LazyItemScope.() -> Unit,
    ) : LazyItem()

    /** A sticky header in the lazy list. */
    public data class StickyHeader(
        override val key: Any? = null,
        override val contentType: Any? = null,
        val content: @Composable LazyItemScope.(globalIndex: Int) -> Unit,
    ) : LazyItem()
}

/** A group of lazy list items. */
public interface LazyGroup {
    /**
     * Adds the content of this group to the list.
     *
     * @param index the position of the group in the list, which the content uses for the keys of
     *   the synthetic items it adds: the identity of a group is not stable, because the content of
     *   the list is rebuilt with every composition.
     */
    public fun LazyListGroupScope.content(index: Int)
}

/** Creates a [LazyGroup] from a DSL block. */
public inline fun LazyGroup(
    crossinline content: LazyListGroupScope.(index: Int) -> Unit,
): LazyGroup =
    object : LazyGroup {
        override fun LazyListGroupScope.content(index: Int) = content(index)
    }

@DslBuilder(generateFunction = false, resultName = "GroupedLazyListSnapshot")
internal interface GroupedLazyListSpec : GroupedLazyListScope {
    @DslList(resultName = "groups") fun addGroup(group: LazyGroup)
}

@DslBuilder(generateFunction = false, resultName = "LazyListGroupSnapshot")
internal interface LazyListGroupSpec : LazyListGroupScope {
    @DslList(resultName = "items") fun addItem(item: LazyItem)
}

private fun buildGroups(
    isVertical: Boolean,
    reverse: Boolean,
    spacing: Dp,
    content: GroupedLazyListScope.() -> Unit,
): List<LazyGroup> {
    val builder = GroupedLazyListSpecBuilder(isVertical, reverse, spacing)
    builder.content()
    return builder.build().groups
}

private fun LazyListScope.buildGroups(
    groups: List<LazyGroup>,
    isVertical: Boolean,
    spacing: Dp,
) {
    groups.forEachIndexed { index, group ->
        if (index > 0) {
            // A positional default key cannot collide with a caller-provided key.
            stickyHeader(contentType = "spacer") {
                Spacer(
                    Modifier.run {
                        if (isVertical) {
                            height(spacing)
                        } else {
                            width(spacing)
                        }
                    },
                )
            }
        }

        val builder = LazyListGroupSpecBuilder()
        with(group) { builder.content(index) }
        builder.build().items.forEach { item ->
            when (item) {
                is LazyItem.Item ->
                    item(
                        key = item.key,
                        contentType = item.contentType,
                        content = item.content,
                    )

                is LazyItem.StickyHeader ->
                    stickyHeader(
                        key = item.key,
                        contentType = item.contentType,
                        content = item.content,
                    )
            }
        }
    }
}
