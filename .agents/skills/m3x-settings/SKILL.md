---
name: m3x-settings
description: Build M3X settings pages and generated settings models with validated edits, draft sessions, numeric mapping, structured errors, and optional DataStore status and retry binding. Use when adding app preferences, custom settings renderers, settings KSP generation, or migrating handwritten settings UI to M3X.
license: MIT
---

# M3X Settings

Repository: https://github.com/xfqwdsj/m3-extended

Use this skill for the settings contract and UI layer. Repository-relative links below point to the
canonical guides; when installed outside the repository, resolve them against the repository URL
above.

## What this skill covers

The settings system provides:

- UI-independent settings trees, item contracts, sources, visibility, dependencies, and numeric
  descriptors.
- Generated settings descriptions from annotated Kotlin data classes.
- Compose edit sessions, draft validation, commit results, and structured presentation errors.
- A Material 3 Expressive settings page, rows, sliders, ranges, steppers, selectors, switches, text
  editors, category headings, and custom editor slots.
- An optional DataStore adapter that exposes persistence status and retry without launching a second
  collector.
- A storage-independent mode for applications that supply their own `SettingsModel<T>`.

Structured model persistence itself belongs to `m3x-persistence`. Keep settings contracts and storage
implementation separate.

## Required component order

```text
M3X Material > Material 3 Expressive > Material 3
```

Use `SettingsPage`, generated M3X settings rows, and M3X custom editor primitives when they cover the
responsibility. If extending the UI outside that layer, use Material 3 Expressive next, then
Material 3. Do not introduce another design system's settings controls and expect M3X visual and
interaction contracts to remain valid.

## Choose the settings layer

| Need | Modules |
| --- | --- |
| UI-independent settings model and validation | `settings-page-core` |
| Compose edit sessions and structured errors | `settings-page-compose` |
| Material 3 Expressive page and built-in editors | `settings-page-material` |
| Generated descriptors | `settings-page-processor` and KSP |
| DataStore status/retry binding | `settings-page-datastore` plus the persistence modules |
| Resource-agnostic labels | `resource-core`; add `resource-compose` for composition resolution |

## Generate editable descriptions

Annotate the model with `@SettingsStore` and editable primary-constructor properties with
`@SettingsItem`.

Requirements:

- Editable properties must be readable primary-constructor properties.
- The data class must expose an accessible `copy` function for immutable updates, including category
  properties and fields inside sealed choices.
- Body properties and computed properties cannot be generated editable fields.
- Keep generated describer instances stable while the page is displayed.
- Apply KSP to every platform compilation that references generated declarations, or use a documented
  common-metadata producer setup.
- Use `SettingsResetMode.None` unless reset semantics are wanted. `Menu` generates item defaults from
  a fresh no-argument model and therefore requires an accessible constructor with required defaults.
- `@CustomSettingsItem(rendererId = ...)` must be paired with `@SettingsItem`; the page routes that
  item to the caller's custom editor.

For handwritten trees, use the public `SettingsTree` and `SettingsItem` contracts. Use
`CompositeSettingsTree` to combine generated and handwritten sources while retaining each source's
ownership.

## Mount the page correctly

The page consumes the enclosing theme and overlay host. For an M3X application, keep the generated
describer stable and mount the page inside the M3X theme and `OverlayHost`.

```kotlin
@Composable
fun AppSettingsPage(store: MutableStore<AppSettings>) {
    val describer = remember(store) { store.settingsDescriber() }
    val overlayState = remember { OverlayHostState() }

    M3xMaterialTheme {
        OverlayHost(overlayState) {
            SettingsPage(describer)
        }
    }
}
```

Do not wrap the page in a second `M3xMaterialTheme`, rerun KSP generation during composition, or
recreate the describer on each recomposition. Settings editors use the enclosing shared transition
state and item identities.

## Preserve edit transaction semantics

Use the built-in commit path; do not assign model fields directly from UI.

- Switches submit immediately.
- Sliders submit on gesture completion by default. Use `SliderCommitMode.WhileDragging` only when live
  updates are intended. Keyboard adjustment follows the configured behavior.
- Built-in modal editors keep a draft until Save.
- Cancel, Back, and outside dismissal discard the draft and do not commit it.
- `SettingsEditSession` exposes draft, validation, and reset behavior for custom renderers.
- A local commit result means the write was accepted into the local persistence queue. It does not
  prove disk durability.
- Stable item IDs connect rows, editors, shared text, and retained shared-transition content.
- Keep labels, descriptions, and selected values on both row and editor endpoints using the provided
  shared-text mechanism when the same text appears in both places.
- Reduced motion must continue to remove or simplify shared-text transitions as documented.

## Represent numbers explicitly

For handwritten numeric items, choose `finiteInt`, `finiteLong`, `finiteFloat`, `finiteDouble`, or the
documented `input...` factory. Numeric kind must be explicit because JavaScript represents several
Kotlin numeric types with one runtime number type.

- Slider/range stops must map to distinct, evenly spaced raw values.
- Respect the documented maximum interval count.
- Preserve model bounds and step validation.
- Selector or wheel hints may render as a discrete slider when valid, otherwise as a number stepper.
- Keep pair range types matching the declared numeric kinds.

Generated descriptors infer numeric representation from property declarations.

## Own error presentation and recovery

Settings UI reports structured errors; it does not silently substitute generic text.

- Serve `SettingsPage`, public rows, and public editors' `onError` callbacks from application-owned
  UI.
- Validation/configuration failures identify the affected item.
- Persistence callbacks occur once per observed status transition; failed user actions report per
  action.
- `localizedMessage()` is an optional composable helper for app-owned presentation. It excludes
  persistence exception messages.
- Retry a failed write through the structured error's persistence source and `retrySave()`.
- Replace a host after an unrecoverable read failure using the persistence lifecycle rules.
- A missing callback means the notification is ignored; do not assume the library displays it.

## Keep persistence optional

Use `@SettingsStore(bindDataStore = false)` when the generated tree should consume an application-owned
`SettingsModel<T>` instead of a generated store-bound descriptor. That mode has no persistence status
or retry binding, but local validation and reset semantics remain the same.

When DataStore binding is wanted, add `settings-page-datastore` explicitly. The adapter maps
`StoreStatus` to `SettingsPersistenceStatus` and preserves the source flow's timing and retry path.
The settings core and Material renderer remain independent of DataStore.

## Review checklist

1. Confirm the model is a primary-constructor data class with accessible `copy`.
2. Confirm KSP is applied everywhere generated declarations are referenced.
3. Confirm the describer is stable.
4. Confirm the page is inside an M3X Material theme and the required `OverlayHost`.
5. Confirm edit timing follows the selected control behavior.
6. Confirm Save/Cancel/Back/outside-dismiss semantics use the settings commit path.
7. Confirm numeric kinds and bounds are explicit and representable on every target.
8. Confirm errors reach app-owned presentation and retries use the structured source.
9. Confirm DataStore binding is optional and not leaked into core/UI contracts.

## Canonical guides

- [Settings guide](../../../docs/settings.md)
- [Structured persistence guide](../../../docs/persistence.md)
- [Settings component contract](../../../docs/design-system/components.md#c6-settings-separate-immediate-edits-from-modal-transactions)
- [Custom settings renderer contracts](../../../docs/extension-contracts.md#custom-settings-renderers)
- [Annotation and item contracts](../../../modules/settings-page-core/src/commonMain/kotlin/annotation/Annotation.kt)
- [Settings commit path](../../../modules/settings-page-core/src/commonMain/kotlin/SettingsCommit.kt)
- [Edit session](../../../modules/settings-page-compose/src/commonMain/kotlin/SettingsEditSession.kt)
- [Structured presentation errors](../../../modules/settings-page-compose/src/commonMain/kotlin/SettingsPresentationError.kt)
- [Material settings page](../../../modules/settings-page-material/src/commonMain/kotlin/SettingsPage.kt)
- [DataStore adapter](../../../modules/settings-page-datastore/src/commonMain/kotlin/SettingsPersistence.kt)
- [Generated settings model example](../../../modules/demo/src/commonMain/kotlin/DemoPreferences.kt)
- [Settings page example](../../../modules/demo/src/commonMain/kotlin/DemoPages.kt)
