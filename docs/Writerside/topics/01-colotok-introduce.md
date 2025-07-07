# Introduction

COLOTOK, this is a logging library for kotlin.  

# Feature
✅ Print log with color  
✅ Formatter  
✅ Print log where you want  
　🌟 ConsoleProvider  
　🌟 FileProvider    
　🌟 StreamProvider  
✅ Log Rotation  
　🌟 SizeBaseRotation  
　🌟 DateBaseRotation(; DurationBase)    
✅ Customize output location  
　🌟 example [print log into slack](https://github.com/milkcocoa0902/colotok_slack_integration_sample)  
✅ Structure Logging  
✅ MDC (Mapped Diagnostic Context)


# Integration
basic dependency

```kotlin
dependencies {
    // add this line
    implementation("io.github.milkcocoa0902:colotok:0.3.2")
}
```

or when you use kotlin multiplatform(;KMP)

```kotlin
commonMain.dependncies{
    implementation("io.github.milkcocoa0902:colotok:0.3.2")
}

jvmMain.dependencies{
    implementation("io.github.milkcocoa0902:colotok-jvm:0.3.2")
}

androidMain.dependencies{
    implementation("io.github.milkcocoa0902:colotok-android:0.3.2")
}

jsMain.dependencies{
    implementation("io.github.milkcocoa0902:colotok-js:0.3.2")
}
```

# Plugins

Colotok provides several plugins to extend its functionality:

|       plugin       |                      artifact                      |           feature           |    Platform    |
|:------------------:|:--------------------------------------------------:|:---------------------------:|:--------------:|
| colotok-coroutines | `io.github.milkcocoa0902:colotok-coroutines:0.3.2` |      coroutine support      | Multi Platform |
|   colotok-slf4j    |   `io.github.milkcocoa0902:colotok-slf4j:0.3.2`    | as SLF4J backend (JVM only) |      JVM       |
| colotok-cloudwath  | `io.github.milkcocoa0902:colotok-cloudwatch:0.3.2` | send logs to AWS CloudWatch |      JVM       |
|    colotok-loki    |    `io.github.milkcocoa0902:colotok-loki:0.3.2`    |  send logs to Grafana Loki  | Multi Platform |

# Dependencies
if you use structure logging or create your own provider, you need to add `kotlinx.serialization`.  
when colotok formats into text from your structure, using `kotlinx.serialization` internally.

```kotlin

plugins {
    // add this.
    // set version for your use
    kotlin("plugin.serialization") version "2.1.10"
}

dependencies {
    // add this line to use @Serializable
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.0")
}
```



# Usage
## Configuration
configure colotok with code.  
see below.

```kotlin
val logger = LoggerFactory()
    .addProvider(ConsoleProvider())
    .getLogger()

```

more details config
```Kotlin
val fileProvider: FileProvider
val logger = ColotokLoggerFactory()
    .addProvider(ConsoleProvider{
        // show above info level in console
        level = LogLevel.INFO
    })
    .addProvider(FileProvider(File("test.log").toOkioPath()){
        level = LogLevel.INFO
        // memory buffering to save i/o
        enableBuffer = true
        // memory buffer size, if buffer excced this, append to file
        bufferSize = 2048
        // use size base rotation
        rotation = SizeBaseRotation(size = 4096L)
    }.apply {
        fileProvider = this
    }).getLogger()

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
