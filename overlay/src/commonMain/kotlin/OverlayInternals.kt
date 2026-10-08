package top.ltfan.m3x.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

/**
 * Handles back gestures for the entries of an [OverlayHost].
 *
 * The handler uses the dispatcher's overlay priority, so overlays receive back gestures before the
 * page. It is enabled while [entry] is set. The page handles back gestures when no overlay accepts
 * them.
 *
 * A predictive back gesture is forwarded to the entry the same way as an instant one: the entry
 * receives the raw progress while the gesture is in progress, and it decides what completing or
 * cancelling it means.
 *
 * @param entry the topmost visible entry that accepts back gestures, or `null` when no visible
 *   entry accepts them
 */
@Composable
internal fun OverlayHostBackHandler(entry: OverlayHandle?) {
    val impl = remember { OverlayBackHandlerImpl() }
    // A host without a dispatcher, such as a preview, simply has no back gestures: the overlays of
    // the host keep working without them.
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
    SideEffect {
        impl.entry = entry
        impl.isBackEnabled = dispatcher != null && entry != null
    }
    if (dispatcher != null) {
        DisposableEffect(dispatcher, impl) {
            dispatcher.addHandler(impl, NavigationEventDispatcher.PRIORITY_OVERLAY)
            onDispose { impl.remove() }
        }
    }
}

private class OverlayBackHandlerImpl :
    NavigationEventHandler<NavigationEventInfo>(
        initialInfo = NavigationEventInfo.None,
        isBackEnabled = false,
    ) {
    var entry: OverlayHandle? = null

    /** The entry the running gesture belongs to, fixed when the gesture starts. */
    private var gestureEntry: OverlayHandle? = null
    private var gestureHandler: OverlayBackHandler? = null
    private var ignoredGesture = false

    override fun onBackStarted(event: NavigationEvent) {
        // An entry retains timeline ownership through preview and terminal playback. Additional
        // sequences are consumed until that controller releases it.
        ignoredGesture = entry?.backPhase != OverlayBackPhase.Idle
        if (ignoredGesture) return
        gestureEntry = entry
        gestureHandler = entry?.backHandler
        entry?.let {
            // The session change starts the timeline driver for this captured gesture.
            it.backSession++
            it.backPhase = OverlayBackPhase.Progress
        }
        onBackProgressed(event)
    }

    override fun onBackProgressed(event: NavigationEvent) {
        if (ignoredGesture) return
        val target = gestureEntry ?: return
        if (target.backPhase != OverlayBackPhase.Progress) return
        val progress = event.progress.coerceIn(0f, 1f)
        target.backProgress = progress
        gestureHandler?.onProgress?.invoke(progress)
    }

    override fun onBackCompleted() {
        if (ignoredGesture) {
            ignoredGesture = false
            return
        }
        val target = gestureEntry ?: entry
        val superseded = gestureEntry != null && target?.backPhase != OverlayBackPhase.Progress
        // Commit through the captured handler while its owner remains attached and the gesture
        // phase still owns the timeline. A newer semantic request resolves that phase.
        val handler = gestureHandler ?: target?.backHandler
        if (
            target != null &&
                !superseded &&
                (gestureEntry != null || target.backPhase == OverlayBackPhase.Idle)
        ) {
            if (!target.detached) handler?.onCommit?.invoke()
            target.backPhase = OverlayBackPhase.Completed
        }
        gestureEntry = null
        gestureHandler = null
    }

    override fun onBackCancelled() {
        if (ignoredGesture) {
            ignoredGesture = false
            return
        }
        gestureEntry
            ?.takeIf { it.backPhase == OverlayBackPhase.Progress }
            ?.let {
                gestureHandler?.onCancel?.invoke()
                it.backPhase = OverlayBackPhase.Cancelled
            }
        gestureEntry = null
        gestureHandler = null
    }
}

internal val LocalOverlayInputActive = compositionLocalOf { true }

/**
 * Remembers a [FocusRequester] that asks for focus once [enabled] and the node it is attached to
 * exist, so keyboard and accessibility input land on the overlay content instead of the content
 * below it.
 */
@Composable
public fun rememberOverlayFocusRequester(enabled: Boolean = true): FocusRequester {
    val requester = remember { FocusRequester() }
    val inputActive = LocalOverlayInputActive.current
    val sharedTransitionActive = LocalSharedTransitionScope.current?.isTransitionActive == true
    LaunchedEffect(requester, enabled, inputActive, sharedTransitionActive) {
        if (enabled && inputActive) {
            if (!requester.requestFocus()) {
                withFrameNanos {}
                requester.requestFocus()
            }
        }
    }
    return requester
}
