package top.ltfan.m3x.settingspage.material

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.animation.LocalBlurAnimationEnabled
import top.ltfan.m3x.compose.ui.visual.LocalBlurEnabled
import top.ltfan.m3x.material.LocalMaterialSurfaceRenderer
import top.ltfan.m3x.material.MaterialSurfaceAppearance
import top.ltfan.m3x.material.MaterialSurfaceEdge
import top.ltfan.m3x.material.MaterialSurfaceEffects
import top.ltfan.m3x.material.MaterialSurfaceRenderer
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.MaterialSurfaceStyle
import top.ltfan.m3x.material.PlainMaterialSurfaceRenderer
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.theme.toMaterialShapes
import top.ltfan.m3x.material.theming.capsule.capsuleShapes
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState
import top.ltfan.m3x.resource.Resource
import top.ltfan.m3x.settingspage.SettingsItem
import top.ltfan.m3x.settingspage.SettingsResetMode
import top.ltfan.m3x.settingspage.SettingsResources
import top.ltfan.m3x.settingspage.SettingsTree
import top.ltfan.m3x.settingspage.material.SettingsPageFixtures.ConditionalCategory
import top.ltfan.m3x.settingspage.material.SettingsPageFixtures.ConditionalTextItem
import top.ltfan.m3x.settingspage.material.SettingsPageFixtures.DefaultSwitchItem
import top.ltfan.m3x.settingspage.material.SettingsPageFixtures.SliderItem
import top.ltfan.m3x.settingspage.material.SettingsPageFixtures.SwitchItem
import top.ltfan.m3x.settingspage.material.SettingsPageFixtures.TextItem

class SettingsPageTest {
    @Test
    fun predictiveBackSeeksTheSettingsRowAndEditorTogether() = runComposeUiTest {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val owner =
            object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher = dispatcher
            }
        val state = SettingsPageState()
        val item = TextItem()
        val tree =
            object : SettingsTree {
                override val items = listOf(item)
            }
        val motion =
            object : MotionScheme {
                override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)

                override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)

                override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)

                override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)

                override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)

                override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)
            }
        setContent {
            ProvideM3xMaterialRendering {
                CompositionLocalProvider(
                    LocalNavigationEventDispatcherOwner provides owner,
                    LocalBlurAnimationEnabled provides false,
                ) {
                    MaterialTheme(
                        colorScheme = lightColorScheme(surfaceContainerHigh = Color.Red),
                        motionScheme = motion,
                    ) {
                        Box(
                            Modifier.size(480.dp, 640.dp).background(Color.White).testTag("scene"),
                        ) {
                            OverlayHost(remember { OverlayHostState() }) {
                                SettingsPage(tree, state = state)
                            }
                        }
                    }
                }
            }
        }
        mainClock.autoAdvance = false
        onNodeWithText("Name").performClick()
        mainClock.advanceTimeBy(160)
        val opening = onNodeWithTag("scene").captureToImage().toPixelMap()
        mainClock.advanceTimeBy(1600)
        val shown = onNodeWithTag("scene").captureToImage().toPixelMap()
        runOnIdle { input.backStarted(NavigationEvent()) }
        mainClock.advanceTimeBy(64)
        assertTrue(
            pixelDifference(shown, onNodeWithTag("scene").captureToImage().toPixelMap()) < 0.005f,
            "Starting a zero-progress Back gesture must preserve the displayed frame",
        )
        runOnIdle { input.backProgressed(NavigationEvent(progress = 0.4f)) }
        mainClock.advanceTimeBy(64)
        val halfway = onNodeWithTag("scene").captureToImage().toPixelMap()
        runOnIdle {
            assertEquals(item.id, state.transitionState.currentState)
            assertEquals(null, state.transitionState.targetState)
            assertEquals(0.4f, state.transitionState.fraction)
            assertSame(item, state.activeItem)
        }
        assertTrue(pixelDifference(shown, halfway) > 0.01f)
        runOnIdle { input.backProgressed(NavigationEvent(progress = 0.6f)) }
        mainClock.advanceTimeBy(64)
        runOnIdle { input.backCancelled() }
        mainClock.advanceTimeUntil(timeoutMillis = 2000) { state.transitionState.fraction <= 0.4f }
        runOnIdle {
            assertEquals(item.id, state.transitionState.currentState)
            assertEquals(null, state.transitionState.targetState)
        }
        val cancelledFraction = state.transitionState.fraction
        mainClock.advanceTimeBy(1200)
        assertTrue(
            pixelDifference(shown, onNodeWithTag("scene").captureToImage().toPixelMap()) < 0.005f,
        )
        runOnIdle { assertSame(item, state.activeItem) }
        runOnIdle { input.backStarted(NavigationEvent()) }
        mainClock.advanceTimeBy(64)
        runOnIdle { input.backProgressed(NavigationEvent(progress = cancelledFraction)) }
        mainClock.advanceTimeBy(64)
        runOnIdle {
            assertEquals(item.id, state.transitionState.currentState)
            assertEquals(null, state.transitionState.targetState)
            assertEquals(cancelledFraction, state.transitionState.fraction)
            assertSame(item, state.activeItem)
        }
        runOnIdle { input.backCancelled() }
        mainClock.advanceTimeBy(1200)
        runOnIdle { input.backStarted(NavigationEvent()) }
        mainClock.advanceTimeBy(64)
        runOnIdle { input.backProgressed(NavigationEvent(progress = 0.6f)) }
        mainClock.advanceTimeBy(64)
        runOnIdle { input.backCompleted() }
        mainClock.advanceTimeUntil(timeoutMillis = 2000) { state.activeItem == null }
        runOnIdle {
            assertEquals(null, state.activeItem)
            assertEquals("before", item.value)
        }
        // This comparison starts a fresh entry after exit settles. Immediate reversals have their
        // own tests and must continue from the interrupted values rather than restart this frame.
        mainClock.advanceTimeBy(1600)
        runOnIdle {
            assertEquals(null, state.transitionState.currentState)
            assertEquals(null, state.transitionState.targetState)
        }
        onNodeWithText("Name").performClick()
        mainClock.advanceTimeBy(160)
        assertTrue(
            pixelDifference(opening, onNodeWithTag("scene").captureToImage().toPixelMap()) < 0.005f,
            "Reopening after completed Back must follow the original entry animation",
        )
        mainClock.advanceTimeBy(1600)
        assertTrue(
            pixelDifference(shown, onNodeWithTag("scene").captureToImage().toPixelMap()) < 0.005f,
        )
        dispatcher.dispose()
    }

    @Test fun completedBackThenSameEditorRewindsItsExit() = backThenReopen(false)

    @Test fun completedBackThenDifferentEditorStartsItsOwnEntry() = backThenReopen(true)

    @Test fun completedBackThenSameEditorPreservesBlur() = backThenReopen(false, blurEnabled = true)

    @Test
    fun completedBackThenDifferentEditorPreservesBlur() = backThenReopen(true, blurEnabled = true)

    @Test
    fun expressiveBackThenSameEditorPreservesBlur() =
        backThenReopen(false, blurEnabled = true, expressive = true)

    @Test
    fun expressiveBackThenDifferentEditorPreservesBlur() =
        backThenReopen(true, blurEnabled = true, expressive = true)

    @Test
    fun backCompletionImmediatelyReopensSameEditor() =
        backThenReopen(false, blurEnabled = true, expressive = true, reopenDelayMillis = 16)

    @Test
    fun backCompletionImmediatelyOpensDifferentEditor() =
        backThenReopen(true, blurEnabled = true, expressive = true, reopenDelayMillis = 16)

    @Test fun repeatedImmediateReopenKeepsOneEditor() = repeatedBackReopen(reopenDelayMillis = 16)

    @Test fun repeatedSameFrameRequestKeepsOneEditor() = repeatedBackReopen(reopenDelayMillis = 0)

    private fun repeatedBackReopen(reopenDelayMillis: Long) = runComposeUiTest {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val owner =
            object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher = dispatcher
            }
        val state = SettingsPageState()
        val first = TextItem()
        val tree =
            object : SettingsTree {
                override val items = listOf(first)
            }
        val host = OverlayHostState()
        val motion =
            object : MotionScheme {
                override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)

                override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)

                override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)

                override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)

                override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)

                override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)
            }
        setContent {
            ProvideM3xMaterialRendering {
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                    MaterialTheme(
                        colorScheme = lightColorScheme(surfaceContainerHigh = Color.Red),
                        motionScheme = motion,
                    ) {
                        Box(
                            Modifier.size(480.dp, 640.dp).background(Color.White).testTag("scene"),
                        ) {
                            OverlayHost(host) {
                                SettingsPage(tree, state = state)
                            }
                        }
                    }
                }
            }
        }
        mainClock.autoAdvance = false
        onNodeWithText("Name").performClick()
        mainClock.advanceTimeBy(2000)
        val fullyShown = onNodeWithTag("scene").captureToImage().toPixelMap()
        fun redBounds(): List<Int> {
            val pixels = onNodeWithTag("scene").captureToImage().toPixelMap()
            val points = mutableListOf<Pair<Int, Int>>()
            for (y in 0 until pixels.height step 2) for (x in 0 until pixels.width step 2) {
                val c = pixels[x, y]
                if (c.red - (c.green + c.blue) / 2 > 0.02f) points += x to y
            }
            assertTrue(points.isNotEmpty(), "The editor lost its container")
            return listOf(
                points.minOf { it.first },
                points.minOf { it.second },
                points.maxOf { it.first },
                points.maxOf { it.second },
            )
        }
        repeat(5) { iteration ->
            runOnIdle { input.backStarted(NavigationEvent(progress = 0f)) }
            mainClock.advanceTimeBy(64)
            runOnIdle { input.backProgressed(NavigationEvent(progress = 0.4f)) }
            mainClock.advanceTimeBy(64)
            runOnIdle { input.backCompleted() }
            if (reopenDelayMillis > 0) mainClock.advanceTimeBy(reopenDelayMillis)
            if (reopenDelayMillis == 0L) runOnIdle { state.open(first) }
            else
                onNode(hasClickAction() and hasAnyAncestor(hasTestTag("settings-row:${first.id}")))
                    .performSemanticsAction(SemanticsActions.OnClick) { it() }
            runOnIdle { assertSame(first, state.activeItem) }
            var prior = redBounds()
            repeat(8) { frame ->
                mainClock.advanceTimeByFrame()
                val next = redBounds()
                assertTrue(
                    prior.indices.all { abs(next[it] - prior[it]) < 64 },
                    "Reopen $iteration jumped at frame $frame: $prior -> $next",
                )
                prior = next
            }
            mainClock.advanceTimeBy(2000)
            assertTrue(
                pixelDifference(fullyShown, onNodeWithTag("scene").captureToImage().toPixelMap()) <
                    0.005f,
                "Reopen $iteration must settle to the full editor frame",
            )
            runOnIdle { assertSame(first, state.activeItem) }
            assertEquals(
                1,
                onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size,
                "Reopen $iteration left a residual editor",
            )
        }
        dispatcher.dispose()
    }

    @Test
    fun m3xDefaultBackImmediatelyReopensSameEditor() =
        backThenReopen(
            false,
            blurEnabled = true,
            expressive = true,
            reopenDelayMillis = 16,
            scheme = MotionScheme.expressive(),
        )

    @Test
    fun m3xDefaultBackImmediatelyOpensDifferentEditor() =
        backThenReopen(
            true,
            blurEnabled = true,
            expressive = true,
            reopenDelayMillis = 16,
            scheme = MotionScheme.expressive(),
        )

    private fun backThenReopen(
        different: Boolean,
        blurEnabled: Boolean = false,
        expressive: Boolean = false,
        reopenDelayMillis: Long = 32,
        scheme: MotionScheme? = null,
    ) = runComposeUiTest {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val owner =
            object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher = dispatcher
            }
        val state = SettingsPageState()
        val first = TextItem()
        val second = TextItem("other", "Other")
        val tree =
            object : SettingsTree {
                override val items = listOf(first, second)
            }
        val motion =
            object : MotionScheme {
                override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)

                override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)

                override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)

                override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)

                override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)

                override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> =
                    tween(1000, easing = LinearEasing)
            }
        setContent {
            ProvideM3xMaterialRendering {
                CompositionLocalProvider(
                    LocalNavigationEventDispatcherOwner provides owner,
                    LocalBlurAnimationEnabled provides blurEnabled,
                ) {
                    MaterialTheme(
                        colorScheme = lightColorScheme(surfaceContainerHigh = Color.Red),
                        motionScheme =
                            scheme ?: if (expressive) MotionScheme.expressive() else motion,
                    ) {
                        Box(
                            Modifier.size(480.dp, 640.dp).background(Color.White).testTag("scene"),
                        ) {
                            OverlayHost(remember { OverlayHostState() }) {
                                SettingsPage(tree, state = state)
                            }
                        }
                    }
                }
            }
        }
        mainClock.autoAdvance = false
        val label = if (different) "Other" else "Name"
        onNodeWithText(label).performClick()
        mainClock.advanceTimeBy(2000)
        val fullyShown = onNodeWithTag("scene").captureToImage().toPixelMap()
        runOnIdle { state.close() }
        mainClock.advanceTimeBy(2000)
        onNodeWithText("Name").performClick()
        mainClock.advanceTimeBy(2000)
        runOnIdle { input.backStarted(NavigationEvent()) }
        mainClock.advanceTimeBy(64)
        runOnIdle { input.backProgressed(NavigationEvent(progress = 0.2f)) }
        mainClock.advanceTimeBy(64)
        runOnIdle { input.backProgressed(NavigationEvent(progress = 0.4f)) }
        mainClock.advanceTimeBy(64)
        runOnIdle { input.backCompleted() }
        if (reopenDelayMillis > 0) mainClock.advanceTimeBy(reopenDelayMillis)
        val retiringSession = state.editSession
        onNode(
                hasClickAction() and
                    hasAnyAncestor(
                        hasTestTag("settings-row:${if (different) second.id else first.id}"),
                    ),
            )
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        runOnIdle {
            assertTrue(state.editSession !== retiringSession, "Click must open a new edit session")
        }
        runOnIdle { assertSame(if (different) second else first, state.activeItem) }
        fun redBounds(): List<Int> {
            val pixels = onNodeWithTag("scene").captureToImage().toPixelMap()
            val points = mutableListOf<Pair<Int, Int>>()
            for (y in 0 until pixels.height step 2) for (x in 0 until pixels.width step 2) {
                val c = pixels[x, y]
                if (c.red - (c.green + c.blue) / 2 > 0.02f) points += x to y
            }
            assertTrue(points.isNotEmpty(), "Reopened editor lost its container")
            return listOf(
                points.minOf { it.first },
                points.minOf { it.second },
                points.maxOf { it.first },
                points.maxOf { it.second },
            )
        }
        var prior = if (expressive) emptyList() else redBounds()
        repeat(12) { frame ->
            mainClock.advanceTimeByFrame()
            if (expressive) {
                if (frame < 2)
                    assertTrue(
                        pixelDifference(
                            fullyShown,
                            onNodeWithTag("scene").captureToImage().toPixelMap(),
                        ) > 0.005f,
                        "Reopening must animate before reaching its full frame",
                    )
            } else {
                val next = redBounds()
                assertTrue(
                    prior.indices.all { abs(next[it] - prior[it]) < 64 },
                    "Reopen jumped at frame $frame: $prior -> $next, different=$different",
                )
                prior = next
            }
        }
        runOnIdle { input.backStarted(NavigationEvent()) }
        mainClock.advanceTimeBy(64)
        runOnIdle { input.backProgressed(NavigationEvent(progress = 0.3f)) }
        mainClock.advanceTimeBy(64)
        runOnIdle {
            assertEquals(0.3f, state.transitionState.fraction)
            assertEquals(null, state.transitionState.targetState)
            assertSame(if (different) second else first, state.activeItem)
            input.backCancelled()
        }
        mainClock.advanceTimeBy(2000)
        assertTrue(
            pixelDifference(fullyShown, onNodeWithTag("scene").captureToImage().toPixelMap()) <
                0.005f,
            "Reopened editor must settle to its original full frame, different=$different current=${state.transitionState.currentState} target=${state.transitionState.targetState} fraction=${state.transitionState.fraction} active=${state.activeItem?.id}",
        )
        dispatcher.dispose()
    }

    private fun pixelDifference(first: PixelMap, second: PixelMap): Float {
        var difference = 0f
        var count = 0
        for (y in 0 until first.height step 4) for (x in 0 until first.width step 4) {
            val a = first[x, y]
            val b = second[x, y]
            difference += abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue)
            count += 3
        }
        return difference / count
    }

    @Test
    fun sharedEditorKeepsItsPositionDuringRapidReversals() =
        rapidEditorReversals(MotionScheme.expressive())

    private fun rapidEditorReversals(motion: MotionScheme) = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = SettingsPageState()
        val item = TextItem()
        val tree =
            object : SettingsTree {
                override val items = listOf(item)
            }
        setContent {
            ProvideM3xMaterialRendering {
                CompositionLocalProvider(LocalBlurAnimationEnabled provides false) {
                    MaterialTheme(
                        colorScheme = lightColorScheme(surfaceContainerHigh = Color.Red),
                        motionScheme = motion,
                    ) {
                        Box(
                            Modifier.size(480.dp, 640.dp).background(Color.White).testTag("scene"),
                        ) {
                            OverlayHost(remember { OverlayHostState() }) {
                                SettingsPage(tree, state = state)
                            }
                        }
                    }
                }
            }
        }
        fun bounds(): List<Int>? {
            val pixels = onNodeWithTag("scene").captureToImage().toPixelMap()
            var maximum = 0f
            for (y in 0 until pixels.height step 2) for (x in 0 until pixels.width step 2) {
                val color = pixels[x, y]
                maximum = maxOf(maximum, color.red - (color.green + color.blue) / 2f)
            }
            if (maximum < 0.08f) return null
            val points = mutableListOf<Pair<Int, Int>>()
            for (y in 0 until pixels.height step 2) for (x in 0 until pixels.width step 2) {
                val color = pixels[x, y]
                if (color.red - (color.green + color.blue) / 2f > maxOf(0.08f, maximum * 0.5f))
                    points += x to y
            }
            return listOf(
                points.minOf { it.first },
                points.minOf { it.second },
                points.maxOf { it.first },
                points.maxOf { it.second },
            )
        }
        mainClock.advanceTimeBy(1000)
        onNodeWithText("Name").performClick()
        mainClock.advanceTimeBy(600)
        var previous = bounds()
        mainClock.advanceTimeByFrame()
        repeat(8) { reversal ->
            var before = bounds()
            runOnIdle {
                if (reversal % 2 == 0) state.close()
                else {
                    state.open(item)
                }
            }
            repeat(10) { frame ->
                mainClock.advanceTimeByFrame()
                val after = bounds()
                val prior = previous
                if (frame <= 1 && prior != null && before != null && after != null) {
                    val current = before
                    assertTrue(
                        prior.indices.all { abs(after[it] - current[it]) <= 64 },
                        "Bounds jumped on reversal $reversal frame $frame: $prior -> $before -> $after",
                    )
                }
                previous = before
                before = after
            }
        }
        mainClock.advanceTimeBy(4000)
        mainClock.autoAdvance = true
    }

    @Test
    fun sharedEditorContainerFadesOnEveryOpeningAndClosing() = runComposeUiTest {
        mainClock.autoAdvance = false
        val blurEnabled = mutableStateOf(true)
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(TextItem())
            }
        setContent {
            ProvideM3xMaterialRendering {
                CompositionLocalProvider(LocalBlurAnimationEnabled provides blurEnabled.value) {
                    MaterialTheme(
                        colorScheme = lightColorScheme(surfaceContainerHigh = Color.Red),
                    ) {
                        Box(
                            Modifier.size(480.dp, 640.dp)
                                .background(Color.White)
                                .testTag("fade-scene"),
                        ) {
                            OverlayHost(remember { OverlayHostState() }) {
                                SettingsPage(
                                    tree,
                                    labels =
                                        SettingsPageLabels(
                                            cancel = Resource.Global.Static("Cancel"),
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
        fun opacity(): Float {
            val pixels = onNodeWithTag("fade-scene").captureToImage().toPixelMap()
            var maximum = 0f
            for (y in 0 until pixels.height step 4) for (x in 0 until pixels.width step 4) {
                val color = pixels[x, y]
                maximum = maxOf(maximum, color.red - (color.green + color.blue) / 2f)
            }
            return maximum
        }
        mainClock.advanceTimeBy(1000)
        for (enabled in listOf(true, false)) {
            runOnIdle { blurEnabled.value = enabled }
            onNodeWithText("Name").performClick()
            val entering = mutableListOf<Float>()
            repeat(40) {
                mainClock.advanceTimeBy(16)
                entering += opacity()
            }
            mainClock.advanceTimeBy(1000)
            val full = opacity()
            assertTrue(full > 0.3f, "The editor container must be visible: $full")
            assertTrue(
                entering.any { it / full in 0.1f..0.85f },
                "The editor must fade in: $entering",
            )
            val settledPixels = onNodeWithTag("fade-scene").captureToImage().toPixelMap()
            val fullDim = 1f - settledPixels[4, 4].red
            assertTrue(
                fullDim > 0.25f,
                "The scrim must remain dimmed after entry settles: $fullDim",
            )
            fun redBounds(pixels: PixelMap): List<Int> {
                val points = mutableListOf<Pair<Int, Int>>()
                for (y in 0 until pixels.height step 4) for (x in 0 until pixels.width step 4) {
                    val color = pixels[x, y]
                    if (color.red - (color.green + color.blue) / 2f > 0.15f) points += x to y
                }
                return listOf(
                    points.minOf { it.first },
                    points.minOf { it.second },
                    points.maxOf { it.first },
                    points.maxOf { it.second },
                )
            }
            val settledBounds = redBounds(settledPixels)
            onNodeWithText("Cancel").performClick()
            val exiting = mutableListOf<Float>()
            val scrim = mutableListOf<Float>()
            repeat(40) { frame ->
                mainClock.advanceTimeBy(16)
                exiting += opacity() / full
                val pixels = onNodeWithTag("fade-scene").captureToImage().toPixelMap()
                scrim += (1f - pixels[4, 4].red) / fullDim
                if (frame == 0) {
                    val bounds = redBounds(pixels)
                    assertTrue(
                        bounds.zip(settledBounds).all { (actual, initial) ->
                            abs(actual - initial) <= 4
                        },
                        "Closing must start at the settled dialog bounds: $settledBounds -> $bounds",
                    )
                    assertTrue(
                        scrim.last() > 0.9f,
                        "The scrim must retain its initial exit frame: $scrim",
                    )
                }
            }
            assertTrue(scrim.any { it in 0.1f..0.85f }, "The scrim must fade out: $scrim")
            assertTrue(scrim.last() < 0.03f, "The scrim must finish fading: $scrim")
            assertTrue(exiting.any { it in 0.1f..0.85f }, "The editor must fade out: $exiting")
            assertTrue(exiting.last() < 0.03f, "The editor container must disappear: $exiting")
            mainClock.advanceTimeBy(1000)
        }
    }

    @Test
    fun editorAndRowKeepTheirOwnCornersWhenTheSharedTransitionSettles() = verifySettlement(false)

    @Test
    fun capsuleEditorAndRowKeepTheirOwnCornersWhenTheSharedTransitionSettles() =
        verifySettlement(true)

    private fun verifySettlement(useCapsule: Boolean) = runComposeUiTest {
        val item = TextItem()
        val state = SettingsPageState()
        val tree =
            object : SettingsTree {
                override val items = listOf(item)
            }
        val panelShapes = mutableListOf<Shape>()
        val renderer =
            object : MaterialSurfaceRenderer by PlainMaterialSurfaceRenderer {
                @Composable
                override fun appearance(
                    role: MaterialSurfaceRole,
                    style: MaterialSurfaceStyle?,
                    containerColor: Color,
                    contentColor: Color,
                    effects: MaterialSurfaceEffects?,
                    shape: Shape,
                    excludedEdges: Set<MaterialSurfaceEdge>,
                ): MaterialSurfaceAppearance {
                    if (role == MaterialSurfaceRole.Panel && containerColor == Color.Red) {
                        SideEffect { panelShapes += shape }
                    }
                    return PlainMaterialSurfaceRenderer.appearance(
                        role,
                        style,
                        containerColor,
                        contentColor,
                        effects,
                        shape,
                        excludedEdges,
                    )
                }
            }
        setContent {
            val shapeRoles =
                if (useCapsule) capsuleShapes().toMaterialShapes() else MaterialTheme.shapes
            MaterialTheme(
                shapes = shapeRoles,
                colorScheme =
                    lightColorScheme(surface = Color.Green, surfaceContainerHigh = Color.Red),
            ) {
                Box(Modifier.size(480.dp, 640.dp).background(Color.Blue).testTag("settlement")) {
                    CompositionLocalProvider(LocalMaterialSurfaceRenderer provides renderer) {
                        OverlayHost(remember { OverlayHostState() }) {
                            val editorShape =
                                if (useCapsule) shapeRoles.extraLarge else RoundedCornerShape(40.dp)
                            SettingsPage(tree, state = state, editorShape = editorShape)
                        }
                    }
                }
            }
        }

        fun pixels() = onNodeWithTag("settlement").captureToImage().toPixelMap()
        fun isPanel(color: Color) = color.red > 0.8f && color.green < 0.2f && color.blue < 0.2f

        val row = onNodeWithTag("settings-row:${item.id}").fetchSemanticsNode().boundsInRoot
        val initial = pixels()
        val x = row.right.toInt() - 8
        val y = row.center.y.toInt()
        val initialRowColor = initial[x, y]
        val container =
            onAllNodesWithTag("settings-segment-container:${item.id}", useUnmergedTree = true)
        val restingHeight = container.fetchSemanticsNodes().single().size.height
        mainClock.autoAdvance = false
        onNodeWithText("Name").performSemanticsAction(SemanticsActions.OnClick)
        val containerHeights = mutableListOf<Int>()
        repeat(16) {
            mainClock.advanceTimeByFrame()
            containerHeights +=
                container.fetchSemanticsNodes(atLeastOneRootRequired = false).map { it.size.height }
        }
        assertTrue(
            containerHeights.any { it > restingHeight + 2 },
            "The surface must remeasure while it expands: $containerHeights",
        )
        mainClock.advanceTimeBy(1500)
        runOnIdle {
            assertEquals(item.id, state.transitionState.currentState)
            assertEquals(item.id, state.transitionState.targetState)
        }
        val open = pixels()
        var panelLeft = open.width
        var panelTop = open.height
        for (py in 0 until open.height) for (px in 0 until open.width) {
            if (isPanel(open[px, py])) {
                panelLeft = minOf(panelLeft, px)
                panelTop = minOf(panelTop, py)
            }
        }
        assertTrue(panelLeft < open.width && panelTop < open.height)
        val inset = (open.width * (6f / 480f)).toInt()
        val corner = open[panelLeft + inset, panelTop + inset]
        assertFalse(isPanel(corner), "Editor corner at ($panelLeft, $panelTop) + $inset: $corner")

        runOnIdle { state.close() }
        var rowFullyVisible = false
        repeat(75) { frame ->
            mainClock.advanceTimeByFrame()
            val color = pixels()[x, y]
            if (color == initialRowColor) rowFullyVisible = true
            else
                assertFalse(
                    rowFullyVisible,
                    "At closing frame $frame the fully returned row flashed: $color",
                )
        }
        assertTrue(rowFullyVisible)
        runOnIdle {
            val first = panelShapes.first()
            assertTrue(
                panelShapes.all { it === first },
                "The editor switched its shape while mounted",
            )
        }
        mainClock.autoAdvance = true
    }

    @Test
    fun editorContentKeepsItsLookaheadConstraintsWhileThePanelMoves() = runComposeUiTest {
        val item =
            object : SettingsItem.Item.Custom<String> {
                override val rendererId = "measure-editor"
                override val id = "name"
                override var value = "before"
                override val children: List<SettingsItem>? = null
                override val shouldShow = true
                override val enabled = true
                override val resources: SettingsResources =
                    object : SettingsResources {
                        override val label = Resource.Global.Static("Name")
                    }
            }
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(item)
            }
        val measured = mutableListOf<Constraints>()
        setContent {
            MaterialTheme {
                Box(Modifier.size(520.dp, 720.dp)) {
                    OverlayHost(remember { OverlayHostState() }) {
                        SettingsPage(
                            tree,
                            customEditor = { _, _, _ ->
                                Layout(content = {}) { _, constraints ->
                                    if (!isLookingAhead) measured += constraints
                                    layout(constraints.maxWidth, 64.dp.roundToPx()) {}
                                }
                            },
                        )
                    }
                }
            }
        }
        mainClock.autoAdvance = false
        onNodeWithText("Name").performSemanticsAction(SemanticsActions.OnClick)
        repeat(6) { mainClock.advanceTimeByFrame() }
        val openingMeasurements = runOnIdle {
            assertTrue(measured.isNotEmpty())
            measured.size
        }
        repeat(12) { mainClock.advanceTimeByFrame() }
        runOnIdle {
            assertEquals(1, measured.distinct().size, "Editor body constraints changed: $measured")
            assertEquals(
                openingMeasurements,
                measured.size,
                "Editor body remeasured during the move",
            )
        }
        mainClock.autoAdvance = true
    }

    @Test
    fun capsuleRowBackgroundFollowsItsNativePressedShape() = runComposeUiTest {
        val item = TextItem("middle", "Middle")
        val category =
            object : SettingsItem.Category by ConditionalCategory(item) {
                override val children: List<SettingsItem> =
                    listOf(TextItem("first", "First"), item, TextItem("last", "Last"))
            }
        val tree =
            object : SettingsTree {
                override val items = listOf(category)
            }
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(surface = Color.Green),
                shapes = capsuleShapes().toMaterialShapes(),
            ) {
                Box(Modifier.size(480.dp, 640.dp).background(Color.Blue).testTag("press-scene")) {
                    OverlayHost(remember { OverlayHostState() }) { SettingsPage(tree) }
                }
            }
        }
        val row = onNodeWithTag("settings-row:middle").fetchSemanticsNode().boundsInRoot
        fun corner(): Color {
            val pixels = onNodeWithTag("press-scene").captureToImage().toPixelMap()
            val inset = (pixels.width * (4f / 480f)).toInt()
            return pixels[row.left.toInt() + inset, row.top.toInt() + inset]
        }
        assertTrue(corner().green > 0.8f)
        mainClock.autoAdvance = false
        onNodeWithText("Middle").performTouchInput { down(center) }
        mainClock.advanceTimeBy(400)
        val pressed = corner()
        assertTrue(pressed.blue > 0.8f && pressed.green < 0.2f, "Pressed corner: $pressed")
        onNodeWithText("Middle").performTouchInput { cancel() }
        mainClock.autoAdvance = true
    }

    @Test
    fun inlineOnlySliderShowsRestoreLongPressIndication() = runComposeUiTest {
        val item = SliderItem(resetMode = SettingsResetMode.Menu, defaultValue = 0)
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(item)
            }
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(surface = Color.Green)) {
                Box(Modifier.size(400.dp, 640.dp).testTag("ripple-scene")) {
                    OverlayHost(remember { OverlayHostState() }) {
                        SettingsPage(
                            tree,
                            labels =
                                SettingsPageLabels(
                                    restoreDefault = Resource.Global.Static("Restore default"),
                                ),
                        )
                    }
                }
            }
        }
        waitForIdle()
        onNodeWithText("Level").assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
        val before = onNodeWithTag("ripple-scene").captureToImage().toPixelMap()
        mainClock.autoAdvance = false
        onNodeWithTag("settings-row:level").performTouchInput {
            down(Offset(32f, 16f))
        }
        mainClock.advanceTimeBy(200)
        val pressed = onNodeWithTag("ripple-scene").captureToImage().toPixelMap()
        assertTrue(pixelMapsDiffer(before, pressed), "Restore-enabled slider row has no ripple")
        mainClock.advanceTimeBy(1000)
        onNodeWithText("Restore default").assertIsDisplayed()
        onNodeWithTag("settings-row:level").performTouchInput { up() }
        mainClock.autoAdvance = true
    }

    @Test
    fun sharedEditorKeepsTheRowsRippleInsideItsSegmentCorners() = runComposeUiTest {
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(TextItem())
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(onSurface = Color.Red)) {
                    Box(Modifier.size(320.dp, 480.dp).background(Color.Blue)) {
                        OverlayHost(remember { OverlayHostState() }) {
                            SettingsPage(
                                tree,
                            )
                        }
                    }
                }
            }
        }
        val row = onNodeWithText("Name").fetchSemanticsNode().parent!!.boundsInRoot
        mainClock.autoAdvance = false
        onNodeWithText("Name").performTouchInput { down(center) }
        mainClock.advanceTimeBy(500)
        onNodeWithText("Name").performTouchInput { up() }
        mainClock.advanceTimeBy(32)
        val pixels = onRoot().captureToImage().toPixelMap()
        val corner = pixels[row.left.toInt() + 2, row.top.toInt() + 2]
        assertTrue(corner.red < 0.025f, "Shared source ripple escapes rounded segment: $corner")
        mainClock.autoAdvance = true
    }

    @Test
    fun showHideRowRemainsMeasuredAndShrinksDuringExit() = runComposeUiTest {
        val item = ConditionalTextItem()
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(item)
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(remember { OverlayHostState() }) { SettingsPage(tree) }
                }
            }
        }
        val row = onNodeWithTag("settings-row:conditional")
        val initialHeight = row.fetchSemanticsNode().boundsInRoot.height
        mainClock.autoAdvance = false
        runOnIdle { item.shown = false }
        mainClock.advanceTimeBy(100)
        row.assertIsDisplayed()
        val exitHeight = row.fetchSemanticsNode().boundsInRoot.height
        assertTrue(
            exitHeight in 0f..initialHeight,
            "Exit row height: $initialHeight -> $exitHeight",
        )
        mainClock.advanceTimeBy(1000)
        waitForIdle()
        row.assertDoesNotExist()
        mainClock.autoAdvance = true
    }

    @Test
    fun checkedSwitchKeepsItsSegmentColorAndCornerShape() = runComposeUiTest {
        val item = SwitchItem()
        val category =
            object : SettingsItem.Category by ConditionalCategory(item) {
                override val children: List<SettingsItem> = listOf(item, TextItem())
            }
        val tree =
            object : SettingsTree {
                override val items = listOf(category)
            }
        setContent {
            MaterialTheme(
                colorScheme =
                    lightColorScheme(
                        surface = Color.Green,
                        secondaryContainer = Color.Magenta,
                    ),
            ) {
                Box(Modifier.size(320.dp, 480.dp).background(Color.Blue)) {
                    OverlayHost(remember { OverlayHostState() }) { SettingsPage(tree) }
                }
            }
        }

        fun segmentPixels(): List<Color> {
            val row = onNodeWithText("Switch").fetchSemanticsNode().parent!!.boundsInRoot
            val pixels = onRoot().captureToImage().toPixelMap()
            val inset = pixels.width * (4f / 320f)
            return listOf(
                pixels[(row.left + row.width * 0.55f).toInt(), row.center.y.toInt()],
                pixels[(row.left + inset).toInt(), (row.bottom - inset).toInt()],
            )
        }

        val off = segmentPixels()
        assertTrue(off.all { it.green > 0.8f && it.red < 0.1f && it.blue < 0.1f })
        onNodeWithText("Switch").performSemanticsAction(SemanticsActions.OnClick)
        runOnIdle { assertTrue(item.value) }
        assertEquals(off, segmentPixels())
    }

    @Test
    fun remainingSegmentGainsOuterCornersWhenTheLastRowLeaves() = runComposeUiTest {
        val conditional = ConditionalTextItem()
        val category =
            object : SettingsItem.Category by ConditionalCategory(conditional) {
                override val children: List<SettingsItem> = listOf(TextItem(), conditional)
            }
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(category)
            }
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(surface = Color.Red)) {
                Box(Modifier.size(320.dp, 480.dp).background(Color.Blue)) {
                    OverlayHost(remember { OverlayHostState() }) { SettingsPage(tree) }
                }
            }
        }

        fun sampleBottomCorner(): Color {
            val row = onNodeWithText("Name").fetchSemanticsNode().parent!!.boundsInRoot
            val pixels = onRoot().captureToImage().toPixelMap()
            val inset = pixels.width * (4f / 320f)
            return pixels[(row.left + inset).toInt(), (row.bottom - inset).toInt()]
        }

        assertEquals(Color.Red, sampleBottomCorner())
        mainClock.autoAdvance = false
        runOnIdle { conditional.shown = false }
        mainClock.advanceTimeBy(100)
        onNodeWithTag("settings-row:conditional").assertIsDisplayed()
        mainClock.advanceTimeBy(1000)
        onNodeWithTag("settings-row:conditional").assertDoesNotExist()
        assertTrue(sampleBottomCorner().blue > 0.8f)
        mainClock.autoAdvance = true
    }

    @Test
    fun enteringRowAndRapidVisibilityReversalKeepOneStableRow() = runComposeUiTest {
        val item = ConditionalTextItem()
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(item)
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(remember { OverlayHostState() }) { SettingsPage(tree) }
                }
            }
        }
        val row = onNodeWithTag("settings-row:conditional")
        mainClock.autoAdvance = false
        runOnIdle { item.shown = false }
        mainClock.advanceTimeBy(100)
        runOnIdle { item.shown = true }
        mainClock.advanceTimeBy(100)
        row.assertIsDisplayed()
        mainClock.advanceTimeBy(1000)
        waitForIdle()
        row.assertIsDisplayed()
        mainClock.autoAdvance = true
    }

    @Test
    fun childEntersWithinVisibleCategoryAndReversesWithoutReordering() = runComposeUiTest {
        val stable = ConditionalTextItem(id = "stable")
        val child = ConditionalTextItem(id = "child", initiallyShown = false)
        val category =
            object : SettingsItem.Category {
                override val id = "entry-category"
                override val children: List<SettingsItem> = listOf(stable, child)
                override val shouldShow = true
                override val enabled = true
                override val resources =
                    object : SettingsResources {
                        override val label: Resource<String> =
                            Resource.Global.Static("Entry category")
                    }
            }
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(category)
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(remember { OverlayHostState() }) { SettingsPage(tree) }
                }
            }
        }
        val childRow = onNodeWithTag("settings-row:child")
        childRow.assertDoesNotExist()
        mainClock.autoAdvance = false
        runOnIdle { child.shown = true }
        mainClock.advanceTimeBy(100)
        childRow.assertIsDisplayed()
        val enteringHeight = childRow.fetchSemanticsNode().boundsInRoot.height
        mainClock.advanceTimeBy(1000)
        val settledHeight = childRow.fetchSemanticsNode().boundsInRoot.height
        assertTrue(
            enteringHeight < settledHeight,
            "Entry should grow from $enteringHeight to $settledHeight",
        )
        runOnIdle { child.shown = false }
        mainClock.advanceTimeBy(100)
        runOnIdle { child.shown = true }
        mainClock.advanceTimeBy(1000)
        childRow.assertIsDisplayed()
        assertEquals(settledHeight, childRow.fetchSemanticsNode().boundsInRoot.height)
        mainClock.autoAdvance = true
    }

    @Test
    fun newlyVisibleGroupRowsEnterBesideAnOnscreenNeighbor() = runComposeUiTest {
        val stable = ConditionalTextItem(id = "group-anchor")
        val newRow = ConditionalTextItem(id = "new-group-row")
        val newGroup = ConditionalCategory(newRow, initiallyShown = false)
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(stable, newGroup)
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(remember { OverlayHostState() }) { SettingsPage(tree) }
                }
            }
        }
        val row = onNodeWithTag("settings-row:new-group-row")
        row.assertDoesNotExist()
        mainClock.autoAdvance = false
        runOnIdle { newGroup.shown = true }
        mainClock.advanceTimeBy(100)
        row.assertIsDisplayed()
        val enteringHeight = row.fetchSemanticsNode().boundsInRoot.height
        mainClock.advanceTimeBy(1000)
        val settledHeight = row.fetchSemanticsNode().boundsInRoot.height
        assertTrue(
            enteringHeight < settledHeight,
            "New group row should enter from $enteringHeight to $settledHeight",
        )
        mainClock.autoAdvance = true
    }

    @Test
    fun hidingWholeCategoryShrinksItsRowsBeforeRemovingThem() = runComposeUiTest {
        val item = ConditionalTextItem()
        val category = ConditionalCategory(item)
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(category)
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(remember { OverlayHostState() }) { SettingsPage(tree) }
                }
            }
        }
        val row = onNodeWithTag("settings-row:conditional")
        val initialHeight = row.fetchSemanticsNode().boundsInRoot.height
        mainClock.autoAdvance = false
        runOnIdle { category.shown = false }
        mainClock.advanceTimeBy(100)
        row.assertIsDisplayed()
        val exitingHeight = row.fetchSemanticsNode().boundsInRoot.height
        assertTrue(
            exitingHeight < initialHeight,
            "Category row should shrink from $initialHeight to $exitingHeight",
        )
        mainClock.advanceTimeBy(1000)
        row.assertDoesNotExist()
        mainClock.autoAdvance = true
    }

    @Test
    fun secondaryClickRestoresDefault() = runComposeUiTest {
        val item = DefaultSwitchItem()
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(remember { OverlayHostState() }) {
                        Box(Modifier.size(320.dp, 240.dp)) {
                            SettingsRow(
                                item,
                                SettingsPageLabels(
                                    restoreDefault = Resource.Global.Static("Restore default"),
                                ),
                                onOpen = {},
                                modifier = Modifier.testTag("pointer-row"),
                            )
                        }
                    }
                }
            }
        }
        onNodeWithTag("pointer-row").performMouseInput { rightClick(Offset(120f, 20f)) }
        val menu = onNodeWithText("Restore default").assertIsDisplayed()
        val rowBounds = onNodeWithTag("pointer-row").fetchSemanticsNode().boundsInRoot
        val menuBounds = menu.fetchSemanticsNode().boundsInRoot
        assertTrue(menuBounds.left in (rowBounds.left + 110f)..(rowBounds.left + 130f))
        menu.performClick()
        assertEquals(false, item.value)
    }

    @Test
    fun replacingTheRowItemReopensItsMenuAtTheCurrentPointer() = runComposeUiTest {
        val first = DefaultSwitchItem(id = "first")
        val second = DefaultSwitchItem(id = "second")
        var item by mutableStateOf(first)
        val host = OverlayHostState()
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(host) {
                        Box(Modifier.size(320.dp, 240.dp)) {
                            SettingsRow(
                                item,
                                SettingsPageLabels(
                                    restoreDefault = Resource.Global.Static("Restore default"),
                                ),
                                onOpen = {},
                                modifier = Modifier.testTag("pointer-row"),
                            )
                        }
                    }
                }
            }
        }
        onNodeWithTag("pointer-row").performMouseInput { rightClick(Offset(120f, 20f)) }
        onNodeWithText("Restore default").performClick()
        waitForIdle()
        runOnIdle { item = second }
        onNodeWithTag("pointer-row").performMouseInput { rightClick(Offset(220f, 20f)) }
        val menu = onNodeWithText("Restore default").assertIsDisplayed()
        val rowBounds = onNodeWithTag("pointer-row").fetchSemanticsNode().boundsInRoot
        val menuBounds = menu.fetchSemanticsNode().boundsInRoot
        assertTrue(menuBounds.left in (rowBounds.left + 210f)..(rowBounds.left + 230f))
        menu.performClick()
        runOnIdle {
            assertEquals(1, first.writes)
            assertEquals(false, second.value)
            assertEquals(1, second.writes)
        }
    }

    @Test
    fun hidingAnAncestorImmediatelyDisablesTheActiveEditor() = runComposeUiTest {
        val item = ConditionalTextItem()
        val category = ConditionalCategory(item)
        val state = SettingsPageState()
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(category)
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(remember { OverlayHostState() }) {
                        SettingsPage(
                            tree,
                            state = state,
                            labels = SettingsPageLabels(save = Resource.Global.Static("Save")),
                        )
                    }
                }
            }
        }
        onNodeWithText("Conditional row").performClick()
        onNodeWithText("Save").assertIsEnabled()
        mainClock.autoAdvance = false
        val retiringSession = state.editSession
        runOnIdle { category.shown = false }
        mainClock.advanceTimeByFrame()
        onNodeWithText("Save").assertIsNotEnabled()
        onNode(hasSetTextAction()).assertDoesNotExist()
        assertEquals("value", retiringSession.draft)
        mainClock.advanceTimeBy(1000)
        waitForIdle()
        onNodeWithText("Save").assertDoesNotExist()
        assertEquals("value", retiringSession.draft)
        mainClock.autoAdvance = true
    }

    @Test
    fun hidingAnAncestorDisablesAnOpenRestoreMenuImmediately() = runComposeUiTest {
        val item = DefaultSwitchItem()
        val category = ConditionalCategory(item)
        var parentShown by mutableStateOf(true)
        val parent =
            object : SettingsItem.Category {
                override val id = "outer-category"
                override val children: List<SettingsItem> = listOf(category)
                override val shouldShow: Boolean
                    get() = parentShown

                override val enabled = true
                override val resources =
                    object : SettingsResources {
                        override val label: Resource<String> =
                            Resource.Global.Static("Outer category")
                    }
            }
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(parent)
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(remember { OverlayHostState() }) {
                        SettingsPage(
                            tree,
                            labels =
                                SettingsPageLabels(
                                    restoreDefault = Resource.Global.Static("Restore default"),
                                ),
                        )
                    }
                }
            }
        }
        onNodeWithText("Default switch").performTouchInput { longClick() }
        onNodeWithText("Restore default").assertIsDisplayed()
        mainClock.autoAdvance = false
        runOnIdle { parentShown = false }
        mainClock.advanceTimeByFrame()
        onNodeWithText("Restore default").assertDoesNotExist()
        onNodeWithText("Restore default", useUnmergedTree = true)
            .assert(hasAnyAncestor(isNotEnabled()))
        assertEquals(true, item.value)
        assertEquals(0, item.writes)
        mainClock.advanceTimeBy(1000)
        mainClock.autoAdvance = true
    }

    @Test
    fun blurExitUsesTheSameGeometryAndHonorsLocalBlurSwitch() = runComposeUiTest {
        val blurItem = ConditionalTextItem(id = "blur-row")
        val sharpItem = ConditionalTextItem(id = "sharp-row")
        val blurTree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(blurItem)
            }
        val sharpTree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(sharpItem)
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(remember { OverlayHostState() }) {
                        Row {
                            CompositionLocalProvider(LocalBlurEnabled provides true) {
                                Box(Modifier.size(320.dp, 420.dp).testTag("blur-on")) {
                                    SettingsPage(
                                        blurTree,
                                        contentPadding = PaddingValues(0.dp),
                                        groupSpacing = 0.dp,
                                    )
                                }
                            }
                            CompositionLocalProvider(LocalBlurEnabled provides false) {
                                Box(Modifier.size(320.dp, 420.dp).testTag("blur-off")) {
                                    SettingsPage(
                                        sharpTree,
                                        contentPadding = PaddingValues(0.dp),
                                        groupSpacing = 0.dp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        mainClock.autoAdvance = false
        val beforeOn = onNodeWithTag("blur-on").captureToImage().toPixelMap()
        val beforeOff = onNodeWithTag("blur-off").captureToImage().toPixelMap()
        assertFalse(pixelMapsDiffer(beforeOn, beforeOff), "Settled rows should look identical")
        runOnIdle {
            blurItem.shown = false
            sharpItem.shown = false
        }
        mainClock.advanceTimeBy(100)
        val onHeight =
            onNodeWithTag("settings-row:blur-row").fetchSemanticsNode().boundsInRoot.height
        val afterOn = onNodeWithTag("blur-on").captureToImage().toPixelMap()
        val afterOff = onNodeWithTag("blur-off").captureToImage().toPixelMap()
        assertTrue(
            pixelMapsDiffer(afterOn, afterOff),
            "The active blur setting should affect exit pixels",
        )
        assertEquals(
            onHeight,
            onNodeWithTag("settings-row:sharp-row").fetchSemanticsNode().boundsInRoot.height,
        )
        mainClock.advanceTimeBy(1000)
        mainClock.autoAdvance = true
    }

    private fun pixelMapsDiffer(
        first: PixelMap,
        second: PixelMap,
    ): Boolean {
        if (first.width != second.width || first.height != second.height) return true
        for (y in 0 until first.height) {
            for (x in 0 until first.width) if (first[x, y] != second[x, y]) return true
        }
        return false
    }

    @Test
    fun segmentsUseMaterialColorAndEditorSamplesBackdrop() = runComposeUiTest {
        val segmentColor = Color(0xFFBCE7CB)
        val editorState = SettingsPageState()
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(TextItem())
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(surface = segmentColor)) {
                    val source = rememberBackdropLayer()
                    Box(Modifier.size(240.dp, 240.dp)) {
                        Box(
                            Modifier.fillMaxSize().captureBackdrop(source).background(Color.Blue),
                        )
                        Box(Modifier.fillMaxSize().background(Color.Red))
                        BackdropHost(source) {
                            OverlayHost(remember { OverlayHostState() }) {
                                SettingsPage(tree, state = editorState)
                            }
                        }
                    }
                }
            }
        }
        fun sampleBacking(): Color {
            val title =
                onNode(hasText("Name") and !hasSetTextAction()).fetchSemanticsNode().boundsInRoot
            val pixels = onRoot().captureToImage().toPixelMap()
            return pixels[pixels.width - 45, title.center.y.toInt()]
        }
        waitForIdle()
        assertEquals(segmentColor, sampleBacking())
        mainClock.autoAdvance = false
        onNodeWithText("Name").performClick()
        mainClock.advanceTimeBy(1000)
        val editorColor = sampleBacking()
        assertTrue(editorColor.blue - editorColor.red > 0.2f, "Editor samples blue: $editorColor")
        mainClock.autoAdvance = true
    }

    @Test
    fun unrelatedSliderDoesNotDrawOverEditorDuringSharedTransition() = runComposeUiTest {
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(TextItem(), SliderItem())
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(primary = Color.Red)) {
                    val source = rememberBackdropLayer()
                    Box(Modifier.size(320.dp, 480.dp)) {
                        Box(Modifier.fillMaxSize().captureBackdrop(source).background(Color.Blue))
                        BackdropHost(source) {
                            OverlayHost(remember { OverlayHostState() }) {
                                SettingsPage(tree)
                            }
                        }
                    }
                }
            }
        }
        val slider =
            onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
                .fetchSemanticsNode()
                .boundsInRoot
        mainClock.autoAdvance = false
        onNodeWithText("Name").performClick()
        for (elapsed in listOf(64L, 128L, 256L, 512L)) {
            mainClock.advanceTimeBy(elapsed)
            val pixels = onRoot().captureToImage().toPixelMap()
            val x = (slider.left + slider.width * 0.25f).toInt()
            val y = slider.center.y.toInt()
            val color = pixels[x, y]
            if (elapsed == 64L) {
                assertTrue(color.red < 0.99f, "The scrim dims the page during opening: $color")
            } else {
                assertTrue(color.red - color.blue < 0.4f, "Unrelated slider above editor: $color")
            }
        }
        mainClock.autoAdvance = true
    }
}
