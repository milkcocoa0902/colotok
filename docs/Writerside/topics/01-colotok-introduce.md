# Introduction

COLOTOK, this is a logging library for kotlin.  

## Feature
✅ Kotlin Multiplatform Support    
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

# Integration
basic dependency

```kotlin
dependencies {
    // add this line
    implementation("io.github.milkcocoa0902:colotok:0.3.0")
}
```

or when you use kotlin multiplatform(;KMP)

```kotlin
commonMain.dependncies{
    implementation("io.github.milkcocoa0902:colotok:0.3.0")
}

jvmMain.dependencies{
    implementation("io.github.milkcocoa0902:colotok-jvm:0.3.0")
}

androidMain.dependencies{
    implementation("io.github.milkcocoa0902:colotok-android:0.3.0")
}

jsMain.dependencies{
    implementation("io.github.milkcocoa0902:colotok-js:0.3.0")
}
```

# Dependencies

if you will use the provider which File or Stream, your application needs to be depended on `Okio`
```kotlin
dependencies {
  implementation("com.squareup.okio:okio:3.10.2")
}
```

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



## Usage
### create logger instance

```Kotlin
val fileProvider: FileProvider
val logger = LoggerFactory()
    .addProvider(ConsoleProvider{
        // show above info level in console
        level = LogLevel.INFO
    })
    .addProvider(File("test.log").toOkioPath()){
        // write above trace level for file
        level = LogLevel.TRACE
        
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