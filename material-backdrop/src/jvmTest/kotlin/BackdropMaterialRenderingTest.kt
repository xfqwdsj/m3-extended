package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.ltfan.backdrop.BackdropMask
import top.ltfan.backdrop.BackdropStyle
import top.ltfan.backdrop.effects.colorControls
import top.ltfan.backdrop.highlight.Highlight
import top.ltfan.backdrop.shadow.Shadow
import top.ltfan.m3x.backdrop.BackdropAppearanceState
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.BackdropLayerHandle
import top.ltfan.m3x.backdrop.backdropSurface
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropAppearanceState
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.backdrop.theme.BackdropAdaptation
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering

class BackdropMaterialRenderingTest {
    @Test
    fun allAppBarVariantsLetCapsuleShadowsCrossTheActionsRow() {
        for (variant in 0..3) runComposeUiTest {
            setContent {
                ProvideM3xMaterialRendering {
                    MaterialTheme {
                        val source = rememberBackdropLayer(Color.White)
                        BackdropHost(source) {
                            Box(
                                Modifier.size(320.dp, 340.dp)
                                    .background(Color.White)
                                    .testTag("root"),
                            ) {
                                Box(Modifier.fillMaxSize().captureBackdrop(source))
                                val actions: @Composable RowScope.() -> Unit = {
                                    BackdropTopAppBarControl(
                                        modifier = Modifier.testTag("capsule"),
                                        material =
                                            M3xBackdropMaterial(
                                                style =
                                                    BackdropStyle(
                                                        highlight = { Highlight.None },
                                                        shadow = {
                                                            Shadow(
                                                                radius = 16.dp,
                                                                offset = DpOffset.Zero,
                                                                color =
                                                                    Color.Black.copy(alpha = 0.8f),
                                                            )
                                                        },
                                                    ),
                                                blurRadius = 0.dp,
                                                adaptation = BackdropAdaptation.Fixed,
                                                interactiveLighting = false,
                                            ),
                                    ) {
                                        Box(Modifier.size(48.dp))
                                    }
                                }
                                when (variant) {
                                    0 ->
                                        BackdropTopAppBar(
                                            title = {},
                                            actions = actions,
                                            windowInsets = WindowInsets(0, 0, 0, 0),
                                        )
                                    1 ->
                                        BackdropCenterAlignedTopAppBar(
                                            title = {},
                                            actions = actions,
                                            windowInsets = WindowInsets(0, 0, 0, 0),
                                        )
                                    2 ->
                                        BackdropMediumFlexibleTopAppBar(
                                            title = {},
                                            actions = actions,
                                            windowInsets = WindowInsets(0, 0, 0, 0),
                                        )
                                    3 ->
                                        BackdropLargeFlexibleTopAppBar(
                                            title = {},
                                            actions = actions,
                                            windowInsets = WindowInsets(0, 0, 0, 0),
                                        )
                                }
                            }
                        }
                    }
                }
            }
            val capsule = onNodeWithTag("capsule").fetchSemanticsNode().boundsInRoot
            val image = onNodeWithTag("root").captureToImage()
            val shadow =
                image.toPixelMap()[capsule.center.x.toInt(), (capsule.bottom + 12f).toInt()]
            assertTrue(
                shadow.red < 0.95f,
                "variant=$variant expected shadow outside actions row, got $shadow",
            )
        }
    }

    @Test
    fun actionsSampleTheContainingMaterialInEveryBarVariant() {
        for (variant in 0..3) runComposeUiTest {
            setContent {
                ProvideM3xMaterialRendering {
                    MaterialTheme {
                        val page = rememberBackdropLayer(Color.Blue)
                        BackdropHost(page) {
                            Box(
                                Modifier.size(320.dp, 340.dp)
                                    .background(Color.Blue)
                                    .testTag("root"),
                            ) {
                                Box(
                                    Modifier.fillMaxSize()
                                        .captureBackdrop(page)
                                        .background(Color.Blue),
                                )
                                val actions: @Composable RowScope.() -> Unit = {
                                    BackdropTopAppBarControl(
                                        Modifier.testTag("capsule"),
                                        material =
                                            M3xBackdropMaterial(
                                                style =
                                                    BackdropStyle(
                                                        highlight = { Highlight.None },
                                                        shadow = { Shadow.None },
                                                    ),
                                                blurRadius = 0.dp,
                                                tintAlpha = 0f,
                                                adaptation = BackdropAdaptation.Fixed,
                                                interactiveLighting = false,
                                            ),
                                    ) {
                                        Box(Modifier.size(48.dp))
                                    }
                                }
                                val insets = WindowInsets(0, 0, 0, 0)
                                when (variant) {
                                    0 ->
                                        BackdropTopAppBar(
                                            title = {},
                                            modifier = Modifier.testTag("bar"),
                                            actions = actions,
                                            effects = {},
                                            mask = BackdropMask(Color.Red, alpha = 1f),
                                            windowInsets = insets,
                                        )
                                    1 ->
                                        BackdropCenterAlignedTopAppBar(
                                            title = {},
                                            modifier = Modifier.testTag("bar"),
                                            actions = actions,
                                            effects = {},
                                            mask = BackdropMask(Color.Red, alpha = 1f),
                                            windowInsets = insets,
                                        )
                                    2 ->
                                        BackdropMediumFlexibleTopAppBar(
                                            title = {},
                                            modifier = Modifier.testTag("bar"),
                                            actions = actions,
                                            effects = {},
                                            mask = BackdropMask(Color.Red, alpha = 1f),
                                            windowInsets = insets,
                                        )
                                    else ->
                                        BackdropLargeFlexibleTopAppBar(
                                            title = {},
                                            modifier = Modifier.testTag("bar"),
                                            actions = actions,
                                            effects = {},
                                            mask = BackdropMask(Color.Red, alpha = 1f),
                                            windowInsets = insets,
                                        )
                                }
                            }
                        }
                    }
                }
            }
            val center = onNodeWithTag("capsule").fetchSemanticsNode().boundsInRoot.center
            val pixel =
                onNodeWithTag("root")
                    .captureToImage()
                    .toPixelMap()[center.x.toInt(), center.y.toInt()]
            val bar = onNodeWithTag("bar").fetchSemanticsNode().boundsInRoot
            val expectedRed = appBarRampIntensity(center.y - bar.top + 0.5f, bar.height)
            assertTrue(
                abs(pixel.red - expectedRed) < 0.03f &&
                    abs(pixel.blue - (1f - expectedRed)) < 0.03f,
                "variant=$variant expected containing material red, got $pixel",
            )
        }
    }

    @Test
    fun fixedSurfacesUseMaterialForegroundAndRespectExplicitColors() = runComposeUiTest {
        var defaultColor = Color.Unspecified
        var explicitColor = Color.Unspecified
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(
                    colorScheme = darkColorScheme(surface = Color.Black, onSurface = Color.White),
                ) {
                    Box(Modifier.size(200.dp)) {
                        BackdropSurface { defaultColor = LocalContentColor.current }
                        BackdropSurface(contentColor = Color.Red) {
                            explicitColor = LocalContentColor.current
                        }
                    }
                }
            }
        }
        waitForIdle()
        assertEquals(Color.White, defaultColor)
        assertEquals(Color.Red, explicitColor)
    }

    @Test
    fun appBarPlacementMatchesMaterialWithRtlInsetsAndLargeFonts() {
        for (variant in 0..3) {
            val results = mutableListOf<List<Rect>>()
            for (glass in listOf(false, true)) runComposeUiTest {
                setContent {
                    ProvideM3xMaterialRendering {
                        MaterialTheme {
                            CompositionLocalProvider(
                                LocalLayoutDirection provides LayoutDirection.Rtl,
                                LocalDensity provides Density(1f, 2f),
                            ) {
                                val title: @Composable () -> Unit = {
                                    Text("A long title", Modifier.testTag("title"))
                                }
                                val navigation: @Composable () -> Unit = {
                                    Box(Modifier.size(48.dp).testTag("nav"))
                                }
                                val actions: @Composable RowScope.() -> Unit = {
                                    Box(Modifier.size(72.dp, 48.dp).testTag("actions"))
                                }
                                val insets = WindowInsets(8, 24, 12, 0)
                                Box(Modifier.size(360.dp, 400.dp)) {
                                    if (glass)
                                        when (variant) {
                                            0 ->
                                                BackdropTopAppBar(
                                                    title,
                                                    navigationIcon = navigation,
                                                    actions = actions,
                                                    windowInsets = insets,
                                                )
                                            1 ->
                                                BackdropCenterAlignedTopAppBar(
                                                    title,
                                                    navigationIcon = navigation,
                                                    actions = actions,
                                                    windowInsets = insets,
                                                )
                                            2 ->
                                                BackdropMediumFlexibleTopAppBar(
                                                    title,
                                                    navigationIcon = navigation,
                                                    actions = actions,
                                                    windowInsets = insets,
                                                )
                                            else ->
                                                BackdropLargeFlexibleTopAppBar(
                                                    title,
                                                    navigationIcon = navigation,
                                                    actions = actions,
                                                    windowInsets = insets,
                                                )
                                        }
                                    else
                                        when (variant) {
                                            0 ->
                                                TopAppBar(
                                                    title,
                                                    navigationIcon = navigation,
                                                    actions = actions,
                                                    windowInsets = insets,
                                                )
                                            1 ->
                                                CenterAlignedTopAppBar(
                                                    title,
                                                    navigationIcon = navigation,
                                                    actions = actions,
                                                    windowInsets = insets,
                                                )
                                            2 ->
                                                MediumTopAppBar(
                                                    title,
                                                    navigationIcon = navigation,
                                                    actions = actions,
                                                    windowInsets = insets,
                                                )
                                            else ->
                                                LargeTopAppBar(
                                                    title,
                                                    navigationIcon = navigation,
                                                    actions = actions,
                                                    windowInsets = insets,
                                                )
                                        }
                                }
                            }
                        }
                    }
                }
                results +=
                    listOf(
                        onNodeWithTag("nav").fetchSemanticsNode().boundsInRoot,
                        onNodeWithTag("actions").fetchSemanticsNode().boundsInRoot,
                    )
            }
            assertEquals(results[0], results[1], "variant=$variant")
        }
    }

    @Test
    fun fullyHiddenSingleRowBarDoesNotDrawOrExposeActions() = runComposeUiTest {
        lateinit var collapse: () -> Unit
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    val state = rememberTopAppBarState()
                    val behavior = TopAppBarDefaults.enterAlwaysScrollBehavior(state)
                    SideEffect { collapse = { state.heightOffset = state.heightOffsetLimit } }
                    Box(Modifier.size(320.dp, 100.dp).background(Color.White).testTag("root")) {
                        BackdropTopAppBar(
                            title = { Text("Title") },
                            scrollBehavior = behavior,
                            windowInsets = WindowInsets(0, 0, 0, 0),
                            actions = {
                                BackdropTopAppBarControl(Modifier.testTag("actions")) {
                                    Box(Modifier.size(48.dp))
                                }
                            },
                        )
                    }
                }
            }
        }
        runOnIdle { collapse() }
        waitForIdle()
        onNodeWithTag("actions").assertIsNotDisplayed()
        val image = onNodeWithTag("root").captureToImage()
        assertTrue(image.toPixelMap()[image.width - 24, 20].red > 0.95f)
    }

    @Test
    fun samplerReadsRawBackgroundBeforeEffectsAndTint() = runComposeUiTest {
        lateinit var samplerState: BackdropAppearanceState
        setContent {
            ProvideM3xMaterialRendering {
                val source = rememberBackdropLayer(Color.Red)
                val sampler = rememberBackdropAppearanceState(true, Color.White, true, 50)
                SideEffect { samplerState = sampler }
                BackdropHost(source) {
                    Box(Modifier.size(80.dp)) {
                        Box(Modifier.fillMaxSize().captureBackdrop(source))
                        Box(
                            Modifier.fillMaxSize()
                                .onGloballyPositioned { sampler.updateCoordinates(it) }
                                .backdropSurface(
                                    shape = RectangleShape,
                                    effects = { colorControls(brightness = 0.8f) },
                                    highlight = { Highlight.None },
                                    shadow = { Shadow.None },
                                    onDrawBackdrop = { draw, _ ->
                                        with(sampler) { drawAndRecord(draw) }
                                    },
                                    onDrawSurface = { drawRect(Color.Blue) },
                                ),
                        )
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5000) { samplerState.appearance != null }
        assertTrue(samplerState.appearance!!.relativeLuminance in 0.20f..0.23f)
    }

    @Test
    fun defaultMaterialKeepsThemeContentWithoutAppearanceSampling() = runComposeUiTest {
        lateinit var defaultAppearance: MaterialBackdropAppearance
        lateinit var adaptedAppearance: MaterialBackdropAppearance
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    val source = rememberBackdropLayer(Color.Red)
                    BackdropHost(source) {
                        val default =
                            rememberMaterialAppearance(
                                role = MaterialSurfaceRole.Control,
                                containerColor = Color.White,
                                contentColor = Color.Green,
                                effects = {},
                            )
                        val adapted =
                            rememberMaterialAppearance(
                                role = MaterialSurfaceRole.Control,
                                material =
                                    M3xBackdropMaterial(
                                        blurRadius = 0.dp,
                                        adaptation = BackdropAdaptation.Background,
                                        adaptiveContentColor = true,
                                    ),
                                containerColor = Color.White,
                                contentColor = Color.Green,
                                effects = {},
                            )
                        SideEffect {
                            defaultAppearance = default
                            adaptedAppearance = adapted
                        }
                        Box(Modifier.size(100.dp)) {
                            Box(
                                Modifier.fillMaxSize()
                                    .captureBackdrop(source)
                                    .background(Color.Red),
                            )
                            Row {
                                Box(
                                    Modifier.size(48.dp)
                                        .materialBackdropSurface(default, RectangleShape),
                                )
                                Box(
                                    Modifier.size(48.dp)
                                        .materialBackdropSurface(adapted, RectangleShape),
                                )
                            }
                        }
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5000) { adaptedAppearance.sampler.appearance != null }
        assertEquals(null, defaultAppearance.sampler.appearance)
        assertEquals(Color.Green, defaultAppearance.contentColor)
    }

    @Test
    fun hostBatchKeepsDistinctLocalSamples() = runComposeUiTest {
        lateinit var redSampler: BackdropAppearanceState
        lateinit var blueSampler: BackdropAppearanceState
        lateinit var blueSamplerTwo: BackdropAppearanceState
        lateinit var blueSamplerThree: BackdropAppearanceState
        setContent {
            ProvideM3xMaterialRendering {
                val redSource = rememberBackdropLayer(Color.Red)
                val blueSource = rememberBackdropLayer(Color.Blue)
                val surface:
                    @Composable
                    (
                        BackdropLayerHandle,
                        BackdropAppearanceState,
                        Color,
                    ) -> Unit =
                    { source, sampler, tint ->
                        Box(Modifier.size(40.dp)) {
                            Box(Modifier.fillMaxSize().captureBackdrop(source))
                            Box(
                                Modifier.fillMaxSize()
                                    .onGloballyPositioned { sampler.updateCoordinates(it) }
                                    .backdropSurface(
                                        shape = RectangleShape,
                                        highlight = { Highlight.None },
                                        shadow = { Shadow.None },
                                        onDrawBackdrop = { draw, _ ->
                                            with(sampler) { drawAndRecord(draw) }
                                        },
                                        onDrawSurface = { drawRect(tint) },
                                    ),
                            )
                        }
                    }
                BackdropHost(redSource) {
                    val red = rememberBackdropAppearanceState(true, Color.White, true, 50)
                    SideEffect { redSampler = red }
                    Row {
                        surface(redSource, red, Color.Transparent)
                        BackdropHost(blueSource) {
                            val blue = rememberBackdropAppearanceState(true, Color.White, true, 50)
                            val blueTwo =
                                rememberBackdropAppearanceState(true, Color.White, true, 50)
                            val blueThree =
                                rememberBackdropAppearanceState(true, Color.White, true, 50)
                            SideEffect {
                                blueSampler = blue
                                blueSamplerTwo = blueTwo
                                blueSamplerThree = blueThree
                            }
                            Row {
                                surface(blueSource, blue, Color.Transparent)
                                surface(blueSource, blueTwo, Color.Transparent)
                                surface(blueSource, blueThree, Color.Transparent)
                            }
                        }
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5000) {
            redSampler.appearance != null &&
                blueSampler.appearance != null &&
                blueSamplerTwo.appearance != null &&
                blueSamplerThree.appearance != null
        }
        assertTrue(
            redSampler.appearance!!.relativeLuminance > blueSampler.appearance!!.relativeLuminance,
        )
        assertTrue(redSampler.appearance!!.relativeLuminance in 0.20f..0.23f)
        assertTrue(blueSampler.appearance!!.relativeLuminance in 0.06f..0.08f)
        assertTrue(blueSamplerTwo.appearance!!.relativeLuminance in 0.06f..0.08f)
        assertTrue(blueSamplerThree.appearance!!.relativeLuminance in 0.06f..0.08f)
    }
}
