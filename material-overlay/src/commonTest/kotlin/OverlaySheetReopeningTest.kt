package top.ltfan.m3x.material.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.TextButton
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState

class OverlaySheetReopeningTest {
    @Test
    fun persistentSheetShowsOnEveryVisibilityChange() = runInitializedComposeUiTest {
        var visible by mutableStateOf(false)
        lateinit var state: OverlayBottomSheetState
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(OverlayHostState()) {
                        state = rememberOverlayBottomSheetState(skipPartiallyExpanded = true)
                        OverlayBottomSheet(
                            onDismissRequest = { visible = false },
                            sheetState = state,
                            visible = { visible },
                            dragHandle = null,
                        ) {
                            Box(Modifier.fillMaxWidth().height(160.dp)) { Text("Repeated sheet") }
                        }
                    }
                }
            }
        }
        repeat(3) {
            runOnIdle { visible = true }
            waitUntil { state.currentValue == OverlayBottomSheetValue.Expanded }
            onNodeWithText("Repeated sheet").assertIsDisplayed()
            runOnIdle { visible = false }
            waitUntil { state.currentValue == OverlayBottomSheetValue.Hidden }
        }
    }

    @Test
    fun mouseCanDismissFromContentAndButtonsWithoutAutomaticTooltip() =
        runInitializedComposeUiTest {
            lateinit var state: OverlayBottomSheetState
            var visible by mutableStateOf(true)
            var dismissals = 0
            var clicks = 0
            var handleLabel = ""
            setContent {
                ProvideM3xMaterialRendering {
                    MaterialTheme {
                        handleLabel = OverlayStrings.bottomSheetDragHandleDescription()
                        OverlayHost(OverlayHostState()) {
                            state = rememberOverlayBottomSheetState(skipPartiallyExpanded = true)
                            OverlayBottomSheet(
                                onDismissRequest = {
                                    dismissals++
                                    visible = false
                                },
                                sheetState = state,
                                visible = { visible },
                            ) {
                                Box(Modifier.fillMaxWidth().height(80.dp).testTag("sheet-body")) {
                                    Text("Body")
                                }
                                TextButton(
                                    onClick = { clicks++ },
                                    modifier = Modifier.fillMaxWidth().testTag("sheet-button"),
                                ) {
                                    Text("Action")
                                }
                            }
                        }
                    }
                }
            }
            for (tag in listOf("sheet-body", "sheet-button")) {
                runOnIdle { visible = true }
                waitUntil { state.currentValue == OverlayBottomSheetValue.Expanded }
                onAllNodesWithText(handleLabel).assertCountEquals(0)
                onNodeWithTag(tag).performMouseInput {
                    moveTo(center)
                    press()
                    moveTo(center + Offset(0f, 30f), delayMillis = 50)
                    moveTo(center + Offset(0f, 260f), delayMillis = 100)
                    release()
                }
                waitUntil { !visible && state.currentValue == OverlayBottomSheetValue.Hidden }
            }
            runOnIdle {
                assertEquals(2, dismissals)
                assertEquals(0, clicks)
            }
        }
}
