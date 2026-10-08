# Overlay integration

Create one remembered `OverlayHostState` for the host region and place `OverlayHost` around the page.
The host supplies ordering, modal input isolation, Back dispatch, and the shared transition scope.
For glass, install `ProvideM3xMaterialRendering` (or the complete `M3xMaterialTheme`) and put the
page's `BackdropHost` outside it as shown in the
[page template](../README.md#backdrop-page-setup).

A page with no glass uses the existing minimal setup:

```kotlin
val overlayState = remember { OverlayHostState() }
OverlayHost(overlayState) {
    OverlayDialog(onDismissRequest = { /* dismiss */ }) { Text("Dialog") }
}
```

This is a composable-body excerpt. Hosted overlay components register with an enclosing host,
which owns their shared lifecycle and input boundary. Omitting it reports a configuration error
outside inspection, even for a hidden entry. Compose previews draw in place under
`LocalInspectionMode`.

## Keep state at the owning component

Visibility callbacks and component state remain with the page. The host reads visibility in its own
composition, orders entries by opening, and retains entries until exit and shared transitions finish.
Content using a caller-owned animation root is released with that root.
Reopening brings an existing entry to the front. Registration order breaks ties in the same frame.
The topmost modal entry blocks accessibility and pointer access to content beneath it, even when
outside dismissal is disabled. Closing intent immediately releases interception, Back routing,
keyboard focus, and accessibility isolation. Retained exit drawing stays at its stack order without
remaining a pointer target, including over the former panel area.

`OverlayHost` supplies one `SharedTransitionScope` for page and entries and an
`AnimatedVisibilityScope` for each entry. Custom hosts can provide a scope independently through
`ProvideLocalSharedTransitionScope`; hosted modal editors still need an overlay host.
Use the public shared-bounds and anchor helpers to coordinate endpoints.

Sources: [host and lifetime](../overlay/src/commonMain/kotlin/Overlay.kt),
[shared bounds](../material-overlay/src/commonMain/kotlin/SharedTransitionBounds.kt), and
[anchors](../material-overlay/src/commonMain/kotlin/SharedTransitionAnchor.kt).

## Dismissal contract

An outside gesture begins outside the panel. Dialog and sheet release must also remain outside;
entering the panel or cancelling the gesture preserves the overlay. Each accepted gesture requests
at most one dismissal.

| Component | Outside event | Mouse buttons | Back / Escape | Completion |
| --- | --- | --- | --- | --- |
| `OverlayDialog` | Release outside when enabled | Primary | `dismissOnBackPress` requests dismissal | Callback on accepted event |
| `OverlayBottomSheet` | Release outside when enabled | Primary | Collapses expanded sheet to partial when possible, otherwise hides | Callback after hide animation |
| `OverlayMenu` | Press outside when enabled | Any | `dismissOnBackPress` requests dismissal | Callback on accepted event |
| Focusable `OverlayTooltipBox` | Press outside | Any | Requests dismissal | Callback or default state dismissal immediately |

Dismissible panels expose an accessible Dismiss action. A sheet also exposes a labeled Close sheet
scrim action when outside dismissal is enabled. These use the same component close paths. Plain,
non-focusable tooltips pass through outside input and expose no Dismiss action.

Source: [scrim/input handling](../overlay/src/commonMain/kotlin/OverlayScrim.kt).
Regressions: [dismissal paths](../material-overlay/src/commonTest/kotlin/OverlayDismissBehaviorTest.kt) and
[input lifetime](../overlay/src/nonBrowserTest/kotlin/OverlayInputLifetimeTest.kt).

Back gestures use NavigationEvent at overlay priority. The topmost visible entry that accepts Back
receives that gesture's events through completion or cancellation. A newer semantic target takes
control of the original entry's animation and supersedes its pending gesture result.
Dialogs (including alert dialogs), menus and focusable tooltips preview their configured exit:
container effects, foreground blur, scrim and library shared anchors follow the same animation
specifications as ordinary dismissal. Disabled transitions and accessibility motion settings retain
their effective behavior. Ordinary tooltips and snackbars do not claim Back.

A sheet previews the action Back would perform: an expanded sheet returns to its partial anchor
when available; otherwise it exits. Ordinary sheets seek their offset motion, while shared sheets
seek their shared-bounds exit. Cancellation restores the presentation without requesting dismissal.
Completion continues from the preview; `confirmValueChange` can veto a sheet state change, restoring
the sheet without reporting dismissal.

Each host-managed appearance has one stable seekable animation root. The host derives the panel,
shared anchor, foreground effects and scrim from that root. Ordinary sheet position follows its
stable offset timeline, with the scrim derived from physical offset. Back previews seek the configured
exit; cancellation rewinds that same segment before restoring its state. A new owner request interrupts
the previous animation immediately, including a closing gesture's remaining playback.
During terminal playback, the host consumes subsequent Back sequences until the presentation
settles or a new owner request takes control. Progress updates supersede pending seeks on the
same animation root.

Caller-owned shared content supplies its root through
`rememberOverlayNavigationTransition(state, transition, targetState, backTarget, isVisible)`.
The caller updates the semantic `targetState` and lets this connection exclusively drive `state`.
The visibility mapping receives each panel's shared content key, so outgoing settings editors keep
their own identity when another item opens. One presentation scrim follows the root while outgoing
content remains composed until its animations finish. Shared anchors borrow their panel's inverse
visibility instead of maintaining another seekable state.

Regression: [predictive Back](../overlay/src/nonBrowserTest/kotlin/OverlayPredictiveBackTest.kt).
Native timeline regression:
[unequal child durations](../overlay/src/nonBrowserTest/kotlin/OverlayTimelineTest.kt).
Gesture ownership regression:
[interruption and terminal playback](../overlay/src/nonBrowserTest/kotlin/OverlayGestureTakeoverTest.kt).
Settings regression:
[cancellation, completion and reopening](../settings-page-material/src/nonBrowserTest/kotlin/CardSettingsTest.kt).

## Compose a hosted surface

Choose the material role from the content's purpose: a panel provides container backing, while an
inverse panel provides the inverse Material color pair for transient feedback. The component owns
its local presentation state, and the host coordinates that state with the enclosing stack. For
example, sheet expansion belongs to sheet state while ordering and modal isolation belong to the
host.

Public component APIs live in `top.ltfan.m3x.material`. Host primitives live in `top.ltfan.m3x.overlay`; Material shared-transition helpers remain in
`top.ltfan.m3x.material.overlay`. Custom components consume those enclosing services and choose
appearance through the same [material contracts](design-system/materials.md).
