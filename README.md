# COLOTOK
COLOTOK; Cocoa LogTool for Kotlin  

![](https://img.shields.io/static/v1?label=kotlin-jvm&message=1.8.10&color=magenta)
![](https://img.shields.io/static/v1?label=jdk&message=11&color=magenta)
[![](https://jitpack.io/v/milkcocoa0902/colotok.svg)](https://jitpack.io/#milkcocoa0902/colotok)
[![codecov](https://codecov.io/gh/milkcocoa0902/colotok/graph/badge.svg?token=XGH4U42LVM)](https://codecov.io/gh/milkcocoa0902/colotok)


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

if you will use the provider which File or Stream, your application needs to be depend on `Okio`
```kotlin: buind.gradle.kts
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
val logger = LoggerFactory()
    .addProvider(ConsoleProvider{
        // show above info level in console
        level = LogLevel.INFO
    })
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

## Formatter(Text)
colotok has builtin text formatter.
1. PlainTextFormatter
2. SimpleTextFormatter
3. DetailTextFormatter

### 1. PlainFormatter
this formatter shows as below style's log

```
logger.info("message what happen")

// message what happen
```

### 2. SimpleFormatter
this formatter shows as below style's log

```Kotlin
logger.info("message what happen")

// 2023-12-29 12:23:14.220383  [INFO] - message what happen
```

### 3. DetailFormatter
this formatter shows as below style's log

```Kotlin
logger.ingo("message what happen", mapOf("param1" to "a custom attribute"))

// 2023-12-29T12:21:13.354328+09:00 (main)[INFO] - message what happen, additional = {param1=a custom attribute}
```

## Formatter(Structure)
colotok has builtin structured formatter.
1. SimpleStructureFormatter
2. DetailStructureFormatter

if you has a class
```kotlin
@Serializable
class LogDetail(val scope: String, val message: String): LogStructure

@Serializable
class Log(val name: String, val logDetail: LogDetail): LogStructure
```

### 1. SimpleStructureFormatter
this formatter shows bellow style's log

```Kotlin
logger.info(
    Log(
        name = "illegal state",
        LogDetail(
            "args",
            "argument must be greater than zero"
        )
    )
)

// // {"message":{"name":"illegal state","logDetail":{"scope":"args","message":"argument must be greater than zero"}},"level":"INFO","date":"2023-12-29"}


logger.info("message what happen")

// {"msg":"message what happen","level":"INFO","date":"2023-12-29"}
```
### 2. DetailStructureFormatter
this formatter shows bellow style's log

```Kotlin
logger.info(
    Log(
        name = "illegal state",
        LogDetail(
            "args",
            "argument must be greater than zero"
        )
    ),
    // you can pass additional attrs
    mapOf("additional" to "additional param")
)

// {"message":{"name":"illegal state","logDetail":{"scope":"args","message":"argument must be greater than zero"}},"level":"INFO","additional":"additional param","date":"2023-12-29T12:34:56"}


logger.info("message what happen")

// {"message":"message what happen","level":"INFO","thread":"main","date":"2023-12-29T12:27:22.5908"}
```



## Provider
colotok has builtin provider. Provider is used for output log.

1. ConsoleProvider
2. FileProvider
3. StreamProvider

### 1. ConsoleProvider
this provider outputs log into console with ansi-color

### 2. FileProvider
this provider output log into file without ansi-color.

### 3. StreamProvider
this provider output log into stream where you specified.  

### 4. Customize
you can also output to remote or sql or others by create own provider.  
example

#### define custom provider
if you want to write log into slack, you create a SlackProvider like this

```kotlin
@Serializable
data class SlackWebhookPayload(
    val text: String,
)

class SlackProvider(config: SlackProviderConfig): Provider {
    constructor(config: SlackProviderConfig.() -> Unit): this(SlackProviderConfig().apply(config))

    class SlackProviderConfig() : ProviderConfig {
        var webhook_url: String = ""

        override var level: level = LogLevel.DEBUG

        override var formatter: Formatter = DetailTextFormatter
    }

    private val webhookUrl = config.webhook_url
    private val logLevel = config.level
    private val formatter = config.formatter

    override fun write(name: String, msg: String, level: LogLevel, attr: Map<String, String>) {
        if(level.isEnabledFor(logLevel).not()){
            return
        }
        kotlin.runCatching {
            webhookUrl.httpPost()
                .appendHeader("Content-Type" to "application/json")
                .body(Json.encodeToString(text = formatter.format(msg, level, attr)))
                .response()
        }.getOrElse { println(it) }
    }

    override fun <T : LogStructure> write(
        name: String,
        msg: T,
        serializer: KSerializer<T>,
        level: LogLevel,
        attr: Map<String, String>
    ) {
        if(level.isEnabledFor(logLevel).not()){
            return
        }
        kotlin.runCatching {
            webhookUrl.httpPost()
                .appendHeader("Content-Type" to "application/json")
                .body(Json.encodeToString(text = formatter.format(msg, serializer, level, attr)))
                .response()
        }.getOrElse { println(it) }
    }
}
```

#### use your provider
now you can use SlackProvider to write the log into slack.

```kotlin
val logger = LoggerFactory()
        .addProvider(ConsoleProvider{
            formatter = DetailTextFormatter
            level = LogLevel.DEBUG
        })
        .addProvider(SlackProvider{
            webhook_url = "your slack webhook url"
            formatter = SimpleTextFormatter
            level = LogLevel.WARN
        })
        .getLogger()
```

#### print the log
```kotlin
logger.info("info level log")
// written the log only console

logger.error("error level log")
// written the log both of console and slack
```



## LogLevel
1. TRACE (all log)
2. DEBUG (ignore TRACE)
3. INFO (ignore DEBUG and TRACE)
4. WARN (only WARN or ERROR)
5. ERROR (only this)
6. OFF (no log will present)

# Document
https://milkcocoa0902.github.io/colotok/01-colotok-introduce.html