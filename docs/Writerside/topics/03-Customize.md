# Customize

Colotok keeps context-wide attributes and MDC values separate:

- context attributes are defaults attached to every record created by a logger;
- MDC values are ambient values attached to records created while they are present.

## Logger name and default attributes

Use `getLogger(name)` to give records a logger name. Use `withAttrs()` to replace the context's
default attributes, or `putAttrs()` to merge additional attributes into them.

```kotlin
val context = ColotokLoggerContext()
    .withAttrs(mapOf("service" to "checkout"))
    .putAttrs(mapOf("environment" to "production"))
    .addProvider(ConsoleProvider(ConsoleProviderConfig()))

val logger = context.getLogger("order-api")
logger.info("order accepted", mapOf("orderId" to "A-123"))
```

The per-call attributes override a context attribute with the same key. Attributes are copied when
the log call creates its record, so changing the original map later does not change the queued log.

## Mapped Diagnostic Context (MDC)

MDC is useful for request or operation values that should be available to subsequent log calls.
Values are captured in each record and can be read or removed through the common API:

```kotlin
MDC.put("requestId", "req-123")
logger.info("request started")

MDC.remove("requestId")
MDC.clear()
```

### Coroutine scope on JVM

On JVM, use `withMdcScope` around a coroutine operation. It captures the MDC present when the
scope starts, preserves it across suspension and dispatcher changes, and restores the outer MDC
when the scope exits.

```kotlin
import com.milkcocoa.info.colotok.core.logger.ColotokLogger
import com.milkcocoa.info.colotok.core.logger.MDC
import com.milkcocoa.info.colotok.core.logger.withMdcScope

suspend fun handleRequest(logger: ColotokLogger) {
    withMdcScope {
        MDC.put("requestId", "req-123")
        logger.info("request started")

        // requestId remains available after suspension or a dispatcher change.
        callDownstreamService()
    }
}
```

A child coroutine inherits an MDC snapshot from its creation point. Later mutations in the child,
parent, or a sibling coroutine are isolated from one another. A nested `withMdcScope` restores its
parent scope when it returns.

### Coroutine scope on Android

Android provides the same `MDCContext` propagation and restoration behavior, but it does not
currently expose the `withMdcScope` convenience function. Install the context element explicitly:

```kotlin
import com.milkcocoa.info.colotok.core.logger.ColotokLogger
import com.milkcocoa.info.colotok.core.logger.MDC
import kotlinx.coroutines.withContext

suspend fun handleRequest(logger: ColotokLogger) {
    withContext(MDC.asCoroutineContext()) {
        MDC.put("requestId", "req-123")
        logger.info("request started")
        callDownstreamService()
    }
}
```

### Scope on JS/Node

JS/Node stores MDC in Node `AsyncLocalStorage`. Use `MDC.withContext` or its `withMdcScope` alias
to create a scope. Nested scopes restore their parent, and native Node async-resource chains retain
the scoped values.

```kotlin
import com.milkcocoa.info.colotok.core.logger.ColotokLogger
import com.milkcocoa.info.colotok.core.logger.MDC

fun handleRequest(logger: ColotokLogger) {
    MDC.withContext {
        MDC.put("requestId", "req-123")
        logger.info("request started")
        startNodeAsyncOperation()
    }
}
```

The JS `withMdcScope` and `MDC.withContext` blocks are not suspending lambdas. Kotlin coroutine
siblings are not guaranteed to isolate MDC mutations because a coroutine `Job` is not always a
distinct Node async resource.

### Kotlin/Native

On Kotlin/Native, MDC is thread-local and no coroutine scope helper is provided. Basic operations
and logging-call snapshots work on the current thread, but automatic propagation or isolation is
not guaranteed when a coroutine switches threads.

On every platform, each log record captures a deep snapshot of its MDC at the logging call, so a
later mutation does not change an already queued event. The built-in formatters do not include MDC
by default; a custom formatter can select a `%{key}` text placeholder or an `Element.CUSTOM`
structured field.

## Context lifecycle

Providers process records asynchronously. Call `context.shutdown()` from a suspend context at the
end of the application to drain records and release provider resources. Use `forceShutdown()` only
when queued records may be discarded.
