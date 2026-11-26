package top.ltfan.m3x.backdrop.highlight

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.ltfan.m3x.backdrop.internal.BackdropPrimitiveRuntime
import top.ltfan.m3x.backdrop.internal.BoundaryGeometry

internal class HighlightSdfHost(
    override val scope: () -> CoroutineScope,
    val settings: () -> HighlightSdfSettings,
) : BackdropPrimitiveRuntime

/** Owns the work and the complete field for one contour renderer. Calls are confined to Compose. */
internal class HighlightSdfPreparation(private val host: HighlightSdfHost) {
    private data class Key(
        val geometry: BoundaryGeometry,
        val size: Size,
        val density: Float,
        val fontScale: Float,
        val layoutDirection: LayoutDirection,
        val maximumWidth: Float,
        val settings: HighlightSdfSettings,
    )

    private var key: Key? = null
    private var generation = 0
    private var revision by mutableIntStateOf(0)
    private var framePassed = true
    private var quarterDrawn: CompletableDeferred<Unit>? = null
    private var stableFrames = 0
    private var started = false
    private var job: Job? = null
    private var current: HighlightSdfField? = null

    fun field(
        geometry: BoundaryGeometry,
        size: Size,
        density: Float,
        fontScale: Float,
        layoutDirection: LayoutDirection,
        maximumWidth: Float,
        safeSize: IntSize,
    ): HighlightSdfField? {
        val settings = host.settings()
        if (
            safeSize.width !in 1..16384 ||
                safeSize.height !in 1..16384 ||
                settings.mode == Off ||
                (settings.backend == Gpu && !highlightGpuSdfAvailable) ||
                (settings.mode == Auto && geometry.segmentCount < 24)
        ) {
            reset()
            return null
        }

        val reservedWidth = ceil(maximumWidth / 4f) * 4f
        if (reservedWidth > 4096f) {
            reset()
            return null
        }
        val nextKey =
            Key(geometry, size, density, fontScale, layoutDirection, reservedWidth, settings)
        if (key != nextKey) {
            reset()
            key = nextKey
        }
        // Reading this snapshot state subscribes the primitive's recording to field commits.
        // Incrementing it after updating current schedules a new recording for that field.
        revision
        val field = current
        if (field != null) quarterDrawn?.complete(Unit)
        if (started || job?.isActive == true) return field

        // Only count recordings requested by the application. One retained, idle recording
        // represents no future search work to amortize. The clock separates observations by frame.
        if (settings.mode == Auto && stableFrames < 2) {
            if (framePassed) {
                framePassed = false
                stableFrames++
                job =
                    host.scope().launch {
                        withFrameNanos {}
                        framePassed = true
                    }
            }
            return null
        }

        started = true
        val outputs =
            HighlightTiles().apply {
                prepare(safeSize, geometry.contourFreeBounds, reservedWidth)
            }
        val outputRects = List(outputs.count) { outputs[it] }
        val half = HighlightSdfLayout(outputRects, 2, reservedWidth)
        val quarter =
            if (settings.mode == Progressive) HighlightSdfLayout(outputRects, 4, reservedWidth)
            else null
        val backend =
            when (settings.backend) {
                Auto -> if (highlightGpuSdfAvailable && half.bandTexels >= 32_768L) Gpu else Cpu
                else -> settings.backend
            }
        val expected = generation
        job =
            host.scope().launch {
                withFrameNanos {}
                if (quarter != null) {
                    val drawn = CompletableDeferred<Unit>()
                    quarterDrawn = drawn
                    if (prepare(quarter, geometry, settings.backend, backend, expected)) {
                        drawn.await()
                        withFrameNanos {}
                    }
                    quarterDrawn = null
                }
                prepare(half, geometry, settings.backend, backend, expected)
            }
        return current
    }

    private suspend fun prepare(
        layout: HighlightSdfLayout,
        geometry: BoundaryGeometry,
        preference: HighlightSdfBackend,
        backend: HighlightSdfBackend,
        expected: Int,
    ): Boolean {
        if (
            expected != generation ||
                !layout.fitsBudget ||
                layout.bandTexels * geometry.segmentCount > 134_217_728L
        )
            return false
        val gpu =
            if (backend == Gpu) {
                try {
                    bakeHighlightGpuSdf(layout, geometry)
                } catch (failure: RuntimeException) {
                    if (failure is CancellationException) throw failure
                    null
                }
            } else null
        val field =
            gpu
                ?: when {
                    preference == Gpu -> return false
                    // Cap both preparation work and retained texture storage for expensive
                    // outlines.
                    layout.bandTexels * maxOf(geometry.segmentCount, 4) > 16_777_216L ->
                        return false
                    else -> {
                        val pixels =
                            withContext(highlightSdfCpuDispatcher) {
                                bakeHighlightCpuSdf(layout, geometry)
                            }
                        if (generation != expected) return false
                        uploadHighlightSdf(layout, pixels)
                    }
                }
        if (generation != expected) {
            field.close()
            return false
        }
        val previous = current
        current = field
        previous?.close()
        revision++
        return true
    }

    fun reset() {
        if (key == null) return
        generation++
        job?.cancel()
        job = null
        current?.close()
        current = null
        key = null
        stableFrames = 0
        framePassed = true
        quarterDrawn = null
        started = false
    }
}

private val highlightSdfCpuDispatcher = Dispatchers.Default.limitedParallelism(2)
