package top.ltfan.m3x.settingspage.material

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import top.ltfan.m3x.material.LocalMaterialSurfaceRenderer
import top.ltfan.m3x.material.MaterialSurfaceAppearance
import top.ltfan.m3x.material.MaterialSurfaceEdge
import top.ltfan.m3x.material.MaterialSurfaceEffects
import top.ltfan.m3x.material.MaterialSurfaceRenderer
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.MaterialSurfaceStyle
import top.ltfan.m3x.material.PlainMaterialSurfaceRenderer
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState
import top.ltfan.m3x.resource.Resource
import top.ltfan.m3x.settingspage.NumberType
import top.ltfan.m3x.settingspage.RangeBasedNumberInfo
import top.ltfan.m3x.settingspage.SettingsItem
import top.ltfan.m3x.settingspage.SettingsPersistenceStatus
import top.ltfan.m3x.settingspage.SettingsResetMode
import top.ltfan.m3x.settingspage.SettingsStoreDescriber
import top.ltfan.m3x.settingspage.SettingsTree
import top.ltfan.m3x.settingspage.SettingsValue
import top.ltfan.m3x.settingspage.SliderCommitMode
import top.ltfan.m3x.settingspage.compose.SettingsEditSession
import top.ltfan.m3x.settingspage.compose.SettingsPresentationError
import top.ltfan.m3x.settingspage.material.SettingsPageFixtures.ConditionalCategory
import top.ltfan.m3x.settingspage.material.SettingsPageFixtures.DefaultSwitchItem
import top.ltfan.m3x.settingspage.material.SettingsPageFixtures.MultiSelectorItem
import top.ltfan.m3x.settingspage.material.SettingsPageFixtures.RangeItem
import top.ltfan.m3x.settingspage.material.SettingsPageFixtures.RangeSliderItem
import top.ltfan.m3x.settingspage.material.SettingsPageFixtures.RuntimeBoundsSliderItem
import top.ltfan.m3x.settingspage.material.SettingsPageFixtures.SelectorItem
import top.ltfan.m3x.settingspage.material.SettingsPageFixtures.SliderItem
import top.ltfan.m3x.settingspage.material.SettingsPageFixtures.SwitchItem
import top.ltfan.m3x.settingspage.material.SettingsPageFixtures.TextItem

class SettingsPageInteractionTest {
    @Test
    fun settingsSegmentsDrawWithoutASurfaceRenderer() = runComposeUiTest {
        val renderedShapes = mutableListOf<Shape>()
        val tree =
            object : SettingsTree {
                override val items = listOf(TextItem())
            }
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
                    renderedShapes.add(shape)
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
            MaterialTheme {
                CompositionLocalProvider(LocalMaterialSurfaceRenderer provides renderer) {
                    OverlayHost(remember { OverlayHostState() }) { SettingsPage(tree) }
                }
            }
        }
        onNodeWithText("Name").assertIsDisplayed()
        runOnIdle { assertTrue(renderedShapes.isEmpty()) }
    }

    @Test
    fun removingThePageClosesTheEditorAndReleasesItsPresentation() = runComposeUiTest {
        var mounted by mutableStateOf(true)
        val state = SettingsPageState()
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
                            SettingsPage(
                                tree,
                                state = state,
                                labels = SettingsPageLabels(save = Resource.Global.Static("Save")),
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
    fun numericEditorRetainsItsDraftThroughExitAndCanReopen() = runComposeUiTest {
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> =
                    listOf(
                        SliderItem(
                            NumberType.finiteInt(RangeBasedNumberInfo.Type.Slider, 0, 10, 1, true),
                        ),
                    )
            }
        val state = SettingsPageState()
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(remember { OverlayHostState() }) {
                        SettingsPage(
                            tree,
                            state = state,
                            labels = SettingsPageLabels(cancel = Resource.Global.Static("Cancel")),
                        )
                    }
                }
            }
        }
        repeat(2) {
            onNodeWithText("Level").performSemanticsAction(SemanticsActions.OnClick)
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
    fun longPressRestoresDefaultWithoutTogglingSwitch() = runComposeUiTest {
        val item = DefaultSwitchItem()
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(remember { OverlayHostState() }) {
                        SettingsRow(
                            item,
                            SettingsPageLabels(
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
    fun rowsWithoutAnEnabledMenuCannotRestore() = runComposeUiTest {
        val disabled = DefaultSwitchItem(enabled = false, labelText = "Disabled switch")
        val noMenu =
            DefaultSwitchItem(resetMode = SettingsResetMode.None, labelText = "No menu switch")
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(remember { OverlayHostState() }) {
                        Column {
                            SettingsRow(
                                disabled,
                                SettingsPageLabels(
                                    restoreDefault = Resource.Global.Static("Restore default"),
                                ),
                                onOpen = {},
                            )
                            SettingsRow(
                                noMenu,
                                SettingsPageLabels(
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
    fun callerReceivesStructuredValidationFailure() = runComposeUiTest {
        val item = TextItem()
        val store =
            object : SettingsStoreDescriber {
                override val items: List<SettingsItem> = listOf(item)
                override val status =
                    MutableStateFlow<SettingsPersistenceStatus>(SettingsPersistenceStatus.Ready)

                override fun retrySave() = Unit
            }
        var error by mutableStateOf<SettingsPresentationError?>(null)
        val labels = SettingsPageLabels(save = Resource.Global.Static("Save"))
        setContent {
            ProvideM3xMaterialRendering {
                OverlayHost(remember { OverlayHostState() }) {
                    SettingsPage(store, labels = labels, onError = { error = it })
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
    fun persistenceFailuresAreReportedOnceAndNeverRenderedAutomatically() = runComposeUiTest {
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
        var recomposition by mutableIntStateOf(0)
        setContent {
            ProvideM3xMaterialRendering {
                OverlayHost(remember { OverlayHostState() }) {
                    Text("Composition $recomposition")
                    SettingsPage(
                        store,
                        onError = errors::add,
                        labels =
                            SettingsPageLabels(
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
    fun selectorExposesRadioRoleSelectionAndGroupWithCallerModifier() = runComposeUiTest {
        val item = SelectorItem()
        val session = SettingsEditSession().apply { open(item) }
        setContent {
            ProvideM3xMaterialRendering {
                SettingsItemEditor(
                    item,
                    session,
                    null,
                    SettingsPageLabels(),
                    {},
                    Modifier.testTag("editor"),
                )
            }
        }
        onNodeWithTag("editor").assertExists()
        onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup)).assertExists()
        onNodeWithText("One")
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        onNodeWithText("Two").assertIsNotSelected().performClick()
        onNodeWithText("Two").assertIsSelected()
        runOnIdle { assertEquals("two", (session.draft as SettingsValue<*>).id) }
    }

    @Test
    fun rowAndEditorRenderTheSameSharedTextStrings() = runComposeUiTest {
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(SelectorItem())
            }
        setContent {
            ProvideM3xMaterialRendering {
                MaterialTheme {
                    OverlayHost(remember { OverlayHostState() }) {
                        SettingsPage(
                            tree,
                            labels = SettingsPageLabels(cancel = Resource.Global.Static("Cancel")),
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
    fun multiSelectorExposesCheckboxStateAndUpdatesDraftOncePerClick() = runComposeUiTest {
        val item = MultiSelectorItem()
        val session = SettingsEditSession().apply { open(item) }
        setContent {
            ProvideM3xMaterialRendering {
                SettingsItemEditor(item, session, null, SettingsPageLabels(), {})
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
    fun switchRowExposesSwitchStateAndCommitsOnce() = runComposeUiTest {
        val item = SwitchItem()
        setContent {
            ProvideM3xMaterialRendering {
                SettingsRow(item, SettingsPageLabels(), {}, Modifier.testTag("switch"))
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
        onNodeWithTag("switch")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ToggleableState,
                    ToggleableState.On,
                ),
            )
        runOnIdle {
            assertEquals(true, item.value)
            assertEquals(1, item.writes)
        }
    }

    @Test
    fun disabledSwitchRowDoesNotAcceptInput() = runComposeUiTest {
        val item = SwitchItem()
        setContent {
            ProvideM3xMaterialRendering {
                SettingsRow(
                    item,
                    SettingsPageLabels(),
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
    fun wheelUsesDiscreteSliderWithoutChangingTheModel() = runComposeUiTest {
        val type = NumberType.finiteInt(RangeBasedNumberInfo.Type.Wheel, 0, 10, 2, false)
        val setting = SliderItem(type)
        val errors = mutableListOf<SettingsPresentationError>()
        setContent {
            ProvideM3xMaterialRendering {
                SettingsRow(
                    setting,
                    SettingsPageLabels(),
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
    fun sliderControlsShareTheClickableListSurface() = runComposeUiTest {
        val setting =
            SliderItem(NumberType.finiteInt(RangeBasedNumberInfo.Type.Slider, 0, 10, 1, true))
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(setting)
            }
        setContent {
            ProvideM3xMaterialRendering {
                OverlayHost(remember { OverlayHostState() }) { SettingsPage(tree) }
            }
        }
        val rowBounds =
            onNodeWithText("Level").assertHasClickAction().fetchSemanticsNode().boundsInRoot
        val sliderBounds =
            onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
                .fetchSemanticsNode()
                .boundsInRoot
        assertTrue(
            sliderBounds.top >= rowBounds.top && sliderBounds.bottom <= rowBounds.bottom,
            "Slider $sliderBounds should be laid out inside row $rowBounds",
        )
    }

    @Test
    fun inlineOnlySliderRemainsEnabledWithoutOpeningAnEditor() = runComposeUiTest {
        val setting =
            SliderItem(NumberType.finiteInt(RangeBasedNumberInfo.Type.Slider, 0, 10, 1, false))
        val state = SettingsPageState()
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(setting)
            }
        setContent {
            ProvideM3xMaterialRendering {
                OverlayHost(remember { OverlayHostState() }) {
                    SettingsPage(tree, state = state)
                }
            }
        }
        onNodeWithText("Level")
            .assertIsEnabled()
            .assertHasClickAction()
            .performSemanticsAction(SemanticsActions.OnClick)
        runOnIdle { assertEquals(null, state.activeItem) }
        onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .performTouchInput {
                down(center)
                moveTo(centerRight)
                up()
            }
        runOnIdle {
            assertEquals(null, state.activeItem)
            assertEquals(1, setting.writes)
        }
    }

    @Test
    fun numberEditorsApplyModifiersAndRejectInputWithoutDefaultErrorText() = runComposeUiTest {
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
                        SettingsPageLabels(),
                        Modifier.testTag("number"),
                        errors::add,
                    )
                    RangeEditor(
                        range,
                        rangeSession,
                        SettingsPageLabels(),
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
    fun editingTextDiscardsOnCancelAndPersistsOnSave() = runComposeUiTest {
        val item = TextItem()
        val state = SettingsPageState()
        val store =
            object : SettingsStoreDescriber {
                override val items: List<SettingsItem> = listOf(item)
                override val status =
                    MutableStateFlow<SettingsPersistenceStatus>(SettingsPersistenceStatus.Ready)

                override fun retrySave() = Unit
            }
        setContent {
            ProvideM3xMaterialRendering {
                OverlayHost(remember { OverlayHostState() }) {
                    SettingsPage(
                        store,
                        state = state,
                        labels =
                            SettingsPageLabels(
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
    fun sliderWritesOnlyAfterGestureFinishes() = runComposeUiTest {
        val setting = SliderItem()
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(setting)
            }
        setContent {
            ProvideM3xMaterialRendering {
                OverlayHost(remember { OverlayHostState() }) { SettingsPage(tree) }
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
    fun rangeSliderWritesOnlyAfterGestureFinishes() = runComposeUiTest {
        val setting = RangeSliderItem()
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(setting)
            }
        setContent {
            ProvideM3xMaterialRendering {
                OverlayHost(remember { OverlayHostState() }) { SettingsPage(tree) }
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
    fun hiddenAncestorCannotCommitSliderOnRelease() = runComposeUiTest {
        val setting = SliderItem()
        val category = ConditionalCategory(setting)
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(category)
            }
        setContent {
            ProvideM3xMaterialRendering {
                OverlayHost(remember { OverlayHostState() }) { SettingsPage(tree) }
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
    fun immediateSliderCommitsBeforeReleaseWithoutDuplicateFinalWrite() = runComposeUiTest {
        val setting = SliderItem()
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(setting)
            }
        setContent {
            ProvideM3xMaterialRendering {
                OverlayHost(remember { OverlayHostState() }) {
                    SettingsPage(tree, sliderCommitMode = SliderCommitMode.WhileDragging)
                }
            }
        }
        val slider = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
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
    fun immediateRangeSliderCommitsBeforeReleaseWithoutDuplicateFinalWrite() = runComposeUiTest {
        val setting = RangeSliderItem()
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(setting)
            }
        setContent {
            ProvideM3xMaterialRendering {
                OverlayHost(remember { OverlayHostState() }) {
                    SettingsPage(tree, sliderCommitMode = SliderCommitMode.WhileDragging)
                }
            }
        }
        val slider =
            onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))[1]
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
    fun runtimeBoundsAndValueChangesRefreshTheSliderRow() = runComposeUiTest {
        val setting = RuntimeBoundsSliderItem()
        val tree =
            object : SettingsTree {
                override val items: List<SettingsItem> = listOf(setting)
            }
        setContent {
            ProvideM3xMaterialRendering {
                OverlayHost(remember { OverlayHostState() }) { SettingsPage(tree) }
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
}
