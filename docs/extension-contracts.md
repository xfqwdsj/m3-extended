# Extension contracts and shared mechanisms

This guide describes the contracts of specific public APIs. Module boundaries are documented in
[architecture](architecture.md); the APIs discussed here do not exhaust a module's scope.

Choose a shared API by its state ownership and dependency boundary. A helper does not need an
application call site to be a useful public API, and similar implementation fragments do not imply
that their input, cancellation and completion contracts are interchangeable.

## State and animation

The Flow-to-property adapters support source-confirmed reads and callback-owned writes. They are
distinct from `SettingsEditSession`, which owns a local draft and confirmation. Temporary
reset state is suitable for transient application feedback; snackbar timing still follows its
accessibility and dismissal rules.

`withDefaultVisibilityThreshold` fills an omitted spring threshold while preserving explicit
thresholds, stiffness, damping and non-spring specifications.
`MotionScheme.sharedBoundsSpec` chooses the Material role and physical rectangle tolerance.
`rewindTransition` rewinds an existing seekable segment without replacing child timings.
Foreground blur/fade helpers remain separate from app-bar background blur and mask transitions.

`FocusRequestingEffect` waits for an entrance to settle. Overlay mounting and input activation use
`rememberOverlayFocusRequester`; these are different focus triggers and should not be substituted.

Sources: [Flow bridge](../runtime/src/commonMain/kotlin/FlowAsMutableState.kt),
[spring threshold](../animation-core/src/commonMain/kotlin/VisibilityThreshold.kt),
[rewind](../animation/src/commonMain/kotlin/RewindTransition.kt), and
[overlay focus](../overlay/src/commonMain/kotlin/OverlayInternals.kt).

## Layout and gestures

`keepSizeWhenLookingAhead` retains the measured lookahead constraints. A panel that fills animated
bounds while preserving its lookahead content size uses a separate panel layout mechanism.

`inspectDragGestures` observes initial down, pointer replacement, consumption,
release and cancellation. `DampedDragState` coordinates a bounded value, press response and velocity
using caller-supplied specifications and callbacks. Floating-bar tab selection, thresholds, scale
ratios and effects remain decisions of the consuming component.

Sources: [grouped lists](../foundation/src/commonMain/kotlin/GroupedLazyList.kt),
[lookahead size](../foundation/src/commonMain/kotlin/KeepSizeWhenLookingAhead.kt),
[gesture observation](../foundation/src/commonMain/kotlin/gestures/DragGestureInspector.kt), and
[drag state](../foundation/src/commonMain/kotlin/gestures/DampedDragState.kt).

## Custom presentation and rendering

Use `OverlayHandle`, `OverlayInteraction`, bounds modifiers and `Overlay` for an unstyled custom
presentation. The caller supplies interaction bounds and dismissal/back policy; the host owns order,
modal isolation and retained transitions. `MaterialOverlay` additionally forwards the caller's
Material and renderer environment. `transitionFor` exposes the binding needed by a shared page-side
anchor. Mutable host entry lists, retirement flags and gesture session counters remain internal.

`OverlayHostDecoration` receives read-only layer identities and order. An implementation returns
capture modifiers and content wrappers; it must retain host input and transition ownership.
`BackdropOverlayDecoration` adapts Backdrop to this host contract.

`MaterialSurfaceRenderer` supplies role configuration, a remembered renderer layer and resolved
foreground/drawing behavior. Components keep shape, layout and their interaction source.
`captureEnvironment` forwards renderer configuration from a caller into its hosted content without
replacing host rendering scopes. `BackdropSurfaceEffects` and `BackdropIndicatorEffects` carry
explicit replacement chains for the Backdrop adapter; ordinary components do not expose Backdrop
types in their signatures.

`FloatingBottomBarRenderer` supplies optics and optional tab capture to the bar's single layout and
gesture implementation. Renderers observe progress through callbacks so frame-level drawing reads
remain frame-level reads. `ProvideFloatingBottomBarTabScale` scopes the additional captured tab layer
without changing the visible row's layout.

Sources: [unstyled presentation](../overlay/src/commonMain/kotlin/Overlay.kt),
[Material caller environment](../material-overlay/src/commonMain/kotlin/HostedOverlay.kt),
[surface SPI](../material/src/commonMain/kotlin/MaterialSurface.kt), and
[Backdrop implementation](../material-backdrop/src/commonMain/kotlin/BackdropMaterialRenderer.kt).

## Custom settings renderers

Settings validation and commit contracts are usable independently of UI. `sliderMappingOrNull` offers the requested slider domain or a stepped mapping for another
numeric presentation; the renderer decides whether to show a slider or a different editor.
`SliderCommitMode` describes when a slider preview is submitted; it does not own gesture processing.

`SettingsEditSession` and `SettingsPresentationError` support renderer-independent editing. Material
localization belongs to the Material adapter. A custom renderer can present the same structured
errors and retry paths without depending on Card rendering or DataStore.

`asSettingsStatus` maps readiness and failures directly from the source StateFlow without
introducing a background collector. `jsonStoreCodec` accepts the
application's serializer and JSON configuration; file paths and backend coordination remain owned by
the application and store host.

Sources: [number mapping](../settings-page-core/src/commonMain/kotlin/NumberSliderMapping.kt),
[editor errors](../settings-page-compose/src/commonMain/kotlin/SettingsPresentationError.kt), and
[DataStore adapter](../settings-page-datastore/src/commonMain/kotlin/SettingsPersistence.kt).
