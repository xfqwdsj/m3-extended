package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.backdrop.BackdropEdge
import top.ltfan.backdrop.BackdropFill
import top.ltfan.backdrop.BackdropRamp
import top.ltfan.backdrop.drawBackdrop
import top.ltfan.backdrop.effects.progressiveBlur
import top.ltfan.backdrop.side
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering

class ScrollEdgeDirectionTest {
    @Test
    fun upstreamBlurAndScrollEdgeFillAreStrongestAtTheNamedPhysicalEdge() {
        for (direction in LayoutDirection.entries) for (edge in BackdropEdge.entries) {
            for (fill in listOf(false, true)) runInitializedComposeUiTest {
                val side = edge.side(direction)
                setContent {
                    ProvideM3xMaterialRendering {
                        MaterialTheme {
                            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                                val source = rememberBackdropLayer()
                                BackdropHost(source) {
                                    Box(Modifier.size(80.dp).testTag("scene")) {
                                        Canvas(Modifier.fillMaxSize().captureBackdrop(source)) {
                                            if (fill) drawRect(Color.Black)
                                            else
                                                for (y in 0 until 80 step 4) for (x in
                                                    0 until 80 step 4) {
                                                    drawRect(
                                                        if ((x + y) % 8 == 0) Color.Black
                                                        else Color.White,
                                                        Offset(x.toFloat(), y.toFloat()),
                                                        Size(4f, 4f),
                                                    )
                                                }
                                        }
                                        if (fill)
                                            Box(
                                                Modifier.fillMaxSize()
                                                    .backdropScrollEdge(
                                                        edge = edge,
                                                        effects = {},
                                                        fill =
                                                            BackdropFill(
                                                                Color.White,
                                                                alpha = 1f,
                                                                ramp = BackdropRamp(),
                                                            ),
                                                    ),
                                            )
                                        else
                                            Box(
                                                Modifier.fillMaxSize().drawBackdrop(
                                                    source.backdrop,
                                                    { RectangleShape },
                                                ) {
                                                    source()
                                                    effects { progressiveBlur(8f, edge) }
                                                    clipSurface()
                                                },
                                            )
                                    }
                                }
                            }
                        }
                    }
                }
                val pixels = onNodeWithTag("scene").captureToImage().toPixelMap()
                fun samples(coordinate: Int): List<Float> =
                    (12 until 68).map {
                        if (side.isVertical) pixels[it, coordinate].red
                        else pixels[coordinate, it].red
                    }
                val leading = samples(8)
                val trailing = samples(71)
                fun contrast(values: List<Float>) = values.max() - values.min()
                val a = if (fill) leading.average().toFloat() else 1f - contrast(leading)
                val b = if (fill) trailing.average().toFloat() else 1f - contrast(trailing)
                assertTrue(
                    if (side.carriesContent) a > b + 0.1f else b > a + 0.1f,
                    "edge=$edge direction=$direction fill=$fill leading=$a trailing=$b",
                )
            }
        }
    }
}
