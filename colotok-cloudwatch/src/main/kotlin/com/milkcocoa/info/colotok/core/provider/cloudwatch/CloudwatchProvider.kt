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
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.collections.push
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.provider.details.Provider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlin.concurrent.atomics.AtomicArray
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class CloudwatchProvider(config: CloudwatchProviderConfig): Provider {
    constructor(config: CloudwatchProviderConfig.()->Unit): this(CloudwatchProviderConfig().apply(config))
    constructor(): this(CloudwatchProviderConfig())

    private val logLevel = config.level
    private val formatter = config.formatter
    private val cloudwatchLogGroup = config.logGroup
    private val cloudwatchLogStream = config.logStream
    private val credential = config.credential

    private var sequenceToken: String? = null

    private val mutex = Mutex()

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
    ) {
        if (level.isEnabledFor(logLevel).not()) {
            return
        }

        runCatching {
            runBlocking {
                mutex.withLock {
                    val response = client().putLogEvents putLogEvents@{
                        this.logGroupName = this@CloudwatchProvider.cloudwatchLogGroup
                        this.logStreamName = this@CloudwatchProvider.cloudwatchLogStream
                        this.sequenceToken = this@CloudwatchProvider.sequenceToken
                        this.logEvents = listOf(InputLogEvent {
                            this.timestamp = Clock.System.now().toEpochMilliseconds()
                            this.message = formatter.format(msg, level, attr)
                        })
                    }

                    this@CloudwatchProvider.sequenceToken = response.nextSequenceToken
                }
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
    ) {
        if (level.isEnabledFor(logLevel).not()) {
            return
        }

        runCatching {
            runBlocking {
                mutex.withLock {
                    val response = client().putLogEvents putLogEvents@{
                        this.logGroupName = this@CloudwatchProvider.cloudwatchLogGroup
                        this.logStreamName = this@CloudwatchProvider.cloudwatchLogStream
                        this.sequenceToken = this@CloudwatchProvider.sequenceToken
                        this.logEvents = listOf(InputLogEvent {
                            this.timestamp = Clock.System.now().toEpochMilliseconds()
                            this.message = formatter.format(msg, serializer, level, attr)
                        })
                    }

                    this@CloudwatchProvider.sequenceToken = response.nextSequenceToken
                }
            }
        }
    }
}