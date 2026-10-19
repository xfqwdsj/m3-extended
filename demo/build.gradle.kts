import kotlin.time.Duration.Companion.seconds
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.ksp)
    alias(libs.plugins.spotless)
}

compose.resources {
    packageOfResClass = "top.ltfan.m3x.demo.generated.resources"
    generateResClass = always
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    android {
        namespace = "top.ltfan.m3x.demo"
        compileSdk {
            version =
                release(37) {
                    minorApiLevel = 2
                }
        }
        minSdk = 24
        androidResources.enable = true
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
    }
    js {
        browser {
            @OptIn(ExperimentalJsTestDsl::class)
            test {
                timeout = 120.seconds
                chromium()
            }
        }
        binaries.executable()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    macosArm64()
    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            dependencies {
                implementation(project(":ui"))
                implementation(project(":ui-platform-private"))
                implementation(project(":material-bundle"))
                implementation(project(":material-theming"))
                implementation(project(":settings-page-material"))
                implementation(project(":datastore-compose"))
                implementation(project(":settings-page-datastore"))
                implementation(project(":datastore-serialization"))
                implementation(project(":resource-compose"))
                implementation(libs.navigation3.ui)
                implementation(libs.serialization.json)
                implementation(libs.coroutines)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.material.icons.core)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":compose-test-support"))
            }
        }

        val nonBrowserTest =
            create("nonBrowserTest") {
                dependsOn(commonTest.get())
            }

        jvmTest {
            dependsOn(nonBrowserTest)
        }
        nativeTest {
            dependsOn(nonBrowserTest)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xcontext-sensitive-resolution")
    }
}

dependencies {
    add("kspCommonMainMetadata", project(":settings-page-processor"))
    add("kspCommonMainMetadata", project(":datastore-processor"))
}

tasks
    .matching {
        (it.name.startsWith("compile") && it.name.contains("Kotlin")) ||
            it.name.endsWith("SourcesJar", ignoreCase = true) ||
            (it.name.startsWith("ksp") && it.name != "kspCommonMainKotlinMetadata")
    }
    .configureEach {
        dependsOn("kspCommonMainKotlinMetadata")
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
