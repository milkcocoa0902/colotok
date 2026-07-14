package quality.consumers.slf4j2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Slf4j2Consumer {
    private static final String LOGGER_NAME = "quality.consumer.slf4j2";

    private Slf4j2Consumer() {
    }

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(LOGGER_NAME);
        if (!LOGGER_NAME.equals(logger.getName())) {
            throw new AssertionError(
                "Expected logger name " + LOGGER_NAME + ", but was " + logger.getName()
            );
        }

        logger.atInfo()
            .addArgument("ok")
            .log("SLF4J 2 isolated fluent consumer {}");
    }
}
