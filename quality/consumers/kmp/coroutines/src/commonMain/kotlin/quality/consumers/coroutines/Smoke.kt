package quality.consumers.coroutines

import com.milkcocoa.info.colotok.core.logger.ColotokLoggerContext
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.logger.infoAsync
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import com.milkcocoa.info.colotok.core.provider.details.Provider

private class RecordingProvider : Provider(ConsoleProviderConfig()) {
    val messages = mutableListOf<String>()

    override suspend fun onMessage(record: LogRecord) {
        messages.add((record as LogRecord.PlainText).msg)
    }
}

suspend fun smoke(): List<String> {
    val provider = RecordingProvider()
    val context = ColotokLoggerContext().addProvider(provider)
    try {
        context.getLogger("published.coroutines").infoAsync("published coroutines works")
        provider.flush()
        return provider.messages.toList()
    } finally {
        context.shutdown()
    }
}