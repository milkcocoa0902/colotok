package com.milkcocoa.info.colotok.core.provider.cloudwatch

import com.milkcocoa.info.colotok.core.formatter.builtin.structure.SimpleStructureFormatter
import com.milkcocoa.info.colotok.core.level.LogLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CloudwatchProviderConfigTest {
    
    @Test
    fun `test default values`() {
        val config = CloudwatchProviderConfig()
        
        assertEquals(LogLevel.DEBUG, config.level)
        assertEquals(SimpleStructureFormatter, config.formatter)
        assertEquals(50, config.logBufferSize)
    }
    
    @Test
    fun `test setting values`() {
        val config = CloudwatchProviderConfig().apply {
            level = LogLevel.INFO
            logGroup = "test-group"
            logStream = "test-stream"
            credential = CloudwatchCredential.Default("us-west-2")
            logBufferSize = 100
        }
        
        assertEquals(LogLevel.INFO, config.level)
        assertEquals("test-group", config.logGroup)
        assertEquals("test-stream", config.logStream)
        assertEquals(CloudwatchCredential.Default("us-west-2"), config.credential)
        assertEquals(100, config.logBufferSize)
    }
    
    @Test
    fun `test credential types`() {
        // Test FromEnvironments credential
        val envCredential = CloudwatchCredential.FromEnvironments("us-east-1")
        assertEquals("us-east-1", envCredential.region)
        
        // Test StaticCredentials credential
        val staticCredential = CloudwatchCredential.StaticCredentials(
            region = "us-west-1",
            accessKeyId = "test-access-key",
            secretAccessKey = "test-secret-key"
        )
        assertEquals("us-west-1", staticCredential.region)
        assertEquals("test-access-key", staticCredential.accessKeyId)
        assertEquals("test-secret-key", staticCredential.secretAccessKey)
        
        // Test Profile credential
        val profileCredential = CloudwatchCredential.Profile(
            region = "eu-west-1",
            profileName = "test-profile"
        )
        assertEquals("eu-west-1", profileCredential.region)
        assertEquals("test-profile", profileCredential.profileName)
        
        // Test Default credential
        val defaultCredential = CloudwatchCredential.Default("ap-northeast-1")
        assertEquals("ap-northeast-1", defaultCredential.region)
    }
}