package top.ltfan.m3x.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering

class ToggleButtonSizeTest {
    @Test
    fun everyVariantUsesTheRequestedSizeAndNativeIconSlot() = runInitializedComposeUiTest {
        var size by mutableStateOf(ToggleButtonSize.Small)
        var selected by mutableStateOf(false)
        var changes = 0
        val change: (Boolean) -> Unit = {
            selected = it
            changes++
        }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    Column {
                        ToggleButton(
                            selected,
                            change,
                            Modifier.testTag("filled"),
                            buttonSize = size,
                            icon = { Box(Modifier.fillMaxSize().testTag("icon")) },
                        ) {
                            Text("Filled")
                        }
                        ElevatedToggleButton(
                            false,
                            {},
                            Modifier.testTag("elevated"),
                            buttonSize = size,
                        ) {
                            Text("Elevated")
                        }
                        FilledTonalToggleButton(
                            false,
                            {},
                            Modifier.testTag("tonal"),
                            buttonSize = size,
                        ) {
                            Text("Tonal")
                        }
                        OutlinedToggleButton(
                            false,
                            {},
                            Modifier.testTag("outlined"),
                            buttonSize = size,
                        ) {
                            Text("Outlined")
                        }
                    }
                }
            }
        }
        val smallIcon =
            onNodeWithTag("icon", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.size
        runOnIdle { size = ToggleButtonSize.Large }
        for (tag in listOf("filled", "elevated", "tonal", "outlined")) {
            onNodeWithTag(tag)
                .assertIsToggleable()
                .assertHeightIsAtLeast(ToggleButtonSize.Large.height)
        }
        val largeIcon =
            onNodeWithTag("icon", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.size
        assertTrue(largeIcon.width > smallIcon.width)
        onNodeWithTag("filled").performClick().assertIsOn()
        runOnIdle { assertEquals(1, changes) }
    }
}
