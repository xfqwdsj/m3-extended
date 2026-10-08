package top.ltfan.m3x.material

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/** Selects ordinary surface and floating-bar rendering without changing theme or host ownership. */
@Composable
public fun ProvidePlainMaterialRendering(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalMaterialSurfaceRenderer provides PlainMaterialSurfaceRenderer,
        LocalFloatingBottomBarRenderer provides PlainFloatingBottomBarRenderer,
        content = content,
    )
}
