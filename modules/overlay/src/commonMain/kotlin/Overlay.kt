package top.ltfan.m3x.overlay

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.createChildTransition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.first
import top.ltfan.m3x.compose.animation.LocalBlurAnimationEnabled
import top.ltfan.m3x.compose.ui.accessibility.LocalAccessibilityPreferences
import top.ltfan.m3x.compose.ui.accessibility.LocalAccessibilitySettings
import top.ltfan.m3x.compose.ui.visual.LocalBlurEnabled

/**
 * A single overlay hosted by an [OverlayHostState]; [content] is read on every composition so the
 * host always draws the latest lambda.
 */
public class OverlayHandle {
    /** Content drawn by the host for this entry. */
    internal var content: @Composable () -> Unit by mutableStateOf({})
    internal var bindAnimation: @Composable () -> Unit = {}
    internal var immediateContent: Boolean = false
    internal var scrimAnimationSpec: FiniteAnimationSpec<Float> = snap()

    /** Forwards caller design and policy values while the host keeps its rendering scopes. */
    internal var provideCallerValues: @Composable (@Composable () -> Unit) -> Unit by
        mutableStateOf({ it() })

    /** Whether this entry blocks the content under it. */
    internal var isModal: Boolean by mutableStateOf(false)

    /**
     * Position of this entry in the stack of the host, newest in front.
     *
     * The host draws, hit-tests and routes back gestures in this order, so an overlay that opens
     * later is above the overlays that were already open. Entries with equal order keep the order
     * they were registered in.
     */
    internal var frontOrder: Int by mutableStateOf(0)

    /**
     * The owner's visibility provider, read directly in the host composition before page content.
     * The host stores its result in [visibleIntent] so entry and page endpoints observe the same
     * semantic change in one frame. Replacing the provider updates the host's snapshot
     * subscriptions.
     */
    internal var visible: () -> Boolean by mutableStateOf({ true })

    /**
     * How the entry takes pointer input, or `null` for an entry that lets the pointer input pass
     * through, such as a tooltip or a snackbar.
     *
     * The host composes one interception layer per entry together with the content of the entry.
     * The layer therefore appears and disappears together with the content, and the layer of an
     * entry covers the entries under it and the host content.
     */
    internal var interaction: OverlayInteraction? = null

    /**
     * Whether the owner of the entry has left the composition. The host plays the exit of the entry
     * and removes it once the exit and any shared transition have finished.
     */
    internal var detached: Boolean by mutableStateOf(false)
    internal var detachedWhileVisible: Boolean = false

    /**
     * Whether the owner wants the entry shown, written by the host in its own composition.
     *
     * Input routing follows this intent. The active timeline handles gesture previews separately
     * and returns to the latest owner target on cancellation.
     */
    internal var visibleIntent: Boolean by mutableStateOf(true)

    /**
     * The stable animation root for an entry without a caller-owned [navigationTransition]. The
     * timeline seeks it during Back previews and animates toward [visibleIntent] otherwise.
     * Reopening continues from its current animation values.
     */
    internal val visibleState: SeekableTransitionState<Boolean> = SeekableTransitionState(false)
    internal var visibilityTransition: OverlayVisibilityTransition? by mutableStateOf(null)
    internal var navigationTransition: OverlayNavigationTransition? = null

    /**
     * Prepares component-owned state before an entry whose owner disappeared starts to leave.
     *
     * The host keeps the entry visible until this callback completes. This lets a component move
     * its state to the same closed value used by all of its other dismissal paths before the entry
     * and a page-side shared endpoint change visibility together.
     */
    internal var prepareForOwnerExit: (suspend () -> Unit)? = null

    /**
     * How the entry takes back gestures, or `null` for an entry that leaves back to the page, such
     * as a non-focusable tooltip or a snackbar.
     */
    internal var backHandler: OverlayBackHandler? = null
    internal var backProgress: Float? by mutableStateOf(null)
    internal var backPhase: OverlayBackPhase by mutableStateOf(OverlayBackPhase.Idle)
    internal var backSession: Int by mutableStateOf(0)

    internal var sharedContentKey: Any? = null
    internal var navigationContentKey: Any? = null

    /**
     * Plays the exit animation of the entry. The host runs it after the entry is no longer visible,
     * before removing the entry.
     */
    internal var onExit: (suspend () -> Unit)? = null

    /**
     * The enter and exit transitions of the entry's [AnimatedVisibilityScope]. They define how the
     * entry animates when it appears and disappears.
     */
    internal var enterTransition: EnterTransition = EnterTransition.None
    internal var exitTransition: ExitTransition = ExitTransition.None

    /** Removes this entry from its host. */
    internal var remove: () -> Unit = {}
}

/**
 * Ordered stack of overlays drawn above the host content, in one window.
 *
 * Entries follow their opening order. An entry moves to the front when it becomes visible. It then
 * draws and takes pointer input above entries that were already open, regardless of registration
 * order.
 */
@Stable
public class OverlayHostState {
    internal val entries: SnapshotStateList<OverlayHandle> = mutableStateListOf()

    private var frontCounter = 0

    /** Returns the retained visibility binding for the last registered matching shared key. */
    public fun transitionFor(key: Any?): OverlayVisibilityTransition? =
        entries.lastOrNull { it.sharedContentKey == key }?.visibilityTransition

    /** Whether at least one live entry blocks the content under it. */
    public val isModal: Boolean
        get() = entries.any { it.isModal && it.blocksContentUnder() }

    internal fun add(entry: OverlayHandle) {
        if (!entries.contains(entry)) {
            entry.frontOrder = ++frontCounter
            entries.add(entry)
        }
    }

    internal fun remove(entry: OverlayHandle) {
        entries.remove(entry)
    }

    /** Moves [entry] to the front of the stack, where it draws and takes input above the rest. */
    public fun bringToFront(entry: OverlayHandle) {
        entry.frontOrder = ++frontCounter
    }
}

/** The nearest enclosing [OverlayHostState], if any. */
public val LocalOverlayHost: ProvidableCompositionLocal<OverlayHostState?> =
    staticCompositionLocalOf {
        null
    }

/**
 * The [SharedTransitionScope] provided for this composition. It may come from [OverlayHost] or
 * [ProvideLocalSharedTransitionScope]. It is `null` when no scope has been provided.
 */
public val LocalSharedTransitionScope: ProvidableCompositionLocal<SharedTransitionScope?> =
    staticCompositionLocalOf {
        null
    }

/**
 * Runs [content] with the provided [SharedTransitionScope].
 *
 * @throws IllegalStateException when no shared transition scope was provided
 */
@Composable
public inline fun <R> localSharedTransitionScope(
    content: @Composable SharedTransitionScope.() -> R,
): R =
    with(
        checkNotNull(LocalSharedTransitionScope.current) {
            "No SharedTransitionScope was provided"
        },
    ) {
        content()
    }

/**
 * Provides this [SharedTransitionScope] to [content]. The provider can be used independently of an
 * overlay host.
 */
@Composable
public inline fun SharedTransitionScope.ProvideLocalSharedTransitionScope(
    crossinline content: @Composable SharedTransitionScope.() -> Unit,
) {
    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
        content()
    }
}

/**
 * The [AnimatedVisibilityScope] of the enclosing overlay entry, which the entry content uses for
 * its shared bounds and shared elements.
 */
public val LocalOverlayAnimatedVisibilityScope:
    ProvidableCompositionLocal<AnimatedVisibilityScope?> =
    staticCompositionLocalOf {
        null
    }

/**
 * Hosts overlay content above [content] inside the same composition and window, in the order the
 * overlays are opened.
 *
 * An entry that becomes visible moves to the front of the stack of the host: it draws above the
 * entries that were already open, takes pointer input before them, and hears a back gesture before
 * them. Entries that are registered in the same frame keep the order they are registered in.
 *
 * While a modal entry is present, [content] and every entry in front of it have their semantics
 * cleared and do not receive pointer events, so the overlay is the only reachable content.
 *
 * The host owns the single [SharedTransitionScope] of its content and its entries, and gives every
 * entry an [AnimatedVisibilityScope], so page content and overlays can share elements with each
 * other.
 *
 * To include a captured page in the automatic backdrop stack, provide its handle with
 * `BackdropHost` outside this host. Apply `captureBackdrop` only to the page source content inside
 * [content], with glass surfaces outside that capture node. This host then captures and links its
 * overlay entries in their drawing order, regardless of how many entries are open. Without a page
 * handle, the internal stack remains but no empty-source handle is provided to built-in surfaces;
 * they use their tint fallback.
 *
 * @param state the state that tracks entries hosted above the page
 * @param modifier the [Modifier] to be applied to the root layout
 * @param content the page content shown below hosted entries
 */
@Composable
public fun OverlayHost(
    state: OverlayHostState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    SharedTransitionScope { sharedModifier ->
        CompositionLocalProvider(
            LocalOverlayHost provides state,
            LocalSharedTransitionScope provides this,
        ) {
            Box(modifier = modifier.then(sharedModifier)) {
                val entries = state.entries.toList()
                val topmostModalOrder =
                    entries
                        .filter { it.isModal && it.blocksContentUnder() }
                        .maxOfOrNull { it.frontOrder }
                val modalActive = topmostModalOrder != null
                // Each entry samples the page and the entries below it. It also records its own
                // layer for surfaces above it. A sheet can therefore sample a snackbar behind it.
                // The handles are built in the order the entries are drawn, so the handle of an
                // entry under another one exists before the layer above it is built.
                val decoration = LocalOverlayHostDecoration.current
                val decorations =
                    decoration.layers(
                        entries.map { OverlayLayerInfo(it, it.frontOrder, it.isModal) },
                    )
                // Entries are composed and measured before the page content. A shared animation
                // reads the leaving element's bounds before the arriving element writes its bounds.
                // During dismissal, the leaving element lives in an overlay entry. The entries
                // still draw above the content through their z index, which follows the order the
                // overlays were opened in, so an overlay that is opened later draws above the ones
                // that were already open.
                entries.forEach { entry ->
                    key(entry) {
                        val layerDecoration = decorations[entry] ?: OverlayLayerDecoration.None
                        val capture = layerDecoration.modifier
                        val inputActive = entry.acceptsInput
                        val blocked =
                            !inputActive ||
                                (topmostModalOrder != null && entry.frontOrder < topmostModalOrder)
                        // Retained exit drawing stays at its stack order while its layout moves
                        // below the page's hit plane, including the panel's own controls.
                        val exitLayer = rememberGraphicsLayer()
                        val order = 1f + entry.frontOrder
                        val focusManager = LocalFocusManager.current
                        var ownsFocus by remember(entry) { mutableStateOf(false) }
                        LaunchedEffect(blocked) {
                            if (blocked && ownsFocus) focusManager.clearFocus(force = true)
                        }
                        Box(
                            Modifier.zIndex(if (inputActive) order else -1f)
                                .drawWithContent {
                                    if (entry.acceptsInput) drawContent()
                                    else exitLayer.record { this@drawWithContent.drawContent() }
                                }
                                .onFocusChanged { ownsFocus = it.hasFocus }
                                .focusProperties {
                                    onEnter = { if (blocked) cancelFocusChange() }
                                }
                                .then(capture)
                                .overlayBlocked(blocked),
                        ) {
                            CompositionLocalProvider(LocalOverlayInputActive provides !blocked) {
                                layerDecoration.Content {
                                    entry.provideCallerValues { OverlayHandleContent(entry, state) }
                                }
                            }
                        }
                        Box(
                            Modifier.matchParentSize().zIndex(order).drawWithContent {
                                if (!entry.acceptsInput) drawLayer(exitLayer)
                            },
                        )
                    }
                }
                // Claim empty page space without consuming gestures so retired layouts cannot
                // receive hits through gaps between the page's interactive children.
                Box(
                    Modifier.matchParentSize().pointerInput(Unit) {
                        awaitPointerEventScope { while (true) awaitPointerEvent() }
                    },
                )
                Box(Modifier.overlayBlocked(modalActive)) {
                    content()
                }
                // One handler routes back gestures for the whole host. Overlays receive them before
                // the page. Only the frontmost visible entry that accepts back gestures receives
                // the event. Hidden entries stay out of back dispatch.
                OverlayHostBackHandler(
                    entries.filter { it.takesBackGestures }.maxByOrNull { it.frontOrder },
                )
            }
        }
    }
}

/**
 * Makes this subtree unreachable for accessibility services while [blocked] is true, so they only
 * see the overlay above it. Pointer input is taken by the interception layer of the overlay itself.
 */
@Composable
private fun Modifier.overlayBlocked(blocked: Boolean): Modifier =
    if (blocked) {
        this.clearAndSetSemantics {}
    } else {
        this
    }

/**
 * Draws a single entry and removes it once its exit animation has finished and no shared element
 * transition is still running, so a shared element keeps a live exit target until it has reached
 * its destination.
 *
 * The visibility of the entry is read from its owner here, in the composition of the host, and
 * stored in [OverlayHandle.visibleIntent] before the entry is composed. The active root observes
 * semantic changes independently of gesture previews.
 *
 * Removal is coordinated outside the visibility content so it can also wait for shared animations.
 */
@Composable
private fun OverlayHandleContent(entry: OverlayHandle, state: OverlayHostState) =
    localSharedTransitionScope {
        val sharedTransitionScope = this
        if (entry.navigationTransition?.ownerDisposed == true) {
            // The caller's disposed root can no longer drive or finish this entry's animations.
            DisposableEffect(entry) {
                entry.resolveBackGesture()
                state.remove(entry)
                onDispose {}
            }
            return@localSharedTransitionScope
        }
        entry.bindAnimation()
        var ownerExitPrepared by remember(entry) { mutableStateOf(false) }
        val prepareForOwnerExit = entry.prepareForOwnerExit
        val visible =
            when {
                !entry.detached -> entry.visible()
                // Preparation may only prolong an entry that had not started leaving. If the
                // component already drove its target to false, making it visible again here would
                // reverse PostExit to Visible and briefly redraw a shared element at its anchor.
                prepareForOwnerExit != null && !ownerExitPrepared && entry.visibleIntent -> true

                else -> false
            }
        if (entry.visibleIntent != visible) {
            entry.visibleIntent = visible
            // An entry that becomes visible moves to the front of the stack of the host, like a
            // window that is brought to the front when it is shown. An overlay that is opened
            // later therefore draws above the overlays that were already open, however early it
            // was registered.
            if (visible) state.bringToFront(entry)
        }
        // The host transition owns both the scrim and the panel's child transition, and a back
        // gesture seeks this one state, so removing an entry waits for both while the panel keeps
        // its independent appearance effects.
        val navigation = entry.navigationTransition
        val ownRoot =
            if (navigation == null) rememberTransition(entry.visibleState, label = "OverlayHandle")
            else null
        val transition =
            navigation?.visibility?.invoke(entry.navigationContentKey)
                ?: rememberOverlayVisibilityTransition(checkNotNull(ownRoot)) { it }
        entry.visibilityTransition = transition
        if (ownRoot != null) OverlayBackTimeline(entry, ownRoot)
        val interaction = entry.interaction
        val scrimColor = interaction?.scrimColor

        // A visibility child owns the settled scrim value while shared endpoints leave the root.
        val scrimTransition =
            navigation?.scrimVisibility
                ?: checkNotNull(ownRoot).createChildTransition(label = "OverlayPresentation") { it }
        val scrimAlpha by
            scrimTransition.animateFloat(
                transitionSpec = {
                    if (interaction?.animateScrim == false) snap() else entry.scrimAnimationSpec
                },
                label = "OverlayScrimAlpha",
            ) { visible ->
                if (visible && scrimColor != null) 1f else 0f
            }

        val panel: @Composable () -> Unit = {
            transition.AnimatedVisibility(
                visible = { if (entry.immediateContent) visible else it },
                enter = entry.enterTransition,
                exit = entry.exitTransition,
            ) {
                val visibility = this
                CompositionLocalProvider(LocalOverlayAnimatedVisibilityScope provides visibility) {
                    entry.content()
                }
            }
        }
        val layerVisible = transition.isPresent()
        OverlayLayer(
            entry,
            if (navigation == null || navigation.entry === entry) scrimAlpha else 0f,
            layerVisible,
        ) {
            panel()
        }

        LaunchedEffect(entry, entry.detached) {
            if (entry.detached && !ownerExitPrepared) {
                try {
                    prepareForOwnerExit?.invoke()
                } finally {
                    // A failed preparation must not strand an ownerless entry in the host forever.
                    ownerExitPrepared = true
                }
            }
        }

        LaunchedEffect(entry) {
            // The exit of the entry is played when the owner leaves, not when the entry is hidden:
            // an entry that is kept composed while it is hidden must stay registered, so that a
            // later opening of the owner finds the entry already in place.
            snapshotFlow { entry.detached }.first { it }
            snapshotFlow { transition.isHidden() }.first { it }
            // A gesture resolves its timelines before it releases the entry, so waiting for the
            // phase keeps a seek that is still in flight from stranding the entry.
            snapshotFlow { entry.backPhase }.first { it == OverlayBackPhase.Idle }
            entry.onExit?.invoke()
            snapshotFlow { sharedTransitionScope.isTransitionActive }.first { !it }
            state.remove(entry)
        }
    }

/**
 * Registers [content] as an overlay entry above the host content, or composes it in place when
 * there is no host, which keeps previews usable.
 *
 * The entry is registered as soon as the owner composes and follows [visible] while it is composed,
 * so an overlay that is kept composed starts in the frame its state changes. When the composable
 * that owns the entry leaves the composition, the entry plays [onExit] and is then removed from the
 * host. [onExit] runs in the scope of the host, so it also completes when the owner is already
 * gone. Entries using a caller-owned animation root are removed with that root.
 *
 * @param visible reads whether the entry is shown, usually `visible = { state }`. The host reads it
 *   in its own composition, so a shared animation between the entry and the page starts in the
 *   frame the state changes in both directions.
 */
@Composable
public fun Overlay(
    entry: OverlayHandle,
    modal: Boolean,
    visible: () -> Boolean = { true },
    interaction: OverlayInteraction? = null,
    backHandler: OverlayBackHandler? = null,
    sharedContentState: SharedContentState? = null,
    navigationTransition: OverlayNavigationTransition? = null,
    navigationContentKey: Any? = sharedContentState?.key,
    bindAnimation: @Composable () -> Unit = {},
    immediateContent: Boolean = false,
    enter: EnterTransition = EnterTransition.None,
    exit: ExitTransition = ExitTransition.None,
    prepareForOwnerExit: (suspend () -> Unit)? = null,
    onExit: (suspend () -> Unit)? = null,
    scrimAnimationSpec: FiniteAnimationSpec<Float> = snap(),
    provideCallerValues: @Composable (@Composable () -> Unit) -> Unit = { it() },
    content: @Composable () -> Unit,
) {
    val host = LocalOverlayHost.current
    entry.scrimAnimationSpec = scrimAnimationSpec
    entry.bindAnimation = bindAnimation
    entry.immediateContent = immediateContent
    entry.isModal = modal
    entry.visible = visible
    entry.interaction = interaction
    entry.backHandler = backHandler
    entry.sharedContentKey = sharedContentState?.key
    entry.navigationContentKey = navigationContentKey
    entry.navigationTransition = navigationTransition
    if (navigationTransition != null && (visible() || navigationTransition.entry == null)) {
        navigationTransition.entry = entry
    }
    entry.prepareForOwnerExit = prepareForOwnerExit
    entry.onExit = onExit
    entry.enterTransition = enter
    entry.exitTransition = exit
    entry.content = content
    val accessibilityPreferences = LocalAccessibilityPreferences.current
    val accessibilitySettings = LocalAccessibilitySettings.current
    val blurEnabled = LocalBlurEnabled.current
    val blurAnimationEnabled = LocalBlurAnimationEnabled.current
    entry.provideCallerValues = { hostedContent ->
        provideCallerValues {
            CompositionLocalProvider(
                LocalAccessibilityPreferences provides accessibilityPreferences,
                LocalAccessibilitySettings provides accessibilitySettings,
                LocalBlurEnabled provides blurEnabled,
                LocalBlurAnimationEnabled provides blurAnimationEnabled,
                content = hostedContent,
            )
        }
    }

    if (host == null) {
        check(LocalInspectionMode.current) {
            "OverlayHost is required for interactive overlays. Wrap this content in " +
                "OverlayHost(remember { OverlayHostState() }) and keep previews in inspection mode."
        }
        if (visible()) content()
        return
    }

    DisposableEffect(host) {
        host.add(entry)
        entry.remove = { host.remove(entry) }
        onDispose {
            entry.detachedWhileVisible = entry.visibleIntent
            entry.detached = true
        }
    }
}

/**
 * How an overlay entry takes pointer input.
 *
 * One interception layer is composed per entry together with the content of the entry, so the layer
 * and the content appear and disappear together. While the layer is active it covers the entries
 * under it and the host content, like the scene layers of a platform popup, and a press outside
 * [bounds] can dismiss the entry.
 *
 * @param bounds the bounds of the interactive content of the entry in window coordinates
 * @param active whether the interception layer exists
 * @param blocking whether the layer blocks the pointer input of everything under it
 * @param dismissOnClickOutside whether an outside gesture can dismiss the entry at [trigger]
 * @param trigger the pointer event that dismisses the entry
 * @param primaryButtonOnly whether only the primary button dismisses
 * @param onDismissRequest called when the entry is dismissed
 * @param accessibleScrimLabel label for a dismiss action on the scrim, when outside dismissal is
 *   enabled
 * @param scrimColor color of the dim drawn under the content of the entry, or `null` for no dim
 * @param animateScrim whether scrim alpha animates between the host visibility states
 */
public class OverlayInteraction(
    public val bounds: OverlayBounds,
    public val active: () -> Boolean = { true },
    public val blocking: () -> Boolean = { true },
    public val dismissOnClickOutside: () -> Boolean = { false },
    public val trigger: OverlayDismissTrigger = OverlayDismissTrigger.Press,
    public val primaryButtonOnly: Boolean = false,
    public val onDismissRequest: () -> Unit = {},
    public val accessibleScrimLabel: String? = null,
    /** Color of the dim drawn under the content of the entry, or `null` for no dim. */
    public val scrimColor: Color? = null,
    /** Whether scrim alpha animates between the host visibility states. */
    public val animateScrim: Boolean = true,
    /** Optional alpha derived from component motion, read by the host composition. */
    public val scrimProgress: (() -> Float)? = null,
)

/**
 * Whether the entry takes input right now.
 *
 * Input follows the intent of the owner: an entry whose owner hid it stops taking input, even while
 * a back gesture is in progress, so a gesture is never delivered to an overlay that is leaving.
 */
internal val OverlayHandle.acceptsInput: Boolean
    get() = !detached && visible()

/**
 * Whether a back gesture belongs to this entry.
 *
 * A gesture belongs to the overlay its owner keeps on screen, and it keeps belonging to it while it
 * runs, whatever the gesture makes the overlay show. Gating this on what the overlay shows would
 * hand a gesture that already started to the page, so the page and the overlay would both go back.
 */
internal val OverlayHandle.takesBackGestures: Boolean
    get() =
        !detached &&
            backHandler?.enabled?.invoke() == true &&
            (visible() || backPhase != OverlayBackPhase.Idle)

/** Whether this entry currently blocks the content under it. */
internal fun OverlayHandle.blocksContentUnder(): Boolean =
    acceptsInput && (interaction?.active() ?: true)

/**
 * How an overlay entry handles back gestures.
 *
 * The host routes back gestures to the topmost visible entry that has a handler, so a component
 * never registers a handler of its own. An overlay that is kept composed while it is hidden
 * therefore never takes a back gesture from the page. The handler of the host is registered with
 * the overlay priority of the `NavigationEventDispatcher`, so back gestures reach the overlays of
 * the host before the page.
 *
 * @param enabled whether the component accepts back gestures, usually its `dismissOnBackPress`
 *   property
 * @param previewTarget the visibility the host seeks while the gesture is in progress, or `null`
 *   when the component moves its own content through [onProgress] instead, such as a sheet whose
 *   position is owned by its anchors
 * @param onProgress called with the progress of a predictive back gesture while it is in progress
 * @param onCommit called when a back gesture completes
 * @param onCancel called when a predictive back gesture is canceled
 */
public class OverlayBackHandler(
    public val enabled: () -> Boolean,
    public val previewTarget: () -> Boolean? = { false },
    public val onProgress: (Float) -> Unit = {},
    public val onCommit: () -> Unit = {},
    public val onCancel: () -> Unit = {},
)

/**
 * Keeps an entry's interaction layout stable while its panel appears and disappears. Pointer
 * handling follows owner visibility intent; scrim drawing follows the retained host transition. An
 * entry without an interaction draws [content] alone.
 */
@Composable
private fun OverlayLayer(
    entry: OverlayHandle,
    scrimAlpha: Float,
    layerVisible: Boolean,
    content: @Composable () -> Unit,
) {
    val interaction = entry.interaction
    if (interaction == null || !interaction.active()) {
        content()
        return
    }
    val blocking = interaction.blocking()
    val scrimColor = interaction.scrimColor
    val dismissEnabled = interaction.dismissOnClickOutside()
    Box(
        modifier =
            Modifier.fillMaxSize()
                .then(
                    if (layerVisible && entry.acceptsInput) {
                        Modifier.overlayContainerBounds(interaction.bounds)
                            .overlayPointerInput(
                                bounds = interaction.bounds,
                                consumePointerInputOutside = blocking,
                                dismissEnabled = dismissEnabled,
                                trigger = interaction.trigger,
                                primaryButtonOnly = interaction.primaryButtonOnly,
                                onDismissRequest = interaction.onDismissRequest,
                            )
                    } else {
                        Modifier
                    },
                ),
        contentAlignment = Alignment.TopStart,
    ) {
        if (layerVisible && scrimColor != null) {
            // The host retains this layer until scrim and panel transitions finish; scrim
            // opacity follows its own progress and may reach zero before the panel finishes.
            val scrimSemantics =
                if (dismissEnabled && interaction.accessibleScrimLabel != null) {
                    Modifier.semantics(mergeDescendants = true) {
                        traversalIndex = 1f
                        contentDescription = interaction.accessibleScrimLabel
                        onClick {
                            interaction.onDismissRequest()
                            true
                        }
                    }
                } else {
                    Modifier
                }
            Canvas(Modifier.fillMaxSize().then(scrimSemantics)) {
                val effectiveScrimAlpha = interaction.scrimProgress?.invoke() ?: scrimAlpha
                drawRect(color = scrimColor, alpha = effectiveScrimAlpha.coerceIn(0f, 1f))
            }
        }
        content()
    }
}
