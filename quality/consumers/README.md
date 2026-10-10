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
three KMP fixtures. A successful producer build alone does not count as consumer validation.

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
failure fails the command; there is no metadata-version bypass or ignored compatibility failure.

To retain publications for diagnostics, set `COLOTOK_CONSUMER_REPOSITORY` to a dedicated empty
directory. The default repository is temporary and removed on exit. Do not reuse a directory
containing older publications when assessing missing-publication failures.

## Validation record

For the 1.0.0 toolchain baseline, the producer is Kotlin/KGP 2.4.21. Local validation on
2026-10-10 used Gradle 9.8.0, AGP 9.0.1, Android platform 36, Node 24.16.0 and Colotok 0.5.0
publications built from this working tree.

| Consumer Kotlin/KGP | Common metadata | JVM runtime | Android compile | Node runtime | Apple |
| --- | --- | --- | --- | --- | --- |
| 2.4.21 | Passed | Passed | Passed | Passed | Awaiting macOS CI |
| 2.3.21 | Passed | Passed | Passed | Incompatible KLIB ABI | Not verified |

Results cover the smoke fixtures and resolved transitive dependencies, not every public API.
They do not establish a minimum consumer Kotlin version or guarantee all 2.3.x versions.
The Kotlin 2.3.21 JS compiler rejected `colotok-js` built with Kotlin 2.4.21: the artifact has
KLIB ABI 2.4.0, while that compiler accepts ABI versions up to 2.3.0. The resolved Kotlin
2.4.21 JS standard library also has the newer ABI. Use the tested 2.4.21 consumer for JS/Node;
successful common metadata resolution or JVM compilation does not establish JS compatibility.
Compiler-specific npm locks are kept in separate directories to avoid mixing JS test tooling.

Quality CI runs the Linux and Apple consumer jobs. Its current automatic entry points are
pushes/PRs for `main`, and the release workflow's `workflow_call`; a PR targeting `develop`
does not automatically execute this workflow.

The local end-to-end `run.sh` invocation (temporary publication repository, both SLF4J consumers,
and all three Kotlin 2.4.21 KMP consumers) passed. Existing module JVM tests and
`apiCheck checkKotlinAbi` passed as well. Apple Gradle configuration was checked on Linux,
but that does not establish native compilation or runtime success.
An empty isolated repository was also tested: common metadata compilation failed to resolve
`io.github.milkcocoa0902:colotok:0.5.0`, as intended, even after a successful consumer build.
