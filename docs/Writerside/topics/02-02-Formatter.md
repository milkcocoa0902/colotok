# Formatter

Formatter tells logger how to format the text 

## TextFormatter
`TextFormatter` is used to format as text. 'text' means **NOT structured**  

Colotok has 3 types of builtin `TextFormatter`

### PlainTextFormatter

```Kotlin
logger.info("message what happen")

// message what happen
```


### SimpleTextFormatter
```Kotlin
logger.info("message what happen")

// 2023-12-29 12:23:14.220383  [INFO] - message what happen
```

### DetailsTextFormatter
```Kotlin
logger.ingo("message what happen", mapOf("param1" to "a custom attribute"))

// 2023-12-29T12:21:13.354328 (main)[INFO] - message what happen, additional = {param1=a custom attribute}
```

## StructuredFormatter
`StructuredFormatter` is used to format as structured log

Colotok has 2 types of builtin `StructuredFormatter`.
1. SimpleStructureFormatter
2. DetailStructureFormatter

if you has a class
```kotlin
@Serializable
class LogDetail(val scope: String, val message: String): LogStructure

@Serializable
class Log(val name: String, val logDetail: LogDetail): LogStructure
```


### SimpleStructureFormatter

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

// // {"message":{"name":"illegal state","logDetail":{"scope":"args","message":"argument must be greater than zero"}},"level":"INFO","date":"2023-12-29"}


logger.info("message what happen")

// {"msg":"message what happen","level":"INFO","date":"2023-12-29"}
```

### DetailStructureFormatter
```Kotlin
logger.info(
    Log(
        name = "illegal state",
        LogDetail(
            "args",
            "argument must be greater than zero"
        )
    ),
    Log.serializer(),
    // you can pass additional attrs
    mapOf("additional" to "additional param")
)

// {"message":{"name":"illegal state","logDetail":{"scope":"args","message":"argument must be greater than zero"}},"level":"INFO","additional":"additional param","date":"2023-12-29T12:34:56"}


logger.info("message what happen")

// {"message":"message what happen","level":"INFO","thread":"main","date":"2023-12-29T12:27:22.5908"}
```

### Mask Field
if your structure has the field 

- password
- secret

these fields are masked with __*__

```Kotlin
@Serializable
class Credential(
    val username: String,
    val password: String,
    val raw_password: String
): LogStructure

logger.info(
    Credential(
        username = "user_name",
        password = "this field is masked",
        raw_password = "this field is not masked"
    ),
    Credential.serializer()
)

// {"message":{"username":"user_name","password":"**********************","raw_password":"this field is not masked"},"level":"ERROR","date":"2023-12-31"}
```