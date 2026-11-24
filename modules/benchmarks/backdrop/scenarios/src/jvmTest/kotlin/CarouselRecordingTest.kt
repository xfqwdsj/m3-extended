package top.ltfan.m3x.backdrop.benchmark.scenarios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.registerSkikoComposeImplementation
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.drawBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer

@OptIn(InternalComposeUiApi::class)
class CarouselRecordingTest {
    @Test
    fun theUpperCardSamplesMovingLowerContentInTheSameFrame() {
        registerSkikoComposeImplementation()
        val phase = mutableFloatStateOf(0f)
        val lowerColor = mutableStateOf(Color.Red)
        val scene =
            ImageComposeScene(width = 420, height = 600) {
                val page = rememberBackdropLayer()
                Box(Modifier.fillMaxSize().captureBackdrop(page).background(Color.Blue))
                CarouselCards(page, { phase.floatValue }) { index, source ->
                    val size = Modifier.size(200.dp, 260.dp)
                    when (index) {
                        0 -> Box(size.background(lowerColor.value))
                        1 ->
                            Box(
                                size.drawBackdrop(source, { RectangleShape }) {
                                    val clipped = image {
                                        source()
                                        clipSurface()
                                    }
                                    group {
                                        draw(clipped)
                                        paintSurface { drawRect(Color.White.copy(alpha = .15f)) }
                                    }
                                },
                            )
                        else -> Box(size)
                    }
                }
            }

        try {
            var time = 0L
            fun frame(check: (Color, Color) -> Unit) {
                Snapshot.sendApplyNotifications()
                time += ScenarioTiming.FRAME_NANOS
                scene.render(time).use {
                    val pixels = it.toComposeImageBitmap().toPixelMap()
                    check(pixels[125, 300], pixels[180, 300])
                }
            }
            repeat(4) { frame { _, _ -> } }
            frame { overlap, exterior ->
                assertTrue(overlap.red > .9f && overlap.blue in 0.1f..0.2f, "Overlap: $overlap")
                assertTrue(exterior.blue > .9f && exterior.red in 0.1f..0.2f, "Outside: $exterior")
            }
            lowerColor.value = Color.Green
            frame { overlap, exterior ->
                assertTrue(
                    overlap.green > .9f && overlap.blue in 0.1f..0.2f,
                    "Updated overlap: $overlap",
                )
                assertTrue(exterior.blue > .9f, "Outside: $exterior")
            }
            phase.floatValue = .5f
            frame { oldOverlap, exterior ->
                assertTrue(
                    oldOverlap.blue > .9f && oldOverlap.green in 0.1f..0.2f,
                    "Moved overlap: $oldOverlap",
                )
                assertTrue(exterior.blue > .9f, "Outside: $exterior")
            }
        } finally {
            scene.close()
        }
    }
}
