package top.ltfan.m3x.material.overlay

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrimDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import kotlin.math.min
import top.ltfan.m3x.compose.foundation.keepSizeWhenLookingAhead
import top.ltfan.m3x.compose.ui.accessibility.SpatialMotionEnabled
import top.ltfan.m3x.material.MaterialSurfaceEffects
import top.ltfan.m3x.material.MaterialSurfaceLayer
import top.ltfan.m3x.material.rememberMaterialSurfaceLayer
import top.ltfan.m3x.overlay.LocalOverlayAnimatedVisibilityScope
import top.ltfan.m3x.overlay.OverlayBackHandler
import top.ltfan.m3x.overlay.OverlayBounds
import top.ltfan.m3x.overlay.OverlayDismissTrigger
import top.ltfan.m3x.overlay.OverlayHandle
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayInteraction
import top.ltfan.m3x.overlay.OverlayNavigationTransition
import top.ltfan.m3x.overlay.overlayContainerBounds
import top.ltfan.m3x.overlay.overlayContentBounds
import top.ltfan.m3x.overlay.rememberOverlayFocusRequester

/** Distance the dialog content travels while it appears or disappears. */
private val AnimatedLayerOffset = 10.dp

/** Scale difference of the dialog content at the low point of the transition. */
private const val AnimatedLayerScale = 0.05f

/**
 * Properties used to customize the behavior of a [OverlayDialog].
 *
 * @property dismissOnBackPress whether the dialog can be dismissed with the system back button
 * @property dismissOnClickOutside whether a touch tap or primary mouse click that starts and ends
 *   outside the content can dismiss the dialog on release
 * @property usePlatformDefaultWidth whether the width of the dialog's content is limited to the
 *   platform default, which is smaller than the screen width
 * @property usePlatformInsets whether the dialog's content is measured inside the system bars
 * @property useSoftwareKeyboardInset whether the dialog's content is measured outside the software
 *   keyboard
 * @property scrimColor color of the scrim that obscures the content under the dialog, or
 *   [Color.Unspecified] to use [ScrimDefaults.color]
 * @property animateTransition whether the appearance and disappearance of the dialog are animated
 */
@Immutable
public class OverlayDialogProperties(
    public val dismissOnBackPress: Boolean = true,
    public val dismissOnClickOutside: Boolean = true,
    public val usePlatformDefaultWidth: Boolean = true,
    public val usePlatformInsets: Boolean = true,
    public val useSoftwareKeyboardInset: Boolean = true,
    public val scrimColor: Color = Color.Unspecified,
    public val animateTransition: Boolean = true,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OverlayDialogProperties) return false

        if (dismissOnBackPress != other.dismissOnBackPress) return false
        if (dismissOnClickOutside != other.dismissOnClickOutside) return false
        if (usePlatformDefaultWidth != other.usePlatformDefaultWidth) return false
        if (usePlatformInsets != other.usePlatformInsets) return false
        if (useSoftwareKeyboardInset != other.useSoftwareKeyboardInset) return false
        if (scrimColor != other.scrimColor) return false
        if (animateTransition != other.animateTransition) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dismissOnBackPress.hashCode()
        result = 31 * result + dismissOnClickOutside.hashCode()
        result = 31 * result + usePlatformDefaultWidth.hashCode()
        result = 31 * result + usePlatformInsets.hashCode()
        result = 31 * result + useSoftwareKeyboardInset.hashCode()
        result = 31 * result + scrimColor.hashCode()
        result = 31 * result + animateTransition.hashCode()
        return result
    }
}

/**
 * In-window dialog hosted by the enclosing [OverlayHost]: a scrim that dismisses the dialog plus
 * centered content that is measured against the platform insets.
 *
 * Interactive use requires an enclosing [OverlayHost]. In inspection-mode previews without one, the
 * overlay is composed in place without host-managed ordering, modal isolation, back dispatch, or
 * backdrop stacking.
 *
 * An outside touch tap or primary mouse click requests dismissal on release only when it starts and
 * ends outside the content. Back requests dismissal when enabled; an accessible Dismiss action is
 * available while either dismissal route is enabled. The configured appearance transition applies
 * to ordinary dismissal and Back previews.
 *
 * @param onDismissRequest called when the user requests to dismiss the dialog
 * @param modifier the [Modifier] to be applied to the dialog content
 * @param properties the behavior of the dialog
 * @param visible reads whether the dialog is shown, usually `visible = { state }`. Keep the dialog
 *   composed and pass the state here when the dialog shares its bounds with another element, so the
 *   dialog and the element it grows out of change in the same frame in both directions
 * @param sharedContentState the state of the shared bounds between the dialog content and the page
 *   element it grows out of, usually created with `rememberSharedContentState`. When it is `null`
 *   the dialog keeps its own appearance animation.
 * @param navigationTransition caller-owned motion shared by this dialog and its page endpoints
 * @param sharedClipShape the shape of the panel, used to clip the shared bounds while they are
 *   drawn in the overlay and to draw its backdrop
 * @param containerColor the container color of the panel of the dialog. When it is
 *   `Color.Unspecified` the dialog draws no backdrop of its own, and the content draws its own
 *   container
 * @param contentColor the foreground color for the dialog panel
 * @param content the content of the dialog
 */
@Composable
public fun OverlayDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: OverlayDialogProperties = OverlayDialogProperties(),
    visible: () -> Boolean = { true },
    sharedContentState: SharedContentState? = null,
    navigationTransition: OverlayNavigationTransition? = null,
    sharedClipShape: Shape = RectangleShape,
    containerColor: Color = Color.Unspecified,
    effects: MaterialSurfaceEffects? = null,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable BoxScope.() -> Unit,
) {
    val effectiveSharedContentState = sharedContentState.takeIf { SpatialMotionEnabled }
    val motionScheme = MaterialTheme.motionScheme
    val dismissActionLabel = OverlayStrings.dismissDialog()
    val entry = remember { OverlayHandle() }
    val bounds = remember { OverlayBounds() }
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val currentProperties by rememberUpdatedState(properties)
    // The shared bounds drive the appearance of the dialog while it is set, so the dialog does
    // not run its own appearance transition then.
    val currentOwnTransition by
        rememberUpdatedState(properties.animateTransition && effectiveSharedContentState == null)

    MaterialOverlay(
        navigationTransition = navigationTransition,
        navigationContentKey = sharedContentState?.key,
        sharedContentState = effectiveSharedContentState,
        entry = entry,
        modal = true,
        visible = visible,
        // The host routes back gestures to the topmost overlay that accepts them, so the dialog
        // only declares whether it accepts them and what dismissing means. An instant back press
        // and a completed predictive back gesture are both a dismissal.
        backHandler =
            OverlayBackHandler(
                enabled = { currentProperties.dismissOnBackPress },
                onCommit = { currentOnDismissRequest() },
            ),
        // The dialog and its interception layer are one entry: the layer covers the page and the
        // entries under the dialog while it is shown, and a press outside its content dismisses
        // it when the properties allow it. The dim lives in the layer too, like the dim of a
        // sheet, so it fades in and out in every case, including a dialog whose panel is carried
        // by a shared animation instead of its own transition.
        interaction =
            OverlayInteraction(
                bounds = bounds,
                dismissOnClickOutside = { currentProperties.dismissOnClickOutside },
                trigger = OverlayDismissTrigger.Release,
                primaryButtonOnly = true,
                onDismissRequest = { currentOnDismissRequest() },
                scrimColor =
                    if (currentProperties.scrimColor.isSpecified) currentProperties.scrimColor
                    else ScrimDefaults.color,
                animateScrim = currentProperties.animateTransition,
            ),
        // The entry retains the panel through its exit. Shared bounds own the container
        // crossfade and geometry; an unshared panel applies optics to the whole surface.
        enter =
            when {
                effectiveSharedContentState != null -> EnterTransition.None
                currentProperties.animateTransition ->
                    scaleIn(initialScale = 1f, animationSpec = motionScheme.defaultSpatialSpec())

                else -> EnterTransition.None
            },
        exit =
            when {
                effectiveSharedContentState != null ->
                    scaleOut(targetScale = 1f, animationSpec = motionScheme.slowSpatialSpec())

                currentProperties.animateTransition ->
                    scaleOut(targetScale = 1f, animationSpec = motionScheme.fastSpatialSpec())

                else -> ExitTransition.None
            },
    ) {
        var placed by remember { mutableStateOf(false) }
        val focusRequester = rememberOverlayFocusRequester(enabled = placed)
        val insets = rememberDialogInsets(currentProperties)
        val panelBackdrop = rememberMaterialSurfaceLayer()

        // Only an unshared panel uses this spatial progress. Shared bounds carry the
        // container's geometry while its inner content owns defocus.
        val entryVisibility = LocalOverlayAnimatedVisibilityScope.current
        val dialogProgress: Float =
            if (
                currentOwnTransition &&
                    SpatialMotionEnabled &&
                    entryVisibility != null &&
                    effectiveSharedContentState == null
            ) {
                val currentProgress by
                    entryVisibility.transition.animateFloat(
                        label = "OverlayDialogProgress",
                        transitionSpec = {
                            if (targetState == EnterExitState.Visible) {
                                motionScheme.defaultSpatialSpec()
                            } else {
                                motionScheme.fastSpatialSpec()
                            }
                        },
                    ) { enterExitState ->
                        if (enterExitState == EnterExitState.Visible) 1f else 0f
                    }
                currentProgress
            } else {
                1f
            }
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .overlayContainerBounds(bounds)
                    .semantics {
                        dialog()
                        if (
                            currentProperties.dismissOnBackPress ||
                                currentProperties.dismissOnClickOutside
                        ) {
                            dismiss(dismissActionLabel) {
                                currentOnDismissRequest()
                                true
                            }
                        }
                    }
                    .onPlaced { placed = true }
                    .focusRequester(focusRequester)
                    .focusGroup(),
        ) {
            DialogLayout(
                insets = insets,
                usePlatformDefaultWidth = currentProperties.usePlatformDefaultWidth,
                modifier = modifier,
                bounds = bounds,
                progress = dialogProgress,
                animateOptics = currentProperties.animateTransition,
                sharedContentState = effectiveSharedContentState,
                sharedClipShape = sharedClipShape,
                containerColor = containerColor,
                contentColor = contentColor,
                panelBackdrop = panelBackdrop,
                effects = effects,
                content = content,
            )
        }
    }
}

/** The platform insets of a [OverlayDialog], resolved for the current density. */
@Immutable
private class OverlayInsets(
    val paddingValues: PaddingValues,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val horizontal: Int
        get() = left + right

    val vertical: Int
        get() = top + bottom
}

@Composable
private fun rememberDialogInsets(properties: OverlayDialogProperties): OverlayInsets {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val systemBars =
        if (properties.usePlatformInsets) {
            WindowInsets.systemBars.asPaddingValues()
        } else {
            PaddingValues(0.dp)
        }
    val ime =
        if (properties.useSoftwareKeyboardInset) {
            WindowInsets.ime.asPaddingValues()
        } else {
            PaddingValues(0.dp)
        }

    val paddingValues =
        PaddingValues(
            start =
                maxOf(
                    systemBars.calculateLeftPadding(layoutDirection),
                    ime.calculateLeftPadding(layoutDirection),
                ),
            top = maxOf(systemBars.calculateTopPadding(), ime.calculateTopPadding()),
            end =
                maxOf(
                    systemBars.calculateRightPadding(layoutDirection),
                    ime.calculateRightPadding(layoutDirection),
                ),
            bottom = maxOf(systemBars.calculateBottomPadding(), ime.calculateBottomPadding()),
        )
    with(density) {
        return OverlayInsets(
            paddingValues = paddingValues,
            left = paddingValues.calculateLeftPadding(layoutDirection).roundToPx(),
            top = paddingValues.calculateTopPadding().roundToPx(),
            right = paddingValues.calculateRightPadding(layoutDirection).roundToPx(),
            bottom = paddingValues.calculateBottomPadding().roundToPx(),
        )
    }
}

@Composable
private fun DialogLayout(
    insets: OverlayInsets,
    usePlatformDefaultWidth: Boolean,
    modifier: Modifier,
    bounds: OverlayBounds,
    progress: Float,
    animateOptics: Boolean,
    sharedContentState: SharedContentState?,
    sharedClipShape: Shape,
    containerColor: Color,
    contentColor: Color,
    panelBackdrop: MaterialSurfaceLayer,
    effects: MaterialSurfaceEffects? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Layout(
        content = {
            // The panel spans the bounds a shared animation gives the dialog and draws the backdrop
            // over them, so the backdrop it samples belongs to the panel and moves with it. The
            // content is centered against the dialog and keeps its own layout, so the animation
            // reveals the dialog instead of reflowing its content.
            MaterialPanelSurface(
                shape = sharedClipShape,
                containerColor = containerColor,
                contentColor = contentColor,
                exportedBackdrop = panelBackdrop,
                effects = effects,
                modifier =
                    Modifier.overlaySharedBounds(sharedContentState, sharedClipShape)
                        .overlayBlur(sharedContentState == null && animateOptics)
                        .overlayContentBounds(bounds)
                        // Spatial motion stays separate from the panel's optical layer.
                        .graphicsLayer {
                            val reversedProgress = 1f - progress
                            val scale = 1f - reversedProgress * AnimatedLayerScale
                            scaleX = scale
                            scaleY = scale
                            translationY = AnimatedLayerOffset.toPx() * reversedProgress
                        },
            ) {
                Box(
                    modifier =
                        Modifier.keepSizeWhenLookingAhead()
                            .overlayBlur(sharedContentState != null && animateOptics, fade = false)
                            .then(modifier),
                    content = content,
                )
            }
        },
        modifier = Modifier.fillMaxSize().consumeWindowInsets(insets.paddingValues),
        measurePolicy = dialogMeasurePolicy(insets, usePlatformDefaultWidth),
    )
}

private fun dialogMeasurePolicy(
    insets: OverlayInsets,
    usePlatformDefaultWidth: Boolean,
): MeasurePolicy = MeasurePolicy { measurables, constraints ->
    val platformConstraints = applyPlatformConstraints(constraints, insets, usePlatformDefaultWidth)
    val placeables = measurables.map { it.measure(platformConstraints) }
    val contentWidth = placeables.maxOfOrNull { it.width } ?: constraints.minWidth
    val contentHeight = placeables.maxOfOrNull { it.height } ?: constraints.minHeight

    // When unconstrained, use the content size as the layout dimensions to allow the
    // content's preferred size to be measured.
    val width = if (constraints.hasBoundedWidth) constraints.maxWidth else contentWidth
    val height = if (constraints.hasBoundedHeight) constraints.maxHeight else contentHeight
    layout(width, height) {
        val sizeWithoutInsets = IntSize(width - insets.horizontal, height - insets.vertical)
        val position =
            IntOffset(
                x = insets.left + (sizeWithoutInsets.width - contentWidth) / 2,
                y = insets.top + (sizeWithoutInsets.height - contentHeight) / 2,
            )
        placeables.forEach { it.place(position.x, position.y) }
    }
}

private fun Density.applyPlatformConstraints(
    constraints: Constraints,
    insets: OverlayInsets,
    usePlatformDefaultWidth: Boolean,
): Constraints {
    val platformConstraints = constraints.offset(-insets.horizontal, -insets.vertical)
    // The content is centered against the layout that holds it, so it is measured with its own
    // size: a minimum constraint of that layout never stretches the dialog.
    val contentConstraints = platformConstraints.copy(minWidth = 0, minHeight = 0)
    return if (usePlatformDefaultWidth) {
        contentConstraints.constrain(platformDefaultConstraints(contentConstraints))
    } else {
        contentConstraints
    }
}

private fun Density.platformDefaultConstraints(constraints: Constraints): Constraints {
    // The incoming constraints can already be narrower than the platform default, for example
    // while a shared animation measures the content against animated bounds, so the minimum is
    // clamped together with the maximum; a maximum below the minimum is not a valid constraint.
    val maxWidth = min(preferredDialogWidth(constraints), constraints.maxWidth)
    return constraints.copy(minWidth = min(constraints.minWidth, maxWidth), maxWidth = maxWidth)
}

// Ported from Android: the preferred dialog width is defined by abc_config_prefDialogWidth.
private fun Density.preferredDialogWidth(constraints: Constraints): Int {
    val smallestWidth = min(constraints.maxWidth, constraints.maxHeight).toDp()
    return when {
        smallestWidth >= 600.dp -> 580.dp
        smallestWidth >= 480.dp -> 440.dp
        else -> 320.dp
    }.roundToPx()
}
