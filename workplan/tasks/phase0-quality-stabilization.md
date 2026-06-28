# Phase0: Quality Stabilization

## Source Context
- Primary: `docs/QUALITY_IMPROVEMENT_PLAN.md`
- Supporting: `README.md`, `docs/Writerside/topics/02-configuration.md`, `docs/Writerside/topics/Official-Plugin.md`
- Code boundaries inspected: `Provider.kt`, `AsyncProvider.kt`, `LogRecord.kt`, `FileProvider.kt`, text/structured formatters, Loki and CloudWatch providers, existing JVM/common tests
- Repository notes: no `AGENTS.md`, `CLAUDE.md`, `.github/copilot-instructions.md`, or established planning directory was found. This workplan uses `workplan/tasks/` per the `phase-workplan` fallback convention.

## Goal
Phase0 stabilizes Colotok before feature expansion. The goal is to remove log loss, lifecycle ambiguity, resource leaks, and log metadata drift in the core runtime and buffering pipeline.

Phase0 is complete when:

1. `FileProvider` does not leak or leave unflushed sinks.
2. `Provider.flush`, `join`, `close`, and `forceShutdown` have explicit behavior and regression tests.
3. `AsyncProvider` does not silently lose buffered records on publish failure.
4. `LogRecord` stores MDC and event timestamp as log-call-time snapshots.
5. Formatters and remote providers use the event metadata stored on `LogRecord`.

## Non-Goals
- Do not add time-based flush in Phase0.
- Do not redesign provider APIs for ergonomics.
- Do not complete P1/P2 work such as `DateBaseRotation`, SLF4J 1.x completion, Android console defaults, diagnostics cleanup, or metrics recursion cleanup unless required to make a Phase0 test deterministic.
- Do not change public artifact layout or publishing configuration.

## Current State
- `Provider` uses a `Channel<LogRecord>` and `LogRecord.Pin` to create a flush boundary.
- `flush()` sends the pin before the timeout block, so timeout does not cover enqueueing the flush token.
- `close()` closes the channel without waiting. `join()` closes and waits. `forceShutdown()` cancels and blocks until the job finishes.
- `AsyncProvider` clears its internal buffer before publishing; failed publish attempts are counted and printed, but records are not retained.
- `LogRecord` stores `MDC.getThreadLocalContext()` by reference, so later MDC mutations can affect queued records.
- Formatters, Loki, and CloudWatch call `Clock.System.now()` at format/publish time, not log-call time.
- `FileProvider` opens an appending sink per message and writes through `SinkUtil.write`, but does not close the sink.

## Boundary Decision
- Owner boundary: Phase0 belongs to runtime/core and official provider integration boundaries.
- Why this belongs there: the affected behavior is part of Colotok's central log delivery contract, not a downstream plugin feature.
- Cross-boundary impact: `colotok` changes affect all providers; `colotok-coroutines` changes affect Loki and CloudWatch; timestamp changes touch formatter and remote-provider outputs.

## Task Breakdown

### Task 1: Fix `FileProvider` sink lifecycle
- Objective: ensure every file sink opened for a log record is flushed/closed reliably.
- Affected modules/files:
  - `colotok/src/commonMain/kotlin/com/milkcocoa/info/colotok/core/provider/builtin/file/FileProvider.kt`
  - `colotok/src/commonMain/kotlin/com/milkcocoa/info/colotok/util/SinkUtil.kt` if a helper adjustment is needed
  - `colotok/src/jvmTest/kotlin/com/milkcocoa/info/colotok/core/provider/builtin/FileProviderTest.kt`
- Expected behavior:
  - Each write uses `use` or an equivalent close-safe pattern.
  - Existing file output and rotation behavior remain source-compatible.
  - `flush()` and `shutdown()` preserve file contents in focused tests.
- Validation:
  - `./gradlew :colotok:jvmTest`
- Notes:
  - Keep this change isolated from provider lifecycle refactors.
  - If `SinkUtil.write` remains useful, do not broaden it into a lifecycle owner; sink ownership should stay with the caller.

### Task 2: Add provider lifecycle contract tests
- Objective: define expected behavior before changing lifecycle internals.
- Affected modules/files:
  - `colotok/src/jvmTest/kotlin/com/milkcocoa/info/colotok/core/logger/ContextShutdownTest.kt`
  - `colotok/src/jvmTest/kotlin/com/milkcocoa/info/colotok/core/logger/ForceShutdownTest.kt`
  - new or existing provider lifecycle test under `colotok/src/jvmTest/kotlin/com/milkcocoa/info/colotok/core/provider/details/`
- Expected behavior:
  - `flush()` waits for records accepted before the call and invokes `onFlush()`.
  - `flush(timeout)` respects the timeout for the whole flush operation, including pin enqueue or equivalent boundary creation.
  - `flush()` after `close()` or `forceShutdown()` is a silent no-op from the caller's perspective and does not throw.
  - `join()` drains accepted records and calls `onFlush()`/`onClosed()` once.
  - `close()` stops accepting new records without pretending to drain.
  - `forceShutdown()` cancels quickly and documents that queued records may be lost.
- Validation:
  - `./gradlew :colotok:jvmTest`
- Notes:
  - Prefer small fake providers with deterministic delays and counters.
  - Tests should not depend on wall-clock sleeps when `kotlinx.coroutines.test` can model the behavior.

### Task 3: Harden `Provider` lifecycle implementation
- Objective: implement the contract fixed by Task 2.
- Affected modules/files:
  - `colotok/src/commonMain/kotlin/com/milkcocoa/info/colotok/core/provider/details/Provider.kt`
  - related shutdown tests
- Expected behavior:
  - `flush()` returns without error when the provider is closed or cancelled.
  - `flush()` does not attempt `channel.send(LogRecord.Pin(...))` after the channel is already closed.
  - timeout behavior is bounded and predictable.
  - `onFlush()` failures do not deadlock a waiting flush token.
  - `onClosed()` remains best-effort and is not called multiple times during normal shutdown.
- Validation:
  - `./gradlew :colotok:jvmTest :colotok-coroutines:jvmTest`
- Notes:
  - Avoid adding time-based flush here.
  - Do not change the public `flush(timeout: Duration = 1000.milliseconds)` signature unless unavoidable.
  - If behavior on closed provider must throw, use a documented standard coroutine/channel exception rather than a new broad runtime exception.

### Task 4: Preserve `AsyncProvider` buffered records on publish failure
- Objective: stop silent log loss when `onPublish(records)` fails.
- Affected modules/files:
  - `colotok-coroutines/src/commonMain/kotlin/com/milkcocoa/info/colotok/core/provider/details/AsyncProvider.kt`
  - `colotok-coroutines/src/commonTest/kotlin/com/milkcocoa/info/colotok/core/provider/details/AsyncProviderTest.kt`
  - Loki/CloudWatch tests if provider-specific behavior needs verification
- Expected behavior:
  - A failed publish retains the failed records for a later `flush()` or later publish attempt.
  - No automatic retry loop is added in Phase0.
  - Retained failed records are eligible for resend on the next size-based publish trigger or manual `flush()`.
  - Record order is preserved across failure and retry.
  - Successful retry clears only records that were actually published.
  - `publish_failed` metrics remain best-effort and do not mask the retained batch.
- Validation:
  - `./gradlew :colotok-coroutines:jvmTest`
  - `./gradlew :colotok-loki:jvmTest :colotok-cloudwatch:test` when integration provider behavior is touched
- Notes:
  - The least invasive policy is to restore the failed batch to the front of the buffer under the existing mutex.
  - Call out potential duplicate delivery risk if `onPublish` fails after the remote service accepted the batch but before the client returns success.
  - Do not add a retry loop in Phase0 unless required for a deterministic retry test; retention is the Phase0 target.

### Task 5: Snapshot MDC at log call time
- Objective: make queued records immune to later MDC mutation.
- Affected modules/files:
  - `colotok/src/commonMain/kotlin/com/milkcocoa/info/colotok/core/logger/LogRecord.kt`
  - `colotok/src/commonMain/kotlin/com/milkcocoa/info/colotok/core/logger/MDC.kt`
  - `colotok/src/jvmTest/kotlin/com/milkcocoa/info/colotok/core/logger/ColotokMdcBasicTest.kt`
  - formatter tests that use `Element.CUSTOM`
- Expected behavior:
  - `LogRecord.PlainText`, `LogRecord.StructuredText`, and `LogRecord.Metrics` store a deep-copied MDC snapshot.
  - Mutating `MDC` after calling `logger.info(...)` does not affect the already queued record.
  - Existing coroutine MDC propagation behavior remains intact.
- Validation:
  - `./gradlew :colotok:jvmTest`
- Notes:
  - Prefer reusing `MDCContextData.deepCopy()`.
  - Keep the `MDCContextData` type stable unless immutability is introduced in a later phase.

### Task 6: Store event timestamp on `LogRecord`
- Objective: make the event timestamp represent log-call time instead of format/publish time.
- Affected modules/files:
  - `colotok/src/commonMain/kotlin/com/milkcocoa/info/colotok/core/logger/LogRecord.kt`
  - `colotok/src/commonMain/kotlin/com/milkcocoa/info/colotok/core/formatter/details/TextFormatter.kt`
  - `colotok/src/commonMain/kotlin/com/milkcocoa/info/colotok/core/formatter/details/StructuredFormatter.kt`
  - `colotok-loki/src/commonMain/kotlin/com/milkcocoa/info/colotok/core/provider/loki/LokiProvider.kt`
  - `colotok-cloudwatch/src/main/kotlin/com/milkcocoa/info/colotok/core/provider/cloudwatch/CloudwatchProvider.kt`
  - formatter, Loki payload, and CloudWatch provider tests
- Expected behavior:
  - every non-pin `LogRecord` has an event timestamp set at construction time.
  - text and structured formatters render `Element.DATE`, `Element.TIME`, and `Element.DATETIME` from the record timestamp.
  - Loki and CloudWatch event timestamps use the record timestamp.
  - Tests can inject deterministic records or mock `Clock.System.now()` only at record creation time.
- Validation:
  - `./gradlew :colotok:jvmTest :colotok-loki:jvmTest :colotok-cloudwatch:test`
  - `./gradlew :colotok:compileKotlinJs :colotok-loki:compileKotlinJs`
- Notes:
  - Add new constructor properties with defaults where possible to reduce source compatibility impact.
  - Structured formatter has an overload that currently reads thread and MDC directly; remove or route it through record metadata so behavior is consistent.

### Task 7: Phase0 integration verification and documentation notes
- Objective: confirm Phase0 fixes work together and record behavior changes for release notes/public docs.
- Affected modules/files:
  - `docs/QUALITY_IMPROVEMENT_PLAN.md` if status tracking is added later
  - `docs/Writerside/topics/02-configuration.md`
  - `docs/Writerside/topics/Official-Plugin.md`
  - `README.md` only if public behavior notes are needed immediately
- Expected behavior:
  - Public docs accurately describe shutdown/flush and async buffering semantics after fixes.
  - No docs claim time-based flush exists before it is implemented.
  - Validation commands pass for touched modules.
- Validation:
  - `./gradlew :colotok:jvmTest :colotok-coroutines:jvmTest :colotok-loki:jvmTest :colotok-slf4j:test :colotok-slf4j2:test :colotok-cloudwatch:test`
  - `./gradlew :colotok:compileKotlinJs :colotok-coroutines:compileKotlinJs :colotok-loki:compileKotlinJs`
- Notes:
  - Keep docs updates factual and small. Broader user-facing docs for time-based flush belong to the later flush feature phase.

## Compatibility Impact
- `FileProvider` sink lifecycle changes should be behavior-only and source-compatible.
- Provider lifecycle hardening should make `flush()` after `close()` or `forceShutdown()` silent and source-compatible for callers.
- Async publish retention changes failed-send behavior from drop to retry/retain. This is a correctness fix but may cause duplicate remote delivery when the remote service accepted a batch and the client still reported failure.
- Adding event timestamp to `LogRecord` can affect direct construction sites. Add default constructor values and update tests to preserve source compatibility where practical.
- Formatter output dates will shift from output time to log-call time. This is intentional and should be mentioned in release notes.

## Data and Persistence Impact
- No schema or persistent data migration is required.
- File logs may become more reliable because sinks close deterministically.
- Remote providers may retain records longer after publish failures, increasing in-memory buffer size until a successful retry or shutdown policy handles the records.

## Observability Impact
- Existing `buffer_full` and `publish_failed` metric names should remain stable.
- Phase0 should not introduce high-cardinality metric labels.
- Internal metrics recursion is a P2 item, but Phase0 changes must avoid making recursion worse.
- Direct `println` cleanup is P2, but new Phase0 code should not add new direct library diagnostics.

## Validation Plan
- Focused validation after each task:
  - `./gradlew :colotok:jvmTest`
  - `./gradlew :colotok-coroutines:jvmTest`
  - `./gradlew :colotok-loki:jvmTest`
  - `./gradlew :colotok-cloudwatch:test`
- Full JVM validation before Phase0 is considered complete:
  - `./gradlew :colotok:jvmTest :colotok-coroutines:jvmTest :colotok-loki:jvmTest :colotok-slf4j:test :colotok-slf4j2:test :colotok-cloudwatch:test`
- KMP-facing compile validation:
  - `./gradlew :colotok:compileKotlinJs :colotok-coroutines:compileKotlinJs :colotok-loki:compileKotlinJs`
- Android/Native validation to confirm:
  - Add or run target-specific compilation when Phase0 changes touch `androidMain`, `nativeMain`, or expect/actual APIs.

## Risks and Open Questions
- Resolved: `flush()` after `close()` or `forceShutdown()` should be silent from the caller's perspective. The implementation should avoid enqueueing `LogRecord.Pin` into a closed channel, likely by checking job/channel state or using a non-throwing send path.
- Open: should async publish failure retention have a maximum retained size? Keep this under discussion; Phase0 should not add a new config surface unless implementation proves it necessary.
- Resolved: failed publishes should not be retried automatically in a retry loop. Retained records should be retried on the next normal send trigger or manual `flush()`.
- Open: how should CloudWatch partial success or sequence-token failures be handled? Working model: if a batch `1, 2, 3, 4, 5` fails at `3`, records `3, 4, 5` should remain retry candidates because later records may fail due sequence-token ordering. This needs CloudWatch-specific investigation before broadening Phase0.
- Resolved: `LogRecord.Pin` must remain a control record and should not carry an event timestamp.

## Implementation Order
1. Fix `FileProvider` sink lifecycle and run `:colotok:jvmTest`.
2. Add provider lifecycle contract tests before changing lifecycle behavior.
3. Harden `Provider.flush`, `join`, `close`, and `forceShutdown` according to those tests.
4. Add `AsyncProvider` publish failure retention tests.
5. Implement failed-batch retention in `AsyncProvider`.
6. Add MDC snapshot regression tests.
7. Implement deep-copy MDC snapshots in `LogRecord`.
8. Add event timestamp regression tests for formatters and remote providers.
9. Implement `LogRecord` event timestamps and route formatters/Loki/CloudWatch through them.
10. Run full JVM and JS compile validation.
11. Update public docs/release notes only for finalized Phase0 behavior changes.
