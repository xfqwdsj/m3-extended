# m3-extended

[![M3X BOM on Maven Central](https://img.shields.io/maven-central/v/top.ltfan.m3x/m3x-bom)](https://central.sonatype.com/artifact/top.ltfan.m3x/m3x-bom)

An extended Material 3 Expressive design system for Compose Multiplatform. M3X coordinates appearance
and interaction through shared design contracts, with reusable infrastructure for application UI.
Its resource and AndroidX DataStore layers can also be used independently of Material UI.

## Getting started

Import the bill of materials (BOM) to align library versions, then declare the modules your
application uses. `material-bundle` selects the complete Material composition with Capsule shapes.

```kotlin
dependencies {
    implementation(platform("top.ltfan.m3x:m3x-bom:<version>"))
    implementation("top.ltfan.m3x:material-bundle")
}
```

In a Kotlin Multiplatform build, declare both dependencies in the source set that uses M3X.

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project.dependencies.platform("top.ltfan.m3x:m3x-bom:<version>"))
            implementation("top.ltfan.m3x:material-bundle")
        }
    }
}
```

For a Gradle version catalog, put the version on the BOM alias.

```toml
[versions]
m3x = "<version>"

[libraries]
m3x-bom = { module = "top.ltfan.m3x:m3x-bom", version.ref = "m3x" }
m3x-material-bundle = { module = "top.ltfan.m3x:material-bundle" }
```

Make sure your `settings.gradle.kts` includes the repositories used by M3X and its dependencies:

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}
```

Wrap application UI in `M3xMaterialTheme` to install the integrated theme and renderers.

```kotlin
import androidx.compose.runtime.Composable
import top.ltfan.m3x.material.bundle.M3xMaterialTheme

@Composable
fun App() {
    M3xMaterialTheme {
        // Application content
    }
}
```

For an explicit coordinate, use `top.ltfan.m3x:<module>:<version>` with that module's
release-group version. [Module table](#modules) · [Release groups](docs/releases.md#choose-publication-groups)

The settings and DataStore processors use Kotlin Symbol Processing (KSP).
[Settings setup](docs/settings.md) · [DataStore setup](docs/persistence.md)

## Documentation

Start with the [documentation guide](docs/README.md). The
[design-system contracts](docs/design-system/README.md) specify theme composition, shape and material
roles, accessibility, motion, and component behavior individually, with source and regression links.

- [Backdrop integration](docs/backdrops.md) and [overlays](docs/overlays.md): page sources, stacking, fallback, and dismissal.
- [Settings](docs/settings.md) and [persistence](docs/persistence.md): generated models, validated edits, and store lifecycle.
- [Architecture](docs/architecture.md) and [development](docs/development.md): module boundaries, generation, tests, and launchers.

## Modules

The library and tooling projects live under [`modules/`](modules).

| Module | Responsibility |
| --- | --- |
| `runtime` | Compose Runtime state and Flow integration |
| `ui` | Compose UI infrastructure and public platform integration |
| `ui-platform-private` | Opt-in private platform integration for Compose UI |
| `animation-core` | Compose animation foundations |
| `animation` | Animation and motion integration for Compose UI |
| `foundation` | Unstyled Compose UI foundations |
| `navigation-event` | NavigationEvent integration with Compose transitions |
| `material3` | Extensions for upstream Compose Material 3 |
| `shape` | Structured contours and signed-distance geometry for Compose shapes |
| `capsule` | Continuous rounded shapes, continuity profiles, and shape interpolation |
| `backdrop` | Backdrop source capture, effects, and rendering for Compose Multiplatform |
| `overlay` | Design-independent overlay hosting, input, and transitions |
| `overlay-backdrop` | Backdrop source stacking for hosted overlays |
| `material-theme` | M3X theme contracts and extension values |
| `material` | Material 3 Expressive components |
| `material-overlay` | Material 3 Expressive overlay components |
| `material-backdrop` | Backdrop rendering and appearance policy for Material surfaces |
| `material-theming` | Composition of the base Material and M3X themes |
| `material-theming-capsule` | Capsule shape factories and presets for M3X theming |
| `material-bundle` | Integrated Material theme, components, and renderer adapters |
| `m3x-bom` | Version constraints for published libraries and platform variants |
| `resource-core` | UI-independent resource abstractions |
| `resource-compose` | Resource resolution and rendering in Compose |
| `settings-page-core` | UI-independent settings contracts |
| `settings-page-compose` | Compose integration for settings |
| `settings-page-material` | Material 3 Expressive settings presentation |
| `settings-page-datastore` | DataStore integration for settings |
| `datastore-core` | Persistence foundations built on AndroidX DataStore |
| `datastore-compose` | Compose integration for DataStore persistence |
| `datastore-serialization` | Serialization integration for DataStore persistence |
| `datastore-processor` | KSP code generation for DataStore persistence |
| `settings-page-processor` | KSP code generation for settings |
| `processor-support` | Shared compiler infrastructure for the processors |

The `demo` module contains the shared application. The `demo-android`, `demo-desktop`, `demo-ios`,
`demo-macos`, and `demo-web` modules own its platform entry points and packaging.
The benchmark projects supply shared scenarios, a JVM harness, and an Android app and test
harness. [Benchmark projects](modules/benchmarks/backdrop) · [Benchmark guide](docs/benchmarks.md)

## Theme integration

The Material UI is designed and tested for `MaterialExpressiveTheme` and reads the caller's theme.
For the adaptive Capsule preset and coordinated M3X theme, explicitly add
`top.ltfan.m3x:material-bundle` and wrap the UI in `M3xMaterialTheme`.
Choose `material-theming` with `M3xBaseTheme` when using the independent base theme, and add
`material-theming-capsule` explicitly when selecting its continuous shape factory.
See [theme composition](docs/design-system/theme.md) for inheritance and scoped customization.

## Backdrop page setup

Choose a page subtree to supply pixels for Backdrop surfaces. Create a page handle and provide it
with `BackdropHost` outside `OverlayHost`. Capture the page background and ordinary content.
Glass surfaces sample that capture as siblings. The Backdrop overlay adapter links the overlay
layers in draw order.

Inside an existing `MaterialTheme`, use `ProvideM3xMaterialRendering` to install the renderer
adapters. `M3xMaterialTheme` already installs these adapters. The page below captures a background,
draws a floating bottom bar, and opens a hosted dialog.

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import top.ltfan.m3x.backdrop.BackdropHost
import top.ltfan.m3x.backdrop.captureBackdrop
import top.ltfan.m3x.backdrop.rememberBackdropLayer
import top.ltfan.m3x.material.FloatingBottomBar
import top.ltfan.m3x.material.bundle.ProvideM3xMaterialRendering
import top.ltfan.m3x.material.item
import top.ltfan.m3x.material.overlay.OverlayDialog
import top.ltfan.m3x.overlay.OverlayHost
import top.ltfan.m3x.overlay.OverlayHostState

@Composable
fun ExampleBackdropPage() {
    var selected by remember { mutableIntStateOf(0) }
    var dialogOpen by remember { mutableStateOf(false) }
    val pageLayer = rememberBackdropLayer()
    val overlayState = remember { OverlayHostState() }

    ProvideM3xMaterialRendering {
        BackdropHost(pageLayer) {
            OverlayHost(overlayState) {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .captureBackdrop(pageLayer)
                            .background(MaterialTheme.colorScheme.background),
                    ) {
                        Button(onClick = { dialogOpen = true }) { Text("Open dialog") }
                    }

                    FloatingBottomBar(
                        selectedTabIndex = selected,
                        onTabSelected = { selected = it },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    ) {
                        item { Text("Home") }
                        item { Text("Settings") }
                    }

                    OverlayDialog(
                        onDismissRequest = { dialogOpen = false },
                        visible = { dialogOpen },
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        Text("Dialog content")
                    }
                }
            }
        }
    }
}
```

The `captureBackdrop` modifier precedes `background` so the background is
inside the captured draw subtree. Additional page capture layers can be built
with the lower-level backdrop APIs when a page needs more than one source.

See the host requirements and source boundaries in the integration guides.
[Backdrop integration](docs/backdrops.md) · [Overlay integration](docs/overlays.md)

## Demo application

Run the desktop application with `./gradlew :demo-desktop:run`. Build the Android launcher
with `./gradlew :demo-android:assembleDebug` (Android API 24 or later). Use
`:demo-android:assembleOptimized` for an APK with R8, resource shrinking, and debug signing. Package the desktop launchers with
`./gradlew :demo-desktop:packageReleaseArtifacts` and the native macOS launcher with
`./gradlew :demo-macos:packageReleaseArtifacts`. Web launchers
are `:demo-web:jsBrowserDevelopmentRun` and `:demo-web:wasmJsBrowserDevelopmentRun`.
`:demo-web:pagesDistribution` builds the Wasm site for GitHub Pages.
Run the experimental macOS Native application with `./gradlew :demo-macos:runDebugExecutableMacosArm64`.
Open `modules/demo-ios/iosApp/M3xDemo.xcodeproj` and run the shared `M3xDemo` scheme.
The target uses Kotlin's direct framework integration and supports multiple windows on iPad.
Select a Development Team for device builds.
See [demo packaging](docs/development.md#package-demo-applications) for CI artifact names, outputs,
and packaging JDK requirements.

The demo includes component and overlay catalogs, a Backdrop Lab, and settings examples.
[Demo architecture](docs/architecture.md#demo-and-platform-applications) · [Development](docs/development.md)
