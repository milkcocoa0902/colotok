@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)
package quality.benchmarks.platforms

import kotlin.concurrent.atomics.AtomicReference
import platform.posix.fputs
import platform.posix.stderr

private val sink = AtomicReference<Any?>(null)
actual fun platformName(): String = "linuxX64"
actual fun consume(value: Any?) { sink.store(value) }
actual fun allocatedBytes(): Long = -1
actual fun platformPrint(message: String) { println(message) }
actual fun publish(message: String) { fputs(message + "\n", stderr) }
actual fun runtimeArguments(arguments: Array<String>): Array<String> = arguments
