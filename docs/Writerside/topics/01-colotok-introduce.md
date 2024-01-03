# Introduction

COLOTOK, this is a logging library for kotlin.  

## Feature
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
　🌟 example   
✅ Structure Logging  

## Install 
edit your build.gradle.kts like below.

```kotlin
repositories {
    mavenCentral()
    // add this line
    maven(url =  "https://jitpack.io" )
}

dependencies {
    // add this line
    implementation("com.github.milkcocoa0902:colotok:0.1.9")
}
```

if you use structure logging or create your own provider, you need to add `kotlinx-serialization`

```kotlin

plugins {
    // add this.
    // set version for your use
    kotlin("plugin.serialization") version "1.9.21"
}

dependencies {
    // add this line to use KSerializer<T> and @Serializable
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.0")
}
```

## Usage
### create logger instance

```Kotlin
val fileProvider: FileProvider
val logger = LoggerFactory()
    .addProvider(ConsoleProvider{
        // show above info level in console
        logLevel = LogLevel.INFO
    })
    .addProvider(FileProvider(Path.of("./test.log")){
        // write above trace level for file
        logLevel = LogLevel.TRACE
        
        // memory buffering to save i/o
        enableBuffer = true
        
        // memory buffer size, if buffer excced this, append to file
        bufferSize = 2048
        
        // use size base rotation
        rotation = SizeBaseRotation(size = 4096)
    }.apply {
        fileProvider = this
    })
    .getLogger()
```

### print the log
```Kotlin
logger.trace("TRACE LEVEL LOG")
logger.debug("DEBUG LEVEL LOG")
logger.info("INFO LEVEL LOG")
logger.warn("WARN LEVEL LOG")
logger.error("ERROR LEVEL LOG")

```