# M3X documentation

M3X is an extended Material 3 Expressive design system for Compose Multiplatform. These guides
explain how applications use its design contracts and how modules share responsibility for their
implementation. Resource and persistence foundations also support applications outside Compose.

## Build an application

1. Follow [Getting Started](../README.md#getting-started) for dependencies and repositories, then choose modules in the [module table](../README.md#modules).
2. Read the [design-system contracts](design-system/README.md) before choosing theme overrides.
3. Use the [page setup](../README.md#backdrop-page-setup) to establish background and overlay ownership.
4. Follow [backdrop integration](backdrops.md) and [overlay integration](overlays.md) for custom surfaces.
5. Connect editable models through [settings](settings.md) and [persistence](persistence.md).

## Understand the design system

| Contract area | What it determines |
| --- | --- |
| [Theme composition](design-system/theme.md) | Material inputs, extension values, inheritance, and override ownership |
| [Shape system](design-system/shapes.md) | Logical roles, physical corners, adaptive radii, and component shape states |
| [Material system](design-system/materials.md) | Surface roles, optical effects, color, masks, adaptation, and fallback |
| [Motion and accessibility](design-system/accessibility-and-motion.md) | Live system preferences, visual response, animation boundaries, and shared lighting |
| [Component behavior](design-system/components.md) | Ownership of layout, appearance, state, input, and edit transactions |

Each area states design rules and identifies the source and regressions that implement them.
Contract identifiers give reviewers a stable name for a rule; implementation details and default
tuning remain owned by source.

## Work on the library

[Architecture](architecture.md) explains layers, dependency direction and integration ownership.
[Extension contracts](extension-contracts.md) describe public API extension contracts.
[Development](development.md) covers generation, verification, publication, and platform launchers.
[AGENTS.md](../AGENTS.md) contains the rules for repository changes.
