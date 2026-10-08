import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import kotlin.time.Duration.Companion.seconds
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.dokka)
    alias(libs.plugins.spotless)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.ksp)
    signing
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
    explicitApi()

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    android {
        namespace = "top.ltfan.m3x.material"
        compileSdk {
            version =
                release(37) {
                    minorApiLevel = 2
                }
        }
        androidResources.enable = true
        minSdk = 21
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
    }
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
        binaries.executable()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            dependencies {
                implementation(project(":ui"))
                implementation(project(":animation"))
                api(project(":material-theme"))
                api(project(":foundation"))
                api(project(":material3"))
                api(libs.compose.animation)
                api(libs.compose.runtime)
                api(libs.compose.foundation)
                api(libs.compose.ui)
                api(libs.compose.material3)
                api(libs.compose.components.resources)
                api(libs.dslUtilities)
                implementation(libs.coroutines)
            }
        }

        val skikoMain =
            create("skikoMain") {
                dependsOn(commonMain.get())
            }
        jvmMain {
            dependsOn(skikoMain)
        }
        appleMain {
            dependsOn(skikoMain)
        }
        webMain {
            dependsOn(skikoMain)
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":material-bundle"))
                implementation(project(":compose-test-support"))
                implementation(project(":material-theming"))
            }
        }

        val nonBrowserTest =
            create("nonBrowserTest") {
                dependsOn(commonTest.get())
            }

        getByName("jvmTest").dependsOn(nonBrowserTest)
        getByName("nativeTest").dependsOn(nonBrowserTest)
    }

    compilerOptions {
        freeCompilerArgs.add("-Xcontext-sensitive-resolution")
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.dslUtilities.ksp)
}

tasks.configureEach {
    if (name.startsWith("ksp") && name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

tasks.withType<Jar>().configureEach {
    if (name.contains("sourcesJar", ignoreCase = true)) {
        dependsOn("kspCommonMainKotlinMetadata")
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

dokka {
    dokkaSourceSets {
        configureEach {
            sourceLink {
                remoteUrl =
                    uri("https://github.com/xfqwdsj/m3-extended/tree/v${version}/${project.name}")
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    pom {
        name = project.name
        description = "Material 3 Expressive components for M3X."
        url = "https://github.com/xfqwdsj/m3-extended"

        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/license/mit/"
                distribution = "repo"
            }
            license {
                name = "Apache License, Version 2.0 (adapted floating bottom bar)"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
                distribution = "repo"
            }
        }

        developers {
            developer {
                id = "xfqwdsj"
                name = "LTFan"
                email = "xfqwdsj@qq.com"
                roles = listOf("Author", "Maintainer")
            }
        }

        scm {
            connection = "scm:git:https://github.com/xfqwdsj/m3-extended.git"
            developerConnection = "scm:git:https://github.com/xfqwdsj/m3-extended.git"
            url = "https://github.com/xfqwdsj/m3-extended"
        }
    }

    configure(
        KotlinMultiplatform(javadocJar = JavadocJar.Dokka(tasks.dokkaGeneratePublicationHtml)),
    )
}

publishing {
    repositories {
        maven {
            name = "gitHubPackages"
            url = uri("https://maven.pkg.github.com/xfqwdsj/m3-extended")
            credentials(PasswordCredentials::class)
        }
    }
}

signing {
    sign(publishing.publications)
    val publishSigningMode = findProperty("publishSigningMode") as String?
    if (publishSigningMode == "inMemory") return@signing
    useGpgCmd()
}

group = "top.ltfan.m3x"
