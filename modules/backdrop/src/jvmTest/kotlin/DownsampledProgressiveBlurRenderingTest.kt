package top.ltfan.m3x.backdrop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.m3x.backdrop.backdrops.rememberCanvasBackdrop
import top.ltfan.m3x.backdrop.effects.progressiveBlur

internal class DownsampledProgressiveBlurRenderingTest {
    @Test
    fun downsampledShaderTracksFullResolutionAcrossDirectionsAndRadii() = runRenderTest {
        val failures = mutableListOf<String>()
        val direction = mutableStateOf(LayoutDirection.Ltr)
        val radius = mutableFloatStateOf(128f)
        val ramp = BackdropRamp(from = .125f, to = 1f)
        val mode = mutableIntStateOf(0)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction.value) {
                val source = rememberCanvasBackdrop { pattern() }
                Box(
                    Modifier.size(560.dp, 360.dp).testTag("downsample").drawBackdrop(
                        source,
                        { RectangleShape },
                    ) {
                        if (mode.intValue == 0) {
                            effects {
                                progressiveBlur(radius.floatValue, BackdropEdge.Top, ramp)
                            }
                        } else {
                            progressiveBlur(
                                radius.floatValue,
                                BackdropEdge.Top,
                                ramp,
                                ProgressiveBlurMode.Full,
                            )
                        }
                    },
                )
            }
        }
        for (nextDirection in LayoutDirection.entries) for (nextRadius in listOf(128f, 160f)) {
            runOnIdle {
                direction.value = nextDirection
                radius.floatValue = nextRadius
                mode.intValue = 0
            }
            val reference = onNodeWithTag("downsample").captureToImage().toPixelMap()
            runOnIdle { mode.intValue = 1 }
            val actual = onNodeWithTag("downsample").captureToImage().toPixelMap()
            val error = difference(reference, actual)
            println(
                "DOWNSAMPLE_BLUR direction=$nextDirection radius=$nextRadius error=${error.toList()}",
            )
            if (error[0] >= .03f || error[1] >= .30f || error[2] >= .45f)
                failures += "direction=$nextDirection radius=$nextRadius error=${error.toList()}"
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun downsampledShaderMatchesFullAcrossRampsEdgesAndTransparentInput() = runRenderTest {
        val failures = mutableListOf<String>()
        val direction = mutableStateOf(LayoutDirection.Ltr)
        val edge = mutableStateOf(BackdropEdge.Top)
        val start = mutableFloatStateOf(0f)
        val end = mutableFloatStateOf(1f)
        val mode = mutableIntStateOf(0)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction.value) {
                val source = rememberCanvasBackdrop { translucentPattern() }
                Box(
                    Modifier.size(560.dp, 360.dp).testTag("downsample-ramp").drawBackdrop(
                        source,
                        { RectangleShape },
                    ) {
                        val ramp =
                            BackdropRamp(
                                anchors = BackdropRampAnchors.offsets(12.dp, 8.dp),
                                from = start.floatValue,
                                to = end.floatValue,
                            )
                        if (mode.intValue == 0) {
                            effects { progressiveBlur(128f, edge.value, ramp) }
                        } else {
                            progressiveBlur(128f, edge.value, ramp, ProgressiveBlurMode.Full)
                        }
                    },
                )
            }
        }
        for (nextDirection in LayoutDirection.entries) {
            for (nextEdge in BackdropEdge.entries) {
                for ((from, to) in listOf(.125f to 1f, 1f to .125f)) {
                    runOnIdle {
                        direction.value = nextDirection
                        edge.value = nextEdge
                        start.floatValue = from
                        end.floatValue = to
                        mode.intValue = 0
                    }
                    val reference = onNodeWithTag("downsample-ramp").captureToImage().toPixelMap()
                    runOnIdle { mode.intValue = 1 }
                    val actual = onNodeWithTag("downsample-ramp").captureToImage().toPixelMap()
                    val error = difference(reference, actual)
                    if (error[0] >= .03f || error[1] >= .35f || error[2] >= .50f) {
                        failures +=
                            "direction=$nextDirection edge=$nextEdge ramp=$from..$to " +
                                "error=${error.toList()}"
                    }
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun downsampledShaderTracksMovingSourceContent() = runRenderTest {
        val shift = mutableIntStateOf(0)
        val start = mutableFloatStateOf(0f)
        val mode = mutableIntStateOf(0)
        setContent {
            val source = rememberCanvasBackdrop { pattern(shift.intValue.toFloat()) }
            Box(
                Modifier.size(560.dp, 360.dp).testTag("downsample-scroll").drawBackdrop(
                    source,
                    { RectangleShape },
                ) {
                    val ramp = BackdropRamp(from = start.floatValue, to = 1f)
                    if (mode.intValue == 0) {
                        effects { progressiveBlur(128f, BackdropEdge.Top, ramp) }
                    } else {
                        progressiveBlur(128f, BackdropEdge.Top, ramp, ProgressiveBlurMode.Full)
                    }
                },
            )
        }
        val failures = mutableListOf<String>()
        for (from in listOf(0f, .125f)) for (offset in listOf(0, 7, 19, 31)) {
            runOnIdle {
                shift.intValue = offset
                start.floatValue = from
                mode.intValue = 0
            }
            val reference = onNodeWithTag("downsample-scroll").captureToImage().toPixelMap()
            runOnIdle { mode.intValue = 1 }
            val actual = onNodeWithTag("downsample-scroll").captureToImage().toPixelMap()
            val error = difference(reference, actual)
            if (from == 0f) {
                assertTrue(
                    error.all { it == 0f },
                    "Clear-end offset=$offset error=${error.toList()}",
                )
            } else if (error[0] >= .03f || error[1] >= .30f || error[2] >= .45f) {
                failures += "offset=$offset error=${error.toList()}"
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    private fun DrawScope.pattern(shift: Float = 0f) {
        drawRect(Color(0.08f, 0.12f, 0.2f))
        for (y in -32..392 step 3) for (x in -32..592 step 3) {
            if ((x / 3 + y / 3) % 2 == 0) {
                drawRect(
                    Color.White.copy(alpha = if (x < 280) .75f else .35f),
                    Offset(x + shift, y.toFloat()),
                    Size(2f, 2f),
                )
            }
        }
    }

    private fun DrawScope.translucentPattern() {
        drawRect(Color.White.copy(alpha = .12f))
        for (y in -32..392 step 7) for (x in -32..592 step 5) {
            drawRect(
                Color(0.15f, 0.55f, 0.9f, alpha = .28f),
                Offset(x.toFloat(), y.toFloat()),
                Size(3f + if (y % 2 == 0) 1f else 0f, 2f),
            )
        }
    }

    private fun difference(reference: PixelMap, actual: PixelMap): FloatArray {
        var sum = 0f
        var maximum = 0f
        var jump = 0f
        fun components(color: Color) =
            floatArrayOf(
                color.red * color.alpha,
                color.green * color.alpha,
                color.blue * color.alpha,
                color.alpha,
            )
        for (y in 4 until reference.height - 4) for (x in 4 until reference.width - 4) {
            val a = components(reference[x, y])
            val b = components(actual[x, y])
            val previousA = components(reference[x - 1, y])
            val previousB = components(actual[x - 1, y])
            val aboveA = components(reference[x, y - 1])
            val aboveB = components(actual[x, y - 1])
            for (channel in 0..3) {
                val error = abs(a[channel] - b[channel])
                sum += error
                maximum = maxOf(maximum, error)
                jump =
                    maxOf(
                        jump,
                        abs((b[channel] - previousB[channel]) - (a[channel] - previousA[channel])),
                        abs((b[channel] - aboveB[channel]) - (a[channel] - aboveA[channel])),
                    )
            }
        }
        return floatArrayOf(
            sum / ((reference.width - 8) * (reference.height - 8) * 4),
            maximum,
            jump,
        )
    }
}
