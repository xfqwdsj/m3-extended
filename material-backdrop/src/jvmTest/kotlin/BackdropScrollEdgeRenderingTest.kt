package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.backdrop.BackdropBlur
import top.ltfan.backdrop.BackdropEdge
import top.ltfan.backdrop.BackdropFill
import top.ltfan.backdrop.BackdropRamp
import top.ltfan.backdrop.BackdropRampAnchors
import top.ltfan.backdrop.BackdropStyle
import top.ltfan.backdrop.drawBackdrop
import top.ltfan.backdrop.highlight.Highlight
import top.ltfan.backdrop.highlight.HighlightStyle
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropTheme
import top.ltfan.m3x.material.backdrop.theme.ProvideBackdropTheme

class BackdropScrollEdgeRenderingTest {
    @Test
    fun hiddenEdgeLeavesTranslucentSourceUnchanged() = runComposeUiTest {
        val drawEdge = mutableStateOf(false)
        setContent {
            MaterialTheme {
                val source = rememberBackdropLayer()
                BackdropHost(source) {
                    Box(Modifier.size(120.dp, 56.dp).background(Color.Black).testTag("scene")) {
                        Canvas(Modifier.size(120.dp, 56.dp).captureBackdrop(source)) {
                            drawRect(Color.Red.copy(alpha = .3f))
                        }
                        if (drawEdge.value)
                            Box(
                                Modifier.size(24.dp, 56.dp)
                                    .backdropScrollEdge(
                                        edge = BackdropEdge.Start,
                                        visible = { false },
                                        fill = BackdropFill.None,
                                    ),
                            )
                    }
                }
            }
        }
        val before = onNodeWithTag("scene").captureToImage().toPixelMap()[8, 28]
        runOnIdle { drawEdge.value = true }
        val after = onNodeWithTag("scene").captureToImage().toPixelMap()[8, 28]
        assertTrue(
            abs(before.red - after.red) < .01f,
            "A hidden edge must not redraw its translucent source: $before vs $after",
        )
    }

    @Test
    fun verticalEdgeKeepsItsOutsetBlurWhenExportingItsBackground() = runComposeUiTest {
        setContent {
            MaterialTheme {
                val page = rememberBackdropLayer()
                val exported = rememberBackdropLayer()
                BackdropHost(page) {
                    Box(Modifier.size(128.dp, 160.dp).testTag("scene")) {
                        Canvas(Modifier.size(128.dp, 160.dp).captureBackdrop(page)) {
                            drawRect(Color.White)
                            for (x in 0 until size.width.toInt() step 4) drawRect(
                                Color.Black,
                                Offset(x.toFloat(), 0f),
                                Size(2f, size.height),
                            )
                        }
                        Box(
                            Modifier.size(128.dp, 64.dp)
                                .testTag("vertical-edge")
                                .backdropScrollEdge(
                                    material =
                                        M3xBackdropMaterial(
                                            style =
                                                BackdropStyle.NoEffects {
                                                    blur = { BackdropBlur.Uniform(12.dp) }
                                                },
                                            ramp =
                                                BackdropRamp(
                                                    anchors =
                                                        BackdropRampAnchors.offsets(24.dp, 40.dp),
                                                ),
                                        ),
                                    fill = BackdropFill.None,
                                    exportedBackdrop = exported.layer,
                                ),
                        )
                    }
                }
            }
        }
        onNodeWithTag("vertical-edge").assertHeightIsEqualTo(64.dp)
        val pixels = onNodeWithTag("scene").captureToImage().toPixelMap()
        fun contrast(y: Int): Float =
            (24 until 104).map { pixels[it, y].red }.let { it.max() - it.min() }
        assertTrue(contrast(16) < .1f, "The container has full blur")
        assertTrue(contrast(72) < .2f, "Blur continues outside the 64px container")
        assertTrue(contrast(112) > .9f, "The page is clear past the 40px external fade")
    }

    @Test
    fun finiteStripNeedsClampedSourceToKeepSharpStripesFromShowingThrough() {
        for (edge in listOf(BackdropEdge.Start, BackdropEdge.End)) {
            val jumps = mutableMapOf<TileMode, Float>()
            for (tile in listOf(TileMode.Decal, TileMode.Clamp)) runComposeUiTest {
                setContent {
                    MaterialTheme {
                        val source = rememberBackdropLayer(tileMode = tile)
                        BackdropHost(source) {
                            Box(Modifier.size(120.dp, 56.dp)) {
                                Canvas(Modifier.size(120.dp, 56.dp).captureBackdrop(source)) {
                                    drawRect(Color.White)
                                    for (x in 0 until size.width.toInt() step 8) drawRect(
                                        Color.Black,
                                        Offset(x.toFloat(), 0f),
                                        Size(4f, size.height),
                                    )
                                }
                                Box(
                                    Modifier.align(
                                            if (edge == BackdropEdge.Start) Alignment.TopStart
                                            else Alignment.TopEnd,
                                        )
                                        .size(24.dp, 56.dp)
                                        .testTag("edge")
                                        .backdropScrollEdge(
                                            edge = edge,
                                            material =
                                                M3xBackdropMaterial(
                                                    style =
                                                        BackdropStyle.NoEffects {
                                                            blur = { BackdropBlur.Uniform(12.dp) }
                                                        },
                                                    ramp = BackdropRamp(),
                                                ),
                                            fill = BackdropFill.None,
                                        ),
                                )
                            }
                        }
                    }
                }
                val pixels = onNodeWithTag("edge").captureToImage().toPixelMap()
                val values =
                    (1..8).map {
                        pixels[if (edge == BackdropEdge.Start) it else pixels.width - 1 - it, 28]
                            .red
                    }
                jumps[tile] = values.zipWithNext { a, b -> abs(a - b) }.max()
            }
            assertTrue(jumps.getValue(TileMode.Clamp) < .06f, "Clamped boundary: $jumps")
            assertTrue(
                jumps.getValue(TileMode.Decal) > jumps.getValue(TileMode.Clamp) + .1f,
                "Transparent exterior reveals the original stripes: $jumps",
            )
        }
    }

    @Test
    fun rawHeaderCaptureAvoidsInheritingTheControlContour() {
        val colored = mutableMapOf<Boolean, Int>()
        for (raw in listOf(false, true)) runComposeUiTest {
            setContent {
                MaterialTheme {
                    val control =
                        M3xBackdropMaterial(
                            style =
                                BackdropStyle.NoEffects {
                                    highlight = {
                                        Highlight(
                                            width = 4.dp,
                                            opacity = 1f,
                                            style = HighlightStyle.Solid(Color.Red),
                                        )
                                    }
                                },
                        )
                    ProvideBackdropTheme(M3xBackdropTheme(control = control)) {
                        val page = rememberBackdropLayer(Color.Gray)
                        val header = rememberBackdropLayer()
                        BackdropHost(page) {
                            Box(Modifier.size(120.dp, 96.dp)) {
                                Box(Modifier.size(120.dp, 96.dp).captureBackdrop(page))
                                Box(
                                    Modifier.size(120.dp, 56.dp)
                                        .testTag("header")
                                        .captureBackdrop(header)
                                        .drawBackdrop(
                                            page.backdrop,
                                            { RectangleShape },
                                            render = if (raw) ({ source() }) else null,
                                        )
                                        .backdropScrollEdge(visible = { false }),
                                )
                            }
                        }
                    }
                }
            }
            val pixels = onNodeWithTag("header").captureToImage().toPixelMap()
            colored[raw] =
                (0 until pixels.height).sumOf { y ->
                    (0 until pixels.width).count { x ->
                        pixels[x, y].red > pixels[x, y].green + .1f
                    }
                }
        }
        assertTrue(
            colored.getValue(false) > 100,
            "The old implicit render captures a control contour: $colored",
        )
        assertTrue(
            colored.getValue(true) == 0,
            "Raw capture and a scroll-edge role keep the header undecorated: $colored",
        )
    }
}
