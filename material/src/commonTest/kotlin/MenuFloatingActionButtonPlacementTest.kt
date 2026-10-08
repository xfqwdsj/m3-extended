package top.ltfan.m3x.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.overlay.MenuFloatingActionButton
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState

class MenuFloatingActionButtonPlacementTest {
    @Test
    fun callerModifierPlacesTheButtonInsideItsParent() = runInitializedComposeUiTest {
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(OverlayHostState()) {
                        Box(Modifier.size(240.dp).testTag("stage")) {
                            MenuFloatingActionButton(
                                expanded = { false },
                                onExpandedChange = {},
                                modifier = Modifier.align(Alignment.Center).testTag("fab"),
                                buttonContent = { Text("FAB") },
                                menuContent = { Text("Menu") },
                            )
                        }
                    }
                }
            }
        }
        val stage = onNodeWithTag("stage").fetchSemanticsNode().boundsInRoot
        val fab = onNodeWithTag("fab").fetchSemanticsNode().boundsInRoot
        assertTrue(
            abs(fab.center.x - stage.center.x) < 1f && abs(fab.center.y - stage.center.y) < 1f,
            "The caller's alignment is ignored: stage=$stage fab=$fab",
        )
        assertTrue(
            fab.width < stage.width / 2f,
            "The caller's modifier reached a wrapper instead of the button: fab=$fab",
        )
        onNodeWithTag("fab").assertIsDisplayed()
    }
}
