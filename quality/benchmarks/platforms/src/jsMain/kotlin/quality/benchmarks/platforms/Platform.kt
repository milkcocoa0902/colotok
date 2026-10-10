package quality.benchmarks.platforms

actual fun platformName(): String = "js"
actual fun consume(value: Any?) { js("globalThis").colotokBenchmarkSink = value }
actual fun allocatedBytes(): Long = -1
actual fun platformPrint(message: String) { console.info(message) }
actual fun publish(message: String) { js("process.stderr").write(message + "\n") }
actual fun runtimeArguments(arguments: Array<String>): Array<String> = js("process.argv.slice(2)")
