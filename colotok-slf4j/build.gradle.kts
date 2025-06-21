plugins {
    kotlin("jvm")
}

group = "com.github.milkcocoa0902"
version = "0.3.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":colotok"))
    implementation("org.slf4j:slf4j-api:1.7.36")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
}