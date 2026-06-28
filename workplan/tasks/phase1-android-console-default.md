# Phase1: Android Console Default Output Policy

## Source Context
- Primary: `docs/QUALITY_IMPROVEMENT_PLAN.md`
- Supporting: `README.md`, `docs/Writerside/topics/02-configuration.md`, `docs/Writerside/topics/02-01-provider-configuration.md`
- Current code:
  - `colotok/src/androidMain/kotlin/com/milkcocoa/info/colotok/core/provider/builtin/console/ConsoleProvider.android.kt`
  - `colotok/src/androidMain/kotlin/com/milkcocoa/info/colotok/core/provider/builtin/console/ConsoleProviderConfig.andriod.kt`
  - `colotok/src/commonMain/kotlin/com/milkcocoa/info/colotok/core/provider/builtin/console/ConsoleProviderConfig.kt`
  - `colotok/src/commonMain/kotlin/com/milkcocoa/info/colotok/core/logger/ColotokLoggerContext.kt`

## Goal
Make Android console output behavior explicit: `ConsoleProvider()` should write to Logcat by default on Android unless users explicitly disable it.

Safe automatic debug-build detection is desirable, but it is not a Phase1 dependency. Android framework `Log.isLoggable()` controls tag/level loggability rather than build type, and AGP 8.0 no longer generates `BuildConfig` by default. Phase1 should therefore avoid relying on `BuildConfig.DEBUG` or Logcat behavior as an automatic release/debug gate.

## Non-Goals
- Do not rely on `BuildConfig.DEBUG`.
- Do not treat `Log.isLoggable()` as release/debug detection.
- Do not require Android `Context` in the common `ConsoleProviderConfig`.
- Do not redesign the default logger context in Phase1 unless a policy decision requires it.
- Do not add a full Android test infrastructure overhaul unless needed to verify a code change.

## Current State
- `ColotokLoggerContext.DEFAULT` installs `ConsoleProvider(ConsoleProviderConfig())`.
- On Android, `ConsoleProviderConfig` defaults `isEnabledForRelease = false` and `detectDebugModeFn = { false }`.
- `ConsoleProvider.android` returns without logging when release output is disabled and debug detection is false.
- As a result, Android default logger output is silent unless users set `isEnabledForRelease = true` or provide `detectDebugModeFn = { true }`.
- README and Writerside examples present `ConsoleProvider()` as normal console output without an Android-specific caveat.
- The filename is currently `ConsoleProviderConfig.andriod.kt`; renaming is cosmetic and should only be done if it does not create noisy churn.

## Boundary Decision
- Owner boundary: Android actual implementation and public documentation.
- Why this belongs there: output behavior differs by platform and can surprise Android users.
- Cross-boundary impact: common docs and examples may need Android notes; common API should remain stable.

## Task Breakdown
### Task 1: Invert the Android default output policy
- Objective: make output enabled by default while preserving a clear explicit opt-out.
- Affected modules/files:
  - `ConsoleProviderConfig.andriod.kt`
  - `ConsoleProvider.android.kt`
  - README and Writerside docs
- Expected behavior:
  - `ConsoleProvider()` writes to Logcat by default on Android.
  - Users can explicitly disable Android console output.
  - Existing `isEnabledForRelease` and `detectDebugModeFn` behavior is either preserved as a compatibility bridge or migrated with clear documentation.
  - Safe automatic debug detection is deferred unless a reliable mechanism is found during implementation.
- Validation:
  - `./gradlew :colotok:compileReleaseKotlinAndroid`
- Notes:
  - The least noisy implementation should avoid a broad common API change.
  - If a new Android-only flag is added, prefer an explicit name such as `isOutputEnabled` over overloading release/debug wording.

### Task 2: Document Android-specific behavior
- Objective: make the default-output and explicit-opt-out behavior clear.
- Affected modules/files:
  - `README.md`
  - `docs/Writerside/topics/02-configuration.md`
  - `docs/Writerside/topics/02-01-provider-configuration.md`
- Expected behavior:
  - Explain that Android `ConsoleProvider()` outputs by default in Phase1.
  - Explain how to disable Android console output.
  - If legacy `isEnabledForRelease` and `detectDebugModeFn` remain, explain their compatibility behavior.
  - Mention that automatic debug-build detection is not inferred from `BuildConfig.DEBUG`.
  - Avoid claiming time-based flush or other unrelated behavior.
- Validation:
  - Documentation review.

### Task 3: Add focused Android compile or test validation
- Objective: ensure Android actual sources still compile after any doc or code changes.
- Affected modules/files:
  - Android source set if code changes are made.
- Expected behavior:
  - `compileReleaseKotlinAndroid` passes.
  - If an Android unit test source set is added, it verifies default-enabled behavior and explicit-disabled behavior.
- Validation:
  - `./gradlew :colotok:compileReleaseKotlinAndroid`

### Task 4: Add tests or compile checks around the selected policy
- Objective: verify that the Android actual source expresses the selected default.
- Affected modules/files:
  - `ConsoleProviderConfig.andriod.kt`
  - `ConsoleProvider.android.kt`
- Expected behavior:
  - Default config enables output.
  - Explicit opt-out disables output.
  - Any new API is additive where practical.
  - Existing explicit `detectDebugModeFn` and `isEnabledForRelease` behavior is not silently broken without documentation.
- Validation:
  - `./gradlew :colotok:compileReleaseKotlinAndroid`
  - `./gradlew :colotok:jvmTest`

## Compatibility Impact
- Android output becomes enabled by default. This is intentional and should be called out in release notes.
- Existing users who relied on the previous silent default need an explicit opt-out.

## Data and Persistence Impact
- None.

## Validation Plan
- `./gradlew :colotok:compileReleaseKotlinAndroid`
- `./gradlew :colotok:jvmTest`
- If Android test infra is added: run the corresponding Android unit test task confirmed from Gradle.

## Risks and Open Questions
- Resolved: `ConsoleProvider()` should log by default on Android unless explicitly disabled.
- Deferred: safe automatic debug-build detection remains a future enhancement.
- Open: exact Android-only opt-out API name if the existing `isEnabledForRelease` / `detectDebugModeFn` pair is too confusing for the inverted default.
- Risk: automatic debug detection without app context can be wrong; avoid implicit heuristics.

## Implementation Order
1. Update Android config/provider behavior so output is enabled by default and explicit opt-out is possible.
2. Add the smallest practical validation for default-enabled and explicit-disabled behavior.
3. Update README and Writerside provider docs with Android-specific console behavior.
4. Run Android compile validation.
5. Note automatic debug detection as future work if no reliable mechanism is selected.
