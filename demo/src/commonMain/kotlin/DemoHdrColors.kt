package top.ltfan.m3x.demo

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces

internal fun demoHdrWhite(strength: Float): Color =
    if (strength > 1f)
        Color(strength, strength, strength, colorSpace = ColorSpaces.LinearExtendedSrgb)
    else Color.White
