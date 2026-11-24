package top.ltfan.m3x.backdrop.highlight

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import top.ltfan.m3x.backdrop.BackdropImageInput
import top.ltfan.m3x.backdrop.BackdropPrimitiveDrawContext
import top.ltfan.m3x.backdrop.BackdropRenderResources
import top.ltfan.m3x.backdrop.PrimitiveResources
import top.ltfan.m3x.backdrop.RuntimeShader
import top.ltfan.m3x.backdrop.RuntimeShaderVariantCache
import top.ltfan.m3x.backdrop.internal.clipOutline
import top.ltfan.m3x.backdrop.internal.environmentHighlightTileShader
import top.ltfan.m3x.backdrop.internal.highlightDirectionUsesSharedCurve
import top.ltfan.m3x.backdrop.internal.setRuntimeShader
import top.ltfan.m3x.backdrop.isRuntimeShaderSupported
import top.ltfan.m3x.backdrop.shaderControls
import top.ltfan.m3x.compose.shape.Contour
import top.ltfan.m3x.compose.shape.ContourShape
import top.ltfan.m3x.compose.shape.toPath

internal fun boundaryUniformCapacity(candidateCount: Int): Int {
    require(candidateCount >= 0)
    var capacity = 1
    while (capacity < candidateCount) capacity *= 2
    return capacity
}

private data class ShaderSignature(
    val boundaryKind: BoundaryShaderKind?,
    val certifiedNormalsOnly: Boolean,
    val segmentCapacity: Int,
    val segmentGroupSize: Int,
    val sharedDirectionCurve: Boolean,
    val hasEnvironment: Boolean,
    val angularCapacity: Int,
    val exclusionCapacity: Int,
    val usesSdfValidityMask: Boolean,
)

/** Generates environment-lit contour pixels into receiver-owned layers. */
internal class EnvironmentHighlightRenderer(
    private val resources: BackdropRenderResources,
    private val boundaryProvider: BoundaryGeometryProvider? = null,
) {
    private var highlightLayer: GraphicsLayer? = null
    private val additionalLayers = mutableListOf<GraphicsLayer>()
    private val tiles = HighlightTiles()
    private val sdf =
        (resources as? PrimitiveResources)?.highlightSdfHost?.let(::HighlightSdfPreparation)
    private var boundaryOutline: Outline? = null
    private var boundaryContour: Contour? = null
    private var boundaryGeometry: HighlightBoundaryGeometry? = null
    private var surfaceSize = Size.Zero
    private var shaderSignature: ShaderSignature? = null
    private var currentShader: RuntimeShader? = null
    private val paint = Paint()
    private var clipPath: Path? = null
    private val runtimeShaderCache = RuntimeShaderVariantCache<ShaderSignature>()

    fun draw(
        scope: DrawScope,
        highlight: Highlight,
        outline: Outline,
        context: BackdropPrimitiveDrawContext,
        environment: BackdropImageInput?,
    ) {
        if (surfaceSize != scope.size) {
            surfaceSize = scope.size
            boundaryOutline = null
            boundaryContour = null
            boundaryGeometry = null
        }
        with(scope) {
            when (highlight) {
                is Highlight.Basic ->
                    drawBasic(
                        outline,
                        toHighlightPx(highlight.width),
                        highlight.colorGain,
                        highlight.opacity,
                        highlight.color,
                    )
                is Highlight.Contour -> drawContour(highlight, outline, context, environment)
                Highlight.None -> refresh()
            }
        }
    }

    fun refresh() {
        sdf?.reset()
        releaseOutputLayers()
    }

    fun release() {
        refresh()
        runtimeShaderCache.clear()
        shaderSignature = null
        currentShader = null
        boundaryOutline = null
        boundaryContour = null
        boundaryGeometry = null
        clipPath = null
    }

    private fun DrawScope.drawContour(
        highlight: Highlight.Contour,
        outline: Outline,
        context: BackdropPrimitiveDrawContext,
        environment: BackdropImageInput?,
    ) {
        val width = toHighlightPx(highlight.width)
        if (
            highlight.opacity <= 0f ||
                width == 0f ||
                ((highlight.colorGain == 0f || highlight.color.alpha == 0f) &&
                    (environment == null || highlight.environmentGain == 0f))
        ) {
            refresh()
            return
        }
        if (!isRuntimeShaderSupported()) {
            drawBasic(outline, width, highlight.colorGain, highlight.opacity, highlight.color)
            return
        }
        val maximumWidthScale = highlight.direction.maximumWidthScale
        require((width * maximumWidthScale).isFinite()) {
            "direction-scaled highlight width must remain finite in pixels"
        }
        val geometry = boundaryGeometry(outline, context)
        if (geometry.isEmpty) {
            refresh()
            return
        }
        val safeSize = IntSize(ceil(size.width).toInt(), ceil(size.height).toInt())
        val contourFreeBounds =
            if (geometry.isAnalytic && (environment == null || !supportsMergedHighlightTiles))
                Rect.Zero
            else geometry.contourFreeBounds
        val maximumWidth = width * maximumWidthScale
        tiles.prepare(safeSize, contourFreeBounds, maximumWidth)
        val field =
            sdf?.field(geometry, size, density, fontScale, layoutDirection, maximumWidth, safeSize)

        val angular = highlight.direction as? HighlightDirection.Angular
        val angularCapacity = angular?.profile?.capacity ?: 0
        val segmentCapacity = boundaryUniformCapacity(geometry.segmentCount)
        val segmentGroupSize = geometry.highlightSegmentGroupSize(segmentCapacity)
        val sharedDirectionCurve = highlightDirectionUsesSharedCurve(highlight.direction)
        val boundaryKind = if (field == null) geometry.shaderKind(segmentGroupSize) else null
        val signature =
            ShaderSignature(
                boundaryKind = boundaryKind,
                certifiedNormalsOnly =
                    boundaryKind == BoundaryShaderKind.Segments &&
                        geometry.certifiesNormalsFor(size),
                segmentCapacity =
                    if (boundaryKind == null || boundaryKind == BoundaryShaderKind.Analytic) 0
                    else segmentCapacity,
                segmentGroupSize =
                    if (boundaryKind == BoundaryShaderKind.Segments) segmentGroupSize else 0,
                sharedDirectionCurve = sharedDirectionCurve,
                hasEnvironment = environment != null,
                angularCapacity = angularCapacity,
                exclusionCapacity = highlight.cuts.size,
                usesSdfValidityMask = field?.usesValidityMask == true,
            )
        val shader =
            if (shaderSignature == signature) {
                checkNotNull(currentShader)
            } else {
                runtimeShaderCache
                    .obtain(signature) {
                        environmentHighlightTileShader(
                            boundary = geometry,
                            segmentCapacity = signature.segmentCapacity,
                            segmentGroupSize = signature.segmentGroupSize,
                            certifiedNormalsOnly = signature.certifiedNormalsOnly,
                            sharedDirectionCurve = signature.sharedDirectionCurve,
                            hasEnvironment = signature.hasEnvironment,
                            angularCapacity = signature.angularCapacity,
                            exclusionCapacity = signature.exclusionCapacity,
                            useSdf = signature.boundaryKind == null,
                            usesSdfValidityMask = signature.usesSdfValidityMask,
                        )
                    }
                    .also {
                        currentShader = it
                        shaderSignature = signature
                    }
            }
        if (field == null) geometry.bindBoundary(shader, segmentCapacity, segmentGroupSize)
        shader.setFloatUniform("tileBounds", 0f, 0f, size.width, size.height)
        shader.setFloatUniform("width", width)
        shader.setFloatUniform("colorGain", highlight.colorGain)
        shader.setColorUniform("fixedColor", highlight.color)
        shader.setFloatUniform("environmentGain", highlight.environmentGain)
        val radians = normalizedAngle(highlight.direction.angle) * (PI / 180.0).toFloat()
        shader.setFloatUniform("directionVector", cos(radians), sin(radians))
        if (angular != null) {
            angular.profile.upload(shader)
            shader.setFloatUniform("angularMaximumWidthScale", maximumWidthScale)
        } else {
            val incidence = highlight.direction as? HighlightDirection.Incidence
            shader.setFloatUniform("directionEnabled", if (incidence == null) 0f else 1f)
            shader.setFloatUniform(
                "directionMode",
                if (incidence?.mode == HighlightDirection.Mode.Single) 0f else 1f,
            )
            shader.setFloatUniform(
                "directionStrengthRange",
                incidence?.strength?.from ?: 1f,
                incidence?.strength?.to ?: 1f,
            )
            shader.setFloatUniform(
                "directionStrengthCurve",
                incidence?.strength?.curve?.shaderControls() ?: floatArrayOf(0f, 0f, 1f, 1f),
            )
            shader.setFloatUniform(
                "directionWidthRange",
                incidence?.widthScale?.from ?: 1f,
                incidence?.widthScale?.to ?: 1f,
            )
            if (!sharedDirectionCurve) {
                shader.setFloatUniform(
                    "directionWidthCurve",
                    incidence?.widthScale?.curve?.shaderControls() ?: floatArrayOf(0f, 0f, 1f, 1f),
                )
            }
        }
        if (highlight.cuts.isNotEmpty()) shader.setFloatUniform("exclusions", highlight.cutUniforms)

        val tileCount = field?.layout?.tiles?.size ?: tiles.count
        val mergedEffect =
            if (supportsMergedHighlightTiles && environment != null && tileCount > 1)
                mergeHighlightTiles(environment, shader, tiles, field)
            else null
        trimOutputLayers(if (mergedEffect == null) tileCount else 1)
        val recordingDensity = Density(density, fontScale)
        val roundedClipPath =
            if (outline is Outline.Rounded) clipPath ?: Path().also { clipPath = it } else null
        val canvas = drawContext.canvas
        canvas.save()
        try {
            canvas.clipOutline(outline, roundedClipPath)
            if (mergedEffect != null) {
                val layer = outputLayer(0)
                layer.alpha = highlight.opacity
                layer.blendMode = BlendMode.SrcOver
                layer.renderEffect = mergedEffect
                layer.record(recordingDensity, layoutDirection, safeSize) {
                    checkNotNull(environment).draw(this)
                }
                drawLayer(layer)
                return
            }
            for (index in 0 until tileCount) {
                val output = field?.layout?.tiles?.get(index)?.output ?: tiles[index]
                val storage =
                    IntRect(
                        maxOf(0, output.left - 1),
                        maxOf(0, output.top - 1),
                        if (output.right < safeSize.width) output.right + 1 else output.right,
                        if (output.bottom < safeSize.height) output.bottom + 1 else output.bottom,
                    )
                val storageSize = IntSize(storage.width, storage.height)
                val origin = Offset(-storage.left.toFloat(), -storage.top.toFloat())
                shader.setFloatUniform("origin", origin.x, origin.y)
                val layer = outputLayer(index)
                layer.alpha = highlight.opacity
                layer.blendMode = BlendMode.SrcOver
                val replayEffect = field?.bind(shader, index, origin, environment)
                if (environment != null) {
                    layer.renderEffect =
                        replayEffect
                            ?: environment.runtimeShaderEffect(shader, "environment", origin)
                    layer.record(recordingDensity, layoutDirection, storageSize) {
                        val recording = drawContext.canvas
                        recording.save()
                        try {
                            recording.translate(origin.x, origin.y)
                            environment.draw(this)
                        } finally {
                            recording.restore()
                        }
                    }
                } else if (replayEffect != null) {
                    layer.renderEffect = replayEffect
                    layer.record(recordingDensity, layoutDirection, storageSize) {
                        drawRect(Color.Transparent)
                    }
                } else {
                    layer.renderEffect = null
                    paint.setRuntimeShader(shader)
                    layer.record(recordingDensity, layoutDirection, storageSize) {
                        drawContext.canvas.drawRect(Rect(0f, 0f, size.width, size.height), paint)
                    }
                }
                canvas.save()
                try {
                    canvas.clipRect(
                        output.left.toFloat() - if (output.left == 0) 1f else 0f,
                        output.top.toFloat() - if (output.top == 0) 1f else 0f,
                        output.right.toFloat() + if (output.right == safeSize.width) 1f else 0f,
                        output.bottom.toFloat() + if (output.bottom == safeSize.height) 1f else 0f,
                    )
                    canvas.translate(storage.left.toFloat(), storage.top.toFloat())
                    drawLayer(layer)
                } finally {
                    canvas.restore()
                }
            }
        } finally {
            canvas.restore()
        }
    }

    private fun DrawScope.drawBasic(
        outline: Outline,
        width: Float,
        colorGain: Float,
        opacity: Float,
        color: Color,
    ) {
        refresh()
        if (width <= 0f || colorGain <= 0f) return
        val amount = minOf(1f, color.alpha * colorGain * opacity)
        if (amount <= 0f) return
        val path = outline.toPath()
        val canvas = drawContext.canvas
        canvas.save()
        try {
            canvas.clipOutline(outline, clipPath ?: Path().also { clipPath = it })
            drawPath(
                path,
                color.copy(alpha = amount),
                style =
                    Stroke(
                        width = width * 2f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
            )
        } finally {
            canvas.restore()
        }
    }

    private fun boundaryGeometry(
        outline: Outline,
        context: BackdropPrimitiveDrawContext,
    ): HighlightBoundaryGeometry {
        boundaryProvider
            ?.geometry(context.shape, context.size, context.layoutDirection, context.density)
            ?.let { shared ->
                if (boundaryGeometry !== shared) {
                    boundaryOutline = outline
                    boundaryContour = null
                    boundaryGeometry = shared
                }
                return shared
            }
        val cached = boundaryGeometry?.takeIf { boundaryOutline == outline }
        if (cached?.isAnalytic == true) return cached

        val outlineGeometry =
            if (cached == null && outline !is Outline.Generic) HighlightBoundaryGeometry(outline)
            else null
        val contour =
            if (outlineGeometry?.isAnalytic == true) null
            else
                (context.shape as? ContourShape)
                    ?.createContour(context.size, context.layoutDirection, context.density)
                    // Path normalization resolves the fill boundary when loops overlap or nest.
                    ?.takeIf { it.loops.size == 1 }
        if (cached != null && boundaryContour == contour) return cached

        val resolved =
            if (contour != null) HighlightBoundaryGeometry(contour)
            else outlineGeometry ?: HighlightBoundaryGeometry(outline)
        boundaryOutline = outline
        boundaryContour = contour
        boundaryGeometry = resolved
        return resolved
    }

    private fun outputLayer(index: Int): GraphicsLayer {
        if (index == 0)
            return highlightLayer ?: resources.createGraphicsLayer().also { highlightLayer = it }
        while (additionalLayers.size < index) additionalLayers += resources.createGraphicsLayer()
        return additionalLayers[index - 1]
    }

    private fun trimOutputLayers(count: Int) {
        while (additionalLayers.size > maxOf(0, count - 1)) {
            additionalLayers.removeAt(additionalLayers.lastIndex).let {
                it.renderEffect = null
                resources.releaseGraphicsLayer(it)
            }
        }
        if (count > 0) return
        highlightLayer?.let {
            it.renderEffect = null
            resources.releaseGraphicsLayer(it)
        }
        highlightLayer = null
    }

    private fun releaseOutputLayers() {
        trimOutputLayers(0)
        paint.setRuntimeShader(null)
    }
}

private fun DrawScope.toHighlightPx(width: Dp): Float =
    width.toPx().also {
        require(it.isFinite() && it >= 0f) {
            "highlight width must remain finite and non-negative in pixels"
        }
    }
