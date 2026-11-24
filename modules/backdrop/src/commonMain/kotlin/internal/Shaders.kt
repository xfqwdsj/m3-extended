/*
  Copyright 2025 Kyant

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package top.ltfan.m3x.backdrop.internal

import org.intellij.lang.annotations.Language
import top.ltfan.m3x.backdrop.BuiltinRuntimeShader

@Language("AGSL")
private const val RoundedRectSDF =
    """
float radiusAt(float2 coord, float4 radii) {
  if (coord.x >= 0.0) {
    if (coord.y <= 0.0) return radii.y;
    else return radii.z;
  } else {
    if (coord.y <= 0.0) return radii.x;
    else return radii.w;
  }
}

float sdRoundedRect(float2 coord, float2 halfSize, float radius) {
  float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
  float outside = length(max(cornerCoord, 0.0)) - radius;
  float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);
  return outside + inside;
}

float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
  float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
  if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {
    float2 outside = max(cornerCoord, 0.0);
    float outsideLength = length(outside);
    return outsideLength > 0.0 ? sign(coord) * (outside / outsideLength) : float2(0.0);
  } else {
    float gradX = step(cornerCoord.y, cornerCoord.x);
    return sign(coord) * float2(gradX, 1.0 - gradX);
  }
}
"""

internal fun lensRefractionShader(
    boundary: String,
    dispersion: Boolean,
    skipInterior: Boolean = false,
): String {
    val dispersiveSample =
        if (dispersion) DispersiveSample else "return content.eval(coord + displacement);"
    val interiorCheck = if (skipInterior) InteriorCheck else ""
    return lensRefractionSource(boundary, dispersion, skipInterior, dispersiveSample, interiorCheck)
}

private val RoundedRectRefractionBoundary: String by lazy {
    """
uniform float2 origin;
uniform float4 cornerRadii;
$RoundedRectSDF

float3 refractionBoundary(float2 point, float2 centeredCoord) {
  float2 halfSize = size * 0.5;
  float radius = radiusAt(centeredCoord, cornerRadii);
  float distance = max(-sdRoundedRect(centeredCoord, halfSize, radius), 0.0);
  float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
  return float3(distance, gradSdRoundedRect(centeredCoord, halfSize, gradRadius));
}
"""
}

internal val RoundedRectRefractionShader by lazy {
    BuiltinRuntimeShader(lensRefractionShader(RoundedRectRefractionBoundary, dispersion = false))
}

internal val RoundedRectRefractionWithDispersionShader by lazy {
    BuiltinRuntimeShader(lensRefractionShader(RoundedRectRefractionBoundary, dispersion = true))
}

private const val LensDispersion =
    """
half4 sampleDispersion(float2 refractedCoord, float2 dispersedCoord) {
  half4 color = half4(0.0);

  half4 red = content.eval(refractedCoord + dispersedCoord);
  color.r += red.r / 3.5;
  color.a += red.a / 7.0;

  half4 orange = content.eval(refractedCoord + dispersedCoord * (2.0 / 3.0));
  color.r += orange.r / 3.5;
  color.g += orange.g / 7.0;
  color.a += orange.a / 7.0;

  half4 yellow = content.eval(refractedCoord + dispersedCoord * (1.0 / 3.0));
  color.r += yellow.r / 3.5;
  color.g += yellow.g / 3.5;
  color.a += yellow.a / 7.0;

  half4 green = content.eval(refractedCoord);
  color.g += green.g / 3.5;
  color.a += green.a / 7.0;

  half4 cyan = content.eval(refractedCoord - dispersedCoord * (1.0 / 3.0));
  color.g += cyan.g / 3.5;
  color.b += cyan.b / 3.0;
  color.a += cyan.a / 7.0;

  half4 blue = content.eval(refractedCoord - dispersedCoord * (2.0 / 3.0));
  color.b += blue.b / 3.0;
  color.a += blue.a / 7.0;

  half4 purple = content.eval(refractedCoord - dispersedCoord);
  color.r += purple.r / 7.0;
  color.b += purple.b / 3.0;
  color.a += purple.a / 7.0;

  return color;
}
"""

private const val DispersiveSample =
    """
float2 halfSize = size * 0.5;
float2 normalizedPosition = centeredCoord / halfSize;
float2 dispersedCoord = displacement * (normalizedPosition.x * normalizedPosition.y);
return sampleDispersion(coord + displacement, dispersedCoord);
"""

private const val InteriorCheck =
    """
if (point.x > safeInterior.x && point.y > safeInterior.y &&
  point.x < safeInterior.z && point.y < safeInterior.w) return content.eval(coord);
"""

private fun lensRefractionSource(
    boundary: String,
    dispersion: Boolean,
    skipInterior: Boolean,
    dispersiveSample: String,
    interiorCheck: String,
): String =
    """
uniform shader content;
uniform float2 size;
uniform float refractionHeight;
uniform float refractionAmount;
uniform float depthEffect;
${if (skipInterior) "uniform float4 safeInterior;" else ""}

$boundary

${if (dispersion) LensDispersion else ""}

half4 main(float2 coord) {
  float2 point = coord - origin;
$interiorCheck
  float2 centeredCoord = point - size * 0.5;
  float3 boundary = refractionBoundary(point, centeredCoord);
  if (boundary.x >= refractionHeight) return content.eval(coord);

  float depth = 1.0 - boundary.x / refractionHeight;
  float strength = 1.0 - sqrt(1.0 - depth * depth);
  float centerLength = length(centeredCoord);
  float2 depthDirection = centerLength > 0.0 ? centeredCoord / centerLength : float2(0.0);
  float2 gradVector = boundary.yz + depthEffect * depthDirection;
  float gradLength = length(gradVector);
  if (!(gradLength > 0.0)) return content.eval(coord);
  float2 displacement = strength * refractionAmount * (gradVector / gradLength);
$dispersiveSample
}
"""
