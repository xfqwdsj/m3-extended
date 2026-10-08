# Persistence and observable stores

## Establish one DataStore owner

The persistence foundation is AndroidX DataStore. `datastore-core` exports its core dependency and
`StoreHost` creates instances with `DataStoreFactory`. Applications supply `StoreFile` for platform
storage/path configuration and `StoreCodec` for byte encoding. `StoreDefinition` binds a stable name,
default value, and codec.

One name in a host belongs to one definition instance. Repeated requests share the cached DataStore
and mutable state. A different definition with the same name is rejected even if its model type
matches. Keep definitions stable, and avoid simultaneous hosts owning the same file.

Source: [definitions and storage](../datastore-core/src/commonMain/kotlin/StoreDefinition.kt) and
[host ownership](../datastore-core/src/commonMain/kotlin/StoreHost.kt).
Regression: [host identity and lifecycle](../datastore-core/src/commonTest/kotlin/StoreHostTest.kt).

## Choose the observation adapter

Use `storeHost(scope, file)` or `mutableStateStore` from `datastore-compose` when compositions read
`MutableStore.value`. These use snapshot cells. Outside Compose use `ValueCellFactory.Plain`;
`MutableStore.data` also exposes the visible value through `StateFlow`.

`Store` is an integration seam over this runtime. `DataStoreStore` is its standard supported adapter.
`FlowStore` accepts a Flow and writer for controlled integrations and tests. Its presence does not
make non-AndroidX backends part of the supported persistence foundation.

Sources: [adapters](../datastore-core/src/commonMain/kotlin/Store.kt) and
[snapshot observation](../datastore-compose/src/commonMain/kotlin/SnapshotStore.kt).

## Treat local acceptance and saved state separately

`MutableStore` updates its visible value optimistically and queues transformations. Writes made
before the initial read wait for it. Pending transforms compose in order against the latest backend
value; disk emissions do not overwrite optimistic state while transforms are pending. Use
`awaitInitialized()` when UI must wait for the first persisted value.

Use `update { current -> ... }` for field edits so independent edits compose against current data.
Transforms must be pure: they can run repeatedly during retries, backend updates, and rebasing.
`value = ...` and `write` queue a replacement value. Generated settings setters use `update`.

| Status | Application responsibility |
| --- | --- |
| `Loading` | Decide whether to show initial values or a loading placeholder |
| `Ready` | Render current visible state |
| `WriteFailed` | Keep local state, surface an error if desired, and call `retrySave()` to retry |
| `ReadFailed` | Replace the failed host; further local updates are rejected |

Before creating a replacement host, call `closeAndJoin()` so the original DataStore releases its
resources. `close()` cancels without waiting. A settings commit's `AcceptedLocally` result refers to
the write queue; status reports asynchronous persistence failures.

Source: [optimistic writes](../datastore-core/src/commonMain/kotlin/MutableStore.kt).
Regression: [queue, read/write failures, and rebasing](../datastore-core/src/commonTest/kotlin/MutableStoreTest.kt).

## Generate model bindings

Annotate a serialized model with `@Store(codec = ...)` and apply `datastore-processor` in consuming
KSP compilations. The serialization plugin is required: generated definitions reference the model
serializer. The processor creates `<ClassName>Store` and constructor-property delegates. The codec
factory and optional derivation provider are explicit annotation inputs.

`@Derived` on a readable computed property creates a read-only delegate. A pure `withXxx` model
transformation can create a writable delegate with a selected getter. These model delegates differ
from the settings describer's validated UI commit path.

Sources: [annotations](../datastore-core/src/commonMain/kotlin/annotation/Annotation.kt),
[generator](../datastore-processor/src/main/kotlin/generator/StoreFileGenerator.kt), and
[demo model](../demo/src/commonMain/kotlin/DemoPreferences.kt).
Regression: [generated files](../datastore-processor/src/test/kotlin/StoreFileGeneratorTest.kt).
