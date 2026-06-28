# Phase1: SLF4J Formatting Semantics

## Source Context
- Primary: `docs/QUALITY_IMPROVEMENT_PLAN.md`
- Current code:
  - `colotok-slf4j/src/main/kotlin/com/milkcocoa/info/colotok/core/logger/ColotokLogger4J.kt`
  - `colotok-slf4j2/src/main/kotlin/com/milkcocoa/info/colotok/core/logger/ColotokLogger4J2.kt`
  - `colotok-slf4j/src/test/kotlin/com/milkcocoa/info/colotok/core/logger/ColotokLogger4JTest.kt`
  - `colotok-slf4j2/src/test/kotlin/com/milkcocoa/info/colotok/core/logger/ColotokLogger4J2Test.kt`

## Goal
Make the minimum Phase1 SLF4J 1.x parameterized logging path safe and recognizable for normal SLF4J callers by supporting `{}` placeholders and basic throwable extraction.

Full SLF4J conformance is deferred to a future compatibility phase.

## Non-Goals
- Do not add structured logging APIs to SLF4J bindings.
- Do not preserve Java `String.format` `%s` behavior as the primary path.
- Do not change `ColotokLogger` core formatting.
- Do not implement Marker overloads in this task unless they need the same helper; Marker completion is covered separately.
- Do not exhaustively reimplement or test every SLF4J placeholder edge case in Phase1.

## Current State
- SLF4J 1.x overloads call `String.format(format.orEmpty(), ...)`.
- SLF4J callers expect `{}` placeholders.
- SLF4J 2.x already uses `org.slf4j.helpers.MessageFormatter.arrayFormat`.
- Existing SLF4J 1.x test currently asserts `%s` behavior, while SLF4J 2.x test asserts `{}` behavior.
- Vararg handling in SLF4J 1.x passes the array without spread, which can produce incorrect formatting or exceptions.

## Boundary Decision
- Owner boundary: JVM SLF4J integration modules.
- Why this belongs there: placeholder semantics are part of the SLF4J compatibility contract.
- Cross-boundary impact: no KMP core API changes are required.

## Task Breakdown
### Task 1: Update SLF4J 1.x formatting tests
- Objective: make expected behavior match SLF4J semantics.
- Affected modules/files:
  - `ColotokLogger4JTest.kt`
- Expected behavior:
  - `logger.debug("value={}", "A")` logs `value=A`.
  - Multiple placeholders format in order.
  - Extra arguments follow the default `MessageFormatter` result where practical.
  - A trailing `Throwable` in argument arrays is extracted into the `cause` attribute in the common case.
- Validation:
  - `./gradlew :colotok-slf4j:test --tests 'com.milkcocoa.info.colotok.core.logger.ColotokLogger4JTest'`

### Task 2: Introduce a small formatting helper in SLF4J 1.x logger
- Objective: route all non-marker parameterized overloads through one implementation.
- Affected modules/files:
  - `ColotokLogger4J.kt`
- Expected behavior:
  - Use `org.slf4j.helpers.MessageFormatter`.
  - Return both formatted message and extracted throwable when present.
  - Preserve explicit `(msg, Throwable)` overload behavior.
  - Avoid new public API.
- Validation:
  - `./gradlew :colotok-slf4j:test`

### Task 3: Verify SLF4J 2.x remains aligned
- Objective: ensure 2.x behavior still matches the semantics now expected for 1.x.
- Affected modules/files:
  - `ColotokLogger4J2Test.kt`
- Expected behavior:
  - Existing `{}` formatting tests pass.
  - Add a trailing throwable test if missing and if the current implementation supports it.
- Validation:
  - `./gradlew :colotok-slf4j2:test`

## Compatibility Impact
- This intentionally changes SLF4J 1.x parameterized output from Java `%s` formatting to SLF4J `{}` formatting.
- Users who passed `%s` patterns to SLF4J 1.x may see literal `%s`; this aligns with SLF4J expectations.

## Data and Persistence Impact
- None.

## Validation Plan
- `./gradlew :colotok-slf4j:test --tests 'com.milkcocoa.info.colotok.core.logger.ColotokLogger4JTest'`
- `./gradlew :colotok-slf4j2:test --tests 'com.milkcocoa.info.colotok.core.logger.ColotokLogger4J2Test'`
- `./gradlew :colotok-slf4j:test :colotok-slf4j2:test`

## Risks and Open Questions
- Resolved for Phase1: test representative `{}` formatting only. Full edge-case coverage belongs to a future SLF4J compatibility phase.
- Risk: trailing throwable extraction must not duplicate throwable text in both message and attributes.

## Implementation Order
1. Change SLF4J 1.x tests from `%s` expectations to `{}` expectations.
2. Add multi-argument and trailing throwable tests for SLF4J 1.x.
3. Implement a private MessageFormatter helper in `ColotokLogger4J`.
4. Add or confirm equivalent SLF4J 2.x throwable extraction tests.
5. Run both SLF4J module test suites.
6. Record remaining SLF4J edge-case compatibility as future work if needed.
