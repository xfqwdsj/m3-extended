package top.ltfan.m3x.compose.ui.color

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** Converts this color to Material HCT using its sRGB representation. */
public fun Color.toHct(): Hct = Hct.fromInt(toArgb())

/** Converts this Material HCT color to its sRGB representation. */
public fun Hct.toColor(): Color = Color(toInt())
