// The settings file is the entry point of every Gradle build.
// Its primary purpose is to define the subprojects.
// It is also used for some aspects of project-wide configuration, like managing plugins,
// dependencies, etc.
// https://docs.gradle.org/current/userguide/settings_file_basics.html

pluginManagement {
    includeBuild("build-logic")

    // Use the version catalog to manage dependencies and plugins.
    // The version catalog is defined in `gradle/libs.versions.toml`.
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    // Use Maven Central as the default repository (where Gradle will download dependencies) in all
    // subprojects.
    repositories {
        mavenCentral()
        google()
        mavenLocal()
    }
}

rootProject.name = "m3-extended"

// Compose runtime and UI infrastructure.
include(
    ":runtime",
    ":ui",
    ":ui-platform-private",
    ":animation-core",
    ":animation",
    ":foundation",
    ":navigation-event",
    ":material3",
)

// Structured contours and continuous rounded shapes.
include(":shape", ":capsule")

// Backdrop rendering.
include(":backdrop")

// Overlay presentation and its Backdrop adapter.
include(":overlay", ":overlay-backdrop")

// Material components, theming, and the integrated bundle.
include(
    ":material-theme",
    ":material",
    ":material-overlay",
    ":material-backdrop",
    ":material-theming",
    ":material-theming-capsule",
    ":material-bundle",
)

// Resource abstractions and Compose integration.
include(
    ":resource-core",
    ":resource-compose",
)

// Settings contracts and integration.
include(
    ":settings-page-core",
    ":settings-page-compose",
    ":settings-page-material",
    ":settings-page-datastore",
)

// DataStore persistence foundations and integration.
include(
    ":datastore-core",
    ":datastore-compose",
    ":datastore-serialization",
)

// Symbol processors and shared processor infrastructure.
include(
    ":datastore-processor",
    ":settings-page-processor",
    ":processor-support",
)

// Dependency version alignment.
include(":m3x-bom")

// Demo application and platform launchers.
include(
    ":demo",
    ":demo-android",
    ":demo-desktop",
    ":demo-web",
    ":demo-macos",
    ":demo-ios",
)

// Benchmark harnesses.
include(
    ":benchmarks:backdrop:scenarios",
    ":benchmarks:backdrop:jvm",
    ":benchmarks:backdrop:android-app",
    ":benchmarks:backdrop:android-macro",
)

fun placeModules(parent: ProjectDescriptor, directory: File) {
    parent.children.forEach { module ->
        module.projectDir = directory.resolve(module.name)
        placeModules(module, module.projectDir)
    }
}

placeModules(rootProject, file("modules"))
