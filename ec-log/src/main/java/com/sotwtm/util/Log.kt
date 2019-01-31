package com.sotwtm.util

import android.support.annotation.IntDef


/**
 * A logger that support enable/disable.
 * It can be used for replacing the [android.util.Log] easily by replace the package path in import.
 *
 * @author sheungon
 */
object Log {

    /**
     * Level to enable all logs
     * @see android.util.Log.VERBOSE
     *
     */
    const val VERBOSE = android.util.Log.VERBOSE
    /**
     * @see android.util.Log.DEBUG
     *
     */
    const val DEBUG = android.util.Log.DEBUG
    /**
     * @see android.util.Log.INFO
     *
     */
    const val INFO = android.util.Log.INFO
    /**
     * @see android.util.Log.WARN
     *
     */
    const val WARN = android.util.Log.WARN
    /**
     * @see android.util.Log.ERROR
     *
     */
    const val ERROR = android.util.Log.ERROR
    /**
     * @see android.util.Log.ASSERT
     *
     */
    const val ASSERT = android.util.Log.ASSERT
    /**
     * Level to disable all log usually in live version app
     */
    const val NONE = 100
    // Internal use
    private const val UNKNOWN = -1

    private const val LOG_MAX_CHAR_CHUNK = 2048

    @JvmStatic
    private val LOGGER_V = object : Logger {
        override fun printLog(tag: String?, msg: String?): Int = android.util.Log.v(tag, msg)
        override fun printLog(tag: String?, msg: String?, tr: Throwable?): Int = android.util.Log.v(tag, msg, tr)
    }

    @JvmStatic
    private val LOGGER_D = object : Logger {
        override fun printLog(tag: String?, msg: String?): Int = android.util.Log.d(tag, msg)
        override fun printLog(tag: String?, msg: String?, tr: Throwable?): Int = android.util.Log.d(tag, msg, tr)
    }

    @JvmStatic
    private val LOGGER_I = object : Logger {
        override fun printLog(tag: String?, msg: String?): Int = android.util.Log.i(tag, msg)
        override fun printLog(tag: String?, msg: String?, tr: Throwable?): Int = android.util.Log.i(tag, msg, tr)
    }

    @JvmStatic
    private val LOGGER_W = object : Logger {
        override fun printLog(tag: String?, msg: String?): Int = android.util.Log.w(tag, msg)
        override fun printLog(tag: String?, msg: String?, tr: Throwable?): Int = android.util.Log.w(tag, msg, tr)
    }

    @JvmStatic
    private val LOGGER_E = object : Logger {
        override fun printLog(tag: String?, msg: String?): Int = android.util.Log.e(tag, msg)
        override fun printLog(tag: String?, msg: String?, tr: Throwable?): Int = android.util.Log.e(tag, msg, tr)
    }

    @JvmStatic
    private val LOGGER_WTF = object : Logger {
        override fun printLog(tag: String?, msg: String?): Int = android.util.Log.wtf(tag, msg)
        override fun printLog(tag: String?, msg: String?, tr: Throwable?): Int = android.util.Log.wtf(tag, msg, tr)
    }

    /**
     * The default log tag for [v], [d], [i], [w], [e], [wtf]
     */
    @Suppress("MemberVisibilityCanBePrivate")
    @JvmStatic
    var defaultLogTag: String = "ECLog"

    /**
     * The extra action to do on method [wtf]* or overloading methods called.
     * This action will be taken when it is release build.
     * @see actionOnWtfDebug
     */
    @Suppress("MemberVisibilityCanBePrivate")
    @JvmStatic
    var actionOnWtf: OnWtfListener? = null

    /**
     * The extra action to do on method [wtf]* or overloading methods called.
     * This action will be taken when it is Debug build.
     * @see actionOnWtf
     */
    @Suppress("MemberVisibilityCanBePrivate")
    @JvmStatic
    var actionOnWtfDebug: OnWtfListener? = null

    /**
     * Set loggable level.
     * [VERBOSE] to enable all logs. [NONE] to disable all logs.
     * @return The current log level
     * @see LogLevel
     *
     */
    @LogLevel
    @JvmStatic
    var logLevel: Int = UNKNOWN
        @LogLevel
        get() =
            if (field == UNKNOWN) {
                if (BuildConfig.DEBUG) VERBOSE else NONE
            } else field
        set(@LogLevel lLevel) = when (lLevel) {
            VERBOSE, DEBUG, INFO, WARN, ERROR, ASSERT, NONE -> field = lLevel
            UNKNOWN -> {
                // Keep it as original one. Shouldn't enter here anyway
            }
            else -> throw IllegalArgumentException("Invalid log level passed to setLogLevel: $lLevel")
        }

    /**
     * It will check the debug level.
     *
     * @return true on log level other than NONE
     */
    @JvmStatic
    val isDebuggable: Boolean
        get() = logLevel != NONE

    @JvmStatic
    private val chunkPrefix: String
        get() = "<" + android.os.Process.myTid() + ">"

    /**Possible values for setting log level in [.setLogLevel] */
    @IntDef(VERBOSE, DEBUG, INFO, WARN, ERROR, ASSERT, NONE, UNKNOWN)
    @Retention(AnnotationRetention.SOURCE)
    annotation class LogLevel


    /**
     * @param level The log level going to check.
     * @return `true` if the input level is loggable.
     * @see LogLevel
     *
     */
    @JvmStatic
    fun isLoggable(@LogLevel level: Int): Boolean =
        level != NONE && android.util.Log.isLoggable(defaultLogTag, level)

    /**
     * @param tr The [Throwable] going to be converted
     * @return The Stacktrace string for the input [Throwable]
     */
    @JvmStatic
    fun getStackTraceString(tr: Throwable?): String = android.util.Log.getStackTraceString(tr)

    /**
     * For show UI related log or other repeating logs.
     * @param msg A log message.
     * @param tr  A [Throwable] of an error.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    @JvmOverloads
    fun v(
        msg: String?,
        tr: Throwable? = null
    ): Int =
        if (logLevel > VERBOSE) 0
        else printLog(LOGGER_V, defaultLogTag, msg.toTraceableLog(), tr)

    /**
     * For show UI related log or other repeating logs.
     * @param tag A log tag for this log message.
     * @param msg A log message.
     * @param tr  A [Throwable] of an error.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    @JvmOverloads
    fun v(
        tag: String?,
        msg: String?,
        tr: Throwable? = null
    ): Int =
        if (logLevel > VERBOSE || (msg == null && tr == null)) 0
        else printLog(LOGGER_V, tag, msg, tr)

    /**
     * For debug level message
     * @param msg A log message.
     * @param tr A [Throwable] related to this log.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    @JvmOverloads
    fun d(
        msg: String?,
        tr: Throwable? = null
    ): Int =
        if (logLevel > DEBUG) 0
        else printLog(LOGGER_D, defaultLogTag, msg.toTraceableLog(), tr)

    /**
     * For debug level message
     * @param tag A log tag for this log message.
     * @param msg A log message.
     * @param tr A [Throwable] related to this log.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    @JvmOverloads
    fun d(
        tag: String?,
        msg: String?,
        tr: Throwable? = null
    ): Int =
        if (logLevel > DEBUG || (msg == null && tr == null)) 0
        else printLog(LOGGER_D, tag, msg, tr)

    /**
     * For info message
     * @param msg A log message.
     * @param tr A [Throwable] related to this log.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    @JvmOverloads
    fun i(
        msg: String?,
        tr: Throwable? = null
    ): Int =
        if (logLevel > INFO) 0
        else printLog(LOGGER_I, defaultLogTag, msg.toTraceableLog(), tr)

    /**
     * For info message
     * @param tag A log tag for this log message.
     * @param msg A log message.
     * @param tr A [Throwable] related to this log.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    @JvmOverloads
    fun i(
        tag: String?,
        msg: String?,
        tr: Throwable? = null
    ): Int =
        if (logLevel > INFO || (msg == null && tr == null)) 0
        else printLog(LOGGER_I, tag, msg, tr)

    /**
     * For warning message.
     * @param msg A log message.
     * @param tr A [Throwable] related to this log.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    @JvmOverloads
    fun w(
        msg: String?,
        tr: Throwable? = null
    ): Int =
        if (logLevel > WARN) 0
        else printLog(LOGGER_W, defaultLogTag, msg.toTraceableLog(), tr)

    /**
     * For warning message.
     * @param tag A log tag for this log message.
     * @param msg A log message.
     * @param tr A [Throwable] related to this log.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    @JvmOverloads
    fun w(
        tag: String?,
        msg: String?,
        tr: Throwable? = null
    ): Int =
        if (logLevel > WARN || (msg == null && tr == null)) 0
        else printLog(LOGGER_W, tag, msg, tr)


    /**
     * For error message
     * @param msg A log message.
     * @param tr A [Throwable] related to this log.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    @JvmOverloads
    fun e(
        msg: String?,
        tr: Throwable? = null
    ): Int =
        if (logLevel > ERROR) 0
        else printLog(LOGGER_E, defaultLogTag, msg.toTraceableLog(), tr)

    /**
     * For error message
     * @param tag A log tag for this log message.
     * @param msg A log message.
     * @param tr A [Throwable] related to this log.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    @JvmOverloads
    fun e(
        tag: String?,
        msg: String?,
        tr: Throwable? = null
    ): Int =
        if (logLevel > ERROR || (msg == null && tr == null)) 0
        else printLog(LOGGER_E, tag, msg, tr)

    /**
     * For What a Terrible Failure
     * @param msg A log message.
     * @param tr A [Throwable] related to this log.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    @JvmOverloads
    fun wtf(
        msg: String?,
        tr: Throwable? = null
    ): Int = wtf(defaultLogTag, msg.toTraceableLog(), tr)

    /**
     * For What a Terrible Failure
     * @param tag A log tag for this log message.
     * @param msg A log message.
     * @param tr A [Throwable] related to this log.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    @JvmOverloads
    fun wtf(
        tag: String?,
        msg: String?,
        tr: Throwable? = null
    ): Int =
        if (logLevel > ASSERT || (msg == null && tr == null)) 0
        else {
            val wrote = printLog(LOGGER_WTF, tag, msg, tr)

            // runtime exception if you turn on assert
            if (BuildConfig.DEBUG) {
                actionOnWtfDebug?.onWtf(msg, tr)
                    ?: {
                        if (tr == null) {
                            throw WtfException(msg)
                        } else {
                            throw WtfException(msg, tr)
                        }
                    }.invoke()
            } else {
                actionOnWtf?.onWtf(msg, tr)
            }

            wrote
        }

    private fun printLog(
        logger: Logger,
        tag: String?,
        msg: String?,
        tr: Throwable? = null
    ): Int {

        if (msg != null && msg.length > LOG_MAX_CHAR_CHUNK) {
            var wroteByte = 0
            var chunkCount = 0

            val prefix = chunkPrefix
            var i = 0
            while (i < msg.length) {
                val endPos = i + LOG_MAX_CHAR_CHUNK
                wroteByte += if (endPos <= msg.length) {
                    logger.printLog(tag, chunkCount.toString() + prefix + msg.substring(i, endPos))
                } else {
                    logger.printLog(tag, chunkCount.toString() + prefix + msg.substring(i))
                }
                chunkCount++
                i += LOG_MAX_CHAR_CHUNK
            }

            if (tr != null) {
                wroteByte += logger.printLog(tag, "", tr)
            }

            return wroteByte
        }

        return logger.printLog(tag, msg, tr)
    }

    @JvmStatic
    private fun StringBuilder.appendTraceInfoPrefix() {

        append("<")
        append(android.os.Process.myTid())
        append(">[")

        try {
            Throwable().stackTrace?.let { stackTrace ->

                // Look for Log class's upper level
                val outerClass = stackTrace.find { it.className != javaClass.name } ?: return

                val className = outerClass.className
                val subClass: String = className.substringAfter('$', "")

                // Form a log that supports lookup to source code with this format
                append("(")
                    .append(outerClass.fileName)
                    .append(":")
                    .append(outerClass.lineNumber)
                    .append(")")
                if (subClass.isNotEmpty()) {
                    append(subClass)
                }
                append("#")
                    .append(outerClass.methodName)
            }
        } finally {
            append("] ")
        }
    }

    @JvmStatic
    private fun String?.toTraceableLog(): String {

        val sb = StringBuilder()
        sb.appendTraceInfoPrefix()
        if (this != null) sb.append(this)

        return sb.toString()
    }


    //////////////////////////////////////////
    // Class and interface
    //////////////////////////////////////////
    /**
     * Listener to capture [Log.wtf] event and react to it
     */
    interface OnWtfListener {
        /**
         * @param msg The message for the wtf event.
         * @param tr The [Throwable] causing this wtf event.
         */
        fun onWtf(msg: String?, tr: Throwable?)
    }

    private interface Logger {
        fun printLog(tag: String?, msg: String?): Int
        fun printLog(tag: String?, msg: String?, tr: Throwable?): Int
    }
}
