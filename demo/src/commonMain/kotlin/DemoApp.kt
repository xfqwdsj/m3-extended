package top.ltfan.m3x.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlin.time.Duration.Companion.milliseconds
import top.ltfan.backdrop.BackdropHdrScope
import top.ltfan.backdrop.BackdropMask
import top.ltfan.backdrop.LocalBackdropHdrHeadroom
import top.ltfan.backdrop.LocalBackdropHdrRefreshInterval
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.animation.LocalBlurAnimationEnabled
import top.ltfan.m3x.compose.ui.privateapi.ProvidePrivateCornerRadii
import top.ltfan.m3x.compose.ui.visual.LocalBlurEnabled
import top.ltfan.m3x.datastore.StoreFile
import top.ltfan.m3x.datastore.compose.storeHost
import top.ltfan.m3x.demo.generated.resources.Res
import top.ltfan.m3x.demo.generated.resources.demo_back
import top.ltfan.m3x.demo.generated.resources.demo_reset_lab_parameters
import top.ltfan.m3x.demo.generated.resources.demo_restore_defaults
import top.ltfan.m3x.demo.generated.resources.demo_settings_editor_demo
import top.ltfan.m3x.demo.generated.resources.demo_toggle_runtime_bounds
import top.ltfan.m3x.demo.generated.resources.demo_toggle_runtime_options
import top.ltfan.m3x.demo.generated.resources.demo_visual_settings
import top.ltfan.m3x.material.FloatingBottomBar
import top.ltfan.m3x.material.IconButton
import top.ltfan.m3x.material.IconButtonDefaults
import top.ltfan.m3x.material.backdrop.BackdropCenterAlignedTopAppBar
import top.ltfan.m3x.material.backdrop.BackdropLargeFlexibleTopAppBar
import top.ltfan.m3x.material.backdrop.BackdropMediumFlexibleTopAppBar
import top.ltfan.m3x.material.backdrop.BackdropTopAppBar
import top.ltfan.m3x.material.backdrop.BackdropTopAppBarControl
import top.ltfan.m3x.material.backdrop.backdropIndicatorEffects
import top.ltfan.m3x.material.bundle.M3xMaterialTheme
import top.ltfan.m3x.material.item
import top.ltfan.m3x.material.overlay.IconButtonWithTooltip
import top.ltfan.m3x.material.theme.rememberAdaptiveCornerRadii
import top.ltfan.m3x.material.theming.capsule.capsuleShapes
import top.ltfan.m3x.material.theming.roundedShapes
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState

/** Complete shared demo; platform launchers supply normal persisted application settings. */
@Composable
fun DemoApp(storeFile: StoreFile = rememberDemoStoreFile()) {
    ProvidePrivateCornerRadii { DemoAppContent(storeFile) }
}

@Composable
private fun DemoAppContent(storeFile: StoreFile) {
    val scope = rememberCoroutineScope()
    val host = remember(scope, storeFile) { storeHost(scope, storeFile) }
    DisposableEffect(host) { onDispose { host.close() } }
    val preferences = remember(host) { host.mutable(DemoPreferencesStore) }
    val features = remember(host) { host.mutable(FeatureSettingsStore) }
    val lab = remember(host) { host.mutable(LabParametersStore) }
    val prefs = preferences.value
    val density = LocalDensity.current
    val dark =
        when (prefs.appearance.theme) {
            ThemeMode.System -> isSystemInDarkTheme()
            ThemeMode.Light -> false
            ThemeMode.Dark -> true
        }
    val scheme =
        if (prefs.appearance.uiPalette == UiPalette.Colorful) colorfulColorScheme(dark)
        else
            when (prefs.appearance.theme) {
                ThemeMode.System -> if (dark) darkColorScheme() else null
                ThemeMode.Light -> lightColorScheme()
                ThemeMode.Dark -> darkColorScheme()
            }
    CompositionLocalProvider(
        LocalBackdropHdrRefreshInterval provides
            prefs.effects.hdrRefreshIntervalMs.toInt().milliseconds,
        LocalBlurEnabled provides prefs.effects.blur,
        LocalBlurAnimationEnabled provides prefs.effects.blurAnimation,
        LocalDensity provides
            Density(density.density, density.fontScale * prefs.appearance.fontScale),
    ) {
        BackdropHdrScope {
            val hdrHeadroomAvailable = LocalBackdropHdrHeadroom.current > 1.01f
            val radii = rememberAdaptiveCornerRadii()
            val shapes =
                remember(prefs.appearance.shape, radii) {
                    if (prefs.appearance.shape == ShapePreset.Capsule) capsuleShapes(radii)
                    else roundedShapes(radii)
                }
            M3xMaterialTheme(
                colorScheme = scheme,
                shapes = shapes,
                backdrop =
                    prefs.themeBackdrop(
                        interactionHdr =
                            prefs.effects.hdrOutput &&
                                prefs.effects.interactionHdr &&
                                hdrHeadroomAvailable,
                        hdrHighlightAvailable = prefs.effects.hdrOutput && hdrHeadroomAvailable,
                    ),
                accessibility = prefs.themeAccessibility(),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ) {
                    val backStack =
                        rememberNavBackStack(DemoNavigationConfiguration, DemoRoute.Components)
                    val current = backStack.last() as DemoRoute
                    ApplyDemoHdrWindowSettings(
                        prefs.effects.hdrOutput ||
                            (current == DemoRoute.Lab && lab.value.optics.hdrBackground),
                        prefs.effects.hdrHeadroom,
                    )
                    val page = rememberBackdropLayer()
                    val background = rememberBackdropLayer()
                    val overlay = remember { OverlayHostState() }
                    val manualSettings = remember { ManualSettingsTree() }
                    var pageSize by remember { mutableStateOf(IntSize.Zero) }
                    // The app bar mask continues the page background instead of covering it with a
                    // flat
                    // theme color. The gradient is anchored to the page, so the bar samples the
                    // same
                    // colors the page paints behind it rather than restarting the gradient in its
                    // own
                    // bounds.
                    val backgroundColors = demoBackgroundColors(prefs.appearance.background)
                    val maskBrush =
                        remember(backgroundColors, pageSize) {
                            if (pageSize.width == 0 || pageSize.height == 0) null
                            else
                                object : ShaderBrush() {
                                    override fun createShader(size: Size): Shader =
                                        LinearGradientShader(
                                            from = Offset.Zero,
                                            to =
                                                Offset(
                                                    pageSize.width.toFloat(),
                                                    pageSize.height.toFloat(),
                                                ),
                                            colors = backgroundColors,
                                        )
                                }
                        }
                    fun navigate(route: DemoRoute) {
                        if (current != route) {
                            backStack.clear()
                            backStack.add(route)
                        }
                    }
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val scrollBehavior =
                            rememberDemoScrollBehavior(current, prefs.appearance.bar, maxWidth)
                        BackdropHost(page) {
                            OverlayHost(overlay) {
                                Scaffold(
                                    modifier =
                                        Modifier.fillMaxSize().let { modifier ->
                                            if (scrollBehavior == null) modifier
                                            else
                                                modifier.nestedScroll(
                                                    scrollBehavior.nestedScrollConnection,
                                                )
                                        },
                                    containerColor = Color.Transparent,
                                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                                    topBar = {
                                        val title: @Composable () -> Unit = {
                                            Text("M3X · ${current.title()}")
                                        }
                                        val navigation: @Composable () -> Unit = {
                                            if (backStack.size > 1)
                                                BackdropTopAppBarControl {
                                                    IconButton(
                                                        onClick = {
                                                            backStack.removeAt(backStack.lastIndex)
                                                        },
                                                        shapes = IconButtonDefaults.staticShapes(),
                                                        content = {
                                                            Icon(
                                                                Icons.AutoMirrored.Filled.ArrowBack,
                                                                demoText(Res.string.demo_back),
                                                            )
                                                        },
                                                    )
                                                }
                                        }
                                        val actions: @Composable RowScope.() -> Unit = {
                                            BackdropTopAppBarControl {
                                                when (current) {
                                                    DemoRoute.Settings -> {
                                                        IconButtonWithTooltip(
                                                            rememberVectorPainter(
                                                                Icons.Default.Refresh,
                                                            ),
                                                            demoText(
                                                                Res.string.demo_restore_defaults,
                                                            ),
                                                            buttonShapes =
                                                                IconButtonDefaults.staticShapes(),
                                                            onClick = {
                                                                preferences.write(DemoPreferences())
                                                            },
                                                        )
                                                        IconButtonWithTooltip(
                                                            rememberVectorPainter(
                                                                Icons.AutoMirrored.Filled.List,
                                                            ),
                                                            demoText(
                                                                Res.string
                                                                    .demo_settings_editor_demo,
                                                            ),
                                                            buttonShapes =
                                                                IconButtonDefaults.staticShapes(),
                                                            onClick = {
                                                                backStack.add(
                                                                    DemoRoute.SettingsCatalog,
                                                                )
                                                            },
                                                        )
                                                    }
                                                    DemoRoute.SettingsCatalog -> {
                                                        IconButtonWithTooltip(
                                                            rememberVectorPainter(
                                                                Icons.AutoMirrored.Filled.List,
                                                            ),
                                                            demoText(
                                                                Res.string
                                                                    .demo_toggle_runtime_options,
                                                            ),
                                                            buttonShapes =
                                                                IconButtonDefaults.staticShapes(),
                                                            onClick = {
                                                                manualSettings.toggleModes()
                                                            },
                                                        )
                                                        IconButtonWithTooltip(
                                                            rememberVectorPainter(
                                                                Icons.Default.Refresh,
                                                            ),
                                                            demoText(
                                                                Res.string
                                                                    .demo_toggle_runtime_bounds,
                                                            ),
                                                            buttonShapes =
                                                                IconButtonDefaults.staticShapes(),
                                                            onClick = {
                                                                manualSettings.toggleScaleBounds()
                                                            },
                                                        )
                                                    }
                                                    DemoRoute.Lab ->
                                                        IconButtonWithTooltip(
                                                            rememberVectorPainter(
                                                                Icons.Default.Refresh,
                                                            ),
                                                            demoText(
                                                                Res.string
                                                                    .demo_reset_lab_parameters,
                                                            ),
                                                            buttonShapes =
                                                                IconButtonDefaults.staticShapes(),
                                                            onClick = {
                                                                lab.write(LabParameters())
                                                            },
                                                        )
                                                    else ->
                                                        IconButtonWithTooltip(
                                                            rememberVectorPainter(
                                                                Icons.Default.Settings,
                                                            ),
                                                            demoText(
                                                                Res.string.demo_visual_settings,
                                                            ),
                                                            buttonShapes =
                                                                IconButtonDefaults.staticShapes(),
                                                            onClick = {
                                                                navigate(DemoRoute.Settings)
                                                            },
                                                        )
                                                }
                                            }
                                        }
                                        when (prefs.appearance.bar) {
                                            BarVariant.Small ->
                                                BackdropTopAppBar(
                                                    title,
                                                    modifier = Modifier,
                                                    scrollBehavior = scrollBehavior,
                                                    navigationIcon = navigation,
                                                    actions = actions,
                                                    mask = BackdropMask(brush = maskBrush),
                                                )
                                            BarVariant.Centered ->
                                                BackdropCenterAlignedTopAppBar(
                                                    title,
                                                    modifier = Modifier,
                                                    scrollBehavior = scrollBehavior,
                                                    navigationIcon = navigation,
                                                    actions = actions,
                                                    mask = BackdropMask(brush = maskBrush),
                                                )
                                            BarVariant.Medium ->
                                                BackdropMediumFlexibleTopAppBar(
                                                    title,
                                                    modifier = Modifier,
                                                    scrollBehavior = scrollBehavior,
                                                    navigationIcon = navigation,
                                                    actions = actions,
                                                    mask = BackdropMask(brush = maskBrush),
                                                )
                                            BarVariant.Large ->
                                                BackdropLargeFlexibleTopAppBar(
                                                    title,
                                                    modifier = Modifier,
                                                    scrollBehavior = scrollBehavior,
                                                    navigationIcon = navigation,
                                                    actions = actions,
                                                    mask = BackdropMask(brush = maskBrush),
                                                )
                                        }
                                    },
                                    bottomBar = {
                                        Box(
                                            Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            FloatingBottomBar(
                                                indicatorEffects =
                                                    if (
                                                        !prefs.effects.lens ||
                                                            prefs.appearance.preset ==
                                                                MaterialPreset.Blur
                                                    )
                                                        backdropIndicatorEffects { _ -> }
                                                    else null,
                                                selectedTabIndex =
                                                    DemoTabs.indexOf(
                                                        if (current == DemoRoute.SettingsCatalog)
                                                            DemoRoute.Settings
                                                        else current,
                                                    ),
                                                onTabSelected = { navigate(DemoTabs[it]) },
                                                modifier =
                                                    Modifier.navigationBarsPadding().padding(16.dp),
                                            ) {
                                                DemoTabs.forEachIndexed { index, route ->
                                                    item(key = route) {
                                                        Icon(
                                                            when (index) {
                                                                0 -> Icons.Default.Home
                                                                1 -> Icons.Default.Info
                                                                2 -> Icons.Default.Build
                                                                else -> Icons.Default.Settings
                                                            },
                                                            contentDescription = null,
                                                        )
                                                        Text(
                                                            route.tabTitle(),
                                                            Modifier.padding(horizontal = 10.dp),
                                                            style =
                                                                MaterialTheme.typography
                                                                    .labelMedium,
                                                            maxLines = 1,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    },
                                ) { innerPadding ->
                                    Box(Modifier.fillMaxSize().onSizeChanged { pageSize = it }) {
                                        Box(Modifier.fillMaxSize().captureBackdrop(page)) {
                                            Box(
                                                Modifier.fillMaxSize()
                                                    .captureBackdrop(background)
                                                    .background(
                                                        demoBackground(prefs.appearance.background),
                                                    ),
                                            )
                                            BackdropHost(background) {
                                                CompositionLocalProvider(
                                                    LocalDemoTopInset provides
                                                        innerPadding.calculateTopPadding(),
                                                ) {
                                                    NavDisplay(
                                                        backStack = backStack,
                                                        onBack = {
                                                            if (backStack.size > 1)
                                                                backStack.removeAt(
                                                                    backStack.lastIndex,
                                                                )
                                                            else navigate(DemoRoute.Components)
                                                        },
                                                        entryProvider = { key ->
                                                            NavEntry(key) {
                                                                when (key) {
                                                                    DemoRoute.Components ->
                                                                        ComponentsPage()
                                                                    DemoRoute.Overlays ->
                                                                        OverlaysPage()
                                                                    DemoRoute.Lab ->
                                                                        BackdropLabPage(
                                                                            lab,
                                                                            hdrOutput =
                                                                                prefs.effects
                                                                                    .hdrOutput,
                                                                        )
                                                                    DemoRoute.Settings ->
                                                                        SettingsPage(preferences)
                                                                    DemoRoute.SettingsCatalog ->
                                                                        SettingsCatalogPage(
                                                                            features,
                                                                            manualSettings,
                                                                        )
                                                                }
                                                            }
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun demoBackground(background: PreviewBackground): Brush =
    Brush.linearGradient(demoBackgroundColors(background))

/** The page colors, shared with the app bar mask so the bar continues the page background. */
@Composable
internal fun demoBackgroundColors(background: PreviewBackground): List<Color> {
    val scheme = MaterialTheme.colorScheme
    return if (background == PreviewBackground.Theme)
        listOf(scheme.background, scheme.surfaceContainer)
    else colorfulBackground(scheme.background.luminance() < 0.5f)
}

internal val LocalDemoTopInset = compositionLocalOf { 80.dp }

internal val DemoLabWideBreakpoint = 900.dp

internal fun isNarrowDemoLab(route: DemoRoute, width: Dp): Boolean =
    route == DemoRoute.Lab && width < DemoLabWideBreakpoint

internal fun shouldCollapseDemoBar(route: DemoRoute, bar: BarVariant, width: Dp): Boolean =
    (bar == BarVariant.Medium || bar == BarVariant.Large) && !isNarrowDemoLab(route, width)

@Composable
internal fun rememberDemoScrollBehavior(
    route: DemoRoute,
    bar: BarVariant,
    width: Dp,
): TopAppBarScrollBehavior? {
    val narrowLab = isNarrowDemoLab(route, width)
    val behavior =
        if (narrowLab) null
        else if (shouldCollapseDemoBar(route, bar, width))
            TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        else TopAppBarDefaults.pinnedScrollBehavior()
    // Re-entering the wide layout starts the bar expanded, including after resizing mid-scroll.
    LaunchedEffect(route, bar, narrowLab) {
        behavior?.state?.heightOffset = 0f
        behavior?.state?.contentOffset = 0f
    }
    return behavior
}

/** Reading width of a page column; wider windows keep the content centered instead of stretched. */
internal val DemoContentWidth = 840.dp

/** Width of the two-column backdrop lab, which keeps a preview beside its parameter list. */
internal val DemoWideContentWidth = 1200.dp

/** Horizontal page gutter shared by every page. */
internal val DemoGutter = 24.dp

/** Clearance for the floating bottom bar, whose own height and margin are fixed. */
internal val DemoBottomInset = 128.dp

/**
 * Padding for a page that scrolls beneath the glass top bar: the first item starts below the bar,
 * and everything above it slides under the bar while scrolling.
 */
internal val DemoContentPadding: PaddingValues
    @Composable
    get() =
        PaddingValues(
            top = LocalDemoTopInset.current + 16.dp,
            bottom = DemoBottomInset,
            start = DemoGutter,
            end = DemoGutter,
        )

/** Centers a page column at [width] so a desktop window does not stretch every line of text. */
@Composable
internal fun DemoPageHost(
    modifier: Modifier = Modifier,
    width: Dp = DemoContentWidth,
    content: @Composable () -> Unit,
) {
    Box(
        modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(Modifier.fillMaxHeight().widthIn(max = width).fillMaxWidth()) { content() }
    }
}
