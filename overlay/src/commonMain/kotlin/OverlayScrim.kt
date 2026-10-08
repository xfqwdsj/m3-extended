package top.ltfan.m3x.overlay

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.areAnyPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.roundToIntRect

/** The pointer event an overlay reacts to when the pointer is outside its content. */
public enum class OverlayDismissTrigger {
    /** Dismiss as soon as the pointer goes down outside the content. */
    Press,

    /** Dismiss when the pointer goes up outside the content. */
    Release,
}

/**
 * Measured bounds of an overlay in window coordinates, used to tell pointer events inside the
 * interactive content from events on the scrim around it.
 */
public class OverlayBounds {
    /** Bounds of the overlay container, which covers the whole window. */
    public var container: IntRect = IntRect.Zero
        internal set

    /** Bounds of the interactive content drawn above the scrim. */
    public var content: IntRect = IntRect.Zero
        internal set

    /** Whether [positionInContainer] is inside [content]. */
    public fun containsContent(positionInContainer: Offset): Boolean {
        val x = positionInContainer.x + container.left
        val y = positionInContainer.y + container.top
        if (!x.isFinite() || !y.isFinite()) return false
        // The right and bottom edges of the content belong to the area outside it, like the touch
        // handling of the platform dialogs treats them.
        return x >= content.left && x < content.right && y >= content.top && y < content.bottom
    }
}

/** Records the bounds of this node as the container of [bounds]. */
public fun Modifier.overlayContainerBounds(bounds: OverlayBounds): Modifier = onGloballyPositioned {
    bounds.container = it.boundsInWindow().roundToIntRect()
}

/** Records the bounds of this node as the interactive content of [bounds]. */
public fun Modifier.overlayContentBounds(
    bounds: OverlayBounds,
    enabled: Boolean = true,
): Modifier = onGloballyPositioned {
    // An overlay that is not shown clears the bounds it recorded, so a later press is not
    // judged against the place the content had while it was leaving.
    bounds.content = if (enabled) it.boundsInWindow().roundToIntRect() else IntRect.Zero
}

/**
 * Reports gestures that start outside [OverlayBounds.content], following Android dialog touch
 * handling. An outside press starts outside the content. Dismissal requires its release to remain
 * outside. A press after a cancelled gesture starts a fresh gesture.
 *
 * @param consumePointerInputOutside whether gestures that started outside the content are consumed,
 *   which keeps them from reaching the content below
 */
internal fun Modifier.overlayPointerInput(
    bounds: OverlayBounds,
    consumePointerInputOutside: Boolean,
    dismissEnabled: Boolean,
    trigger: OverlayDismissTrigger,
    primaryButtonOnly: Boolean,
    onDismissRequest: () -> Unit,
): Modifier =
    pointerInput(
        bounds,
        consumePointerInputOutside,
        dismissEnabled,
        trigger,
        primaryButtonOnly,
    ) {
        awaitPointerEventScope {
            var startedOutside = false
            var gestureCanDismiss = false
            var triggered = false
            var pointerId: PointerId? = null

            while (true) {
                val event = awaitPointerEvent()
                when (event.type) {
                    PointerEventType.Press -> {
                        if (pointerId != null) {
                            // A second pointer cannot take the gesture over. Some hosts deliver no
                            // cancel event, so a new press after cancellation must also discard the
                            // stale pointer instead of inheriting its outside-press state.
                            val originalStillDown =
                                event.changes.any {
                                    it.id == pointerId && it.previousPressed && it.pressed
                                }
                            if (originalStillDown) {
                                if (consumePointerInputOutside && startedOutside) {
                                    event.changes.forEach { it.consume() }
                                }
                                continue
                            }
                            startedOutside = false
                            gestureCanDismiss = false
                            triggered = false
                            pointerId = null
                        }
                        val change = event.changes.firstOrNull() ?: continue
                        pointerId = change.id
                        triggered = false
                        startedOutside = !bounds.containsContent(change.position)
                        gestureCanDismiss =
                            startedOutside && (!primaryButtonOnly || isPrimaryButton(event))
                        if (consumePointerInputOutside && startedOutside) {
                            change.consume()
                        }
                        if (
                            dismissEnabled &&
                                gestureCanDismiss &&
                                trigger == OverlayDismissTrigger.Press
                        ) {
                            triggered = true
                            onDismissRequest()
                        }
                    }

                    PointerEventType.Move -> {
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: continue
                        if (consumePointerInputOutside && startedOutside) {
                            change.consume()
                        }
                    }

                    PointerEventType.Release -> {
                        val change = event.changes.firstOrNull { it.id == pointerId }
                        if (change == null) {
                            if (consumePointerInputOutside && startedOutside) {
                                event.changes.forEach { it.consume() }
                            }
                            continue
                        }
                        val insideContent = bounds.containsContent(change.position)
                        if (consumePointerInputOutside && startedOutside) {
                            change.consume()
                        }
                        if (
                            dismissEnabled &&
                                gestureCanDismiss &&
                                !insideContent &&
                                !triggered &&
                                trigger == OverlayDismissTrigger.Release
                        ) {
                            triggered = true
                            onDismissRequest()
                        }
                        startedOutside = false
                        gestureCanDismiss = false
                        pointerId = null
                    }

                    else -> {
                        if (consumePointerInputOutside) {
                            event.changes
                                .filter { !bounds.containsContent(it.position) }
                                .forEach { it.consume() }
                        }
                    }
                }
            }
        }
    }

/**
 * Whether [event] carries a primary button press: touch input has no pressed mouse button, so it is
 * accepted here too.
 */
private fun isPrimaryButton(event: PointerEvent): Boolean =
    !event.buttons.areAnyPressed || event.buttons.isPrimaryPressed
