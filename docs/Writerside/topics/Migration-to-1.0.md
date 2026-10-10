# Migrating to Colotok 1.0

These notes describe the upcoming, unreleased 1.0.0 API and changes from the published 0.5.0 API.
They cover the lifecycle changes associated with [Issue #41](https://github.com/milkcocoa0902/colotok/issues/41).

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
Returning does not guarantee worker termination or resource release. Queued records can be lost,
and forced shutdown does not start a final flush. An operation that ignores cancellation can delay
cleanup; there is no guarantee that arbitrary application code can be stopped immediately.

Replace code that assumes cleanup is complete immediately after the force call with notification:

```kotlin
context.forceShutdown(applicationScope) { result ->
    result.exceptionOrNull()?.let { failure ->
        reportLoggingShutdownFailure(failure)
    }
    finishApplicationShutdown()
}
```

The application supplies `applicationScope`, its failure reporter, and its remaining shutdown work.
Report shutdown failures through a destination outside the stopped logger context.
The callback may suspend and runs in the supplied scope's execution context after **all targeted
providers have terminated and completed resource cleanup**. It can also be registered after those
providers have stopped. No polling, thread sleep, or Colotok blocking wrapper is needed.

The result has the following meaning:

| Result | Meaning |
| --- | --- |
| `Result.success(Unit)` | Forced cancellation and cleanup completed without a recorded provider failure; queued records may still have been lost |
| `Result.failure(cause)` | A processing, flush, or close failure was recorded; the Context reports the first failure it observes |

If resource cleanup fails, completion means the worker has stopped and the cleanup attempt has
finished; it does not guarantee that the failing resource was released successfully.

The callback scope must remain active through notification. Scope cancellation may prevent or
interrupt the callback, but does not cancel the shutdown request. Callback exceptions follow that
scope's normal exception handling and are not stored as provider failures. Provider termination
does not wait for the callback to finish. Separate completion registrations have no ordering guarantee.
Provide appropriate exception handling on the application scope when callbacks can fail; callback
exceptions are not synchronously thrown from the force request.

For a single Provider, `provider.forceShutdown { result -> ... }` uses the Provider's own scope;
`provider.forceShutdown(applicationScope) { result -> ... }` selects an external scope. If the
Provider's entire scope is cancelled, its default notification can also be suppressed. Keeping
the process alive until completion remains the application's responsibility.

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

`flush()` remains valid only while the provider is open. After closure starts it reports
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

A provider's hooks must not wait for their own `join()` or `flush()`, including from
`withContext(NonCancellable)`. The base classes reject those self-waits with `IllegalStateException`.
A hook may request forced shutdown and return; the application outside that worker observes
completion. See [Create Plugin](Create-Plugin.md) for the provider lifecycle and client ownership rules.

## Removed blocking wrappers

The custom `com.milkcocoa.info.colotok.util.runBlocking` and
`com.milkcocoa.info.colotok.core.coroutines.blocking` APIs have been removed.
Call suspending functions from a suspend context, or register shutdown completion through the
callback API when the entry point is synchronous. The force path no longer uses a blocking bridge
or a JS implementation that launches work while pretending to have waited for it.

This removal does not deprecate the upstream `kotlinx.coroutines.runBlocking` API. It remains an
application choice at suitable blocking entry points, such as JVM `main` or ordinary test methods;
do not use it inside a provider hook to wait for that same provider.

## Compatibility and release validation

These are public API changes, including constructor visibility, removed Logger methods, new
abstract interface methods, and removed wrappers. Updating the API/ABI baseline records the new
contract; it does not make old application binaries compatible. Recompile and migrate consumers
to the Context-based lifecycle API and updated custom-provider contract before adopting 1.0.0.

JVM, Node, and Android host lifecycle regressions have passed locally. Apple test sources compile
for iOS Arm64, iOS Simulator Arm64, and macOS Arm64; Apple runtime validation requires macOS CI.
Updated API/ABI baselines, published consumers, and the Writerside build/check must also be
validated before release.
