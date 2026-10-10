# Configuration

Colotok output the log where you specified by the provider and formatted with you passed.

## Get Logger

Create loggers through `ColotokLoggerContext` and retain the context for application shutdown.
In the upcoming 1.0.0 API, `ColotokLogger` constructors are internal and loggers no longer expose
`shutdown()` or `forceShutdown()`. See [Migrating to 1.0](Migration-to-1.0.md).

```Kotlin
val context = ColotokLoggerContext()
    .addProvider(ConsoleProvider(ConsoleProviderConfig()))
val logger = context.getLogger()
```

you can get the logger instance by `ColotokLoggerContext#getLogger()`

`ConsoleProvider` is a builtin provider which used for print the log into console

> if none of `addProvider()` is called, the logger will not print the log anywhere
> {style="note"}

On Android, the default `ConsoleProvider()` does not write to Logcat. From your Android source set,
supply the debug-mode decision explicitly when debug output is wanted:

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
Colotok can also print structured logs using `kotlinx.serialization`.

> Apply the Kotlin serialization compiler plugin in your application. The `colotok` artifact
> already exposes the serialization runtime used by its public API.
> {style="note"}

Implement the log structure. The serialization plugin generates the serializer used by the logger.

```kotlin
@Serializable
data class LogDetail(val scope: String, val message: String): LogStructure

@Serializable
data class Log(val name: String, val logDetail: LogDetail): LogStructure
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
        logDetail = LogDetail(
            scope = "args",
            message = "argument must be greater than zero"
        )
    )
)

// {"message":{"name":"illegal state","logDetail":{"scope":"args","message":"argument must be greater than zero"}},"level":"INFO","thread":"main","date":"2023-12-29T12:34:56"}
```

## Shutdown and Flushing Logs

Since Colotok processes logs asynchronously, stop application logging and explicitly shut down the logger context before the application exits.

The following lifecycle contract describes the upcoming 1.0.0 API. A logger uses its context's
providers; stopping the context stops those destinations for all loggers that share them.
`shallowCopy()` also shares Provider instances, so stopping a copied context affects the original
context's shared destinations. Copies do not have independent provider lifetimes.

### Normal Shutdown
`shutdown()` gracefully closes each configured provider and suspends until its queued logs have been processed, its provider-specific buffer has been flushed, and its resources have been closed. Do not continue logging concurrently with shutdown.

```kotlin
suspend fun main() {
    val context = ColotokLoggerContext()
    // ... setup providers and get logger ...

    // At the end of the application
    context.shutdown()
}
```

### Force Shutdown
Use `forceShutdown()` to stop accepting records and request cancellation of the provider workers.
This call returns without waiting for worker termination or resource release on every target,
including JVM, Android, JS/Node, and Native.

Queued records may be lost, and an in-progress operation is cancelled cooperatively. Forced
shutdown does not start a final flush. The close hook runs after the operation's cleanup; a custom
operation that ignores cancellation can delay resource release. Returning from this method is
not permission to terminate the process if logging cleanup is still required.

```kotlin
context.forceShutdown()
```

To continue application shutdown after cleanup, use the completion overload:

```kotlin
context.forceShutdown(applicationScope) { result ->
    result.exceptionOrNull()?.let { failure ->
        reportLoggingShutdownFailure(failure)
    }
    finishApplicationShutdown()
}
```

`applicationScope` is an application-owned `CoroutineScope`; the reporting and finishing functions
are application-specific. The callback is a suspend lambda and is dispatched in the supplied
scope's execution context. It is scheduled after **all targeted workers and their resource cleanup**
have finished, including when the context has no providers or the providers have already stopped.
Resource release is performed once; each completion registration can be notified once.
If a close hook fails, the callback reports that failure; do not assume every external resource
was successfully released. Report shutdown failures through a destination outside the stopped
logger context.

- `Result.success(Unit)` means cancellation and cleanup finished without a recorded provider
  failure. It does **not** guarantee delivery of queued records.
- `Result.failure(cause)` reports a recorded processing, flush, or close failure. The context
  reports the first failure it observes after all targeted providers have finished.
- The callback is not part of provider termination. Callback exceptions belong to the supplied
  scope's normal exception handling and do not change the provider result.
- The scope must remain active until notification completes. Cancelling it can suppress or
  interrupt the callback, but does not undo the shutdown request. Process termination can also
  prevent notification. No callback ordering across separate requests is guaranteed.

Supply an application scope with appropriate exception handling if the callback can fail; its
exception is not thrown synchronously from the `forceShutdown()` call.

For a single Provider, `provider.forceShutdown { result -> ... }` uses the Provider's own scope.
`provider.forceShutdown(applicationScope) { result -> ... }` selects an external notification
scope. Cancelling the Provider's entire scope can suppress the default notification.

From a suspend function, `provider.join()` is another completion boundary: after forced shutdown
it waits for the worker and cleanup, then throws `ProviderClosedException` to report forced
termination. If a provider failure was recorded, that original failure is thrown instead.
Calling `context.shutdown()` after forcing its providers similarly waits and reports their
forced termination or failure; it does not convert forced termination into graceful success.

### Manual Flush
If you only want to ensure that records accepted before a point are processed without shutting down the context, flush individual providers while they are open:

```kotlin
suspend fun flushLogs(logger: ColotokLogger) {
    logger.providers.forEach { it.flush() }
}
```

`Provider.close()` starts graceful closure but does not wait. `Provider.join()` starts closure when
necessary and suspends until worker termination and resource release. `flush()` is only valid
while open: after graceful or forced closure starts it throws `ProviderClosedException`, unless
a recorded provider failure takes precedence. Lifecycle hooks must not wait for their own
provider's `join()` or `flush()`; these self-waits are rejected with `IllegalStateException`.

## Event snapshots and delivery

The event timestamp, thread, caller where supported, attributes, and MDC are snapshotted at the logging call. Delayed formatting and remote publication keep that original event time. Mutating the original maps afterwards does not change output, and every provider receives the same event snapshot for that call. JavaScript currently leaves caller empty because no portable JS call-site implementation is provided.

On JS/Node, MDC follows Node `AsyncLocalStorage` resources. Root operations, nested scope restoration, and native async-chain propagation are supported. Kotlin coroutine siblings are not guaranteed to have isolated MDC mutation because a coroutine `Job` is not always a separate Node async resource. Logging-call snapshots remain isolated.

On Kotlin/Native, MDC is thread-local. Basic operations and logging-call snapshots are supported on the current thread; automatic propagation or isolation across coroutine thread switches is not guaranteed.

For an `AsyncProvider`, `bufferSize` is a publish threshold in `1..4096`. Failed batches are retained up to `min(bufferSize * 4, 4096)` records. When that capacity is exhausted, the newest incoming record is dropped, preserving the older records for retry. If a remote destination accepts part of a batch before the provider reports failure, retrying the retained batch can produce duplicates; integrations should be designed for at-least-once delivery.

An automatic publish failure is recorded and can be retried by a later publish attempt. A failure from an explicit `flush()` is propagated and moves the provider to its failed terminal state; `join()` reports the same original failure.
