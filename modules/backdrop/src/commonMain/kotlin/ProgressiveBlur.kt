package top.ltfan.m3x.backdrop

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import top.ltfan.m3x.backdrop.effects.blur
import top.ltfan.m3x.backdrop.effects.coverRamp
import top.ltfan.m3x.backdrop.effects.downsampledProgressiveBlur
import top.ltfan.m3x.backdrop.effects.layeredProgressiveBlur
import top.ltfan.m3x.backdrop.effects.minimumAreaForDownsampledBlur
import top.ltfan.m3x.backdrop.effects.progressiveBlur
import top.ltfan.m3x.backdrop.effects.progressiveBlurGeometry

/** Selects the spatial blur algorithm independently of material colors and geometry. */
@Immutable
public sealed class ProgressiveBlurMode {
    /** Uses the complete shader when available, otherwise the eight-level approximation. */
    public data object Auto : ProgressiveBlurMode()

    /**
     * Requires the complete shader. Large, consistently soft transitions can use a reduced internal
     * grid. Unsupported platforms produce a transparent image.
     */
    public data object Full : ProgressiveBlurMode()

    /** Blends adjacent uniformly blurred images; [levels] includes the unchanged image. */
    public data class Layered(public val levels: Int = 8) : ProgressiveBlurMode() {
        init {
            require(levels >= 2) { "Layered progressive blur needs at least two levels" }
        }
    }

    /** Applies the ramp to the alpha of one uniformly blurred image. */
    public data object Alpha : ProgressiveBlurMode()
}

/** Default algorithm for built-in progressive backgrounds; a surface can override it. */
public val LocalProgressiveBlurMode: ProvidableCompositionLocal<ProgressiveBlurMode> =
    compositionLocalOf {
        ProgressiveBlurMode.Auto
    }

internal fun ProgressiveBlurMode.resolve(
    shaderSupported: Boolean = isRuntimeShaderSupported(),
    blurSupported: Boolean = isRenderEffectSupported(),
): ProgressiveBlurMode? =
    when (this) {
        Auto ->
            when {
                shaderSupported -> ProgressiveBlurMode.Full
                blurSupported -> ProgressiveBlurMode.Layered()
                else -> null
            }
        Full -> takeIf { shaderSupported }
        else -> takeIf { blurSupported }
    }

/**
 * Processes the current image with [mode]. Unsupported algorithms produce a transparent image.
 * [ProgressiveBlurMode.Auto] selects the full shader when supported, otherwise layered blur.
 * Geometry and ramp coverage are shared by all algorithms. This low-level operation leaves
 * preference and accessibility checks to the caller; Material surfaces perform those checks before
 * drawing.
 */
public fun BackdropImageScope.progressiveBlur(
    radius: Float,
    edge: BackdropEdge,
    ramp: BackdropRamp = BackdropRamp(),
    mode: ProgressiveBlurMode = ProgressiveBlurMode.Auto,
) {
    require(radius.isFinite() && radius >= 0f) { "Blur radius must be finite and non-negative" }
    require(ramp.from.isFinite() && ramp.from in 0f..1f && ramp.to.isFinite() && ramp.to in 0f..1f)
    when (val resolved = mode.resolve()) {
        Full -> {
            val minimumArea = minimumAreaForDownsampledBlur(radius, ramp)
            val geometry =
                if (minimumArea.isFinite() && size.let { it.width * it.height } >= minimumArea)
                    with(density) { progressiveBlurGeometry(size, layoutDirection, edge, ramp) }
                else null
            if (geometry != null) downsampledProgressiveBlur(radius, ramp, geometry)
            else effects { progressiveBlur(radius, edge, ramp) }
        }
        is Layered -> layeredProgressiveBlur(radius, edge, ramp, resolved.levels)
        Alpha -> {
            effects {
                coverRamp(ramp, edge)
                blur(radius)
            }
            val coverage = image {
                paint { room ->
                    val side = edge.side(layoutDirection)
                    val thickness = if (side.isVertical) room.size.height else room.size.width
                    val span =
                        ramp.span(
                            BackdropAxis(
                                side,
                                0f..thickness,
                                0f..thickness,
                                density,
                                fontScale,
                            ),
                        )
                    fun at(coordinate: Float): Offset {
                        val along = if (side.carriesContent) thickness - coordinate else coordinate
                        return if (side.isVertical) Offset(0f, along) else Offset(along, 0f)
                    }
                    val start = at(span.start)
                    val end = at(span.endInclusive)
                    drawRect(
                        ramp.brush(Color.White, start, end),
                        room.extension.originInNode,
                        Size(
                            room.size.width + room.extension.left + room.extension.right,
                            room.size.height + room.extension.top + room.extension.bottom,
                        ),
                    )
                }
            }
            mask(coverage)
        }
        else -> group {}
    }
}
