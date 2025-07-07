import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.kotlin.dsl.api
import org.gradle.kotlin.dsl.project

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.serialization")
    id("maven-publish")
    id("signing")
    id("com.vanniktech.maven.publish")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11)) // 実際のビルド環境
    }
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    androidTarget {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }

        publishLibraryVariants(
            "release",
//            "debug"
        )
    }

    iosX64()
    iosArm64()
    macosX64()
    macosArm64()
    iosSimulatorArm64()

    js(IR) {
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":colotok"))
            api(project(":colotok-coroutines"))
            api(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.kotlin.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlin.serialization.json)
        }
    }
}


android {
    compileSdk = 34
    namespace = "com.milkcocoa.info.colotok"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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
        artifactId = "colotok-loki",
    )

    pom {
        name.set("Colotok")
        description.set("${CORE_LIBRARY_DESCRIPTION} - Loki integration")
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
