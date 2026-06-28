# Repository Guidelines

## Project Structure & Module Organization

Colotok is a Kotlin Multiplatform logging library. Core APIs live in `colotok/src/commonMain/kotlin`, with platform-specific implementations under `jvmMain`, `androidMain`, `jsMain`, and `nativeMain`. Tests follow the same source-set layout, for example `colotok/src/jvmTest/kotlin` and `colotok-loki/src/commonTest/kotlin`.

Integration modules are split by capability: `colotok-coroutines`, `colotok-slf4j`, `colotok-slf4j2`, `colotok-loki`, and `colotok-cloudwatch`. `sample/` contains local usage examples. User-facing documentation is in `docs/Writerside/topics`, and planning notes are in `workplan/tasks`.

## Build, Test, and Development Commands

- `./gradlew build` builds all modules and runs configured checks.
- `./gradlew test` runs JVM tests across applicable modules.
- `./gradlew :colotok:jvmTest` runs core JVM tests only.
- `./gradlew :colotok-loki:allTests` runs multiplatform tests for the Loki module.
- `./gradlew ktlintCheck` checks Kotlin style using ktlint.
- `./gradlew ktlintFormat` formats Kotlin and Gradle Kotlin DSL files.

Use module-scoped tasks while iterating, then run the broader task before opening a PR.

## Coding Style & Naming Conventions

Write Kotlin with 4-space indentation and follow the existing package root `com.milkcocoa.info.colotok`. Public types use PascalCase, functions and properties use camelCase, and tests should end with `Test`. Keep common code in `commonMain`; add `expect`/`actual` or platform source-set code only when behavior truly differs by platform.

Prefer small provider, formatter, and logger components that match the existing architecture. Avoid changing published artifact names or package paths without a migration plan.

## Testing Guidelines

The project uses Kotlin test, JUnit Jupiter for JVM tests, MockK where mocking is needed, and kotlinx-coroutines-test for coroutine behavior. Add tests beside the source set affected by the change. For shared behavior, prefer `commonTest`; for JVM-specific behavior, use `jvmTest`.

Cover formatter output, provider lifecycle, MDC propagation, rotation behavior, and plugin integration when those areas change.

## Commit & Pull Request Guidelines

Recent history uses short, descriptive commits, sometimes with emoji and scope-like tags such as `🐛 [fix] Correct DateBaseRotation behavior`. Keep commits focused and explain the behavior changed.

PRs should include a concise summary, linked issue or motivation, affected modules, and test commands run. Include screenshots only for documentation or visual changes.

## Security & Configuration Tips

Do not commit credentials from `local.properties`, publishing settings, CloudWatch credentials, or local log files such as `application.log` and `test.log`. Keep publishing and Nexus credentials local.
