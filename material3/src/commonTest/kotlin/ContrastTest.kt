package top.ltfan.m3x.compose.material3

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.ui.color.Hct
import top.ltfan.m3x.compose.ui.color.contrastRatioAgainst
import top.ltfan.m3x.compose.ui.color.withContrastAgainst

class ContrastTest {
    @Test
    fun semanticColorsMeetTargetsAcrossLightDarkAndConflictingSurfaceFamilies() {
        for (base in
            listOf(
                lightColorScheme(),
                darkColorScheme(),
                lightColorScheme(
                    surface = Color.Black,
                    surfaceContainer = Color.White,
                    primary = Color.Gray,
                ),
            )) {
            val scheme = base.withIncreasedContrast()
            val surfaces =
                listOf(
                    scheme.background,
                    scheme.surface,
                    scheme.surfaceVariant,
                    scheme.surfaceBright,
                    scheme.surfaceDim,
                    scheme.surfaceContainer,
                    scheme.surfaceContainerHigh,
                    scheme.surfaceContainerHighest,
                    scheme.surfaceContainerLow,
                    scheme.surfaceContainerLowest,
                )
            surfaces.forEach { background ->
                for (foreground in
                    listOf(
                        scheme.onSurface,
                        scheme.onSurfaceVariant,
                        scheme.primary,
                        scheme.secondary,
                        scheme.tertiary,
                        scheme.error,
                    )) {
                    assertTrue(
                        foreground.contrastRatioAgainst(background) >= 6.999f,
                        "$foreground against $background",
                    )
                }
                assertTrue(scheme.outline.contrastRatioAgainst(background) >= 2.999f)
                assertTrue(scheme.outlineVariant.contrastRatioAgainst(background) >= 2.999f)
            }
            val pairs =
                listOf(
                    scheme.onPrimary to scheme.primary,
                    scheme.onSecondary to scheme.secondary,
                    scheme.onTertiary to scheme.tertiary,
                    scheme.onError to scheme.error,
                    scheme.onPrimaryContainer to scheme.primaryContainer,
                    scheme.onSecondaryContainer to scheme.secondaryContainer,
                    scheme.onTertiaryContainer to scheme.tertiaryContainer,
                    scheme.onErrorContainer to scheme.errorContainer,
                    scheme.inverseOnSurface to scheme.inverseSurface,
                    scheme.inversePrimary to scheme.inverseSurface,
                    scheme.onBackground to scheme.background,
                )
            pairs.forEach { (foreground, background) ->
                assertTrue(foreground.contrastRatioAgainst(background) >= 6.999f)
            }
            for ((foregrounds, backgrounds) in
                listOf(
                    listOf(scheme.onPrimaryFixed, scheme.onPrimaryFixedVariant) to
                        listOf(scheme.primaryFixed, scheme.primaryFixedDim),
                    listOf(scheme.onSecondaryFixed, scheme.onSecondaryFixedVariant) to
                        listOf(scheme.secondaryFixed, scheme.secondaryFixedDim),
                    listOf(scheme.onTertiaryFixed, scheme.onTertiaryFixedVariant) to
                        listOf(scheme.tertiaryFixed, scheme.tertiaryFixedDim),
                )) {
                foregrounds.forEach { foreground ->
                    backgrounds.forEach {
                        assertTrue(foreground.contrastRatioAgainst(it) >= 6.999f)
                    }
                }
            }
            assertEquals(scheme.toString(), scheme.withIncreasedContrast().toString())
        }
    }

    @Test
    fun materialHctMatchesUpstreamVectorsAndPreservesHueWhenAdjustingTone() {
        val blue = Hct.fromInt(0xff0000ff.toInt())
        assertEquals(282.788, blue.hue, 0.01)
        assertEquals(87.230, blue.chroma, 0.01)
        assertEquals(32.302, blue.tone, 0.01)
        val light = Hct.from(282.788, 87.230, 90.0)
        assertEquals(282.239, light.hue, 0.01)
        assertEquals(19.144, light.chroma, 0.01)
        assertEquals(90.035, light.tone, 0.01)
        val original = Color(0xff6750a4)
        val adjusted = original.withContrastAgainst(Color.White)
        assertTrue(adjusted.contrastRatioAgainst(Color.White) >= 7f)
        assertTrue(
            abs(Hct.fromInt(adjusted.toArgb()).hue - Hct.fromInt(original.toArgb()).hue) < 2.0,
        )
    }

    @Test
    fun transparentForegroundIsMeasuredAfterComposition() {
        assertEquals(1f, Color.Black.copy(alpha = 0f).contrastRatioAgainst(Color.White))
        assertEquals(21f, Color.Black.contrastRatioAgainst(Color.White), 0.00001f)
    }
}
