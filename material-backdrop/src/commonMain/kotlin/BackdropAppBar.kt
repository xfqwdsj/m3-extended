package top.ltfan.m3x.material.backdrop

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.ltfan.backdrop.BackdropEdge
import top.ltfan.backdrop.BackdropEffectScope
import top.ltfan.backdrop.BackdropMask
import top.ltfan.backdrop.backdrops.LayerBackdrop
import top.ltfan.backdrop.effects.progressiveBlur
import top.ltfan.m3x.backdrop.LocalBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.animation.BackdropBlurAnimationEnabled
import top.ltfan.m3x.compose.ui.visual.BackdropBlurEnabled
import top.ltfan.m3x.compose.ui.visual.LocalBlurEnabled
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.backdrop.internal.LocalBackdropAppBarSurface
import top.ltfan.m3x.material.backdrop.internal.M3xSingleRowTopAppBar
import top.ltfan.m3x.material.backdrop.internal.M3xTwoRowsTopAppBar
import top.ltfan.m3x.material.backdrop.theme.backdrop
import top.ltfan.m3x.material.theme.extended

/** Defaults shared by the four Backdrop top app bars. */
public object BackdropAppBarDefaults {
    /**
     * Progressive blur of the theme's scroll-edge material, enabled by [LocalBlurEnabled]: [radius]
     * defaults to that material's blur radius, so a bar that replaces the effect chain still
     * follows the theme.
     */
    @Composable
    public fun progressiveEffects(
        radius: Dp = MaterialTheme.extended.backdrop.scrollEdge.blurRadius,
    ): BackdropEffectScope.() -> Unit {
        val blurEnabled = BackdropBlurEnabled
        val ramp = MaterialTheme.extended.backdrop.scrollEdge
        return if (blurEnabled) {
            {
                progressiveBlur(radius.toPx(), BackdropEdge.Top, ramp.ramp)
            }
        } else {
            {}
        }
    }

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

/**
 * The spec a scroll edge appears with. It animates an effect rather than a position, so it takes
 * the motion scheme's effects spec, which settles without overshooting the strength the bar shows
 * at rest.
 */
@Composable
internal fun scrollEdgePresenceSpec(): FiniteAnimationSpec<Float> =
    MaterialTheme.motionScheme.defaultEffectsSpec()

/**
 * Whether the content has moved under the bar.
 *
 * [TopAppBarScrollBehavior] reports this through [TopAppBarState.overlappedFraction]. The bar
 * publishes its height as the offset limit. The fraction tracks a collapsing bar and a pinned bar
 * whose content offset grows as content passes underneath.
 */
private fun TopAppBarScrollBehavior?.hasContentUnderBar(): Boolean {
    if (this == null) return true
    return state.overlappedFraction > 0.01f
}

@Composable
private fun Modifier.backdropAppBarSurface(
    effects: (BackdropEffectScope.() -> Unit)?,
    mask: BackdropMask?,
    exportedBackdrop: LayerBackdrop,
    edge: BackdropEdge,
    scrollBehavior: TopAppBarScrollBehavior?,
): Modifier {
    val backdrop = MaterialTheme.extended.backdrop
    val material = backdrop.scrollEdge
    // A bar without a scroll behavior cannot tell that content moved under it, so its glass stays
    // in place rather than fading out with nothing to bring it back.
    val target by
        remember(scrollBehavior) {
            derivedStateOf { if (scrollBehavior.hasContentUnderBar()) 1f else 0f }
        }
    val presence =
        animateFloatAsState(
            targetValue = target,
            animationSpec = scrollEdgePresenceSpec(),
            label = "BackdropScrollEdge",
        )
    val animateBlur = BackdropBlurAnimationEnabled
    val opticalPresence = if (animateBlur) presence else null
    val appearance =
        rememberMaterialAppearance(
            MaterialSurfaceRole.ScrollEdge,
            material = material,
            effects = effects,
            edge = edge,
            presence = opticalPresence,
            shape = RectangleShape,
        )
    // Finite blur motion changes radius and mask strength without fading the background layer.
    // Its fallback keeps the material at full strength and fades only this background box.
    val background =
        if (animateBlur) this else graphicsLayer { alpha = presence.value.coerceIn(0f, 1f) }
    return background.materialBackdropSurface(
        appearance = appearance,
        shape = RectangleShape,
        exportedBackdrop = exportedBackdrop,
        mask = mask,
        edge = edge,
        presence = opticalPresence,
    )
}

/**
 * Small top app bar with progressive Backdrop blur, or the same progressive tint without a source.
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
 * @param mask the progressive mask applied to the backdrop content behind the app bar
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
    mask: BackdropMask? = BackdropMask(),
) {
    ProvideBackdropTopAppBarLayout(edge, scrollBehavior, effects, mask) {
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
 * Center-aligned small top app bar with progressive Backdrop blur, or the same progressive tint
 * without a source.
 *
 * @param title the title displayed in the app bar
 * @param modifier the [Modifier] applied to the app bar
 * @param subtitle optional text displayed below the title
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
 * @param mask the progressive mask applied to the backdrop content behind the app bar
 */
@Composable
public fun BackdropCenterAlignedTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    expandedHeight: Dp = TopAppBarDefaults.TopAppBarExpandedHeight,
    contentPadding: PaddingValues = TopAppBarDefaults.ContentPadding,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    edge: BackdropEdge = BackdropEdge.Top,
    colors: TopAppBarColors = BackdropAppBarDefaults.colors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    effects: (BackdropEffectScope.() -> Unit)? = null,
    mask: BackdropMask? = BackdropMask(),
) {
    ProvideBackdropTopAppBarLayout(edge, scrollBehavior, effects, mask) {
        M3xSingleRowTopAppBar(
            title = title,
            subtitle = subtitle ?: {},
            titleHorizontalAlignment = Alignment.CenterHorizontally,
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
 * Medium flexible top app bar with progressive Backdrop blur, or the same progressive tint without
 * a source.
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
 * @param mask the progressive mask applied to the backdrop content behind the app bar
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
    mask: BackdropMask? = BackdropMask(),
) {
    ProvideBackdropTopAppBarLayout(edge, scrollBehavior, effects, mask) {
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
 * Large flexible top app bar with progressive Backdrop blur, or the same progressive tint without a
 * source.
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
 * @param mask the progressive mask applied to the backdrop content behind the app bar
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
    mask: BackdropMask? = BackdropMask(),
) {
    ProvideBackdropTopAppBarLayout(edge, scrollBehavior, effects, mask) {
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
    mask: BackdropMask?,
    content: @Composable () -> Unit,
) {
    val page = LocalBackdrop.current
    val bar = rememberBackdropLayer()
    val surface = Modifier.backdropAppBarSurface(effects, mask, bar.layer, edge, scrollBehavior)
    CompositionLocalProvider(
        LocalBackdropAppBarSurface provides surface,
        LocalBackdrop provides if (page != null) bar else null,
        content = content,
    )
}

private fun Dp.appBarHeightOr(defaultValue: Dp): Dp =
    if (this == Dp.Unspecified || this == Dp.Infinity) defaultValue else this
