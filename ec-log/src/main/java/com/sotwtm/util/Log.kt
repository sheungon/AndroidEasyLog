package com.sotwtm.util

import android.support.annotation.IntDef


/**
 * A logger that support enable/disable.
 * It can be used for replacing the [android.util.Log] easily
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
    /**Internal use */
    private const val UNKNOWN = -1

    private const val LOG_MAX_CHAR_CHUNK = 2048

    @JvmStatic
    private val LOGGER_V = object : Logger {
        override fun printLog(tag: String?, msg: String?): Int {
            return android.util.Log.v(tag, msg)
        }

        override fun printLog(tag: String?, msg: String?, tr: Throwable?): Int {
            return android.util.Log.v(tag, msg, tr)
        }
    }
    @JvmStatic
    private val LOGGER_D = object : Logger {
        override fun printLog(tag: String?, msg: String?): Int {
            return android.util.Log.d(tag, msg)
        }

        override fun printLog(tag: String?, msg: String?, tr: Throwable?): Int {
            return android.util.Log.d(tag, msg, tr)
        }
    }
    @JvmStatic
    private val LOGGER_I = object : Logger {
        override fun printLog(tag: String?, msg: String?): Int {
            return android.util.Log.i(tag, msg)
        }

        override fun printLog(tag: String?, msg: String?, tr: Throwable?): Int {
            return android.util.Log.i(tag, msg, tr)
        }
    }
    @JvmStatic
    private val LOGGER_W = object : Logger {
        override fun printLog(tag: String?, msg: String?): Int {
            return android.util.Log.w(tag, msg)
        }

        override fun printLog(tag: String?, msg: String?, tr: Throwable?): Int {
            return android.util.Log.w(tag, msg, tr)
        }
    }
    @JvmStatic
    private val LOGGER_E = object : Logger {
        override fun printLog(tag: String?, msg: String?): Int {
            return android.util.Log.e(tag, msg)
        }

        override fun printLog(tag: String?, msg: String?, tr: Throwable?): Int {
            return android.util.Log.e(tag, msg, tr)
        }
    }
    @JvmStatic
    private val LOGGER_WTF = object : Logger {
        override fun printLog(tag: String?, msg: String?): Int {
            return android.util.Log.wtf(tag, msg)
        }

        override fun printLog(tag: String?, msg: String?, tr: Throwable?): Int {
            return android.util.Log.wtf(tag, msg, tr)
        }
    }

    @JvmStatic
    private var _DefaultTag = "Log"
    @JvmStatic
    private var _ActionOnWtf: OnWtfListener? = null
    @JvmStatic
    private var _ActionOnWtfDebug: OnWtfListener? = null

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
        get() = if (field == UNKNOWN) {
            if (BuildConfig.DEBUG) VERBOSE else NONE
        } else field
        set(@LogLevel lLevel) = when (lLevel) {
            VERBOSE, DEBUG, INFO, WARN, ERROR, ASSERT, NONE -> field = lLevel

            UNKNOWN -> {
                // Keep it as original one. Shouldn't enter here anyway
            }

            else -> throw IllegalArgumentException("Invalid log level passed to setLogLevel: " + lLevel)
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
     * The default log tag for [.v], [.d], [.i],
     * [.w], [.e], [.wtf]
     * @param logTag The log tag
     */
    @JvmStatic
    fun setDefaultLogTag(logTag: String) {
        _DefaultTag = logTag
    }

    /**
     * The extra action to do on method [.wtf]* or overloading methods called.
     * This action will be taken when it is release build.
     * @param action The action to be executed on [.wtf] called.
     * @see .setActionOnWtfDebug
     */
    @JvmStatic
    fun setActionOnWtf(action: OnWtfListener?) {
        _ActionOnWtf = action
    }

    /**
     * The extra action to do on method [.wtf]* or overloading methods called.
     * This action will be taken when it is Debug build.
     * @param action The action to be executed on [.wtf] called.
     * @see .setActionOnWtf
     */
    @JvmStatic
    fun setActionOnWtfDebug(action: OnWtfListener?) {
        _ActionOnWtfDebug = action
    }

    /**
     * @param level The log level going to check.
     * @return `true` if the input level is loggable.
     * @see LogLevel
     *
     */
    @JvmStatic
    fun isLoggable(@LogLevel level: Int): Boolean {
        return level != NONE && android.util.Log.isLoggable(_DefaultTag, level)
    }

    /**
     * Append log for prefix with Class name, method name line number and tid in `logBuilder`
     *
     * @param logBuilder - A log message builder.
     */
    @JvmStatic
    private fun getCustomPrefix(logBuilder: StringBuilder) {

        val ste = Throwable().stackTrace

        logBuilder.append("<")
        logBuilder.append(android.os.Process.myTid())
        logBuilder.append(">[(")

        if (null != ste && ste.size >= 4) {

            val stackTraceClass = ste[3]

            val className = stackTraceClass.className
            val subClassIndex = className.indexOf('$')
            val subClass: String? = if (subClassIndex >= 0) {
                className.substring(subClassIndex)
            } else {
                null
            }

            val javaName = stackTraceClass.fileName
            val method = stackTraceClass.methodName
            val line = stackTraceClass.lineNumber

            // Logcat supports lookup to source code with this format
            logBuilder.append(javaName)
                    .append(":")
                    .append(line)
                    .append(")")
            if (subClass != null) {
                logBuilder.append(subClass)
            }
            logBuilder.append("#")
                    .append(method)
        }

        logBuilder.append("] ")
    }

    /**
     * Return string with ClassName.Method Line <tid>, and optionally tag
     *
     * @param msg A log message
     * @return <tag>ClassName.Method Line <tid>
    </tid></tag></tid> */
    @JvmStatic
    private fun getOutputLog(msg: String?): String {

        val sb = StringBuilder()

        getCustomPrefix(sb)

        sb.append(msg)

        return sb.toString()
    }

    /**
     * @param tr The [Throwable] going to be converted
     * @return The Stacktrace string for the input [Throwable]
     */
    @JvmStatic
    fun getStackTraceString(tr: Throwable?): String {
        return android.util.Log.getStackTraceString(tr)
    }

    /**
     * Please put UI related log here.
     *
     * @param msg A log message
     * @return no. of byte wrote to log
     */
    @JvmStatic
    fun v(msg: String?): Int {
        return v(_DefaultTag, getOutputLog(msg))
    }

    /**
     * Please put UI related log here.
     *
     * @param tag A log tag for this Log message.
     * @param msg A log message.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    fun v(tag: String?,
          msg: String?): Int {
        if (msg == null) {
            return 0
        }

        return if (logLevel > VERBOSE) {
            0
        } else printLog(LOGGER_V, tag, msg)

    }

    /**
     * Please put UI related log here. Providing either parameter msg or tr is
     * enough.
     *
     * @param tag A log tag for this Log message.
     * @param msg A log message.
     * @param tr  A [Throwable] of an error.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    fun v(tag: String?,
          msg: String?,
          tr: Throwable?): Int {
        if (msg == null && tr == null) {
            return 0
        }

        return if (logLevel > VERBOSE) {
            0
        } else printLog(LOGGER_V, tag, msg, tr)

    }

    /**
     * @param msg A log message.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    fun d(msg: String?): Int {
        return d(_DefaultTag, getOutputLog(msg))
    }

    /**
     * @param tag A log tag for this Log message.
     * @param msg A log message.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    fun d(tag: String?,
          msg: String?): Int {
        if (msg == null) {
            return 0
        }

        return if (logLevel > DEBUG) {
            0
        } else printLog(LOGGER_D, tag, msg)

    }

    /**
     * Providing either parameter msg or tr is enough.
     *
     * @param tag A log tag for this Log message.
     * @param msg A log message.
     * @param tr A [Throwable] related to this log.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    fun d(tag: String?,
          msg: String?,
          tr: Throwable?): Int {
        if (msg == null && tr == null) {
            return 0
        }

        return if (logLevel > DEBUG) {
            0
        } else printLog(LOGGER_D, tag, msg, tr)

    }

    /**
     * @param msg A log message.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    fun i(msg: String?): Int {
        return i(_DefaultTag, getOutputLog(msg))
    }

    /**
     * @param tag A log tag for this Log message.
     * @param msg A log message.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    fun i(tag: String?,
          msg: String?): Int {
        if (msg == null) {
            return 0
        }

        return if (logLevel > INFO) {
            0
        } else printLog(LOGGER_I, tag, msg)

    }

    /**
     * Providing either parameter msg or tr is enough.
     *
     * @param tag A log tag for this Log message.
     * @param msg A log message.
     * @param tr A [Throwable] related to this log.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    fun i(tag: String?,
          msg: String?,
          tr: Throwable?): Int {
        if (msg == null && tr == null) {
            return 0
        }

        return if (logLevel > INFO) {
            0
        } else printLog(LOGGER_I, tag, msg, tr)

    }

    /**
     * @param msg A log message.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    fun w(msg: String?): Int {
        return w(_DefaultTag, getOutputLog(msg))
    }

    /**
     * @param tag A log tag for this Log message.
     * @param msg A log message.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    fun w(tag: String?,
          msg: String?): Int {
        if (msg == null) {
            return 0
        }

        return if (logLevel > WARN) {
            0
        } else printLog(LOGGER_W, tag, msg)

    }

    /**
     * Providing either parameter msg or tr is enough.
     *
     * @param tag A log tag for this Log message.
     * @param msg A log message.
     * @param tr A [Throwable] related to this log.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    fun w(tag: String?,
          msg: String?,
          tr: Throwable?): Int {
        if (msg == null && tr == null) {
            return 0
        }

        return if (logLevel > WARN) {
            0
        } else printLog(LOGGER_W, tag, msg, tr)

    }

    /**
     * @param msg A log message.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    fun e(msg: String?): Int {
        return e(_DefaultTag, getOutputLog(msg))
    }

    /**
     * @param tag A log tag for this Log message.
     * @param msg A log message.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    fun e(tag: String?,
          msg: String?): Int {
        if (msg == null) {
            return 0
        }

        return if (logLevel > ERROR) {
            0
        } else printLog(LOGGER_E, tag, msg)

    }

    /**
     * @param msg A log message.
     * @param tr A [Throwable] related to this log.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    fun e(msg: String?, tr: Throwable?): Int {
        return e(_DefaultTag, msg, tr)
    }

    /**
     * Providing either parameter msg or tr is enough.
     *
     * @param tag A log tag for this Log message.
     * @param msg A log message.
     * @param tr A [Throwable] related to this log.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    fun e(tag: String?,
          msg: String?,
          tr: Throwable?): Int {
        if (msg == null && tr == null) {
            return 0
        }

        return if (logLevel > ERROR) {
            0
        } else printLog(LOGGER_E, tag, msg, tr)

    }

    /**
     * @param msg A log message.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    fun wtf(msg: String?): Int {
        return wtf(_DefaultTag, getOutputLog(msg))
    }

    /**
     * Providing either parameter msg or tr is enough.
     *
     * @param msg A log message.
     * @param tr A [Throwable] related to this log.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    fun wtf(msg: String?,
            tr: Throwable?): Int {
        return wtf(_DefaultTag, msg, tr)
    }

    /**
     * Providing either parameter msg or tr is enough.
     *
     * @param tag A log tag for this Log message.
     * @param msg A log message.
     * @param tr A [Throwable] related to this log.
     * @return no. of byte wrote to log
     */
    @JvmStatic
    @JvmOverloads
    fun wtf(tag: String?,
            msg: String?,
            tr: Throwable? = null): Int {

        if (msg == null && tr == null) {
            return 0
        }

        if (logLevel > ASSERT) {
            return 0
        }

        // runtime exception if you turn on assert
        if (BuildConfig.DEBUG) {
            _ActionOnWtfDebug?.onWtf(msg, tr)
                    ?: {
                        if (tr == null) {
                            throw RuntimeException(msg)
                        } else {
                            throw RuntimeException(msg, tr)
                        }
                    }.invoke()
        } else {
            _ActionOnWtf?.onWtf(msg, tr)
        }

        return printLog(LOGGER_WTF, tag, msg, tr)
    }

    private fun printLog(logger: Logger,
                         tag: String?,
                         msg: String?,
                         tr: Throwable? = null): Int {

        if (msg != null && msg.length > LOG_MAX_CHAR_CHUNK) {
            var wroteByte = 0
            var chunkCount = 0

            val prefix = chunkPrefix
            var i = 0
            while (i < msg.length) {
                val endPos = i + LOG_MAX_CHAR_CHUNK
                if (endPos <= msg.length) {
                    wroteByte += logger.printLog(tag, chunkCount.toString() + prefix + msg.substring(i, endPos))
                } else {
                    wroteByte += logger.printLog(tag, chunkCount.toString() + prefix + msg.substring(i))
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
