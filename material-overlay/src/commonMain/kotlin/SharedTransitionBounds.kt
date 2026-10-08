package top.ltfan.m3x.material.overlay

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import top.ltfan.m3x.compose.material3.sharedBoundsSpec
import top.ltfan.m3x.compose.ui.accessibility.SpatialMotionEnabled
import top.ltfan.m3x.overlay.LocalOverlayAnimatedVisibilityScope
import top.ltfan.m3x.overlay.LocalSharedTransitionScope

/**
 * Marks [this] as the overlay side of the shared bounds identified by [sharedContentState], so an
 * overlay component grows out of the element that is marked with the same state on the page.
 *
 * The bounds are clipped to [shape] while they are drawn in the transition overlay. Without the
 * clip, a panel whose content cannot shrink to the animated bounds would draw at its full size on
 * the first frame.
 *
 * Shared bounds own geometry and container crossfading. Apply foreground blur inside the container
 * with fading disabled so non-shared content inherits its opacity exactly once. Individually shared
 * descendants keep their own overlay layers.
 *
 * When no shared transition scope is provided, or [sharedContentState] is `null`, the modifier is a
 * no-op, so a component keeps its plain appearance in previews and standalone use.
 *
 * @param sharedContentState the state shared with the page side, created with
 *   `rememberSharedContentState`
 * @param shape the shape the bounds are clipped to while they are drawn in the overlay
 * @param resizeMode how the overlayed content follows the animated bounds
 * @param renderInOverlayDuringTransition whether the bounds are drawn in the transition overlay
 *   while a shared animation is running. The overlay draws above every overlay entry, so pass
 *   `false` when the bounds should keep the order of the entry they are composed in, for example a
 *   menu whose button is lifted into an entry above the panel.
 */
@Composable
public fun Modifier.overlaySharedBounds(
    sharedContentState: SharedContentState?,
    shape: Shape = RectangleShape,
    resizeMode: ResizeMode = ResizeMode.RemeasureToBounds,
    renderInOverlayDuringTransition: Boolean = true,
): Modifier {
    if (sharedContentState == null || !SpatialMotionEnabled) return this
    val motionScheme = MaterialTheme.motionScheme
    val sharedTransitionScope = LocalSharedTransitionScope.current ?: return this
    val animatedVisibilityScope = LocalOverlayAnimatedVisibilityScope.current ?: return this
    with(sharedTransitionScope) {
        val bounds =
            this@overlaySharedBounds.sharedBounds(
                boundsTransform = { _, _ -> motionScheme.sharedBoundsSpec() },
                enter = fadeIn(MaterialTheme.motionScheme.slowEffectsSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.slowEffectsSpec()),
                sharedContentState = sharedContentState,
                animatedVisibilityScope = animatedVisibilityScope,
                resizeMode = resizeMode,
                renderInOverlayDuringTransition = renderInOverlayDuringTransition,
                clipInOverlayDuringTransition = OverlayClip(shape),
            )
        return bounds
    }
}

/**
 * Marks [this] as the page side of the shared bounds identified by [sharedContentState] for content
 * that already runs inside an [AnimatedVisibilityScope] of its own, such as the content of an
 * `AnimatedContent` or an `AnimatedVisibility`.
 *
 * Use [SharedTransitionAnchor] instead when the element the overlay grows out of is not part of an
 * animation of its own.
 *
 * The [animatedVisibilityScope] drives when this endpoint takes part in the animation: while the
 * scope is becoming visible this endpoint provides the target bounds, and while it is leaving it
 * provides the bounds the animation starts from. The `enter` and `exit` animations of that scope
 * must not be `EnterTransition.None` and `ExitTransition.None` unless the overlay side runs a
 * transition of its own, because a shared animation recognizes the leaving endpoint through its
 * running transition.
 *
 * @param sharedContentState the state shared with the overlay component
 * @param animatedVisibilityScope the visibility of this endpoint
 * @param shape the shape the bounds are clipped to while they are drawn in the overlay
 * @param resizeMode how the overlayed content follows the animated bounds
 */
@Composable
public fun Modifier.sharedTransitionBounds(
    sharedContentState: SharedContentState,
    animatedVisibilityScope: AnimatedVisibilityScope,
    shape: Shape = RectangleShape,
    resizeMode: ResizeMode = ResizeMode.RemeasureToBounds,
): Modifier {
    if (!SpatialMotionEnabled) return this
    val motionScheme = MaterialTheme.motionScheme
    val sharedTransitionScope = LocalSharedTransitionScope.current ?: return this
    with(sharedTransitionScope) {
        return this@sharedTransitionBounds.sharedBounds(
            boundsTransform = { _, _ -> motionScheme.sharedBoundsSpec() },
            enter = fadeIn(MaterialTheme.motionScheme.slowEffectsSpec()),
            exit = fadeOut(MaterialTheme.motionScheme.slowEffectsSpec()),
            sharedContentState = sharedContentState,
            animatedVisibilityScope = animatedVisibilityScope,
            resizeMode = resizeMode,
            clipInOverlayDuringTransition = OverlayClip(shape),
        )
    }
}
