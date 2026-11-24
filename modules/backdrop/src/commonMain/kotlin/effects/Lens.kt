package top.ltfan.m3x.backdrop.effects

import androidx.annotation.FloatRange
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import top.ltfan.m3x.backdrop.BackdropEffectScope
import top.ltfan.m3x.backdrop.BackdropEffectScopeImpl
import top.ltfan.m3x.backdrop.BackdropLens
import top.ltfan.m3x.backdrop.RuntimeShader
import top.ltfan.m3x.backdrop.highlight.BoundaryGeometryProvider
import top.ltfan.m3x.backdrop.highlight.HighlightBoundaryGeometry
import top.ltfan.m3x.backdrop.highlight.bindBoundary
import top.ltfan.m3x.backdrop.highlight.boundaryUniformCapacity
import top.ltfan.m3x.backdrop.internal.RoundedRectRefractionShader
import top.ltfan.m3x.backdrop.internal.RoundedRectRefractionWithDispersionShader
import top.ltfan.m3x.backdrop.internal.contourLensSampling
import top.ltfan.m3x.backdrop.internal.lensRefractionShader
import top.ltfan.m3x.backdrop.internal.lensSampling
import top.ltfan.m3x.backdrop.isRuntimeShaderSupported
import top.ltfan.m3x.compose.shape.ContourShape

/** Refracts near the resolved silhouette. Rectangles and circular corners use an analytic path. */
public fun BackdropEffectScope.lens(
    @FloatRange(from = 0.0) refractionHeight: Float,
    @FloatRange(from = 0.0) refractionAmount: Float,
    depthEffect: Boolean = false,
    chromaticAberration: Boolean = false,
): Unit = applyLens(refractionHeight, refractionAmount, depthEffect, chromaticAberration)

private fun BackdropEffectScope.applyLens(
    refractionHeight: Float,
    refractionAmount: Float,
    depthEffect: Boolean,
    chromaticAberration: Boolean,
    boundaries: LensBoundaryCache? = null,
) {
    require(refractionHeight.isFinite() && refractionHeight >= 0f) {
        "refractionHeight must be finite and non-negative."
    }
    require(refractionAmount.isFinite() && refractionAmount >= 0f) {
        "refractionAmount must be finite and non-negative."
    }
    if (!isRuntimeShaderSupported()) return
    if (refractionHeight <= 0f || refractionAmount <= 0f) return

    val effectSize = size
    if (effectSize.isEmpty()) return
    val scope = this as BackdropEffectScopeImpl
    val resolved =
        (boundaries ?: scope.lensBoundaryCache).get(shape, effectSize, layoutDirection, this)
    val boundary = resolved.geometry
    if (boundary.isEmpty) return
    val radii = resolved.radii
    val contour = if (radii == null) resolved.contour else null
    val sampling =
        if (radii != null)
            lensSampling(effectSize, radii, refractionHeight, refractionAmount, chromaticAberration)
        else
            contourLensSampling(
                effectSize,
                boundary.bounds,
                refractionHeight,
                refractionAmount,
                chromaticAberration,
            )
    val bind: RuntimeShader.() -> Unit = {
        val extension = scope.extension
        setFloatUniform("size", effectSize.width, effectSize.height)
        if (radii != null) setFloatUniform("cornerRadii", radii)
        contour?.bind(this, refractionHeight)
        setFloatUniform("origin", extension.left, extension.top)
        setFloatUniform("refractionHeight", refractionHeight)
        setFloatUniform("refractionAmount", -refractionAmount)
        setFloatUniform("depthEffect", if (depthEffect) 1f else 0f)
    }
    if (contour != null) {
        runtimeShaderEffect("Lens", contour.shader(chromaticAberration), "content", sampling, bind)
    } else {
        val program =
            if (chromaticAberration) RoundedRectRefractionWithDispersionShader
            else RoundedRectRefractionShader
        runtimeShaderEffect("Lens", program, "content", sampling, bind)
    }
}

internal class ContourLens(private val boundary: HighlightBoundaryGeometry) {
    private val capacity = boundaryUniformCapacity(boundary.segmentCount)
    private val groupSize =
        if (!boundary.isAnalytic && !boundary.usesConvexSupport && capacity >= 32) 8 else 0

    private val singleSample by lazy { createShader(dispersion = false) }
    private val dispersed by lazy { createShader(dispersion = true) }

    fun shader(dispersion: Boolean): String = if (dispersion) dispersed else singleSample

    private fun createShader(dispersion: Boolean): String {
        val functions =
            boundary.shaderFunctions(
                capacity,
                groupSize,
                unrollConvexSupportSearch = boundary.segmentCount <= 8,
            )
        val search =
            if (boundary.isAnalytic) "nearestBoundaryWithPosition(point, closest)"
            else "nearestBoundaryWithPosition(point, refractionHeight, closest)"
        return lensRefractionShader(
            contourRefractionBoundary(functions, search),
            dispersion,
            skipInterior = true,
        )
    }

    fun bind(shader: RuntimeShader, height: Float) {
        boundary.bindBoundary(shader, capacity, groupSize)
        val interior = contourSafeInterior(boundary, height)
        shader.setFloatUniform(
            "safeInterior",
            interior.left,
            interior.top,
            interior.right,
            interior.bottom,
        )
    }
}

internal class ResolvedLensBoundary(val geometry: HighlightBoundaryGeometry, size: Size) {
    val radii: FloatArray? = geometry.centeredCircularRadii(size)
    val contour: ContourLens by lazy { ContourLens(geometry) }
}

/** Reuses flattened geometry across optics updates while observing outline and contour state. */
internal class LensBoundaryCache : BoundaryGeometryProvider {
    private data class Key(
        val shape: Shape,
        val size: Size,
        val direction: LayoutDirection,
        val density: Density,
    )

    private var key: Key? = null
    private var geometry: State<ResolvedLensBoundary>? = null

    fun get(
        shape: Shape,
        size: Size,
        direction: LayoutDirection,
        density: Density,
    ): ResolvedLensBoundary {
        val fixedDensity = Density(density.density, density.fontScale)
        val next = Key(shape, size, direction, fixedDensity)
        if (key != next) {
            geometry = derivedStateOf {
                val outline =
                    HighlightBoundaryGeometry(shape.createOutline(size, direction, fixedDensity))
                val boundary =
                    if (outline.isAnalytic) outline
                    else
                        (shape as? ContourShape)
                            ?.createContour(size, direction, fixedDensity)
                            ?.takeIf { it.loops.size == 1 }
                            ?.let(::HighlightBoundaryGeometry) ?: outline
                ResolvedLensBoundary(boundary, size)
            }
            key = next
        }
        return checkNotNull(geometry).value
    }

    override fun geometry(
        shape: Shape,
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): HighlightBoundaryGeometry = get(shape, size, layoutDirection, density).geometry

    fun clear() {
        geometry = null
        key = null
    }
}

/**
 * Applies dp distances with the same shape, validation and platform contract as the pixel overload.
 */
public fun BackdropEffectScope.lens(config: BackdropLens): Unit =
    lens(
        config.refractionHeight.toPx(),
        config.refractionAmount.toPx(),
        config.depthEffect,
        config.chromaticAberration,
    )

internal fun BackdropEffectScope.lens(config: BackdropLens, boundaries: LensBoundaryCache) =
    applyLens(
        config.refractionHeight.toPx(),
        config.refractionAmount.toPx(),
        config.depthEffect,
        config.chromaticAberration,
        boundaries,
    )

private fun contourSafeInterior(boundary: HighlightBoundaryGeometry, height: Float): Rect {
    val margin = height + maxOf(1f, boundary.coordinateScale * .000002f)
    return boundary.contourFreeBounds.deflate(margin).takeUnless { it.isEmpty } ?: Rect.Zero
}

private fun contourRefractionBoundary(functions: String, search: String): String =
    """
$functions
float3 refractionBoundary(float2 point, float2 centeredCoord) {
  float2 closest;
  return $search;
}
"""
