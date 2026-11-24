package top.ltfan.m3x.buildlogic

import com.android.build.api.dsl.ManagedDevices
import groovy.json.JsonSlurper
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidTestDevicePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val profileFile = project.layout.settingsDirectory.file("gradle/android-test-device.json")
        val contents = project.providers.fileContents(profileFile).asText.get()
        val fields = JsonSlurper().parseText(contents) as Map<*, *>
        val profile =
            DeviceProfile(
                name = fields["name"] as String,
                hardwareProfile = fields["hardwareProfile"] as String,
                // The managed device DSL takes the major API, while SDK package IDs include .0.
                apiLevel = (fields["systemImageApi"] as String).removeSuffix(".0").toInt(),
                systemImageSource = fields["systemImageSource"] as String,
            )
        project.extensions.add(DeviceProfile::class.java, "androidTestDevice", profile)
    }

    data class DeviceProfile(
        val name: String,
        val hardwareProfile: String,
        val apiLevel: Int,
        val systemImageSource: String,
    ) {
        fun configure(devices: ManagedDevices) {
            devices.localDevices.create(name) {
                device = hardwareProfile
                apiLevel = this@DeviceProfile.apiLevel
                systemImageSource = this@DeviceProfile.systemImageSource
            }
        }
    }
}
