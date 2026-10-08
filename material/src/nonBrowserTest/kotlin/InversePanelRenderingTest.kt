package top.ltfan.m3x.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.backdrop.BackdropStyle
import top.ltfan.backdrop.highlight.Highlight
import top.ltfan.backdrop.shadow.Shadow
import top.ltfan.m3x.backdrop.LocalBackdrop
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.backdrop.BackdropSurface
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropTheme
import top.ltfan.m3x.material.backdrop.theme.ProvideBackdropMaterial
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.overlay.OverlayPlainTooltip
import top.ltfan.m3x.material.overlay.OverlaySnackbar

class InversePanelRenderingTest {
    @Test
    fun feedbackUsesInverseMaterialWithAndWithoutASource() {
        for (scheme in listOf(lightColorScheme(), darkColorScheme())) {
            for (sampled in listOf(false, true)) {
                for (variant in 0..2) runInitializedComposeUiTest {
                    setContent {
                        ProvideM3xMaterialRendering {
                            MaterialTheme(colorScheme = scheme) {
                                val source = rememberBackdropLayer(scheme.surface)
                                CompositionLocalProvider(
                                    LocalBackdrop provides if (sampled) source else null,
                                ) {
                                    Box(Modifier.size(240.dp, 100.dp).background(scheme.surface)) {
                                        if (sampled) {
                                            Box(
                                                Modifier.fillMaxSize()
                                                    .captureBackdrop(source)
                                                    .background(scheme.surface),
                                            )
                                        }
                                        ProvideBackdropMaterial(
                                            MaterialSurfaceRole.InversePanel,
                                            M3xBackdropTheme()
                                                .inversePanel
                                                .copy(
                                                    style =
                                                        BackdropStyle(
                                                            effects = BackdropStyle.NoEffects,
                                                            highlight = { Highlight.None },
                                                            shadow = { Shadow.None },
                                                        ),
                                                    blurRadius = 0.dp,
                                                    interactiveLighting = false,
                                                ),
                                        ) {
                                            val modifier =
                                                Modifier.size(160.dp, 60.dp).testTag("feedback")
                                            if (variant == 0) {
                                                OverlayPlainTooltip(
                                                    modifier,
                                                    shape = RectangleShape,
                                                ) {}
                                            } else if (variant == 1) {
                                                OverlaySnackbar(modifier, shape = RectangleShape) {}
                                            } else {
                                                BackdropSurface(
                                                    modifier,
                                                    shape = RectangleShape,
                                                    role = MaterialSurfaceRole.InversePanel,
                                                ) {}
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    val pixels = onNodeWithTag("feedback").captureToImage().toPixelMap()
                    val board = pixels[pixels.width / 2, pixels.height / 2]
                    val expected =
                        scheme.inverseSurface.copy(alpha = 0.8f).compositeOver(scheme.surface)
                    assertTrue(abs(expected.red - board.red) < 0.025f, "$sampled/$variant: $board")
                    assertTrue(
                        contrast(board, scheme.surface) > 3f,
                        "Background separation: $board",
                    )
                    assertTrue(
                        contrast(board, scheme.inverseOnSurface) > 4.5f,
                        "Text contrast: $board",
                    )
                }
            }
        }
    }

    private fun contrast(first: Color, second: Color): Float {
        val a = first.luminance()
        val b = second.luminance()
        return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
    }
}
