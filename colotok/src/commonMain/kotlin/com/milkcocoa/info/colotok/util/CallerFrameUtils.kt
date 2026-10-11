package com.milkcocoa.info.colotok.util

/** Keeps the first frame after the first contiguous group of logging frames. */
internal inline fun <T> firstCallerFrame(
    stack: Array<T>,
    isLoggingFrame: (T) -> Boolean
): T? {
    var foundLoggingFrame = false
    for (frame in stack) {
        if (isLoggingFrame(frame)) {
            foundLoggingFrame = true
        } else if (foundLoggingFrame) {
            return frame
        }
    }
    return null
}

internal fun formatCallerFrame(
    className: String,
    methodName: String,
    lineNumber: Int
): String =
    buildString {
        var segmentStart = 0
        var separator = className.indexOf('.')
        if (separator < 0) append('.') // Preserve the existing default-package representation.
        while (separator >= 0) {
            if (segmentStart == separator) throw NoSuchElementException("Char sequence is empty.")
            append(className[segmentStart])
            append('.')
            segmentStart = separator + 1
            separator = className.indexOf('.', segmentStart)
        }
        append(className, segmentStart, className.length)
        append('#')
        append(methodName)
        append(':')
        append(lineNumber)
    }