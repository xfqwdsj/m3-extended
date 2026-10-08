# Design-system contracts

M3X extends Material 3 Expressive with design contracts consumed through the application's theme
and hosts. Applications own content and host lifetimes; components own their declared behavior.

## Contract map

| IDs | Contract | Specification |
| --- | --- | --- |
| T1–T5 | Theme ownership and scoped configuration | [Theme](theme.md) |
| S1–S6 | Shape ownership and geometry resolution | [Shapes](shapes.md) |
| M1–M7 | Surface appearance and source integration | [Materials](materials.md) |
| A1–A6 | Accessibility and motion policy | [Accessibility and motion](accessibility-and-motion.md) |
| L1–L2 | Shared interaction-light participation | [Accessibility and motion](accessibility-and-motion.md#shared-interaction-light) |
| C1–C6 | Component and host responsibilities | [Components](components.md) |

Each contract states a design responsibility, its observable behavior, and how callers customize
or compose it. Source and regression links identify its implementation and verification points.
Platform-specific behavior is verified on its corresponding runner.

## Read and customize in order

Start with the theme boundary, then choose shapes and material roles. Treat accessibility as an
input to resolved appearance. Add the page and overlay hosts before tuning individual surfaces.
For a local appearance change, override the relevant role or component parameter. For custom
rendering, preserve the same source, fallback, and interaction responsibilities described in
[backdrop integration](../backdrops.md).

A visual preset supplies optical values. It does not define an application color palette,
replace typography, or select the application's accessibility policy.
