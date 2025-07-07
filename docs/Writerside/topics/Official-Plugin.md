# Official Plugin

Colotok provides several official plugins to extend its functionality for different use cases. This page describes each plugin, when to use it, and how to set it up.

## colotok-coroutines

The `colotok-coroutines` plugin provides coroutine support for Colotok, allowing you to log asynchronously in coroutine contexts.

**Description**: As a default, Colotok is not coroutines-friendly. This plugin adds async versions of all logging methods.

**When to Use**: Use this plugin when your application uses Kotlin coroutines and you want to:
- Log asynchronously without blocking the current coroutine
- Integrate logging with your coroutine-based application flow
- Avoid blocking I/O operations during logging

**Setup**: Add the dependency to your project:

```kotlin
// Gradle Kotlin DSL
implementation("io.github.milkcocoa0902:colotok-coroutines:0.3.3")
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

    // With structured logging
    logger.infoAsync(YourLogStructure())

    // Scoped logging
    logger.atInfoAsync {
        // All logs in this block are at INFO level
        print("Log message 1")
        print("Log message 2")
    }
}
```

**Platform Support**: This plugin is available for all platforms supported by Kotlin Multiplatform.

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
implementation("io.github.milkcocoa0902:colotok-cloudwatch:0.3.3")
```

**Usage**: Configure the CloudWatch provider with your AWS credentials and log group/stream information:

```kotlin
val logger = ColotokLoggerFactory()
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
        logBufferSize = 50 // Default: logs are sent when buffer reaches this size
    })
    .getLogger()

// Use the logger as normal
logger.info("This log will be sent to CloudWatch")
```

**Buffering and Flushing**: The CloudWatch provider buffers logs to improve performance. Logs are sent to CloudWatch when:
1. The buffer reaches the configured size (`logBufferSize`)
2. You explicitly call `flush()` on the provider

```kotlin
// Get a reference to the provider
val cloudwatchProvider = CloudwatchProvider { /* config */ }
val logger = ColotokLoggerFactory()
    .addProvider(cloudwatchProvider)
    .getLogger()

// Later, flush logs explicitly
runBlocking {
    cloudwatchProvider.flush()
}
```

**Platform Support**: This plugin is available for JVM platform only.

## colotok-slf4j (JVM only)

The `colotok-slf4j` plugin allows Colotok to be used as an SLF4J implementation.

**Description**: This plugin enables applications that use SLF4J for logging to use Colotok as the logging backend.

**When to Use**: Use this plugin when:
- Your application or its dependencies use SLF4J for logging
- You want to route SLF4J logs through Colotok
- You need to integrate with existing code that uses SLF4J

**Setup**: Add the dependency to your project:

```kotlin
// Gradle Kotlin DSL
implementation("io.github.milkcocoa0902:colotok-slf4j:0.3.3")
```

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
implementation("io.github.milkcocoa0902:colotok-loki:0.3.3")
```

**Usage**: Configure the Loki provider with your Loki server information:

```kotlin
val logger = ColotokLoggerFactory()
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
        bufferSize = 50 // Default: logs are sent when buffer reaches this size
    })
    .getLogger()

// Use the logger as normal
logger.info("This log will be sent to Loki")
```

**Buffering and Flushing**: The Loki provider buffers logs to improve performance. Logs are sent to Loki when:
1. The buffer reaches the configured size (`bufferSize`)
2. You explicitly call `flush()` on the provider

```kotlin
// Get a reference to the provider
val lokiProvider = LokiProvider { /* config */ }
val logger = ColotokLoggerFactory()
    .addProvider(lokiProvider)
    .getLogger()

// Later, flush logs explicitly
runBlocking {
    lokiProvider.flush()
}
```

**Platform Support**: This plugin is available for all platforms supported by Kotlin Multiplatform.
