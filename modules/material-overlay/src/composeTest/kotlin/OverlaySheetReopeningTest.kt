package top.ltfan.m3x.material.overlay

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import top.ltfan.m3x.material.TextButton
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState

class OverlaySheetReopeningTest {
    @Test
    fun persistentSheetReopensAfterProgrammaticHideAndMouseDismissal() = runComposeUiTest {
        lateinit var state: OverlayBottomSheetState
        var visible by mutableStateOf(false)
        var dismissals = 0
        var clicks = 0
        var handleLabel = ""
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(motionScheme = SheetTestMotion) {
                    handleLabel = OverlayStrings.bottomSheetDragHandleDescription()
                    OverlayHost(remember { OverlayHostState() }) {
                        state =
                            rememberOverlayBottomSheetState(
                                enabledValues =
                                    setOf(
                                        OverlayBottomSheetValue.Hidden,
                                        OverlayBottomSheetValue.Expanded,
                                    ),
                            )
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
        val body = onNodeWithTag("sheet-body")
        body.assertDoesNotExist()
        runOnIdle { visible = true }
        body.assertIsDisplayed()
        runOnIdle {
            assertEquals(OverlayBottomSheetValue.Expanded, state.currentValue)
            visible = false
        }
        body.assertDoesNotExist()
        runOnIdle {
            assertEquals(OverlayBottomSheetValue.Hidden, state.currentValue)
            assertEquals(0, dismissals)
        }

        for ((index, tag) in listOf("sheet-body", "sheet-button").withIndex()) {
            runOnIdle { visible = true }
            val start = onNodeWithTag(tag)
            start.assertIsDisplayed()
            runOnIdle { assertEquals(OverlayBottomSheetValue.Expanded, state.currentValue) }
            onAllNodesWithText(handleLabel).assertCountEquals(0)

            start.performMouseInput {
                moveTo(center)
                press()
                moveTo(center + Offset(0f, 32.dp.toPx()), delayMillis = 50)
                moveTo(center + Offset(0f, 120.dp.toPx()), delayMillis = 100)
                release()
            }
            body.assertDoesNotExist()
            runOnIdle {
                assertEquals(OverlayBottomSheetValue.Hidden, state.currentValue)
                assertFalse(visible)
                assertEquals(index + 1, dismissals)
                assertEquals(0, clicks)
            }
        }
    }

    // Fixed durations let Compose test synchronization settle sheet motion across device densities.
    private object SheetTestMotion : MotionScheme by MotionScheme.standard() {
        override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = tween(96)

        override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = tween(96)
    }
}
