package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.m3x.backdrop.BackdropBlur
import top.ltfan.m3x.backdrop.BackdropEdge
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.BackdropStyle
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial

class BackdropStyleAlgorithmsTest {
    @Test
    fun materialPreservesNativeAlgorithmsAndCanSwitchTheirTopology() = runComposeUiTest {
        val algorithm = mutableStateOf<BackdropBlur>(BackdropBlur.None)
        val material =
            M3xBackdropMaterial(
                style = BackdropStyle.NoEffects { blur = { algorithm.value } },
                tintAlpha = 0f,
            )
        setContent {
            MaterialTheme {
                val source = rememberBackdropLayer(Color.Black)
                BackdropHost(source) {
                    Box(Modifier.size(96.dp)) {
                        Canvas(Modifier.size(96.dp).captureBackdrop(source)) {
                            for (x in 0 until size.width.toInt() step 4) {
                                drawRect(
                                    Color.White,
                                    Offset(x.toFloat(), 0f),
                                    Size(2f, size.height),
                                )
                            }
                        }
                        BackdropSurface(
                            modifier = Modifier.size(96.dp).testTag("material"),
                            shape = RectangleShape,
                            material = material,
                        ) {}
                    }
                }
            }
        }
        fun contrast(): Float {
            val image = onNodeWithTag("material").captureToImage()
            val pixels = image.toPixelMap()
            val y = 4
            return (8 until image.width - 8).map { pixels[it, y].red }.let { it.max() - it.min() }
        }
        assertTrue(contrast() > .9f)
        for (blur in
            listOf(
                BackdropBlur.Uniform(8.dp),
                BackdropBlur.LayeredProgressive(8.dp, BackdropEdge.Top),
                BackdropBlur.Progressive(8.dp, BackdropEdge.Top),
            )) {
            runOnIdle { algorithm.value = blur }
            assertTrue(contrast() < .3f, "Native $blur must blur the stripe source")
        }
        runOnIdle { algorithm.value = BackdropBlur.None }
        assertTrue(contrast() > .9f)
    }
}
