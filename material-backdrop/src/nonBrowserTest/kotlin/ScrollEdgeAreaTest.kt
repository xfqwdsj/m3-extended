package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.backdrop.BackdropMask
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering

/** The area a scroll edge covers has to end without leaving an edge of its own behind. */
class ScrollEdgeAreaTest {
    @Test
    fun theAreaAScrollEdgeCoversEndsWithoutALine() = runInitializedComposeUiTest {
        // A smooth gradient page turns the end of the covered area into a one pixel line, which a
        // second difference along a column finds.
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(surface = Color.White)) {
                    val source = rememberBackdropLayer()
                    Box(Modifier.size(200.dp, 300.dp).background(Color.Black).testTag("root")) {
                        Box(
                            Modifier.size(200.dp, 300.dp)
                                .captureBackdrop(source)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0.2f, 0.4f, 0.3f), Color(0.8f, 0.5f, 0.6f)),
                                    ),
                                ),
                        )
                        BackdropHost(source) {
                            BackdropTopAppBar(
                                {},
                                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                                // The demo's own mask: a copy of the page gradient, so the area the
                                // bar
                                // covers continues the page colours.
                                mask =
                                    BackdropMask(
                                        // The demo's own mask: a copy of the page gradient, so the
                                        // area the bar covers continues the page colours.
                                        brush =
                                            object : ShaderBrush() {
                                                override fun createShader(size: Size): Shader =
                                                    LinearGradientShader(
                                                        from = Offset.Zero,
                                                        to = Offset(size.width, size.height),
                                                        colors =
                                                            listOf(
                                                                Color(0.2f, 0.4f, 0.3f),
                                                                Color(0.8f, 0.5f, 0.6f),
                                                            ),
                                                    )
                                            },
                                    ),
                            )
                        }
                    }
                }
            }
        }
        val pixels = onNodeWithTag("root").captureToImage().toPixelMap()
        val end = appBarRampEnd(height = 64f).toInt()
        val rows =
            ((end - 24)..(end + 12)).map { y ->
                (20 until pixels.width - 20).map { x -> pixels[x, y].red }.average().toFloat()
            }
        val spikes =
            rows.zipWithNext().zipWithNext().map { (a, b) ->
                val (y0, y1) = a
                val (_, y2) = b
                if (y1 > y0 && y1 > y2) minOf(y1 - y0, y1 - y2) else 0f
            }
        val worst = spikes.max()
        assertTrue(worst < 0.01f, "the covered area must not end in a line: worst spike $worst")
    }

    @Test
    fun theBlurEndsWithoutAStepOfItsOwn() = runInitializedComposeUiTest {
        // A page with a hard edge under the bar: the blur turns that edge into a ramp, and a step
        // in
        // the ramp is where the blur changes how it normalizes near the covered area's end.
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(surface = Color.White)) {
                    val source = rememberBackdropLayer()
                    Box(Modifier.size(200.dp, 300.dp).background(Color.Black).testTag("root")) {
                        Box(Modifier.size(200.dp, 300.dp).captureBackdrop(source)) {
                            Box(Modifier.size(200.dp, 40.dp).background(Color.Black))
                            Box(Modifier.size(200.dp, 260.dp).background(Color.White))
                        }
                        BackdropHost(source) {
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
        val rows = (30..100).map { y -> pixels[100, y].red }
        val steps =
            rows.zipWithNext().zipWithNext().map { (a, b) ->
                val (y0, y1) = a
                val (_, y2) = b
                if (y1 > y0 && y1 > y2) minOf(y1 - y0, y1 - y2) else 0f
            }
        val worst = steps.max()
        assertTrue(
            worst < 0.004f,
            "the blur must not step: worst $worst in $rows",
        )
    }
}
