
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties
import kotlin.io.encoding.ExperimentalEncodingApi
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

group = "io.github.milkcocoa0902"
version = "0.3.2"

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
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.okio)
            implementation(project(":colotok"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
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
