# Component behavior

M3X components consume shared design values and application-owned state. The contracts below
explain how layout, appearance, input, and lifecycle responsibilities compose. Named APIs identify
where each contract applies and how an application uses it.

## C1. Scroll surfaces follow actual content overlap

Backdrop top bars consume a `TopAppBarScrollBehavior`; connect its `nestedScrollConnection`
to the scrolling content. Glass appears when its state reports content under the bar, and clears
when content returns to the start. Background optics follow the [bounded transition contract](accessibility-and-motion.md).
With no behavior, glass stays present within the bar, preserving the supplied fill brush without
progressive extension. When replacing the content tracked by a behavior, clear its
state as the demo does when opening another page.

Measure content insets through `Scaffold`'s current `innerPadding`. For a lazy list, put the current
top inset in `contentPadding` so content can scroll under a collapsing bar as its height changes.
The full-size background remains behind the app bar. The demo pins the narrow Lab's bar and disables
scroll updates while its retained overlap animation clears the background optics. Navigation and
breakpoint changes reset offsets.

`BackdropTopAppBarControl` groups a navigation icon or actions in a single capsule material. Paired
M3X top bars allow control shadows outside their rows while clipping title content.

Sources: [bars](../../modules/material-backdrop/src/commonMain/kotlin/BackdropAppBar.kt),
[control surface](../../modules/material-backdrop/src/commonMain/kotlin/BackdropTopAppBarControl.kt), and
[demo layout](../../modules/demo/src/commonMain/kotlin/DemoApp.kt).
Regressions: [scroll presence](../../modules/material-backdrop/src/jvmTest/kotlin/BackdropScrollPresenceTest.kt)
and [measured demo layout](../../modules/demo/src/jvmTest/kotlin/DemoLayoutTest.kt).

## C2. Floating navigation sizes to its tabs

`FloatingBottomBar` uses the extended `full` shape and Control material. The selected index belongs
to the caller; selection requests use `onTabSelected`. Each tab is at least `tabMinWidth` wide
(76 dp by default). Setting it to zero permits pure content sizing. Label length does not set
unrelated tabs' proportions. The bar uses current Material motion and the library blur preference.

Source: [floating bar](../../modules/material/src/commonMain/kotlin/FloatingBottomBar.kt).
Regressions: [sizing](../../modules/material-backdrop/src/composeTest/kotlin/FloatingBottomBarSizingTest.kt) and
[interaction/layout](../../modules/material-backdrop/src/jvmTest/kotlin/FloatingBottomBarTest.kt).

## C3. Content surfaces preserve Material elevation

Content surfaces use native Material colors, elevation and border by default. A component whose
visual contract needs an ornate renderer backing can provide an explicit opt-in and a renderer-neutral
style override. The current card component uses `backdrop = true` to select `Surface` and
`surfaceStyle` to override that backing; the selected decorative material can participate in shared
interaction light when a source and the lighting policy enable it. The resolved `LocalCardShape` is
available for descendants that need the card's clip shape.

Source: [card](../../modules/material/src/commonMain/kotlin/Card.kt).

## C4. Overlay ordering, lifetime, and input belong to a host

An overlay becoming visible moves to the front by opening order. The topmost modal layer blocks
input and accessibility access to content beneath it. The host routes Back and captures each entry
for the backdrop stack. Page and overlays share one transition scope. Visibility lambdas are read
by the host so both endpoints react in the same frame. An owner's removal keeps the entry alive
through its exit and shared transitions; component cleanup runs before final removal. Input follows
owner visibility intent independently of that drawing lifetime, so a closing entry releases the
page immediately.

Interactive overlays require `OverlayHost` even while hidden. `LocalInspectionMode` permits in-place
preview drawing. A glass-free page still requires the overlay host; source pixels are the separate
responsibility of `BackdropHost`. Dismissal timing is component-specific and is specified in
[overlay integration](../overlays.md#dismissal-contract).

Back preview traverses the current exit animation and restores it on cancellation. The same
NavigationEvent gesture stays with its original entry. Sheets select available states through
`enabledValues`, which includes Expanded and the initial state. A small sheet keeps its enabled
partial anchor at the expanded position; growing a sheet from those converged anchors preserves
full expansion. State-change vetoes remain caller-owned.
See [gesture integration](../overlays.md#dismissal-contract).

Source: [overlay host](../../modules/overlay/src/commonMain/kotlin/Overlay.kt).
Regressions: [visibility](../../modules/overlay/src/jvmTest/kotlin/OverlayVisibilityProviderTest.kt),
[dismissal](../../modules/material-overlay/src/composeTest/kotlin/OverlayDismissBehaviorTest.kt), and
[sheet reopening](../../modules/material-overlay/src/composeTest/kotlin/OverlaySheetReopeningTest.kt).

## C5. List structure uses the public DSL

Use `group`, `item`, `items`, and `stickyHeader` to build a grouped lazy list, or implement
`LazyGroup` for custom registration. Keep item keys stable and unique within the list.

In a `GroupedLazyColumn`, `segmentedListGroup` supplies Material list item shapes and a modifier
for row spacing. Apply both to each `SegmentedListItem`. The optional `header` sits outside
the segment count and exposes heading semantics. It inherits the on-surface variant color and
title-medium typography. A sticky heading uses the surface color as its backing and pins until
the next group boundary.

Sources: [generic list](../../modules/foundation/src/commonMain/kotlin/GroupedLazyList.kt) and
[segmented list group](../../modules/material/src/commonMain/kotlin/SegmentedListGroup.kt).
Regressions: [generic DSL](../../modules/foundation/src/commonTest/kotlin/GroupedLazyListDslTest.kt) and
[segmented sections](../../modules/material/src/composeTest/kotlin/SegmentedListGroupTest.kt).

## C6. Settings separate immediate edits from modal transactions

Switches commit immediately. Sliders commit after the gesture by default, or continuously with
`SliderCommitMode.WhileDragging`. Modal editors keep a draft until Save. Cancel, outside dismissal,
and Back discard it. Input acceptance means queued local persistence, not confirmed disk durability.
All UI commit paths use `SettingsCommit`; errors go to caller-owned presentation.

Stable item IDs connect row and editor. Labels, descriptions, and selected Selector values use
`SettingsSharedText` where both endpoints show the same text. Values rendered only on one side fade
with their surroundings. Reduced motion disables shared text transitions. Rows and retained editors
derive visibility from the same editor transition. Each item ID preserves its content identity.
The page consumes the enclosing theme and overlay host.

Source: [settings UI](../../modules/settings-page-material/src/commonMain/kotlin/SettingsPage.kt).
Details: [settings guide](../settings.md).
Regressions: [settings interaction](../../modules/settings-page-material/src/composeTest/kotlin/SettingsPageInteractionTest.kt)
and [rendering transitions](../../modules/settings-page-material/src/jvmTest/kotlin/SettingsPageTest.kt).

## C7. Expressive controls retain Material interaction ownership

M3X exposes Expressive controls and follows official replacement recommendations. Wrappers address
Backdrop rendering or extended-theme integration; controls that work directly use Material APIs.
Connected button groups provide single- and multi-selection. Material owns the control's
layout, semantics, interaction states, typography, and motion. M3X shape defaults adapt the designated
resting or selected role; explicit shapes and other caller-supplied values keep precedence. Components
with host-managed optics retain the host, material, and lifecycle contracts above.

Toggle buttons accept Material's `ToggleButtonSize` and optional icon slot. The size selects native
height, padding, typography, and icon dimensions; the resting shape still follows the extended theme.
Slider controls take a state and a required change callback, with an optional completion callback.
The change callback receives a proposed value for the owner to accept and assign to the state.
Thumbs take explicit orientation when used outside their default control slots.

Search fields, collapsed bars, and expanded results share one `SearchBarState`. Menu items use the
Material action, checkbox, or radio semantics and content slots, while the enclosing `OverlayHost`
continues to own presentation and dismissal. Top bars expose subtitle and alignment inputs; flexible
bars choose their default expanded height from the subtitle's presence.

Sources: [toggle buttons](../../modules/material/src/commonMain/kotlin/ToggleButton.kt),
[sliders](../../modules/material/src/commonMain/kotlin/Slider.kt),
[search](../../modules/material/src/commonMain/kotlin/SearchBar.kt), and
[menu items](../../modules/material-overlay/src/commonMain/kotlin/OverlayMenu.kt).
Regressions: [size and icon slots](../../modules/material/src/composeTest/kotlin/ToggleButtonSizeTest.kt),
[slider ownership](../../modules/material/src/composeTest/kotlin/SliderCallbackTest.kt),
[menu semantics](../../modules/material/src/composeTest/kotlin/OverlayMenuItemSemanticsTest.kt), and
[top-bar inputs](../../modules/material/src/composeTest/kotlin/BackdropAppBarApiTest.kt).
