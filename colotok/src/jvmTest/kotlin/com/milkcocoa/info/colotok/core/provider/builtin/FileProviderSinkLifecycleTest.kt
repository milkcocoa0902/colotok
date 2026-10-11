package com.milkcocoa.info.colotok.core.provider.builtin

import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.provider.builtin.file.FileProvider
import com.milkcocoa.info.colotok.core.provider.builtin.file.getFileSystem
import com.milkcocoa.info.colotok.core.provider.rotation.Rotation
import com.milkcocoa.info.colotok.core.provider.rotation.SizeBaseRotation
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okio.Buffer
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.ForwardingSink
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Sink
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class FileProviderSinkLifecycleTest {
    @TempDir
    lateinit var tempDir: java.nio.file.Path

    private lateinit var fileSystem: TrackingFileSystem
    private val providers = mutableListOf<FileProvider>()

    @BeforeEach
    fun setup() {
        fileSystem = TrackingFileSystem(FileSystem.SYSTEM)
        mockkStatic(::getFileSystem)
        every { getFileSystem() } returns fileSystem
    }

    @AfterEach
    fun tearDown() {
        runBlocking {
            providers.forEach {
                it.forceShutdown()
                withTimeout(5.seconds) { runCatching { it.join() } }
            }
        }
        unmockkAll()
    }

    @Test
    fun consecutive_records_reuse_sink_and_are_visible_before_provider_flush() =
        runBlocking {
            val provider = provider()
            listOf("first", "日本語", "last").forEach { provider.write(record(it)) }

            withTimeout(5.seconds) { repeat(3) { fileSystem.flushed.receive() } }

            assertEquals("first\n日本語\nlast\n", logFile.readText())
            assertEquals(listOf("open", "write", "flush", "write", "flush", "write", "flush"), fileSystem.events)

            provider.join()
            assertEquals(1, fileSystem.events.count { it == "close" })
        }

    @Test
    fun explicit_flush_releases_sink_and_next_record_reopens_original_path() =
        runBlocking {
            val provider = provider()
            provider.write(record("before"))
            provider.flush(5.seconds)
            assertEquals(1, fileSystem.events.count { it == "close" })

            val moved = tempDir.resolve("moved.log")
            Files.move(logFile, moved)
            provider.write(record("after"))
            provider.join()

            assertEquals("before\n", moved.readText())
            assertEquals("after\n", logFile.readText())
            assertEquals(2, fileSystem.events.count { it == "open" })
            assertEquals(2, fileSystem.events.count { it == "close" })
        }

    @Test
    fun unused_provider_flush_and_shutdown_do_not_create_file_or_sink() =
        runBlocking {
            val provider = provider()
            provider.flush(5.seconds)
            provider.join()

            assertFalse(logFile.exists())
            assertTrue(fileSystem.events.isEmpty())
        }

    @Test
    fun unused_forced_shutdown_does_not_open_sink() =
        runBlocking {
            val provider = provider()
            val completion = CompletableDeferred<Result<Unit>>()
            provider.forceShutdown(this) { completion.complete(it) }
            withTimeout(5.seconds) { completion.await() }.getOrThrow()

            assertFalse(logFile.exists())
            assertTrue(fileSystem.events.isEmpty())
        }

    @Test
    fun directory_destination_appends_to_application_log() =
        runBlocking {
            val applicationLog = tempDir.resolve("application.log")
            Files.writeString(applicationLog, "existing\n")
            val provider = provider(tempDir.toOkioPath())
            provider.write(record("追加"))
            provider.join()

            assertEquals("existing\n追加\n", applicationLog.readText())
            assertEquals(listOf(applicationLog.toOkioPath()), fileSystem.openedPaths)
        }

    @Test
    fun size_rotation_closes_before_move_and_reopens_only_for_next_record() =
        runBlocking {
            val sizeRotation = SizeBaseRotation(4)
            val provider =
                provider(
                    rotation =
                        object : Rotation {
                            override fun isRotateNeeded(filePath: Path): Boolean = sizeRotation.isRotateNeeded(filePath)

                            override fun doRotate(filePath: Path) {
                                assertEquals("close", fileSystem.events.last())
                                fileSystem.events.add("rotate")
                                sizeRotation.doRotate(filePath)
                            }
                        }
                )
            // Each UTF-8 record is four bytes; equality must not trigger rotation.
            provider.write(record("あ"))
            provider.write(record("あ"))
            provider.flush(5.seconds)

            assertFalse(logFile.exists())
            assertEquals("あ\nあ\n", tempDir.resolve("test.log.1").readText())
            assertEquals(listOf("open", "write", "flush", "write", "flush", "close", "rotate"), fileSystem.events)

            provider.write(record("a"))
            provider.join()
            assertEquals("a\n", logFile.readText())
            assertEquals(2, fileSystem.events.count { it == "open" })
            assertEquals(2, fileSystem.events.count { it == "close" })
        }

    @ParameterizedTest
    @EnumSource(FailurePoint::class)
    fun io_and_rotation_failures_are_reported_without_retry(point: FailurePoint) =
        runBlocking {
            val failure = IOException("failed at $point")
            fileSystem.failurePoint = point
            fileSystem.failure = failure
            val rotation =
                object : Rotation {
                    override fun isRotateNeeded(filePath: Path): Boolean {
                        if (point == FailurePoint.ROTATION_CHECK) throw failure
                        return point == FailurePoint.ROTATE
                    }

                    override fun doRotate(filePath: Path) {
                        fileSystem.events.add("rotate")
                        throw failure
                    }
                }
            val provider = provider(rotation = rotation)
            provider.write(record("message"))

            assertSame(failure, assertFailsWith<IOException> { provider.flush(5.seconds) })
            assertSame(failure, assertFailsWith<IOException> { provider.join() })
            assertEquals(1, fileSystem.events.count { it == "open" })
            assertEquals(if (point == FailurePoint.OPEN) 0 else 1, fileSystem.events.count { it == "close" })
            assertEquals(if (point == FailurePoint.OPEN) 0 else 1, fileSystem.events.count { it == "write" })
            assertEquals(if (point == FailurePoint.ROTATE) 1 else 0, fileSystem.events.count { it == "rotate" })
        }

    @Test
    fun write_failure_keeps_cleanup_failure_as_suppressed() =
        runBlocking {
            val writeFailure = IOException("write failed")
            val closeFailure = IOException("close failed")
            fileSystem.failurePoint = FailurePoint.WRITE
            fileSystem.failure = writeFailure
            fileSystem.closeFailure = closeFailure
            val provider = provider()
            provider.write(record("message"))

            assertSame(writeFailure, assertFailsWith<IOException> { provider.join() })
            assertEquals(listOf(closeFailure), writeFailure.suppressedExceptions)
            assertEquals(1, fileSystem.events.count { it == "close" })
        }

    @Test
    fun formatter_failure_does_not_open_sink() =
        runBlocking {
            val failure = IllegalStateException("format failed")
            val provider =
                FileProvider(logFile.toOkioPath()) {
                    formatter =
                        object : Formatter by MessageFormatter {
                            override fun format(record: LogRecord.PlainText): String = throw failure
                        }
                }.also(providers::add)
            provider.write(record("message"))

            assertSame(failure, assertFailsWith<IllegalStateException> { provider.join() })
            assertTrue(fileSystem.events.isEmpty())
        }

    @Test
    fun forced_shutdown_waits_for_in_flight_io_before_closing_and_notifying() =
        runBlocking {
            val enteredWrite = CountDownLatch(1)
            val releaseWrite = CountDownLatch(1)
            fileSystem.beforeWrite = {
                enteredWrite.countDown()
                check(releaseWrite.await(5, TimeUnit.SECONDS)) { "write was not released" }
            }
            val provider = provider()
            val completion = CompletableDeferred<Result<Unit>>()
            try {
                provider.write(record("message"))
                assertTrue(enteredWrite.await(5, TimeUnit.SECONDS))
                provider.forceShutdown(this) { completion.complete(it) }

                assertFalse(completion.isCompleted)
                assertFalse(fileSystem.events.contains("close"))
            } finally {
                releaseWrite.countDown()
            }
            withTimeout(5.seconds) { completion.await() }.getOrThrow()

            assertEquals(listOf("open", "write", "flush", "close"), fileSystem.events)
            assertEquals("message\n", logFile.readText())
        }

    private val logFile: java.nio.file.Path
        get() = tempDir.resolve("test.log")

    private fun provider(
        path: Path = logFile.toOkioPath(),
        rotation: Rotation? = null
    ): FileProvider =
        FileProvider(path) {
            formatter = MessageFormatter
            this.rotation = rotation
        }.also(providers::add)

    private fun record(message: String): LogRecord = LogRecord.PlainText("test", message, LogLevel.INFO, emptyMap())

    enum class FailurePoint {
        OPEN,
        WRITE,
        FLUSH,
        CLOSE,
        ROTATION_CHECK,
        ROTATE
    }

    private class TrackingFileSystem(
        delegate: FileSystem
    ) : ForwardingFileSystem(delegate) {
        val events = CopyOnWriteArrayList<String>()
        val openedPaths = CopyOnWriteArrayList<Path>()
        val flushed = Channel<Unit>(Channel.UNLIMITED)
        var failurePoint: FailurePoint? = null
        var failure = IOException("I/O failed")
        var closeFailure: IOException? = null
        var beforeWrite: (() -> Unit)? = null

        override fun appendingSink(
            file: Path,
            mustExist: Boolean
        ): Sink {
            events.add("open")
            openedPaths.add(file)
            if (failurePoint == FailurePoint.OPEN) throw failure
            return object : ForwardingSink(super.appendingSink(file, mustExist)) {
                override fun write(
                    source: Buffer,
                    byteCount: Long
                ) {
                    events.add("write")
                    beforeWrite?.invoke()
                    if (failurePoint == FailurePoint.WRITE) throw failure
                    super.write(source, byteCount)
                }

                override fun flush() {
                    events.add("flush")
                    if (failurePoint == FailurePoint.FLUSH) throw failure
                    super.flush()
                    flushed.trySend(Unit)
                }

                override fun close() {
                    events.add("close")
                    super.close()
                    closeFailure?.let { throw it }
                    if (failurePoint == FailurePoint.CLOSE) throw failure
                }
            }
        }
    }

    private object MessageFormatter : Formatter {
        override fun format(record: LogRecord.PlainText): String = record.msg

        override fun <T : LogStructure> format(record: LogRecord.StructuredText<T>): String =
            error("unexpected structured record")

        override fun format(record: LogRecord.Metrics): String = error("unexpected metrics record")
    }
}