# Introduction

Colotok is a Kotlin Multiplatform logging library.

## Features

- Console, file, and stream output
- Text and JSON formatting
- Size-based and duration-based file rotation
- Structured logging with Kotlin serialization
- Mapped Diagnostic Context (MDC)
- Runtime metrics
- Custom providers and formatters
- Loki, CloudWatch, and SLF4J integrations

## Installation

For a JVM or Android project, add the core dependency:

```kotlin
dependencies {
    implementation("io.github.milkcocoa0902:colotok:1.0.0")
}
```

For a Kotlin Multiplatform project, add the root artifact to `commonMain`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.milkcocoa0902:colotok:1.0.0")
        }
    }
}
```

The core module, `colotok-coroutines`, and `colotok-loki` support JVM, Android, JS/Node,
`iosArm64`, `iosSimulatorArm64`, and `macosArm64`. Apple x86 targets (`iosX64` and `macosX64`)
are not supported in 1.0.0. Gradle module metadata selects the target-specific variant from the
common dependency; do not add target-suffixed coordinates manually.

## Integration modules

Colotok provides several plugins to extend its functionality:

|       plugin       |                      artifact                      |             feature             |    Platform    |
|:------------------:|:--------------------------------------------------:|:-------------------------------:|:--------------:|
| colotok-coroutines | `io.github.milkcocoa0902:colotok-coroutines:1.0.0` |        coroutine support        | Multi Platform |
|   colotok-slf4j    |   `io.github.milkcocoa0902:colotok-slf4j:1.0.0`    | SLF4J 1.7.x bindings (JVM only) |      JVM       |
|   colotok-slf4j2   |   `io.github.milkcocoa0902:colotok-slf4j2:1.0.0`   |  SLF4J 2.x bindings (JVM only)  |      JVM       |
| colotok-cloudwatch | `io.github.milkcocoa0902:colotok-cloudwatch:1.0.0` |   send logs to AWS CloudWatch   |      JVM       |
|    colotok-loki    |    `io.github.milkcocoa0902:colotok-loki:1.0.0`    |    send logs to Grafana Loki    | Multi Platform |

## Kotlin serialization

Structured logging requires the Kotlin serialization compiler plugin because message classes use
`@Serializable`. The `colotok` artifact already exposes the serialization runtime required by its
public API, so a separate runtime dependency is normally unnecessary.

Colotok 1.0.0 is built with Kotlin/KGP **2.4.21**. Use Kotlin 2.4.21 for JS/Node:
Kotlin 2.3.21 cannot read the JS KLIB ABI produced by 2.4.21.
See [compiler compatibility](https://github.com/milkcocoa0902/colotok/blob/develop/quality/consumers/README.md#compiler-compatibility) for the tested compiler and platform combinations.

```kotlin

plugins {
    // Use the same version as your Kotlin Gradle plugin.
    kotlin("plugin.serialization") version "2.4.21"
}
```


## Usage

### Create a logger

Create a Context, add a Provider, and obtain a logger:

```kotlin
val logger = ColotokLoggerContext()
    .addProvider(ConsoleProvider(ConsoleProviderConfig()))
    .getLogger()

```

Configure each Provider before adding it to the Context:
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

### Write logs

Use a method matching the log level:

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
