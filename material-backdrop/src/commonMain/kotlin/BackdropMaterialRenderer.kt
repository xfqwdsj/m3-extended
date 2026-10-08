package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import top.ltfan.backdrop.BackdropEffectScope
import top.ltfan.backdrop.highlight.HighlightExclusion
import top.ltfan.m3x.backdrop.BackdropLayerHandle
import top.ltfan.m3x.backdrop.LocalBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.material.MaterialSurfaceAppearance
import top.ltfan.m3x.material.MaterialSurfaceEdge
import top.ltfan.m3x.material.MaterialSurfaceEffects
import top.ltfan.m3x.material.MaterialSurfaceLayer
import top.ltfan.m3x.material.MaterialSurfaceRenderer
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.MaterialSurfaceStyle
import top.ltfan.m3x.material.backdrop.theme.LocalM3xBackdropTheme
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial
import top.ltfan.m3x.material.backdrop.theme.backdrop
import top.ltfan.m3x.material.theme.extended

/** Backdrop effects supplied to renderer-independent components. */
public class BackdropSurfaceEffects(public val block: BackdropEffectScope.() -> Unit) :
    MaterialSurfaceEffects

/** Wraps an effect chain without exposing Backdrop in ordinary component signatures. */
public fun backdropEffects(block: BackdropEffectScope.() -> Unit): MaterialSurfaceEffects =
    BackdropSurfaceEffects(block)

private class BackdropMaterialLayer(val handle: BackdropLayerHandle) : MaterialSurfaceLayer {
    @Composable
    override fun Content(enabled: Boolean, content: @Composable () -> Unit) {
        if (enabled && LocalBackdrop.current != null) {
            CompositionLocalProvider(LocalBackdrop provides handle, content = content)
        } else content()
    }
}

/** Uses M3X Backdrop roles for ordinary components without owning their layout or state. */
public object BackdropMaterialRenderer : MaterialSurfaceRenderer {
    @Composable
    override fun style(role: MaterialSurfaceRole): MaterialSurfaceStyle =
        MaterialTheme.extended.backdrop.material(role)

    @Composable
    override fun captureEnvironment(): @Composable (@Composable () -> Unit) -> Unit {
        val theme = LocalM3xBackdropTheme.current
        return { content ->
            CompositionLocalProvider(LocalM3xBackdropTheme provides theme, content = content)
        }
    }

    @Composable
    override fun rememberLayer(): MaterialSurfaceLayer {
        val handle = rememberBackdropLayer()
        return remember(handle) { BackdropMaterialLayer(handle) }
    }

    @Composable
    override fun appearance(
        role: MaterialSurfaceRole,
        style: MaterialSurfaceStyle?,
        containerColor: Color,
        contentColor: Color,
        effects: MaterialSurfaceEffects?,
        shape: Shape,
        excludedEdges: Set<MaterialSurfaceEdge>,
    ): MaterialSurfaceAppearance {
        require(style == null || style is M3xBackdropMaterial) {
            "Backdrop rendering requires M3xBackdropMaterial"
        }
        require(effects == null || effects is BackdropSurfaceEffects) {
            "Backdrop rendering requires BackdropSurfaceEffects"
        }
        val appearance =
            rememberMaterialAppearance(
                role,
                style,
                containerColor,
                contentColor,
                effects?.block,
                shape = shape,
                highlightExclusions =
                    if (MaterialSurfaceEdge.Bottom in excludedEdges)
                        setOf(HighlightExclusion.Bottom)
                    else emptySet(),
            )
        return object : MaterialSurfaceAppearance {
            override val contentColor: Color = appearance.contentColor

            @Composable
            override fun Modifier.modifier(
                shape: Shape,
                exportedLayer: MaterialSurfaceLayer?,
                interactionSource: InteractionSource?,
            ): Modifier =
                materialBackdropSurface(
                    appearance,
                    shape,
                    exportedBackdrop = (exportedLayer as BackdropMaterialLayer?)?.handle?.layer,
                    interactionSource = interactionSource,
                )
        }
    }
}
