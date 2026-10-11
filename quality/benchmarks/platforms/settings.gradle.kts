pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories { mavenCentral() }
}
rootProject.name = "ColotokPlatformBenchmarks"
include(":current", ":noCaller")
project(":current").projectDir = file("build/projects/current")
project(":noCaller").projectDir = file("build/projects/noCaller")
