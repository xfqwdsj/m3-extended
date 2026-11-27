---
name: m3x-persistence
description: Use M3X structured persistence for typed Kotlin models with AndroidX DataStore, KSP-generated store definitions and delegates, serialization codecs, optimistic observable state, initialization, status, and failure recovery. Use when an app needs durable structured settings or state rather than an untyped key-value wrapper.
license: MIT
---

# M3X Structured Persistence

Repository: https://github.com/xfqwdsj/m3-extended

Use this skill when modeling durable application state with M3X. Repository-relative links below
point to the canonical guide and implementation; when installed outside the repository, resolve them
against the repository URL above.

## What this skill covers

M3X's persistence system is a model-first structured store built on AndroidX DataStore. It provides:

- `StoreDefinition` values that bind a stable name, default typed model, and byte codec.
- `StoreHost` ownership that caches one DataStore and observable store per stable name.
- `MutableStore` with optimistic visible state, ordered transforms, asynchronous persistence,
  initialization waiting, status, and retry.
- Compose snapshot observation through `datastore-compose`.
- A JSON serialization adapter through `datastore-serialization`.
- KSP generation of `<Model>Store` definitions and constructor-property delegates through
  `@Store`, plus read-only or writable derived delegates through `@Derived`.
- An optional settings adapter that projects persistence status and retry without changing ownership.

This is structured persistence of typed models, not a thin preferences facade.

## Required component order for persistence-backed UI

When the stored state is displayed with UI:

```text
M3X Material > Material 3 Expressive > Material 3
```

Persistence does not own presentation. Use the settings and Material skills for the UI layer, but
do not switch to another design-system component stack when presenting M3X application state.

## Choose the modules

| Need | Modules |
| --- | --- |
| Core definitions, host, mutable store | `datastore-core` |
| Compose-observable `MutableStore.value` | `datastore-compose` |
| JSON `StoreCodec` helper | `datastore-serialization` |
| Generated definitions and delegates | `datastore-processor` plus KSP and the serialization plugin |
| Settings status/retry projection | `settings-page-datastore` |
| Non-Compose cell observation | `datastore-core` with `ValueCellFactory.Plain` |

Align M3X module versions through the BOM.

## Establish one owner

Applications own the host and file-selection policy.

- One `StoreHost` owns a set of stores and creates their DataStores.
- One store name belongs to exactly one `StoreDefinition` instance in that host.
- Recurring requests for the same definition instance share one DataStore and `MutableStore`.
- A different definition object with the same name is rejected even when its model type matches.
- Keep the definition name, default value, and codec stable for the definition's lifetime.
- Avoid simultaneous hosts owning the same backing file.
- The application supplies `StoreFile`, which supplies the platform storage and path choice.
- `DataStoreStore` is the standard supported `Store` adapter. `FlowStore` is an integration/test seam,
  not evidence that arbitrary non-AndroidX backends are supported by the persistence foundation.

Before replacing a host after an unrecoverable read failure, call `closeAndJoin()` and wait for the
old resources to release. `close()` cancels without waiting.

## Use structured model generation

A generated model typically looks like this:

```kotlin
@Serializable
@Store(name = "app-state", codec = AppJsonCodec::class)
data class AppState(
    val enabled: Boolean = true,
    val title: String = "",
)

object AppJsonCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun <T> create(serializer: KSerializer<T>): StoreCodec<T> =
        jsonStoreCodec(serializer, json)
}
```

Consumer compilation must apply `datastore-processor` through KSP. The consuming module must also
apply the Kotlin serialization plugin because generated definitions reference the model serializer.
The codec factory and optional derivation provider are explicit annotation inputs.

The processor generates:

- `<ClassName>Store`, implementing `StoreDefinition`.
- A delegate for each constructor property.
- Read-only delegates for readable `@Derived` computed properties.
- Writable delegates for documented pure `withXxx`-style transformations with a selected getter.

Generated model delegates and settings UI commit bindings are different layers. Settings validation
must use the settings commit path rather than assuming a generated model setter has performed UI
validation.

## Observe state in the right environment

For Compose, create a snapshot-observable host and store:

```kotlin
val scope = rememberCoroutineScope()
val host = remember(scope, storeFile) { storeHost(scope, storeFile) }
val appState = remember(host) { host.mutable(AppStateStore) }

DisposableEffect(host) {
    onDispose { host.close() }
}
```

Outside Compose, use `ValueCellFactory.Plain` (or a custom cell factory with a documented observation
contract). `MutableStore.data` exposes the visible value as a `StateFlow` in all environments.

Do not create a second collector merely to display settings persistence status; use the settings
adapter that projects the existing `StoreStatus` flow.

## Preserve optimistic write semantics

`MutableStore` has three important guarantees:

1. Writes made before the initial value is read wait instead of being lost or overwritten.
2. Pending transforms compose in order and persist against the latest backend value.
3. Disk emissions do not overwrite optimistic local state while transforms remain pending.

Use `update { current -> ... }` for field edits so independent edits compose against current data.
The transform must be pure: it can run more than once during retries, disk updates, and rebasing.
Use `value = ...` or `write(value)` only for replacement values.

`awaitInitialized()` waits for the first persisted value. Use it when the first frame must not show the
default value as if it were loaded state. Otherwise, the application chooses whether to display
defaults or a loading placeholder.

## Handle status and failures explicitly

| Status | Meaning and application responsibility |
| --- | --- |
| `Loading` | The first read is not ready. Decide whether to show defaults or a placeholder. |
| `Ready` | The visible value is usable. Render it. |
| `WriteFailed` | Local optimistic state remains visible. Surface an error if needed and call `retrySave()`. |
| `ReadFailed` | Local updates are rejected. Replace the host after coordinating resource release. |

A successful local update means the value is visible and queued for persistence, not that bytes are
already durable. Verify durability through status and platform tests when the product contract
requires it.

## Keep serialization and file policy caller-owned

- Use `jsonStoreCodec(serializer, json)` for JSON, with an explicit `Json` configuration when unknown
  fields, defaults, or migration behavior matter.
- Keep file names, platform directories, encryption, and storage permissions in the application or
  platform adapter. The M3X core owns the DataStore coordination contract, not the host path.
- Treat the codec as part of the persisted format contract. Changing it requires migration or
  compatibility planning.
- Do not replace `StoreHost` identity semantics with global singletons that hide file ownership.
- Close hosts with their enclosing composition or application lifecycle.

## Review checklist

1. Confirm the persisted unit is a typed model with a stable definition name and codec.
2. Confirm KSP and the serialization plugin are applied to every relevant compilation.
3. Confirm one host owns the file and one definition identity owns each name.
4. Confirm Compose uses snapshot cells; non-Compose use does not claim snapshot observation.
5. Confirm field edits use pure `update` transforms.
6. Confirm initialization behavior matches the desired first-frame UI.
7. Confirm write failures retain local state and retry; read failures replace the host.
8. Confirm host replacement waits for `closeAndJoin`.
9. Confirm generated delegates are not mistaken for settings-level validation.

## Canonical guides

- [Persistence and observable stores](../../../docs/persistence.md)
- [Settings integration](../../../docs/settings.md)
- [Processor generation dependencies](../../../docs/development.md#preserve-generation-dependencies)
- [Store definition and codec contract](../../../modules/datastore-core/src/commonMain/kotlin/StoreDefinition.kt)
- [Store host ownership](../../../modules/datastore-core/src/commonMain/kotlin/StoreHost.kt)
- [Mutable store semantics](../../../modules/datastore-core/src/commonMain/kotlin/MutableStore.kt)
- [Store status](../../../modules/datastore-core/src/commonMain/kotlin/StoreStatus.kt)
- [Store annotations](../../../modules/datastore-core/src/commonMain/kotlin/annotation/Annotation.kt)
- [JSON codec helper](../../../modules/datastore-serialization/src/commonMain/kotlin/JsonStoreCodec.kt)
- [Compose snapshot adapter](../../../modules/datastore-compose/src/commonMain/kotlin/SnapshotStore.kt)
- [Settings DataStore projection](../../../modules/settings-page-datastore/src/commonMain/kotlin/SettingsPersistence.kt)
- [Structured model example](../../../modules/demo/src/commonMain/kotlin/DemoPreferences.kt)
- [Platform StoreFile example](../../../modules/demo/src/commonMain/kotlin/DemoStorage.kt)
- [Host and adapter usage example](../../../modules/demo/src/commonMain/kotlin/DemoApp.kt)
