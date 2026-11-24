package top.ltfan.m3x.demo

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import top.ltfan.m3x.demo.generated.resources.Res
import top.ltfan.m3x.demo.generated.resources.demo_decrease_size
import top.ltfan.m3x.demo.generated.resources.demo_drag_me
import top.ltfan.m3x.demo.generated.resources.demo_increase_size
import top.ltfan.m3x.demo.generated.resources.demo_move_down
import top.ltfan.m3x.demo.generated.resources.demo_move_left
import top.ltfan.m3x.demo.generated.resources.demo_move_right
import top.ltfan.m3x.demo.generated.resources.demo_move_up
import top.ltfan.m3x.demo.generated.resources.demo_neighbor
import top.ltfan.m3x.demo.generated.resources.demo_reset_placement
import top.ltfan.m3x.demo.generated.resources.demo_resize
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.TextButton
import top.ltfan.m3x.material.backdrop.BackdropSurface
import top.ltfan.m3x.material.theme.extended

@Composable
internal fun MovableLabSurfaces(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val areaWidth = maxWidth.value
        val areaHeight = maxHeight.value
        val density = LocalDensity.current.density
        repeat(2) { index ->
            var displacement by remember { mutableStateOf(Offset.Zero) }
            var width by remember { mutableFloatStateOf(150f) }
            var height by remember { mutableFloatStateOf(56f) }
            val surfaceWidth = width.coerceIn(minOf(64f, areaWidth), areaWidth)
            val surfaceHeight = height.coerceIn(minOf(40f, areaHeight), areaHeight)
            val x =
                (areaWidth * (0.05f + index * 0.5f) + displacement.x).coerceIn(
                    0f,
                    (areaWidth - surfaceWidth).coerceAtLeast(0f),
                )
            val y =
                (areaHeight * 0.35f + displacement.y).coerceIn(
                    0f,
                    (areaHeight - surfaceHeight).coerceAtLeast(0f),
                )
            val label =
                if (index == 0) demoText(Res.string.demo_drag_me)
                else demoText(Res.string.demo_neighbor)
            val moveRightLabel = demoText(Res.string.demo_move_right)
            val moveLeftLabel = demoText(Res.string.demo_move_left)
            val moveDownLabel = demoText(Res.string.demo_move_down)
            val moveUpLabel = demoText(Res.string.demo_move_up)
            val increaseSizeLabel = demoText(Res.string.demo_increase_size)
            val decreaseSizeLabel = demoText(Res.string.demo_decrease_size)
            val resetPlacementLabel = demoText(Res.string.demo_reset_placement)
            val resizeLabel = demoText(Res.string.demo_resize)
            fun resize(delta: Offset) {
                width = (width + delta.x).coerceIn(minOf(64f, areaWidth), areaWidth)
                height = (height + delta.y).coerceIn(minOf(40f, areaHeight), areaHeight)
            }
            Box(
                Modifier.offset {
                        IntOffset((x * density).roundToInt(), (y * density).roundToInt())
                    }
                    .size(surfaceWidth.dp, surfaceHeight.dp)
                    .testTag("lab-surface-$index"),
            ) {
                BackdropSurface(
                    Modifier.fillMaxSize()
                        .pointerInput(areaWidth, areaHeight, density) {
                            detectDragGestures { change, amount ->
                                change.consume()
                                val next = displacement + amount / density
                                val originX = areaWidth * (0.05f + index * 0.5f)
                                val originY = areaHeight * 0.35f
                                displacement =
                                    Offset(
                                        (originX + next.x).coerceIn(
                                            0f,
                                            (areaWidth - width).coerceAtLeast(0f),
                                        ) - originX,
                                        (originY + next.y).coerceIn(
                                            0f,
                                            (areaHeight - height).coerceAtLeast(0f),
                                        ) - originY,
                                    )
                            }
                        }
                        .semantics {
                            customActions =
                                listOf(
                                    CustomAccessibilityAction(moveRightLabel) {
                                        displacement += Offset(16f, 0f)
                                        true
                                    },
                                    CustomAccessibilityAction(moveLeftLabel) {
                                        displacement -= Offset(16f, 0f)
                                        true
                                    },
                                    CustomAccessibilityAction(moveDownLabel) {
                                        displacement += Offset(0f, 16f)
                                        true
                                    },
                                    CustomAccessibilityAction(moveUpLabel) {
                                        displacement -= Offset(0f, 16f)
                                        true
                                    },
                                    CustomAccessibilityAction(increaseSizeLabel) {
                                        resize(Offset(16f, 8f))
                                        true
                                    },
                                    CustomAccessibilityAction(decreaseSizeLabel) {
                                        resize(Offset(-16f, -8f))
                                        true
                                    },
                                    CustomAccessibilityAction(resetPlacementLabel) {
                                        displacement = Offset.Zero
                                        width = 150f
                                        height = 56f
                                        true
                                    },
                                )
                        },
                    shape = MaterialTheme.extended.shapes.full,
                    role = MaterialSurfaceRole.Control,
                ) {
                    TextButton(onClick = {}, modifier = Modifier.fillMaxSize()) { Text(label) }
                }
                Box(
                    Modifier.testTag("lab-resize-$index")
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .pointerInput(areaWidth, areaHeight, density) {
                            detectDragGestures { change, amount ->
                                change.consume()
                                resize(amount / density)
                            }
                        }
                        .semantics { contentDescription = "$resizeLabel $label" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("↘")
                }
            }
        }
    }
}
