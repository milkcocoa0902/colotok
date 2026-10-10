import org.gradle.api.initialization.resolve.RepositoriesMode

pluginManagement {
    val consumerKotlin = providers.gradleProperty("consumerKotlin").getOrElse("2.4.21")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("org.jetbrains.kotlin.multiplatform") version consumerKotlin
        id("com.android.kotlin.multiplatform.library") version "9.0.1"
    }
}

rootProject.name = "colotok-kmp-consumer"
include("core", "coroutines", "loki")

val colotokRepository =
    providers.gradleProperty("colotokRepository").orNull
        ?: error("Pass -PcolotokRepository=<isolated publication path>")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        exclusiveContent {
            forRepository {
                maven {
                    name = "colotokIsolated"
                    url = uri(colotokRepository)
                    metadataSources { gradleMetadata() }
                }
            }
            filter { includeGroup("io.github.milkcocoa0902") }
        }
        google()
        mavenCentral()
    }
}