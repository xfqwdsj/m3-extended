plugins { `kotlin-dsl` }

kotlin { jvmToolchain(25) }

dependencies {
    compileOnly(libs.android.gradle.api) { isTransitive = false }
}

gradlePlugin {
    plugins {
        create("androidTestDevice") {
            id = "top.ltfan.m3x.android-test-device"
            implementationClass = "top.ltfan.m3x.buildlogic.AndroidTestDevicePlugin"
        }
    }
}
