package top.ltfan.m3x.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.jetbrains.compose.resources.getString
import top.ltfan.m3x.compose.test.runInitializedComposeUiTest
import top.ltfan.m3x.datastore.MutableStore
import top.ltfan.m3x.datastore.compose.storeHost
import top.ltfan.m3x.demo.generated.resources.Res
import top.ltfan.m3x.demo.generated.resources.demo_animate
import top.ltfan.m3x.demo.generated.resources.demo_editor_demo_only_these_settings_do_not_affect_the_app
import top.ltfan.m3x.demo.generated.resources.demo_enable_profile
import top.ltfan.m3x.demo.generated.resources.demo_interactions
import top.ltfan.m3x.demo.generated.resources.demo_live_resource
import top.ltfan.m3x.demo.generated.resources.demo_persistent_glass_tooltip
import top.ltfan.m3x.demo.generated.resources.demo_tooltip
import top.ltfan.m3x.material.backdrop.BackdropLargeFlexibleTopAppBar
import top.ltfan.m3x.material.bundle.M3xMaterialTheme
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState

class DemoLayoutTest {
    @Test
    fun largeBarCollapseUpdatesScrollContentInsets() = runInitializedComposeUiTest {
        var scrollBehavior: TopAppBarScrollBehavior? = null
        setContent {
            val behavior =
                rememberDemoScrollBehavior(DemoRoute.Components, BarVariant.Large, 400.dp)!!
            scrollBehavior = behavior
            M3xMaterialTheme {
                Scaffold(
                    modifier = Modifier.nestedScroll(behavior.nestedScrollConnection),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        BackdropLargeFlexibleTopAppBar(
                            title = { Text("Large") },
                            modifier = Modifier.testTag("demo-large-bar"),
                            scrollBehavior = behavior,
                        )
                    },
                ) { insets ->
                    CompositionLocalProvider(
                        LocalDemoTopInset provides insets.calculateTopPadding(),
                    ) {
                        LazyColumn(
                            Modifier.testTag("demo-scroll-content"),
                            contentPadding = DemoContentPadding,
                        ) {
                            item { Text("first", Modifier.testTag("demo-first-item")) }
                            items((0..20).toList()) { Text("row $it") }
                        }
                    }
                }
            }
        }
        waitForIdle()
        val bar = onNodeWithTag("demo-large-bar")
        val firstItem = onNodeWithTag("demo-first-item")
        val initialBarHeight = bar.fetchSemanticsNode().boundsInRoot.height
        val initialItemTop = firstItem.fetchSemanticsNode().boundsInRoot.top
        val initialListOffset =
            onNodeWithTag("demo-scroll-content")
                .fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange]
                .value()
        runOnIdle {
            scrollBehavior!!
                .nestedScrollConnection
                .onPreScroll(
                    Offset(0f, -40f),
                    NestedScrollSource.UserInput,
                )
        }
        waitForIdle()
        assertTrue(bar.fetchSemanticsNode().boundsInRoot.height < initialBarHeight)
        assertTrue(firstItem.fetchSemanticsNode().boundsInRoot.top < initialItemTop)
        val finalListOffset =
            onNodeWithTag("demo-scroll-content")
                .fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange]
                .value()
        assertEquals(initialListOffset, finalListOffset)
    }

    @Test
    fun narrowLabDisablesScrollAndWideReturnResetsBar() = runInitializedComposeUiTest {
        val width = mutableStateOf(DemoLabWideBreakpoint)
        var behavior: TopAppBarScrollBehavior? = null
        setContent {
            behavior = rememberDemoScrollBehavior(DemoRoute.Lab, BarVariant.Large, width.value)
        }
        waitForIdle()
        assertNotNull(behavior)
        runOnIdle {
            behavior!!.state.heightOffsetLimit = -100f
            behavior!!
                .nestedScrollConnection
                .onPreScroll(
                    Offset(0f, -24f),
                    NestedScrollSource.UserInput,
                )
        }
        assertEquals(-24f, behavior!!.state.heightOffset)
        runOnIdle { width.value = DemoLabWideBreakpoint - 1.dp }
        waitForIdle()
        assertNull(behavior)
        runOnIdle { width.value = DemoLabWideBreakpoint }
        waitForIdle()
        assertNotNull(behavior)
        assertEquals(0f, behavior!!.state.heightOffset)
        runOnIdle {
            behavior!!.state.heightOffsetLimit = -100f
            behavior!!
                .nestedScrollConnection
                .onPreScroll(
                    Offset(0f, -24f),
                    NestedScrollSource.UserInput,
                )
        }
        assertEquals(-24f, behavior!!.state.heightOffset)
    }

    @Test
    fun settingsCatalogExplainsItsScopeAndKeepsEditorsUsable() = runInitializedComposeUiTest {
        lateinit var store: MutableStore<FeatureSettings>
        var topInsetPx = 0f
        val scopeExplainer =
            getString(Res.string.demo_editor_demo_only_these_settings_do_not_affect_the_app)
        val editorLabel = getString(Res.string.demo_enable_profile)
        setContent {
            val scope = rememberCoroutineScope()
            val host = remember(scope) { storeHost(scope, DemoStoreFile({ null }, { _, _ -> })) }
            DisposableEffect(host) { onDispose { host.close() } }
            store = remember(host) { host.mutable(FeatureSettingsStore) }
            val density = LocalDensity.current
            topInsetPx = with(density) { 120.dp.toPx() }
            CompositionLocalProvider(LocalDemoTopInset provides 120.dp) {
                Box(Modifier.size(400.dp, 700.dp).testTag("catalog-root")) {
                    M3xMaterialTheme {
                        OverlayHost(OverlayHostState()) {
                            SettingsCatalogPage(store, remember { ManualSettingsTree() })
                        }
                    }
                }
            }
        }
        waitForIdle()
        onNodeWithText(scopeExplainer).assertIsDisplayed()
        val rootTop = onNodeWithTag("catalog-root").fetchSemanticsNode().boundsInRoot.top
        assertEquals(
            rootTop,
            onNodeWithTag("settings-catalog").fetchSemanticsNode().boundsInRoot.top,
        )
        onNodeWithText(editorLabel).assertIsDisplayed().performClick()
        waitForIdle()
        assertFalse(store.value.general.enabled)
        val scrollable = onNode(hasScrollAction())
        scrollable.performScrollToIndex(1)
        scrollable.performSemanticsAction(SemanticsActions.ScrollBy) { action ->
            action(0f, topInsetPx)
        }
        waitForIdle()
        val editorTop = onNodeWithText(editorLabel).fetchSemanticsNode().boundsInRoot.top
        assertTrue(
            editorTop < rootTop + topInsetPx,
            "Editor top $editorTop should be above top inset limit ${rootTop + topInsetPx}",
        )
    }

    @Test
    fun plainTooltipAnchorRemainsUsableWhileHovered() = runInitializedComposeUiTest {
        val anchorLabel = getString(Res.string.demo_tooltip)
        val tooltipLabel = getString(Res.string.demo_persistent_glass_tooltip)
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

    @Test
    fun collapsedBlurExampleDoesNotLeaveSpacingUntilDisposal() = runInitializedComposeUiTest {
        val liveLabel =
            "${getString(Res.string.demo_live_resource)} · 0 " +
                getString(Res.string.demo_interactions)
        val animateLabel = getString(Res.string.demo_animate)
        setContent {
            M3xMaterialTheme { OverlayHost(OverlayHostState()) { ComponentsPage() } }
        }
        fun cardHeight(): Float =
            onNodeWithText(liveLabel).fetchSemanticsNode().parent!!.boundsInRoot.height
        mainClock.autoAdvance = false
        onNodeWithText(animateLabel).performClick()
        val heights = mutableListOf<Float>()
        repeat(70) {
            mainClock.advanceTimeBy(16)
            heights.add(cardHeight())
        }
        val final = heights.last()
        val tail = heights.takeLast(10)
        val lastAboveFinal = tail.lastOrNull { it > final + 0.5f } ?: final
        assertTrue(
            lastAboveFinal - final < 3f,
            "Final height discontinuity: $lastAboveFinal -> $final; $heights",
        )
    }

    @Test
    fun catalogRuntimeBoundsToggleRefreshesTheRuntimeScaleRow() = runInitializedComposeUiTest {
        lateinit var store: MutableStore<FeatureSettings>
        val manual = ManualSettingsTree()
        setContent {
            val scope = rememberCoroutineScope()
            val host = remember(scope) { storeHost(scope, DemoStoreFile({ null }, { _, _ -> })) }
            DisposableEffect(host) { onDispose { host.close() } }
            store = remember(host) { host.mutable(FeatureSettingsStore) }
            Box(Modifier.size(400.dp, 700.dp)) {
                M3xMaterialTheme {
                    OverlayHost(OverlayHostState()) { SettingsCatalogPage(store, manual) }
                }
            }
        }
        waitForIdle()
        onNode(hasScrollAction()).performScrollToNode(hasTestTag("settings-row:manual.scale"))

        fun row(matcher: SemanticsMatcher) =
            onNode(matcher and hasAnyAncestor(hasTestTag("settings-row:manual.scale")))

        fun sliderPosition(): Float =
            row(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
                .fetchSemanticsNode()
                .config[SemanticsProperties.ProgressBarRangeInfo]
                .current

        row(hasText(manual.scale.toString())).assertIsDisplayed()
        assertEquals(0.5f, sliderPosition())
        runOnIdle { manual.toggleScaleBounds() }
        waitForIdle()
        assertEquals(0.25f, sliderPosition())
        row(hasText(manual.scale.toString())).assertIsDisplayed()
        runOnIdle { manual.scale = 3.5f }
        waitForIdle()
        val draggedText = manual.scale.toString()
        row(hasText(draggedText)).assertIsDisplayed()
        runOnIdle { manual.toggleScaleBounds() }
        waitForIdle()
        assertEquals(2f, manual.scale)
        assertEquals(1f, sliderPosition())
        row(hasText(draggedText)).assertDoesNotExist()
        row(hasText(manual.scale.toString())).assertIsDisplayed()
    }
}
