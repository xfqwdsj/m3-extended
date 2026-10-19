// The settings file is the entry point of every Gradle build.
// Its primary purpose is to define the subprojects.
// It is also used for some aspects of project-wide configuration, like managing plugins,
// dependencies, etc.
// https://docs.gradle.org/current/userguide/settings_file_basics.html

pluginManagement {
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

// Overlay and Backdrop infrastructure.
include(
    ":overlay",
    ":backdrop",
    ":overlay-backdrop",
)

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

// Shared Compose test infrastructure.
include(":compose-test-support")

// Demo application and platform launchers.
include(
    ":demo",
    ":demo-android",
    ":demo-desktop",
    ":demo-web",
    ":demo-macos",
    ":demo-ios",
)
