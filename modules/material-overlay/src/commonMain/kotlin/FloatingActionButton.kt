package top.ltfan.m3x.material.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.PopupPositionProvider
import top.ltfan.m3x.compose.ui.accessibility.SpatialMotionEnabled
import top.ltfan.m3x.material.MaterialSurfaceEffects
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.materialSurface
import top.ltfan.m3x.material.rememberSurfaceAppearance
import top.ltfan.m3x.overlay.LocalOverlayHost
import top.ltfan.m3x.overlay.LocalSharedTransitionScope
import top.ltfan.m3x.overlay.OverlayBounds
import top.ltfan.m3x.overlay.OverlayHandle
import top.ltfan.m3x.overlay.overlayContainerBounds

/**
 * A backdrop floating action button with a menu that grows out of it.
 *
 * The button is the page side of the menu's shared bounds: an endpoint that covers the button is
 * present while the menu is hidden, so the panel grows out of the button and shrinks back into it.
 * The menu is an overlay entry hosted by the enclosing `OverlayHost`, so a press outside it
 * dismisses it and the content under it stays unreachable. Interactive use requires an enclosing
 * `OverlayHost`. In inspection-mode previews without one, the menu is composed next to the button
 * without host-managed ordering, modal isolation, back dispatch, or backdrop stacking. Without a
 * source backdrop the button draws its translucent container tint directly.
 *
 * While the menu is open, the button occupies a host entry above the menu panel and its
 * interception layer. It keeps drawing and taking pointer input at its original position. Hover,
 * clicks, and the ripple remain active. The button is composed once and moves between the page and
 * the entry, which keeps its state, its icon animation and its interactions.
 *
 * [buttonContent] receives the expansion state. The component crossfades between states and applies
 * rotation and scale using the motion scheme. Reduced motion retains only the crossfade.
 *
 * @param expanded reads whether the menu is expanded, usually `expanded = { state }`. The host
 *   reads the same state, so the panel and the button change in the same frame in both directions
 * @param onExpandedChange called with the state the menu should have when the user expands or
 *   collapses it, such as a press on the button or a press outside the menu
 * @param modifier the [Modifier] to be applied to the button
 * @param shape the shape of the button
 * @param menuShape the shape of the menu panel, shared with other overlay menus
 * @param containerColor the container color of the button and of the menu panel
 * @param contentColor the preferred color for content inside the button and the menu
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting interactions for the button
 * @param menuContentDescription an optional description of the menu, read by accessibility services
 * @param popupPositionProvider provides the position of the menu
 * @param buttonContent the icon of the button for the given expansion state
 * @param menuContent the content of the menu, typically `OverlayMenuGroup`s of menu items, or
 *   `null` for a button without a menu
 */
@Composable
public fun MenuFloatingActionButton(
    expanded: () -> Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.shape,
    menuShape: Shape = MenuDefaults.shape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    interactionSource: MutableInteractionSource? = null,
    menuContentDescription: String? = null,
    popupPositionProvider: PopupPositionProvider =
        rememberOverlayMenuPositionProvider(horizontalAlignment = Alignment.End),
    buttonContent: @Composable (expanded: Boolean) -> Unit,
    menuContent: (@Composable ColumnScope.() -> Unit)? = null,
    effects: MaterialSurfaceEffects? = null,
) {
    val iconMotion = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val iconEffectsMotion = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val expandedValue = expanded()
    var menuPresent by remember { mutableStateOf(false) }
    var size by remember { mutableStateOf(DpSize.Zero) }
    val density = LocalDensity.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val sharedContentState = sharedTransitionScope?.let { scope ->
        val key = remember { Any() }
        with(scope) { rememberSharedContentState(key) }
    }
    // The button stays in an overlay entry for the full lifetime of the menu panel, so it draws
    // and takes pointer input above the panel during both entrance and exit. Movable content keeps
    // the button, its state, its icon animation and its interactions across both locations.
    val buttonInOverlay = sharedContentState != null && (expandedValue || menuPresent)
    val currentExpanded = rememberUpdatedState(expanded)
    val currentOnExpandedChange = rememberUpdatedState(onExpandedChange)
    val currentButtonContent = rememberUpdatedState(buttonContent)
    val currentShape = rememberUpdatedState(shape)
    val currentInteractionSource = rememberUpdatedState(interactionSource)
    // The owner retains adaptation while the button moves through hosted exit composition.
    val currentAppearance =
        rememberUpdatedState(
            rememberSurfaceAppearance(
                MaterialSurfaceRole.Control,
                containerColor = containerColor,
                contentColor = contentColor,
                effects = effects,
                shape = shape,
            ),
        )
    val button = remember {
        movableContentOf {
            val interactions =
                currentInteractionSource.value ?: remember { MutableInteractionSource() }
            val appearance = currentAppearance.value
            FloatingActionButton(
                onClick = { currentOnExpandedChange.value(!currentExpanded.value()) },
                modifier =
                    Modifier.materialSurface(
                        appearance,
                        currentShape.value,
                        interactionSource = interactions,
                    ),
                containerColor = Color.Transparent,
                contentColor = appearance.contentColor,
                elevation =
                    FloatingActionButtonDefaults.elevation(
                        0.dp,
                        0.dp,
                        0.dp,
                        0.dp,
                    ),
                interactionSource = interactions,
            ) {
                AnimatedContent(
                    targetState = currentExpanded.value(),
                    transitionSpec = {
                        fadeIn(animationSpec = iconEffectsMotion) togetherWith
                            fadeOut(animationSpec = iconEffectsMotion)
                    },
                ) { expanded ->
                    val rotation by
                        transition.animateFloat(
                            label = "MenuFabIconRotation",
                            transitionSpec = { iconMotion },
                        ) {
                            val degrees = if (expanded) -45f else 45f
                            if (it == EnterExitState.Visible) 0f else degrees
                        }

                    val scale by
                        transition.animateFloat(
                            label = "MenuFabIconScale",
                            transitionSpec = { iconMotion },
                        ) {
                            val factor = if (expanded) 1 / 1.27f else 1.27f
                            if (it == EnterExitState.Visible) 1f else factor
                        }

                    Box(
                        Modifier.scale(if (SpatialMotionEnabled) scale else 1f)
                            .rotate(if (SpatialMotionEnabled) rotation else 0f),
                    ) {
                        currentButtonContent.value(expanded)
                    }
                }
            }
        }
    }
    // The place the button occupies in the page, in window coordinates: the entry that holds the
    // button while the menu is open places it there, and the page keeps that place with a spacer.
    var slotBounds by remember { mutableStateOf(IntRect.Zero) }
    val buttonEntry = remember { OverlayHandle() }
    val buttonEntryBounds = remember { OverlayBounds() }
    // The button's entry belongs above its menu panel. Entries move to the front when they become
    // visible. The button follows the menu each time it opens, keeping this order across repeated
    // openings.
    val host = LocalOverlayHost.current
    var wasExpanded by remember { mutableStateOf(false) }
    SideEffect {
        if (expandedValue && !wasExpanded) host?.bringToFront(buttonEntry)
        wasExpanded = expandedValue
    }

    // The caller's modifier goes on the outermost box: the boxes below wrap the button exactly, so
    // parent-data modifiers such as `align` only have the caller's layout to act on here.
    Box(modifier = modifier, contentAlignment = Alignment.BottomEnd) {
        // The box wraps the button tightly so the menu positions itself against
        // the button bounds.
        Box(
            Modifier.onGloballyPositioned { coordinates ->
                slotBounds = IntRect(coordinates.positionInWindow().round(), coordinates.size)
                with(density) {
                    size =
                        DpSize(
                            width = coordinates.size.width.toDp(),
                            height = coordinates.size.height.toDp(),
                        )
                }
            },
        ) {
            // The page side of the shared bounds: an endpoint that covers the button, so the
            // panel grows out of the button and shrinks back into it. It draws nothing itself;
            // the button keeps drawing under it. The size is enforced inside the bounds, so the
            // animated bounds never inflate the box that holds the button and the anchor.
            if (sharedContentState != null) {
                SharedTransitionAnchor(
                    sharedContentState = sharedContentState,
                    visible = !expandedValue,
                    shape = shape,
                    renderInOverlayDuringTransition = false,
                ) {
                    Spacer(Modifier.requiredSize(size))
                }
            }

            if (buttonInOverlay) {
                // The button is drawn in its entry; the page keeps the place of the button, so the
                // layout of the page does not change while the menu is open.
                Spacer(Modifier.requiredSize(size))
            } else {
                button()
            }

            if (menuContent != null) {
                OverlayMenu(
                    expanded = expanded,
                    onDismissRequest = { onExpandedChange(false) },
                    popupPositionProvider = popupPositionProvider,
                    // The shared bounds drive the appearance of the panel, so the panel itself
                    // does not scale or fade.
                    animatePanel = false,
                    sharedContentState = sharedContentState,
                    shape = menuShape,
                    collapsedShape =
                        remember(shape, size, density) {
                            if (shape is CornerBasedShape) {
                                val buttonSize =
                                    with(density) { Size(size.width.toPx(), size.height.toPx()) }
                                shape.copy(
                                    topStart = CornerSize(shape.topStart.toPx(buttonSize, density)),
                                    topEnd = CornerSize(shape.topEnd.toPx(buttonSize, density)),
                                    bottomEnd =
                                        CornerSize(shape.bottomEnd.toPx(buttonSize, density)),
                                    bottomStart =
                                        CornerSize(shape.bottomStart.toPx(buttonSize, density)),
                                )
                            } else shape
                        },
                    containerColor = containerColor,
                    contentColor = contentColor,
                    effects = effects,
                ) {
                    DisposableEffect(Unit) {
                        menuPresent = true
                        onDispose { menuPresent = false }
                    }
                    Column(
                        modifier =
                            Modifier.then(
                                if (menuContentDescription != null) {
                                    Modifier.semantics {
                                        contentDescription = menuContentDescription
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                        content = menuContent,
                    )
                }
            }

            // The entry is composed after the menu, so the button draws and takes pointer input
            // above the panel of the menu and above the interception layer of its entry.
            if (sharedContentState != null && menuContent != null) {
                MaterialOverlay(
                    entry = buttonEntry,
                    modal = false,
                    immediateContent = true,
                    // The host keeps the button above the panel until the menu panel leaves its
                    // composition, including the full exit animation.
                    visible = { expanded() || menuPresent },
                    enter = EnterTransition.None,
                    exit = ExitTransition.None,
                ) {
                    Box(Modifier.fillMaxSize().overlayContainerBounds(buttonEntryBounds)) {
                        Box(
                            Modifier.offset {
                                    IntOffset(
                                        slotBounds.left - buttonEntryBounds.container.left,
                                        slotBounds.top - buttonEntryBounds.container.top,
                                    )
                                }
                                .size(size),
                        ) {
                            button()
                        }
                    }
                }
            }
        }
    }
}
