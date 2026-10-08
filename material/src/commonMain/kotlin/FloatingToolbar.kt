/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package top.ltfan.m3x.material

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.FloatingToolbarColors
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarHorizontalFabPosition
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.FloatingToolbarVerticalFabPosition
import androidx.compose.material3.HorizontalFloatingToolbar as MaterialHorizontalFloatingToolbar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalFloatingToolbar as MaterialVerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import top.ltfan.m3x.material.theme.extended

/**
 * A horizontal floating toolbar displays navigation and key actions in a [Row]. It can be
 * positioned anywhere on the screen and floats over the rest of the content.
 *
 * Note: This component will stay expanded to maintain the toolbar visibility for users with touch
 * exploration services enabled (e.g., TalkBack). When touch exploration is not enabled, this
 * component can be collapsed or hidden based on its [expanded] state and any provided
 * [scrollBehavior].
 *
 * @param expanded whether the FloatingToolbar is in expanded mode, i.e. showing [leadingContent]
 *   and [trailingContent]. Note that the toolbar will stay expanded in case a touch exploration
 *   service (e.g., TalkBack) is active.
 * @param modifier the [Modifier] to be applied to this FloatingToolbar.
 * @param colors the colors used for this floating toolbar. There are two predefined
 *   [FloatingToolbarColors] at [FloatingToolbarDefaults.standardFloatingToolbarColors] and
 *   [FloatingToolbarDefaults.vibrantFloatingToolbarColors] which you can use or modify.
 * @param contentPadding the padding applied to the content of this FloatingToolbar.
 * @param scrollBehavior a [FloatingToolbarScrollBehavior]. If null, this FloatingToolbar will not
 *   automatically react to scrolling. Note that the toolbar will not react to scrolling in case a
 *   touch exploration service (e.g., TalkBack) is active.
 * @param shape the shape used for this FloatingToolbar.
 * @param leadingContent the leading content of this FloatingToolbar. The default layout here is a
 *   [Row], so content inside will be placed horizontally. Only showing if [expanded] is true.
 * @param trailingContent the trailing content of this FloatingToolbar. The default layout here is a
 *   [Row], so content inside will be placed horizontally. Only showing if [expanded] is true.
 * @param expandedShadowElevation the elevation for the shadow below this floating toolbar when
 *   expanded.
 * @param collapsedShadowElevation the elevation for the shadow below this floating toolbar when
 *   collapsed.
 * @param content the main content of this FloatingToolbar. The default layout here is a [Row], so
 *   content inside will be placed horizontally.
 */
@Composable
public fun HorizontalFloatingToolbar(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    colors: FloatingToolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
    contentPadding: PaddingValues = FloatingToolbarDefaults.ContentPadding,
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    shape: Shape = MaterialTheme.extended.shapes.full,
    leadingContent: @Composable (RowScope.() -> Unit)? = null,
    trailingContent: @Composable (RowScope.() -> Unit)? = null,
    expandedShadowElevation: Dp = FloatingToolbarDefaults.ContainerExpandedElevation,
    collapsedShadowElevation: Dp = FloatingToolbarDefaults.ContainerCollapsedElevation,
    content: @Composable RowScope.() -> Unit,
): Unit =
    MaterialHorizontalFloatingToolbar(
        expanded = expanded,
        modifier = modifier,
        colors = colors,
        contentPadding = contentPadding,
        scrollBehavior = scrollBehavior,
        shape = shape,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        expandedShadowElevation = expandedShadowElevation,
        collapsedShadowElevation = collapsedShadowElevation,
        content = content,
    )

/**
 * A floating toolbar that displays horizontally. The bar features its content within a [Row], and
 * an adjacent floating icon button. It can be positioned anywhere on the screen, floating above
 * other content, and even in a `Scaffold`'s floating action button slot. Its [expanded] flag
 * controls the visibility of the actions with slide animations.
 *
 * Note: This component will stay expanded to maintain the toolbar visibility for users with touch
 * exploration services enabled (e.g., TalkBack). When touch exploration is not enabled, this
 * component can be collapsed or hidden based on its [expanded] state and any provided
 * [scrollBehavior].
 *
 * In case the toolbar is aligned to the right or the left of the screen, you may apply a
 * [FloatingToolbarDefaults.floatingToolbarVerticalNestedScroll] `Modifier` to update the [expanded]
 * state when scrolling occurs.
 *
 * For a toolbar at the top or bottom center, keep it expanded on scroll. Attach a [scrollBehavior]
 * to hide or show the entire component.
 *
 * Note that if your app uses a `Snackbar`, it's best to position the toolbar in a `Scaffold`'s FAB
 * slot. This ensures the `Snackbar` appears above the toolbar, preventing any visual overlap or
 * interference.
 *
 * @param expanded whether the floating toolbar is expanded. In its expanded state, the FAB and the
 *   toolbar content are organized horizontally. Otherwise, only the FAB is visible. Note that the
 *   toolbar will stay expanded in case a touch exploration service (e.g., TalkBack) is active.
 * @param floatingActionButton a floating action button to be displayed by the toolbar. It's
 *   recommended to use a [FloatingToolbarDefaults.VibrantFloatingActionButton] or
 *   [FloatingToolbarDefaults.StandardFloatingActionButton] that is styled to match the [colors].
 *   Note that the provided FAB's size is controlled by the floating toolbar and animates according
 *   to its state. In case a custom FAB is provided, make sure it's set with a [fillMaxSize] to be
 *   sized correctly.
 * @param modifier the [Modifier] to be applied to this floating toolbar.
 * @param colors the colors used for this floating toolbar. There are two predefined
 *   [FloatingToolbarColors] at [FloatingToolbarDefaults.standardFloatingToolbarColors] and
 *   [FloatingToolbarDefaults.vibrantFloatingToolbarColors] which you can use or modify. See also
 *   [floatingActionButton] for more information on the right FAB to use for proper styling.
 * @param contentPadding the padding applied to the content of this floating toolbar.
 * @param scrollBehavior a [FloatingToolbarScrollBehavior]. If provided, this FloatingToolbar will
 *   automatically react to scrolling. If your toolbar is positioned along a center edge of the
 *   screen (like top or bottom center), it's best to use this scroll behavior to make the entire
 *   toolbar scroll off-screen as the user scrolls. This would prevent the FAB from appearing
 *   off-center, which may occur in this case when using the [expanded] flag to simply expand or
 *   collapse the toolbar. Note that the toolbar will not react to scrolling in case a touch
 *   exploration service (e.g., TalkBack) is active.
 * @param shape the shape used for this floating toolbar content.
 * @param floatingActionButtonPosition the position of the floating toolbar's floating action
 *   button. By default, the FAB is placed at the end of the toolbar (i.e. aligned to the right in
 *   left-to-right layout, or to the left in right-to-left layout).
 * @param animationSpec the animation spec to use for this floating toolbar expand and collapse
 *   animation.
 * @param expandedShadowElevation the elevation for the shadow below this floating toolbar when
 *   expanded.
 * @param collapsedShadowElevation the elevation for the shadow below this floating toolbar when
 *   collapsed.
 * @param content the main content of this floating toolbar. The default layout here is a [Row], so
 *   content inside will be placed horizontally.
 */
@Composable
public fun HorizontalFloatingToolbar(
    expanded: Boolean,
    floatingActionButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    colors: FloatingToolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
    contentPadding: PaddingValues = FloatingToolbarDefaults.ContentPadding,
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    shape: Shape = MaterialTheme.extended.shapes.full,
    floatingActionButtonPosition: FloatingToolbarHorizontalFabPosition =
        FloatingToolbarHorizontalFabPosition.End,
    animationSpec: FiniteAnimationSpec<Float> = FloatingToolbarDefaults.animationSpec(),
    expandedShadowElevation: Dp = FloatingToolbarDefaults.ContainerExpandedElevationWithFab,
    collapsedShadowElevation: Dp = FloatingToolbarDefaults.ContainerCollapsedElevationWithFab,
    content: @Composable RowScope.() -> Unit,
): Unit =
    MaterialHorizontalFloatingToolbar(
        expanded = expanded,
        floatingActionButton = floatingActionButton,
        modifier = modifier,
        colors = colors,
        contentPadding = contentPadding,
        scrollBehavior = scrollBehavior,
        shape = shape,
        floatingActionButtonPosition = floatingActionButtonPosition,
        animationSpec = animationSpec,
        expandedShadowElevation = expandedShadowElevation,
        collapsedShadowElevation = collapsedShadowElevation,
        content = content,
    )

/**
 * A vertical floating toolbar displays navigation and key actions in a [Column]. It can be
 * positioned anywhere on the screen and floats over the rest of the content.
 *
 * Note: This component will stay expanded to maintain the toolbar visibility for users with touch
 * exploration services enabled (e.g., TalkBack). When touch exploration is not enabled, this
 * component can be collapsed or hidden based on its [expanded] state and any provided
 * [scrollBehavior].
 *
 * @param expanded whether the FloatingToolbar is in expanded mode, i.e. showing [leadingContent]
 *   and [trailingContent]. Note that the toolbar will stay expanded in case a touch exploration
 *   service (e.g., TalkBack) is active.
 * @param modifier the [Modifier] to be applied to this FloatingToolbar.
 * @param colors the colors used for this floating toolbar. There are two predefined
 *   [FloatingToolbarColors] at [FloatingToolbarDefaults.standardFloatingToolbarColors] and
 *   [FloatingToolbarDefaults.vibrantFloatingToolbarColors] which you can use or modify.
 * @param contentPadding the padding applied to the content of this FloatingToolbar.
 * @param scrollBehavior a [FloatingToolbarScrollBehavior]. If null, this FloatingToolbar will not
 *   automatically react to scrolling. Note that the toolbar will not react to scrolling in case a
 *   touch exploration service (e.g., TalkBack) is active.
 * @param shape the shape used for this FloatingToolbar.
 * @param leadingContent the leading content of this FloatingToolbar. The default layout here is a
 *   [Column], so content inside will be placed vertically. Only showing if [expanded] is true.
 * @param trailingContent the trailing content of this FloatingToolbar. The default layout here is a
 *   [Column], so content inside will be placed vertically. Only showing if [expanded] is true.
 * @param expandedShadowElevation the elevation for the shadow below this floating toolbar when
 *   expanded.
 * @param collapsedShadowElevation the elevation for the shadow below this floating toolbar when
 *   collapsed.
 * @param content the main content of this FloatingToolbar. The default layout here is a [Column],
 *   so content inside will be placed vertically.
 */
@Composable
public fun VerticalFloatingToolbar(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    colors: FloatingToolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
    contentPadding: PaddingValues = FloatingToolbarDefaults.ContentPadding,
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    shape: Shape = MaterialTheme.extended.shapes.full,
    leadingContent: @Composable (ColumnScope.() -> Unit)? = null,
    trailingContent: @Composable (ColumnScope.() -> Unit)? = null,
    expandedShadowElevation: Dp = FloatingToolbarDefaults.ContainerExpandedElevation,
    collapsedShadowElevation: Dp = FloatingToolbarDefaults.ContainerCollapsedElevation,
    content: @Composable ColumnScope.() -> Unit,
): Unit =
    MaterialVerticalFloatingToolbar(
        expanded = expanded,
        modifier = modifier,
        colors = colors,
        contentPadding = contentPadding,
        scrollBehavior = scrollBehavior,
        shape = shape,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        expandedShadowElevation = expandedShadowElevation,
        collapsedShadowElevation = collapsedShadowElevation,
        content = content,
    )

/**
 * A floating toolbar that displays vertically. The bar features its content within a [Column], and
 * an adjacent floating icon button. It can be positioned anywhere on the screen, floating above
 * other content, and its [expanded] flag controls the visibility of the actions with slide
 * animations.
 *
 * Note: This component will stay expanded to maintain the toolbar visibility for users with touch
 * exploration services enabled (e.g., TalkBack). When touch exploration is not enabled, this
 * component can be collapsed or hidden based on its [expanded] state and any provided
 * [scrollBehavior].
 *
 * In case the toolbar is aligned to the top or the bottom of the screen, you may apply a
 * [FloatingToolbarDefaults.floatingToolbarVerticalNestedScroll] `Modifier` to update the [expanded]
 * state when scrolling occurs.
 *
 * For a toolbar at the left or right center, keep it expanded on scroll. Attach a [scrollBehavior]
 * to hide or show the entire component.
 *
 * @param expanded whether the floating toolbar is expanded. In its expanded state, the FAB and the
 *   toolbar content are organized vertically. Otherwise, only the FAB is visible. Note that the
 *   toolbar will stay expanded in case a touch exploration service (e.g., TalkBack) is active.
 * @param floatingActionButton a floating action button to be displayed by the toolbar. It's
 *   recommended to use a [FloatingToolbarDefaults.VibrantFloatingActionButton] or
 *   [FloatingToolbarDefaults.StandardFloatingActionButton] that is styled to match the [colors].
 *   Note that the provided FAB's size is controlled by the floating toolbar and animates according
 *   to its state. In case a custom FAB is provided, make sure it's set with a [fillMaxSize] to be
 *   sized correctly.
 * @param modifier the [Modifier] to be applied to this floating toolbar.
 * @param colors the colors used for this floating toolbar. There are two predefined
 *   [FloatingToolbarColors] at [FloatingToolbarDefaults.standardFloatingToolbarColors] and
 *   [FloatingToolbarDefaults.vibrantFloatingToolbarColors] which you can use or modify. See also
 *   [floatingActionButton] for more information on the right FAB to use for proper styling.
 * @param contentPadding the padding applied to the content of this floating toolbar.
 * @param scrollBehavior a [FloatingToolbarScrollBehavior]. If provided, this FloatingToolbar will
 *   automatically react to scrolling. If your toolbar is positioned along a center edge of the
 *   screen (like left or right center), it's best to use this scroll behavior to make the entire
 *   toolbar scroll off-screen as the user scrolls. This would prevent the FAB from appearing
 *   off-center, which may occur in this case when using the [expanded] flag to simply expand or
 *   collapse the toolbar. Note that the toolbar will not react to scrolling in case a touch
 *   exploration service (e.g., TalkBack) is active.
 * @param shape the shape used for this floating toolbar content.
 * @param floatingActionButtonPosition the position of the floating toolbar's floating action
 *   button. By default, the FAB is placed at the bottom of the toolbar (i.e. aligned to the
 *   bottom).
 * @param animationSpec the animation spec to use for this floating toolbar expand and collapse
 *   animation.
 * @param expandedShadowElevation the elevation for the shadow below this floating toolbar when
 *   expanded.
 * @param collapsedShadowElevation the elevation for the shadow below this floating toolbar when
 *   collapsed.
 * @param content the main content of this floating toolbar. The default layout here is a [Column],
 *   so content inside will be placed vertically.
 */
@Composable
public fun VerticalFloatingToolbar(
    expanded: Boolean,
    floatingActionButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    colors: FloatingToolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
    contentPadding: PaddingValues = FloatingToolbarDefaults.ContentPadding,
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    shape: Shape = MaterialTheme.extended.shapes.full,
    floatingActionButtonPosition: FloatingToolbarVerticalFabPosition =
        FloatingToolbarVerticalFabPosition.Bottom,
    animationSpec: FiniteAnimationSpec<Float> = FloatingToolbarDefaults.animationSpec(),
    expandedShadowElevation: Dp = FloatingToolbarDefaults.ContainerExpandedElevationWithFab,
    collapsedShadowElevation: Dp = FloatingToolbarDefaults.ContainerCollapsedElevationWithFab,
    content: @Composable ColumnScope.() -> Unit,
): Unit =
    MaterialVerticalFloatingToolbar(
        expanded = expanded,
        floatingActionButton = floatingActionButton,
        modifier = modifier,
        colors = colors,
        contentPadding = contentPadding,
        scrollBehavior = scrollBehavior,
        shape = shape,
        floatingActionButtonPosition = floatingActionButtonPosition,
        animationSpec = animationSpec,
        expandedShadowElevation = expandedShadowElevation,
        collapsedShadowElevation = collapsedShadowElevation,
        content = content,
    )
