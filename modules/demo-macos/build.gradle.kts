import java.time.Year

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.moko.resources)
    alias(libs.plugins.spotless)
}

multiplatformResources {
    resourcesPackage.set("top.ltfan.m3x.demo.macos.generated.resources")
}

kotlin {
    macosArm64 {
        binaries.executable {
            entryPoint = "top.ltfan.m3x.demo.macos.main"
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":demo"))
                implementation(project(":ui"))
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
            }
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
            macOS {
                appCategory = "public.app-category.developer-tools"
            }
        }
    }
}

// Moko writes bundles beside the executable. Stage them in Contents/Resources for NSBundle lookup.
val stageMacosBundles =
    tasks.register<Sync>("stageMacosBundles") {
        description = "Stages Moko resource bundles in the native macOS application."
        dependsOn("createDistributableNativeReleaseMacosArm64")
        from(layout.buildDirectory.dir("bin/macosArm64/releaseExecutable")) {
            include("*.bundle/**")
        }
        into(nativeAppDir.map { it.dir("Contents/Resources") })
    }

val packageMacosAppZip =
    tasks.register<Exec>("packageMacosAppZip") {
        description = "Packages the native macOS application bundle as a portable zip archive."
        group = "distribution"
        dependsOn(stageMacosBundles)
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

spotless {
    kotlin {
        target("src/*/kotlin/**/*.kt")
        ktfmt().kotlinlangStyle()
    }
    kotlinGradle {
        ktfmt().kotlinlangStyle()
    }
}
