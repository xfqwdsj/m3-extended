package top.ltfan.m3x.settingspage.material

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.text.TextRange
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
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.compose.animation.LocalBlurAnimationEnabled
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.compose.ui.visual.LocalBlurEnabled
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState
import top.ltfan.m3x.resource.Resource
import top.ltfan.m3x.resource.compose.composeResource
import top.ltfan.m3x.settingspage.NumberType
import top.ltfan.m3x.settingspage.RangeBasedNumberInfo
import top.ltfan.m3x.settingspage.SettingsItem
import top.ltfan.m3x.settingspage.SettingsPersistenceStatus
import top.ltfan.m3x.settingspage.SettingsResetMode
import top.ltfan.m3x.settingspage.SettingsResources
import top.ltfan.m3x.settingspage.SettingsStoreDescriber
import top.ltfan.m3x.settingspage.SettingsTree
import top.ltfan.m3x.settingspage.SettingsValidation
import top.ltfan.m3x.settingspage.SettingsValue
import top.ltfan.m3x.settingspage.SliderCommitMode
import top.ltfan.m3x.settingspage.SliderMode
import top.ltfan.m3x.settingspage.compose.SettingsEditSession
import top.ltfan.m3x.settingspage.compose.SettingsPresentationError

class CardSettingsTest {
    @Test
    fun removingThePageClosesTheEditorAndReleasesItsPresentation() = runInitializedComposeUiTest {
        var mounted by mutableStateOf(true)
        val state = CardSettingsState()
        val item = TextItem()
        val tree =
            object : SettingsTree {
                override val items = listOf(item)
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(remember { OverlayHostState() }) {
                        if (mounted)
                            CardSettings(
                                tree,
                                state = state,
                                cardBackdrop = false,
                                labels = CardSettingsLabels(save = Resource.Global.Static("Save")),
                            )
                    }
                }
            }
        }
        onNodeWithText("Name").performClick()
        val previousRoot = state.transitionState
        runOnIdle { mounted = false }
        waitForIdle()
        onNodeWithText("Save").assertDoesNotExist()
        runOnIdle {
            assertEquals(null, state.activeItem)
            assertTrue(previousRoot !== state.transitionState)
            assertEquals(null, state.transitionState.currentState)
        }
        runOnIdle { mounted = true }
        onNodeWithText("Name").performClick()
        onNodeWithText("Save").assertIsDisplayed()
        runOnIdle { state.close() }
        waitForIdle()
        onNodeWithText("Save").assertDoesNotExist()
    }

    @Test
    fun predictiveBackSeeksTheSettingsRowAndEditorTogether() = runInitializedComposeUiTest {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val owner =
            object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher = dispatcher
            }
        val state = CardSettingsState()
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
                                CardSettings(tree, state = state, cardBackdrop = false)
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

    private fun repeatedBackReopen(reopenDelayMillis: Long) = runInitializedComposeUiTest {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val owner =
            object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher = dispatcher
            }
        val state = CardSettingsState()
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
                                CardSettings(tree, state = state, cardBackdrop = false)
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
    ) = runInitializedComposeUiTest {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)
        val owner =
            object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher = dispatcher
            }
        val state = CardSettingsState()
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
                                CardSettings(tree, state = state, cardBackdrop = false)
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

    private fun rapidEditorReversals(motion: MotionScheme) = runInitializedComposeUiTest {
        mainClock.autoAdvance = false
        val state = CardSettingsState()
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
                                CardSettings(tree, state = state, cardBackdrop = false)
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
    fun sharedEditorContainerFadesOnEveryOpeningAndClosing() = runInitializedComposeUiTest {
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
                                CardSettings(
                                    tree,
                                    cardBackdrop = false,
                                    labels =
                                        CardSettingsLabels(
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
    fun sharedEditorKeepsTheRowsRippleInsideItsCardCorners() = runInitializedComposeUiTest {
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(TextItem())
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme(colorScheme = lightColorScheme(onSurface = Color.Red)) {
                    Box(Modifier.size(320.dp, 480.dp).background(Color.Blue)) {
                        OverlayHost(OverlayHostState()) {
                            CardSettings(
                                tree,
                                cardBackdrop = false,
                                cardShape = RoundedCornerShape(32.dp),
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
        assertTrue(corner.red < 0.025f, "Shared source ripple escapes rounded card: $corner")
        mainClock.autoAdvance = true
    }

    @Test
    fun numericEditorRetainsItsDraftThroughExitAndCanReopen() = runInitializedComposeUiTest {
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> =
                    listOf(
                        SliderItem(
                            NumberType.finiteInt(RangeBasedNumberInfo.Type.Slider, 0, 10, 1, true),
                        ),
                    )
            }
        val state = CardSettingsState()
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(OverlayHostState()) {
                        CardSettings(
                            tree,
                            state = state,
                            labels = CardSettingsLabels(cancel = Resource.Global.Static("Cancel")),
                        )
                    }
                }
            }
        }
        repeat(2) {
            onNodeWithText("Level").performClick()
            val retiring = state.editSession
            onNodeWithText("Cancel").performClick()
            waitForIdle()
            runOnIdle {
                assertEquals(null, state.activeItem)
                assertEquals(null, state.draft)
                assertEquals(5, retiring.draft)
            }
        }
    }

    @Test
    fun showHideRowRemainsMeasuredAndShrinksDuringExit() = runInitializedComposeUiTest {
        val item = ConditionalTextItem()
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(item)
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(OverlayHostState()) { CardSettings(tree) }
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
    fun enteringRowAndRapidVisibilityReversalKeepOneStableRow() = runInitializedComposeUiTest {
        val item = ConditionalTextItem()
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(item)
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(OverlayHostState()) { CardSettings(tree) }
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
    fun childEntersWithinVisibleCategoryAndReversesWithoutReordering() =
        runInitializedComposeUiTest {
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
                    MaterialTheme { OverlayHost(OverlayHostState()) { CardSettings(tree) } }
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
    fun newlyVisibleGroupRowsEnterBesideAnOnscreenNeighbor() = runInitializedComposeUiTest {
        val stable = ConditionalTextItem(id = "group-anchor")
        val newRow = ConditionalTextItem(id = "new-group-row")
        val newGroup = ConditionalCategory(newRow, initiallyShown = false)
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(stable, newGroup)
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme { OverlayHost(OverlayHostState()) { CardSettings(tree) } }
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
    fun hidingWholeCategoryShrinksItsRowsBeforeRemovingThem() = runInitializedComposeUiTest {
        val item = ConditionalTextItem()
        val category = ConditionalCategory(item)
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(category)
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme { OverlayHost(OverlayHostState()) { CardSettings(tree) } }
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
    fun longPressRestoresDefaultWithoutTogglingSwitch() = runInitializedComposeUiTest {
        val item = DefaultSwitchItem()
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(OverlayHostState()) {
                        SettingsRow(
                            item,
                            CardSettingsLabels(
                                restoreDefault = Resource.Global.Static("Restore default"),
                            ),
                            onOpen = {},
                        )
                    }
                }
            }
        }
        onNodeWithText("Default switch").performTouchInput { longClick() }
        onNodeWithText("Restore default").assertIsDisplayed().performClick()
        waitForIdle()
        assertEquals(false, item.value)
        assertEquals(1, item.writes)
    }

    @Test
    fun secondaryClickRestoresDefault() = runInitializedComposeUiTest {
        val item = DefaultSwitchItem()
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(OverlayHostState()) {
                        Box(Modifier.size(320.dp, 240.dp)) {
                            SettingsRow(
                                item,
                                CardSettingsLabels(
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
    fun replacingTheRowItemReopensItsMenuAtTheCurrentPointer() = runInitializedComposeUiTest {
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
                                CardSettingsLabels(
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
    fun rowsWithoutAnEnabledMenuCannotRestore() = runInitializedComposeUiTest {
        val disabled = DefaultSwitchItem(enabled = false, labelText = "Disabled switch")
        val noMenu =
            DefaultSwitchItem(resetMode = SettingsResetMode.None, labelText = "No menu switch")
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(OverlayHostState()) {
                        Column {
                            SettingsRow(
                                disabled,
                                CardSettingsLabels(
                                    restoreDefault = Resource.Global.Static("Restore default"),
                                ),
                                onOpen = {},
                            )
                            SettingsRow(
                                noMenu,
                                CardSettingsLabels(
                                    restoreDefault = Resource.Global.Static("Restore default"),
                                ),
                                onOpen = {},
                            )
                        }
                    }
                }
            }
        }
        onNodeWithText("Disabled switch").performTouchInput { longClick() }
        onNodeWithText("No menu switch").performTouchInput { longClick() }
        waitForIdle()
        onNodeWithText("Restore default").assertDoesNotExist()
        assertEquals(true, disabled.value)
        assertEquals(0, disabled.writes)
    }

    @Test
    fun hidingAnAncestorImmediatelyDisablesTheActiveEditor() = runInitializedComposeUiTest {
        val item = ConditionalTextItem()
        val category = ConditionalCategory(item)
        val state = CardSettingsState()
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(category)
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(OverlayHostState()) {
                        CardSettings(
                            tree,
                            state = state,
                            labels = CardSettingsLabels(save = Resource.Global.Static("Save")),
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
    fun hidingAnAncestorDisablesAnOpenRestoreMenuImmediately() = runInitializedComposeUiTest {
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
                    OverlayHost(OverlayHostState()) {
                        CardSettings(
                            tree,
                            labels =
                                CardSettingsLabels(
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
    fun blurExitUsesTheSameGeometryAndHonorsLocalBlurSwitch() = runInitializedComposeUiTest {
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
                    OverlayHost(OverlayHostState()) {
                        Row {
                            CompositionLocalProvider(LocalBlurEnabled provides true) {
                                Box(Modifier.size(320.dp, 420.dp).testTag("blur-on")) {
                                    CardSettings(
                                        blurTree,
                                        cardBackdrop = false,
                                        contentPadding = PaddingValues(0.dp),
                                        cardSpacing = 0.dp,
                                    )
                                }
                            }
                            CompositionLocalProvider(LocalBlurEnabled provides false) {
                                Box(Modifier.size(320.dp, 420.dp).testTag("blur-off")) {
                                    CardSettings(
                                        sharpTree,
                                        cardBackdrop = false,
                                        contentPadding = PaddingValues(0.dp),
                                        cardSpacing = 0.dp,
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
    fun cardsAndEditorSampleBackdropInsteadOfCoveringItWithOpaqueCards() =
        runInitializedComposeUiTest {
            val editorState = CardSettingsState()
            val tree =
                object : SettingsTree {
                    override val items: List<SettingsItem> = listOf(TextItem())
                }
            setContent {
                ProvideM3xMaterialRendering {
                    MaterialTheme(colorScheme = lightColorScheme()) {
                        val source = rememberBackdropLayer()
                        Box(Modifier.size(240.dp, 240.dp)) {
                            Box(
                                Modifier.fillMaxSize()
                                    .captureBackdrop(source)
                                    .background(Color.Blue),
                            )
                            Box(Modifier.fillMaxSize().background(Color.Red))
                            BackdropHost(source) {
                                OverlayHost(OverlayHostState()) {
                                    CardSettings(tree, state = editorState)
                                }
                            }
                        }
                    }
                }
            }
            fun verifySample() {
                val title =
                    onNode(hasText("Name") and !hasSetTextAction())
                        .fetchSemanticsNode()
                        .boundsInRoot
                val pixels = onRoot().captureToImage().toPixelMap()
                val color = pixels[pixels.width - 45, title.center.y.toInt()]
                assertTrue(color.blue - color.red > 0.2f, "Sampled blue backing: $color")
            }
            waitForIdle()
            verifySample()
            mainClock.autoAdvance = false
            onNodeWithText("Name").performClick()
            mainClock.advanceTimeBy(1000)
            verifySample()
            mainClock.autoAdvance = true
        }

    @Test
    fun unrelatedSliderDoesNotDrawOverEditorDuringSharedTransition() = runInitializedComposeUiTest {
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
                            OverlayHost(OverlayHostState()) {
                                CardSettings(tree, cardBackdrop = false)
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

    @Test
    fun callerReceivesStructuredValidationFailure() = runInitializedComposeUiTest {
        val item = TextItem()
        val store =
            object : SettingsStoreDescriber {
                override val items: List<SettingsItem> = listOf(item)
                override val status =
                    MutableStateFlow<SettingsPersistenceStatus>(SettingsPersistenceStatus.Ready)

                override fun retrySave() = Unit
            }
        var error by mutableStateOf<SettingsPresentationError?>(null)
        val labels = CardSettingsLabels(save = Resource.Global.Static("Save"))
        setContent {
            ProvideM3xMaterialRendering {
                OverlayHost(OverlayHostState()) {
                    CardSettings(store, labels = labels, onError = { error = it })
                }
            }
        }
        onNodeWithText("Name").performClick()
        onNode(hasSetTextAction()).performTextReplacement("x")
        onNodeWithText("Save").performClick()
        runOnIdle {
            assertIs<SettingsPresentationError.EditFailed>(error)
            assertEquals("before", item.value)
        }
    }

    @Test
    fun persistenceFailuresAreReportedOnceAndNeverRenderedAutomatically() =
        runInitializedComposeUiTest {
            val failure = IllegalStateException("private-storage-path")
            val store =
                object : SettingsStoreDescriber {
                    override val items: List<SettingsItem> = emptyList()
                    override val status =
                        MutableStateFlow<SettingsPersistenceStatus>(
                            SettingsPersistenceStatus.ReadFailed(failure),
                        )

                    override fun retrySave() = Unit
                }
            val errors = mutableListOf<SettingsPresentationError>()
            var recomposition by mutableStateOf(0)
            setContent {
                ProvideM3xMaterialRendering {
                    OverlayHost(OverlayHostState()) {
                        Text("Composition $recomposition")
                        CardSettings(
                            store,
                            onError = errors::add,
                            labels =
                                CardSettingsLabels(
                                    readFailed = Resource.Global.Static("Read failure"),
                                    writeFailed = Resource.Global.Static("Write failure"),
                                ),
                        )
                    }
                }
            }
            runOnIdle {
                assertEquals(1, errors.size)
                val error = assertIs<SettingsPresentationError.ReadFailed>(errors.single())
                assertSame(failure, error.cause)
                assertSame(store, error.source.persistence)
                recomposition++
            }
            onNodeWithText("private-storage-path").assertDoesNotExist()
            onNodeWithText("Read failure").assertDoesNotExist()
            runOnIdle {
                assertEquals(1, errors.size)
                store.status.value = SettingsPersistenceStatus.WriteFailed(failure)
            }
            onNodeWithText("Write failure").assertDoesNotExist()
            runOnIdle {
                assertIs<SettingsPresentationError.WriteFailed>(errors.last())
                assertEquals(2, errors.size)
            }
        }

    @Test
    fun selectorExposesRadioRoleSelectionAndGroupWithCallerModifier() =
        runInitializedComposeUiTest {
            val item = SelectorItem()
            val session = SettingsEditSession().apply { open(item) }
            setContent {
                ProvideM3xMaterialRendering {
                    SettingsItemEditor(
                        item,
                        session,
                        null,
                        CardSettingsLabels(),
                        {},
                        Modifier.testTag("editor"),
                    )
                }
            }
            onNodeWithTag("editor").assertExists()
            onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
                .assertExists()
            onNodeWithText("One")
                .assertIsSelected()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            onNodeWithText("Two").assertIsNotSelected().performClick()
            onNodeWithText("Two").assertIsSelected()
            runOnIdle { assertEquals("two", (session.draft as SettingsValue<*>).id) }
        }

    @Test
    fun rowAndEditorRenderTheSameSharedTextStrings() = runInitializedComposeUiTest {
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(SelectorItem())
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(OverlayHostState()) {
                        CardSettings(
                            tree,
                            labels = CardSettingsLabels(cancel = Resource.Global.Static("Cancel")),
                        )
                    }
                }
            }
        }
        onNodeWithText("Choice").assertIsDisplayed()
        onNodeWithText("One").assertIsDisplayed()
        onNodeWithText("Two").assertDoesNotExist()
        onNodeWithText("Choice").performClick()
        waitForIdle()
        // The editor is open, and its title and selected option render the row strings unchanged.
        onNodeWithText("Two").assertIsDisplayed()
        onNodeWithText("Choice").assertIsDisplayed()
        onNodeWithText("One").assertIsDisplayed()
        onNodeWithText("Cancel").performClick()
        waitForIdle()
        onNodeWithText("Choice").assertIsDisplayed()
        onNodeWithText("One").assertIsDisplayed()
        onNodeWithText("Two").assertDoesNotExist()
    }

    @Test
    fun multiSelectorExposesCheckboxStateAndUpdatesDraftOncePerClick() =
        runInitializedComposeUiTest {
            val item = MultiSelectorItem()
            val session = SettingsEditSession().apply { open(item) }
            setContent {
                ProvideM3xMaterialRendering {
                    SettingsItemEditor(item, session, null, CardSettingsLabels(), {})
                }
            }

            onNodeWithText("One")
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
                .assert(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.ToggleableState,
                        ToggleableState.On,
                    ),
                )
            onNodeWithText("Two")
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
                .assert(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.ToggleableState,
                        ToggleableState.Off,
                    ),
                )
                .performClick()
            runOnIdle {
                assertEquals(
                    setOf("one", "two"),
                    (session.draft as Set<*>).map { (it as SettingsValue<*>).id }.toSet(),
                )
                assertEquals(setOf("one"), item.value.map { it.id }.toSet())
            }
            onNodeWithText("One").performClick()
            runOnIdle {
                assertEquals(
                    setOf("two"),
                    (session.draft as Set<*>).map { (it as SettingsValue<*>).id }.toSet(),
                )
                assertEquals(setOf("one"), item.value.map { it.id }.toSet())
            }
        }

    @Test
    fun switchRowExposesSwitchStateAndCommitsOnce() = runInitializedComposeUiTest {
        val item = SwitchItem()
        setContent {
            ProvideM3xMaterialRendering {
                SettingsRow(item, CardSettingsLabels(), {}, Modifier.testTag("switch"))
            }
        }
        onNodeWithTag("switch")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ToggleableState,
                    ToggleableState.Off,
                ),
            )
            .performClick()
        runOnIdle {
            assertEquals(true, item.value)
            assertEquals(1, item.writes)
        }
    }

    @Test
    fun disabledSwitchRowDoesNotAcceptInput() = runInitializedComposeUiTest {
        val item = SwitchItem()
        setContent {
            ProvideM3xMaterialRendering {
                SettingsRow(
                    item,
                    CardSettingsLabels(),
                    {},
                    Modifier.testTag("switch"),
                    interactive = false,
                )
            }
        }
        onNodeWithTag("switch").assertIsNotEnabled().performClick()
        runOnIdle { assertEquals(0, item.writes) }
    }

    @Test
    fun wheelUsesDiscreteSliderWithoutChangingTheModel() = runInitializedComposeUiTest {
        val type = NumberType.finiteInt(RangeBasedNumberInfo.Type.Wheel, 0, 10, 2, false)
        val setting = SliderItem(type)
        val errors = mutableListOf<SettingsPresentationError>()
        setContent {
            ProvideM3xMaterialRendering {
                SettingsRow(
                    setting,
                    CardSettingsLabels(),
                    {},
                    Modifier.testTag("row"),
                    onError = errors::add,
                )
            }
        }
        onNodeWithTag("row").assertExists()
        val slider = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        assertEquals(
            4,
            slider.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].steps,
        )
        slider.performTouchInput {
            down(center)
            moveTo(centerRight)
            up()
        }
        runOnIdle {
            assertEquals(1, setting.writes)
            assertEquals(0, setting.value % 2)
            assertEquals(
                RangeBasedNumberInfo.Type.Wheel,
                setting.type.let { it as NumberType.Finite<*> }.type,
            )
            assertEquals(emptyList(), errors)
        }
    }

    @Test
    fun numberEditorsApplyModifiersAndRejectInputWithoutDefaultErrorText() =
        runInitializedComposeUiTest {
            val single =
                SliderItem(NumberType.finiteInt(RangeBasedNumberInfo.Type.Wheel, 0, 10, 3, true))
            val range = RangeItem()
            val singleSession = SettingsEditSession().apply { open(single) }
            val rangeSession = SettingsEditSession().apply { open(range) }
            val errors = mutableListOf<SettingsPresentationError>()
            setContent {
                ProvideM3xMaterialRendering {
                    Column {
                        NumberEditor(
                            single,
                            singleSession,
                            CardSettingsLabels(),
                            Modifier.testTag("number"),
                            errors::add,
                        )
                        RangeEditor(
                            range,
                            rangeSession,
                            CardSettingsLabels(),
                            Modifier.testTag("range"),
                            errors::add,
                        )
                        NumberStepper(
                            3,
                            single.type as NumberType.Finite<*>,
                            {},
                            Modifier.testTag("stepper"),
                        )
                    }
                }
            }
            onNodeWithTag("number").assertExists()
            onNodeWithTag("range").assertExists()
            onNodeWithTag("stepper").assertExists()
            onNode(hasSetTextAction() and hasText("5"))
                .assert(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.TextSelectionRange,
                        TextRange(1),
                    ),
                )
            onNode(hasSetTextAction() and hasText("1"))
                .assert(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.TextSelectionRange,
                        TextRange(1),
                    ),
                )
            onNode(hasSetTextAction() and hasText("9"))
                .assert(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.TextSelectionRange,
                        TextRange(1),
                    ),
                )
            onNode(hasSetTextAction() and hasText("5")).performTextReplacement("invalid")
            runOnIdle {
                assertIs<SettingsPresentationError.EditFailed>(errors.single())
                assertEquals(5, single.value)
            }
            onNodeWithText("Enter a valid number").assertDoesNotExist()
        }

    @Test
    fun editingTextDiscardsOnCancelAndPersistsOnSave() = runInitializedComposeUiTest {
        val item = TextItem()
        val state = CardSettingsState()
        val store =
            object : SettingsStoreDescriber {
                override val items: List<SettingsItem> = listOf(item)
                override val status =
                    MutableStateFlow<SettingsPersistenceStatus>(SettingsPersistenceStatus.Ready)

                override fun retrySave() = Unit
            }
        setContent {
            ProvideM3xMaterialRendering {
                OverlayHost(OverlayHostState()) {
                    CardSettings(
                        store,
                        state = state,
                        labels =
                            CardSettingsLabels(
                                cancel = Resource.Global.Static("Cancel"),
                                save = Resource.Global.Static("Save"),
                            ),
                    )
                }
            }
        }
        onNodeWithText("Name").assertHasClickAction().assertIsEnabled()
        onNodeWithText("Name").performClick()
        onNode(hasSetTextAction())
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(6)),
            )
        onNode(hasSetTextAction()).performTextInputSelection(TextRange(2))
        onNode(hasSetTextAction()).performTextInput("X")
        onNode(hasSetTextAction() and hasText("beXfore"))
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(3)),
            )
        runOnIdle { state.changeDraft("updated") }
        onNode(hasSetTextAction() and hasText("updated"))
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(7)),
            )
        onNode(hasSetTextAction()).performTextReplacement("cancelled")
        onNodeWithText("Cancel").performClick()
        waitForIdle()
        assertEquals("before", item.value)

        onNodeWithText("Name").performClick()
        onNode(hasSetTextAction())
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(6)),
            )
        onNode(hasSetTextAction()).performTextReplacement("after")
        onNodeWithText("Save").performClick()
        waitForIdle()
        assertEquals("after", item.value)
        onNodeWithText("Name").performClick()
        onNode(hasSetTextAction())
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(5)),
            )
        onNode(hasSetTextAction()).performTextInput("!")
        onNode(hasSetTextAction()).performImeAction()
        waitForIdle()
        assertEquals("after!", item.value)
    }

    @Test
    fun sliderWritesOnlyAfterGestureFinishes() = runInitializedComposeUiTest {
        val setting = SliderItem()
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(setting)
            }
        setContent {
            ProvideM3xMaterialRendering {
                OverlayHost(OverlayHostState()) { CardSettings(tree) }
            }
        }
        val slider = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        slider.performTouchInput {
            down(center)
            moveTo(centerRight)
        }
        assertEquals(0, setting.writes)
        onRoot().performTouchInput { up() }
        waitForIdle()
        assertEquals(1, setting.writes)
    }

    @Test
    fun rangeSliderWritesOnlyAfterGestureFinishes() = runInitializedComposeUiTest {
        val setting = RangeSliderItem()
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(setting)
            }
        setContent {
            ProvideM3xMaterialRendering {
                OverlayHost(OverlayHostState()) { CardSettings(tree) }
            }
        }
        val slider =
            onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))[1]
        slider.performTouchInput {
            down(center)
            moveTo(center + Offset(160f, 0f))
        }
        assertEquals(0, setting.writes)
        slider.performTouchInput { up() }
        waitForIdle()
        assertEquals(1, setting.writes)
        assertEquals(2 to 10, setting.value)
    }

    @Test
    fun hiddenAncestorCannotCommitSliderOnRelease() = runInitializedComposeUiTest {
        val setting = SliderItem()
        val category = ConditionalCategory(setting)
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(category)
            }
        setContent {
            ProvideM3xMaterialRendering {
                OverlayHost(OverlayHostState()) { CardSettings(tree) }
            }
        }
        val slider = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        slider.performTouchInput {
            down(center)
            moveTo(centerRight)
        }
        assertEquals(0, setting.writes)
        runOnIdle { category.shown = false }
        onRoot().performTouchInput { up() }
        waitForIdle()
        assertEquals(0, setting.writes)
        assertEquals(5, setting.value)
    }

    @Test
    fun immediateSliderCommitsBeforeReleaseWithoutDuplicateFinalWrite() =
        runInitializedComposeUiTest {
            val setting = SliderItem()
            val tree =
                object : SettingsTree {
                    override val items: List<SettingsItem> = listOf(setting)
                }
            setContent {
                ProvideM3xMaterialRendering {
                    OverlayHost(OverlayHostState()) {
                        CardSettings(tree, sliderCommitMode = SliderCommitMode.WhileDragging)
                    }
                }
            }
            val slider =
                onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            slider.performTouchInput {
                down(center)
                moveTo(centerRight)
            }
            waitForIdle()
            assertTrue(setting.writes > 0)
            assertEquals(10, setting.value)
            val writes = setting.writes
            slider.performTouchInput { up() }
            waitForIdle()
            assertEquals(writes, setting.writes)
        }

    @Test
    fun immediateRangeSliderCommitsBeforeReleaseWithoutDuplicateFinalWrite() =
        runInitializedComposeUiTest {
            val setting = RangeSliderItem()
            val tree =
                object : SettingsTree {
                    override val items: List<SettingsItem> = listOf(setting)
                }
            setContent {
                ProvideM3xMaterialRendering {
                    OverlayHost(OverlayHostState()) {
                        CardSettings(tree, sliderCommitMode = SliderCommitMode.WhileDragging)
                    }
                }
            }
            val slider =
                onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))[
                    1]
            slider.performTouchInput {
                down(center)
                moveTo(center + Offset(160f, 0f))
            }
            waitForIdle()
            assertTrue(setting.writes > 0)
            assertEquals(2 to 10, setting.value)
            val writes = setting.writes
            slider.performTouchInput { up() }
            waitForIdle()
            assertEquals(writes, setting.writes)
        }

    @Test
    fun runtimeBoundsAndValueChangesRefreshTheSliderRow() = runInitializedComposeUiTest {
        val setting = RuntimeBoundsSliderItem()
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(setting)
            }
        setContent {
            ProvideM3xMaterialRendering {
                OverlayHost(OverlayHostState()) { CardSettings(tree) }
            }
        }

        fun sliderPosition(): Float =
            onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
                .fetchSemanticsNode()
                .config[SemanticsProperties.ProgressBarRangeInfo]
                .current

        waitForIdle()
        onNodeWithText("Runtime scale").assertIsDisplayed()
        val initialText = setting.value.toString()
        onNodeWithText(initialText).assertIsDisplayed()
        assertEquals(0.5f, sliderPosition())
        runOnIdle { setting.maximumScale = 4f }
        waitForIdle()
        assertEquals(0.25f, sliderPosition())
        onNodeWithText(initialText).assertIsDisplayed()
        runOnIdle { setting.value = 3.5f }
        waitForIdle()
        val draggedText = setting.value.toString()
        onNodeWithText(draggedText).assertIsDisplayed()
        runOnIdle {
            setting.maximumScale = 2f
            setting.value = setting.value.coerceAtMost(2f)
        }
        waitForIdle()
        assertEquals(1f, sliderPosition())
        onNodeWithText(draggedText).assertDoesNotExist()
        onNodeWithText(setting.value.toString()).assertIsDisplayed()
    }

    private class RuntimeBoundsSliderItem : SettingsItem.Item.SingleNumber<Float> {
        var maximumScale: Float by mutableFloatStateOf(2f)
        private val state = mutableFloatStateOf(1f)

        override var value: Float
            get() = state.floatValue
            set(value) {
                state.floatValue = value
            }

        override val type: NumberType<Float>
            get() =
                NumberType.finiteFloat(
                    RangeBasedNumberInfo.Type.Slider,
                    0f,
                    maximumScale,
                    0.1f,
                    true,
                    SliderMode.Continuous,
                )

        override val id: String = "runtime-scale"
        override val children: List<SettingsItem>? = null
        override val shouldShow: Boolean = true
        override val enabled: Boolean = true
        override val resources: SettingsResources =
            object : SettingsResources {
                override val label: Resource<String> = Resource.Global.Static("Runtime scale")
            }
    }

    private class SliderItem(
        override val type: NumberType<Int> =
            NumberType.finiteInt(
                RangeBasedNumberInfo.Type.Slider,
                0,
                10,
                1,
                false,
            ),
    ) : SettingsItem.Item.SingleNumber<Int> {
        private val state = mutableStateOf(5)
        var writes: Int = 0
            private set

        override var value: Int
            get() = state.value
            set(value) {
                state.value = value
                writes++
            }

        override val id: String = "level"
        override val children: List<SettingsItem>? = null
        override val shouldShow: Boolean = true
        override val enabled: Boolean = true
        override val resources: SettingsResources =
            object : SettingsResources {
                override val label: Resource<String> = Resource.Global.Static("Level")
            }
    }

    private class SwitchItem : SettingsItem.Item.Switch {
        private val state = mutableStateOf(false)
        var writes = 0
            private set

        override var value: Boolean
            get() = state.value
            set(value) {
                state.value = value
                writes++
            }

        override val id = "switch"
        override val children: List<SettingsItem>? = null
        override val shouldShow = true
        override val enabled = true
        override val resources =
            object : SettingsResources {
                override val label: Resource<String> = Resource.Global.Static("Switch")
                override val description: Resource<String> = Resource.Global.Static("Off")
            }
    }

    private class DefaultSwitchItem(
        initialValue: Boolean = true,
        override val enabled: Boolean = true,
        override val resetMode: SettingsResetMode = SettingsResetMode.Menu,
        private val labelText: String = "Default switch",
        override val id: String = "default-switch",
    ) : SettingsItem.Item.Switch {
        private var storedValue = initialValue
        override var value: Boolean
            get() = storedValue
            set(value) {
                storedValue = value
                writes++
            }

        var writes = 0
        override val defaultValue = false
        override val children: List<SettingsItem>? = null
        override val shouldShow = true
        override val resources =
            object : SettingsResources {
                override val label: Resource<String> = Resource.Global.Static(labelText)
            }
    }

    private class RangeItem : SettingsItem.Item.NumberRange<Int> {
        override val id = "range"
        override var value = 1 to 9
        override val startType = NumberType.inputInt()
        override val endType = NumberType.inputInt()
        override val children: List<SettingsItem>? = null
        override val shouldShow = true
        override val enabled = true
        override val resources =
            object : SettingsResources {
                override val label: Resource<String> = Resource.Global.Static("Range")
            }
    }

    private class RangeSliderItem : SettingsItem.Item.NumberRange<Int> {
        private val type = NumberType.finiteInt(RangeBasedNumberInfo.Type.Slider, 0, 10, 1, false)
        private val state = mutableStateOf(2 to 8)
        var writes: Int = 0
            private set

        override val id = "range-slider"
        override var value: Pair<Int, Int>
            get() = state.value
            set(value) {
                state.value = value
                writes++
            }

        override val startType = type
        override val endType = type
        override val children: List<SettingsItem>? = null
        override val shouldShow = true
        override val enabled = true
        override val resources =
            object : SettingsResources {
                override val label: Resource<String> = Resource.Global.Static("Range slider")
            }
    }

    private class SelectorItem : SettingsItem.Item.Selector<Int> {
        override val id = "selector"
        override val options = listOf(option("one", "One", 1), option("two", "Two", 2))
        override var value: SettingsValue<Int> = options.first()
        override val children: List<SettingsItem>? = null
        override val shouldShow = true
        override val enabled = true
        override val resources =
            object : SettingsResources {
                override val label: Resource<String> = Resource.Global.Static("Choice")
            }
    }

    private class MultiSelectorItem : SettingsItem.Item.MultiSelector<Int> {
        override val id = "multi-selector"
        override val options = listOf(option("one", "One", 1), option("two", "Two", 2))
        override var value = setOf(options.first())
        override val children: List<SettingsItem>? = null
        override val shouldShow = true
        override val enabled = true
        override val resources =
            object : SettingsResources {
                override val label: Resource<String> = Resource.Global.Static("Choices")
            }
    }

    companion object {
        private fun option(optionId: String, label: String, number: Int): SettingsValue<Int> =
            object : SettingsValue<Int> {
                override val id = optionId
                override val value = number
                override val resources =
                    object : SettingsResources {
                        override val label: Resource<String> = Resource.Global.Static(label)
                    }
            }
    }

    private class TextItem(override val id: String = "name", label: String = "Name") :
        SettingsItem.Item.Text {
        private val state = mutableStateOf("before")
        override var value: String
            get() = state.value
            set(value) {
                state.value = value
            }

        override val minLength: Int = 2

        override fun validate(candidate: String): String? =
            SettingsValidation.text(candidate, minLength, maxLength)

        override val children: List<SettingsItem>? = null
        override val shouldShow: Boolean = true
        override val enabled: Boolean = true
        override val resources: SettingsResources =
            object : SettingsResources {
                override val label: Resource<String> = composeResource { label }
            }
    }

    private class ConditionalTextItem(
        override val id: String = "conditional",
        initiallyShown: Boolean = true,
    ) : SettingsItem.Item.Text {
        private val visibility = mutableStateOf(initiallyShown)
        var shown: Boolean
            get() = visibility.value
            set(value) {
                visibility.value = value
            }

        override var value: String = "value"
        override val minLength: Int = 0
        override val maxLength: Int = 20

        override fun validate(candidate: String): String? = null

        override val children: List<SettingsItem>? = null
        override val shouldShow: Boolean
            get() = visibility.value

        override val enabled: Boolean = true
        override val resources: SettingsResources =
            object : SettingsResources {
                override val label: Resource<String> = Resource.Global.Static("Conditional row")
            }
    }

    private class ConditionalCategory(
        item: SettingsItem.Item<*>,
        initiallyShown: Boolean = true,
    ) : SettingsItem.Category {
        private val visibility = mutableStateOf(initiallyShown)
        var shown: Boolean
            get() = visibility.value
            set(value) {
                visibility.value = value
            }

        override val id = "conditional-category"
        override val children: List<SettingsItem> = listOf(item)
        override val shouldShow: Boolean
            get() = visibility.value

        override val enabled: Boolean = true
        override val resources: SettingsResources =
            object : SettingsResources {
                override val label: Resource<String> =
                    Resource.Global.Static("Conditional category")
            }
    }
}
