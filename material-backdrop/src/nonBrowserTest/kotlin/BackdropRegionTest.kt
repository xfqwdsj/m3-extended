package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.ltfan.backdrop.BackdropMask
import top.ltfan.backdrop.BackdropRamp
import top.ltfan.backdrop.BackdropRampAnchors
import top.ltfan.m3x.backdrop.LocalBackdrop
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.compose.ui.visual.LocalBlurEnabled
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.backdrop.theme.LocalM3xBackdropTheme
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropTheme
import top.ltfan.m3x.material.backdrop.theme.ProvideBackdropMaterial
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.theme.LocalM3xThemeExtensions
import top.ltfan.m3x.material.theme.M3xThemeExtensions

/** The room a scroll edge's effects cover past the surface, which its ramp decides. */
class BackdropRegionTest {
    @Test
    fun anExtentCarriesTheMaskPastTheSurface() = runInitializedComposeUiTest {
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(surface = Color.White)) {
                    val source = rememberBackdropLayer(Color.Black)
                    Box(Modifier.size(200.dp, 300.dp).background(Color.Black).testTag("root")) {
                        Box(
                            Modifier.size(200.dp, 300.dp)
                                .captureBackdrop(source)
                                .background(Color.Black),
                        )
                        CompositionLocalProvider(LocalBackdrop provides source) {
                            ProvideBackdropMaterial(
                                MaterialSurfaceRole.ScrollEdge,
                                M3xBackdropTheme().scrollEdge.copy(ramp = ReachingRamp),
                            ) {
                                BackdropTopAppBar(
                                    {},
                                    Modifier.testTag("bar"),
                                    windowInsets = WindowInsets(0, 0, 0, 0),
                                    mask = BackdropMask(Color.White),
                                )
                            }
                        }
                    }
                }
            }
        }
        val root = onNodeWithTag("root").captureToImage().toPixelMap()

        // The surface keeps its own box: the room is drawn around it, not added to it.
        onNodeWithTag("bar").assertHeightIsEqualTo(64.dp)
        // The mask reaches past the bar and thins out over the ramp, then the extent it was asked
        // for ends.
        val below = 66
        val past = 89
        assertTrue(root[100, below].red > 0f, "just below the bar: ${root[100, below]}")
        assertTrue(
            root[100, below].red < root[100, 8].red,
            "weaker past the bar: ${root[100, below]} vs ${root[100, 8]}",
        )
        assertTrue(
            root[100, past].red < 0.02f,
            "past the extent the mask is gone: ${root[100, past]}",
        )
    }

    @Test
    fun anExtentCarriesTheMaskPastTheSurfaceWithoutABackdrop() = runInitializedComposeUiTest {
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(surface = Color.White)) {
                    Box(Modifier.size(200.dp, 300.dp).background(Color.Black).testTag("root")) {
                        ProvideBackdropMaterial(
                            MaterialSurfaceRole.ScrollEdge,
                            M3xBackdropTheme().scrollEdge.copy(ramp = ReachingRamp),
                        ) {
                            BackdropTopAppBar(
                                {},
                                Modifier.testTag("bar"),
                                windowInsets = WindowInsets(0, 0, 0, 0),
                                mask = BackdropMask(Color.White),
                            )
                        }
                    }
                }
            }
        }
        val root = onNodeWithTag("root").captureToImage().toPixelMap()

        // Without a captured source the surface draws its fallback tint, which the extent covers
        // too.
        onNodeWithTag("bar").assertHeightIsEqualTo(64.dp)
        assertTrue(root[100, 66].red > 0f, "just below the bar: ${root[100, 66]}")
        assertTrue(
            root[100, 66].red < root[100, 8].red,
            "weaker past the bar: ${root[100, 66]} vs ${root[100, 8]}",
        )
        assertTrue(
            root[100, 89].red < 0.02f,
            "past the extent the mask is gone: ${root[100, 89]}",
        )
    }

    @Test
    fun aBarRampsDownToTheMiddleOfItsContentAndFadesBelowIt() = runInitializedComposeUiTest {
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(surface = Color.White)) {
                    val source = rememberBackdropLayer(Color.Black)
                    Box(Modifier.size(200.dp, 300.dp).background(Color.Black).testTag("root")) {
                        Box(
                            Modifier.size(200.dp, 300.dp)
                                .captureBackdrop(source)
                                .background(Color.Black),
                        )
                        CompositionLocalProvider(LocalBackdrop provides source) {
                            BackdropTopAppBar(
                                {},
                                Modifier.testTag("bar"),
                                windowInsets = WindowInsets(0.dp, 24.dp, 0.dp, 0.dp),
                                mask = BackdropMask(Color.White, alpha = 1f),
                            )
                        }
                    }
                }
            }
        }
        val root = onNodeWithTag("root").captureToImage().toPixelMap()

        // Content is 64dp tall under a 24dp inset, so the ramp holds full strength down to 56, the
        // middle of that content, and is spent past the bar, at 120.
        assertEquals(1f, root[100, 20].red, 0.02f)
        assertEquals(1f, root[100, 56].red, 0.02f)
        // Below the bar's own box the fade must still be covered: the region is drawn, not clipped.
        assertTrue(root[100, 72].red in 0.05f..0.99f, "past the bar: ${root[100, 72]}")
        assertTrue(root[100, 90].red > 0.0f, "inside the fade: ${root[100, 90]}")
        assertEquals(0f, root[100, 120].red, 0.02f)
        // The bar keeps its own box: the room is drawn around it, not added to it.
        onNodeWithTag("bar").assertHeightIsEqualTo(88.dp)
    }

    @Test
    fun aThemeRampShapeMovesTheBarGeometry() = runInitializedComposeUiTest {
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(surface = Color.White)) {
                    val source = rememberBackdropLayer(Color.Black)
                    Box(Modifier.size(200.dp, 300.dp).background(Color.Black).testTag("root")) {
                        Box(
                            Modifier.size(200.dp, 300.dp)
                                .captureBackdrop(source)
                                .background(Color.Black),
                        )
                        CompositionLocalProvider(LocalBackdrop provides source) {
                            CompositionLocalProvider(
                                LocalM3xBackdropTheme provides
                                    M3xBackdropTheme().let { theme ->
                                        theme.copy(
                                            scrollEdge =
                                                theme.scrollEdge.copy(
                                                    ramp =
                                                        BackdropRamp(
                                                            anchors =
                                                                BackdropRampAnchors.offsets(
                                                                    plateau = 64.dp,
                                                                    fade = 64.dp,
                                                                ),
                                                        ),
                                                ),
                                        )
                                    },
                                LocalM3xThemeExtensions provides M3xThemeExtensions(),
                            ) {
                                BackdropTopAppBar(
                                    {},
                                    Modifier.testTag("bar"),
                                    windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                                    mask = BackdropMask(Color.White, alpha = 1f),
                                )
                            }
                        }
                    }
                }
            }
        }
        val pixels = onNodeWithTag("root").captureToImage().toPixelMap()

        // A plateau of one content height puts full strength at the bar's own edge, and a fade of
        // one
        // spends it one content height past the surface.
        assertEquals(1f, pixels[100, 0].red, 0.02f)
        assertEquals(0f, pixels[100, 128].red, 0.02f)
        onNodeWithTag("bar").assertHeightIsEqualTo(64.dp)
    }

    @Test
    fun theBlurReachesIntoTheFadeBelowTheBar() = runInitializedComposeUiTest {
        // A checkerboard page is sharp where nothing blurs it and soft where the bar's fade covers
        // it, so the local contrast of a row tells the two apart.
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(surface = Color.White)) {
                    val source = rememberBackdropLayer()
                    Box(Modifier.size(200.dp, 300.dp).background(Color.Black).testTag("root")) {
                        Box(Modifier.size(200.dp, 300.dp).captureBackdrop(source)) {
                            Column {
                                repeat(150) { row ->
                                    Row {
                                        repeat(100) { column ->
                                            Box(
                                                Modifier.size(2.dp)
                                                    .background(
                                                        if ((row + column) % 2 == 0) Color.White
                                                        else Color.Black,
                                                    ),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        CompositionLocalProvider(LocalBackdrop provides source) {
                            BackdropTopAppBar(
                                {},
                                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                                mask = null,
                            )
                        }
                    }
                }
            }
        }
        val pixels = onNodeWithTag("root").captureToImage().toPixelMap()

        fun contrast(y: Int): Float =
            (2 until pixels.width - 2).maxOf { x ->
                abs(pixels[x, y].red - pixels[x + 1, y].red)
            }

        // The fade thins the blur out as its intensity falls, so the blur reaches nothing where the
        // area the effect covers ends. A blur that kept a radius there would stop at a visible
        // line.
        val end = appBarRampEnd(height = 64f)

        val page = (200..220).maxOf { contrast(it) }
        assertTrue(page > 0.5f, "the page itself must stay sharp: $page")
        // The bar's own top rows are blurred too: the blur reads the layer it draws into, so
        // nothing
        // there may come from outside the backdrop and let the content behind show through.
        val top = (2..8).maxOf { contrast(it) }
        assertTrue(top < page * 0.4f, "the top of the bar must be blurred: top=$top")
        // The blur is spent where the ramp still has room: the rows it covers this far are soft,
        // and the last stretch of the span thins out instead.
        val band = (24..(end - 16f).toInt()).maxOf { contrast(it) }
        assertTrue(band < page * 0.4f, "the fade must blur the page: band=$band page=$page")

        // It thins out row by row: the contrast only grows as the blur spends itself, and the page
        // is sharp again inside the room the effect covers, so no row marks where it stops.
        val rows = ((end - 24f).toInt()..(end + 8f).toInt()).map { contrast(it) }
        assertTrue(
            rows.zipWithNext().all { (a, b) -> b >= a - 0.02f },
            "the blur thins out without a line: $rows",
        )
        assertTrue(rows.last() > page * 0.9f, "the page is sharp again: ${rows.last()}")
    }

    @Test
    fun aPathShapeKeepsItsOutlineWithoutABackdropSource() = runInitializedComposeUiTest {
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(surface = Color.White)) {
                    Box(Modifier.size(200.dp).background(Color.Black), Alignment.Center) {
                        BackdropSurface(
                            Modifier.size(100.dp).testTag("surface"),
                            shape = TriangleShape,
                            role = MaterialSurfaceRole.ScrollEdge,
                            effects = {},
                            mask = BackdropMask(Color.White),
                        ) {}
                    }
                }
            }
        }
        val pixels = onNodeWithTag("surface").captureToImage().toPixelMap()

        // The surface draws its fallback mask inside its own outline, corners included.
        assertTrue(
            pixels[4, 4].red < 0.1f,
            "the top-left corner, outside the path: ${pixels[4, 4]}",
        )
        // The theme's ramp fades towards the surface's own bottom, so the sample sits high up.
        assertTrue(pixels[50, 20].red > 0.1f, "inside the path: ${pixels[50, 20]}")
    }

    @Test
    fun aMaskWithoutBlurStillCoversTheRampItFadesOver() = runInitializedComposeUiTest {
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(surface = Color.White)) {
                    val source = rememberBackdropLayer(Color.Black)
                    Box(Modifier.size(200.dp, 300.dp).background(Color.Black).testTag("root")) {
                        Box(
                            Modifier.size(200.dp, 300.dp)
                                .captureBackdrop(source)
                                .background(Color.Black),
                        )
                        CompositionLocalProvider(
                            LocalBackdrop provides source,
                            LocalBlurEnabled provides false,
                        ) {
                            BackdropTopAppBar(
                                {},
                                Modifier.testTag("bar"),
                                windowInsets = WindowInsets(0, 0, 0, 0),
                                mask = BackdropMask(Color.White),
                            )
                        }
                    }
                }
            }
        }
        val root = onNodeWithTag("root").captureToImage().toPixelMap()

        // With the blur off the mask carries the separation, so it still fades over the ramp's
        // span: the layer covers the room that span reaches and the mask spends itself there.
        onNodeWithTag("bar").assertHeightIsEqualTo(64.dp)
        val end = appBarRampEnd(height = 64f)
        val justBelow = root[100, 66].red
        assertTrue(justBelow > 0.1f, "the mask reaches past the bar: $justBelow")
        assertTrue(root[100, (end - 4f).toInt()].red < justBelow, "the mask thins out")
        assertTrue(root[100, (end + 8f).toInt()].red < 0.02f, "the mask is spent past the bar")
    }

    @Test
    fun aGlassSurfaceUnderTheBarStaysItselfAndIsOnlyBlurred() = runInitializedComposeUiTest {
        // A page captures its content; a preview inside it captures its own artwork; a control of
        // that preview samples the preview. The bar samples the page, so its effect layer holds the
        // control as the page drew it.
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(surface = Color.White)) {
                    val page = rememberBackdropLayer()
                    val preview = rememberBackdropLayer()
                    Box(Modifier.size(200.dp, 300.dp).background(Color.Black).testTag("root")) {
                        Box(Modifier.size(200.dp, 300.dp).captureBackdrop(page)) {
                            Box(Modifier.size(200.dp, 300.dp)) {
                                Column {
                                    repeat(150) { row ->
                                        Row {
                                            repeat(100) { column ->
                                                Box(
                                                    Modifier.size(2.dp)
                                                        .background(
                                                            if ((row + column) % 2 == 0) Color.White
                                                            else Color.Black,
                                                        ),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            CompositionLocalProvider(LocalBackdrop provides preview) {
                                Box(Modifier.size(200.dp, 300.dp)) {
                                    Box(
                                        Modifier.size(200.dp, 300.dp)
                                            .captureBackdrop(preview)
                                            .background(Color(0xFF2E7D32)),
                                    )
                                    BackdropSurface(
                                        Modifier.offset(y = 74.dp)
                                            .size(80.dp, 40.dp)
                                            .testTag("button"),
                                        shape = RoundedCornerShape(percent = 50),
                                        role = MaterialSurfaceRole.Control,
                                        effects = {},
                                    ) {}
                                }
                            }
                        }
                        CompositionLocalProvider(LocalBackdrop provides page) {
                            BackdropTopAppBar(
                                {},
                                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                                mask = null,
                            )
                        }
                    }
                }
            }
        }
        val pixels = onNodeWithTag("root").captureToImage().toPixelMap()
        fun contrast(y: Int): Float =
            (2 until pixels.width - 2).maxOf { x ->
                abs(pixels[x, y].red - pixels[x + 1, y].red)
            }

        // The control's own pixels reach the effect layer, so the bar draws them rather than what
        // lies behind: the column through it holds one value inside the fade.
        val inside = (82..86).map { pixels[8, it].red }
        assertTrue(
            inside.maxOf { it } - inside.minOf { it } < 0.02f,
            "the control keeps its own fill under the bar: $inside",
        )
        // The page above the control is blurred, and the blur thins out smoothly over the fade
        // rather than stopping at a line.
        val page = contrast(66)
        assertTrue(page < 0.05f, "the page under the bar is blurred: $page")
        val rows = (72..88).map { contrast(it) }
        val step = rows.zipWithNext().maxOf { (a, b) -> abs(a - b) }
        assertTrue(step < 0.25f, "the fade thins out smoothly: $rows")
    }
}

/** A shape whose outline is a path, the kind a continuous-corner shape produces. */
private val TriangleShape =
    object : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density,
        ): Outline =
            Outline.Generic(
                Path().apply {
                    moveTo(size.width / 2f, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                },
            )
    }

/** A ramp that keeps the bar's own content at full strength and reaches 20dp past the bar. */
private val ReachingRamp: BackdropRamp =
    BackdropRamp(anchors = BackdropRampAnchors.offsets(plateau = 24.dp, fade = 20.dp))
