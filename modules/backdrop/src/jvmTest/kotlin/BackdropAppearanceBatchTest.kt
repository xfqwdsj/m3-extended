package top.ltfan.m3x.backdrop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.CompletableDeferred

class BackdropAppearanceBatchTest {
    @Test
    fun rasterAtlasReadbackWithNestedRenderEffectsUnderConcurrentGc() = runComposeUiTest {
        var finished = false
        val gcRequests = AtomicInteger()
        var pixelsWereRead = false
        setContent {
            val density = LocalDensity.current
            val layoutDirection = LocalLayoutDirection.current
            val nested = List(12) { rememberGraphicsLayer() }
            val thumbnails = List(8) { rememberGraphicsLayer() }
            val atlas = rememberGraphicsLayer()
            LaunchedEffect(atlas) {
                val running = AtomicBoolean(true)
                val collector = Thread {
                    while (running.get()) {
                        repeat(8) { ByteArray(64 * 1024) }
                        System.gc()
                        gcRequests.incrementAndGet()
                        Thread.sleep(5)
                    }
                }
                    .apply {
                        isDaemon = true
                        start()
                    }
                try {
                    nested.forEach { it.renderEffect = BlurEffect(1.5f, 1.5f, TileMode.Decal) }
                    for (index in nested.indices.reversed()) {
                        nested[index].record(density, layoutDirection, IntSize(20, 20)) {
                            if (index == nested.lastIndex) drawRect(Color.Red)
                            else drawLayer(nested[index + 1])
                        }
                    }
                    thumbnails.forEach { thumbnail ->
                        thumbnail.record(density, layoutDirection, IntSize(5, 5)) {
                            drawLayer(nested.first())
                        }
                    }
                    val pixels = IntArray(10 * 20)
                    repeat(250) {
                        atlas.record(density, layoutDirection, IntSize(10, 20)) {
                            thumbnails.forEachIndexed { index, thumbnail ->
                                val x = index % 2 * 5f
                                val y = index / 2 * 5f
                                translate(left = x, top = y) { drawLayer(thumbnail) }
                            }
                        }
                        readAppearancePixels(atlas, density, layoutDirection, pixels)
                        thumbnails.indices.forEach { index ->
                            val center = (index / 2 * 5 + 2) * 10 + index % 2 * 5 + 2
                            check(
                                pixels[center] ushr 24 != 0 && (pixels[center] ushr 16 and 255) > 0,
                            )
                        }
                    }
                    pixelsWereRead = true
                } finally {
                    running.set(false)
                    collector.join()
                }
                finished = true
            }
        }
        waitUntil(timeoutMillis = 60_000) { finished }
        assertEquals(true, pixelsWereRead)
        assertEquals(true, gcRequests.get() > 0)
    }

    @Test
    fun disposedParticipantIsSkippedWhenReadbackCompletes() = runComposeUiTest {
        val readbackStarted = CompletableDeferred<Unit>()
        val releaseReadback = CompletableDeferred<Unit>()
        lateinit var setSecondVisible: (Boolean) -> Unit
        lateinit var secondState: BackdropAppearanceState
        lateinit var firstState: BackdropAppearanceState
        setContent {
            ProvideBackdropAppearanceBatch {
                val batch = LocalAppearanceBatch.current!!
                DisposableEffect(batch) {
                    val readback = batch.readback
                    batch.readback = { layer, pixels ->
                        readbackStarted.complete(Unit)
                        releaseReadback.await()
                        readback(layer, pixels)
                    }
                    onDispose { batch.readback = readback }
                }
                var secondVisible by remember { mutableStateOf(true) }
                SideEffect { setSecondVisible = { secondVisible = it } }
                val first = rememberBackdropAppearanceState(true, Color.White, true, 50)
                SideEffect { firstState = first }
                SamplingBox(first, Color.Black)
                if (secondVisible) {
                    val second = rememberBackdropAppearanceState(true, Color.White, true, 50)
                    SideEffect { secondState = second }
                    SamplingBox(second, Color.White)
                }
            }
        }
        waitUntil(timeoutMillis = 5000) { readbackStarted.isCompleted }
        runOnIdle { setSecondVisible(false) }
        releaseReadback.complete(Unit)
        waitUntil(timeoutMillis = 5000) { firstState.appearance != null }
        assertEquals(null, secondState.appearance)
    }

    @Test
    fun manyDrawnSurfacesShareOneReadbackAndUnchangedSamplesAreNotRepeated() = runComposeUiTest {
        var readbacks = 0
        val states = arrayOfNulls<BackdropAppearanceState>(30)
        setContent {
            ProvideBackdropAppearanceBatch {
                val batch = LocalAppearanceBatch.current!!
                DisposableEffect(batch) {
                    val readback = batch.readback
                    batch.readback = { layer, pixels ->
                        readbacks++
                        readback(layer, pixels)
                    }
                    onDispose { batch.readback = readback }
                }
                Column {
                    repeat(30) { index ->
                        val state = rememberBackdropAppearanceState(true, Color.White, true, 50)
                        SideEffect { states[index] = state }
                        Box(
                            Modifier.size(20.dp)
                                .onGloballyPositioned(state::updateCoordinates)
                                .drawWithContent {
                                    with(state) {
                                        drawAndRecord {
                                            drawRect(
                                                if (index % 2 == 0) Color.Black else Color.White,
                                            )
                                        }
                                    }
                                },
                        )
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5000) {
            readbacks == 1 && states.all { it?.appearance != null }
        }
        waitForIdle()
        assertEquals(1, readbacks)
        states.forEachIndexed { index, state ->
            val appearance = assertNotNull(state).appearance
            val expected = if (index % 2 == 0) 0f else 1f
            assertEquals(expected, assertNotNull(appearance).relativeLuminance, 0.0001f)
        }
        val generations = states.map { assertNotNull(it).generation }
        val stableSince = System.currentTimeMillis()
        waitUntil(timeoutMillis = 1000) { System.currentTimeMillis() > stableSince + 150 }
        assertEquals(1, readbacks)
        states.forEachIndexed { index, state ->
            assertEquals(generations[index], assertNotNull(state).generation)
        }
    }
}

@Composable
private fun SamplingBox(state: BackdropAppearanceState, color: Color) {
    Box(
        Modifier.size(20.dp).onGloballyPositioned(state::updateCoordinates).drawWithContent {
            with(state) {
                drawAndRecord { drawRect(color) }
            }
        },
    )
}
