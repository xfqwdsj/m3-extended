---
name: m3x-foundations
description: Use M3X lower-level Compose foundations directly, including Flow and state bridges, grouped lazy lists, drag utilities, animation and predictive Back, Material 3 utilities, structured shape and Capsule geometry, and UI-independent resources. Use when a consumer needs a focused primitive without the complete M3X Material bundle.
license: MIT
---

# M3X Compose Foundations

Repository: https://github.com/xfqwdsj/m3-extended

Use this skill for the reusable lower layers below M3X's Material integration. Repository-relative
links below point to the canonical architecture and source contracts; when installed outside the
repository, resolve them against the repository URL above.

## What this skill covers

The foundation modules provide focused building blocks:

| Module | Consumer capability |
| --- | --- |
| `runtime` | Flow-as-Compose state, scoped StateFlow observation, and auto-restorable transient state. |
| `ui` | Accessibility preferences and system observation, platform corner radii, color/contrast helpers, and visual policy. |
| `ui-platform-private` | Opt-in private platform probes; not part of the public foundation default. |
| `animation-core` | Shared animation thresholds. |
| `animation` | Blur transitions, interpolated shapes, rewind transitions, focus helpers, and accessibility-aware motion. |
| `foundation` | Unstyled grouped lazy lists, drag inspection and damped drag state, lookahead sizing, padding and inset utilities. |
| `navigation-event` | Seekable predictive Back integration with NavigationEvent transitions. |
| `material3` | Extensions over upstream Compose Material 3, including increased-contrast colors and shared-bounds motion. |
| `shape` | Double-precision contours, path segments, flattening, distance/SDF geometry, and Compose/SVG output. |
| `capsule` | Continuous rounded rectangles, continuity profiles, contours, and interpolation. |
| `resource-core` | UI-independent `Resource` contracts, including context-free global values. |
| `resource-compose` | Composition resolution and painter adapters for resources. |

These modules are useful independently. Do not add `material-bundle` merely to obtain a foundation
API, and do not treat a foundation module as a second design-system host.

## Preserve composition and runtime ownership

### Flow and state bridges

- `Flow.collectAsMutableState(initial, set)` collects the latest emission into Compose state.
  Assigning the delegated property forwards to `set`; it does not mutate the internally collected
  value.
- The `StateFlow<T>.collectAsMutableState(scope, set)` overload launches collection in the caller's
  scope. The caller owns the scope and its cancellation.
- Use `rememberAutoRestorableState` for transient UI flags. Each write cancels the previous restore
  job and schedules a reset to the captured initial value after the delay.
- Do not use an auto-restorable state for data that must survive recomposition or persistence.

### Grouped lists and gestures

- Build grouped lists with the public `group`, `item`, `items`, and `stickyHeader` DSL.
- Keep item keys stable and unique within the list.
- In Material integrations, `segmentedListGroup` supplies both row shape state and spacing. Apply
  both to each segmented row and keep headers outside the segment count.
- `inspectDragGestures` observes the initial down, pointer replacement, consumption, release, and
  cancellation. Do not duplicate its gesture state machine in a component.
- `DampedDragState` coordinates bounded value, press response, and velocity. Component-specific
  thresholds, selection rules, and visual mapping remain caller-owned.
- `keepSizeWhenLookingAhead` preserves measured lookahead constraints; it does not replace a panel's
  own layout contract.

### Animation and predictive Back

- Use `Modifier.blurTransition` for optical progress only; caller owns layout and input.
- Use `Modifier.blurEnterExit` or `BlurEnterExit` when exiting content must remain composed until the
  registered visibility transition completes.
- Do not add a second opacity layer when an enclosing transition already owns fade. Use `fade = false`
  when the enclosing transition owns opacity.
- Respect accessibility policy getters rather than manually combining reduced-motion,
  reduced-transparency, and blur switches.
- `rememberInterpolatedShape` interpolates compatible Compose shapes. A shape that is not
  interpolatable may need an explicit fallback rather than assuming a continuous morph.
- `rewindTransition` replays the existing exit segment in reverse while preserving child timing; it
  does not redefine the caller's transition state contract.
- `SeekableTransitionNavigationEventHandler` seeks a `SeekableTransitionState` during predictive Back,
  animates a canceled gesture to the current state, and snaps to the back target on completion.
  NavigationEvent and state ownership stay with the caller.

## Preserve shape and Capsule boundaries

- `shape` owns structured geometry and numerical contour operations. `capsule` builds continuous
  rounded geometry on top of those contour contracts.
- `ContourShape` reports ordered closed contours made from lines, arcs, and cubic curves, or returns
  null when the shape has no structured form.
- Backdrop and other consumers may optimize structured contours, but must fall back to the shape's
  ordinary `Outline` when contour data is absent.
- Continuous geometry may normalize radii to fit measured bounds. Unknown, invalid, and zero radius
  measurements have distinct meanings; do not collapse them into one fallback value.
- Keep Capsule-specific geometry out of base Material and foundation runtime dependencies. Add the
  shape/Capsule or theming adapter module only where the application uses it.
- Physical corners remain screen-relative in LTR and RTL; logical corners follow layout direction.

## Use resources without leaking UI

- `Resource.Global` can be read without Compose or a platform context.
- `ComposeResource` resolves a value only inside composition.
- `Resource<T>.resolve()` handles global and Compose adapters, and fails when the resource has no
  Compose adapter. Do not assume every custom resource subtype is resolvable.
- Libraries and applications own resource declarations and packaging. Resource resolution does not
  transfer host or platform ownership.

## Material 3 and design-system boundary

The foundations are intentionally below the design system. When a foundation API emits visible
application UI, use the M3X design-system order:

```text
M3X Material > Material 3 Expressive > Material 3
```

`material3` integrates upstream Material behavior and does not turn a foundation primitive into an
M3X-styled component by itself. Use M3X Material for the styled, theme-aware entry point when one is
provided.

## Review checklist

1. Confirm the focused module, not the bundle, is the only new dependency required.
2. Confirm the caller owns scope, host, layout, gestures, and transition state where the helper does
   not explicitly take ownership.
3. Confirm state writes, retries, and cancellation follow the documented ownership rules.
4. Confirm accessibility and blur gates use policy getters.
5. Confirm structured shape consumers have an ordinary `Outline` fallback.
6. Confirm logical and physical shape semantics remain distinct.
7. Confirm resources use the correct global or Compose resolver.
8. Confirm visible UI still follows the M3X component order.

## Canonical guides

- [Architecture and module boundaries](../../../docs/architecture.md)
- [Shape system](../../../docs/design-system/shapes.md)
- [Accessibility, motion, and interaction light](../../../docs/design-system/accessibility-and-motion.md)
- [State and animation extension contracts](../../../docs/extension-contracts.md#state-and-animation)
- [Layout and gesture extension contracts](../../../docs/extension-contracts.md#layout-and-gestures)
- [Runtime state and Flow bridge](../../../modules/runtime/src/commonMain/kotlin/FlowAsMutableState.kt)
- [Auto-restorable state](../../../modules/runtime/src/commonMain/kotlin/AutoRestorableState.kt)
- [Grouped lazy list DSL](../../../modules/foundation/src/commonMain/kotlin/GroupedLazyList.kt)
- [Drag state](../../../modules/foundation/src/commonMain/kotlin/gestures/DampedDragState.kt)
- [Blur animation](../../../modules/animation/src/commonMain/kotlin/BlurAnimation.kt)
- [Interpolated shape](../../../modules/animation/src/commonMain/kotlin/InterpolatedShape.kt)
- [Seekable predictive Back](../../../modules/navigation-event/src/commonMain/kotlin/SeekableTransitionNavigationEventHandler.kt)
- [Material 3 contrast extension](../../../modules/material3/src/commonMain/kotlin/Contrast.kt)
- [Structured contour shape](../../../modules/shape/src/commonMain/kotlin/ContourShape.kt)
- [Capsule contours](../../../modules/capsule/src/commonMain/kotlin/Contour.kt)
- [Resource contract](../../../modules/resource-core/src/commonMain/kotlin/Resource.kt)
- [Compose resource resolution](../../../modules/resource-compose/src/commonMain/kotlin/ComposeResource.kt)
