# Custom Provider
Colotok can print the logs where you want.  
For example, if you want to write log into slack, you create a SlackProvider like below.  

```Kotlin
dependencies {
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:2.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
}
```

```kotlin
@Serializable
data class SlackWebhookPayload(
    val text: String,
)

class SlackProvider(config: SlackProviderConfig): Provider {
    constructor(config: SlackProviderConfig.() -> Unit): this(SlackProviderConfig().apply(config))

    class SlackProviderConfig() : ProviderConfig {
        var webhook_url: String = ""

        override var level: level = LogLevel.DEBUG

        override var formatter: Formatter = DetailTextFormatter
    }

    private val webhookUrl = config.webhook_url
    private val logLevel = config.level
    private val formatter = config.formatter

    override fun write(name: String, msg: String, level: LogLevel, attr: Map<String, String>) {
        if(level.isEnabledFor(logLevel).not()){
            return
        }
        kotlin.runCatching {
            webhookUrl.httpPost()
                .appendHeader("Content-Type" to "application/json")
                .body(Json.encodeToString(text = formatter.format(msg, level, attr)))
                .response()
        }.getOrElse { println(it) }
    }

    override fun <T : LogStructure> write(
        name: String,
        msg: T,
        serializer: KSerializer<T>,
        level: LogLevel,
        attr: Map<String, String>
    ) {
        if(level.isEnabledFor(logLevel).not()){
            return
        }
        kotlin.runCatching {
            webhookUrl.httpPost()
                .appendHeader("Content-Type" to "application/json")
                .body(Json.encodeToString(text = formatter.format(msg, serializer, level, attr)))
                .response()
        }.getOrElse { println(it) }
    }
}
```

in this provider, uses `fuel` for HTTP request.

```Kotlin
dependencies {
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:2.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
}
```


#### use your provider
now you can use SlackProvider to write the log into slack.

```kotlin
val logger = LoggerFactory()
        .addProvider(ConsoleProvider{
            formatter = DetailTextFormatter
            level = LogLevel.DEBUG
        })
        .addProvider(SlackProvider{
            webhook_url = "your slack webhook url"
            formatter = SimpleTextFormatter
            level = LogLevel.WARN
        })
        .getLogger()
```

#### print the log
```kotlin
logger.info("info level log")
// written the log only console

logger.error("error level log")
// written the log both of console and slack
```
