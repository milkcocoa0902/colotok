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

On Android, the default `ConsoleProvider()` does not write to Logcat. Supply the debug-mode decision explicitly when debug output is wanted:

```Kotlin
val logger = ColotokLoggerContext()
    .addProvider(ConsoleProvider {
        detectDebugModeFn = { BuildConfig.DEBUG }
    })
    .getLogger()
```

Output is enabled only when `isOutputEnabled && (isEnabledForRelease || detectDebugModeFn())` is true. The defaults are `true`, `false`, and `{ false }`, respectively, so the default result is no output. Colotok does not infer `BuildConfig.DEBUG`.

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
`shutdown()` stops accepting new logs and suspends until all queued logs have been processed, provider buffers have been flushed, and resources have been closed.

```kotlin
val context = ColotokLoggerContext()
// ... setup providers and get logger ...

// At the end of application
context.shutdown()
```

### Force Shutdown
If you need to close the logger immediately without waiting for the queue to be cleared, use `forceShutdown()`.

Queued records may be lost. The provider close hook still runs.

```kotlin
context.forceShutdown()
```

### Manual Flush
If you only want to ensure that records accepted before a point are processed without shutting down the context, flush individual providers while they are open:

```kotlin
context.providers.forEach { it.flush() }
```

`Provider.close()` starts graceful closure but does not wait. `Provider.join()` starts closure when necessary and suspends until completion. Once graceful or forced closure has started, `flush()` throws `ProviderClosedException`.

## Event snapshots and delivery

The event timestamp, thread, caller where supported, attributes, and MDC are snapshotted at the logging call. Delayed formatting and remote publication keep that original event time. Mutating the original maps afterwards does not change output, and every provider receives the same event snapshot for that call. JavaScript currently leaves caller empty because no portable JS call-site implementation is provided.

On JS/Node, MDC follows Node `AsyncLocalStorage` resources. Root operations, nested scope restoration, and native async-chain propagation are supported. Kotlin coroutine siblings are not guaranteed to have isolated MDC mutation because a coroutine `Job` is not always a separate Node async resource. Logging-call snapshots remain isolated.

On Kotlin/Native, MDC is thread-local. Basic operations and logging-call snapshots are supported on the current thread; automatic propagation or isolation across coroutine thread switches is not guaranteed.

For an `AsyncProvider`, `bufferSize` is a publish threshold in `1..4096`. Failed batches are retained up to `min(bufferSize * 4, 4096)` records. When that capacity is exhausted, the newest incoming record is dropped, preserving the older records for retry. If a remote destination accepts part of a batch before the provider reports failure, retrying the retained batch can produce duplicates; integrations should be designed for at-least-once delivery.

An automatic publish failure is recorded and can be retried by a later publish attempt. A failure from an explicit `flush()` is propagated and moves the provider to its failed terminal state; `join()` reports the same original failure.
