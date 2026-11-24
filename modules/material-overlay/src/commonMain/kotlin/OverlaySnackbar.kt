/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults as MaterialButtonDefaults
import androidx.compose.material3.ComposeMaterial3Flags
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.ltfan.m3x.compose.animation.blurTransition
import top.ltfan.m3x.compose.ui.accessibility.SpatialMotionEnabled
import top.ltfan.m3x.material.IconButton
import top.ltfan.m3x.material.MaterialSurfaceEffects
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.TextButton
import top.ltfan.m3x.material.materialSurface
import top.ltfan.m3x.material.rememberSurfaceAppearance
import top.ltfan.m3x.material.theme.extended
import top.ltfan.m3x.overlay.OverlayHandle
import top.ltfan.m3x.overlay.OverlayHost

/** Contains the default values used by the backdrop snackbar components. */
public object OverlaySnackbarDefaults {
    /** The default container color of a snackbar. */
    public val color: Color
        @Composable inline get() = SnackbarDefaults.color

    /** The default content color of a snackbar. */
    public val contentColor: Color
        @Composable inline get() = SnackbarDefaults.contentColor

    /** The default action color of a snackbar. */
    public val actionColor: Color
        @Composable inline get() = SnackbarDefaults.actionColor

    /** The default action content color of a snackbar. */
    public val actionContentColor: Color
        @Composable inline get() = SnackbarDefaults.actionContentColor

    /** The default dismiss action content color of a snackbar. */
    public val dismissActionContentColor: Color
        @Composable inline get() = SnackbarDefaults.dismissActionContentColor

    /** The default shape of a snackbar. */
    public val shape: Shape
        @Composable inline get() = SnackbarDefaults.shape
}

/**
 * Hosts a [SnackbarHostState] above the overlay content: the current snackbar is drawn on a
 * backdrop surface in the lower area of the window, and each snackbar is dismissed after its
 * duration has elapsed.
 *
 * Interactive use requires an enclosing [OverlayHost]. In inspection-mode previews without one, the
 * snackbar is composed in place without host-managed ordering, modal isolation, back dispatch, or
 * backdrop stacking.
 *
 * @param hostState the state of the snackbar queue
 * @param modifier the [Modifier] to be applied to the host
 * @param snackbar the composable that draws a single snackbar
 * @param sharedContentState the state of the shared bounds between the current snackbar and the
 *   page element it grows out of, usually created with `rememberSharedContentState`. When it is
 *   `null` each snackbar keeps its own appearance animation.
 * @param sharedClipShape the shape the shared bounds are clipped to while they are drawn in the
 *   overlay, usually the shape of the snackbar
 */
@Composable
public fun OverlaySnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    effects: MaterialSurfaceEffects? = null,
    snackbar: @Composable (SnackbarData) -> Unit = { OverlaySnackbar(it, effects = effects) },
    sharedContentState: SharedContentState? = null,
    sharedClipShape: Shape = OverlaySnackbarDefaults.shape,
) {
    val currentSnackbarData = hostState.currentSnackbarData
    val accessibilityManager = LocalAccessibilityManager.current
    LaunchedEffect(currentSnackbarData) {
        if (currentSnackbarData != null) {
            val duration =
                currentSnackbarData.visuals.duration.toDuration(
                    currentSnackbarData.visuals.actionLabel != null,
                    accessibilityManager,
                )
            delay(duration)
            currentSnackbarData.dismiss()
        }
    }

    val paneTitle = OverlayStrings.snackbarPaneTitle()
    val entry = remember { OverlayHandle() }

    // The snackbar host stays in the overlay for as long as it is composed, and each snackbar
    // animates in and out inside the entry.
    MaterialOverlay(
        entry,
        modal = false,
        // The entry stays visible while the host is composed. It reads the current snackbar
        // directly in its content. The snackbar and its layer compose together, so a new message
        // appears without waiting for the owner to forward it.
        visible = { true },
        enter = EnterTransition.None,
        exit = ExitTransition.None,
    ) {
        val currentSnackbar = hostState.currentSnackbarData
        Box(
            modifier =
                modifier
                    // The host fills the window so the current snackbar is drawn in the lower
                    // area of the window, above the content of the page.
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.BottomCenter,
        ) {
            OverlaySnackbarTransition(current = currentSnackbar) { data ->
                Box(
                    Modifier.overlaySharedBounds(sharedContentState, sharedClipShape).semantics {
                        // A snackbar that is leaving stays composed until it has faded out, so
                        // only the current message announces itself and offers the dismiss
                        // action: a message that has left is no longer read and cannot be
                        // dismissed again.
                        if (data === currentSnackbar) {
                            liveRegion = LiveRegionMode.Polite
                            this.paneTitle = paneTitle
                            dismiss {
                                data.dismiss()
                                true
                            }
                        }
                    },
                ) {
                    snackbar(data)
                }
            }
        }
    }
}

/**
 * Shows [current] with the structure used by the platform snackbar host. Each snackbar is composed
 * at its own size. Alpha and scale animate on a graphics layer, keeping the layout stable between
 * frames. A new snackbar appears without waiting for a remeasure.
 */
@Composable
private fun OverlaySnackbarTransition(
    current: SnackbarData?,
    content: @Composable (SnackbarData) -> Unit,
) {
    val spatialMotion = SpatialMotionEnabled
    val motionScheme = MaterialTheme.motionScheme
    val items = remember { mutableStateListOf<SnackbarData>() }
    if (current != null && items.none { it === current }) {
        items.add(current)
    }
    Box {
        items.toList().forEach { item ->
            key(item) {
                val visible = item === current
                // The animation starts from the hidden value, so a snackbar that appears is
                // animated into place; an item that leaves is removed once it has faded out.
                val alpha = remember { Animatable(0f) }
                val scale = remember { Animatable(0.8f) }
                LaunchedEffect(visible, spatialMotion) {
                    if (visible) {
                        // Both properties animate at the same time, so a snackbar never rests
                        // in
                        // its small state while one of them finishes first.
                        coroutineScope {
                            launch { alpha.animateTo(1f, motionScheme.fastEffectsSpec()) }
                            launch {
                                if (spatialMotion)
                                    scale.animateTo(1f, motionScheme.fastSpatialSpec())
                                else scale.snapTo(1f)
                            }
                        }
                    } else {
                        coroutineScope {
                            launch { alpha.animateTo(0f, motionScheme.fastEffectsSpec()) }
                            launch {
                                if (spatialMotion)
                                    scale.animateTo(0.8f, motionScheme.fastSpatialSpec())
                                else scale.snapTo(1f)
                            }
                        }
                        items.remove(item)
                    }
                }
                Box(
                    Modifier.blurTransition(
                            { alpha.value },
                            maxRadius = MaterialTheme.extended.enterExitBlurRadius,
                        )
                        .graphicsLayer {
                            scaleX = if (spatialMotion) scale.value else 1f
                            scaleY = if (spatialMotion) scale.value else 1f
                        },
                ) {
                    content(item)
                }
            }
        }
    }
}

/**
 * A snackbar that shows a [SnackbarData] message with optional action and dismiss controls. The
 * selected surface renderer determines its container treatment.
 *
 * @param snackbarData the data of the snackbar
 * @param modifier the [Modifier] to be applied to the snackbar
 * @param actionOnNewLine whether the action is placed on its own line below the message
 * @param shape the shape of the snackbar
 * @param containerColor the container color of the snackbar
 * @param contentColor the content color of the snackbar
 * @param actionColor the color of the action label
 * @param actionContentColor the content color of the action
 * @param dismissActionContentColor the content color of the dismiss action
 * @param effects optional renderer-specific effects applied to the snackbar surface
 */
@Composable
public fun OverlaySnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    actionOnNewLine: Boolean = false,
    shape: Shape = OverlaySnackbarDefaults.shape,
    containerColor: Color = OverlaySnackbarDefaults.color,
    contentColor: Color = OverlaySnackbarDefaults.contentColor,
    actionColor: Color = OverlaySnackbarDefaults.actionColor,
    actionContentColor: Color = OverlaySnackbarDefaults.actionContentColor,
    dismissActionContentColor: Color = OverlaySnackbarDefaults.dismissActionContentColor,
    effects: MaterialSurfaceEffects? = null,
) {
    val actionLabel = snackbarData.visuals.actionLabel
    val actionComposable: (@Composable () -> Unit)? =
        if (actionLabel != null) {
            @Composable {
                TextButton(
                    colors = MaterialButtonDefaults.textButtonColors(contentColor = actionColor),
                    onClick = { snackbarData.performAction() },
                    content = { Text(actionLabel) },
                )
            }
        } else {
            null
        }
    val dismissActionComposable: (@Composable () -> Unit)? =
        if (snackbarData.visuals.withDismissAction) {
            @Composable {
                val dismissLabel = OverlayStrings.snackbarDismiss()
                OverlayTooltipBox(
                    tooltip = { OverlayPlainTooltip { Text(dismissLabel) } },
                    state = rememberOverlayTooltipState(),
                ) {
                    IconButton(
                        onClick = { snackbarData.dismiss() },
                        content = {
                            Icon(Icons.Filled.Close, contentDescription = dismissLabel)
                        },
                    )
                }
            }
        } else {
            null
        }

    OverlaySnackbar(
        modifier = modifier.padding(12.dp),
        action = actionComposable,
        dismissAction = dismissActionComposable,
        actionOnNewLine = actionOnNewLine,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        actionContentColor = actionContentColor,
        dismissActionContentColor = dismissActionContentColor,
        effects = effects,
        content = { Text(snackbarData.visuals.message) },
    )
}

/**
 * A snackbar with custom content and a container drawn by the selected surface renderer.
 *
 * @param modifier the [Modifier] to be applied to the snackbar
 * @param action the action of the snackbar
 * @param dismissAction the dismiss action of the snackbar
 * @param actionOnNewLine whether the action is placed on its own line below the message
 * @param shape the shape of the snackbar
 * @param containerColor the container color of the snackbar
 * @param contentColor the content color of the snackbar
 * @param actionContentColor the content color of the action
 * @param dismissActionContentColor the content color of the dismiss action
 * @param effects optional renderer-specific effects applied to the snackbar surface
 * @param content the message of the snackbar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun OverlaySnackbar(
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
    dismissAction: @Composable (() -> Unit)? = null,
    actionOnNewLine: Boolean = false,
    shape: Shape = OverlaySnackbarDefaults.shape,
    containerColor: Color = OverlaySnackbarDefaults.color,
    contentColor: Color = OverlaySnackbarDefaults.contentColor,
    actionContentColor: Color = OverlaySnackbarDefaults.actionContentColor,
    dismissActionContentColor: Color = OverlaySnackbarDefaults.dismissActionContentColor,
    effects: MaterialSurfaceEffects? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = Color.Transparent,
        contentColor = contentColor,
    ) {
        val appearance =
            rememberSurfaceAppearance(
                MaterialSurfaceRole.InversePanel,
                containerColor = containerColor,
                contentColor = contentColor,
                effects = effects,
                shape = shape,
            )
        Box(Modifier.materialSurface(appearance, shape)) {
            val textStyle = MaterialTheme.typography.bodyMedium
            val actionTextStyle = MaterialTheme.typography.labelLarge
            CompositionLocalProvider(
                LocalTextStyle provides textStyle,
                LocalContentColor provides appearance.contentColor,
            ) {
                if (actionOnNewLine && action != null) {
                    if (ComposeMaterial3Flags.isSnackbarStylingFixEnabled) {
                        NewLineButtonSnackbar(
                            text = content,
                            action = action,
                            dismissAction = dismissAction,
                            actionTextStyle = actionTextStyle,
                            actionContentColor = actionContentColor,
                            dismissActionContentColor = dismissActionContentColor,
                        )
                    } else {
                        LegacyNewLineButtonSnackbar(
                            text = content,
                            action = action,
                            dismissAction = dismissAction,
                            actionTextStyle = actionTextStyle,
                            actionContentColor = actionContentColor,
                            dismissActionContentColor = dismissActionContentColor,
                        )
                    }
                } else {
                    if (ComposeMaterial3Flags.isSnackbarStylingFixEnabled) {
                        OneRowSnackbar(
                            text = content,
                            action = action,
                            dismissAction = dismissAction,
                            actionTextStyle = actionTextStyle,
                            actionTextColor = actionContentColor,
                            dismissActionColor = dismissActionContentColor,
                        )
                    } else {
                        LegacyOneRowSnackbar(
                            text = content,
                            action = action,
                            dismissAction = dismissAction,
                            actionTextStyle = actionTextStyle,
                            actionTextColor = actionContentColor,
                            dismissActionColor = dismissActionContentColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegacyNewLineButtonSnackbar(
    text: @Composable () -> Unit,
    action: @Composable () -> Unit,
    dismissAction: @Composable (() -> Unit)?,
    actionTextStyle: TextStyle,
    actionContentColor: Color,
    dismissActionContentColor: Color,
) {
    Column(
        modifier =
            Modifier
                // Fill max width, up to ContainerMaxWidth.
                .widthIn(max = ContainerMaxWidth)
                .fillMaxWidth()
                .padding(start = HorizontalSpacing, bottom = SeparateButtonExtraY),
    ) {
        Box(
            Modifier.paddingFromBaseline(HeightToFirstLine, LongButtonVerticalOffset)
                .padding(end = HorizontalSpacingButtonSide),
        ) {
            text()
        }

        Box(
            Modifier.align(Alignment.End)
                .padding(end = if (dismissAction == null) HorizontalSpacingButtonSide else 0.dp),
        ) {
            Row {
                CompositionLocalProvider(
                    LocalContentColor provides actionContentColor,
                    LocalTextStyle provides actionTextStyle,
                    content = action,
                )
                if (dismissAction != null) {
                    CompositionLocalProvider(
                        LocalContentColor provides dismissActionContentColor,
                        content = dismissAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun LegacyOneRowSnackbar(
    text: @Composable () -> Unit,
    action: @Composable (() -> Unit)?,
    dismissAction: @Composable (() -> Unit)?,
    actionTextStyle: TextStyle,
    actionTextColor: Color,
    dismissActionColor: Color,
) {
    val textTag = "text"
    val actionTag = "action"
    val dismissActionTag = "dismissAction"
    Layout(
        {
            Box(Modifier.layoutId(textTag).padding(vertical = LegacySnackbarVerticalPadding)) {
                text()
            }
            if (action != null) {
                Box(Modifier.layoutId(actionTag)) {
                    CompositionLocalProvider(
                        LocalContentColor provides actionTextColor,
                        LocalTextStyle provides actionTextStyle,
                        content = action,
                    )
                }
            }
            if (dismissAction != null) {
                Box(Modifier.layoutId(dismissActionTag)) {
                    CompositionLocalProvider(
                        LocalContentColor provides dismissActionColor,
                        content = dismissAction,
                    )
                }
            }
        },
        modifier =
            Modifier.padding(
                start = HorizontalSpacing,
                end = if (dismissAction == null) HorizontalSpacingButtonSide else 0.dp,
            ),
    ) { measurables, constraints ->
        val containerWidth = min(constraints.maxWidth, ContainerMaxWidth.roundToPx())
        val actionButtonPlaceable =
            measurables.fastFirstOrNull { it.layoutId == actionTag }?.measure(constraints)
        val dismissButtonPlaceable =
            measurables.fastFirstOrNull { it.layoutId == dismissActionTag }?.measure(constraints)
        val actionButtonWidth = actionButtonPlaceable?.width ?: 0
        val actionButtonHeight = actionButtonPlaceable?.height ?: 0
        val dismissButtonWidth = dismissButtonPlaceable?.width ?: 0
        val dismissButtonHeight = dismissButtonPlaceable?.height ?: 0
        val extraSpacingWidth = if (dismissButtonWidth == 0) TextEndExtraSpacing.roundToPx() else 0
        val textMaxWidth =
            (containerWidth - actionButtonWidth - dismissButtonWidth - extraSpacingWidth)
                .coerceAtLeast(constraints.minWidth)
        val textPlaceable =
            measurables
                .fastFirst { it.layoutId == textTag }
                .measure(constraints.copy(minHeight = 0, maxWidth = textMaxWidth))

        val firstTextBaseline = textPlaceable[FirstBaseline]
        val lastTextBaseline = textPlaceable[LastBaseline]
        val hasText =
            firstTextBaseline != AlignmentLine.Unspecified &&
                lastTextBaseline != AlignmentLine.Unspecified
        val isOneLine = firstTextBaseline == lastTextBaseline || !hasText
        val dismissButtonPlaceX = containerWidth - dismissButtonWidth
        val actionButtonPlaceX = dismissButtonPlaceX - actionButtonWidth

        val textPlaceY: Int
        val containerHeight: Int
        val actionButtonPlaceY: Int
        if (isOneLine) {
            val minContainerHeight = SingleLineContainerHeight.roundToPx()
            val contentHeight = max(actionButtonHeight, dismissButtonHeight)
            containerHeight = max(minContainerHeight, contentHeight)
            textPlaceY = (containerHeight - textPlaceable.height) / 2
            actionButtonPlaceY =
                if (actionButtonPlaceable != null) {
                    actionButtonPlaceable[FirstBaseline].let {
                        if (it != AlignmentLine.Unspecified) {
                            textPlaceY + firstTextBaseline - it
                        } else {
                            0
                        }
                    }
                } else {
                    0
                }
        } else {
            val baselineOffset = HeightToFirstLine.roundToPx()
            textPlaceY = baselineOffset - firstTextBaseline
            val minContainerHeight = TwoLinesContainerHeight.roundToPx()
            val contentHeight = textPlaceY + textPlaceable.height
            containerHeight = max(minContainerHeight, contentHeight)
            actionButtonPlaceY =
                if (actionButtonPlaceable != null) {
                    (containerHeight - actionButtonPlaceable.height) / 2
                } else {
                    0
                }
        }
        val dismissButtonPlaceY =
            if (dismissButtonPlaceable != null) {
                (containerHeight - dismissButtonPlaceable.height) / 2
            } else {
                0
            }

        layout(containerWidth, containerHeight) {
            textPlaceable.placeRelative(0, textPlaceY)
            actionButtonPlaceable?.placeRelative(actionButtonPlaceX, actionButtonPlaceY)
            dismissButtonPlaceable?.placeRelative(dismissButtonPlaceX, dismissButtonPlaceY)
        }
    }
}

@Composable
private fun NewLineButtonSnackbar(
    text: @Composable () -> Unit,
    action: @Composable () -> Unit,
    dismissAction: @Composable (() -> Unit)?,
    actionTextStyle: TextStyle,
    actionContentColor: Color,
    dismissActionContentColor: Color,
) {
    Column(
        modifier =
            Modifier.widthIn(max = ContainerMaxWidth)
                .fillMaxWidth()
                .padding(start = HorizontalSpacing),
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(vertical = SnackbarVerticalPadding)
                    .padding(end = HorizontalSpacing),
        ) {
            text()
        }

        Row(
            Modifier.align(Alignment.End)
                .padding(
                    bottom = ActionButtonBottomPadding,
                    end = if (dismissAction == null) HorizontalSpacingButtonSide else 0.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides actionContentColor,
                LocalTextStyle provides actionTextStyle,
                content = action,
            )
            if (dismissAction != null) {
                CompositionLocalProvider(
                    LocalContentColor provides dismissActionContentColor,
                    content = dismissAction,
                )
            }
        }
    }
}

@Composable
private fun OneRowSnackbar(
    text: @Composable () -> Unit,
    action: @Composable (() -> Unit)?,
    dismissAction: @Composable (() -> Unit)?,
    actionTextStyle: TextStyle,
    actionTextColor: Color,
    dismissActionColor: Color,
) {
    val textTag = "text"
    val actionTag = "action"
    val dismissActionTag = "dismissAction"
    Layout(
        {
            Box(Modifier.layoutId(textTag).padding(vertical = SnackbarVerticalPadding)) {
                text()
            }
            if (action != null) {
                Box(Modifier.layoutId(actionTag)) {
                    CompositionLocalProvider(
                        LocalContentColor provides actionTextColor,
                        LocalTextStyle provides actionTextStyle,
                        content = action,
                    )
                }
            }
            if (dismissAction != null) {
                Box(Modifier.layoutId(dismissActionTag)) {
                    CompositionLocalProvider(
                        LocalContentColor provides dismissActionColor,
                        content = dismissAction,
                    )
                }
            }
        },
        modifier =
            Modifier.padding(
                start = HorizontalSpacing,
                end = if (dismissAction == null) TextEndExtraSpacing else 0.dp,
            ),
    ) { measurables, constraints ->
        val minContainerHeight = SingleLineContainerHeight.roundToPx()
        val containerWidth = min(constraints.maxWidth, ContainerMaxWidth.roundToPx())
        val actionButtonPlaceable =
            measurables.fastFirstOrNull { it.layoutId == actionTag }?.measure(constraints)
        val dismissButtonPlaceable =
            measurables.fastFirstOrNull { it.layoutId == dismissActionTag }?.measure(constraints)
        val actionButtonWidth = actionButtonPlaceable?.width ?: 0
        val dismissButtonWidth = dismissButtonPlaceable?.width ?: 0
        val extraSpacingWidth = if (dismissButtonWidth == 0) TextEndExtraSpacing.roundToPx() else 0
        val textMaxWidth =
            (containerWidth - actionButtonWidth - dismissButtonWidth - extraSpacingWidth)
                .coerceAtLeast(constraints.minWidth)
        val textPlaceable =
            measurables
                .fastFirst { it.layoutId == textTag }
                .measure(constraints.copy(minHeight = 0, maxWidth = textMaxWidth))

        val containerHeight =
            maxOf(
                minContainerHeight,
                textPlaceable.height,
                actionButtonPlaceable?.height ?: 0,
                dismissButtonPlaceable?.height ?: 0,
            )

        val dismissButtonPlaceX = containerWidth - dismissButtonWidth
        val actionButtonPlaceX = dismissButtonPlaceX - actionButtonWidth

        layout(containerWidth, containerHeight) {
            textPlaceable.placeRelative(0, (containerHeight - textPlaceable.height) / 2)
            actionButtonPlaceable?.placeRelative(
                actionButtonPlaceX,
                (containerHeight - actionButtonPlaceable.height) / 2,
            )
            dismissButtonPlaceable?.placeRelative(
                dismissButtonPlaceX,
                (containerHeight - dismissButtonPlaceable.height) / 2,
            )
        }
    }
}

/**
 * Converts the [SnackbarDuration] of this snackbar to the time it is shown, taking the recommended
 * timeout of the accessibility manager into account.
 *
 * @param hasAction whether the snackbar has an action
 * @param accessibilityManager the accessibility manager of the platform, if any
 */
internal fun SnackbarDuration.toDuration(
    hasAction: Boolean,
    accessibilityManager: AccessibilityManager?,
): Duration {
    val original =
        when (this) {
            SnackbarDuration.Indefinite -> Duration.INFINITE
            SnackbarDuration.Long -> 10000.milliseconds
            SnackbarDuration.Short -> 4000.milliseconds
        }
    if (accessibilityManager == null) {
        return original
    }
    return accessibilityManager
        .calculateRecommendedTimeoutMillis(
            original.inWholeMilliseconds,
            containsIcons = true,
            containsText = true,
            containsControls = hasAction,
        )
        .milliseconds
}

private val ContainerMaxWidth = 600.dp
private val HeightToFirstLine = 30.dp
private val HorizontalSpacing = 16.dp
private val HorizontalSpacingButtonSide = 8.dp
private val SeparateButtonExtraY = 2.dp
private val TextEndExtraSpacing = 8.dp
private val LongButtonVerticalOffset = 12.dp

// Hosted snackbar content uses Material's internal spacing and container dimensions.
private val LegacySnackbarVerticalPadding = 6.dp
private val SnackbarVerticalPadding = 14.dp
private val ActionButtonBottomPadding = 4.dp
private val SingleLineContainerHeight = 48.dp
private val TwoLinesContainerHeight = 68.dp
