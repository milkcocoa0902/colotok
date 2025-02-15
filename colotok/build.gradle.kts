@file:OptIn(ExperimentalEncodingApi::class)

import cl.franciscosolis.sonatypecentralupload.SonatypeCentralUploadTask
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.daemon.common.isDaemonEnabled
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
    id("maven-publish")
    id("signing")
    id("com.vanniktech.maven.publish") version "0.30.0"
}

group = "io.github.milkcocoa0902"
version = "0.3.1"

java.sourceCompatibility = JavaVersion.VERSION_11
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17)) // 🔹 Java 17 でビルド
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_11.toString()  // 🔹 Java 11 互換のソースコード
    targetCompatibility = JavaVersion.VERSION_11.toString()  // 🔹 Java 11 互換のバイトコードを出力
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            jvmArgs("--add-opens", "java.base/java.time=ALL-UNNAMED")
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
            implementation(libs.kotlin.serialization.core)
            implementation(libs.kotlin.serialization.json)
            implementation(libs.kotlin.serialization.properties)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.okio)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
        }
        nativeMain.dependencies {
        }
        jsMain.dependencies {
            implementation(libs.okio.nodefilesystem)
        }
        jvmTest.dependencies {
            implementation(project.dependencies.platform(libs.junit.bom))
            implementation(libs.junit.jupiter)
            implementation(libs.mockk)
        }
    }
    compilerOptions {
        // Common compiler options applied to all Kotlin source sets
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
//    explicitApi()

    targets.configureEach {
        compilations.configureEach {
            compilerOptions.configure {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
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
                    "*_TestComponentDataSupplier*",
                )
            }
        }
    }
}
//
//koverReport {
//    filters {
//        excludes{
//            classes("")
//        }
//    }
//}

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
        groupId = group.toString(),
        artifactId = "colotok",
        version = version.toString()
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
        val publishTarget = findProperty("PUBLISH_TARGET") as String? ?: "nexus"  // デフォルト Nexus

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