plugins {
    application
    java
}

dependencies {
    implementation("io.github.milkcocoa0902:colotok-slf4j:${providers.gradleProperty("colotokVersion").get()}")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

application {
    mainClass.set("quality.consumers.slf4j1.Slf4j1Consumer")
}

tasks.register("consumerSmoke") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Compiles and runs the isolated SLF4J 1 consumer"
    dependsOn(tasks.named("run"))
}
