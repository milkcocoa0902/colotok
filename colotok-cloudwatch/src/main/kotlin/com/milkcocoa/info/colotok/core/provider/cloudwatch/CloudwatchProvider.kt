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
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.provider.details.AsyncProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class CloudwatchProvider(config: CloudwatchProviderConfig): AsyncProvider {
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

    private suspend fun client() = CloudWatchLogsClient.invoke { 
        this.region = credential.region
        this.credentialsProvider = when(this@CloudwatchProvider.credential){
            is CloudwatchCredential.Default -> DefaultChainCredentialsProvider()
            is CloudwatchCredential.StaticCredentials -> StaticCredentialsProvider.invoke {
                this.accessKeyId = credential.accessKeyId
                this.secretAccessKey = credential.secretAccessKey
            }
            is CloudwatchCredential.Profile -> ProfileCredentialsProvider(
                profileName = credential.profileName
            )
            is CloudwatchCredential.FromEnvironments -> EnvironmentCredentialsProvider()
        }
    }


    private suspend fun createGroupIfNotExists() = runCatching {
        client().createLogGroup {
            this.logGroupName = this@CloudwatchProvider.cloudwatchLogGroup
        }
    }

    private suspend fun createStreamIfNotExists() = runCatching {
        client().createLogStream createLogStream@{
            this.logGroupName = this@CloudwatchProvider.cloudwatchLogGroup
            this.logStreamName = this@CloudwatchProvider.cloudwatchLogStream
        }
    }


    @OptIn(ExperimentalTime::class)
    override fun write(
        name: String,
        msg: String,
        level: Level,
        attr: Map<String, String>
    ) = runBlocking { 
        this@CloudwatchProvider.writeAsync(name, msg, level, attr)
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun writeAsync(
        name: String,
        msg: String,
        level: Level,
        attr: Map<String, String>
    ) {
        if (level.isEnabledFor(logLevel).not()) {
            return
        }

        val shouldSendLogs = mutex.withLock {
            buf.add(InputLogEvent {
                this.timestamp = Clock.System.now().toEpochMilliseconds()
                this.message = formatter.format(msg, level, attr)
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
                val response = client().putLogEvents putLogEvents@{
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

    @OptIn(ExperimentalTime::class)
    override fun <T : LogStructure> write(
        name: String,
        msg: T,
        serializer: KSerializer<T>,
        level: Level,
        attr: Map<String, String>
    ) = runBlocking {
        this@CloudwatchProvider.writeAsync(name, msg, serializer, level, attr)
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun <T : LogStructure> writeAsync(
        name: String,
        msg: T,
        serializer: KSerializer<T>,
        level: Level,
        attr: Map<String, String>
    ) {
        if (level.isEnabledFor(logLevel).not()) {
            return
        }

        val shouldSendLogs = mutex.withLock {
            buf.add(InputLogEvent {
                this.timestamp = Clock.System.now().toEpochMilliseconds()
                this.message = formatter.format(msg, serializer, level, attr)
            })

            buf.size >= logBufferSize
        }

        if (shouldSendLogs) {
            sendLogsToCloudWatch()
        }
    }

    /**
     * Flushes the buffer to CloudWatch regardless of buffer size.
     */
    suspend fun flush() {
        sendLogsToCloudWatch()
    }
}
