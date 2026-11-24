import com.android.build.api.dsl.ManagedVirtualDevice
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
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
        androidResources.enable = true
        namespace = "top.ltfan.m3x.overlay.backdrop"
        compileSdk {
            version =
                release(37) {
                    minorApiLevel = 2
                }
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }
            .configure {
                instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                managedDevices {
                    localDevices.create("pixel6Api37") {
                        device = "Pixel 6"
                        apiLevel = 37
                        systemImageSource = "google_apis"
                        pageAlignment = ManagedVirtualDevice.PageAlignment.FORCE_4KB_PAGES
                    }
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
        browser()
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
                api(project(":overlay"))
                api(project(":backdrop"))
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.test.runner)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
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

val documentationTag = providers.gradleProperty("releaseTag").getOrElse("v${rootProject.version}")

dokka {
    dokkaSourceSets {
        configureEach {
            documentedVisibilities(VisibilityModifier.Public, VisibilityModifier.Protected)
            sourceLink {
                remoteUrl =
                    uri(
                        "https://github.com/xfqwdsj/m3-extended/tree/${documentationTag}/modules/${project.name}",
                    )
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    pom {
        name = project.name
        description = "Backdrop integration for Compose overlay infrastructure."
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
