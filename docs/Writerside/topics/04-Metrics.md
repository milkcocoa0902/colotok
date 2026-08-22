# Metrics

Colotok reports accepted enqueue attempts, provider rejection and publish failures, and
`AsyncProvider` retention-buffer size. These metrics do not by themselves confirm destination
delivery.

## MetricsCollector

`MetricsCollector` is an interface to collect metrics. You can implement this interface to send metrics to your monitoring system like Prometheus, InfluxDB, etc.

```kotlin
interface MetricsCollector {
    /**
     * Increment log count.
     * @param level log level
     * @param providerName provider name
     */
    fun incrementLogCount(level: Level, providerName: String)

    /**
     * Increment error count.
     * @param providerName provider name
     * @param errorType error type
     */
    fun incrementErrorCount(providerName: String, errorType: String)

    /**
     * Update buffer size.
     * @param providerName provider name
     * @param size current buffer size
     */
    fun updateBufferSize(providerName: String, size: Int)

    /**
     * Record write duration.
     * @param providerName provider name
     * @param durationMs duration in milliseconds
     */
    fun recordWriteDuration(providerName: String, durationMs: Long)
}
```

`recordWriteDuration()` is available for custom instrumentation, but Colotok has no built-in
production invocation. A future built-in duration metric must first define whether it measures
queue wait, handler time, publish time, or end-to-end time.

## Runtime Emission Matrix

| Event | Log count | Error | Buffer size | Write duration |
|---|---:|---|---|---:|
| Accepted normal record | incremented after enqueue acceptance | none | updated by `AsyncProvider` | not emitted |
| Record filtered by level | none | none | none | not emitted |
| Full rejection from default synchronous `write()` | none | `buffer_full` | none | not emitted |
| Rejection after normal close or force shutdown | none | `provider_closed` | none | not emitted |
| Rejection after provider failure | none | `provider_failed` | none | not emitted |
| Async publish failure | no additional count | `publish_failed` | failed batch retained | not emitted |
| Newest record dropped at the retention limit | no additional count | `retention_limit_reached` | remains at the limit | not emitted |
| `LogRecord.Metrics` | no self-count | no self-error | no self-update | not emitted |
| Collector throws | best-effort metric is lost | no recursive error | no recursive update | not emitted |

The log count is recorded when the provider channel accepts a record, not after destination
delivery. Filtered records and `LogRecord.Metrics` do not produce runtime metrics.

## Writing Paths and Backpressure

`Provider.write()` performs a non-blocking enqueue attempt. With the default `SUSPEND` overflow
policy it does not wait for capacity; when the channel is full, the newest record is rejected and
the accepted FIFO prefix remains queued.

`AsyncProvider.writeAsync()` waits until channel capacity is available, so channel pressure alone
does not produce a full rejection. Coroutine `*Async` calls targeting a regular `Provider` still
delegate to its non-blocking `write()` path. Rejection after normal close is reported as
`provider_closed`; a failed provider reports `provider_failed` and rethrows the failure to the async
caller.

## Metrics Selection Strategy

You can specify how to collect metrics for each provider using `MetricsCollectorSpec`.

### Inherit

The provider inherits the global collector configured in `ColotokLoggerContext`. This is the default.

```kotlin
val logger = ColotokLoggerContext()
    .withMetrics(globalCollector)
    .addProvider(ConsoleProvider(ConsoleProviderConfig().apply {
        metricsSpec = MetricsCollectorSpec.Inherit
    }))
    .getLogger()
```

### Explicit

The provider uses a specific collector. You can choose whether to also report to the global collector using the `inheritParent` flag.

```kotlin
val logger = ColotokLoggerContext()
    .withMetrics(globalCollector)
    .addProvider(LokiProvider {
        // Only report to lokiCollector
        metricsSpec = MetricsCollectorSpec.Explicit(lokiCollector, inheritParent = false)
    })
    .addProvider(FileProvider(...) {
        // Report to both fileCollector AND globalCollector
        metricsSpec = MetricsCollectorSpec.Explicit(fileCollector, inheritParent = true)
    })
    .getLogger()
```

### NoOp

The base or external metrics collector is disabled for the provider. Internal metrics logging can
still be enabled separately.

```kotlin
.addProvider(ConsoleProvider(ConsoleProviderConfig().apply {
    metricsSpec = MetricsCollectorSpec.NoOp
}))
```

## Internal Metrics Logging

By setting `enableInternalMetricsLogging = true`, you can output metrics events as `LogRecord.Metrics` to the provider itself. 
This is useful for debugging or monitoring without external infrastructure.

```kotlin
.addProvider(LokiProvider {
    enableInternalMetricsLogging = true
})
```

Internal metrics logging can be used alongside `MetricsCollectorSpec`. For example, you can report to a global Prometheus collector while also logging metrics events to a local file.

Internal metrics use the same provider level filter and channel. For `AsyncProvider`, they also use
the same retention buffer. They can be rejected under capacity pressure, but `LogRecord.Metrics`
does not emit further metrics, preventing recursive growth.
