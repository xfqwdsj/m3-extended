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
