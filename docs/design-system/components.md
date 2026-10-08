# Component behavior

M3X components consume shared design values and application-owned state. The contracts below
explain how layout, appearance, input, and lifecycle responsibilities compose. Named APIs identify
where each contract applies and how an application uses it.

## C1. Scroll surfaces follow actual content overlap

Backdrop top bars consume a `TopAppBarScrollBehavior`; connect its `nestedScrollConnection`
to the scrolling content. Glass appears when its state reports content under the bar, and clears
when content returns to the start. Background optics follow the [bounded transition contract](accessibility-and-motion.md).
With no behavior, glass stays present. When replacing the content tracked by a behavior, clear its
state as the demo does when opening another page.

Measure content insets through `Scaffold`'s current `innerPadding`. For a lazy list, put the current
top inset in `contentPadding` so content can scroll under a collapsing bar as its height changes.
The full-size background remains behind the app bar. The demo disables the Lab's collapsing
behavior at the narrow breakpoint and resets offsets when returning to the wide layout.

`BackdropTopAppBarControl` groups a navigation icon or actions in a single capsule material. Paired
M3X top bars allow control shadows outside their rows while clipping title content.

Sources: [bars](../../material-backdrop/src/commonMain/kotlin/BackdropAppBar.kt),
[control surface](../../material-backdrop/src/commonMain/kotlin/BackdropTopAppBarControl.kt), and
[demo layout](../../demo/src/commonMain/kotlin/DemoApp.kt).
Regressions: [scroll presence](../../material-backdrop/src/nonBrowserTest/kotlin/BackdropScrollPresenceTest.kt)
and [measured demo layout](../../demo/src/nonBrowserTest/kotlin/DemoLayoutTest.kt).

## C2. Floating navigation sizes to its tabs

`FloatingBottomBar` uses the extended `full` shape and Control material. The selected index belongs
to the caller; selection requests use `onTabSelected`. Each tab is at least `tabMinWidth` wide
(76 dp by default). Setting it to zero permits pure content sizing. Label length does not set
unrelated tabs' proportions. The bar uses current Material motion and the library blur preference.

Source: [floating bar](../../material/src/commonMain/kotlin/FloatingBottomBar.kt).
Regressions: [sizing](../../material-backdrop/src/commonTest/kotlin/FloatingBottomBarSizingTest.kt) and
[interaction/layout](../../material-backdrop/src/nonBrowserTest/kotlin/FloatingBottomBarTest.kt).

## C3. Card appearance combines one backing with Material elevation

M3X `Card` uses the Surface material by default, retaining Material card elevation and border.
Set `backdrop = false` to use direct card colors; `backdropMaterial` replaces its backing material.
Grouped cards keep a continuous backing appearance and trim only the group's internal corners.
The resolved `LocalCardShape` is available for descendants that need to clip shared content.

Sources: [card](../../material/src/commonMain/kotlin/Card.kt) and
[grouped cards](../../material/src/commonMain/kotlin/CardLazyGroup.kt).
Shape contract: [S6](shapes.md#s6-grouped-cards-preserve-the-outer-silhouette).

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
NavigationEvent gesture stays with its original entry. Sheets retain their partial-collapse and
state-change veto behavior. See [gesture integration](../overlays.md#dismissal-contract).

Source: [overlay host](../../overlay/src/commonMain/kotlin/Overlay.kt).
Regressions: [visibility](../../overlay/src/commonTest/kotlin/OverlayVisibilityProviderTest.kt),
[dismissal](../../material-overlay/src/commonTest/kotlin/OverlayDismissBehaviorTest.kt), and
[sheet reopening](../../material-overlay/src/commonTest/kotlin/OverlaySheetReopeningTest.kt).

## C5. List structure uses the public DSL

`GroupedLazyListScope`, `LazyListGroupScope`, and `CardLazyGroup` are sealed DSL scopes. Build through
`group`, `item`, `items`, `stickyHeader`, `header`, and `card`; use `LazyGroup` for custom group content.
Generated collectors own low-level registration. A card group has one header slot: the latest header
call wins and that header renders before ordinary entries. Keep item keys stable and unique.

Sources: [generic list](../../foundation/src/commonMain/kotlin/GroupedLazyList.kt) and
[Material helpers](../../material/src/commonMain/kotlin/layout/GroupedLazyList.kt).
Regressions: [generic DSL](../../foundation/src/commonTest/kotlin/GroupedLazyListDslTest.kt) and
[card DSL](../../material/src/commonTest/kotlin/CardLazyGroupDslTest.kt).

## C6. Settings separate immediate edits from modal transactions

Switches commit immediately. Sliders commit after the gesture by default, or continuously with
`SliderCommitMode.WhileDragging`. Modal editors keep a draft until Save. Cancel, outside dismissal,
and Back discard it. Input acceptance means queued local persistence, not confirmed disk durability.
All UI commit paths use `SettingsCommit`; errors go to caller-owned presentation.

Stable item IDs connect row and editor. Labels, descriptions, and selected Selector values use
`SettingsSharedText` where both endpoints show the same text. Values rendered only on one side fade
with their surroundings; reduced motion disables that sharing. Rows and retained editors derive
visibility from one editor transition, with each item ID preserving its content identity. The page
consumes the enclosing theme and overlay host.

Source: [settings UI](../../settings-page-material/src/commonMain/kotlin/CardSettings.kt).
Details: [settings guide](../settings.md).
Regression: [settings rendering and interaction](../../settings-page-material/src/nonBrowserTest/kotlin/CardSettingsTest.kt).

## C7. Expressive controls retain Material interaction ownership

M3X uses the Expressive interaction overload when Material supplies one. Material owns the control's
layout, semantics, interaction states, typography, and motion. M3X shape defaults adapt the designated
resting or selected role; explicit shapes and other caller-supplied values keep precedence. Components
with host-managed optics retain the host, material, and lifecycle contracts above.

Toggle buttons accept Material's `ToggleButtonSize` and optional icon slot. The size selects native
height, padding, typography, and icon dimensions; the resting shape still follows the extended theme.
Slider state overloads accept explicit change and completion callbacks. A change callback receives a
proposed value for the owner to accept; omitting it lets the native slider state update itself.
Value-based adapters keep the value in application-owned state.

Search fields, collapsed bars, and expanded results share one `SearchBarState`. Menu items use the
Material action, checkbox, or radio semantics and content slots, while the enclosing `OverlayHost`
continues to own presentation and dismissal. Top bars expose subtitle and alignment inputs; flexible
bars choose their default expanded height from the subtitle's presence.

Sources: [toggle buttons](../../material/src/commonMain/kotlin/ToggleButton.kt),
[sliders](../../material/src/commonMain/kotlin/Slider.kt),
[search](../../material/src/commonMain/kotlin/SearchBar.kt), and
[menu items](../../material-overlay/src/commonMain/kotlin/OverlayMenu.kt).
Regressions: [size and icon slots](../../material/src/commonTest/kotlin/ToggleButtonSizeTest.kt),
[slider ownership](../../material/src/commonTest/kotlin/SliderCallbackTest.kt),
[menu semantics](../../material/src/commonTest/kotlin/OverlayMenuItemSemanticsTest.kt), and
[top-bar inputs](../../material/src/commonTest/kotlin/BackdropAppBarApiTest.kt).
