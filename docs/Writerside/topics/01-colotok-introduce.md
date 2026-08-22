# Introduction

Colotok is a Kotlin Multiplatform logging library.

# Feature
✅ Print log with color  
✅ Formatter  
✅ Print log where you want  
　🌟 ConsoleProvider  
　🌟 FileProvider    
　🌟 StreamProvider  
✅ Log Rotation  
　🌟 SizeBaseRotation  
　🌟 DateBaseRotation (duration based)<br/>
✅ Customize output location  
　🌟 example [print log into slack](https://github.com/milkcocoa0902/colotok_slack_integration_sample)  
✅ Structured Logging<br/>
✅ MDC (Mapped Diagnostic Context)<br/>
✅ Metrics Collection


# Integration
basic dependency

```kotlin
dependencies {
    // add this line
    implementation("io.github.milkcocoa0902:colotok:0.5.0")
}
```

For a Kotlin Multiplatform project, add the root artifact to `commonMain`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.milkcocoa0902:colotok:0.5.0")
        }
    }
}
```

The core module, `colotok-coroutines`, and `colotok-loki` support JVM, Android, JS/Node,
`iosArm64`, `iosSimulatorArm64`, and `macosArm64`. Apple x86 targets (`iosX64` and `macosX64`)
are not published in 0.5.0. Gradle module metadata selects the target-specific variant from the
common dependency; do not add target-suffixed coordinates manually.

# Plugins

Colotok provides several plugins to extend its functionality:

|       plugin       |                      artifact                      |             feature             |    Platform    |
|:------------------:|:--------------------------------------------------:|:-------------------------------:|:--------------:|
| colotok-coroutines | `io.github.milkcocoa0902:colotok-coroutines:0.5.0` |        coroutine support        | Multi Platform |
|   colotok-slf4j    |   `io.github.milkcocoa0902:colotok-slf4j:0.5.0`    | SLF4J 1.7.x bindings (JVM only) |      JVM       |
|   colotok-slf4j2   |   `io.github.milkcocoa0902:colotok-slf4j2:0.5.0`   |  SLF4J 2.x bindings (JVM only)  |      JVM       |
| colotok-cloudwatch | `io.github.milkcocoa0902:colotok-cloudwatch:0.5.0` |   send logs to AWS CloudWatch   |      JVM       |
|    colotok-loki    |    `io.github.milkcocoa0902:colotok-loki:0.5.0`    |    send logs to Grafana Loki    | Multi Platform |

# Dependencies

Structured logging requires the Kotlin serialization compiler plugin because message classes use
`@Serializable`. The `colotok` artifact already exposes the serialization runtime required by its
public API, so a separate runtime dependency is normally unnecessary.

```kotlin

plugins {
    // Use the same version as your Kotlin Gradle plugin.
    kotlin("plugin.serialization") version "2.3.21"
}
```



# Usage
## Configuration
configure colotok with code.  
see below.

```kotlin
val logger = ColotokLoggerContext()
    .addProvider(ConsoleProvider(ConsoleProviderConfig()))
    .getLogger()

```

more details config
```Kotlin
import okio.Path.Companion.toPath

val fileProvider = FileProvider("test.log".toPath()) {
    level = LogLevel.INFO
    // use size-based rotation
    rotation = SizeBaseRotation(size = 4096L)
}

val logger = ColotokLoggerContext()
    .addProvider(ConsoleProvider(ConsoleProviderConfig().apply {
        // show above info level in console
        level = LogLevel.INFO
    }))
    .addProvider(fileProvider)
    .getLogger()

logger.trace("TRACE LEVEL LOG")
logger.debug("DEBUG LEVEL LOG")
logger.info("INFO LEVEL LOG")
logger.warn("WARN LEVEL LOG")
logger.error("ERROR LEVEL LOG")
```

## Print
now, you can print log into your space.

```kotlin
logger.trace("TRACE LEVEL LOG")
logger.debug("DEBUG LEVEL LOG")
logger.info("INFO LEVEL LOG")
logger.warn("WARN LEVEL LOG")
logger.error("ERROR LEVEL LOG")

logger.atInfo {
    print("in this block")
    print("all of logs are printed out with INFO level")
}

// or you can add additional parameters
logger.info("INFO LEVEL LOG", mapOf("param1" to "a custom attr"))
```
