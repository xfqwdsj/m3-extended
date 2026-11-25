import groovy.json.JsonOutput
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.moko.resources)
    alias(libs.plugins.spotless)
}

multiplatformResources {
    resourcesPackage.set("top.ltfan.m3x.demo.web.generated.resources")
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
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
            }
        }
    }
}

val stageWasmLocalization =
    tasks.register<Copy>("stageWasmLocalization") {
        description = "Stages Moko localization assets in the production Wasm distribution."
        from(layout.buildDirectory.dir("compileSync/wasmJs/main/productionExecutable/optimized")) {
            include("localization/**")
        }
        into(layout.buildDirectory.dir("dist/wasmJs/productionExecutable"))
        mustRunAfter("wasmJsBrowserDistribution")
    }

tasks
    .matching { it.name == "wasmJsBrowserDistribution" }
    .configureEach {
        finalizedBy(stageWasmLocalization)
    }

abstract class GeneratePagesLoader : DefaultTask() {
    @get:InputFile @get:PathSensitive(PathSensitivity.NONE) abstract val index: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val startupFiles: ConfigurableFileCollection

    @get:OutputFile abstract val output: RegularFileProperty

    @TaskAction
    fun writeLoader() {
        val entries = startupFiles.files.map { it.name }.sorted()
        check("demo-web.js" in entries && entries.any { it.endsWith(".wasm") })
        val source = index.get().asFile.readText()
        val marker = "<!-- startup-resources -->"
        check(marker in source)
        val manifest =
            """<script type="application/json" id="startup-resources">${JsonOutput.toJson(entries)}</script>"""
        output.get().asFile.apply {
            parentFile.mkdirs()
            writeText(source.replace(marker, manifest))
        }
    }
}

val wasmDistributionDirectory = layout.buildDirectory.dir("dist/wasmJs/productionExecutable")
val generatePagesLoader =
    tasks.register<GeneratePagesLoader>("generatePagesLoader") {
        description = "Embeds the production startup file list in the Pages index."
        dependsOn("wasmJsBrowserDistribution", stageWasmLocalization)
        index.set(wasmDistributionDirectory.map { it.file("index.html") })
        startupFiles.from(
            files(wasmDistributionDirectory).asFileTree.matching {
                include("demo-web.js", "*.wasm")
            },
        )
        output.set(layout.buildDirectory.file("generated/pagesLoader/index.html"))
    }

val pagesDistribution =
    tasks.register<Sync>("pagesDistribution") {
        group = "distribution"
        description = "Assembles the static Wasm demo site for GitHub Pages."
        from(wasmDistributionDirectory) { exclude("index.html") }
        from(generatePagesLoader)
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
