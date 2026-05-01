# Metrics

Colotok can collect metrics about its operation, such as the number of logs output, the number of errors, and the time taken to write logs.

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

## Metrics Selection Strategy

You can specify how to collect metrics for each provider using `MetricsCollectorSpec`.

### Inherit

The provider inherits the global collector configured in `ColotokLoggerContext`. This is the default.

```kotlin
val logger = ColotokLoggerContext()
    .withMetrics(globalCollector)
    .addProvider(ConsoleProvider {
        metricsSpec = MetricsCollectorSpec.Inherit
    })
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

Metrics collection is disabled for the provider.

```kotlin
.addProvider(ConsoleProvider {
    metricsSpec = MetricsCollectorSpec.NoOp
})
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
