package top.ltfan.m3x.overlay.backdrop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import top.ltfan.backdrop.interaction.ProvideInteractionLighting
import top.ltfan.m3x.backdrop.BackdropLayerHandle
import top.ltfan.m3x.backdrop.LocalBackdrop
import top.ltfan.m3x.backdrop.captureStackBackdrop
import top.ltfan.m3x.backdrop.rememberStackedBackdropLayer
import top.ltfan.m3x.overlay.LocalOverlayHostDecoration
import top.ltfan.m3x.overlay.OverlayHostDecoration
import top.ltfan.m3x.overlay.OverlayLayerDecoration
import top.ltfan.m3x.overlay.OverlayLayerInfo

/** Connects ordered overlay layers to the supplied page source and isolated modal lighting. */
public object BackdropOverlayDecoration : OverlayHostDecoration {
    @Composable
    override fun layers(entries: List<OverlayLayerInfo>): Map<Any, OverlayLayerDecoration> {
        val pageBackdrop = LocalBackdrop.current
        val handles = mutableMapOf<Any, BackdropLayerHandle>()
        val result = mutableMapOf<Any, OverlayLayerDecoration>()
        entries
            .sortedBy { it.order }
            .forEach { entry ->
                key(entry.key) {
                    val below = buildList {
                        pageBackdrop?.let { add(it) }
                        entries
                            .filter { it.order < entry.order }
                            .sortedBy { it.order }
                            .forEach { lower ->
                                handles[lower.key]?.let { add(it) }
                            }
                    }
                    val handle = rememberStackedBackdropLayer(below)
                    handles[entry.key] = handle
                    result[entry.key] =
                        object : OverlayLayerDecoration {
                            override val modifier: Modifier = Modifier.captureStackBackdrop(handle)

                            @Composable
                            override fun Content(content: @Composable () -> Unit) {
                                CompositionLocalProvider(
                                    LocalBackdrop provides
                                        if (pageBackdrop != null) handle else null,
                                ) {
                                    ProvideInteractionLighting(
                                        isolate = entry.modal,
                                        content = content,
                                    )
                                }
                            }
                        }
                }
            }
        return result
    }
}

/** Selects Backdrop integration for overlay hosts in this subtree without creating a host. */
@Composable
public fun ProvideBackdropOverlay(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalOverlayHostDecoration provides BackdropOverlayDecoration,
        content = content,
    )
}
