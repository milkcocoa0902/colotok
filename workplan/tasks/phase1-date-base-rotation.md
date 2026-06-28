# Phase1: DateBaseRotation

## Source Context
- Primary: `docs/QUALITY_IMPROVEMENT_PLAN.md`
- Supporting: `README.md`, `docs/Writerside/topics/02-01-provider-configuration.md`
- Current code:
  - `colotok/src/commonMain/kotlin/com/milkcocoa/info/colotok/core/provider/rotation/DateBaseRotation.kt`
  - `colotok/src/commonMain/kotlin/com/milkcocoa/info/colotok/core/provider/rotation/SizeBaseRotation.kt`
  - `colotok/src/commonMain/kotlin/com/milkcocoa/info/colotok/core/provider/builtin/file/FileProvider.kt`
  - `colotok/src/jvmTest/kotlin/com/milkcocoa/info/colotok/core/provider/builtin/FileProviderTest.kt`

## Goal
Fix duration-based file rotation so it rotates when a log file's age is greater than or equal to the configured period, handles missing metadata safely, and does not print debug output from library internals.

## Non-Goals
- Do not redesign the `Rotation` interface.
- Do not add retention limits, compression, cleanup, or date-pattern file naming.
- Do not change `SizeBaseRotation` except for shared test helper extraction if useful.
- Do not change `FileProvider` buffering or lifecycle semantics.

## Current State
- `DateBaseRotation.isRotateNeeded()` computes `createdAt + period > now`, which rotates fresh files and skips expired files.
- `createdAtMillis` is force-unwrapped, so missing metadata can throw.
- `doRotate()` calls `println` while computing the next rotation index.
- Existing file provider tests cover `SizeBaseRotation`, but no test covers `DateBaseRotation`.

## Boundary Decision
- Owner boundary: core file rotation behavior in `colotok`.
- Why this belongs there: `DateBaseRotation` is a public built-in rotation strategy advertised in README and Writerside docs.
- Cross-boundary impact: only `FileProvider` users with `DateBaseRotation` should observe behavior changes.

## Task Breakdown
### Task 1: Add DateBaseRotation regression tests
- Objective: lock down the correct age threshold before changing implementation.
- Affected modules/files:
  - New focused test under `colotok/src/jvmTest/kotlin/com/milkcocoa/info/colotok/core/provider/rotation/`
  - Optional helper reuse from `FileProviderTest`
- Expected behavior:
  - A file younger than `period` does not rotate.
  - A file exactly at `period` rotates.
  - A file older than `period` rotates.
  - Missing creation metadata does not throw; it either falls back to another stable timestamp or returns `false`.
  - `doRotate()` produces no debug stdout.
- Validation:
  - `./gradlew :colotok:jvmTest --tests '*DateBaseRotation*'`
- Notes:
  - Tests should derive timestamps from actual file metadata after file creation, then mock `Clock.System.now()` relative to that value.
  - If missing metadata cannot be produced using the system filesystem, keep that case small and implementation-focused rather than adding broad filesystem abstraction.

### Task 2: Correct rotation predicate and metadata handling
- Objective: make the implementation match the documented duration-based behavior.
- Affected modules/files:
  - `DateBaseRotation.kt`
- Expected behavior:
  - Rotation is needed when `fileTimestamp + period <= Clock.System.now()`.
  - `createdAtMillis ?: lastModifiedAtMillis` can be used as a safe timestamp source.
  - If no usable timestamp exists, return `false` rather than throwing.
- Validation:
  - `./gradlew :colotok:jvmTest --tests '*DateBaseRotation*'`

### Task 3: Remove debug output from rotation
- Objective: keep library internals quiet during normal rotation.
- Affected modules/files:
  - `DateBaseRotation.kt`
- Expected behavior:
  - Rotation index selection matches `SizeBaseRotation`.
  - No `println` calls remain in `DateBaseRotation`.
- Validation:
  - `./gradlew :colotok:jvmTest`

### Task 4: Confirm docs remain accurate
- Objective: ensure docs describe threshold behavior without implying unsupported retention features.
- Affected modules/files:
  - `README.md`
  - `docs/Writerside/topics/02-01-provider-configuration.md`
- Expected behavior:
  - Existing DateBaseRotation examples remain valid.
  - Add a short note only if threshold wording needs clarification.
- Validation:
  - Documentation review.

## Compatibility Impact
- This fixes incorrect behavior. Users may see fewer immediate rotations and correct rotations after the configured age.
- Missing metadata no longer throws from `isRotateNeeded()` when called directly.

## Data and Persistence Impact
- Existing rotated files remain in the same `application.log.N` style.
- Rotation timing changes can alter the number of rotated files produced after upgrade.

## Validation Plan
- `./gradlew :colotok:jvmTest --tests '*DateBaseRotation*'`
- `./gradlew :colotok:jvmTest --tests 'com.milkcocoa.info.colotok.core.provider.builtin.FileProviderTest'`
- `./gradlew :colotok:jvmTest`

## Risks and Open Questions
- Filesystem creation time behavior differs by platform. JVM tests should avoid assuming a hardcoded creation timestamp.
- If `createdAtMillis` is unavailable but `lastModifiedAtMillis` is present, fallback behavior is preferable to throwing.

## Implementation Order
1. Add focused failing DateBaseRotation tests.
2. Fix `isRotateNeeded()` predicate and metadata fallback.
3. Remove debug `println` calls from `doRotate()`.
4. Run focused tests and then `:colotok:jvmTest`.
5. Update docs only if wording is currently misleading.
