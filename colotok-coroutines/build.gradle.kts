import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("maven-publish")
    id("signing")
    id("com.vanniktech.maven.publish")
}

repositories {
    mavenCentral()
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
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    androidTarget {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }

        publishLibraryVariants(
            "release",
//            "debug"
        )
    }

    iosX64()
    iosArm64()
    macosX64()
    macosArm64()
    iosSimulatorArm64()

    js(IR) {
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":colotok"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlin.serialization.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}


android {
    compileSdk = 34
    namespace = "com.milkcocoa.info.colotok"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}


val CORE_LIBRARY_DESCRIPTION: String by project
val PROJECT_URL: String by project
val LICENSE_TYPE: String by project
val LICENSE_URL: String by project
val LICENSE_DISTRIBUTION: String by project
val DEVELOPER_ID: String by project
val DEVELOPER_NAME: String by project
val DEVELOPER_EMAIL: String by project
val REPOSITORY_URL: String by project


mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    coordinates(
        artifactId = "colotok-coroutines",
    )

    pom {
        name.set("Colotok")
        description.set("${CORE_LIBRARY_DESCRIPTION} - Coroutines integration")
        url.set(PROJECT_URL)

        licenses {
            license {
                name.set(LICENSE_TYPE)
                url.set(LICENSE_URL)
                distribution.set(LICENSE_DISTRIBUTION)
            }
        }

        developers {
            developer {
                id.set(DEVELOPER_ID)
                name.set(DEVELOPER_NAME)
                email.set(DEVELOPER_EMAIL)
            }
        }

        scm {
            url.set(REPOSITORY_URL)
        }
    }
}