plugins {
    kotlin("jvm")
}

group = "io.github.milkcocoa0902"
version = "0.3.2"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":colotok"))
    implementation(platform(awssdk.bom))
    implementation(awssdk.services.cloudwatchlogs)
    testImplementation(kotlin("test"))
    implementation(libs.kotlin.serialization.core)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}