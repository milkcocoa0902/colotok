import org.gradle.api.JavaVersion
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "com.github.milkcocoa0902"
version = "0.2.3"

dependencies {
    implementation(libs.kotlin.serialization.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.okio)
    implementation(project(":colotok"))
    implementation(project(":colotok-cloudwatch"))
    implementation(project(":colotok-coroutines"))
    implementation(project(":colotok-loki"))
    testImplementation(kotlin("test"))
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)

    implementation(libs.slf4j2.api)
    implementation(project(":colotok-slf4j2"))

}

tasks.test {
    useJUnitPlatform()
}
java.sourceCompatibility = JavaVersion.VERSION_11
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17)) // 🔹 Java 17 でビルド
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_11.toString()  // 🔹 Java 11 互換のソースコード
    targetCompatibility = JavaVersion.VERSION_11.toString()  // 🔹 Java 11 互換のバイトコードを出力
}

kotlin {
    jvmToolchain(11)
}
