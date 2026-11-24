package top.ltfan.m3x.backdrop.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.fastCoerceIn
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import top.ltfan.m3x.backdrop.BackdropEdge
import top.ltfan.m3x.backdrop.BackdropImage
import top.ltfan.m3x.backdrop.BackdropImageScope
import top.ltfan.m3x.backdrop.BackdropPrimitiveInput
import top.ltfan.m3x.backdrop.BackdropRamp
import top.ltfan.m3x.backdrop.BackdropSampling
import top.ltfan.m3x.backdrop.RenderImageNode
import top.ltfan.m3x.backdrop.isRenderEffectSupported

/**
 * Approximates spatial blur by mixing adjacent uniformly blurred versions of the current image.
 * [levels] includes the unchanged, zero-radius input and must be at least two. Each other level
 * independently blurs that same input at a radius evenly spaced between zero and [radius], in
 * surface pixels. Each branch records the region where its weight is positive, with sampling room.
 *
 * [ramp] shares its anchors, curve and coverage with [progressiveBlur]. Its [BackdropRamp.stops]
 * controls the initial curve sampling, independently of the number of blur levels. Level crossings
 * and plateau compaction determine the final gradient stop count. Adjacent levels use complementary
 * premultiplied weights, subject to color and render-target quantization. Blur can redistribute
 * source alpha; a separate alpha mask controls the resulting image's fade.
 *
 * Radius must be finite and non-negative, and ramp endpoints must be finite and in `0..1`. Anchors
 * must resolve to a finite increasing span. Unsupported render effects and zero radius preserve the
 * current image and request no coverage; radius, endpoint and level-count validation still applies.
 * Algorithm selection belongs to the caller.
 */
public fun BackdropImageScope.layeredProgressiveBlur(
    radius: Float,
    edge: BackdropEdge,
    ramp: BackdropRamp = BackdropRamp(),
    levels: Int = 8,
) {
    layeredProgressiveBlurImpl(radius, edge, ramp, levels, isRenderEffectSupported())
}

internal fun BackdropImageScope.layeredProgressiveBlurImpl(
    radius: Float,
    edge: BackdropEdge,
    ramp: BackdropRamp,
    levels: Int,
    supported: Boolean,
) {
    validateProgressiveBlur(radius, ramp)
    require(levels >= 2) { "Layered progressive blur needs at least two levels" }
    if (radius == 0f || !supported) return
    val geometry = density.progressiveBlurGeometry(size, layoutDirection, edge, ramp) ?: return
    effects { cover(geometry.coverage) }
    val original = current
    replace(
        image {
            drawPrimitive(
                layeredBlurNode(original, radius, ramp, levels, geometry),
                1f,
                BlendMode.SrcOver,
            )
        },
    )
}

internal fun layeredBlurNode(
    original: BackdropImage,
    radius: Float,
    ramp: BackdropRamp,
    levels: Int,
    geometry: ProgressiveBlurGeometry,
): RenderImageNode.Primitive<LayeredBlurPrimitive> {
    val scale = levels - 1
    val lastLevel = ceil(maxOf(ramp.from, ramp.to) * scale).toInt()
    val support =
        if (lastLevel == 0) 0f else gaussianSupport(radius * (lastLevel.toFloat() / scale))
    return RenderImageNode.Primitive(
        LayeredBlurPrimitive(radius, ramp, levels, geometry.start, geometry.end),
        listOf(
            BackdropPrimitiveInput(
                original,
                BackdropSampling.outsets(support, support, support, support),
            ),
        ),
    )
}

/**
 * Returns the surface-local region in which [level] can have a non-zero gradient weight. The
 * gradient keeps its endpoint value beyond the anchors, so endpoint-active levels extend to the
 * corresponding surface edge. A one-pixel margin protects fractional layer boundaries.
 */
internal fun layeredBlurBand(
    ramp: BackdropRamp,
    level: Int,
    levels: Int,
    start: Offset,
    end: Offset,
    surface: Rect,
): Rect? = layeredBlurBand(layeredBlurStops(ramp, levels), level, levels, start, end, surface)

internal fun layeredBlurBand(
    stops: List<Pair<Float, Float>>,
    level: Int,
    levels: Int,
    start: Offset,
    end: Offset,
    surface: Rect,
): Rect? {
    if (levels < 2 || level !in 0 until levels || surface.isEmpty) return null
    val vertical = start.x == end.x
    if (!vertical && start.y != end.y) return null
    if (stops.size < 2) return null
    val scale = levels - 1
    val lowerWeightBoundary = (level - 1f) / scale
    val upperWeightBoundary = (level + 1f) / scale
    var fromFraction = Float.POSITIVE_INFINITY
    var toFraction = Float.NEGATIVE_INFINITY
    for (index in 0 until stops.lastIndex) {
        val a = stops[index]
        val b = stops[index + 1]
        val minimumIntensity = minOf(a.second, b.second)
        val maximumIntensity = maxOf(a.second, b.second)
        if (minimumIntensity >= upperWeightBoundary || maximumIntensity <= lowerWeightBoundary)
            continue
        fromFraction = minOf(fromFraction, a.first)
        toFraction = maxOf(toFraction, b.first)
    }
    if (fromFraction > toFraction) return null
    val axisStart = if (vertical) start.y else start.x
    val axisEnd = if (vertical) end.y else end.x
    val first =
        if (fromFraction == 0f && layeredBlurWeight(stops.first().second, level, levels) > 0f) {
            if (axisStart <= axisEnd) surface.topOrLeft(vertical)
            else surface.bottomOrRight(vertical)
        } else {
            axisStart + (axisEnd - axisStart) * fromFraction
        }
    val second =
        if (toFraction == 1f && layeredBlurWeight(stops.last().second, level, levels) > 0f) {
            if (axisStart <= axisEnd) surface.bottomOrRight(vertical)
            else surface.topOrLeft(vertical)
        } else {
            axisStart + (axisEnd - axisStart) * toFraction
        }
    var low = minOf(first, second)
    var high = maxOf(first, second)
    if (low >= high) return null
    low = maxOf(surface.topOrLeft(vertical), low - 1f)
    high = minOf(surface.bottomOrRight(vertical), high + 1f)
    if (low >= high) return null
    return if (vertical) {
        Rect(surface.left, low, surface.right, high)
    } else {
        Rect(low, surface.top, high, surface.bottom)
    }
}

private fun Rect.topOrLeft(vertical: Boolean): Float = if (vertical) top else left

private fun Rect.bottomOrRight(vertical: Boolean): Float = if (vertical) bottom else right

internal fun layeredBlurWeight(intensity: Float, level: Int, levels: Int): Float =
    (1f - abs(intensity * (levels - 1) - level)).fastCoerceIn(0f, 1f)

/** Inserts level crossings into the sampled ramp so interpolation only mixes adjacent levels. */
internal fun layeredBlurStops(ramp: BackdropRamp, levels: Int): List<Pair<Float, Float>> {
    val intensities = ramp.intensities()
    return buildList {
        for (index in 0 until intensities.lastIndex) {
            val from = intensities[index]
            val to = intensities[index + 1]
            if (index == 0 || from != to || from != intensities[index - 1]) {
                add(index.toFloat() / intensities.lastIndex to from)
            }
            if (from == to) continue
            val scale = levels - 1
            val first = floor(minOf(from, to) * scale).toInt() + 1
            val last = ceil(maxOf(from, to) * scale).toInt() - 1
            val crossings = if (from < to) first..last else last downTo first
            for (level in crossings) {
                val intensity = level.toFloat() / scale
                val fraction = (intensity - from) / (to - from)
                if (fraction > 0f && fraction < 1f) {
                    add((index + fraction) / intensities.lastIndex to intensity)
                }
            }
        }
        add(1f to intensities.last())
    }
}

/** Retains the stops in this level's support, the nearby zero weights, and the endpoints. */
internal fun layeredBlurColors(
    stops: List<Pair<Float, Float>>,
    level: Int,
    levels: Int,
): List<Pair<Float, Color>> = buildList {
    val direction = if (stops.first().second <= stops.last().second) 1 else -1
    val scale = levels - 1
    val center = (direction * level).toFloat()
    // Search the monotone ramp in level coordinates. Include equal limits to retain the exact
    // zero-weight neighbors even when a crossing rounds to a sampled intensity.
    val from = stops.firstMatchingStop { direction * (it * scale) >= center - 1f }
    val until = stops.firstMatchingStop { direction * (it * scale) > center + 1f }
    fun weight(index: Int): Float = layeredBlurWeight(stops[index].second, level, levels)
    fun addStop(index: Int, alpha: Float) {
        add(stops[index].first to Color.White.copy(alpha = alpha))
    }
    addStop(0, weight(0))
    for (index in maxOf(1, from - 1)..minOf(stops.lastIndex - 1, until)) {
        val alpha = weight(index)
        if (alpha != weight(index - 1) || alpha != weight(index + 1)) addStop(index, alpha)
    }
    addStop(stops.lastIndex, weight(stops.lastIndex))
}

private inline fun List<Pair<Float, Float>>.firstMatchingStop(matches: (Float) -> Boolean): Int {
    var low = 0
    var high = size
    while (low < high) {
        val mid = (low + high) ushr 1
        if (matches(this[mid].second)) high = mid else low = mid + 1
    }
    return low
}
