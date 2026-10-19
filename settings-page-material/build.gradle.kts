import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import kotlin.time.Duration.Companion.seconds
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.spotless)
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
    signing
}

compose.resources {
    packageOfResClass = "top.ltfan.m3x.settingspage.material.generated.resources"
    generateResClass = always
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
        namespace = "top.ltfan.m3x.settingspage.material"
        androidResources.enable = true
        compileSdk {
            version =
                release(37) {
                    minorApiLevel = 2
                }
        }
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
            dependencies {
                implementation(project(":ui"))
                implementation(project(":overlay"))
                api(project(":material"))
                api(project(":material-overlay"))
                api(project(":foundation"))
                api(project(":animation"))
                api(project(":settings-page-core"))
                api(project(":settings-page-compose"))
                api(project(":resource-compose"))
                implementation(project(":material3"))
                api(libs.compose.animation)
                api(libs.compose.foundation)
                api(libs.compose.material3)
                api(libs.compose.runtime)
                api(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.coroutines)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":material-bundle"))
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
        description = "Material 3 Expressive presentation for M3X settings."
        url = "https://github.com/xfqwdsj/m3-extended"

        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/license/mit/"
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
