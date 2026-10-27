package top.ltfan.m3x.material.backdrop

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.ltfan.backdrop.BackdropBlur
import top.ltfan.backdrop.BackdropEdge
import top.ltfan.backdrop.BackdropEffectScope
import top.ltfan.backdrop.BackdropFill
import top.ltfan.backdrop.BackdropImageScope
import top.ltfan.backdrop.LocalBackdropStyle
import top.ltfan.backdrop.backdrops.LayerBackdrop
import top.ltfan.m3x.backdrop.LocalProgressiveBlurMode
import top.ltfan.m3x.backdrop.ProgressiveBlurMode
import top.ltfan.m3x.backdrop.progressiveBlur as progressiveImageBlur
import top.ltfan.m3x.compose.ui.visual.BackdropBlurEnabled
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial
import top.ltfan.m3x.material.backdrop.theme.backdrop
import top.ltfan.m3x.material.backdrop.theme.resolveAgainst
import top.ltfan.m3x.material.theme.extended

/** Defaults for progressive backgrounds at physical or layout-relative scroll edges. */
public object BackdropScrollEdgeDefaults {
    /**
     * Image-level progressive blur using the theme scroll-edge radius and ramp. The master and
     * accessibility blur gates apply; a custom graph owns its source, fill and presence.
     */
    @Composable
    public fun progressiveBlur(
        edge: BackdropEdge = BackdropEdge.Top,
        radius: Dp =
            MaterialTheme.extended.backdrop.scrollEdge.style
                .resolveAgainst(LocalBackdropStyle.current)
                .blur!!
                .invoke()
                .let { blur ->
                    when (blur) {
                        is BackdropBlur.Uniform -> blur.radius
                        is BackdropBlur.Progressive -> blur.radius
                        is BackdropBlur.LayeredProgressive -> blur.radius
                        BackdropBlur.None -> 0.dp
                    }
                },
        mode: ProgressiveBlurMode = LocalProgressiveBlurMode.current,
    ): BackdropImageScope.() -> Unit {
        val enabled = BackdropBlurEnabled
        val ramp = MaterialTheme.extended.backdrop.scrollEdge.ramp
        return {
            if (enabled) progressiveImageBlur(with(density) { radius.toPx() }, edge, ramp, mode)
        }
    }
}

/** Bounded effects motion for scroll-edge strength. */
@Composable
internal fun scrollEdgePresenceSpec(): FiniteAnimationSpec<Float> =
    MaterialTheme.motionScheme.defaultEffectsSpec()

/** Animates a caller-owned visibility condition with the theme's bounded effects motion. */
@Composable
public fun rememberScrollEdgePresence(visible: () -> Boolean): State<Float> {
    val currentVisible = rememberUpdatedState(visible)
    val target by remember { derivedStateOf { if (currentVisible.value()) 1f else 0f } }
    return animateFloatAsState(target, scrollEdgePresenceSpec(), label = "BackdropScrollEdge")
}

/**
 * Draws the theme scroll-edge material over the enclosing captured source. [visible] can read
 * scroll state, list state, or another caller-owned condition. [edge] names the strongest side;
 * Start and End follow layout direction. Children remain sharp.
 *
 * [progressive] coordinates blur and fill ramps, including their drawing room outside the measured
 * surface. False keeps automatic uniform blur and fill within a static container; explicit native
 * progressive blur retains its configured geometry. [fill] defaults to the role tint;
 * [BackdropFill.None] selects a transparent fill for a blur-only edge. A [material] overrides the
 * theme role, including its default ramp. Explicit [effects] replaces the optical chain. Blur
 * remains subject to [blurEnabled], master and accessibility gates.
 *
 * Finite source boundary behavior belongs to the capture's tile mode. An opaque bounded strip
 * normally needs Clamp so blur cannot reveal the unchanged content through transparent exterior
 * samples. Capture the complete background and controls, and draw the edge outside that capture.
 */
@Composable
public fun Modifier.backdropScrollEdge(
    edge: BackdropEdge = BackdropEdge.Top,
    visible: () -> Boolean = { true },
    material: M3xBackdropMaterial? = null,
    effects: (BackdropEffectScope.() -> Unit)? = null,
    fill: BackdropFill? = BackdropFill(),
    exportedBackdrop: LayerBackdrop? = null,
    progressive: Boolean = true,
    blurEnabled: Boolean = true,
    progressiveBlurMode: ProgressiveBlurMode? = null,
): Modifier =
    backdropScrollEdge(
        appearance =
            rememberScrollEdgeAppearance(
                visible,
                material,
                effects,
                edge,
                progressive,
                blurEnabled,
                progressiveBlurMode,
            ),
        edge = edge,
        fill = fill,
        exportedBackdrop = exportedBackdrop,
        progressive = progressive,
    )

@Composable
internal fun rememberScrollEdgeAppearance(
    visible: () -> Boolean,
    material: M3xBackdropMaterial? = null,
    effects: (BackdropEffectScope.() -> Unit)? = null,
    edge: BackdropEdge = BackdropEdge.Top,
    progressive: Boolean = true,
    blurEnabled: Boolean = true,
    progressiveBlurMode: ProgressiveBlurMode? = null,
): BackdropSurfaceAppearance {
    val presence = rememberScrollEdgePresence(visible)
    return rememberMaterialAppearance(
        MaterialSurfaceRole.ScrollEdge,
        material = material,
        effects = effects,
        edge = edge,
        presence = { presence.value },
        shape = RectangleShape,
        blurEnabled = blurEnabled,
        progressiveBlurMode = progressiveBlurMode,
        progressiveBlur = progressive,
    )
}

@Composable
internal fun Modifier.backdropScrollEdge(
    appearance: BackdropSurfaceAppearance,
    edge: BackdropEdge,
    fill: BackdropFill?,
    exportedBackdrop: LayerBackdrop?,
    progressive: Boolean,
): Modifier =
    materialBackdropSurface(
        appearance = appearance,
        shape = RectangleShape,
        edge = edge,
        fill = fill,
        exportedBackdrop = exportedBackdrop,
        progressiveFill = progressive,
        observeInput = false,
    )
