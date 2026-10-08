package top.ltfan.m3x.settingspage.material

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import org.jetbrains.compose.resources.stringResource
import top.ltfan.m3x.compose.animation.BlurEnterExit
import top.ltfan.m3x.compose.animation.blurTransition
import top.ltfan.m3x.compose.animation.core.withDefaultVisibilityThreshold
import top.ltfan.m3x.compose.foundation.GroupedLazyColumn
import top.ltfan.m3x.compose.foundation.GroupedLazyListScope
import top.ltfan.m3x.compose.material3.CardColorsLowest
import top.ltfan.m3x.compose.material3.ListItemColorsTransparent
import top.ltfan.m3x.compose.material3.sharedBoundsSpec
import top.ltfan.m3x.compose.ui.accessibility.SpatialMotionEnabled
import top.ltfan.m3x.material.CardLazyGroup
import top.ltfan.m3x.material.LocalCardShape
import top.ltfan.m3x.material.LocalMaterialSurfaceRenderer
import top.ltfan.m3x.material.MaterialSurfaceRole
import top.ltfan.m3x.material.MaterialSurfaceStyle
import top.ltfan.m3x.material.OutlinedButton
import top.ltfan.m3x.material.Switch
import top.ltfan.m3x.material.TextButton
import top.ltfan.m3x.material.card
import top.ltfan.m3x.material.header
import top.ltfan.m3x.material.item
import top.ltfan.m3x.material.overlay.OverlayAlertDialogButtons
import top.ltfan.m3x.material.overlay.OverlayAlertDialogContent
import top.ltfan.m3x.material.overlay.OverlayDialog
import top.ltfan.m3x.material.overlay.OverlayMenu
import top.ltfan.m3x.material.overlay.OverlayMenuItem
import top.ltfan.m3x.material.overlay.rememberOverlayMenuPositionProvider
import top.ltfan.m3x.material.theme.extended
import top.ltfan.m3x.overlay.LocalOverlayAnimatedVisibilityScope
import top.ltfan.m3x.overlay.OverlayNavigationTransition
import top.ltfan.m3x.overlay.localSharedTransitionScope
import top.ltfan.m3x.overlay.rememberOverlayNavigationTransition
import top.ltfan.m3x.resource.Resource
import top.ltfan.m3x.resource.compose.composeResource
import top.ltfan.m3x.resource.compose.resolve
import top.ltfan.m3x.settingspage.CompositeSettingsTree
import top.ltfan.m3x.settingspage.NumberType
import top.ltfan.m3x.settingspage.RangeBasedNumberInfo
import top.ltfan.m3x.settingspage.SettingsCommit
import top.ltfan.m3x.settingspage.SettingsItem
import top.ltfan.m3x.settingspage.SettingsItems
import top.ltfan.m3x.settingspage.SettingsNumbers
import top.ltfan.m3x.settingspage.SettingsPersistence
import top.ltfan.m3x.settingspage.SettingsPersistenceStatus
import top.ltfan.m3x.settingspage.SettingsResetMode
import top.ltfan.m3x.settingspage.SettingsSource
import top.ltfan.m3x.settingspage.SettingsTree
import top.ltfan.m3x.settingspage.SettingsValidation
import top.ltfan.m3x.settingspage.SettingsValue
import top.ltfan.m3x.settingspage.SettingsValues
import top.ltfan.m3x.settingspage.SliderCommitMode
import top.ltfan.m3x.settingspage.compose.SettingsEditError
import top.ltfan.m3x.settingspage.compose.SettingsEditSession
import top.ltfan.m3x.settingspage.compose.SettingsPresentationError
import top.ltfan.m3x.settingspage.compose.reportFailure
import top.ltfan.m3x.settingspage.material.generated.resources.Res
import top.ltfan.m3x.settingspage.material.generated.resources.m3x_settings_cancel
import top.ltfan.m3x.settingspage.material.generated.resources.m3x_settings_custom_editor_missing
import top.ltfan.m3x.settingspage.material.generated.resources.m3x_settings_from
import top.ltfan.m3x.settingspage.material.generated.resources.m3x_settings_invalid_bounds
import top.ltfan.m3x.settingspage.material.generated.resources.m3x_settings_invalid_number
import top.ltfan.m3x.settingspage.material.generated.resources.m3x_settings_invalid_step
import top.ltfan.m3x.settingspage.material.generated.resources.m3x_settings_loading
import top.ltfan.m3x.settingspage.material.generated.resources.m3x_settings_not_finite
import top.ltfan.m3x.settingspage.material.generated.resources.m3x_settings_out_of_bounds
import top.ltfan.m3x.settingspage.material.generated.resources.m3x_settings_range_order
import top.ltfan.m3x.settingspage.material.generated.resources.m3x_settings_read_failed
import top.ltfan.m3x.settingspage.material.generated.resources.m3x_settings_restore_default
import top.ltfan.m3x.settingspage.material.generated.resources.m3x_settings_retry
import top.ltfan.m3x.settingspage.material.generated.resources.m3x_settings_save
import top.ltfan.m3x.settingspage.material.generated.resources.m3x_settings_selected_count
import top.ltfan.m3x.settingspage.material.generated.resources.m3x_settings_to
import top.ltfan.m3x.settingspage.material.generated.resources.m3x_settings_too_long
import top.ltfan.m3x.settingspage.material.generated.resources.m3x_settings_too_short
import top.ltfan.m3x.settingspage.material.generated.resources.m3x_settings_unavailable
import top.ltfan.m3x.settingspage.material.generated.resources.m3x_settings_value
import top.ltfan.m3x.settingspage.material.generated.resources.m3x_settings_write_failed
import top.ltfan.m3x.settingspage.sliderMappingOrNull

private val LocalSettingsRowAvailable = compositionLocalOf { { true } }

/** A custom editor changes only the draft. The page commits it on confirmation. */
public typealias CustomSettingsEditor =
    @Composable
    (
        item: SettingsItem.Item.Custom<*>,
        draft: Any?,
        onDraftChange: (Any?) -> Unit,
    ) -> Unit

/**
 * Action labels and optional error text used by [SettingsPresentationError.localizedMessage].
 * Resource adapters can localize each value. Error labels are never rendered automatically.
 */
public class CardSettingsLabels(
    public val loading: Resource<String> = composeResource {
        stringResource(Res.string.m3x_settings_loading)
    },
    public val readFailed: Resource<String> = composeResource {
        stringResource(Res.string.m3x_settings_read_failed)
    },
    public val writeFailed: Resource<String> = composeResource {
        stringResource(Res.string.m3x_settings_write_failed)
    },
    public val retry: Resource<String> = composeResource {
        stringResource(Res.string.m3x_settings_retry)
    },
    public val cancel: Resource<String> = composeResource {
        stringResource(Res.string.m3x_settings_cancel)
    },
    public val save: Resource<String> = composeResource {
        stringResource(Res.string.m3x_settings_save)
    },
    public val restoreDefault: Resource<String> = composeResource {
        stringResource(Res.string.m3x_settings_restore_default)
    },
    public val from: Resource<String> = composeResource {
        stringResource(Res.string.m3x_settings_from)
    },
    public val to: Resource<String> = composeResource {
        stringResource(Res.string.m3x_settings_to)
    },
    public val selectedCount: (Int) -> Resource<String> = { count ->
        composeResource { stringResource(Res.string.m3x_settings_selected_count, count) }
    },
    public val invalidNumber: Resource<String> = composeResource {
        stringResource(Res.string.m3x_settings_invalid_number)
    },
    public val unavailable: Resource<String> = composeResource {
        stringResource(Res.string.m3x_settings_unavailable)
    },
    public val value: Resource<String> = composeResource {
        stringResource(Res.string.m3x_settings_value)
    },
    public val customEditorMissing: (String) -> Resource<String> = { id ->
        composeResource { stringResource(Res.string.m3x_settings_custom_editor_missing, id) }
    },
)

/** The draft is discarded whenever the editor closes without confirmation. */
@Stable
public class CardSettingsState {
    private var presentationState by mutableStateOf(SeekableTransitionState<String?>(null))
    public val transitionState: SeekableTransitionState<String?>
        get() = presentationState

    internal fun releasePresentation() {
        dismiss()
        presentationState = SeekableTransitionState(null)
    }

    private var session by mutableStateOf(SettingsEditSession())
    public val editSession: SettingsEditSession
        get() = session

    public val activeItem: SettingsItem.Item<*>?
        get() = editSession.activeItem

    public val draft: Any?
        get() = editSession.draft

    public val error: SettingsPresentationError.EditFailed?
        get() = activeItem?.let { item ->
            editSession.error?.let { SettingsPresentationError.EditFailed(item, it) }
        }

    public fun open(item: SettingsItem.Item<*>) {
        if (!item.enabled || !item.shouldShow) return
        session = SettingsEditSession().apply { open(item) }
    }

    public fun changeDraft(value: Any?) {
        editSession.changeDraft(value)
    }

    public fun invalidInput() {
        editSession.invalidInput()
    }

    public fun dismiss() {
        // Hosted exit content retains its captured session until the dialog finishes drawing.
        session = SettingsEditSession()
    }

    public fun close() {
        dismiss()
    }

    public fun confirm(): Boolean = editSession.confirm()
}

private class SettingsGroupVisibilityRecord(
    var root: SettingsItem,
    initiallyVisible: Boolean,
) {
    val visibility = MutableTransitionState(initiallyVisible)
    val rows = linkedMapOf<String, SettingsVisibilityRecord>()
    val rowOrder = mutableListOf<String>()
}

private class SettingsVisibilityRecord(
    var item: SettingsItem.Item<*>,
    initiallyVisible: Boolean,
) {
    val visibility = MutableTransitionState(initiallyVisible)
}

private fun insertByVisibleNeighbors(
    order: MutableList<String>,
    visibleIds: List<String>,
    id: String,
) {
    if (id in order) return
    val position = visibleIds.indexOf(id)
    val previous = visibleIds.take(position).lastOrNull { it in order }
    val next = visibleIds.drop(position + 1).firstOrNull { it in order }
    when {
        previous != null -> order.add(order.indexOf(previous) + 1, id)
        next != null -> order.add(order.indexOf(next), id)
        else -> order.add(id)
    }
}

/** A rendered settings group: its root item and the rows the page currently shows for it. */
private typealias RenderedSettingsGroup = Pair<SettingsItem, List<SettingsItem.Item<*>>>

/** Whether a source with this status still contributes its items to the page. */
private fun SettingsPersistenceStatus.isReadable(): Boolean =
    this == SettingsPersistenceStatus.Ready || this is SettingsPersistenceStatus.WriteFailed

/** The groups and their visible rows that every readable source of the page renders, in order. */
private fun List<Pair<SettingsSource, SettingsPersistenceStatus>>.readableGroups():
    List<RenderedSettingsGroup> = filter { (_, status) ->
    status.isReadable()
}
    .flatMap { (source, _) -> source.tree.items.flatMap(SettingsItems::visibleGroups) }
    .map { root ->
        val direct = if (root is SettingsItem.Category) root.children else listOf(root)
        root to direct.flatMap(SettingsItems::visibleEntries)
    }
    .filter { (_, rows) -> rows.isNotEmpty() }

/** Whether any readable source of the page still renders the item with [itemId]. */
private fun List<Pair<SettingsSource, SettingsPersistenceStatus>>.rendersItem(
    itemId: String,
): Boolean = any { (source, status) ->
    status.isReadable() &&
        source.tree.items.flatMap(SettingsItems::visibleGroups).any { group ->
            (if (group is SettingsItem.Category) group.children else listOf(group))
                .flatMap(SettingsItems::visibleEntries)
                .any { it.id == itemId }
        }
}

/**
 * Registers every group and row of [currentGroups] and marks them visible. A row that left the tree
 * while still on screen keeps its record and is marked invisible so it can animate out. Returns
 * whether the registry changed.
 */
private fun updateVisibilityRegistry(
    groups: MutableMap<String, SettingsGroupVisibilityRecord>,
    groupOrder: MutableList<String>,
    currentGroups: List<RenderedSettingsGroup>,
    visibleLazyKeys: Set<String>,
    firstVisibleContent: Boolean,
): Boolean {
    var changed = false
    val currentGroupOrder = currentGroups.map { it.first.id }
    currentGroups.forEach { (root, visibleRows) ->
        insertByVisibleNeighbors(groupOrder, currentGroupOrder, root.id)
        val groupOrderIndex = groupOrder.indexOf(root.id)
        val previousGroup = groupOrder.getOrNull(groupOrderIndex - 1)?.let(groups::get)
        val nextGroup = groupOrder.getOrNull(groupOrderIndex + 1)?.let(groups::get)
        val isNewGroup = root.id !in groups
        val adjacentGroupIsVisible =
            previousGroup?.rowOrder?.lastOrNull()?.let { it in visibleLazyKeys } == true ||
                nextGroup?.root?.id?.let { it in visibleLazyKeys } == true ||
                nextGroup?.rowOrder?.firstOrNull()?.let { it in visibleLazyKeys } == true
        val group =
            groups.getOrPut(root.id) {
                changed = true
                SettingsGroupVisibilityRecord(root, firstVisibleContent)
            }
        group.root = root
        group.visibility.targetState = true
        val visibleIds = visibleRows.mapTo(mutableSetOf()) { it.id }
        val visibleOrderIds = visibleRows.map { it.id }
        visibleRows.forEachIndexed { rowIndex, item ->
            insertByVisibleNeighbors(group.rowOrder, visibleOrderIds, item.id)
            val hasVisibleNeighbor =
                item.id in visibleLazyKeys ||
                    root.id in visibleLazyKeys ||
                    (isNewGroup && adjacentGroupIsVisible) ||
                    visibleOrderIds.withIndex().any { (index, candidateId) ->
                        candidateId in visibleLazyKeys &&
                            index >= rowIndex - 1 &&
                            index <= rowIndex + 1
                    }
            val row =
                group.rows.getOrPut(item.id) {
                    changed = true
                    SettingsVisibilityRecord(item, firstVisibleContent || !hasVisibleNeighbor)
                }
            row.item = item
            row.visibility.targetState = true
        }
        group.rows.values.toList().forEach { row ->
            if (row.item.id !in visibleIds) {
                if (row.item.id !in visibleLazyKeys) {
                    group.rows.remove(row.item.id)
                    group.rowOrder.remove(row.item.id)
                    changed = true
                } else row.visibility.targetState = false
            }
        }
    }
    return changed
}

/**
 * Marks every registered group that left [currentGroupIds] invisible and drops the records of the
 * rows that have already left the screen. Returns whether the registry changed.
 */
private fun pruneMissingVisibilityRecords(
    groups: MutableMap<String, SettingsGroupVisibilityRecord>,
    groupOrder: MutableList<String>,
    currentGroupIds: Set<String>,
    visibleLazyKeys: Set<String>,
): Boolean {
    var changed = false
    groups.values.toList().forEach { group ->
        if (group.root.id !in currentGroupIds) {
            group.visibility.targetState = false
            group.rows.values.toList().forEach { row ->
                row.visibility.targetState = false
                if (row.item.id !in visibleLazyKeys) {
                    group.rows.remove(row.item.id)
                    group.rowOrder.remove(row.item.id)
                    changed = true
                }
            }
            if (
                group.root.id !in visibleLazyKeys && group.rows.keys.none { it in visibleLazyKeys }
            ) {
                groups.remove(group.root.id)
                groupOrder.remove(group.root.id)
                changed = true
            }
        }
    }
    return changed
}

/**
 * Drops the records whose exit animation has settled and whose IDs [targets] no longer render.
 * Returns whether the registry changed.
 */
private fun pruneSettledVisibilityRecords(
    groups: MutableMap<String, SettingsGroupVisibilityRecord>,
    groupOrder: MutableList<String>,
    targets: Pair<Set<String>, Set<String>>,
): Boolean {
    var changed = false
    groups.values.toList().forEach { group ->
        group.rows.values.toList().forEach { row ->
            if (
                row.visibility.isIdle &&
                    !row.visibility.currentState &&
                    !row.visibility.targetState &&
                    row.item.id !in targets.second
            ) {
                group.rows.remove(row.item.id)
                group.rowOrder.remove(row.item.id)
                changed = true
            }
        }
        if (
            !group.visibility.targetState &&
                group.rows.isEmpty() &&
                group.root.id !in targets.first &&
                (group.root !is SettingsItem.Category ||
                    (group.visibility.isIdle && !group.visibility.currentState))
        ) {
            groups.remove(group.root.id)
            groupOrder.remove(group.root.id)
            changed = true
        }
    }
    return changed
}

/** The visibility pair of every registered group and row, keyed by ID. */
private fun visibilitySnapshot(
    groups: Map<String, SettingsGroupVisibilityRecord>,
): List<Pair<String, Pair<Boolean, Boolean>>> =
    groups.values.flatMap { group ->
        listOf(group.root.id to (group.visibility.currentState to group.visibility.targetState)) +
            group.rows.values.map { row ->
                row.item.id to (row.visibility.currentState to row.visibility.targetState)
            }
    }

/** The card optics the page applies to the cards it renders. */
private class SettingsCardAppearance(
    val shape: Shape,
    val colors: CardColors,
    val elevation: CardElevation,
    val border: BorderStroke?,
    val material: MaterialSurfaceStyle?,
)

/**
 * The page values a rendered settings card reads while the page records the content of its grouped
 * list.
 *
 * The grouped list DSL records its items outside composition, so the card and row helpers cannot
 * read the page's locals; one instance carries them instead of a long parameter list repeated on
 * every helper.
 */
private class SettingsCardContext(
    val sharedTransition: SharedTransitionScope,
    val state: CardSettingsState,
    val transition: Transition<String?>,
    val navigationTransition: OverlayNavigationTransition,
    val labels: CardSettingsLabels,
    val sliderCommitMode: SliderCommitMode,
    val sourceStatuses: List<Pair<SettingsSource, SettingsPersistenceStatus>>,
    val coroutineScope: CoroutineScope,
    val onError: (SettingsPresentationError) -> Unit,
)

/**
 * Grouped Material settings page. Its shared bounds need a provided SharedTransitionScope; its
 * editor needs an OverlayHost. [cardBackdrop] applies the selected surface renderer inside the
 * grouped card boundaries; disabling it uses [cardColors] directly. The backing spans grouped
 * segments with undecorated edges. [cardMaterial] supplies a renderer-specific style for each
 * segment.
 *
 * Errors are delivered to [onError] and are never rendered automatically. Persistence failures are
 * reported once per observed status transition; edit failures are reported for each failed
 * input/commit action. The caller owns presentation and recovery, including rebuilding a host after
 * a read failure.
 */
@Composable
public fun CardSettings(
    store: SettingsTree,
    modifier: Modifier = Modifier,
    state: CardSettingsState = remember { CardSettingsState() },
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    cardSpacing: Dp = 12.dp,
    cardShape: Shape = MaterialTheme.shapes.large,
    cardColors: CardColors = CardColorsLowest,
    cardElevation: CardElevation = CardDefaults.cardElevation(),
    cardBorder: BorderStroke? = null,
    editorShape: Shape = MaterialTheme.shapes.extraLarge,
    customEditor: CustomSettingsEditor? = null,
    labels: CardSettingsLabels = remember { CardSettingsLabels() },
    sliderCommitMode: SliderCommitMode = SliderCommitMode.OnRelease,
    cardBackdrop: Boolean = true,
    cardMaterial: MaterialSurfaceStyle? = null,
    onError: (SettingsPresentationError) -> Unit = {},
) {
    val resolvedCardMaterial =
        if (!cardBackdrop) null
        else cardMaterial ?: LocalMaterialSurfaceRenderer.current.style(MaterialSurfaceRole.Surface)
    DisposableEffect(state) {
        onDispose { state.releasePresentation() }
    }
    val transition = rememberTransition(state.transitionState)
    val navigationTransition =
        rememberOverlayNavigationTransition(
            state.transitionState,
            transition,
            state.activeItem?.id,
            null,
            isVisible = { id, key -> id != null && key == "setting:$id" },
        )
    val coroutineScope = rememberCoroutineScope()
    val visibilityGroups = remember { linkedMapOf<String, SettingsGroupVisibilityRecord>() }
    val groupOrder = remember { mutableListOf<String>() }
    var visibilityRevision by remember { mutableStateOf(0) }
    val visibilityInitialized = remember { mutableStateOf(false) }
    SettingsItems.requireUniqueIds(store.items)
    val sources =
        if (store is CompositeSettingsTree) {
            store.sources
        } else {
            listOf(SettingsSource("default", store, store as? SettingsPersistence))
        }
    val activeItem = state.activeItem
    val sourceStatuses = rememberSourceStatuses(sources, onError)
    val visibleLazyKeys by
        remember(lazyListState) {
            derivedStateOf {
                lazyListState.layoutInfo.visibleItemsInfo.mapTo(mutableSetOf()) {
                    it.key.toString()
                }
            }
        }
    val currentGroups = sourceStatuses.readableGroups()
    val firstVisibleContent = !visibilityInitialized.value
    val currentGroupIds = currentGroups.mapTo(mutableSetOf()) { (root, _) -> root.id }
    val currentRowIds = currentGroups.flatMapTo(mutableSetOf()) { (_, rows) -> rows.map { it.id } }
    val activeEligibleState =
        remember(activeItem, sourceStatuses) {
            derivedStateOf {
                activeItem != null &&
                    activeItem.enabled &&
                    activeItem.shouldShow &&
                    sourceStatuses.rendersItem(activeItem.id)
            }
        }
    val activeEligible by activeEligibleState
    LaunchedEffect(activeItem?.id, activeEligible) {
        if (activeItem != null && !activeEligible) state.close()
    }
    val latestTargets by rememberUpdatedState(currentGroupIds to currentRowIds)
    var registryChanged =
        updateVisibilityRegistry(
            groups = visibilityGroups,
            groupOrder = groupOrder,
            currentGroups = currentGroups,
            visibleLazyKeys = visibleLazyKeys,
            firstVisibleContent = firstVisibleContent,
        )
    if (currentGroups.isNotEmpty()) visibilityInitialized.value = true
    if (
        pruneMissingVisibilityRecords(
            groups = visibilityGroups,
            groupOrder = groupOrder,
            currentGroupIds = currentGroupIds,
            visibleLazyKeys = visibleLazyKeys,
        )
    ) {
        registryChanged = true
    }
    SideEffect {
        if (registryChanged) visibilityRevision++
    }
    val groupsForRender =
        remember(visibilityRevision) { groupOrder.mapNotNull { visibilityGroups[it] } }
    LaunchedEffect(Unit) {
        snapshotFlow {
            visibilityRevision
            visibilitySnapshot(visibilityGroups)
        }
            .collect {
                if (pruneSettledVisibilityRecords(visibilityGroups, groupOrder, latestTargets)) {
                    visibilityRevision++
                }
            }
    }
    val appearance =
        SettingsCardAppearance(
            shape = cardShape,
            colors = cardColors,
            elevation = cardElevation,
            border = cardBorder,
            material = resolvedCardMaterial,
        )
    localSharedTransitionScope {
        val cardContext =
            SettingsCardContext(
                sharedTransition = this,
                state = state,
                transition = transition,
                navigationTransition = navigationTransition,
                labels = labels,
                sliderCommitMode = sliderCommitMode,
                sourceStatuses = sourceStatuses,
                coroutineScope = coroutineScope,
                onError = onError,
            )
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            GroupedLazyColumn(
                modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
                state = lazyListState,
                contentPadding = contentPadding,
                spacing = 0.dp,
            ) {
                settingsLoadingCards(sourceStatuses, labels, appearance)
                groupsForRender.forEachIndexed { groupIndex, group ->
                    settingsGroupCard(group, groupIndex, cardSpacing, appearance, cardContext)
                }
            }
        }

        val editors = remember(state) { mutableStateMapOf<String, SettingsEditSession>() }
        state.activeItem?.let { editors[it.id] = state.editSession }
        LaunchedEffect(state, transition) {
            snapshotFlow {
                Triple(transition.currentState, transition.targetState, transition.isRunning)
            }
                .collect { (current, target, running) ->
                    if (!running && current == target) {
                        editors.keys.toList().filter { it != target }.forEach(editors::remove)
                    }
                }
        }
        editors.forEach { (id, session) ->
            val active = session.activeItem ?: return@forEach
            key(id) {
                SettingsEditorDialog(
                    active = active,
                    session = session,
                    editorShape = editorShape,
                    eligible = { state.activeItem?.id == id && activeEligible },
                    customEditor = customEditor,
                    context = cardContext,
                    onDismiss = { state.dismiss() },
                )
            }
        }
    }
}

/**
 * The status of every source of the page. A read or write failure is reported to [onError] once per
 * observed status transition.
 */
@Composable
private fun rememberSourceStatuses(
    sources: List<SettingsSource>,
    onError: (SettingsPresentationError) -> Unit,
): List<Pair<SettingsSource, SettingsPersistenceStatus>> {
    val currentOnError by rememberUpdatedState(onError)
    return sources.map { source ->
        key(source.id, source.persistence) {
            val status =
                source.persistence?.status?.collectAsState()?.value
                    ?: SettingsPersistenceStatus.Ready
            LaunchedEffect(status) {
                when (status) {
                    is SettingsPersistenceStatus.ReadFailed ->
                        currentOnError(SettingsPresentationError.ReadFailed(source, status.cause))
                    is SettingsPersistenceStatus.WriteFailed ->
                        currentOnError(SettingsPresentationError.WriteFailed(source, status.cause))
                    else -> Unit
                }
            }
            source to status
        }
    }
}

/** Records the loading placeholder card of every source that has not finished reading yet. */
private fun GroupedLazyListScope.settingsLoadingCards(
    sourceStatuses: List<Pair<SettingsSource, SettingsPersistenceStatus>>,
    labels: CardSettingsLabels,
    appearance: SettingsCardAppearance,
) {
    sourceStatuses.forEach { (source, status) ->
        if (status == SettingsPersistenceStatus.Loading)
            card(shape = { appearance.shape }) {
                item(key = "${source.id}:status-loading") {
                    Text(labels.loading.resolve(), Modifier.padding(20.dp))
                }
            }
    }
}

/** Records one rendered settings group as a card holding its category header and its rows. */
private fun GroupedLazyListScope.settingsGroupCard(
    group: SettingsGroupVisibilityRecord,
    groupIndex: Int,
    cardSpacing: Dp,
    appearance: SettingsCardAppearance,
    context: SettingsCardContext,
) {
    val root = group.root
    card(
        shape = { appearance.shape },
        colors = { appearance.colors },
        backdropMaterial = { appearance.material },
        elevation = { appearance.elevation },
        border = { appearance.border },
    ) {
        if (root is SettingsItem.Category) {
            header(
                key = root.id,
                modifier = { Modifier.settingsGroupHeader(group, groupIndex, cardSpacing) },
            ) {
                SettingsCategoryHeader(root)
            }
        }
        group.rowOrder
            .mapNotNull { group.rows[it] }
            .forEach { record ->
                settingsRowItem(record, group, groupIndex, cardSpacing, context)
            }
    }
}

/** The modifier that animates a category header into and out of the list. */
@Composable
private fun Modifier.settingsGroupHeader(
    group: SettingsGroupVisibilityRecord,
    groupIndex: Int,
    cardSpacing: Dp,
): Modifier {
    val root = group.root
    val reduceMotion = !SpatialMotionEnabled
    val groupTransition = rememberTransition(group.visibility)
    val headerFraction by
        groupTransition.animateFloat(
            transitionSpec = { MaterialTheme.motionScheme.fastEffectsSpec() },
            label = "settings-header-${root.id}",
        ) { visible ->
            if (visible) 1f else 0f
        }
    val gapPx =
        with(LocalDensity.current) { (if (groupIndex == 0) 0.dp else cardSpacing).roundToPx() }
    return blurTransition(
            { headerFraction },
            maxRadius = MaterialTheme.extended.enterExitBlurRadius,
        )
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            val layoutFraction =
                if (reduceMotion) (if (group.visibility.targetState) 1f else 0f)
                else headerFraction.coerceIn(0f, 1f)
            val gapHeight = (gapPx * layoutFraction).roundToInt()
            val height = ((gapPx + placeable.height) * layoutFraction).roundToInt()
            layout(placeable.width, height) { placeable.place(0, gapHeight) }
        }
}

/** The label and optional description a category header renders. */
@Composable
private fun SettingsCategoryHeader(root: SettingsItem) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            root.resources.label.resolve(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        root.resources.description?.resolve()?.let { description ->
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Records one visible settings row of [group] as a card item. */
private fun CardLazyGroup.settingsRowItem(
    record: SettingsVisibilityRecord,
    group: SettingsGroupVisibilityRecord,
    groupIndex: Int,
    cardSpacing: Dp,
    context: SettingsCardContext,
) {
    val entry = record.item
    val root = group.root
    item(
        key = entry.id,
        contentType = entry::class.simpleName,
        modifier = {
            if (
                groupIndex > 0 &&
                    root !is SettingsItem.Category &&
                    group.rowOrder.firstOrNull() == entry.id
            ) {
                Modifier.settingsRowGap(record, root, cardSpacing)
            } else Modifier
        },
    ) {
        with(context.sharedTransition) { SettingsRowEntry(record, group, context) }
    }
}

/** The top gap a category-less group needs between itself and the group above it. */
@Composable
private fun Modifier.settingsRowGap(
    record: SettingsVisibilityRecord,
    root: SettingsItem,
    cardSpacing: Dp,
): Modifier {
    val rowTransition = rememberTransition(record.visibility)
    val gap by
        rowTransition.animateDp(
            transitionSpec = {
                if (SpatialMotionEnabled)
                    MaterialTheme.motionScheme
                        .fastSpatialSpec<Dp>()
                        .withDefaultVisibilityThreshold(Dp.VisibilityThreshold)
                else snap()
            },
            label = "settings-gap-${root.id}",
        ) { visible ->
            if (visible) cardSpacing else 0.dp
        }
    return padding(top = gap.coerceAtLeast(0.dp))
}

/**
 * The content of one settings row. It animates with the visibility of its own record and, while the
 * shared transition hands the item over to the editor, keeps the row in place as a hidden copy.
 */
@Composable
private fun SharedTransitionScope.SettingsRowEntry(
    record: SettingsVisibilityRecord,
    group: SettingsGroupVisibilityRecord,
    context: SettingsCardContext,
) {
    val entry = record.item
    val motionScheme = MaterialTheme.motionScheme
    val visibleState = record.visibility
    val reduceMotion = !SpatialMotionEnabled
    AnimatedVisibility(
        visibleState = visibleState,
        enter =
            if (reduceMotion) EnterTransition.None
            else
                expandVertically(
                    animationSpec =
                        MaterialTheme.motionScheme
                            .fastSpatialSpec<IntSize>()
                            .withDefaultVisibilityThreshold(IntSize.VisibilityThreshold),
                    clip = false,
                ),
        exit =
            if (reduceMotion) ExitTransition.None
            else
                shrinkVertically(
                    animationSpec =
                        MaterialTheme.motionScheme
                            .fastSpatialSpec<IntSize>()
                            .withDefaultVisibilityThreshold(IntSize.VisibilityThreshold),
                    clip = false,
                ),
        modifier = Modifier.fillMaxWidth().testTag("settings-row:${entry.id}"),
    ) {
        CompositionLocalProvider(
            LocalSettingsRowAvailable provides
                {
                    group.visibility.targetState &&
                        visibleState.targetState &&
                        group.root.shouldShow &&
                        record.item.enabled &&
                        record.item.shouldShow &&
                        context.sourceStatuses.rendersItem(entry.id)
                },
        ) {
            BlurEnterExit(
                maxRadius = MaterialTheme.extended.enterExitBlurRadius,
                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
            ) {
                Box {
                    Box(Modifier.alpha(0f).clearAndSetSemantics {}) {
                        SettingsRow(
                            entry,
                            context.labels,
                            sliderCommitMode = context.sliderCommitMode,
                            onOpen = {},
                            interactive = false,
                        )
                    }
                    context.transition.AnimatedVisibility(
                        visible = { it != entry.id },
                        enter =
                            if (reduceMotion) fadeIn(motionScheme.fastEffectsSpec())
                            else EnterTransition.None,
                        exit =
                            if (reduceMotion) fadeOut(motionScheme.fastEffectsSpec())
                            else ExitTransition.None,
                    ) {
                        val rowSharedText =
                            if (!SpatialMotionEnabled) null
                            else SettingsSharedText("setting:${entry.id}", this)

                        Box(
                            Modifier.then(
                                    if (reduceMotion) Modifier
                                    else
                                        Modifier.sharedBounds(
                                            boundsTransform = { _, _ ->
                                                motionScheme.sharedBoundsSpec()
                                            },
                                            enter =
                                                fadeIn(
                                                    MaterialTheme.motionScheme.slowEffectsSpec(),
                                                ),
                                            exit =
                                                fadeOut(
                                                    MaterialTheme.motionScheme.slowEffectsSpec(),
                                                ),
                                            sharedContentState =
                                                rememberSharedContentState("setting:${entry.id}"),
                                            animatedVisibilityScope = this,
                                            renderInOverlayDuringTransition =
                                                context.state.activeItem?.id == entry.id,
                                        ),
                                )
                                .clip(LocalCardShape.current)
                                .then(
                                    if (visibleState.targetState) Modifier
                                    else Modifier.clearAndSetSemantics {},
                                ),
                        ) {
                            BlurEnterExit(
                                maxRadius = MaterialTheme.extended.enterExitBlurRadius,
                                animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                                fade = false,
                            ) {
                                SettingsRow(
                                    entry,
                                    context.labels,
                                    sliderCommitMode = context.sliderCommitMode,
                                    onOpen = {
                                        if (visibleState.targetState) {
                                            context.state.open(entry)
                                        }
                                    },
                                    onError = context.onError,
                                    interactive = visibleState.targetState,
                                    sharedText = rowSharedText,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The dialog that edits the active item. Its texts share their strings with the row the editor was
 * opened from.
 *
 * The overlay host keeps the content of an entry composed through its exit animation, and it
 * invokes the content lambda it last received rather than the newest one. [eligible] is therefore a
 * provider: the content reads the current value while the host composes it, so hiding an ancestor
 * disables the editor immediately. [session] is the opposite case: the page passes the session of
 * the composition that opened the editor, so the retiring draft stays readable while the dialog
 * finishes drawing.
 */
@Composable
private fun SharedTransitionScope.SettingsEditorDialog(
    active: SettingsItem.Item<*>,
    session: SettingsEditSession,
    editorShape: Shape,
    eligible: () -> Boolean,
    customEditor: CustomSettingsEditor?,
    context: SettingsCardContext,
    onDismiss: () -> Unit,
) {
    val state = context.state
    val labels = context.labels
    OverlayDialog(
        navigationTransition = context.navigationTransition,
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding().widthIn(min = 280.dp, max = 560.dp).fillMaxWidth(),
        visible = { state.activeItem?.id == active.id },
        sharedContentState = rememberSharedContentState("setting:${active.id}"),
        sharedClipShape = editorShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        val reduceMotion = !SpatialMotionEnabled
        val overlayVisibility = LocalOverlayAnimatedVisibilityScope.current
        val editorSharedText =
            if (overlayVisibility == null || reduceMotion) null
            else SettingsSharedText("setting:${active.id}", overlayVisibility)
        val save: () -> Unit = {
            if (eligible() && state.confirm()) onDismiss() else state.error?.let(context.onError)
        }
        BoxWithConstraints {
            OverlayAlertDialogContent(
                modifier = Modifier.heightIn(max = maxHeight * 0.9f).fillMaxWidth(),
                icon = null,
                title = {
                    Text(
                        active.resources.label.resolve(),
                        Modifier.settingsSharedLabel(editorSharedText),
                    )
                },
                text = {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        active.resources.description?.resolve()?.let {
                            Text(it, Modifier.settingsSharedDescription(editorSharedText))
                        }
                        SettingsItemEditor(
                            active,
                            session,
                            customEditor,
                            labels,
                            save,
                            interactive = eligible(),
                            onError = context.onError,
                            sharedText = editorSharedText,
                        )
                    }
                },
                buttons = {
                    OverlayAlertDialogButtons(
                        confirmButton = {
                            TextButton(
                                enabled =
                                    eligible() &&
                                        (active !is SettingsItem.Item.Custom<*> ||
                                            customEditor != null),
                                onClick = save,
                            ) {
                                Text(labels.save.resolve())
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = onDismiss) { Text(labels.cancel.resolve()) }
                        },
                    )
                },
                buttonContentColor = MaterialTheme.colorScheme.primary,
                iconContentColor = MaterialTheme.colorScheme.secondary,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
public fun SettingsValidation.Issue.localizedMessage(): String =
    when (this) {
        is SettingsValidation.Issue.TooShort ->
            stringResource(Res.string.m3x_settings_too_short, minimum)
        is SettingsValidation.Issue.TooLong ->
            stringResource(Res.string.m3x_settings_too_long, maximum)
        SettingsValidation.Issue.NotFinite -> stringResource(Res.string.m3x_settings_not_finite)
        SettingsValidation.Issue.InvalidBounds ->
            stringResource(Res.string.m3x_settings_invalid_bounds)
        is SettingsValidation.Issue.OutOfBounds ->
            stringResource(Res.string.m3x_settings_out_of_bounds, minimum, maximum)
        is SettingsValidation.Issue.InvalidStep ->
            stringResource(Res.string.m3x_settings_invalid_step, step)
        SettingsValidation.Issue.RangeOrder -> stringResource(Res.string.m3x_settings_range_order)
    }

@Composable
public fun SettingsRow(
    item: SettingsItem.Item<*>,
    labels: CardSettingsLabels,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    sliderCommitMode: SliderCommitMode = SliderCommitMode.OnRelease,
    onError: (SettingsPresentationError) -> Unit = {},
    sharedText: SettingsSharedText? = null,
) {
    val currentInteractive = rememberUpdatedState(interactive)
    val currentItem = rememberUpdatedState(item)
    val currentAvailability = rememberUpdatedState(LocalSettingsRowAvailable.current)
    val eligibilityState =
        remember(item.id) {
            derivedStateOf {
                currentInteractive.value &&
                    currentItem.value.enabled &&
                    currentItem.value.shouldShow &&
                    currentItem.value.resetMode == SettingsResetMode.Menu &&
                    currentAvailability.value()
            }
        }
    val eligible by eligibilityState
    var menuExpanded by remember(item.id) { mutableStateOf(false) }
    val restoreLabel = labels.restoreDefault.resolve()
    var menuAnchor by remember(item.id) { mutableStateOf<Offset?>(null) }
    val openMenu = { position: Offset? ->
        if (eligible && item.defaultValue != null) {
            menuAnchor = position
            menuExpanded = true
        }
    }
    val currentOpenMenu = rememberUpdatedState(openMenu)
    LaunchedEffect(item.id, eligible) {
        if (!eligible) menuExpanded = false
    }
    Box(Modifier.fillMaxWidth().restoreDefaultGestures(eligible) { currentOpenMenu.value(it) }) {
        SettingsRowContent(
            item,
            labels,
            onOpen,
            modifier
                .semantics {
                    if (eligible) {
                        onLongClick(label = restoreLabel) {
                            currentOpenMenu.value(null)
                            true
                        }
                    }
                }
                .fillMaxWidth(),
            interactive,
            sliderCommitMode,
            onError,
            sharedText,
        )
        if (item.resetMode == SettingsResetMode.Menu) {
            SettingsRestoreDefaultMenu(
                label = restoreLabel,
                expanded = { menuExpanded && eligibilityState.value },
                eligible = { eligibilityState.value },
                anchor = { menuAnchor },
                onDismiss = { menuExpanded = false },
                onRestore = {
                    SettingsCommit.tryRestoreDefault(item).reportFailure(item, onError)
                },
            )
        }
    }
}

/** The menu that restores the default value of a row, anchored at the pointer that opened it. */
@Composable
private fun SettingsRestoreDefaultMenu(
    label: String,
    expanded: () -> Boolean,
    eligible: () -> Boolean,
    anchor: () -> Offset?,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
) {
    val currentAnchor = rememberUpdatedState(anchor)
    val defaultPositionProvider = rememberOverlayMenuPositionProvider()
    val pointerOnlyPositionProvider = rememberOverlayMenuPositionProvider(anchorSpacing = 0.dp)
    val pointerPositionProvider =
        remember(defaultPositionProvider, pointerOnlyPositionProvider) {
            pointerMenuPositionProvider(
                defaultPositionProvider,
                pointerOnlyPositionProvider,
            ) {
                currentAnchor.value()
            }
        }
    OverlayMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        popupPositionProvider = pointerPositionProvider,
    ) {
        OverlayMenuItem(
            text = { Text(label) },
            enabled = eligible(),
            onClick = {
                onDismiss()
                if (eligible()) onRestore()
            },
        )
    }
}

/**
 * Anchors the menu at [pointer] inside the row bounds. Without a pointer, the row bounds anchor the
 * menu through [fallback].
 *
 * [pointer] is a provider because the row records the pointer in the gesture that opens the menu
 * while the menu is placed afterward, so the position must be read at placement time rather than
 * captured when the menu is composed.
 */
private fun pointerMenuPositionProvider(
    fallback: PopupPositionProvider,
    pointerOnly: PopupPositionProvider,
    pointer: () -> Offset?,
): PopupPositionProvider =
    object : PopupPositionProvider {
        override fun calculatePosition(
            anchorBounds: IntRect,
            windowSize: IntSize,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize,
        ): IntOffset {
            val anchor =
                pointer()
                    ?: return fallback.calculatePosition(
                        anchorBounds,
                        windowSize,
                        layoutDirection,
                        popupContentSize,
                    )
            val x = anchorBounds.left + anchor.x.roundToInt()
            val y = anchorBounds.top + anchor.y.roundToInt()
            return pointerOnly.calculatePosition(
                IntRect(x, y, x, y),
                windowSize,
                layoutDirection,
                popupContentSize,
            )
        }
    }

private fun Modifier.restoreDefaultGestures(
    enabled: Boolean,
    onRestore: (Offset) -> Unit,
): Modifier =
    if (!enabled) this
    else
        pointerInput(enabled) {
                awaitEachGesture {
                    val pass = PointerEventPass.Initial
                    val down = awaitFirstDown(pass = pass)
                    if (down.type == PointerType.Touch || down.type == PointerType.Stylus) {
                        try {
                            withTimeout(viewConfiguration.longPressTimeoutMillis) {
                                waitForUpOrCancellation(pass = pass)
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            onRestore(down.position)
                            waitForUpOrCancellation(pass = pass)?.consume()
                        }
                    }
                }
            }
            .pointerInput(enabled) {
                awaitPointerEventScope {
                    while (true) {
                        val press = awaitPointerEvent(PointerEventPass.Initial)
                        if (
                            press.type == PointerEventType.Press &&
                                press.changes.any { it.type == PointerType.Mouse } &&
                                press.buttons.isSecondaryPressed
                        ) {
                            val pointerPosition = press.changes.first().position
                            press.changes.forEach { it.consume() }
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                event.changes.forEach { it.consume() }
                                if (
                                    event.type == PointerEventType.Release ||
                                        event.changes.none { it.pressed }
                                )
                                    break
                            }
                            onRestore(pointerPosition)
                        }
                    }
                }
            }

@Composable
private fun SettingsRowContent(
    item: SettingsItem.Item<*>,
    labels: CardSettingsLabels,
    onOpen: () -> Unit,
    modifier: Modifier,
    interactive: Boolean,
    sliderCommitMode: SliderCommitMode,
    onError: (SettingsPresentationError) -> Unit,
    sharedText: SettingsSharedText?,
) {
    val finite = (item as? SettingsItem.Item.SingleNumber<*>)?.type as? NumberType.Finite<*>
    val mapping = remember(finite) { finite?.sliderMappingOrNull() }
    if (finite != null && (mapping != null || finite.type == RangeBasedNumberInfo.Type.Slider)) {
        SliderSettingsRow(
            item,
            finite,
            interactive,
            onOpen,
            modifier,
            onError,
            sliderCommitMode,
            sharedText,
        )
        return
    }
    if (item is SettingsItem.Item.NumberRange<*> && item.usesSliderBounds()) {
        RangeSliderSettingsRow(
            item,
            labels,
            interactive,
            onOpen,
            modifier,
            onError,
            sliderCommitMode,
            sharedText,
        )
        return
    }
    SettingsListItem(item, labels, onOpen, modifier, interactive, onError, sharedText)
}

/** Whether either bound of the range renders as a slider instead of a plain row. */
@Composable
private fun SettingsItem.Item.NumberRange<*>.usesSliderBounds(): Boolean {
    val startType = startType as? NumberType.Finite<*>
    val endType = endType as? NumberType.Finite<*>
    val startSlider =
        remember(startType) { startType?.sliderMappingOrNull() != null } ||
            startType?.type == RangeBasedNumberInfo.Type.Slider
    val endSlider =
        remember(endType) { endType?.sliderMappingOrNull() != null } ||
            endType?.type == RangeBasedNumberInfo.Type.Slider
    return startSlider || endSlider
}

/** The plain list row of an item that neither renders nor edits as a slider. */
@Composable
private fun SettingsListItem(
    item: SettingsItem.Item<*>,
    labels: CardSettingsLabels,
    onOpen: () -> Unit,
    modifier: Modifier,
    interactive: Boolean,
    onError: (SettingsPresentationError) -> Unit,
    sharedText: SettingsSharedText?,
) {
    val title = item.resources.label.resolve()
    val description = item.resources.description?.resolve()
    val icon = item.resources.icon?.resolve() as? Painter
    val summary =
        when (item) {
            is SettingsItem.Item.Switch -> description
            is SettingsItem.Item.Selector<*> -> item.value.resources.label.resolve()
            is SettingsItem.Item.MultiSelector<*> -> labels.selectedCount(item.value.size).resolve()
            else -> description ?: item.value.toString()
        }
    val enabled = item.enabled && interactive
    val listItemModifier = modifier.semantics {
        if (item is SettingsItem.Item.Switch) role = Role.Switch
        stateDescription = summary.orEmpty()
    }
    val supportingContent: (@Composable () -> Unit)? =
        summary
            ?.takeIf { it.isNotBlank() }
            ?.let { summaryText ->
                {
                    val summaryModifier =
                        when (item) {
                            is SettingsItem.Item.Selector<*> ->
                                Modifier.settingsSharedValue(sharedText, item.value.id)
                            !is SettingsItem.Item.MultiSelector<*> if description != null ->
                                Modifier.settingsSharedDescription(sharedText)

                            else -> Modifier
                        }
                    Text(summaryText, summaryModifier)
                }
            }
    val leadingContent: (@Composable () -> Unit)? = icon?.let {
        { Icon(it, contentDescription = null) }
    }
    val trailingContent: (@Composable () -> Unit)? =
        if (item is SettingsItem.Item.Switch) {
            {
                Switch(
                    item.value,
                    onCheckedChange = null,
                    enabled = item.enabled && interactive,
                )
            }
        } else null
    if (item is SettingsItem.Item.Switch) {
        ListItem(
            checked = item.value,
            onCheckedChange = {
                SettingsCommit.tryCommit(item, it).reportFailure(item, onError)
            },
            enabled = enabled,
            supportingContent = supportingContent,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            modifier = listItemModifier,
            colors = ListItemColorsTransparent,
            content = { Text(title, Modifier.settingsSharedLabel(sharedText)) },
        )
    } else {
        ListItem(
            onClick = onOpen,
            enabled = enabled,
            supportingContent = supportingContent,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            modifier = listItemModifier,
            colors = ListItemColorsTransparent,
            content = { Text(title, Modifier.settingsSharedLabel(sharedText)) },
        )
    }
}

@Composable
public fun SettingsItemEditor(
    item: SettingsItem.Item<*>,
    state: SettingsEditSession,
    custom: CustomSettingsEditor?,
    labels: CardSettingsLabels,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    onError: (SettingsPresentationError) -> Unit = {},
    sharedText: SettingsSharedText? = null,
) {
    SettingsItemEditor(
        item,
        state,
        custom,
        labels,
        onSave,
        modifier,
        interactive = true,
        onError = onError,
        sharedText = sharedText,
    )
}

@Composable
private fun SettingsItemEditor(
    item: SettingsItem.Item<*>,
    state: SettingsEditSession,
    custom: CustomSettingsEditor?,
    labels: CardSettingsLabels,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean,
    onError: (SettingsPresentationError) -> Unit = {},
    sharedText: SettingsSharedText? = null,
) {
    val currentOnError by rememberUpdatedState(onError)
    val currentInteractive = rememberUpdatedState(interactive && item.enabled)
    val focusManager = LocalFocusManager.current
    LaunchedEffect(interactive) {
        if (!interactive) focusManager.clearFocus(force = true)
    }
    LaunchedEffect(item, custom) {
        if (item is SettingsItem.Item.Custom<*> && custom == null) {
            currentOnError(SettingsPresentationError.MissingCustomEditor(item))
        }
    }
    Column(modifier.settingsEditorInput(interactive)) {
        val enabled = item.enabled && interactive
        when (item) {
            is SettingsItem.Item.Selector<*> -> SelectorEditor(item, state, enabled, sharedText)

            is SettingsItem.Item.MultiSelector<*> -> MultiSelectorEditor(item, state, enabled)

            is SettingsItem.Item.Text -> TextEditor(item, state, enabled, onSave)

            is SettingsItem.Item.SingleNumber<*> ->
                NumberEditor(item, state, labels, enabled = enabled, onError = onError)

            is SettingsItem.Item.NumberRange<*> ->
                RangeEditor(item, state, labels, enabled = enabled, onError = onError)

            is SettingsItem.Item.Custom<*> ->
                if (custom != null)
                    custom(item, state.draft) { draft ->
                        if (currentInteractive.value) state.changeDraft(draft)
                    }
                else Unit

            is SettingsItem.Item.Switch -> Unit
        }
    }
}

/** Blocks every pointer event while the editor is not interactive. */
private fun Modifier.settingsEditorInput(interactive: Boolean): Modifier =
    then(if (interactive) Modifier else Modifier.clearAndSetSemantics {}).pointerInput(
        interactive,
    ) {
        if (!interactive)
            awaitPointerEventScope {
                while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach {
                    it.consume()
                }
            }
    }

/** The radio group that selects one option of [item]. */
@Composable
private fun SelectorEditor(
    item: SettingsItem.Item.Selector<*>,
    state: SettingsEditSession,
    enabled: Boolean,
    sharedText: SettingsSharedText?,
) {
    @Suppress("UNCHECKED_CAST") val options = item.options as List<SettingsValue<Any?>>
    Column(Modifier.selectableGroup()) {
        options.forEach { option ->
            val current = state.draft as? SettingsValue<*>
            val selected = current?.id == option.id
            ListItem(
                selected = selected,
                onClick = { state.changeDraft(if (selected) current else option) },
                enabled = enabled,
                supportingContent = option.resources.description?.resolve()?.let { { Text(it) } },
                trailingContent = {
                    RadioButton(selected, onClick = null, enabled = enabled)
                },
                colors = ListItemColorsTransparent,
                content = {
                    Text(
                        option.resources.label.resolve(),
                        Modifier.settingsSharedValue(sharedText, option.id),
                    )
                },
            )
        }
    }
}

/** The checkbox list that toggles the options of [item] in the draft. */
@Composable
private fun MultiSelectorEditor(
    item: SettingsItem.Item.MultiSelector<*>,
    state: SettingsEditSession,
    enabled: Boolean,
) {
    @Suppress("UNCHECKED_CAST") val selected = state.draft as Set<SettingsValue<Any?>>
    @Suppress("UNCHECKED_CAST")
    (item.options as List<SettingsValue<Any?>>).forEach { option ->
        val checked = SettingsValues.containsId(selected, option.id)
        ListItem(
            checked = checked,
            onCheckedChange = { checkedNow ->
                state.changeDraft(
                    if (checkedNow) SettingsValues.withOption(selected, option)
                    else SettingsValues.withoutOption(selected, option.id).toSet(),
                )
            },
            enabled = enabled,
            trailingContent = { Checkbox(checked, onCheckedChange = null, enabled = enabled) },
            colors = ListItemColorsTransparent,
            content = { Text(option.resources.label.resolve()) },
        )
    }
}

/** The single-line or multiline text field that edits the draft of [item]. */
@Composable
private fun TextEditor(
    item: SettingsItem.Item.Text,
    state: SettingsEditSession,
    enabled: Boolean,
    onSave: () -> Unit,
) {
    val draft = state.draft as String
    val textState = key(state, item.id) { rememberTextFieldState(draft) }
    val focus = remember(state, item.id) { FocusRequester() }
    LaunchedEffect(state, textState, draft) {
        if (textState.text.toString() != draft) textState.setTextAndPlaceCursorAtEnd(draft)
    }
    LaunchedEffect(state, textState) {
        snapshotFlow { textState.text.toString() }.drop(1).collect(state::changeDraft)
    }
    LaunchedEffect(state, item.id) { focus.requestFocus() }
    OutlinedTextField(
        state = textState,
        label = { Text(item.resources.label.resolve()) },
        lineLimits =
            if (item.multiline) TextFieldLineLimits.MultiLine() else TextFieldLineLimits.SingleLine,
        keyboardOptions =
            KeyboardOptions(imeAction = if (item.multiline) ImeAction.Default else ImeAction.Done),
        onKeyboardAction = {
            state.changeDraft(textState.text.toString())
            onSave()
        },
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
        enabled = enabled,
    )
}

@Composable
public fun NumberEditor(
    item: SettingsItem.Item.SingleNumber<*>,
    state: SettingsEditSession,
    labels: CardSettingsLabels,
    modifier: Modifier = Modifier,
    onError: (SettingsPresentationError) -> Unit = {},
) {
    NumberEditor(item, state, labels, modifier, enabled = true, onError = onError)
}

@Composable
private fun NumberEditor(
    item: SettingsItem.Item.SingleNumber<*>,
    state: SettingsEditSession,
    labels: CardSettingsLabels,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onError: (SettingsPresentationError) -> Unit = {},
) {
    val current = state.draft as Number
    val finite = item.type as? NumberType.Finite<*>
    val textState = key(state, item.id) { rememberTextFieldState(current.toString()) }
    val currentOnError by rememberUpdatedState(onError)
    LaunchedEffect(state, item, textState) {
        snapshotFlow { textState.text.toString() }
            .drop(1)
            .collect { input ->
                val parsed = SettingsNumbers.parse(input, item.type.kind)
                if (parsed == null) {
                    state.invalidInput()
                    currentOnError(
                        SettingsPresentationError.EditFailed(item, SettingsEditError.InvalidInput),
                    )
                } else state.changeDraft(parsed)
            }
    }
    val mapping = remember(finite) { finite?.sliderMappingOrNull() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (finite != null && finite.type != RangeBasedNumberInfo.Type.Slider && mapping == null) {
            NumberStepper(
                current,
                finite,
                enabled = enabled,
                onChange = {
                    textState.setTextAndPlaceCursorAtEnd(it.toString())
                    state.changeDraft(it)
                },
            )
        }
        if (finite?.allowInput != false)
            OutlinedTextField(
                state = textState,
                label = { Text(labels.value.resolve()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
            )
    }
}

@Composable
public fun RangeEditor(
    item: SettingsItem.Item.NumberRange<*>,
    state: SettingsEditSession,
    labels: CardSettingsLabels,
    modifier: Modifier = Modifier,
    onError: (SettingsPresentationError) -> Unit = {},
) {
    RangeEditor(item, state, labels, modifier, enabled = true, onError = onError)
}

@Composable
private fun RangeEditor(
    item: SettingsItem.Item.NumberRange<*>,
    state: SettingsEditSession,
    labels: CardSettingsLabels,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onError: (SettingsPresentationError) -> Unit = {},
) {
    val pair = state.draft as Pair<*, *>
    val start = pair.first as Number
    val end = pair.second as Number
    val finite = item.startType as? NumberType.Finite<*>
    val endFinite = item.endType as? NumberType.Finite<*>
    val startText = key(state, item.id, "start") { rememberTextFieldState(start.toString()) }
    val endText = key(state, item.id, "end") { rememberTextFieldState(end.toString()) }
    val currentOnError by rememberUpdatedState(onError)
    LaunchedEffect(state, item, startText, endText) {
        snapshotFlow { startText.text.toString() to endText.text.toString() }
            .drop(1)
            .collect { (startInput, endInput) ->
                updateRangeDraft(startInput, endInput, item, state, currentOnError)
            }
    }
    val startMapping = remember(finite) { finite?.sliderMappingOrNull() }
    val endMapping = remember(endFinite) { endFinite?.sliderMappingOrNull() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (
            finite != null &&
                finite.type != RangeBasedNumberInfo.Type.Slider &&
                startMapping == null
        ) {
            NumberStepper(
                start,
                finite,
                onChange = {
                    startText.setTextAndPlaceCursorAtEnd(it.toString())
                    updateRangeDraft(
                        startText.text.toString(),
                        endText.text.toString(),
                        item,
                        state,
                        onError,
                    )
                },
            )
        }
        if (
            endFinite != null &&
                endFinite.type != RangeBasedNumberInfo.Type.Slider &&
                endMapping == null
        ) {
            NumberStepper(
                end,
                endFinite,
                onChange = {
                    endText.setTextAndPlaceCursorAtEnd(it.toString())
                    updateRangeDraft(
                        startText.text.toString(),
                        endText.text.toString(),
                        item,
                        state,
                        onError,
                    )
                },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (finite?.allowInput != false) {
                OutlinedTextField(
                    state = startText,
                    label = { Text(labels.from.resolve()) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                )
            }
            if (endFinite?.allowInput != false) {
                OutlinedTextField(
                    state = endText,
                    label = { Text(labels.to.resolve()) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                )
            }
        }
    }
}

@Composable
public fun NumberStepper(
    value: Number,
    finite: NumberType.Finite<*>,
    onChange: (Number) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val previous = SettingsNumbers.stepNumber(value, finite, increase = false)
    val next = SettingsNumbers.stepNumber(value, finite, increase = true)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        OutlinedButton(
            enabled = enabled && previous != value,
            onClick = { onChange(previous) },
        ) {
            Text("−")
        }
        Text(value.toString(), modifier = Modifier.padding(12.dp))
        OutlinedButton(
            enabled = enabled && next != value,
            onClick = { onChange(next) },
        ) {
            Text("+")
        }
    }
}

private fun updateRangeDraft(
    startText: String,
    endText: String,
    item: SettingsItem.Item.NumberRange<*>,
    state: SettingsEditSession,
    onError: (SettingsPresentationError) -> Unit,
) {
    val parsedStart = SettingsNumbers.parse(startText, item.startType.kind)
    val parsedEnd = SettingsNumbers.parse(endText, item.endType.kind)
    if (parsedStart == null || parsedEnd == null) {
        state.invalidInput()
        onError(SettingsPresentationError.EditFailed(item, SettingsEditError.InvalidInput))
    } else state.changeDraft(parsedStart to parsedEnd)
}
