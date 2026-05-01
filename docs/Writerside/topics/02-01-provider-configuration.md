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

> Note: すべての Provider は Channel を使用して非同期に動作します。バッファに残っているログを失わないよう、アプリケーション終了前には `flush()` または `close()` を呼び出して、すべての Provider が処理を完了するのを待機してください。
{style="note"}