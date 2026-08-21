package com.milkcocoa.info.colotok.core.provider.cloudwatch

import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.provider.details.AsyncProvider
import kotlin.time.Instant

class CloudwatchProvider(config: CloudwatchProviderConfig) :
    AsyncProvider(validateCloudwatchConfig(config)) {
    constructor(config: CloudwatchProviderConfig.() -> Unit) : this(
        CloudwatchProviderConfig().apply(config)
    )

    constructor() : this(CloudwatchProviderConfig())

    private val cloudwatchLogGroup = config.logGroup!!
    private val cloudwatchLogStream = config.logStream!!
    private val credential = config.credential!!
    private var sequenceToken: String? = null
    private val clientDelegate = lazy { config.clientFactory.create(credential) }
    private val client: CloudwatchClient
        get() = clientDelegate.value

    override suspend fun onPublish(records: List<LogRecord>) {
        val batches =
            partitionCloudwatchEvents(
                records.mapIndexed { index, record ->
                    IndexedCloudwatchEvent(
                        index = index,
                        event =
                            CloudwatchEvent(
                                timestampMillis = eventTimestamp(record).toEpochMilliseconds(),
                                message = record.format(config.formatter)
                            )
                    )
                }
            )
        if (batches.isEmpty()) return

        client.ensureLogGroup(cloudwatchLogGroup)
        client.ensureLogStream(cloudwatchLogGroup, cloudwatchLogStream)
        batches.forEach { batch ->
            sequenceToken =
                client.putLogEvents(
                    logGroup = cloudwatchLogGroup,
                    logStream = cloudwatchLogStream,
                    events = batch,
                    sequenceToken = sequenceToken
                )
        }
    }

    override fun onClosed() {
        if (clientDelegate.isInitialized()) client.close()
    }

    private fun eventTimestamp(record: LogRecord): Instant =
        when (record) {
            is LogRecord.PlainText -> record.eventTimestamp
            is LogRecord.StructuredText<*> -> record.eventTimestamp
            is LogRecord.Metrics -> record.eventTimestamp
            is LogRecord.Pin -> error("Pin records cannot be published")
        }
}

private fun validateCloudwatchConfig(config: CloudwatchProviderConfig): CloudwatchProviderConfig =
    config.apply {
        val configuredLogGroup = checkNotNull(logGroup) { "Cloudwatch log group is null" }
        check(configuredLogGroup.isNotBlank()) { "Cloudwatch log group must not be blank" }
        val configuredLogStream = checkNotNull(logStream) { "Cloudwatch log stream is null" }
        check(configuredLogStream.isNotBlank()) { "Cloudwatch log stream must not be blank" }
        val configuredCredential = checkNotNull(credential) { "Credential is null" }
        check(configuredCredential.region.isNotBlank()) { "Cloudwatch region must not be blank" }
        when (configuredCredential) {
            is CloudwatchCredential.StaticCredentials -> {
                check(configuredCredential.accessKeyId.isNotBlank()) {
                    "Cloudwatch access key ID must not be blank"
                }
                check(configuredCredential.secretAccessKey.isNotBlank()) {
                    "Cloudwatch secret access key must not be blank"
                }
            }
            is CloudwatchCredential.Profile ->
                check(configuredCredential.profileName.isNotBlank()) {
                    "Cloudwatch profile name must not be blank"
                }
            is CloudwatchCredential.Default,
            is CloudwatchCredential.FromEnvironments -> Unit
        }
    }