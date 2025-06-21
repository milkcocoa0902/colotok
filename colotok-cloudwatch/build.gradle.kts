import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm")
    id("maven-publish")
    id("signing")
    id("com.vanniktech.maven.publish")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":colotok"))
    implementation(project(":colotok-coroutines"))
    implementation(platform(awssdk.bom))
    implementation(awssdk.services.cloudwatchlogs)
    testImplementation(kotlin("test"))
    implementation(libs.kotlin.serialization.core)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}

val CORE_LIBRARY_DESCRIPTION: String by project
val PROJECT_URL: String by project
val LICENSE_TYPE: String by project
val LICENSE_URL: String by project
val LICENSE_DISTRIBUTION: String by project
val DEVELOPER_ID: String by project
val DEVELOPER_NAME: String by project
val DEVELOPER_EMAIL: String by project
val REPOSITORY_URL: String by project


mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    coordinates(
        artifactId = "colotok-cloudwatch",
    )

    pom {
        name.set("Colotok")
        description.set("${CORE_LIBRARY_DESCRIPTION} - CloudWatch integration")
        url.set(PROJECT_URL)

        licenses {
            license {
                name.set(LICENSE_TYPE)
                url.set(LICENSE_URL)
                distribution.set(LICENSE_DISTRIBUTION)
            }
        }

        developers {
            developer {
                id.set(DEVELOPER_ID)
                name.set(DEVELOPER_NAME)
                email.set(DEVELOPER_EMAIL)
            }
        }

        scm {
            url.set(REPOSITORY_URL)
        }
    }
}