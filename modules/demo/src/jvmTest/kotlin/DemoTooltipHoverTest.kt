package top.ltfan.m3x.demo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import top.ltfan.m3x.demo.generated.resources.MR
import top.ltfan.m3x.demo.generated.resources.demo_persistent_glass_tooltip
import top.ltfan.m3x.demo.generated.resources.demo_tooltip
import top.ltfan.m3x.material.bundle.M3xMaterialTheme
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState

class DemoTooltipHoverTest {
    @Test
    fun plainTooltipAnchorRemainsUsableWhileHovered() = runComposeUiTest {
        val anchorLabel = MR.strings.demo_tooltip.localized()
        val tooltipLabel = MR.strings.demo_persistent_glass_tooltip.localized()
        setContent {
            M3xMaterialTheme { OverlayHost(OverlayHostState()) { OverlaysPage() } }
        }
        mainClock.autoAdvance = false
        val anchor = onNodeWithText(anchorLabel)
        anchor.performMouseInput { moveTo(center) }
        mainClock.advanceTimeBy(200)
        anchor.performMouseInput { moveTo(center + Offset(1f, 0f)) }
        mainClock.advanceTimeBy(200)
        onNodeWithText(tooltipLabel).assertIsDisplayed()
        anchor.performMouseInput { click() }
        mainClock.advanceTimeBy(200)
        onNodeWithText(tooltipLabel).assertIsDisplayed()
    }
}
