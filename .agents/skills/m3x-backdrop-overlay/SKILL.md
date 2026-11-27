---
name: m3x-backdrop-overlay
description: Integrate M3X Backdrop rendering and hosted overlays in Compose Multiplatform pages, including page capture, glass surfaces, dialog and sheet presentation, dismissal and predictive Back, shared transitions, tooltips, menus, and snackbars. Use when adding M3X optical surfaces or any component that must register with OverlayHost.
license: MIT
---

# M3X Backdrop and Overlays

Repository: https://github.com/xfqwdsj/m3-extended

Use this skill for page capture, Backdrop-backed surfaces, and hosted overlay presentation.
Repository-relative links below point to the canonical integration guides; when installed outside the
repository, resolve them against the repository URL above.

## What this skill covers

The Backdrop and overlay systems provide:

- Page-region pixel capture with `BackdropHost`, `rememberBackdropLayer`, and
  `Modifier.captureBackdrop`.
- Blur, lens, color, fill, highlight, shadow, progressive blur, HDR output hooks, and source
  adaptation primitives.
- Material surface roles and presets that consume Backdrop sources.
- Overlay source stacking through `overlay-backdrop`.
- Hosted dialogs, alert dialogs, bottom sheets, menus, focusable tooltips, snackbars, and floating
  actions.
- Host-owned ordering, modal input isolation, Back dispatch, focus and accessibility isolation,
  retained exits, shared transitions, and predictive Back previews.

Use `m3x-material` for ordinary theme and component selection. This skill concerns pixels and host
lifecycle.

## Required component order

```text
M3X Material > Material 3 Expressive > Material 3
```

Use M3X's hosted overlay or Backdrop-aware component when M3X provides the same responsibility. If
M3X does not provide it, use the Material 3 Expressive equivalent when available, then Material 3.
Do not mix another design system's overlay or glass component stack and expect the M3X visual and
interaction contracts to hold.

## Establish the host tree

For a Backdrop page, install renderer adapters once, place `BackdropHost` outside `OverlayHost`, and
capture the page subtree that supplies source pixels.

```kotlin
val pageLayer = rememberBackdropLayer()
val overlayState = remember { OverlayHostState() }

ProvideM3xMaterialRendering {
    BackdropHost(pageLayer) {
        OverlayHost(overlayState) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .captureBackdrop(pageLayer)
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    PageContent()
                }

                FloatingBottomBar(/* ... */)
                OverlayDialog(/* ... */)
            }
        }
    }
}
```

If the application already uses `M3xMaterialTheme`, do not duplicate
`ProvideM3xMaterialRendering`; the integrated theme installs it.

Core host rules:

- Applications own `BackdropHost` and `OverlayHost` lifetimes. Components must not create their own
  hosts.
- `BackdropHost` must enclose the host-managed overlays that sample its source; the documented page
  order is `BackdropHost` outside `OverlayHost`.
- Create one remembered `OverlayHostState` per host region and keep it stable.
- An interactive overlay without an enclosing `OverlayHost` is a configuration error outside
  inspection mode, even when the overlay is hidden.
- `captureBackdrop` must precede `background` in the modifier chain when the background is part of
  the intended capture.
- Keep sampling surfaces outside their own source subtree. A material must not sample the pixels it
  is currently drawing into.
- Applications keep visibility state and component intent; the host owns ordering, modal isolation,
  Back dispatch, retained exits, and the shared transition scope.

## Preserve overlay lifecycle

- Registration order breaks same-frame tie cases; reopening brings the existing entry to the front.
- Content beneath the topmost modal entry is not an accessibility or pointer target, even when
  outside dismissal is disabled.
- Closing intent immediately releases interception, Back routing, keyboard focus, and accessibility
  isolation. Retained exit pixels remain in stack order without remaining input targets.
- The host retains entries until exit and shared-transition work complete.
- Do not bypass host registration by drawing a custom overlay as an unrelated sibling. Use the public
  overlay primitives or a host decoration when lower-level presentation is required.
- Custom `OverlayHostDecoration` implementations must preserve host input and transition ownership.
- Shared page/editor anchors must use the public shared-bounds helpers and stable content identity.

## Follow dismissal and Back contracts

Use each component's documented close path rather than implementing a parallel dismissal state.

- Outside gestures must begin outside the panel and complete outside where the component requires
  release confirmation.
- Dialogs and menus request dismissal once per accepted event.
- Bottom sheets release outside when enabled; Back collapses an expanded sheet to a partial anchor
  when possible, otherwise it exits.
- Cancelling a predictive Back gesture restores the same presentation without requesting dismissal.
- Completing Back continues from the previewed animation; caller vetoes remain caller-owned.
- Sheet state confirmation can veto a sheet change and restore the sheet without reporting dismissal.
- A new owner request interrupts the previous presentation animation and takes over its stable
  animation root.
- Ordinary tooltips and snackbars do not claim Back; focusable tooltip and dialog/menu behaviors use
  their documented Back ownership.

## Supply Backdrop pixels deliberately

- A supplied background is baked into the source. Alternatively, capture a background drawn inside
  the captured subtree.
- Changing a background color with the same source handle updates capture without replacing the
  handle when tile mode is unchanged.
- Each overlay entry samples the page and lower entries, then exports its captured pixels for higher
  entries. Lower layers contribute once at each stack level.
- The overlay adapter maintains a null source when the page source is absent. Built-in Material
  surfaces must use their documented fallback rather than crashing or inventing source pixels.
- `TileMode.Clamp` extends boundary texels; `TileMode.Decal` makes the exterior transparent;
  `TileMode.Repeated` and `TileMode.Mirror` repeat or mirror the source.
- Diagnose missing coverage before increasing blur radius or tint opacity. Transparency at the source
  boundary cannot be repaired by an effect.
- Select Backdrop material roles by content purpose. `Control`, `Panel`, `Surface`, `InversePanel`,
  and `ScrollEdge` are behavior contracts, not interchangeable optical styles.
- Keep reduced-transparency, reduced-motion, blur gating, and contrast policy in the M3X appearance
  layer unless the caller explicitly owns a custom render graph.

## Advanced controls

Use `LocalHighlightSdfSettings` only when contour highlight preparation needs an explicit SDF mode or
backend. Automatic mode is the default. Forced GPU rendering falls back to contour rendering when GPU
field baking is unavailable.

Keep output capability separate from theme appearance. HDR hooks do not make every intermediate
capture or display path HDR-capable; validate on the target platform when HDR output is claimed.

## Review checklist

1. Confirm host order and exactly one stable `OverlayHostState`.
2. Confirm the captured subtree and modifier order match the intended source boundary.
3. Confirm no sampling surface is inside its own captured source.
4. Confirm hidden interactive overlays still have a host.
5. Confirm modal isolation, Back, focus, and accessibility ownership come from the host.
6. Confirm source absence reaches the documented fallback.
7. Confirm overlay stacking and retained exit behavior survive reopening and interruption.
8. Confirm accessible dismissal and reduced-motion behavior remain intact.

## Canonical guides

- [Backdrop integration](../../../docs/backdrops.md)
- [Overlay integration](../../../docs/overlays.md)
- [Page setup example](../../../README.md#backdrop-page-setup)
- [Material system](../../../docs/design-system/materials.md)
- [Component and host responsibilities](../../../docs/design-system/components.md)
- [Accessibility, motion, and interaction light](../../../docs/design-system/accessibility-and-motion.md)
- [Custom presentation and rendering contracts](../../../docs/extension-contracts.md#custom-presentation-and-rendering)
- [Backdrop public API](../../../modules/backdrop/src/commonMain/kotlin/Backdrop.kt)
- [Overlay host and lifetime](../../../modules/overlay/src/commonMain/kotlin/Overlay.kt)
- [Material hosted overlay](../../../modules/material-overlay/src/commonMain/kotlin/HostedOverlay.kt)
- [Shared bounds](../../../modules/material-overlay/src/commonMain/kotlin/SharedTransitionBounds.kt)
- [Predictive Back integration](../../../modules/navigation-event/src/commonMain/kotlin/SeekableTransitionNavigationEventHandler.kt)
