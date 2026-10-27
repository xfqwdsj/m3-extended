package top.ltfan.m3x.material.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState

class OverlaySheetEnabledValuesTest {
    @Test
    fun expandedAndInitialValuesMustBeEnabled() {
        assertFailsWith<IllegalArgumentException> {
            OverlayBottomSheetState(setOf(OverlayBottomSheetValue.Hidden), { 56f }, { 125f })
        }
        assertFailsWith<IllegalArgumentException> {
            OverlayBottomSheetState(setOf(OverlayBottomSheetValue.Expanded), { 56f }, { 125f })
        }
        val state =
            OverlayBottomSheetState(
                setOf(OverlayBottomSheetValue.Expanded),
                { 56f },
                { 125f },
                initialValue = OverlayBottomSheetValue.Expanded,
            )
        assertEquals(OverlayBottomSheetValue.Expanded, state.currentValue)
    }

    @Test
    fun shortSheetRetainsEnabledPartialAnchorAtItsExpandedPosition() = runInitializedComposeUiTest {
        lateinit var state: OverlayBottomSheetState
        val contentHeight = mutableStateOf(80.dp)
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(OverlayHostState()) {
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
        waitUntil { state.isVisible && !state.isAnimationRunning }
        runOnIdle {
            assertTrue(state.hasPartiallyExpandedState)
            assertEquals(
                state.anchorOffset(OverlayBottomSheetValue.Expanded),
                state.anchorOffset(OverlayBottomSheetValue.PartiallyExpanded),
            )
        }
        runOnIdle { contentHeight.value = 800.dp }
        waitUntil {
            state.currentValue == OverlayBottomSheetValue.Expanded && !state.isAnimationRunning
        }
    }
}
