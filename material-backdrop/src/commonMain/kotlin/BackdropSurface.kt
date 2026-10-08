package top.ltfan.m3x.material.backdrop

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlin.math.abs
import kotlin.math.max
import kotlin.time.Duration
import top.ltfan.backdrop.BackdropEdge
import top.ltfan.backdrop.BackdropEffectScope
import top.ltfan.backdrop.BackdropInsets
import top.ltfan.backdrop.BackdropMask
import top.ltfan.backdrop.BackdropMaskRampCache
import top.ltfan.backdrop.BackdropStyle
import top.ltfan.backdrop.backdropMaskRoom
import top.ltfan.backdrop.backdrops.LayerBackdrop
import top.ltfan.backdrop.clipToGrownShape
import top.ltfan.backdrop.drawBackdropMask
import top.ltfan.backdrop.effects.blur
import top.ltfan.backdrop.effects.colorControls
import top.ltfan.backdrop.effects.coverRamp
import top.ltfan.backdrop.effects.progressiveBlur
import top.ltfan.backdrop.highlight.Highlight
import top.ltfan.backdrop.highlight.HighlightExclusion
import top.ltfan.backdrop.interaction.InteractionLight
import top.ltfan.backdrop.interaction.InteractionLighting
import top.ltfan.backdrop.interaction.interactionLightSource
import top.ltfan.backdrop.side
import top.ltfan.m3x.backdrop.BackdropAppearance
import top.ltfan.m3x.backdrop.BackdropAppearanceState
import top.ltfan.m3x.backdrop.LocalBackdrop
import top.ltfan.m3x.backdrop.backdropSurface
import top.ltfan.m3x.backdrop.prefersDarkContent
import top.ltfan.m3x.backdrop.rememberBackdropAppearanceState
import top.ltfan.m3x.compose.animation.BackdropBlurAnimationEnabled
import top.ltfan.m3x.compose.ui.accessibility.BackgroundTransparencyEnabled
import top.ltfan.m3x.compose.ui.accessibility.IncreasedContrastEnabled
import top.ltfan.m3x.compose.ui.accessibility.SpatialMotionEnabled
import top.ltfan.m3x.compose.ui.color.contrastRatioAgainst
import top.ltfan.m3x.compose.ui.color.withContrastAgainst
import top.ltfan.m3x.compose.ui.visual.BackdropBlurEnabled
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.backdrop.theme.BackdropAdaptation
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropLighting
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial
import top.ltfan.m3x.material.backdrop.theme.backdrop
import top.ltfan.m3x.material.theme.extended

/**
 * A Material-colored backdrop surface using the current role's optical and interaction settings.
 * [material] overrides the role for this surface. [effects] replaces its full optical effect chain.
 * The inverse panel role defaults to the Material inverse surface color pair. Without a captured
 * source the surface draws the configured Material tint directly.
 *
 * [mask] asks for a progressive mask over the content behind the surface; without one the tint is
 * flat, whatever the role blurs. The scroll edge role selects the progressive blur, and a surface
 * that wants that blur masked passes the mask here.
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
 * @param edge the edge used by edge-aware effects and masks
 * @param interactionSource the source shared with an interactive child for synthetic press and
 *   focus lighting. Physical pointer input is observed directly; when `null`, focus and keyboard
 *   activation are observed through the modifier chain.
 * @param mask an optional progressive mask over the backdrop content
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
    mask: BackdropMask? = null,
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
        )
    Box(
        modifier.materialBackdropSurface(
            appearance,
            shape,
            edge = edge,
            interactionSource = interactionSource,
            mask = mask,
        ),
    ) {
        CompositionLocalProvider(LocalContentColor provides appearance.contentColor) { content() }
    }
}

internal class MaterialBackdropAppearance(
    val role: MaterialSurfaceRole,
    val material: M3xBackdropMaterial,
    val style: BackdropStyle,
    val tint: Color,
    val fallback: Color,
    val contentColor: Color,
    val sampler: BackdropAppearanceState,
    val lighting: M3xBackdropLighting,
    val reduceMotion: Boolean,
)

@Composable
internal fun rememberMaterialAppearance(
    role: MaterialSurfaceRole,
    material: M3xBackdropMaterial? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = LocalContentColor.current,
    effects: (BackdropEffectScope.() -> Unit)? = null,
    edge: BackdropEdge = BackdropEdge.Top,
    presence: State<Float>? = null,
    shape: Shape = RectangleShape,
    highlightExclusions: Set<HighlightExclusion> = emptySet(),
): MaterialBackdropAppearance {
    val theme = MaterialTheme.extended.backdrop
    val transparent = BackgroundTransparencyEnabled
    val increasedContrast = IncreasedContrastEnabled
    val resolved = material ?: theme.material(role)
    val source = LocalBackdrop.current
    val blurEnabled = BackdropBlurEnabled
    val animateBlur = BackdropBlurAnimationEnabled
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
            // A scroll edge whose blur is off shows the page unblurred, so its mask has to carry
            // the whole separation and takes the fallback strength.
            role == MaterialSurfaceRole.ScrollEdge && !blurEnabled ->
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
    val style =
        resolved.style.copy(
            effects =
                if (!transparent) BackdropStyle.NoEffects
                else
                    effects
                        ?: {
                            if (adaptive) {
                                val signed = brightnessFraction * 2f - 1f
                                val amount = signed * abs(signed)
                                colorControls(
                                    brightness =
                                        if (amount > 0f) 0.1f + 0.4f * amount
                                        else 0.1f + 0.3f * amount,
                                    contrast = if (amount > 0f) 1f - amount else 1f,
                                )
                            }
                            if (blurEnabled) {
                                val radius =
                                    if (adaptive && animateBlur)
                                        resolved.blurRadius.toPx() *
                                            (0.5f + brightnessFraction * 1.5f)
                                    else resolved.blurRadius.toPx()
                                if (role == MaterialSurfaceRole.ScrollEdge) {
                                    // A scroll edge is only as strong as it is present, so its blur
                                    // follows the bar in and out. The presence is read here, while
                                    // drawing, so the animation needs no recomposition.
                                    val shown = (presence?.value ?: 1f).coerceIn(0f, 1f)
                                    if (shown > 0.001f)
                                        progressiveBlur(
                                            radius * shown,
                                            edge,
                                            resolved.ramp,
                                        )
                                } else blur(radius)
                            }
                            resolved.style.effects(this)
                        },
            highlight = {
                val configuredHighlight = resolved.style.highlight()
                if (configuredHighlight is Highlight.Config && highlightExclusions.isNotEmpty())
                    configuredHighlight.copy(
                        exclusions = configuredHighlight.exclusions + highlightExclusions,
                    )
                else configuredHighlight
            },
            interactionLighting = {
                if (theme.lighting.enabled && resolved.interactiveLighting)
                    InteractionLighting.Config(
                        interiorOpacity = theme.lighting.interiorAlpha,
                        contourOpacity = theme.lighting.edgeAlpha,
                    )
                else InteractionLighting.None
            },
        )
    return MaterialBackdropAppearance(
        role,
        resolved,
        style,
        surfaceTint,
        surfaceTint.copy(
            alpha =
                if (!transparent || increasedContrast) 1f
                else resolved.fallbackAlpha.coerceIn(0f, 1f),
        ),
        surfaceForeground,
        sampler,
        theme.lighting,
        !SpatialMotionEnabled,
    )
}

@Composable
internal fun Modifier.materialBackdropSurface(
    appearance: MaterialBackdropAppearance,
    shape: Shape,
    edge: BackdropEdge = BackdropEdge.Top,
    exportedBackdrop: LayerBackdrop? = null,
    interactionSource: InteractionSource? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    mask: BackdropMask? = null,
    presence: State<Float>? = null,
    observeInput: Boolean = true,
): Modifier {
    val material = appearance.material
    val source = LocalBackdrop.current
    // Only an explicit mask ramps: a role selects how the surface blurs, never that it masks. A
    // caller-supplied background layer joins the ramp, so a page that paints its own background
    // continues into the surface instead of being covered by a theme color.
    val ramped = mask != null && BackgroundTransparencyEnabled
    val maskTint = if (source == null) appearance.fallback else appearance.tint
    val maskBrush = mask?.brush ?: SolidColor(maskTint.copy(alpha = 1f))
    val maskAlpha = (mask?.alpha ?: maskTint.alpha).coerceIn(0f, 1f)
    // A mask that carries its own ramp follows the bar where that ramp leaves its ends to the
    // drawing area, so changing the curve keeps the shape the surface measured.
    val layoutDirection = LocalLayoutDirection.current
    val maskRamp = mask?.ramp ?: material.ramp
    val rampCache = remember { BackdropMaskRampCache() }
    if (source == null)
        return if (ramped)
            // The chain extends the caller's modifier, so the drawing applies to the implicit
            // receiver. Nothing samples beyond the surface here, so the mask covers exactly what
            // its
            // own ramp reaches past it.
            drawBehind {
                val shown = (presence?.value ?: 1f).coerceIn(0f, 1f)
                if (shown > 0.001f) {
                    val room = backdropMaskRoom(maskRamp, edge, size)
                    clipToGrownShape(shape, room.insets) {
                        translate(-room.insets.left.toPx(), -room.insets.top.toPx()) {
                            drawBackdropMask(
                                ramped = true,
                                brush = maskBrush,
                                alpha = maskAlpha * shown,
                                ramp = maskRamp,
                                edge = edge,
                                surfaceSize = size,
                                leadingInset = room.leading,
                                domain = room.domain,
                                cache = rampCache,
                            )
                        }
                    }
                }
            }
        else
            drawBehind {
                val shown = (presence?.value ?: 1f).coerceIn(0f, 1f)
                if (shown > 0.001f) {
                    clipToGrownShape(shape, BackdropInsets.None) {
                        drawBackdropMask(
                            ramped = false,
                            brush = maskBrush,
                            alpha = maskAlpha * shown,
                            ramp = maskRamp,
                            edge = edge,
                            surfaceSize = size,
                            leadingInset = 0f,
                            domain = size,
                            cache = rampCache,
                        )
                    }
                }
            }
    val interactionLightModifier =
        if (observeInput)
            Modifier.interactionLightSource(interactionSource) {
                if (material.interactiveLighting && appearance.lighting.enabled) {
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
    return onGloballyPositioned { appearance.sampler.updateCoordinates(it) }
        .backdropSurface(
            shape = shape,
            effects = {
                // The mask needs room for the full span of its ramp, even when no optical effect
                // runs. Without that room, the layer ends at the surface and clips the mask. The
                // clipped ramp leaves a visible line.
                coverRamp(maskRamp, edge)
                appearance.style.effects(this)
            },
            highlight = appearance.style.highlight,
            shadow = appearance.style.shadow,
            innerShadow = appearance.style.innerShadow,
            interactionLighting = appearance.style.interactionLighting,
            exportedBackdrop = exportedBackdrop,
            layerBlock = layerBlock,
            onDrawBackdrop = { draw, _ -> with(appearance.sampler) { drawAndRecord(draw) } },
            onDrawSurface = { room ->
                val shown = (presence?.value ?: 1f).coerceIn(0f, 1f)
                if (material.dimmingAlpha > 0f)
                    drawRect(
                        Color.Black.copy(alpha = (material.dimmingAlpha * shown).coerceIn(0f, 1f)),
                    )
                // Nothing is masked while the glass is absent, so the page shows through whole.
                if (shown > 0.001f && ramped) {
                    // The mask covers the room the effects cover, which is the layer the node
                    // resolved for them: the ramp decides how far that reaches, so the two agree.
                    val room = room.extension
                    translate(room.originInNode.x, room.originInNode.y) {
                        drawBackdropMask(
                            ramped = true,
                            brush = maskBrush,
                            alpha = maskAlpha * shown,
                            ramp = maskRamp,
                            edge = edge,
                            surfaceSize = room.let { _ -> size },
                            leadingInset =
                                if (edge.side(layoutDirection).isVertical) room.top else room.left,
                            domain =
                                Size(
                                    size.width + room.left + room.right,
                                    size.height + room.top + room.bottom,
                                ),
                            cache = rampCache,
                        )
                    }
                } else if (shown > 0.001f) {
                    // A flattened mask covers the surface alone: an opaque band past it would hide
                    // the content a reader needs to see.
                    drawBackdropMask(
                        ramped = false,
                        brush = maskBrush,
                        alpha = maskAlpha * shown,
                        ramp = maskRamp,
                        edge = edge,
                        surfaceSize = room.size,
                        leadingInset = 0f,
                        domain = room.size,
                        cache = rampCache,
                    )
                }
            },
        )
        .then(interactionLightModifier)
}

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
