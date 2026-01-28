import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm")
    id("maven-publish")
    id("signing")
    id("com.vanniktech.maven.publish")
}

dependencies {
    implementation(project(":colotok"))
    compileOnly(libs.slf4j2.api)
    testImplementation(kotlin("test"))
    testImplementation(libs.slf4j2.api)
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11)) // 実際のビルド環境
    }
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
        artifactId = "colotok-slf4j2",
    )

    pom {
        name.set("Colotok")
        description.set("${CORE_LIBRARY_DESCRIPTION} - SLF4J 2.x integration")
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
