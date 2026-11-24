package top.ltfan.m3x.backdrop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.m3x.backdrop.backdrops.rememberCanvasBackdrop
import top.ltfan.m3x.backdrop.effects.lens

internal class ContourLensRenderingTest {
    @Test
    fun refractionFollowsCutCornerAndUpdatesWhenShapeChanges() = runRenderTest {
        val cut = mutableIntStateOf(24)
        val shape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ) = CutCornerShape(cut.intValue.dp).createOutline(size, layoutDirection, density)
            }
        setContent {
            val source = rememberCanvasBackdrop {
                drawRect(Color.Red, samplingBounds.topLeft, samplingBounds.size)
                drawRect(
                    Color.Blue,
                    Offset(16f, samplingBounds.top),
                    Size(samplingBounds.right - 16f, samplingBounds.height),
                )
            }
            Box(
                Modifier.size(64.dp).testTag("lens").drawBackdrop(source, { shape }) {
                    effects { lens(12f, 12f) }
                },
            )
        }

        fun sample() = onNodeWithTag("lens").captureToImage().toPixelMap()[12, 13]
        val cutFrame = sample()
        assertTrue(
            cutFrame.blue > .75f,
            "Cut-corner refraction should cross the color boundary: $cutFrame",
        )
        runOnIdle { cut.intValue = 4 }
        val shallowCutFrame = sample()
        assertTrue(
            shallowCutFrame.red > .9f,
            "The same point is now inside the clear region: $shallowCutFrame",
        )
    }
}
