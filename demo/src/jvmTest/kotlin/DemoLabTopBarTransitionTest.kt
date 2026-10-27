package top.ltfan.m3x.demo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.backdrop.BackdropFill
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.backdrop.BackdropLargeFlexibleTopAppBar
import top.ltfan.m3x.material.backdrop.BackdropTopAppBar
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering

class DemoLabTopBarTransitionTest {
    @Test fun pinnedBarReusesTheOrdinaryScrollToTopTransition() = transition(BarVariant.Small)

    @Test fun collapsingBarReusesTheOrdinaryScrollToTopTransition() = transition(BarVariant.Large)

    private fun transition(variant: BarVariant) = runInitializedComposeUiTest {
        val switched = mutableStateOf(false)
        val behaviors = arrayOfNulls<TopAppBarScrollBehavior>(3)
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    val source = rememberBackdropLayer()
                    BackdropHost(source) {
                        Box(Modifier.size(720.dp, 320.dp)) {
                            Canvas(Modifier.fillMaxSize().captureBackdrop(source)) {
                                for (y in 0 until size.height.toInt() step 4) {
                                    drawRect(
                                        if (y % 8 == 0) Color.Black else Color.White,
                                        Offset(0f, y.toFloat()),
                                        Size(size.width, 4f),
                                    )
                                }
                            }
                            Row {
                                repeat(3) { index ->
                                    val route =
                                        if (!switched.value) DemoRoute.Components
                                        else if (index == 0) DemoRoute.Lab else DemoRoute.Settings
                                    val behavior =
                                        rememberDemoScrollBehavior(route, variant, 240.dp)
                                    behaviors[index] = behavior
                                    Box(Modifier.size(240.dp, 320.dp).testTag("panel$index")) {
                                        val surfaceBehavior =
                                            if (index == 2 && switched.value) null else behavior
                                        if (variant == BarVariant.Large)
                                            BackdropLargeFlexibleTopAppBar(
                                                title = {},
                                                scrollBehavior = surfaceBehavior,
                                                fill = BackdropFill(Color.Gray, alpha = 0.6f),
                                            )
                                        else
                                            BackdropTopAppBar(
                                                title = {},
                                                scrollBehavior = surfaceBehavior,
                                                fill = BackdropFill(Color.Gray, alpha = 0.6f),
                                            )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        waitForIdle()
        mainClock.autoAdvance = false
        runOnIdle {
            behaviors.forEach {
                it!!.state.contentOffset = -100f
                if (variant == BarVariant.Large) it.state.heightOffset = -24f
            }
        }
        mainClock.advanceTimeBy(1000)
        runOnIdle { switched.value = true }
        repeat(24) { frame ->
            mainClock.advanceTimeByFrame()
            val lab = onNodeWithTag("panel0").captureToImage().toPixelMap()
            val reference = onNodeWithTag("panel1").captureToImage().toPixelMap()
            var delta = 0f
            for (y in 0 until lab.height) for (x in 0 until lab.width) {
                delta = maxOf(delta, abs(lab[x, y].red - reference[x, y].red))
            }
            assertTrue(
                delta < 0.01f,
                "Narrow Lab differs from the ordinary animated overlap reset at frame $frame: $delta",
            )
        }
        val cleared = onNodeWithTag("panel0").captureToImage().toPixelMap()
        val static = onNodeWithTag("panel2").captureToImage().toPixelMap()
        var staticDelta = 0f
        for (y in 0 until cleared.height) for (x in 0 until cleared.width) {
            staticDelta = maxOf(staticDelta, abs(cleared[x, y].red - static[x, y].red))
        }
        assertTrue(
            staticDelta > 0.1f,
            "The control must detect the former null/static surface path",
        )
    }
}
