# Colotok quality improvement plan

> Status after the 2026-07-10 reassessment: the planned P0/P1 code changes are
> implementation-complete, but the P0/P1 quality gate remains open. Release-blocking lifecycle,
> MDC, resource-ownership, SLF4J, Android compatibility, and verification gaps are tracked in
> `workplan/post-p1/index.md`. Time-based flush is deferred until those gates
> close because it is a feature addition, not a prerequisite for runtime correctness.

This document fixes the direction for stabilizing Colotok before adding new runtime features.
The goal is to remove correctness bugs and lifecycle risks first, then add behavior such as
time-based flushing on top of a stable provider pipeline.

## Scope

This plan covers the logging runtime, provider lifecycle, buffering, formatting metadata,
MDC behavior, built-in providers, official integration providers, SLF4J bridges, and tests.

This plan does not cover new feature expansion, API redesign for convenience, visual docs polish,
or publishing automation except where those items block correctness or compatibility.

## Guiding principles

1. Preserve source compatibility unless a bug cannot be fixed safely without an API change.
2. Prefer fixing data loss, lifecycle leaks, and runtime exceptions before adding new behavior.
3. Keep provider behavior deterministic: enqueue, process, flush, close, and force-close must have clear contracts.
4. Capture log event metadata at log call time, not at formatter or publisher execution time.
5. Avoid platform-specific shortcuts in common contracts unless the platform limitation is explicit in the API.
6. Add regression tests for every fixed bug, especially async and shutdown paths.
7. Keep metrics non-recursive and best-effort; metrics must not create unbounded log traffic.

## Priority order

### P0: Data loss and lifecycle correctness

These are release-blocking because they can lose logs, leak resources, or leave shutdown behavior ambiguous.

1. Fix `FileProvider` sink handling.
   - Current issue: `appendingSink()` is opened per message and written without `use`, `close`, or explicit flush.
   - Expected outcome: every opened sink is closed reliably, and tests verify file writes survive flush/shutdown.
   - Main files: `FileProvider.kt`, `SinkUtil.kt`, file provider tests.

2. Define and harden provider lifecycle.
   - Current issue: `flush()` sends a pin before the timeout block and does not clearly handle closed or failed providers.
   - Expected outcome: `flush()` either completes all work up to the call, fails predictably, or times out within the requested duration.
   - Main files: `Provider.kt`, shutdown tests.

3. Preserve buffered records on publish failure.
   - Current issue: `AsyncProvider` clears the buffer before publishing and drops the batch if publish fails.
   - Expected outcome: failed batches are either requeued, retained for next flush, or intentionally dropped by an explicit overflow policy.
   - Main files: `AsyncProvider.kt`, Loki/CloudWatch provider tests.

4. Fix MDC snapshot semantics.
   - Current issue: `LogRecord` stores the current MDC object by reference.
   - Expected outcome: each log record contains an immutable/deep-copied MDC snapshot from the log call time.
   - Main files: `LogRecord.kt`, MDC tests, formatter tests.

5. Fix event timestamp semantics.
   - Current issue: formatters and remote providers call `Clock.System.now()` when they format or publish.
   - Expected outcome: `LogRecord` stores the event timestamp; formatters, Loki, and CloudWatch use that timestamp.
   - Main files: `LogRecord.kt`, `TextFormatter.kt`, `StructuredFormatter.kt`, `LokiProvider.kt`, `CloudwatchProvider.kt`.

### P1: Runtime bugs and compatibility holes

These are likely to break users during normal usage, but they are less directly tied to log loss than P0.

1. Fix `DateBaseRotation`.
   - Current issue: the rotation predicate is reversed, `createdAtMillis` is force-unwrapped, and debug `println` calls remain.
   - Expected outcome: rotation happens when file age is greater than or equal to the configured period, missing metadata is handled safely, and no debug output leaks.
   - Main files: `DateBaseRotation.kt`, rotation tests.

2. Complete SLF4J 1.x bridge behavior.
   - Current issue: many methods still call `TODO()`, including `isXEnabled()` and Marker overloads.
   - Expected outcome: all Logger methods are safe to call; Marker overloads either delegate with marker attributes or intentionally ignore markers without throwing.
   - Main files: `ColotokLogger4J.kt`, SLF4J 1.x tests.

3. Align SLF4J formatting semantics.
   - Current issue: SLF4J 1.x uses `String.format`, while SLF4J callers normally use `{}` placeholders.
   - Expected outcome: SLF4J bindings use SLF4J message formatting rules consistently.
   - Main files: `ColotokLogger4J.kt`, `ColotokLogger4J2.kt`.

4. Make logger factory caches thread-safe.
   - Current issue: SLF4J factory caches use plain mutable maps.
   - Expected outcome: concurrent logger acquisition is safe on JVM.
   - Main files: `ColotokLoggerFactory4J.kt`, `ColotokLoggerFactory4J2.kt`.

5. Clarify Android console default behavior.
   - Current issue: Android console output is disabled by default unless debug detection is supplied.
   - Expected outcome: default behavior is intentional and documented, or debug detection has a safe default that works for typical Android apps.
   - Main files: Android console provider/config docs and tests.

### P2: Observability and async behavior

These improve correctness under load and make the runtime easier to reason about.

1. Make internal metrics logging non-recursive.
   - Current issue: internal metrics are written back into the same provider, and async buffer metrics can trigger more metrics.
   - Expected outcome: metrics records do not emit metrics about themselves, including buffer size updates.
   - Main files: `InternalLoggingMetricsCollector.kt`, `Provider.kt`, `AsyncProvider.kt`.

2. Add bounded behavior tests for channel overflow.
   - Current issue: synchronous `write()` uses `trySend`; if the channel is full, logs are dropped with only a metric.
   - Expected outcome: this policy is either documented as best-effort or exposed as configurable behavior.
   - Main files: `Provider.kt`, provider config docs.

3. Remove direct `println` diagnostics from library internals.
   - Current issue: provider and formatter internals print failures/warnings directly.
   - Expected outcome: diagnostics go through metrics, structured error hooks, or documented best-effort stderr behavior.
   - Main files: `Provider.kt`, `AsyncProvider.kt`, `StructuredFormatter.kt`, rotation classes.

4. Add multi-platform lifecycle tests where practical.
   - Current issue: most lifecycle coverage is JVM-heavy.
   - Expected outcome: common tests cover `AsyncProvider` flush, publish failure, and shutdown semantics.
   - Main files: common tests in `colotok-coroutines`, JVM tests for file and SLF4J.

## Time-based flush plan

Time-based flushing should be added only after P0 lifecycle and buffer semantics are fixed.

Target behavior:

1. `AsyncProviderConfig` gains an optional flush interval.
2. If `bufferSize` is reached, publish immediately.
3. If the buffer is non-empty and no size-based publish happens before the interval, publish on the timer.
4. `flush()` still provides a synchronous boundary for all records accepted before the call.
5. `join()` drains records and publishes the remaining buffer once.
6. `forceShutdown()` cancels quickly and documents that queued records may be lost.

Design constraints:

1. The timer must not publish concurrently with size-based flush or manual flush.
2. The timer must stop when the provider is closed.
3. The timer must not keep JS or Native runtimes alive unexpectedly beyond the provider lifecycle.
4. Failed timer publishes must follow the same retention/drop policy as normal publishes.
5. Tests should use `kotlinx.coroutines.test` virtual time where possible.

## Compatibility policy

Patch-level fixes may change incorrect behavior when the current behavior is clearly a bug:

1. `DateBaseRotation` rotating at the wrong time.
2. SLF4J methods throwing `NotImplementedError`.
3. MDC values changing after log call.
4. Remote timestamps representing publish time instead of event time.
5. Resource leaks in file writes.

Behavior that may surprise users should be documented in release notes:

1. Event timestamp changes from output time to log-call time.
2. Failed async publishes may be retried/retained instead of dropped.
3. Internal metrics logging may emit fewer records after recursion guards are added.

API additions should be additive by default. If a breaking change is required, introduce a deprecated bridge first and remove it in a later minor release.

## Test strategy

Each bug fix should include the smallest regression test that fails before the fix.

Required test areas:

1. File sink close/flush behavior.
2. Provider flush timeout and closed-provider behavior.
3. AsyncProvider publish failure retention.
4. MDC snapshot after mutation.
5. Event timestamp propagation through text formatter, structured formatter, Loki, and CloudWatch.
6. Date-based rotation threshold.
7. SLF4J 1.x `isXEnabled()` and Marker overloads.
8. Internal metrics recursion guard.
9. Time-based flush with virtual time after P0/P1 fixes land.

Verification commands for JVM-focused changes:

```bash
./gradlew :colotok:jvmTest :colotok-coroutines:jvmTest :colotok-loki:jvmTest :colotok-slf4j:test :colotok-slf4j2:test :colotok-cloudwatch:test
```

Verification commands for KMP-facing changes:

```bash
./gradlew :colotok:compileKotlinJs :colotok-coroutines:compileKotlinJs :colotok-loki:compileKotlinJs
```

Native and Android validation should be added when a change touches `nativeMain`, `androidMain`, or common lifecycle behavior with platform-specific actual implementations.

## Suggested execution sequence

The original P0/P1 implementation sequence has been executed. The remaining order is revised to:

1. Close provider lifecycle, failure signaling, bounded buffering, and resource-ownership gaps.
2. Close event snapshot and multiplatform MDC correctness gaps.
3. Close SLF4J, Android, rotation, and publication compatibility gaps.
4. Establish repeatable multiplatform, ABI, consumer, and documentation quality gates.
5. Complete the remaining P2 diagnostics cleanup only after the release-blocking gates pass.
6. Move time-based flush to a separate feature roadmap and reconsider it after stabilization.

The executable task split and evidence are in `workplan/post-p1/index.md`.
