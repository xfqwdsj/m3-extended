package top.ltfan.m3x.material

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/** Renderer-specific configuration selected for a component surface. */
public interface MaterialSurfaceStyle

/** Renderer-specific replacement effects for a component surface. */
public interface MaterialSurfaceEffects

/** Physical edges a renderer excludes from its surface treatment. */
public enum class MaterialSurfaceEdge {
    Bottom,
}

/** Purpose of a surface within the design system. */
public enum class MaterialSurfaceRole {
    Control,
    Panel,
    InversePanel,
    Surface,
    ScrollEdge,
}

/** A renderer-owned layer whose lifetime is remembered by the component. */
public interface MaterialSurfaceLayer {
    @Composable public fun Content(enabled: Boolean, content: @Composable () -> Unit)
}

/** Resolved foreground and drawing behavior, without exposing renderer state. */
public interface MaterialSurfaceAppearance {
    public val contentColor: Color

    @Composable
    public fun Modifier.modifier(
        shape: Shape,
        exportedLayer: MaterialSurfaceLayer? = null,
        interactionSource: InteractionSource? = null,
    ): Modifier
}

/** Selects the rendering mechanism while components retain layout and interaction ownership. */
public interface MaterialSurfaceRenderer {
    /** Captures the selected role configuration for deferred component composition. */
    @Composable public fun style(role: MaterialSurfaceRole): MaterialSurfaceStyle? = null

    /** Captures renderer values at an overlay caller while retaining host rendering ownership. */
    @Composable
    public fun captureEnvironment(): @Composable (@Composable () -> Unit) -> Unit = { it() }

    @Composable public fun rememberLayer(): MaterialSurfaceLayer

    @Composable
    public fun appearance(
        role: MaterialSurfaceRole,
        style: MaterialSurfaceStyle?,
        containerColor: Color,
        contentColor: Color,
        effects: MaterialSurfaceEffects?,
        shape: Shape,
        excludedEdges: Set<MaterialSurfaceEdge>,
    ): MaterialSurfaceAppearance
}

/** Draws ordinary container colors while leaving renderer-specific styles and effects unused. */
public object PlainMaterialSurfaceRenderer : MaterialSurfaceRenderer {
    private val layer =
        object : MaterialSurfaceLayer {
            @Composable
            override fun Content(enabled: Boolean, content: @Composable () -> Unit) {
                content()
            }
        }

    @Composable override fun rememberLayer(): MaterialSurfaceLayer = layer

    @Composable
    override fun appearance(
        role: MaterialSurfaceRole,
        style: MaterialSurfaceStyle?,
        containerColor: Color,
        contentColor: Color,
        effects: MaterialSurfaceEffects?,
        shape: Shape,
        excludedEdges: Set<MaterialSurfaceEdge>,
    ): MaterialSurfaceAppearance =
        object : MaterialSurfaceAppearance {
            override val contentColor: Color = contentColor

            @Composable
            override fun Modifier.modifier(
                shape: Shape,
                exportedLayer: MaterialSurfaceLayer?,
                interactionSource: InteractionSource?,
            ): Modifier = background(containerColor, shape)
        }
}

/** Surface integration selected by the application or its full theme composition. */
public val LocalMaterialSurfaceRenderer: ProvidableCompositionLocal<MaterialSurfaceRenderer> =
    staticCompositionLocalOf {
        PlainMaterialSurfaceRenderer
    }

/** Remembers an optional renderer layer without coupling layout to its representation. */
@Composable
public fun rememberMaterialSurfaceLayer(): MaterialSurfaceLayer =
    LocalMaterialSurfaceRenderer.current.rememberLayer()

/** Resolves the surface while leaving visual policy to the selected renderer. */
@Composable
public fun rememberSurfaceAppearance(
    role: MaterialSurfaceRole,
    style: MaterialSurfaceStyle? = null,
    containerColor: Color,
    contentColor: Color,
    effects: MaterialSurfaceEffects? = null,
    shape: Shape,
    excludedEdges: Set<MaterialSurfaceEdge> = emptySet(),
): MaterialSurfaceAppearance =
    LocalMaterialSurfaceRenderer.current.appearance(
        role,
        style,
        containerColor,
        contentColor,
        effects,
        shape,
        excludedEdges,
    )

/** Appends a resolved surface after the caller's layout and drawing modifiers. */
@Composable
public fun Modifier.materialSurface(
    appearance: MaterialSurfaceAppearance,
    shape: Shape,
    exportedLayer: MaterialSurfaceLayer? = null,
    interactionSource: InteractionSource? = null,
): Modifier = with(appearance) { modifier(shape, exportedLayer, interactionSource) }
