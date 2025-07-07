package com.milkcocoa.info.colotok.core.provider.loki

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class LokiValueTest {
    
    @OptIn(ExperimentalTime::class)
    @Test
    fun testLokiValueSerialization() {
        // Create a LokiValue with a specific timestamp
        val timestamp = Instant.fromEpochSeconds(1721224800, 0) // 2024-07-17T12:00:00Z
        val value = "user logged in"
        val lokiValue = LokiValue(timestamp, value)
        
        // Serialize to JSON
        val json = Json.encodeToString(LokiValue.serializer(), lokiValue)
        
        // Expected JSON: ["1721224800000000000","user logged in"]
        val expectedJson = """["1721224800000000000","user logged in"]"""
        assertEquals(expectedJson, json)
        
        // Deserialize back to LokiValue
        val deserializedValue = Json.decodeFromString(LokiValue.serializer(), json)
        
        // Verify the deserialized value matches the original
        assertEquals(timestamp, deserializedValue.timestamp)
        assertEquals(value, deserializedValue.value)
    }
    
    @OptIn(ExperimentalTime::class)
    @Test
    fun testLokiValueDeserialization() {
        // JSON representing a LokiValue
        val json = """["1721224801000000000","user fetched profile"]"""
        
        // Deserialize to LokiValue
        val lokiValue = Json.decodeFromString(LokiValue.serializer(), json)
        
        // Expected values
        val expectedTimestamp = Instant.fromEpochSeconds(1721224801, 0) // 2024-07-17T12:00:01Z
        val expectedValue = "user fetched profile"
        
        // Verify the deserialized value has the expected properties
        assertEquals(expectedTimestamp, lokiValue.timestamp)
        assertEquals(expectedValue, lokiValue.value)
    }
}