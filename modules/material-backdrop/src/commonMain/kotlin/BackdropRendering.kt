package top.ltfan.m3x.material.backdrop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import top.ltfan.m3x.material.LocalFloatingBottomBarRenderer
import top.ltfan.m3x.material.LocalMaterialSurfaceRenderer

/**
 * Selects Backdrop surface and floating-bar rendering while retaining the caller's theme and hosts.
 */
@Composable
public fun ProvideBackdropRendering(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalMaterialSurfaceRenderer provides BackdropMaterialRenderer,
        LocalFloatingBottomBarRenderer provides BackdropFloatingBottomBarRenderer,
        content = content,
    )
}
