# Provider

colotok has builtin provider.
Provider is used for output log.

## ConsoleProvider
ConsoleProvider write the log into console

```Kotlin
....
.addProvider(ConsoleProvider{
    level = LogLevel.DEBUG
    formatter = DetailTextFormatter
    colorize = true
    
    traceLevelColor = AnsiColor.WHITE
    debugLevelColor = AnsiColor.BLUE
    infoLevelColor = AnsiColor.GEEN
    warnLevelColor = AnsiColor.YELLOW
    errorLevelColor = AnsiColor.RED
})
```

ConsoleProvider can colorize with ANSI-Color


## FileProvider
FileProvider write the log into file.  

> FileProvider depends on Okio, so you need additional dependency to use it.

If `enableBuffer` is true, FileProvider will buffering until buffer is full,  
when buffer becomes full, write content into file and cleared the buffer.  


```Kotlin
val fileProvider: FileProvider

....
.addProvider(FileProvider(File("./test.log").toOkioPath()){
    level = LogLevel.TRACE
    formatter = DetailTextFormatter
    enableBuffer = true
    bufferSize = 2048
    rotation = SizeBaseRotation(size = 8192)
}.apply { fileProvider = this }
```

### LogRotation
FileProvider can log-rotation.


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
    enableBuffer = true
    bufferSize = 64.KiB()
    formatter = SimpleStructureFormatter
    outputStreamBuilder = { blackholeSink() }
}.apply { streamProvider = this })
```


> if you want to flash buffer immediately, you can call `flush()` on FileProvider and StreamProvider. when end of application, buffered contents will discard.
{style="note"}