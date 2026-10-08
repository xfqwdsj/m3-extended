package top.ltfan.m3x.material.backdrop

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.ltfan.backdrop.BackdropMask
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.animation.LocalBlurAnimationEnabled
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.compose.ui.accessibility.AccessibilitySettings
import top.ltfan.m3x.compose.ui.accessibility.LocalAccessibilitySettings
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering

/**
 * The bar's mask and blur follow the scroll state its behavior reports, so the glass arrives with
 * the content that scrolls under the bar and leaves when that content returns to its start.
 */
class BackdropScrollPresenceTest {
    @Test
    fun theBarShowsItsGlassOnceContentMovesUnderIt() = runInitializedComposeUiTest {
        lateinit var behavior: TopAppBarScrollBehavior
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(surface = Color.White)) {
                    behavior = TopAppBarDefaults.pinnedScrollBehavior()
                    val source = rememberBackdropLayer()
                    Box(Modifier.size(200.dp, 300.dp).background(Color.Black).testTag("root")) {
                        Box(Modifier.fillMaxSize().captureBackdrop(source).background(Color.Black))
                        BackdropHost(source) {
                            Bar(behavior, Modifier.testTag("bar"))
                        }
                    }
                }
            }
        }
        val atRest = onNodeWithTag("root").captureToImage().toPixelMap()
        assertEquals(0f, atRest[100, 32].red, 0.02f, "the page shows through the bar at rest")

        // The bar publishes its height as the offset limit, so content that has moved a bar
        // height under it reports a full overlap.
        behavior.state.contentOffset = -80f
        waitForIdle()
        val scrolled = onNodeWithTag("root").captureToImage().toPixelMap()
        assertTrue(scrolled[100, 32].red > 0.5f, "the mask arrives: ${scrolled[100, 32]}")

        // Content back at its start takes the glass with it.
        behavior.state.contentOffset = 0f
        waitForIdle()
        val back = onNodeWithTag("root").captureToImage().toPixelMap()
        assertEquals(0f, back[100, 32].red, 0.02f, "the mask leaves: ${back[100, 32]}")
    }

    @Test
    fun theBlurArrivesWithTheGlass() = runInitializedComposeUiTest {
        lateinit var behavior: TopAppBarScrollBehavior
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(surface = Color.White)) {
                    behavior = TopAppBarDefaults.pinnedScrollBehavior()
                    val source = rememberBackdropLayer()
                    Box(Modifier.size(200.dp, 120.dp).background(Color.Black).testTag("root")) {
                        Box(Modifier.size(200.dp, 120.dp).captureBackdrop(source)) {
                            Checkerboard()
                        }
                        BackdropHost(source) { Bar(behavior) }
                    }
                }
            }
        }
        val atRest = onNodeWithTag("root").captureToImage().toPixelMap()
        assertEquals(1f, contrast(atRest), 0.02f, "the bar shows the page sharp at rest")

        behavior.state.contentOffset = -80f
        waitForIdle()
        val scrolled = onNodeWithTag("root").captureToImage().toPixelMap()
        assertTrue(contrast(scrolled) < 0.6f, "the blur arrives: ${contrast(scrolled)}")
    }

    @Test
    fun disablingBlurMotionCrossfadesTheFullStrengthBackground() = verifyFixedBackground(false)

    @Test fun reducedMotionCrossfadesTheFullStrengthBackground() = verifyFixedBackground(true)

    private fun verifyFixedBackground(reducedMotion: Boolean) = runInitializedComposeUiTest {
        val animateBlur = mutableStateOf(reducedMotion)
        val reduceMotion = mutableStateOf(reducedMotion)
        lateinit var behavior: TopAppBarScrollBehavior
        val motion =
            object : MotionScheme by MotionScheme.standard() {
                override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(motionScheme = motion) {
                    CompositionLocalProvider(
                        LocalBlurAnimationEnabled provides animateBlur.value,
                        LocalAccessibilitySettings provides
                            AccessibilitySettings(reduceMotion = reduceMotion.value),
                    ) {
                        behavior = TopAppBarDefaults.pinnedScrollBehavior()
                        val source = rememberBackdropLayer()
                        Box(Modifier.size(200.dp, 120.dp).testTag("root")) {
                            Box(Modifier.size(200.dp, 120.dp).captureBackdrop(source)) {
                                Checkerboard()
                            }
                            BackdropHost(source) {
                                BackdropTopAppBar(
                                    {},
                                    windowInsets = WindowInsets(0, 0, 0, 0),
                                    scrollBehavior = behavior,
                                    mask = BackdropMask(Color.White, alpha = 0f),
                                )
                            }
                        }
                    }
                }
            }
        }
        mainClock.autoAdvance = false
        fun image() = onNodeWithTag("root").captureToImage().toPixelMap()
        mainClock.advanceTimeBy(1200)
        val absent = contrast(image())
        runOnIdle { behavior.state.contentOffset = -80f }
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeBy(500)
        val faded = contrast(image())
        mainClock.advanceTimeBy(1200)
        val full = contrast(image())
        assertTrue(
            abs(faded - (absent + full) / 2f) < 0.08f,
            "Fallback must blend the fixed full-strength blur: absent=$absent half=$faded full=$full",
        )
        runOnIdle {
            behavior.state.contentOffset = 0f
            animateBlur.value = true
            reduceMotion.value = false
        }
        mainClock.advanceTimeBy(1200)
        runOnIdle { behavior.state.contentOffset = -80f }
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeBy(500)
        val dynamic = contrast(image())
        assertTrue(
            dynamic < faded - 0.15f,
            "Enabled finite-radius motion must not also fade the background: dynamic=$dynamic faded=$faded",
        )
    }

    @Test
    fun aBarWithoutAScrollBehaviorKeepsItsGlass() = runInitializedComposeUiTest {
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(surface = Color.White)) {
                    val source = rememberBackdropLayer()
                    Box(Modifier.size(200.dp, 300.dp).background(Color.Black).testTag("root")) {
                        Box(Modifier.fillMaxSize().captureBackdrop(source).background(Color.Black))
                        BackdropHost(source) { Bar(scrollBehavior = null) }
                    }
                }
            }
        }
        val pixels = onNodeWithTag("root").captureToImage().toPixelMap()
        assertTrue(pixels[100, 32].red > 0.5f, "the mask stays: ${pixels[100, 32]}")
    }

    @Test
    fun thePresenceSpecNeverOvershoots() = runInitializedComposeUiTest {
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(motionScheme = MotionScheme.standard()) {
                    assertNoBounce(scrollEdgePresenceSpec())
                }
                MaterialTheme(motionScheme = MotionScheme.expressive()) {
                    assertNoBounce(scrollEdgePresenceSpec())
                }
                MaterialTheme {
                    assertNoBounce(scrollEdgePresenceSpec())
                }
            }
        }
    }

    private fun assertNoBounce(spec: FiniteAnimationSpec<Float>) {
        val spring = spec as? SpringSpec<Float>
        assertTrue(
            spring == null || spring.dampingRatio >= Spring.DampingRatioNoBouncy,
            "an effect spec must settle without overshooting: $spec",
        )
    }

    private fun contrast(pixels: PixelMap): Float =
        (2 until pixels.width - 2).maxOf { x -> abs(pixels[x, 32].red - pixels[x + 1, 32].red) }

    @Composable
    private fun Bar(
        scrollBehavior: TopAppBarScrollBehavior?,
        modifier: Modifier = Modifier,
    ) {
        BackdropTopAppBar(
            {},
            modifier,
            windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
            scrollBehavior = scrollBehavior,
            mask = BackdropMask(Color.White, alpha = 1f),
        )
    }

    @Composable
    private fun Checkerboard() {
        Column {
            repeat(60) { row ->
                Row {
                    repeat(100) { column ->
                        Box(
                            Modifier.size(2.dp)
                                .background(
                                    if ((row + column) % 2 == 0) Color.White else Color.Black,
                                ),
                        )
                    }
                }
            }
        }
    }
}
