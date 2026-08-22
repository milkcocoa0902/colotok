# Official Plugin

Colotok provides several official plugins to extend its functionality for different use cases. This page describes each plugin, when to use it, and how to set it up.

## colotok-coroutines

The `colotok-coroutines` plugin provides suspending logging extensions for use from coroutine contexts, as well as the `AsyncProvider` base class for batched providers.

**Description**: The extensions create the log record once and dispatch writes to the configured providers concurrently. For an `AsyncProvider`, they suspend until its channel accepts the record. A regular `Provider` keeps the same best-effort, non-blocking enqueue behavior as its synchronous `write()` method. Use `AsyncProvider` for a channel-backed, batched remote provider.

**When to Use**: Use this plugin when your application uses Kotlin coroutines and you want to:
- Use suspending logging calls from coroutine code
- Integrate logging with your coroutine-based application flow
- Wait for channel capacity when writing to an `AsyncProvider`

**Setup**: Add the dependency to your project:

```kotlin
// Gradle Kotlin DSL
implementation("io.github.milkcocoa0902:colotok-coroutines:0.5.0")
```

**Usage**: The plugin provides async versions of all standard logging methods:

```kotlin
// Use in a coroutine context
runBlocking {
    // Async logging methods
    logger.traceAsync("Trace message")
    logger.debugAsync("Debug message")
    logger.infoAsync("Info message")
    logger.warnAsync("Warning message")
    logger.errorAsync("Error message")

    // With attributes
    logger.infoAsync("Info with attributes", mapOf("key" to "value"))

    // Scoped logging
    logger.atInfoAsync {
        // All logs in this block are at INFO level
        print("Log message 1")
        print("Log message 2")
    }
}
```

**Platform Support**: This plugin is published for the same targets as the core library: JVM, Android, JavaScript, iOS arm64, iOS Simulator arm64, and macOS arm64.

## colotok-cloudwatch (JVM only)

The `colotok-cloudwatch` plugin provides integration with Amazon CloudWatch Logs, allowing you to send logs directly to AWS CloudWatch.

**Description**: This plugin enables sending logs to AWS CloudWatch Logs service, with support for different credential providers and buffered logging.

**When to Use**: Use this plugin when:
- Your application runs on AWS infrastructure
- You want to centralize logs in CloudWatch
- You need to monitor logs across multiple instances
- You want to leverage CloudWatch's log analysis features

**Setup**: Add the dependency to your project:

```kotlin
// Gradle Kotlin DSL
implementation("io.github.milkcocoa0902:colotok-cloudwatch:0.5.0")
```

**Usage**: Configure the CloudWatch provider with your AWS credentials and log group/stream information:

| Property | Description | Default |
| :--- | :--- | :--- |
| `logGroup` | CloudWatch log group name | `null` |
| `logStream` | CloudWatch log stream name | `null` |
| `credential` | CloudWatch credentials | `null` |
| `level` | Minimum log level to publish | `DEBUG` |
| `formatter` | Formatter used for CloudWatch event messages | `SimpleStructureFormatter` |
| `bufferSize` | Publish threshold; retained failures may grow to `min(bufferSize * 4, 4096)` | `50` |


```kotlin
val logger = ColotokLoggerContext()
    .addProvider(CloudwatchProvider {
        // Set minimum log level
        level = LogLevel.INFO

        // Configure CloudWatch log group and stream
        logGroup = "your-application-logs"
        logStream = "instance-1"

        // Configure AWS credentials
        credential = CloudwatchCredential.StaticCredentials(
            region = "us-west-2",
            accessKeyId = "YOUR_ACCESS_KEY_ID",
            secretAccessKey = "YOUR_SECRET_ACCESS_KEY"
        )

        // Or use other credential types:
        // credential = CloudwatchCredential.Default(region = "us-west-2")
        // credential = CloudwatchCredential.Profile(region = "us-west-2", profileName = "default")
        // credential = CloudwatchCredential.FromEnvironments(region = "us-west-2")

        // Configure buffer size (optional)
        bufferSize = 50 // Default: publication is attempted at this threshold
    })
    .getLogger()

// Use the logger as normal
logger.info("This log will be sent to CloudWatch")
```

**Buffering and Flushing**: The CloudWatch provider buffers logs to improve performance. Publication is attempted when the buffer reaches the configured threshold (`bufferSize`), when you explicitly call `flush()`, and during graceful shutdown. After a failed attempt, the retained buffer is retried at later threshold multiples, at the retention limit, or on a flush.

```kotlin
// Get a reference to the provider
val cloudwatchProvider = CloudwatchProvider { /* config */ }
val logger = ColotokLoggerContext()
    .addProvider(cloudwatchProvider)
    .getLogger()

// Later, flush logs explicitly
runBlocking {
    cloudwatchProvider.flush()
}
```

**Platform Support**: This plugin is available for JVM platform only.

## colotok-slf4j, colotok-slf4j2 (JVM only)

The `colotok-slf4j` and `colotok-slf4j2` plugins allow Colotok to be used as an SLF4J backend.

**Description**: This plugin enables applications that use SLF4J for logging to use Colotok as the logging backend.

**When to Use**: Use this plugin when:
- Your application or its dependencies use SLF4J for logging
- You want to route SLF4J logs through Colotok
- You need to integrate with existing code that uses SLF4J

**Setup**: Add the dependency to your project:

```kotlin
// Gradle Kotlin DSL

// For SLF4J 1.7.x
implementation("io.github.milkcocoa0902:colotok-slf4j:0.5.0")

// For SLF4J 2.x
implementation("io.github.milkcocoa0902:colotok-slf4j2:0.5.0")
```

Each binding exposes its matching `slf4j-api` major as a transitive compile dependency. Do not add
another API dependency unless you intentionally control the SLF4J version. Keep only one SLF4J
provider/binding on the runtime classpath.

You'll also need to configure SLF4J to use Colotok as its implementation. This typically involves ensuring that the Colotok SLF4J binding is the only SLF4J implementation on the classpath.

**Usage**: Once configured, you can use SLF4J as normal, and the logs will be processed by Colotok:

```kotlin
// Get an SLF4J logger
val logger = LoggerFactory.getLogger("YourLoggerName")

// Use SLF4J logging methods
logger.trace("Trace message")
logger.debug("Debug message")
logger.info("Info message")
logger.warn("Warning message")
logger.error("Error message")

// With exception
try {
    // Some code that might throw
} catch (e: Exception) {
    logger.error("An error occurred", e)
}
```

**Platform Support**: This plugin is available for JVM platform only.  

> SLF4J bindings for Colotok will use `ColotokLoggerContext.DEFAULT`, so if you want to customize the logger context, you'll need to configure it before using SLF4J.
{style="note"}

## colotok-loki

The `colotok-loki` plugin provides integration with Grafana Loki, allowing you to send logs directly to a Loki server.

**Description**: This plugin enables sending logs to a Grafana Loki server using its HTTP API, with support for buffering and authentication.

**When to Use**: Use this plugin when:
- You use Grafana for visualization and monitoring
- You want to centralize logs in Loki
- You need advanced log querying capabilities
- You want to create dashboards and alerts based on log data

**Setup**: Add the dependency to your project:

```kotlin
// Gradle Kotlin DSL
implementation("io.github.milkcocoa0902:colotok-loki:0.5.0")
```

**Usage**: Configure the Loki provider with your Loki server information:


| Property | Description | Default |
| :--- | :--- | :--- |
| `host` | Loki host URL | `null` |
| `logStream` | Labels for Loki stream | `null` |
| `credential` | Loki credentials | `null` |
| `level` | Minimum log level to publish | `INFO` |
| `formatter` | Formatter used for Loki values | `SimpleTextFormatter` |
| `bufferSize` | Publish threshold; retained failures may grow to `min(bufferSize * 4, 4096)` | `50` |
| `httpClient` | Ktor HTTP client. The lazy default is provider-owned; an injected client is caller-owned | lazy `HttpClient(CIO)` |

```kotlin
val logger = ColotokLoggerContext()
    .addProvider(LokiProvider {
        // Set minimum log level
        level = LogLevel.INFO

        // Configure Loki server URL
        host = "http://your-loki-server:3100"

        // Configure log stream labels
        logStream = mapOf(
            "app" to "your-application",
            "environment" to "production",
            "instance" to "instance-1"
        )

        // Configure authentication (optional)
        credential = Credential.Basic(
            username = "your-username",
            password = "your-password"
        )

        // Configure buffer size (optional)
        bufferSize = 50 // Default: publication is attempted at this threshold
    })
    .getLogger()

// Use the logger as normal
logger.info("This log will be sent to Loki")
```

Loki and CloudWatch use the timestamp captured by the original logging call, not the later publish time.
Injected Loki clients are not closed by the provider; the caller must close them after provider shutdown.

**Buffering and Flushing**: The Loki provider buffers logs to improve performance. Publication is attempted when the buffer reaches the configured threshold (`bufferSize`), when you explicitly call `flush()`, and during graceful shutdown. After a failed attempt, the retained buffer is retried at later threshold multiples, at the retention limit, or on a flush.

```kotlin
// Get a reference to the provider
val lokiProvider = LokiProvider { /* config */ }
val logger = ColotokLoggerContext()
    .addProvider(lokiProvider)
    .getLogger()

// Later, flush logs explicitly
runBlocking {
    lokiProvider.flush()
}
```

**Platform Support**: This plugin is published for the same targets as the core library: JVM, Android, JavaScript, iOS arm64, iOS Simulator arm64, and macOS arm64.
