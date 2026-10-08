package top.ltfan.m3x.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFalse
import top.ltfan.backdrop.BackdropStyle
import top.ltfan.backdrop.highlight.Highlight
import top.ltfan.backdrop.shadow.Shadow
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.backdrop.theme.BackdropAdaptation
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial
import top.ltfan.m3x.material.backdrop.theme.ProvideBackdropMaterial
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.overlay.MenuFloatingActionButton
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState

class FabAppearanceTest {
    @Test
    fun closingMenuKeepsTheAdaptedForeground() = runInitializedComposeUiTest {
        val expanded = mutableStateOf(false)
        val colors = mutableListOf<Color>()
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    val source = rememberBackdropLayer(Color.Black)
                    BackdropHost(source) {
                        OverlayHost(OverlayHostState()) {
                            Box(Modifier.size(240.dp, 240.dp)) {
                                Box(
                                    Modifier.fillMaxSize()
                                        .captureBackdrop(source)
                                        .background(Color.Black),
                                )
                                ProvideBackdropMaterial(
                                    MaterialSurfaceRole.Control,
                                    M3xBackdropMaterial(
                                        style =
                                            BackdropStyle(
                                                effects = BackdropStyle.NoEffects,
                                                highlight = { Highlight.None },
                                                shadow = { Shadow.None },
                                            ),
                                        blurRadius = 0.dp,
                                        tintAlpha = 0f,
                                        adaptation = BackdropAdaptation.Background,
                                        adaptiveContentColor = true,
                                        interactiveLighting = false,
                                    ),
                                ) {
                                    MenuFloatingActionButton(
                                        expanded = { expanded.value },
                                        onExpandedChange = { expanded.value = it },
                                        containerColor = Color.Black,
                                        contentColor = Color.Black,
                                        buttonContent = {
                                            val color = LocalContentColor.current
                                            SideEffect { colors.add(color) }
                                            Text("FAB")
                                        },
                                        menuContent = { Text("Menu") },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { colors.lastOrNull() == Color.White }
        runOnIdle { expanded.value = true }
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { colors.lastOrNull() == Color.White }
        runOnIdle {
            colors.clear()
            expanded.value = false
        }
        waitForIdle()
        assertFalse(
            colors.contains(Color.Black),
            "Closing returned to the unadapted foreground: $colors",
        )
    }
}
