package top.ltfan.m3x.material.layout

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.ltfan.m3x.compose.foundation.LazyItem

internal fun LazyItem(
    key: Any? = null,
    contentType: Any? = null,
    sticky: Boolean = false,
    content: @Composable LazyItemScope.() -> Unit,
) =
    if (sticky) {
        LazyItem.StickyHeader(key, contentType) { content() }
    } else {
        LazyItem.Item(key, contentType, content)
    }

internal data class LazyItemWithModifier(
    val item: LazyItem,
    val modifier: @Composable LazyItemScope.() -> Modifier,
)

internal fun LazyItem.withModifier(
    modifier: @Composable LazyItemScope.() -> Modifier = { Modifier },
): LazyItemWithModifier =
    LazyItemWithModifier(
        item = this,
        modifier = modifier,
    )
