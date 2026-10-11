# Plugins

Colotok integration modules provide remote destinations, SLF4J bindings, and coroutine logging
extensions. Add the modules required by your application alongside the core library.

## Available modules

| Module | Purpose | Platforms |
| --- | --- | --- |
| `colotok-coroutines` | Suspending logging calls and batched `AsyncProvider` | JVM, Android, JS/Node, iOS Arm64, iOS Simulator Arm64, macOS Arm64 |
| `colotok-loki` | Grafana Loki destination | Same targets as core |
| `colotok-cloudwatch` | AWS CloudWatch Logs destination | JVM |
| `colotok-slf4j` | SLF4J 1.7 binding | JVM |
| `colotok-slf4j2` | SLF4J 2.x binding | JVM |

See [Official Plugins](Official-Plugin.md) for configuration and usage examples.

## Coroutine logging

Add the coroutine module to `commonMain`:

```kotlin
commonMain.dependencies {
    implementation("io.github.milkcocoa0902:colotok-coroutines:1.0.0")
}
```

Call the logging extensions from a suspend function:

```kotlin
suspend fun logRequest(logger: ColotokLogger) {
    logger.infoAsync("Request received")
}
```

For an `AsyncProvider`, the call waits for channel capacity. A regular `Provider` uses its
non-blocking `write()` implementation.

Choose the logging call according to the enqueue behavior you need:

- `atInfoAsync { print(...) }` provides a suspending block with non-blocking `print()` calls.
  Each `print()` uses `Provider.write()`, including for `AsyncProvider`.
- Message calls such as `infoAsync(message)` and `atAsync(level, message)` wait for channel
  capacity when the destination is an `AsyncProvider`.

## Custom integrations

Subclass `Provider` to process individual records, or `AsyncProvider` to publish batches.
Both classes provide queue processing, flush, and shutdown behavior. `ProviderConfig` defines
level, formatter, and metrics settings.

See [Create Plugin](Create-Plugin.md) for a custom provider example and
[Custom Formatter](Custom-Formatter.md) for formatting options.
