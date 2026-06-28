package com.milkcocoa.info.colotok.core.provider.rotation

import com.milkcocoa.info.colotok.core.provider.builtin.file.getFileSystem
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import okio.Path.Companion.toOkioPath
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.notExists
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class DateBaseRotationTest {
    @TempDir
    lateinit var tempDir: java.nio.file.Path

    private lateinit var logFile: java.nio.file.Path

    @BeforeEach
    fun setup() {
        mockkObject(kotlin.time.Clock.System)
        logFile = tempDir.resolve("application.log")
        logFile.createFile()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun fresh_file_does_not_rotate_before_period() {
        val rotation = DateBaseRotation(period = 7.days)
        val baseTimestamp = fileTimestamp()
        every { kotlin.time.Clock.System.now() } returns baseTimestamp.plus(7.days).minus(1.milliseconds)

        assertFalse(rotation.isRotateNeeded(logFile.toOkioPath()))
    }

    @Test
    fun file_rotates_when_age_reaches_period() {
        val rotation = DateBaseRotation(period = 7.days)
        val baseTimestamp = fileTimestamp()
        every { kotlin.time.Clock.System.now() } returns baseTimestamp.plus(7.days)

        assertTrue(rotation.isRotateNeeded(logFile.toOkioPath()))
    }

    @Test
    fun file_rotates_when_age_exceeds_period() {
        val rotation = DateBaseRotation(period = 7.days)
        val baseTimestamp = fileTimestamp()
        every { kotlin.time.Clock.System.now() } returns baseTimestamp.plus(7.days).plus(1.milliseconds)

        assertTrue(rotation.isRotateNeeded(logFile.toOkioPath()))
    }

    @Test
    fun missing_file_does_not_throw_or_rotate() {
        val rotation = DateBaseRotation(period = 7.days)
        every { kotlin.time.Clock.System.now() } returns Instant.parse("2024-01-01T00:00:00Z")

        assertFalse(rotation.isRotateNeeded(tempDir.resolve("missing.log").toOkioPath()))
    }

    @Test
    fun doRotate_does_not_write_debug_stdout() {
        Files.writeString(logFile, "message")
        tempDir.resolve("application.log.1").createFile()

        val stdout = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(stdout))
        try {
            DateBaseRotation(period = 7.days).doRotate(logFile.toOkioPath())
        } finally {
            System.setOut(originalOut)
        }

        assertEquals("", stdout.toString())
        assertTrue(logFile.notExists())
        assertTrue(tempDir.resolve("application.log.2").exists())
    }

    private fun fileTimestamp(): Instant {
        val metadata = getFileSystem().metadata(logFile.toOkioPath())
        val timestampMillis = metadata.createdAtMillis ?: metadata.lastModifiedAtMillis
        return Instant.fromEpochMilliseconds(checkNotNull(timestampMillis))
    }
}
