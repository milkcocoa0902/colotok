import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmExtension

plugins {
    id("org.jetbrains.kotlin.multiplatform") apply false
    id("com.android.kotlin.multiplatform.library") apply false
}

// Use the host Node installation so KGP does not add a distribution repository.
allprojects {
    plugins.withType<NodeJsPlugin> {
        extensions.configure<NodeJsEnvSpec> {
            download.set(false)
        }
    }
}

// Compiler versions use different JS test tooling; keep their npm locks separate.
val consumerKotlin = providers.gradleProperty("consumerKotlin").getOrElse("2.4.21")
plugins.withType<NodeJsRootPlugin> {
    extensions.configure<NpmExtension> {
        lockFileDirectory.set(layout.projectDirectory.dir("kotlin-js-store/$consumerKotlin"))
    }
}

val apple = providers.gradleProperty("consumerApple").getOrElse("false").toBoolean()
val consumerArtifacts = mapOf("core" to "colotok", "coroutines" to "colotok-coroutines", "loki" to "colotok-loki")
val colotokVersion = providers.gradleProperty("colotokVersion").get()

subprojects {
    apply(plugin = "org.jetbrains.kotlin.multiplatform")
    apply(plugin = "com.android.kotlin.multiplatform.library")

    extensions.configure<KotlinMultiplatformExtension> {
        if (apple) {
            iosArm64()
            iosSimulatorArm64()
            macosArm64()
        } else {
            jvm()
            js(IR) { nodejs() }
        }
        targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
            namespace = "quality.consumers.${project.name}"
            compileSdk = 36
        }
        sourceSets {
            commonMain.dependencies {
                implementation("io.github.milkcocoa0902:${consumerArtifacts.getValue(project.name)}:$colotokVersion")
            }
            commonTest.dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
            }
        }
    }
}

tasks.register("consumerSmoke") {
    group = "verification"
    val checks =
        if (apple) {
            listOf(
                "compileCommonMainKotlinMetadata",
                "compileKotlinIosArm64",
                "compileKotlinIosSimulatorArm64",
                "macosArm64Test"
            )
        } else {
            listOf("compileCommonMainKotlinMetadata", "jvmTest", "jsNodeTest", "compileAndroidMain")
        }
    dependsOn(consumerArtifacts.keys.flatMap { module -> checks.map { ":$module:$it" } })
}