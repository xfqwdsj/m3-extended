# AGENTS.md

## Scope and navigation

M3X is an extended Material 3 Expressive design system for Compose Multiplatform. Its modules
separate design contracts, UI implementation, and reusable infrastructure. Resource and DataStore
foundations also support non-UI use.

- [README](README.md) owns the module responsibility table and page setup.
- [Design-system contracts](docs/design-system/README.md) specify consumer-visible behavior.
- [Architecture](docs/architecture.md) explains dependency and host ownership boundaries.
- [Development](docs/development.md) documents generation, build wiring, verification, and release tasks.
- `settings.gradle.kts` declares modules; each module's `build.gradle.kts` owns its configuration.

Keep this file focused on persistent modification rules. Put behavior explanations in the relevant
guide and link their source/tests. Describe module responsibilities through stable abstractions and
dependency boundaries; never narrow them to current features or treat current APIs as their final
scope. Use concrete APIs to explain contracts and integration. Keep volatile versions and target
details in build configuration.

## Source conventions

- Library modules use `explicitApi()`: public declarations require explicit `public`.
- Modifier factories are extensions on `Modifier` and preserve the receiver chain in order, including
  factories declared by renderer interfaces.
- Comments and KDoc are English and describe current behavior positively. State the concrete reason
  for surprising behavior; omit change history and unsupported negative comparisons.
- Import declarations instead of using fully qualified names in source or KDoc. Resolve name
  collisions with explicit import aliases. Package/import declarations, reflection/protocol
  identifiers, and string-based external names are exempt.
- `gradle/libs.versions.toml` owns dependency and plugin versions. Add dependencies to the catalog;
  do not declare versions inline in build scripts.
- Source roots omit the longest common prefix of their package declarations from directory paths.
  Keep only subpackage suffix directories; a sole package becomes the source root prefix.
- Compose-only utility modules use `top.ltfan.m3x.compose.<subject>` for both the Kotlin package
  root and the Android namespace; nested adapters extend that root, as in
  `top.ltfan.m3x.compose.ui.privateapi`.
- Tests use the module root package directly under the test source root and import production
  subpackages. Test filenames/classes distinguish responsibilities.

## Design boundaries to preserve

- Components consume the caller's Expressive theme and enclosing hosts. `OverlayHost` owns order,
  modal input isolation, Back dispatch, and shared transitions; `BackdropHost` owns supplied source
  pixels and batched appearance readbacks. Components must not create their own hosts.
- An unhosted interactive overlay is a configuration error outside `LocalInspectionMode`, including
  when hidden. Inspection draws overlays in place.
- Native Material shape roles and M3X extension shapes have separate owners. Preserve inheritance,
  explicit preset precedence, physical-corner semantics, and unknown measurement fallback.
- Static theme preferences and dynamic accessibility settings remain separate. Live platform state
  belongs to the dedicated dynamic provider; limit recomposition to its readers.
- `material-theming-capsule` is optional outside the complete bundle. Keep Capsule geometry out of
  production dependencies of `material`, `foundation`, base theming, and settings modules. Private platform probes remain in their opt-in module.
- `settings-page-core` and `datastore-core` stay free of Compose/Material. UI types belong to their
  optional adapters/renderers. Persistence uses AndroidX DataStore; preserve host/file ownership.
- Preserve common-metadata KSP producer dependencies and generated source directories in `foundation`,
  `material`, and `demo`. Consumers and source JARs must see the generated symbols.
- Keep per-module targets, KSP, resources, and publication configuration in module build scripts;
  the root declares shared plugin aliases with `apply false`.
- Preserve JS/Wasm `binaries.executable()` declarations required by Compose test setup and keep
  shared Compose UI tests on supported runners. Preserve the JS Skiko startup wait in
  `compose-test-support/src/jsMain/kotlin/InitializedComposeUiTest.kt`.
- Keep `commonTest` JS-safe by starting Compose UI tests through `runInitializedComposeUiTest`.
  Frame-by-frame rendering tests that cannot complete in browser runners live in `nonBrowserTest`;
  tests that must skip Android host coverage live in `composeTest`.
- Preserve `androidResources` for resource-bearing Android KMP modules, native macOS resource
  staging, and `mavenLocal()` resolution of locally published sibling libraries.
- The shared demo owns UI/models/storage and consumes both processors. Platform application
  modules own entry points, windows, and packaging.

## Change workflow

- Reproduce a compiler/library/platform claim with a minimal probe before fixing it; quote the
  observed output. Probes and temporary copies stay outside the repository and must not mutate it.
- Fix every reachable instance of a problem class. Document an actual boundary instead of adding
  checks for unreachable cases. Preserve unrelated working-tree edits.
- Update guides when a documented contract or integration requirement becomes inaccurate. Small API
  or behavior changes do not automatically require guide edits; keep local details in KDoc and tests.
  Prefer revising existing text. Add a paragraph or section only when a distinct, durable concept
  belongs in the guide; exercise particular restraint with new paragraphs and avoid change logs.
- Preserve continuous Markdown structures. Do not insert blank lines within a continuous list, table
  or other block unless its syntax or intended rendering requires them.
- Run `./gradlew spotlessCheck`, then affected modules' builds/tests. Use `jvmTest` for KMP JVM tests
  and `test` for JVM-only processors. For documentation-only work, validate links, examples, and
  diff scope; distinguish those checks from code compilation and test execution.
- Use `--no-build-cache --rerun-tasks` when fresh execution is required. Add
  `--no-configuration-cache` when fresh configuration is specifically required.
- After processor changes regenerate consumer output and review its diff; generated output is part
  of the contract.
- Validate JS/Wasm/browser claims on those runners. Run the node/install setup before web tests;
  JVM success is not cross-platform evidence. Web dependency changes require
  `./gradlew kotlinUpgradeYarnLock` and review of `kotlin-js-store/yarn.lock`.
- Do not commit, push, or publish unless the session agreement authorizes it.
- Keep `overlay` independent of Material and Backdrop. Renderer adapters decorate its single host;
  they must preserve entry order, input isolation, retained exits and the shared transition root.
- Keep ordinary `material` and base theme contracts free of renderer-specific types. Full rendering
  is selected through the public surface/bar contracts and `material-bundle` composition.
- Keep settings persistence status independent of DataStore; adapters must preserve the source flow's
  timing and retry ownership. Shared processor code stays in compiler-only dependencies.
- Structural changes preserve the complete composition's existing defaults and behavior. Keep helper
  bug fixes and visual retuning separate from module migration; validate sensitive frames against
  the pre-migration baseline.
