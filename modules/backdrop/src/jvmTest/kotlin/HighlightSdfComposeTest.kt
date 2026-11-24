package top.ltfan.m3x.backdrop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.m3x.backdrop.backdrops.rememberCanvasBackdrop
import top.ltfan.m3x.backdrop.highlight.Highlight
import top.ltfan.m3x.backdrop.highlight.HighlightDirection
import top.ltfan.m3x.backdrop.highlight.HighlightSdfBackend
import top.ltfan.m3x.backdrop.highlight.HighlightSdfMode
import top.ltfan.m3x.backdrop.highlight.HighlightSdfSettings
import top.ltfan.m3x.backdrop.highlight.LocalHighlightSdfSettings
import top.ltfan.m3x.backdrop.highlight.highlight

internal class HighlightSdfComposeTest {
    @Test fun cpuFieldFramesFollowTheCurrentContour() = changingGeometry(HighlightSdfBackend.Cpu)

    @Test fun gpuFieldFramesFollowTheCurrentContour() = changingGeometry(HighlightSdfBackend.Gpu)

    private fun changingGeometry(backend: HighlightSdfBackend) = runRenderTest {
        initializeTestGraphics()
        val generation = mutableIntStateOf(0)
        val shape = GenericShape { _, _ ->
            val x = if (generation.intValue % 2 == 0) 2f else 36f
            addOval(Rect(x, 3f, x + 60f, 66f))
        }
        val blue = Color(.2f, .35f, .65f)
        setContent {
            val source = rememberCanvasBackdrop { drawRect(blue) }
            Column {
                for (reference in listOf(true, false)) {
                    CompositionLocalProvider(
                        LocalHighlightSdfSettings provides
                            HighlightSdfSettings(
                                if (reference) HighlightSdfMode.Off
                                else HighlightSdfMode.Progressive,
                                backend,
                            ),
                    ) {
                        Box(
                            Modifier.size(100.dp, 70.dp)
                                .background(Color.Black)
                                .testTag(if (reference) "contour" else "sdf")
                                .drawBackdrop(source, { shape }) {
                                    replace(image {})
                                    highlight(
                                        Highlight(
                                            width = 9.dp,
                                            blurRadius = 0.dp,
                                            colorGain = .7f,
                                            environmentGain = .4f,
                                            direction = HighlightDirection.Incidence(angle = 30f),
                                        ),
                                        environment = input,
                                    )
                                },
                        )
                    }
                }
            }
        }

        fun compareFrame(revision: Int): Float {
            val reference = onNodeWithTag("contour").captureToImage().toPixelMap()
            val actual = onNodeWithTag("sdf").captureToImage().toPixelMap()
            val left = if (revision % 2 == 0) 3 else 37
            val expired = if (revision % 2 == 0) 93 else 4
            assertTrue(actual[left, 34].blue > .15f, "$backend missed revision $revision")
            assertTrue(
                actual[expired, 34].blue < .05f,
                "$backend retained stale contour at revision $revision",
            )
            var error = 0f
            for (y in 0 until actual.height) {
                for (x in 0 until actual.width) {
                    val a = actual[x, y]
                    val b = reference[x, y]
                    error += abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue)
                }
            }
            return error / (actual.width * actual.height * 3)
        }

        var sampled = 0
        for (revision in 0 until 6) {
            runOnIdle { generation.intValue = revision }
            repeat(5) {
                mainClock.advanceTimeByFrame()
                waitForIdle()
                val error = compareFrame(revision)
                assertTrue(error < .02f, "$backend revision $revision RGB MAE $error")
                if (error > .0001f) sampled++
            }
        }
        assertTrue(sampled >= 4, "$backend never showed the filtered field ($sampled frames)")
    }
}
