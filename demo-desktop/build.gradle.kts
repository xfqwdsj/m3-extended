import java.time.Year
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.tasks.Jar
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.spotless)
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(project(":demo"))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
}

val demoVersion = version.toString()
// macOS and Windows installer versions use the numeric core of the project version.
// Artifact names retain pre-release and build metadata.
val numericParts = demoVersion.substringBefore('-').substringBefore('+').split('.')
val numericVersion = (numericParts + List(3 - numericParts.size) { "0" }).joinToString(".")
// jpackage rejects a macOS app version whose first component is zero.
val macosVersion =
    numericVersion.takeIf { (it.substringBefore('.').toIntOrNull() ?: 0) > 0 } ?: "1.0.0"
val hostOs =
    System.getProperty("os.name").lowercase().let {
        when {
            it.contains("mac") -> "macos"
            it.contains("win") -> "windows"
            else -> "linux"
        }
    }
val hostArch =
    System.getProperty("os.arch").lowercase().let {
        when (it) {
            "aarch64",
            "arm64" -> "arm64"
            else -> "x64"
        }
    }
val releaseAppDir = layout.buildDirectory.dir("compose/binaries/main-release/app")
val releaseArtifactDir = layout.buildDirectory.dir("compose/artifacts")
val desktopJdk = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(25))
    vendor.set(JvmVendorSpec.ADOPTIUM)
}
// Compose ProGuard reads JDK classes from jmods; Temurin 24+ supplies them separately.
// desktopJavaHome overrides the packaging toolchain for both ProGuard and jpackage.
val packagingJavaHome: String =
    providers.gradleProperty("desktopJavaHome").orNull
        ?: desktopJdk.get().metadata.installationPath.asFile.absolutePath

compose.desktop {
    application {
        mainClass = "top.ltfan.m3x.demo.desktop.MainKt"
        // ProGuard and jpackage run on the same JDK used for packaging.
        javaHome = packagingJavaHome
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Deb, TargetFormat.Msi)
            packageName = "M3X Demo"
            packageVersion = demoVersion
            description = "M3 Extended design system demo for Compose Multiplatform."
            vendor = "xfqwdsj"
            copyright = "© ${Year.now().value} xfqwdsj. All rights reserved."
            licenseFile.set(rootProject.file("LICENSE"))
            linux {
                packageName = "m3x-demo"
                packageVersion = demoVersion
                debMaintainer = "xfqwdsj@qq.com"
                appCategory = "Utility"
                menuGroup = "Utility"
            }
            macOS {
                packageVersion = macosVersion
                packageBuildVersion = macosVersion
                dmgPackageVersion = macosVersion
                dmgPackageBuildVersion = macosVersion
            }
            windows {
                packageVersion = numericVersion
                menuGroup = "M3X Demo"
                upgradeUuid = "b534ea92-0cee-43f9-9424-63b55808ffde"
                dirChooser = true
                perUserInstall = true
            }
        }
        buildTypes.release.proguard {
            // ProGuard still removes unused classes. Bytecode optimization is disabled because it
            // rewrites Navigation 3's generic serialization call into unverifiable bytecode.
            optimize.set(false)
            obfuscate.set(false)
        }
    }
}

// The plugin exposes the ProGuard input as a classpath, so a version-only change leaves the task
// up to date and keeps the previous version's output name. Track the version explicitly.
tasks
    .matching { it.name == "proguardReleaseJars" }
    .configureEach {
        inputs.property("releaseVersion", demoVersion)
    }

val packageReleasePortableTar =
    tasks.register<Exec>("packageReleasePortableTar") {
        group = "compose desktop"
        description = "Packages the ProGuard-shrunk desktop image as a portable tar.gz archive."
        dependsOn("createReleaseDistributable")
        val sourceDir = releaseAppDir.get().asFile
        val archive =
            releaseArtifactDir
                .get()
                .asFile
                .resolve(
                    "M3X-Demo-$demoVersion-desktop-$hostOs-$hostArch.tar.gz",
                )
        inputs.dir(releaseAppDir)
        outputs.file(archive)
        doFirst {
            archive.parentFile.mkdirs()
            archive.delete()
        }
        // The system tar preserves the executable bits of the packaged launcher and runtime.
        commandLine("tar", "-czf", archive.absolutePath, "-C", sourceDir.absolutePath, ".")
    }

val packageReleasePortableZip =
    tasks.register<Zip>("packageReleasePortableZip") {
        group = "compose desktop"
        description = "Packages the ProGuard-shrunk desktop image as a portable zip archive."
        dependsOn("createReleaseDistributable")
        from(releaseAppDir)
        archiveFileName.set("M3X-Demo-$demoVersion-desktop-$hostOs-$hostArch.zip")
        destinationDirectory.set(releaseArtifactDir)
    }

val packageReleasePortable =
    tasks.register("packageReleasePortable") {
        group = "compose desktop"
        description =
            "Packages the ProGuard-shrunk desktop image in the archive format used by this host."
        if (hostOs == "windows") {
            dependsOn(packageReleasePortableZip)
        } else {
            dependsOn(packageReleasePortableTar)
        }
    }

// The plugin names the fat jar after the jpackage version; use the release version instead.
tasks
    .matching { it.name == "packageReleaseUberJarForCurrentOS" }
    .configureEach {
        if (this is Jar) {
            archiveFileName.set("M3X-Demo-$demoVersion-desktop-$hostOs-$hostArch.jar")
        }
    }

val stageReleaseInstaller =
    tasks.register<Sync>("stageReleaseInstaller") {
        group = "compose desktop"
        description =
            "Stages the host installer with the project version, OS, and architecture in its name."
        val version = demoVersion
        val arch = hostArch
        duplicatesStrategy = DuplicatesStrategy.FAIL
        when (hostOs) {
            "macos" -> {
                dependsOn("packageReleaseDmg")
                from(layout.buildDirectory.dir("compose/binaries/main-release/dmg")) {
                    include("*.dmg")
                    rename { "M3X-Demo-$version-desktop-macos-$arch.dmg" }
                }
            }
            "linux" -> {
                dependsOn("packageReleaseDeb")
                from(layout.buildDirectory.dir("compose/binaries/main-release/deb")) {
                    include("*.deb")
                    rename { "M3X-Demo-$version-desktop-linux-$arch.deb" }
                }
            }
            else -> {
                dependsOn("packageReleaseMsi")
                from(layout.buildDirectory.dir("compose/binaries/main-release/msi")) {
                    include("*.msi")
                    rename { "M3X-Demo-$version-desktop-windows-$arch.msi" }
                }
            }
        }
        into(layout.buildDirectory.dir("compose/release-installer"))
    }

tasks.register<Sync>("packageReleaseArtifacts") {
    group = "distribution"
    description = "Stages the current host's release desktop artifacts for CI."
    from(tasks.named<Jar>("packageReleaseUberJarForCurrentOS").flatMap { it.archiveFile })
    from(if (hostOs == "windows") packageReleasePortableZip else packageReleasePortableTar)
    from(stageReleaseInstaller)
    val artifactBase = "M3X-Demo-$demoVersion-desktop-$hostOs-$hostArch"
    val portableExtension = if (hostOs == "windows") "zip" else "tar.gz"
    val installerExtension =
        when (hostOs) {
            "macos" -> "dmg"
            "linux" -> "deb"
            else -> "msi"
        }
    val expectedNames =
        setOf(
            "$artifactBase.jar",
            "$artifactBase.$portableExtension",
            "$artifactBase.$installerExtension",
        )
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
