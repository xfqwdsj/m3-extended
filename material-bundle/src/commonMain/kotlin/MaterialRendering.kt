package top.ltfan.m3x.material.bundle

import androidx.compose.runtime.Composable
import top.ltfan.m3x.material.backdrop.ProvideBackdropRendering
import top.ltfan.m3x.overlay.backdrop.ProvideBackdropOverlay

/** Selects M3X rendering integrations while retaining the caller's theme and hosts. */
@Composable
public fun ProvideM3xMaterialRendering(content: @Composable () -> Unit) {
    ProvideBackdropRendering { ProvideBackdropOverlay(content) }
}
