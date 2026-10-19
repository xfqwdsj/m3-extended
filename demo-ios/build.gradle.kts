plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.spotless)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "M3xDemo"
            isStatic = true
        }
    }

    sourceSets {
        iosMain {
            dependencies {
                implementation(project(":demo"))
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
            }
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
