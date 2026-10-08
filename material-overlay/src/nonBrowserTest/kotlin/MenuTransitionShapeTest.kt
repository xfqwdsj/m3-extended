package top.ltfan.m3x.material.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.LocalMaterialSurfaceRenderer
import top.ltfan.m3x.material.MaterialSurfaceAppearance
import top.ltfan.m3x.material.MaterialSurfaceEdge
import top.ltfan.m3x.material.MaterialSurfaceEffects
import top.ltfan.m3x.material.MaterialSurfaceRenderer
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.MaterialSurfaceStyle
import top.ltfan.m3x.material.PlainMaterialSurfaceRenderer
import top.ltfan.m3x.overlay.LocalOverlayAnimatedVisibilityScope
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState

class MenuTransitionShapeTest {
    @Test
    fun normalizedThresholdRepresentsOnePixelAcrossDensitiesAndCornerRanges() {
        val collapsed = RoundedCornerShape(28.dp)
        val expanded = RoundedCornerShape(4.dp)
        assertEquals(
            1f / 24f,
            menuCornerProgressThreshold(collapsed, expanded, Size(200f, 200f), Density(1f)),
        )
        assertEquals(
            1f / 48f,
            menuCornerProgressThreshold(collapsed, expanded, Size(200f, 200f), Density(2f)),
        )
        assertEquals(
            1f / 72f,
            menuCornerProgressThreshold(
                AbsoluteRoundedCornerShape(28.dp, 76.dp, 16.dp, 8.dp),
                expanded,
                Size(200f, 200f),
                Density(1f),
            ),
        )
    }

    @Test
    fun cornerInterpolationPreservesPhysicalCornersAndClampsProgress() {
        var progress = 0f
        val shape =
            MenuTransitionShape(
                AbsoluteRoundedCornerShape(4.dp, 8.dp, 12.dp, 16.dp),
                AbsoluteRoundedCornerShape(20.dp, 24.dp, 28.dp, 32.dp),
            ) {
                progress
            }
        for (direction in LayoutDirection.entries) {
            progress = 0.5f
            val outline =
                shape.createOutline(Size(200f, 200f), direction, Density(1f)) as Outline.Rounded
            assertEquals(12f, outline.roundRect.topLeftCornerRadius.x)
            assertEquals(16f, outline.roundRect.topRightCornerRadius.x)
            progress = -0.2f
            assertEquals(4f, shape.radius(direction))
            progress = 1.2f
            assertEquals(20f, shape.radius(direction))
        }
    }

    @Test
    fun cornersFollowVisibilityEffectsMotionAndReverseContinuously() = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        var visible by mutableStateOf(false)
        var animated: Shape? = null
        val motion =
            object : MotionScheme by MotionScheme.expressive() {
                override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> =
                    tween(1_000, easing = LinearEasing)
            }
        setContent {
            MaterialTheme(motionScheme = motion) {
                AnimatedVisibility(visible) {
                    CompositionLocalProvider(LocalOverlayAnimatedVisibilityScope provides this) {
                        val shape =
                            rememberMenuTransitionShape(
                                RoundedCornerShape(28.dp),
                                RoundedCornerShape(4.dp),
                                Size(200f, 200f),
                            )
                        SideEffect { animated = shape }
                    }
                }
            }
        }
        runOnIdle { visible = true }
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeBy(256)
        val opening = animated!!.radius()
        assertTrue(
            opening in 20f..24f,
            "The slow effects spec must control corner progress: $opening",
        )
        runOnIdle { visible = false }
        mainClock.advanceTimeByFrame()
        val reversed = animated!!.radius()
        assertTrue(
            kotlin.math.abs(reversed - opening) < 1f,
            "Reversal must preserve the current radius",
        )
        mainClock.advanceTimeBy(256)
        assertTrue(animated!!.radius() > reversed, "Closing must return toward the button radius")
        mainClock.advanceTimeBy(1_100)
        runOnIdle { visible = true }
        mainClock.advanceTimeBy(1_100)
        assertEquals(4f, animated!!.radius())
    }

    @Test
    fun expressiveCornersSettleWithoutAnImperceptibleTailOrBounce() = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        var visible by mutableStateOf(false)
        var animated: Shape? = null
        setContent {
            MaterialTheme(motionScheme = MotionScheme.expressive()) {
                AnimatedVisibility(visible) {
                    CompositionLocalProvider(LocalOverlayAnimatedVisibilityScope provides this) {
                        val shape =
                            rememberMenuTransitionShape(
                                RoundedCornerShape(28.dp),
                                RoundedCornerShape(4.dp),
                                Size(200f, 200f),
                            )
                        SideEffect { animated = shape }
                    }
                }
            }
        }
        runOnIdle { visible = true }
        mainClock.advanceTimeByFrame()
        var previous = 28f
        repeat(16) {
            mainClock.advanceTimeBy(16)
            val radius = animated!!.radius()
            assertTrue(
                radius in 4f..previous,
                "Corners must converge without bounce: $previous -> $radius",
            )
            previous = radius
        }
        assertEquals(4f, previous, "A subpixel corner tail must not keep the transition running")
        mainClock.advanceTimeBy(400)
        assertEquals(4f, animated!!.radius())
    }

    @Test
    fun fabMenuUsesButtonCornersBeforeSettlingAtMenuCorners() = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        var expanded by mutableStateOf(false)
        var panelShape: Shape? = null
        val renderer =
            object : MaterialSurfaceRenderer by PlainMaterialSurfaceRenderer {
                @Composable
                override fun appearance(
                    role: MaterialSurfaceRole,
                    style: MaterialSurfaceStyle?,
                    containerColor: Color,
                    contentColor: Color,
                    effects: MaterialSurfaceEffects?,
                    shape: Shape,
                    excludedEdges: Set<MaterialSurfaceEdge>,
                ): MaterialSurfaceAppearance {
                    if (role == MaterialSurfaceRole.Panel) panelShape = shape
                    return PlainMaterialSurfaceRenderer.appearance(
                        role,
                        style,
                        containerColor,
                        contentColor,
                        effects,
                        shape,
                        excludedEdges,
                    )
                }
            }
        val motion =
            object : MotionScheme by MotionScheme.expressive() {
                override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> =
                    tween(1_000, easing = LinearEasing)
            }
        setContent {
            MaterialTheme(motionScheme = motion) {
                CompositionLocalProvider(LocalMaterialSurfaceRenderer provides renderer) {
                    OverlayHost(remember { OverlayHostState() }) {
                        MenuFloatingActionButton(
                            expanded = { expanded },
                            onExpandedChange = { expanded = it },
                            shape = RoundedCornerShape(28.dp),
                            menuShape = RoundedCornerShape(4.dp),
                            buttonContent = { Box(Modifier.size(24.dp)) },
                            menuContent = { Box(Modifier.size(160.dp)) },
                        )
                    }
                }
            }
        }
        mainClock.advanceTimeBy(1_100)
        runOnIdle { expanded = true }
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeBy(256)
        assertTrue(panelShape!!.radius() in 20f..24f)
        mainClock.advanceTimeBy(1_100)
        assertEquals(4f, panelShape!!.radius())
        runOnIdle { expanded = false }
        mainClock.advanceTimeBy(256)
        assertTrue(panelShape!!.radius() in 8f..12f)
    }

    private fun Shape.radius(direction: LayoutDirection = LayoutDirection.Ltr): Float =
        (createOutline(Size(200f, 200f), direction, Density(1f)) as Outline.Rounded)
            .roundRect
            .topLeftCornerRadius
            .x
}
