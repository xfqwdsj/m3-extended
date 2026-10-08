package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.backdrop.BackdropCurve
import top.ltfan.backdrop.BackdropMask
import top.ltfan.backdrop.BackdropRamp
import top.ltfan.m3x.backdrop.LocalBackdrop
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.compose.ui.visual.LocalBlurEnabled
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropTheme
import top.ltfan.m3x.material.backdrop.theme.ProvideBackdropMaterial
import top.ltfan.m3x.material.backdrop.theme.backdrop
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.theme.extended

class BackdropMaskTest {
    /** A straight curve, so a test can predict the ramp by hand. */
    private val linearCurve = BackdropCurve.Linear

    @Test
    fun actionCapsuleSamplesTheExportedBarAtItsOwnPosition() = runInitializedComposeUiTest {
        val background = mutableStateOf(Color.Blue)
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(surface = Color.White)) {
                    val source = rememberBackdropLayer()
                    Box(Modifier.size(320.dp, 180.dp)) {
                        Box(
                            Modifier.fillMaxSize()
                                .captureBackdrop(source)
                                .background(background.value),
                        )
                        Box(Modifier.fillMaxSize().background(Color.Green))
                        CompositionLocalProvider(LocalBackdrop provides source) {
                            BackdropTopAppBar(
                                {},
                                windowInsets = WindowInsets(0, 0, 0, 0),
                                actions = {
                                    BackdropTopAppBarControl(Modifier.testTag("capsule")) {
                                        Box(Modifier.size(48.dp))
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
        val sampled = mutableMapOf<Color, Color>()
        for (color in listOf(Color.Blue, Color.Red)) {
            runOnIdle { background.value = color }
            waitForIdle()
            val pixels = onNodeWithTag("capsule").captureToImage().toPixelMap()
            sampled[color] = pixels[pixels.width / 2, pixels.height / 2]
        }
        val blue = sampled.getValue(Color.Blue)
        val red = sampled.getValue(Color.Red)
        // The bar's mask covers most of the capsule, so the page color shows through as a tint:
        // enough to tell the two pages apart, and never the green backing behind them.
        assertTrue(
            blue.blue > red.blue && red.red > blue.red,
            "Capsule must follow the captured page color: blue=$blue red=$red",
        )
        assertTrue(
            blue.green <= maxOf(blue.red, blue.blue) + 0.02f &&
                red.green <= maxOf(red.red, red.blue) + 0.02f,
            "Capsule must not sample the green backing: blue=$blue red=$red",
        )
    }

    @Test
    fun fourBarsKeepTheBlurCurveWithBlurOffAndWithoutSource() = runInitializedComposeUiTest {
        val variant = mutableIntStateOf(0)
        val mode = mutableIntStateOf(0)
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(surface = Color.White)) {
                    val source = rememberBackdropLayer(Color.Black)
                    Box(Modifier.size(240.dp, 280.dp).background(Color.Black)) {
                        Box(Modifier.fillMaxSize().captureBackdrop(source).background(Color.Black))
                        CompositionLocalProvider(
                            LocalBackdrop provides if (mode.intValue == 2) null else source,
                        ) {
                            CompositionLocalProvider(
                                LocalBlurEnabled provides (mode.intValue == 0),
                            ) {
                                val modifier = Modifier.testTag("bar")
                                val insets = WindowInsets(0, 0, 0, 0)
                                when (variant.intValue) {
                                    0 ->
                                        BackdropTopAppBar(
                                            {},
                                            modifier,
                                            windowInsets = insets,
                                            mask =
                                                BackdropMask(
                                                    Color.White,
                                                    alpha = 0.6f,
                                                ),
                                        )
                                    1 ->
                                        BackdropCenterAlignedTopAppBar(
                                            {},
                                            modifier,
                                            windowInsets = insets,
                                            mask =
                                                BackdropMask(
                                                    Color.White,
                                                    alpha = 0.6f,
                                                ),
                                        )
                                    2 ->
                                        BackdropMediumFlexibleTopAppBar(
                                            {},
                                            modifier,
                                            windowInsets = insets,
                                            mask =
                                                BackdropMask(
                                                    Color.White,
                                                    alpha = 0.6f,
                                                ),
                                        )
                                    else ->
                                        BackdropLargeFlexibleTopAppBar(
                                            {},
                                            modifier,
                                            windowInsets = insets,
                                            mask =
                                                BackdropMask(
                                                    Color.White,
                                                    alpha = 0.6f,
                                                ),
                                        )
                                }
                            }
                        }
                    }
                }
            }
        }
        for (bar in 0..3) for (path in 0..2) {
            runOnIdle {
                variant.intValue = bar
                mode.intValue = path
            }
            waitForIdle()
            val pixels = onNodeWithTag("bar").captureToImage().toPixelMap()
            for (fraction in listOf(0f, 0.25f, 0.5f, 0.75f, 0.99f)) {
                val y = (pixels.height * fraction).toInt().coerceAtMost(pixels.height - 1)
                val expected = 0.6f * appBarRampIntensity(y + 0.5f, pixels.height.toFloat())
                val actual = pixels[pixels.width / 2, y].red
                assertTrue(
                    abs(expected - actual) < 0.025f,
                    "bar=$bar path=$path y=$y expected=$expected actual=$actual",
                )
            }
        }
    }

    @Test
    fun maskBrushPaintsThePageBackgroundThroughTheSameRamp() = runInitializedComposeUiTest {
        // The page is pure blue and the mask paints red, so the two contributions stay separable.
        val background = Brush.verticalGradient(listOf(Color.Red, Color.Red))
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(surface = Color.White)) {
                    Box(Modifier.size(240.dp, 200.dp).background(Color.Blue)) {
                        BackdropTopAppBar(
                            {},
                            Modifier.testTag("bar"),
                            windowInsets = WindowInsets(0, 0, 0, 0),
                            mask = BackdropMask(brush = background, alpha = 1f),
                        )
                    }
                }
            }
        }
        val pixels = onNodeWithTag("bar").captureToImage().toPixelMap()
        for (fraction in listOf(0.0f, 0.25f, 0.5f, 0.75f, 0.99f)) {
            val y = (pixels.height * fraction).toInt().coerceAtMost(pixels.height - 1)
            val strength = appBarRampIntensity(y + 0.5f, pixels.height.toFloat())
            val actual = pixels[pixels.width / 2, y]
            // The mask keeps its maximum alpha while its colors come from the caller's brush, so
            // the sampled pixel is the brush over the page at the ramp's alpha.
            assertTrue(
                abs(actual.red - strength) < 0.03f,
                "fraction=$fraction expected red=$strength actual=$actual",
            )
            assertTrue(
                abs(actual.blue - (1f - strength)) < 0.03f,
                "fraction=$fraction expected blue=${1f - strength} actual=$actual",
            )
        }
    }

    @Test
    fun themeRampMovesTheMaskAndItsBlurTogether() = runInitializedComposeUiTest {
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(surface = Color.White)) {
                    // A black page keeps the sampled pixel equal to the mask's own alpha.
                    Box(Modifier.size(240.dp, 280.dp).background(Color.Black)) {
                        ProvideBackdropMaterial(
                            MaterialSurfaceRole.ScrollEdge,
                            M3xBackdropTheme()
                                .scrollEdge
                                .copy(ramp = BackdropRamp(curve = linearCurve)),
                        ) {
                            BackdropTopAppBar(
                                {},
                                Modifier.testTag("bar"),
                                windowInsets = WindowInsets(0, 0, 0, 0),
                                mask = BackdropMask(Color.White, alpha = 0.6f),
                            )
                        }
                    }
                }
            }
        }
        val pixels = onNodeWithTag("bar").captureToImage().toPixelMap()
        for (fraction in listOf(0.0f, 0.25f, 0.5f, 0.75f, 0.99f)) {
            val y = (pixels.height * fraction).toInt().coerceAtMost(pixels.height - 1)
            val expected =
                0.6f *
                    appBarRampIntensity(
                        y + 0.5f,
                        pixels.height.toFloat(),
                        ramp = BackdropRamp(curve = linearCurve),
                    )
            val actual = pixels[pixels.width / 2, y].red
            assertTrue(
                abs(expected - actual) < 0.025f,
                "fraction=$fraction expected=$expected actual=$actual",
            )
        }
    }

    @Test
    fun disabledBlurHandsTheSeparationToTheMask() = runInitializedComposeUiTest {
        val blurEnabled = mutableStateOf(true)
        var tint = 0f
        var fallback = 0f
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(surface = Color.White)) {
                    val source = rememberBackdropLayer(Color.Black)
                    val scrollEdge = MaterialTheme.extended.backdrop.scrollEdge
                    SideEffect {
                        tint = scrollEdge.tintAlpha
                        fallback = maxOf(scrollEdge.tintAlpha, scrollEdge.fallbackAlpha)
                    }
                    Box(Modifier.size(240.dp, 280.dp).background(Color.Black)) {
                        Box(Modifier.fillMaxSize().captureBackdrop(source).background(Color.Black))
                        CompositionLocalProvider(
                            LocalBackdrop provides source,
                            LocalBlurEnabled provides blurEnabled.value,
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
        // With blur on, the theme's sampled tint applies; with it off, the mask has to carry the
        // whole separation and takes the theme's stronger fallback alpha.
        val expected = listOf(tint, fallback)
        for ((index, blur) in listOf(true, false).withIndex()) {
            runOnIdle { blurEnabled.value = blur }
            waitForIdle()
            val pixels = onNodeWithTag("bar").captureToImage().toPixelMap()
            val actual = pixels[pixels.width / 2, 0].red
            assertTrue(
                abs(expected[index] - actual) < 0.03f,
                "blur=$blur expected=${expected[index]} actual=$actual",
            )
        }
    }

    @Test
    fun progressiveBlurOfARoleDoesNotCarryAMask() = runInitializedComposeUiTest {
        var fallback = 0f
        var ramp = BackdropRamp()
        // The scroll edge role chooses the progressive blur; masking stays an explicit request, so
        // the same role draws a flat tint beside the ramped one until a mask asks for the ramp.
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(surface = Color.White)) {
                    Box(Modifier.size(200.dp, 240.dp).background(Color.Black)) {
                        val scrollEdge = MaterialTheme.extended.backdrop.scrollEdge
                        SideEffect {
                            fallback = scrollEdge.fallbackAlpha
                            ramp = scrollEdge.ramp
                        }
                        Row {
                            BackdropSurface(
                                Modifier.size(100.dp, 200.dp).testTag("flat"),
                                shape = RectangleShape,
                                role = MaterialSurfaceRole.ScrollEdge,
                                effects = {},
                            ) {}
                            BackdropSurface(
                                Modifier.size(100.dp, 200.dp).testTag("masked"),
                                shape = RectangleShape,
                                role = MaterialSurfaceRole.ScrollEdge,
                                effects = {},
                                mask = BackdropMask(),
                            ) {}
                        }
                    }
                }
            }
        }
        val flat = onNodeWithTag("flat").captureToImage().toPixelMap()
        val masked = onNodeWithTag("masked").captureToImage().toPixelMap()
        // The mask follows the material's ramp: 0 on the surface's content side, which for a top
        // edge
        // is its bottom, growing towards its own far edge.
        val axis = axisOf(masked.height.toFloat())
        val span = ramp.span(axis)
        for (fraction in listOf(0.0f, 0.25f, 0.5f, 0.75f, 0.99f)) {
            val y = (flat.height * fraction).toInt().coerceAtMost(flat.height - 1)
            val flatAlpha = flat[flat.width / 2, y].red
            val maskedAlpha = masked[masked.width / 2, y].red
            assertTrue(
                abs(flatAlpha - fallback) < 0.03f,
                "fraction=$fraction a surface without a mask must stay flat: $flatAlpha",
            )
            val position =
                ((masked.height - (y + 0.5f)) - span.start) / (span.endInclusive - span.start)
            val expected = fallback * ramp.intensityAt(position.coerceIn(0f, 1f))
            assertTrue(
                abs(maskedAlpha - expected) < 0.03f,
                "fraction=$fraction the requested mask must ramp: $maskedAlpha vs $expected",
            )
        }
    }

    @Test
    fun maskRampOverridesTheMaterialRampForTheMaskAlone() = runInitializedComposeUiTest {
        var fallback = 0f
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(surface = Color.White)) {
                    Box(Modifier.size(240.dp, 280.dp).background(Color.Black)) {
                        val scrollEdge = MaterialTheme.extended.backdrop.scrollEdge
                        SideEffect {
                            fallback = scrollEdge.fallbackAlpha
                        }
                        Row {
                            BackdropSurface(
                                Modifier.size(120.dp, 200.dp).testTag("theme"),
                                shape = RectangleShape,
                                role = MaterialSurfaceRole.ScrollEdge,
                                effects = {},
                                mask = BackdropMask(Color.White),
                            ) {}
                            BackdropSurface(
                                Modifier.size(120.dp, 200.dp).testTag("override"),
                                shape = RectangleShape,
                                role = MaterialSurfaceRole.ScrollEdge,
                                effects = {},
                                mask =
                                    BackdropMask(
                                        Color.White,
                                        ramp = BackdropRamp(curve = linearCurve),
                                    ),
                            ) {}
                        }
                    }
                }
            }
        }
        val theme = onNodeWithTag("theme").captureToImage().toPixelMap()
        val override = onNodeWithTag("override").captureToImage().toPixelMap()
        for (fraction in listOf(0.25f, 0.5f, 0.75f)) {
            val y = (theme.height * fraction).toInt().coerceAtMost(theme.height - 1)
            val position = (y + 0.5f) / theme.height
            val expected =
                fallback *
                    BackdropRamp(curve = linearCurve).intensityAt((1f - position).coerceIn(0f, 1f))
            val actual = override[override.width / 2, y].red
            assertTrue(
                abs(expected - actual) < 0.03f,
                "fraction=$fraction expected=$expected actual=$actual",
            )
            // Whatever shape the theme's curve has, the mask's own ramp is what it paints.
            val differs =
                (0 until theme.height step 4).any { row ->
                    abs(override[override.width / 2, row].red - theme[theme.width / 2, row].red) >
                        0.02f
                }
            assertTrue(differs, "the mask's own ramp must replace the material's")
        }
    }
}
