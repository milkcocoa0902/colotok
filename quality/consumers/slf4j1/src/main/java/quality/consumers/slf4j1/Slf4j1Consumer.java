package quality.consumers.slf4j1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Slf4j1Consumer {
    private static final String LOGGER_NAME = "quality.consumer.slf4j1";

    private Slf4j1Consumer() {
    }

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(LOGGER_NAME);
        if (!LOGGER_NAME.equals(logger.getName())) {
            throw new AssertionError(
                "Expected logger name " + LOGGER_NAME + ", but was " + logger.getName()
            );
        }

        logger.info("SLF4J 1 isolated consumer {}", "ok");
    }
}
