package top.ltfan.m3x.compose.ui

import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import kotlin.test.Test
import kotlin.test.assertNull
import top.ltfan.m3x.compose.ui.layout.CornerRadii
import top.ltfan.m3x.compose.ui.layout.rememberDisplayCornerRadii
import top.ltfan.m3x.compose.ui.layout.rememberWindowCornerRadii

class CornerRadiiFallbackTest {
    @Test
    fun publicJvmReadersReturnUnknownWithoutHooksOrAHostWindow() = runComposeUiTest {
        var display: CornerRadii? = CornerRadii(topLeft = Unspecified)
        var window: CornerRadii? = CornerRadii(topLeft = Dp.Unspecified)
        setContent {
            display = rememberDisplayCornerRadii()
            window = rememberWindowCornerRadii()
        }
        waitForIdle()
        assertNull(display)
        assertNull(window)
    }
}
