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

package top.ltfan.m3x.material

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider as MaterialRangeSlider
import androidx.compose.material3.RangeSliderState
import androidx.compose.material3.Slider as MaterialSlider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults as MaterialSliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.VerticalSlider as MaterialVerticalSlider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import top.ltfan.m3x.material.theme.extended

/**
 * [Material Design slider](https://m3.material.io/components/sliders/overview)
 *
 * Sliders allow users to make selections from a range of values.
 *
 * Sliders reflect a range of values along a horizontal bar, from which users may select a single
 * value. They are ideal for adjusting settings such as volume, brightness, or applying image
 * filters.
 *
 * Continuous sliders allow any value within the state's range. Configure [SliderState.steps] to
 * restrict selection to evenly spaced values between the endpoints.
 *
 * @param state [SliderState] which contains the slider's current value.
 * @param onValueChange receives proposed values to update the state.
 * @param modifier the [Modifier] to be applied to this slider
 * @param enabled controls the enabled state of this slider. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param onValueChangeFinished called when the interaction finishes.
 * @param colors [SliderColors] that will be used to resolve the colors used for this slider in
 *   different states. See [MaterialSliderDefaults.colors].
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 *   for this slider. You can create and pass in your own `remember`ed instance to observe
 *   [Interaction]s and customize the appearance / behavior of this slider in different states.
 * @param thumb the thumb to be displayed on the slider, it is placed on top of the track. The
 *   lambda receives a [SliderState] which is used to obtain the current active track.
 * @param track the track to be displayed on the slider, it is placed underneath the thumb. The
 *   lambda receives a [SliderState] which is used to obtain the current active track.
 */
@Composable
public fun Slider(
    state: SliderState,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = MaterialSliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    thumb: @Composable (SliderState) -> Unit = {
        SliderDefaults.Thumb(
            interactionSource = interactionSource,
            colors = colors,
            enabled = enabled,
        )
    },
    track: @Composable (SliderState) -> Unit = { sliderState ->
        MaterialSliderDefaults.Track(
            colors = colors,
            enabled = enabled,
            sliderState = sliderState,
            trackCornerSize = Dp.Unspecified,
        )
    },
): Unit =
    MaterialSlider(
        state = state,
        modifier = modifier,
        enabled = enabled,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        interactionSource = interactionSource,
        thumb = thumb,
        track = track,
    )

/**
 * [Material Design range slider](https://m3.material.io/components/sliders/overview)
 *
 * Range Sliders expand upon [Slider] using the same concepts but allow the user to select 2 values.
 *
 * The two values are still bounded by the value range but they also cannot cross each other.
 *
 * It uses the provided startThumb for the slider's start thumb and endThumb for the slider's end
 * thumb. It also uses the provided track for the slider's track. If nothing is passed for these
 * parameters, it will use [SliderDefaults.Thumb] and [MaterialSliderDefaults.Track] for the thumbs
 * and track.
 *
 * Continuous range sliders allow any values within the state's range. Configure
 * [RangeSliderState.steps] to restrict selection to evenly spaced values between the endpoints.
 *
 * @param state [RangeSliderState] which contains the current values of the RangeSlider.
 * @param onValueChange receives proposed values for the caller to assign to [state].
 * @param modifier modifiers for the Range Slider layout
 * @param enabled whether this component is enabled and accepts user input
 * @param onValueChangeFinished called when the user finishes changing the range.
 * @param colors [SliderColors] that will be used to determine the color of the Range Slider parts
 *   in different state. See [MaterialSliderDefaults.colors] to customize.
 * @param startThumbInteractionSource the [MutableInteractionSource] representing the stream of
 *   [Interaction]s for the start thumb. You can create and pass in your own `remember`ed instance
 *   to observe.
 * @param endThumbInteractionSource the [MutableInteractionSource] representing the stream of
 *   [Interaction]s for the end thumb. You can create and pass in your own `remember`ed instance to
 *   observe.
 * @param startThumb the start thumb to be displayed on the Range Slider. The lambda receives a
 *   [RangeSliderState] which is used to obtain the current active track.
 * @param endThumb the end thumb to be displayed on the Range Slider. The lambda receives a
 *   [RangeSliderState] which is used to obtain the current active track.
 * @param track the track to be displayed on the range slider, it is placed underneath the thumb.
 *   The lambda receives a [RangeSliderState] which is used to obtain the current active track.
 */
@Composable
public fun RangeSlider(
    state: RangeSliderState,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = MaterialSliderDefaults.colors(),
    startThumbInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    endThumbInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    startThumb: @Composable (RangeSliderState) -> Unit = {
        SliderDefaults.Thumb(
            interactionSource = startThumbInteractionSource,
            colors = colors,
            enabled = enabled,
        )
    },
    endThumb: @Composable (RangeSliderState) -> Unit = {
        SliderDefaults.Thumb(
            interactionSource = endThumbInteractionSource,
            colors = colors,
            enabled = enabled,
        )
    },
    track: @Composable (RangeSliderState) -> Unit = { rangeSliderState ->
        MaterialSliderDefaults.Track(
            colors = colors,
            enabled = enabled,
            rangeSliderState = rangeSliderState,
            trackCornerSize = Dp.Unspecified,
        )
    },
): Unit =
    MaterialRangeSlider(
        state = state,
        modifier = modifier,
        enabled = enabled,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        startThumbInteractionSource = startThumbInteractionSource,
        endThumbInteractionSource = endThumbInteractionSource,
        startThumb = startThumb,
        endThumb = endThumb,
        track = track,
    )

/**
 * [Material Design slider](https://m3.material.io/components/sliders/overview)
 *
 * Vertical Sliders allow users to make selections from a range of values.
 *
 * Vertical Sliders reflect a range of values along a vertical bar, from which users may select a
 * single value. They are ideal for adjusting settings such as volume, brightness, or applying image
 * filters.
 *
 * @param state [SliderState] which contains the slider's current value.
 * @param onValueChange receives proposed values to update the state.
 * @param modifier the [Modifier] to be applied to this slider
 * @param enabled controls the enabled state of this slider. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param topToBottom controls the direction of this slider. Default is top to bottom.
 * @param colors [SliderColors] that will be used to resolve the colors used for this slider in
 *   different states. See [MaterialSliderDefaults.colors].
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 *   for this slider. You can create and pass in your own `remember`ed instance to observe
 *   [Interaction]s and customize the appearance / behavior of this slider in different states.
 * @param thumb the thumb to be displayed on the slider, it is placed on top of the track. The
 *   lambda receives a [SliderState] which is used to obtain the current active track.
 * @param track the track to be displayed on the slider, it is placed underneath the thumb. The
 *   lambda receives a [SliderState] which is used to obtain the current active track.
 */
@Composable
public fun VerticalSlider(
    state: SliderState,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    topToBottom: Boolean = true,
    colors: SliderColors = MaterialSliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    thumb: @Composable (SliderState) -> Unit = {
        SliderDefaults.Thumb(
            interactionSource = interactionSource,
            isVertical = true,
            colors = colors,
            enabled = enabled,
            thumbSize = DpSize(44.dp, 4.dp),
        )
    },
    track: @Composable (SliderState) -> Unit = { sliderState ->
        MaterialSliderDefaults.Track(
            colors = colors,
            enabled = enabled,
            sliderState = sliderState,
            trackCornerSize = Dp.Unspecified,
        )
    },
): Unit =
    MaterialVerticalSlider(
        state = state,
        modifier = modifier,
        enabled = enabled,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        topToBottom = topToBottom,
        colors = colors,
        interactionSource = interactionSource,
        thumb = thumb,
        track = track,
    )

/** Shape-aware default thumbs; slider tracks remain owned by Material. */
public object SliderDefaults {
    /**
     * The Default thumb for [Slider] and [RangeSlider]
     *
     * @param interactionSource the [MutableInteractionSource] representing the stream of
     *   [Interaction]s for this thumb. You can create and pass in your own `remember`ed instance to
     *   observe its interactions.
     * @param modifier the [Modifier] to be applied to the thumb.
     * @param colors [SliderColors] that will be used to resolve the colors used for this thumb in
     *   different states. See [MaterialSliderDefaults.colors].
     * @param enabled controls the enabled state of this slider. When `false`, this component will
     *   not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param thumbSize the size of the thumb.
     * @param shape the thumb shape, using the M3X full shape by default.
     */
    @Composable
    public fun Thumb(
        interactionSource: MutableInteractionSource,
        modifier: Modifier = Modifier,
        colors: SliderColors = MaterialSliderDefaults.colors(),
        enabled: Boolean = true,
        thumbSize: DpSize = DpSize(4.dp, 44.dp),
        shape: Shape = MaterialTheme.extended.shapes.full,
    ): Unit = SliderThumb(interactionSource, modifier, colors, enabled, thumbSize, false, shape)

    /**
     * The Default thumb for [Slider], [VerticalSlider] and [RangeSlider]
     *
     * @param interactionSource the [MutableInteractionSource] representing the stream of
     *   [Interaction]s for this thumb. You can create and pass in your own `remember`ed instance to
     *   observe its interactions.
     * @param isVertical whether the thumb belongs to a vertical slider.
     * @param modifier the [Modifier] to be applied to the thumb.
     * @param colors [SliderColors] that will be used to resolve the colors used for this thumb in
     *   different states. See [MaterialSliderDefaults.colors].
     * @param enabled controls the enabled state of this slider. When `false`, this component will
     *   not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param thumbSize the size of the thumb.
     * @param shape the thumb shape, using the M3X full shape by default.
     */
    @Composable
    public fun Thumb(
        interactionSource: MutableInteractionSource,
        isVertical: Boolean,
        modifier: Modifier = Modifier,
        colors: SliderColors = MaterialSliderDefaults.colors(),
        enabled: Boolean = true,
        thumbSize: DpSize = if (isVertical) DpSize(44.dp, 4.dp) else DpSize(4.dp, 44.dp),
        shape: Shape = MaterialTheme.extended.shapes.full,
    ): Unit =
        SliderThumb(
            interactionSource,
            modifier,
            colors,
            enabled,
            thumbSize,
            isVertical,
            shape,
        )
}

@Composable
private fun SliderThumb(
    interactionSource: MutableInteractionSource,
    modifier: Modifier,
    colors: SliderColors,
    enabled: Boolean,
    thumbSize: DpSize,
    isVertical: Boolean,
    shape: Shape,
) {
    val interactions = remember { mutableStateListOf<Interaction>() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is FocusInteraction.Focus -> interactions.add(interaction)
                is FocusInteraction.Unfocus -> interactions.remove(interaction.focus)
                is PressInteraction.Press -> interactions.add(interaction)
                is PressInteraction.Release -> interactions.remove(interaction.press)
                is PressInteraction.Cancel -> interactions.remove(interaction.press)
                is DragInteraction.Start -> interactions.add(interaction)
                is DragInteraction.Stop -> interactions.remove(interaction.start)
                is DragInteraction.Cancel -> interactions.remove(interaction.start)
            }
        }
    }

    val size =
        if (interactions.isNotEmpty()) {
            if (isVertical) {
                thumbSize.copy(height = thumbSize.height / 2)
            } else {
                thumbSize.copy(width = thumbSize.width / 2)
            }
        } else {
            thumbSize
        }
    Box(
        modifier
            .size(thumbSize)
            .hoverable(interactionSource = interactionSource)
            .pointerHoverIcon(icon = PointerIcon.Hand),
        contentAlignment = Alignment.Center,
    ) {
        Spacer(
            Modifier.size(size)
                .background(if (enabled) colors.thumbColor else colors.disabledThumbColor, shape),
        )
    }
}
