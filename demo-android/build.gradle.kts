import com.android.build.api.artifact.SingleArtifact

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.spotless)
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

android {
    namespace = "top.ltfan.m3x.demo.android"
    compileSdk {
        version =
            release(37) {
                minorApiLevel = 2
            }
    }
    defaultConfig {
        applicationId = "top.ltfan.m3x.demo"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = version.toString()
    }
    buildFeatures {
        compose = true
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("debug")
        }
        create("optimized") {
            initWith(getByName("release"))
            // Use the local debug key for installable demo builds.
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    packaging.resources.excludes += "META-INF/**/LICENSE.txt"
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":demo"))
    implementation(libs.activity.compose)
}

val demoVersion = version.toString()

androidComponents {
    onVariants(selector().withBuildType("optimized")) { variant ->
        tasks.register<Sync>("packageReleaseArtifacts") {
            group = "distribution"
            description = "Stages the optimized Android APK for CI."
            val artifactName = "M3X-Demo-$demoVersion-android.apk"
            from(variant.artifacts.get(SingleArtifact.APK)) {
                include("*.apk")
                rename { artifactName }
            }
            duplicatesStrategy = DuplicatesStrategy.FAIL
            val expectedNames = setOf(artifactName)
            // Include unexpected staged files in the incremental check so Sync removes stale
            // assets.
            outputs.upToDateWhen { task ->
                (task as Sync).destinationDir.list()?.toSet() == expectedNames
            }
            into(layout.buildDirectory.dir("release-assets"))
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
