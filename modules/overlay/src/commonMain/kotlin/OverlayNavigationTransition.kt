package top.ltfan.m3x.overlay

import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.createChildTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue

/**
 * Connects a caller-owned animation root to page content and hosted overlays. Each panel derives
 * visibility directly from that root.
 */
@Stable
public class OverlayNavigationTransition
internal constructor(
    internal val scrimVisibility: Transition<Boolean>,
    internal val visibility: @Composable (Any?) -> OverlayVisibilityTransition,
) {
    internal var entry: OverlayHandle? by mutableStateOf(null)
    internal var ownerDisposed: Boolean by mutableStateOf(false)
}

/**
 * Drives [state] exclusively from [targetState] and the bound overlay's back events.
 *
 * Callers update [targetState] instead of launching animations on [state]. [isVisible] maps a root
 * value and an overlay's shared content key to that overlay's visibility. Retained outgoing panels
 * keep their own mapping while the same root moves to another target.
 *
 * @param state the seekable root exclusively driven by this connection
 * @param transition the transition created from [state]
 * @param targetState the caller's semantic target, independent of gesture preview state
 * @param backTarget the semantic value reached when back closes the overlay
 * @param isVisible maps a root value and each panel's shared content key to its visibility
 */
@Composable
public fun <T> rememberOverlayNavigationTransition(
    state: SeekableTransitionState<T>,
    transition: Transition<T>,
    targetState: T,
    backTarget: T,
    isVisible: (T, Any?) -> Boolean = { value, _ -> value != backTarget },
): OverlayNavigationTransition {
    val currentMapping by rememberUpdatedState(isVisible)
    val scrimVisibility =
        transition.createChildTransition(label = "OverlayPresentation") { it != backTarget }
    val handle =
        remember(state, transition) {
            OverlayNavigationTransition(scrimVisibility) { key ->
                rememberOverlayVisibilityTransition(transition) {
                    currentMapping(it, key)
                }
            }
        }
    DisposableEffect(handle) {
        onDispose { handle.ownerDisposed = true }
    }
    val target =
        if (handle.entry?.let { it.detached && it.detachedWhileVisible } == true) backTarget
        else targetState
    OverlayTimeline(state, transition, target, backTarget, handle.entry)
    return handle
}
