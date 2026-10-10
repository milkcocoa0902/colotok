package quality.benchmarks.platforms

@Volatile private var sink: Any? = null
actual fun platformName(): String = "android"
actual fun consume(value: Any?) { sink = value }
actual fun allocatedBytes(): Long = android.os.Debug.getRuntimeStat("art.gc.bytes-allocated")?.toLongOrNull() ?: -1
actual fun platformPrint(message: String) { android.util.Log.i("ColotokBenchmark", message) }
actual fun publish(message: String) { System.err.println(message) }
actual fun runtimeArguments(arguments: Array<String>): Array<String> = arguments
