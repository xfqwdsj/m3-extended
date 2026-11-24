package top.ltfan.m3x.material.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OverlaySheetStateValidationTest {
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
}
