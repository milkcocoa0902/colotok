import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.10"
    id("maven-publish")
}

group = "com.github.milkcocoa0902"
version = "0.1.3"
java.sourceCompatibility = JavaVersion.VERSION_11
tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

afterEvaluate {
    publishing {
        publications {
            // Creates a Maven publication called "release".
            register(components.first().name, MavenPublication::class){
                from(components.first())
                groupId = "com.github.milkcocoa0902"
                artifactId = "CLK"
                version = version
            }
        }
    }
}