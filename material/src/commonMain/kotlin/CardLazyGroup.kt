package top.ltfan.m3x.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyScopeMarker
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import top.ltfan.dslutilities.DslBuilder
import top.ltfan.dslutilities.DslList
import top.ltfan.dslutilities.DslValue
import top.ltfan.m3x.compose.foundation.GroupedLazyListScope
import top.ltfan.m3x.compose.foundation.LazyGroup
import top.ltfan.m3x.compose.foundation.LazyItem
import top.ltfan.m3x.compose.foundation.LazyListGroupScope
import top.ltfan.m3x.compose.foundation.group
import top.ltfan.m3x.compose.foundation.item
import top.ltfan.m3x.compose.foundation.stickyHeader
import top.ltfan.m3x.compose.material3.CardColorsLowest
import top.ltfan.m3x.material.layout.LazyItem as MaterialLazyItem
import top.ltfan.m3x.material.layout.LazyItemWithModifier
import top.ltfan.m3x.material.layout.withModifier

/**
 * A grouped lazy list section that renders its items inside Material3 [Card] containers.
 * Corner-based card shapes have their inner corners trimmed so that a consecutive sequence appears
 * as a single grouped surface. The optional regular or sticky header occupies the first slot; the
 * most recently configured header is used.
 *
 * @property useStickyHeader whether a synthetic empty sticky header should be inserted to reset
 *   sticky state when this group does not use its own sticky header.
 * @property isVertical whether the parent lazy list scrolls vertically (otherwise horizontally).
 * @property reverse whether the parent lazy list is laid out in reverse scrolling direction.
 */
@LazyScopeMarker
public sealed interface CardLazyGroup {
    /** @see CardLazyGroup */
    public val useStickyHeader: Boolean

    /** @see CardLazyGroup */
    public val isVertical: Boolean

    /** @see CardLazyGroup */
    public val reverse: Boolean
}

@DslBuilder(generateFunction = false, resultName = "CardLazyGroupSnapshot")
internal interface CardLazyGroupSpec : CardLazyGroup {
    @DslList(resultName = "items") fun addItem(item: LazyItemWithModifier)

    @DslValue(resultName = "header") fun setHeader(item: LazyItemWithModifier?)
}

private fun CardLazyGroup.recordItem(item: LazyItemWithModifier) {
    (this as CardLazyGroupSpec).addItem(item)
}

private fun CardLazyGroup.recordHeader(item: LazyItemWithModifier) {
    (this as CardLazyGroupSpec).setHeader(item)
}

internal fun CardLazyGroupSnapshot.orderedItems(): List<LazyItemWithModifier> = buildList {
    header?.let(::add)
    addAll(items)
}

/**
 * Position of a card within a group with more than one item. [Start] and [End] follow the current
 * layout direction in horizontal lists.
 */
public enum class CardGroupShapePosition {
    /** The top card of a vertical group. */
    Top,

    /** The bottom card of a vertical group. */
    Bottom,

    /** The card at the logical start of a horizontal group. */
    Start,

    /** The card at the logical end of a horizontal group. */
    End,

    /** A card between the outer cards. */
    Middle,
}

/**
 * Supplies a grouped shape when the base shape is not a [CornerBasedShape]. Single-card groups use
 * the base shape directly. A middle card calls this provider only when one is supplied. Without a
 * provider, outer cards use their base shape and middle cards use [RectangleShape].
 */
public typealias CardGroupShapeFallback =
    (baseShape: Shape, position: CardGroupShapePosition) -> Shape

/**
 * Adds a regular item wrapped in a card using a composable [Modifier] lambda, allowing the modifier
 * to depend on composition state.
 *
 * @param key a stable and unique key representing the item. Using the same key for multiple items
 *   in the list is not allowed. Type of the key should be savable via Bundle on Android. If null is
 *   passed the position in the list will represent the key. When specified, the key maintains the
 *   scroll position. If you add or remove items before the current visible item, the item with this
 *   key remains first. This can be overridden by calling 'requestScrollToItem' on the
 *   'LazyListState'.
 * @param contentType the type of the content of this item. The item compositions of the same type
 *   could be reused more efficiently. Note that null is a valid type and items of such type will be
 *   considered compatible.
 * @param modifier composable lambda returning a [Modifier] applied to the wrapping [Card].
 * @param content composable content of the card body.
 */
public fun CardLazyGroup.item(
    key: Any? = null,
    contentType: Any? = null,
    modifier: @Composable LazyItemScope.() -> Modifier,
    content: @Composable LazyItemScope.() -> Unit,
) {
    recordItem(LazyItem.Item(key, contentType, content).withModifier(modifier))
}

/**
 * Adds a regular item wrapped in a card using a plain [Modifier].
 *
 * @param key a stable and unique key representing the item. Using the same key for multiple items
 *   in the list is not allowed. Type of the key should be savable via Bundle on Android. If null is
 *   passed the position in the list will represent the key. When specified, the key maintains the
 *   scroll position. If you add or remove items before the current visible item, the item with this
 *   key remains first. This can be overridden by calling 'requestScrollToItem' on the
 *   'LazyListState'.
 * @param contentType the type of the content of this item. The item compositions of the same type
 *   could be reused more efficiently. Note that null is a valid type and items of such type will be
 *   considered compatible.
 * @param modifier modifier applied to the wrapping [Card].
 * @param content composable content of the card body.
 */
public fun CardLazyGroup.item(
    key: Any? = null,
    contentType: Any? = null,
    modifier: Modifier = Modifier,
    content: @Composable LazyItemScope.() -> Unit,
) {
    item(key, contentType, { modifier }, content)
}

/**
 * Configures the header at the start of the group. The most recent header call supplies this slot.
 * Supports sticky behavior and rotation for horizontal lists.
 *
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
 * @param text composable producing the header text/content.
 */
public fun CardLazyGroup.header(
    key: Any? = null,
    contentType: Any? = null,
    modifier: @Composable LazyItemScope.() -> Modifier = { Modifier },
    sticky: Boolean = false,
    text: @Composable () -> Unit,
) {
    val group = this
    recordHeader(
        MaterialLazyItem(
                key = key,
                contentType = CardHeaderContent(contentType ?: "card-header"),
                sticky = sticky,
            ) {
                val modifier =
                    if (group.isVertical) {
                            Modifier.fillMaxWidth()
                        } else {
                            Modifier.fillMaxHeight()
                        }
                        .semantics(mergeDescendants = true) {
                            heading()
                        }

                val content =
                    @Composable {
                        CompositionLocalProvider(
                            LocalTextStyle provides
                                MaterialTheme.typography.titleMedium.merge(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                ),
                        ) {
                            fun Modifier.size(size: Dp) =
                                if (group.isVertical) {
                                    height(size)
                                } else {
                                    width(size)
                                }

                            fun Modifier.padding(padding: Dp) =
                                if (group.isVertical) {
                                    padding(horizontal = padding)
                                } else {
                                    padding(vertical = padding)
                                }

                            Spacer(Modifier.size(16.dp))
                            Box(
                                Modifier.padding(16.dp).run {
                                    if (group.isVertical) return@run this

                                    layout { measurable, constraints ->
                                        val factor = if (!group.reverse) 1f else -1f
                                        val degrees =
                                            when (layoutDirection) {
                                                LayoutDirection.Ltr -> -90f * factor
                                                LayoutDirection.Rtl -> 90f * factor
                                            }

                                        val placeable =
                                            measurable.measure(
                                                Constraints(
                                                    minWidth = constraints.minHeight,
                                                    maxWidth = constraints.maxHeight,
                                                    minHeight = constraints.minWidth,
                                                    maxHeight = constraints.maxWidth,
                                                ),
                                            )

                                        val width = placeable.width
                                        val height = placeable.height

                                        layout(width = height, height = width) {
                                            val x = if (degrees == 90f) height else 0
                                            val y = if (degrees == -90f) width else 0
                                            placeable.placeWithLayer(x, y) {
                                                rotationZ = degrees
                                                transformOrigin = TransformOrigin(0f, 0f)
                                            }
                                        }
                                    }
                                },
                            ) {
                                text()
                            }
                            Spacer(Modifier.size(12.dp))
                        }
                    }

                if (group.isVertical) {
                    Column(modifier) { content() }
                } else {
                    Row(modifier) { content() }
                }
            }
            .withModifier(modifier),
    )
}

/**
 * Convenience overload of [header] accepting a string resource.
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
    textRes: StringResource,
    key: Any? = null,
    contentType: Any? = "card-header",
    modifier: @Composable LazyItemScope.() -> Modifier,
    sticky: Boolean = false,
) {
    header(key, contentType, modifier, sticky) {
        Text(stringResource(textRes))
    }
}

/**
 * Convenience overload of [header] that takes a plain [Modifier].
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
    textRes: StringResource,
    key: Any? = null,
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

/**
 * Convenience overload of [header] accepting a plain string.
 *
 * @param text header text value.
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
    text: String,
    key: Any? = null,
    contentType: Any? = "card-header",
    modifier: @Composable LazyItemScope.() -> Modifier,
    sticky: Boolean = false,
) {
    header(key, contentType, modifier, sticky) {
        Text(text)
    }
}

/**
 * Convenience overload of [header] that takes a plain [Modifier].
 *
 * @param text header text value.
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
    text: String,
    key: Any? = null,
    contentType: Any? = "card-header",
    modifier: Modifier = Modifier,
    sticky: Boolean = false,
) {
    header(
        text = text,
        key = key,
        contentType = contentType,
        modifier = { modifier },
        sticky = sticky,
    )
}

/**
 * Adds multiple indexed items with per-index keys, content types, modifiers and content.
 *
 * @param count number of items to add.
 * @param key function returning a key for each index.
 * @param contentType function returning a content type for each index.
 * @param modifier function returning a modifier for the item card at index.
 * @param content composable lambda producing the item content at index.
 */
public inline fun CardLazyGroup.items(
    count: Int,
    key: (index: Int) -> Any? = { null },
    contentType: (index: Int) -> Any? = { null },
    crossinline modifier: @Composable LazyItemScope.(index: Int) -> Modifier = { Modifier },
    crossinline content: @Composable LazyItemScope.(index: Int) -> Unit,
) {
    for (index in 0 until count) {
        item(key(index), contentType(index), { modifier(index) }) {
            content(index)
        }
    }
}

/**
 * Adds multiple items from a list with per-item keys, content types, modifiers and content.
 *
 * @param items source list.
 * @param key function returning a key for each item.
 * @param contentType function returning a content type for each item.
 * @param modifier function returning a modifier for each item.
 * @param content composable lambda producing the item content.
 */
public inline fun <T> CardLazyGroup.items(
    items: List<T>,
    key: (item: T) -> Any? = { null },
    contentType: (item: T) -> Any? = { null },
    crossinline modifier: @Composable LazyItemScope.(item: T) -> Modifier = { Modifier },
    crossinline content: @Composable LazyItemScope.(T) -> Unit,
) {
    items(
        count = items.size,
        key = { index -> key(items[index]) },
        contentType = { index -> contentType(items[index]) },
        modifier = { index -> modifier(items[index]) },
        content = { index -> content(items[index]) },
    )
}

/**
 * Adds a sticky header with access to the global index inside the grouped lazy list.
 *
 * @param key a stable and unique key representing the item. Using the same key for multiple items
 *   in the list is not allowed. Type of the key should be savable via Bundle on Android. If null is
 *   passed the position in the list will represent the key. When specified, the key maintains the
 *   scroll position. If you add or remove items before the current visible item, the item with this
 *   key remains first. This can be overridden by calling 'requestScrollToItem' on the
 *   'LazyListState'.
 * @param contentType the type of the content of this item. The item compositions of the same type
 *   could be reused more efficiently. Note that null is a valid type and items of such type will be
 *   considered compatible.
 * @param modifier composable lambda returning a [Modifier] for the header card.
 * @param content composable lambda receiving the global index parameter.
 */
public fun CardLazyGroup.stickyHeader(
    key: Any? = null,
    contentType: Any? = null,
    modifier: @Composable LazyItemScope.() -> Modifier = { Modifier },
    content: @Composable LazyItemScope.(globalIndex: Int) -> Unit,
) {
    recordItem(LazyItem.StickyHeader(key, contentType, content).withModifier(modifier))
}

/**
 * Overload of [stickyHeader] accepting a plain [Modifier].
 *
 * @param key a stable and unique key representing the item. Using the same key for multiple items
 *   in the list is not allowed. Type of the key should be savable via Bundle on Android. If null is
 *   passed the position in the list will represent the key. When specified, the key maintains the
 *   scroll position. If you add or remove items before the current visible item, the item with this
 *   key remains first. This can be overridden by calling 'requestScrollToItem' on the
 *   'LazyListState'.
 * @param contentType the type of the content of this item. The item compositions of the same type
 *   could be reused more efficiently. Note that null is a valid type and items of such type will be
 *   considered compatible.
 * @param modifier modifier applied to the header card.
 * @param content composable lambda receiving the global index parameter.
 */
public fun CardLazyGroup.stickyHeader(
    key: Any? = null,
    contentType: Any? = null,
    modifier: Modifier = Modifier,
    content: @Composable LazyItemScope.(globalIndex: Int) -> Unit,
) {
    stickyHeader(key, contentType, { modifier }, content)
}

/**
 * Runtime [LazyGroup] that renders the configured [CardLazyGroup] cards. Corner-based shapes have
 * their inner corners trimmed; other shapes use the optional fallback at each grouped position.
 */
private class CardGroup(
    private val useStickyHeader: Boolean,
    private val isVertical: Boolean,
    private val reverse: Boolean,
    private val shape: @Composable () -> Shape,
    private val shapeFallback: CardGroupShapeFallback?,
    private val colors: @Composable () -> CardColors,
    private val elevation: @Composable () -> CardElevation,
    private val border: @Composable () -> BorderStroke?,
    private val backdropMaterial: @Composable () -> MaterialSurfaceStyle?,
    private val items: List<LazyItemWithModifier>,
) : LazyGroup {
    /** Builds the grouped lazy list content with a shape for each card's position in its group. */
    override fun LazyListGroupScope.content(index: Int) {
        val scope = this@CardGroup
        val entries = this@CardGroup.items
        if (!useStickyHeader) {
            // A positional default key cannot collide with a caller-provided key.
            stickyHeader(contentType = "reset") {}
        }
        entries.forEachIndexed { index, entry ->
            val modifier = entry.modifier
            val getShape =
                @Composable {
                    if (entries.size == 1) scope.shape()
                    else
                        when (index) {
                            0 -> scope.getShape(false)
                            entries.lastIndex -> scope.getShape(true)
                            else ->
                                if (scope.shapeFallback == null) RectangleShape
                                else scope.getMiddleShape()
                        }
                }

            @Composable
            fun LazyItemScope.CardItem(content: @Composable () -> Unit) {
                Card(
                    modifier = modifier(),
                    shape = getShape(),
                    colors = scope.colors(),
                    elevation = scope.elevation(),
                    border = scope.border(),
                    backdropMaterial = scope.backdropMaterial(),
                ) {
                    content()
                }
            }

            when (entry.item) {
                is LazyItem.Item -> {
                    val (key, contentType, content) = entry.item
                    item(key, contentType) { CardItem { content() } }
                }

                is LazyItem.StickyHeader -> {
                    val (key, contentType, content) = entry.item
                    stickyHeader(key, contentType) { CardItem { content(it) } }
                }
            }
        }
    }

    /**
     * Resolves the first or last card's shape. Corner-based shapes preserve their outer corners;
     * other shapes retain their outline or use the supplied fallback.
     *
     * @param footer whether we are computing the shape for the last (footer) element.
     */
    @Composable
    private fun getShape(footer: Boolean): Shape {
        val reverse = if (footer) !reverse else reverse
        val shape = shape()

        if (shape is CornerBasedShape) {
            return when (isVertical) {
                true ->
                    when (reverse) {
                        false ->
                            shape.copy(
                                bottomEnd = ZeroCornerSize,
                                bottomStart = ZeroCornerSize,
                            )

                        true ->
                            shape.copy(
                                topStart = ZeroCornerSize,
                                topEnd = ZeroCornerSize,
                            )
                    }

                false ->
                    when (reverse) {
                        false ->
                            shape.copy(
                                topEnd = ZeroCornerSize,
                                bottomEnd = ZeroCornerSize,
                            )

                        true ->
                            shape.copy(
                                topStart = ZeroCornerSize,
                                bottomStart = ZeroCornerSize,
                            )
                    }
            }
        }

        val position = fallbackCardPosition(isVertical, reverse, footer)
        return resolveFallbackCardShape(shape, position, shapeFallback)
    }

    /** Resolves a middle card only when a fallback was supplied. */
    @Composable
    private fun getMiddleShape(): Shape {
        val shape = shape()
        return if (shape is CornerBasedShape) RectangleShape
        else resolveFallbackCardShape(shape, CardGroupShapePosition.Middle, shapeFallback)
    }
}

internal fun fallbackCardPosition(
    isVertical: Boolean,
    reverse: Boolean,
    footer: Boolean,
): CardGroupShapePosition {
    val trailing = reverse != footer
    return when {
        isVertical && !trailing -> CardGroupShapePosition.Top
        isVertical -> CardGroupShapePosition.Bottom
        !trailing -> CardGroupShapePosition.Start
        else -> CardGroupShapePosition.End
    }
}

internal fun resolveFallbackCardShape(
    baseShape: Shape,
    position: CardGroupShapePosition,
    fallback: CardGroupShapeFallback?,
): Shape = fallback?.invoke(baseShape, position) ?: baseShape

/**
 * Content type wrapper used to distinguish card header items from normal items in the list.
 *
 * @property contentType original user provided content type or default header marker.
 */
private data class CardHeaderContent(val contentType: Any?)

/**
 * Adds a single item wrapped in a card to the receiver [GroupedLazyListScope].
 *
 * @param key a stable and unique key representing the item. Using the same key for multiple items
 *   in the list is not allowed. Type of the key should be savable via Bundle on Android. If null is
 *   passed the position in the list will represent the key. When specified, the key maintains the
 *   scroll position. If you add or remove items before the current visible item, the item with this
 *   key remains first. This can be overridden by calling 'requestScrollToItem' on the
 *   'LazyListState'.
 * @param contentType the type of the content of this item. The item compositions of the same type
 *   could be reused more efficiently. Note that null is a valid type and items of such type will be
 *   considered compatible.
 * @param modifier composable lambda returning a [Modifier] applied to the wrapping [Card].
 * @param shape composable lambda returning the shape applied to the card; inner corners of
 *   corner-based edge cards are trimmed.
 * @param shapeFallback optional shape resolver for grouped cards whose base shape is not a
 *   [CornerBasedShape].
 * @param colors composable lambda providing the [CardColors] for the card.
 * @param elevation composable lambda providing the [CardElevation] for the card.
 * @param backdropMaterial composable lambda providing the optional renderer-specific surface style
 *   for each card.
 * @param border composable lambda providing an optional [BorderStroke] for the card.
 * @param content composable content of the card body.
 */
public fun GroupedLazyListScope.cardItem(
    key: Any? = null,
    contentType: Any? = null,
    modifier: @Composable LazyItemScope.() -> Modifier = { Modifier },
    shape: @Composable () -> Shape = { CardDefaults.shape },
    shapeFallback: CardGroupShapeFallback? = null,
    colors: @Composable () -> CardColors = { CardColorsLowest },
    elevation: @Composable () -> CardElevation = { CardDefaults.cardElevation() },
    border: @Composable () -> BorderStroke? = { null },
    backdropMaterial: @Composable () -> MaterialSurfaceStyle? = {
        LocalMaterialSurfaceRenderer.current.style(MaterialSurfaceRole.Surface)
    },
    content: @Composable LazyItemScope.() -> Unit,
) {
    card(
        shape = shape,
        shapeFallback = shapeFallback,
        colors = colors,
        elevation = elevation,
        border = border,
        backdropMaterial = backdropMaterial,
    ) {
        item(key, contentType, modifier, content)
    }
}

/**
 * Adds a single [CardLazyGroup] to the receiver [GroupedLazyListScope].
 *
 * @param useStickyHeader whether a synthetic empty sticky header should be inserted if this group
 *   has no explicit sticky header.
 * @param shape composable lambda returning the default shape for cards.
 * @param shapeFallback optional shape resolver for grouped cards whose base shape is not a
 *   [CornerBasedShape]. It receives the logical outer side of each edge card or
 *   [CardGroupShapePosition.Middle] for an inner card. Single-card groups use [shape] directly.
 * @param colors composable lambda returning the default [CardColors].
 * @param elevation composable lambda returning the default [CardElevation].
 * @param backdropMaterial composable lambda providing the optional renderer-specific surface style
 *   for each card.
 * @param border composable lambda returning an optional [BorderStroke].
 * @param content DSL block configuring the card group (items, header, etc.).
 */
public fun GroupedLazyListScope.card(
    useStickyHeader: Boolean = false,
    shape: @Composable () -> Shape = { CardDefaults.shape },
    shapeFallback: CardGroupShapeFallback? = null,
    colors: @Composable () -> CardColors = { CardColorsLowest },
    elevation: @Composable () -> CardElevation = { CardDefaults.cardElevation() },
    border: @Composable () -> BorderStroke? = { null },
    backdropMaterial: @Composable () -> MaterialSurfaceStyle? = {
        LocalMaterialSurfaceRenderer.current.style(MaterialSurfaceRole.Surface)
    },
    content: CardLazyGroup.() -> Unit,
) {
    val builder =
        CardLazyGroupSpecBuilder(
            useStickyHeader = useStickyHeader,
            isVertical = isVertical,
            reverse = reverse,
        )
    builder.content()
    val snapshot = builder.build()
    group(
        CardGroup(
            useStickyHeader = snapshot.useStickyHeader,
            isVertical = snapshot.isVertical,
            reverse = snapshot.reverse,
            shape = shape,
            shapeFallback = shapeFallback,
            colors = colors,
            elevation = elevation,
            border = border,
            backdropMaterial = backdropMaterial,
            items = snapshot.orderedItems(),
        ),
    )
}

/**
 * Adds multiple [CardLazyGroup] instances, one for each element of [groups].
 *
 * @param groups source list used to create groups.
 * @param useStickyHeader function deciding whether each group uses a synthetic sticky header.
 * @param shape function returning shape for each group element.
 * @param shapeFallback optional shape resolver for non-[CornerBasedShape] cards. The group element
 *   is supplied as the first argument.
 * @param colors function returning colors for each group element.
 * @param elevation function returning elevation for each group element.
 * @param backdropMaterial function providing the optional renderer-specific surface style for each
 *   group.
 * @param border function returning border for each group element.
 * @param content DSL block configuring each created [CardLazyGroup].
 */
public inline fun <T> GroupedLazyListScope.cards(
    groups: List<T>,
    useStickyHeader: (T) -> Boolean = { false },
    crossinline shape: @Composable (T) -> Shape = { CardDefaults.shape },
    noinline shapeFallback:
        ((group: T, baseShape: Shape, position: CardGroupShapePosition) -> Shape)? =
        null,
    crossinline colors: @Composable (T) -> CardColors = { CardColorsLowest },
    crossinline elevation: @Composable (T) -> CardElevation = { CardDefaults.cardElevation() },
    crossinline border: @Composable (T) -> BorderStroke? = { null },
    crossinline backdropMaterial: @Composable (T) -> MaterialSurfaceStyle? = { null },
    crossinline content: CardLazyGroup.(T) -> Unit,
): Unit = groups.forEach { group ->
    card(
        useStickyHeader = useStickyHeader(group),
        shape = { shape(group) },
        shapeFallback =
            if (shapeFallback == null) null
            else
                { baseShape, position ->
                    shapeFallback(group, baseShape, position)
                },
        colors = { colors(group) },
        elevation = { elevation(group) },
        border = { border(group) },
        backdropMaterial = { backdropMaterial(group) },
    ) {
        content(group)
    }
}
