package top.ltfan.m3x.compose.ui.color

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs

/**
 * Relative luminance contrast after compositing this foreground over [background] forced opaque.
 */
public fun Color.contrastRatioAgainst(background: Color): Float {
    val opaque = background.copy(alpha = 1f)
    val foreground = compositeOver(opaque).luminance()
    val behind = opaque.luminance()
    return Contrast.ratioOfYs(foreground * 100.0, behind * 100.0).toFloat()
}

/**
 * Returns an opaque foreground adjusted toward [minimumRatio] using Material HCT tone. [background]
 * is treated as opaque. Hue and chroma are retained as the gamut permits; when the target is
 * unreachable, returns whichever of black or white has greater contrast.
 */
public fun Color.withContrastAgainst(background: Color, minimumRatio: Float = 7f): Color {
    require(minimumRatio.isFinite() && minimumRatio in 1f..21f)
    return contrastingForeground(listOf(background), minimumRatio)
}

/**
 * Returns an opaque foreground adjusted to contrast against every supplied opaque background. The
 * background list must be nonempty and [ratio] must be finite and between 1 and 21. The closest
 * qualifying HCT tone is preferred; an unreachable target uses the better endpoint.
 */
public fun Color.contrastingForeground(backgrounds: List<Color>, ratio: Float): Color {
    val original = copy(alpha = 1f)
    fun score(color: Color): Float = backgrounds.minOf { color.contrastRatioAgainst(it) }
    if (score(original) >= ratio) return original
    val endpoint = listOf(Color.Black, Color.White).maxBy { score(it) }
    if (score(endpoint) < ratio) return endpoint
    val hct = Hct.fromInt(original.toArgb())
    val tones = backgrounds.map { Hct.fromInt(it.toArgb()).tone }
    val dark = tones.map { Contrast.darker(it, ratio.toDouble()) }
    val light = tones.map { Contrast.lighter(it, ratio.toDouble()) }
    val candidates = buildList {
        if (dark.all { it != null }) add(dark.filterNotNull().min())
        if (light.all { it != null }) add(light.filterNotNull().max())
    }
        .map { tone -> Color(Hct.from(hct.hue, hct.chroma, tone).toInt()) }
    return candidates
        .filter { score(it) >= ratio }
        .minByOrNull { abs(Hct.fromInt(it.toArgb()).tone - hct.tone) } ?: endpoint
}
