/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package top.ltfan.m3x.material.overlay

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.jvm.JvmName
import kotlinx.coroutines.CancellationException
import top.ltfan.m3x.material.overlay.internal.AnchoredDraggableState
import top.ltfan.m3x.material.overlay.internal.SheetOffsetTimeline
import top.ltfan.m3x.material.overlay.internal.snapTo

/**
 * State of a Material overlay bottom sheet.
 *
 * Contains states relating to its swipe position as well as animations between state values.
 *
 * @param skipPartiallyExpanded whether the partially expanded state, if the sheet is large enough,
 *   should be skipped. If true, the sheet will always expand to the
 *   [OverlayBottomSheetValue.Expanded] state and move to the [OverlayBottomSheetValue.Hidden] state
 *   if available when hiding the sheet, either programmatically or by user interaction.
 * @param positionalThreshold the positional threshold, in px, to be used when calculating the
 *   target state while a drag is in progress and when settling after the drag ends. This is the
 *   distance from the start of a transition. It will be, depending on the direction of the
 *   interaction, added or subtracted from/to the origin offset. It should always be a positive
 *   value.
 * @param velocityThreshold the velocity threshold (in px per second) that the end velocity has to
 *   exceed in order to animate to the next state, even if the [positionalThreshold] has not been
 *   reached.
 * @param initialValue the initial value of the state.
 * @param confirmValueChange optional callback invoked to confirm or veto a pending state change.
 * @param skipHiddenState whether the hidden state should be skipped. If true, the sheet will always
 *   expand to the [OverlayBottomSheetValue.Expanded] state and move to the
 *   [OverlayBottomSheetValue.PartiallyExpanded] if available, either programmatically or by user
 *   interaction.
 */
@Stable
public class OverlayBottomSheetState
internal constructor(
    internal val skipPartiallyExpanded: Boolean,
    positionalThreshold: () -> Float,
    velocityThreshold: () -> Float,
    initialValue: OverlayBottomSheetValue = OverlayBottomSheetValue.Hidden,
    internal val confirmValueChange: (OverlayBottomSheetValue) -> Boolean = { true },
    internal val skipHiddenState: Boolean = false,
) {
    init {
        if (skipPartiallyExpanded) {
            require(initialValue != OverlayBottomSheetValue.PartiallyExpanded) {
                "The initial value must not be set to PartiallyExpanded if skipPartiallyExpanded is set to true."
            }
        }
        if (skipHiddenState) {
            require(initialValue != OverlayBottomSheetValue.Hidden) {
                "The initial value must not be set to Hidden if skipHiddenState is set to true."
            }
        }
    }

    /**
     * The current value of the state.
     *
     * If no swipe or animation is in progress, this corresponds to the state the bottom sheet is
     * currently in. If a swipe or an animation is in progress, this corresponds the state the sheet
     * was in before the swipe or animation started.
     */
    public val currentValue: OverlayBottomSheetValue
        get() = anchoredDraggableState.currentValue

    /**
     * The target value of the bottom sheet state.
     *
     * If a swipe is in progress, this is the value that the sheet would animate to if the swipe
     * finishes. If an animation is running, this is the target value of that animation. Finally, if
     * no swipe or animation is in progress, this is the same as the [currentValue].
     */
    public val targetValue: OverlayBottomSheetValue
        get() = anchoredDraggableState.targetValue

    /** Whether the modal bottom sheet is visible. */
    public val isVisible: Boolean
        get() = anchoredDraggableState.currentValue != OverlayBottomSheetValue.Hidden

    /**
     * Whether an expanding or collapsing sheet animation is currently in progress.
     *
     * See [expand], [partialExpand], [show] or [hide] for more information.
     */
    public val isAnimationRunning: Boolean
        get() = anchoredDraggableState.isAnimationRunning

    /**
     * Require the current offset (in pixels) of the bottom sheet.
     *
     * The offset will be initialized during the first measurement phase of the provided sheet
     * content.
     *
     * During the first composition, an [IllegalStateException] is thrown. In subsequent
     * compositions, the offset will be derived from the anchors of the previous pass. Always prefer
     * accessing the offset from a LaunchedEffect as it will be scheduled to be executed the next
     * frame, after layout.
     *
     * @throws IllegalStateException If the offset has not been initialized yet
     */
    public fun requireOffset(): Float = anchoredDraggableState.requireOffset()

    /** Position of a measured state in pixels, or NaN before that anchor is available. */
    public fun anchorOffset(value: OverlayBottomSheetValue): Float =
        anchoredDraggableState.anchors.positionOf(value)

    /** Whether a predictive Back session currently owns the sheet's preview target. */
    public val isBackGestureInProgress: Boolean
        get() = backTarget != null

    /** Whether the sheet has an expanded state defined. */
    public val hasExpandedState: Boolean
        get() = anchoredDraggableState.anchors.hasAnchorFor(OverlayBottomSheetValue.Expanded)

    /** Whether the modal bottom sheet has a partially expanded state defined. */
    public val hasPartiallyExpandedState: Boolean
        get() =
            anchoredDraggableState.anchors.hasAnchorFor(OverlayBottomSheetValue.PartiallyExpanded)

    /**
     * If [confirmValueChange] returns true, fully expand the bottom sheet with animation and
     * suspend until it is fully expanded or animation has been canceled.
     *
     * @throws CancellationException if the animation is interrupted
     */
    public suspend fun expand() {
        if (confirmValueChange(OverlayBottomSheetValue.Expanded)) {
            animateTo(OverlayBottomSheetValue.Expanded, showMotionSpec)
        }
    }

    /**
     * If [confirmValueChange] returns true, animate the bottom sheet and suspend until it is
     * partially expanded or animation has been canceled.
     *
     * @throws CancellationException if the animation is interrupted
     * @throws IllegalStateException if [skipPartiallyExpanded] is set to true
     */
    public suspend fun partialExpand() {
        check(!skipPartiallyExpanded) {
            "Attempted to animate to partial expanded when skipPartiallyExpanded was enabled. Set skipPartiallyExpanded to false to use this function."
        }
        if (confirmValueChange(OverlayBottomSheetValue.PartiallyExpanded)) {
            animateTo(OverlayBottomSheetValue.PartiallyExpanded, hideMotionSpec)
        }
    }

    /**
     * If [confirmValueChange] returns true, expand the bottom sheet with animation and suspend
     * until it is [OverlayBottomSheetValue.PartiallyExpanded] if defined, else
     * [OverlayBottomSheetValue.Expanded].
     *
     * @throws CancellationException if the animation is interrupted
     */
    public suspend fun show() {
        val targetValue =
            when {
                hasPartiallyExpandedState -> OverlayBottomSheetValue.PartiallyExpanded
                else -> OverlayBottomSheetValue.Expanded
            }
        if (confirmValueChange(targetValue)) animateTo(targetValue, showMotionSpec)
    }

    /**
     * If [confirmValueChange] returns true, hide the bottom sheet with animation and suspend until
     * it is fully hidden or animation has been canceled.
     *
     * @throws CancellationException if the animation is interrupted
     */
    public suspend fun hide() {
        check(!skipHiddenState) {
            "Attempted to animate to hidden when skipHiddenState was enabled. Set skipHiddenState to false to use this function."
        }
        if (confirmValueChange(OverlayBottomSheetValue.Hidden)) {
            animateTo(OverlayBottomSheetValue.Hidden, hideMotionSpec)
        }
    }

    /**
     * Moves to hidden when the composable that owns the sheet has already been removed.
     *
     * Owner removal forces the sheet into its closed state. This bypasses [confirmValueChange] so a
     * dismissal veto cannot retain an ownerless sheet. The sheet state, overlay lifecycle, and
     * shared endpoint then converge on the same hidden state.
     */
    internal suspend fun hideForOwnerExit() {
        animateTo(OverlayBottomSheetValue.Hidden, hideMotionSpec)
    }

    /**
     * Animate to a [targetValue]. If the [targetValue] is not in the set of anchors, the
     * [currentValue] will be updated to the [targetValue] without updating the offset.
     *
     * @param targetValue the target value of the animation
     * @param animationSpec an [FiniteAnimationSpec]
     * @throws CancellationException if the interaction interrupted by another interaction like a
     *   gesture interaction or another programmatic interaction like a [animateTo] or [snapTo]
     *   call.
     */
    internal suspend fun animateTo(
        targetValue: OverlayBottomSheetValue,
        animationSpec: FiniteAnimationSpec<Float>,
    ) {
        anchoredDraggableState.anchoredDrag(targetValue = targetValue) { anchors, latestTarget ->
            val targetOffset = anchors.positionOf(latestTarget)
            if (!targetOffset.isNaN()) {
                val prev = if (offset.isNaN()) 0f else offset
                offsetTimeline.prepare(prev, animationSpec)
                offsetTimeline.animateTo(targetOffset) { dragTo(it) }
            }
        }
    }

    /**
     * Snap to a [targetValue] without any animation.
     *
     * @param targetValue the target value of the animation
     * @throws CancellationException if the interaction interrupted by another interaction like a
     *   gesture interaction or another programmatic interaction like a [animateTo] or [snapTo]
     *   call.
     */
    internal suspend fun snapTo(targetValue: OverlayBottomSheetValue) {
        anchoredDraggableState.snapTo(targetValue)
    }

    /**
     * Find the closest anchor taking into account the velocity and settle at it with an animation.
     */
    internal suspend fun settle(
        velocity: Float,
        onTargetSelected: (OverlayBottomSheetValue) -> Unit = {},
    ) {
        anchoredDraggableState.settle(velocity, onTargetSelected) { target ->
            animateTo(target, anchoredDraggableMotionSpec)
        }
    }

    internal val offsetTimeline = SheetOffsetTimeline()

    internal var anchoredDraggableMotionSpec: FiniteAnimationSpec<Float> = snap()

    internal var anchoredDraggableState =
        AnchoredDraggableState(
            initialValue = initialValue,
            animationSpec = { anchoredDraggableMotionSpec },
            confirmValueChange = confirmValueChange,
            positionalThreshold = { positionalThreshold() },
            velocityThreshold = velocityThreshold,
        )

    /**
     * The anchor a running back gesture commits to, or `null` while no gesture moves the sheet.
     *
     * The gesture moves [offset] like a drag does, so the sheet has one owner of its position and
     * one settle animation. This target tells the panel and its dim where the movement is heading.
     */
    internal var backTarget: OverlayBottomSheetValue? by mutableStateOf(null)

    /**
     * The fraction of its dismissal the sheet currently previews, or `null` while no gesture moves
     * the sheet.
     *
     * A gesture publishes every frame it reaches here, including the frames of the playback of a
     * canceled gesture, so this reads what the sheet shows at any moment.
     */
    internal var backPreview: Float? by mutableStateOf(null)

    internal val offset: Float
        get() = anchoredDraggableState.offset

    /** Whether a pointer is directly dragging the sheet, before its target is committed. */
    internal val isUserDragging = mutableStateOf(false)

    /**
     * The value a released drag is settling to, or `null` while no released drag is settling.
     *
     * [targetValue] is only the anchor that would win if the pointer were released now, so it stays
     * speculative until a release commits the settle. This is the committed target of that settle.
     */
    internal val releaseTarget = mutableStateOf<OverlayBottomSheetValue?>(null)

    /**
     * Whether the user has let go and the sheet is settling after a drag, until the settle ends.
     */
    internal val released = mutableStateOf(false)

    /**
     * Whether the owner of the sheet is being removed.
     *
     * The host keeps the entry of such a sheet visible until [hideForOwnerExit] has converged, so a
     * page-side shared endpoint must not take over before the entry leaves.
     */
    internal val ownerExitPending = mutableStateOf(false)

    internal var showMotionSpec: FiniteAnimationSpec<Float> = snap()

    internal var hideMotionSpec: FiniteAnimationSpec<Float> = snap()

    public companion object {
        /** The default [Saver] implementation for [OverlayBottomSheetState]. */
        public fun Saver(
            skipPartiallyExpanded: Boolean,
            positionalThreshold: () -> Float,
            velocityThreshold: () -> Float,
            confirmValueChange: (OverlayBottomSheetValue) -> Boolean,
            skipHiddenState: Boolean,
        ): Saver<OverlayBottomSheetState, OverlayBottomSheetValue> =
            Saver(
                save = { it.currentValue },
                restore = { savedValue ->
                    OverlayBottomSheetState(
                        skipPartiallyExpanded,
                        positionalThreshold,
                        velocityThreshold,
                        savedValue,
                        confirmValueChange,
                        skipHiddenState,
                    )
                },
            )

        /**
         * The default [Saver] implementation for [OverlayBottomSheetState], using a fixed
         * [density].
         */
        public fun Saver(
            skipPartiallyExpanded: Boolean,
            confirmValueChange: (OverlayBottomSheetValue) -> Boolean,
            density: Density,
            skipHiddenState: Boolean,
        ): Saver<OverlayBottomSheetState, OverlayBottomSheetValue> =
            Saver(
                skipPartiallyExpanded = skipPartiallyExpanded,
                confirmValueChange = confirmValueChange,
                skipHiddenState = skipHiddenState,
                positionalThreshold = {
                    with(density) { OverlayBottomSheetDefaults.PositionalThreshold.toPx() }
                },
                velocityThreshold = {
                    with(density) { OverlayBottomSheetDefaults.VelocityThreshold.toPx() }
                },
            )
    }

    /** Creates a [OverlayBottomSheetState] with the given [density] for its thresholds. */
    public constructor(
        skipPartiallyExpanded: Boolean,
        density: Density,
        initialValue: OverlayBottomSheetValue = OverlayBottomSheetValue.Hidden,
        confirmValueChange: (OverlayBottomSheetValue) -> Boolean = { true },
        skipHiddenState: Boolean = false,
    ) : this(
        skipPartiallyExpanded = skipPartiallyExpanded,
        positionalThreshold = {
            with(density) { OverlayBottomSheetDefaults.PositionalThreshold.toPx() }
        },
        velocityThreshold = {
            with(density) { OverlayBottomSheetDefaults.VelocityThreshold.toPx() }
        },
        initialValue = initialValue,
        confirmValueChange = confirmValueChange,
        skipHiddenState = skipHiddenState,
    )
}

/** Possible values of [OverlayBottomSheetState]. */
public enum class OverlayBottomSheetValue {
    /** The sheet is not visible. */
    Hidden,

    /** The sheet is visible at full height. */
    Expanded,

    /** The sheet is partially visible. */
    PartiallyExpanded,
}

/** Contains the default values used by [OverlayBottomSheet]. */
@Stable
@OptIn(ExperimentalMaterial3Api::class)
public object OverlayBottomSheetDefaults {
    /** The default max width used by [OverlayBottomSheet]. */
    public val SheetMaxWidth: Dp
        inline get() = BottomSheetDefaults.SheetMaxWidth

    /** Default insets to be used and consumed by the [OverlayBottomSheet]'s content. */
    public val windowInsets: WindowInsets
        @Composable inline get() = BottomSheetDefaults.modalWindowInsets

    internal val PositionalThreshold = 56.dp

    internal val VelocityThreshold = 125.dp
}

/** Creates and remembers a [OverlayBottomSheetState] for a modal bottom sheet. */
@Composable
public fun rememberOverlayBottomSheetState(
    skipPartiallyExpanded: Boolean = false,
    confirmValueChange: (OverlayBottomSheetValue) -> Boolean = { true },
): OverlayBottomSheetState =
    rememberOverlayBottomSheetStateInternal(
        skipPartiallyExpanded = skipPartiallyExpanded,
        confirmValueChange = confirmValueChange,
        initialValue = OverlayBottomSheetValue.Hidden,
    )

/** Creates and remembers a [OverlayBottomSheetState] with the given [initialValue]. */
@Composable
internal fun rememberOverlayBottomSheetStateInternal(
    skipPartiallyExpanded: Boolean = false,
    confirmValueChange: (OverlayBottomSheetValue) -> Boolean = { true },
    initialValue: OverlayBottomSheetValue = OverlayBottomSheetValue.Hidden,
    skipHiddenState: Boolean = false,
    positionalThreshold: Dp = OverlayBottomSheetDefaults.PositionalThreshold,
    velocityThreshold: Dp = OverlayBottomSheetDefaults.VelocityThreshold,
): OverlayBottomSheetState {
    val density = LocalDensity.current
    val positionalThresholdToPx = { with(density) { positionalThreshold.toPx() } }
    val velocityThresholdToPx = { with(density) { velocityThreshold.toPx() } }
    return rememberSaveable(
        skipPartiallyExpanded,
        confirmValueChange,
        skipHiddenState,
        saver =
            OverlayBottomSheetState.Saver(
                skipPartiallyExpanded = skipPartiallyExpanded,
                positionalThreshold = positionalThresholdToPx,
                velocityThreshold = velocityThresholdToPx,
                confirmValueChange = confirmValueChange,
                skipHiddenState = skipHiddenState,
            ),
    ) {
        OverlayBottomSheetState(
            skipPartiallyExpanded,
            positionalThresholdToPx,
            velocityThresholdToPx,
            initialValue,
            confirmValueChange,
            skipHiddenState,
        )
    }
}

/**
 * A [NestedScrollConnection] that routes the swipe gestures within the bounds of the bottom sheet
 * to its [OverlayBottomSheetState], so the sheet follows a scrollable content that has reached its
 * edge.
 */
@Suppress("FunctionName")
internal fun ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
    sheetState: OverlayBottomSheetState,
    orientation: Orientation,
    onFling: (velocity: Float) -> Unit,
): NestedScrollConnection =
    object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.toFloat()
            return if (delta < 0 && source == NestedScrollSource.UserInput) {
                sheetState.anchoredDraggableState.dispatchRawDelta(delta).toOffset()
            } else {
                Offset.Zero
            }
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            return if (source == NestedScrollSource.UserInput) {
                sheetState.anchoredDraggableState.dispatchRawDelta(available.toFloat()).toOffset()
            } else {
                Offset.Zero
            }
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            val toFling = available.toFloat()
            val currentOffset = sheetState.requireOffset()
            val minAnchor = sheetState.anchoredDraggableState.anchors.minAnchor()
            return if (toFling < 0 && currentOffset > minAnchor) {
                onFling(toFling)
                // Since we go to the anchor with tween settling, consume all for the best UX.
                available
            } else {
                Velocity.Zero
            }
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            onFling(available.toFloat())
            return available
        }

        private fun Float.toOffset(): Offset =
            Offset(
                x = if (orientation == Orientation.Horizontal) this else 0f,
                y = if (orientation == Orientation.Vertical) this else 0f,
            )

        @JvmName("velocityToFloat")
        private fun Velocity.toFloat() = if (orientation == Orientation.Horizontal) x else y

        @JvmName("offsetToFloat")
        private fun Offset.toFloat(): Float = if (orientation == Orientation.Horizontal) x else y
    }
