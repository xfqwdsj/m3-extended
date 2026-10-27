package top.ltfan.m3x.material.backdrop

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlin.math.abs
import kotlin.math.max
import kotlin.time.Duration
import top.ltfan.backdrop.BackdropBlur
import top.ltfan.backdrop.BackdropEdge
import top.ltfan.backdrop.BackdropEffectScope
import top.ltfan.backdrop.BackdropFill
import top.ltfan.backdrop.BackdropFillRampCache
import top.ltfan.backdrop.BackdropImageScope
import top.ltfan.backdrop.BackdropInsets
import top.ltfan.backdrop.BackdropRenderScope
import top.ltfan.backdrop.BackdropRoom
import top.ltfan.backdrop.BackdropStyle
import top.ltfan.backdrop.LocalBackdropStyle
import top.ltfan.backdrop.backdropFillRoom
import top.ltfan.backdrop.backdrops.LayerBackdrop
import top.ltfan.backdrop.blur
import top.ltfan.backdrop.clipToGrownShape
import top.ltfan.backdrop.drawBackdrop
import top.ltfan.backdrop.drawBackdropFill
import top.ltfan.backdrop.effects.colorControls
import top.ltfan.backdrop.effects.coverRamp
import top.ltfan.backdrop.effects.lens
import top.ltfan.backdrop.effects.vibrancy
import top.ltfan.backdrop.highlight.Highlight
import top.ltfan.backdrop.highlight.HighlightAngleDomain
import top.ltfan.backdrop.highlight.HighlightDirection
import top.ltfan.backdrop.highlight.highlight
import top.ltfan.backdrop.interaction.InteractionLight
import top.ltfan.backdrop.interaction.InteractionLighting
import top.ltfan.backdrop.interaction.interactionLight
import top.ltfan.backdrop.interaction.interactionLightSource
import top.ltfan.backdrop.isRenderEffectSupported
import top.ltfan.backdrop.isRuntimeShaderSupported
import top.ltfan.backdrop.shadow.innerShadow
import top.ltfan.backdrop.shadow.shadow
import top.ltfan.backdrop.side
import top.ltfan.m3x.backdrop.BackdropAppearance
import top.ltfan.m3x.backdrop.BackdropAppearanceState
import top.ltfan.m3x.backdrop.LocalBackdrop
import top.ltfan.m3x.backdrop.LocalProgressiveBlurMode
import top.ltfan.m3x.backdrop.ProgressiveBlurMode
import top.ltfan.m3x.backdrop.prefersDarkContent
import top.ltfan.m3x.backdrop.progressiveBlur
import top.ltfan.m3x.backdrop.rememberBackdropAppearanceState
import top.ltfan.m3x.compose.animation.BackdropBlurAnimationEnabled
import top.ltfan.m3x.compose.ui.accessibility.BackgroundTransparencyEnabled
import top.ltfan.m3x.compose.ui.accessibility.IncreasedContrastEnabled
import top.ltfan.m3x.compose.ui.accessibility.SpatialMotionEnabled
import top.ltfan.m3x.compose.ui.color.contrastRatioAgainst
import top.ltfan.m3x.compose.ui.color.withContrastAgainst
import top.ltfan.m3x.compose.ui.visual.BackdropBlurEnabled
import top.ltfan.m3x.material.MaterialSurfaceEdge
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.backdrop.theme.BackdropAdaptation
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropLighting
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial
import top.ltfan.m3x.material.backdrop.theme.backdrop
import top.ltfan.m3x.material.backdrop.theme.resolveAgainst
import top.ltfan.m3x.material.theme.extended

/**
 * A Material-colored backdrop surface using the current role's optical and interaction settings.
 * [material] overrides the role for this surface. [effects] replaces its complete optical effect
 * chain. The inverse panel role defaults to the Material inverse surface color pair. Without a
 * captured source the surface draws the configured Material tint directly.
 *
 * [fill] requests progressive filling independently of optical effects. When omitted, the style
 * fill retains its configured ramp; a style fill without a ramp paints a flat tint. The scroll-edge
 * role selects progressive processing for a uniform blur configuration.
 *
 * Backdrop surfaces use
 * [Material color roles](https://m3.material.io/styles/color/the-color-system/color-roles).
 *
 * @param modifier the [Modifier] applied to the surface
 * @param shape the shape of the surface
 * @param role the theme material role used by the surface
 * @param material the material used by this surface, or `null` to use the material for [role]
 * @param containerColor the fallback container color and the base tint when the material has no
 *   specified tint
 * @param contentColor the preferred color for content inside the surface
 * @param effects an optional replacement for the material's optical effect chain
 * @param edge the edge used by edge-aware effects and fills
 * @param interactionSource the source shared with an interactive child for synthetic press and
 *   focus lighting. Physical pointer input is observed directly; when `null`, focus and keyboard
 *   activation are observed through the modifier chain.
 * @param fill an optional progressive fill over the backdrop content
 * @param render an optional complete render graph, replacing the material graph and built-in
 *   optical effects, tint, fill, and decorations
 * @param presence optional background visibility fraction; child content retains its own opacity
 * @param blurEnabled whether built-in blur is allowed, subject to master and accessibility gates
 * @param progressiveBlurMode the progressive blur algorithm, or null to inherit the enclosing mode
 * @param content the content displayed inside the surface
 */
@Composable
public fun BackdropSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    role: MaterialSurfaceRole = MaterialSurfaceRole.Panel,
    material: M3xBackdropMaterial? = null,
    containerColor: Color =
        if (role == MaterialSurfaceRole.InversePanel) MaterialTheme.colorScheme.inverseSurface
        else MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    effects: (BackdropEffectScope.() -> Unit)? = null,
    edge: BackdropEdge = BackdropEdge.Top,
    interactionSource: InteractionSource? = null,
    fill: BackdropFill? = null,
    render: (BackdropRenderScope.() -> Unit)? = null,
    presence: (() -> Float)? = null,
    blurEnabled: Boolean = true,
    progressiveBlurMode: ProgressiveBlurMode? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val appearance =
        rememberMaterialAppearance(
            role,
            material,
            containerColor,
            contentColor,
            effects,
            edge,
            shape = shape,
            presence = presence,
            blurEnabled = blurEnabled,
            progressiveBlurMode = progressiveBlurMode,
        )
    Box(
        modifier.materialBackdropSurface(
            appearance,
            shape,
            edge = edge,
            interactionSource = interactionSource,
            fill = fill,
            render = render,
        ),
    ) {
        CompositionLocalProvider(LocalContentColor provides appearance.contentColor) { content() }
    }
}

/** Draws a theme material without content; sizing belongs to the caller. */
@Composable
public fun BackdropSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    role: MaterialSurfaceRole = MaterialSurfaceRole.Panel,
    material: M3xBackdropMaterial? = null,
    containerColor: Color =
        if (role == MaterialSurfaceRole.InversePanel) MaterialTheme.colorScheme.inverseSurface
        else MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    effects: (BackdropEffectScope.() -> Unit)? = null,
    edge: BackdropEdge = BackdropEdge.Top,
    interactionSource: InteractionSource? = null,
    fill: BackdropFill? = null,
    render: (BackdropRenderScope.() -> Unit)? = null,
    presence: (() -> Float)? = null,
    blurEnabled: Boolean = true,
    progressiveBlurMode: ProgressiveBlurMode? = null,
) {
    val appearance =
        rememberMaterialAppearance(
            role,
            material,
            containerColor,
            contentColor,
            effects,
            edge,
            shape = shape,
            presence = presence,
            blurEnabled = blurEnabled,
            progressiveBlurMode = progressiveBlurMode,
        )
    Spacer(
        modifier.materialBackdropSurface(
            appearance,
            shape,
            edge = edge,
            interactionSource = interactionSource,
            fill = fill,
            render = render,
        ),
    )
}

/** Resolved theme material drawing state and the matching foreground color. */
public class BackdropSurfaceAppearance
internal constructor(
    internal val role: MaterialSurfaceRole,
    internal val material: M3xBackdropMaterial,
    internal val style: BackdropStyle,
    internal val effects: BackdropEffectScope.() -> Unit,
    internal val tint: Color,
    internal val fallback: Color,
    public val contentColor: Color,
    internal val sampler: BackdropAppearanceState,
    internal val samplingEnabled: Boolean,
    internal val lighting: M3xBackdropLighting,
    internal val reduceMotion: Boolean,
    internal val animateBlur: Boolean,
    internal val presence: (() -> Float)?,
    internal val progressiveBlurMode: ProgressiveBlurMode,
    internal val progressiveBlur: Boolean,
    internal val blurEnabled: Boolean,
    internal val drawsBackground: Boolean,
    internal val shape: Shape,
    internal val edge: BackdropEdge,
    internal val colorEffects: BackdropEffectScope.() -> Unit,
    internal val opticalBlur: () -> BackdropBlur,
) {
    /** Zero presence draws no source; positive finite motion keeps its optical strength. */
    internal fun backgroundAlpha(): () -> Float {
        val presence = presence ?: return { 1f }
        return {
            val shown = presence().coerceIn(0f, 1f)
            if (animateBlur && shown > 0f) 1f else shown
        }
    }

    /** Fill strength follows finite optical motion; fallback opacity belongs to the background. */
    internal fun opticalPresence(): () -> Float {
        val presence = presence
        return if (animateBlur && presence != null) ({ presence().coerceIn(0f, 1f) }) else ({ 1f })
    }
}

/**
 * Resolves a theme material for a caller-owned container or drawing-only background.
 * [progressiveBlur] defaults to the scroll-edge role and converts uniform style blur to progressive
 * blur when enabled. Explicit native progressive configurations retain their algorithm and ramp.
 * [presence] controls background optics independently of child content. [blurEnabled] can disable
 * built-in blur locally, while the master and accessibility gates remain authoritative.
 */
@Composable
public fun rememberBackdropSurfaceAppearance(
    role: MaterialSurfaceRole = MaterialSurfaceRole.Panel,
    material: M3xBackdropMaterial? = null,
    containerColor: Color =
        if (role == MaterialSurfaceRole.InversePanel) MaterialTheme.colorScheme.inverseSurface
        else MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    effects: (BackdropEffectScope.() -> Unit)? = null,
    edge: BackdropEdge = BackdropEdge.Top,
    presence: (() -> Float)? = null,
    shape: Shape = RectangleShape,
    blurEnabled: Boolean = true,
    progressiveBlurMode: ProgressiveBlurMode? = null,
    progressiveBlur: Boolean = role == MaterialSurfaceRole.ScrollEdge,
): BackdropSurfaceAppearance =
    rememberMaterialAppearance(
        role,
        material,
        containerColor,
        contentColor,
        effects,
        edge,
        presence,
        shape,
        blurEnabled = blurEnabled,
        progressiveBlurMode = progressiveBlurMode,
        progressiveBlur = progressiveBlur,
    )

@Composable
internal fun rememberMaterialAppearance(
    role: MaterialSurfaceRole,
    material: M3xBackdropMaterial? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = LocalContentColor.current,
    effects: (BackdropEffectScope.() -> Unit)? = null,
    edge: BackdropEdge = BackdropEdge.Top,
    presence: (() -> Float)? = null,
    shape: Shape = RectangleShape,
    excludedEdges: Set<MaterialSurfaceEdge> = emptySet(),
    blurEnabled: Boolean = true,
    progressiveBlurMode: ProgressiveBlurMode? = null,
    progressiveBlur: Boolean = role == MaterialSurfaceRole.ScrollEdge,
    shaderSupported: Boolean = isRuntimeShaderSupported(),
): BackdropSurfaceAppearance {
    val theme = MaterialTheme.extended.backdrop
    val transparent = BackgroundTransparencyEnabled
    val increasedContrast = IncreasedContrastEnabled
    val resolved = material ?: theme.material(role)
    val receivingStyle = resolved.style.resolveAgainst(LocalBackdropStyle.current)
    val source = LocalBackdrop.current
    val requestedBlur = blurEnabled && BackdropBlurEnabled
    val allowedBlur = requestedBlur && isRenderEffectSupported()
    val mode = progressiveBlurMode ?: LocalProgressiveBlurMode.current
    val automaticProgressive by
        remember(receivingStyle.blur) {
            derivedStateOf { receivingStyle.blur!!.invoke() is BackdropBlur.Uniform }
        }
    val drawsBackground =
        !(progressiveBlur &&
            automaticProgressive &&
            requestedBlur &&
            effects == null &&
            resolved.render == null &&
            mode == ProgressiveBlurMode.Full &&
            !shaderSupported)
    val animateBlur = allowedBlur && BackdropBlurAnimationEnabled
    val adaptive =
        resolved.adaptation == BackdropAdaptation.Background && source != null && transparent
    val sampler =
        rememberBackdropAppearanceState(
            adaptive,
            MaterialTheme.colorScheme.background,
            contentColor.luminance() < 0.5f,
        )
    val brightness =
        animateFloatAsState(
            sampler.appearance?.perceptualBrightness ?: 0.5f,
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
            label = "BackdropBrightness",
        )
    val brightnessFraction by
        remember(brightness) { derivedStateOf { brightness.value.coerceIn(0f, 1f) } }
    val base = if (resolved.tint.isSpecified) resolved.tint else containerColor
    val tintAlpha =
        when {
            !transparent -> 1f
            // A scroll edge whose blur is off shows the page unblurred, so its fill has to carry
            // the whole separation and takes the fallback strength.
            role == MaterialSurfaceRole.ScrollEdge && !allowedBlur ->
                max(resolved.tintAlpha, resolved.fallbackAlpha)
            else -> resolved.tintAlpha
        }
    var darkContent by remember { mutableStateOf(contentColor.luminance() < 0.5f) }
    val signed = brightnessFraction * 2f - 1f
    val amount = signed * abs(signed)
    val adjustment = if (amount > 0f) 0.1f + 0.4f * amount else 0.1f + 0.3f * amount
    val contrast = if (amount > 0f) 1f - amount else 1f
    val predicted =
        (brightnessFraction * contrast + 0.5f - contrast * 0.5f + adjustment).coerceIn(0f, 1f)
    val predictedSurface =
        base
            .copy(alpha = tintAlpha.coerceIn(0f, 1f))
            .compositeOver(Color(predicted, predicted, predicted))
    val foregroundLuminance = predictedSurface.luminance()
    LaunchedEffect(foregroundLuminance, adaptive) {
        if (adaptive)
            darkContent =
                BackdropAppearance(foregroundLuminance, 0f).prefersDarkContent(darkContent)
    }
    val foreground =
        if (adaptive && resolved.adaptiveContentColor && sampler.appearance != null)
            if (darkContent) Color.Black else Color.White
        else contentColor
    val knownSource = source != null && sampler.appearance != null
    val contrastAppearance =
        remember(increasedContrast, base, foreground, tintAlpha, knownSource) {
            if (increasedContrast) resolveMaterialContrast(base, foreground, tintAlpha, knownSource)
            else null
        }
    val surfaceTint = contrastAppearance?.first ?: base.copy(alpha = tintAlpha.coerceIn(0f, 1f))
    val surfaceForeground = contrastAppearance?.second ?: foreground
    val effectChain: BackdropEffectScope.() -> Unit =
        if (!transparent) ({})
        else
            effects
                ?: {
                    vibrancy(receivingStyle.vibrancy!!.invoke())
                    if (this.shape is CornerBasedShape) lens(receivingStyle.lens!!.invoke())
                }
    val style =
        if (!transparent)
            BackdropStyle.NoEffects {
                fill = { BackdropFill() }
                insets = receivingStyle.insets
            }
        else
            receivingStyle {
                highlight = { receivingStyle.highlight!!.invoke().excludeEdges(excludedEdges) }
                interactionLighting = {
                    if (theme.lighting.enabled && resolved.interactiveLighting)
                        receivingStyle.interactionLighting!!.invoke()
                    else InteractionLighting.None
                }
            }
    return BackdropSurfaceAppearance(
        role,
        resolved,
        style,
        effectChain,
        surfaceTint,
        surfaceTint.copy(
            alpha =
                if (!transparent || increasedContrast) 1f
                else resolved.fallbackAlpha.coerceIn(0f, 1f),
        ),
        surfaceForeground,
        sampler,
        adaptive,
        theme.lighting,
        !SpatialMotionEnabled,
        animateBlur,
        presence,
        mode,
        progressiveBlur && effects == null,
        allowedBlur,
        drawsBackground,
        shape,
        edge,
        {
            if (transparent && effects == null)
                colorControls(receivingStyle.colorControls!!.invoke())
            if (transparent && effects == null && adaptive) {
                val signed = brightnessFraction * 2f - 1f
                val amount = signed * abs(signed)
                colorControls(
                    brightness = if (amount > 0f) 0.1f + 0.4f * amount else 0.1f + 0.3f * amount,
                    contrast = if (amount > 0f) 1f - amount else 1f,
                )
            }
        },
        {
            if (!transparent || !allowedBlur || effects != null) BackdropBlur.None
            else {
                val scale =
                    (if (adaptive && animateBlur) 0.5f + brightnessFraction * 1.5f else 1f) *
                        (if (animateBlur) (presence?.invoke() ?: 1f).coerceIn(0f, 1f) else 1f)
                receivingStyle.blur!!.invoke().scaled(scale)
            }
        },
    )
}

/**
 * Draws [appearance] behind a caller-owned container, preserving this modifier and child content.
 * [render] replaces the built-in graph. [observeInput] controls interaction-light observation.
 * [progressiveFill] controls ramping independently of the supplied fill brush and opacity.
 */
@Composable
public fun Modifier.materialBackdropSurface(
    appearance: BackdropSurfaceAppearance,
    shape: Shape = appearance.shape,
    edge: BackdropEdge = appearance.edge,
    exportedBackdrop: LayerBackdrop? = null,
    interactionSource: InteractionSource? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    fill: BackdropFill? = null,
    progressiveFill: Boolean = true,
    observeInput: Boolean = true,
    render: (BackdropRenderScope.() -> Unit)? = null,
): Modifier {
    if (!appearance.drawsBackground && render == null && appearance.material.render == null)
        return this
    val source = LocalBackdrop.current
    val backgroundAlpha = appearance.backgroundAlpha()
    val opticalPresence = appearance.opticalPresence()
    // A supplied fill or style fill ramp enables progressive filling independently of role blur.
    // An explicit brush preserves the caller's background colors within that ramp.
    val fillGeometry by
        remember(fill, appearance.style.fill) {
            derivedStateOf {
                val configured = fill ?: appearance.style.fill!!.invoke()
                configured.ramp to configured.edge
            }
        }
    val ramped =
        (fill != null || fillGeometry.first != null) &&
            progressiveFill &&
            BackgroundTransparencyEnabled
    val fillTint = if (source == null) appearance.fallback else appearance.tint
    val explicitBrush = fill?.brush
    val explicitAlpha = fill?.alpha
    val fallbackBrush = SolidColor(fillTint.copy(alpha = 1f))
    val fallbackAlpha = fillTint.alpha.coerceIn(0f, 1f)
    fun fillBrush(): Brush =
        explicitBrush ?: appearance.style.fill!!.invoke().brush ?: fallbackBrush
    fun fillAlpha(): Float {
        val styled = appearance.style.fill!!.invoke()
        return (explicitAlpha ?: styled.alpha ?: if (styled.brush != null) 1f else fallbackAlpha)
            .coerceIn(0f, 1f)
    }
    // Resolve layout-relative fill geometry in the receiving surface's layout direction.
    val layoutDirection = LocalLayoutDirection.current
    val fillRamp = fillGeometry.first ?: appearance.material.ramp
    val fillEdge = if (fill == null && fillGeometry.first != null) fillGeometry.second else edge
    val rampCache = remember { BackdropFillRampCache() }
    if (source == null) {
        // Without a source the surface paints the resolved fallback tint, through the same ramp
        // when it has one. The background alpha parameter scales the fallback directly; there is
        // no captured background to fade.
        return if (ramped)
            drawBehind {
                val room = backdropFillRoom(fillRamp, fillEdge, size)
                val shown = backgroundAlpha() * opticalPresence()
                if (shown > 0.001f) {
                    clipToGrownShape(shape, room.insets) {
                        translate(-room.insets.left.toPx(), -room.insets.top.toPx()) {
                            drawBackdropFill(
                                ramped = true,
                                brush = fillBrush(),
                                ramp = fillRamp,
                                domain = room.domain,
                                cache = rampCache,
                                edge = fillEdge,
                                surfaceSize = size,
                                leadingInset = room.leading,
                                alpha = fillAlpha() * shown,
                            )
                        }
                    }
                }
            }
        else
            drawBehind {
                val shown = backgroundAlpha() * opticalPresence()
                if (shown > 0.001f) {
                    clipToGrownShape(shape, BackdropInsets.None) {
                        drawBackdropFill(
                            ramped = false,
                            brush = fillBrush(),
                            ramp = fillRamp,
                            domain = size,
                            cache = rampCache,
                            edge = fillEdge,
                            surfaceSize = size,
                            leadingInset = 0f,
                            alpha = fillAlpha() * shown,
                        )
                    }
                }
            }
    }
    val interactionLightModifier =
        if (observeInput)
            Modifier.interactionLightSource(interactionSource) {
                if (appearance.material.interactiveLighting && appearance.lighting.enabled) {
                    if (appearance.reduceMotion)
                        InteractionLight.Config(
                            color = appearance.lighting.color,
                            radius = appearance.lighting.radius,
                            releaseDuration = Duration.ZERO,
                        )
                    else
                        InteractionLight.Config(
                            color = appearance.lighting.color,
                            radius = appearance.lighting.radius,
                        )
                } else InteractionLight.None
            }
        else Modifier
    val paintMaterial: DrawScope.(BackdropRoom) -> Unit = { room ->
        val shown = opticalPresence().coerceIn(0f, 1f)
        if (appearance.material.dimmingAlpha > 0f)
            drawRect(
                Color.Black.copy(
                    alpha = (appearance.material.dimmingAlpha * shown).coerceIn(0f, 1f),
                ),
            )
        val extension = room.extension
        if (ramped) {
            translate(extension.originInNode.x, extension.originInNode.y) {
                drawBackdropFill(
                    ramped = true,
                    brush = fillBrush(),
                    ramp = fillRamp,
                    domain =
                        Size(
                            room.size.width + extension.left + extension.right,
                            room.size.height + extension.top + extension.bottom,
                        ),
                    cache = rampCache,
                    edge = fillEdge,
                    surfaceSize = room.size,
                    leadingInset =
                        if (fillEdge.side(layoutDirection).isVertical) extension.top
                        else extension.left,
                    alpha = fillAlpha() * shown,
                )
            }
        } else {
            // A flattened fill covers the surface alone: an opaque band past it would hide
            // the content a reader needs to see.
            drawBackdropFill(
                ramped = false,
                brush = fillBrush(),
                ramp = fillRamp,
                domain = room.size,
                cache = rampCache,
                edge = fillEdge,
                surfaceSize = room.size,
                leadingInset = 0f,
                alpha = fillAlpha() * shown,
            )
        }
    }

    val builtInRender: BackdropRenderScope.() -> Unit = {
        // Appearance sampling reads the raw source before optical effects and tint, matching the
        // colors a reader actually sees behind the surface.
        val raw =
            if (appearance.samplingEnabled) {
                image {
                    source()
                    drawWithContent { drawContent, _ ->
                        with(appearance.sampler) { drawAndRecord { drawContent() } }
                    }
                }
            } else input
        val material = image {
            replace(raw)
            effects {
                // The fill needs room for the full span of its ramp, even when no optical effect
                // runs. Without that room, the layer would end at the surface and clip the fill.
                if (ramped) coverRamp(fillRamp, fillEdge)
            }
            materialEffects(appearance)
            clipSurface()
        }
        val lighting = appearance.style.interactionLighting!!.invoke()
        group {
            shadow(appearance.style.shadow!!.invoke())
            draw(material)
            paintSurface(compositingStrategy = CompositingStrategy.Auto, draw = paintMaterial)
            if (lighting is InteractionLighting.Config) interactionLight(lighting)
        }
        foreground {
            highlight(
                appearance.style.highlight!!.invoke(),
                colorSource = material,
                interactionLighting = lighting,
            )
            innerShadow(appearance.style.innerShadow!!.invoke())
        }
    }
    return onGloballyPositioned { appearance.sampler.updateCoordinates(it) }
        .drawBackdrop(
            backdrop = source.backdrop,
            shape = { shape },
            backgroundAlpha = backgroundAlpha,
            insets = { appearance.style.insets!!.invoke() },
            layerBlock = layerBlock,
            exportedBackdrop = exportedBackdrop,
            render = render ?: appearance.material.render ?: builtInRender,
        )
        .then(interactionLightModifier)
}

/** Preserves native blur algorithms while adapting uniform role blur to a scroll edge. */
internal fun BackdropImageScope.materialEffects(appearance: BackdropSurfaceAppearance) {
    effects { appearance.colorEffects(this) }
    if (appearance.progressiveBlur && appearance.progressiveBlurMode == ProgressiveBlurMode.Alpha) {
        val configured = appearance.opticalBlur()
        if (configured is BackdropBlur.Uniform) {
            progressiveBlur(
                with(density) { configured.radius.toPx() },
                appearance.edge,
                appearance.material.ramp,
                ProgressiveBlurMode.Alpha,
            )
        } else blur(appearance.opticalBlur)
    } else
        blur {
            val configured = appearance.opticalBlur()
            if (!appearance.progressiveBlur || configured !is BackdropBlur.Uniform) configured
            else
                when (val mode = appearance.progressiveBlurMode) {
                    ProgressiveBlurMode.Auto ->
                        if (isRuntimeShaderSupported())
                            BackdropBlur.Progressive(
                                configured.radius,
                                appearance.edge,
                                appearance.material.ramp,
                            )
                        else
                            BackdropBlur.LayeredProgressive(
                                configured.radius,
                                appearance.edge,
                                appearance.material.ramp,
                            )
                    is ProgressiveBlurMode.Layered ->
                        BackdropBlur.LayeredProgressive(
                            configured.radius,
                            appearance.edge,
                            appearance.material.ramp,
                            mode.levels,
                        )
                    ProgressiveBlurMode.Full ->
                        BackdropBlur.Progressive(
                            configured.radius,
                            appearance.edge,
                            appearance.material.ramp,
                        )
                    ProgressiveBlurMode.Alpha -> configured
                }
        }
    effects { appearance.effects(this) }
}

private fun BackdropBlur.scaled(scale: Float): BackdropBlur =
    when (this) {
        BackdropBlur.None -> this
        is BackdropBlur.Uniform -> copy(radius = radius * scale)
        is BackdropBlur.Progressive -> copy(radius = radius * scale)
        is BackdropBlur.LayeredProgressive -> copy(radius = radius * scale)
    }

/**
 * Hides the contour response along [edges]. Backdrop measures disabled angles relative to the
 * highlight direction, so the physical bottom normal is translated into that relative domain.
 */
private fun Highlight.excludeEdges(edges: Set<MaterialSurfaceEdge>): Highlight {
    if (edges.isEmpty() || this !is Highlight.Config) return this
    val directionAngle =
        when (val direction = direction) {
            HighlightDirection.Uniform -> 0f
            is HighlightDirection.Incidence -> direction.angle
            is HighlightDirection.Angular -> direction.angle
        }
    val startEdge = BottomEdgeAngle - ExclusionHalfAngle
    val endEdge = BottomEdgeAngle + ExclusionHalfAngle
    val ranges =
        if (disabledAngleDomain == HighlightAngleDomain.Unsigned) {
            val start = unsignedDistance(startEdge, directionAngle)
            val end = unsignedDistance(endEdge, directionAngle)
            val crosses = angularDistanceContains(directionAngle)
            listOf((if (crosses) 0f else minOf(start, end))..maxOf(start, end))
        } else {
            directedCut(directionAngle)
        }
    return copy(disabledAngles = disabledAngles + ranges)
}

private fun directedCut(directionAngle: Float): List<ClosedFloatingPointRange<Float>> {
    val start = normalizeAngle(BottomEdgeAngle - ExclusionHalfAngle - directionAngle)
    val end = normalizeAngle(BottomEdgeAngle + ExclusionHalfAngle - directionAngle)
    return if (start < end) listOf(start..end) else listOf(start..360f, 0f..end)
}

private fun unsignedDistance(angle: Float, direction: Float): Float {
    val turn = normalizeAngle(angle - direction)
    return minOf(turn, 360f - turn)
}

private fun angularDistanceContains(direction: Float): Boolean {
    val from = normalizeAngle(BottomEdgeAngle - ExclusionHalfAngle - direction)
    val to = normalizeAngle(BottomEdgeAngle + ExclusionHalfAngle - direction)
    return if (from <= to) from == 0f || to >= 180f && from <= 180f else true
}

private fun normalizeAngle(angle: Float): Float = ((angle % 360f) + 360f) % 360f

private const val BottomEdgeAngle = 90f
private const val ExclusionHalfAngle = 45f

/**
 * Resolves contrast against the full SDR backdrop range, with an opaque unknown-source fallback.
 */
internal fun resolveMaterialContrast(
    base: Color,
    foreground: Color,
    alpha: Float,
    knownSource: Boolean,
): Pair<Color, Color> {
    val content = foreground.withContrastAgainst(base.copy(alpha = 1f))
    val container = base.copy(alpha = 1f).withContrastAgainst(content)
    if (!knownSource) return container to content
    fun sufficient(opacity: Float): Boolean =
        listOf(Color.Black, Color.White).all { behind ->
            content.contrastRatioAgainst(container.copy(alpha = opacity).compositeOver(behind)) >=
                7f
        }
    var low = alpha.coerceIn(0f, 1f)
    var high = 1f
    if (sufficient(low)) return container.copy(alpha = low) to content
    repeat(24) {
        val middle = (low + high) / 2f
        if (sufficient(middle)) high = middle else low = middle
    }
    return container.copy(alpha = high) to content
}
