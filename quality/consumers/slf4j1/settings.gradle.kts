import org.gradle.api.initialization.resolve.RepositoriesMode

rootProject.name = "colotok-slf4j1-consumer"

val colotokRepository =
    providers.gradleProperty("colotokRepository").orNull
        ?: error("Pass the isolated publication repository with -PcolotokRepository=<path>")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        exclusiveContent {
            forRepository {
                maven {
                    name = "colotokIsolated"
                    url = uri(colotokRepository)
                }
            }
            filter {
                includeGroup("io.github.milkcocoa0902")
            }
        }
        mavenCentral()
    }
}