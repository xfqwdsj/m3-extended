package top.ltfan.m3x.compose.ui.privateapi

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.compose.ui.layout.CornerRadii
import top.ltfan.m3x.compose.ui.layout.CornerRadiiHook
import top.ltfan.m3x.compose.ui.layout.LocalDisplayCornerRadiiHook
import top.ltfan.m3x.compose.ui.layout.LocalWindowCornerRadiiHook

class PrivateHookRegistrationTest {
    @Test
    fun providerRegistersOnlyTheMacWindowHookAndPreservesOuterScope() =
        runInitializedComposeUiTest {
            val parentDisplay: CornerRadiiHook = { CornerRadii(11.dp) }
            val parentWindow: CornerRadiiHook = { CornerRadii(13.dp) }
            var beforeWindow: CornerRadiiHook? = null
            var providedDisplay: CornerRadiiHook? = null
            var providedWindow: CornerRadiiHook? = null
            var afterWindow: CornerRadiiHook? = null

            setContent {
                CompositionLocalProvider(
                    LocalDisplayCornerRadiiHook provides parentDisplay,
                    LocalWindowCornerRadiiHook provides parentWindow,
                ) {
                    beforeWindow = LocalWindowCornerRadiiHook.current
                    ProvidePrivateCornerRadii {
                        providedDisplay = LocalDisplayCornerRadiiHook.current
                        providedWindow = LocalWindowCornerRadiiHook.current
                    }
                    afterWindow = LocalWindowCornerRadiiHook.current
                }
            }
            waitForIdle()

            if (System.getProperty("os.name").contains("mac", ignoreCase = true)) {
                assertSame(parentDisplay, providedDisplay)
                assertNotSame(parentWindow, providedWindow)
            } else {
                assertSame(parentDisplay, providedDisplay)
                assertSame(parentWindow, providedWindow)
            }
            assertSame(parentWindow, beforeWindow)
            assertSame(parentWindow, afterWindow)
        }
}
