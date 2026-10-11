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

Scoped calls such as `atInfoAsync { print(...) }` accept a suspending block, but `print()` uses
non-blocking `Provider.write()`, including for `AsyncProvider`. Use `infoAsync(message)` or
`atAsync(level, message)` when you need to wait for an AsyncProvider's channel capacity.

## Custom integrations

Subclass `Provider` to process individual records, or `AsyncProvider` to publish batches.
Both classes provide queue processing, flush, and shutdown behavior. `ProviderConfig` defines
level, formatter, and metrics settings.

See [Create Plugin](Create-Plugin.md) for a custom provider example and
[Custom Formatter](Custom-Formatter.md) for formatting options.
