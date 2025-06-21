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
//    implementation(project(":colotok-slf4j"))
    testImplementation(kotlin("test"))
    implementation("org.slf4j:slf4j-api:1.7.36")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}