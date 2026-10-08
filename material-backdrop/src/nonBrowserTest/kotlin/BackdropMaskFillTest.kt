package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.backdrop.BackdropMask
import top.ltfan.m3x.backdrop.LocalBackdrop
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.compose.ui.accessibility.AccessibilityPreference
import top.ltfan.m3x.compose.ui.accessibility.AccessibilityPreferences
import top.ltfan.m3x.compose.ui.accessibility.ProvideAccessibility
import top.ltfan.m3x.material.backdrop.theme.LocalM3xBackdropTheme
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropTheme
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.theme.LocalM3xThemeExtensions
import top.ltfan.m3x.material.theme.M3xThemeExtensions

class BackdropMaskFillTest {
    @Test
    fun flattenedMasksKeepTheirFillWithAndWithoutSampling() {
        for (sampled in listOf(false, true)) for (gradient in
            listOf(false, true)) runInitializedComposeUiTest {
            setContent {
                ProvideM3xMaterialRendering {
                    MaterialTheme {
                        val background = rememberBackdropLayer()
                        val color = Color.Blue.copy(alpha = .5f)
                        val mask =
                            if (gradient) {
                                BackdropMask(
                                    brush = Brush.verticalGradient(listOf(color, color)),
                                    alpha = 1f,
                                )
                            } else BackdropMask(color, alpha = 1f)
                        ProvideAccessibility(
                            AccessibilityPreferences(
                                reduceTransparency = AccessibilityPreference.Enabled,
                            ),
                        ) {
                            CompositionLocalProvider(
                                LocalM3xBackdropTheme provides M3xBackdropTheme(),
                                LocalM3xThemeExtensions provides
                                    M3xThemeExtensions(
                                        accessibilityPreferences =
                                            AccessibilityPreferences(
                                                reduceTransparency =
                                                    AccessibilityPreference.Enabled,
                                            ),
                                    ),
                                LocalBackdrop provides if (sampled) background else null,
                            ) {
                                Box(Modifier.size(240.dp, 160.dp).testTag("root")) {
                                    Box(
                                        Modifier.fillMaxSize()
                                            .captureBackdrop(background)
                                            .background(Color.Black),
                                    )
                                    BackdropTopAppBar(
                                        {},
                                        windowInsets = WindowInsets(0, 0, 0, 0),
                                        mask = mask,
                                        effects = {},
                                    )
                                }
                            }
                        }
                    }
                }
            }
            val pixel = onNodeWithTag("root").captureToImage().toPixelMap()[120, 20]
            assertTrue(
                pixel.red < .01f && pixel.green < .01f && abs(pixel.blue - .5f) < .01f,
                "sampled=$sampled gradient=$gradient pixel=$pixel",
            )
        }
    }
}
