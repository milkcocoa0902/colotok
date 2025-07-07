package com.milkcocoa.info.colotok.core.provider.loki

import com.milkcocoa.info.colotok.core.formatter.builtin.text.SimpleTextFormatter
import com.milkcocoa.info.colotok.core.level.LogLevel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class LokiProviderTest {
    
    @Test
    fun testLokiProviderConfigConstructor() {
        // Test constructor with config object
        val config = LokiProviderConfig().apply {
            host = "https://loki.example.com"
            logStream = mapOf("app" to "test-app")
        }
        
        try {
            val provider = LokiProvider(config)
            assertNotNull(provider)
        } catch (e: Exception) {
            fail("LokiProvider constructor with config object should not throw: ${e.message}")
        }
    }
    
    @Test
    fun testLokiProviderLambdaConstructor() {
        // Test constructor with config lambda
        try {
            val provider = LokiProvider {
                host = "https://loki.example.com"
                logStream = mapOf("app" to "test-app")
                level = LogLevel.DEBUG
                bufferSize = 10
            }
            assertNotNull(provider)
        } catch (e: Exception) {
            fail("LokiProvider constructor with lambda should not throw: ${e.message}")
        }
    }
    
    @Test
    fun testLokiProviderValidation() {
        // Test that provider requires host
        try {
            LokiProvider {
                // No host provided
                logStream = mapOf("app" to "test-app")
            }
            fail("LokiProvider should throw when host is not provided")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("host") == true, "Exception should mention missing host")
        }
        
        // Test that provider requires logStream
        try {
            LokiProvider {
                host = "https://loki.example.com"
                // No logStream provided
            }
            fail("LokiProvider should throw when logStream is not provided")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("stream") == true, "Exception should mention missing log stream")
        }
    }
    
    @Test
    fun testFormatterConfiguration() {
        // Test that formatter can be configured
        val customFormatter = SimpleTextFormatter
        
        val provider = LokiProvider {
            host = "https://loki.example.com"
            logStream = mapOf("app" to "test-app")
            formatter = customFormatter
        }
        
        // We can't directly access the formatter, but we can verify the provider was created
        assertNotNull(provider)
    }
    
    @Test
    fun testBufferSizeConfiguration() {
        // Test that buffer size can be configured
        val customBufferSize = 100
        
        val provider = LokiProvider {
            host = "https://loki.example.com"
            logStream = mapOf("app" to "test-app")
            bufferSize = customBufferSize
        }
        
        // We can't directly access the buffer size, but we can verify the provider was created
        assertNotNull(provider)
    }
    
    @Test
    fun testCredentialConfiguration() {
        // Test that credentials can be configured
        val provider = LokiProvider {
            host = "https://loki.example.com"
            logStream = mapOf("app" to "test-app")
            credential = Credential.Basic("user", "pass")
        }
        
        // We can't directly access the credentials, but we can verify the provider was created
        assertNotNull(provider)
    }
}