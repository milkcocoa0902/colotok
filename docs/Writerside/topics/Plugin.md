# Plugin

Plugins are a powerful way to extend Colotok's functionality beyond its core features. This page explains what plugins are, how they work, and how they can enhance your logging experience.

## What are Colotok Plugins?

In Colotok, plugins are extensions that add new capabilities to the logging system. They can:

- Add support for new logging destinations (like cloud services or databases)
- Provide integration with other libraries and frameworks
- Add new formatting options
- Enhance existing functionality with additional features

Plugins follow a consistent architecture that makes them easy to use and integrate with your application.

## Plugin Architecture

Colotok has a flexible plugin architecture built around core provider contracts:

- **Provider abstract class and IProvider interface**: The foundation for destinations that receive log records
- **ProviderConfig Interface**: Defines configuration options for providers
- **AsyncProvider abstract class**: Extends `Provider` with a channel-backed, batched publishing lifecycle

This architecture allows plugins to be:

- **Modular**: Each plugin focuses on a specific functionality
- **Configurable**: Plugins can be customized to meet your specific needs
- **Composable**: Multiple plugins can be used together in the same application

## Benefits of Using Plugins

Using Colotok plugins offers several advantages:

1. **Extended Functionality**: Access features beyond the core library
2. **Simplified Integration**: Easily connect with other systems and services
3. **Consistent API**: Use the same logging API regardless of the destination
4. **Platform Flexibility**: Some plugins support the Kotlin Multiplatform targets published by Colotok
5. **Customization**: Adapt logging behavior to your specific requirements

## Types of Plugins

Colotok plugins generally fall into these categories:

### 1. Integration Plugins

These plugins connect Colotok with external systems:

- **colotok-cloudwatch**: Sends logs to AWS CloudWatch
- **colotok-loki**: Sends logs to Grafana Loki
- **colotok-slf4j**: SLF4J 1.7 binding backed by Colotok
- **colotok-slf4j2**: SLF4J 2.x service provider backed by Colotok

### 2. Enhancement Plugins

These plugins add new capabilities to Colotok:

- **colotok-coroutines**: Adds suspending logging extensions and the `AsyncProvider` base class for batched delivery

### 3. Custom Plugins

You can also create your own plugins to:

- Send logs to custom destinations
- Implement specialized formatting
- Add domain-specific logging features

## Getting Started with Plugins

To use a plugin, you need to:

1. Add the plugin dependency to your project
2. Configure the plugin according to your needs
3. Add the plugin's provider to a `ColotokLoggerContext` when the plugin supplies one

For example, to use the colotok-coroutines plugin:

```kotlin
// Add dependency
implementation("io.github.milkcocoa0902:colotok-coroutines:0.5.0")

// The extension is suspending and can be called from a coroutine.
runBlocking {
    logger.infoAsync("Async log message")
}
```

## Learn More

For more information about Colotok plugins, check these resources:

- [Official Plugins](Official-Plugin.md): Detailed information about the plugins provided by Colotok
- [Creating Plugins](Create-Plugin.md): Guide to creating your own custom plugins
