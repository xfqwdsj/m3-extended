package top.ltfan.m3x.material.backdrop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestResult
import top.ltfan.backdrop.BackdropStyle
import top.ltfan.backdrop.highlight.Highlight
import top.ltfan.backdrop.shadow.Shadow
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.material.FloatingBottomBar
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.backdrop.theme.BackdropAdaptation
import top.ltfan.m3x.material.backdrop.theme.M3xBackdropMaterial
import top.ltfan.m3x.material.backdrop.theme.ProvideBackdropMaterial
import top.ltfan.m3x.material.bundle.M3xMaterialTheme
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.item

class FloatingBottomBarRenderingTest {
    @Test
    fun enlargedIndicatorSamplesThePageAboveAndBelowTheTabs() = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    val source = rememberBackdropLayer(Color.Magenta)
                    BackdropHost(source) {
                        Box(Modifier.size(320.dp, 160.dp).testTag("root"), Alignment.Center) {
                            Box(Modifier.fillMaxSize().captureBackdrop(source))
                            ProvideBackdropMaterial(
                                MaterialSurfaceRole.Control,
                                M3xBackdropMaterial(
                                    style =
                                        BackdropStyle(
                                            effects = {},
                                            highlight = { Highlight.None },
                                            shadow = { Shadow.None },
                                        ),
                                    tint = Color.Gray,
                                    tintAlpha = 1f,
                                    blurRadius = 0.dp,
                                    adaptation = BackdropAdaptation.Fixed,
                                    adaptiveContentColor = false,
                                    interactiveLighting = false,
                                ),
                            ) {
                                FloatingBottomBar(
                                    1,
                                    {},
                                    Modifier.testTag("bar"),
                                    indicatorEffects = backdropIndicatorEffects {},
                                ) {
                                    repeat(3) { item { Box(Modifier.size(64.dp, 24.dp)) } }
                                }
                            }
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(500)
        val bar = onNodeWithTag("bar")
        val bounds = bar.fetchSemanticsNode().boundsInRoot
        bar.performTouchInput { down(Offset(width / 2f, height / 2f)) }
        mainClock.advanceTimeBy(1000)
        val pixels = onNodeWithTag("root").captureToImage().toPixelMap()
        val x = bounds.center.x.toInt()
        for (y in listOf(bounds.top.toInt() - 4, bounds.bottom.toInt() + 3)) {
            val color = pixels[x, y]
            assertTrue(
                color.red > 0.9f && color.blue > 0.9f && color.green < 0.1f,
                "The expanded indicator samples the page at ($x, $y): $color",
            )
        }
        val interior = pixels[x, bounds.center.y.toInt()]
        assertTrue(
            abs(interior.red - interior.green) < 0.02f && interior.green in 0.45f..0.6f,
            "The recorded tab material remains gray: $interior",
        )
        bar.performTouchInput { up() }
    }

    private data class IndicatorMeasurement(
        val rawCenter: Float,
        val anchorCenter: Float,
    ) {
        val relativeCenter: Float
            get() = rawCenter - anchorCenter
    }

    @Test
    fun externalDragRetargetsTheSelectionAnimationWithoutJumping(): TestResult =
        externalDragCase(LayoutDirection.Ltr, mouse = false)

    @Test
    fun externalDragRetargetsTheSelectionAnimationWithoutJumpingLtrMouse(): TestResult =
        externalDragCase(LayoutDirection.Ltr, mouse = true)

    @Test
    fun externalDragRetargetsTheSelectionAnimationWithoutJumpingRtlTouch(): TestResult =
        externalDragCase(LayoutDirection.Rtl, mouse = false)

    @Test
    fun externalDragRetargetsTheSelectionAnimationWithoutJumpingRtlMouse(): TestResult =
        externalDragCase(LayoutDirection.Rtl, mouse = true)

    private fun externalDragCase(direction: LayoutDirection, mouse: Boolean): TestResult =
        runInitializedComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                ProvideM3xMaterialRendering {
                    CompositionLocalProvider(LocalLayoutDirection provides direction) {
                        MaterialTheme(
                            colorScheme =
                                lightColorScheme(
                                    surface = Color.White,
                                    surfaceContainerHighest = Color.Red,
                                    outline = Color.Red,
                                ),
                        ) {
                            Box(Modifier.size(360.dp, 100.dp).background(Color.White)) {
                                FloatingBottomBar(0, {}, Modifier.testTag("bar")) {
                                    repeat(3) { index ->
                                        item {
                                            Box(
                                                Modifier.size(64.dp, 24.dp)
                                                    .testTag("tab-anchor-$index"),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(500)
            val bar = onNodeWithTag("bar")
            val bounds = bar.fetchSemanticsNode().boundsInRoot
            val tabWidth = (bounds.width - 8f) / 3f
            fun center(index: Int): Offset =
                Offset(
                    if (direction == LayoutDirection.Ltr) 4f + (index + 0.5f) * tabWidth
                    else bounds.width - 4f - (index + 0.5f) * tabWidth,
                    bounds.height / 2f,
                )
            fun indicator(): IndicatorMeasurement {
                val pixels = bar.captureToImage().toPixelMap()
                val colored =
                    (0 until pixels.width).filter {
                        val pixel = pixels[it, pixels.height / 2]
                        pixel.red > 0.8f && pixel.green < 0.3f
                    }
                assertTrue(
                    colored.isNotEmpty(),
                    "Missing indicator: $direction/mouse=$mouse",
                )
                val rawCenter = (colored.first() + colored.last()) / 2f
                val barLeft = bar.fetchSemanticsNode().boundsInRoot.left
                val anchorCenter =
                    onNodeWithTag("tab-anchor-1", useUnmergedTree = true)
                        .fetchSemanticsNode()
                        .boundsInRoot
                        .center
                        .x - barLeft
                return IndicatorMeasurement(
                    rawCenter = rawCenter,
                    anchorCenter = anchorCenter,
                )
            }
            fun assertRelativeNoJump(
                before: IndicatorMeasurement,
                after: IndicatorMeasurement,
                event: String,
            ) {
                // One pixel permits endpoint quantization in the recorded indicator bounds.
                assertEquals(
                    before.relativeCenter,
                    after.relativeCenter,
                    absoluteTolerance = 1f,
                    message =
                        "$direction/mouse=$mouse $event: " +
                            "raw ${before.rawCenter} -> ${after.rawCenter}, " +
                            "anchor ${before.anchorCenter} -> ${after.anchorCenter}, " +
                            "relative ${before.relativeCenter} -> ${after.relativeCenter}",
                )
            }
            if (mouse)
                bar.performMouseInput {
                    moveTo(center(1))
                    press()
                }
            else bar.performTouchInput { down(center(1)) }
            mainClock.advanceTimeBy(16)
            val before = indicator()
            if (mouse) bar.performMouseInput { moveTo(center(2), delayMillis = 0) }
            else bar.performTouchInput { moveTo(center(2), delayMillis = 0) }
            mainClock.advanceTimeBy(16)
            val after = indicator()
            assertTrue(
                abs(after.relativeCenter - before.relativeCenter) < tabWidth * 0.4f,
                "$direction/mouse=$mouse frame16 relative " +
                    "${before.relativeCenter} -> ${after.relativeCenter}, " +
                    "raw ${before.rawCenter} -> ${after.rawCenter}, " +
                    "anchor ${before.anchorCenter} -> ${after.anchorCenter}, width=$tabWidth",
            )
            if (mouse) bar.performMouseInput { moveTo(center(1), delayMillis = 0) }
            else bar.performTouchInput { moveTo(center(1), delayMillis = 0) }
            val retargeted = indicator()
            assertRelativeNoJump(after, retargeted, "zero-time retarget to tab 1")

            if (mouse) bar.performMouseInput { moveTo(center(2), delayMillis = 0) }
            else bar.performTouchInput { moveTo(center(2), delayMillis = 0) }
            val retargetedAgain = indicator()
            assertRelativeNoJump(retargeted, retargetedAgain, "zero-time retarget to tab 2")

            mainClock.advanceTimeBy(1500)
            val settled = indicator()
            assertTrue(
                abs(settled.rawCenter - center(2).x) < 3f,
                "$direction/mouse=$mouse final raw=${settled.rawCenter}, " +
                    "anchor=${settled.anchorCenter}, target=${center(2).x}",
            )
            if (mouse) bar.performMouseInput { moveTo(center(1), delayMillis = 0) }
            else bar.performTouchInput { moveTo(center(1), delayMillis = 0) }
            val settledRetarget = indicator()
            assertRelativeNoJump(settled, settledRetarget, "zero-time settled retarget to tab 1")
            mainClock.advanceTimeBy(1500)
            val finalAtTab1 = indicator()
            assertTrue(
                abs(finalAtTab1.rawCenter - center(1).x) < 3f,
                "$direction/mouse=$mouse final raw=${finalAtTab1.rawCenter}, " +
                    "anchor=${finalAtTab1.anchorCenter}, target=${center(1).x}",
            )
            if (mouse) bar.performMouseInput { release() } else bar.performTouchInput { up() }
            mainClock.advanceTimeBy(1500)
            val released = indicator()
            assertTrue(
                abs(released.rawCenter - center(1).x) < 3f,
                "$direction/mouse=$mouse released raw=${released.rawCenter}, " +
                    "anchor=${released.anchorCenter}, target=${center(1).x}",
            )
        }

    @Test
    fun panelSwayDoesNotExposeARecordingEdgeNearTheBarBoundsLtrLeft(): TestResult =
        panelSwayCase(
            LayoutDirection.Ltr,
            selectedIndex = 0,
            touchFraction = 0.2f,
            dragDirection = -1,
        )

    @Test
    fun panelSwayDoesNotExposeARecordingEdgeNearTheBarBoundsLtrRight(): TestResult =
        panelSwayCase(
            LayoutDirection.Ltr,
            selectedIndex = 2,
            touchFraction = 0.8f,
            dragDirection = 1,
        )

    @Test
    fun panelSwayDoesNotExposeARecordingEdgeNearTheBarBoundsRtlLeft(): TestResult =
        panelSwayCase(
            LayoutDirection.Rtl,
            selectedIndex = 2,
            touchFraction = 0.2f,
            dragDirection = -1,
        )

    @Test
    fun panelSwayDoesNotExposeARecordingEdgeNearTheBarBoundsRtlRight(): TestResult =
        panelSwayCase(
            LayoutDirection.Rtl,
            selectedIndex = 0,
            touchFraction = 0.8f,
            dragDirection = 1,
        )

    private fun panelSwayCase(
        direction: LayoutDirection,
        selectedIndex: Int,
        touchFraction: Float,
        dragDirection: Int,
    ): TestResult = runInitializedComposeUiTest {
        setContent {
            ProvideM3xMaterialRendering {
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    M3xMaterialTheme(
                        colorScheme =
                            lightColorScheme(
                                primary = Color.Black,
                                onSurface = Color.White,
                            ),
                    ) {
                        val source = rememberBackdropLayer(Color(0.1f, 0.3f, 0.9f))
                        BackdropHost(source) {
                            Box(Modifier.size(400.dp, 200.dp).testTag("root")) {
                                Box(Modifier.matchParentSize().captureBackdrop(source))
                                FloatingBottomBar(
                                    selectedIndex,
                                    {},
                                    Modifier.align(Alignment.Center).testTag("bar"),
                                    indicatorEffects = backdropIndicatorEffects {},
                                ) {
                                    item { Text("First") }
                                    item { Text("Second") }
                                    item { Text("Third") }
                                }
                            }
                        }
                    }
                }
            }
        }
        mainClock.autoAdvance = false
        mainClock.advanceTimeBy(500)
        val bar = onNodeWithTag("bar")
        val bounds = bar.fetchSemanticsNode().boundsInRoot
        val size = bar.fetchSemanticsNode().size
        val touch = Offset(size.width * touchFraction, size.height / 2f)
        bar.performTouchInput { down(touch) }
        mainClock.advanceTimeBy(800)
        val firstDragOffset = dragDirection * 60f
        val finalDragOffset = dragDirection * 140f
        bar.performTouchInput {
            moveTo(Offset(touch.x + firstDragOffset, touch.y), delayMillis = 0)
            moveTo(Offset(touch.x + finalDragOffset, touch.y), delayMillis = 0)
        }
        mainClock.advanceTimeBy(16)

        val pixels = onNodeWithTag("root").captureToImage().toPixelMap()
        val density = pixels.width / 400f
        val edge = if (dragDirection < 0) bounds.left else bounds.right
        // Search the fixed capture-domain edge, away from rounded top and bottom corners.
        val roiLeft = floor(edge - 2f * density).toInt().coerceAtLeast(1)
        val roiRight = ceil(edge + 2f * density).toInt().coerceAtMost(pixels.width - 1)
        val roiTop = ceil(bounds.top + bounds.height * 0.25f).toInt().coerceAtLeast(0)
        val roiBottom =
            floor(bounds.bottom - bounds.height * 0.25f).toInt().coerceAtMost(pixels.height)
        val roiHeight = roiBottom - roiTop
        var longestRun = 0
        var longestX = roiLeft
        var longestY = roiTop
        for (x in roiLeft..roiRight) {
            var run = 0
            var runStart = roiTop
            for (y in roiTop until roiBottom) {
                val pixel = pixels[x, y]
                val previous = pixels[x - 1, y]
                val hardStep =
                    pixel.alpha >= 0.95f &&
                        previous.alpha >= 0.95f &&
                        abs(pixel.red - previous.red) +
                            abs(pixel.green - previous.green) +
                            abs(pixel.blue - previous.blue) > 0.25f
                if (hardStep) {
                    if (run == 0) runStart = y
                    run++
                    if (run > longestRun) {
                        longestRun = run
                        longestX = x
                        longestY = runStart
                    }
                } else {
                    run = 0
                }
            }
        }
        // The 2dp edge neighborhood allows pixel quantization at the capture boundary.
        assertTrue(
            longestRun < roiHeight / 2,
            "$direction selected=$selectedIndex touch=$touchFraction " +
                "drag=$firstDragOffset then $finalDragOffset: " +
                "x=$longestX y=$longestY " +
                "run=$longestRun ROI=($roiLeft,$roiTop)-($roiRight,$roiBottom)",
        )
    }

    @Test
    fun interiorLightingSurvivesSelectedAndUnselectedHitRegions() = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(
                    colorScheme = lightColorScheme(surface = Color.Black, onSurface = Color.Black),
                ) {
                    val source = rememberBackdropLayer(Color.Black)
                    Box(Modifier.size(240.dp, 96.dp).background(Color.Black)) {
                        Box(Modifier.fillMaxSize().captureBackdrop(source))
                        BackdropHost(source) {
                            ProvideBackdropMaterial(
                                MaterialSurfaceRole.Control,
                                M3xBackdropMaterial(
                                    style =
                                        BackdropStyle(
                                            effects = BackdropStyle.NoEffects,
                                            highlight = { Highlight.None },
                                            shadow = { Shadow.None },
                                        ),
                                    blurRadius = 0.dp,
                                    tintAlpha = 0f,
                                    adaptation = BackdropAdaptation.Fixed,
                                    adaptiveContentColor = false,
                                ),
                            ) {
                                FloatingBottomBar(
                                    0,
                                    {},
                                    Modifier.testTag("bar"),
                                    indicatorEffects = backdropIndicatorEffects {},
                                ) {
                                    item { Box(Modifier.size(64.dp, 24.dp)) }
                                    item { Box(Modifier.size(64.dp, 24.dp)) }
                                }
                            }
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(500)
        fun selectedBrightness(): Float {
            val pixels = onNodeWithTag("bar").captureToImage().toPixelMap()
            return pixels[pixels.width / 4, pixels.height / 2].red
        }
        val baseline = selectedBrightness()
        repeat(3) { attempt ->
            onNodeWithTag("bar").performTouchInput { down(Offset(width / 4f, height / 2f)) }
            mainClock.advanceTimeBy(250)
            val selected = selectedBrightness()
            assertTrue(
                selected > baseline + 0.05f,
                "Selected press $attempt: $baseline -> $selected",
            )
            onNodeWithTag("bar").performTouchInput { up() }
            mainClock.advanceTimeBy(500)
            val released = selectedBrightness()
            assertTrue(released < baseline + 0.01f, "Release removes the light: $released")
        }
        onNodeWithTag("bar").performTouchInput {
            down(Offset(width / 4f, height / 2f))
            moveTo(Offset(width * 0.75f, height / 2f))
        }
        mainClock.advanceTimeBy(250)
        val draggedPixels = onNodeWithTag("bar").captureToImage().toPixelMap()
        val dragged = draggedPixels[draggedPixels.width * 3 / 4, draggedPixels.height / 2].red
        assertTrue(dragged > baseline + 0.05f, "Dragged light: $dragged")
        onNodeWithTag("bar").performTouchInput { up() }
        mainClock.advanceTimeBy(500)
        onNodeWithTag("bar").performTouchInput { down(Offset(width * 0.75f, height / 2f)) }
        mainClock.advanceTimeBy(250)
        val pixels = onNodeWithTag("bar").captureToImage().toPixelMap()
        assertTrue(pixels[pixels.width * 3 / 4, pixels.height / 2].red > baseline + 0.05f)
        onNodeWithTag("bar").performTouchInput { up() }
    }

    @Test
    fun restingIndicatorUsesThemeForegroundInLightAndDarkThemes() = runInitializedComposeUiTest {
        val dark = mutableStateOf(false)
        setContent {
            ProvideM3xMaterialRendering {
                val background = if (dark.value) Color.Black else Color.White
                MaterialTheme(
                    colorScheme =
                        if (dark.value)
                            darkColorScheme(surface = Color.Black, onSurface = Color.White)
                        else lightColorScheme(surface = Color.White, onSurface = Color.Black),
                ) {
                    val source = rememberBackdropLayer()
                    Box(Modifier.size(240.dp, 80.dp)) {
                        Box(Modifier.fillMaxSize().captureBackdrop(source).background(background))
                        BackdropHost(source) {
                            ProvideBackdropMaterial(
                                MaterialSurfaceRole.Control,
                                M3xBackdropMaterial(
                                    style =
                                        BackdropStyle(
                                            effects = BackdropStyle.NoEffects,
                                            highlight = { Highlight.None },
                                            shadow = { Shadow.None },
                                        ),
                                    blurRadius = 0.dp,
                                    tintAlpha = 0.12f,
                                    adaptation = BackdropAdaptation.Fixed,
                                    adaptiveContentColor = false,
                                    interactiveLighting = false,
                                ),
                            ) {
                                FloatingBottomBar(0, {}, Modifier.testTag("bar")) {
                                    item { Box(Modifier.size(48.dp, 24.dp)) }
                                    item { Box(Modifier.size(48.dp, 24.dp)) }
                                }
                            }
                        }
                    }
                }
            }
        }
        waitForIdle()
        var pixels = onNodeWithTag("bar").captureToImage().toPixelMap()
        val light = pixels[pixels.width / 4, pixels.height / 2]
        assertTrue(light.red in 0.85f..0.95f, "Light theme resting overlay: $light")
        runOnIdle { dark.value = true }
        waitForIdle()
        pixels = onNodeWithTag("bar").captureToImage().toPixelMap()
        val darkColor = pixels[pixels.width / 4, pixels.height / 2]
        assertTrue(darkColor.red in 0.05f..0.15f, "Dark theme resting overlay: $darkColor")
    }

    @Test
    fun pressedIndicatorDrawsAboveTheBarBoundary() = runInitializedComposeUiTest {
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    val source = rememberBackdropLayer()
                    Box(Modifier.size(280.dp, 160.dp).testTag("root")) {
                        Box(Modifier.fillMaxSize().captureBackdrop(source).background(Color.Blue))
                        Box(Modifier.fillMaxSize().background(Color.Red))
                        BackdropHost(source) {
                            FloatingBottomBar(
                                0,
                                {},
                                Modifier.align(Alignment.Center).testTag("bar"),
                            ) {
                                item { Text("First") }
                                item { Text("Second") }
                            }
                        }
                    }
                }
            }
        }
        val bar = onNodeWithTag("bar").fetchSemanticsNode().boundsInRoot
        val x = (bar.left + bar.width / 4).toInt()
        val y = (bar.top - 5).toInt()
        val before = onNodeWithTag("root").captureToImage().toPixelMap()[x, y]
        onNodeWithTag("bar").performTouchInput { down(Offset(width / 4f, height / 2f)) }
        mainClock.advanceTimeBy(500)
        val after = onNodeWithTag("root").captureToImage().toPixelMap()[x, y]
        onNodeWithTag("bar").performTouchInput { up() }
        assertTrue(
            after.blue > before.blue + 0.1f,
            "Pressed selection extends above the bar: $before -> $after",
        )
    }

    @Test
    fun tabDrawingCrossesItsSlotButStaysInsideTheBar() = runInitializedComposeUiTest {
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    FloatingBottomBar(0, {}, Modifier.testTag("bar")) {
                        item {
                            Canvas(Modifier.size(50.dp, 30.dp)) {
                                drawRect(
                                    Color.Red,
                                    Offset(-size.width, -size.height),
                                    Size(size.width * 3f, size.height * 3f),
                                )
                            }
                        }
                        item { Box(Modifier.size(50.dp, 30.dp)) }
                    }
                }
            }
        }
        val pixels = onNodeWithTag("bar").captureToImage().toPixelMap()
        // The slot boundary sits at (width - 8) / 2 + 4; sample just past it so the first tab's
        // 3x overdraw must have crossed into the second slot.
        val slotBoundary = (pixels.width - 8) / 2 + 4
        val crossed = pixels[slotBoundary + 5, pixels.height / 2]
        assertTrue(crossed.red > 0.95f && crossed.green < 0.05f, "Cross-slot content: $crossed")
        val corner = pixels[0, 0]
        assertTrue(
            corner.alpha < 0.05f || corner.green > 0.05f,
            "Bar corner clips overflow: $corner",
        )
    }

    @Test
    fun multiwordLabelsFitAndRemainCenteredInWeightedTabs() = runInitializedComposeUiTest {
        lateinit var result: TextLayoutResult
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    FloatingBottomBar(0, {}, Modifier.testTag("bar")) {
                        item { Text("Components") }
                        item { Text("Overlays") }
                        item { Text("Backdrop lab", maxLines = 1, onTextLayout = { result = it }) }
                        item { Text("Settings") }
                    }
                }
            }
        }
        waitForIdle()
        assertTrue(!result.didOverflowWidth, "The complete label fits its slot")
        val label = onNodeWithText("Backdrop lab").fetchSemanticsNode().boundsInRoot
        val first = onNodeWithText("Components").fetchSemanticsNode().boundsInRoot.center.x
        val second = onNodeWithText("Overlays").fetchSemanticsNode().boundsInRoot.center.x
        assertTrue(abs(label.center.x - (first + 2f * (second - first))) < 2f)
    }

    @Test
    fun selectedContentKeepsSharpEdgesAboveMaterialBlur() = runInitializedComposeUiTest {
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(primary = Color.Black)) {
                    val source = rememberBackdropLayer(Color.White)
                    BackdropHost(source) {
                        Box(Modifier.size(160.dp, 80.dp)) {
                            Box(
                                Modifier.fillMaxSize()
                                    .captureBackdrop(source)
                                    .background(Color.White),
                            )
                            FloatingBottomBar(0, {}, Modifier.testTag("bar")) {
                                item {
                                    Canvas(Modifier.size(48.dp, 24.dp)) {
                                        repeat((size.width / 4).toInt()) { index ->
                                            drawRect(
                                                Color.Black,
                                                Offset(index * 4f, 0f),
                                                Size(2f, size.height),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        waitForIdle()
        val pixels = onNodeWithTag("bar").captureToImage().toPixelMap()
        val y = pixels.height / 2
        var sharpest = 0f
        for (x in pixels.width / 4 until pixels.width * 3 / 4) {
            sharpest = maxOf(sharpest, abs(pixels[x, y].red - pixels[x + 1, y].red))
        }
        assertTrue(sharpest > 0.6f, "Selected foreground edge contrast: $sharpest")
        onNodeWithTag("bar").performTouchInput { down(Offset(width / 2f, height / 2f)) }
        mainClock.advanceTimeBy(500)
        val pressedPixels = onNodeWithTag("bar").captureToImage().toPixelMap()
        var pressedSharpest = 0f
        for (x in pressedPixels.width / 4 until pressedPixels.width * 3 / 4) {
            pressedSharpest =
                maxOf(
                    pressedSharpest,
                    abs(pressedPixels[x, y].red - pressedPixels[x + 1, y].red),
                )
        }
        onNodeWithTag("bar").performTouchInput { up() }
        assertTrue(pressedSharpest > 0.6f, "Pressed foreground edge contrast: $pressedSharpest")
    }
}
