package com.milkcocoa.info.colotok.core.provider.loki

import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.LogLevel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertNotNull

class LokiStructuredLoggingTest {

    // Define a simple log structure for testing
    @Serializable
    data class TestLogStructure(
        val message: String,
        val userId: Int,
        val metadata: Map<String, String> = emptyMap()
    ) : LogStructure

    @Test
    fun testStructuredLogging() {
        // Create a provider
        val provider = LokiProvider {
            host = "https://loki.example.com"
            logStream = mapOf("app" to "test-app")
        }

        // Create a structured log message
        val logMessage = TestLogStructure(
            message = "User logged in",
            userId = 12345,
            metadata = mapOf("ip" to "192.168.1.1", "browser" to "Chrome")
        )

        // Test that we can call write with a structured message without exceptions
        try {
            provider.write(
                name = "auth-service",
                msg = logMessage,
                serializer = serializer<TestLogStructure>(),
                level = LogLevel.INFO,
                attr = mapOf("component" to "authentication")
            )
            // If we get here, the test passes
        } catch (e: Exception) {
            kotlin.test.fail("Structured logging should not throw: ${e.message}")
        }
    }

    @Test
    fun testStructuredLoggingWithDifferentLevels() {
        // Create a provider
        val provider = LokiProvider {
            host = "https://loki.example.com"
            logStream = mapOf("app" to "test-app")
            level = LogLevel.DEBUG // Set to DEBUG to allow all levels
        }

        // Create structured log messages for different levels
        val debugMessage = TestLogStructure(message = "Debug message", userId = 1)
        val infoMessage = TestLogStructure(message = "Info message", userId = 2)
        val warnMessage = TestLogStructure(message = "Warning message", userId = 3)
        val errorMessage = TestLogStructure(message = "Error message", userId = 4)

        // Test that we can log at different levels without exceptions
        try {
            provider.write(name = "test", msg = debugMessage, serializer = serializer<TestLogStructure>(), level = LogLevel.DEBUG, attr = emptyMap())
            provider.write(name = "test", msg = infoMessage, serializer = serializer<TestLogStructure>(), level = LogLevel.INFO, attr = emptyMap())
            provider.write(name = "test", msg = warnMessage, serializer = serializer<TestLogStructure>(), level = LogLevel.WARN, attr = emptyMap())
            provider.write(name = "test", msg = errorMessage, serializer = serializer<TestLogStructure>(), level = LogLevel.ERROR, attr = emptyMap())
            // If we get here, the test passes
        } catch (e: Exception) {
            kotlin.test.fail("Structured logging at different levels should not throw: ${e.message}")
        }
    }

    @Test
    fun testStructuredLoggingWithAttributes() {
        // Create a provider
        val provider = LokiProvider {
            host = "https://loki.example.com"
            logStream = mapOf("app" to "test-app")
        }

        // Create a structured log message
        val logMessage = TestLogStructure(
            message = "API request processed",
            userId = 12345
        )

        // Test that we can log with different attributes without exceptions
        try {
            // Log with empty attributes
            provider.write(name = "api", msg = logMessage, serializer = serializer<TestLogStructure>(), level = LogLevel.INFO, attr = emptyMap())

            // Log with some attributes
            provider.write(
                name = "api", 
                msg = logMessage, 
                serializer = serializer<TestLogStructure>(),
                level = LogLevel.INFO, 
                attr = mapOf("endpoint" to "/users", "method" to "GET")
            )

            // Log with many attributes
            provider.write(
                name = "api", 
                msg = logMessage, 
                serializer = serializer<TestLogStructure>(),
                level = LogLevel.INFO, 
                attr = mapOf(
                    "endpoint" to "/users",
                    "method" to "GET",
                    "status" to "200",
                    "duration" to "150ms",
                    "user-agent" to "Mozilla/5.0",
                    "referer" to "https://example.com"
                )
            )

            // If we get here, the test passes
        } catch (e: Exception) {
            kotlin.test.fail("Structured logging with attributes should not throw: ${e.message}")
        }
    }
}
