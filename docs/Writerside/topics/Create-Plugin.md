# Create Plugin

A custom provider connects Colotok to a destination that is not built in. Use `Provider` for a destination that handles one record at a time, or subclass `AsyncProvider` when a remote service benefits from batches.

`AsyncProvider` is the preferred base for remote delivery because it already owns the channel, batching threshold, retention policy, flush behavior, and lifecycle. Do not add another queue unless the destination protocol requires a distinct durable store.

## Dependencies

Add the core and coroutine artifacts to the custom provider module:

```kotlin
dependencies {
    implementation("io.github.milkcocoa0902:colotok:<version>")
    implementation("io.github.milkcocoa0902:colotok-coroutines:<version>")
}
```

## Define a transport boundary

Keep the network library outside the provider's buffering logic. A small interface makes ownership explicit and lets tests use a deterministic fake.

```kotlin
interface WebhookSender {
    /** Returns only after the destination accepts the complete batch. */
    suspend fun send(endpoint: String, messages: List<String>)
}
```

The application can implement this interface with Ktor, OkHttp, or another client. Treat non-success responses as failures by throwing from `send`; silently swallowing them would make `AsyncProvider` discard records as if publication succeeded.

## Define the configuration

```kotlin
import com.milkcocoa.info.colotok.core.formatter.builtin.text.SimpleTextFormatter
import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.metrics.MetricsCollectorSpec
import com.milkcocoa.info.colotok.core.provider.details.AsyncProviderConfig

class WebhookProviderConfig : AsyncProviderConfig {
    override var level: Level = LogLevel.INFO
    override var formatter: Formatter = SimpleTextFormatter
    override var metricsSpec: MetricsCollectorSpec = MetricsCollectorSpec.Inherit
    override var enableInternalMetricsLogging: Boolean = false
    override var bufferSize: Int = 50

    var endpoint: String? = null
    var sender: WebhookSender? = null
}
```

`bufferSize` is the threshold at which Colotok attempts a batch publish. Its valid range is `1..4096`.

## Implement the provider

```kotlin
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.provider.details.AsyncProvider

class WebhookProvider(
    config: WebhookProviderConfig,
) : AsyncProvider(validateWebhookConfig(config)) {
    constructor(configure: WebhookProviderConfig.() -> Unit) :
        this(WebhookProviderConfig().apply(configure))

    private val endpoint = requireNotNull(config.endpoint)
    private val sender = requireNotNull(config.sender)
    private val formatter = config.formatter

    override suspend fun onPublish(records: List<LogRecord>) {
        // AsyncProvider calls this outside its buffer lock.
        // Let failures escape so the records remain retained for retry.
        sender.send(
            endpoint = endpoint,
            messages = records.map { it.format(formatter) },
        )
    }
}

private fun validateWebhookConfig(
    config: WebhookProviderConfig,
): WebhookProviderConfig = config.apply {
    require(!endpoint.isNullOrBlank()) { "endpoint is required" }
    requireNotNull(sender) { "sender is required" }
}
```

Validate required values in the expression passed to `AsyncProvider`. This ensures invalid configuration fails before the base class starts its worker.

The example has no custom mutable buffer, does not block a coroutine with `runBlocking`, and does not perform network I/O while holding a provider-owned lock. Publication errors propagate to the base class instead of being printed or ignored.

## Use the provider

```kotlin
val provider = WebhookProvider {
    endpoint = "https://example.invalid/logs"
    sender = applicationWebhookSender
    level = LogLevel.WARN
    bufferSize = 50
}

val context = ColotokLoggerContext()
    .addProvider(provider)

val logger = context.getLogger("application")
logger.warn("delivery is delayed")

// At application shutdown; this suspends until graceful completion.
suspend fun stopLogging() {
    context.shutdown()
}
```

## Delivery and lifecycle contract

- `write` accepts records only while the provider is open. Logger attributes and MDC have already been snapshotted when the provider receives a record.
- `flush()` waits for records accepted before its marker and publishes the provider-specific buffer. It is valid only while the provider is open.
- `close()` starts graceful closure without waiting. `join()` closes when necessary and waits for the worker, final flush, and close hook.
- `forceShutdown()` cancels immediately. Queued records may be lost, but the close hook is still invoked.
- After graceful or forced closure starts, `flush()` throws `ProviderClosedException`.

On automatic publish failure, `AsyncProvider` retains records up to `min(bufferSize * 4, 4096)` and can retry them on a later publish attempt. At the retention limit it drops the newest incoming record, preserving the older failed batch. A failure during an explicit `flush()` is propagated to the caller and puts the provider in its failed terminal state; `join()` reports the same original failure.

Delivery is at least once, not exactly once. If a transport accepts part of a batch and then throws, the complete retained batch can be retried and accepted messages may appear again. Prefer an idempotency key or destination-side deduplication when duplicates matter.

## Resource ownership

Decide whether the provider or caller owns its network client:

- A client constructed internally should be closed exactly once from `onClosed()`.
- An injected client is usually caller-owned and should not be closed by the provider.
- Lazy initialization prevents an unused provider from opening resources merely because it is closed.

Document the chosen ownership rule in the configuration API. Avoid guessing ownership from the client type.

## Testing

Test the provider through a fake `WebhookSender` rather than a live network endpoint. At minimum, verify:

1. formatting and record order in a successful batch;
2. failure propagation from `onPublish` and retention until a later retry;
3. graceful `join()` performs the final publish;
4. forced shutdown may drop queued records but closes owned resources;
5. injected resources remain caller-owned;
6. invalid configuration fails before creating a worker or network client.

Also test the transport adapter separately for request encoding, authentication, non-success responses, timeouts, and client closure.
