# Custom Formatter
Define your formatter which you need.  

## Formatter Elements

### `Element.DATETIME`
To used to embed the datetime when you print the log.   
Formatted as `yyyy-MM-ddTHH:mm:ss.SSS`  

> Your application locale is used. NOT UTC
{style="note"}

### `Element.DATE`
To used to embed the date when you print the log.  
Formatted as `yyyy-MM-dd`

> when structured logging, ignored if `DATETIME` is also specified,  
> and if used with `TIME`, would be work like `DATETIME`
{style=warning}

### `Element.TIME`
To used to embed the time when you print the log.  
Formatted as `HH:mm:ss.SSS`

> Your application locale is used. NOT UTC
{style="note"}

> when structured logging, ignored if `DATETIME` is also specified,  
> and if used with `DATE`, would be work like `DATETIME`
{style=warning}

### `Element.LEVEL` ...  
To used to embed the log level.

### `Element.MESSAGE` ...   
To used to embed the message what you want.

### `Element.THREAD` ...  
To used to embed the thread name

### `Element.CALLER` ...  
To used to embed the caller where you print the log.

### `Element.ATTR` ...  
To used to embed the additional attributes

### `Element.NAME` ...  
To used to embed the logger name

### `Element.CUSTOM(name)` ...   
To used to embed the Custom field which provided by logger context.

## Custom Text Formatter
Defines your custom formatter

``` kotlin
object CustomTextFormatter: TextFormatter(
    fmt = """ 
    [${Element.LEVEL}] 
    (${Element.THREAD}) ${Element.DATETIME} 
    ${Element.CALLER}} -> ${Element.MESSAGE}, 
    attr = ${Element.ATTR}
    """.trimIndent()
        .replace("\n", "")
)
```

Then you can use formatter.

```kotlin
val logger = LoggerFactory()
    .addProvider(ConsoleProvider{
        this.formatter = CustomTextFormatter
    })
    .getLogger()
```

## Custom Structured Formatter
Defines your custom formatter

```kotlin
object CustomStructuredFormatter : StructuredFormatter(
    field = listOf(
        Element.THREAD,
        Element.MESSAGE,
        Element.LEVEL,
        Element.DATETIME,
        Element.THREAD,
        Element.ATTR
    ),
    mask = listOf(
        "password",
        "secret",
        "credential",
    )
)
```

and then, you can use this formatter.

```kotlin
val logger = LoggerFactory()
    .addProvider(ConsoleProvider{
        this.formatter = CustomStructuredFormatter
    })
    .getLogger()
```

### Field masking
Structured Formatter will replace by `*` which provided by `mask` field. 

