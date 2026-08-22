# Provider

Colotok has built-in providers. A provider receives accepted log records and writes them to a
destination.

## Common Provider Configuration

Every provider config exposes the following options. The defaults below apply to the core built-in
providers (`ConsoleProvider`, `FileProvider`, and `StreamProvider`); integration providers may
choose different level and formatter defaults, as listed in the [official plugin guide](Official-Plugin.md).

| Property | Description | Default |
| :--- | :--- | :--- |
| `level` | Minimum log level for this provider | `LogLevel.DEBUG` |
| `formatter` | Formatter to use | `DetailTextFormatter` |
| `metricsSpec` | How to collect metrics (`Inherit`, `Explicit`, `NoOp`) | `MetricsCollectorSpec.Inherit` |
| `enableInternalMetricsLogging` | Whether to log metrics as internal records (`LogRecord.Metrics`) | `false` |

## ConsoleProvider
ConsoleProvider write the log into console

```Kotlin
....
.addProvider(ConsoleProvider(ConsoleProviderConfig().apply {
    level = LogLevel.DEBUG
    formatter = DetailTextFormatter
    colorize = true

    traceLevelColor = AnsiColor.WHITE
    debugLevelColor = AnsiColor.BLUE
    infoLevelColor = AnsiColor.GREEN
    warnLevelColor = AnsiColor.YELLOW
    errorLevelColor = AnsiColor.RED
}))
```

On Android, `ConsoleProvider()` does not write to Logcat with its default configuration. From your
Android source set, provide the debug-mode decision explicitly when debug output is wanted:

```Kotlin
.addProvider(ConsoleProvider {
    detectDebugModeFn = { BuildConfig.DEBUG }
})
```

| `isOutputEnabled` | `isEnabledForRelease` | `detectDebugModeFn()` | Output |
| :---: | :---: | :---: | :---: |
| `false` | any | any | disabled |
| `true` | `true` | any | enabled |
| `true` | `false` | `true` | enabled |
| `true` | `false` | `false` | disabled (default) |

Colotok does not infer debug builds from `BuildConfig.DEBUG`.

On JVM and Native targets, `ConsoleProvider` can colorize output with ANSI colors. The color
properties are platform-specific; Android's `ConsoleProvider` writes to Logcat and does not expose
these color properties.


## FileProvider
FileProvider writes the log into a file asynchronously.

`FileProvider` accepts an Okio `Path`. Okio is exposed transitively by the `colotok` artifact; add a
direct Okio dependency only when your own code uses Okio APIs.

```Kotlin
import okio.Path.Companion.toPath

.addProvider(FileProvider("test.log".toPath()) {
    level = LogLevel.TRACE
    formatter = DetailTextFormatter
    // use size base rotation
    rotation = SizeBaseRotation(size = 8192L)
    
    // Metrics configuration
    enableInternalMetricsLogging = true
    metricsSpec = MetricsCollectorSpec.Inherit
})
```

FileProvider can rotate log files using `rotation`. A rotated file is renamed to
`application.log.1`, `application.log.2`, and so on. When the path passed to `FileProvider` is an
existing directory, the active file is `application.log` inside that directory.


#### SizeBaseRotation
This rotation runs after a write when the file size is greater than [size] bytes.

```Kotlin
rotation = SizeBaseRotation(size = 8192L)
```


#### DateBaseRotation
This rotation runs after a write when the file's creation time (or, when unavailable, last modified
time) is at least [period] old.
```Kotlin
import kotlin.time.Duration.Companion.days

rotation = DateBaseRotation(period = 7.days)
```


## StreamProvider
StreamProvider write the log into stream

`StreamProvider` accepts an Okio `Sink` factory. Okio is exposed transitively by the `colotok`
artifact; add a direct Okio dependency only when your own code uses Okio APIs.

```Kotlin
import okio.blackholeSink

val logger = ColotokLoggerContext()
    .addProvider(StreamProvider {
        formatter = SimpleStructureFormatter
        outputStreamBuilder = { blackholeSink() }
    })
    .getLogger()
```

> Note: Provider は Channel を使用して非同期に動作します。`flush()` は Provider が開いている間だけ利用でき、呼び出し以前に受理されたログの処理を待ちます。`close()` は graceful close を開始するだけで待機しません。終了を待つには `join()`、Context 全体では `shutdown()` を使用してください。graceful / force を問わず close 開始後の `flush()` は `ProviderClosedException` になります。
{style="note"}

## AsyncProvider buffering

`Provider.write()` はnon-blockingのenqueue試行です。default `SUSPEND` policyでもcapacityを
待たず、channelが満杯なら既存のFIFO prefixを残してnewest recordをrejectします。
`AsyncProvider.writeAsync()`はcapacityが空くまでsuspendします。通常の`Provider`に対する
coroutine `*Async` APIは、同じnon-blocking `write()` pathへdelegateします。

`bufferSize` は送信を試みる閾値で、有効範囲は `1..4096` です。送信失敗時は `min(bufferSize * 4, 4096)` 件まで既存レコードを保持します。上限到達後は、古い未送信レコードを残すため新しいレコードを破棄します。

送信先がバッチの一部だけを受理してから失敗した場合、保持したバッチの再送で重複が発生し得ます。リモート Provider は at-least-once delivery として扱い、受信側を重複許容にしてください。

自動 publish の失敗は記録され、後続の publish で再試行できます。明示的な `flush()` の失敗は呼び出し元へ伝播して Provider を failed 状態にし、その後の `join()` も同じ原因を通知します。

ログ呼び出し時に attributes と MDC は snapshot されます。その後に元の map を変更しても、非同期 formatter の出力は変化しません。
