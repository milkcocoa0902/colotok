package com.milkcocoa.info.colotok.core.provider.loki

import com.milkcocoa.info.colotok.core.formatter.builtin.text.SimpleTextFormatter
import com.milkcocoa.info.colotok.core.level.LogLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LokiProviderConfigTest {
    
    @Test
    fun testDefaultConfig() {
        val config = LokiProviderConfig()
        
        // Verify default values
        assertEquals(LogLevel.INFO, config.level)
        assertEquals(SimpleTextFormatter, config.formatter)
        assertEquals(null, config.host)
        assertEquals(null, config.logStream)
        assertEquals(50, config.bufferSize)
        assertEquals(null, config.credential)
    }
    
    @Test
    fun testCustomConfig() {
        val config = LokiProviderConfig().apply {
            level = LogLevel.DEBUG
            host = "https://loki.example.com"
            logStream = mapOf("app" to "test-app", "env" to "test")
            bufferSize = 100
            credential = Credential.Basic("user", "pass")
        }
        
        // Verify custom values
        assertEquals(LogLevel.DEBUG, config.level)
        assertEquals("https://loki.example.com", config.host)
        assertEquals(mapOf("app" to "test-app", "env" to "test"), config.logStream)
        assertEquals(100, config.bufferSize)
        assertEquals(Credential.Basic("user", "pass"), config.credential)
    }
    
    @Test
    fun testCredentialBasic() {
        val credential = Credential.Basic("testuser", "testpass")
        
        assertEquals("testuser", credential.username)
        assertEquals("testpass", credential.password)
    }
}