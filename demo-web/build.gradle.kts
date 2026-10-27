import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.spotless)
}

kotlin {
    js {
        browser()
        binaries.executable()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":demo"))
                implementation(libs.compose.ui)
                implementation(libs.compose.runtime)
            }
        }
    }
}

val pagesDistribution =
    tasks.register<Sync>("pagesDistribution") {
        group = "distribution"
        description = "Assembles the static Wasm demo site for GitHub Pages."
        dependsOn("wasmJsBrowserDistribution")
        from(layout.buildDirectory.dir("dist/wasmJs/productionExecutable"))
        into(layout.buildDirectory.dir("pages"))
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
