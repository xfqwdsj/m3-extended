---
name: m3x-material
description: Build consumer UI with M3X Material 3 Expressive themes, shapes, components, surface roles, and accessibility policy. Use when selecting or customizing M3X components, wrapping an application in M3xMaterialTheme, adapting Material 3 Expressive, or reviewing M3X/Material visual integration.
license: MIT
---

# M3X Material and Theme

Repository: https://github.com/xfqwdsj/m3-extended

Use this skill for M3X's design-system and component layer. Repository-relative links below point to
the canonical contracts; when installed outside the repository, resolve them against the repository
URL above.

## What this skill covers

The M3X Material layer provides:

- Material 3 Expressive-based components such as badges, buttons and button groups, cards, floating
  bars and toolbars, loading indicators, pull-to-refresh, search bars, selectable chips, sliders,
  split buttons, switches, and toggle buttons.
- M3X theme contracts and extension values layered over Material's native theme values.
- Adaptive and continuous shape roles, including optional Capsule geometry.
- Backdrop-aware Material surface roles and presets.
- Accessibility and motion policy integrated with Material's semantics and interaction ownership.
- Shared interaction lighting and renderer selection for components that opt into them.

Hosted overlays and Backdrop source setup are covered by `m3x-backdrop-overlay`. Generated settings
pages are covered by `m3x-settings`.

## Required component order

Use this order whenever the application adopts M3X:

```text
M3X Material > Material 3 Expressive > Material 3
```

1. Use the M3X-provided component, wrapper, or surface when it represents the same responsibility.
2. If M3X does not provide it, use the Material 3 Expressive component or API.
3. If no suitable Material 3 Expressive form exists, use Material 3.

M3X components are customizations or adaptations built above Material 3 Expressive. Native Material
still owns its design-system values, component semantics, sizing, layout, and interaction behavior.
Do not mix another design system's controls into an M3X application and expect M3X's visual contract
to hold.

## Choose the theme boundary

Use `M3xMaterialTheme` for the complete M3X root. It wraps `MaterialExpressiveTheme`, installs M3X
extension values, selects the adaptive Capsule preset at the root by default, and provides renderer
and interaction-lighting adapters.

```kotlin
import top.ltfan.m3x.material.bundle.M3xMaterialTheme

@Composable
fun App() {
    M3xMaterialTheme {
        AppContent()
    }
}
```

Use `M3xBaseTheme` when the application needs the independent rounded theme composition without
selecting rendering adapters. Use a nested M3X theme for scoped theme overrides; respect the input
inheritance rules instead of rebuilding parent values manually.

Inside an existing Material 3 Expressive composition, preserve the caller's theme and add only the
M3X adapters or theme layer the application explicitly needs. `ProvideM3xMaterialRendering` installs
the rendering adapters; `M3xMaterialTheme` already includes them. For complete design-system values,
prefer the integrated theme rather than expecting baseline extension fallbacks to match it.

## Preserve theme ownership

- Material owns `colorScheme`, typography, motion scheme, and the eight native shape roles.
- M3X owns its extension values exposed through `MaterialTheme.extended`.
- Do not copy native Material roles into M3X extension roles or replace one owner implicitly.
- An explicit `M3xShapes` preset supplies native and extension shapes and wins over factory or radius
  policy arguments.
- A nested theme without shape arguments inherits current native roles and M3X extension shapes.
- An explicit component shape or slot wins over theme defaults.
- For a caller-owned container, use the M3X surface appearance APIs rather than recreating a
  component's internal rendering path.

Coordination matters more than isolated replacements. Change one complete shape preset, one theme
role, or one surface's explicit appearance through its documented scope.

## Shape rules

- Native Material shape roles and M3X extension shapes have separate owners. Preserve both.
- `M3xShapeFactory.logical` is direction-aware; `physical` keeps screen-relative corners in LTR and
  RTL.
- `RoundedM3xShapeFactory` uses standard rounded geometry; `CapsuleM3xShapeFactory` uses continuous
  rounded geometry.
- Unknown adaptive measurements use the documented fallback. Zero is a real measured radius and
  must not be treated as missing.
- Interactive controls retain Material's pressed, selected, dragged, and connected-control behavior.
  M3X changes the theme-aware silhouette, not the interaction state model.
- For nested container-like surfaces, inset physical radii by matching padding and clamp at zero.

## Choose a material role by purpose

When a component or caller-owned surface explicitly selects Backdrop appearance, use the role that
matches the content:

| Role | Use |
| --- | --- |
| `Control` | Compact interactive glass with vibrancy, lensing, and exterior shadow. |
| `Panel` | Larger containers with stronger blur and depth lensing. |
| `Surface` | Explicit decorative content backing; elevation remains with the consuming component. |
| `InversePanel` | Transient feedback using the inverse Material color pair. |
| `ScrollEdge` | Progressive separation over scrolling content. |

General content surfaces begin with native Material color, elevation, and border. Select a
renderer-backed role only when the design needs source processing, optical decoration, shared light,
or a custom Backdrop graph. Effect precedence and preset inheritance are renderer contracts; do not
replace a whole chain when overriding one role or one surface is sufficient.

## Accessibility and motion

- Keep static `AccessibilityPreferences` separate from live `SystemAccessibilitySettings`.
- `ProvideAccessibility` observes and resolves platform state; nested themes inherit and resolve
  their own preferences without duplicating the platform reader.
- Read effective policy through the inline getters such as `SpatialMotionEnabled`,
  `BackgroundTransparencyEnabled`, `IncreasedContrastEnabled`, and `BackdropBlurEnabled`.
- Explicit high-contrast palettes and custom content colors remain the application's responsibility.
- Reduced transparency changes material appearance; it does not remove host or source ownership.
- Reduced motion replaces spatial presentation with fading where the documented component contract
  requires it. Preserve direct dragging and material interaction semantics.
- Foreground blur is an appearance layer, not a layout or input owner.

## Component review checklist

1. Confirm the component is the M3X entry point when M3X provides that responsibility.
2. Confirm the app is inside a Material 3 Expressive-compatible theme boundary.
3. Confirm native Material values remain native and M3X extensions are read through the M3X API.
4. Confirm caller-supplied shapes, colors, content, and slots retain precedence.
5. Confirm semantics, sizing, layout, and gestures remain Material-owned unless the M3X component
   explicitly defines otherwise.
6. Confirm reduced transparency, increased contrast, and reduced motion use policy getters rather
   than being manually merged.
7. Confirm a renderer-backed surface has a documented fallback when no Backdrop source is available.

## Canonical guides

- [Theme composition](../../../docs/design-system/theme.md)
- [Shape system](../../../docs/design-system/shapes.md)
- [Material system](../../../docs/design-system/materials.md)
- [Accessibility, motion, and interaction light](../../../docs/design-system/accessibility-and-motion.md)
- [Component responsibilities](../../../docs/design-system/components.md)
- [Public components and module table](../../../README.md#modules)
- [Theme integration](../../../README.md#theme-integration)
- [Theme root implementation](../../../modules/material-bundle/src/commonMain/kotlin/M3xMaterialTheme.kt)
- [Base theme](../../../modules/material-theming/src/commonMain/kotlin/M3xBaseTheme.kt)
- [Theme extension values](../../../modules/material-theme/src/commonMain/kotlin/M3xThemeExtensions.kt)
- [Capsule theme factories](../../../modules/material-theming-capsule/src/commonMain/kotlin/CapsuleShapes.kt)
- [Structured shape geometry](../../../modules/shape/src/commonMain/kotlin/ContourShape.kt)
- [Material 3 contrast extension](../../../modules/material3/src/commonMain/kotlin/Contrast.kt)
