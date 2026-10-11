package com.milkcocoa.info.colotok.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CallerFrameUtilsTest {
    @Test
    fun caller_is_the_first_frame_after_logging_even_when_logging_reappears() {
        val stack = arrayOf("runtime", "logging", "logging", "caller", "logging", "outer")

        assertEquals("caller", firstCallerFrame(stack) { it == "logging" })
    }

    @Test
    fun absent_logging_or_absent_caller_returns_null() {
        assertNull(firstCallerFrame(emptyArray<String>()) { it == "logging" })
        assertNull(firstCallerFrame(arrayOf("runtime", "caller")) { it == "logging" })
        assertNull(firstCallerFrame(arrayOf("runtime", "logging", "logging")) { it == "logging" })
        assertNull(firstCallerFrame(arrayOf("logging")) { it == "logging" })
    }

    @Test
    fun package_abbreviation_preserves_class_method_and_line() {
        assertEquals("c.e.Example#log:42", formatCallerFrame("com.example.Example", "log", 42))
        assertEquals("c.Outer\$Inner#<init>:-1", formatCallerFrame("com.Outer\$Inner", "<init>", -1))
        assertEquals("日.例#呼出:7", formatCallerFrame("日本語.例", "呼出", 7))
    }

    @Test
    fun default_package_preserves_the_leading_dot() {
        assertEquals(".Example#log:42", formatCallerFrame("Example", "log", 42))
    }

    @Test
    fun empty_package_segments_preserve_the_existing_failure() {
        assertFailsWith<NoSuchElementException> { formatCallerFrame(".Example", "log", 42) }
        assertFailsWith<NoSuchElementException> { formatCallerFrame("com..Example", "log", 42) }
    }
}