package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue
import top.ltfan.backdrop.BackdropMask
import top.ltfan.m3x.backdrop.BackdropLayerHandle
import top.ltfan.m3x.backdrop.LocalBackdrop
import top.ltfan.m3x.material.FloatingBottomBar
import top.ltfan.m3x.material.backdrop.theme.backdrop
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.item
import top.ltfan.m3x.material.overlay.MaterialPanelSurface
import top.ltfan.m3x.material.overlay.OverlayPlainTooltip
import top.ltfan.m3x.material.rememberMaterialSurfaceLayer
import top.ltfan.m3x.material.theme.extended

class BackdropFallbackRenderingTest {
    private val schemes =
        listOf(
            lightColorScheme(
                surface = Color.White,
                surfaceContainerHighest = Color(0xFFE0E0E0),
                outline = Color.Black,
                onSurface = Color.Black,
            ),
            darkColorScheme(
                surface = Color(0xFF101010),
                surfaceContainerHighest = Color(0xFF404040),
                outline = Color.White,
                onSurface = Color.White,
            ),
            lightColorScheme(
                surface = Color.White,
                surfaceContainerHighest = Color.Yellow,
                outline = Color.Black,
                onSurface = Color.Black,
            ),
        )

    @Test
    fun bottomBarUsesTranslucentSurfaceAndSolidIndicatorWithoutBackdrop() {
        schemes.forEach { scheme -> assertBottomBarColors(scheme) }
    }

    private fun assertBottomBarColors(scheme: ColorScheme) = runComposeUiTest {
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = scheme) {
                    FloatingBottomBar(
                        selectedTabIndex = 0,
                        onTabSelected = {},
                        modifier = Modifier.testTag("bar"),
                    ) {
                        item { Text("First") }
                        item { Text("Second") }
                    }
                }
            }
        }
        val image = onNodeWithTag("bar").captureToImage()
        val pixels = image.toPixelMap()
        assertClose(
            scheme.surface.copy(alpha = 0.6f),
            pixels[image.width * 3 / 4, image.height - 14],
        )
        assertClose(scheme.surfaceContainerHighest, pixels[image.width / 4, image.height - 14])
        assertReadable(
            scheme.onSurface,
            scheme.surface.copy(alpha = 0.6f).compositeOver(scheme.background),
        )
        assertReadable(scheme.onSurface, scheme.surfaceContainerHighest)
    }

    @Test
    fun topBarAndTooltipUseTheirUsualTintsWithoutSampling() {
        schemes.forEach { scheme -> assertTopBarAndTooltipColors(scheme) }
    }

    private fun assertTopBarAndTooltipColors(scheme: ColorScheme) = runComposeUiTest {
        val topBarTint = Color.Red.copy(alpha = 0.4f)
        val tooltipTint = Color.Blue.copy(alpha = 0.5f)
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = scheme) {
                    Box(Modifier.size(320.dp, 220.dp)) {
                        BackdropTopAppBar(
                            title = { Text("Title") },
                            modifier = Modifier.testTag("topBar"),
                            mask = BackdropMask(topBarTint, alpha = 0.1f),
                        )
                        OverlayPlainTooltip(
                            modifier = Modifier.testTag("tooltip"),
                            containerColor = tooltipTint,
                        ) {
                            Box(Modifier.size(80.dp, 40.dp))
                        }
                    }
                }
            }
        }
        val topBar = onNodeWithTag("topBar").captureToImage()
        // The mask alpha multiplies the alpha of the mask color, so a translucent mask color
        // weakens the ramp instead of being overridden by it.
        assertClose(
            topBarTint.copy(
                alpha =
                    topBarTint.alpha *
                        0.1f *
                        appBarRampIntensity(topBar.height / 2 + 0.5f, topBar.height.toFloat()),
            ),
            topBar.toPixelMap()[topBar.width - 12, topBar.height / 2],
        )
        val tooltip = onNodeWithTag("tooltip").captureToImage()
        assertClose(
            tooltipTint.copy(alpha = 0.8f),
            tooltip.toPixelMap()[tooltip.width / 2, tooltip.height / 2],
        )
    }

    @Test
    fun topBarUsesProgressiveMaskOpacityByDefaultWithoutBackdrop() = runComposeUiTest {
        var fallback = 0f
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    val scrollEdge = MaterialTheme.extended.backdrop.scrollEdge
                    SideEffect { fallback = scrollEdge.fallbackAlpha }
                    BackdropTopAppBar(
                        title = {},
                        modifier = Modifier.testTag("topBar"),
                        mask = BackdropMask(Color.Red),
                    )
                }
            }
        }
        val image = onNodeWithTag("topBar").captureToImage()
        val pixels = image.toPixelMap()
        assertClose(Color.Red.copy(alpha = fallback), pixels[image.width - 12, image.height / 4])
        assertClose(
            Color.Red.copy(
                alpha = fallback * appBarRampIntensity(image.height - 1.5f, image.height.toFloat()),
            ),
            pixels[image.width - 12, image.height - 2],
        )
    }

    @Test
    fun panelFallbackDoesNotExportAnEmptyBackdrop() = runComposeUiTest {
        var childBackdrop: BackdropLayerHandle? = null
        val tint = Color.Red.copy(alpha = 0.5f)
        val scheme = schemes.first()
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = scheme) {
                    val exported = rememberMaterialSurfaceLayer()
                    MaterialPanelSurface(
                        shape = RectangleShape,
                        containerColor = tint,
                        exportedBackdrop = exported,
                        modifier = Modifier.testTag("panel"),
                    ) {
                        val current = LocalBackdrop.current
                        SideEffect { childBackdrop = current }
                        Box(Modifier.size(80.dp))
                    }
                }
            }
        }
        waitForIdle()
        assertNull(childBackdrop)
        val image = onNodeWithTag("panel").captureToImage()
        assertClose(tint.copy(alpha = 0.6f), image.toPixelMap()[image.width / 2, image.height / 2])
    }

    private fun assertClose(expected: Color, actual: Color) {
        val difference =
            maxOf(
                abs(expected.red - actual.red),
                abs(expected.green - actual.green),
                abs(expected.blue - actual.blue),
                abs(expected.alpha - actual.alpha),
            )
        assertTrue(difference < 0.08f, "Expected $expected, got $actual")
    }

    private fun assertReadable(foreground: Color, background: Color) {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        val contrast = (lighter + 0.05f) / (darker + 0.05f)
        assertTrue(contrast >= 4.5f, "Expected readable contrast, got $contrast")
    }
}
