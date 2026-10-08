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

import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TooltipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.PopupPositionProvider
import kotlin.jvm.JvmInline
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import top.ltfan.m3x.compose.ui.accessibility.SpatialMotionEnabled
import top.ltfan.m3x.compose.ui.accessibility.rememberTouchExplorationOrSwitchAccessState
import top.ltfan.m3x.material.MaterialSurfaceEffects
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.materialSurface
import top.ltfan.m3x.material.rememberSurfaceAppearance
import top.ltfan.m3x.overlay.OverlayBackHandler
import top.ltfan.m3x.overlay.OverlayBounds
import top.ltfan.m3x.overlay.OverlayHandle
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayInteraction
import top.ltfan.m3x.overlay.overlayContainerBounds
import top.ltfan.m3x.overlay.overlayContentBounds
import top.ltfan.m3x.overlay.panelPlacement

/** Contains the default values used by the backdrop tooltip components. */
@OptIn(ExperimentalMaterial3Api::class)
public object OverlayTooltipDefaults {
    /** The default maximum width of a plain tooltip. */
    public val plainTooltipMaxWidth: Dp
        inline get() = TooltipDefaults.plainTooltipMaxWidth

    /** The default container color of a plain tooltip. */
    public val plainTooltipContainerColor: Color
        @Composable inline get() = TooltipDefaults.plainTooltipContainerColor

    /** The default content color of a plain tooltip. */
    public val plainTooltipContentColor: Color
        @Composable inline get() = TooltipDefaults.plainTooltipContentColor

    /** The default shape of a plain tooltip. */
    public val plainTooltipContainerShape: Shape
        @Composable inline get() = TooltipDefaults.plainTooltipContainerShape

    /** The spacing between a tooltip and its anchor. */
    public val SpacingBetweenTooltipAndAnchor: Dp = 4.dp

    internal val TooltipDuration = 1500.milliseconds

    internal val GlobalMutatorMutex: MutatorMutex = MutatorMutex()
}

/**
 * A plain tooltip with a renderer-selected surface: a short descriptive message for its anchor,
 * shown on long press, on mouse hover, and on keyboard focus.
 *
 * @param modifier the [Modifier] to be applied to the tooltip
 * @param maxWidth the maximum width of the tooltip
 * @param shape the shape of the tooltip container
 * @param containerColor the container color of the tooltip
 * @param contentColor the content color of the tooltip
 * @param effects optional renderer-specific effects applied to the tooltip surface
 * @param content the content of the tooltip
 */
@Composable
public fun OverlayPlainTooltip(
    modifier: Modifier = Modifier,
    maxWidth: Dp = OverlayTooltipDefaults.plainTooltipMaxWidth,
    shape: Shape = OverlayTooltipDefaults.plainTooltipContainerShape,
    containerColor: Color = OverlayTooltipDefaults.plainTooltipContainerColor,
    contentColor: Color = OverlayTooltipDefaults.plainTooltipContentColor,
    effects: MaterialSurfaceEffects? = null,
    content: @Composable () -> Unit,
) {
    val appearance =
        rememberSurfaceAppearance(
            MaterialSurfaceRole.InversePanel,
            containerColor = containerColor,
            contentColor = contentColor,
            effects = effects,
            shape = shape,
        )
    Surface(
        modifier = modifier,
        shape = shape,
        color = Color.Transparent,
        contentColor = contentColor,
    ) {
        Box(
            modifier =
                Modifier.sizeIn(
                        minWidth = TooltipMinWidth,
                        maxWidth = maxWidth,
                        minHeight = TooltipMinHeight,
                    )
                    .materialSurface(appearance, shape)
                    .padding(PlainTooltipContentPadding),
        ) {
            CompositionLocalProvider(
                LocalContentColor provides appearance.contentColor,
                LocalTextStyle provides MaterialTheme.typography.bodySmall,
                content = content,
            )
        }
    }
}

/**
 * Wraps [content] with a tooltip anchored to it. The tooltip is shown on long press for touch
 * input, on mouse hover, and on keyboard focus, and it is announced to accessibility services.
 *
 * Interactive use requires an enclosing [OverlayHost]. In inspection-mode previews without one, the
 * overlay is composed in place without host-managed ordering, modal isolation, back dispatch, or
 * backdrop stacking.
 *
 * A tooltip that has an action ([hasAction]) becomes focusable on its own while an accessibility
 * service reads the screen by touch exploration or switch access, and after Tab was pressed on the
 * anchor, so the action is reachable. The tooltip then takes the focus, and hands it back to the
 * anchor when it hides.
 *
 * @param tooltip the content of the tooltip
 * @param state the state of the tooltip's visibility
 * @param modifier the [Modifier] to be applied to the anchor
 * @param positionProvider provides the position of the tooltip relative to the anchor
 * @param onDismissRequest called when any touch or mouse button is pressed outside a focusable
 *   tooltip, or on Back or its accessible Dismiss action; by default the tooltip dismisses itself
 * @param focusable whether the tooltip takes the focus and covers the content around it while it is
 *   shown, so its content can be interacted with. A plain tooltip lets pointer input pass through
 *   to the content under it and never takes the focus. The tooltip is announced to accessibility
 *   services either way
 * @param enableUserInput whether the anchor handles long press, mouse hover, and keyboard focus to
 *   show the tooltip
 * @param hasAction whether the tooltip contains an action, which makes the tooltip focusable while
 *   an accessibility service reads the screen, and after Tab is pressed on the anchor
 * @param sharedContentState the state of the shared bounds between the tooltip and the page element
 *   it grows out of, usually created with `rememberSharedContentState`. When it is `null` the
 *   tooltip keeps its own appearance.
 * @param sharedClipShape the shape the shared bounds are clipped to while they are drawn in the
 *   overlay, usually the shape of the tooltip
 * @param content the anchor content
 */
@Composable
public fun OverlayTooltipBox(
    tooltip: @Composable () -> Unit,
    state: OverlayTooltipState,
    modifier: Modifier = Modifier,
    positionProvider: PopupPositionProvider = rememberOverlayTooltipPositionProvider(),
    onDismissRequest: (() -> Unit)? = null,
    focusable: Boolean = false,
    enableUserInput: Boolean = true,
    hasAction: Boolean = false,
    sharedContentState: SharedContentState? = null,
    sharedClipShape: Shape = RectangleShape,
    content: @Composable () -> Unit,
) {
    val entry = remember { OverlayHandle() }
    val bounds = remember { OverlayBounds() }
    // The bounds of the anchor are stored as numbers: the coordinates of a node change when it
    // moves, and a stored coordinates object would keep the position it had when the state was
    // written.
    val anchorBounds = remember { mutableStateOf(IntRect.Zero) }
    val tooltipFocusRequester = remember { FocusRequester() }
    val anchorFocusRequester = remember { FocusRequester() }
    // The tooltip becomes focusable on its own while an accessibility service reads the screen, and
    // after Tab was pressed on the anchor: the action of the tooltip is reachable then without the
    // user having to find the tooltip first.
    val forceFocusable = remember { mutableStateOf(false) }
    val accessibilityServiceEnabled = rememberTouchExplorationOrSwitchAccessState()
    // The tooltip reads this in its own composition, so the focus it takes follows the state of the
    // accessibility service and of the Tab key in the frame they change.
    val isFocusable = {
        focusable || (hasAction && (accessibilityServiceEnabled.value || forceFocusable.value))
    }
    val shouldBeFocusable = isFocusable()
    val dismissRequest = onDismissRequest ?: state::dismiss
    // The focus of the tooltip and the focus of the anchor are told apart by two flags: the anchor
    // does not dismiss a tooltip that holds the focus, and does not show a tooltip again from the
    // focus that the tooltip hands back. The entry of the tooltip lives in the window of the host,
    // so the platform does not restore the focus of the anchor by itself.
    val focusInTooltip = remember { mutableStateOf(false) }
    val focusReturning = remember { mutableStateOf(false) }
    LaunchedEffect(shouldBeFocusable, state.isVisible) {
        if (shouldBeFocusable && state.isVisible) {
            // The tooltip takes the focus from its own content; the anchor only has to know that
            // the
            // focus is no longer with it, so that its blur does not dismiss the tooltip.
            focusInTooltip.value = true
        } else if (focusInTooltip.value) {
            focusInTooltip.value = false
            focusReturning.value = true
            forceFocusable.value = false
            anchorFocusRequester.requestFocus()
        }
    }

    MaterialOverlay(
        entry = entry,
        sharedContentState = sharedContentState.takeIf { SpatialMotionEnabled },
        modal = false,
        // The host reads the tooltip state in its own composition. The tooltip transition starts in
        // the frame that state changes. A shared animation then reads the tooltip's bounds at the
        // start of dismissal.
        visible = { state.isVisible },
        // A focusable tooltip covers the content around it through the layer of its entry; a
        // plain tooltip stays out of the way of the pointer input.
        interaction =
            if (shouldBeFocusable)
                OverlayInteraction(
                    bounds = bounds,
                    dismissOnClickOutside = { true },
                    onDismissRequest = dismissRequest,
                )
            else null,
        backHandler =
            if (shouldBeFocusable)
                OverlayBackHandler(
                    enabled = { true },
                    onCommit = dismissRequest,
                )
            else null,
        // The tooltip fades in and out like the platform tooltip.
        enter =
            scaleIn(
                initialScale = 1f,
                animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
            ),
        exit =
            scaleOut(
                targetScale = 1f,
                animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
            ),
    ) {
        val paneDescription = OverlayStrings.tooltipPaneDescription()
        val dismissActionLabel = OverlayStrings.dismissTooltip()
        TooltipPopup(
            bounds = bounds,
            anchorBounds = anchorBounds.value,
            positionProvider = positionProvider,
            paneDescription = paneDescription,
            dismissActionLabel = dismissActionLabel,
            onDismissRequest = dismissRequest,
            sharedContentState = sharedContentState,
            sharedClipShape = sharedClipShape,
            focusRequester = tooltipFocusRequester,
            isFocusable = isFocusable,
            tooltip = tooltip,
        )
    }

    OverlayTooltipAnchor(
        state = state,
        enableUserInput = enableUserInput,
        hasAction = hasAction,
        forceFocusable = forceFocusable,
        focusInTooltip = focusInTooltip,
        focusReturning = focusReturning,
        focusRequester = anchorFocusRequester,
        modifier =
            modifier.onGloballyPositioned { coordinates ->
                anchorBounds.value =
                    IntRect(coordinates.positionInWindow().round(), coordinates.size)
            },
        content = content,
    )

    DisposableEffect(state) { onDispose { state.onDispose() } }
}

@Composable
private fun TooltipPopup(
    bounds: OverlayBounds,
    anchorBounds: IntRect,
    positionProvider: PopupPositionProvider,
    paneDescription: String,
    dismissActionLabel: String,
    onDismissRequest: () -> Unit,
    sharedContentState: SharedContentState?,
    sharedClipShape: Shape,
    focusRequester: FocusRequester,
    isFocusable: () -> Boolean,
    tooltip: @Composable () -> Unit,
) {
    val effectiveSharedContentState = sharedContentState.takeIf { SpatialMotionEnabled }
    val layoutDirection = LocalLayoutDirection.current
    val containerSize = LocalWindowInfo.current.containerSize
    // The tooltip only takes the focus while it is focusable: a plain tooltip stays out of the
    // focus
    // order of the page.
    val focusableNow = isFocusable()
    val focusModifier =
        if (focusableNow) {
            Modifier.focusRequester(focusRequester).focusable()
        } else {
            Modifier
        }
    if (focusableNow) {
        // The tooltip takes the focus itself: the entry of the tooltip lives in the window of the
        // host, so no platform window moves the focus into it like a focusable popup would.
        LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
    }

    Box(Modifier.fillMaxSize().overlayContainerBounds(bounds)) {
        // The panel is placed relative to the window and reports a zero size, so it does not change
        // the layout of the entry. The placement stays outside the shared bounds, so the position
        // of
        // the tooltip is calculated in the frame the entry appears, not from the bounds the shared
        // animation moves.
        Box(
            Modifier.panelPlacement(
                bounds = bounds,
                anchorBounds = anchorBounds,
                positionProvider = positionProvider,
                windowSize = containerSize,
                layoutDirection = layoutDirection,
            ),
        ) {
            Box(
                Modifier.overlaySharedBounds(effectiveSharedContentState, sharedClipShape)
                    .overlayBlur(fade = effectiveSharedContentState == null)
                    .overlayContentBounds(bounds)
                    .then(focusModifier)
                    .semantics {
                        liveRegion = LiveRegionMode.Assertive
                        paneTitle = paneDescription
                        if (focusableNow) {
                            dismiss(dismissActionLabel) {
                                onDismissRequest()
                                true
                            }
                        }
                    },
            ) {
                tooltip()
            }
        }
    }
}

@Composable
private fun OverlayTooltipAnchor(
    state: OverlayTooltipState,
    enableUserInput: Boolean,
    hasAction: Boolean,
    forceFocusable: MutableState<Boolean>,
    focusInTooltip: MutableState<Boolean>,
    focusReturning: MutableState<Boolean>,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val longPressLabel = OverlayStrings.tooltipLongPressLabel()
    Box(
        modifier =
            modifier
                // The requester gives the focus of the tooltip back to the anchor, which resolves
                // to
                // the first focusable element of its content.
                .focusRequester(focusRequester)
                .handleGestures(enableUserInput, state)
                .anchorSemantics(longPressLabel, enableUserInput, state, scope)
                .keyboardBehavior(
                    enabled = enableUserInput,
                    state = state,
                    scope = scope,
                    hasAction = hasAction,
                    forceFocusable = forceFocusable,
                    focusInTooltip = focusInTooltip,
                    focusReturning = focusReturning,
                ),
    ) {
        content()
    }
}

private fun Modifier.handleGestures(enabled: Boolean, state: OverlayTooltipState): Modifier =
    if (enabled) {
        this.pointerInput(state) {
                coroutineScope {
                    awaitEachGesture {
                        // The long press may finish before or after the tooltip is shown, so its
                        // state is tracked in a flow to handle both orders.
                        val isLongPressedFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
                        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                        val pass = PointerEventPass.Initial
                        val inputType = awaitFirstDown(pass = pass).type

                        if (inputType == PointerType.Touch || inputType == PointerType.Stylus) {
                            try {
                                withTimeout(longPressTimeout) {
                                    waitForUpOrCancellation(pass = pass)
                                }
                            } catch (_: PointerEventTimeoutCancellationException) {
                                this@coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                                    try {
                                        isLongPressedFlow.tryEmit(true)
                                        state.show(MutatePriority.PreventUserInput)
                                    } finally {
                                        if (state.isVisible) {
                                            isLongPressedFlow.collectLatest { isLongPressed ->
                                                if (!isLongPressed) state.dismiss()
                                            }
                                        }
                                    }
                                }

                                // Consume the children's click handling; the long press may still
                                // be in progress.
                                val upEvent = waitForUpOrCancellation(pass = pass)
                                upEvent?.consume()
                            } finally {
                                isLongPressedFlow.tryEmit(false)
                            }
                        }
                    }
                }
            }
            .pointerInput(state) {
                coroutineScope {
                    awaitPointerEventScope {
                        val pass = PointerEventPass.Main
                        while (true) {
                            val event = awaitPointerEvent(pass)
                            val inputType = event.changes[0].type
                            if (inputType == PointerType.Mouse) {
                                when (event.type) {
                                    PointerEventType.Enter -> {
                                        this@coroutineScope.launch {
                                            state.show(MutatePriority.UserInput)
                                        }
                                    }

                                    PointerEventType.Exit -> state.dismiss()
                                }
                            }
                        }
                    }
                }
            }
    } else {
        this
    }

private fun Modifier.anchorSemantics(
    label: String,
    enabled: Boolean,
    state: OverlayTooltipState,
    scope: CoroutineScope,
): Modifier =
    if (enabled) {
        this.semantics {
            onLongClick(label = label) {
                scope.launch { state.show() }
                true
            }
        }
    } else {
        this
    }

private fun Modifier.keyboardBehavior(
    enabled: Boolean,
    state: OverlayTooltipState,
    scope: CoroutineScope,
    hasAction: Boolean,
    forceFocusable: MutableState<Boolean>,
    focusInTooltip: MutableState<Boolean>,
    focusReturning: MutableState<Boolean>,
): Modifier =
    if (enabled) {
        // The tooltip is dismissed when the anchor loses the keyboard focus it had: the focus
        // events of an anchor that never held the focus, such as the initial event of a mouse
        // driven tooltip, leave the tooltip alone. A tooltip that holds the focus itself keeps the
        // anchor unfocused without being dismissed, and the focus it hands back to the anchor does
        // not show the tooltip again.
        var wasFocused = false
        this.onFocusChanged {
                val focused = it.isFocused
                scope.launch {
                    // The tooltip shows while the anchor is keyboard focused.
                    if (focused) {
                        if (focusReturning.value) {
                            focusReturning.value = false
                        } else {
                            state.show(MutatePriority.PreventUserInput)
                        }
                    }
                    if (!focused && wasFocused && !focusInTooltip.value) state.dismiss()
                }
                wasFocused = focused
            }
            .onPreviewKeyEvent { event ->
                if (!state.isVisible) {
                    forceFocusable.value = false
                }
                // Tab moves the focus from the anchor into the tooltip, which holds the action: the
                // key event is consumed and the tooltip is made focusable for the request.
                if (
                    hasAction &&
                        event.type == KeyEventType.KeyDown &&
                        event.key == Key.Tab &&
                        state.isVisible
                ) {
                    forceFocusable.value = true
                    true
                } else {
                    false
                }
            }
    } else {
        forceFocusable.value = false
        this
    }

/**
 * The state that is associated with a [OverlayTooltipBox]. Each instance of [OverlayTooltipBox]
 * should have its own state. Custom implementations must expose visibility changes through Compose
 * observable state.
 */
@Stable
public interface OverlayTooltipState {
    /** Whether the tooltip is currently being shown or animating out. */
    public val isVisible: Boolean

    /**
     * Whether the tooltip is persistent. A persistent tooltip is dismissed only when the user
     * interacts elsewhere or when [dismiss] is called; a non-persistent tooltip also dismisses
     * after a short duration.
     */
    public val isPersistent: Boolean

    /**
     * Shows the tooltip associated with this state. All other tooltips associated with the same
     * [MutatorMutex] are dismissed.
     *
     * @param mutatePriority the priority to be used with the mutator mutex
     */
    public suspend fun show(mutatePriority: MutatePriority = MutatePriority.Default)

    /** Dismisses the tooltip associated with this state if it is currently shown. */
    public fun dismiss()

    /** Cleans up the state when it leaves composition. */
    public fun onDispose()
}

/** Creates and remembers a [OverlayTooltipState]. */
@Composable
public fun rememberOverlayTooltipState(
    initialIsVisible: Boolean = false,
    isPersistent: Boolean = false,
    mutatorMutex: MutatorMutex = OverlayTooltipDefaults.GlobalMutatorMutex,
): OverlayTooltipState =
    remember(isPersistent, mutatorMutex) {
        OverlayTooltipStateImpl(
            initialIsVisible = initialIsVisible,
            isPersistent = isPersistent,
            mutatorMutex = mutatorMutex,
        )
    }

@Stable
private class OverlayTooltipStateImpl(
    initialIsVisible: Boolean,
    override val isPersistent: Boolean,
    private val mutatorMutex: MutatorMutex,
) : OverlayTooltipState {
    override var isVisible: Boolean by mutableStateOf(initialIsVisible)
        private set

    /** The continuation used to clean up the show call. */
    private var job: (CancellableContinuation<Unit>)? = null

    override suspend fun show(mutatePriority: MutatePriority) {
        val cancellableShow: suspend () -> Unit = {
            suspendCancellableCoroutine { continuation ->
                isVisible = true
                job = continuation
            }
        }

        mutatorMutex.mutate(mutatePriority) {
            try {
                if (isPersistent) {
                    cancellableShow()
                } else {
                    withTimeout(OverlayTooltipDefaults.TooltipDuration) { cancellableShow() }
                }
            } finally {
                // The timeout or cancellation has occurred, so the tooltip is closed.
                isVisible = false
                job = null
            }
        }
    }

    override fun dismiss() {
        isVisible = false
        // Ends the show call of this tooltip: a persistent tooltip holds the mutator mutex until
        // its
        // show call ends, so a tooltip that is dismissed without ending its call would keep other
        // tooltips from showing.
        job?.cancel()
        job = null
    }

    override fun onDispose() {
        job?.cancel()
    }
}

/**
 * Remembers a [PopupPositionProvider] that places a tooltip relative to its anchor: horizontally it
 * centers on the anchor and falls back to the anchor edges, vertically it prefers to be above the
 * anchor and falls back to below it. A tooltip that fits on neither side is kept inside the window.
 *
 * @param positioning where the tooltip is placed relative to the anchor
 * @param spacingBetweenTooltipAndAnchor the spacing between the tooltip and the anchor
 */
@Composable
public fun rememberOverlayTooltipPositionProvider(
    positioning: OverlayTooltipAnchorPosition = OverlayTooltipAnchorPosition.Above,
    spacingBetweenTooltipAndAnchor: Dp = OverlayTooltipDefaults.SpacingBetweenTooltipAndAnchor,
): PopupPositionProvider {
    val tooltipAnchorSpacing =
        with(LocalDensity.current) { spacingBetweenTooltipAndAnchor.roundToPx() }
    return remember(tooltipAnchorSpacing, positioning) {
        TooltipPositionProviderImpl(positioning, tooltipAnchorSpacing)
    }
}

/** Where a tooltip is placed relative to its anchor. */
@Immutable
@JvmInline
public value class OverlayTooltipAnchorPosition private constructor(private val value: Int) {
    public companion object {
        /** Places the tooltip above the anchor. */
        public val Above: OverlayTooltipAnchorPosition = OverlayTooltipAnchorPosition(1)

        /** Places the tooltip below the anchor. */
        public val Below: OverlayTooltipAnchorPosition = OverlayTooltipAnchorPosition(2)

        /** Places the tooltip to the left of the anchor. */
        public val Left: OverlayTooltipAnchorPosition = OverlayTooltipAnchorPosition(3)

        /** Places the tooltip to the right of the anchor. */
        public val Right: OverlayTooltipAnchorPosition = OverlayTooltipAnchorPosition(4)
    }

    override fun toString(): String =
        when (this) {
            Above -> "Above"
            Below -> "Below"
            Left -> "Left"
            Right -> "Right"
            else -> "Invalid"
        }
}

private class TooltipPositionProviderImpl(
    private val positioning: OverlayTooltipAnchorPosition,
    private val tooltipAnchorSpacing: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val position =
            when (positioning) {
                OverlayTooltipAnchorPosition.Left -> leftPositioning(anchorBounds, popupContentSize)
                OverlayTooltipAnchorPosition.Right ->
                    rightPositioning(anchorBounds, popupContentSize, windowSize)

                OverlayTooltipAnchorPosition.Below ->
                    belowPositioning(anchorBounds, popupContentSize, windowSize)

                else -> abovePositioning(anchorBounds, popupContentSize, windowSize)
            }
        // A tooltip that does not fit next to its anchor on either side is kept inside the window,
        // which can bring it over the anchor.
        return IntOffset(
            x =
                position.x.coerceIn(
                    0,
                    (windowSize.width - popupContentSize.width).coerceAtLeast(0),
                ),
            y =
                position.y.coerceIn(
                    0,
                    (windowSize.height - popupContentSize.height).coerceAtLeast(0),
                ),
        )
    }

    private fun leftPositioning(anchorBounds: IntRect, popupContentSize: IntSize): IntOffset {
        // Horizontal preference: left -> right; vertical preference: center.
        var x = anchorBounds.left - (popupContentSize.width + tooltipAnchorSpacing)
        if (x < 0) {
            // Flip the tooltip to the right when it collides with the left screen edge.
            x = anchorBounds.right + tooltipAnchorSpacing
        }
        val y = (anchorBounds.top + anchorBounds.bottom - popupContentSize.height) / 2
        return IntOffset(x, y)
    }

    private fun rightPositioning(
        anchorBounds: IntRect,
        popupContentSize: IntSize,
        windowSize: IntSize,
    ): IntOffset {
        // Horizontal preference: right -> left; vertical preference: center.
        var x = anchorBounds.right + tooltipAnchorSpacing
        if (x + popupContentSize.width > windowSize.width) {
            // Flip the tooltip to the left when it collides with the right screen edge.
            x = anchorBounds.left - (popupContentSize.width + tooltipAnchorSpacing)
        }
        val y = (anchorBounds.top + anchorBounds.bottom - popupContentSize.height) / 2
        return IntOffset(x, y)
    }

    private fun abovePositioning(
        anchorBounds: IntRect,
        popupContentSize: IntSize,
        windowSize: IntSize,
    ): IntOffset {
        // Horizontal preference: middle -> start -> end; vertical preference: above -> below.
        var x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        if (x < 0) {
            x = anchorBounds.left
        } else if (x + popupContentSize.width > windowSize.width) {
            x = anchorBounds.right - popupContentSize.width
        }

        var y = anchorBounds.top - popupContentSize.height - tooltipAnchorSpacing
        if (y < 0) y = anchorBounds.bottom + tooltipAnchorSpacing
        return IntOffset(x, y)
    }

    private fun belowPositioning(
        anchorBounds: IntRect,
        popupContentSize: IntSize,
        windowSize: IntSize,
    ): IntOffset {
        // Horizontal preference: middle -> start -> end; vertical preference: below -> above.
        var x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        if (x < 0) {
            x = anchorBounds.left
        } else if (x + popupContentSize.width > windowSize.width) {
            x = anchorBounds.right - popupContentSize.width
        }

        var y = anchorBounds.bottom + tooltipAnchorSpacing
        if (y + popupContentSize.height > windowSize.height) {
            y = anchorBounds.top - popupContentSize.height - tooltipAnchorSpacing
        }
        return IntOffset(x, y)
    }
}

// The hosted plain tooltip uses Material's minimum content dimensions.
private val TooltipMinWidth = 40.dp
private val TooltipMinHeight = 24.dp
private val PlainTooltipContentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
