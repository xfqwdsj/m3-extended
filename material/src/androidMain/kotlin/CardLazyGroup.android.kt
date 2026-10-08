package top.ltfan.m3x.material

import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

/**
 * Convenience overload of [CardLazyGroup.header] accepting a string resource.
 *
 * @param textRes string resource for the header text value.
 * @param key a stable and unique key representing the item. Using the same key for multiple items
 *   in the list is not allowed. Type of the key should be savable via Bundle on Android. If null is
 *   passed the position in the list will represent the key. When specified, the key maintains the
 *   scroll position. If you add or remove items before the current visible item, the item with this
 *   key remains first. This can be overridden by calling 'requestScrollToItem' on the
 *   'LazyListState'.
 * @param contentType the type of the content of this item. The item compositions of the same type
 *   could be reused more efficiently. Note that null is a valid type and items of such type will be
 *   considered compatible.
 * @param modifier composable lambda returning a [Modifier] for the header container.
 * @param sticky whether the header should behave as a sticky header.
 */
public fun CardLazyGroup.header(
    @StringRes textRes: Int,
    key: Any? = "card-header-${textRes.hashCode()}",
    contentType: Any? = "card-header",
    modifier: @Composable LazyItemScope.() -> Modifier,
    sticky: Boolean = false,
) {
    header(key, contentType, modifier, sticky) {
        Text(stringResource(textRes))
    }
}

/**
 * Convenience overload of [CardLazyGroup.header] that takes a plain [Modifier].
 *
 * @param textRes string resource for the header text value.
 * @param key a stable and unique key representing the item. Using the same key for multiple items
 *   in the list is not allowed. Type of the key should be savable via Bundle on Android. If null is
 *   passed the position in the list will represent the key. When specified, the key maintains the
 *   scroll position. If you add or remove items before the current visible item, the item with this
 *   key remains first. This can be overridden by calling 'requestScrollToItem' on the
 *   'LazyListState'.
 * @param contentType the type of the content of this item. The item compositions of the same type
 *   could be reused more efficiently. Note that null is a valid type and items of such type will be
 *   considered compatible.
 * @param modifier modifier applied to the header container.
 * @param sticky whether the header should behave as a sticky header.
 */
public fun CardLazyGroup.header(
    @StringRes textRes: Int,
    key: Any? = "card-header-${textRes.hashCode()}",
    contentType: Any? = "card-header",
    modifier: Modifier = Modifier,
    sticky: Boolean = false,
) {
    header(
        textRes = textRes,
        key = key,
        contentType = contentType,
        modifier = { modifier },
        sticky = sticky,
    )
}
