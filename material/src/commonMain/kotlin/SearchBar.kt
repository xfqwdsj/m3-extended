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

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.AppBarWithSearch as MaterialAppBarWithSearch
import androidx.compose.material3.AppBarWithSearchColors
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExpandedDockedSearchBar as MaterialExpandedDockedSearchBar
import androidx.compose.material3.ExpandedDockedSearchBarWithGap as MaterialExpandedDockedSearchBarWithGap
import androidx.compose.material3.ExpandedFullScreenContainedSearchBar as MaterialExpandedFullScreenContainedSearchBar
import androidx.compose.material3.ExpandedFullScreenSearchBar as MaterialExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar as MaterialSearchBar
import androidx.compose.material3.SearchBarColors
import androidx.compose.material3.SearchBarDefaults as MaterialSearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import top.ltfan.m3x.material.theme.extended

/**
 * [Material Design search](https://m3.material.io/components/search/overview)
 *
 * A search bar represents a field that allows users to enter a keyword or phrase and get relevant
 * information. It can be used as a way to navigate through an app via search queries.
 *
 * The [SearchBar] component represents a search bar in the collapsed state. It should be used in
 * conjunction with an [ExpandedFullScreenSearchBar] or [ExpandedDockedSearchBar] to display search
 * results when expanded.
 *
 * @param state the state of the search bar. This state should also be passed to the [inputField]
 *   and the expanded search bar.
 * @param inputField the input field of this search bar that allows entering a query, typically a
 *   [MaterialSearchBarDefaults.InputField].
 * @param modifier the [Modifier] to be applied to this search bar when collapsed.
 * @param shape the shape of this search bar when collapsed.
 * @param colors [SearchBarColors] that will be used to resolve the colors used for this search bar
 *   in different states. See [MaterialSearchBarDefaults.colors].
 * @param tonalElevation when [SearchBarColors.containerColor] is [ColorScheme.surface], a
 *   translucent primary color overlay is applied on top of the container. A higher tonal elevation
 *   value will result in a darker color in light theme and lighter color in dark theme. See also:
 *   [Surface].
 * @param shadowElevation the elevation for the shadow below this search bar.
 */
@Composable
public fun SearchBar(
    state: SearchBarState,
    inputField: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.extended.shapes.full,
    colors: SearchBarColors = MaterialSearchBarDefaults.colors(),
    tonalElevation: Dp = MaterialSearchBarDefaults.TonalElevation,
    shadowElevation: Dp = MaterialSearchBarDefaults.ShadowElevation,
): Unit =
    MaterialSearchBar(
        state = state,
        inputField = inputField,
        modifier = modifier,
        shape = shape,
        colors = colors,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
    )

/**
 * [Material Design search](https://m3.material.io/components/search/overview)
 *
 * A search bar represents a field that allows users to enter a keyword or phrase and get relevant
 * information. It can be used as a way to navigate through an app via search queries.
 *
 * An [AppBarWithSearch] is a [SearchBar] with additional handling for top app bar behavior, such as
 * window insets and scrolling. Using an [AppBarWithSearch] as the top bar of a [Scaffold] ensures
 * that the search bar remains at the top of the screen. Like with [SearchBar], [AppBarWithSearch]
 * should be used in conjunction with an [ExpandedFullScreenSearchBar] or [ExpandedDockedSearchBar]
 * to display search results when expanded.
 *
 * @param state the state of the search bar. This state should also be passed to the [inputField]
 *   and the expanded search bar.
 * @param inputField the input field of this search bar that allows entering a query, typically a
 *   [MaterialSearchBarDefaults.InputField].
 * @param modifier the [Modifier] to be applied to this search bar when collapsed.
 * @param navigationIcon the icon displayed at the start of the app bar before the search bar. This
 *   should typically be an [IconButton] or [IconToggleButton].
 * @param actions the icons displayed at the end of the app bar after the search bar. This should
 *   typically be [IconButton]s. The default layout here is a [Row], so icons inside will be placed
 *   horizontally.
 * @param shape the shape of this search bar when collapsed.
 * @param colors [SearchBarColors] that will be used to resolve the colors used for this search bar.
 *   See [MaterialSearchBarDefaults.colors].
 * @param tonalElevation when [SearchBarColors.containerColor] is [ColorScheme.surface], a
 *   translucent primary color overlay is applied on top of the container. A higher tonal elevation
 *   value will result in a darker color in light theme and lighter color in dark theme. See also:
 *   [Surface].
 * @param shadowElevation the elevation for the shadow below this search bar.
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content.
 * @param windowInsets the window insets that the search bar will respect when collapsed.
 * @param scrollBehavior a [SearchBarScrollBehavior] which works in conjunction with a scrolled
 *   content to change the search bar appearance as the content scrolls. If null, the search bar
 *   will not automatically react to scrolling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun AppBarWithSearch(
    state: SearchBarState,
    inputField: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    shape: Shape = MaterialTheme.extended.shapes.full,
    colors: AppBarWithSearchColors = MaterialSearchBarDefaults.appBarWithSearchColors(),
    tonalElevation: Dp = MaterialSearchBarDefaults.TonalElevation,
    shadowElevation: Dp = MaterialSearchBarDefaults.ShadowElevation,
    contentPadding: PaddingValues = MaterialSearchBarDefaults.AppBarContentPadding,
    windowInsets: WindowInsets = MaterialSearchBarDefaults.windowInsets,
    scrollBehavior: SearchBarScrollBehavior? = null,
): Unit =
    MaterialAppBarWithSearch(
        state = state,
        inputField = inputField,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        shape = shape,
        colors = colors,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        contentPadding = contentPadding,
        windowInsets = windowInsets,
        scrollBehavior = scrollBehavior,
    )

/**
 * [ExpandedFullScreenContainedSearchBar] represents a search bar that is currently expanding or in
 * the expanded state, showing search results, preserving the collapsed shape of the [inputField]
 * without divider. This component is displayed in a new full-screen dialog. If this expansion
 * behavior is undesirable, for example on medium or large screens such as tablets,
 * [ExpandedDockedSearchBar] can be used instead.
 *
 * @param state the state of the search bar. This state should also be passed to the [inputField]
 *   and the collapsed search bar.
 * @param inputField the input field of this search bar that allows entering a query, typically a
 *   [MaterialSearchBarDefaults.InputField].
 * @param modifier the [Modifier] to be applied to this expanded search bar.
 * @param collapsedShape the shape of the search bar when it is collapsed. When fully expanded, the
 *   shape will always be [MaterialSearchBarDefaults.fullScreenShape].
 * @param colors [SearchBarColors] that will be used to resolve the colors used for this search bar
 *   in different states. See [MaterialSearchBarDefaults.containedColors].
 * @param tonalElevation when [SearchBarColors.containerColor] is [ColorScheme.surface], a
 *   translucent primary color overlay is applied on top of the container. A higher tonal elevation
 *   value will result in a darker color in light theme and lighter color in dark theme. See also:
 *   [Surface].
 * @param shadowElevation the elevation for the shadow below this search bar.
 * @param windowInsets the window insets that this search bar will respect when expanded.
 * @param properties the platform-specific properties to configure the dialog's behavior. Any
 *   properties which limit the dialog's size (e.g. [DialogProperties.usePlatformDefaultWidth]) are
 *   ignored.
 * @param content the content of this search bar to display search results below the [inputField].
 */
@Composable
public fun ExpandedFullScreenContainedSearchBar(
    state: SearchBarState,
    inputField: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    collapsedShape: Shape = MaterialTheme.extended.shapes.full,
    colors: SearchBarColors = MaterialSearchBarDefaults.containedColors(state = state),
    tonalElevation: Dp = MaterialSearchBarDefaults.TonalElevation,
    shadowElevation: Dp = MaterialSearchBarDefaults.ShadowElevation,
    windowInsets: @Composable () -> WindowInsets = {
        MaterialSearchBarDefaults.fullScreenWindowInsets
    },
    properties: DialogProperties = DialogProperties(),
    content: @Composable ColumnScope.() -> Unit,
): Unit =
    MaterialExpandedFullScreenContainedSearchBar(
        state = state,
        inputField = inputField,
        modifier = modifier,
        collapsedShape = collapsedShape,
        colors = colors,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        windowInsets = windowInsets,
        properties = properties,
        content = content,
    )

/**
 * [ExpandedFullScreenSearchBar] represents a search bar that is currently expanding or in the
 * expanded state, showing search results. This component is displayed in a new full-screen dialog.
 * If this expansion behavior is undesirable, for example on medium or large screens such as
 * tablets, [ExpandedDockedSearchBar] can be used instead.
 *
 * @param state the state of the search bar. This state should also be passed to the [inputField]
 *   and the collapsed search bar.
 * @param inputField the input field of this search bar that allows entering a query, typically a
 *   [MaterialSearchBarDefaults.InputField].
 * @param modifier the [Modifier] to be applied to this expanded search bar.
 * @param collapsedShape the shape of the search bar when it is collapsed. When fully expanded, the
 *   shape will always be [MaterialSearchBarDefaults.fullScreenShape].
 * @param colors [SearchBarColors] that will be used to resolve the colors used for this search bar
 *   in different states. See [MaterialSearchBarDefaults.colors].
 * @param tonalElevation when [SearchBarColors.containerColor] is [ColorScheme.surface], a
 *   translucent primary color overlay is applied on top of the container. A higher tonal elevation
 *   value will result in a darker color in light theme and lighter color in dark theme. See also:
 *   [Surface].
 * @param shadowElevation the elevation for the shadow below this search bar.
 * @param windowInsets the window insets that this search bar will respect when expanded.
 * @param properties the platform-specific properties to configure the dialog's behavior. Any
 *   properties which limit the dialog's size (e.g. [DialogProperties.usePlatformDefaultWidth]) are
 *   ignored.
 * @param content the content of this search bar to display search results below the [inputField].
 */
@Composable
public fun ExpandedFullScreenSearchBar(
    state: SearchBarState,
    inputField: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    collapsedShape: Shape = MaterialTheme.extended.shapes.full,
    colors: SearchBarColors = MaterialSearchBarDefaults.colors(),
    tonalElevation: Dp = MaterialSearchBarDefaults.TonalElevation,
    shadowElevation: Dp = MaterialSearchBarDefaults.ShadowElevation,
    windowInsets: @Composable () -> WindowInsets = {
        MaterialSearchBarDefaults.fullScreenWindowInsets
    },
    properties: DialogProperties = DialogProperties(),
    content: @Composable ColumnScope.() -> Unit,
): Unit =
    MaterialExpandedFullScreenSearchBar(
        state = state,
        inputField = inputField,
        modifier = modifier,
        collapsedShape = collapsedShape,
        colors = colors,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        windowInsets = windowInsets,
        properties = properties,
        content = content,
    )

/**
 * [ExpandedDockedSearchBarWithGap] represents a search bar that is currently expanding or in the
 * expanded state, showing search results. This component is displayed in a popup under (with a gap)
 * the collapsed search bar. It is recommended to use [ExpandedDockedSearchBarWithGap] on medium and
 * large screens such as tablets, and to instead use [ExpandedFullScreenSearchBar] on compact screen
 * such as phones.
 *
 * @param state the state of the search bar. This state should also be passed to the [inputField]
 *   and the collapsed search bar.
 * @param inputField the input field of this search bar that allows entering a query, typically a
 *   [MaterialSearchBarDefaults.InputField].
 * @param modifier the [Modifier] to be applied to this expanded search bar.
 * @param shape the shape of the container wrapping both the [inputField] and [content].
 * @param dropdownShape the shape of the drop-down containing search results.
 * @param dropdownGapSize the size of the gap between the drop-down containing search results and
 *   the search bar.
 * @param dropdownScrimColor [Color] that will be used for the scrim behind the drop-down. Use
 *   [Color.Unspecified] to remove it.
 * @param colors [SearchBarColors] that will be used to resolve the colors used for this search bar
 *   in different states. See [MaterialSearchBarDefaults.colors].
 * @param tonalElevation when [SearchBarColors.containerColor] is [ColorScheme.surface], a
 *   translucent primary color overlay is applied on top of the container. A higher tonal elevation
 *   value will result in a darker color in light theme and lighter color in dark theme. See also:
 *   [Surface].
 * @param shadowElevation the elevation for the shadow below this search bar.
 * @param properties the platform-specific properties to configure the dialog's behavior. Any
 *   properties which limit the dialog's size (e.g. [DialogProperties.usePlatformDefaultWidth]) are
 *   ignored.
 * @param content the content of this search bar to display search results below the [inputField].
 */
@Composable
public fun ExpandedDockedSearchBarWithGap(
    state: SearchBarState,
    inputField: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialSearchBarDefaults.dockedShape,
    dropdownShape: Shape = MaterialSearchBarDefaults.dockedDropdownShape,
    dropdownGapSize: Dp = MaterialSearchBarDefaults.dockedDropdownGapSize,
    dropdownScrimColor: Color = MaterialSearchBarDefaults.dockedDropdownScrimColor,
    colors: SearchBarColors = MaterialSearchBarDefaults.colors(),
    tonalElevation: Dp = MaterialSearchBarDefaults.TonalElevation,
    shadowElevation: Dp = MaterialSearchBarDefaults.ShadowElevation,
    properties: PopupProperties = PopupProperties(focusable = true, clippingEnabled = false),
    content: @Composable ColumnScope.() -> Unit,
): Unit =
    MaterialExpandedDockedSearchBarWithGap(
        state = state,
        inputField = inputField,
        modifier = modifier,
        shape = shape,
        dropdownShape = dropdownShape,
        dropdownGapSize = dropdownGapSize,
        dropdownScrimColor = dropdownScrimColor,
        colors = colors,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        properties = properties,
        content = content,
    )

/**
 * [ExpandedDockedSearchBar] represents a search bar that is currently expanding or in the expanded
 * state, showing search results. This component is displayed in a popup over the collapsed search
 * bar. It is recommended to use [ExpandedDockedSearchBar] on medium and large screens such as
 * tablets, and to instead use [ExpandedFullScreenSearchBar] on compact screen such as phones.
 *
 * @param state the state of the search bar. This state should also be passed to the [inputField]
 *   and the collapsed search bar.
 * @param inputField the input field of this search bar that allows entering a query, typically a
 *   [MaterialSearchBarDefaults.InputField].
 * @param modifier the [Modifier] to be applied to this expanded search bar.
 * @param shape the shape of the container wrapping both the [inputField] and [content].
 * @param colors [SearchBarColors] that will be used to resolve the colors used for this search bar
 *   in different states. See [MaterialSearchBarDefaults.colors].
 * @param tonalElevation when [SearchBarColors.containerColor] is [ColorScheme.surface], a
 *   translucent primary color overlay is applied on top of the container. A higher tonal elevation
 *   value will result in a darker color in light theme and lighter color in dark theme. See also:
 *   [Surface].
 * @param shadowElevation the elevation for the shadow below this search bar.
 * @param properties the platform-specific properties to configure the dialog's behavior. Any
 *   properties which limit the dialog's size (e.g. [DialogProperties.usePlatformDefaultWidth]) are
 *   ignored.
 * @param content the content of this search bar to display search results below the [inputField].
 */
@Composable
public fun ExpandedDockedSearchBar(
    state: SearchBarState,
    inputField: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialSearchBarDefaults.dockedShape,
    colors: SearchBarColors = MaterialSearchBarDefaults.colors(),
    tonalElevation: Dp = MaterialSearchBarDefaults.TonalElevation,
    shadowElevation: Dp = MaterialSearchBarDefaults.ShadowElevation,
    properties: PopupProperties = PopupProperties(focusable = true, clippingEnabled = false),
    content: @Composable ColumnScope.() -> Unit,
): Unit =
    MaterialExpandedDockedSearchBar(
        state = state,
        inputField = inputField,
        modifier = modifier,
        shape = shape,
        colors = colors,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        properties = properties,
        content = content,
    )
