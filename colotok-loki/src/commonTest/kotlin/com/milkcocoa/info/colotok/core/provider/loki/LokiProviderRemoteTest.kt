package com.milkcocoa.info.colotok.core.provider.loki

import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.logger.LogRecord
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Instant

class LokiProviderRemoteTest {
    @Test
    fun `invalid configuration does not create default client`() {
        var createCount = 0
        val config = validConfig().apply {
            host = " "
            httpClientFactory = {
                createCount++
                HttpClient(MockEngine { respond("") })
            }
        }

        assertFailsWith<IllegalStateException> { LokiProvider(config) }
        assertEquals(0, createCount)
    }

    @Test
    fun `unused provider does not initialize client during shutdown`() = runTest {
        var createCount = 0
        val config = validConfig().apply {
            httpClientFactory = {
                createCount++
                HttpClient(MockEngine { respond("") })
            }
        }
        val provider = LokiProvider(config)

        provider.close()
        provider.join()

        assertEquals(0, createCount)
    }

    @Test
    fun `unused force shutdown does not initialize client`() {
        var createCount = 0
        val config = validConfig().apply {
            httpClientFactory = {
                createCount++
                HttpClient(MockEngine { respond("") })
            }
        }
        val provider = LokiProvider(config)

        provider.forceShutdown()

        assertEquals(0, createCount)
    }

    @Test
    fun `owned client is closed once but injected client remains caller owned`() = runTest {
        var ownedCloseCount = 0
        val ownedConfig = validConfig().apply {
            httpClientFactory = { HttpClient(MockEngine { respond("") }) }
            httpClientCloser = {
                ownedCloseCount++
                it.close()
            }
        }
        val ownedProvider = LokiProvider(ownedConfig)
        ownedProvider.onPublish(listOf(plain()))
        ownedProvider.close()
        ownedProvider.join()
        assertEquals(1, ownedCloseCount)

        var injectedCloseCount = 0
        var injectedRequestCount = 0
        val injected = HttpClient(MockEngine {
            injectedRequestCount++
            respond("")
        })
        val injectedConfig = validConfig().apply {
            httpClientCloser = {
                injectedCloseCount++
                it.close()
            }
            httpClient = injected
        }
        val injectedProvider = LokiProvider(injectedConfig)
        injectedProvider.onPublish(listOf(plain()))
        injectedProvider.close()
        injectedProvider.join()
        assertEquals(0, injectedCloseCount)
        injected.get("https://loki.example.com/health")
        assertEquals(2, injectedRequestCount)
        injected.close()
    }

    @Test
    fun `request contains event timestamp rather than publish time`() = runTest {
        var requestBody = ""
        val client = HttpClient(MockEngine { request ->
            requestBody = (request.body as TextContent).text
            respond("", HttpStatusCode.NoContent)
        })
        val provider = LokiProvider(validConfig().apply { httpClient = client })
        val eventTime = Instant.fromEpochSeconds(1_700_000_000, 123_456_789)

        provider.onPublish(listOf(plain(eventTimestamp = eventTime)))
        provider.close()
        provider.join()

        assertTrue(requestBody.contains("1700000000123456789"))
        client.close()
    }

    @Test
    fun `non-success response fails the publish`() = runTest {
        val client = HttpClient(MockEngine { respond("unavailable", HttpStatusCode.ServiceUnavailable) })
        val provider = LokiProvider(validConfig().apply { httpClient = client })

        val failure = assertFailsWith<IllegalStateException> {
            provider.onPublish(listOf(plain()))
        }
        assertTrue(failure.message.orEmpty().contains("503"))
        provider.close()
        provider.join()
        client.close()
    }

    @Test
    fun `body response failure is consumed and a later request can succeed`() = runTest {
        var requests = 0
        val client = HttpClient(MockEngine {
            requests++
            if (requests == 1) {
                respond("temporarily unavailable", HttpStatusCode.ServiceUnavailable)
            } else {
                respond("", HttpStatusCode.NoContent)
            }
        })
        val provider = LokiProvider(validConfig().apply { httpClient = client })

        assertFailsWith<IllegalStateException> { provider.onPublish(listOf(plain())) }
        provider.onPublish(listOf(plain()))

        assertEquals(2, requests)
        provider.close()
        provider.join()
        client.close()
    }

    private fun validConfig() = LokiProviderConfig().apply {
        host = "https://loki.example.com"
        logStream = mapOf("app" to "test")
        bufferSize = 2
    }

    private fun plain(
        eventTimestamp: Instant = Instant.fromEpochMilliseconds(1_000),
    ) = LogRecord.PlainText(
        name = "test",
        msg = "message",
        level = LogLevel.INFO,
        attr = emptyMap(),
        eventTimestamp = eventTimestamp,
    )
}
