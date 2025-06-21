package com.milkcocoa.info.colotok.core.provider.cloudwatch

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import com.milkcocoa.info.colotok.core.formatter.builtin.structure.SimpleStructureFormatter
import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.details.ProviderConfig

sealed interface CloudwatchCredential{
    val region: String
    data class FromEnvironments(
        override val region: String,
    ) : CloudwatchCredential

    data class StaticCredentials(
        override val region: String,
        val accessKeyId: String,
        val secretAccessKey: String
    ):  CloudwatchCredential

    data class Profile(
        override  val region: String,
        val profileName: String,
    ): CloudwatchCredential

    data class Default(
        override val  region: String,
    ): CloudwatchCredential
}

class CloudwatchProviderConfig: ProviderConfig {
    override var level: Level = LogLevel.DEBUG
    override var formatter: Formatter = SimpleStructureFormatter

    var logGroup: String? = null
    var logStream: String? = null
    var credential: CloudwatchCredential? = null
    var logBufferSize:  Int = 50
}