import kotlin.time.Duration.Companion.seconds
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.spotless)
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
    explicitApi()

    jvm()
    macosArm64()
    iosSimulatorArm64()
    iosArm64()
    js {
        browser {
            @OptIn(ExperimentalJsTestDsl::class)
            test {
                timeout = 120.seconds
                chromium()
            }
        }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.ui.test)
            api(libs.coroutines.test)
        }

        val nonJsMain =
            create("nonJsMain") {
                dependsOn(commonMain.get())
            }
        jvmMain {
            dependsOn(nonJsMain)
        }
        nativeMain {
            dependsOn(nonJsMain)
        }
        wasmJsMain {
            dependsOn(nonJsMain)
        }
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
