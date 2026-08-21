import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    id("maven-publish")
    id("signing")
    alias(libs.plugins.mavenPublish)
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
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    android {
        compileSdk = 36
        namespace = "com.milkcocoa.info.colotok"
        compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
        withHostTest {}
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
            api(libs.kotlinx.coroutines.core)
            implementation(libs.kotlin.serialization.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }

    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        enabled.set(true)
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
        artifactId = "colotok-coroutines"
    )

    pom {
        name.set("Colotok")
        description.set("${CORE_LIBRARY_DESCRIPTION} - Coroutines integration")
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