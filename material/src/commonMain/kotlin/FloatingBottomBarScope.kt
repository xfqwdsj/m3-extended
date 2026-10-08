package top.ltfan.m3x.material

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.ltfan.dslutilities.DslBuilder
import top.ltfan.dslutilities.DslList

/** Marks the item DSL of [FloatingBottomBar]. */
@DslMarker public annotation class FloatingBottomBarDsl

/** Scope for declaring the tabs of a [FloatingBottomBar]. */
@FloatingBottomBarDsl public sealed interface FloatingBottomBarScope

/**
 * Adds one tab. A null [key] uses its position as the composition key. Selection and clicks are
 * handled by [FloatingBottomBar].
 */
public fun FloatingBottomBarScope.item(
    key: Any? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    (this as FloatingBottomBarSpec).addItem(FloatingBottomBarEntry(key, modifier, content))
}

/** Adds [count] tabs, providing each index to [itemContent]. */
public inline fun FloatingBottomBarScope.items(
    count: Int,
    key: (index: Int) -> Any? = { null },
    modifier: (index: Int) -> Modifier = { Modifier },
    crossinline itemContent: @Composable ColumnScope.(index: Int) -> Unit,
) {
    require(count >= 0) { "The tab count must not be negative" }
    for (index in 0 until count) {
        item(key(index), modifier(index)) { itemContent(index) }
    }
}

/** Adds one tab per element of [items], providing each value to [itemContent]. */
public inline fun <T> FloatingBottomBarScope.items(
    items: List<T>,
    key: (item: T) -> Any? = { null },
    modifier: (item: T) -> Modifier = { Modifier },
    crossinline itemContent: @Composable ColumnScope.(item: T) -> Unit,
) {
    for (value in items) {
        item(key(value), modifier(value)) { itemContent(value) }
    }
}

internal class FloatingBottomBarEntry(
    val key: Any?,
    val modifier: Modifier,
    val content: @Composable ColumnScope.() -> Unit,
)

@DslBuilder(generateFunction = false, resultName = "FloatingBottomBarSnapshot")
internal interface FloatingBottomBarSpec : FloatingBottomBarScope {
    @DslList(resultName = "items") fun addItem(item: FloatingBottomBarEntry)
}

internal fun buildFloatingBottomBarItems(
    content: FloatingBottomBarScope.() -> Unit,
): List<FloatingBottomBarEntry> {
    val builder = FloatingBottomBarSpecBuilder()
    builder.content()
    return builder.build().items
}
