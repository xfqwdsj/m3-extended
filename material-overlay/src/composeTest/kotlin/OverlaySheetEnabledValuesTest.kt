package top.ltfan.m3x.material.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState

class OverlaySheetEnabledValuesTest {

    @Test
    fun shortSheetRetainsEnabledPartialAnchorAtItsExpandedPosition() = runComposeUiTest {
        lateinit var state: OverlayBottomSheetState
        val contentHeight = mutableStateOf(80.dp)
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(remember { OverlayHostState() }) {
                        state = rememberOverlayBottomSheetState()
                        OverlayBottomSheet(
                            onDismissRequest = {},
                            sheetState = state,
                            dragHandle = null,
                        ) {
                            Box(Modifier.fillMaxWidth().height(contentHeight.value))
                        }
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { state.isVisible && !state.isAnimationRunning }
        runOnIdle {
            assertTrue(state.hasPartiallyExpandedState)
            assertEquals(
                state.anchorOffset(OverlayBottomSheetValue.Expanded),
                state.anchorOffset(OverlayBottomSheetValue.PartiallyExpanded),
            )
        }
        runOnIdle { contentHeight.value = 800.dp }
        waitUntil(timeoutMillis = 5_000) {
            state.currentValue == OverlayBottomSheetValue.Expanded && !state.isAnimationRunning
        }
    }
}
