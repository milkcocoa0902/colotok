# Migrating from Colotok 0.5.0 to 1.0.0

This guide describes the source and behavior changes when upgrading from 0.5.0 to 1.0.0.
Update all Colotok modules together and rebuild your application and custom providers.

```kotlin
commonMain.dependencies {
    implementation("io.github.milkcocoa0902:colotok:1.0.0")
    // When using suspending logging calls or AsyncProvider:
    implementation("io.github.milkcocoa0902:colotok-coroutines:1.0.0")
}
```

Use the root coordinates in shared code; Gradle module metadata selects the platform variant.
The core, coroutines, and Loki modules support JVM, Android, JS/Node, iOS Arm64,
iOS Simulator Arm64, and macOS Arm64. CloudWatch and both SLF4J bindings are JVM-only.
Apple x86 targets and SwiftPM/XCFramework distribution are outside the 1.0.0 support range.

Colotok 1.0.0 is built with Kotlin/KGP 2.4.21. Use Kotlin 2.4.21 for JS/Node;
Kotlin 2.3.21 cannot read the JS KLIB ABI produced by 2.4.21. Match the serialization compiler
plugin to your application's Kotlin Gradle plugin.

## Logger creation and ownership

Create loggers with `ColotokLoggerContext.getLogger()`. The `ColotokLogger` constructors are
internal, and Logger no longer exposes `shutdown()` or `forceShutdown()`.

Before:

```kotlin
val logger = ColotokLogger("checkout") {
    providers = listOf(provider)
    defaultAttrs = mapOf("service" to "checkout")
}
// Later, from a suspend function:
logger.shutdown()
```

After:

```kotlin
val context = ColotokLoggerContext()
    .addProvider(provider)
    .withAttrs(mapOf("service" to "checkout"))
val logger = context.getLogger("checkout")
// Later, from a suspend function:
context.shutdown()
```

Retain the context for application shutdown. Its loggers share its Provider instances, so stopping
the context stops those destinations for every logger using them. `shallowCopy()` shares Provider
instances too; it does not create an independent provider lifecycle. Registering the same Provider
with multiple contexts also shares that destination and its shutdown effects.

## Forced shutdown and completion

`forceShutdown()` now requests cancellation without blocking on JVM, Android, JS/Node, or Native.
The method returns before worker termination and resource cleanup. Forced shutdown can discard
queued records and skips the final flush. Worker cancellation is cooperative: an operation that
ignores cancellation delays shutdown.

Use a completion callback to continue application shutdown after the Providers stop:

```kotlin
context.forceShutdown(applicationScope) { result ->
    result.exceptionOrNull()?.let { failure ->
        reportLoggingShutdownFailure(failure)
    }
    finishApplicationShutdown()
}
```

Define `applicationScope`, `reportLoggingShutdownFailure`, and `finishApplicationShutdown` in
your application.
Report shutdown failures through a destination outside the stopped logger context.
The callback may suspend and runs in the supplied scope's execution context after **all targeted
providers have terminated and completed resource cleanup**. It can also be registered after those
providers have stopped.

The result has the following meaning:

| Result | Meaning |
| --- | --- |
| `Result.success(Unit)` | Forced cancellation and cleanup completed without a recorded provider failure; queued records may still have been lost |
| `Result.failure(cause)` | A processing, flush, or close failure was recorded; the Context reports the first failure it observes |

A resource cleanup failure is reported through `Result.failure(cause)`. Handle that failure
according to the resource's recovery requirements.

The callback scope must remain active through notification. Scope cancellation may prevent or
interrupt the callback, but does not cancel the shutdown request. Callback exceptions follow that
scope's normal exception handling and are not stored as provider failures. Provider termination
does not wait for the callback to finish. Separate completion registrations have no ordering guarantee.
Provide appropriate exception handling on the application scope when callbacks can fail; callback
exceptions are not synchronously thrown from the force request.

For a single Provider, `provider.forceShutdown { result -> ... }` uses the Provider's own scope;
`provider.forceShutdown(applicationScope) { result -> ... }` selects an external scope. If the
Provider's entire scope is cancelled, its default notification can also be suppressed. Keeping
the process alive until the callback completes is the application's responsibility.

## Suspend completion and errors

For normal application shutdown, use `context.shutdown()` to drain accepted records, perform the
final flush, and release resources. It reports provider failures to the caller.

For a forced Provider shutdown, `join()` now waits for worker termination and cleanup **before**
reporting `ProviderClosedException`. If a provider failure was saved, the original failure takes
precedence. `context.shutdown()` after a force request waits and reports the same forced/failure
outcomes; it does not convert the operation into graceful success.

The completion callback considers expected forced cancellation a success. This differs from
`join()`, which retains its distinction between graceful and forced termination. Report expected
forced termination separately from actual resource-release failures.

`flush()` is valid only while the provider is open. After closure starts it reports
`ProviderClosedException`, unless a saved provider failure takes precedence.

## Custom provider implementations

Prefer subclasses of `Provider` or `AsyncProvider`. They inherit the new shutdown and notification
behavior. Direct implementations of `IProvider` must now supply these two methods:

```kotlin
fun forceShutdown()
fun forceShutdown(
    callbackScope: CoroutineScope,
    onComplete: suspend (Result<Unit>) -> Unit,
)
```

The no-argument method is now abstract. The overload taking only `onComplete` has a default
implementation that forwards to the explicit-scope overload using `coroutineScope`. This is an
API change for implementations that relied on the old no-op default. A direct implementation
owns cancellation, resource-release ordering, repeated requests, and notification semantics;
implementing the interface alone does not inherit the base classes' lifecycle guarantees.

`ColotokLoggerContext.addProvider()` accepts `Provider` instances. An implementation of
`IProvider` alone cannot be registered in a Context; extend `Provider` for Context integration.

A provider's hooks must not wait for their own `join()` or `flush()`, including from
`withContext(NonCancellable)`. The base classes reject those self-waits with `IllegalStateException`.
A hook may request forced shutdown and return; the application outside that worker observes
completion. See [Create Plugin](Create-Plugin.md) for the provider lifecycle and client ownership rules.

## Removed blocking wrappers

The custom `com.milkcocoa.info.colotok.util.runBlocking` and
`com.milkcocoa.info.colotok.core.coroutines.blocking` APIs have been removed.
Call suspending functions from a suspend context. From a synchronous entry point, use the
shutdown completion callback.

The upstream `kotlinx.coroutines.runBlocking` API can be used at blocking entry points such as
JVM `main` or ordinary test methods. Provider hooks must not block waiting for their own Provider.

## Event metadata and delivery behavior

Scoped calls such as `atInfoAsync { print(...) }` accept a suspending block, but `print()` uses
non-blocking `Provider.write()`, including for `AsyncProvider`. Use `infoAsync(message)` or
`atAsync(level, message)` when you need to wait for an AsyncProvider's channel capacity.

Records capture event time, attributes, MDC, thread name, and caller where supported at the
logging call. Delayed formatting and Loki/CloudWatch publication use that event time rather than
publication time. Update custom formatters or timestamp-based assertions that assumed output time.
JavaScript records use an empty caller value. The structured message object itself is not
copied: use immutable message types and avoid changing their fields or collections after logging.

`Provider.write()` is a non-blocking, best-effort enqueue attempt. Even with the default `SUSPEND`
overflow policy, it rejects the newest record when the channel is full. `AsyncProvider.writeAsync()`
waits for channel capacity; coroutine logging calls targeting a regular `Provider` retain that
Provider's non-blocking behavior. Enqueue acceptance and log-count metrics do not prove delivery.

`AsyncProvider.bufferSize` is a publish threshold in `1..4096`. Failed batches are retained for retry,
with retention limited to `min(bufferSize * 4, 4096)`; new records are dropped at that limit.
Publish is attempted at threshold multiples, at the retention limit, on explicit flush, and during
graceful shutdown. There is no time-based automatic flush. For low-volume applications, explicitly
flush open providers when required and shut down the Context before exiting.

Retrying a batch that was partly accepted can duplicate records. Queues and buffers are bounded
and forced shutdown can discard records, so neither at-least-once nor exactly-once delivery is
guaranteed. Make remote destinations tolerant of duplicates. Automatic publish failures retain
records for a later attempt; explicit/final flush failures propagate through `flush()` / `join()`.
The flush timeout includes enqueueing and processing the marker; timing out does not undo a marker
already enqueued or cancel all delivery work.

If publication fails when the retention buffer reaches its limit, subsequent incoming records
are dropped without another automatic publish attempt. Call `flush()` or perform graceful
shutdown to retry that full buffer. If this flush fails, the Provider enters its failed state;
subsequent lifecycle calls report the original failure.

## Platform and provider constraints

- Android console output is disabled by default. Supply `detectDebugModeFn = { BuildConfig.DEBUG }`
  from the Android source set, or explicitly enable release output.
- JVM/Android MDC supports coroutine context propagation. Android uses `MDC.asCoroutineContext()`;
  the `withMdcScope` convenience function is JVM-only. JS/Node uses `AsyncLocalStorage`, but Kotlin
  coroutine siblings do not have guaranteed MDC mutation isolation. Native MDC is thread-local
  and does not automatically follow coroutine thread switches. Each event captures a separate MDC snapshot.
- `FileProvider` reuses a lazily opened sink, flushes each write, and closes it at rotation,
  explicit flush, shutdown, or failure. Explicit flush releases the sink; the next record reopens it.
  An unused Provider does not open the file during shutdown. Rotation runs after writes and flushes,
  without a background timer.
- A Loki client created internally is provider-owned and released after worker cleanup. An injected
  `httpClient` is caller-owned; close it after provider shutdown.

`IProvider.channel`, `job`, and `coroutineScope`, `LogRecord.Pin`, and
`Provider.effectiveMetricsCollector` are public extension points in 1.0.0. See [public extension points](Create-Plugin.md#public-extension-points) for ownership and usage rules.

Structured formatters support nested and mixed arrays, including primitive values and nulls.
Field masking also traverses objects inside arrays. See [field masking](Custom-Formatter.md#field-masking)
for details.

## Binary compatibility

Constructor visibility, removed Logger methods, new abstract interface methods, and removed
blocking wrappers break binary compatibility with 0.5.0. Update the affected calls and rebuild
applications and custom Providers when upgrading to 1.0.0.
