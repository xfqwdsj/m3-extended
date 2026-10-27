package top.ltfan.m3x.material.bundle

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.backdrop.BackdropColorControls
import top.ltfan.backdrop.BackdropFill
import top.ltfan.backdrop.BackdropStyle
import top.ltfan.backdrop.ProvideBackdropStyle
import top.ltfan.backdrop.highlight.Highlight
import top.ltfan.backdrop.shadow.Shadow
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.backdropSurface
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.backdrop.BackdropSurface
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropTheme
import top.ltfan.m3x.material.backdrop.theme.ProvideBackdropMaterial
import top.ltfan.m3x.material.backdrop.theme.ProvideBackdropTheme

class BackdropThemeRenderingTest {
    @Test
    fun receivingStyleOverridesOpticsAndNullRestoresInheritanceForBothSurfacePaths() =
        runComposeUiTest {
            val base = BackdropStyle.NoEffects {
                colorControls = { BackdropColorControls(saturation = 0f) }
            }
            val opticalOverride = BackdropStyle { colorControls = { BackdropColorControls.None } }
            val restored = opticalOverride { colorControls = null }
            setContent {
                ProvideBackdropTheme(
                    M3xBackdropTheme(control = M3xBackdropMaterial(style = base)),
                ) {
                    val source = rememberBackdropLayer(Color.Red)
                    BackdropHost(source) {
                        Box(Modifier.size(240.dp, 160.dp)) {
                            Box(Modifier.size(240.dp, 160.dp).captureBackdrop(source))
                            listOf(BackdropStyle {}, opticalOverride, restored).forEachIndexed {
                                index,
                                style ->
                                ProvideBackdropStyle(style) {
                                    Box(
                                        Modifier.offset(x = (index * 80).dp)
                                            .size(80.dp)
                                            .testTag("low-$index")
                                            .backdropSurface(shape = RectangleShape),
                                    )
                                    BackdropSurface(
                                        modifier =
                                            Modifier.offset(x = (index * 80).dp, y = 80.dp)
                                                .size(80.dp)
                                                .testTag("material-$index"),
                                        shape = RectangleShape,
                                        material =
                                            M3xBackdropMaterial(
                                                style = BackdropStyle {},
                                                tintAlpha = 0f,
                                            ),
                                    ) {}
                                }
                            }
                        }
                    }
                }
            }
            for (path in listOf("low", "material")) {
                for (index in 0..2) {
                    val image = onNodeWithTag("$path-$index").captureToImage()
                    val color = image.toPixelMap()[image.width / 2, image.height / 2]
                    if (index == 1)
                        assertTrue(color.red > .98f && color.green < .02f, "$path override: $color")
                    else
                        assertTrue(
                            abs(color.red - color.green) < .02f && color.red > .1f,
                            "$path inherited saturation: $color",
                        )
                }
            }
        }

    @Test
    fun lowLevelSurfaceDrawsInheritedEffectsAndTracksControlOverrides() = runComposeUiTest {
        var effectCalls = 0
        val fillColor = mutableStateOf(Color.Red)
        val material =
            M3xBackdropMaterial(
                style =
                    BackdropStyle.Default {
                        vibrancy = {
                            effectCalls++
                            1f
                        }
                        fill = { BackdropFill(fillColor.value) }
                        highlight = { Highlight.None }
                        shadow = { Shadow.None }
                    },
            )
        setContent {
            M3xMaterialTheme(backdrop = M3xBackdropTheme(control = material)) {
                val source = rememberBackdropLayer(Color.Black)
                BackdropHost(source) {
                    Box(Modifier.size(160.dp, 80.dp)) {
                        Box(Modifier.size(160.dp, 80.dp).captureBackdrop(source))
                        Box(
                            Modifier.size(80.dp)
                                .testTag("inherited")
                                .backdropSurface(shape = RectangleShape),
                        )
                        ProvideBackdropMaterial(
                            MaterialSurfaceRole.Control,
                            material.copy(render = { fill(Color.Green) }),
                        ) {
                            Box(
                                Modifier.offset(x = 80.dp)
                                    .size(80.dp)
                                    .testTag("custom")
                                    .backdropSurface(shape = RectangleShape),
                            )
                        }
                    }
                }
            }
        }
        fun pixel(tag: String): Color {
            val image = onNodeWithTag(tag).captureToImage()
            return image.toPixelMap()[image.width / 2, image.height / 2]
        }
        assertTrue(pixel("inherited").red > .98f)
        assertTrue(effectCalls > 0, "The control's optical producers must reach low-level surfaces")
        assertTrue(pixel("custom").green > .98f)
        runOnIdle { fillColor.value = Color.Blue }
        assertTrue(pixel("inherited").blue > .98f)
        assertTrue(pixel("custom").green > .98f)
    }
}
