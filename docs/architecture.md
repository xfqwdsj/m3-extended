# Architecture and module boundaries

M3X separates reusable Compose foundations, design-system contracts, implementations and integrations.
A module's boundary follows its abstraction and dependency direction. Concrete APIs illustrate those
boundaries; they do not define an exhaustive or final feature set.

[settings.gradle.kts](../settings.gradle.kts) declares the modules. Each module owns its targets,
source sets, direct dependencies, generation and publication configuration. The
[README module table](../README.md#modules) is the overview for choosing dependencies.

## Compose foundations

`runtime`, `ui`, `animation-core`, `animation` and `foundation` extend the corresponding Compose
layers. Runtime integration remains independent of UI; UI and animation foundations remain
independent of the M3X design system. `foundation` supports unstyled UI and does not depend on
Material, Backdrop or the M3X navigation adapter.

Integration with a separately owned framework belongs at an explicit dependency boundary.
`material3` integrates upstream Compose Material 3, and `navigation-event` integrates NavigationEvent.
`ui-platform-private` is an opt-in platform integration; public UI infrastructure does not depend on
private platform providers. Dependencies introduced by upstream Compose remain owned by upstream.

Sources: [runtime](../runtime/build.gradle.kts), [UI](../ui/build.gradle.kts),
[animation foundations](../animation-core/build.gradle.kts),
[UI animation](../animation/build.gradle.kts), [foundation](../foundation/build.gradle.kts),
[Material integration](../material3/build.gradle.kts), and
[NavigationEvent integration](../navigation-event/build.gradle.kts).

## Presentation and rendering

`overlay` is the design-independent presentation layer. `backdrop` integrates Backdrop with Compose
UI independently of Material and overlay presentation. `overlay-backdrop` joins those layers through
the host's extension contract. Design-specific components and rendering adapters consume these
foundations rather than introducing parallel host ownership.

Applications own the enclosing hosts. Components consume those hosts, and adapters participate in
their lifecycle. The integration must preserve the host's ordering, input and transition contracts.
[Overlay integration](overlays.md), [Backdrop integration](backdrops.md) and
[extension contracts](extension-contracts.md#custom-presentation-and-rendering) describe the concrete
APIs and behavior.

Sources: [overlay layer](../overlay/build.gradle.kts),
[Backdrop integration](../backdrop/build.gradle.kts), and
[host adapter](../overlay-backdrop/build.gradle.kts).

## Design system and optional integrations

`material-theme` defines M3X theme contracts. `material` implements Material 3 Expressive components
against those contracts and caller-selected rendering. Their production dependencies exclude the
Backdrop integration, overlay layer, Capsule and private platform providers.

`material-overlay` integrates Material components with the presentation layer. `material-backdrop`
integrates Material rendering with Backdrop. Renderer-specific configuration remains with its
integration, while base design contracts remain usable independently.

`material-theming` integrates M3X and Material theme configuration. `material-theming-capsule` adds
Capsule integration. `material-bundle` composes the design system and its integrations into the
complete application entry point. Consumers can choose this composition or select independent layers.
Private platform integration remains an explicit application choice.

The [theme contracts](design-system/theme.md) specify defaults and inheritance. The
[material contracts](design-system/materials.md) specify rendering behavior. These contracts belong to
the APIs implementing them, rather than narrowing the scope of a shared module.

Sources: [theme contracts](../material-theme/build.gradle.kts),
[components](../material/build.gradle.kts), [Material presentation](../material-overlay/build.gradle.kts),
[Material rendering integration](../material-backdrop/build.gradle.kts),
[theming](../material-theming/build.gradle.kts),
[Capsule integration](../material-theming-capsule/build.gradle.kts), and
[complete composition](../material-bundle/build.gradle.kts).

## Resources

`resource-core` defines resource abstractions independently of UI. `resource-compose` integrates them
with Compose. Resource consumers choose the adapter appropriate to their execution environment.

## Settings and persistence

`settings-page-core` defines UI-independent settings contracts. Compose and Material integration
belong to `settings-page-compose` and `settings-page-material`. Settings contracts remain independent
of storage implementation; `settings-page-datastore` joins them to DataStore persistence.

`datastore-core` provides persistence foundations built on AndroidX DataStore. Compose and
serialization integration belong to `datastore-compose` and `datastore-serialization`. Applications
own persistence hosts and file selection. A UI adapter consumes that ownership rather than creating
another persistence lifetime.

The [settings guide](settings.md), [persistence guide](persistence.md) and
[extension contracts](extension-contracts.md#custom-settings-renderers) describe API-level ownership
and integration requirements.

Sources: [resource abstractions](../resource-core/build.gradle.kts),
[Compose resources](../resource-compose/build.gradle.kts),
[settings contracts](../settings-page-core/build.gradle.kts),
[settings persistence integration](../settings-page-datastore/build.gradle.kts), and
[persistence foundations](../datastore-core/build.gradle.kts).

## Tooling

`datastore-processor` and `settings-page-processor` provide code generation for their domains.
`processor-support` is shared processor infrastructure and stays outside production runtime
dependencies. Modules consuming generated declarations own their producer dependencies and generated
source inclusion. See [development](development.md#preserve-generation-dependencies).

`compose-test-support` provides shared Compose test infrastructure. Tests live with the layer they
verify. Integration tests may depend on the complete composition without adding it to a lower
layer's production dependencies. Runner-specific constraints belong in
[development](development.md#test-targets).

## Demo and platform applications

`demo` is the shared application. Platform modules own its entry points and platform integration.
Application policy stays in the application; reusable infrastructure belongs to the appropriate
library layer when it has an independent contract.

## Package and source ownership

Kotlin packages follow module ownership. Android namespaces match their module's production package
root. Each Kotlin source root omits the longest common prefix of its package declarations and retains
only subpackage suffix directories. Tests use their module's package root.

Material integration packages distinguish `material.overlay`, `material.backdrop`, `material.bundle`
and `material.theming.capsule` under `top.ltfan.m3x`. Platform entry points use `demo.android`,
`demo.desktop`, `demo.ios`, `demo.macos` and `demo.web`. These package boundaries keep ownership
explicit as the implementations grow.
