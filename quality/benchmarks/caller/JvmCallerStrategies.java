package quality.benchmarks.caller;

import com.milkcocoa.info.colotok.util.CallerFrameUtilsKt;
import java.lang.StackWalker.StackFrame;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.stream.Stream;

/** Experimental acquisition backends; the production library is not changed. */
public final class JvmCallerStrategies {
    private static final String STRATEGY = System.getProperty("caller.strategy", "thread-array");
    private static final String[] OWNERS = {
        "com.milkcocoa.info.colotok.util.ThreadWrapper",
        "com.milkcocoa.info.colotok.core.logger.LogEventMetadata",
        "com.milkcocoa.info.colotok.core.logger.LogRecord",
        "com.milkcocoa.info.colotok.core.logger.ColotokLogger",
        "com.milkcocoa.info.colotok.core.logger.LevelScopedColotokLogger",
        "com.milkcocoa.info.colotok.core.logger.ColotokLoggerExtensionKt",
        "com.milkcocoa.info.colotok.core.logger.ColotokLogger4J",
        "com.milkcocoa.info.colotok.core.logger.ColotokLogger4J2"
    };
    private static final String[] NESTED_PREFIXES = new String[OWNERS.length];
    static {
        for (int i = 0; i < OWNERS.length; i++) NESTED_PREFIXES[i] = OWNERS[i] + '$';
    }
    // Preserve reflection frames visible in the existing Thread/Throwable arrays.
    private static final StackWalker WALKER = StackWalker.getInstance(
        EnumSet.of(StackWalker.Option.SHOW_REFLECT_FRAMES));
    private static final StackWalker CLASS_WALKER = StackWalker.getInstance(
        EnumSet.of(StackWalker.Option.SHOW_REFLECT_FRAMES, StackWalker.Option.RETAIN_CLASS_REFERENCE));
    private static final ClassValue<Boolean> LOGGING_CLASS = new ClassValue<Boolean>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            return isLogging(type.getName());
        }
    };

    private static boolean isLogging(String name) {
        for (int i = 0; i < OWNERS.length; i++) {
            if (name.equals(OWNERS[i]) || name.startsWith(NESTED_PREFIXES[i])) return true;
        }
        return name.startsWith("org.slf4j.");
    }

    public static String capture() {
        switch (STRATEGY) {
            case "thread-array": return fromArray(Thread.currentThread().getStackTrace());
            case "throwable-array": return fromArray(new Throwable().getStackTrace());
            case "walker-stream": return WALKER.walk(JvmCallerStrategies::fromStream);
            case "walker-iterator": return WALKER.walk(stream -> fromIterator(stream.iterator(), false));
            case "walker-classvalue": return CLASS_WALKER.walk(stream -> fromIterator(stream.iterator(), true));
            default: throw new IllegalArgumentException("Unknown strategy: " + STRATEGY);
        }
    }

    private static String fromArray(StackTraceElement[] stack) {
        boolean foundLogging = false;
        for (StackTraceElement frame : stack) {
            if (isLogging(frame.getClassName())) foundLogging = true;
            else if (foundLogging) {
                return CallerFrameUtilsKt.formatCallerFrame(frame.getClassName(), frame.getMethodName(), frame.getLineNumber());
            }
        }
        return "";
    }

    private static String fromStream(Stream<StackFrame> stream) {
        return stream.dropWhile(frame -> !isLogging(frame.getClassName()))
            .dropWhile(frame -> isLogging(frame.getClassName()))
            .findFirst().map(JvmCallerStrategies::format).orElse("");
    }

    private static String fromIterator(Iterator<StackFrame> frames, boolean classifyByClass) {
        boolean foundLogging = false;
        while (frames.hasNext()) {
            StackFrame frame = frames.next();
            boolean logging = classifyByClass ? LOGGING_CLASS.get(frame.getDeclaringClass()) : isLogging(frame.getClassName());
            if (logging) foundLogging = true;
            else if (foundLogging) return format(frame);
        }
        return "";
    }

    private static String format(StackFrame frame) {
        return CallerFrameUtilsKt.formatCallerFrame(frame.getClassName(), frame.getMethodName(), frame.getLineNumber());
    }
}
