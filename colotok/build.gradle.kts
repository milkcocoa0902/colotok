import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.serialization")
//    id("org.jetbrains.kotlin.android") version "2.1.10"
//    id("maven-publish")
//    jacoco
}

group = "com.github.milkcocoa0902"
version = "0.2.3"
java.sourceCompatibility = JavaVersion.VERSION_11
tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

repositories {
    mavenCentral()
    maven(url = "https://plugins.gradle.org/m2/")
}

val ktlint by configurations.creating

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

dependencies {
//    api(libs.kotlin.serialization.core)
//    implementation(libs.kotlin.serialization.json)
//    implementation(libs.kotlin.serialization.properties)
//    testImplementation(platform(libs.junit.bom))
//    testImplementation(libs.junit.jupiter)
//    testImplementation(libs.mockk)
//
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
//
//    ktlint("com.pinterest.ktlint:ktlint-cli:1.1.0") {
//        attributes {
//            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
//        }
//    }
}

// tasks.test {
//    useJUnitPlatform()
//    // https://github.com/mockk/mockk/issues/681
//    jvmArgs("--add-opens", "java.base/java.time=ALL-UNNAMED")
//    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
// }
//
// tasks.jacocoTestReport {
//    dependsOn(tasks.test) // tests are required to run before generating the report
//    reports {
//        xml.required = true
//        csv.required = false
//        html.outputLocation = layout.buildDirectory.dir("jacocoHtml")
//    }
// }
//
// jacoco {
//    toolVersion = "0.8.9"
//    reportsDirectory = layout.buildDirectory.dir("jacocoReport")
// }
//
// afterEvaluate {
//    publishing {
//        publications {
//            // Creates a Maven publication called "release".
//            register(components.first().name, MavenPublication::class) {
//                from(components.first())
//                groupId = "com.github.milkcocoa0902"
//                artifactId = "colotok"
//                version = version
//            }
//        }
//    }
// }
//
// val ktlintCheck by tasks.registering(JavaExec::class) {
//    group = LifecycleBasePlugin.VERIFICATION_GROUP
//    description = "Check Kotlin code style"
//    classpath = ktlint
//    mainClass.set("com.pinterest.ktlint.Main")
//    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
//    // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
//    args(
//        "**/src/**/*.kt",
//        "**.kts",
//        "!**/build/**"
//    )
// }
//
// tasks.check {
//    dependsOn(ktlintCheck)
// }
//
// tasks.register<JavaExec>("ktlintFormat") {
//    group = LifecycleBasePlugin.VERIFICATION_GROUP
//    description = "Check Kotlin code style and format"
//    classpath = ktlint
//    mainClass.set("com.pinterest.ktlint.Main")
//    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
//    // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
//    args(
//        "-F",
//        "**/src/**/*.kt",
//        "**.kts",
//        "!**/build/**"
//    )
// }