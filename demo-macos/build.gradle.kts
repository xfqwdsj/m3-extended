import java.time.Year

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.spotless)
}

kotlin {
    macosArm64 {
        binaries.executable {
            entryPoint = "top.ltfan.m3x.demo.macos.main"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":demo"))
            implementation(project(":ui"))
            implementation(libs.compose.ui)
            implementation(libs.compose.runtime)
        }
    }
}

val demoVersion = version.toString()
// The native bundle uses the numeric core of the project version.
val bundleParts = demoVersion.substringBefore('-').substringBefore('+').split('.')
val bundleVersion = (bundleParts + List(3 - bundleParts.size) { "0" }).joinToString(".")
val copyrightNotice = "© ${Year.now().value} xfqwdsj. All rights reserved."
val macosArchiveDir = layout.buildDirectory.dir("macos/artifacts")
val nativeAppDir =
    layout.buildDirectory.dir(
        "compose/binaries/main/native-macosArm64-release-app-image/M3XDemo.app",
    )

compose.desktop {
    nativeApplication {
        targets(kotlin.targets.getByName("macosArm64"))
        distributions {
            packageName = "M3XDemo"
            packageVersion = bundleVersion
            copyright = copyrightNotice
            // The app-image task packages the native executable and Compose resources in a bare
            // .app.
            macOS {
                appCategory = "public.app-category.developer-tools"
            }
        }
    }
}

val packageMacosAppZip =
    tasks.register<Exec>("packageMacosAppZip") {
        description = "Packages the native macOS application bundle as a portable zip archive."
        group = "distribution"
        dependsOn("createDistributableNativeReleaseMacosArm64")
        val archive =
            macosArchiveDir.get().asFile.resolve("M3X-Demo-$demoVersion-macos-native-arm64.zip")
        inputs.dir(nativeAppDir)
        outputs.file(archive)
        doFirst {
            archive.parentFile.mkdirs()
            archive.delete()
        }
        commandLine(
            "ditto",
            "-c",
            "-k",
            "--norsrc",
            "--noextattr",
            "--noqtn",
            "--keepParent",
            nativeAppDir.get().asFile.absolutePath,
            archive.absolutePath,
        )
    }

tasks.register<Sync>("packageReleaseArtifacts") {
    group = "distribution"
    description = "Stages the native macOS application archive for CI."
    from(packageMacosAppZip)
    val expectedNames = setOf("M3X-Demo-$demoVersion-macos-native-arm64.zip")
    // Include unexpected staged files in the incremental check so Sync removes stale assets.
    outputs.upToDateWhen { task ->
        (task as Sync).destinationDir.list()?.toSet() == expectedNames
    }
    into(layout.buildDirectory.dir("release-assets"))
}

// Native resources are loaded from the compose-resources directory beside the executable.
listOf("Debug", "Release").forEach { mode ->
    val stage =
        tasks.register<Sync>("stage${mode}MacosResources") {
            description = "Stage macOS resources for $mode"
            dependsOn("macosArm64ProcessResources")
            from(layout.buildDirectory.dir("processedResources/macosArm64/main")) {
                include("composeResources/**")
            }
            into(
                layout.buildDirectory.dir(
                    "bin/macosArm64/${mode.lowercase()}Executable/compose-resources",
                ),
            )
        }
    tasks
        .matching { it.name == "link${mode}ExecutableMacosArm64" }
        .configureEach {
            finalizedBy(stage)
        }
    tasks
        .matching { it.name == "run${mode}ExecutableMacosArm64" }
        .configureEach {
            dependsOn(stage)
        }
}

spotless {
    kotlin {
        target("src/*/kotlin/**/*.kt")
        ktfmt().kotlinlangStyle()
    }
    kotlinGradle {
        ktfmt().kotlinlangStyle()
    }
}
