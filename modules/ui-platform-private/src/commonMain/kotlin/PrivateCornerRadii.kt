package top.ltfan.m3x.compose.ui.privateapi

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Installs private platform corner readers for the current composition when supported. */
@Composable public expect fun ProvidePrivateCornerRadii(content: @Composable () -> Unit)

/** Converts a native point radius to M3X dp, rejecting unsupported scale values. */
internal fun radiusToDp(points: Double, backingScale: Double, density: Density): Dp? {
    if (!points.isFinite() || points < 0.0) return null
    if (!backingScale.isFinite() || backingScale <= 0.0) return null
    if (!density.density.isFinite() || density.density <= 0f) return null
    return (points * backingScale / density.density)
        .takeIf { it.isFinite() && it >= 0.0 }
        ?.toFloat()
        ?.takeIf { it.isFinite() }
        ?.dp
}
