package com.milkcocoa.info.colotok.core.provider.cloudwatch

import aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider
import aws.sdk.kotlin.runtime.auth.credentials.EnvironmentCredentialsProvider
import aws.sdk.kotlin.runtime.auth.credentials.ProfileCredentialsProvider
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.cloudwatchlogs.CloudWatchLogsClient
import aws.sdk.kotlin.services.cloudwatchlogs.createLogGroup
import aws.sdk.kotlin.services.cloudwatchlogs.createLogStream
import aws.sdk.kotlin.services.cloudwatchlogs.model.InputLogEvent
import aws.sdk.kotlin.services.cloudwatchlogs.putLogEvents
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.provider.details.AsyncProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class CloudwatchProvider(config: CloudwatchProviderConfig): AsyncProvider() {
    constructor(config: CloudwatchProviderConfig.()->Unit): this(CloudwatchProviderConfig().apply(config))
    constructor(): this(CloudwatchProviderConfig())

    private val logLevel = config.level
    private val formatter = config.formatter
    private val cloudwatchLogGroup = config.logGroup ?: error("Cloudwatch log group is null")
    private val cloudwatchLogStream = config.logStream ?: error("Cloudwatch log stream is null")
    private val credential = config.credential ?:  error("Credential is null")

    private var sequenceToken: String? = null

    private val mutex = Mutex()

    private val buf = mutableListOf<InputLogEvent>()
    private val logBufferSize = config.logBufferSize
    private val region = credential.region

    private val client by lazy {
        CloudWatchLogsClient {
            this.region = this@CloudwatchProvider.region
            this.credentialsProvider = when (val c = this@CloudwatchProvider.credential) {
                is CloudwatchCredential.Default -> DefaultChainCredentialsProvider()
                is CloudwatchCredential.StaticCredentials -> StaticCredentialsProvider {
                    this.accessKeyId = c.accessKeyId
                    this.secretAccessKey = c.secretAccessKey
                }

                is CloudwatchCredential.Profile -> ProfileCredentialsProvider(
                    profileName = c.profileName
                )

                is CloudwatchCredential.FromEnvironments -> EnvironmentCredentialsProvider()
            }
        }
    }


    private suspend fun createGroupIfNotExists() = runCatching {
        client.createLogGroup {
            this.logGroupName = this@CloudwatchProvider.cloudwatchLogGroup
        }
    }

    private suspend fun createStreamIfNotExists() = runCatching {
        client.createLogStream {
            this.logGroupName = this@CloudwatchProvider.cloudwatchLogGroup
            this.logStreamName = this@CloudwatchProvider.cloudwatchLogStream
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun writeAsync(record: LogRecord) {
        if(record.level.isEnabledFor(logLevel).not()){
            return
        }

        val shouldSendLogs = mutex.withLock {
            buf.add(InputLogEvent {
                this.timestamp = Clock.System.now().toEpochMilliseconds()
                this.message = record.format(formatter)
            })

            buf.size >= logBufferSize
        }

        if (shouldSendLogs) {
            sendLogsToCloudWatch()
        }
    }


    /**
     * Sends the buffered logs to CloudWatch.
     * This method is synchronized with the mutex to prevent concurrent modifications to the buffer.
     */
    private suspend fun sendLogsToCloudWatch() {
        runCatching {
            mutex.withLock {
                if (buf.isEmpty()) return@withLock

                createGroupIfNotExists()
                createStreamIfNotExists()
                
                val response = client.putLogEvents {
                    this.logGroupName = this@CloudwatchProvider.cloudwatchLogGroup
                    this.logStreamName = this@CloudwatchProvider.cloudwatchLogStream
                    this.sequenceToken = this@CloudwatchProvider.sequenceToken
                    this.logEvents = buf
                }

                this@CloudwatchProvider.sequenceToken = response.nextSequenceToken
                buf.clear()
            }
        }
    }

    /**
     * Flushes the buffer to CloudWatch regardless of buffer size.
     */
    override suspend fun onFlush() {
        super.onFlush()
        sendLogsToCloudWatch()
    }

    override fun onClosed() {
        client.close()
    }
}
