package com.milkcocoa.info.colotok.core.provider.cloudwatch

import com.milkcocoa.info.colotok.core.formatter.builtin.structure.SimpleStructureFormatter
import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.metrics.MetricsCollectorSpec
import com.milkcocoa.info.colotok.core.provider.details.AsyncProviderConfig
import com.milkcocoa.info.colotok.core.provider.details.ProviderConfig
import kotlin.time.Duration.Companion.seconds

sealed interface CloudwatchCredential {
    val region: String

    data class FromEnvironments(
        override val region: String
    ) : CloudwatchCredential

    data class StaticCredentials(
        override val region: String,
        val accessKeyId: String,
        val secretAccessKey: String
    ) : CloudwatchCredential

    data class Profile(
        override val region: String,
        val profileName: String
    ) : CloudwatchCredential

    data class Default(
        override val region: String
    ) : CloudwatchCredential
}

class CloudwatchProviderConfig : AsyncProviderConfig {
    internal var clientFactory: CloudwatchClientFactory = AwsCloudwatchClientFactory

    override var level: Level = LogLevel.DEBUG
    override var formatter: Formatter = SimpleStructureFormatter
    override var metricsSpec: MetricsCollectorSpec = MetricsCollectorSpec.Inherit
    override var enableInternalMetricsLogging: Boolean = false
    override var autoFlash: ProviderConfig.AutoFlash = ProviderConfig.AutoFlash.Enabled(30.seconds)

    var logGroup: String? = null
    var logStream: String? = null
    var credential: CloudwatchCredential? = null
    override var bufferSize: Int = 50
}