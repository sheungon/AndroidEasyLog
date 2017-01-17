package com.sotwtm.util;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * A logger that support enable/disable.
 * Created by johntsai on 14/7/15.
 *
 * @author John
 */
@SuppressWarnings("unused")
public final class Log {

    /**Possible values for setting log level in {@link #setLogLevel(int)}*/
    @IntDef({VERBOSE, DEBUG, INFO, WARN, ERROR, ASSERT, NONE, UNKNOWN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LogLevel {}

    /**
     * Level to enable all logs
     * @see android.util.Log#VERBOSE
     * */
    public static final int VERBOSE = android.util.Log.VERBOSE;
    /**
     * @see android.util.Log#DEBUG
     * */
    public static final int DEBUG = android.util.Log.DEBUG;
    /**
     * @see android.util.Log#INFO
     * */
    public static final int INFO = android.util.Log.INFO;
    /**
     * @see android.util.Log#WARN
     * */
    public static final int WARN = android.util.Log.WARN;
    /**
     * @see android.util.Log#ERROR
     * */
    public static final int ERROR = android.util.Log.ERROR;
    /**
     * @see android.util.Log#ASSERT
     * */
    public static final int ASSERT = android.util.Log.ASSERT;
    /**
     * Level to disable all log usually in live version app
     */
    public static final int NONE = 100;
    /**Internal use*/
    static final int UNKNOWN = -1;

    private static final int LOG_MAX_CHAR_CHUNK = 2048;

    private static final Logger LOGGER_V = new Logger() {
        @Override
        public int printLog(@Nullable String tag, @Nullable String msg) {
            return android.util.Log.v(tag, msg);
        }
        @Override
        public int printLog(@Nullable String tag, @Nullable String msg, @Nullable Throwable tr) {
            return android.util.Log.v(tag, msg, tr);
        }
    };
    private static final Logger LOGGER_D = new Logger() {
        @Override
        public int printLog(@Nullable String tag, @Nullable String msg) {
            return android.util.Log.d(tag, msg);
        }
        @Override
        public int printLog(@Nullable String tag, @Nullable String msg, @Nullable Throwable tr) {
            return android.util.Log.d(tag, msg, tr);
        }
    };
    private static final Logger LOGGER_I = new Logger() {
        @Override
        public int printLog(@Nullable String tag, @Nullable String msg) {
            return android.util.Log.i(tag, msg);
        }
        @Override
        public int printLog(@Nullable String tag, @Nullable String msg, @Nullable Throwable tr) {
            return android.util.Log.i(tag, msg, tr);
        }
    };
    private static final Logger LOGGER_W = new Logger() {
        @Override
        public int printLog(@Nullable String tag, @Nullable String msg) {
            return android.util.Log.w(tag, msg);
        }
        @Override
        public int printLog(@Nullable String tag, @Nullable String msg, @Nullable Throwable tr) {
            return android.util.Log.w(tag, msg, tr);
        }
    };
    private static final Logger LOGGER_E = new Logger() {
        @Override
        public int printLog(@Nullable String tag, @Nullable String msg) {
            return android.util.Log.e(tag, msg);
        }
        @Override
        public int printLog(@Nullable String tag, @Nullable String msg, @Nullable Throwable tr) {
            return android.util.Log.e(tag, msg, tr);
        }
    };
    private static final Logger LOGGER_WTF = new Logger() {
        @Override
        public int printLog(@Nullable String tag, @Nullable String msg) {
            return android.util.Log.wtf(tag, msg);
        }
        @Override
        public int printLog(@Nullable String tag, @Nullable String msg, @Nullable Throwable tr) {
            return android.util.Log.wtf(tag, msg, tr);
        }
    };

    @LogLevel
    static int _logLevel = UNKNOWN;

    @NonNull
    static String _DefaultTag = "Log";
    @Nullable
    static OnWtfListener _ActionOnWtf = null;
    @Nullable
    static OnWtfListener _ActionOnWtfDebug = null;


    public static void setDefaultLogTag(@NonNull String logTag) {
        _DefaultTag = logTag;
    }

    /**
     * The extra action to do on method {@link #wtf(String)}* or overloading methods called.
     * This action will be taken when it is release build.
     * @see #setActionOnWtfDebug(OnWtfListener)
     * */
    public static void setActionOnWtf(@Nullable OnWtfListener action) {
        _ActionOnWtf = action;
    }

    /**
     * The extra action to do on method {@link #wtf(String)}* or overloading methods called.
     * This action will be taken when it is Debug build.
     * @see #setActionOnWtf(OnWtfListener)
     * */
    public static void setActionOnWtfDebug(@Nullable OnWtfListener action) {
        _ActionOnWtfDebug = action;
    }

    public static void setLogLevel(@LogLevel int lLevel) {
        switch (lLevel) {
            case VERBOSE:
            case DEBUG:
            case INFO:
            case WARN:
            case ERROR:
            case ASSERT:
            case NONE:
                _logLevel = lLevel;
                break;

            case UNKNOWN:
                // Keep it as original one. Shouldn't be here anyway
                _logLevel = getLogLevel();
                break;

            default:
                throw new IllegalArgumentException("Invalid log level passed to setLogLevel: " + lLevel);
        }
    }

    /**
     * Hidden constructor
     */
    private Log() {
    }

    public static int getLogLevel() {
        if (_logLevel == UNKNOWN) {
            return BuildConfig.DEBUG ? VERBOSE : NONE;
        }
        return _logLevel;
    }

    public static boolean isLoggable(int level) {
        return level != NONE
                && android.util.Log.isLoggable(_DefaultTag, level);
    }

    /**
     * Append log for prefix with Class name, method name line number and tid in {@code logBuilder}
     *
     * @param logBuilder - A log message builder.
     */
    private static void getCustomPrefix(StringBuilder logBuilder) {

        StackTraceElement[] ste = new Throwable().getStackTrace();

        logBuilder.append("<");
        logBuilder.append(android.os.Process.myTid());
        logBuilder.append(">[(");

        if (null != ste && ste.length >= 4) {

            StackTraceElement stackTraceClass = ste[3];

            String subClass;
            String className = stackTraceClass.getClassName();
            int subClassIndex = className.indexOf('$');
            if (subClassIndex >= 0) {
                subClass = className.substring(subClassIndex);
            } else {
                subClass = null;
            }

            String javaName = stackTraceClass.getFileName();
            String method = stackTraceClass.getMethodName();
            int line = stackTraceClass.getLineNumber();

            // Logcat supports lookup to source code with this format
            logBuilder.append(javaName)
                    .append(":")
                    .append(line)
                    .append(")");
            if (subClass != null) {
                logBuilder.append(subClass);
            }
            logBuilder.append("#")
                    .append(method);
        }

        logBuilder.append("] ");
    }

    /**
     * Return string with ClassName.Method Line <tid>, and optionally tag
     *
     * @param msg - A log message
     * @return <tag>ClassName.Method Line <tid>
     */
    @NonNull
    private static String getOutputLog(@Nullable String msg) {

        StringBuilder sb = new StringBuilder();

        getCustomPrefix(sb);

        sb.append(msg);

        return sb.toString();
    }

    public static String getStackTraceString(Throwable tr) {
        return android.util.Log.getStackTraceString(tr);
    }

    /**
     * Please put UI related log here.
     *
     * @param msg - A log message
     * @return no. of byte wrote to log
     */
    public static int v(@Nullable String msg) {
        return v(_DefaultTag, getOutputLog(msg));
    }

    /**
     * Please put UI related log here.
     *
     * @param tag - A log tag for this Log message.
     * @param msg - A log message.
     * @return no. of byte wrote to log
     */
    public static int v(@Nullable String tag,
                        @Nullable String msg) {
        if (msg == null) {
            return 0;
        }

        if (getLogLevel() > VERBOSE) {
            return 0;
        }

        return printLog(LOGGER_V, tag, msg);
    }

    /**
     * Please put UI related log here. Providing either parameter msg or tr is
     * enough.
     *
     * @param tag - A log tag for this Log message.
     * @param msg - A log message.
     * @param tr  - A {@link Throwable} of an error.
     * @return no. of byte wrote to log
     */
    public static int v(@Nullable String tag,
                        @Nullable String msg,
                        @Nullable Throwable tr) {
        if (msg == null && tr == null) {
            return 0;
        }

        if (getLogLevel() > VERBOSE) {
            return 0;
        }

        return printLog(LOGGER_V, tag, msg, tr);
    }

    /**
     * Please do not put UI related log here.
     *
     * @return no. of byte wrote to log
     */
    public static int d(@Nullable String msg) {
        return d(_DefaultTag, getOutputLog(msg));
    }

    /**
     * Please do not put UI related log here.
     *
     * @return no. of byte wrote to log
     */
    public static int d(@Nullable String tag,
                        @Nullable String msg) {
        if (msg == null) {
            return 0;
        }

        if (getLogLevel() > DEBUG) {
            return 0;
        }

        return printLog(LOGGER_D, tag, msg);
    }

    /**
     * Please do not put UI related log here.
     * <p/>
     * Providing either parameter msg or tr is enough.
     *
     * @return no. of byte wrote to log
     */
    public static int d(@Nullable String tag,
                        @Nullable String msg,
                        @Nullable Throwable tr) {
        if (msg == null && tr == null) {
            return 0;
        }

        if (getLogLevel() > DEBUG) {
            return 0;
        }

        return printLog(LOGGER_D, tag, msg, tr);
    }

    /**
     * Please do not put UI related log here.
     *
     * @return no. of byte wrote to log
     */
    public static int i(@Nullable String msg) {
        return i(_DefaultTag, getOutputLog(msg));
    }

    /**
     * Please do not put UI related log here.
     *
     * @return no. of byte wrote to log
     */
    public static int i(@Nullable String tag,
                        @Nullable String msg) {
        if (msg == null) {
            return 0;
        }

        if (getLogLevel() > INFO) {
            return 0;
        }

        return printLog(LOGGER_I, tag, msg);
    }

    /**
     * Please do not put UI related log here.
     * <p/>
     * Providing either parameter msg or tr is enough.
     *
     * @return no. of byte wrote to log
     */
    public static int i(@Nullable String tag,
                        @Nullable String msg,
                        @Nullable Throwable tr) {
        if (msg == null && tr == null) {
            return 0;
        }

        if (getLogLevel() > INFO) {
            return 0;
        }

        return printLog(LOGGER_I, tag, msg, tr);
    }

    /**
     * Please do not put UI related log here.
     *
     * @return no. of byte wrote to log
     */
    public static int w(@Nullable String msg) {
        return w(_DefaultTag, getOutputLog(msg));
    }

    /**
     * Please do not put UI related log here.
     *
     * @return no. of byte wrote to log
     */
    public static int w(@Nullable String tag,
                        @Nullable String msg) {
        if (msg == null) {
            return 0;
        }

        if (getLogLevel() > WARN) {
            return 0;
        }

        return printLog(LOGGER_W, tag, msg);
    }

    /**
     * Please do not put UI related log here.
     * <p/>
     * Providing either parameter msg or tr is enough.
     *
     * @return no. of byte wrote to log
     */
    public static int w(@Nullable String tag,
                        @Nullable String msg,
                        @Nullable Throwable tr) {
        if (msg == null && tr == null) {
            return 0;
        }

        if (getLogLevel() > WARN) {
            return 0;
        }

        return printLog(LOGGER_W, tag, msg, tr);
    }

    /**
     * Please do not put UI related log here.
     *
     * @return no. of byte wrote to log
     */
    public static int e(@Nullable String msg) {
        return e(_DefaultTag, getOutputLog(msg));
    }

    /**
     * Please do not put UI related log here.
     *
     * @return no. of byte wrote to log
     */
    public static int e(@Nullable String tag,
                        @Nullable String msg) {
        if (msg == null) {
            return 0;
        }

        if (getLogLevel() > ERROR) {
            return 0;
        }

        return printLog(LOGGER_E, tag, msg);
    }

    /**
     * Please do not put UI related log here.
     *
     * @return no. of byte wrote to log
     */
    public static int e(@Nullable String msg, @Nullable Throwable tr) {
        return e(_DefaultTag, msg, tr);
    }

    /**
     * Please do not put UI related log here.
     * <p/>
     * Providing either parameter msg or tr is enough.
     *
     * @return no. of byte wrote to log
     */
    public static int e(@Nullable String tag,
                        @Nullable String msg,
                        @Nullable Throwable tr) {
        if (msg == null && tr == null) {
            return 0;
        }

        if (getLogLevel() > ERROR) {
            return 0;
        }

        return printLog(LOGGER_E, tag, msg, tr);
    }

    /**
     * Please do not put UI related log here.
     *
     * @return no. of byte wrote to log
     */
    public static int wtf(@Nullable String msg) {
        return wtf(_DefaultTag, getOutputLog(msg));
    }

    /**
     * Please do not put UI related log here.
     *
     * @return no. of byte wrote to log
     */
    public static int wtf(@Nullable String tag,
                          @Nullable String msg) {
        return wtf(tag, msg, null);
    }

    /**
     * Please do not put UI related log here.
     * <p/>
     * Providing either parameter msg or tr is enough.
     *
     * @return no. of byte wrote to log
     */
    public static int wtf(@Nullable String msg,
                          @Nullable Throwable tr) {
        return wtf(_DefaultTag, msg, tr);
    }

    /**
     * Please do not put UI related log here.
     * <p/>
     * Providing either parameter msg or tr is enough.
     *
     * @return no. of byte wrote to log
     */
    public static int wtf(@Nullable String tag,
                          @Nullable String msg,
                          @Nullable Throwable tr) {

        if (msg == null && tr == null) {
            return 0;
        }

        if (getLogLevel() > ASSERT) {
            return 0;
        }

        // runtime exception if you turn on assert
        if (BuildConfig.DEBUG) {
            if (_ActionOnWtfDebug != null) {
                _ActionOnWtfDebug.onWtf(msg, tr);
            } else if (tr == null) {
                throw new RuntimeException(msg);
            } else {
                throw new RuntimeException(msg, tr);
            }
        } else {
            if (_ActionOnWtf != null) {
                _ActionOnWtf.onWtf(msg, tr);
            }
        }

        return printLog(LOGGER_WTF, tag, msg, tr);
    }

    /**
     * It will check the debug level.
     *
     * @return true on log level other than NONE
     */
    public static boolean isDebuggable() {
        return getLogLevel() != NONE;
    }

    private static String getChunkPrefix() {

        return "<" + android.os.Process.myTid() + ">";
    }

    private static int printLog(@NonNull Logger logger,
                                @Nullable String tag,
                                @Nullable String msg) {

        return printLog(logger, tag, msg, null);
    }

    private static int printLog(@NonNull Logger logger,
                                @Nullable String tag,
                                @Nullable String msg,
                                @Nullable Throwable tr) {

        if (msg != null &&
                msg.length() > LOG_MAX_CHAR_CHUNK) {
            int wroteByte = 0;
            int chunkCount = 0;

            String prefix = getChunkPrefix();
            for (int i = 0; i < msg.length(); i += LOG_MAX_CHAR_CHUNK) {
                int endPos = i + LOG_MAX_CHAR_CHUNK;
                if (endPos <= msg.length()) {
                    wroteByte += logger.printLog(tag, chunkCount+prefix+msg.substring(i, endPos));
                } else {
                    wroteByte += logger.printLog(tag, chunkCount+prefix+msg.substring(i));
                }
                chunkCount++;
            }

            if (tr != null) {
                wroteByte += logger.printLog(tag, "", tr);
            }

            return wroteByte;
        }

        return logger.printLog(tag, msg, tr);
    }


    //////////////////////////////////////////
    // Class and interface
    //////////////////////////////////////////
    public interface OnWtfListener {
        void onWtf(@Nullable String msg, @Nullable Throwable tr);
    }

    private interface Logger {
        int printLog(@Nullable String tag, @Nullable String msg);
        int printLog(@Nullable String tag, @Nullable String msg, @Nullable Throwable tr);
    }
}
