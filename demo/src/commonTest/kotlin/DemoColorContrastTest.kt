package top.ltfan.m3x.demo

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertTrue

class DemoColorContrastTest {
    @Test
    fun colorfulSchemesKeepTextRoleContrastAndArtworkMatchesBothPalettes() {
        for (dark in listOf(false, true)) {
            val colorful = colorfulColorScheme(dark)
            val theme = if (dark) darkColorScheme() else lightColorScheme()
            val pairs =
                listOf(
                    colorful.onPrimary to colorful.primary,
                    colorful.onPrimaryContainer to colorful.primaryContainer,
                    colorful.onSecondary to colorful.secondary,
                    colorful.onSecondaryContainer to colorful.secondaryContainer,
                    colorful.onTertiary to colorful.tertiary,
                    colorful.onTertiaryContainer to colorful.tertiaryContainer,
                    colorful.onSurface to colorful.surface,
                    colorful.onSurfaceVariant to colorful.surfaceVariant,
                    colorful.onBackground to colorful.background,
                )
            for ((foreground, background) in pairs) {
                assertTrue(
                    contrast(foreground, background) >= 4.5f,
                    "Role contrast: $foreground / $background",
                )
            }
            val stops = colorfulBackground(dark)
            for (scheme in listOf(theme, colorful)) {
                for (segment in 0 until stops.lastIndex) {
                    for (step in 0..100) {
                        val t = step / 100f
                        val a = stops[segment]
                        val b = stops[segment + 1]
                        val background =
                            Color(
                                a.red + (b.red - a.red) * t,
                                a.green + (b.green - a.green) * t,
                                a.blue + (b.blue - a.blue) * t,
                            )
                        for (foreground in
                            listOf(scheme.onSurface, scheme.onSurfaceVariant, scheme.primary)) {
                            assertTrue(
                                contrast(foreground, background) >= 4.5f,
                                "Artwork contrast: $foreground / $background",
                            )
                        }
                    }
                }
            }
        }
    }

    private fun contrast(a: Color, b: Color): Float {
        val x = a.luminance()
        val y = b.luminance()
        return (maxOf(x, y) + 0.05f) / (minOf(x, y) + 0.05f)
    }
}
