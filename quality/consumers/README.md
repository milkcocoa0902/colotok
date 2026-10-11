# Published consumer smoke tests

`run.sh` publishes Colotok into a temporary Maven repository and builds independent consumers
against those publications. It does not publish to Maven Central, Nexus, or GitHub Releases.

The KMP consumers use root coordinates in `commonMain`. Only Gradle module metadata is accepted
for Colotok, and its group is resolved exclusively from the isolated repository. Missing
publications cannot fall back to a previously released artifact. The consumer build is not
included in the producer build and does not use project dependencies or dependency substitution.

Each fixture declares just one Colotok dependency:

| Fixture | Direct dependency | Checks |
| --- | --- | --- |
| core | `colotok` | Basic logger, custom provider, delivery and shutdown |
| coroutines | `colotok-coroutines` | Transitive core API and suspending log calls |
| loki | `colotok-loki` | Transitive core/coroutines API, Loki configuration, suspending log calls |

Loki configuration is used without creating an HTTP client or sending network requests.
The existing SLF4J 1/2 Java consumers also run in Linux mode.
All KMP fixture build settings are defined once in `kmp/build.gradle.kts`; the three fixture
directories contain only their independent smoke-test sources.

## Run locally

Use JDK 17 or newer, an Android SDK with platform 36, and Node.js 24 on PATH for Linux mode.
Configure `ANDROID_HOME` for both the producer and the independent consumer. The producer's
`local.properties` is not inherited by the consumer. Gradle downloads other required dependencies.
The producer also uses its existing Java 11 toolchain configuration.

```bash
export ANDROID_HOME=/path/to/android-sdk
quality/consumers/run.sh
```

This runs common metadata compilation, Android compilation, JVM tests and Node tests for all
three KMP fixtures. Each fixture resolves the published artifacts in an independent build.

On an Apple Silicon Mac, run the Apple publications and independent consumer instead:

```bash
quality/consumers/run.sh apple
```

Apple mode compiles common metadata and iOS Arm64/Simulator Arm64 code, then runs macOS Arm64
tests. Xcode and the Android SDK are required by the producer. Other Apple architectures are
not part of the declared support range.
The Android-KMP plugin also creates an Android target in Apple mode, so its configuration and
publication are retained for common metadata resolution; the Apple smoke task runs native checks.

## Compiler compatibility

The default consumer compiler is Kotlin 2.4.21. To test multiple consumer compilers against the
same artifacts, use:

```bash
COLOTOK_CONSUMER_KOTLIN_VERSIONS=2.4.21,2.3.21 quality/consumers/run.sh
```

The producer compiler remains the version in `gradle/libs.versions.toml`. The runner publishes
once, then cleans and builds the consumer separately for each compiler. Any selected consumer
failure exits the command with a nonzero status.

To retain publications for diagnostics, set `COLOTOK_CONSUMER_REPOSITORY` to a dedicated empty
directory. The default repository is temporary and removed on exit. Do not reuse a directory
containing older publications when assessing missing-publication failures.

## Tested compiler and platform combinations

The library uses Kotlin/KGP 2.4.21. The following combinations have been tested with the smoke
fixtures and their transitive dependencies:

| Consumer Kotlin/KGP | Common metadata | JVM runtime | Android compile | Node runtime | Apple |
| --- | --- | --- | --- | --- | --- |
| 2.4.21 | Passed | Passed | Passed | Passed | iOS Arm64/Simulator Arm64 compile and macOS Arm64 runtime passed |
| 2.3.21 | Passed | Passed | Passed | Incompatible KLIB ABI | Not tested |

Kotlin 2.3.21 accepts KLIB ABI versions up to 2.3.0. Colotok and the Kotlin standard library
built with Kotlin 2.4.21 use KLIB ABI 2.4.0. Use Kotlin 2.4.21 for JS/Node consumers.
Compiler-specific npm locks are stored in separate directories.

Apple checks compile iOS Arm64 and iOS Simulator Arm64 code and execute macOS Arm64 tests.
iOS device and simulator runtime tests are outside this suite. The smoke fixtures exercise
logger creation, custom Providers, log delivery, and shutdown; they cover a subset of the public API.

## Continuous integration

Quality CI runs Linux and Apple consumer jobs on pushes to `develop`, PRs targeting
`main` or `develop`, manual dispatch, and calls from the documentation/release workflows.

The [2026-10-11 Quality run](https://github.com/milkcocoa0902/colotok/actions/runs/38158279085)
on commit `59fb635fa8cf261a6a5aaec5fdf746ce08590480` passed both consumer jobs, module tests,
Apple compilation/macOS runtime tests, and Writerside checks. The API compatibility job failed
because the Android API dump differed from the generated API. Run the workflow after updating
API dumps or changing the release version.
