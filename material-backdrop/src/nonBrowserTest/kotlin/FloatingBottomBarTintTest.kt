package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.compose.ui.accessibility.AccessibilitySettings
import top.ltfan.m3x.compose.ui.accessibility.LocalAccessibilitySettings
import top.ltfan.m3x.material.FloatingBottomBar
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.item

class FloatingBottomBarTintTest {
    @Test fun reducedMotionReleaseKeepsTintAlignedLtr() = probe(LayoutDirection.Ltr)

    @Test fun reducedMotionReleaseKeepsTintAlignedRtl() = probe(LayoutDirection.Rtl)

    private fun probe(direction: LayoutDirection) = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        var selected by mutableIntStateOf(0)
        setContent {
            ProvideM3xMaterialRendering {
                CompositionLocalProvider(
                    LocalLayoutDirection provides direction,
                    LocalAccessibilitySettings provides AccessibilitySettings(reduceMotion = true),
                ) {
                    MaterialTheme(
                        colorScheme = lightColorScheme(primary = Color.Red, surface = Color.White),
                    ) {
                        val source = rememberBackdropLayer()
                        Box(Modifier.size(500.dp, 160.dp).testTag("scene")) {
                            Box(
                                Modifier.fillMaxSize()
                                    .captureBackdrop(source)
                                    .background(Color.White),
                            )
                            BackdropHost(source) {
                                FloatingBottomBar(
                                    selected,
                                    { selected = it },
                                    Modifier.align(Alignment.Center).testTag("bar"),
                                    indicatorEffects = backdropIndicatorEffects {},
                                ) {
                                    repeat(4) {
                                        item { Box(Modifier.size(24.dp).background(Color.Black)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        fun tintedCenter(): Float {
            val pixels = onNodeWithTag("scene").captureToImage().toPixelMap()
            var sum = 0L
            var count = 0
            for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
                val c = pixels[x, y]
                if (c.red > 0.6f && c.green < 0.3f && c.blue < 0.3f) {
                    sum += x
                    count++
                }
            }
            assertTrue(count > 0, "Tinted content is visible")
            return sum.toFloat() / count
        }
        for (index in listOf(0, 1, 3)) {
            runOnIdle { selected = index }
            mainClock.advanceTimeBy(1000)
            val baseline = tintedCenter()
            repeat(2) { attempt ->
                onNodeWithTag("bar").performTouchInput {
                    val tabWidth = (width - 8f) / 4f
                    fun position(value: Float) =
                        Offset(
                            if (direction == LayoutDirection.Ltr) 4f + value * tabWidth
                            else width - 4f - value * tabWidth,
                            height / 2f,
                        )
                    down(position(if (attempt == 0) index + 0.5f else (index + 1) % 4 + 0.5f))
                    moveTo(position(index + 0.75f), delayMillis = 160)
                }
                mainClock.advanceTimeBy(32)
                onNodeWithTag("bar").performTouchInput { up() }
                repeat(3) { frame ->
                    mainClock.advanceTimeBy(16)
                    val center = tintedCenter()
                    assertTrue(
                        abs(center - baseline) <= 1f,
                        "$direction item=$index attempt=$attempt frame=$frame: $baseline -> $center",
                    )
                }
            }
        }
    }
}
