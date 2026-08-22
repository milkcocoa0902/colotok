# Formatter

A `Formatter` converts a queued `LogRecord` to the string written by a provider. The built-in
formatters are available from the `com.milkcocoa.info.colotok.core.formatter.builtin` packages.

## TextFormatter
`TextFormatter` is used to format as text. 'text' means **NOT structured**  

Colotok has three built-in `TextFormatter` implementations. `%D` (`DATETIME`), `%d` (`DATE`),
and `%T` (`TIME`) are independent replacements. All timestamp values use the UTC event timestamp
captured when the log call creates the record. Other available placeholders are `%L` (level), `%l`
(message), `%t` (thread), `%a` (additional attributes), `%C` (caller where the target supports it),
and `%{key}` (an MDC value). `%n` (`Element.NAME`) is reserved but is not currently rendered.

### PlainTextFormatter

```Kotlin
val logger = ColotokLoggerContext()
    .addProvider(ConsoleProvider(ConsoleProviderConfig().apply {
        formatter = PlainTextFormatter
    }))
    .getLogger()

logger.info("message what happen")

// message what happen
```


### SimpleTextFormatter
```Kotlin
val logger = ColotokLoggerContext()
    .addProvider(ConsoleProvider(ConsoleProviderConfig().apply {
        formatter = SimpleTextFormatter
    }))
    .getLogger()

logger.info("message what happen")

// 2023-12-29 12:23:14.220383  [INFO] - message what happen
```

### DetailTextFormatter
```Kotlin
val logger = ColotokLoggerContext()
    .addProvider(ConsoleProvider(ConsoleProviderConfig().apply {
        formatter = DetailTextFormatter
    }))
    .getLogger()

logger.info("message what happen", mapOf("param1" to "a custom attribute"))

// 2023-12-29T12:21:13.354328 (main)[INFO] - message what happen, additional = {param1=a custom attribute}
```

## StructuredFormatter
`StructuredFormatter` is used to format as structured log

Colotok has 2 types of builtin `StructuredFormatter`.
1. SimpleStructureFormatter
2. DetailStructureFormatter

`StructuredFormatter` emits JSON and at most one `date` field. `DATETIME` takes precedence over
`DATE` and `TIME`; `DATE` plus `TIME` is treated as `DATETIME`. Otherwise, a single `DATE` or
`TIME` emits only that component. The built-in `SimpleStructureFormatter` emits `message`, `level`,
and a UTC date; `DetailStructureFormatter` emits `message`, `level`, a UTC datetime, `thread`, and
additional attributes.

For example, define a structured message type:
```kotlin
@Serializable
data class LogDetail(val scope: String, val message: String): LogStructure

@Serializable
data class Log(val name: String, val logDetail: LogDetail): LogStructure
```


### SimpleStructureFormatter

```Kotlin
val logger = ColotokLoggerContext()
    .addProvider(ConsoleProvider(ConsoleProviderConfig().apply {
        formatter = SimpleStructureFormatter
    }))
    .getLogger()

logger.info(
    Log(
        name = "illegal state",
        logDetail = LogDetail(
            scope = "args",
            message = "argument must be greater than zero"
        )
    )
)

// {"message":{"name":"illegal state","logDetail":{"scope":"args","message":"argument must be greater than zero"}},"level":"INFO","date":"2023-12-29"}


logger.info("message what happen")

// {"message":"message what happen","level":"INFO","date":"2023-12-29"}
```

### DetailStructureFormatter
```Kotlin
val logger = ColotokLoggerContext()
    .addProvider(ConsoleProvider(ConsoleProviderConfig().apply {
        formatter = DetailStructureFormatter
    }))
    .getLogger()

logger.info(
    Log(
        name = "illegal state",
        logDetail = LogDetail(
            scope = "args",
            message = "argument must be greater than zero"
        )
    ),
    // you can pass additional attributes
    mapOf("additional" to "additional param")
)

// {"message":{"name":"illegal state","logDetail":{"scope":"args","message":"argument must be greater than zero"}},"level":"INFO","thread":"main","additional":"additional param","date":"2023-12-29T12:34:56"}


logger.info("message what happen")

// {"message":"message what happen","level":"INFO","thread":"main","date":"2023-12-29T12:27:22.5908"}
```

### Mask Field
If a structured value has a field whose name is `password` or `secret`, that field is masked
recursively in the built-in structured formatters. Matching is case-insensitive; the replacement
contains up to 32 asterisks based on the serialized value length.

- password
- secret

```Kotlin
@Serializable
data class Credential(
    val username: String,
    val password: String,
    val raw_password: String
): LogStructure

val logger = ColotokLoggerContext()
    .addProvider(ConsoleProvider(ConsoleProviderConfig().apply {
        formatter = SimpleStructureFormatter
    }))
    .getLogger()

logger.info(
    Credential(
        username = "user_name",
        password = "this field is masked",
        raw_password = "this field is not masked"
    )
)

// {"message":{"username":"user_name","password":"**********************","raw_password":"this field is not masked"},"level":"INFO","date":"2023-12-31"}
```
