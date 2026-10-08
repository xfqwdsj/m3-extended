package top.ltfan.m3x.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import kotlin.test.Test
import kotlin.test.assertTrue
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.overlay.MenuFloatingActionButton
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState

class MenuFloatingActionButtonAnimationTest {
    @Test
    fun iconCrossfadesDuringRotationAndScale() = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        var expanded by mutableStateOf(false)
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    Box(Modifier.fillMaxSize().background(Color.Blue).testTag("icon-scene")) {
                        OverlayHost(remember { OverlayHostState() }) {
                            MenuFloatingActionButton(
                                expanded = { expanded },
                                onExpandedChange = { expanded = it },
                                modifier = Modifier.align(Alignment.Center).testTag("icon-button"),
                                containerColor = Color.Blue,
                                buttonContent = {
                                    Box(Modifier.size(20.dp).background(Color.White))
                                },
                                menuContent = { Box(Modifier.size(100.dp)) },
                            )
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(1_000)
        val center = onNodeWithTag("icon-button").fetchSemanticsNode().boundsInRoot.center
        fun centerRed(): Float =
            onNodeWithTag("icon-scene")
                .captureToImage()
                .toPixelMap()[center.x.toInt(), center.y.toInt()]
                .red

        repeat(2) {
            runOnIdle { expanded = !expanded }
            var faded = false
            val samples = mutableListOf<Float>()
            repeat(20) {
                mainClock.advanceTimeBy(16)
                val red = centerRed()
                samples += red
                if (red > 0.01f && red < 0.98f) faded = true
            }
            assertTrue(
                faded,
                "The overlapping icons must fade during both state changes: $samples at $center",
            )
            mainClock.advanceTimeBy(1_000)
            assertTrue(centerRed() > 0.98f, "The settled icon must be opaque")
        }
    }

    @Test
    fun buttonDrawsAboveOverlappingMenuThroughoutBothTransitions() = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        var expanded by mutableStateOf(false)
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    Box(Modifier.fillMaxSize().background(Color.White).testTag("layering-scene")) {
                        OverlayHost(remember { OverlayHostState() }) {
                            Box(Modifier.fillMaxSize()) {
                                MenuFloatingActionButton(
                                    expanded = { expanded },
                                    onExpandedChange = { expanded = it },
                                    modifier =
                                        Modifier.align(Alignment.Center).testTag("layering-button"),
                                    containerColor = Color.Blue,
                                    contentColor = Color.White,
                                    popupPositionProvider =
                                        object : PopupPositionProvider {
                                            override fun calculatePosition(
                                                anchorBounds: IntRect,
                                                windowSize: IntSize,
                                                layoutDirection: LayoutDirection,
                                                popupContentSize: IntSize,
                                            ): IntOffset = anchorBounds.topLeft
                                        },
                                    buttonContent = {},
                                    menuContent = {
                                        Box(
                                            Modifier.size(240.dp, 200.dp)
                                                .background(Color.Red)
                                                .testTag("layering-panel"),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        repeat(2) {
            val buttonBounds = onNodeWithTag("layering-button").fetchSemanticsNode().boundsInRoot
            val bluePoint = buttonBounds.center
            val redPoint = Offset(bluePoint.x + 100f, bluePoint.y + 100f)
            fun assertBlueButton() {
                val pixels = onNodeWithTag("layering-scene").captureToImage().toPixelMap()
                val buttonColor = pixels[bluePoint.x.toInt(), bluePoint.y.toInt()]
                assertTrue(
                    buttonColor.blue >= 0.55f && buttonColor.red <= 0.45f,
                    "The button was covered during the menu transition: $buttonColor",
                )
            }
            fun assertBlueButtonAndRedMenu() {
                val pixels = onNodeWithTag("layering-scene").captureToImage().toPixelMap()
                val buttonColor = pixels[bluePoint.x.toInt(), bluePoint.y.toInt()]
                assertTrue(
                    buttonColor.blue >= 0.55f && buttonColor.red <= 0.45f,
                    "The button was covered during the menu transition: $buttonColor",
                )
                val menuColor = pixels[redPoint.x.toInt(), redPoint.y.toInt()]
                assertTrue(
                    menuColor.red > 0.9f && menuColor.green < 0.1f,
                    "The overlapping menu panel was not visible: $menuColor",
                )
            }

            assertBlueButton()
            runOnIdle { expanded = true }
            repeat(40) {
                mainClock.advanceTimeBy(16)
                assertBlueButton()
            }
            assertBlueButtonAndRedMenu()

            runOnIdle { expanded = false }
            repeat(40) {
                mainClock.advanceTimeBy(16)
                val pixels = onNodeWithTag("layering-scene").captureToImage().toPixelMap()
                val buttonColor = pixels[bluePoint.x.toInt(), bluePoint.y.toInt()]
                assertTrue(
                    buttonColor.blue >= 0.55f && buttonColor.red <= 0.45f,
                    "The button was covered during menu exit: $buttonColor",
                )
            }
            assertBlueButton()
        }
    }

    @Test
    fun sharedPanelFadesContinuouslyAsItShrinksIntoTheFloatingActionButton() =
        runInitializedComposeUiTest {
            mainClock.autoAdvance = false
            var expanded by mutableStateOf(false)
            setContent {
                ProvideM3xMaterialRendering {
                    MaterialTheme {
                        Box(Modifier.fillMaxSize().background(Color.White).testTag("scene")) {
                            OverlayHost(remember { OverlayHostState() }) {
                                Box(Modifier.fillMaxSize().padding(80.dp)) {
                                    MenuFloatingActionButton(
                                        expanded = { expanded },
                                        onExpandedChange = { expanded = it },
                                        modifier =
                                            Modifier.align(Alignment.BottomEnd).testTag("menu-fab"),
                                        containerColor = Color.White,
                                        contentColor = Color.Black,
                                        buttonContent = { Text("FAB") },
                                        menuContent = {
                                            Box(
                                                Modifier.size(240.dp, 200.dp)
                                                    .background(Color.Red)
                                                    .testTag("panel"),
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            repeat(2) {
                runOnIdle { expanded = true }
                mainClock.advanceTimeBy(1000)
                val openBounds =
                    onNodeWithTag("panel", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                val buttonWidth = onNodeWithTag("menu-fab").fetchSemanticsNode().boundsInRoot.width
                val openOpacity = sampleSceneOpacity()
                assertTrue(openOpacity > 0.95f, "The open menu was not fully visible: $openOpacity")

                runOnIdle { expanded = false }
                var previousOpacity = openOpacity
                var lastVisibleOpacity = openOpacity
                var closestWidth = openBounds.width
                var sawIntermediateOpacity = false
                var sawIntermediateBounds = false
                var panelDisappeared = false
                for (frame in 0 until 50) {
                    mainClock.advanceTimeBy(16)
                    // Exit drawing remains composed after its interactive semantics close.
                    val panels =
                        onAllNodesWithTag("panel", useUnmergedTree = true).fetchSemanticsNodes()
                    if (panels.isEmpty()) {
                        panelDisappeared = true
                        break
                    }
                    val opacity = sampleSceneOpacity()
                    val panelWidth = panels.single().boundsInRoot.width
                    assertTrue(
                        opacity <= previousOpacity + 0.05f,
                        "The panel opacity did not descend continuously: " +
                            "previous=$previousOpacity current=$opacity",
                    )
                    previousOpacity = opacity
                    lastVisibleOpacity = opacity
                    // Shared bounds can release layout during a fully transparent disposal frame.
                    closestWidth = minOf(closestWidth, panelWidth)
                    if (opacity in 0.1f..0.9f) {
                        sawIntermediateOpacity = true
                        if (panelWidth < openBounds.width && panelWidth > buttonWidth) {
                            sawIntermediateBounds = true
                        }
                    }
                }
                assertTrue(panelDisappeared, "The shared panel did not leave the composition")
                assertTrue(
                    sawIntermediateOpacity,
                    "The panel did not fade through an intermediate opacity",
                )
                assertTrue(
                    sawIntermediateBounds,
                    "The fading panel did not shrink between its open size and button",
                )
                assertTrue(
                    lastVisibleOpacity < 0.03f,
                    "The panel was removed before its fade completed: $lastVisibleOpacity",
                )
                assertTrue(
                    closestWidth <= buttonWidth + 2f,
                    "The bounds never reached the button before the panel was removed: " +
                        "panel=$closestWidth button=$buttonWidth",
                )
            }
        }

    private fun ComposeUiTest.sampleSceneOpacity(): Float =
        onNodeWithTag("scene").captureToImage().toPixelMap().let { pixels ->
            var maximum = 0f
            for (y in 0 until pixels.height) {
                for (x in 0 until pixels.width) {
                    val color = pixels[x, y]
                    val opacity = color.red - (color.green + color.blue) / 2f
                    if (opacity > maximum) maximum = opacity
                }
            }
            maximum
        }
}
