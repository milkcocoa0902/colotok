# Custom Formatter

Custom formatters subclass either `TextFormatter` or `StructuredFormatter`. Both format the timestamp captured when the log call is made, in UTC.

## Formatter Elements

### `Element.DATETIME`

Embeds an ISO-8601 UTC date-time. In a structured formatter, this is emitted in the `date` JSON field.

### `Element.DATE`

Embeds an ISO-8601 date (`yyyy-MM-dd`).

> For structured logging, `DATETIME` takes precedence. `DATE` plus `TIME` is treated as `DATETIME`; a single `DATE` emits only the date.
{style="note"}

### `Element.TIME`

Embeds an ISO-8601 UTC time.

> For structured logging, `DATETIME` takes precedence. `DATE` plus `TIME` is treated as `DATETIME`; a single `TIME` emits only the time.
{style="note"}

### `Element.LEVEL`

Embeds the log level.

### `Element.MESSAGE`

Embeds the log message. In a structured formatter, a `LogStructure` is emitted as JSON under the `message` field.

### `Element.THREAD`

Embeds the thread name.

### `Element.CALLER`

Embeds the caller captured for the logging event.

### `Element.ATTR`

Embeds additional attributes. In a structured formatter, every attribute is emitted as a top-level JSON field.

### `Element.NAME`

`Element.NAME` is reserved for a logger name, but the current formatter implementations do not render it. Do not include it in a custom format until that implementation support is added.

### `Element.CUSTOM(name)`

Embeds the named MDC value. In a structured formatter, an absent value is emitted as an empty string. In a text formatter, placeholders whose MDC key is absent remain unchanged.

## Custom Text Formatter

Define a custom text formatter:

```kotlin
object CustomTextFormatter : TextFormatter(
    fmt = """
    [${Element.LEVEL}]
    (${Element.THREAD}) ${Element.DATETIME}
    ${Element.CALLER} -> ${Element.MESSAGE},
    attr = ${Element.ATTR}
    """.trimIndent()
        .replace("\n", "")
)
```

Then configure a provider to use it.

```kotlin
val logger = ColotokLoggerContext()
    .addProvider(ConsoleProvider(ConsoleProviderConfig().apply {
        formatter = CustomTextFormatter
    }))
    .getLogger()
```

## Custom Structured Formatter

Define a custom structured formatter:

```kotlin
object CustomStructuredFormatter : StructuredFormatter(
    field = listOf(
        Element.THREAD,
        Element.MESSAGE,
        Element.LEVEL,
        Element.DATETIME,
        Element.ATTR,
    ),
    mask = listOf(
        "password",
        "secret",
        "credential",
    )
)
```

Then configure a provider to use it.

```kotlin
val logger = ColotokLoggerContext()
    .addProvider(ConsoleProvider(ConsoleProviderConfig().apply {
        formatter = CustomStructuredFormatter
    }))
    .getLogger()
```

### Field masking

For a `LogStructure`, `mask` matches field names case-insensitively, including nested objects and arrays. Matching values are replaced with up to 32 `*` characters. It does not mask plain-text messages, attributes, or MDC values.
