package top.ltfan.m3x.backdrop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay

/**
 * SDR appearance statistics. Relative luminance is linear; perceptual brightness is sRGB encoded.
 */
public data class BackdropAppearance(
    public val relativeLuminance: Float,
    public val perceptualBrightness: Float,
)

/**
 * Measures sRGB pixels composited over [background]. Transparent samples retain the page's
 * contribution. This readback represents SDR appearance, including on an HDR display.
 */
public fun measureBackdropAppearance(pixels: IntArray, background: Color): BackdropAppearance {
    require(pixels.isNotEmpty())
    var luminance = 0f
    for (pixel in pixels) {
        val alpha = (pixel ushr 24) / 255f
        val red = (pixel ushr 16 and 255) / 255f
        val green = (pixel ushr 8 and 255) / 255f
        val blue = (pixel and 255) / 255f
        luminance +=
            0.2126f * srgbToLinear(red * alpha + background.red * (1f - alpha)) +
                0.7152f * srgbToLinear(green * alpha + background.green * (1f - alpha)) +
                0.0722f * srgbToLinear(blue * alpha + background.blue * (1f - alpha))
    }
    luminance /= pixels.size
    return BackdropAppearance(luminance, linearToSrgb(luminance))
}

private fun srgbToLinear(value: Float): Float =
    if (value <= 0.04045f) value / 12.92f else ((value + 0.055f) / 1.055f).pow(2.4f)

private fun linearToSrgb(value: Float): Float =
    if (value <= 0.0031308f) value * 12.92f else 1.055f * value.pow(1f / 2.4f) - 0.055f

/** Chooses a black or white foreground with a contrast-ratio dead band to prevent flicker. */
public fun BackdropAppearance.prefersDarkContent(current: Boolean): Boolean {
    val blackContrast = (relativeLuminance + 0.05f) / 0.05f
    val whiteContrast = 1.05f / (relativeLuminance + 0.05f)
    return if (current) blackContrast * 1.15f >= whiteContrast
    else blackContrast > whiteContrast * 1.15f
}

/** Per-surface result; the raw background thumbnail is read together with other host samples. */
@Stable
public class BackdropAppearanceState internal constructor(initialDarkContent: Boolean) {
    public var appearance: BackdropAppearance? by mutableStateOf(null)
        internal set

    public var darkContent: Boolean by mutableStateOf(initialDarkContent)
        internal set

    public var samplingAvailable: Boolean by mutableStateOf(true)
        internal set

    internal var coordinates: LayoutCoordinates? = null
    internal var generation: Long = 0
    internal var recordedSize: IntSize = IntSize.Zero
    internal var record: DrawScope.(DrawScope.() -> Unit) -> Unit = { it() }
    internal var thumbnail: GraphicsLayer? = null
    internal var lastRecordedAt: TimeMark? = null

    /** Updates the receiving surface coordinates for visibility and sampling. */
    public fun updateCoordinates(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
    }

    /** Draws the source once onscreen and records a small raw-background thumbnail. */
    public fun DrawScope.drawAndRecord(drawBackdrop: DrawScope.() -> Unit) {
        record(drawBackdrop)
    }
}

internal class AppearanceBatch(
    private val atlas: GraphicsLayer,
    private val density: Density,
    private val layoutDirection: LayoutDirection,
    private val window: Any,
) {
    private class Entry(
        val state: BackdropAppearanceState,
        val layer: GraphicsLayer,
        var background: Color,
        val intervalMillis: Long,
        var consumedGeneration: Long = -1L,
    )

    private val entries = linkedMapOf<BackdropAppearanceState, Entry>()
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private var pixelBuffer = IntArray(0)
    private val tileBuffer = IntArray(25)
    internal var readback: suspend (GraphicsLayer, IntArray) -> Unit = { layer, pixels ->
        readAppearancePixels(layer, density, layoutDirection, pixels)
    }

    fun ownsWindow(candidate: Any): Boolean = window === candidate

    fun register(
        state: BackdropAppearanceState,
        layer: GraphicsLayer,
        background: Color,
        intervalMillis: Long,
    ) {
        entries[state] = Entry(state, layer, background, intervalMillis)
    }

    fun update(state: BackdropAppearanceState, background: Color) {
        entries[state]?.background = background
    }

    fun unregister(state: BackdropAppearanceState) {
        entries.remove(state)
        state.appearance = null
    }

    fun request() {
        requests.trySend(Unit)
    }

    suspend fun run() {
        for (ignored in requests) {
            delay(
                (entries.values
                        .filter { it.state.samplingAvailable }
                        .minOfOrNull { it.intervalMillis } ?: 200L)
                    .milliseconds,
            )
            val batch =
                entries.values.filter { entry ->
                    val coords = entry.state.coordinates
                    coords != null &&
                        coords.isAttached &&
                        !coords.boundsInWindow().isEmpty &&
                        entry.state.samplingAvailable &&
                        entry.state.thumbnail === entry.layer &&
                        entry.state.recordedSize == IntSize(5, 5) &&
                        entry.state.generation > 0 &&
                        entry.state.generation != entry.consumedGeneration
                }
            if (batch.isEmpty()) continue
            val generations = batch.map { it.state.generation }
            val columns = ceil(sqrt(batch.size.toDouble())).toInt()
            val rows = (batch.size + columns - 1) / columns
            val imageWidth = columns * 5
            val imageHeight = rows * 5
            try {
                atlas.record(density, layoutDirection, IntSize(imageWidth, imageHeight)) {
                    batch.forEachIndexed { index, entry ->
                        val x = (index % columns) * 5f
                        val y = (index / columns) * 5f
                        clipRect(x, y, x + 5f, y + 5f) {
                            translate(left = x, top = y) { drawLayer(entry.layer) }
                        }
                    }
                }
                val pixelCount = imageWidth * imageHeight
                if (pixelBuffer.size < pixelCount) pixelBuffer = IntArray(pixelCount)
                readback(atlas, pixelBuffer)
                batch.forEachIndexed { index, entry ->
                    if (
                        entries[entry.state] !== entry ||
                            entry.state.generation != generations[index]
                    )
                        return@forEachIndexed
                    val tileX = (index % columns) * 5
                    val tileY = (index / columns) * 5
                    for (row in 0 until 5) {
                        val start = (tileY + row) * imageWidth + tileX
                        pixelBuffer.copyInto(tileBuffer, row * 5, start, start + 5)
                    }
                    val measured = measureBackdropAppearance(tileBuffer, entry.background)
                    entry.state.darkContent = measured.prefersDarkContent(entry.state.darkContent)
                    entry.state.appearance = measured
                    entry.consumedGeneration = generations[index]
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                batch.forEach {
                    if (entries[it.state] === it) {
                        it.state.samplingAvailable = false
                        it.state.appearance = null
                    }
                }
            }
        }
    }
}

internal val LocalAppearanceBatch = staticCompositionLocalOf<AppearanceBatch?> { null }

@Composable
internal fun ProvideBackdropAppearanceBatch(content: @Composable () -> Unit) {
    val window = LocalWindowInfo.current
    val inherited = LocalAppearanceBatch.current
    if (inherited != null && inherited.ownsWindow(window)) {
        content()
        return
    }
    val atlas = rememberGraphicsLayer()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val batch =
        remember(atlas, density, layoutDirection, window) {
            AppearanceBatch(atlas, density, layoutDirection, window)
        }
    CompositionLocalProvider(LocalAppearanceBatch provides batch) {
        LaunchedEffect(batch) { batch.run() }
        content()
    }
}

/**
 * Creates an SDR sampler that records a thumbnail no more often than [sampleIntervalMillis]. A
 * [BackdropHost] batches newly drawn visible thumbnails into shared readbacks on the shortest
 * active participant interval; each surface retains its own local sample and background
 * compositing. Material surfaces enable this sampler only for an explicit background-adaptation
 * choice. Optical animation and input lighting do not run a readback loop. Each result describes
 * the receiving surface region.
 */
@Composable
public fun rememberBackdropAppearanceState(
    enabled: Boolean,
    background: Color,
    initialDarkContent: Boolean,
    sampleIntervalMillis: Long = 200,
): BackdropAppearanceState {
    require(sampleIntervalMillis > 0)
    val layer = if (enabled) rememberGraphicsLayer() else null
    val state = remember { BackdropAppearanceState(initialDarkContent) }
    val currentBackground = rememberUpdatedState(background)
    val window = LocalWindowInfo.current
    val inheritedBatch = LocalAppearanceBatch.current?.takeIf { it.ownsWindow(window) }
    val source = LocalBackdrop.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val fallbackAtlas = if (enabled && inheritedBatch == null) rememberGraphicsLayer() else null
    val batch =
        inheritedBatch
            ?: fallbackAtlas?.let { atlas ->
                remember(atlas, density, layoutDirection, window) {
                    AppearanceBatch(atlas, density, layoutDirection, window)
                }
            }
    LaunchedEffect(batch, inheritedBatch, enabled, layer) {
        if (batch != null && inheritedBatch == null && enabled && layer != null) batch.run()
    }
    DisposableEffect(enabled, layer, batch, source, sampleIntervalMillis) {
        if (enabled && layer != null && batch != null) {
            state.thumbnail = layer
            state.samplingAvailable = true
            state.recordedSize = IntSize.Zero
            state.generation = 0
            state.lastRecordedAt = null
            state.appearance = null
            batch.register(state, layer, currentBackground.value, sampleIntervalMillis)
        } else {
            state.thumbnail = null
            state.appearance = null
        }
        onDispose {
            batch?.unregister(state)
            state.thumbnail = null
            state.recordedSize = IntSize.Zero
            state.lastRecordedAt = null
        }
    }
    state.record =
        if (enabled && layer != null && state.samplingAvailable && batch != null) {
            { drawBackdrop ->
                drawBackdrop()
                val coords = state.coordinates
                if (
                    coords != null &&
                        coords.isAttached &&
                        !coords.boundsInWindow().isEmpty &&
                        size.width > 0f &&
                        size.height > 0f
                ) {
                    val last = state.lastRecordedAt
                    if (last == null || last.elapsedNow() >= sampleIntervalMillis.milliseconds) {
                        val sourceSize = size
                        layer.record(size = IntSize(5, 5)) {
                            draw(
                                density,
                                layoutDirection,
                                drawContext.canvas,
                                sourceSize,
                                drawContext.graphicsLayer,
                            ) {
                                scale(5f / sourceSize.width, 5f / sourceSize.height, Offset.Zero) {
                                    drawBackdrop()
                                }
                            }
                        }
                        state.recordedSize = IntSize(5, 5)
                        state.generation++
                        state.lastRecordedAt = TimeSource.Monotonic.markNow()
                        batch.update(state, currentBackground.value)
                        batch.request()
                    }
                }
            }
        } else {
            { it() }
        }
    return state
}

internal expect suspend fun readAppearancePixels(
    layer: GraphicsLayer,
    density: Density,
    layoutDirection: LayoutDirection,
    pixels: IntArray,
)
