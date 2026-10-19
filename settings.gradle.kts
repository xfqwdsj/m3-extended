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

include(":runtime")

include(":ui")

include(":ui-platform-private")

include(":resource-core")

include(":resource-compose")

include(":material3")

include(":material-theme")

include(":animation-core")

include(":animation")

include(":foundation")

include(":navigation-event")

include(":backdrop")

include(":overlay")

include(":overlay-backdrop")

include(":material")

include(":material-overlay")

include(":material-backdrop")

include(":material-theming-capsule")

include(":material-bundle")

include(":material-theming")

include(":datastore-core")

include(":datastore-compose")

include(":datastore-processor")

include(":settings-page-core")

include(":settings-page-compose")

include(":settings-page-material")

include(":settings-page-processor")

include(":demo")

include(":demo-android")

include(":demo-desktop")

include(":demo-web")

include(":demo-macos")

include(":demo-ios")

include(":compose-test-support")

rootProject.name = "m3-extended"

include(":settings-page-datastore")

include(":datastore-serialization")

include(":processor-support")
