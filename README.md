# CLK
ClK; Cocoa LogTool for Kotlin

# Feature
- Print log with color
- Formatter
- Print log into Console, File
- Log Rotation
- Customize output location


# Usage
``` kotlin
val logger = LoggerFactory()
    .addProvider(ConsoleProvider())
    .addProvider(FileProvider("./test.log"))
    .getLogger()

logger.trace("TRACE LEVEL LOG")
logger.debug("DEBUG LEVEL LOG")
logger.info("INFO LEVEL LOG")
logger.warn("WARN LEVEL LOG")
logger.error("ERROR LEVEL LOG")
```

more config
``` 
val fileProvider: FileProvider
val logger = LoggerFactory()
    .addProvider(ConsoleProvider{
        // show above info level in console
        logLevel = LogLevel.INFO
    })
    .addProvider(FileProvider("./test.log"){
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

// you may need to flash, if log cache is enabled for `FileProvider`
fileProvider.flush()
```


## Formatter
CLK has builtin formatter.
1. PlainFormatter
2. SimpleFormatter
3. DetailFormatter

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

