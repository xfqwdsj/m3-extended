package top.ltfan.m3x.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/** Read-only identity and drawing order supplied to a host renderer adapter. */
public data class OverlayLayerInfo(
    public val key: Any,
    public val order: Int,
    public val modal: Boolean,
)

/** Captures and wraps one hosted layer without owning its input or transition. */
public interface OverlayLayerDecoration {
    public val modifier: Modifier

    @Composable public fun Content(content: @Composable () -> Unit)

    public companion object {
        public val None: OverlayLayerDecoration =
            object : OverlayLayerDecoration {
                override val modifier: Modifier = Modifier

                @Composable
                override fun Content(content: @Composable () -> Unit) {
                    content()
                }
            }
    }
}

/** Builds renderer connections for the complete drawing stack before entries are composed. */
public fun interface OverlayHostDecoration {
    @Composable public fun layers(entries: List<OverlayLayerInfo>): Map<Any, OverlayLayerDecoration>
}

/** Renderer integration selected by the application around its single overlay host. */
public val LocalOverlayHostDecoration: ProvidableCompositionLocal<OverlayHostDecoration> =
    staticCompositionLocalOf {
        OverlayHostDecoration { emptyMap() }
    }
