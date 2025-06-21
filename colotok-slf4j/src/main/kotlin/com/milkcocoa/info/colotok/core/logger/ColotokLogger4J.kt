package com.milkcocoa.info.colotok.core.logger

import org.slf4j.Logger
import org.slf4j.Marker

class ColotokLogger4J(private val name: String): Logger {
    private val delegate = ColotokLogger.getDefault()


    override fun info(msg: String?) {
        delegate.info(msg.orEmpty())
    }

    override fun info(format: String?, arg: Any?) {
        delegate.info(String.format(format.orEmpty(), arg))
    }

    override fun info(format: String?, arg1: Any?, arg2: Any?) {
        delegate.info(String.format(format.orEmpty(), arg1, arg2))
    }

    override fun info(format: String?, vararg arguments: Any?) {
        delegate.info(String.format(format.orEmpty(), arguments))
    }

    override fun info(msg: String?, t: Throwable?) {
        delegate.info(msg.orEmpty(), mapOf("cause" to t.toString()))
    }

    override fun isInfoEnabled(marker: Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun info(marker: Marker?, msg: String?) {
        TODO("Not yet implemented")
    }

    override fun info(marker: Marker?, format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun info(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        TODO("Not yet implemented")
    }

    override fun info(marker: Marker?, format: String?, vararg arguments: Any?) {
        TODO("Not yet implemented")
    }

    override fun info(marker: Marker?, msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun isWarnEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun debug(msg: String?) {
        delegate.debug (msg.orEmpty())
    }

    override fun debug(format: String?, arg: Any?) {
        delegate.debug(String.format(format.orEmpty(), arg))
    }

    override fun debug(format: String?, arg1: Any?, arg2: Any?) {
        delegate.debug(String.format(format.orEmpty(), arg1, arg2))
    }

    override fun debug(format: String?, vararg arguments: Any?) {
        delegate.debug(String.format(format.orEmpty(), arguments))
    }

    override fun debug(msg: String?, t: Throwable?) {
        delegate.debug(msg.orEmpty(), mapOf("cause" to t.toString()))
    }

    override fun isDebugEnabled(marker: Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun debug(marker: Marker?, msg: String?) {
        TODO("Not yet implemented")
    }

    override fun debug(marker: Marker?, format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun debug(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        TODO("Not yet implemented")
    }

    override fun debug(marker: Marker?, format: String?, vararg arguments: Any?) {
        TODO("Not yet implemented")
    }

    override fun debug(marker: Marker?, msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun isInfoEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun warn(msg: String?) {
        delegate.warn ( msg.orEmpty() )
    }

    override fun warn(format: String?, arg: Any?) {
        delegate.warn(String.format(format.orEmpty(), arg))
    }

    override fun warn(format: String?, vararg arguments: Any?) {
        delegate.warn(String.format(format.orEmpty(), arguments))
    }

    override fun warn(format: String?, arg1: Any?, arg2: Any?) {
        delegate.warn(String.format(format.orEmpty(), arg1, arg2))
    }

    override fun warn(msg: String?, t: Throwable?) {
        delegate.warn(msg.orEmpty(), mapOf("cause" to t.toString()))
    }

    override fun isWarnEnabled(marker: Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun warn(marker: Marker?, msg: String?) {
        TODO("Not yet implemented")
    }

    override fun warn(marker: Marker?, format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun warn(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        TODO("Not yet implemented")
    }

    override fun warn(marker: Marker?, format: String?, vararg arguments: Any?) {
        TODO("Not yet implemented")
    }

    override fun warn(marker: Marker?, msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun isErrorEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun error(msg: String?) {
        delegate.error (msg.orEmpty())
    }

    override fun error(format: String?, arg: Any?) {
        delegate.error(String.format(format.orEmpty(), arg))
    }

    override fun error(format: String?, arg1: Any?, arg2: Any?) {
        delegate.error(String.format(format.orEmpty(), arg1, arg2))
    }

    override fun error(format: String?, vararg arguments: Any?) {
        delegate.error(String.format(format.orEmpty(), arguments))
    }

    override fun error(msg: String?, t: Throwable?) {
        delegate.error(msg.orEmpty(), mapOf("cause" to t.toString()))
    }

    override fun isErrorEnabled(marker: Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun error(marker: Marker?, msg: String?) {
        TODO("Not yet implemented")
    }

    override fun error(marker: Marker?, format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun error(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        TODO("Not yet implemented")
    }

    override fun error(marker: Marker?, format: String?, vararg arguments: Any?) {
        TODO("Not yet implemented")
    }

    override fun error(marker: Marker?, msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun getName(): String = name
    override fun isTraceEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun trace(msg: String?) {
        delegate.trace (msg.orEmpty())
    }

    override fun trace(format: String?, arg: Any?) {
        delegate.trace(String.format(format.orEmpty(), arg))
    }

    override fun trace(format: String?, arg1: Any?, arg2: Any?) {
        delegate.trace(String.format(format.orEmpty(), arg1, arg2))
    }

    override fun trace(format: String?, vararg arguments: Any?) {
        delegate.trace(String.format(format.orEmpty(), arguments))
    }

    override fun trace(msg: String?, t: Throwable?) {
        delegate.trace(msg.orEmpty(), mapOf("cause" to t.toString()))
    }

    override fun isTraceEnabled(marker: Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun trace(marker: Marker?, msg: String?) {
        TODO("Not yet implemented")
    }

    override fun trace(marker: Marker?, format: String?, arg: Any?) {
        TODO("Not yet implemented")
    }

    override fun trace(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        TODO("Not yet implemented")
    }

    override fun trace(marker: Marker?, format: String?, vararg argArray: Any?) {
        TODO("Not yet implemented")
    }

    override fun trace(marker: Marker?, msg: String?, t: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun isDebugEnabled(): Boolean {
        TODO("Not yet implemented")
    }
}