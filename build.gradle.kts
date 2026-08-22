import kotlinx.validation.KotlinApiBuildTask
import kotlinx.validation.KotlinApiCompareTask

plugins {
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.sonatypeCentralUpload) apply false
    id("maven-publish")
    id("signing")
    alias(libs.plugins.mavenPublish) apply false
    alias(libs.plugins.kover)
    alias(libs.plugins.binaryCompatibilityValidator)
    jacoco
}

apiValidation {
    ignoredProjects.add("sample")
}

kover {
    merge {
        projects(
            ":colotok",
            ":colotok-coroutines",
            ":colotok-loki",
            ":colotok-cloudwatch",
            ":colotok-slf4j",
            ":colotok-slf4j2"
        )

        createVariant("jvmAggregated") {
            if (project != rootProject) {
                add("jvm")
            }
        }
    }

    reports {
        filters {
            includes {
                classes(
                    "com.milkcocoa.info.colotok.*",
                    "org.slf4j.impl.*"
                )
            }
            excludes {
                classes(
                    "*BuildConfig*",
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
                    "*_TestInjector*"
                )
            }
        }
    }
}

val publicationGroup = providers.gradleProperty("GROUP").get()
val publicationVersion = providers.gradleProperty("VERSION").get()

// ルート build.gradle.kts
subprojects {
    group = publicationGroup
    version = publicationVersion

    val moduleName = name
    pluginManager.withPlugin("kotlin-multiplatform") {
        if (moduleName in setOf("colotok", "colotok-coroutines", "colotok-loki")) {
            // BCV 0.18.1 does not discover AGP 9 external Android-KMP targets (BCV #312).
            // Remove this bridge once BCV or KGP validates that target directly.
            val androidKmpApiBuild =
                tasks.register<KotlinApiBuildTask>("androidKmpApiBuild") {
                    description = "Builds the public Android API dump for the AGP 9 Android-KMP target"
                    dependsOn("compileAndroidMain")
                    inputClassesDirs.from(layout.buildDirectory.dir("classes/kotlin/android/main"))
                    outputApiFile.set(layout.buildDirectory.file("api/android/$moduleName.api"))
                    runtimeClasspath.from(configurations.named("bcv-rt-jvm-cp-resolver"))
                }
            val androidKmpApiCheck =
                tasks.register<KotlinApiCompareTask>("androidKmpApiCheck") {
                    group = LifecycleBasePlugin.VERIFICATION_GROUP
                    description = "Checks the Android-KMP public API against the checked-in golden dump"
                    dependsOn(androidKmpApiBuild)
                    projectApiFile.set(layout.projectDirectory.file("api/android/$moduleName.api"))
                    generatedApiFile.set(androidKmpApiBuild.flatMap { it.outputApiFile })
                }

            tasks.named("apiCheck") {
                dependsOn(androidKmpApiCheck)
            }
        }
    }

    tasks.withType<Sign>().configureEach {
        onlyIf("publication signing is enabled") {
            !providers.gradleProperty("colotok.skipPublicationSigning")
                .map(String::toBoolean)
                .getOrElse(false)
        }
    }
}

val ktlint by configurations.creating

dependencies {
    ktlint("com.pinterest.ktlint:ktlint-cli:1.1.0") {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
}

val ktlintCheck by tasks.registering(JavaExec::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
    args(
        "**/src/**/*.kt",
        "**.kts",
        "!**/build/**"
    )
}

// tasks.check {
//    dependsOn(ktlintCheck)
// }

tasks.register<JavaExec>("ktlintFormat") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style and format"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
    args(
        "-F",
        "**/src/**/*.kt",
        "**.kts",
        "!**/build/**"
    )
}