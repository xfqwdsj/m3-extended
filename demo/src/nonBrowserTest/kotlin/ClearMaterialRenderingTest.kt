package top.ltfan.m3x.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.backdrop.BackdropPresets
import top.ltfan.m3x.material.backdrop.BackdropSurface
import top.ltfan.m3x.material.bundle.M3xMaterialTheme

class ClearMaterialRenderingTest {
    @Test
    fun clearPreservesLightBackgroundAndAddsOnlyThemeTint() = runInitializedComposeUiTest {
        setContent {
            M3xMaterialTheme(
                colorScheme = lightColorScheme(surface = Color(0.9f, 0.96f, 1f)),
                backdrop = BackdropPresets.GlassClear,
            ) {
                val source = rememberBackdropLayer()
                Box(Modifier.size(200.dp, 100.dp)) {
                    Box(Modifier.fillMaxSize().captureBackdrop(source).background(Color.White))
                    BackdropHost(source) {
                        BackdropSurface(
                            Modifier.size(96.dp).testTag("clear"),
                            RectangleShape,
                            role = MaterialSurfaceRole.Panel,
                        ) {}
                    }
                }
            }
        }
        val pixels = onNodeWithTag("clear").captureToImage().toPixelMap()
        val color = pixels[pixels.width / 2, pixels.height / 2]
        assertTrue(color.red > 0.95f, "Clear must preserve a light background: $color")
        assertTrue(color.blue > color.red, "Clear adds the supplied light blue theme tint: $color")
    }
}
