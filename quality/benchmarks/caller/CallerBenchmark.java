package quality.benchmarks.caller;

import com.milkcocoa.info.colotok.core.formatter.builtin.text.SimpleTextFormatter;
import com.milkcocoa.info.colotok.core.level.LogLevel;
import com.milkcocoa.info.colotok.core.logger.LogRecord;
import com.milkcocoa.info.colotok.core.logger.LogRecordKt;
import com.milkcocoa.info.colotok.core.logger.MDC;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import kotlin.time.Clock;
import org.openjdk.jmh.annotations.*;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 3, jvmArgsAppend = {"-Xms512m", "-Xmx512m", "-XX:+UseG1GC"})
@Threads(1)
public class CallerBenchmark {
    // Extra non-inlined application frames; JMH's own stack is present in both cases.
    @Param({"0", "16"})
    public int depth;

    @Setup(Level.Trial)
    public void setup() {
        MDC.INSTANCE.clear();
        LogRecord.PlainText record = plainAtDepth(depth);
        String caller = LogRecordKt.getEventCallerSnapshot(record);
        boolean omitted = Boolean.getBoolean("caller.omitted");
        if (omitted != caller.isEmpty()) throw new IllegalStateException("Unexpected caller: " + caller);
        if (!omitted && !caller.contains("CallerBenchmark#plainAtDepth")) {
            throw new IllegalStateException("Caller must point to application benchmark: " + caller);
        }
        String output = record.format(SimpleTextFormatter.INSTANCE);
        if (!output.contains("[INFO] - benchmark message")) throw new IllegalStateException(output);
    }

    @Benchmark
    public Object plainRecord() {
        return plainAtDepth(depth);
    }

    @Benchmark
    public Object metricsRecord() {
        return metricsAtDepth(depth);
    }

    @Benchmark
    public Object plainRecordAndSimpleFormat() {
        return plainAtDepth(depth).format(SimpleTextFormatter.INSTANCE);
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public LogRecord.PlainText plainAtDepth(int remaining) {
        if (remaining > 0) return plainAtDepth(remaining - 1);
        return new LogRecord.PlainText("benchmark", "benchmark message", LogLevel.INFO.INSTANCE,
            Collections.emptyMap(), Clock.System.INSTANCE.now());
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public LogRecord.Metrics metricsAtDepth(int remaining) {
        if (remaining > 0) return metricsAtDepth(remaining - 1);
        return new LogRecord.Metrics("benchmark", "benchmark message", LogLevel.INFO.INSTANCE,
            Collections.emptyMap(), Clock.System.INSTANCE.now());
    }
}
