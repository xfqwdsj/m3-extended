package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.m3x.material.FloatingBottomBar
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.item

class FloatingBottomBarSizingTest {
    @Test
    fun narrowTabContentStillGetsTheMinimumSlotWidth() = runComposeUiTest {
        var density = Density(1f)
        setContent {
            density = LocalDensity.current
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    FloatingBottomBar(0, {}, Modifier.testTag("bar")) {
                        repeat(3) { item { Box(Modifier.size(16.dp, 24.dp)) } }
                    }
                }
            }
        }
        waitForIdle()
        val width = onNodeWithTag("bar").fetchSemanticsNode().boundsInRoot.width
        assertTrue(
            abs(width - with(density) { 3 * 76.dp.roundToPx() + 2 * 4.dp.roundToPx() }) < 1f,
            "Three narrow tabs keep 76dp slots: bar width $width",
        )
    }

    @Test
    fun contentWiderThanTheFloorStillDrivesTheBar() = runComposeUiTest {
        var density = Density(1f)
        setContent {
            density = LocalDensity.current
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    FloatingBottomBar(0, {}, Modifier.testTag("bar")) {
                        item { Box(Modifier.size(120.dp, 24.dp)) }
                        item { Box(Modifier.size(16.dp, 24.dp)) }
                        item { Box(Modifier.size(16.dp, 24.dp)) }
                    }
                }
            }
        }
        waitForIdle()
        val width = onNodeWithTag("bar").fetchSemanticsNode().boundsInRoot.width
        assertTrue(
            abs(width - with(density) { 3 * 120.dp.roundToPx() + 2 * 4.dp.roundToPx() }) < 1f,
            "The widest tab widens every equal slot: bar width $width",
        )
    }

    @Test
    fun callerCanTuneTheTabWidthFloor() = runComposeUiTest {
        var density = Density(1f)
        setContent {
            density = LocalDensity.current
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    FloatingBottomBar(0, {}, Modifier.testTag("bar"), tabMinWidth = 100.dp) {
                        repeat(3) { item { Box(Modifier.size(16.dp, 24.dp)) } }
                    }
                }
            }
        }
        waitForIdle()
        val width = onNodeWithTag("bar").fetchSemanticsNode().boundsInRoot.width
        assertTrue(
            abs(width - with(density) { 3 * 100.dp.roundToPx() + 2 * 4.dp.roundToPx() }) < 1f,
            "tabMinWidth replaces the default floor: bar width $width",
        )
    }

    @Test
    fun zeroTabWidthFloorSizesTheBarToItsContent() = runComposeUiTest {
        var density = Density(1f)
        setContent {
            density = LocalDensity.current
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    FloatingBottomBar(0, {}, Modifier.testTag("bar"), tabMinWidth = 0.dp) {
                        repeat(3) { item { Box(Modifier.size(16.dp, 24.dp)) } }
                    }
                }
            }
        }
        waitForIdle()
        val width = onNodeWithTag("bar").fetchSemanticsNode().boundsInRoot.width
        assertTrue(
            abs(width - with(density) { 3 * 16.dp.roundToPx() + 2 * 4.dp.roundToPx() }) < 1f,
            "A zero floor sizes the bar purely to its tabs: bar width $width",
        )
    }
}
