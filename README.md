# m3-extended

![Maven Central Version](https://img.shields.io/maven-central/v/top.ltfan.m3x/material)

An extended Material 3 Expressive design system for Compose Multiplatform. M3X coordinates appearance
and interaction through shared design contracts, with reusable infrastructure for application UI.
Its resource and AndroidX DataStore layers can also be used independently of Material UI.

## Getting Started

Add the components you need to your `build.gradle.kts` file. For the complete Material composition and
M3X theme with Capsule shapes:

```kotlin
dependencies {
    implementation("top.ltfan.m3x:material-bundle:<version>")
}
```

In a Kotlin Multiplatform module, put these dependency declarations in the source set that uses
M3X, such as `commonMain.dependencies`.

Or if you are using Gradle Version Catalogs, add the following to your `gradle/libs.versions.toml`:

```toml
[versions]
m3x = "<version>"

[libraries]
m3x-material-bundle = { module = "top.ltfan.m3x:material-bundle", version.ref = "m3x" }
```

Then use the catalog aliases in the same dependency scope:

```kotlin
dependencies {
    implementation(libs.m3x.material.bundle)
}
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

Wrap application UI in `M3xMaterialTheme` when using the optional theme:

```kotlin
import top.ltfan.m3x.material.bundle.M3xMaterialTheme

@Composable
fun App() {
    M3xMaterialTheme {
        // Application content
    }
}
```

Choose other library modules from the [module table](#modules), using
`top.ltfan.m3x:<module>:<version>`. The settings and DataStore processors require the additional
KSP configuration described in the [settings](docs/settings.md) and
[persistence](docs/persistence.md) guides.

## Documentation

Start with the [documentation guide](docs/README.md). The
[design-system contracts](docs/design-system/README.md) specify theme composition, shape and material
roles, accessibility, motion, and component behavior individually, with source and regression links.

- [Backdrop integration](docs/backdrops.md) and [overlays](docs/overlays.md): page sources, stacking, fallback, and dismissal.
- [Settings](docs/settings.md) and [persistence](docs/persistence.md): generated models, validated edits, and store lifecycle.
- [Architecture](docs/architecture.md) and [development](docs/development.md): module boundaries, generation, tests, and launchers.

## Modules

| Module | Responsibility |
| --- | --- |
| `runtime` | Integration with Compose Runtime. |
| `ui`, `ui-platform-private` | UI infrastructure and separately selected private platform integration. |
| `animation-core`, `animation` | Low-level animation foundations and Compose UI animation support. |
| `foundation` | Unstyled Compose UI foundations. |
| `navigation-event` | Integration with NavigationEvent. |
| `material3` | Integration with upstream Compose Material 3. |
| `overlay` | Design-independent overlay infrastructure. |
| `backdrop`, `overlay-backdrop` | Backdrop integration for Compose UI and overlay infrastructure. |
| `material-theme`, `material` | M3X theme contracts and Material 3 Expressive components. |
| `material-overlay`, `material-backdrop` | Material overlay components and Backdrop integration. |
| `material-theming`, `material-theming-capsule` | Theme integration and optional Capsule integration. |
| `material-bundle` | Integrated M3X design system. |
| `resource-core`, `resource-compose` | UI-independent resource abstractions and Compose integration. |
| `settings-page-core`, `settings-page-compose`, `settings-page-material` | Settings contracts and optional Compose and Material integration. |
| `settings-page-datastore` | DataStore integration for settings. |
| `datastore-core`, `datastore-compose`, `datastore-serialization` | DataStore persistence foundations and optional Compose and serialization integration. |
| `datastore-processor`, `settings-page-processor`, `processor-support` | Code generation and shared processor infrastructure. |
| `compose-test-support` | Shared Compose test infrastructure. |
| `demo` | Shared demo application. |
| `demo-android`, `demo-desktop`, `demo-web`, `demo-macos`, `demo-ios` | Platform integration for the shared demo. |

Module boundaries follow abstraction and dependency direction. The guides describe concrete API
contracts within those boundaries; their examples do not define a module's full scope.

## Theme integration

The Material UI is designed and tested for `MaterialExpressiveTheme` and reads the caller's theme.
For the adaptive Capsule preset and coordinated M3X theme, explicitly add
`top.ltfan.m3x:material-bundle` and wrap the UI in `M3xMaterialTheme`.
Choose `material-theming` with `M3xBaseTheme` when using the independent base theme, and add
`material-theming-capsule` explicitly when selecting its continuous shape factory.
See [theme composition](docs/design-system/theme.md) for inheritance and scoped customization.

## Backdrop page setup

Backdrop needs a page source that the glass surfaces can sample. Create one
handle for the page, provide it outside `OverlayHost`, and capture only the page
background and ordinary content. Place glass components outside that capture
node so they do not sample themselves. `OverlayHost` then captures and links
any number of overlays in their drawing order; the caller does not build the
overlay layer stack.

Inside an existing `MaterialTheme`, a page can use this template (with the
usual Compose and M3X imports):

```kotlin
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

See [backdrop integration](docs/backdrops.md) for source boundaries, fallback, and local sampling;
[overlay integration](docs/overlays.md) specifies host requirements and dismissal behavior.

## Demo application

Run the desktop application with `./gradlew :demo-desktop:run`. Build the Android launcher
with `./gradlew :demo-android:assembleDebug` (Android API 24 or later). Web launchers
are `:demo-web:jsBrowserDevelopmentRun` and `:demo-web:wasmJsBrowserDevelopmentRun`.
Run the experimental macOS Native application with `./gradlew :demo-macos:runDebugExecutableMacosArm64`.
Open `demo-ios/iosApp/M3xDemo.xcodeproj` and run the shared `M3xDemo` scheme on a simulator or device supported by its deployment target. The target uses Kotlin's direct framework integration and supports iPad multiple windows. Device builds require a Development Team; simulator builds do not need signing.

The demo includes component and overlay catalogs, a Backdrop Lab, and settings examples.
[Architecture](docs/architecture.md#demo-and-platform-applications) explains their state boundaries;
[development](docs/development.md) covers build and packaging details.
