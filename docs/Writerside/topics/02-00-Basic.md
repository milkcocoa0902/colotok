# Basic

Colotok output the log where you specified by the provider and formatted with you passed.  

## Get Logger

```Kotlin
val logger = LoggerFactory()
    .addProvider(ConsoleProvider())
    .getLogger()
```

you can get the logger instance by `LoggerFactory.getLogger()`  

`ConsoleProvider` is a builtin provider which used for print the log into console

> if none of `addProvider()` is called, the logger will not print the log anywhere
{style="note"}

## Print PlainLog
Colotok has 5 levels of the log.

1. TRACE (verbose)
2. DEBUG
3. INFO
4. WARN
5. ERROR

use bellow methods for appropriately level

```Kotlin
logger.trace("TRACE LEVEL LOG")
logger.debug("DEBUG LEVEL LOG")
logger.info("INFO LEVEL LOG")
logger.warn("WARN LEVEL LOG")
logger.error("ERROR LEVEL LOG")
```

or

```Kotlin
logger.atInfo {
    print("in this block")
    print("all of logs are printed out with INFO level")
}
```

## Print StructuredLog
Colotok also can print structued log using `kotlinx-serialization`.  

> need for dependencies to kotlinx-serializations on your app
{style="note"}

implement the log structure

```kotlin
@Serializable
class LogDetail(val scope: String, val message: String): LogStructure

@Serializable
class Log(val name: String, val logDetail: LogDetail): LogStructure
```

then write the log
```Kotlin
logger.info(
    Log(
        name = "illegal state",
        LogDetail(
            "args",
            "argument must be greater than zero"
        )
    ),
    Log.serializer()
)
```