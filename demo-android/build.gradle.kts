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

spotless {
    kotlin {
        target("src/*/kotlin/**/*.kt")
        ktfmt().kotlinlangStyle()
    }
    kotlinGradle {
        ktfmt().kotlinlangStyle()
    }
}
