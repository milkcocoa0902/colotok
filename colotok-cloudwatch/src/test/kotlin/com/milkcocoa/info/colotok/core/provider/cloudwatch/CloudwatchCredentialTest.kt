package com.milkcocoa.info.colotok.core.provider.cloudwatch

import kotlin.test.Test
import kotlin.test.assertEquals

class CloudwatchCredentialTest {
    
    @Test
    fun `test FromEnvironments credential`() {
        val credential = CloudwatchCredential.FromEnvironments("us-east-1")
        assertEquals("us-east-1", credential.region)
    }
    
    @Test
    fun `test StaticCredentials credential`() {
        val credential = CloudwatchCredential.StaticCredentials(
            region = "us-west-1",
            accessKeyId = "test-access-key",
            secretAccessKey = "test-secret-key"
        )
        assertEquals("us-west-1", credential.region)
        assertEquals("test-access-key", credential.accessKeyId)
        assertEquals("test-secret-key", credential.secretAccessKey)
    }
    
    @Test
    fun `test Profile credential`() {
        val credential = CloudwatchCredential.Profile(
            region = "eu-west-1",
            profileName = "test-profile"
        )
        assertEquals("eu-west-1", credential.region)
        assertEquals("test-profile", credential.profileName)
    }
    
    @Test
    fun `test Default credential`() {
        val credential = CloudwatchCredential.Default("ap-northeast-1")
        assertEquals("ap-northeast-1", credential.region)
    }
    
    @Test
    fun `test equality of credentials`() {
        val credential1 = CloudwatchCredential.Default("us-west-2")
        val credential2 = CloudwatchCredential.Default("us-west-2")
        val credential3 = CloudwatchCredential.Default("us-east-1")
        
        assertEquals(credential1, credential2)
        assert(credential1 != credential3)
        
        val staticCredential1 = CloudwatchCredential.StaticCredentials(
            region = "us-west-1",
            accessKeyId = "test-access-key",
            secretAccessKey = "test-secret-key"
        )
        val staticCredential2 = CloudwatchCredential.StaticCredentials(
            region = "us-west-1",
            accessKeyId = "test-access-key",
            secretAccessKey = "test-secret-key"
        )
        val staticCredential3 = CloudwatchCredential.StaticCredentials(
            region = "us-west-1",
            accessKeyId = "different-access-key",
            secretAccessKey = "test-secret-key"
        )
        
        assertEquals(staticCredential1, staticCredential2)
        assert(staticCredential1 != staticCredential3)
    }
}