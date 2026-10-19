package top.ltfan.m3x.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.M3xMaterialTheme

class LabPlacementTest {
    @Test
    fun previewSurfacesMoveAndResizeIndependently() = runInitializedComposeUiTest {
        setContent {
            M3xMaterialTheme {
                Box(Modifier.size(600.dp, 240.dp)) {
                    GlassPreview(Modifier.size(600.dp, 240.dp)) {
                        MovableLabSurfaces(Modifier.size(600.dp, 240.dp))
                    }
                }
            }
        }
        val first = onNodeWithTag("lab-surface-0")
        val second = onNodeWithTag("lab-surface-1")
        val before = first.fetchSemanticsNode().boundsInRoot
        val neighbor = second.fetchSemanticsNode().boundsInRoot
        first.performTouchInput {
            down(center)
            moveTo(center + Offset(36f, 20f), delayMillis = 100)
            moveTo(center + Offset(60f, 36f), delayMillis = 100)
            up()
        }
        waitForIdle()
        val moved = first.fetchSemanticsNode().boundsInRoot
        assertTrue(
            moved.left > before.left && moved.top > before.top,
            "before=$before moved=$moved",
        )
        onNodeWithTag("lab-resize-0").performTouchInput {
            down(center)
            moveTo(center + Offset(40f, 20f), delayMillis = 100)
            moveTo(center + Offset(60f, 30f), delayMillis = 100)
            up()
        }
        waitForIdle()
        val resized = first.fetchSemanticsNode().boundsInRoot
        assertTrue(
            resized.width > moved.width && resized.height > moved.height,
            "moved=$moved resized=$resized",
        )
        assertTrue(second.fetchSemanticsNode().boundsInRoot == neighbor)
    }

    @Test
    fun desktopMouseMovesAndResizesPreview() = runInitializedComposeUiTest {
        setContent {
            M3xMaterialTheme {
                GlassPreview(Modifier.size(600.dp, 240.dp)) {
                    MovableLabSurfaces(Modifier.size(600.dp, 240.dp))
                }
            }
        }
        val first = onNodeWithTag("lab-surface-0")
        val before = first.fetchSemanticsNode().boundsInRoot
        first.performMouseInput {
            moveTo(center)
            press()
            moveTo(center + Offset(36f, 20f), delayMillis = 100)
            moveTo(center + Offset(60f, 36f), delayMillis = 100)
            release()
        }
        waitForIdle()
        val moved = first.fetchSemanticsNode().boundsInRoot
        assertTrue(
            moved.left > before.left && moved.top > before.top,
            "before=$before moved=$moved",
        )
        onNodeWithTag("lab-resize-0").performMouseInput {
            moveTo(center)
            press()
            moveTo(center + Offset(40f, 20f), delayMillis = 100)
            moveTo(center + Offset(60f, 30f), delayMillis = 100)
            release()
        }
        waitForIdle()
        val resized = first.fetchSemanticsNode().boundsInRoot
        assertTrue(
            resized.width > moved.width && resized.height > moved.height,
            "moved=$moved resized=$resized",
        )
    }
}
