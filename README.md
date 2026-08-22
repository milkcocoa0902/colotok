# COLOTOK
COLOTOK; Code-Base Logging Runtime  for Kotlin  

![](https://img.shields.io/static/v1?label=kotlin&message=2.1.20&color=magenta)
![](https://img.shields.io/static/v1?label=jdk&message=11&color=magenta)
[![](https://jitpack.io/v/milkcocoa0902/colotok.svg)](https://jitpack.io/#milkcocoa0902/colotok)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.milkcocoa0902/colotok.svg)](https://search.maven.org/artifact/io.github.milkcocoa0902/colotok)
[![codecov](https://codecov.io/gh/milkcocoa0902/colotok/graph/badge.svg?token=XGH4U42LVM)](https://codecov.io/gh/milkcocoa0902/colotok)

<img width="1442" height="723" alt="Colotok Concepts" src="https://github.com/user-attachments/assets/566c1e04-cb95-47ed-9436-ee9729eebc50" />

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
✅ Metrics Collection
　🌟 Built-in metrics (Accepted enqueue count, Error count, AsyncProvider buffer size)
　🌟 Write-duration extension point (Not emitted automatically by the runtime)
　🌟 Multiple collection strategies (Inherit, Explicit, Internal Logging)
　🌟 Composite metrics (Collect to multiple destinations simultaneously)


# Integration
basic dependency

```kotlin
dependencies {
    // add this line
    implementation("io.github.milkcocoa0902:colotok:0.5.0")
}
```

or when you use kotlin multiplatform(;KMP)

```kotlin
commonMain.dependncies{
    implementation("io.github.milkcocoa0902:colotok:0.5.0")
}

jvmMain.dependencies{
    implementation("io.github.milkcocoa0902:colotok-jvm:0.5.0")
}

androidMain.dependencies{
    implementation("io.github.milkcocoa0902:colotok-android:0.5.0")
}

jsMain.dependencies{
    implementation("io.github.milkcocoa0902:colotok-js:0.5.0")
}
```

# Plugins

Colotok provides several plugins to extend its functionality:

|       plugin       |                      artifact                      |             feature             |    Platform    |
|:------------------:|:--------------------------------------------------:|:-------------------------------:|:--------------:|
| colotok-coroutines | `io.github.milkcocoa0902:colotok-coroutines:0.5.0` |        coroutine support        | Multi Platform |
|   colotok-slf4j    |   `io.github.milkcocoa0902:colotok-slf4j:0.5.0`    | SLF4J 1.7.x bindings (JVM only) |      JVM       |
|   colotok-slf4j2   |   `io.github.milkcocoa0902:colotok-slf4j2:0.5.0`   |  SLF4J 2.x bindings (JVM only)  |      JVM       |
| colotok-cloudwatch | `io.github.milkcocoa0902:colotok-cloudwatch:0.5.0` |   send logs to AWS CloudWatch   |      JVM       |
|    colotok-loki    |    `io.github.milkcocoa0902:colotok-loki:0.5.0`    |    send logs to Grafana Loki    | Multi Platform |

Each SLF4J binding publishes the matching `slf4j-api` major as a transitive compile dependency.
Applications only need the selected Colotok binding unless they intentionally manage the SLF4J API version themselves.

# Dependencies

If you want to use **Structured Logging** or **Internal Metrics Logging**, you need to enable the Kotlin Serialization plugin in your project. 

Colotok already includes the necessary serialization libraries, so you generally don't need to add them manually unless you want to use a specific version.

```kotlin
plugins {
    // Required for @Serializable
    kotlin("plugin.serialization") version "2.1.10" 
}
```



# Usage
## Configuration
configure colotok with code.  
see below.

```kotlin
val logger = ColotokLoggerContext()
    .addProvider(ConsoleProvider())
    .getLogger()

```

On Android, a default `ConsoleProvider()` does not write to Logcat. Android output is enabled
only when `isOutputEnabled` is `true` and either release output is allowed or the supplied debug
detector returns `true`:

```kotlin
val logger = ColotokLoggerContext()
    .addProvider(ConsoleProvider {
        detectDebugModeFn = { BuildConfig.DEBUG }
    })
    .getLogger()
```

| `isOutputEnabled` | `isEnabledForRelease` | `detectDebugModeFn()` | Output |
| :---: | :---: | :---: | :---: |
| `false` | any | any | disabled |
| `true` | `true` | any | enabled |
| `true` | `false` | `true` | enabled |
| `true` | `false` | `false` | disabled (default) |

Colotok deliberately does not infer `BuildConfig.DEBUG`. Set `isOutputEnabled = false` when
output must remain disabled regardless of the other gates.

more details config
```Kotlin
val fileProvider: FileProvider
val logger = ColotokLoggerContext()
    .addProvider(ConsoleProvider(ConsoleProviderConfig().apply {
        // show above info level in console
        level = LogLevel.INFO
    }))
    .addProvider(FileProvider(File("test.log").toOkioPath()){
        level = LogLevel.INFO
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
logger.info("message what happen", mapOf("param1" to "a custom attribute"))

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

// {"message":"message what happen","level":"INFO","date":"2023-12-29"}
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

`StructuredFormatter` emits at most one JSON `date` field. `Element.DATETIME` takes precedence
over `Element.DATE` and `Element.TIME`; `DATE` plus `TIME` is treated as `DATETIME`. A single
`DATE` or `TIME` emits only that component. These choices are normalized silently.



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

You can create a `Provider` for a local destination or an `AsyncProvider` for a batched remote
destination. Prefer the provided lifecycle and buffering implementation instead of maintaining a
second queue inside a custom provider. See the [custom provider guide](https://milkcocoa0902.github.io/colotok/Create-Plugin.html).

`Provider.write()` performs a non-blocking enqueue attempt. With the default `SUSPEND` overflow
policy it does not wait for capacity; when the channel is full, the newest record is rejected and
the already accepted FIFO prefix is preserved. `AsyncProvider.writeAsync()` suspends until channel
capacity is available. Coroutine `*Async` calls targeting a regular `Provider` still delegate to
the same non-blocking `write()` path.



## LogLevel
1. TRACE (all log)
2. DEBUG (ignore TRACE)
3. INFO (ignore DEBUG and TRACE)
4. WARN (only WARN or ERROR)
5. ERROR (only this)
6. OFF (no log will present)


## MDC (Mapped Diagnostic Context)
Colotok supports MDC functionality since version 0.3.0, which allows you to add contextual information to your logs. MDC is designed to be coroutine-friendly.  
MDC replace the `Element.CUSTOM` when formatting.


```kotlin
// Set MDC values
MDC.put("requestId", "12345")
MDC.put("userId", "user-abc")

// Log with MDC context
logger.info("Processing request") // MDC values will be included automatically

// Clear specific MDC value
MDC.remove("userId")

// Clear all MDC values
MDC.clear()

// Using MDC with coroutines
// On JVM platform
suspend fun processRequest() {
    withMdcScope(Dispatchers.IO) {
        MDC.put("requestId", "12345")
        // MDC context is preserved across coroutine boundaries
        someAsyncOperation()
    }
}

// On JS/Node (native async-chain scope)
fun processRequest() {
    MDC.withContext {
        MDC.put("requestId", "12345")
        // MDC context is preserved within this block
        someOperation()
    }
}

// Alternative syntax on JS
fun processRequest() {
    withMdcScope {
        MDC.put("requestId", "12345")
        // MDC context is preserved within this block
        someOperation()
    }
}
```

On JS/Node, MDC follows Node `AsyncLocalStorage` resources. Root operations, nested scope restoration,
and values across a native async chain are supported. Kotlin coroutine siblings are not distinct Node
async resources in every resume path, so sibling-local MDC mutation is not guaranteed to be isolated.
Each `LogRecord` still takes a deep MDC snapshot when the logging call is made.

On Kotlin/Native, MDC is thread-local. Basic operations and log-call snapshots are supported on the
current thread, but automatic propagation or isolation across coroutine thread switches is not guaranteed.

## Metrics Configuration

Colotok can collect metrics about its operation.

```kotlin
val logger = ColotokLoggerContext()
    .withMetrics(CustomMetricsCollector()) // Global metrics collector
    .addProvider(ConsoleProvider {
        // This provider will inherit the global collector
        metricsSpec = MetricsCollectorSpec.Inherit 
    })
    .addProvider(LokiProvider {
        // This provider logs metrics to itself as LogRecord.Metrics
        enableInternalMetricsLogging = true
        // And also reports to an explicit collector, inheriting the global one too
        metricsSpec = MetricsCollectorSpec.Explicit(lokiCollector, inheritParent = true)
    })
    .getLogger()
```

Runtime metrics describe enqueue and buffering behavior; they do not by themselves confirm that a
destination accepted a record.

| Event | Log count | Error | Buffer size | Write duration |
|---|---:|---|---|---:|
| Accepted normal record | incremented after enqueue acceptance | none | updated by `AsyncProvider` | not emitted |
| Record filtered by level | none | none | none | not emitted |
| Full rejection from default synchronous `write()` | none | `buffer_full` | none | not emitted |
| Rejection after normal close or force shutdown | none | `provider_closed` | none | not emitted |
| Rejection after provider failure | none | `provider_failed` | none | not emitted |
| Async publish failure | no additional count | `publish_failed` | failed batch retained | not emitted |
| Newest record dropped at the retention limit | no additional count | `retention_limit_reached` | remains at the limit | not emitted |
| `LogRecord.Metrics` | no self-count | no self-error | no self-update | not emitted |
| Collector throws | best-effort metric is lost | no recursive error | no recursive update | not emitted |

The default synchronous `write()` is best-effort and reports `buffer_full` instead of waiting.
`AsyncProvider.writeAsync()` waits for capacity, so channel pressure alone does not produce a full
rejection. Terminal failure is still reported, and a provider failure is rethrown to the async
caller.

`recordWriteDuration()` remains part of `MetricsCollector` for custom instrumentation, but Colotok
does not currently call it automatically. A future built-in duration metric must first define
whether it measures queue wait, handler time, publish time, or end-to-end time.

Internal metrics logging writes `LogRecord.Metrics` back to the same provider. It shares the same
level filter and channel capacity and, for `AsyncProvider`, the same retention buffer. Internal
metrics can therefore be rejected under pressure. Metrics records do not emit metrics about
themselves, which prevents recursive growth. `MetricsCollectorSpec.NoOp` disables the base
collector; internal metrics logging can still be enabled separately.

## Logger Shutdown

Use graceful shutdown when queued records must be processed before application exit.

```kotlin
val context = ColotokLoggerContext()
    .addProvider(FileProvider(path))
    
val logger = context.getLogger()

// ... logging ...

// Stops acceptance and suspends until providers flush and close.
context.shutdown()

// Cancels immediately. Queued records may be lost.
context.forceShutdown()
```

Calling `Provider.close()` only starts graceful closure; call `Provider.join()` to wait for it.
`Provider.flush()` waits for records accepted before its flush marker while the provider remains
open. Calling `flush()` after graceful or forced closure has started throws
`ProviderClosedException`.

The event timestamp, thread, caller where supported, attributes, and MDC are captured when the logging call creates
the record. Delayed formatting or remote delivery therefore keeps the original event time, and later
attribute/MDC mutation does not change output. All providers for one log call receive the same event instance.
JavaScript currently leaves caller empty because no portable JS call-site implementation is provided.

`AsyncProvider.bufferSize` is the publish threshold (`1..4096`). After a publish failure, records
are retained up to four times that threshold, with an absolute cap of 4096. Once full, the newest
record is dropped so older retained records remain available for retry. A failed publish retains the
submitted records; a destination that accepted only part of a batch may therefore receive
duplicates on retry. Remote consumers should tolerate at-least-once delivery.

Automatic publish failures are recorded and retained for a later attempt. A failure during an
explicit `flush()` is returned to the caller and makes the provider fail; `join()` then reports the
same original failure.

# Document
https://milkcocoa0902.github.io/colotok/01-colotok-introduce.html
