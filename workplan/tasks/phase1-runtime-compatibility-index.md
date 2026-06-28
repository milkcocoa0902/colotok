# Phase1: Runtime Bugs and Compatibility Holes

## Source Context
- Primary: `docs/QUALITY_IMPROVEMENT_PLAN.md`
- Supporting: `README.md`, `docs/Writerside/topics/02-01-provider-configuration.md`, `workplan/tasks/phase0-quality-stabilization.md`
- Task documents:
  - `workplan/tasks/phase1-date-base-rotation.md`
  - `workplan/tasks/phase1-slf4j-factory-cache-thread-safety.md`
  - `workplan/tasks/phase1-slf4j-formatting-semantics.md`
  - `workplan/tasks/phase1-slf4j1-bridge-completion.md`
  - `workplan/tasks/phase1-android-console-default.md`

## Goal
Phase1 fixes runtime bugs and compatibility holes that can break normal user code after Phase0's delivery and lifecycle behavior has been stabilized.

Phase1 is complete when:

1. `DateBaseRotation` rotates at the correct age threshold and no longer leaks debug output.
2. SLF4J 1.x logger methods are safe to call, including `isXEnabled()` and Marker overloads.
3. SLF4J 1.x and 2.x use SLF4J `{}` message formatting semantics consistently.
4. SLF4J factory caches are safe under concurrent logger acquisition.
5. Android `ConsoleProvider` default behavior is explicit, tested where practical, and defaults to output unless users explicitly disable it.

## Non-Goals
- Do not add time-based async flush in Phase1.
- Do not redesign provider lifecycle or buffering behavior fixed in Phase0.
- Do not implement P2 work such as metrics recursion cleanup, channel overflow policy redesign, or diagnostics cleanup except where directly touched by a P1 bug.
- Do not change artifact layout or publishing configuration.

## Current State
- Phase0 has stabilized flush, failed async publish retention, file sink lifecycle, MDC snapshotting, and event timestamps.
- `DateBaseRotation` currently reverses the rotation predicate, force-unwraps file creation metadata, and prints debug data during rotation.
- SLF4J 1.x still contains many `TODO("Not yet implemented")` methods and uses Java `String.format` instead of SLF4J placeholders.
- SLF4J 2.x already uses `MessageFormatter`, but both SLF4J factories use plain mutable maps for caches.
- Android `ConsoleProvider()` is installed by the default logger context, but Android output is currently silent by default because debug detection defaults to `false`.
- Android framework `Log.isLoggable()` controls tag/level loggability, not whether the app is a debug or release build. AGP 8.0 also no longer generates `BuildConfig` by default, so Phase1 should not rely on `BuildConfig.DEBUG` as an automatic debug detector.

## Boundary Decision
- Owner boundary: Phase1 belongs to runtime/core provider behavior and JVM integration modules.
- Why this belongs there: these are correctness and compatibility fixes for existing public behavior, not new logging features.
- Cross-boundary impact: `colotok` changes affect file rotation and Android console behavior; `colotok-slf4j` and `colotok-slf4j2` changes affect JVM integrations and tests.

## Task Breakdown
The executable task order is:

1. `workplan/tasks/phase1-date-base-rotation.md`
2. `workplan/tasks/phase1-slf4j-factory-cache-thread-safety.md`
3. `workplan/tasks/phase1-slf4j-formatting-semantics.md`
4. `workplan/tasks/phase1-slf4j1-bridge-completion.md`
5. `workplan/tasks/phase1-android-console-default.md`

### Task 1: Fix `DateBaseRotation`
- Objective: make duration-based rotation correct, safe, and quiet.
- Workplan: `workplan/tasks/phase1-date-base-rotation.md`
- Validation: `./gradlew :colotok:jvmTest`

### Task 2: Make SLF4J factory caches thread-safe
- Objective: remove races when multiple threads request logger instances.
- Workplan: `workplan/tasks/phase1-slf4j-factory-cache-thread-safety.md`
- Validation: `./gradlew :colotok-slf4j:test :colotok-slf4j2:test`

### Task 3: Align SLF4J formatting semantics
- Objective: make SLF4J 1.x use `{}` formatting and throwable extraction like SLF4J callers expect.
- Workplan: `workplan/tasks/phase1-slf4j-formatting-semantics.md`
- Validation: `./gradlew :colotok-slf4j:test :colotok-slf4j2:test`

### Task 4: Complete SLF4J 1.x bridge behavior
- Objective: remove remaining `TODO()` runtime failures from the SLF4J 1.x bridge.
- Workplan: `workplan/tasks/phase1-slf4j1-bridge-completion.md`
- Validation: `./gradlew :colotok-slf4j:test`

### Task 5: Invert and clarify Android console default behavior
- Objective: make Android console output policy explicit and verifiable: output by default, opt out explicitly.
- Workplan: `workplan/tasks/phase1-android-console-default.md`
- Validation: `./gradlew :colotok:compileReleaseKotlinAndroid`

## Compatibility Impact
- `DateBaseRotation` changes incorrect behavior. Fresh files should stop rotating immediately; expired files should rotate.
- SLF4J 1.x `{}` formatting changes output for users who accidentally relied on Java `%s` formatting. This is intentional because SLF4J callers expect `{}`.
- SLF4J Marker handling should be source- and binary-compatible. The least invasive Phase1 behavior is to ignore markers while making overloads safe.
- Thread-safe factory caches should preserve object identity per logger name while reducing race risk.
- Android console behavior will become more verbose on Android by default. This is an intentional Phase1 compatibility decision: users who do not want output must opt out explicitly.
- Automatic debug-build detection remains a future enhancement unless it can be implemented without `BuildConfig.DEBUG` or app-specific build metadata.

## Data and Persistence Impact
- `DateBaseRotation` may change when log files are renamed. Existing rotated files remain compatible.
- SLF4J and Android console tasks do not require persistent data migration.

## Validation Plan
- Focused runtime validation:
  - `./gradlew :colotok:jvmTest`
  - `./gradlew :colotok:compileReleaseKotlinAndroid`
- Focused JVM integration validation:
  - `./gradlew :colotok-slf4j:test`
  - `./gradlew :colotok-slf4j2:test`
- Full Phase1 validation before completion:
  - `./gradlew :colotok:jvmTest :colotok-slf4j:test :colotok-slf4j2:test`
  - `./gradlew :colotok:compileKotlinJs :colotok:compileReleaseKotlinAndroid`
  - `git diff --check`

## Risks and Open Questions
- Resolved: Android console should emit by default unless users explicitly disable output. Safe automatic debug detection is deferred until a reliable Android-specific mechanism is chosen.
- Resolved for Phase1: SLF4J Marker values should be ignored while making overloads safe. Full Marker semantics belong to a future SLF4J compatibility phase.
- Risk: `DateBaseRotation` tests depend on filesystem metadata availability. Tests should read actual metadata after file creation and derive mocked `Clock.System.now()` from that value.

## Implementation Order
1. Implement and validate `DateBaseRotation`; it is isolated and removes a clear runtime bug.
2. Make SLF4J factory caches thread-safe; this is small and independent.
3. Align SLF4J 1.x formatting with SLF4J semantics.
4. Complete SLF4J 1.x `isXEnabled()` and Marker overloads using the formatting helper from step 3.
5. Invert Android console defaults and document the explicit opt-out behavior.
6. Run full Phase1 validation.
