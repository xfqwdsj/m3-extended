package top.ltfan.m3x.backdrop.highlight

import androidx.annotation.FloatRange
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.ltfan.m3x.backdrop.BackdropGeometry
import top.ltfan.m3x.backdrop.BackdropImage
import top.ltfan.m3x.backdrop.BackdropImageScope
import top.ltfan.m3x.backdrop.BackdropPrimitive
import top.ltfan.m3x.backdrop.BackdropPrimitiveDrawContext
import top.ltfan.m3x.backdrop.BackdropPrimitiveInput
import top.ltfan.m3x.backdrop.BackdropPrimitiveRenderer
import top.ltfan.m3x.backdrop.BackdropRenderResources
import top.ltfan.m3x.backdrop.RenderImageNode
import top.ltfan.m3x.backdrop.effects.blur
import top.ltfan.m3x.backdrop.interaction.InteractionLighting
import top.ltfan.m3x.backdrop.interaction.interactionLight
import top.ltfan.m3x.backdrop.internal.BoundaryFlatness
import top.ltfan.m3x.backdrop.internal.BoundaryGeometryProvider
import top.ltfan.m3x.backdrop.internal.ShapeProvider
import top.ltfan.m3x.backdrop.isRuntimeShaderSupported
import top.ltfan.m3x.backdrop.primitive
import top.ltfan.m3x.backdrop.requireAlpha

private val DefaultBasicWidth = 1.dp
private val DefaultContourWidth = 1.2.dp
private val DefaultBlurRadius = 12.dp
private const val DefaultColorGain = .04f
private const val DefaultEnvironmentGain = 1.46f
private val DefaultDirection = HighlightDirection()

/** Maximum curve-to-segment deviation in local pixels for contour highlights. */
public const val HighlightFlatness: Float = BoundaryFlatness

private val DefaultDisabledAngles
    get() = emptySet<ClosedFloatingPointRange<Float>>()

/**
 * Parameters for a surface highlight. [Contour] supports directional lighting and an optional
 * Gaussian-filtered environment image. [Basic] draws a uniform inner stroke.
 */
@Stable
public sealed interface Highlight {
    /** Omits the contour primitive and releases its rendering resources. */
    @Stable public data object None : Highlight

    /** Draws a uniform inner stroke in [color]. */
    @Stable
    public data class Basic(
        public val width: Dp = DefaultBasicWidth,
        @param:FloatRange(from = 0.0) public val colorGain: Float = DefaultColorGain,
        @param:FloatRange(from = 0.0, to = 1.0) public val opacity: Float = 1f,
        public val color: Color = Color.White,
    ) : Highlight {
        init {
            validateHighlightStroke(width, colorGain, opacity, color)
        }
    }

    /** Evaluates directional lighting along the contour. */
    @Stable
    public class Contour
    public constructor(
        public val width: Dp = DefaultContourWidth,
        public val blurRadius: Dp = DefaultBlurRadius,
        @param:FloatRange(from = 0.0) public val colorGain: Float = DefaultColorGain,
        @param:FloatRange(from = 0.0) public val environmentGain: Float = DefaultEnvironmentGain,
        @param:FloatRange(from = 0.0, to = 1.0) public val opacity: Float = 1f,
        public val direction: HighlightDirection = DefaultDirection,
        disabledAngles: Set<ClosedFloatingPointRange<Float>> = DefaultDisabledAngles,
        public val disabledAngleDomain: HighlightAngleDomain = HighlightAngleDomain.Directed,
        public val color: Color = Color.White,
    ) : Highlight {
        public val disabledAngles: Set<ClosedFloatingPointRange<Float>> =
            FrozenSet(
                disabledAngles
                    .map { range ->
                        val limit =
                            if (disabledAngleDomain == HighlightAngleDomain.Unsigned) 180f else 360f
                        require(
                            range.start.isFinite() &&
                                range.endInclusive.isFinite() &&
                                range.start >= 0f &&
                                range.endInclusive <= limit &&
                                range.start <= range.endInclusive,
                        ) {
                            "Disabled ranges must be ordered and fit their angular domain"
                        }
                        range.start..range.endInclusive
                    }
                    .toSet(),
            )

        internal val cuts: Set<HighlightAngularCut> = buildSet {
            for (range in this@Contour.disabledAngles) {
                val middle = (range.start + range.endInclusive) * .5f
                val half =
                    minOf(180f, (range.endInclusive - range.start) * .5f + HighlightAngleTolerance)
                add(HighlightAngularCut(direction.angle + middle, half, 0f))
                if (disabledAngleDomain == HighlightAngleDomain.Unsigned)
                    add(HighlightAngularCut(direction.angle - middle, half, 0f))
            }
        }

        internal val cutUniforms: FloatArray = cuts.shaderValues()

        init {
            validateHighlightStroke(width, colorGain, opacity, color)
            require(blurRadius.value.isFinite() && blurRadius.value >= 0f) {
                "blurRadius must be finite and non-negative"
            }
            require(environmentGain.isFinite() && environmentGain >= 0f) {
                "environmentGain must be finite and non-negative"
            }
        }

        public fun copy(
            width: Dp = this.width,
            blurRadius: Dp = this.blurRadius,
            colorGain: Float = this.colorGain,
            environmentGain: Float = this.environmentGain,
            opacity: Float = this.opacity,
            direction: HighlightDirection = this.direction,
            disabledAngles: Set<ClosedFloatingPointRange<Float>> = this.disabledAngles,
            disabledAngleDomain: HighlightAngleDomain = this.disabledAngleDomain,
            color: Color = this.color,
        ): Contour =
            Highlight(
                width = width,
                blurRadius = blurRadius,
                colorGain = colorGain,
                environmentGain = environmentGain,
                opacity = opacity,
                direction = direction,
                disabledAngles = disabledAngles,
                disabledAngleDomain = disabledAngleDomain,
                color = color,
            )

        public operator fun component1(): Dp = width

        public operator fun component2(): Dp = blurRadius

        public operator fun component3(): Float = colorGain

        public operator fun component4(): Float = environmentGain

        public operator fun component5(): Float = opacity

        public operator fun component6(): HighlightDirection = direction

        public operator fun component7(): Set<ClosedFloatingPointRange<Float>> = disabledAngles

        public operator fun component8(): HighlightAngleDomain = disabledAngleDomain

        public operator fun component9(): Color = color

        override fun equals(other: Any?): Boolean =
            this === other ||
                (other is Contour &&
                    width == other.width &&
                    blurRadius == other.blurRadius &&
                    colorGain == other.colorGain &&
                    environmentGain == other.environmentGain &&
                    opacity == other.opacity &&
                    direction == other.direction &&
                    disabledAngles == other.disabledAngles &&
                    disabledAngleDomain == other.disabledAngleDomain &&
                    color == other.color)

        override fun hashCode(): Int {
            var result = width.hashCode()
            result = 31 * result + blurRadius.hashCode()
            result = 31 * result + colorGain.hashCode()
            result = 31 * result + environmentGain.hashCode()
            result = 31 * result + opacity.hashCode()
            result = 31 * result + direction.hashCode()
            result = 31 * result + disabledAngles.hashCode()
            result = 31 * result + disabledAngleDomain.hashCode()
            result = 31 * result + color.hashCode()
            return result
        }

        override fun toString(): String =
            "Highlight(width=$width, blurRadius=$blurRadius, colorGain=$colorGain, environmentGain=$environmentGain, opacity=$opacity, direction=$direction, disabledAngles=$disabledAngles, disabledAngleDomain=$disabledAngleDomain, color=$color)"
    }

    public companion object {
        @Stable public val Default: Contour = Highlight()
    }
}

@Suppress("FunctionName")
public fun Highlight(
    width: Dp = DefaultContourWidth,
    blurRadius: Dp = DefaultBlurRadius,
    @FloatRange(from = 0.0) colorGain: Float = DefaultColorGain,
    @FloatRange(from = 0.0) environmentGain: Float = DefaultEnvironmentGain,
    @FloatRange(from = 0.0, to = 1.0) opacity: Float = 1f,
    direction: HighlightDirection = DefaultDirection,
    disabledAngles: Set<ClosedFloatingPointRange<Float>> = DefaultDisabledAngles,
    disabledAngleDomain: HighlightAngleDomain = HighlightAngleDomain.Directed,
    color: Color = Color.White,
): Highlight.Contour =
    Highlight.Contour(
        width = width,
        blurRadius = blurRadius,
        colorGain = colorGain,
        environmentGain = environmentGain,
        opacity = opacity,
        direction = direction,
        disabledAngles = disabledAngles,
        disabledAngleDomain = disabledAngleDomain,
        color = color,
    )

private fun validateHighlightStroke(
    width: Dp,
    colorGain: Float,
    opacity: Float,
    color: Color,
) {
    require(width.value.isFinite() && width.value >= 0f) {
        "width must be finite and non-negative"
    }
    require(colorGain.isFinite() && colorGain >= 0f) {
        "colorGain must be finite and non-negative"
    }
    require(opacity.isFinite() && opacity in 0f..1f) {
        "opacity must be finite and between 0 and 1"
    }
    require(color.isSpecified) { "Highlight color must be specified" }
}

private class FrozenSet<T>(elements: Set<T>) : AbstractSet<T>() {
    private val values: Set<T> = elements.toSet()
    override val size: Int
        get() = values.size

    override fun contains(element: T): Boolean = values.contains(element)

    override fun iterator(): Iterator<T> = values.toList().iterator()
}

/**
 * Captures the input for a contour Gaussian. [outside] provides the original backdrop and
 * [clippedInside] provides the processed backdrop clipped with [BackdropImageScope.clipSurface].
 * Complementary coverage combines their premultiplied colors along the material boundary.
 */
public fun BackdropImageScope.contourEnvironment(
    outside: BackdropImage,
    clippedInside: BackdropImage,
    lighting: InteractionLighting = InteractionLighting.None,
): BackdropImage = image {
    val coverage = image { fill(Color.White) }
    replace(outside)
    mask(coverage, inverse = true)
    draw(clippedInside, blendMode = BlendMode.Plus)
    if (lighting is InteractionLighting.Config) interactionLight(lighting)
}

/** Appends a highlight using its configured fixed color. */
public fun BackdropImageScope.highlight(
    config: Highlight,
    alpha: Float = 1f,
    blendMode: BlendMode = BlendMode.SrcOver,
) {
    if (config === Highlight.None) return
    highlightWithConfig({ config }, null, alpha, blendMode)
}

/**
 * Appends a contour whose [environment] supplies color through a normalized Gaussian blur. Exterior
 * samples carry three times the weight of interior samples. At full opacity, [BlendMode.SrcOver]
 * replaces the destination at full coverage and blends with it at partial coverage. Tune both color
 * gains when composing the contour with [BlendMode.Plus].
 */
public fun BackdropImageScope.highlight(
    config: Highlight.Contour,
    environment: BackdropImage,
    alpha: Float = 1f,
    blendMode: BlendMode = BlendMode.SrcOver,
) {
    highlightWithConfig({ config }, environment, alpha, blendMode)
}

/**
 * Configures a contour highlight in declaration order. Drawing reads the visual parameters; the
 * Gaussian radius updates input sampling at resolution.
 */
public fun BackdropImageScope.highlight(
    environment: BackdropImage? = null,
    alpha: Float = 1f,
    blendMode: BlendMode = BlendMode.SrcOver,
    block: HighlightScope.() -> Unit,
) {
    highlightWithConfig({ Highlight(block) }, environment, alpha, blendMode)
}

internal fun BackdropImageScope.highlightWithConfig(
    config: () -> Highlight,
    environment: BackdropImage?,
    alpha: Float = 1f,
    blendMode: BlendMode = BlendMode.SrcOver,
) {
    requireAlpha(alpha)
    environment?.let(::checkImage)
    val highlight = EnvironmentHighlightPrimitive(config, boundaryGeometryCache)
    if (environment == null || !isRuntimeShaderSupported()) {
        primitive(highlight, alpha = alpha, blendMode = blendMode)
        return
    }

    val environmentRadius =
        derivedStateOf(structuralEqualityPolicy()) {
            (config() as? Highlight.Contour)?.blurRadius
        }
    val filtered = image {
        // Weight premultiplied RGB and alpha together. The contour shader divides by the blurred
        // alpha to recover the weighted mean. Scaling the interior keeps the weight in alpha range.
        val interiorWeightReduction = image {
            paint(CompositingStrategy.Auto) { room ->
                drawRect(Color.White, size = room.size, alpha = 2f / 3f)
            }
            clip()
        }
        replace(environment)
        mask(interiorWeightReduction, inverse = true)
        effects { blur((environmentRadius.value ?: 0.dp).toPx()) }
    }
    val directInput = listOf(BackdropPrimitiveInput(environment))
    val filteredInput = listOf(BackdropPrimitiveInput(filtered))
    drawPrimitive(
        RenderImageNode.DeferredPrimitive(listOf(environment, filtered)) {
            val inputs =
                when (environmentRadius.value) {
                    null -> emptyList()
                    0.dp -> directInput
                    else -> filteredInput
                }
            RenderImageNode.Primitive(
                highlight,
                inputs,
            )
        },
        alpha,
        blendMode,
    )
}

private data class EnvironmentHighlightPrimitive(
    val config: () -> Highlight,
    val boundaryProvider: BoundaryGeometryProvider?,
) : BackdropPrimitive<EnvironmentHighlightPrimitive> {
    override fun bounds(geometry: BackdropGeometry): Rect = Rect(Offset.Zero, geometry.size)

    override fun createRenderer(
        resources: BackdropRenderResources,
    ): BackdropPrimitiveRenderer<EnvironmentHighlightPrimitive> =
        Renderer(resources, boundaryProvider)

    private class Renderer(
        resources: BackdropRenderResources,
        boundaryProvider: BoundaryGeometryProvider?,
    ) : BackdropPrimitiveRenderer<EnvironmentHighlightPrimitive> {
        private val renderer = EnvironmentHighlightRenderer(resources, boundaryProvider)
        private var outlineShape: Shape? = null
        private var outlineProvider: ShapeProvider? = null

        override fun draw(
            scope: DrawScope,
            primitive: EnvironmentHighlightPrimitive,
            context: BackdropPrimitiveDrawContext,
        ) {
            val config = primitive.config()
            if (config === Highlight.None) {
                renderer.refresh()
                return
            }
            val shape = context.shape
            if (outlineShape != shape) {
                outlineShape = shape
                outlineProvider = ShapeProvider { shape }
            }
            renderer.draw(
                scope,
                config,
                checkNotNull(outlineProvider)
                    .shape
                    .createOutline(context.size, context.layoutDirection, context.density),
                context,
                environment =
                    if (config is Highlight.Contour && context.inputBounds.isNotEmpty())
                        context.input(0)
                    else null,
            )
        }

        override fun refresh() {
            renderer.refresh()
        }

        override fun release() {
            renderer.release()
        }
    }
}
