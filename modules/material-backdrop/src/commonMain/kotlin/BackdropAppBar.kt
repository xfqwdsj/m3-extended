package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.ltfan.m3x.backdrop.BackdropEdge
import top.ltfan.m3x.backdrop.BackdropEffectScope
import top.ltfan.m3x.backdrop.BackdropFill
import top.ltfan.m3x.backdrop.LocalBackdrop
import top.ltfan.m3x.backdrop.ProgressiveBlurMode
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.backdrop.rememberStackedBackdropLayer
import top.ltfan.m3x.material.backdrop.internal.LocalBackdropAppBarSurface
import top.ltfan.m3x.material.backdrop.internal.M3xSingleRowTopAppBar
import top.ltfan.m3x.material.backdrop.internal.M3xTwoRowsTopAppBar

/** Defaults shared by Backdrop top app bars. */
public object BackdropAppBarDefaults {
    /** Transparent Material colors that leave the Backdrop surface visible. */
    @Composable
    public fun colors(): TopAppBarColors =
        TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
        )

    internal inline fun navigation(
        crossinline navigation: @Composable () -> Unit,
    ): @Composable () -> Unit = {
        CompositionLocalProvider(
            LocalBackdropTopAppBarControlRole provides BackdropTopAppBarControlRole.Navigation,
        ) {
            navigation()
        }
    }

    internal inline fun actions(
        crossinline actions: @Composable RowScope.() -> Unit,
    ): @Composable RowScope.() -> Unit = {
        CompositionLocalProvider(
            LocalBackdropTopAppBarControlRole provides BackdropTopAppBarControlRole.Actions,
        ) {
            actions()
        }
    }
}

/** Adapts Material overlap state; a static container stays fully present. */
@Composable
public fun rememberBackdropAppBarPresence(scrollBehavior: TopAppBarScrollBehavior?): State<Float> =
    rememberScrollEdgePresence {
        scrollBehavior.hasContentUnderBar()
    }

/**
 * Whether the content has moved under the bar.
 *
 * [TopAppBarScrollBehavior] reports this through [TopAppBarState.overlappedFraction]. The bar
 * publishes its height as the offset limit. The fraction tracks a collapsing bar and a pinned bar
 * whose content offset grows as content passes underneath.
 */
public fun TopAppBarScrollBehavior?.hasContentUnderBar(): Boolean {
    if (this == null) return true
    return state.overlappedFraction > 0.01f
}

/**
 * Small top app bar with a theme material background.
 *
 * App bars follow the
 * [Material 3 top app bar guidelines](https://m3.material.io/components/top-app-bar/overview).
 *
 * @param title the title displayed in the app bar
 * @param modifier the [Modifier] applied to the app bar
 * @param subtitle optional text displayed below the title
 * @param titleHorizontalAlignment horizontal alignment of the title and subtitle
 * @param navigationIcon the navigation icon displayed at the start of the app bar
 * @param actions the actions displayed at the end of the app bar
 * @param expandedHeight the expanded height of the app bar
 * @param contentPadding padding around the app bar content
 * @param windowInsets the window insets respected by the app bar
 * @param edge the edge used by the Backdrop surface and its effects
 * @param colors the colors used for app bar content
 * @param scrollBehavior controls the app bar's scroll response and reports when content moves under
 *   it; when supplied, connect its nestedScrollConnection to the scrolling content
 * @param effects an optional replacement for the theme's scroll-edge optical effect chain
 * @param fill the background fill, ramped only when [scrollBehavior] is supplied
 * @param backdropEnabled whether the bar draws and exports its own material background
 * @param blurEnabled whether built-in blur is allowed, subject to the master and accessibility
 *   gates
 * @param progressiveBlurMode the algorithm override, or null to inherit the enclosing selection
 */
@Composable
public fun BackdropTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    expandedHeight: Dp = TopAppBarDefaults.TopAppBarExpandedHeight,
    contentPadding: PaddingValues = TopAppBarDefaults.ContentPadding,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    edge: BackdropEdge = BackdropEdge.Top,
    colors: TopAppBarColors = BackdropAppBarDefaults.colors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    effects: (BackdropEffectScope.() -> Unit)? = null,
    fill: BackdropFill? = BackdropFill(),
    backdropEnabled: Boolean = true,
    blurEnabled: Boolean = true,
    progressiveBlurMode: ProgressiveBlurMode? = null,
) {
    ProvideBackdropTopAppBarLayout(
        edge,
        scrollBehavior,
        effects,
        fill,
        backdropEnabled,
        blurEnabled,
        progressiveBlurMode,
    ) {
        M3xSingleRowTopAppBar(
            title = title,
            subtitle = subtitle ?: {},
            titleHorizontalAlignment = titleHorizontalAlignment,
            modifier = modifier,
            navigationIcon = BackdropAppBarDefaults.navigation(navigationIcon),
            actions = BackdropAppBarDefaults.actions(actions),
            expandedHeight =
                expandedHeight.appBarHeightOr(TopAppBarDefaults.TopAppBarExpandedHeight),
            contentPadding = contentPadding,
            windowInsets = windowInsets,
            colors = colors,
            scrollBehavior = scrollBehavior,
        )
    }
}

/**
 * Medium flexible top app bar with a theme material background.
 *
 * @param title the title displayed in the app bar
 * @param modifier the [Modifier] applied to the app bar
 * @param subtitle optional text displayed below the title
 * @param titleHorizontalAlignment horizontal alignment of the title and subtitle
 * @param navigationIcon the navigation icon displayed at the start of the app bar
 * @param actions the actions displayed at the end of the app bar
 * @param collapsedHeight the collapsed height of the app bar
 * @param expandedHeight the expanded height of the app bar
 * @param windowInsets the window insets respected by the app bar
 * @param edge the edge used by the Backdrop surface and its effects
 * @param colors the colors used for app bar content
 * @param scrollBehavior controls the app bar's scroll response and reports when content moves under
 *   it; when supplied, connect its nestedScrollConnection to the scrolling content
 * @param effects an optional replacement for the theme's scroll-edge optical effect chain
 * @param fill the background fill, ramped only when [scrollBehavior] is supplied
 * @param backdropEnabled whether the bar draws and exports its own material background
 * @param blurEnabled whether built-in blur is allowed, subject to the master and accessibility
 *   gates
 * @param progressiveBlurMode the algorithm override, or null to inherit the enclosing selection
 */
@Composable
public fun BackdropMediumFlexibleTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    collapsedHeight: Dp = TopAppBarDefaults.MediumAppBarCollapsedHeight,
    expandedHeight: Dp =
        if (subtitle != null) TopAppBarDefaults.MediumFlexibleAppBarWithSubtitleExpandedHeight
        else TopAppBarDefaults.MediumFlexibleAppBarWithoutSubtitleExpandedHeight,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    edge: BackdropEdge = BackdropEdge.Top,
    colors: TopAppBarColors = BackdropAppBarDefaults.colors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    effects: (BackdropEffectScope.() -> Unit)? = null,
    fill: BackdropFill? = BackdropFill(),
    backdropEnabled: Boolean = true,
    blurEnabled: Boolean = true,
    progressiveBlurMode: ProgressiveBlurMode? = null,
) {
    ProvideBackdropTopAppBarLayout(
        edge,
        scrollBehavior,
        effects,
        fill,
        backdropEnabled,
        blurEnabled,
        progressiveBlurMode,
    ) {
        M3xTwoRowsTopAppBar(
            titleTextStyle = MaterialTheme.typography.headlineMedium,
            subtitleTextStyle = MaterialTheme.typography.labelLarge,
            title = title,
            subtitle = subtitle ?: {},
            modifier = modifier,
            navigationIcon = BackdropAppBarDefaults.navigation(navigationIcon),
            actions = BackdropAppBarDefaults.actions(actions),
            titleHorizontalAlignment = titleHorizontalAlignment,
            collapsedHeight =
                collapsedHeight.appBarHeightOr(TopAppBarDefaults.MediumAppBarCollapsedHeight),
            expandedHeight =
                expandedHeight.appBarHeightOr(
                    if (subtitle != null)
                        TopAppBarDefaults.MediumFlexibleAppBarWithSubtitleExpandedHeight
                    else TopAppBarDefaults.MediumFlexibleAppBarWithoutSubtitleExpandedHeight,
                ),
            windowInsets = windowInsets,
            colors = colors,
            scrollBehavior = scrollBehavior,
        )
    }
}

/**
 * Large flexible top app bar with a theme material background.
 *
 * @param title the title displayed in the app bar
 * @param modifier the [Modifier] applied to the app bar
 * @param subtitle optional text displayed below the title
 * @param titleHorizontalAlignment horizontal alignment of the title and subtitle
 * @param navigationIcon the navigation icon displayed at the start of the app bar
 * @param actions the actions displayed at the end of the app bar
 * @param collapsedHeight the collapsed height of the app bar
 * @param expandedHeight the expanded height of the app bar
 * @param windowInsets the window insets respected by the app bar
 * @param edge the edge used by the Backdrop surface and its effects
 * @param colors the colors used for app bar content
 * @param scrollBehavior controls the app bar's scroll response and reports when content moves under
 *   it; when supplied, connect its nestedScrollConnection to the scrolling content
 * @param effects an optional replacement for the theme's scroll-edge optical effect chain
 * @param fill the background fill, ramped only when [scrollBehavior] is supplied
 * @param backdropEnabled whether the bar draws and exports its own material background
 * @param blurEnabled whether built-in blur is allowed, subject to the master and accessibility
 *   gates
 * @param progressiveBlurMode the algorithm override, or null to inherit the enclosing selection
 */
@Composable
public fun BackdropLargeFlexibleTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    collapsedHeight: Dp = TopAppBarDefaults.LargeAppBarCollapsedHeight,
    expandedHeight: Dp =
        if (subtitle != null) TopAppBarDefaults.LargeFlexibleAppBarWithSubtitleExpandedHeight
        else TopAppBarDefaults.LargeFlexibleAppBarWithoutSubtitleExpandedHeight,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    edge: BackdropEdge = BackdropEdge.Top,
    colors: TopAppBarColors = BackdropAppBarDefaults.colors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    effects: (BackdropEffectScope.() -> Unit)? = null,
    fill: BackdropFill? = BackdropFill(),
    backdropEnabled: Boolean = true,
    blurEnabled: Boolean = true,
    progressiveBlurMode: ProgressiveBlurMode? = null,
) {
    ProvideBackdropTopAppBarLayout(
        edge,
        scrollBehavior,
        effects,
        fill,
        backdropEnabled,
        blurEnabled,
        progressiveBlurMode,
    ) {
        M3xTwoRowsTopAppBar(
            titleTextStyle = MaterialTheme.typography.displaySmall,
            subtitleTextStyle = MaterialTheme.typography.titleMedium,
            titleBottomPadding = 28.dp,
            title = title,
            subtitle = subtitle ?: {},
            modifier = modifier,
            navigationIcon = BackdropAppBarDefaults.navigation(navigationIcon),
            actions = BackdropAppBarDefaults.actions(actions),
            titleHorizontalAlignment = titleHorizontalAlignment,
            collapsedHeight =
                collapsedHeight.appBarHeightOr(TopAppBarDefaults.LargeAppBarCollapsedHeight),
            expandedHeight =
                expandedHeight.appBarHeightOr(
                    if (subtitle != null)
                        TopAppBarDefaults.LargeFlexibleAppBarWithSubtitleExpandedHeight
                    else TopAppBarDefaults.LargeFlexibleAppBarWithoutSubtitleExpandedHeight,
                ),
            windowInsets = windowInsets,
            colors = colors,
            scrollBehavior = scrollBehavior,
        )
    }
}

@Composable
private fun ProvideBackdropTopAppBarLayout(
    edge: BackdropEdge,
    scrollBehavior: TopAppBarScrollBehavior?,
    effects: (BackdropEffectScope.() -> Unit)?,
    fill: BackdropFill?,
    backdropEnabled: Boolean,
    blurEnabled: Boolean,
    progressiveBlurMode: ProgressiveBlurMode?,
    content: @Composable () -> Unit,
) {
    if (!backdropEnabled) {
        CompositionLocalProvider(LocalBackdropAppBarSurface provides Modifier, content = content)
        return
    }
    val page = LocalBackdrop.current
    val bar = rememberBackdropLayer()
    val controlSource = rememberStackedBackdropLayer(listOfNotNull(page, bar))
    val appearance =
        rememberScrollEdgeAppearance(
            visible = { scrollBehavior.hasContentUnderBar() },
            effects = effects,
            edge = edge,
            progressive = scrollBehavior != null,
            blurEnabled = blurEnabled,
            progressiveBlurMode = progressiveBlurMode,
        )
    val surface =
        Modifier.backdropScrollEdge(
            appearance,
            edge = edge,
            exportedBackdrop = bar.layer,
            fill = fill,
            progressive = scrollBehavior != null,
        )
    CompositionLocalProvider(
        LocalBackdropAppBarSurface provides surface,
        LocalBackdrop provides
            if (page != null && appearance.drawsBackground) controlSource else page,
        content = content,
    )
}

private fun Dp.appBarHeightOr(defaultValue: Dp): Dp =
    if (this == Dp.Unspecified || this == Dp.Infinity) defaultValue else this
