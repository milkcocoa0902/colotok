# Configuration

Colotok output the log where you specified by the provider and formatted with you passed.

## Get Logger

```Kotlin
val logger = ColotokLoggerContext()
    .addProvider(ConsoleProvider())
    .getLogger()
```

you can get the logger instance by `ColotokLoggerContext#getLogger()`

`ConsoleProvider` is a builtin provider which used for print the log into console

> if none of `addProvider()` is called, the logger will not print the log anywhere
> {style="note"}

## Print log

### Text Logging
use bellow methods for appropriately level

```Kotlin
logger.trace("message what happen")
logger.debug("message what happen")
logger.info("message what happen")
logger.warn("message what happen")
logger.error("message what happen")


// 2023-12-29T12:21:13.354328 (main)[TRACE] - message what happen, additional = {}
// 2023-12-29T12:21:13.354328 (main)[DEBUG] - message what happen, additional = {}
// 2023-12-29T12:21:13.354328 (main)[INFO] - message what happen, additional = {}
// 2023-12-29T12:21:13.354328 (main)[WARN] - message what happen, additional = {}
// 2023-12-29T12:21:13.354328 (main)[ERROR] - message what happen, additional = {}
```

or

```Kotlin
logger.atInfo {
    print("in this block")
    print("all of logs are printed out with INFO level")
}

// 2023-12-29T12:21:13.354328 (main)[INFO] - in this block, additional = {}
// 2023-12-29T12:21:13.356133 (main)[INFO] - all of logs are printed out with INFO level, additional = {}
```
> if no formatter is passed to `ConsoleProvider()`, builtin `DetailTextFormatter` is used
> {style="note"}



### Structured Logging
Colotok also can print structured log using `kotlinx-serialization`.

> need for dependencies to kotlinx-serializations on your app
> {style="note"}

implement the log structure

```kotlin
@Serializable
class LogDetail(val scope: String, val message: String): LogStructure

@Serializable
class Log(val name: String, val logDetail: LogDetail): LogStructure
```

and use `DetailStructureFormatter` to format the log.

```Kotlin
val logger = ColotokLoggerContext()
    .addProvider(ConsoleProvider(ConsoleProviderConfig().apply {
        formatter = DetailStructureFormatter
    }))
    .getLogger()
```

then write the log
```Kotlin
logger.info(
    Log(
        name = "illegal state",
        LogDetail(
            "args",
            "argument must be greater than zero"
        )
    ),
    Log.serializer()
)

// {"message":{"name":"illegal state","logDetail":{"scope":"args","message":"argument must be greater than zero"}},"level":"INFO","date":"2023-12-29T12:34:56"}
```

## Shutdown and Flushing Logs

Since Colotok processes logs asynchronously, you should explicitly shut down the logger context to ensure all logs are flushed and resources are released before the application exits.

### Normal Shutdown
`shutdown()` will stop accepting new logs and wait for all queued logs to be processed by the providers.

```kotlin
val context = ColotokLoggerContext()
// ... setup providers and get logger ...

// At the end of application
context.shutdown()
```

### Force Shutdown
If you need to close the logger immediately without waiting for the queue to be cleared, use `forceShutdown()`.

```kotlin
context.forceShutdown()
```

### Manual Flush
If you only want to ensure that all current logs in the queue are written without shutting down the context, you can flush individual providers:

```kotlin
context.providers.forEach { it.flush() }
```