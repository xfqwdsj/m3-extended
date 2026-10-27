import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest

plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.mavenPublish) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.spotless)
}

val commands =
    linkedMapOf(
        "verify" to
            ("Checks formatting, JVM tests and release automation." to
                listOf("checkReleaseAutomation", "checkBuildDocs")),
        "verifyTargets" to
            ("Runs JVM verification and local Web and Apple tests." to
                listOf("verify", "verifyWeb", "verifyApple")),
        "verifyApple" to ("Runs Apple target tests." to emptyList<String>()),
        "verifyWeb" to ("Runs shared non-UI browser tests." to emptyList<String>()),
        "verifyConnected" to
            ("Runs JVM verification and connected Android tests." to listOf("verify")),
        "verifyManaged" to
            ("Runs JVM verification and Android managed-device tests." to listOf("verify")),
        "verifyRelease" to
            ("Checks the release tag, clean checkout and library artifacts." to
                listOf("checkReleaseVersion", "checkReleaseTree")),
        "publishLocal" to
            ("Runs verification and publishes artifacts to Maven Local." to listOf("verify")),
        "publishCentral" to
            ("Verifies the release and publishes to Maven Central." to listOf("verifyRelease")),
        "publishPackages" to
            ("Verifies the release and publishes to GitHub Packages." to listOf("verifyRelease")),
    )

commands.forEach { (name, contract) ->
    tasks.register(name) {
        group = if (name.startsWith("publish")) "publishing" else "verification"
        description = contract.first
        dependsOn(contract.second)
    }
}

val verify = tasks.named("verify")

verify.configure { dependsOn("spotlessCheck") }

subprojects {
    plugins.withId("com.diffplug.spotless") {
        val formatting = tasks.named("spotlessCheck")
        verify.configure { dependsOn(formatting) }
    }
    val moduleTasks = tasks
    plugins.withId("org.jetbrains.kotlin.jvm") {
        val processorTests = tasks.named("test")
        verify.configure { dependsOn(processorTests) }
    }
    verify.configure {
        dependsOn(
            moduleTasks.matching {
                it.name == "jvmTest"
            },
        )
    }
    mapOf(
            "verifyWeb" to setOf("jsBrowserTest", "wasmJsBrowserTest"),
            "verifyConnected" to setOf("connectedAndroidDeviceTest"),
            "verifyManaged" to setOf("allDevicesAndroidDeviceTest"),
        )
        .forEach { (entry, tests) ->
            rootProject.tasks.named(entry).configure {
                dependsOn(moduleTasks.matching { it.name in tests })
            }
        }
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
        val appleTests =
            moduleTasks.withType<KotlinNativeTest>().matching { test ->
                kotlin.targets.withType<KotlinNativeTarget>().any { target ->
                    target.name == test.targetName && target.konanTarget.family.isAppleFamily
                }
            }
        rootProject.tasks.named("verifyApple").configure {
            dependsOn(appleTests)
        }
    }
    plugins.withId("com.vanniktech.maven.publish") {
        rootProject.tasks.named("checkReleaseTree").configure {
            mustRunAfter(moduleTasks.named("assemble"))
        }
        rootProject.tasks.named("verifyRelease").configure {
            dependsOn(moduleTasks.named("assemble"))
        }
        mapOf(
                "publishLocal" to "publishToMavenLocal",
                "publishCentral" to "publishToMavenCentral",
                "publishPackages" to "publishAllPublicationsToGitHubPackagesRepository",
            )
            .forEach { (entry, publication) ->
                rootProject.tasks.named(entry).configure {
                    dependsOn(moduleTasks.matching { it.name == publication })
                }
            }
    }
    tasks
        .matching { it.name.startsWith("publish") }
        .configureEach {
            dependsOn(
                if (name.endsWith("ToMavenLocal")) verify
                else rootProject.tasks.named("verifyRelease"),
            )
        }
}

tasks.register<Exec>("checkReleaseAutomation") {
    group = "verification"
    description = "Tests release drafts, version checks and the passing-CI gate."
    args("--test", ".github/scripts/release.test.mjs")
}

plugins.withType<NodeJsRootPlugin> {
    val node = extensions.getByType<NodeJsEnvSpec>()
    tasks.named<Exec>("checkReleaseAutomation") {
        dependsOn("kotlinNodeJsSetup")
        executable(node.executable.get())
    }
}

val releaseTag = providers.gradleProperty("releaseTag")
val libraryVersion = providers.gradleProperty("version")

tasks.register("checkReleaseVersion") {
    val tagInput = releaseTag
    val versionInput = libraryVersion
    group = "verification"
    description = "Checks that releaseTag matches the publication version."
    inputs.property("releaseTag", tagInput.orElse(""))
    inputs.property("libraryVersion", versionInput)
    doLast {
        val tag = tagInput.orNull ?: error("Supply -PreleaseTag=<tag> for a release operation.")
        require(tag.removePrefix("v") == versionInput.get()) {
            "The release tag must match the version in gradle.properties."
        }
    }
}

tasks.register("checkReleaseTree") {
    mustRunAfter("verify")
    val tagInput = releaseTag
    val repository = layout.projectDirectory.asFile
    group = "verification"
    description = "Checks that the release tag identifies a clean checkout."
    doLast {
        val tag = tagInput.orNull ?: error("Supply -PreleaseTag=<tag> for a release operation.")
        fun git(vararg arguments: String): String {
            val process =
                ProcessBuilder(listOf("git") + arguments)
                    .directory(repository)
                    .redirectErrorStream(true)
                    .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            check(process.waitFor() == 0) { "The release requires an existing Git tag." }
            return output
        }
        require(
            git("rev-parse", "HEAD") == git("rev-parse", "--verify", "refs/tags/$tag^{commit}"),
        ) {
            "The release tag must identify the checked-out commit."
        }
        require(git("status", "--porcelain").isEmpty()) {
            "Release operations require a clean working tree."
        }
    }
}

val buildDocs = layout.projectDirectory.file("docs/development.md").asFile
val tableStart = "<!-- build-contract:start -->"
val tableEnd = "<!-- build-contract:end -->"
val taskTable = buildString {
    appendLine(tableStart)
    appendLine("| Root task | Contract |")
    appendLine("| --- | --- |")
    commands.forEach { (name, contract) ->
        appendLine("| `./gradlew :$name` | ${contract.first} |")
    }
    append(tableEnd)
}
val tablePattern = Regex("${Regex.escape(tableStart)}[\\s\\S]*?${Regex.escape(tableEnd)}")

tasks.register("updateBuildDocs") {
    val document = buildDocs
    val expectedTable = taskTable
    val blockPattern = tablePattern
    group = "documentation"
    description = "Regenerates the root-task table in the development guide."
    inputs.property("taskTable", expectedTable)
    outputs.file(document)
    outputs.upToDateWhen { false }
    doLast {
        val text = document.readText()
        require(blockPattern.findAll(text).count() == 1) {
            "The development guide needs one build-contract block."
        }
        document.writeText(text.replace(blockPattern, expectedTable))
    }
}

tasks.register("checkBuildDocs") {
    mustRunAfter("updateBuildDocs")
    val document = buildDocs
    val expectedTable = taskTable
    val blockPattern = tablePattern
    group = "verification"
    description = "Checks the generated root-task table against task declarations."
    inputs.property("taskTable", expectedTable)
    inputs.file(document)
    doLast {
        val text = document.readText()
        val block = blockPattern.findAll(text).singleOrNull()
        require(block?.value == expectedTable) {
            "Run ./gradlew :updateBuildDocs to update the task contract."
        }
        require(
            !Regex("^##\\s", RegexOption.MULTILINE)
                .containsMatchIn(text.substring(block.range.last + 1)),
        ) {
            "Keep the generated task table in the final level-two section."
        }
    }
}

spotless {
    kotlinGradle {
        target("*.gradle.kts", "gradle/*.gradle.kts")
        ktfmt().kotlinlangStyle()
    }
}

group = "top.ltfan.m3x"
