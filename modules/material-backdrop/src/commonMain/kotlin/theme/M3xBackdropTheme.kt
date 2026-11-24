package top.ltfan.m3x.material.backdrop.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.ltfan.m3x.backdrop.BackdropBlur
import top.ltfan.m3x.backdrop.BackdropFill
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.BackdropLens
import top.ltfan.m3x.backdrop.BackdropRamp
import top.ltfan.m3x.backdrop.BackdropRampAnchors
import top.ltfan.m3x.backdrop.BackdropRenderScope
import top.ltfan.m3x.backdrop.BackdropStyle
import top.ltfan.m3x.backdrop.highlight.Highlight
import top.ltfan.m3x.backdrop.interaction.InteractionLighting
import top.ltfan.m3x.backdrop.shadow.InnerShadow
import top.ltfan.m3x.backdrop.shadow.Shadow
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.MaterialSurfaceStyle

/** Selects whether a material uses its supplied colors or adapts to the local SDR background. */
public enum class BackdropAdaptation {
    /** Uses the material's supplied colors without local appearance sampling. */
    Fixed,

    /** Samples the local background and adapts the material's appearance. */
    Background,
}

/**
 * A complete material configuration. [style] supplies the parameterized optical defaults: blur,
 * color controls, vibrancy, lens, fill, decorations, receiving light opacity, and page insets.
 * Explicit component effects replace the complete optical chain. A null style fill brush selects
 * Material tinting; an explicit transparent brush preserves the source without tint. [tint] uses
 * the component's Material color when unspecified. [dimmingAlpha] darkens rich media underneath
 * Clear glass. [adaptation] enables local background sampling when set to
 * [BackdropAdaptation.Background]. The default is [BackdropAdaptation.Fixed], which keeps the
 * supplied Material colors and skips appearance readback. [adaptiveContentColor] applies when
 * background adaptation is enabled. [ramp] shapes progressive blur when requested by a scroll edge
 * or explicit surface resolution. [render] replaces the complete render graph when supplied; the
 * surface then uses that declaration instead of the parameterized defaults and leaves appearance
 * sampling to the caller. Each adapted surface owns its local thumbnail and result; [BackdropHost]
 * batches readbacks for those surfaces.
 */
@Immutable
public data class M3xBackdropMaterial(
    public val style: BackdropStyle = RegularControlStyle,
    public val tint: Color = Color.Unspecified,
    public val tintAlpha: Float = 0.4f,
    /**
     * Alpha of the fallback tint, used wherever blur contributes nothing: without a captured
     * source, and on a scroll edge whose blur is turned off.
     */
    public val fallbackAlpha: Float = 0.6f,
    public val dimmingAlpha: Float = 0f,
    public val adaptation: BackdropAdaptation = BackdropAdaptation.Fixed,
    public val adaptiveContentColor: Boolean = false,
    /** Whether this role emits and receives shared interaction light. */
    public val interactiveLighting: Boolean = true,
    /**
     * Default ramp for automatic progressive blur and caller-requested progressive filling.
     * Explicit style blur and fill configurations retain their own ramps.
     */
    public val ramp: BackdropRamp = BackdropRamp(),
    /**
     * Optional complete render declaration. It replaces the built-in graph, including tint, fill,
     * decorations, and appearance sampling. Surface insets still use [style], and the call site's
     * background alpha applies to the completed background.
     */
    public val render: (BackdropRenderScope.() -> Unit)? = null,
) : MaterialSurfaceStyle

/** Theme controls for emitted interaction light color, radius, and availability. */
@Immutable
public data class M3xBackdropLighting(
    public val enabled: Boolean = true,
    public val radius: Dp = 160.dp,
    public val color: Color = Color.White,
)

/**
 * Glass Regular defaults organized by material role. Every role keeps its supplied theme colors;
 * background adaptation is an explicit material choice.
 */
@Immutable
public data class M3xBackdropTheme(
    public val control: M3xBackdropMaterial = M3xBackdropMaterial(),
    public val panel: M3xBackdropMaterial =
        M3xBackdropMaterial(
            style = RegularPanelStyle,
            tintAlpha = 0.6f,
        ),

    /**
     * A card's own surface: the panel's glass with the highlight, shadow and inner shadow it would
     * draw for itself turned off, since a card's Material elevation already provides them. Its
     * interactive lighting stays on, so a press or a hover lights a card like any other glass.
     */
    public val surface: M3xBackdropMaterial =
        M3xBackdropMaterial(
            style = SurfaceStyle,
            tintAlpha = 0.6f,
        ),
    /** Uses the component's inverse Material container and a stable supplied foreground. */
    public val inversePanel: M3xBackdropMaterial =
        panel.copy(
            tintAlpha = 0.8f,
            fallbackAlpha = 0.8f,
            adaptation = BackdropAdaptation.Fixed,
            adaptiveContentColor = false,
        ),
    /**
     * The scroll edge: a progressive blur and fill over the content that scrolls beneath a bar. The
     * fill carries legibility, so the radius stays modest and the tint can stay translucent.
     */
    public val scrollEdge: M3xBackdropMaterial =
        M3xBackdropMaterial(
            style = ScrollEdgeStyle,
            tintAlpha = 0.6f,
            fallbackAlpha = 0.8f,
            interactiveLighting = false,
            ramp = ScrollEdgeRamp,
        ),
    public val lighting: M3xBackdropLighting = M3xBackdropLighting(),
) {
    public fun material(role: MaterialSurfaceRole): M3xBackdropMaterial =
        when (role) {
            MaterialSurfaceRole.Control -> control
            MaterialSurfaceRole.Panel -> panel
            MaterialSurfaceRole.Surface -> surface
            MaterialSurfaceRole.InversePanel -> inversePanel
            MaterialSurfaceRole.ScrollEdge -> scrollEdge
        }
}

private val RegularControlStyle = BackdropStyle.Default {
    fill = { BackdropFill(Color.Unspecified) }
    blur = { BackdropBlur.Uniform(4.dp) }
    vibrancy = { 1.5f }
    lens = { BackdropLens(12.dp, 24.dp) }
    interactionLighting = { InteractionLighting.Config() }
    shadow = { Shadow(radius = 16.dp) }
}
private val RegularPanelStyle = BackdropStyle.Default {
    fill = { BackdropFill(Color.Unspecified) }
    blur = { BackdropBlur.Uniform(16.dp) }
    vibrancy = { 1.5f }
    lens = { BackdropLens(24.dp, 24.dp, depthEffect = true) }
    interactionLighting = { InteractionLighting.Config() }
}
private val SurfaceStyle = RegularPanelStyle {
    blur = { BackdropBlur.None }
    lens = { BackdropLens.None }
    highlight = { Highlight.None }
    shadow = { Shadow.None }
    innerShadow = { InnerShadow.None }
}
private val ScrollEdgeStyle = BackdropStyle.Default {
    fill = { BackdropFill(Color.Unspecified) }
    blur = { BackdropBlur.Uniform(8.dp) }
    interactionLighting = { InteractionLighting.Config() }
    highlight = { Highlight.None }
    shadow = { Shadow.None }
}

/**
 * The ramp a scroll edge starts from: full strength over the part of a bar that shows its own
 * content, spent below it.
 *
 * The `24.dp` plateau keeps the bar's own content at full strength. A small top app bar is `64.dp`
 * tall. Its controls occupy a centered `48.dp` row, and its title occupies a centered line about
 * `28.dp` high. All of this content stays within `18.dp` of the edge where scrolling content
 * passes. The `24.dp` of fade is the room the effects cover below the bar, about one control row,
 * which is where the content scrolling under it fades in.
 */
private val ScrollEdgeRamp: BackdropRamp =
    BackdropRamp(anchors = BackdropRampAnchors.offsets(plateau = 24.dp, fade = 24.dp))
