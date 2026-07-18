# Provider

colotok has builtin provider.
Provider is used for output log.

## Common Provider Configuration

All providers support the following configuration options:

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
    infoLevelColor = AnsiColor.GEEN
    warnLevelColor = AnsiColor.YELLOW
    errorLevelColor = AnsiColor.RED
}))
```

On Android, `ConsoleProvider()` does not write to Logcat with its default configuration. Provide the debug-mode decision explicitly when debug output is wanted:

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

ConsoleProvider can colorize with ANSI-Color


## FileProvider
FileProvider writes the log into a file asynchronously.

> FileProvider depends on Okio, so you need the additional dependency to use it.

```Kotlin
.addProvider(FileProvider(File("./test.log").toOkioPath()){
    level = LogLevel.TRACE
    formatter = DetailTextFormatter
    // use size base rotation
    rotation = SizeBaseRotation(size = 8192)
    
    // Metrics configuration
    enableInternalMetricsLogging = true
    metricsSpec = MetricsCollectorSpec.Inherit
})
```

FileProvider can rotate log files using `rotation`.


#### SizeBaseRotation
this rotation will rotate when log size over passed [size].

```Kotlin
rotation = SizeBaseRotation(size = 8192)
```


#### DateBaseRotation
this rotation will rotate when log file spent over [period]
```Kotlin
rotation = DateBaseRotation(period = 7.days)
```


## StreamProvider
StreamProvider write the log into stream

> StreamProvider depends on Okio, so you need additional dependency to use it.
> 
```Kotlin
val streamProvider: StreamProvider

....
.addProvider(StreamProvider{
    formatter = SimpleStructureFormatter
    outputStreamBuilder = { blackholeSink() }
}.apply { streamProvider = this })
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
