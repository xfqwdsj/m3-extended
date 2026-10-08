package top.ltfan.m3x.material

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlin.test.Test
import kotlin.test.assertEquals
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.overlay.OverlayCheckableMenuItem
import top.ltfan.m3x.material.overlay.OverlayMenuItem
import top.ltfan.m3x.material.overlay.OverlaySelectableMenuItem

class OverlayMenuItemSemanticsTest {
    @Test
    fun nativeSlotsAndSelectionRolesKeepCallerOwnedState() = runInitializedComposeUiTest {
        var checked by mutableStateOf(false)
        var selected by mutableStateOf(false)
        var checks = 0
        var selections = 0
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    Column {
                        OverlayMenuItem(
                            text = { Text("Action") },
                            onClick = {},
                            modifier = Modifier.testTag("action"),
                            supportingText = { Text("Supporting") },
                            trailingContent = { Text("Shortcut") },
                        )
                        OverlayCheckableMenuItem(
                            checked,
                            {
                                checked = it
                                checks++
                            },
                            { Text("Check") },
                            Modifier.testTag("check"),
                        )
                        OverlaySelectableMenuItem(
                            selected,
                            {
                                selected = true
                                selections++
                            },
                            { Text("Select") },
                            Modifier.testTag("select"),
                        )
                        OverlayMenuItem(
                            text = { Text("Disabled") },
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.testTag("disabled"),
                        )
                    }
                }
            }
        }
        onNodeWithTag("action")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        onNodeWithText("Supporting", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("Shortcut", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag("check")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .performClick()
            .assertIsSelected()
        onNodeWithTag("select")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .performClick()
            .assertIsSelected()
        onNodeWithTag("disabled").assertIsNotEnabled()
        runOnIdle {
            assertEquals(1, checks)
            assertEquals(1, selections)
        }
    }
}
