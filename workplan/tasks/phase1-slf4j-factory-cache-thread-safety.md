# Phase1: SLF4J Factory Cache Thread Safety

## Source Context
- Primary: `docs/QUALITY_IMPROVEMENT_PLAN.md`
- Current code:
  - `colotok-slf4j/src/main/kotlin/com/milkcocoa/info/colotok/core/logger/ColotokLoggerFactory4J.kt`
  - `colotok-slf4j2/src/main/kotlin/com/milkcocoa/info/colotok/core/logger/ColotokLoggerFactory4J2.kt`
  - `colotok/src/commonMain/kotlin/com/milkcocoa/info/colotok/util/ThreadSafeMap.kt`
  - `colotok/src/jvmMain/kotlin/com/milkcocoa/info/colotok/util/ThreadSafeMap.jvm.kt`

## Goal
Make SLF4J 1.x and 2.x logger factory caches safe when multiple JVM threads request the same or different logger names concurrently.

## Non-Goals
- Do not change SLF4J binding discovery.
- Do not change logger names, attributes, or provider selection.
- Do not redesign `ColotokLoggerContext` cache behavior in this task.
- Do not introduce non-JVM abstractions for JVM-only SLF4J modules.

## Current State
- `ColotokLoggerFactory4J` uses `mutableMapOf<String, ColotokLogger4J>()` with `computeIfAbsent`.
- `ColotokLoggerFactory4J2` uses `mutableMapOf<String, ColotokLogger4J2>()` with `computeIfAbsent`.
- Plain mutable maps can race under concurrent `getLogger(name)` calls.
- Core has a multiplatform `createThreadSafeMap()` helper, but SLF4J modules are JVM-only and can use `ConcurrentHashMap` directly.

## Boundary Decision
- Owner boundary: JVM integration modules `colotok-slf4j` and `colotok-slf4j2`.
- Why this belongs there: SLF4J factories are the concurrency boundary used by external JVM applications.
- Cross-boundary impact: no common runtime behavior should change.

## Task Breakdown
### Task 1: Add concurrent factory tests
- Objective: reproduce and lock down concurrent cache behavior.
- Affected modules/files:
  - `colotok-slf4j/src/test/kotlin/com/milkcocoa/info/colotok/core/logger/ColotokLogger4JTest.kt` or a new factory test file
  - `colotok-slf4j2/src/test/kotlin/com/milkcocoa/info/colotok/core/logger/ColotokLogger4J2Test.kt` or a new factory test file
- Expected behavior:
  - Concurrent requests for the same name return the same logger instance.
  - Concurrent requests for many names complete without throwing.
  - Test code shuts down executors deterministically.
- Validation:
  - `./gradlew :colotok-slf4j:test --tests '*Factory*'`
  - `./gradlew :colotok-slf4j2:test --tests '*Factory*'`

### Task 2: Replace mutable factory caches
- Objective: use JVM-safe cache storage.
- Affected modules/files:
  - `ColotokLoggerFactory4J.kt`
  - `ColotokLoggerFactory4J2.kt`
- Expected behavior:
  - Use `java.util.concurrent.ConcurrentHashMap`.
  - Preserve `computeIfAbsent` semantics.
  - Preserve factory public API and returned logger types.
- Validation:
  - `./gradlew :colotok-slf4j:test :colotok-slf4j2:test`

### Task 3: Verify interaction with default context
- Objective: ensure the factory cache change does not alter existing logger behavior.
- Affected modules/files:
  - Existing SLF4J tests
- Expected behavior:
  - Existing default attributes and logger name behavior still pass.
- Validation:
  - `./gradlew :colotok-slf4j:test :colotok-slf4j2:test`

## Compatibility Impact
- Behavior should be source- and binary-compatible.
- Users may observe more stable instance identity under concurrency.

## Data and Persistence Impact
- None.

## Validation Plan
- `./gradlew :colotok-slf4j:test --tests '*Factory*'`
- `./gradlew :colotok-slf4j2:test --tests '*Factory*'`
- `./gradlew :colotok-slf4j:test :colotok-slf4j2:test`

## Risks and Open Questions
- Concurrent tests can be flaky if they rely on timing. Use barriers or latches instead of sleeps.
- This does not fix any potential race inside `ColotokLoggerContext`; that is outside the SLF4J factory cache scope unless tests expose it directly.

## Implementation Order
1. Add focused concurrent factory tests.
2. Change both factory caches to `ConcurrentHashMap`.
3. Run focused factory tests.
4. Run both SLF4J module test suites.
