/*
 * Copyright 2025 Kyant
 *
 * Modified for M3X.
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

package top.ltfan.m3x.compose.foundation.gestures

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import top.ltfan.m3x.compose.animation.core.withDefaultVisibilityThreshold

/**
 * Coordinates a bounded dragged value, press response and velocity using caller-supplied motion.
 */
public class DampedDragState(
    private val animationScope: CoroutineScope,
    public val initialValue: Float,
    public val valueRange: ClosedRange<Float>,
    public val visibilityThreshold: Float,
    private val valueAnimationSpec: AnimationSpec<Float>,
    effectsAnimationSpec: AnimationSpec<Float>,
    spatialAnimationSpec: AnimationSpec<Float>,
    private val spatialMotionEnabled: () -> Boolean,
    public val initialScale: Float,
    public val pressedScale: Float,
    public val onDragStarted: DampedDragState.(position: Offset) -> Unit,
    public val onDragStopped: DampedDragState.(cancelled: Boolean, position: Offset?) -> Unit,
    public val onDrag:
        DampedDragState.(size: IntSize, position: Offset, dragAmount: Offset) -> Unit,
) {

    private val velocityAnimationSpec =
        spatialAnimationSpec.withDefaultVisibilityThreshold(visibilityThreshold * 10f)
    private val pressProgressAnimationSpec =
        effectsAnimationSpec.withDefaultVisibilityThreshold(0.001f)
    private val scaleXAnimationSpec = spatialAnimationSpec.withDefaultVisibilityThreshold(0.001f)
    private val scaleYAnimationSpec = scaleXAnimationSpec

    private val valueAnimation = Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation = Animatable(0f, 5f)
    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val scaleXAnimation = Animatable(initialScale, 0.001f)
    private val scaleYAnimation = Animatable(initialScale, 0.001f)

    private val mutatorMutex = MutatorMutex()

    private val velocityTracker = VelocityTracker()
    private var velocityAnimationJob: Job? = null

    public val value: Float
        get() = valueAnimation.value

    public val progress: Float
        get() = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)

    public val targetValue: Float
        get() = valueAnimation.targetValue

    public val pressProgress: Float
        get() = pressProgressAnimation.value.coerceIn(0f, 1f)

    public val scaleProgress: Float
        get() = (scaleXAnimation.value - initialScale) / (pressedScale - initialScale)

    public val scaleX: Float
        get() = scaleXAnimation.value

    public val scaleY: Float
        get() = scaleYAnimation.value

    public val velocity: Float
        get() = velocityAnimation.value

    public val modifier: Modifier =
        Modifier.pointerInput(Unit) {
            inspectDragGestures(
                dragPass = PointerEventPass.Initial,
                onDragStart = { down ->
                    onDragStarted(down.position)
                    press()
                },
                onDragEnd = { up ->
                    up.consume()
                    onDragStopped(false, up.position)
                    release()
                },
                onDragCancel = {
                    onDragStopped(true, null)
                    release()
                },
            ) { change, dragAmount ->
                onDrag(size, change.position, dragAmount)
            }
        }

    public fun press() {
        if (!spatialMotionEnabled()) return
        velocityTracker.resetTracking()
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(pressedScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(pressedScale, scaleYAnimationSpec) }
        }
    }

    public fun release() {
        if (!spatialMotionEnabled()) {
            velocityAnimationJob?.cancel()
            animationScope.launch {
                pressProgressAnimation.snapTo(0f)
                scaleXAnimation.snapTo(initialScale)
                scaleYAnimation.snapTo(initialScale)
                velocityAnimation.snapTo(0f)
            }
            return
        }
        animationScope.launch {
            withFrameNanos {}
            if (value != targetValue) {
                val threshold = (valueRange.endInclusive - valueRange.start) * 0.025f
                snapshotFlow { valueAnimation.value }
                    .first { abs(it - valueAnimation.targetValue) < threshold }
            }
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
        }
    }

    public fun updateValue(value: Float) {
        val targetValue = value.coerceIn(valueRange)
        animationScope.launch {
            launch {
                if (!spatialMotionEnabled()) {
                    valueAnimation.snapTo(targetValue)
                    animateVelocityTo(0f)
                    return@launch
                }
                valueAnimation.animateTo(
                    targetValue,
                    valueAnimationSpec,
                ) {
                    updateVelocity()
                }
                animateVelocityTo(0f)
            }
        }
    }

    public fun animateToValue(value: Float) {
        animationScope.launch {
            if (!spatialMotionEnabled()) {
                valueAnimation.snapTo(value.coerceIn(valueRange))
                animateVelocityTo(0f)
                return@launch
            }
            mutatorMutex.mutate {
                press()
                val targetValue = value.coerceIn(valueRange)
                this@launch.launch {
                    valueAnimation.animateTo(targetValue, valueAnimationSpec)
                    animateVelocityTo(0f)
                }
                animateVelocityTo(0f)
                release()
            }
        }
    }

    private fun updateVelocity() {
        if (!spatialMotionEnabled()) return
        velocityTracker.addPosition(
            Clock.System.now().toEpochMilliseconds(),
            Offset(value, 0f),
        )
        val targetVelocity =
            velocityTracker.calculateVelocity().x / (valueRange.endInclusive - valueRange.start)
        animateVelocityTo(targetVelocity)
    }

    // Publish each target before the position animation can complete and reset it to rest.
    private fun animateVelocityTo(target: Float) {
        velocityAnimationJob?.cancel()
        velocityAnimationJob =
            animationScope.launch(start = CoroutineStart.UNDISPATCHED) {
                if (spatialMotionEnabled())
                    velocityAnimation.animateTo(target, velocityAnimationSpec)
                else velocityAnimation.snapTo(0f)
            }
    }
}
