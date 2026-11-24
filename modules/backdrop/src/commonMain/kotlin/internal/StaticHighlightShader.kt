package top.ltfan.m3x.backdrop.internal

import top.ltfan.m3x.backdrop.highlight.HighlightBoundaryGeometry
import top.ltfan.m3x.backdrop.highlight.HighlightDirection
import top.ltfan.m3x.backdrop.highlight.angularDirectionShader
import top.ltfan.m3x.backdrop.highlight.exclusionShader

/** Shades one nearest-contour response from a prefiltered environment image. */
internal fun environmentHighlightTileShader(
    boundary: HighlightBoundaryGeometry,
    segmentCapacity: Int = boundary.segmentCount,
    segmentGroupSize: Int = 0,
    certifiedNormalsOnly: Boolean = false,
    sharedDirectionCurve: Boolean = false,
    hasEnvironment: Boolean = false,
    angularCapacity: Int = 0,
    exclusionCapacity: Int = 0,
    useSdf: Boolean = false,
    usesSdfValidityMask: Boolean = false,
): String {
    val dataSample =
        if (usesSdfValidityMask) {
            SdfSampleWithValidity
        } else {
            "float3 data = (float3(sdfField.eval(position * sdfMap.z + sdfMap.xy).rgb) * 255.0 - 128.0) / 127.0;"
        }
    val geometry =
        if (useSdf) {
            sdfGeometrySource(dataSample)
        } else {
            boundary.shaderFunctions(segmentCapacity, segmentGroupSize, certifiedNormalsOnly)
        }
    val directionWidthCurveUniform =
        if (sharedDirectionCurve) "" else "uniform float4 directionWidthCurve;"
    val directionCurveValues =
        if (angularCapacity > 0) {
            AngularDirectionValues
        } else if (sharedDirectionCurve) {
            SharedDirectionValues
        } else {
            SeparateDirectionValues
        }
    val directionUniforms =
        if (angularCapacity > 0) {
            "uniform float angularMaximumWidthScale;\n" + angularDirectionShader(angularCapacity)
        } else {
            directionUniformsSource(directionWidthCurveUniform)
        }
    val maximumWidth =
        if (angularCapacity > 0) "angularMaximumWidthScale"
        else "max(directionWidthRange.x, directionWidthRange.y)"
    val nearestQuery =
        if (useSdf) "sampleSdf(p)"
        else if (boundary.isAnalytic) "nearestBoundaryWithPosition(p, nearestPosition)"
        else "nearestBoundaryWithPosition(p, conservativeUpper, nearestPosition)"
    val environmentDeclaration = if (hasEnvironment) "uniform shader environment;" else ""
    val environmentContribution =
        if (hasEnvironment) {
            EnvironmentContribution
        } else ""
    val output =
        if (hasEnvironment) {
            EnvironmentOutput
        } else {
            FixedColorOutput
        }
    val magnitudeInitialization =
        if (angularCapacity == 0) {
            "magnitude = directionMode < 0.5 ? max(0.0, projection) : abs(projection);"
        } else ""
    return environmentHighlightSource(
        geometry,
        directionUniforms,
        environmentDeclaration,
        exclusionCapacity,
        maximumWidth,
        nearestQuery,
        magnitudeInitialization,
        directionCurveValues,
        environmentContribution,
        output,
    )
}

internal fun highlightDirectionUsesSharedCurve(direction: HighlightDirection): Boolean {
    val incidence = direction as? HighlightDirection.Incidence ?: return false
    return incidence.strength.curve == incidence.widthScale.curve
}

private const val SdfSampleWithValidity =
    """
half4 fieldSample = sdfField.eval(position * sdfMap.z + sdfMap.xy);
float validity = float(fieldSample.a);
if (validity < 0.99) return float3(sdfMap.w, 0.0, -1.0);
float3 data = (float3(fieldSample.rgb) * (255.0 / validity) - 128.0) / 127.0;
"""

private fun sdfGeometrySource(dataSample: String): String =
    """
uniform float2 origin;
uniform float4 sdfMap;
uniform shader sdfField;

float3 sampleSdf(float2 position) {
$dataSample
  float2 normal = data.yz;
  float magnitude = length(normal);
  return float3(abs(data.x * sdfMap.w), magnitude > 0.01 ? normal / magnitude : float2(0.0, -1.0));
}
"""

private const val AngularDirectionValues =
    """
float2 angularValues = angularResponse(normal);
float widthScale = angularValues.x;
float strengthByDirection = angularValues.y;
"""

private const val SharedDirectionValues =
    """
float sharedDirectionLevel = backdropCurve(magnitude, directionStrengthCurve);
float strengthByDirection = directionEnabled > 0.5
  ? directionStrengthRange.x +
    (directionStrengthRange.y - directionStrengthRange.x) * sharedDirectionLevel
  : 1.0;
float widthScale = directionEnabled > 0.5
  ? directionWidthRange.x +
    (directionWidthRange.y - directionWidthRange.x) * sharedDirectionLevel
  : 1.0;
"""

private const val SeparateDirectionValues =
    """
float strengthByDirection = directionEnabled > 0.5
  ? response(magnitude, directionStrengthRange, directionStrengthCurve) : 1.0;
float widthScale = directionEnabled > 0.5
  ? response(magnitude, directionWidthRange, directionWidthCurve) : 1.0;
"""

private fun directionUniformsSource(directionWidthCurveUniform: String): String =
    """
uniform float directionEnabled;
uniform float directionMode;
uniform float2 directionStrengthRange;
uniform float4 directionStrengthCurve;
uniform float2 directionWidthRange;
$directionWidthCurveUniform
"""

private const val EnvironmentContribution =
    """
half4 environmentSample = environment.eval(coord);
float environmentAlpha = max(0.0, float(environmentSample.a));
if (environmentAlpha > 0.0) {
  float3 environmentColor =
    float3(environmentSample.rgb) / environmentAlpha;
  light += toLinearSrgb(environmentColor) * environmentGain;
}
"""

private const val EnvironmentOutput =
    """
float coverage = fade * exclusion;
return half4(fromLinearSrgb(light) * coverage, coverage);
"""

private const val FixedColorOutput =
    """
light *= fade * exclusion;
float peak = max(light.r, max(light.g, light.b));
if (!(peak > 0.0)) return half4(0.0);
float alpha = clamp(peak, 0.0, 1.0);
return half4(fromLinearSrgb(light / max(alpha, 0.000001)) * alpha, half(alpha));
"""

private fun environmentHighlightSource(
    geometry: String,
    directionUniforms: String,
    environmentDeclaration: String,
    exclusionCapacity: Int,
    maximumWidth: String,
    nearestQuery: String,
    magnitudeInitialization: String,
    directionCurveValues: String,
    environmentContribution: String,
    output: String,
): String =
    """
$geometry
$HighlightCurveShader
uniform float4 tileBounds;
uniform float width;
uniform float colorGain;
layout(color) uniform half4 fixedColor;
uniform float environmentGain;
uniform float2 directionVector;
$directionUniforms
$environmentDeclaration
${exclusionShader(exclusionCapacity)}

float response(float value, float2 range, float4 controls) {
  return range.x + (range.y - range.x) * backdropCurve(value, controls);
}

half4 shadeAt(float2 coord) {
  float2 p = coord - origin;
  if (width <= 0.0) return half4(0.0);
  float maximumBandWidth = width * $maximumWidth;
  float conservativeUpper =
    maximumBandWidth + max(0.0001, abs(maximumBandWidth) * 0.000001) + 0.5;
  float2 nearestPosition;
  float3 nearest = $nearestQuery;
  if (nearest.x >= conservativeUpper) return half4(0.0);
  float2 normal = nearest.yz;
  float projection = dot(normal, directionVector);
  float magnitude = 0.0;
$magnitudeInitialization
$directionCurveValues
  float bandWidth = width * widthScale;
  if (bandWidth <= 0.0 || nearest.x >= bandWidth + 0.5) return half4(0.0);
  float fade = 1.0 - smoothstep(0.0, max(bandWidth, 0.0001), nearest.x);
  float exclusion = exclusionWeight(normal);
  if (fade <= 0.0 || exclusion <= 0.0) return half4(0.0);

  float3 light = toLinearSrgb(fixedColor.rgb) * (fixedColor.a * colorGain * strengthByDirection);
$environmentContribution
$output
}

half4 main(float2 coord) {
  float2 p = coord - origin;
  if (
    p.x < tileBounds.x ||
    p.y < tileBounds.y ||
    p.x >= tileBounds.z ||
    p.y >= tileBounds.w
  ) return half4(0.0);
  return shadeAt(coord);
}
"""

private const val HighlightCurveShader =
    """
float backdropCurveAxis(float t, float first, float second) {
  float inverse = 1.0 - t;
  return 3.0 * inverse * inverse * t * first + 3.0 * inverse * t * t * second + t * t * t;
}

float backdropCurve(float x, float4 controls) {
  if (x <= 0.0) return 0.0;
  if (x >= 1.0) return 1.0;
  if (controls.x == 0.0 && controls.y == 0.0 && controls.z == 1.0 && controls.w == 1.0) return x;
  if (abs(controls.x - 0.333333333) < 0.0000001 && abs(controls.z - 0.666666667) < 0.0000001) {
    return backdropCurveAxis(x, controls.y, controls.w);
  }
  float low = 0.0;
  float high = 1.0;
  float t = x;
  for (int i = 0; i < 8; ++i) {
    float error = backdropCurveAxis(t, controls.x, controls.z) - x;
    float inverse = 1.0 - t;
    float derivative = 3.0 * inverse * inverse * controls.x
      + 6.0 * inverse * t * (controls.z - controls.x)
      + 3.0 * t * t * (1.0 - controls.z);
    if (derivative > 0.01 && abs(error) < 0.0000001) {
      return backdropCurveAxis(t, controls.y, controls.w);
    }
    if (error < 0.0) low = t; else high = t;
    float next = derivative > 0.01 ? t - error / derivative : (low + high) * 0.5;
    t = next > low && next < high ? next : (low + high) * 0.5;
  }

  for (int i = 0; i < 16; ++i) {
    t = (low + high) * 0.5;
    if (backdropCurveAxis(t, controls.x, controls.z) < x) low = t; else high = t;
  }
  return backdropCurveAxis(t, controls.y, controls.w);
}
"""
