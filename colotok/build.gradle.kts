@file:OptIn(ExperimentalEncodingApi::class)

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import kotlin.io.encoding.ExperimentalEncodingApi

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kover)
    id("maven-publish")
    id("signing")
    alias(libs.plugins.mavenPublish)
}

kotlin {
    jvmToolchain(11)

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            jvmArgs("--add-opens", "java.base/java.time=ALL-UNNAMED")
        }
    }

    android {
        compileSdk = 36
        namespace = "com.milkcocoa.info.colotok"
        compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
        withHostTest {}
    }

    iosArm64()
    macosArm64()
    iosSimulatorArm64()

    js(IR) {
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlin.serialization.core)
            implementation(libs.kotlin.serialization.json)
            implementation(libs.kotlin.serialization.properties)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            api(libs.okio)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        jsMain.dependencies {
            implementation(libs.okio.nodefilesystem)
        }
        jvmTest.dependencies {
            implementation(project.dependencies.platform(libs.junit.bom))
            implementation(libs.junit.jupiter)
            implementation(libs.mockk)
            implementation(libs.kotlinx.coroutines.test)
        }
    }

    // すべてのターゲットに共通のコンパイラオプション
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        enabled.set(true)
    }
}

dependencies { }

kover {
    reports {
        filters {
            includes {
                classes("com.milkcocoa.info.colotok.*")
            }
            excludes {
                classes(
                    // Android
                    "*BuildConfig*",
                    // Dagger/Hilt
                    "*_*Factory*",
                    "*_ComponentTreeDeps*",
                    "*Hilt_**",
                    "*HiltWrapper_*",
                    "*_Factory*",
                    "*_GeneratedInjector*",
                    "*_HiltComponents*",
                    "*_HiltModules*",
                    "*_HiltModules_BindsModule*",
                    "*_HiltModules_KeyModule*",
                    "*_MembersInjector*",
                    "*_ProvideFactory*",
                    "*_SingletonC*",
                    "*_TestComponentDataSupplier*"
                )
            }
        }
    }
}
//
// koverReport {
//    filters {
//        excludes{
//            classes("")
//        }
//    }
// }

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
    publishToMavenCentral()
    signAllPublications()
    coordinates(
        artifactId = "colotok"
    )

    pom {
        name.set("Colotok")
        description.set(CORE_LIBRARY_DESCRIPTION)
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

// ローカルのSonatype Nexusにアップロードする設定
publishing {
    val properties = Properties()
    val localPropertiesFile = project.rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        properties.load(localPropertiesFile.inputStream())
    }

    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "colotok"
            from(components["kotlin"])

            pom {
                name.set("Colotok")
                description.set(CORE_LIBRARY_DESCRIPTION)
                url.set(PROJECT_URL)

                licenses {
                    license {
                        name.set(LICENSE_TYPE)
                        url.set(LICENSE_URL)
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
    }

    repositories {
        val publishTarget = findProperty("PUBLISH_TARGET") as String? ?: "nexus" // デフォルト Nexus

        if (publishTarget == "nexus") {
            maven {
                name = "nexus"
                isAllowInsecureProtocol = true
                url = uri("https://nexus.milkcocoa.info/repository/Colotok/")

                credentials {
                    username = properties["nexus.user"] as String?
                    password = properties["nexus.password"] as String?
                }
            }
        }
    }
}