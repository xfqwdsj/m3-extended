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

package top.ltfan.m3x.material

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.DragHandleColors
import androidx.compose.material3.DragHandleShapes
import androidx.compose.material3.DragHandleSizes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDragHandle as MaterialVerticalDragHandle
import androidx.compose.material3.VerticalDragHandleDefaults as MaterialVerticalDragHandleDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import top.ltfan.m3x.material.theme.extended

/**
 * [Material Design drag
 * handle](https://m3.material.io/foundations/layout/understanding-layout/parts-of-layout#314a4c32-be52-414c-8da7-31f059f1776d)
 *
 * A drag handle lets users resize or reposition a component by dragging. For example, it can adjust
 * the relative sizes of adjacent panes. A vertically oriented handle conveys horizontal drag
 * motion.
 *
 * @param modifier the [Modifier] to be applied to this drag handle.
 * @param sizes sizes of this drag handle; see [MaterialVerticalDragHandleDefaults.sizes] for the
 *   default values.
 * @param colors colors of this drag handle; see [MaterialVerticalDragHandleDefaults.colors] for the
 *   default values.
 * @param shapes shapes of this drag handle; see [VerticalDragHandleDefaults.shapes] for the default
 *   values.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this drag handle. You can use this to change the drag handle's
 *   appearance or preview the drag handle in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 */
@Composable
public fun VerticalDragHandle(
    modifier: Modifier = Modifier,
    sizes: DragHandleSizes = MaterialVerticalDragHandleDefaults.sizes(),
    colors: DragHandleColors = MaterialVerticalDragHandleDefaults.colors(),
    shapes: DragHandleShapes = VerticalDragHandleDefaults.shapes(),
    interactionSource: MutableInteractionSource? = null,
): Unit =
    MaterialVerticalDragHandle(
        modifier = modifier,
        sizes = sizes,
        colors = colors,
        shapes = shapes,
        interactionSource = interactionSource,
    )

/** Theme-aware shape defaults for a vertical drag handle. */
public object VerticalDragHandleDefaults {
    /**
     * Creates a [DragHandleShapes] that represents the default, pressed, and dragged shapes used in
     * a [VerticalDragHandle].
     *
     * The resting shape defaults to the M3X full shape.
     *
     * @param shape provides a different shape to override the default shape of the drag handle when
     *   it's not being pressed.
     * @param pressedShape provides a different shape to override the shape of the drag handle when
     *   it's being pressed but not dragged.
     * @param draggedShape provides a different shape to override the shape of the drag handle when
     *   it's being dragged.
     */
    @Composable
    public fun shapes(
        shape: Shape? = null,
        pressedShape: Shape? = null,
        draggedShape: Shape? = null,
    ): DragHandleShapes =
        MaterialVerticalDragHandleDefaults.shapes(
            shape = shape ?: MaterialTheme.extended.shapes.full,
            pressedShape = pressedShape,
            draggedShape = draggedShape,
        )
}
