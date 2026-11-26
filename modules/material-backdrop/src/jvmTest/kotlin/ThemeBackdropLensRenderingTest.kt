package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.BackdropLens
import top.ltfan.m3x.backdrop.BackdropStyle
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.drawBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropTheme
import top.ltfan.m3x.material.backdrop.theme.ProvideBackdropTheme

class ThemeBackdropLensRenderingTest {
    @Test
    fun defaultThemeRenderUsesNonCornerSilhouettes() = runComposeUiTest {
        val shape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ) = CutCornerShape(24.dp).createOutline(size, layoutDirection, density)
            }
        val control =
            M3xBackdropMaterial(
                style =
                    BackdropStyle.NoEffects {
                        lens = { BackdropLens(12.dp, 12.dp) }
                    },
                tintAlpha = 0f,
                interactiveLighting = false,
            )
        setContent {
            MaterialTheme {
                ProvideBackdropTheme(M3xBackdropTheme(control = control)) {
                    val source = rememberBackdropLayer()
                    BackdropHost(source) {
                        Box(Modifier.size(64.dp)) {
                            Box(Modifier.size(64.dp).captureBackdrop(source)) {
                                Canvas(Modifier.fillMaxSize()) {
                                    drawRect(Color.Red)
                                    drawRect(
                                        Color.Blue,
                                        Offset(16f, 0f),
                                        Size(size.width - 16f, size.height),
                                    )
                                }
                            }
                            Box(
                                Modifier.size(64.dp)
                                    .testTag("theme-non-corner-lens")
                                    .drawBackdrop(source.backdrop, { shape }),
                            )
                        }
                    }
                }
            }
        }

        val refracted = onNodeWithTag("theme-non-corner-lens").captureToImage().toPixelMap()[12, 13]
        assertTrue(
            refracted.blue > .75f,
            "The default theme render must lens a non-corner silhouette: $refracted",
        )
    }
}
