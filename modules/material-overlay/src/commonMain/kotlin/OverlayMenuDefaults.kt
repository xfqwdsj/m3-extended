package top.ltfan.m3x.material.overlay

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuGroupShapes
import androidx.compose.material3.MenuItemShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.ltfan.m3x.material.theme.extended

/**
 * Default Material menu elevation; the selected surface renderer supplies any additional shadow.
 */
private val MenuShadowElevation = 0.dp

/** Default values used by the overlay menu components. */
public object OverlayMenuDefaults {
    /** The leading item shape, derived from the current Material theme. */
    public val leadingItemShape: Shape
        @Composable get() = itemCornerShape(leading = true)

    /** The trailing item shape, derived from the current Material theme. */
    public val trailingItemShape: Shape
        @Composable get() = itemCornerShape(leading = false)

    private val leadingGroupShape: Shape
        @Composable get() = itemCornerShape(leading = true)

    private val trailingGroupShape: Shape
        @Composable get() = itemCornerShape(leading = false)

    /** The default shadow elevation of a menu. */
    public val ShadowElevation: Dp = MenuShadowElevation

    /**
     * The shape of a menu item at [index] within a group of [count] items: the first and last items
     * are rounded towards the outside of the group, and the items in between stay evenly rounded.
     *
     * @param index the position of the item within its group
     * @param count the number of items in the group
     */
    @Composable
    public fun itemShape(index: Int, count: Int): Shape {
        if (count <= 1) return MaterialTheme.shapes.medium
        return when (index) {
            0 -> leadingItemShape
            count - 1 -> trailingItemShape
            else -> MenuDefaults.middleItemShape
        }
    }

    /** Interaction shapes for an item at [index] within a group of [count] items. */
    @Composable
    public fun itemShapes(index: Int, count: Int): MenuItemShapes =
        MenuDefaults.itemShapes(shape = itemShape(index, count))

    /**
     * The shape of the group at [index] within a menu of [count] groups: the first and last groups
     * are rounded towards the outside of the menu, and the groups in between stay evenly rounded.
     *
     * @param index the position of the group within the menu
     * @param count the number of groups in the menu
     */
    @Composable
    public fun groupShape(index: Int, count: Int): MenuGroupShapes {
        val defaults = MenuDefaults.groupShapes()
        if (count <= 1) {
            val shape = MaterialTheme.shapes.medium
            val inactiveShape = MaterialTheme.shapes.small
            return remember(defaults, shape, inactiveShape) {
                defaults.copy(
                    shape = shape,
                    inactiveShape = inactiveShape,
                )
            }
        }

        val shape =
            when (index) {
                0 -> leadingGroupShape
                count - 1 -> trailingGroupShape
                else -> MenuDefaults.middleGroupShape
            }
        val inactiveShape = MaterialTheme.shapes.small
        return remember(defaults, shape, inactiveShape) {
            defaults.copy(shape = shape, inactiveShape = inactiveShape)
        }
    }

    @Composable
    private fun itemCornerShape(leading: Boolean): Shape {
        val medium = MaterialTheme.shapes.medium
        val small = MaterialTheme.shapes.small
        val corners = MaterialTheme.extended.shapes.factory
        return if (leading) {
            corners.logical(
                medium.topStart,
                medium.topEnd,
                small.bottomEnd,
                small.bottomStart,
            )
        } else {
            corners.logical(
                small.topStart,
                small.topEnd,
                medium.bottomEnd,
                medium.bottomStart,
            )
        }
    }
}
