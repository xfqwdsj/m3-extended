package top.ltfan.m3x.demo

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Brightness follows the UI theme so ordinary page text stays readable over the artwork. */
internal fun colorfulBackground(dark: Boolean): List<Color> =
    if (dark) listOf(Color(0xFF182C40), Color(0xFF17352C), Color(0xFF3D2536))
    else listOf(Color(0xFFD8ECFF), Color(0xFFD7F3DF), Color(0xFFFFE4EB))

/** Accent families have matching foreground roles in each theme. */
internal fun colorfulColorScheme(dark: Boolean): ColorScheme =
    if (dark)
        darkColorScheme(
            primary = Color(0xFF76DBC9),
            onPrimary = Color(0xFF003730),
            primaryContainer = Color(0xFF005047),
            onPrimaryContainer = Color(0xFF94F8E5),
            secondary = Color(0xFFCCBDFF),
            onSecondary = Color(0xFF32246A),
            secondaryContainer = Color(0xFF493B82),
            onSecondaryContainer = Color(0xFFE7DEFF),
            tertiary = Color(0xFFFFB95C),
            onTertiary = Color(0xFF4A2800),
            tertiaryContainer = Color(0xFF6A3C00),
            onTertiaryContainer = Color(0xFFFFDDB5),
            background = Color(0xFF101713),
            onBackground = Color(0xFFDEE6DF),
            surface = Color(0xFF101713),
            onSurface = Color(0xFFDEE6DF),
            surfaceVariant = Color(0xFF3F4944),
            onSurfaceVariant = Color(0xFFBEC9C1),
            surfaceContainerLowest = Color(0xFF0B120E),
            surfaceContainerLow = Color(0xFF18201B),
            surfaceContainer = Color(0xFF1C241F),
            surfaceContainerHigh = Color(0xFF263029),
            surfaceContainerHighest = Color(0xFF313B34),
            surfaceDim = Color(0xFF101713),
            surfaceBright = Color(0xFF354038),
        )
    else
        lightColorScheme(
            primary = Color(0xFF006A60),
            onPrimary = Color.White,
            primaryContainer = Color(0xFF9CF2DF),
            onPrimaryContainer = Color(0xFF00201C),
            secondary = Color(0xFF6151A2),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE7DEFF),
            onSecondaryContainer = Color(0xFF1D104B),
            tertiary = Color(0xFF8B5000),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFFFDDB5),
            onTertiaryContainer = Color(0xFF2C1600),
            background = Color(0xFFF7FBF9),
            onBackground = Color(0xFF16201D),
            surface = Color(0xFFF7FBF9),
            onSurface = Color(0xFF16201D),
            surfaceVariant = Color(0xFFDBE5E0),
            onSurfaceVariant = Color(0xFF3F4944),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFF0F6F2),
            surfaceContainer = Color(0xFFEAF1EC),
            surfaceContainerHigh = Color(0xFFE3EBE5),
            surfaceContainerHighest = Color(0xFFDDE5DF),
            surfaceDim = Color(0xFFD4DED6),
            surfaceBright = Color(0xFFF7FBF9),
        )
