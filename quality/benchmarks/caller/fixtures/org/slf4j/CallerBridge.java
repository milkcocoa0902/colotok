package org.slf4j;

import com.milkcocoa.info.colotok.core.level.LogLevel;
import com.milkcocoa.info.colotok.core.logger.LogRecord;
import java.util.Collections;
import kotlin.time.Clock;

/** Fixture for the existing org.slf4j.* wrapper exclusion rule, not an SLF4J implementation. */
public final class CallerBridge {
    public static LogRecord.PlainText create() {
        return new LogRecord.PlainText("fixture", "message", LogLevel.INFO.INSTANCE,
            Collections.emptyMap(), Clock.System.INSTANCE.now());
    }
}
