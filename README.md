# COLOTOK
COLOTOK; Cocoa LogTool for Kotlin


# Feature
- Print log with color
- Formatter
- Print log into Console, File, or more...(Custom)
  - builtin, Console, File
- Log Rotation
  - builtin, SizeBaseRotation or DateBaseRotation(; DurationBase)
- Customize output location
- Structure Logging


# Usage
## Configuration
configure colotok with code.  
see below.

``` kotlin
val logger = LoggerFactory()
    .addProvider(ConsoleProvider())
    .addProvider(FileProvider(Path.of("./test.log")))
    .getLogger()

```

more details config
``` 
val fileProvider: FileProvider
val logger = LoggerFactory()
    .addProvider(ConsoleProvider{
        // show above info level in console
        logLevel = LogLevel.INFO
    })
    .addProvider(FileProvider(Path.of("./test.log")){
        // write above trace level for file
        logLevel = LogLevel.TRACE
        
        // memory buffering to save i/o
        enableBuffer = true
        
        // memory buffer size, if buffer excced this, append to file
        bufferSize = 2048
        
        // use size base rotation
        rotation = SizeBaseRotation(size = 4096)
    }.apply {
        fileProvider = this
    })
    .getLogger()

logger.trace("TRACE LEVEL LOG")
logger.debug("DEBUG LEVEL LOG")
logger.info("INFO LEVEL LOG")
logger.warn("WARN LEVEL LOG")
logger.error("ERROR LEVEL LOG")

```

## Print
now, you can print log into your space.

``` kotlin
logger.trace("TRACE LEVEL LOG")
logger.debug("DEBUG LEVEL LOG")
logger.info("INFO LEVEL LOG")
logger.warn("WARN LEVEL LOG")
logger.error("ERROR LEVEL LOG")

logger.atInfo {
    print("in this block")
    print("all of logs are printed out with INFO level")
}

// you may need to flash, if log cache is enabled for `FileProvider`
fileProvider.flush()
```

## Formatter(Text)
colotok has builtin text formatter.
1. PlainTextFormatter
2. SimpleTextFormatter
3. DetailTextFormatter

### 1. PlainFormatter
this formatter shows as below style's log

```
message what happen
```

### 2. SimpleFormatter
this formatter shows as below style's log

```
2023/10/09 11:33:55 [INFO] - message what happen
```

### 3. DetailFormatter
this formatter shows as below style's log

```
2023/10/09 11:33:55 (thread name) [INFO] - message what happen
```

## Formatter(Structure)
colotok has builtin structured formatter.
1. SimpleStructureFormatter
2. DetailStructureFormatter

if you has a class
```kotlin
@Serializable
class LogDetail(val scope: String, val message: String): LogStructure

@Serializable
class Log(val name: String, val logDetail: LogDetail): LogStructure
```

### 1. SimpleStructureFormatter
this formatter shows bellow style's log

```
{"name":"illegal state","logDetail.scope":"args","logDetail.message":"argument must be greater than zero","level":"INFO","date":"2023-12-26 23:12:56"}
```
### 2. DetailStructureFormatter
this formatter shows bellow style's log

```
{"name":"illegal state","logDetail.scope":"args","logDetail.message":"argument must be greater than zero","level":"INFO","thread":"main","date":"2023-12-26 23:12:10"}
```



## Provider
CLK has builtin provider. Provider is used for output log.

1. ConsoleProvider
2. FileProvider

### 1. ConsoleProvider
this provider outputs log into console with ansi-color

### 2. FileProvider
this provider output log into file without ansi-color.

### 3. Customize
you can also output to remote or sql or others by create own provider.  
example

``` kotlin
class NetworkProvider : Provider {
    override fun write(name: String, str: String, level: LogLevel) {
      // POST log message to web api 
       api.execure(formatter.format(str, level))
    }
}
```

## LogLevel
1. TRACE (all log)
2. DEBUG (ignore TRACE)
3. INFO (ignore DEBUG and TRACE)
4. WARN (only WARN or ERROR)
5. ERROR (only one)

## Notice 
if you set structured formatter to provider and print text, it will ignored.  
also you set text formatter and passed structured log will ignored