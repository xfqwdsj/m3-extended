/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.StyleStateKey
import androidx.compose.foundation.style.animate
import androidx.compose.foundation.style.rememberUpdatedStyleState
import androidx.compose.foundation.style.state
import androidx.compose.foundation.style.styleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CheckableDropdownMenuItem as MaterialCheckableDropdownMenuItem
import androidx.compose.material3.DropdownMenuItem as MaterialDropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuGroupShapes
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.MenuItemShapes
import androidx.compose.material3.SelectableDropdownMenuItem as MaterialSelectableDropdownMenuItem
import androidx.compose.material3.SelectableMenuItemColors
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.PopupPositionProvider
import kotlin.math.max
import kotlin.math.min
import top.ltfan.m3x.compose.foundation.keepSizeWhenLookingAhead
import top.ltfan.m3x.compose.ui.accessibility.SpatialMotionEnabled
import top.ltfan.m3x.material.MaterialSurfaceEffects
import top.ltfan.m3x.material.rememberMaterialSurfaceLayer
import top.ltfan.m3x.overlay.LocalOverlayAnimatedVisibilityScope
import top.ltfan.m3x.overlay.OverlayBackHandler
import top.ltfan.m3x.overlay.OverlayBounds
import top.ltfan.m3x.overlay.OverlayDismissTrigger
import top.ltfan.m3x.overlay.OverlayHandle
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayInteraction
import top.ltfan.m3x.overlay.overlayContentBounds
import top.ltfan.m3x.overlay.panelPlacement
import top.ltfan.m3x.overlay.rememberOverlayFocusRequester

/** The vertical padding between the menu container and its items. */
internal val OverlayMenuVerticalPadding = 8.dp

// Menu open/close animation.
private const val ExpandedScaleTarget = 1f
private const val ClosedScaleTarget = 0.8f

/**
 * Calculates the transform origin of a menu relative to its anchor, so the menu grows out of the
 * anchor edge it is aligned with.
 *
 * @param anchorBounds the bounds of the anchor in window coordinates
 * @param menuBounds the bounds of the menu in window coordinates
 */
internal fun calculateMenuTransformOrigin(
    anchorBounds: IntRect,
    menuBounds: IntRect,
): TransformOrigin =
    TransformOrigin(
        pivot(
            menuStart = menuBounds.left,
            menuEnd = menuBounds.right,
            anchorStart = anchorBounds.left,
            anchorEnd = anchorBounds.right,
        ),
        pivot(
            menuStart = menuBounds.top,
            menuEnd = menuBounds.bottom,
            anchorStart = anchorBounds.top,
            anchorEnd = anchorBounds.bottom,
        ),
    )

/**
 * The fractional position of the anchor inside the menu along one axis: the menu edge nearest the
 * anchor when the anchor lies outside the menu, and the center of their intersection when they
 * overlap.
 */
private fun pivot(
    menuStart: Int,
    menuEnd: Int,
    anchorStart: Int,
    anchorEnd: Int,
): Float =
    when {
        menuStart >= anchorEnd -> 0f
        menuEnd <= anchorStart -> 1f
        menuEnd - menuStart == 0 -> 0f
        else -> {
            val intersectionCenter = (max(anchorStart, menuStart) + min(anchorEnd, menuEnd)) / 2
            (intersectionCenter - menuStart).toFloat() / (menuEnd - menuStart)
        }
    }

/**
 * Remembers a [PopupPositionProvider] that places a menu relative to its anchor: it tries to align
 * the menu with the anchor, then to place it on the window edge closer to the anchor, and keeps it
 * inside the window.
 *
 * @param horizontalAlignment the horizontal alignment the menu prefers
 * @param contentOffset the offset from the original position of the menu
 * @param anchorSpacing the spacing between the menu and its anchor
 */
@Composable
public fun rememberOverlayMenuPositionProvider(
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    contentOffset: DpOffset = DpOffset.Zero,
    anchorSpacing: Dp = OverlayMenuAnchorSpacing,
): PopupPositionProvider {
    val density = LocalDensity.current
    val positionProvider =
        remember(density, horizontalAlignment, contentOffset, anchorSpacing) {
            OverlayMenuPositionProvider(
                contentOffset = contentOffset,
                density = density,
                horizontalAlignment = horizontalAlignment,
                anchorSpacing = with(density) { anchorSpacing.roundToPx() },
            )
        }
    return remember(positionProvider) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset =
                positionProvider.calculatePosition(
                    anchorBounds = anchorBounds,
                    windowSize = windowSize,
                    layoutDirection = layoutDirection,
                    menuSize = popupContentSize,
                )
        }
    }
}

/**
 * An in-window menu anchored to its parent layout.
 *
 * The menu scales and fades around a transform origin derived from its anchor. Arrow keys move
 * focus between its items. An outside press or a back press dismisses the menu.
 *
 * The items draw on a renderer-selected panel. The enclosing [OverlayHost] hosts the panel and its
 * interception layer as one overlay entry. They appear and disappear together.
 *
 * The host reads the menu state in its own composition. The panel and its page anchor therefore
 * change in the same frame. The shared bounds animation starts at the panel when the menu closes.
 *
 * Interactive use requires an enclosing [OverlayHost]. In inspection-mode previews without one, the
 * overlay is composed in place without host-managed ordering, modal isolation, back dispatch, or
 * backdrop stacking.
 *
 * The menu is positioned with [popupPositionProvider], which the caller usually obtains from
 * [rememberOverlayMenuPositionProvider].
 *
 * @param expanded reads whether the menu is expanded, usually `expanded = { state }`
 * @param onDismissRequest called on a press outside the menu, including any mouse button, or on
 *   Back or the accessible Dismiss action when enabled
 * @param modifier the [Modifier] to be applied to the menu's content
 * @param popupPositionProvider provides the screen position of the menu
 * @param scrollState a [ScrollState] to be used by the menu's content for item scrolling
 * @param shape the shape of the menu
 * @param containerColor the container color of the menu
 * @param contentColor the preferred color for content inside the menu
 * @param tonalElevation the tonal elevation of the menu
 * @param shadowElevation the elevation of the shadow below the menu
 * @param border the border to draw around the container of the menu
 * @param dismissOnClickOutside whether any touch or mouse button press outside the menu requests
 *   dismissal immediately
 * @param dismissOnBackPress whether the menu is dismissed by a back press
 * @param animatePanel whether the panel scales and fades in and out on its own. It is skipped while
 *   [sharedContentState] is set, because the shared bounds drive the appearance of the panel then.
 * @param sharedContentState the state of the shared bounds between the panel and the page element
 *   the menu grows out of, usually created with `rememberSharedContentState`. The page element is
 *   marked with [SharedTransitionAnchor] or with the same bounds API. When it is `null` the menu
 *   keeps its own appearance animation.
 * @param collapsedShape the panel shape at the hidden endpoint of its visibility transition
 * @param content the content of this menu, typically [OverlayMenuGroup]s of menu items
 */
@Composable
public fun OverlayMenu(
    expanded: () -> Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    popupPositionProvider: PopupPositionProvider = rememberOverlayMenuPositionProvider(),
    scrollState: ScrollState = rememberScrollState(),
    shape: Shape = MenuDefaults.shape,
    containerColor: Color = MenuDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = MenuDefaults.TonalElevation,
    shadowElevation: Dp = OverlayMenuDefaults.ShadowElevation,
    border: BorderStroke? = null,
    dismissOnClickOutside: Boolean = true,
    dismissOnBackPress: Boolean = true,
    animatePanel: Boolean = true,
    sharedContentState: SharedContentState? = null,
    effects: MaterialSurfaceEffects? = null,
    collapsedShape: Shape = shape,
    content: @Composable ColumnScope.() -> Unit,
) {
    val effectiveSharedContentState = sharedContentState.takeIf { SpatialMotionEnabled }
    val entry = remember { OverlayHandle() }
    val bounds = remember { OverlayBounds() }
    val dismissActionLabel = OverlayStrings.dismissMenu()
    val anchorBounds = remember { mutableStateOf(IntRect.Zero) }
    val stableMenuSize = remember { mutableStateOf(IntSize.Zero) }
    // The panel scales out of the anchor: the origin is calculated from the bounds of the anchor
    // and
    // the placed panel, so it does not depend on the position provider the caller passes in.
    val transformOriginState = remember { mutableStateOf(TransformOrigin.Center) }

    // The anchor is tracked for as long as the menu exists, so the first frame of the opening
    // animation already knows where the button is.
    AnchorBoundsTracker(anchorBounds)

    MaterialOverlay(
        sharedContentState = effectiveSharedContentState,
        entry = entry,
        modal = false,
        visible = expanded,
        // The host routes back gestures to the topmost overlay that accepts them, so the menu only
        // declares whether it accepts them. A press outside the bounds is taken by the
        // interception layer of the entry instead.
        backHandler =
            OverlayBackHandler(enabled = { dismissOnBackPress }, onCommit = onDismissRequest),
        // The panel takes pointer input through the interception layer of its entry: the panel
        // and the layer are composed together, and the layer covers the page while the menu is
        // open, so a press outside the menu dismisses it.
        interaction =
            OverlayInteraction(
                bounds = bounds,
                dismissOnClickOutside = { dismissOnClickOutside },
                trigger = OverlayDismissTrigger.Press,
                primaryButtonOnly = false,
                onDismissRequest = onDismissRequest,
            ),
        // Shared bounds carry the panel's appearance, so its exit stays visible until the bounds
        // finish traveling. Without shared bounds, the panel uses its own fade.
        enter =
            if (effectiveSharedContentState != null) EnterTransition.None
            else
                scaleIn(
                    initialScale = 1f,
                    animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                ),
        exit =
            if (effectiveSharedContentState != null) {
                scaleOut(
                    targetScale = 1f,
                    animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
                )
            } else {
                scaleOut(
                    targetScale = 1f,
                    animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                )
            },
    ) {
        OverlayMenuPanel(
            anchorBounds = anchorBounds.value,
            bounds = bounds,
            popupPositionProvider = popupPositionProvider,
            stableMenuSize = stableMenuSize,
            transformOriginState = transformOriginState,
            scrollState = scrollState,
            shape =
                rememberMenuTransitionShape(
                    collapsedShape,
                    shape,
                    Size(
                        stableMenuSize.value.width.toFloat(),
                        stableMenuSize.value.height.toFloat(),
                    ),
                ),
            containerColor = containerColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            border = border,
            modifier = modifier,
            dismissEnabled = dismissOnClickOutside || dismissOnBackPress,
            dismissActionLabel = dismissActionLabel,
            onDismissRequest = onDismissRequest,
            sharedContentState = effectiveSharedContentState,
            animatePanel = animatePanel && effectiveSharedContentState == null,
            effects = effects,
            content = content,
        )
    }
}

/**
 * Tracks the bounds of the parent layout of this node in window coordinates, which is the anchor
 * the menu is positioned against.
 */
@Composable
private fun AnchorBoundsTracker(bounds: MutableState<IntRect>) {
    Layout(
        content = {},
        modifier =
            Modifier.onPlaced { childCoordinates ->
                // Called before the overlay is asked to calculate the menu's position, so the
                // provider never sees the initial value.
                childCoordinates.parentCoordinates?.let {
                    bounds.value = IntRect(it.positionInWindow().round(), it.size)
                }
            },
    ) { _, _ ->
        layout(0, 0) {}
    }
}

/**
 * The menu panel is composed inside its overlay entry. The entry provides the
 * [AnimatedVisibilityScope] that the panel's shared bounds follow. The host drives this scope's
 * visibility from the menu state. The panel starts leaving in the same frame as the page anchor
 * appears.
 *
 * The panel is measured with the window constraints and reports a zero size, so it does not change
 * the layout it is placed in.
 */
@Composable
private fun OverlayMenuPanel(
    anchorBounds: IntRect,
    bounds: OverlayBounds,
    popupPositionProvider: PopupPositionProvider,
    stableMenuSize: MutableState<IntSize>,
    transformOriginState: MutableState<TransformOrigin>,
    scrollState: ScrollState,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    tonalElevation: Dp,
    shadowElevation: Dp,
    border: BorderStroke?,
    modifier: Modifier,
    dismissEnabled: Boolean,
    dismissActionLabel: String,
    onDismissRequest: () -> Unit,
    sharedContentState: SharedContentState?,
    animatePanel: Boolean,
    effects: MaterialSurfaceEffects? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val focusManager = LocalFocusManager.current
    val inputModeManager = LocalInputModeManager.current
    // The panel takes the focus when it appears, so keyboard navigation starts in the menu, whether
    // the menu is opened from a button or from any other trigger.
    val focusRequester = rememberOverlayFocusRequester()
    // The window size is available from the first frame, while the measured container bounds are
    // only known after the overlay has been placed.
    val containerSize = LocalWindowInfo.current.containerSize

    Box(
        Modifier.panelPlacement(
            bounds = bounds,
            anchorBounds = anchorBounds,
            positionProvider = popupPositionProvider,
            windowSize = containerSize,
            layoutDirection = layoutDirection,
            stableSize = stableMenuSize,
            onPlaced = { position, size ->
                transformOriginState.value =
                    calculateMenuTransformOrigin(anchorBounds, IntRect(position, size))
            },
        ),
    ) {
        OverlayMenuSurface(
            transformOriginState = transformOriginState,
            scrollState = scrollState,
            shape = shape,
            containerColor = containerColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            border = border,
            modifier =
                modifier
                    .focusRequester(focusRequester)
                    .focusable()
                    .overlayContentBounds(bounds)
                    .focusGroup()
                    .semantics {
                        if (dismissEnabled) {
                            dismiss(dismissActionLabel) {
                                onDismissRequest()
                                true
                            }
                        }
                    }
                    .onPreviewKeyEvent { keyEvent ->
                        handleOverlayMenuOnKeyEvent(keyEvent, focusManager, inputModeManager)
                    },
            panelModifier =
                Modifier.overlaySharedBounds(
                    sharedContentState = sharedContentState,
                    shape = shape,
                    // The shared panel draws in its menu entry and keeps that entry's drawing
                    // order. The button occupies an entry above the menu. It stays above the panel
                    // while the panel grows out of it and shrinks back into it.
                    renderInOverlayDuringTransition = false,
                ),
            animatePanel = animatePanel,
            sharedContainer = sharedContentState != null,
            effects = effects,
            content = content,
        )
    }
}

/**
 * The renderer-selected panel of a menu: it scales and fades with the menu transition and grows out
 * of the transform origin.
 */
@Composable
private fun OverlayMenuSurface(
    transformOriginState: MutableState<TransformOrigin>,
    scrollState: ScrollState,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    tonalElevation: Dp,
    shadowElevation: Dp,
    border: BorderStroke?,
    modifier: Modifier,
    panelModifier: Modifier,
    animatePanel: Boolean,
    sharedContainer: Boolean,
    effects: MaterialSurfaceEffects? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val transition = LocalOverlayAnimatedVisibilityScope.current?.transition
    val motionScheme = MaterialTheme.motionScheme
    val scale =
        transition
            ?.animateFloat(
                label = "OverlayMenuScale",
                transitionSpec = { motionScheme.defaultSpatialSpec() },
            ) { state ->
                if (state == EnterExitState.Visible) ExpandedScaleTarget else ClosedScaleTarget
            }
            ?.value ?: ExpandedScaleTarget
    val spatialMotion = SpatialMotionEnabled
    val panelBackdrop = rememberMaterialSurfaceLayer()
    val transformOrigin = transformOriginState.value

    Surface(
        modifier =
            panelModifier.overlayBlur(!sharedContainer).graphicsLayer {
                if (animatePanel && spatialMotion) {
                    scaleX = scale
                    scaleY = scale
                }
                this.transformOrigin = transformOrigin
            },
        shape = shape,
        color = Color.Transparent,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        border = border,
    ) {
        // Drawing follows the animated panel bounds. Centered content keeps its own layout,
        // so the animation reveals the panel while preserving item measurements.
        MaterialPanelSurface(
            shape = shape,
            containerColor = containerColor,
            exportedBackdrop = panelBackdrop,
            effects = effects,
        ) {
            Column(
                modifier =
                    Modifier.keepSizeWhenLookingAhead()
                        .overlayBlur(sharedContainer, fade = false)
                        .then(modifier)
                        .padding(vertical = OverlayMenuVerticalPadding)
                        .width(IntrinsicSize.Max)
                        .verticalScroll(scrollState),
                content = content,
            )
        }
    }
}

/**
 * A visually distinct group of menu items within a [OverlayMenu], so related items share a
 * container shape.
 *
 * The shape of the group follows the pointer: while the pointer is over the group, and before it
 * has ever been hovered, the group uses [MenuGroupShapes.shape]; once the pointer has left it, the
 * group morphs to [MenuGroupShapes.inactiveShape].
 *
 * @param shapes the shapes of this group, usually obtained from [OverlayMenuDefaults.groupShape]
 * @param modifier the [Modifier] to be applied to this group
 * @param containerColor the container color of this group. When it is `Color.Unspecified` the group
 *   draws no container of its own, so the menu panel stays visible through it
 * @param interactionSource an optional hoisted [MutableInteractionSource] that observes the hover
 *   interactions of this group
 * @param content the content of this menu group, typically menu items
 */
@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
public fun OverlayMenuGroup(
    shapes: MenuGroupShapes,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactions = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    var hasBeenHovered by remember { mutableStateOf(false) }
    if (hovered) {
        hasBeenHovered = true
    }
    // The group morphs to the inactive shape once the pointer has left it. The style reads that
    // state and Compose animates the shape between the two styles: a shape that can interpolate
    // (such as a continuous rounded rectangle) morphs, a shape of another family switches at once.
    val styleState =
        rememberUpdatedStyleState(interactions) {
            it[OverlayMenuGroupInactiveShapeKey] = hasBeenHovered && !hovered
        }
    val shapeMotion =
        if (SpatialMotionEnabled) MaterialTheme.motionScheme.fastSpatialSpec<Float>() else snap()
    val style =
        remember(shapes, containerColor, shapeMotion) {
            Style {
                if (containerColor.isSpecified) {
                    background(containerColor)
                }
                shape(shapes.shape)
                clip()
                state(OverlayMenuGroupInactiveShapeKey) {
                    animate(shapeMotion) { shape(shapes.inactiveShape) }
                }
            }
        }
    Box(modifier = modifier.fillMaxWidth().hoverable(interactions).styleable(styleState, style)) {
        CompositionLocalProvider(
            // A group without a container of its own keeps the content color of the surrounding
            // menu.
            LocalContentColor provides
                contentColorFor(containerColor).takeOrElse { LocalContentColor.current },
        ) {
            Column(content = content)
        }
    }
}

/**
 * The style state of a menu group after the pointer has left it: it morphs to the inactive shape.
 */
@OptIn(ExperimentalFoundationStyleApi::class)
private val OverlayMenuGroupInactiveShapeKey = StyleStateKey(false)

/**
 * An Expressive menu action inside the enclosing overlay, using Material layout and semantics.
 * [shape] follows the M3X group position; [supportingText] and [trailingContent] use Material
 * slots.
 */
@Composable
public fun OverlayMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.itemColors(),
    shape: Shape = OverlayMenuDefaults.itemShape(0, 1),
    horizontalArrangement: Arrangement.Horizontal =
        MenuDefaults.DropdownMenuItemHorizontalArrangement,
    contentPadding: PaddingValues = OverlayMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
): Unit =
    MaterialDropdownMenuItem(
        onClick = onClick,
        text = text,
        shape = shape,
        modifier = modifier,
        leadingIcon = leadingIcon,
        trailingContent = trailingContent,
        supportingText = supportingText,
        enabled = enabled,
        colors = colors,
        horizontalArrangement = horizontalArrangement,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
    )

/** A checkbox menu action whose checked value belongs to the caller. */
@Composable
public fun OverlayCheckableMenuItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    shapes: MenuItemShapes = OverlayMenuDefaults.itemShapes(0, 1),
    leadingIcon: @Composable (() -> Unit)? = null,
    checkedLeadingIcon: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: SelectableMenuItemColors = MenuDefaults.selectableItemColors(),
    horizontalArrangement: Arrangement.Horizontal =
        MenuDefaults.DropdownMenuItemHorizontalArrangement,
    contentPadding: PaddingValues = OverlayMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
): Unit =
    MaterialCheckableDropdownMenuItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        text = text,
        shapes = shapes,
        modifier = modifier,
        leadingIcon = leadingIcon,
        checkedLeadingIcon = checkedLeadingIcon,
        trailingContent = trailingContent,
        supportingText = supportingText,
        enabled = enabled,
        colors = colors,
        horizontalArrangement = horizontalArrangement,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
    )

/** A radio menu action whose selected value belongs to the caller. */
@Composable
public fun OverlaySelectableMenuItem(
    selected: Boolean,
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    shapes: MenuItemShapes = OverlayMenuDefaults.itemShapes(0, 1),
    leadingIcon: @Composable (() -> Unit)? = null,
    selectedLeadingIcon: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: SelectableMenuItemColors = MenuDefaults.selectableItemColors(),
    horizontalArrangement: Arrangement.Horizontal =
        MenuDefaults.DropdownMenuItemHorizontalArrangement,
    contentPadding: PaddingValues = OverlayMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
): Unit =
    MaterialSelectableDropdownMenuItem(
        selected = selected,
        onClick = onClick,
        text = text,
        shapes = shapes,
        modifier = modifier,
        leadingIcon = leadingIcon,
        selectedLeadingIcon = selectedLeadingIcon,
        trailingContent = trailingContent,
        supportingText = supportingText,
        enabled = enabled,
        colors = colors,
        horizontalArrangement = horizontalArrangement,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
    )

/** The default content padding of a [OverlayMenuItem]. */
public val OverlayMenuItemContentPadding: PaddingValues
    inline get() = MenuDefaults.DropdownMenuSelectableItemContentPadding

/**
 * The keyboard navigation of a menu: the arrow keys move the focus between the menu items, which
 * switches the input mode to the keyboard.
 */
internal fun handleOverlayMenuOnKeyEvent(
    keyEvent: KeyEvent,
    focusManager: FocusManager?,
    inputModeManager: InputModeManager?,
): Boolean =
    if (keyEvent.type == KeyEventType.KeyDown) {
        when (keyEvent.key) {
            Key.DirectionDown -> {
                inputModeManager?.requestInputMode(InputMode.Keyboard)
                focusManager?.moveFocus(FocusDirection.Next)
                true
            }

            Key.DirectionUp -> {
                inputModeManager?.requestInputMode(InputMode.Keyboard)
                focusManager?.moveFocus(FocusDirection.Previous)
                true
            }

            else -> false
        }
    } else {
        false
    }
