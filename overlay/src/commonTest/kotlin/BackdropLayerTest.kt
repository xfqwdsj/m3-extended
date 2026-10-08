package top.ltfan.m3x.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import top.ltfan.backdrop.highlight.Highlight
import top.ltfan.backdrop.shadow.Shadow
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.BackdropLayerHandle
import top.ltfan.m3x.backdrop.LocalBackdrop
import top.ltfan.m3x.backdrop.backdropSurface
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.Button
import top.ltfan.m3x.material.FloatingBottomBar
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.item
import top.ltfan.m3x.material.overlay.MaterialOverlay
import top.ltfan.m3x.material.overlay.OverlayDialog

class BackdropLayerTest {
    @Test
    fun pageHandleAndLayerSurviveRecompositionAndBackgroundChanges() = runInitializedComposeUiTest {
        var tick by mutableIntStateOf(0)
        var background by mutableStateOf<Color?>(null)
        var active by mutableStateOf(true)
        var current: BackdropLayerHandle? = null

        setContent {
            ProvideM3xMaterialRendering {
                if (active) {
                    val label = tick
                    val handle = rememberBackdropLayer(background)
                    SideEffect { current = handle }
                    Text("Frame $label")
                }
            }
        }

        val original = current!!
        runOnIdle { tick++ }
        waitForIdle()
        assertSame(original, current)
        assertSame(original.layer, current.layer)

        runOnIdle { background = Color.Red }
        waitForIdle()
        assertSame(original, current)
        assertSame(original.layer, current.layer)

        runOnIdle { tick++ }
        waitForIdle()
        assertSame(original, current)
        assertSame(original.layer, current.layer)

        runOnIdle {
            background = Color.Blue
            tick++
        }
        waitForIdle()
        assertSame(original, current)
        assertSame(original.layer, current.layer)

        runOnIdle { active = false }
        waitForIdle()
        runOnIdle { active = true }
        waitForIdle()
        assertNotSame(original, current)
    }

    @Test
    fun pageTemplateConnectsBuiltInBarAndOverlay() = runInitializedComposeUiTest {
        var dialogOpen by mutableStateOf(false)
        var selected by mutableIntStateOf(0)

        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    val pageLayer = rememberBackdropLayer()
                    val overlayState = remember { OverlayHostState() }
                    BackdropHost(pageLayer) {
                        OverlayHost(overlayState) {
                            Box(Modifier.fillMaxSize()) {
                                Box(
                                    Modifier.fillMaxSize()
                                        .captureBackdrop(pageLayer)
                                        .background(MaterialTheme.colorScheme.background),
                                ) {
                                    Button(onClick = { dialogOpen = true }) { Text("Open dialog") }
                                }
                                FloatingBottomBar(
                                    selectedTabIndex = selected,
                                    onTabSelected = { selected = it },
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                ) {
                                    item { Text("Home") }
                                    item { Text("Settings") }
                                }
                                OverlayDialog(
                                    onDismissRequest = { dialogOpen = false },
                                    visible = { dialogOpen },
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ) {
                                    Text("Dialog content")
                                }
                            }
                        }
                    }
                }
            }
        }

        onNodeWithText("Open dialog").performClick()
        onNodeWithText("Dialog content").assertIsDisplayed()
    }

    @Test
    fun overlayHandlesSurvivePageRecompositionAndFollowActualStackChanges() =
        runInitializedComposeUiTest {
            var tick by mutableIntStateOf(0)
            var showSecond by mutableStateOf(false)
            var page: BackdropLayerHandle? = null
            var first: BackdropLayerHandle? = null
            var second: BackdropLayerHandle? = null
            val hostState = OverlayHostState()

            setContent {
                ProvideM3xMaterialRendering {
                    MaterialTheme {
                        val label = tick
                        val pageLayer = rememberBackdropLayer()
                        SideEffect { page = pageLayer }
                        BackdropHost(pageLayer) {
                            OverlayHost(hostState) {
                                Box(Modifier.size(96.dp)) {
                                    Box(
                                        Modifier.fillMaxSize()
                                            .captureBackdrop(pageLayer)
                                            .background(Color.Red),
                                    ) {
                                        Text("Page $label")
                                    }
                                    val firstEntry = remember { OverlayHandle() }
                                    MaterialOverlay(firstEntry, modal = false) {
                                        val handle = LocalBackdrop.current!!
                                        SideEffect { first = handle }
                                        Box(Modifier.size(48.dp).background(Color.Green))
                                    }
                                    if (showSecond) {
                                        val secondEntry = remember { OverlayHandle() }
                                        MaterialOverlay(secondEntry, modal = false) {
                                            val handle = LocalBackdrop.current!!
                                            SideEffect { second = handle }
                                            Box(
                                                Modifier.size(48.dp)
                                                    .backdropSurface(
                                                        shape = RectangleShape,
                                                        effects = {},
                                                        highlight = { Highlight.None },
                                                        shadow = { Shadow.None },
                                                    ),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            waitUntil { first != null && hostState.entries.size == 1 }
            val originalPage = page!!
            val originalFirst = first!!
            runOnIdle { tick++ }
            waitForIdle()
            assertSame(originalPage, page)
            assertSame(originalFirst, first)

            runOnIdle { showSecond = true }
            waitUntil { second != null && hostState.entries.size == 2 }
            val originalSecond = second!!
            waitUntil { originalFirst.isSampling }
            runOnIdle { tick++ }
            waitForIdle()
            assertSame(originalPage, page)
            assertSame(originalFirst, first)
            assertSame(originalSecond, second)

            runOnIdle { showSecond = false }
            waitUntil { hostState.entries.size == 1 }
            assertSame(originalFirst, first)
            runOnIdle { showSecond = true }
            waitUntil { hostState.entries.size == 2 && second !== originalSecond }
            assertSame(originalPage, page)
            assertSame(originalFirst, first)
        }
}
