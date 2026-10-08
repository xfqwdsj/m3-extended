package top.ltfan.m3x.material.overlay

import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import top.ltfan.m3x.compose.animation.blurEnterExit
import top.ltfan.m3x.compose.material3.sharedBoundsSpec
import top.ltfan.m3x.compose.ui.accessibility.SpatialMotionEnabled
import top.ltfan.m3x.material.theme.extended
import top.ltfan.m3x.overlay.LocalOverlayHost
import top.ltfan.m3x.overlay.LocalSharedTransitionScope

/**
 * The page side of a shared animation with an overlay component, for an element that is not part of
 * an animation of its own, such as a button, an icon button or a settings value.
 *
 * Use [BottomSheetSharedAnchor] for an [OverlayBottomSheet]. A sheet's
 * [OverlayBottomSheetState.targetValue] is speculative while it is being dragged, so using it
 * directly as [visible] can start the shared transition before the pointer is released.
 *
 * [visible] is the opposite of the overlay's visibility: pass `!panelVisible`, so this endpoint is
 * present while the overlay is closed. The bounds animation then runs between this endpoint and the
 * overlay panel.
 *
 * The endpoint is only a reference for those bounds: it is drawn in the overlay while the
 * transition moves it and in place otherwise, and the element it stands for keeps drawing where it
 * is.
 *
 * The endpoint needs the size of the element it stands for, enforced inside [content] with
 * `Modifier.requiredSize`, for example `content = { Spacer(Modifier.requiredSize(anchorSize)) }`.
 * The bounds animation measures the endpoint against the animated bounds. These bounds override
 * `Modifier.size` and can inflate both the endpoint and its parent box. This moves the element the
 * endpoint stands for.
 *
 * Pass a visual [content] when the endpoint should draw something of its own, for example a copy of
 * the element that should cover it while the overlay is closed. An endpoint is measured against the
 * animated bounds, so it must be simple enough to be laid out at the bounds of the other side.
 *
 * Once bound to a hosted panel, the endpoint derives inverse visibility from that panel's
 * transition. Both endpoints keep the same animation root through ordinary changes, predictive
 * back, and interrupted exits.
 *
 * @param sharedContentState the state shared with the overlay component
 * @param visible fallback visibility before a hosted transition is available or when spatial motion
 *   is disabled, usually `!panelVisible`
 * @param modifier the [Modifier] that gives the endpoint the size and the shape of the element it
 *   stands for
 * @param shape the shape the bounds are clipped to while they are drawn in the overlay
 * @param content the optional content of the endpoint, drawn while the endpoint is present
 */
@Composable
public fun SharedTransitionAnchor(
    sharedContentState: SharedContentState,
    visible: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    content: @Composable BoxScope.() -> Unit = {},
) {
    SharedTransitionAnchor(
        sharedContentState = sharedContentState,
        visible = visible,
        modifier = modifier,
        shape = shape,
        renderInOverlayDuringTransition = true,
        content = content,
    )
}

/** Controls whether an anchor draws in the shared overlay or in its parent. */
@Composable
internal fun SharedTransitionAnchor(
    sharedContentState: SharedContentState,
    visible: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    renderInOverlayDuringTransition: Boolean,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    if (sharedTransitionScope == null || !SpatialMotionEnabled) {
        if (visible) {
            Box(modifier = modifier, content = content)
        }
        return
    }

    val motionScheme = MaterialTheme.motionScheme
    val host = LocalOverlayHost.current
    val transition = host?.transitionFor(sharedContentState.key)
    if (transition == null) {
        if (visible) Box(modifier = modifier, content = content)
        return
    }
    transition.AnimatedVisibility(
        visible = { !it },
        enter = scaleIn(initialScale = 1f, animationSpec = motionScheme.slowEffectsSpec()),
        exit = scaleOut(targetScale = 1f, animationSpec = motionScheme.slowEffectsSpec()),
    ) {
        val animatedVisibilityScope = this
        with(sharedTransitionScope) {
            Box(
                modifier =
                    modifier.sharedBounds(
                        boundsTransform = { _, _ -> motionScheme.sharedBoundsSpec() },
                        enter = fadeIn(MaterialTheme.motionScheme.slowEffectsSpec()),
                        exit = fadeOut(MaterialTheme.motionScheme.slowEffectsSpec()),
                        sharedContentState = sharedContentState,
                        animatedVisibilityScope = animatedVisibilityScope,
                        resizeMode = ResizeMode.RemeasureToBounds,
                        renderInOverlayDuringTransition = renderInOverlayDuringTransition,
                        clipInOverlayDuringTransition = OverlayClip(shape),
                    ),
                content = {
                    Box(
                        Modifier.blurEnterExit(
                            maxRadius = MaterialTheme.extended.enterExitBlurRadius,
                            animationSpec = motionScheme.slowEffectsSpec(),
                            fade = false,
                        ),
                        content = content,
                    )
                },
            )
        }
    }
}

/**
 * The page-side shared endpoint of a [OverlayBottomSheet].
 *
 * The endpoint is the opposite side of the sheet's entry. A committed dismissal ends the entry in
 * the same frame. The endpoint then takes over, and the shared animation starts at the panel's
 * current position. During owner removal, the host keeps the entry visible until the sheet state
 * converges. The endpoint waits for the entry. This keeps the panel as the visible side until it
 * leaves.
 *
 * The speculative target selected while the pointer is still dragging is not a committed dismissal:
 * the endpoint stays hidden while the pointer is down.
 *
 * @param sharedContentState the state also passed to [OverlayBottomSheet]
 * @param sheetState the state also passed to [OverlayBottomSheet]
 * @param modifier the modifier that gives the endpoint the size of the element it represents
 * @param shape the shape used to clip the bounds in the transition overlay
 * @param content the optional visual content of the endpoint
 */
@Composable
public fun BottomSheetSharedAnchor(
    sharedContentState: SharedContentState,
    sheetState: OverlayBottomSheetState,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    content: @Composable BoxScope.() -> Unit = {},
) {
    SharedTransitionAnchor(
        sharedContentState = sharedContentState,
        // A committed dismissal ends the entry of the sheet in the same frame, so the endpoint
        // takes over then and a shared animation starts from the place the panel has on screen.
        // During owner removal, the host keeps the entry visible until the sheet state converges.
        // The endpoint waits for the entry. An earlier match would use the endpoint's bounds as the
        // animation's starting bounds and snap the panel to the endpoint.
        visible =
            !sheetState.isUserDragging.value &&
                sheetState.targetValue == OverlayBottomSheetValue.Hidden &&
                (sheetState.currentValue == OverlayBottomSheetValue.Hidden ||
                    !sheetState.ownerExitPending.value),
        modifier = modifier,
        shape = shape,
        content = content,
    )
}
