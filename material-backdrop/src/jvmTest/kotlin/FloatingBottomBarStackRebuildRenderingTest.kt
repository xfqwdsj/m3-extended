package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.material.FloatingBottomBar
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.item
import top.ltfan.m3x.material.overlay.MaterialOverlay
import top.ltfan.m3x.overlay.OverlayHandle
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState

class FloatingBottomBarStackRebuildRenderingTest {
    private data class FramePixels(
        val pageStripeA: Int,
        val pageStripeB: Int,
        val barA: Int,
        val barB: Int,
    )

    private fun createScene(epoch: MutableIntState): ImageComposeScene {
        @Suppress("UNCHECKED_CAST")
        val epochLocal =
            Class.forName("top.ltfan.backdrop.DrawBackdropModifierKt")
                .getDeclaredMethod("getLocalBackdropRenderEpoch")
                .invoke(null) as ProvidableCompositionLocal<Int>
        return ImageComposeScene(
            width = 360,
            height = 240,
            content = {
                ProvideM3xMaterialRendering {
                    CompositionLocalProvider(epochLocal provides epoch.intValue) {
                        MaterialTheme {
                            val page = rememberBackdropLayer()
                            val overlayHost = remember { OverlayHostState() }
                            val barEntry = remember { OverlayHandle() }
                            BackdropHost(page) {
                                OverlayHost(overlayHost) {
                                    Box(Modifier.fillMaxSize()) {
                                        Canvas(Modifier.fillMaxSize().captureBackdrop(page)) {
                                            val stripe = 14.dp.toPx()
                                            var x = 0f
                                            var index = 0
                                            while (x < size.width) {
                                                drawRect(
                                                    color =
                                                        if (index++ % 2 == 0) Color.Green
                                                        else Color.Blue,
                                                    topLeft = Offset(x, 0f),
                                                    size = Size(stripe, size.height),
                                                )
                                                x += stripe
                                            }
                                        }
                                        MaterialOverlay(barEntry, modal = false) {
                                            Box(Modifier.fillMaxSize()) {
                                                FloatingBottomBar(
                                                    selectedTabIndex = 0,
                                                    onTabSelected = {},
                                                    indicatorEffects = backdropIndicatorEffects {},
                                                    modifier =
                                                        Modifier.align(Alignment.BottomCenter),
                                                ) {
                                                    item { Text("Home") }
                                                    item { Text("Search") }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
        )
    }

    private fun renderPixels(scene: ImageComposeScene, timeNanos: Long): FramePixels {
        scene.render(timeNanos).use { image ->
            val info = ImageInfo(360, 240, ColorType.N32, ColorAlphaType.PREMUL, ColorSpace.sRGB)
            Bitmap().use { bitmap ->
                check(bitmap.allocPixels(info))
                check(image.readPixels(bitmap, 0, 0))
                return FramePixels(
                    pageStripeA = bitmap.getColor(35, 80),
                    pageStripeB = bitmap.getColor(45, 80),
                    barA = bitmap.getColor(130, 205),
                    barB = bitmap.getColor(210, 205),
                )
            }
        }
    }

    @Test
    fun floatingBottomBarInStackSamplesOnlyLayersBelowItAcrossRebuild() {
        val epoch = mutableIntStateOf(0)
        val scene = createScene(epoch)
        try {
            var warmed = FramePixels(0, 0, 0, 0)
            repeat(24) { frame ->
                warmed = renderPixels(scene, frame * 16_000_000L)
            }
            assertNotEquals(
                warmed.pageStripeA,
                warmed.pageStripeB,
                "Page capture should retain stripe contrast",
            )

            epoch.intValue++
            Snapshot.sendApplyNotifications()
            repeat(5) { frame ->
                assertEquals(
                    warmed,
                    renderPixels(scene, (frame + 24) * 16_000_000L),
                    "Stacked FloatingBottomBar pixels changed during rebuild frame $frame",
                )
            }
        } finally {
            scene.close()
        }
    }

    @Test
    fun pressedFloatingBottomBarKeepsSamplingLayersBelowItAcrossRebuild() {
        val epoch = mutableIntStateOf(0)
        val scene = createScene(epoch)
        try {
            var beforePress = FramePixels(0, 0, 0, 0)
            repeat(24) { frame ->
                beforePress = renderPixels(scene, frame * 16_000_000L)
            }

            scene.sendPointerEvent(
                PointerEventType.Press,
                Offset(130f, 205f),
                timeMillis = 400,
                button = PointerButton.Primary,
                type = PointerType.Touch,
            )
            scene.sendPointerEvent(
                PointerEventType.Move,
                Offset(130f, 205f),
                timeMillis = 417,
                type = PointerType.Touch,
            )
            var stablePress = beforePress
            repeat(30) { frame ->
                stablePress = renderPixels(scene, (frame + 24) * 16_000_000L)
            }
            assertNotEquals(
                beforePress.barA,
                stablePress.barA,
                "Pressing the selected tab should change its indicator pixels",
            )

            epoch.intValue++
            Snapshot.sendApplyNotifications()
            repeat(5) { frame ->
                assertEquals(
                    stablePress,
                    renderPixels(scene, (frame + 54) * 16_000_000L),
                    "Pressed FloatingBottomBar pixels changed during rebuild frame $frame",
                )
            }
        } finally {
            scene.close()
        }
    }
}
