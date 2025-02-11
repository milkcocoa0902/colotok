plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "com.github.milkcocoa0902"
version = "0.2.3"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.serialization.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.okio)
    implementation(project(":colotok"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}