# Provider

Colotok has built-in providers. A provider receives accepted log records and writes them to a
destination.

## Common Provider Configuration

Every provider config exposes the following options. The defaults below apply to the core built-in
providers (`ConsoleProvider`, `FileProvider`, and `StreamProvider`); integration providers may
choose different level and formatter defaults, as listed in the [official plugin guide](Official-Plugin.md).

| Property | Description | Default |
| :--- | :--- | :--- |
| `level` | Minimum log level for this provider | `LogLevel.DEBUG` |
| `formatter` | Formatter to use | `DetailTextFormatter` |
| `metricsSpec` | How to collect metrics (`Inherit`, `Explicit`, `NoOp`) | `MetricsCollectorSpec.Inherit` |
| `enableInternalMetricsLogging` | Whether to log metrics as internal records (`LogRecord.Metrics`) | `false` |

## Log levels and filtering

Levels are ordered from least to most severe:

`TRACE < DEBUG < INFO < WARN < ERROR < OFF`

Each provider applies its own `level` as the minimum accepted level. A record is accepted by that
provider when `record.level >= provider.level`.

| Provider level | Accepted standard log levels |
| :--- | :--- |
| `TRACE` | `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR` |
| `DEBUG` | `DEBUG`, `INFO`, `WARN`, `ERROR` |
| `INFO` | `INFO`, `WARN`, `ERROR` |
| `WARN` | `WARN`, `ERROR` |
| `ERROR` | `ERROR` only |
| `OFF` | No standard `TRACE` through `ERROR` records |

`OFF` is a threshold above `ERROR`; it disables all standard logging methods for that provider.
Because thresholds belong to providers, the same logger can send a `DEBUG` event to one provider
while another provider configured at `INFO` filters it out. Filtering happens before enqueue and
does not emit runtime metrics for the filtered record.

## ConsoleProvider
`ConsoleProvider` writes log records to the platform console.

```Kotlin
....
.addProvider(ConsoleProvider(ConsoleProviderConfig().apply {
    level = LogLevel.DEBUG
    formatter = DetailTextFormatter
    colorize = true

    traceLevelColor = AnsiColor.WHITE
    debugLevelColor = AnsiColor.BLUE
    infoLevelColor = AnsiColor.GREEN
    warnLevelColor = AnsiColor.YELLOW
    errorLevelColor = AnsiColor.RED
}))
```

On Android, `ConsoleProvider()` does not write to Logcat with its default configuration. From your
Android source set, provide the debug-mode decision explicitly when debug output is wanted:

```Kotlin
.addProvider(ConsoleProvider {
    detectDebugModeFn = { BuildConfig.DEBUG }
})
```

| `isOutputEnabled` | `isEnabledForRelease` | `detectDebugModeFn()` | Output |
| :---: | :---: | :---: | :---: |
| `false` | any | any | disabled |
| `true` | `true` | any | enabled |
| `true` | `false` | `true` | enabled |
| `true` | `false` | `false` | disabled (default) |

Colotok does not infer debug builds from `BuildConfig.DEBUG`.

On JVM and Native targets, `ConsoleProvider` can colorize output with ANSI colors. The color
properties are platform-specific; Android's `ConsoleProvider` writes to Logcat and does not expose
these color properties.

## FileProvider
FileProvider writes the log into a file asynchronously. It lazily opens and reuses the sink,
flushes each write, and closes it on rotation, explicit flush, shutdown, or failure. After a flush,
the next record reopens the sink; an unused provider does not open a file merely to shut down.

`FileProvider` accepts an Okio `Path`. Okio is exposed transitively by the `colotok` artifact; add a
direct Okio dependency only when your own code uses Okio APIs.

```Kotlin
import okio.Path.Companion.toPath

.addProvider(FileProvider("test.log".toPath()) {
    level = LogLevel.TRACE
    formatter = DetailTextFormatter
    // use size base rotation
    rotation = SizeBaseRotation(size = 8192L)
    
    // Metrics configuration
    enableInternalMetricsLogging = true
    metricsSpec = MetricsCollectorSpec.Inherit
})
```

FileProvider can rotate log files using `rotation`. A rotated file keeps its original name
with an increasing suffix: for example, `test.log.1`, `test.log.2`, and so on. When the path passed to `FileProvider` is an
existing directory, the active file is `application.log` inside that directory.

#### SizeBaseRotation
This rotation is checked after writes and explicit flushes. It runs when the file size is greater than `size` bytes.

```Kotlin
rotation = SizeBaseRotation(size = 8192L)
```

#### DateBaseRotation
This rotation is checked after writes and explicit flushes. It runs when the file's creation time (or, when unavailable, last modified
time) is at least `period` old. Without either timestamp it does not rotate. There is no background rotation timer.
```Kotlin
import kotlin.time.Duration.Companion.days

rotation = DateBaseRotation(period = 7.days)
```

## StreamProvider
`StreamProvider` writes log records to an Okio sink.

`StreamProvider` accepts an Okio `Sink` factory. Okio is exposed transitively by the `colotok`
artifact; add a direct Okio dependency only when your own code uses Okio APIs.

The factory is called for each record. StreamProvider writes, flushes, and closes the returned
Sink for that record. Return a new Sink on each call; returning the same Sink causes later writes
to use a closed resource. The default factory returns a blackhole Sink and discards output.

```Kotlin
import okio.blackholeSink

val logger = ColotokLoggerContext()
    .addProvider(StreamProvider {
        formatter = SimpleStructureFormatter
        outputStreamBuilder = { blackholeSink() }
    })
    .getLogger()
```

## Provider lifecycle

Providers process queued records asynchronously. Call `flush()` while a Provider is open to
wait for accepted records and flush its output. `close()` starts graceful shutdown; `join()`
waits for worker termination and resource cleanup. Use `ColotokLoggerContext.shutdown()` to
shut down all Providers in a Context.

Once shutdown starts, `flush()` throws `ProviderClosedException`. If the Provider recorded a
processing or cleanup failure, lifecycle methods report that original failure instead.

`forceShutdown()` requests cancellation and returns before worker termination and resource
cleanup. Use a completion callback or `join()` to wait for cleanup. After forced shutdown,
`join()` waits for cleanup and throws `ProviderClosedException`, or the original Provider
failure if one was recorded.

See [shutdown and completion](02-configuration.md#force-shutdown) and
[the 1.0.0 migration guide](Migration-to-1.0.md) for examples.

## AsyncProvider buffering

Scoped calls such as `atInfoAsync { print(...) }` accept a suspending block, but `print()` uses
non-blocking `Provider.write()`, including for `AsyncProvider`. Use `infoAsync(message)` or
`atAsync(level, message)` when you need to wait for an AsyncProvider's channel capacity.

`Provider.write()` attempts to enqueue a record without blocking. With the default `SUSPEND`
overflow policy, a full channel rejects the newest record and preserves previously accepted
records in FIFO order. `AsyncProvider.writeAsync()` suspends until channel capacity is available.
Coroutine logging extensions targeting a regular `Provider` use its non-blocking `write()` method.

`bufferSize` is the publish threshold, with a valid range of `1..4096`. Failed batches are retained
for retry, up to `min(bufferSize * 4, 4096)` records. At the retention limit, new records are dropped
so previously buffered records can be retried.

If a destination accepts part of a batch before a request fails, retrying the batch can duplicate
records. Use idempotent requests or destination-side deduplication when duplicates matter.
Queue limits, retention limits, and forced shutdown can cause log loss; at-least-once delivery
is not guaranteed.

Publication is attempted at threshold multiples, at the retention limit, on explicit `flush()`,
and during graceful shutdown. There is no time-based automatic flush. For low-volume applications,
call `flush()` when output is needed before shutdown.

Automatic publish failures retain the batch for a later attempt. An explicit flush failure
propagates to the caller and puts the Provider in its failed state. Subsequent `join()` calls
report the original failure.

If publication fails when the retention buffer reaches its limit, subsequent incoming records
are dropped without another automatic publish attempt. Call `flush()` or perform graceful
shutdown to retry that full buffer. If this flush fails, the Provider enters its failed state;
subsequent lifecycle calls report the original failure.

Attributes and MDC are captured when the log call creates the record. Later changes to the
original maps do not affect formatter output.
