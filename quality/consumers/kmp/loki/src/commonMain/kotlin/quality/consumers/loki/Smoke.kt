package quality.consumers.loki

import com.milkcocoa.info.colotok.core.logger.ColotokLoggerContext
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.logger.infoAsync
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import com.milkcocoa.info.colotok.core.provider.details.AsyncProviderConfig
import com.milkcocoa.info.colotok.core.provider.details.Provider
import com.milkcocoa.info.colotok.core.provider.loki.LokiProviderConfig

private class RecordingProvider : Provider(ConsoleProviderConfig()) {
    val messages = mutableListOf<String>()

    override suspend fun onMessage(record: LogRecord) {
        messages.add((record as LogRecord.PlainText).msg)
    }
}

suspend fun smoke(): List<String> {
    // References Loki and its transitive coroutines API without opening an HTTP client.
    val config: AsyncProviderConfig = LokiProviderConfig()
    check(config.bufferSize == 50)
    val provider = RecordingProvider()
    val context = ColotokLoggerContext().addProvider(provider)
    try {
        context.getLogger("published.loki").infoAsync("published loki works")
        provider.flush()
        return provider.messages.toList()
    } finally {
        context.shutdown()
    }
}