package quality.benchmarks.caller;

import com.milkcocoa.info.colotok.core.level.Level;
import com.milkcocoa.info.colotok.core.level.LogLevel;
import com.milkcocoa.info.colotok.core.logger.ColotokLogger;
import com.milkcocoa.info.colotok.core.logger.ColotokLoggerContext;
import com.milkcocoa.info.colotok.core.logger.LogRecord;
import com.milkcocoa.info.colotok.core.logger.LogRecordKt;
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig;
import com.milkcocoa.info.colotok.core.provider.details.Provider;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.time.Clock;
import kotlin.time.Instant;
import kotlinx.coroutines.channels.BufferOverflow;

public final class CallerStrategiesFixture {
    private static LogRecord.PlainText direct(int remaining) {
        if (remaining > 0) return direct(remaining - 1);
        return new LogRecord.PlainText("fixture", "message", LogLevel.INFO.INSTANCE,
            Collections.emptyMap(), Clock.System.INSTANCE.now());
    }

    private static LogRecord.PlainText reflective() throws Exception {
        return LogRecord.PlainText.class.getConstructor(String.class, String.class, Level.class, Map.class, Instant.class)
            .newInstance("fixture", "message", LogLevel.INFO.INSTANCE, Collections.emptyMap(), Clock.System.INSTANCE.now());
    }

    private static LogRecord.PlainText methodHandle() throws Throwable {
        var constructor = MethodHandles.lookup().findConstructor(LogRecord.PlainText.class,
            MethodType.methodType(void.class, String.class, String.class, Level.class, Map.class, Instant.class));
        return (LogRecord.PlainText) constructor.invokeWithArguments("fixture", "message", LogLevel.INFO.INSTANCE,
            Collections.emptyMap(), Clock.System.INSTANCE.now());
    }

    private static LogRecord.PlainText slf4jBridge() {
        return org.slf4j.CallerBridge.create();
    }

    private static final class Recorder extends Provider {
        final CompletableFuture<LogRecord> message = new CompletableFuture<>();
        Recorder() { super(new ConsoleProviderConfig(), BufferOverflow.SUSPEND); }
        @Override
        public Object onMessage(LogRecord record, Continuation<? super Unit> continuation) {
            message.complete(record);
            return Unit.INSTANCE;
        }
    }

    private static LogRecord loggerAndWorker() throws Exception {
        Recorder recorder = new Recorder();
        ColotokLogger logger = new ColotokLoggerContext().addProvider(recorder).getLogger("fixture");
        try {
            logger.info("message");
            return recorder.message.get(10, TimeUnit.SECONDS);
        } finally {
            recorder.close();
            CompletableFuture<Unit> joined = new CompletableFuture<>();
            Object result = recorder.join(new Continuation<Unit>() {
                @Override public CoroutineContext getContext() { return EmptyCoroutineContext.INSTANCE; }
                @Override public void resumeWith(Object result) {
                    try { kotlin.ResultKt.throwOnFailure(result); joined.complete(Unit.INSTANCE); }
                    catch (Throwable failure) { joined.completeExceptionally(failure); }
                }
            });
            if (result != IntrinsicsKt.getCOROUTINE_SUSPENDED()) joined.complete(Unit.INSTANCE);
            joined.get(10, TimeUnit.SECONDS);
        }
    }

    private static void output(String scenario, LogRecord record) {
        String caller = LogRecordKt.getEventCallerSnapshot(record);
        if (caller.isEmpty()) throw new IllegalStateException("Caller must always be retained: " + scenario);
        System.out.println(scenario + "\t" + caller);
    }

    public static void main(String[] args) throws Throwable {
        output("direct-0", direct(0));
        output("direct-16", direct(16));
        output("reflection", reflective());
        output("method-handle", methodHandle());
        output("slf4j-bridge", slf4jBridge());
        output("logger-after-worker", loggerAndWorker());
    }
}
