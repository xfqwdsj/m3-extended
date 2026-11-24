package top.ltfan.m3x.compose.material3

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import top.ltfan.m3x.compose.ui.color.Contrast
import top.ltfan.m3x.compose.ui.color.Hct
import top.ltfan.m3x.compose.ui.color.contrastRatioAgainst
import top.ltfan.m3x.compose.ui.color.contrastingForeground

/**
 * Derives semantic foregrounds and boundaries with increased contrast. Shared foreground roles
 * resolve against their whole background family; backgrounds move only when foreground adjustment
 * alone cannot meet the text target of 7:1.
 */
public fun ColorScheme.withIncreasedContrast(): ColorScheme {
    val colors = mutableMapOf<String, Color>()
    colors["primary"] = primary.copy(alpha = 1f)
    colors["onPrimary"] = onPrimary.copy(alpha = 1f)
    colors["primaryContainer"] = primaryContainer.copy(alpha = 1f)
    colors["onPrimaryContainer"] = onPrimaryContainer.copy(alpha = 1f)
    colors["inversePrimary"] = inversePrimary.copy(alpha = 1f)
    colors["secondary"] = secondary.copy(alpha = 1f)
    colors["onSecondary"] = onSecondary.copy(alpha = 1f)
    colors["secondaryContainer"] = secondaryContainer.copy(alpha = 1f)
    colors["onSecondaryContainer"] = onSecondaryContainer.copy(alpha = 1f)
    colors["tertiary"] = tertiary.copy(alpha = 1f)
    colors["onTertiary"] = onTertiary.copy(alpha = 1f)
    colors["tertiaryContainer"] = tertiaryContainer.copy(alpha = 1f)
    colors["onTertiaryContainer"] = onTertiaryContainer.copy(alpha = 1f)
    colors["background"] = background.copy(alpha = 1f)
    colors["onBackground"] = onBackground.copy(alpha = 1f)
    colors["surface"] = surface.copy(alpha = 1f)
    colors["onSurface"] = onSurface.copy(alpha = 1f)
    colors["surfaceVariant"] = surfaceVariant.copy(alpha = 1f)
    colors["onSurfaceVariant"] = onSurfaceVariant.copy(alpha = 1f)
    colors["surfaceTint"] = surfaceTint.copy(alpha = 1f)
    colors["inverseSurface"] = inverseSurface.copy(alpha = 1f)
    colors["inverseOnSurface"] = inverseOnSurface.copy(alpha = 1f)
    colors["error"] = error.copy(alpha = 1f)
    colors["onError"] = onError.copy(alpha = 1f)
    colors["errorContainer"] = errorContainer.copy(alpha = 1f)
    colors["onErrorContainer"] = onErrorContainer.copy(alpha = 1f)
    colors["outline"] = outline.copy(alpha = 1f)
    colors["outlineVariant"] = outlineVariant.copy(alpha = 1f)
    colors["scrim"] = scrim.copy(alpha = 1f)
    colors["surfaceBright"] = surfaceBright.copy(alpha = 1f)
    colors["surfaceDim"] = surfaceDim.copy(alpha = 1f)
    colors["surfaceContainer"] = surfaceContainer.copy(alpha = 1f)
    colors["surfaceContainerHigh"] = surfaceContainerHigh.copy(alpha = 1f)
    colors["surfaceContainerHighest"] = surfaceContainerHighest.copy(alpha = 1f)
    colors["surfaceContainerLow"] = surfaceContainerLow.copy(alpha = 1f)
    colors["surfaceContainerLowest"] = surfaceContainerLowest.copy(alpha = 1f)
    colors["primaryFixed"] = primaryFixed.copy(alpha = 1f)
    colors["primaryFixedDim"] = primaryFixedDim.copy(alpha = 1f)
    colors["onPrimaryFixed"] = onPrimaryFixed.copy(alpha = 1f)
    colors["onPrimaryFixedVariant"] = onPrimaryFixedVariant.copy(alpha = 1f)
    colors["secondaryFixed"] = secondaryFixed.copy(alpha = 1f)
    colors["secondaryFixedDim"] = secondaryFixedDim.copy(alpha = 1f)
    colors["onSecondaryFixed"] = onSecondaryFixed.copy(alpha = 1f)
    colors["onSecondaryFixedVariant"] = onSecondaryFixedVariant.copy(alpha = 1f)
    colors["tertiaryFixed"] = tertiaryFixed.copy(alpha = 1f)
    colors["tertiaryFixedDim"] = tertiaryFixedDim.copy(alpha = 1f)
    colors["onTertiaryFixed"] = onTertiaryFixed.copy(alpha = 1f)
    colors["onTertiaryFixedVariant"] = onTertiaryFixedVariant.copy(alpha = 1f)
    fun resolve(foreground: String, backgrounds: List<String>, ratio: Float = 7f) {
        val behind = backgrounds.map { colors.getValue(it) }
        val endpoint =
            listOf(Color.Black, Color.White).maxBy { color ->
                behind.minOf { color.contrastRatioAgainst(it) }
            }
        if (behind.any { endpoint.contrastRatioAgainst(it) < ratio }) {
            val opposite = if (endpoint == Color.Black) Color.White else Color.Black
            backgrounds.forEach { name ->
                val original = colors.getValue(name)
                if (endpoint.contrastRatioAgainst(original) < ratio) {
                    val hct = Hct.fromInt(original.toArgb())
                    val tone =
                        if (opposite == Color.White) Contrast.lighterUnsafe(0.0, ratio.toDouble())
                        else Contrast.darkerUnsafe(100.0, ratio.toDouble())
                    colors[name] = Color(Hct.from(hct.hue, hct.chroma, tone).toInt())
                }
            }
        }
        colors[foreground] =
            colors
                .getValue(foreground)
                .contrastingForeground(backgrounds.map { colors.getValue(it) }, ratio)
    }
    val surfaces =
        listOf(
            "background",
            "surface",
            "surfaceVariant",
            "surfaceBright",
            "surfaceDim",
            "surfaceContainer",
            "surfaceContainerHigh",
            "surfaceContainerHighest",
            "surfaceContainerLow",
            "surfaceContainerLowest",
        )
    resolve("onSurface", surfaces)
    resolve("onSurfaceVariant", surfaces)
    resolve("onBackground", listOf("background"))
    colors["primary"] =
        colors.getValue("primary").contrastingForeground(surfaces.map { colors.getValue(it) }, 7f)
    resolve("onPrimary", listOf("primary"))
    resolve("onPrimaryContainer", listOf("primaryContainer"))
    resolve("onPrimaryFixed", listOf("primaryFixed", "primaryFixedDim"))
    resolve("onPrimaryFixedVariant", listOf("primaryFixed", "primaryFixedDim"))
    colors["secondary"] =
        colors.getValue("secondary").contrastingForeground(surfaces.map { colors.getValue(it) }, 7f)
    resolve("onSecondary", listOf("secondary"))
    resolve("onSecondaryContainer", listOf("secondaryContainer"))
    resolve("onSecondaryFixed", listOf("secondaryFixed", "secondaryFixedDim"))
    resolve("onSecondaryFixedVariant", listOf("secondaryFixed", "secondaryFixedDim"))
    colors["tertiary"] =
        colors.getValue("tertiary").contrastingForeground(surfaces.map { colors.getValue(it) }, 7f)
    resolve("onTertiary", listOf("tertiary"))
    resolve("onTertiaryContainer", listOf("tertiaryContainer"))
    resolve("onTertiaryFixed", listOf("tertiaryFixed", "tertiaryFixedDim"))
    resolve("onTertiaryFixedVariant", listOf("tertiaryFixed", "tertiaryFixedDim"))
    colors["error"] =
        colors.getValue("error").contrastingForeground(surfaces.map { colors.getValue(it) }, 7f)
    resolve("onError", listOf("error"))
    resolve("onErrorContainer", listOf("errorContainer"))
    resolve("inverseOnSurface", listOf("inverseSurface"))
    resolve("inversePrimary", listOf("inverseSurface"))
    resolve("outline", surfaces, 3f)
    resolve("outlineVariant", surfaces, 3f)
    colors["surfaceTint"] = colors.getValue("primary")
    return copy(
        primary = colors.getValue("primary"),
        onPrimary = colors.getValue("onPrimary"),
        primaryContainer = colors.getValue("primaryContainer"),
        onPrimaryContainer = colors.getValue("onPrimaryContainer"),
        inversePrimary = colors.getValue("inversePrimary"),
        secondary = colors.getValue("secondary"),
        onSecondary = colors.getValue("onSecondary"),
        secondaryContainer = colors.getValue("secondaryContainer"),
        onSecondaryContainer = colors.getValue("onSecondaryContainer"),
        tertiary = colors.getValue("tertiary"),
        onTertiary = colors.getValue("onTertiary"),
        tertiaryContainer = colors.getValue("tertiaryContainer"),
        onTertiaryContainer = colors.getValue("onTertiaryContainer"),
        background = colors.getValue("background"),
        onBackground = colors.getValue("onBackground"),
        surface = colors.getValue("surface"),
        onSurface = colors.getValue("onSurface"),
        surfaceVariant = colors.getValue("surfaceVariant"),
        onSurfaceVariant = colors.getValue("onSurfaceVariant"),
        surfaceTint = colors.getValue("surfaceTint"),
        inverseSurface = colors.getValue("inverseSurface"),
        inverseOnSurface = colors.getValue("inverseOnSurface"),
        error = colors.getValue("error"),
        onError = colors.getValue("onError"),
        errorContainer = colors.getValue("errorContainer"),
        onErrorContainer = colors.getValue("onErrorContainer"),
        outline = colors.getValue("outline"),
        outlineVariant = colors.getValue("outlineVariant"),
        scrim = colors.getValue("scrim"),
        surfaceBright = colors.getValue("surfaceBright"),
        surfaceDim = colors.getValue("surfaceDim"),
        surfaceContainer = colors.getValue("surfaceContainer"),
        surfaceContainerHigh = colors.getValue("surfaceContainerHigh"),
        surfaceContainerHighest = colors.getValue("surfaceContainerHighest"),
        surfaceContainerLow = colors.getValue("surfaceContainerLow"),
        surfaceContainerLowest = colors.getValue("surfaceContainerLowest"),
        primaryFixed = colors.getValue("primaryFixed"),
        primaryFixedDim = colors.getValue("primaryFixedDim"),
        onPrimaryFixed = colors.getValue("onPrimaryFixed"),
        onPrimaryFixedVariant = colors.getValue("onPrimaryFixedVariant"),
        secondaryFixed = colors.getValue("secondaryFixed"),
        secondaryFixedDim = colors.getValue("secondaryFixedDim"),
        onSecondaryFixed = colors.getValue("onSecondaryFixed"),
        onSecondaryFixedVariant = colors.getValue("onSecondaryFixedVariant"),
        tertiaryFixed = colors.getValue("tertiaryFixed"),
        tertiaryFixedDim = colors.getValue("tertiaryFixedDim"),
        onTertiaryFixed = colors.getValue("onTertiaryFixed"),
        onTertiaryFixedVariant = colors.getValue("onTertiaryFixedVariant"),
    )
}
