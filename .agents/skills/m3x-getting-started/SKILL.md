---
name: m3x-getting-started
description: Add M3X to a Compose Multiplatform consumer project, align artifacts with the BOM, choose the smallest module composition, and route implementation work through M3X design-system contracts. Use when adopting M3X, selecting dependencies, deciding between M3X and Material APIs, or auditing whether an app follows the required component order.
license: MIT
---

# M3X Getting Started

Repository: https://github.com/xfqwdsj/m3-extended

Use this skill for consumer-side adoption. The repository-relative links below point to the canonical
guides; when this skill is installed outside the repository, resolve those paths against the
repository URL above.

## What this skill covers

M3X is an extended Material 3 Expressive design system for Compose Multiplatform. It provides:

- Material components, themes, shapes, overlays, Backdrop rendering, and accessibility-aware motion.
- Structured settings models and editors.
- A model-first structured persistence system built on AndroidX DataStore.
- Reusable Compose foundations, resources, shape/Capsule geometry, animation, and navigation-event
  integration that can be consumed without the complete Material bundle.

Use the narrower M3X skills after choosing the relevant system.

## Required component order

Before adding or selecting UI:

1. Use the M3X-provided component or adapter when M3X provides the needed responsibility.
2. Otherwise use the Material 3 Expressive component or API.
3. Only when no suitable Material 3 Expressive form exists, use Material 3.

This is the visual compatibility contract:

```text
M3X Material > Material 3 Expressive > Material 3
```

Every component M3X proactively provides is an M3X customization or adaptation built over Material 3
Expressive. M3X does not replace Material 3 Expressive with an unrelated base design system. If a
component is not provided by M3X, native Material remains the intended fallback, subject to the order
above.

Do not mix another design system's equivalent controls into an application presented as using the M3X
design system. Visual behavior is not guaranteed outside this order and the documented theme and host
contracts.

## Add dependencies

Use the M3X BOM to align versions. Declare dependencies in the source set that consumes the APIs.

```kotlin
dependencies {
    implementation(platform("top.ltfan.m3x:m3x-bom:VERSION"))
    implementation("top.ltfan.m3x:material-bundle")
}
```

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project.dependencies.platform("top.ltfan.m3x:m3x-bom:VERSION"))
            implementation("top.ltfan.m3x:material-bundle")
        }
    }
}
```

The consumer repository needs `mavenCentral()` and `google()`. Do not invent versions per module when
the BOM is available.

## Choose the smallest useful composition

| Need | Start with |
| --- | --- |
| Complete Material composition and renderer adapters | `material-bundle` |
| M3X components inside an application-managed theme composition | `material`, `material-theme`, `material-theming`, then add only the adapters needed |
| Capsule shape factories | `material-theming-capsule` in addition to the selected theming composition |
| Hosted overlays | `overlay`, `material-overlay`, and the application's `OverlayHost` |
| Backdrop rendering | `backdrop`, `material-backdrop`, and `overlay-backdrop` when overlay source stacking is needed |
| Settings contracts and Material page | `settings-page-core`, `settings-page-compose`, `settings-page-material` |
| Settings bound to generated DataStore models | above plus `settings-page-datastore`, `datastore-core`, and processors |
| Structured persistence independent of Compose | `datastore-core`, `datastore-serialization`, `datastore-processor`; add `datastore-compose` only for snapshot observation |
| Reusable non-Material foundations | `runtime`, `ui`, `foundation`, `animation-core`, `animation`, `navigation-event`, `shape`, or `capsule` as required |
| UI-independent or Compose resources | `resource-core` and `resource-compose` as required |

Prefer targeted modules when the application already owns its theme or host composition. Use
`material-bundle` when the application wants the complete integrated entry point.

## Establish the root boundaries

For the complete M3X composition, wrap application UI in `M3xMaterialTheme`. It installs the
Material 3 Expressive-based theme, M3X extension values, renderer adapters, and shared interaction
lighting.

```kotlin
import androidx.compose.runtime.Composable
import top.ltfan.m3x.material.bundle.M3xMaterialTheme

@Composable
fun App() {
    M3xMaterialTheme {
        // Application content
    }
}
```

Applications own composition lifetimes and host lifetimes. Components consume the surrounding theme
and hosts; they must not create their own `OverlayHost`, `BackdropHost`, or competing persistence
host.

Required boundaries:

- Put `BackdropHost` outside `OverlayHost` when overlays sample Backdrop pixels.
- Create one remembered `OverlayHostState` per host region.
- An interactive overlay without an enclosing `OverlayHost` is a configuration error, including when
  its visibility is false. Inspection mode is the documented exception.
- Applications own `StoreHost`, `StoreFile`, file paths, and host replacement.
- Add KSP processors only to every compilation that needs generated declarations. DataStore model
  generation also requires the Kotlin serialization plugin.

## Consumer workflow

1. Identify the consumer targets and source set that will import M3X.
2. Add the BOM and the minimum modules for the task.
3. Select one theme boundary: `M3xMaterialTheme` for the complete composition, or the documented
   lower-level theming modules.
4. Add `BackdropHost`, `OverlayHost`, or `StoreHost` only when the requested feature needs them.
5. Follow the component order above for every UI choice.
6. Add processors and generated-source wiring for settings or persistence models.
7. Validate the affected platform. JVM success does not establish Android, Web, or Apple behavior.

## Important module distinctions

- `material` contains Material 3 Expressive components and does not include Backdrop, overlay
  presentation, or Capsule in its production dependency set.
- `material-bundle` is the integrated composition; it is not required for lower-level modules.
- `overlay` is design-independent. `material-overlay` adapts Material presentation to it.
- `backdrop` owns source capture and rendering primitives. `material-backdrop` owns Material
  appearance policy. `overlay-backdrop` joins Backdrop to overlay stacking.
- `settings-page-core` and `datastore-core` stay independent of Compose and Material.
- `datastore-processor`, `settings-page-processor`, and `processor-support` are compiler-side modules,
  not runtime UI dependencies.
- `resource-core` is UI-independent; `resource-compose` supplies Compose resolution.

## Canonical guides

- [Getting started, module table, theme and page setup](../../../README.md#getting-started)
- [Documentation map](../../../docs/README.md)
- [Architecture and module boundaries](../../../docs/architecture.md)
- [Design-system contract map](../../../docs/design-system/README.md)
- [Release groups and consumer version alignment](../../../docs/releases.md#align-consumer-versions)
- [Settings setup](../../../docs/settings.md)
- [Structured persistence setup](../../../docs/persistence.md)
- [Overlay integration](../../../docs/overlays.md)
- [Backdrop integration](../../../docs/backdrops.md)
- [Processor generation dependencies](../../../docs/development.md#preserve-generation-dependencies)
