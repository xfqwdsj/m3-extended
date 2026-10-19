# Development and verification

## Build from the source of configuration

Use the repository Gradle wrapper. Versions and plugin aliases live in
[libs.versions.toml](../gradle/libs.versions.toml); inspect module build scripts and
[CI workflows](../.github/workflows) for toolchain, target, and runner requirements.
The Android target reads its SDK path from `local.properties`.

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

For fresh execution bypass task/build-cache reuse with `--no-build-cache --rerun-tasks`.
Configuration cache is a separate mechanism: these flags do not disable it. Use
`--no-configuration-cache` as well when a check specifically needs configuration to run again.
Do not interpret a cached result, an unavailable runner, or an environment failure as a fresh pass.
For a documentation-only change, check local links, anchors, example correspondence, and diff scope;
report separately whatever code checks actually ran.

## Test targets

`compose-test-support` provides shared Compose test infrastructure. Its startup helper waits for JS
Skiko initialization before creating a test surface. Test source sets follow the runner boundary:

| Source set | Runs on | Use |
| --- | --- | --- |
| `commonTest` | every target | pure tests and Compose UI tests started with `runInitializedComposeUiTest` |
| `composeTest` | JVM, Wasm, JS, and Apple; not Android host | Compose UI tests that need a desktop, browser, or native host (`ui`) |
| `nonBrowserTest` | JVM and Apple; not JS or Wasm | Frame-by-frame rendering tests that cannot complete in browser runners (`material`, `overlay`, `material-overlay`, `material-backdrop`, `foundation`, `settings-page-material`, `demo`) |
| `jvmTest` | JVM | desktop-specific tests |

Shared Compose UI tests are not Android host tests. Modules declaring `withHostTest {}` keep their
supported non-UI host coverage separately. `compose-test-support` exports `compose.ui.test` and
`coroutines.test`, so dependent test source sets do not repeat those declarations. Compose UI tests
in `commonTest` start through `runInitializedComposeUiTest` so JS waits for Skiko before creating the
test surface.

Library JS/Wasm `binaries.executable()` declarations support the Compose test setup. Preserve them.
Browser test modules use `karma.config.d/mocha-timeout.js` to give Mocha and browser connection/activity
checks 120 seconds. Keep this configuration with tests when splitting modules; the JS test DSL timeout
does not configure the Wasm Karma runner.
For web dependency changes, prepare the toolchain and regenerate the repository lock:

```sh
./gradlew kotlinNodeJsSetup kotlinWasmNodeJsSetup kotlinNpmInstall kotlinWasmNpmInstall
./gradlew kotlinUpgradeYarnLock
```

Review `kotlin-js-store/yarn.lock`. The [test workflow](../.github/workflows/test.yml) runs setup
before `allTests --continue`; demo packaging belongs to the release and Pages workflows. A JVM
result does not establish JS, Wasm, Android, or Apple behavior. Rendering checks should exercise the affected source/stack/frame path;
contract pages link existing regression tests by responsibility.

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
Publication tasks are `publishToMavenCentral` and
`publishAllPublicationsToGitHubPackagesRepository`; the
[publishing workflow](../.github/workflows/publish.yml) supplies credentials and signing mode.
Publication, committing, and pushing require the session's explicit authorization.
