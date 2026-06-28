# Phase1: SLF4J 1.x Bridge Completion

## Source Context
- Primary: `docs/QUALITY_IMPROVEMENT_PLAN.md`
- Current code:
  - `colotok-slf4j/src/main/kotlin/com/milkcocoa/info/colotok/core/logger/ColotokLogger4J.kt`
  - `colotok-slf4j/src/test/kotlin/com/milkcocoa/info/colotok/core/logger/ColotokLogger4JTest.kt`
  - `colotok-slf4j2/src/main/kotlin/com/milkcocoa/info/colotok/core/logger/ColotokLogger4J2.kt`

## Goal
Remove `TODO("Not yet implemented")` runtime failures from the SLF4J 1.x bridge with the smallest compatible behavior: enabled checks return safely, Marker overloads delegate safely, and Marker semantics remain minimal.

Full Marker semantics and detailed SLF4J compatibility are deferred to a future phase.

## Non-Goals
- Do not implement advanced Marker routing or filtering in Phase1.
- Do not add provider-level marker filtering.
- Do not change core logger level filtering.
- Do not duplicate formatting logic; use the helper from the SLF4J formatting task.
- Do not add Marker attributes in Phase1 unless a later policy explicitly chooses that output shape.

## Current State
- `ColotokLogger4J` has `TODO()` in plain `isXEnabled()` methods, marker `isXEnabled(marker)` methods, and all Marker logging overloads.
- `ColotokLogger4J2` returns `true` for all enabled checks.
- Current SLF4J 1.x tests do not call `isXEnabled()` or Marker overloads.

## Boundary Decision
- Owner boundary: `colotok-slf4j` JVM integration.
- Why this belongs there: SLF4J users can call every `Logger` method exposed by the interface; throwing `NotImplementedError` is a compatibility bug.
- Cross-boundary impact: no KMP core changes are required.

## Task Breakdown
### Task 1: Add safety tests for enabled checks
- Objective: ensure every `isXEnabled()` method is callable.
- Affected modules/files:
  - `ColotokLogger4JTest.kt`
- Expected behavior:
  - `isTraceEnabled`, `isDebugEnabled`, `isInfoEnabled`, `isWarnEnabled`, and `isErrorEnabled` do not throw.
  - Marker variants do not throw.
  - Return value is `true` for Phase1, matching SLF4J 2.x's current behavior.
- Validation:
  - `./gradlew :colotok-slf4j:test --tests 'com.milkcocoa.info.colotok.core.logger.ColotokLogger4JTest'`

### Task 2: Add safety tests for Marker logging overloads
- Objective: ensure Marker overloads delegate to normal logging.
- Affected modules/files:
  - `ColotokLogger4JTest.kt`
- Expected behavior:
  - Marker overloads for all levels do not throw.
  - Message, level, logger name, default attrs, and throwable attrs behave like non-marker overloads.
  - Marker values are intentionally ignored in Phase1 unless a later policy chooses marker attributes.
- Validation:
  - `./gradlew :colotok-slf4j:test`

### Task 3: Implement enabled checks
- Objective: remove TODOs from `isXEnabled()`.
- Affected modules/files:
  - `ColotokLogger4J.kt`
- Expected behavior:
  - Return `true` for every level and Marker variant, matching `ColotokLogger4J2`.
  - No provider traversal or expensive checks are added in Phase1.
- Validation:
  - `./gradlew :colotok-slf4j:test`

### Task 4: Implement Marker logging overloads
- Objective: delegate Marker overloads through the same path as non-marker overloads.
- Affected modules/files:
  - `ColotokLogger4J.kt`
- Expected behavior:
  - Marker `msg`, `format,arg`, `format,arg1,arg2`, `format,vararg`, and `msg,Throwable` overloads are implemented for trace/debug/info/warn/error.
  - Parameterized marker overloads use SLF4J `{}` formatting helper.
  - Throwable handling matches non-marker overloads.
- Validation:
  - `./gradlew :colotok-slf4j:test`

## Compatibility Impact
- Calls that previously threw `NotImplementedError` will now log or return `true`.
- Marker data is not surfaced as a log attribute in the recommended Phase1 path, avoiding output shape changes.

## Data and Persistence Impact
- None.

## Validation Plan
- `./gradlew :colotok-slf4j:test --tests 'com.milkcocoa.info.colotok.core.logger.ColotokLogger4JTest'`
- `./gradlew :colotok-slf4j:test`

## Risks and Open Questions
- Resolved for Phase1: Marker names are ignored; overloads exist only to avoid runtime failure and preserve normal logging.
- Risk: implementing Marker overloads before the formatting helper can duplicate code. Complete formatting semantics first.

## Implementation Order
1. Complete `phase1-slf4j-formatting-semantics.md` or at least its private helper.
2. Add enabled-check and Marker-overload tests.
3. Implement `isXEnabled()` and Marker `isXEnabled(marker)` methods.
4. Implement Marker logging overloads by delegating to non-marker/helper paths.
5. Run `:colotok-slf4j:test`.
