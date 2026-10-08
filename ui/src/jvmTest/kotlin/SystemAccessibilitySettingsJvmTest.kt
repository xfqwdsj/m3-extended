package top.ltfan.m3x.compose.ui

import java.awt.GraphicsEnvironment
import kotlin.test.Test
import kotlin.test.assertNotNull
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.compose.ui.accessibility.SystemAccessibilitySettings
import top.ltfan.m3x.compose.ui.accessibility.rememberSystemAccessibilitySettings

class SystemAccessibilitySettingsJvmTest {
    @Test
    fun macOSReaderReturnsPublicAccessibilityValuesThroughTheMainQueue() =
        runInitializedComposeUiTest {
            if (
                !System.getProperty("os.name").startsWith("Mac", ignoreCase = true) ||
                    GraphicsEnvironment.isHeadless()
            )
                return@runInitializedComposeUiTest

            var observed: SystemAccessibilitySettings? = null
            setContent { observed = rememberSystemAccessibilitySettings() }
            waitUntil(timeoutMillis = 5000) {
                val current = observed ?: return@waitUntil false
                current.reduceTransparency != null &&
                    current.increaseContrast != null &&
                    current.reduceMotion != null
            }
            assertNotNull(observed)
            assertNotNull(observed.reduceTransparency)
            assertNotNull(observed.increaseContrast)
            assertNotNull(observed.reduceMotion)
        }
}
