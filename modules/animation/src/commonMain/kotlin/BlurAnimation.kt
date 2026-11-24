package top.ltfan.m3x.compose.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.ltfan.m3x.compose.ui.accessibility.SpatialMotionEnabled
import top.ltfan.m3x.compose.ui.visual.BackdropBlurEnabled
import top.ltfan.m3x.compose.ui.visual.DefaultEnterExitBlurRadius
import top.ltfan.m3x.compose.ui.visual.LocalBlurEnabled

/** Controls foreground transition blur and bounded backdrop blur changes. */
public val LocalBlurAnimationEnabled: ProvidableCompositionLocal<Boolean> = compositionLocalOf {
    true
}

/** Whether foreground blur motion is permitted by preferences and accessibility. */
public val BlurAnimationEnabled: Boolean
    @Composable
    inline get() =
        LocalBlurEnabled.current && LocalBlurAnimationEnabled.current && SpatialMotionEnabled

/** Whether bounded backdrop blur may vary rather than crossfade at a fixed strength. */
public val BackdropBlurAnimationEnabled: Boolean
    @Composable inline get() = BackdropBlurEnabled && BlurAnimationEnabled

/**
 * Blurs and fades foreground content from hidden (zero) to sharp (one), clamping [progress] to that
 * range. With [fade] enabled, disabled or unsupported blur preserves fading.
 *
 * [maxRadius] must be finite and nonnegative; zero preserves fading without a render effect.
 * Unbounded edges let blur extend outside layout bounds. Set [fade] to false when an enclosing
 * transition already owns opacity. Layout and interaction remain owned by the caller.
 */
@Composable
public fun Modifier.blurTransition(
    progress: () -> Float,
    maxRadius: Dp = DefaultEnterExitBlurRadius,
    edgeTreatment: BlurredEdgeTreatment = BlurredEdgeTreatment.Unbounded,
    fade: Boolean = true,
): Modifier {
    require(maxRadius.value.isFinite() && maxRadius >= 0.dp)
    val supported = remember { BlurEffect(1f, 1f).isSupported() }
    val enabled = BlurAnimationEnabled && supported
    return graphicsLayer {
        val fraction = progress().coerceIn(0f, 1f)
        alpha = if (fade) fraction else 1f
        val radius = if (enabled) (1f - fraction) * maxRadius.toPx() else 0f
        renderEffect =
            if (radius > 0f) {
                BlurEffect(
                    radius,
                    radius,
                    if (edgeTreatment.shape == null) TileMode.Decal else TileMode.Clamp,
                )
            } else null
        shape = edgeTreatment.shape ?: RectangleShape
        clip = edgeTreatment.shape != null
    }
}

/**
 * Registers blur and fading with the context visibility transition so exit content stays composed
 * until the animation completes. Set [fade] to false when an enclosing transition owns opacity.
 */
@Composable
context(scope: AnimatedVisibilityScope)
public fun Modifier.blurEnterExit(
    maxRadius: Dp = DefaultEnterExitBlurRadius,
    animationSpec: FiniteAnimationSpec<Float>,
    edgeTreatment: BlurredEdgeTreatment = BlurredEdgeTreatment.Unbounded,
    fade: Boolean = true,
): Modifier {
    val progress =
        scope.transition.animateFloat(
            transitionSpec = { animationSpec },
            label = "BlurVisibility",
        ) {
            if (it == EnterExitState.Visible) 1f else 0f
        }
    return blurTransition({ progress.value }, maxRadius, edgeTreatment, fade)
}

/**
 * Wraps content in the optical layer supplied by [blurEnterExit].
 *
 * Spacing that collapses with content belongs in its padding: a parent arrangement reserves its gap
 * until the exiting element leaves composition.
 */
@Composable
public fun AnimatedVisibilityScope.BlurEnterExit(
    modifier: Modifier = Modifier,
    maxRadius: Dp = DefaultEnterExitBlurRadius,
    edgeTreatment: BlurredEdgeTreatment = BlurredEdgeTreatment.Unbounded,
    animationSpec: FiniteAnimationSpec<Float>,
    fade: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.blurEnterExit(maxRadius, animationSpec, edgeTreatment, fade), content = content)
}

/** Animates visibility with caller-owned spatial transitions and foreground optics. */
@Composable
context(transition: Transition<Boolean>)
public fun AnimatedVisibilityWithBlur(
    animationSpec: FiniteAnimationSpec<Float>,
    modifier: Modifier = Modifier,
    maxRadius: Dp = DefaultEnterExitBlurRadius,
    edgeTreatment: BlurredEdgeTreatment = BlurredEdgeTreatment.Unbounded,
    enter: EnterTransition = EnterTransition.None,
    exit: ExitTransition = ExitTransition.None,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    val spatial = SpatialMotionEnabled
    transition.AnimatedVisibility(
        visible = { it },
        enter = if (spatial) enter else EnterTransition.None,
        exit = if (spatial) exit else ExitTransition.None,
        modifier = modifier,
    ) {
        BlurEnterExit(
            animationSpec = animationSpec,
            maxRadius = maxRadius,
            edgeTreatment = edgeTreatment,
        ) {
            content()
        }
    }
}

/** Animates visibility with foreground blur and fading. */
@Composable
public fun AnimatedVisibilityWithBlur(
    visible: Boolean,
    animationSpec: FiniteAnimationSpec<Float>,
    modifier: Modifier = Modifier,
    maxRadius: Dp = DefaultEnterExitBlurRadius,
    edgeTreatment: BlurredEdgeTreatment = BlurredEdgeTreatment.Unbounded,
    enter: EnterTransition = EnterTransition.None,
    exit: ExitTransition = ExitTransition.None,
    label: String = "AnimatedVisibilityWithBlur",
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    val spatial = SpatialMotionEnabled
    AnimatedVisibility(
        visible = visible,
        enter = if (spatial) enter else EnterTransition.None,
        exit = if (spatial) exit else ExitTransition.None,
        modifier = modifier,
        label = label,
    ) {
        BlurEnterExit(
            animationSpec = animationSpec,
            maxRadius = maxRadius,
            edgeTreatment = edgeTreatment,
        ) {
            content()
        }
    }
}

/** Replaces content using explicit optical timing and caller-owned spatial motion. */
@Composable
public fun <S> AnimatedContentWithBlur(
    targetState: S,
    animationSpec: FiniteAnimationSpec<Float>,
    modifier: Modifier = Modifier,
    maxRadius: Dp = DefaultEnterExitBlurRadius,
    edgeTreatment: BlurredEdgeTreatment = BlurredEdgeTreatment.Unbounded,
    transitionSpec: AnimatedContentTransitionScope<S>.() -> ContentTransform = {
        ContentTransform(EnterTransition.None, ExitTransition.None, sizeTransform = null)
    },
    contentAlignment: Alignment = Alignment.TopStart,
    label: String = "AnimatedContentWithBlur",
    contentKey: (targetState: S) -> Any? = { it },
    content: @Composable AnimatedContentScope.(targetState: S) -> Unit,
) {
    val spatial = SpatialMotionEnabled
    AnimatedContent(
        targetState = targetState,
        label = label,
        modifier = modifier,
        transitionSpec = { if (spatial) transitionSpec() else EmptyContentTransform },
        contentAlignment = contentAlignment,
        contentKey = contentKey,
    ) { state ->
        BlurEnterExit(
            animationSpec = animationSpec,
            maxRadius = maxRadius,
            edgeTreatment = edgeTreatment,
        ) {
            content(state)
        }
    }
}
