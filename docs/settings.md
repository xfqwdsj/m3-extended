# Settings models and editors

## Define a tree independently of UI

`settings-page-core` models settings without Compose or Material. A `SettingsTree` supplies items;
optional `SettingsPersistence` supplies persistence status/retry. `SettingsStoreDescriber` combines
both for generated store-backed descriptions. A handwritten tree can expose dynamic items,
options, visibility, and numeric bounds. Observable getters make those changes visible to UI.
`CompositeSettingsTree` combines generated and handwritten sources while retaining their sources.

`Resource<T>` labels keep the tree toolkit-independent. Use `resource-compose` adapters to resolve
Compose strings and painters at rendering time. See [resource boundaries](architecture.md#resources).

Source: [tree and item contracts](../settings-page-core/src/commonMain/kotlin/Settings.kt).
Example: [handwritten demo tree](../demo/src/commonMain/kotlin/ManualSettingsTree.kt).

## Generate editable descriptions

Annotate a data class with `@SettingsStore` and readable primary-constructor properties with
`@SettingsItem`. Both `val` and `var` work. An accessible `copy` function is required for immutable
updates, including category properties and fields inside sealed choices. Body/computed properties
cannot serve as generated editable fields; diagnostics identify the original property.

The processor creates `<Model>Describer` and `MutableStore<Model>.settingsDescriber()`. Keep the
resulting describer instance stable while the page is displayed. Apply KSP to every platform
compilation that references generated declarations (for example `kspJvm` and `kspJs`), or follow the
repository demo's common-metadata generation wiring when generating shared source.

`SettingsResetMode.None` is the default. `Menu` generates a fresh model's default for each item and
requires an accessible no-argument model constructor. Material rows expose reset by long press or
secondary click only for eligible visible, enabled items.

Sources: [annotations](../settings-page-core/src/commonMain/kotlin/annotation/Annotation.kt),
[property validation](../settings-page-processor/src/main/kotlin/EditableSettingsProperty.kt), and
[generation](../settings-page-processor/src/main/kotlin/SettingsStoreFileGenerator.kt).

## Mount the page inside the application theme and host

The existing setup uses the generated platform extension:

```kotlin
@Composable
fun ProfileSettingsPage(store: MutableStore<ExampleSettingsStore>) {
    val describer = remember(store) { store.settingsDescriber() }
    val overlayState = remember { OverlayHostState() }
    MaterialTheme {
        OverlayHost(overlayState) {
            CardSettings(describer)
        }
    }
}
```

This preserves the original host example; the Material component support contract expects an
Expressive theme supplied by the application. Imports and the application's generated model are
omitted. Add `customEditor` for `@CustomSettingsItem(rendererId = ...)`; it receives a draft editor
contract. The demo's settings catalog supplies an accent editor as a reference.

`CardSettings` draws grouped cards, uses stable IDs for row/editor transitions, and consumes the
shared transition scope supplied by `OverlayHost`. Set `cardBackdrop = false` for direct card colors;
`cardMaterial` overrides the grouped backing. It installs neither a theme nor a host.

## Choose edit timing and numeric representation

Switches submit immediately. Sliders submit on gesture completion unless
`sliderCommitMode = SliderCommitMode.WhileDragging`; live updates include keyboard adjustment.
When `allowInput` is enabled, clicking the row opens precise input. Other built-in editors keep a
draft until Save; Cancel, Back, and outside dismissal discard it. `SettingsEditSession` supplies
observable draft/validation state independently of the Material renderer.

For handwritten numeric items choose `NumberType.finiteInt`, `finiteLong`, `finiteFloat`,
`finiteDouble`, or the `input...` factories. Generated descriptions infer representation from the
property declaration. Numeric kind must be explicit because JS represents several Kotlin number
types as JavaScript numbers. Normalized slider values map through that descriptor.

Discrete stops must correspond to distinct, evenly spaced raw values on the target, with a maximum
of 4096 intervals. Selector/Wheel numeric hints currently render as a discrete Slider where valid,
or a NumberStepper otherwise, preserving model bounds and step validation.

Sources: [number validation](../settings-page-core/src/commonMain/kotlin/SettingsNumbers.kt),
[slider mapping](../settings-page-core/src/commonMain/kotlin/NumberSliderMapping.kt), and
[draft sessions](../settings-page-compose/src/commonMain/kotlin/SettingsEditSession.kt).
Regressions: [numeric semantics](../settings-page-core/src/commonTest/kotlin/SettingsNumbersTest.kt)
and [mapping](../settings-page-core/src/commonTest/kotlin/NumberSliderMappingTest.kt).

## Own error presentation and recovery

UI adapters use `SettingsCommit.tryCommit()` and its draft/reset variants. Validation and local
write acceptance are distinct from durable saving. `CardSettings`, `SettingsRow`, and public
editors report structured `SettingsPresentationError` values through `onError`; they do not display errors
automatically. With no callback those notifications are ignored.

Read/write failures retain `SettingsSource` and the original Throwable. Edit/configuration failures
identify the item. Persistence callbacks occur once per observed status transition; failed user
actions report per action. Retry a failed write with `error.source.persistence?.retrySave()`;
replace a host after a failed read as described in [persistence](persistence.md).

`localizedMessage()` is an optional composable helper for application-owned UI. It translates
validation text and excludes persistence exception messages. Built-in labels include English and
Simplified Chinese; override `CardSettingsLabels` to customize them.

```kotlin
var error by remember { mutableStateOf<SettingsPresentationError?>(null) }
Column {
    CardSettings(describer, onError = { error = it })
    error?.let { Text(it.localizedMessage()) }
}
```

The reusable row, item editor, number/range editor, and stepper accept a root `Modifier`. Action
callbacks precede optional arguments; use named arguments for callbacks and modifiers.

Sources: [structured errors](../settings-page-compose/src/commonMain/kotlin/SettingsPresentationError.kt)
and [commit path](../settings-page-core/src/commonMain/kotlin/SettingsCommit.kt).
Regressions: [commit validation](../settings-page-core/src/commonTest/kotlin/SettingsCommitTest.kt)
and [Material editors](../settings-page-material/src/nonBrowserTest/kotlin/CardSettingsTest.kt).

## Persistence adapter and custom renderers

Store-bound generated descriptors require an explicit dependency on `settings-page-datastore`.
The adapter maps `StoreStatus` to the settings-owned `SettingsPersistenceStatus` without launching
another collector. The settings model and Material renderer do not depend on DataStore.
Structured editor failures live in `settings-page-compose` as `SettingsPresentationError`; Material
localization remains a renderer extension. See [extension contracts](extension-contracts.md).

For generated settings without DataStore, use `@SettingsStore(bindDataStore = false)` and pass a
`SettingsModel<T>` implementation to the generated descriptor. The model supplies its current value
and atomic update function; the generated tree has no persistence status or retry binding. The
annotation defaults to the existing DataStore-bound generation path. This option does not change
local commit or default-reset semantics.
