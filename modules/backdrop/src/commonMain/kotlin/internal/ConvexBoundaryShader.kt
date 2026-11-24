package top.ltfan.m3x.backdrop.internal

/**
 * Supporting-line distance for the interior of one convex closed contour, with finite-segment
 * search outside it. Callers bind `activeSegmentCount`, `segments`, `segmentNormalSigns`, and
 * `supportLines` in matching order.
 */
internal fun convexBoundaryShaderFunctions(
    segmentCount: Int,
    segmentCapacity: Int = segmentCount,
    unrollSupportSearch: Boolean = false,
): String {
    require(segmentCount >= 3)
    require(segmentCapacity >= segmentCount)
    val supportSearch =
        if (unrollSupportSearch) {
            (0 until segmentCount).joinToString("\n") { index ->
                "{ ${supportStep(index.toString())} }"
            }
        } else {
            loopSupportSearch(segmentCapacity)
        }
    return convexBoundarySource(segmentCapacity, supportSearch)
}

private fun supportStep(index: String): String =
    """
float4 line = supportLines[$index];
float signedDistance = dot(p - line.zw, line.xy);
if (signedDistance > tolerance) inside = false;
float distance = -signedDistance;
if (distance < selectedDistance - tolerance) {
  selectedDistance = distance;
  bestOutward = line.xy;
}
"""

private fun loopSupportSearch(segmentCapacity: Int): String =
    """
for (int i = 0; i < $segmentCapacity; ++i) {
  if (i >= activeSegmentCount) break;
${supportStep("i")}
}
"""

private fun convexBoundarySource(segmentCapacity: Int, supportSearch: String): String =
    """
uniform float2 origin;
uniform float geometryScale;
uniform int activeSegmentCount;
uniform float4 segments[$segmentCapacity];
uniform float segmentNormalSigns[$segmentCapacity];
uniform float4 supportLines[$segmentCapacity];

float boundaryTolerance(float2 p) {
  float scale = max(geometryScale, max(abs(p.x), abs(p.y)));
  return 9.5367432e-7 * scale;
}

bool containsPoint(float2 p) {
  float tolerance = boundaryTolerance(p);
  for (int i = 0; i < $segmentCapacity; ++i) {
    if (i >= activeSegmentCount) break;
    float4 line = supportLines[i];
    if (dot(p - line.zw, line.xy) > tolerance) return false;
  }
  return true;
}

float3 nearestBoundaryWithPosition(float2 p, float maximumDistance, out float2 closest) {
  float tolerance = boundaryTolerance(p);
  float selectedDistance = 3.402823466e+38;
  float2 bestOutward = float2(0.0, -1.0);
  bool inside = true;
$supportSearch
  if (inside) {
    float distance = max(selectedDistance, 0.0);
    closest = p + bestOutward * distance;
    return float3(distance, bestOutward);
  }

  selectedDistance = 3.402823466e+38;
  float2 bestDelta = float2(0.0);
  float2 bestTangent = float2(1.0, 0.0);
  float bestNormalSign = 1.0;
  bool hasBest = false;
  float searchDistance = maximumDistance + tolerance * float(activeSegmentCount);
  for (int i = 0; i < $segmentCapacity; ++i) {
    if (i >= activeSegmentCount) break;
    float4 segment = segments[i];
    float2 a = segment.xy;
    float2 b = segment.zw;
    float2 lower = max(min(a, b) - p, p - max(a, b));
    if (max(lower.x, lower.y) > min(selectedDistance, searchDistance) + tolerance * 2.0) continue;
    float2 ab = b - a;
    float denominator = dot(ab, ab);
    if (!(denominator > 0.0)) continue;
    float t = clamp(dot(p - a, ab) / denominator, 0.0, 1.0);
    float2 delta = a + t * ab - p;
    float distance = length(delta);
    if (!hasBest || distance < selectedDistance - tolerance) {
      selectedDistance = distance;
      bestDelta = delta;
      bestTangent = ab;
      bestNormalSign = segmentNormalSigns[i];
      hasBest = true;
    }
  }
  closest = p + bestDelta;
  float tangentLength = length(bestTangent);
  float2 outward =
    tangentLength > 0.0
      ? float2(bestTangent.y, -bestTangent.x) / tangentLength * bestNormalSign
      : float2(0.0, -1.0);
  return float3(selectedDistance, outward);
}

float3 nearestBoundaryWithPosition(float2 p, out float2 closest) {
  return nearestBoundaryWithPosition(p, 3.402823466e+38, closest);
}

float3 nearestBoundary(float2 p) {
  float2 closest;
  return nearestBoundaryWithPosition(p, closest);
}
"""
