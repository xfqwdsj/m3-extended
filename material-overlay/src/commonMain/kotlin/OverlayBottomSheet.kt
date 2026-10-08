/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import top.ltfan.m3x.compose.animation.core.withDefaultVisibilityThreshold
import top.ltfan.m3x.compose.foundation.keepSizeWhenLookingAhead
import top.ltfan.m3x.compose.ui.accessibility.SpatialMotionEnabled
import top.ltfan.m3x.material.MaterialSurfaceEdge
import top.ltfan.m3x.material.MaterialSurfaceEffects
import top.ltfan.m3x.material.overlay.internal.AnchoredDraggableState
import top.ltfan.m3x.material.overlay.internal.DraggableAnchors
import top.ltfan.m3x.material.rememberMaterialSurfaceLayer
import top.ltfan.m3x.overlay.LocalSharedTransitionScope
import top.ltfan.m3x.overlay.OverlayBackHandler
import top.ltfan.m3x.overlay.OverlayBounds
import top.ltfan.m3x.overlay.OverlayDismissTrigger
import top.ltfan.m3x.overlay.OverlayHandle
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayInteraction
import top.ltfan.m3x.overlay.overlayContainerBounds
import top.ltfan.m3x.overlay.overlayContentBounds
import top.ltfan.m3x.overlay.rememberOverlayFocusRequester

/**
 * Properties used to customize the behavior of a [OverlayBottomSheet].
 *
 * @property shouldDismissOnBackPress whether the modal bottom sheet can be dismissed by pressing
 *   the back button. If true, pressing the back button will call onDismissRequest.
 * @property shouldDismissOnClickOutside whether a touch tap or primary mouse click on the scrim
 *   requests dismissal on release; the sheet still blocks input to content behind it when this is
 *   false.
 */
@Immutable
public class OverlayBottomSheetProperties(
    public val shouldDismissOnBackPress: Boolean = true,
    public val shouldDismissOnClickOutside: Boolean = true,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OverlayBottomSheetProperties) return false
        if (shouldDismissOnBackPress != other.shouldDismissOnBackPress) return false
        if (shouldDismissOnClickOutside != other.shouldDismissOnClickOutside) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shouldDismissOnBackPress.hashCode()
        result = 31 * result + shouldDismissOnClickOutside.hashCode()
        return result
    }
}

/**
 * Modal bottom sheet with a renderer-selected panel, hosted by the enclosing [OverlayHost].
 *
 * The sheet animates between its hidden, partially expanded and expanded states, follows drags and
 * flings, participates in the nested scrolling of its content, and scales itself down while a
 * predictive back gesture is in progress.
 *
 * Interactive use requires an enclosing [OverlayHost]. In inspection-mode previews without one, the
 * overlay is composed in place without host-managed ordering, modal isolation, back dispatch, or
 * backdrop stacking.
 *
 * @param onDismissRequest executes after the sheet has animated to [OverlayBottomSheetValue.Hidden]
 *   following an outside click, Back dismissal, accessibility Dismiss action, or dismissing drag.
 *   Outside clicks must start and end on the scrim; mouse clicks use the primary button only.
 * @param modifier the [Modifier] to be applied to the bottom sheet
 * @param sheetState the state of the bottom sheet
 * @param sheetMaxWidth the maximum width the sheet will take
 * @param sheetGesturesEnabled whether the bottom sheet can be interacted with by gestures
 * @param shape the shape of the bottom sheet
 * @param containerColor the color used for the background of this bottom sheet
 * @param contentColor the preferred color for content inside this bottom sheet
 * @param tonalElevation tonal overlay applied when [containerColor] is the theme surface color
 * @param scrimColor the color of the scrim that obscures content when the bottom sheet is open
 * @param dragHandle the optional visual marker to swipe the bottom sheet
 * @param contentWindowInsets the window insets to be passed to the bottom sheet content
 * @param properties the behavior of this bottom sheet
 * @param visible reads whether the sheet is shown, usually `visible = { sheetState.targetValue !=
 *   OverlayBottomSheetValue.Hidden }`. A sheet that shares its bounds follows this state, so the
 *   sheet and the element it grows out of change in the same frame in both directions. When it is
 *   `null` the sheet starts to hide as soon as its state commits to
 *   [OverlayBottomSheetValue.Hidden]
 * @param sharedContentState the state of the shared bounds between the sheet and the page element
 *   it grows out of, usually created with `rememberSharedContentState` and passed to
 *   [BottomSheetSharedAnchor]. When it is `null` the sheet keeps its own appearance animation.
 * @param effects optional renderer-specific effects applied to the sheet surface
 * @param content the content to be displayed inside the bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun OverlayBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: OverlayBottomSheetState = rememberOverlayBottomSheetState(),
    sheetMaxWidth: Dp = OverlayBottomSheetDefaults.SheetMaxWidth,
    sheetGesturesEnabled: Boolean = true,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { OverlayBottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = {
        OverlayBottomSheetDefaults.windowInsets
    },
    properties: OverlayBottomSheetProperties = OverlayBottomSheetProperties(),
    visible: (() -> Boolean)? = null,
    sharedContentState: SharedContentState? = null,
    effects: MaterialSurfaceEffects? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spatialMotion = SpatialMotionEnabled
    val effectiveSharedContentState = sharedContentState.takeIf { spatialMotion }
    val surfaceColor =
        if (containerColor == MaterialTheme.colorScheme.surface) {
            MaterialTheme.colorScheme.surfaceColorAtElevation(tonalElevation)
        } else {
            containerColor
        }
    val motionScheme = MaterialTheme.motionScheme
    val closeSheetLabel = OverlayStrings.closeSheet()
    val anchoredDraggableMotion =
        remember(motionScheme) {
            motionScheme.defaultSpatialSpec<Float>().withDefaultVisibilityThreshold(1f)
        }
    val hideMotion =
        remember(motionScheme) {
            motionScheme.fastEffectsSpec<Float>().withDefaultVisibilityThreshold(1f)
        }

    SideEffect {
        // A sheet that shares its bounds appears and disappears through the shared animation, like
        // the panel of a menu: the state moves to its target at once and the shared bounds carry
        // the visible motion. Without shared bounds the sheet slides with its own motion.
        val shared = effectiveSharedContentState != null
        sheetState.showMotionSpec =
            if (shared || !spatialMotion) snap() else anchoredDraggableMotion
        sheetState.hideMotionSpec = if (shared || !spatialMotion) snap() else hideMotion
        sheetState.anchoredDraggableMotionSpec =
            if (spatialMotion) anchoredDraggableMotion else hideMotion
    }

    val scope = rememberCoroutineScope()
    val entry = remember { OverlayHandle() }
    val bounds = remember { OverlayBounds() }
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)

    val animateToDismiss: () -> Unit = {
        if (sheetState.anchoredDraggableState.confirmValueChange(OverlayBottomSheetValue.Hidden)) {
            scope
                .launch { sheetState.hide() }
                .invokeOnCompletion {
                    if (!sheetState.isVisible) currentOnDismissRequest()
                }
        }
    }
    val settleToDismiss: (velocity: Float) -> Unit = {
        scope
            .launch {
                sheetState.settle(it) { target -> sheetState.releaseTarget.value = target }
            }
            .invokeOnCompletion {
                sheetState.releaseTarget.value = null
                sheetState.released.value = false
                if (!sheetState.isVisible) currentOnDismissRequest()
            }
    }

    // Ordinary sheets seek their native offset transition. Completion continues the same segment;
    // cancellation rewinds it. Shared sheets use the host appearance timeline for shared geometry.
    val partial =
        sheetState.currentValue == OverlayBottomSheetValue.Expanded &&
            sheetState.hasPartiallyExpandedState
    val backSeekTarget = !partial && (effectiveSharedContentState != null || !spatialMotion)
    val backGesture = remember(sheetState) { SheetBackGesture() }
    LaunchedEffect(sheetState, backGesture.session) {
        val session = backGesture.session
        val target = sheetState.backTarget
        if (session == 0 || target == null) return@LaunchedEffect
        val timeline =
            SheetBackTimeline(
                state = sheetState,
                target = target,
                startOffset = backGesture.startOffset,
                onDismiss = currentOnDismissRequest,
            )
        try {
            snapshotFlow { backGesture.progress to backGesture.result }
                .collectLatest { (progress, result) ->
                    when (result) {
                        // The gesture owns the position until it is committed or canceled.
                        null -> timeline.seek(progress)
                        SheetBackResult.Completed -> {
                            timeline.settleCommitted()
                            sheetState.backTarget = null
                            sheetState.backPreview = null
                        }
                        SheetBackResult.Cancelled -> {
                            timeline.settleRestored()
                            sheetState.backTarget = null
                            sheetState.backPreview = null
                        }
                    }
                }
        } finally {
            if (backGesture.session == session) {
                sheetState.backTarget = null
                sheetState.backPreview = null
            }
        }
    }
    val backHandler =
        OverlayBackHandler(
            enabled = { properties.shouldDismissOnBackPress },
            previewTarget = { if (backSeekTarget) false else null },
            onProgress = { progress ->
                if (!backSeekTarget) {
                    backGesture.progress = progress
                    // The first progress of a gesture takes over the position of the sheet and
                    // fixes the anchor it returns to for the rest of the gesture.
                    if (!backGesture.inProgress) {
                        val target =
                            if (partial) OverlayBottomSheetValue.PartiallyExpanded
                            else OverlayBottomSheetValue.Hidden
                        val start = sheetState.offset
                        val targetOffset =
                            sheetState.anchoredDraggableState.anchors.positionOf(target)
                        if (!start.isNaN() && !targetOffset.isNaN()) {
                            backGesture.startOffset = start
                            backGesture.result = null
                            sheetState.backTarget = target
                            backGesture.session++
                        }
                    }
                }
            },
            onCommit = {
                val previewed = backGesture.inProgress
                backGesture.result = SheetBackResult.Completed
                // A sheet that leaves the movement to the shared animation hides through the same
                // dismissal path as any other dismissal.
                if (backSeekTarget || !previewed) {
                    scope.launch {
                        if (partial) sheetState.partialExpand() else sheetState.hide()
                        if (!sheetState.isVisible) currentOnDismissRequest()
                    }
                }
            },
            onCancel = { backGesture.result = SheetBackResult.Cancelled },
        )

    // The sheet is shown from the moment it is composed, so its layout exists before the state
    // of the sheet has anchors. It starts to hide when the state of the sheet reaches the hidden
    // value, and a caller that drives the sheet from its own state passes that state in [visible].
    val hasShownOnce = remember { mutableStateOf(false) }
    if (sheetState.targetValue != OverlayBottomSheetValue.Hidden) hasShownOnce.value = true
    val sheetVisible: () -> Boolean =
        visible
            ?: {
                !hasShownOnce.value ||
                    sheetState.currentValue != OverlayBottomSheetValue.Hidden &&
                        // During a drag targetValue is only the anchor that would win if the
                        // pointer
                        // were released now. It must not start dismissal while the pointer is still
                        // down. onDragStopped commits that prediction.
                        (sheetState.isUserDragging.value ||
                            sheetState.targetValue != OverlayBottomSheetValue.Hidden)
            }

    MaterialOverlay(
        bindAnimation = { sheetState.offsetTimeline.Bind() },
        sharedContentState = effectiveSharedContentState,
        entry = entry,
        modal = true,
        // The entry follows the state that shows the sheet, like the panel of a menu: the
        // transition of the entry is what makes a shared animation recognize the panel as the
        // side that leaves when the sheet hides. While the sheet has never been shown the entry
        // stays visible, so the sheet can build its anchors and start to show.
        visible = {
            if (effectiveSharedContentState != null || !spatialMotion) sheetVisible()
            else {
                sheetVisible() ||
                    sheetState.isVisible ||
                    sheetState.targetValue != OverlayBottomSheetValue.Hidden
            }
        },
        // The sheet and its interception layer are one entry: the layer exists while the sheet is
        // not hidden, so a sheet that is kept composed while it is hidden does not cover the page.
        interaction =
            OverlayInteraction(
                bounds = bounds,
                // The layer lives with the content of the sheet, so the dim can fade out while the
                // sheet leaves; it only takes pointer input while the sheet is shown.
                active = { true },
                blocking = { sheetVisible() || sheetState.isVisible },
                dismissOnClickOutside = {
                    properties.shouldDismissOnClickOutside &&
                        (sheetVisible() || sheetState.isVisible)
                },
                trigger = OverlayDismissTrigger.Release,
                primaryButtonOnly = true,
                onDismissRequest = animateToDismiss,
                accessibleScrimLabel = closeSheetLabel,
                scrimColor = scrimColor,
                animateScrim = effectiveSharedContentState != null || !spatialMotion,
                scrimProgress =
                    if (effectiveSharedContentState == null && spatialMotion) {
                        {
                            val anchors = sheetState.anchoredDraggableState.anchors
                            val hiddenOffset = anchors.positionOf(OverlayBottomSheetValue.Hidden)
                            val shownValue =
                                if (sheetState.currentValue != OverlayBottomSheetValue.Hidden) {
                                    sheetState.currentValue
                                } else {
                                    sheetState.targetValue
                                }
                            val shownOffset = anchors.positionOf(shownValue)
                            val offset = sheetState.offset
                            if (
                                shownValue == OverlayBottomSheetValue.Hidden ||
                                    hiddenOffset.isNaN() ||
                                    shownOffset.isNaN() ||
                                    offset.isNaN()
                            ) {
                                0f
                            } else if (
                                // A back gesture returns the sheet along its own path, so its
                                // dim follows the sheet the way the dim of a committed dismissal
                                // does, and neither pops nor waits for the state to commit.
                                !backGesture.inProgress &&
                                    (sheetState.isUserDragging.value ||
                                        sheetState.currentValue != OverlayBottomSheetValue.Hidden &&
                                            sheetState.targetValue !=
                                                OverlayBottomSheetValue.Hidden)
                            ) {
                                1f
                            } else {
                                val distance = hiddenOffset - shownOffset
                                if (distance <= 0f) 0f
                                else ((hiddenOffset - offset) / distance).coerceIn(0f, 1f)
                            }
                        }
                    } else {
                        null
                    },
            ),
        // Shared bounds seek the host exit; ordinary sheets seek their offset specification.
        backHandler = backHandler,
        // Keep the shared panel visible until its bounds return to the anchor, without a second
        // fade.
        enter =
            if (spatialMotion) EnterTransition.None else fadeIn(motionScheme.defaultEffectsSpec()),
        exit =
            if (!spatialMotion) fadeOut(motionScheme.fastEffectsSpec())
            else if (effectiveSharedContentState != null) {
                scaleOut(targetScale = 1f, animationSpec = motionScheme.slowSpatialSpec())
            } else {
                ExitTransition.None
            },
        // Removing the composable is a dismissal path in its own right. Keep the hosted entry
        // alive until its state has reached Hidden, then let the host start the same entry/shared
        // exit used by gestures, back, scrim and semantics actions. Callers therefore do not need
        // to know that the sheet state has to change before they remove its owner.
        prepareForOwnerExit = {
            sheetState.ownerExitPending.value = true
            try {
                sheetState.hideForOwnerExit()
            } finally {
                sheetState.ownerExitPending.value = false
            }
        },
    ) {
        val focusRequester = rememberOverlayFocusRequester()

        Box(
            modifier =
                Modifier.fillMaxSize()
                    .imePadding()
                    .overlayContainerBounds(bounds)
                    .semantics { isTraversalGroup = true }
                    .focusRequester(focusRequester)
                    .focusable(),
        ) {
            val sheetContent: @Composable () -> Unit = {
                OverlayBottomSheetContent(
                    scope = scope,
                    animateToDismiss = animateToDismiss,
                    settleToDismiss = settleToDismiss,
                    modifier = modifier,
                    bounds = bounds,
                    sheetVisible = sheetVisible,
                    sharedContentState = effectiveSharedContentState,
                    sheetState = sheetState,
                    sheetMaxWidth = sheetMaxWidth,
                    sheetGesturesEnabled = sheetGesturesEnabled,
                    semanticsDismissEnabled =
                        sheetGesturesEnabled ||
                            properties.shouldDismissOnBackPress ||
                            properties.shouldDismissOnClickOutside,
                    shape = shape,
                    containerColor = surfaceColor,
                    contentColor = contentColor,
                    dragHandle = dragHandle,
                    contentWindowInsets = contentWindowInsets,
                    effects = effects,
                    content = content,
                )
            }
            if (effectiveSharedContentState != null || !spatialMotion) {
                // The entry retains shared geometry or a reduced-motion fade until completion.
                // The panel is composed inside the entry, like the panel of a menu: the transition
                // of the entry is what makes a shared animation recognize the panel as the side
                // that leaves when the sheet hides.
                sheetContent()
            } else if (sheetVisible() || sheetState.isVisible) {
                // The sheet and the layer that covers the page under it live together: a sheet
                // that is kept composed while it is hidden keeps no panel in the tree, and the
                // panel stays composed until a hide animation has finished.
                sheetContent()
            }
        }
    }
    val requestedVisible = visible?.invoke()
    LaunchedEffect(sheetState, requestedVisible) {
        if (requestedVisible != false) {
            snapshotFlow { sheetState.hasExpandedState }.first { it }
            sheetState.show()
        } else if (sheetState.isVisible) {
            sheetState.hide()
        }
    }
}

/** How a back gesture that moved a sheet ended. */
private enum class SheetBackResult {
    Completed,
    Cancelled,
}

/**
 * The back gesture that currently moves a sheet.
 *
 * [SheetBackTimeline] controls the anchored position through preview and terminal playback.
 * Completion advances the current offset segment; cancellation rewinds it.
 */
private class SheetBackGesture {
    /** Increments when a gesture takes over the sheet, which restarts the driver of the gesture. */
    var session by mutableIntStateOf(0)

    /** The progress of the running gesture, read by its driver. */
    var progress by mutableFloatStateOf(0f)

    /** The position the gesture started from, the beginning of the path it returns along. */
    var startOffset by mutableFloatStateOf(Float.NaN)

    /** How the running gesture ended, or `null` while it is still in progress. */
    var result: SheetBackResult? by mutableStateOf(null)

    /** Whether a back gesture currently moves the sheet. */
    val inProgress: Boolean
        get() = session > 0 && result == null
}

/**
 * The position of a sheet during a back gesture.
 *
 * The gesture seeks the native offset transition used by programmatic movement. Physical writes
 * take the drag mutex without committing the speculative anchor. Completion continues that
 * transition, and cancellation rewinds its existing segment.
 *
 * @param state the state of the sheet
 * @param target the anchor the gesture commits to
 * @param startOffset the position the gesture started from
 * @param onDismiss called when the sheet has become hidden
 */
private class SheetBackTimeline(
    private val state: OverlayBottomSheetState,
    private val target: OverlayBottomSheetValue,
    private val startOffset: Float,
    private val onDismiss: () -> Unit,
) {
    private var prepared = false

    /**
     * Moves the sheet to the part of its path the gesture has reached. The write takes the anchored
     * drag mutex, so a settle that is still in flight is canceled instead of being overwritten.
     */
    suspend fun seek(fraction: Float) {
        val progress = fraction.coerceIn(0f, 1f)
        state.backPreview = progress
        val targetOffset = state.anchoredDraggableState.anchors.positionOf(target)
        if (!targetOffset.isNaN()) {
            state.anchoredDraggableState.controlOffset {
                if (!prepared) {
                    state.offsetTimeline.prepare(startOffset, state.hideMotionSpec)
                    prepared = true
                }
                state.offsetTimeline.seekTo(progress, targetOffset) { dragTo(it) }
            }
        }
    }

    /** Continues the previewed offset segment to the accepted anchor. */
    suspend fun settleCommitted() {
        if (state.anchoredDraggableState.confirmValueChange(target)) {
            state.anchoredDraggableState.anchoredDrag(targetValue = target) { anchors, latestTarget
                ->
                val offset = anchors.positionOf(latestTarget)
                if (!offset.isNaN()) state.offsetTimeline.animateTo(offset) { dragTo(it) }
            }
        } else {
            // A vetoed dismissal returns the sheet to the value it had.
            settleRestored()
        }
        if (!state.isVisible) onDismiss()
    }

    /** Rewinds the original offset segment without replaying the show animation. */
    suspend fun settleRestored() {
        state.anchoredDraggableState.controlOffset {
            state.offsetTimeline.restore { dragTo(it) }
        }
    }
}

@Suppress("UnusedReceiverParameter")
@Composable
private fun BoxScope.OverlayBottomSheetContent(
    scope: CoroutineScope,
    animateToDismiss: () -> Unit,
    settleToDismiss: (velocity: Float) -> Unit,
    bounds: OverlayBounds,
    sheetVisible: () -> Boolean,
    modifier: Modifier = Modifier,
    sharedContentState: SharedContentState?,
    sheetState: OverlayBottomSheetState,
    sheetMaxWidth: Dp,
    sheetGesturesEnabled: Boolean,
    semanticsDismissEnabled: Boolean,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    dragHandle: @Composable (() -> Unit)?,
    contentWindowInsets: @Composable () -> WindowInsets,
    effects: MaterialSurfaceEffects? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spatialMotion = SpatialMotionEnabled
    val bottomSheetPaneTitle = OverlayStrings.bottomSheetPaneTitle()
    // The labels of the semantics actions of the sheet are read here: the actions themselves are
    // set in a semantics block, which cannot read a string resource.
    val collapseActionLabel = OverlayStrings.bottomSheetCollapseDescription()
    val dismissActionLabel = OverlayStrings.bottomSheetDismissDescription()
    val expandActionLabel = OverlayStrings.bottomSheetExpandDescription()
    val panelBackdrop = rememberMaterialSurfaceLayer()
    // Dragging while the shared bounds animate the sheet would move the bounds the animation reads,
    // so the sheet takes gestures again once the transition has finished.
    val sharedTransitionActive = LocalSharedTransitionScope.current?.isTransitionActive == true

    // The offset of the sheet while it is shown, kept while it leaves so a shared animation
    // starts from the place the panel has on screen.
    // A snapshot state: the placement below reads it during layout, and the write has to
    // invalidate that layout so the panel follows the offset of the shown sheet. While the
    // sheet is dismissed the place is frozen at the position it had when the dismissal
    // started, so a shared animation starts where the user let the sheet go.
    val shownOffset = remember { mutableStateOf(0f) }
    // Set when the user lets the sheet go: from that moment the place is frozen at the position
    // of the release, so the settle animation cannot move the start of a shared dismissal. It is
    // cleared once the sheet has settled back into a shown state.
    val currentOffset = sheetState.offset
    // A dismissed sheet does not move its own place: a shared animation carries the panel, and the
    // place the panel has when the animation starts is what the animation starts from. Freeze the
    // position when the sheet commits to Hidden. This applies to both a released drag and a direct
    // dismissal. With shared bounds, direct dismissal snaps the state offset to Hidden immediately.
    // During a drag, targetValue is speculative. The pointer keeps moving the panel until
    // onDragStopped commits the target.
    val dismissing =
        !sheetState.isUserDragging.value &&
            (sheetState.releaseTarget.value ?: sheetState.targetValue) ==
                OverlayBottomSheetValue.Hidden
    if (!dismissing && (sheetVisible() || sheetState.isVisible) && !currentOffset.isNaN()) {
        shownOffset.value = currentOffset
    }
    // The offset of the fully expanded sheet, used to keep the panel in place while a shared
    // animation carries it.
    val expandedOffset = remember { MutableFloat() }
    // The structural position of the panel. When the sheet opens from Hidden, this is the expanded
    // anchor. The shared animation carries the movement, so the panel must not follow the transient
    // hidden offset. Otherwise, the structural position matches the panel's on-screen position.
    // The shared animation starts there during a drag or while a released drag settles back. A
    // committed dismissal holds that position.
    val panelPlace: () -> Float = {
        val openingFromHidden =
            sheetState.currentValue == OverlayBottomSheetValue.Hidden &&
                sheetState.targetValue != OverlayBottomSheetValue.Hidden
        val place = shownOffset.value
        if (openingFromHidden || place.isNaN()) expandedOffset.value else place
    }

    // The surface is centered in the window and the anchors of the sheet place it along the
    // vertical axis: the alignment is applied by a Box because the surface is not a direct child
    // of the box that holds the sheet.
    Box(
        modifier =
            Modifier.fillMaxSize()
                // The place of the panel is structural: a shared animation reads it as the bounds
                // the panel has on screen and carries the movement between the panel and the
                // element it grows out of.
                .offset {
                    val offset =
                        if (
                            sharedContentState != null ||
                                (!spatialMotion && !sheetState.isUserDragging.value)
                        ) {
                            panelPlace()
                        } else {
                            sheetState.offset
                        }
                    IntOffset(0, if (offset.isNaN()) 0 else offset.roundToInt())
                },
        contentAlignment = Alignment.TopCenter,
    ) {
        // The panel moves as a whole, and the movement is part of the bounds a shared animation
        // reads: the offset is applied around the surface, so the panel node, its hit area and
        // the bounds of the shared element move together.
        Box {
            Surface(
                modifier =
                    modifier
                        // The content of the sheet keeps its own layout while a shared animation
                        // scales it
                        // into the animated bounds: a remeasure would move the anchors the sheet
                        // positions
                        // itself with while the bounds are on their way.
                        .overlaySharedBounds(
                            sharedContentState,
                            shape,
                        )
                        .widthIn(max = sheetMaxWidth)
                        .fillMaxWidth()
                        .then(
                            if (sheetGesturesEnabled && !sharedTransitionActive) {
                                Modifier.nestedScroll(
                                    remember(sheetState) {
                                        ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
                                            sheetState = sheetState,
                                            orientation = Orientation.Vertical,
                                            onFling = settleToDismiss,
                                        )
                                    },
                                )
                            } else {
                                Modifier
                            },
                        )
                        .draggable(
                            state = sheetState.anchoredDraggableState.draggableState,
                            orientation = Orientation.Vertical,
                            enabled =
                                sheetGesturesEnabled &&
                                    sheetState.isVisible &&
                                    !sharedTransitionActive,
                            startDragImmediately =
                                sheetState.anchoredDraggableState.isAnimationRunning,
                            onDragStarted = { sheetState.isUserDragging.value = true },
                            onDragStopped = { velocity ->
                                // The place the user let the sheet go: a shared animation of the
                                // dismissal
                                // starts from here, and nothing overwrites it while the sheet
                                // settles.
                                shownOffset.value = sheetState.offset
                                sheetState.released.value = true
                                sheetState.releaseTarget.value = null
                                sheetState.isUserDragging.value = false
                                settleToDismiss(velocity)
                            },
                        )
                        .semantics {
                            paneTitle = bottomSheetPaneTitle
                            traversalIndex = 0f
                            // The actions to operate the sheet are provided by the sheet itself, so
                            // a
                            // sheet without a drag handle is still operable with accessibility
                            // services.
                            if (semanticsDismissEnabled) {
                                dismiss(dismissActionLabel) {
                                    animateToDismiss()
                                    true
                                }
                            }
                            if (sheetGesturesEnabled) {
                                with(sheetState) {
                                    if (currentValue == OverlayBottomSheetValue.PartiallyExpanded) {
                                        expand(expandActionLabel) {
                                            if (
                                                anchoredDraggableState.confirmValueChange(
                                                    OverlayBottomSheetValue.Expanded,
                                                )
                                            ) {
                                                scope.launch { sheetState.expand() }
                                            }
                                            true
                                        }
                                    } else if (hasPartiallyExpandedState) {
                                        collapse(collapseActionLabel) {
                                            if (
                                                anchoredDraggableState.confirmValueChange(
                                                    OverlayBottomSheetValue.PartiallyExpanded,
                                                )
                                            ) {
                                                scope.launch { partialExpand() }
                                            }
                                            true
                                        }
                                    }
                                }
                            }
                        }
                        .consumeWindowInsets(
                            WindowInsets(top = sheetState.offset.toInt().coerceAtLeast(0)),
                        )
                        // Scale up the panel vertically in case the sheet's offset overflows below
                        // the min
                        // anchor. This avoids showing a gap when the sheet opens and bounces when
                        // it is
                        // applied with a bouncy motion. The content inside is scaled back down to
                        // maintain
                        // its aspect ratio.
                        .verticalScaleUp(sheetState),
                shape = shape,
                color = Color.Transparent,
                contentColor = contentColor,
            ) {
                // The surface spans the bounds the shared animation gives the panel and draws the
                // backdrop
                // over them, so the backdrop it samples belongs to the panel and moves with it. The
                // content is centered and keeps its own layout, so the animation reveals the panel
                // instead of reflowing its content.
                MaterialPanelSurface(
                    shape = shape,
                    containerColor = containerColor,
                    exportedBackdrop = panelBackdrop,
                    effects = effects,
                    excludedEdges = setOf(MaterialSurfaceEdge.Bottom),
                    modifier = Modifier.overlayContentBounds(bounds, enabled = sheetVisible()),
                ) {
                    Column(
                        Modifier.fillMaxWidth()
                            .keepSizeWhenLookingAhead()
                            .sheetAnchors(sheetState.anchoredDraggableState) { size, constraints ->
                                val fullHeight = constraints.maxHeight.toFloat()
                                val newAnchors = DraggableAnchors {
                                    OverlayBottomSheetValue.Hidden at fullHeight
                                    if (
                                        size.height > (fullHeight / 2) &&
                                            !sheetState.skipPartiallyExpanded
                                    ) {
                                        OverlayBottomSheetValue.PartiallyExpanded at fullHeight / 2f
                                    }
                                    if (size.height != 0) {
                                        OverlayBottomSheetValue.Expanded at
                                            max(0f, fullHeight - size.height)
                                    }
                                    expandedOffset.value = max(0f, fullHeight - size.height)
                                }
                                val newTarget =
                                    when (sheetState.anchoredDraggableState.targetValue) {
                                        OverlayBottomSheetValue.Hidden ->
                                            OverlayBottomSheetValue.Hidden
                                        OverlayBottomSheetValue.PartiallyExpanded -> {
                                            val hasPartiallyExpandedState =
                                                newAnchors.hasAnchorFor(
                                                    OverlayBottomSheetValue.PartiallyExpanded,
                                                )
                                            if (hasPartiallyExpandedState) {
                                                OverlayBottomSheetValue.PartiallyExpanded
                                            } else if (
                                                newAnchors.hasAnchorFor(
                                                    OverlayBottomSheetValue.Expanded,
                                                )
                                            ) {
                                                OverlayBottomSheetValue.Expanded
                                            } else {
                                                OverlayBottomSheetValue.Hidden
                                            }
                                        }

                                        OverlayBottomSheetValue.Expanded -> {
                                            if (
                                                newAnchors.hasAnchorFor(
                                                    OverlayBottomSheetValue.Expanded,
                                                )
                                            ) {
                                                OverlayBottomSheetValue.Expanded
                                            } else {
                                                OverlayBottomSheetValue.Hidden
                                            }
                                        }
                                    }
                                return@sheetAnchors newAnchors to newTarget
                            }
                            .windowInsetsPadding(contentWindowInsets())
                            // Scale the content down in case the sheet offset overflows below the
                            // min anchor.
                            // The wrapping panel is scaled up, so this keeps the content's aspect
                            // ratio.
                            .verticalScaleDown(sheetState),
                    ) {
                        if (dragHandle != null) {
                            DragHandleWithTooltip {
                                Box(
                                    modifier =
                                        Modifier.clickable(
                                            interactionSource = null,
                                            indication = null,
                                        ) {
                                            when (sheetState.currentValue) {
                                                OverlayBottomSheetValue.Expanded ->
                                                    animateToDismiss()
                                                OverlayBottomSheetValue.PartiallyExpanded ->
                                                    scope.launch { sheetState.expand() }

                                                else -> scope.launch { sheetState.show() }
                                            }
                                        },
                                ) {
                                    dragHandle()
                                }
                            }
                        }
                        content()
                    }
                }
            }
        }
    }
}

/**
 * Keeps the anchors of [state] up to date without placing the content: the panel of the sheet is
 * placed by the offset of its state, so the whole panel moves together with the gestures.
 *
 * Sheet content uses the lookahead constraints (see [keepSizeWhenLookingAhead]). Its size and the
 * constraints supplied to [anchors] stay stable during a shared animation. Deriving anchors from
 * the animated bounds would move the sheet's placement while that animation runs.
 */
private fun <T> Modifier.sheetAnchors(
    state: AnchoredDraggableState<T>,
    anchors: (size: IntSize, constraints: Constraints) -> Pair<DraggableAnchors<T>, T>,
): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val measuredSize = IntSize(placeable.width, placeable.height)
    val newAnchors = anchors(measuredSize, constraints)
    if (!isLookingAhead) {
        state.updateAnchors(newAnchors.first, newAnchors.second)
    }
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

/** Holds the offset of the fully expanded sheet for the placement of the panel. */
private class MutableFloat {
    var value: Float = 0f
}

/** The drag handle drawn on top of a [OverlayBottomSheet]. */
@Suppress("UnusedReceiverParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun OverlayBottomSheetDefaults.DragHandle(
    modifier: Modifier = Modifier,
    width: Dp = 32.dp,
    height: Dp = 4.dp,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    BottomSheetDefaults.DragHandle(
        modifier = modifier,
        width = width,
        height = height,
        shape = shape,
        color = color,
    )
}

@Composable
private fun ColumnScope.DragHandleWithTooltip(content: @Composable (() -> Unit)) {
    val dragHandleDescription = OverlayStrings.bottomSheetDragHandleDescription()
    // The outer box is needed for alignment because the tooltip modifier is only applied to its
    // anchor.
    Box(Modifier.align(Alignment.CenterHorizontally)) {
        OverlayTooltipBox(
            positionProvider =
                rememberOverlayTooltipPositionProvider(
                    positioning = OverlayTooltipAnchorPosition.Above,
                ),
            tooltip = { OverlayPlainTooltip { Text(dragHandleDescription) } },
            state = rememberOverlayTooltipState(),
            content = content,
        )
    }
}

private fun Modifier.verticalScaleUp(sheetState: OverlayBottomSheetState): Modifier =
    graphicsLayer {
        val offset = sheetState.anchoredDraggableState.offset
        val anchor = sheetState.anchoredDraggableState.anchors.minAnchor()
        val overflow = if (offset < anchor) anchor - offset else 0f
        scaleY = if (overflow > 0f) (size.height + overflow) / size.height else 1f
        transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0f)
    }

private fun Modifier.verticalScaleDown(sheetState: OverlayBottomSheetState): Modifier =
    graphicsLayer {
        val offset = sheetState.anchoredDraggableState.offset
        val anchor = sheetState.anchoredDraggableState.anchors.minAnchor()
        val overflow = if (offset < anchor) anchor - offset else 0f
        scaleY = if (overflow > 0f) 1 / ((size.height + overflow) / size.height) else 1f
        transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0f)
    }
