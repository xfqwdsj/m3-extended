package top.ltfan.m3x.overlay

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import top.ltfan.m3x.backdrop.BackdropLayerHandle
import top.ltfan.m3x.backdrop.LocalBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.overlay.MaterialOverlay
import top.ltfan.m3x.material.overlay.OverlayDialog
import top.ltfan.m3x.material.overlay.OverlayDialogProperties

class OverlayFallbackTest {
    @Test
    fun hiddenOverlayWithoutHostFailsOutsideInspectionMode() = runInitializedComposeUiTest {
        val error =
            assertFailsWith<IllegalStateException> {
                setContent {
                    ProvideM3xMaterialRendering {
                        MaterialTheme {
                            MaterialOverlay(OverlayHandle(), modal = false, visible = { false }) {
                                Text("Hidden overlay")
                            }
                        }
                    }
                }
                waitForIdle()
            }
        assertTrue(error.message.orEmpty().contains("OverlayHost is required"))
    }

    @Test
    fun inspectionModeCanComposeOverlayWithoutHost() = runInitializedComposeUiTest {
        setContent {
            ProvideM3xMaterialRendering {
                CompositionLocalProvider(LocalInspectionMode provides true) {
                    MaterialTheme {
                        MaterialOverlay(OverlayHandle(), modal = false) { Text("Preview overlay") }
                    }
                }
            }
        }
        onNodeWithText("Preview overlay").assertIsDisplayed()
    }

    @Test
    fun overlayOnlyProvidesStackHandleWhenPageBackdropExists() = runInitializedComposeUiTest {
        var providePage by mutableStateOf(false)
        var observed: BackdropLayerHandle? = null
        var compositions = 0
        val hostState = OverlayHostState()
        val entry = OverlayHandle()
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    val page = rememberBackdropLayer()
                    CompositionLocalProvider(
                        LocalBackdrop provides if (providePage) page else null,
                    ) {
                        OverlayHost(hostState) {
                            MaterialOverlay(entry, modal = false) {
                                val current = LocalBackdrop.current
                                SideEffect {
                                    observed = current
                                    compositions++
                                }
                                Text("Hosted overlay")
                            }
                        }
                    }
                }
            }
        }

        waitUntil { compositions > 0 && hostState.entries.size == 1 }
        assertNull(observed)
        onNodeWithText("Hosted overlay").assertIsDisplayed()

        runOnIdle { providePage = true }
        waitUntil { observed != null }
        assertEquals(1, hostState.entries.size)

        runOnIdle { providePage = false }
        waitUntil { observed == null }
        assertEquals(1, hostState.entries.size)

        runOnIdle { providePage = true }
        waitUntil { observed != null }
        assertEquals(1, hostState.entries.size)
    }

    @Test
    fun dialogWithoutPageBackdropPassesAbsenceToItsContent() = runInitializedComposeUiTest {
        var observed: BackdropLayerHandle? = null
        var composed = false
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(OverlayHostState()) {
                        OverlayDialog(
                            onDismissRequest = {},
                            properties =
                                OverlayDialogProperties(
                                    usePlatformInsets = false,
                                    usePlatformDefaultWidth = false,
                                    animateTransition = false,
                                ),
                            containerColor = Color.Blue,
                        ) {
                            val current = LocalBackdrop.current
                            SideEffect {
                                observed = current
                                composed = true
                            }
                            Text("Dialog fallback")
                        }
                    }
                }
            }
        }
        waitUntil { composed }
        assertNull(observed)
        onNodeWithText("Dialog fallback").assertIsDisplayed()
    }
}
