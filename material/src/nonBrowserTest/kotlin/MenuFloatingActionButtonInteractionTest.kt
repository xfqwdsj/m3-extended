package top.ltfan.m3x.material

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.backdrop.BackdropStyle
import top.ltfan.backdrop.highlight.Highlight
import top.ltfan.backdrop.shadow.Shadow
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.backdrop.BackdropSurface
import top.ltfan.m3x.material.backdrop.theme.BackdropAdaptation
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropLighting
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropTheme
import top.ltfan.m3x.material.bundle.M3xMaterialTheme
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.overlay.MenuFloatingActionButton
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState

class MenuFloatingActionButtonInteractionTest {
    @Test
    fun releaseLeavesNoSharedInteractionLight() = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        val interactions = MutableInteractionSource()
        setContent {
            ProvideM3xMaterialRendering {
                M3xMaterialTheme(
                    backdrop =
                        M3xBackdropTheme(
                            lighting =
                                M3xBackdropLighting(
                                    color = Color.Red,
                                    radius = 320.dp,
                                    interiorAlpha = 1f,
                                    edgeAlpha = 0f,
                                ),
                        ),
                ) {
                    MaterialTheme {
                        val expanded = remember { mutableStateOf(false) }
                        val source = rememberBackdropLayer(Color.Black)
                        val material =
                            M3xBackdropMaterial(
                                style =
                                    BackdropStyle(
                                        effects = BackdropStyle.NoEffects,
                                        highlight = { Highlight.None },
                                        shadow = { Shadow.None },
                                    ),
                                blurRadius = 0.dp,
                                tintAlpha = 0f,
                                adaptation = BackdropAdaptation.Fixed,
                                interactiveLighting = true,
                            )
                        BackdropHost(source) {
                            OverlayHost(OverlayHostState()) {
                                Box(Modifier.size(240.dp).testTag("stage")) {
                                    Box(
                                        Modifier.fillMaxSize()
                                            .captureBackdrop(source)
                                            .background(Color.Black),
                                    )
                                    BackdropSurface(
                                        modifier =
                                            Modifier.offset(x = 140.dp, y = 80.dp)
                                                .size(16.dp)
                                                .testTag("light-target"),
                                        shape = RectangleShape,
                                        material = material,
                                        containerColor = Color.Black,
                                    ) {}
                                    MenuFloatingActionButton(
                                        expanded = { expanded.value },
                                        onExpandedChange = { expanded.value = it },
                                        modifier = Modifier.testTag("menu-fab"),
                                        interactionSource = interactions,
                                        containerColor = Color.Black,
                                        contentColor = Color.White,
                                        buttonContent = { Text("FAB") },
                                        menuContent = {
                                            Text("Menu", Modifier.testTag("menu-content"))
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        waitForIdle()
        val targetCenter = onNodeWithTag("light-target").fetchSemanticsNode().boundsInRoot.center
        fun targetRed(): Float =
            onNodeWithTag("stage")
                .captureToImage()
                .toPixelMap()[
                    targetCenter.x.toInt(),
                    targetCenter.y.toInt(),
                ]
                .red

        val before = targetRed()
        onNodeWithTag("menu-fab").performTouchInput { down(Offset(24.dp.toPx(), 24.dp.toPx())) }
        waitForIdle()
        val pressed = targetRed()
        onNodeWithTag("menu-fab").performTouchInput { up() }
        mainClock.advanceTimeBy(600)
        waitForIdle()
        onNodeWithTag("menu-content").assertExists()
        mainClock.advanceTimeBy(400)
        waitForIdle()
        val released = targetRed()

        assertTrue(pressed > before + 0.02f, "The receiver did not show the FAB press light.")
        assertTrue(
            abs(released - before) < 0.01f,
            "The receiver stayed lit after the FAB was released: before=$before released=$released",
        )
    }
}
