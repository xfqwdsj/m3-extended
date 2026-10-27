# Development and verification

## Build from the source of configuration

Use the repository Gradle wrapper. Versions and plugin aliases live in
[libs.versions.toml](../gradle/libs.versions.toml); inspect module build scripts and
[CI workflows](../.github/workflows) for toolchain, target, and runner requirements.
Android targets resolve the SDK from `local.properties` or the Android SDK environment variables.

The root build declares shared plugin aliases with `apply false`; per-module target, KSP,
resource, and publishing configuration belongs in each module. `mavenLocal()` in settings permits
locally published sibling libraries to resolve alongside the remote repositories.

## Preserve generation dependencies

In `foundation` and `material`, `kspCommonMainKotlinMetadata` produces DSL collectors. The generated
metadata directory is a common source directory. Other KSP tasks, Kotlin compilations, and source
JAR tasks depend on that producer so every consumer sees those symbols.

`demo` applies both settings and DataStore processors to common metadata and uses the same
producer dependency for compilation, downstream KSP, and source JARs. A processor change requires
regenerating consumer output and comparing the generated files, not only testing the processor.

Sources: [core wiring](../foundation/build.gradle.kts), [Material wiring](../material/build.gradle.kts),
and [demo wiring](../demo/build.gradle.kts).

## Check formatting and affected behavior

```sh
./gradlew spotlessCheck
./gradlew :resource-core:jvmTest
./gradlew :datastore-processor:test :settings-page-processor:test
```

Choose affected module build/test tasks for the actual change. JVM-only processors use `test`;
multiplatform modules expose target tasks such as `jvmTest`. Spotless uses ktfmt's kotlinlang style
with per-module targets; `spotlessApply` formats those targets. The root project also applies
Spotless to its Gradle Kotlin scripts.

Use `--rerun-tasks` on affected tasks when fresh execution is required. Gradle reuses valid outputs
for normal verification. Configuration cache can be inspected separately with
`--no-configuration-cache` when configuration itself needs to run again. Report cached results,
unavailable runners and environment failures separately from fresh test execution.
For a documentation-only change, check local links, anchors, example correspondence, and diff scope;
report separately whatever code checks actually ran.

## Test targets

Daily verification uses `./gradlew :verify`: formatting, JVM and processor tests, release automation and the generated build contract. At key validation points,
run `./gradlew :verifyManaged` for Gradle Managed Devices or `:verifyConnected` for attached devices.
`./gradlew :verifyManaged` is the CI entry point; `:verifyTargets` also runs local Web and Apple tests.

| Source set | Runs on | Use |
| --- | --- | --- |
| `commonTest` | All declared test targets | Tests without Compose UI test infrastructure |
| `composeTest` | JVM and Android devices | Shared Compose UI state, semantics and interaction tests |
| `jvmTest` | JVM | Desktop integration, offscreen pixel and deterministic frame tests |
| `androidDeviceTest` | Android devices | Device integration and runner dependencies |

Compose UI tests call `androidx.compose.ui.test.v2.runComposeUiTest` directly. UI test dependencies
are scoped to JVM/Android tests. Pixel baselines and manually stepped rendering tests remain on JVM;
Android shares semantic and interaction contracts. Web and Apple retain non-UI common tests,
available locally through `:verifyWeb` and `:verifyApple`; CI runs only JVM and Android tests. A JVM pass does not establish
Android, Web or Apple behavior. Rendering tests explicitly supply accessibility preferences where
pixel expectations depend on them.

Module Android targets own instrumentation runners and GMD definitions. AGP creates and tears down
the managed device. Compose-enabled Android targets enable `androidResources` so AGP registers
the test asset output. CI enables KVM and uses Xvfb for JVM rendering tests. Reports include JVM and
Android results. Demo packaging remains in release and Pages workflows.

Browser tests use Kotlin's default headless Chrome runner and require a local Chrome installation.
Prepare the Web toolchain before local browser verification:

```sh
./gradlew kotlinNodeJsSetup kotlinWasmNodeJsSetup kotlinNpmInstall kotlinWasmNpmInstall
./gradlew :verifyWeb
```

Run `./gradlew kotlinUpgradeYarnLock` after Web dependency changes and review
`kotlin-js-store/yarn.lock` and `kotlin-js-store/wasm/yarn.lock`.

## Run the existing launchers

| Platform | Existing entry point |
| --- | --- |
| Desktop | `./gradlew :demo-desktop:run` |
| Android | `./gradlew :demo-android:assembleDebug` |
| JS | `./gradlew :demo-web:jsBrowserDevelopmentRun` |
| Wasm | `./gradlew :demo-web:wasmJsBrowserDevelopmentRun` |
| Experimental macOS Native | `./gradlew :demo-macos:runDebugExecutableMacosArm64` |
| iOS | Open `demo-ios/iosApp/M3xDemo.xcodeproj`, run `M3xDemo` |

The iOS launcher uses direct framework integration and supports iPad multiple windows. Device
builds need a Development Team; simulator builds do not require signing. Inspect its deployment
target in the Xcode project. Native macOS stages resolved transitive Compose resources beside its
executable. Modules with Compose resources enable `androidResources` in the Android KMP target so
accessors and assets reach the application together.

## Package demo applications

| Target | Task | Output |
| --- | --- | --- |
| Android optimized APK | `./gradlew :demo-android:packageReleaseArtifacts` | `demo-android/build/release-assets/` |
| Desktop | `./gradlew :demo-desktop:packageReleaseArtifacts` | `demo-desktop/build/release-assets/` |
| macOS Native | `./gradlew :demo-macos:packageReleaseArtifacts` | `demo-macos/build/release-assets/` (archive of the `.app`) |
| Pages site | `./gradlew :demo-web:pagesDistribution` | `demo-web/build/pages/` |

Each `packageReleaseArtifacts` task synchronizes only its current outputs into its module's
`build/release-assets/`, removing stale staged files. CI uploads these files without renaming them.
Names use `M3X-Demo-<version>-android.apk`,
`M3X-Demo-<version>-desktop-<os>-<arch>.<extension>`, and
`M3X-Demo-<version>-macos-native-arm64.zip`. Desktop OS values are `linux`, `macos`, and
`windows`; architectures are `x64` and `arm64`. The fat jar contains host-specific native libraries
and requires a JVM to run; installers and portable images bundle the selected packaging runtime.
Portable archives use `.tar.gz` on Linux and macOS and `.zip` on Windows. Installers use `.deb`,
`.dmg`, and `.msi`, respectively.

The `optimized` Android build type enables R8 full mode, code and resource shrinking, optimized
resource shrinking, and `proguard-android-optimize.txt` while retaining debug signing. CI uses its
runner's debug keystore; installing an APK signed by a different debug key requires uninstalling the
previous app. Add keeps for reflection that library consumer rules cannot see to
`demo-android/proguard-rules.pro`.

The desktop task produces a ProGuard-shrunk fat jar, a portable archive, and the host
installer or dmg in one directory. The release ProGuard pass does not run bytecode optimization
because the optimized form of Navigation 3's generic serialization call fails JVM verification.
Compose's ProGuard integration reads Java library classes from `jmods`. Temurin 24+ omits those
files from its default JDK archive. CI uses `setup-java` with `java-package: jdk+jmods` to supply
matching JDK 25 modules and caches the installation independently of Gradle's dependency cache.
Desktop packaging selects the Adoptium JDK 25 toolchain by default; `-PdesktopJavaHome=<path>`
overrides that selection for ProGuard and jpackage. The JVM macOS package uses `1.0.0` internally
when the project version has major zero; staged artifact names retain the complete project version.
The macOS Native bundle uses the numeric project version and its archive contains the bare `.app`.

The [pages workflow](../.github/workflows/pages.yml) builds and deploys the Wasm site on every push
to `main`. The [publishing workflow](../.github/workflows/publish.yml) uploads demo artifacts to the
release assets, excluding the website; each matrix job builds only formats supported by its host.

## Inspect publication before releasing

Published modules configure Maven Central and GitHub Packages, POM metadata, Dokka source links,
and signing in their own build scripts. Inspect a module's metadata with
`generatePomFileForKotlinMultiplatformPublication` and read `build/publications/` in that module.
Use `:publishLocal` for local integration. Remote entry points `:publishCentral` and
`:publishPackages` require `-PreleaseTag=<tag>`, a matching version and a clean checkout at the tag. The
[publishing workflow](../.github/workflows/publish.yml) supplies credentials and signing mode.
Publication, committing, and pushing require the session's explicit authorization.

Prepare an editable release draft with the manual draft workflow. Generated notes seed the draft;
resuming it preserves edits. Publishing requires successful push Test CI for the exact release
commit. GitHub Packages attempts independent uploads with `--continue`; any failure blocks Central.
Demo artifacts build from the same verified commit.

## Build and release tasks

This table is generated from root task declarations. Run `./gradlew :updateBuildDocs` after changes.

<!-- build-contract:start -->
| Root task | Contract |
| --- | --- |
| `./gradlew :verify` | Checks formatting, JVM tests and release automation. |
| `./gradlew :verifyTargets` | Runs JVM verification and local Web and Apple tests. |
| `./gradlew :verifyApple` | Runs Apple target tests. |
| `./gradlew :verifyWeb` | Runs shared non-UI browser tests. |
| `./gradlew :verifyConnected` | Runs JVM verification and connected Android tests. |
| `./gradlew :verifyManaged` | Runs JVM verification and Android managed-device tests. |
| `./gradlew :verifyRelease` | Checks the release tag, clean checkout and library artifacts. |
| `./gradlew :publishLocal` | Runs verification and publishes artifacts to Maven Local. |
| `./gradlew :publishCentral` | Verifies the release and publishes to Maven Central. |
| `./gradlew :publishPackages` | Verifies the release and publishes to GitHub Packages. |
<!-- build-contract:end -->
