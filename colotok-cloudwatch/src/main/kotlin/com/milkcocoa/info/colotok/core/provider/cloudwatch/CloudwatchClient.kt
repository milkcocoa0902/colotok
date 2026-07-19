package com.milkcocoa.info.colotok.core.provider.cloudwatch

import aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider
import aws.sdk.kotlin.runtime.auth.credentials.EnvironmentCredentialsProvider
import aws.sdk.kotlin.runtime.auth.credentials.ProfileCredentialsProvider
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.cloudwatchlogs.CloudWatchLogsClient
import aws.sdk.kotlin.services.cloudwatchlogs.createLogGroup
import aws.sdk.kotlin.services.cloudwatchlogs.createLogStream
import aws.sdk.kotlin.services.cloudwatchlogs.model.InputLogEvent
import aws.sdk.kotlin.services.cloudwatchlogs.model.ResourceAlreadyExistsException
import aws.sdk.kotlin.services.cloudwatchlogs.putLogEvents

internal interface CloudwatchClientFactory {
    fun create(credential: CloudwatchCredential): CloudwatchClient
}

internal interface CloudwatchClient {
    suspend fun ensureLogGroup(logGroup: String)

    suspend fun ensureLogStream(
        logGroup: String,
        logStream: String
    )

    suspend fun putLogEvents(
        logGroup: String,
        logStream: String,
        events: List<CloudwatchEvent>,
        sequenceToken: String?
    ): String?

    fun close()
}

internal object AwsCloudwatchClientFactory : CloudwatchClientFactory {
    override fun create(credential: CloudwatchCredential): CloudwatchClient {
        val client =
            CloudWatchLogsClient {
                region = credential.region
                credentialsProvider =
                    when (credential) {
                        is CloudwatchCredential.Default -> DefaultChainCredentialsProvider()
                        is CloudwatchCredential.StaticCredentials ->
                            StaticCredentialsProvider {
                                accessKeyId = credential.accessKeyId
                                secretAccessKey = credential.secretAccessKey
                            }
                        is CloudwatchCredential.Profile ->
                            ProfileCredentialsProvider(
                                profileName = credential.profileName
                            )
                        is CloudwatchCredential.FromEnvironments -> EnvironmentCredentialsProvider()
                    }
            }
        return AwsCloudwatchClient(client)
    }
}

private class AwsCloudwatchClient(
    private val client: CloudWatchLogsClient
) : CloudwatchClient {
    override suspend fun ensureLogGroup(logGroup: String) {
        try {
            client.createLogGroup { logGroupName = logGroup }
        } catch (_: ResourceAlreadyExistsException) {
            // The desired state already exists; other provisioning failures remain visible.
        }
    }

    override suspend fun ensureLogStream(
        logGroup: String,
        logStream: String
    ) {
        try {
            client.createLogStream {
                logGroupName = logGroup
                logStreamName = logStream
            }
        } catch (_: ResourceAlreadyExistsException) {
            // The desired state already exists; other provisioning failures remain visible.
        }
    }

    override suspend fun putLogEvents(
        logGroup: String,
        logStream: String,
        events: List<CloudwatchEvent>,
        sequenceToken: String?
    ): String? =
        client.putLogEvents {
            logGroupName = logGroup
            logStreamName = logStream
            this.sequenceToken = sequenceToken
            logEvents =
                events.map { event ->
                    InputLogEvent {
                        timestamp = event.timestampMillis
                        message = event.message
                    }
                }
        }.nextSequenceToken

    override fun close() = client.close()
}