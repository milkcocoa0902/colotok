# Colotok

**A Kotlin Multiplatform logging runtime for structured events, contextual metadata, and pluggable local or remote destinations.**

[![Maven Central](https://img.shields.io/maven-central/v/io.github.milkcocoa0902/colotok.svg)](https://central.sonatype.com/artifact/io.github.milkcocoa0902/colotok)
[![Quality](https://github.com/milkcocoa0902/colotok/actions/workflows/quality.yml/badge.svg)](https://github.com/milkcocoa0902/colotok/actions/workflows/quality.yml)
[![Codecov](https://codecov.io/gh/milkcocoa0902/colotok/graph/badge.svg?token=XGH4U42LVM)](https://codecov.io/gh/milkcocoa0902/colotok)
[![License](https://img.shields.io/github/license/milkcocoa0902/colotok)](LICENSE)

[Documentation](https://milkcocoa0902.github.io/colotok/01-colotok-introduce.html) ·
[Maven Central](https://central.sonatype.com/artifact/io.github.milkcocoa0902/colotok) ·
[Releases](https://github.com/milkcocoa0902/colotok/releases) ·
[Issues](https://github.com/milkcocoa0902/colotok/issues)

Colotok lets shared Kotlin code create one log event and route it to any combination of console,
file, stream, Grafana Loki, AWS CloudWatch, or your own provider. It includes structured logging,
MDC, configurable formatting, file rotation, runtime metrics, buffered remote delivery, and
explicit provider shutdown.

## Why Colotok?

- **Route once, deliver anywhere.** Attach multiple providers without coupling application log
  calls to a specific destination.
- **Keep events structured.** Log serializable event types or text with attributes, and capture
  MDC together with each event.
- **Use the same runtime across supported targets.** The core, coroutine, and Loki modules cover
  JVM, Android, JS/Node, iOS Arm64, iOS Simulator Arm64, and macOS Arm64.
- **Control delivery explicitly.** Providers define buffering, failure reporting, flushing, and
  graceful or forced shutdown instead of hiding delivery behind process termination.
- **Meet existing JVM code where it is.** Optional SLF4J 1.7 and 2.x bindings route existing
  SLF4J calls through Colotok.

Colotok is a particularly good fit when a KMP application needs more than platform-console output:
structured context, file rotation, a remote log destination, or a reusable provider pipeline.

<img width="1442" height="723" alt="A Colotok logger routes one log event to multiple providers and destinations" src="https://github.com/user-attachments/assets/566c1e04-cb95-47ed-9436-ee9729eebc50" />

## Quick start

Add Colotok `1.0.0` to your project:

Add the root artifact to `commonMain`. Gradle module metadata selects the appropriate target
variant; do not add target-suffixed coordinates such as `-jvm`, `-android`, or `-js` manually.

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.milkcocoa0902:colotok:1.0.0")
        }
    }
}
```

Create a context, attach a provider, and get a logger:

```kotlin
import com.milkcocoa.info.colotok.core.logger.ColotokLoggerContext
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProvider
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig

val context = ColotokLoggerContext()
    .addProvider(ConsoleProvider(ConsoleProviderConfig()))

val logger = context.getLogger("checkout")

logger.info("Order accepted", mapOf("orderId" to "A-42"))
logger.warn("Inventory is low")
```

When the application stops, close the context so accepted records are processed and provider
resources are released:

```kotlin
suspend fun stopLogging() {
    context.shutdown()
}
```

> [!IMPORTANT]
> Android console output is disabled by default. Configure `detectDebugModeFn`, or explicitly opt
> into release output, from the Android source set. Colotok does not infer your application's
> `BuildConfig.DEBUG`. See the [configuration guide](https://milkcocoa0902.github.io/colotok/02-configuration.html).

### Structured events

Enable the Kotlin serialization compiler plugin when your log event types use `@Serializable`.
Use the same version as the Kotlin Gradle plugin in your project.

Colotok 1.0.0 is built with Kotlin/KGP **2.4.21**. Use Kotlin 2.4.21 for JS/Node:
Kotlin 2.3.21 cannot read the JS KLIB ABI produced by 2.4.21.
See [compiler compatibility](quality/consumers/README.md#compiler-compatibility) for the tested compiler and platform combinations.

```kotlin
plugins {
    kotlin("plugin.serialization") version "2.4.21"
}
```

Use immutable event types: serialization runs in the Provider worker. Structured formatters
accept JSON-object messages and arrays of objects; unmasked arrays of primitive values or nested
arrays cause formatting to fail. See [formatter restrictions](docs/Writerside/topics/Custom-Formatter.md#field-masking).

Define an event type and select a structured formatter:

```kotlin
import com.milkcocoa.info.colotok.core.formatter.builtin.structure.DetailStructureFormatter
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import kotlinx.serialization.Serializable

@Serializable
data class CheckoutEvent(
    val orderId: String,
    val total: Long,
) : LogStructure

val logger = ColotokLoggerContext()
    .addProvider(
        ConsoleProvider(
            ConsoleProviderConfig().apply {
                formatter = DetailStructureFormatter
            },
        ),
    )
    .getLogger("checkout")

logger.info(
    CheckoutEvent(orderId = "A-42", total = 4200),
    mapOf("requestId" to "req-7f3"),
)
```

The serialization runtime required by Colotok's public API is already included. A separate runtime
dependency is normally unnecessary.

## Route logs to multiple destinations

Providers are independent destinations. A single context can write locally and remotely without
changing application log calls.

```kotlin
import com.milkcocoa.info.colotok.core.formatter.builtin.structure.DetailStructureFormatter
import com.milkcocoa.info.colotok.core.logger.ColotokLoggerContext
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProvider
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import com.milkcocoa.info.colotok.core.provider.loki.LokiProvider

val context = ColotokLoggerContext()
    .addProvider(ConsoleProvider(ConsoleProviderConfig()))
    .addProvider(
        LokiProvider {
            host = "http://localhost:3100"
            logStream = mapOf("app" to "checkout", "environment" to "local")
            formatter = DetailStructureFormatter
        },
    )

val logger = context.getLogger("checkout")
logger.info("Order accepted", mapOf("orderId" to "A-42"))
```

Add the Loki integration beside the core dependency:

```kotlin
commonMain.dependencies {
    implementation("io.github.milkcocoa0902:colotok-loki:1.0.0")
}
```

See [official integrations](https://milkcocoa0902.github.io/colotok/official-plugin.html) for Loki,
CloudWatch, coroutines, and SLF4J setup.

## Modules and supported targets

| Module | Purpose | Supported targets in 1.0.0 |
| --- | --- | --- |
| `colotok` | Core logger, formatters, MDC, console/file/stream providers, rotation, metrics | JVM, Android, JS/Node, iOS Arm64, iOS Simulator Arm64, macOS Arm64 |
| `colotok-coroutines` | Suspending log calls and buffered `AsyncProvider` support | JVM, Android, JS/Node, iOS Arm64, iOS Simulator Arm64, macOS Arm64 |
| `colotok-loki` | Buffered Grafana Loki provider | JVM, Android, JS/Node, iOS Arm64, iOS Simulator Arm64, macOS Arm64 |
| `colotok-cloudwatch` | Buffered AWS CloudWatch Logs provider | JVM |
| `colotok-slf4j` | SLF4J 1.7 binding | JVM |
| `colotok-slf4j2` | SLF4J 2.x binding | JVM |

Apple x86 targets (`iosX64` and `macosX64`) are not supported in 1.0.0. Check this table before
adopting Colotok for a target not explicitly listed above.

## Choosing a Kotlin logging library

Kotlin logging projects solve different layers of the problem. A *facade* supplies a logging API
and delegates delivery to another backend. A *logging runtime* owns event processing and output
components such as providers, writers, appenders, or sinks.

| Library | Primary role | Structured/context model | Documented outputs and integrations | Good fit when... |
| --- | --- | --- | --- | --- |
| **Colotok** | Provider-based KMP logging runtime | Serializable events, string attributes, MDC snapshots | Console, file, stream, Loki, CloudWatch, custom providers; JVM SLF4J 1.7/2.x bindings | Shared Kotlin code must route logs to local and remote destinations with explicit delivery lifecycle |
| [Kermit](https://github.com/touchlab/Kermit) | Composable KMP logger | Message, tag, throwable, custom `LogWriter` | Platform log writers; Crashlytics and Bugsnag through documented crash-reporting integrations | An application primarily needs composable platform-native logging and crash-reporting integration |
| [Napier](https://github.com/AAkira/Napier) | Lightweight KMP logger | Message, tag, throwable, custom `Antilog` | Android Logcat, Darwin output, Java Util Logging, JavaScript console; Crashlytics examples | An application wants a small common logging API over each platform's standard logger |
| [kotlin-logging](https://github.com/oshai/kotlin-logging) | Kotlin-friendly SLF4J facade; multiplatform support is documented as experimental | Fluent payload API; MDC and rendering depend on SLF4J/backend support | Delegates to the selected SLF4J backend | A JVM application already has an SLF4J/Logback/Log4j2 stack and mainly wants an idiomatic Kotlin API |
| [log4k](https://github.com/smyrgeorge/log4k) | KMP logging, tracing, and metering runtime | Structured tags, span context, JSON | Platform, JSON, and Flow appenders; OTLP integrations; JVM SLF4J bridges | One runtime should cover logging together with tracing, metrics, and OTLP-oriented observability |

The comparison reflects each project's documentation as of 2026-08-22. Target support and
integrations vary by module and release.

## Core capabilities

### Providers and formatting

- Built-in `ConsoleProvider`, `FileProvider`, and `StreamProvider`.
- Custom `Provider` for local destinations and buffered `AsyncProvider` for remote destinations.
- Plain, simple, and detailed text formatters.
- Simple and detailed structured formatters, plus custom formatters.
- Size-based and duration-based file rotation.

### Context and metrics

- Per-call attributes and MDC snapshots.
- JVM coroutine MDC scopes, Node `AsyncLocalStorage` integration, and thread-local Native behavior.
- Accepted-enqueue, rejection/error, and async-buffer metrics.
- Inherited, explicit, internal-logging, and composite metrics collectors.

Platform context propagation is not identical. On JS/Node, Kotlin coroutine siblings are not
guaranteed to isolate MDC mutation. On Kotlin/Native, MDC is thread-local and does not automatically
follow coroutine thread switches. Every log event still captures its own metadata snapshot at the
logging call.

## Delivery and lifecycle contract

Providers use the following delivery and shutdown behavior:

- `Provider.write()` is non-blocking and best-effort. With the default overflow policy, a full
  channel rejects the newest record and preserves the accepted FIFO prefix.
- `AsyncProvider.writeAsync()` suspends for channel capacity and rethrows a failed channel
  cause. Normal closure rejects the record through metrics without throwing. Scoped
  `atInfoAsync { print(...) }` calls use non-blocking `write()`; use message-based async calls
  when capacity waiting is required.
- Failed remote batches are retained within a bounded buffer. Retrying a retained batch can
  duplicate records if a destination accepted only part of the failed request; delivery is not
  exactly once, and bounded buffers can still reject or drop records under pressure.
- Batches are sent at threshold multiples, at the retention limit, on explicit `flush()`, and
  during graceful shutdown. There is no timer-based automatic flush; low-volume records can
  remain buffered until the application flushes or shuts down.
- A failed batch at the retention limit requires explicit flush or graceful shutdown to retry;
  subsequent incoming records are dropped without another automatic publish attempt.
- `flush()` waits for records accepted before its marker while the provider is open. Its timeout
  includes enqueueing the marker and waiting for its processing; the default is one second.
- `ColotokLoggerContext.shutdown()` gracefully drains and closes providers. In 1.0.0,
  `forceShutdown()` requests cancellation and may lose queued records. It returns
  without waiting for worker termination or resource release.

`context.forceShutdown(callbackScope, onComplete)` invokes the callback after all targeted
Providers stop and run their cleanup hooks. `Result.success(Unit)` indicates that no Provider
failure was recorded; forced shutdown can discard queued records. Keep the notification scope
active until the callback completes. Callback exceptions use that scope's exception handling. Create loggers through `context.getLogger()` and stop logging through the context.
See the [1.0 migration guide](docs/Writerside/topics/Migration-to-1.0.md) for changes from the
published 0.5.0 API, including Logger constructors, lifecycle methods, and `IProvider` implementations.

Read the [configuration guide](https://milkcocoa0902.github.io/colotok/02-configuration.html) and
[metrics reference](https://milkcocoa0902.github.io/colotok/04-metrics.html) before relying on
buffering or failure metrics in production.

## Documentation

- [Introduction and installation](https://milkcocoa0902.github.io/colotok/01-colotok-introduce.html)
- [Logger configuration and lifecycle](https://milkcocoa0902.github.io/colotok/02-configuration.html)
- [Provider configuration](https://milkcocoa0902.github.io/colotok/02-01-provider-configuration.html)
- [Formatter configuration](https://milkcocoa0902.github.io/colotok/02-02-formatter.html)
- [Metrics](https://milkcocoa0902.github.io/colotok/04-metrics.html)
- [Official integrations](https://milkcocoa0902.github.io/colotok/official-plugin.html)
- [Create a custom provider](https://milkcocoa0902.github.io/colotok/custom-provider.html)
- [Create a custom formatter](https://milkcocoa0902.github.io/colotok/custom-formatter.html)

## Support and contributing

Questions, bug reports, and focused pull requests are welcome through
[GitHub Issues](https://github.com/milkcocoa0902/colotok/issues). When reporting runtime behavior,
include the Colotok version, target, provider configuration, and a minimal reproduction where
possible.

Colotok is available under the [Apache License 2.0](LICENSE).
