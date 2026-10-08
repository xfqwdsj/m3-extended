package top.ltfan.m3x.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.overlay.OverlayAlertDialogButtons
import top.ltfan.m3x.material.overlay.OverlayAlertDialogContent

class OverlayAlertDialogLayoutTest {
    @Test
    fun basicDialogUsesMaterialSlotPaddingAndActionSpacing() = runInitializedComposeUiTest {
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayAlertDialogContent(
                        modifier = Modifier.width(320.dp).testTag("dialog"),
                        icon = null,
                        title = { Box(Modifier.size(32.dp, 20.dp).testTag("title")) },
                        text = { Box(Modifier.size(32.dp, 20.dp).testTag("body")) },
                        buttons = {
                            OverlayAlertDialogButtons(
                                confirmButton = {
                                    Box(Modifier.size(32.dp, 20.dp).testTag("confirm"))
                                },
                                dismissButton = {
                                    Box(Modifier.size(32.dp, 20.dp).testTag("dismiss"))
                                },
                            )
                        },
                        buttonContentColor = Color.Black,
                        iconContentColor = Color.Black,
                        titleContentColor = Color.Black,
                        textContentColor = Color.Black,
                    )
                }
            }
        }
        val dialog = onNodeWithTag("dialog").fetchSemanticsNode().boundsInRoot
        val title = onNodeWithTag("title").fetchSemanticsNode().boundsInRoot
        val body = onNodeWithTag("body").fetchSemanticsNode().boundsInRoot
        val confirm = onNodeWithTag("confirm").fetchSemanticsNode().boundsInRoot
        val dismiss = onNodeWithTag("dismiss").fetchSemanticsNode().boundsInRoot
        val density = dialog.width / 320f
        assertEquals(24f, (title.left - dialog.left) / density, 0.5f)
        assertEquals(24f, (title.top - dialog.top) / density, 0.5f)
        assertEquals(16f, (body.top - title.bottom) / density, 0.5f)
        assertEquals(24f, (confirm.top - body.bottom) / density, 0.5f)
        assertEquals(8f, (confirm.left - dismiss.right) / density, 0.5f)
        assertEquals(24f, (dialog.right - confirm.right) / density, 0.5f)
        assertEquals(24f, (dialog.bottom - confirm.bottom) / density, 0.5f)
    }
}
