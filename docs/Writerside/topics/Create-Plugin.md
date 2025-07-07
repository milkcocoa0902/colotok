# Create Plugin

This page explains how to create custom plugins for Colotok. Plugins allow you to extend Colotok's functionality in various ways, such as adding support for new logging destinations, implementing new formatting options, or integrating with other libraries and frameworks.

## Plugin Architecture

Colotok has a flexible plugin architecture based on interfaces that define the contract between the core library and plugins. The main interfaces are:

- **Provider**: The core interface that all logging providers must implement
- **ProviderConfig**: The interface for provider configuration
- **AsyncProvider**: An extension of Provider that adds support for asynchronous logging

### Provider Interface

The `Provider` interface is the foundation of Colotok's plugin system. It defines methods for writing logs to various destinations:

```kotlin
interface Provider {
    // Write plain text logs
    fun write(name: String, msg: String, level: Level)

    // Write plain text logs with attributes
    fun write(name: String, msg: String, level: Level, attr: Map<String, String>)

    // Write structured logs
    fun <T : LogStructure> write(name: String, msg: T, serializer: KSerializer<T>, level: Level)

    // Write structured logs with attributes
    fun <T : LogStructure> write(name: String, msg: T, serializer: KSerializer<T>, level: Level, attr: Map<String, String>)
}
```

At minimum, a provider needs to implement the second and fourth methods, as the first and third methods have default implementations that call the second and fourth methods respectively with an empty map.

### ProviderConfig Interface

The `ProviderConfig` interface defines the basic configuration options that all providers must support:

```kotlin
interface ProviderConfig {
    // Minimum log level that the provider will process
    var level: Level

    // Formatter used to format log messages
    var formatter: Formatter
}
```

Specific providers can extend this interface to add their own configuration options.

### AsyncProvider Interface

The `AsyncProvider` interface extends the `Provider` interface to add support for asynchronous logging:

```kotlin
interface AsyncProvider: Provider {
    // Async versions of the Provider methods
    suspend fun writeAsync(name: String, msg: String, level: Level)

    suspend fun writeAsync(name: String, msg: String, level: Level, attr: Map<String, String>)

    suspend fun <T : LogStructure> writeAsync(name: String, msg: T, serializer: KSerializer<T>, level: Level)

    suspend fun <T : LogStructure> writeAsync(name: String, msg: T, serializer: KSerializer<T>, level: Level, attr: Map<String, String>)
}
```

Similar to the `Provider` interface, at minimum, an `AsyncProvider` needs to implement the second and fourth methods.

## Creating a Custom Provider

Let's walk through the process of creating a custom provider for Colotok. We'll create a provider that sends logs to Slack using webhooks.

### Step 1: Define the Provider Configuration

First, define a configuration class for your provider by implementing the `ProviderConfig` interface:

```kotlin
class SlackProviderConfig : ProviderConfig {
    // Required by ProviderConfig
    override var level: Level = LogLevel.DEBUG
    override var formatter: Formatter = DetailTextFormatter

    // Custom configuration options
    var webhookUrl: String = ""
}
```

### Step 2: Implement the Provider

Next, implement the `Provider` interface:

```kotlin
class SlackProvider(config: SlackProviderConfig) : Provider {
    // Convenience constructor that accepts a configuration lambda
    constructor(config: SlackProviderConfig.() -> Unit): this(SlackProviderConfig().apply(config))

    private val webhookUrl = config.webhookUrl
    private val logLevel = config.level
    private val formatter = config.formatter

    // HTTP client for sending requests to Slack
    private val client = HttpClient()

    override fun write(
        name: String,
        msg: String,
        level: Level,
        attr: Map<String, String>
    ) {
        // Skip if the log level is not enabled
        if (level.isEnabledFor(logLevel).not()) {
            return
        }

        // Format the log message
        val formattedMessage = formatter.format(msg, level, attr)

        // Send the log to Slack
        runBlocking {
            client.post(webhookUrl) {
                contentType(ContentType.Application.Json)
                setBody("""{"text": "${formattedMessage.escapeJson()}"}""")
            }
        }
    }

    override fun <T : LogStructure> write(
        name: String,
        msg: T,
        serializer: KSerializer<T>,
        level: Level,
        attr: Map<String, String>
    ) {
        // Skip if the log level is not enabled
        if (level.isEnabledFor(logLevel).not()) {
            return
        }

        // Format the structured log message
        val formattedMessage = formatter.format(msg, serializer, level, attr)

        // Send the log to Slack
        runBlocking {
            client.post(webhookUrl) {
                contentType(ContentType.Application.Json)
                setBody("""{"text": "${formattedMessage.escapeJson()}"}""")
            }
        }
    }

    // Helper function to escape JSON strings
    private fun String.escapeJson(): String {
        return this.replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
```

### Step 3: Add Asynchronous Support (Optional)

If you want to support asynchronous logging, implement the `AsyncProvider` interface:

```kotlin
class SlackProvider(config: SlackProviderConfig) : AsyncProvider {
    // ... same as before ...

    override suspend fun writeAsync(
        name: String,
        msg: String,
        level: Level,
        attr: Map<String, String>
    ) {
        // Skip if the log level is not enabled
        if (level.isEnabledFor(logLevel).not()) {
            return
        }

        // Format the log message
        val formattedMessage = formatter.format(msg, level, attr)

        // Send the log to Slack asynchronously
        client.post(webhookUrl) {
            contentType(ContentType.Application.Json)
            setBody("""{"text": "${formattedMessage.escapeJson()}"}""")
        }
    }

    override suspend fun <T : LogStructure> writeAsync(
        name: String,
        msg: T,
        serializer: KSerializer<T>,
        level: Level,
        attr: Map<String, String>
    ) {
        // Skip if the log level is not enabled
        if (level.isEnabledFor(logLevel).not()) {
            return
        }

        // Format the structured log message
        val formattedMessage = formatter.format(msg, serializer, level, attr)

        // Send the log to Slack asynchronously
        client.post(webhookUrl) {
            contentType(ContentType.Application.Json)
            setBody("""{"text": "${formattedMessage.escapeJson()}"}""")
        }
    }

    // For synchronous methods, delegate to async methods
    override fun write(
        name: String,
        msg: String,
        level: Level,
        attr: Map<String, String>
    ) {
        runBlocking {
            writeAsync(name, msg, level, attr)
        }
    }

    override fun <T : LogStructure> write(
        name: String,
        msg: T,
        serializer: KSerializer<T>,
        level: Level,
        attr: Map<String, String>
    ) {
        runBlocking {
            writeAsync(name, msg, serializer, level, attr)
        }
    }
}
```

### Step 4: Use Your Custom Provider

Now you can use your custom provider with Colotok:

```kotlin
val logger = ColotokLoggerFactory()
    .addProvider(SlackProvider {
        webhookUrl = "https://hooks.slack.com/services/your/webhook/url"
        level = LogLevel.WARN  // Only send WARN and ERROR logs to Slack
        formatter = SimpleTextFormatter
    })
    .getLogger()

// Use the logger as normal
logger.info("This won't be sent to Slack")
logger.warn("This will be sent to Slack")
logger.error("This will also be sent to Slack")
```

## Best Practices

Here are some best practices to follow when creating custom plugins for Colotok:

### 1. Respect Log Levels

Always check the log level before processing a log message:

```kotlin
if (level.isEnabledFor(logLevel).not()) {
    return
}
```

This ensures that your provider only processes logs that meet the configured minimum level.

### 2. Handle Errors Gracefully

Logging should never cause your application to crash. Always wrap external calls in try-catch blocks:

```kotlin
runCatching {
    // External call that might fail
}.getOrElse { exception ->
    // Handle the exception, maybe log it to a fallback destination
    println("Failed to send log: ${exception.message}")
}
```

### 3. Buffer Logs When Appropriate

If your provider sends logs to an external service, consider buffering logs to reduce the number of network requests:

```kotlin
private val buffer = mutableListOf<LogEntry>()
private val bufferSize = 50
private val mutex = Mutex()

override suspend fun writeAsync(
    name: String,
    msg: String,
    level: Level,
    attr: Map<String, String>
) {
    if (level.isEnabledFor(logLevel).not()) {
        return
    }

    val shouldSendLogs = mutex.withLock {
        buffer.add(LogEntry(name, formatter.format(msg, level, attr), System.currentTimeMillis()))
        buffer.size >= bufferSize
    }

    if (shouldSendLogs) {
        sendLogsToExternalService()
    }
}

private suspend fun sendLogsToExternalService() {
    mutex.withLock {
        if (buffer.isEmpty()) return

        // Send the buffered logs
        client.post(serviceUrl) {
            contentType(ContentType.Application.Json)
            setBody(buffer)
        }

        // Clear the buffer
        buffer.clear()
    }
}

// Add a flush method to send any buffered logs
suspend fun flush() {
    sendLogsToExternalService()
}
```

### 4. Make Configuration Flexible

Provide sensible defaults but allow users to customize your provider:

```kotlin
class MyProviderConfig : ProviderConfig {
    override var level: Level = LogLevel.INFO  // Sensible default
    override var formatter: Formatter = SimpleTextFormatter  // Sensible default

    var bufferSize: Int = 50  // Sensible default
    var retryCount: Int = 3  // Sensible default
    var timeout: Long = 5000  // Sensible default
}
```

### 5. Document Your Provider

Add KDoc comments to your provider and configuration classes to help users understand how to use them:

```kotlin
/**
 * A provider that sends logs to MyService.
 *
 * This provider buffers logs and sends them in batches to reduce network traffic.
 * It also supports retrying failed requests.
 *
 * @property config The configuration for this provider
 */
class MyProvider(config: MyProviderConfig) : Provider {
    // ...
}
```

## Testing Your Provider

It's important to test your provider to ensure it works correctly. Here's a simple test for our SlackProvider:

```kotlin
class SlackProviderTest {
    @Test
    fun `test slack provider sends logs`() {
        // Create a mock HTTP client
        val mockClient = MockHttpClient { request ->
            // Verify the request
            assertEquals("https://hooks.slack.com/services/test", request.url.toString())
            assertEquals(ContentType.Application.Json, request.contentType())

            // Return a success response
            MockHttpResponse(HttpStatusCode.OK, "ok")
        }

        // Create the provider with the mock client
        val provider = SlackProvider {
            webhookUrl = "https://hooks.slack.com/services/test"
            level = LogLevel.INFO
        }.apply {
            client = mockClient
        }

        // Test the provider
        provider.write("test", "Test message", LogLevel.INFO, mapOf())

        // Verify that the mock client was called
        assertEquals(1, mockClient.requestCount)
    }
}
```

## Packaging Your Plugin

If you want to share your plugin with others, you should package it as a separate library. Here's a basic structure for a Colotok plugin project:

```
my-colotok-plugin/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   └── kotlin/
│   │       └── com/
│   │           └── example/
│   │               └── colotok/
│   │                   └── plugin/
│   │                       ├── MyProvider.kt
│   │                       └── MyProviderConfig.kt
│   └── test/
│       └── kotlin/
│           └── com/
│               └── example/
│                   └── colotok/
│                       └── plugin/
│                           └── MyProviderTest.kt
└── README.md
```

Your `build.gradle.kts` file should include Colotok as a dependency:

```kotlin
plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    `maven-publish`
}

group = "com.example"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.milkcocoa0902:colotok:0.3.2")

    // Add any other dependencies your plugin needs

    testImplementation(kotlin("test"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
```

## Conclusion

Creating custom plugins for Colotok is a powerful way to extend its functionality to meet your specific needs. By implementing the `Provider` interface and following the best practices outlined in this guide, you can create robust and flexible plugins that integrate Colotok with any logging destination or service.

Remember to check the [Official Plugin](Official-Plugin.md) page for examples of plugins that are officially supported by Colotok.
