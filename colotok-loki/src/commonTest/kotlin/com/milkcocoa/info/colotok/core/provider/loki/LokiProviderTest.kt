package com.milkcocoa.info.colotok.core.provider.loki

import com.milkcocoa.info.colotok.core.formatter.builtin.text.SimpleTextFormatter
import com.milkcocoa.info.colotok.core.level.LogLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LokiProviderTest {

    @Test
    fun config_constructor_exposes_the_validated_config() {
        val config = LokiProviderConfig().apply {
            host = "https://loki.example.com"
            logStream = mapOf("app" to "test-app")
        }
        val provider = LokiProvider(config)

        try {
            assertSame(config, provider.config)
        } finally {
            provider.forceShutdown()
        }
    }

    @Test
    fun lambda_constructor_applies_the_requested_configuration() {
        val credential = Credential.Basic("user", "pass")
        val provider = LokiProvider {
            host = "https://loki.example.com"
            logStream = mapOf("app" to "test-app")
            level = LogLevel.DEBUG
            formatter = SimpleTextFormatter
            bufferSize = 10
            this.credential = credential
        }

        try {
            val config = provider.config as LokiProviderConfig
            assertEquals("https://loki.example.com", config.host)
            assertEquals(mapOf("app" to "test-app"), config.logStream)
            assertEquals(LogLevel.DEBUG, config.level)
            assertEquals(SimpleTextFormatter, config.formatter)
            assertEquals(10, config.bufferSize)
            assertEquals(credential, config.credential)
        } finally {
            provider.forceShutdown()
        }
    }

    @Test
    fun invalid_configuration_is_rejected_before_a_provider_is_created() {
        val missingHost = assertFailsWith<IllegalStateException> {
            LokiProvider {
                logStream = mapOf("app" to "test-app")
            }
        }
        assertTrue(missingHost.message.orEmpty().contains("host", ignoreCase = true))

        val missingStream = assertFailsWith<IllegalStateException> {
            LokiProvider {
                host = "https://loki.example.com"
            }
        }
        assertTrue(missingStream.message.orEmpty().contains("stream", ignoreCase = true))
    }
}
