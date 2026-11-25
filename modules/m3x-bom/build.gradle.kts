import com.vanniktech.maven.publish.JavaPlatform
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    `java-platform`
    alias(libs.plugins.spotless)
    alias(libs.plugins.mavenPublish)
    signing
}

group = "top.ltfan.m3x"

val bom = project

gradle.projectsEvaluated {
    // Read the final coordinates after the library plugins register their target publications.
    val coordinates =
        rootProject.subprojects
            .filter { it != bom && it.plugins.hasPlugin("com.vanniktech.maven.publish") }
            .flatMap { library ->
                library.extensions
                    .getByType<PublishingExtension>()
                    .publications
                    .withType<MavenPublication>()
                    .map { "${it.groupId}:${it.artifactId}:${it.version}" }
            }
            .sorted()
    dependencies.constraints {
        coordinates.forEach { add("api", it) }
    }
}

mavenPublishing {
    configure(JavaPlatform())
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    pom {
        name = "M3X BOM"
        description = "Version alignment for M3X libraries."
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
            }
        }

        scm {
            connection = "scm:git:https://github.com/xfqwdsj/m3-extended.git"
            developerConnection = "scm:git:https://github.com/xfqwdsj/m3-extended.git"
            url = "https://github.com/xfqwdsj/m3-extended"
        }
    }
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
    if (findProperty("publishSigningMode") != "inMemory") useGpgCmd()
}

spotless {
    kotlinGradle {
        ktfmt().kotlinlangStyle()
    }
}
