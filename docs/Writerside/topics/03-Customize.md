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

MDC is thread-local on JVM, Android, and Native targets. On JS/Node it follows
`AsyncLocalStorage` resources. Logging-call snapshots remain isolated from later MDC mutations.
The built-in formatters do not include MDC by default; a custom formatter can select a `%{key}` text
placeholder or an `Element.CUSTOM` structured field.

## Context lifecycle

Providers process records asynchronously. Call `context.shutdown()` from a suspend context at the
end of the application to drain records and release provider resources. Use `forceShutdown()` only
when queued records may be discarded.
