package top.ltfan.m3x.backdrop.internal

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastIsFinite
import kotlin.math.abs
import kotlin.math.sqrt
import top.ltfan.m3x.backdrop.RuntimeShader
import top.ltfan.m3x.compose.shape.Contour
import top.ltfan.m3x.compose.shape.ContourShape
import top.ltfan.m3x.compose.shape.toPath

/** Maximum curve-to-segment deviation used by shared boundary shader geometry. */
internal const val BoundaryFlatness: Float = .2f

/** Supplies the boundary representation resolved for one shape and render context. */
internal fun interface BoundaryGeometryProvider {
    fun geometry(
        shape: Shape,
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): BoundaryGeometry
}

/** Caches the resolved boundary and derived effect state for one declaration key. */
internal class BoundaryGeometryCache : BoundaryGeometryProvider {
    private data class Key(
        val shape: Shape,
        val size: Size,
        val layoutDirection: LayoutDirection,
        val density: Density,
    )

    internal class Entry(val geometry: BoundaryGeometry) {
        private val derived = mutableMapOf<Any, Any>()

        @Suppress("UNCHECKED_CAST")
        internal fun <T : Any> memoized(key: Any, create: () -> T): T =
            derived[key] as? T ?: create().also { derived[key] = it }
    }

    private var key: Key? = null
    private var entry: State<Entry>? = null

    fun resolve(
        shape: Shape,
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Entry {
        val fixedDensity = Density(density.density, density.fontScale)
        val next = Key(shape, size, layoutDirection, fixedDensity)
        if (key != next) {
            entry = derivedStateOf {
                val outlineGeometry =
                    BoundaryGeometry(shape.createOutline(size, layoutDirection, fixedDensity))
                val geometry =
                    if (outlineGeometry.isAnalytic) outlineGeometry
                    else
                        (shape as? ContourShape)
                            ?.createContour(size, layoutDirection, fixedDensity)
                            ?.takeIf { it.loops.size == 1 }
                            ?.let(::BoundaryGeometry) ?: outlineGeometry
                Entry(geometry)
            }
            key = next
        }
        return checkNotNull(entry).value
    }

    override fun geometry(
        shape: Shape,
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): BoundaryGeometry = resolve(shape, size, layoutDirection, density).geometry

    fun clear() {
        entry = null
        key = null
    }
}

internal enum class BoundaryShaderKind {
    Analytic,
    Convex,
    Segments,
}

/** Cached analytic or flattened boundary representation for boundary-driven rendering. */
internal class BoundaryGeometry
private constructor(
    private val outline: Outline?,
    contour: Contour?,
) {
    internal constructor(outline: Outline) : this(outline, null)

    internal constructor(contour: Contour) : this(null, contour)

    private val directGeometry = contour?.let { FlattenedBoundaryGeometry.fromContour(it) }
    internal val bounds: Rect = outline?.bounds ?: requireNotNull(directGeometry).bounds
    private val analyticRadii: FloatArray? =
        when (outline) {
            is Rectangle -> FloatArray(4)
            is Rounded -> circularRadii(outline)?.takeIf { validRadii(bounds, it) }
            is Generic,
            null -> null
        }
    private var flattened: FlattenedBoundaryGeometry? = null
    private var cachedSegmentCapacity: Int = 0
    private var cachedPaddedSegments: FloatArray? = null
    private var cachedPaddedNormalSigns: FloatArray? = null
    private var cachedGroupSize: Int = 0
    private var cachedGroupBounds: FloatArray? = null

    /** An open rectangle containing no contour segments, independent of fill topology. */
    internal val contourFreeBounds: Rect by
        lazy(NONE) {
            val radii = analyticRadii
            val inset =
                if (radii != null) {
                    // At 45 degrees a circular corner is deepest along the diagonal of its square.
                    radii.max() * (1f - .70710677f)
                } else {
                    val segments = cpuGeometry().segments
                    // Each segment must lie entirely beyond at least one rectangle edge.
                    // Taking the largest per-segment inset certifies the shared empty region.
                    segments.indices.step(4).fold(0f) { currentInset, i ->
                        val minX = minOf(segments[i], segments[i + 2])
                        val maxX = maxOf(segments[i], segments[i + 2])
                        val minY = minOf(segments[i + 1], segments[i + 3])
                        val maxY = maxOf(segments[i + 1], segments[i + 3])
                        maxOf(
                            currentInset,
                            minOf(
                                maxX - bounds.left,
                                bounds.right - minX,
                                maxY - bounds.top,
                                bounds.bottom - minY,
                            ),
                        )
                    }
                }
            // Keep the certificate conservative under shader float rounding.
            val result = bounds.deflate(inset + coordinateScale * .000002f)
            if (result.isEmpty) Zero else result
        }

    private val allNormalsCertified: Boolean by
        lazy(NONE) {
            !isAnalytic &&
                cpuGeometry().segmentCount > 0 &&
                cpuGeometry().normalSigns.all { it != 0f }
        }

    /** Whether every point in the surface-local tile from zero to [size] has certified normals. */
    internal fun certifiesNormalsFor(size: Size): Boolean =
        size.width.fastIsFinite() &&
            size.height.fastIsFinite() &&
            size.width >= 0f &&
            size.height >= 0f &&
            size.width <= coordinateScale &&
            size.height <= coordinateScale &&
            allNormalsCertified

    internal val circularCornerRadii: FloatArray?
        get() = analyticRadii

    internal val isAnalytic: Boolean
        get() = analyticRadii != null

    /** A single convex contour whose interior uses supporting-line distance. */
    internal val usesConvexSupport: Boolean
        get() = !isAnalytic && cpuGeometry().convexSupportLines != null

    internal val isEmpty: Boolean
        get() = if (isAnalytic) bounds.isEmpty else cpuGeometry().segmentCount == 0

    internal val segmentCount: Int
        get() = if (isAnalytic) 0 else cpuGeometry().segmentCount

    internal val coordinateScale: Float
        get() =
            if (isAnalytic) {
                maxOf(1f, abs(bounds.left), abs(bounds.top), abs(bounds.right), abs(bounds.bottom))
            } else {
                cpuGeometry().coordinateScale
            }

    internal fun shaderKind(segmentGroupSize: Int): BoundaryShaderKind =
        when {
            isAnalytic -> Analytic
            segmentGroupSize == 0 && usesConvexSupport -> Convex
            else -> Segments
        }

    internal fun paddedSegments(capacity: Int): FloatArray {
        cacheUniformArrays(capacity)
        return requireNotNull(cachedPaddedSegments)
    }

    internal fun paddedNormalSigns(capacity: Int): FloatArray {
        cacheUniformArrays(capacity)
        return requireNotNull(cachedPaddedNormalSigns)
    }

    internal fun segmentGroupBounds(capacity: Int, groupSize: Int): FloatArray {
        cacheUniformArrays(capacity)
        if (cachedGroupSize != groupSize || cachedGroupBounds == null) {
            require(groupSize > 0 && capacity % groupSize == 0)
            val segments = requireNotNull(cachedPaddedSegments)
            cachedGroupBounds =
                FloatArray(capacity / groupSize * 4) { index ->
                    val group = index / 4
                    val component = index % 4
                    var value =
                        if (component < 2) Float.POSITIVE_INFINITY else Float.NEGATIVE_INFINITY
                    for (member in 0 until groupSize) {
                        val start = (group * groupSize + member) * 4
                        val a = segments[start + component % 2]
                        val b = segments[start + 2 + component % 2]
                        value = if (component < 2) minOf(value, a, b) else maxOf(value, a, b)
                    }
                    value
                }
            cachedGroupSize = groupSize
        }
        return requireNotNull(cachedGroupBounds)
    }

    private fun cacheUniformArrays(capacity: Int) {
        if (cachedSegmentCapacity != capacity || cachedPaddedSegments == null) {
            cachedGroupBounds = null
            cachedSegmentCapacity = capacity
            val geometry = cpuGeometry()
            cachedPaddedSegments = FloatArray(capacity * 4).also { geometry.segments.copyInto(it) }
            cachedPaddedNormalSigns =
                FloatArray(capacity).also { geometry.normalSigns.copyInto(it) }
        }
    }

    internal fun cpuGeometry(): FlattenedBoundaryGeometry =
        directGeometry
            ?: flattened
            ?: FlattenedBoundaryGeometry.fromPath(requireNotNull(outline).toPath()).also {
                flattened = it
            }

    internal fun contains(point: Offset): Boolean {
        val radii = analyticRadii ?: return cpuGeometry().contains(point)
        if (
            point.x < bounds.left ||
                point.x > bounds.right ||
                point.y < bounds.top ||
                point.y > bounds.bottom
        )
            return false
        val x = point.x
        val y = point.y
        val (topLeft, topRight, bottomRight, bottomLeft) = radii
        if (topLeft > 0f && x < bounds.left + topLeft && y < bounds.top + topLeft) {
            val dx = x - (bounds.left + topLeft)
            val dy = y - (bounds.top + topLeft)
            if (dx * dx + dy * dy > topLeft * topLeft) return false
        }
        if (topRight > 0f && x > bounds.right - topRight && y < bounds.top + topRight) {
            val dx = x - (bounds.right - topRight)
            val dy = y - (bounds.top + topRight)
            if (dx * dx + dy * dy > topRight * topRight) return false
        }
        if (bottomRight > 0f && x > bounds.right - bottomRight && y > bounds.bottom - bottomRight) {
            val dx = x - (bounds.right - bottomRight)
            val dy = y - (bounds.bottom - bottomRight)
            if (dx * dx + dy * dy > bottomRight * bottomRight) return false
        }
        if (bottomLeft > 0f && x < bounds.left + bottomLeft && y > bounds.bottom - bottomLeft) {
            val dx = x - (bounds.left + bottomLeft)
            val dy = y - (bounds.bottom - bottomLeft)
            if (dx * dx + dy * dy > bottomLeft * bottomLeft) return false
        }
        return true
    }

    internal fun nearestBoundary(point: Offset): Boundary =
        if (analyticRadii == null) {
            val boundary = cpuGeometry().nearestBoundary(point)
            Boundary(
                boundary.distance,
                boundary.outwardNormal,
                boundary.position,
                boundary.signedDistance,
            )
        } else {
            nearestAnalyticBoundary(point, analyticRadii)
        }

    internal fun configureShader(shader: RuntimeShader, originX: Float = 0f, originY: Float = 0f) {
        shader.setFloatUniform("origin", originX, originY)
        shader.setFloatUniform("geometryScale", coordinateScale)
        if (!isAnalytic) shader.setIntUniform("activeSegmentCount", segmentCount)
        if (isAnalytic) {
            shader.setFloatUniform(
                "boundaryBounds",
                bounds.left,
                bounds.top,
                bounds.right,
                bounds.bottom,
            )
            shader.setFloatUniform("cornerRadii", requireNotNull(analyticRadii))
        }
    }

    internal fun segmentGroupSize(segmentCapacity: Int): Int =
        if (!isAnalytic && segmentCapacity >= 32) 8 else 0

    internal fun shaderFunctions(
        segmentCapacity: Int,
        segmentGroupSize: Int = 0,
        certifiedNormalsOnly: Boolean = false,
        unrollConvexSupportSearch: Boolean = false,
    ): String =
        when (shaderKind(segmentGroupSize)) {
            Analytic -> analyticBoundaryShaderFunctions()
            Convex ->
                convexBoundaryShaderFunctions(
                    segmentCount,
                    segmentCapacity,
                    unrollConvexSupportSearch,
                )
            Segments ->
                genericBoundaryShaderFunctions(
                    segmentCount,
                    segmentCapacity,
                    segmentGroupSize,
                    certifiedNormalsOnly,
                )
        }

    private fun nearestAnalyticBoundary(point: Offset, radii: FloatArray): Boundary {
        val (topLeft, topRight, bottomRight, bottomLeft) = radii
        var bestDistance = Float.POSITIVE_INFINITY
        var bestPosition = Offset.Zero
        var bestNormal = Offset(0f, -1f)
        val tolerance =
            (8f * Float.fromBits(0x34000000)) * maxOf(coordinateScale, abs(point.x), abs(point.y))

        fun consider(position: Offset, normal: Offset) {
            val dx = point.x - position.x
            val dy = point.y - position.y
            val distance = sqrt(dx * dx + dy * dy)
            if (distance < bestDistance - tolerance) {
                bestDistance = distance
                bestPosition = position
                bestNormal = normal
            }
        }

        fun edge(start: Offset, end: Offset, normal: Offset) {
            val dx = end.x - start.x
            val dy = end.y - start.y
            val denominator = dx * dx + dy * dy
            val t =
                if (denominator > 0f)
                    (((point.x - start.x) * dx + (point.y - start.y) * dy) / denominator)
                        .fastCoerceIn(
                            0f,
                            1f,
                        )
                else 0f
            consider(Offset(start.x + dx * t, start.y + dy * t), normal)
        }

        edge(
            Offset(bounds.left + topLeft, bounds.top),
            Offset(bounds.right - topRight, bounds.top),
            Offset(0f, -1f),
        )
        edge(
            Offset(bounds.right, bounds.top + topRight),
            Offset(bounds.right, bounds.bottom - bottomRight),
            Offset(1f, 0f),
        )
        edge(
            Offset(bounds.right - bottomRight, bounds.bottom),
            Offset(bounds.left + bottomLeft, bounds.bottom),
            Offset(0f, 1f),
        )
        edge(
            Offset(bounds.left, bounds.bottom - bottomLeft),
            Offset(bounds.left, bounds.top + topLeft),
            Offset(-1f, 0f),
        )

        fun arc(
            radius: Float,
            center: Offset,
            xDirection: Float,
            yDirection: Float,
            fallback: Offset,
        ) {
            if (radius <= 0f) return
            val dx = point.x - center.x
            val dy = point.y - center.y
            if (dx * xDirection < 0f || dy * yDirection < 0f) return
            val length = sqrt(dx * dx + dy * dy)
            val normal = if (length > 0f) Offset(dx / length, dy / length) else fallback
            consider(Offset(center.x + normal.x * radius, center.y + normal.y * radius), normal)
        }

        arc(topLeft, Offset(bounds.left + topLeft, bounds.top + topLeft), -1f, -1f, Offset(0f, -1f))
        arc(
            topRight,
            Offset(bounds.right - topRight, bounds.top + topRight),
            1f,
            -1f,
            Offset(0f, -1f),
        )
        arc(
            bottomRight,
            Offset(bounds.right - bottomRight, bounds.bottom - bottomRight),
            1f,
            1f,
            Offset(0f, 1f),
        )
        arc(
            bottomLeft,
            Offset(bounds.left + bottomLeft, bounds.bottom - bottomLeft),
            -1f,
            1f,
            Offset(0f, 1f),
        )

        val inside = contains(point)
        val signedDistance = if (inside) -bestDistance else bestDistance
        return Boundary(bestDistance, bestNormal, bestPosition, signedDistance)
    }

    private fun circularRadii(outline: Outline.Rounded): FloatArray? {
        val roundRect = outline.roundRect
        val corners =
            listOf(
                roundRect.topLeftCornerRadius,
                roundRect.topRightCornerRadius,
                roundRect.bottomRightCornerRadius,
                roundRect.bottomLeftCornerRadius,
            )
        if (
            corners.fastAny {
                !it.x.fastIsFinite() ||
                    !it.y.fastIsFinite() ||
                    it.x < 0f ||
                    it.y < 0f ||
                    it.x != it.y
            }
        )
            return null
        return FloatArray(4) { corners[it].x }
    }

    private fun validRadii(bounds: Rect, radii: FloatArray): Boolean {
        val (topLeft, topRight, bottomRight, bottomLeft) = radii
        return topLeft + topRight <= bounds.width &&
            bottomLeft + bottomRight <= bounds.width &&
            topLeft + bottomLeft <= bounds.height &&
            topRight + bottomRight <= bounds.height
    }

    internal data class Boundary(
        val distance: Float,
        val outwardNormal: Offset,
        val position: Offset,
        val signedDistance: Float,
    )
}

internal fun BoundaryGeometry.bindBoundary(
    shader: RuntimeShader,
    capacity: Int,
    groupSize: Int,
) {
    configureShader(shader)
    if (!isAnalytic) {
        shader.setFloatUniform("segments", paddedSegments(capacity))
        shader.setFloatUniform("segmentNormalSigns", paddedNormalSigns(capacity))
        if (groupSize == 0 && usesConvexSupport)
            shader.setFloatUniform(
                "supportLines",
                cpuGeometry().paddedConvexSupportLines(capacity),
            )
    }
    if (groupSize > 0)
        shader.setFloatUniform("segmentGroupBounds", segmentGroupBounds(capacity, groupSize))
}

internal fun analyticBoundaryShaderFunctions(): String =
    """
uniform float2 origin;
uniform float geometryScale;
uniform float4 boundaryBounds;
uniform float4 cornerRadii;

float boundaryTolerance(float2 p) {
  float scale = max(geometryScale, max(abs(p.x), abs(p.y)));
  return 9.5367432e-7 * scale;
}
float4 chooseBoundary(float4 best, float2 p, float2 position, float2 outward, float tolerance, inout float2 bestPosition) {
  float distance = length(p - position);
  if (best.w < 0.5 || distance < best.x - tolerance) {
    bestPosition = position;
    return float4(distance, outward, 1.0);
  }
  return best;
}
float2 nearestBoundaryCandidate(float2 p, out float distance, out float2 outward, out float2 closest) {
  float left = boundaryBounds.x;
  float top = boundaryBounds.y;
  float right = boundaryBounds.z;
  float bottom = boundaryBounds.w;
  float4 radii = cornerRadii;
  float tolerance = boundaryTolerance(p);
  float4 best = float4(3.402823466e+38, 0.0, -1.0, 0.0);
  float2 bestPosition = float2(0.0);
  float x = clamp(p.x, left + radii.x, right - radii.y);
  best = chooseBoundary(best, p, float2(x, top), float2(0.0, -1.0), tolerance, bestPosition);
  float y = clamp(p.y, top + radii.y, bottom - radii.z);
  best = chooseBoundary(best, p, float2(right, y), float2(1.0, 0.0), tolerance, bestPosition);
  x = clamp(p.x, left + radii.w, right - radii.z);
  best = chooseBoundary(best, p, float2(x, bottom), float2(0.0, 1.0), tolerance, bestPosition);
  y = clamp(p.y, top + radii.x, bottom - radii.w);
  best = chooseBoundary(best, p, float2(left, y), float2(-1.0, 0.0), tolerance, bestPosition);

  float2 delta = p - float2(left + radii.x, top + radii.x);
  if (radii.x > 0.0 && delta.x <= 0.0 && delta.y <= 0.0) {
    float length = length(delta);
    float2 normal = length > 0.0 ? delta / length : float2(0.0, -1.0);
    best = chooseBoundary(best, p, float2(left + radii.x, top + radii.x) + normal * radii.x, normal, tolerance, bestPosition);
  }
  delta = p - float2(right - radii.y, top + radii.y);
  if (radii.y > 0.0 && delta.x >= 0.0 && delta.y <= 0.0) {
    float length = length(delta);
    float2 normal = length > 0.0 ? delta / length : float2(0.0, -1.0);
    best = chooseBoundary(best, p, float2(right - radii.y, top + radii.y) + normal * radii.y, normal, tolerance, bestPosition);
  }
  delta = p - float2(right - radii.z, bottom - radii.z);
  if (radii.z > 0.0 && delta.x >= 0.0 && delta.y >= 0.0) {
    float length = length(delta);
    float2 normal = length > 0.0 ? delta / length : float2(0.0, 1.0);
    best = chooseBoundary(best, p, float2(right - radii.z, bottom - radii.z) + normal * radii.z, normal, tolerance, bestPosition);
  }
  delta = p - float2(left + radii.w, bottom - radii.w);
  if (radii.w > 0.0 && delta.x <= 0.0 && delta.y >= 0.0) {
    float length = length(delta);
    float2 normal = length > 0.0 ? delta / length : float2(0.0, 1.0);
    best = chooseBoundary(best, p, float2(left + radii.w, bottom - radii.w) + normal * radii.w, normal, tolerance, bestPosition);
  }
  distance = best.x;
  outward = best.yz;
  closest = bestPosition;
  return bestPosition;
}
bool containsPoint(float2 p) {
  if (p.x < boundaryBounds.x || p.x > boundaryBounds.z || p.y < boundaryBounds.y || p.y > boundaryBounds.w) return false;
  float4 r = cornerRadii;
  if (r.x > 0.0 && p.x < boundaryBounds.x + r.x && p.y < boundaryBounds.y + r.x && dot(p - float2(boundaryBounds.x + r.x, boundaryBounds.y + r.x), p - float2(boundaryBounds.x + r.x, boundaryBounds.y + r.x)) > r.x * r.x) return false;
  if (r.y > 0.0 && p.x > boundaryBounds.z - r.y && p.y < boundaryBounds.y + r.y && dot(p - float2(boundaryBounds.z - r.y, boundaryBounds.y + r.y), p - float2(boundaryBounds.z - r.y, boundaryBounds.y + r.y)) > r.y * r.y) return false;
  if (r.z > 0.0 && p.x > boundaryBounds.z - r.z && p.y > boundaryBounds.w - r.z && dot(p - float2(boundaryBounds.z - r.z, boundaryBounds.w - r.z), p - float2(boundaryBounds.z - r.z, boundaryBounds.w - r.z)) > r.z * r.z) return false;
  if (r.w > 0.0 && p.x < boundaryBounds.x + r.w && p.y > boundaryBounds.w - r.w && dot(p - float2(boundaryBounds.x + r.w, boundaryBounds.w - r.w), p - float2(boundaryBounds.x + r.w, boundaryBounds.w - r.w)) > r.w * r.w) return false;
  return true;
}
float3 nearestBoundaryWithPosition(float2 p, out float2 closest) {
  float distance;
  float2 outward;
  nearestBoundaryCandidate(p, distance, outward, closest);
  return float3(distance, outward);
}
float3 nearestBoundary(float2 p) { float distance; float2 outward; float2 closest; nearestBoundaryCandidate(p, distance, outward, closest); return float3(distance, outward); }
"""
