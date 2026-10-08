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

import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonDefaults as MaterialButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.takeOrElse
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import kotlin.math.max
import top.ltfan.m3x.material.MaterialSurfaceEffects
import top.ltfan.m3x.overlay.OverlayHost

/**
 * Material alert dialog with a backdrop container.
 *
 * Interactive use requires an enclosing [OverlayHost]. In inspection-mode previews without one, the
 * overlay is composed in place without host-managed ordering, modal isolation, back dispatch, or
 * backdrop stacking.
 *
 * @param onDismissRequest called when the user tries to dismiss the dialog by clicking outside or
 *   pressing the back button. This is not called when the dismiss button is clicked.
 * @param confirmButton button which is meant to confirm a proposed action, thus resolving what
 *   triggered the dialog. The dialog does not set up any events for this button so they need to be
 *   set up by the caller.
 * @param modifier the [Modifier] to be applied to this dialog
 * @param dismissButton button which is meant to dismiss the dialog. The dialog does not set up any
 *   events for this button so they need to be set up by the caller.
 * @param icon optional icon that will appear above the [title] or above the [text], in case a title
 *   was not provided.
 * @param title title which should specify the purpose of the dialog. The title is not mandatory,
 *   because there may be sufficient information inside the [text].
 * @param text text which presents the details regarding the dialog's purpose.
 * @param shape defines the shape of this dialog's container
 * @param containerColor the color used for the background of this dialog
 * @param iconContentColor the content color used for the icon
 * @param titleContentColor the content color used for the title
 * @param textContentColor the content color used for the text
 * @param tonalElevation when [containerColor] is the theme surface color, a translucent primary
 *   color overlay is applied on top of the container
 * @param visible reads whether the dialog is shown, usually `visible = { state }`
 * @param properties the platform behavior of the dialog
 */
@Composable
public fun OverlayAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    visible: () -> Boolean = { true },
    sharedContentState: SharedContentState? = null,
    properties: OverlayDialogProperties = OverlayDialogProperties(),
    effects: MaterialSurfaceEffects? = null,
) {
    // The surface color of the dialog is resolved here, so the backdrop the dialog draws and the
    // content colors below agree on it.
    val surfaceColor =
        if (containerColor == MaterialTheme.colorScheme.surface) {
            MaterialTheme.colorScheme.surfaceColorAtElevation(tonalElevation)
        } else {
            containerColor
        }
    OverlayBasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        visible = visible,
        sharedContentState = sharedContentState,
        properties = properties,
        shape = shape,
        containerColor = surfaceColor,
        effects = effects,
    ) {
        OverlayAlertDialogContent(
            buttons = {
                OverlayAlertDialogButtons(confirmButton, dismissButton)
            },
            icon = icon,
            title = title,
            text = text,
            // The button content color comes from the dialog's action label token, and
            // TextButtons used for dismiss and confirm buttons apply their own colors.
            buttonContentColor = MaterialTheme.colorScheme.primary,
            iconContentColor = iconContentColor,
            titleContentColor = titleContentColor,
            textContentColor = textContentColor,
        )
    }
}

/**
 * Basic alert dialog with a renderer-selected container. The container can be replaced with
 * [LocalOverlayBasicAlertDialogOverride].
 *
 * Interactive use requires an enclosing [OverlayHost]. In inspection-mode previews without one, the
 * overlay is composed in place without host-managed ordering, modal isolation, back dispatch, or
 * backdrop stacking.
 *
 * @param onDismissRequest called when the user tries to dismiss the dialog by clicking outside or
 *   pressing the back button
 * @param modifier the [Modifier] to be applied to this dialog's content
 * @param visible reads whether the dialog is shown, usually `visible = { state }`
 * @param properties the platform behavior of the dialog
 * @param shape the shape of the dialog panel
 * @param containerColor the container color of the dialog panel
 * @param content the content of the dialog
 */
@Composable
public fun OverlayBasicAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    visible: () -> Boolean = { true },
    sharedContentState: SharedContentState? = null,
    properties: OverlayDialogProperties = OverlayDialogProperties(),
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    effects: MaterialSurfaceEffects? = null,
    content: @Composable () -> Unit,
) {
    with(LocalOverlayBasicAlertDialogOverride.current) {
        OverlayBasicAlertDialogOverrideScope(
                onDismissRequest = onDismissRequest,
                modifier = modifier,
                visible = visible,
                sharedContentState = sharedContentState,
                properties = properties,
                shape = shape,
                containerColor = containerColor,
                effects = effects,
                content = content,
            )
            .OverlayBasicAlertDialog()
    }
}

private object DefaultOverlayBasicAlertDialogOverride : OverlayBasicAlertDialogOverride {
    @Composable
    override fun OverlayBasicAlertDialogOverrideScope.OverlayBasicAlertDialog() {
        val dialogPaneDescription = OverlayStrings.dialog()
        OverlayDialog(
            onDismissRequest = onDismissRequest,
            visible = visible,
            sharedContentState = sharedContentState,
            properties = properties,
            sharedClipShape = shape,
            containerColor = containerColor,
            effects = effects,
        ) {
            Box(
                modifier =
                    modifier
                        .sizeIn(minWidth = DialogMinWidth, maxWidth = DialogMaxWidth)
                        .semantics { paneTitle = dialogPaneDescription },
                propagateMinConstraints = true,
            ) {
                content()
            }
        }
    }
}

/**
 * Interface that allows libraries to override the behavior of the [OverlayBasicAlertDialog]
 * component.
 *
 * To override this component, implement the member function of this interface, then provide the
 * implementation to [LocalOverlayBasicAlertDialogOverride] in the Compose hierarchy.
 */
public interface OverlayBasicAlertDialogOverride {
    /** Behavior function that is called by the [OverlayBasicAlertDialog] component. */
    @Composable public fun OverlayBasicAlertDialogOverrideScope.OverlayBasicAlertDialog()
}

/**
 * Parameters available to [OverlayBasicAlertDialog].
 *
 * @property onDismissRequest called when the user tries to dismiss the dialog by clicking outside
 *   or pressing the back button
 * @property modifier the [Modifier] to be applied to this dialog's content
 * @property visible reads whether the dialog is shown
 * @property properties the platform behavior of the dialog
 * @property shape the shape of the dialog panel
 * @property containerColor the container color of the dialog panel. An override that draws no panel
 *   passes `Color.Unspecified` to the dialog instead
 * @property content the content of the dialog
 */
public class OverlayBasicAlertDialogOverrideScope
internal constructor(
    public val onDismissRequest: () -> Unit,
    public val modifier: Modifier = Modifier,
    public val visible: () -> Boolean = { true },
    public val sharedContentState: SharedContentState? = null,
    public val properties: OverlayDialogProperties = OverlayDialogProperties(),
    public val shape: Shape,
    public val containerColor: Color,
    public val effects: MaterialSurfaceEffects?,
    public val content: @Composable () -> Unit,
)

/** CompositionLocal containing the currently-selected [OverlayBasicAlertDialogOverride]. */
public val LocalOverlayBasicAlertDialogOverride:
    ProvidableCompositionLocal<OverlayBasicAlertDialogOverride> =
    compositionLocalOf {
        DefaultOverlayBasicAlertDialogOverride
    }

/**
 * Material basic-dialog slots with 24dp outer padding, 16dp below the title and 24dp below the
 * body. The hosting dialog owns its surface, size constraints and shared transition.
 *
 * @param buttons Dialog action content.
 * @param modifier Modifier applied to the slot layout.
 * @param icon Optional content placed above the title.
 * @param title Optional title content.
 * @param text Optional body content.
 * @param buttonContentColor Content color provided to the action slot.
 * @param iconContentColor Content color provided to the icon slot.
 * @param titleContentColor Content color provided to the title slot.
 * @param textContentColor Content color provided to the body slot.
 */
@Composable
public fun OverlayAlertDialogContent(
    buttons: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)?,
    title: (@Composable () -> Unit)?,
    text: @Composable (() -> Unit)?,
    buttonContentColor: Color,
    iconContentColor: Color,
    titleContentColor: Color,
    textContentColor: Color,
) {
    // The backdrop panel and the shared bounds of this content are applied by the dialog that hosts
    // it,
    // so the panel follows a shared animation and this content keeps its own layout.
    Column(modifier = modifier.padding(DialogPadding)) {
        icon?.let {
            CompositionLocalProvider(LocalContentColor provides iconContentColor) {
                Box(Modifier.padding(IconPadding).align(Alignment.CenterHorizontally)) {
                    it()
                }
            }
        }
        title?.let {
            ProvideContentColorTextStyle(
                contentColor = titleContentColor,
                textStyle = MaterialTheme.typography.headlineSmall,
            ) {
                Box(
                    // The title is centered when an icon is present.
                    Modifier.padding(TitlePadding)
                        .align(
                            if (icon == null) {
                                Alignment.Start
                            } else {
                                Alignment.CenterHorizontally
                            },
                        ),
                ) {
                    it()
                }
            }
        }
        text?.let {
            ProvideContentColorTextStyle(
                contentColor = textContentColor,
                textStyle = MaterialTheme.typography.bodyMedium,
            ) {
                Box(
                    Modifier.weight(weight = 1f, fill = false)
                        .padding(TextPadding)
                        .align(Alignment.Start),
                ) {
                    it()
                }
            }
        }
        Box(modifier = Modifier.align(Alignment.End)) {
            ProvideContentColorTextStyle(
                contentColor = buttonContentColor,
                textStyle = MaterialTheme.typography.labelLarge,
                content = buttons,
            )
        }
    }
}

/**
 * End-aligned dialog actions with 8dp spacing and wrapping for constrained widths.
 *
 * @param confirmButton Required confirmation action.
 * @param dismissButton Optional dismiss action displayed before the confirmation action.
 */
@Composable
public fun OverlayAlertDialogButtons(
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
) {
    val buttonPadding =
        LocalMinimumInteractiveComponentSize.current.takeOrElse { 0.dp } -
            MaterialButtonDefaults.MinHeight
    OverlayAlertDialogFlowRow(
        mainAxisSpacing = ButtonsMainAxisSpacing,
        crossAxisSpacing =
            (ButtonsCrossAxisSpacing - buttonPadding).coerceIn(0.dp, ButtonsCrossAxisSpacing),
    ) {
        dismissButton?.invoke()
        confirmButton()
    }
}

/**
 * Arranges [content] in rows that fit the available width, with every row aligned to the end of the
 * dialog, so confirming actions appear above dismissive actions.
 */
@Composable
internal fun OverlayAlertDialogFlowRow(
    mainAxisSpacing: Dp,
    crossAxisSpacing: Dp,
    content: @Composable () -> Unit,
) {
    Layout(content) { measurables, constraints ->
        val sequences = mutableListOf<List<Placeable>>()
        val crossAxisSizes = mutableListOf<Int>()
        val crossAxisPositions = mutableListOf<Int>()

        var mainAxisSpace = 0
        var crossAxisSpace = 0

        val currentSequence = mutableListOf<Placeable>()
        var currentMainAxisSize = 0
        var currentCrossAxisSize = 0

        // Return whether the placeable can be added to the current sequence.
        fun canAddToCurrentSequence(placeable: Placeable) =
            currentSequence.isEmpty() ||
                currentMainAxisSize + mainAxisSpacing.roundToPx() + placeable.width <=
                    constraints.maxWidth

        // Store current sequence information and start a new sequence.
        fun startNewSequence() {
            if (sequences.isNotEmpty()) {
                crossAxisSpace += crossAxisSpacing.roundToPx()
            }
            // Ensures that confirming actions appear above dismissive actions.
            @Suppress("ListIterator") sequences.add(0, currentSequence.toList())
            crossAxisSizes += currentCrossAxisSize
            crossAxisPositions += crossAxisSpace

            crossAxisSpace += currentCrossAxisSize
            mainAxisSpace = max(mainAxisSpace, currentMainAxisSize)

            currentSequence.clear()
            currentMainAxisSize = 0
            currentCrossAxisSize = 0
        }

        measurables.fastForEach { measurable ->
            // Ask the child for its preferred size.
            val placeable = measurable.measure(constraints)

            // Start a new sequence if there is not enough space.
            if (!canAddToCurrentSequence(placeable)) startNewSequence()

            // Add the child to the current sequence.
            if (currentSequence.isNotEmpty()) {
                currentMainAxisSize += mainAxisSpacing.roundToPx()
            }
            currentSequence.add(placeable)
            currentMainAxisSize += placeable.width
            currentCrossAxisSize = max(currentCrossAxisSize, placeable.height)
        }

        if (currentSequence.isNotEmpty()) startNewSequence()

        val mainAxisLayoutSize = max(mainAxisSpace, constraints.minWidth)

        val crossAxisLayoutSize = max(crossAxisSpace, constraints.minHeight)

        layout(mainAxisLayoutSize, crossAxisLayoutSize) {
            sequences.fastForEachIndexed { i, placeables ->
                val childrenMainAxisSizes =
                    IntArray(placeables.size) { j ->
                        placeables[j].width +
                            if (j < placeables.lastIndex) mainAxisSpacing.roundToPx() else 0
                    }
                val arrangement = Arrangement.End
                val mainAxisPositions = IntArray(childrenMainAxisSizes.size)
                with(arrangement) {
                    arrange(
                        mainAxisLayoutSize,
                        childrenMainAxisSizes,
                        layoutDirection,
                        mainAxisPositions,
                    )
                }
                placeables.fastForEachIndexed { j, placeable ->
                    placeable.place(x = mainAxisPositions[j], y = crossAxisPositions[i])
                }
            }
        }
    }
}

/** Provides [contentColor] and [textStyle] to the given [content]. */
@Composable
internal fun ProvideContentColorTextStyle(
    contentColor: Color,
    textStyle: TextStyle,
    content: @Composable () -> Unit,
) {
    val mergedStyle = LocalTextStyle.current.merge(textStyle)
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalTextStyle provides mergedStyle,
        content = content,
    )
}

internal val DialogMinWidth = 280.dp
internal val DialogMaxWidth = 560.dp

private val ButtonsMainAxisSpacing = 8.dp

// Material's dialog layout keeps cross-axis button spacing internal.
private val ButtonsCrossAxisSpacing = 8.dp

// Hosted dialog content uses Material's ordinary-pointer insets.
private val DialogPadding = PaddingValues(all = 24.dp)
private val IconPadding = PaddingValues(bottom = 16.dp)
private val TitlePadding = PaddingValues(bottom = 16.dp)
private val TextPadding = PaddingValues(bottom = 24.dp)
