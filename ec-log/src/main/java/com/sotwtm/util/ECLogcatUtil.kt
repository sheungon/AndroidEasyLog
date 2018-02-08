package com.sotwtm.util

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

/**
 * A class to run logcat to record app's log.
 * The logcat process will be running still until the app is force stopped or device restarted.
 *
 * @author sheungon
 */
class ECLogcatUtil private constructor(context: Context) {

    private val mContextRef: WeakReference<Context> = WeakReference(context.applicationContext)

    /**
     * Start a logcat process and log the log to `logFile`.
     * Only one concurrent logcat process will be created even call this method multiple times.
     *
     * @return `true` if a logcat process created successfully or a logcat process already running before.
     * @throws NullPointerException if the logcat path is not set by [.setLogcatDest] yet.
     * @see .startLogcat
     */
    @Synchronized
    fun startLogcat(): Boolean {
        return startLogcat(true)
    }

    /**
     * Start a logcat process and log the log to `logFile`.
     * Only one concurrent logcat process will be created even call this method multiple times.
     *
     * @param clearPreviousLog `true` to clear the destination log file. Otherwise, new log is appended to the end of the file.
     * @return `true` if a logcat process created successfully or a logcat process already running before.
     * @throws NullPointerException if the logcat path is not set by [.setLogcatDest] yet.
     */
    @Synchronized
    fun startLogcat(clearPreviousLog: Boolean): Boolean {

        val context = mContextRef.get()
        if (context == null) {
            Log.e("No Context!")
            return false
        }

        val username = getAppRunByUser(context)
        if (username == null) {
            // Not starting logcat in this case to avoid repeatedly start many logcat process
            Log.w(LOG_TAG, "Cannot start logcat due to app user is unknown.")
            return false
        }
        Log.v(LOG_TAG, "App running by : " + username)

        if (isLogcatRunningBy(username)) {
            Log.v(LOG_TAG, "logcat running already")
            return true
        }

        val sharedPreferences = getSharedPreferences(context)

        val logcatPath : String? = sharedPreferences.getString(PREF_KEY_LOGCAT_PATH, null)
        if (TextUtils.isEmpty(logcatPath)) {
            throw NullPointerException("Logcat path is not set yet!")
        }

        if (clearPreviousLog) {
            val oldLog = File(logcatPath)
            val deletedOldLog = !oldLog.isFile || oldLog.delete()
            if (deletedOldLog) {
                Log.d(LOG_TAG, "Deleted old log.")
            } else {
                Log.e(LOG_TAG, "Error on delete old log.")
            }
        }

        val commandBuilder = StringBuilder(COMMAND_LOGCAT)
        commandBuilder
                .append(COMMAND_SEPARATOR).append("-f").append(COMMAND_SEPARATOR).append(logcatPath)
                .append(COMMAND_SEPARATOR).append("-r").append(COMMAND_SEPARATOR).append(sharedPreferences.getInt(PREF_KEY_LOGCAT_FILE_MAX_SIZE, DEFAULT_LOGCAT_FILE_SIZE))
                .append(COMMAND_SEPARATOR).append("-n").append(COMMAND_SEPARATOR).append(sharedPreferences.getInt(PREF_KEY_LOGCAT_MAX_LOG_FILE, DEFAULT_LOGCAT_MAX_NO_OF_LOG_FILES))
                .append(COMMAND_SEPARATOR).append("-v").append(COMMAND_SEPARATOR).append(sharedPreferences.getString(PREF_KEY_LOGCAT_FORMAT, DEFAULT_LOGCAT_FORMAT.toString()))

        val logcatSince = sharedPreferences.getString(PREF_KEY_LOGCAT_SINCE, null)
        if (logcatSince != null) {
            commandBuilder.append(COMMAND_SEPARATOR).append("-T").append(COMMAND_SEPARATOR).append("0")
        }

        val filterTag = sharedPreferences.getString(PREF_KEY_LOGCAT_FILTER_LOG_TAG, null)
        if (filterTag != null) {
            // Filter all logs by the log tag
            commandBuilder.append(COMMAND_SEPARATOR).append("*:S").append(COMMAND_SEPARATOR).append(filterTag)
        }

        val processParams = commandBuilder.toString().split(COMMAND_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        // Run logcat here
        val processBuilder = ProcessBuilder(*processParams)
        processBuilder.redirectErrorStream(true)
        try {
            processBuilder.start()
            Log.v(LOG_TAG, "Started logcat")
            return isLogcatRunningBy(username)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error on starting logcat", e)
        }

        return false
    }

    /**
     * Stop any running logcat instance
     *
     * @return `true` if a logcat can be stopped by this. Or no logcat process was running.
     */
    fun stopLogcat(): Boolean {

        val context = mContextRef.get()
        if (context == null) {
            Log.e("No Context!")
            return false
        }

        val username = getAppRunByUser(context)
        if (username == null) {
            Log.e(LOG_TAG, "Cannot get ps user!")
            return false
        }

        val pid = getLogcatPIDRunningBy(username) ?: return true

        val processBuilder = ProcessBuilder("kill", pid)
        Log.d(Arrays.toString(processBuilder.command().toTypedArray()))
        try {
            val process = processBuilder.start()
            process.waitFor()
            val exitCode = process.exitValue()
            Log.v(LOG_TAG, "Stopped logcat exit code : " + exitCode)
            return true
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error on kill logcat", e)
        }

        return false
    }

    /**
     * Reset logcat to log down all logs again.
     *
     * @return `true` if a logcat process has been recreated.
     * @see .clearLogcat
     */
    fun resetLogcat(): Boolean {

        val context = mContextRef.get()
        if (context == null) {
            Log.e("No Context!")
            return false
        }


        val logcatStopped = stopLogcat()
        Log.d("Logcat stopped : " + logcatStopped)

        getEditor(context).remove(PREF_KEY_LOGCAT_SINCE).apply()
        Log.d("Reset logcat")

        return startLogcat()
    }

    /**
     * Reset and start to print log since now only
     *
     * @return `true` if a logcat process has been recreated.
     * @see .resetLogcat
     */
    fun clearLogcat(): Boolean {

        val context = mContextRef.get()
        if (context == null) {
            Log.e("No Context!")
            return false
        }

        val logcatSince = LOGCAT_SINCE_FORMAT.format(Date())

        val logcatStopped = stopLogcat()
        Log.d("Logcat stopped : " + logcatStopped)

        getEditor(context).putString(PREF_KEY_LOGCAT_SINCE, logcatSince).apply()
        Log.d("Clear logcat since : " + logcatSince)

        return startLogcat()
    }

    /**
     * Set the maximum size of each logcat file.
     *
     * @param logcatFileMaxSize Size in KB
     * @see .setMaxLogFile
     */
    fun setLogcatFileMaxSize(logcatFileMaxSize: Int) {
        val context = mContextRef.get() ?: return
        getEditor(context).putInt(PREF_KEY_LOGCAT_FILE_MAX_SIZE, logcatFileMaxSize).apply()
    }

    /**
     * Set the format of logcat.
     *
     * @param logFormat Possible values are [LogFormat]
     */
    fun setLogcatFormat(logFormat: LogFormat) {
        val context = mContextRef.get() ?: return
        getEditor(context).putString(PREF_KEY_LOGCAT_FORMAT, logFormat.toString()).apply()
    }

    /**
     * Set the num of log file will be created if a log file excesses the max size
     *
     * @param maxLogFile The maximum number of log file will be created before overwriting the first log file.
     * @see .setLogcatFileMaxSize
     */
    fun setMaxLogFile(maxLogFile: Int) {
        val context = mContextRef.get() ?: return
        getEditor(context).putInt(PREF_KEY_LOGCAT_MAX_LOG_FILE, maxLogFile).apply()
    }

    /**
     * Set logcat should be filtered by the given log tag.
     *
     * @param filterLogTag The log tag. `null` means filtered by nothing.
     */
    fun setFilterLogTag(filterLogTag: String?) {
        val context = mContextRef.get() ?: return
        val editor = getEditor(context)
        if (TextUtils.isEmpty(filterLogTag)) {
            editor.remove(PREF_KEY_LOGCAT_FILTER_LOG_TAG)
        } else {
            editor.putString(PREF_KEY_LOGCAT_FILTER_LOG_TAG, filterLogTag)
        }
        editor.apply()
    }

    /**
     * Set the destination the logcat should save to.
     *
     * @param file The file indicate the path to save the logcat (e.g. /sdcard/log.txt)
     */
    fun setLogcatDest(file: File) {
        val context = mContextRef.get() ?: return
        getEditor(context).putString(PREF_KEY_LOGCAT_PATH, file.absolutePath).apply()
    }


    ////////////////////////////////
    // Class
    ////////////////////////////////
    /**
     * The log format for command `logcat`
     */
    enum class LogFormat {
        Brief,
        Process,
        Tag,
        Thread,
        Raw,
        Time,
        ThreadTime,
        Long;

        override fun toString(): String {
            return name.toLowerCase()
        }
    }

    companion object {

        const val LOG_TAG = "ECLogcatUtil"
        const val SHARED_PREF_FILE_KEY = "LogcatPref"

        val DEFAULT_LOGCAT_FORMAT = LogFormat.Time
        const val DEFAULT_LOGCAT_FILE_SIZE = 256 // KB
        const val DEFAULT_LOGCAT_MAX_NO_OF_LOG_FILES = 1

        private const val COMMAND_PS = "ps"
        private const val COMMAND_LOGCAT = "logcat"
        private const val COMMAND_SEPARATOR = "\n\r"

        private const val PREF_KEY_APP_LINUX_USER_NAME = "AppLinuxUserName"
        private const val PREF_KEY_LOGCAT_SINCE = "LogcatSince"
        private const val PREF_KEY_LOGCAT_FILE_MAX_SIZE = "LogcatFileMaxSize"
        private const val PREF_KEY_LOGCAT_FORMAT = "LogcatFormat"
        private const val PREF_KEY_LOGCAT_MAX_LOG_FILE = "LogcatMaxLogFile"
        private const val PREF_KEY_LOGCAT_FILTER_LOG_TAG = "LogcatFilterLogTag"
        private const val PREF_KEY_LOGCAT_PATH = "LogcatPath"

        internal val LOGCAT_SINCE_FORMAT = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

        private const val REGEX_COLUMN_SEPARATOR = "(\\s+[A-Z]?\\s+|\\s+)"

        private const val PS_COL_USER = "USER"
        private const val PS_COL_PID = "PID"
        private const val PS_COL_NAME = "NAME"

        @Volatile
        private var instance: ECLogcatUtil? = null

        @Synchronized
        fun getInstance(context: Context): ECLogcatUtil {

            var localInstance = instance
            if (localInstance?.mContextRef?.get() == null) {
                localInstance = ECLogcatUtil(context)
                instance = localInstance
            }

            return localInstance
        }

        /**
         * @return The app executed by which Linux user.
         */
        fun getAppRunByUser(context: Context): String? {

            val sharedPreferences = getSharedPreferences(context)
            var myUserName = sharedPreferences.getString(PREF_KEY_APP_LINUX_USER_NAME, null)

            if (TextUtils.isEmpty(myUserName)) {
                val packageName = context.packageName
                Log.d(LOG_TAG, "Retrieving application username. ApplicationPackage = " + packageName)

                /*Don't user `grep` as it could be not available on some devices.*/
                // Execute `ps`
                val psBuilder = ProcessBuilder(COMMAND_PS)
                val ps: Process
                try {
                    ps = psBuilder.start()
                } catch (e: IOException) {
                    Log.e(LOG_TAG, "Not able to run command on this device!", e)
                    return null
                }

                // Read the output
                val `is` = ps.inputStream
                val bf = BufferedReader(InputStreamReader(`is`))
                try {

                    Log.d(LOG_TAG, "======`ps` output start======")

                    // Read the first line and find the target column
                    var line: String? = bf.readLine()
                    if (line == null) {
                        Log.e(LOG_TAG, "'ps' no output?!")
                        return null
                    }
                    Log.d(LOG_TAG, line)

                    // Split by space
                    var columns = line.split(REGEX_COLUMN_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    var userColumn = -1
                    var nameColumn = -1
                    for (i in columns.indices) {
                        when {
                            PS_COL_USER.equals(columns[i], ignoreCase = true) -> userColumn = i
                            PS_COL_NAME.equals(columns[i], ignoreCase = true) -> nameColumn = i
                        }
                    }
                    if (userColumn == -1 || nameColumn == -1) {
                        Log.e(LOG_TAG, "Some column cannot be found from output.")
                        return null
                    }

                    line = bf.readLine()
                    while (line != null) {
                        Log.d(LOG_TAG, line)
                        // Split by space
                        columns = line.split(REGEX_COLUMN_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                        if (packageName == columns[nameColumn]) {
                            myUserName = columns[userColumn]
                            Log.d(LOG_TAG, "Application executed by user : " + myUserName)
                            break
                        }
                        line = bf.readLine()
                    }
                    Log.d(LOG_TAG, "======`ps` output end======")

                    if (TextUtils.isEmpty(myUserName)) {
                        Log.e(LOG_TAG, "Cannot find the owner of current app...")
                    } else {
                        // Cache the user name in preference as it remind the same since installed
                        getEditor(context).putString(PREF_KEY_APP_LINUX_USER_NAME, myUserName).apply()
                    }
                } catch (e: IOException) {
                    Log.e(LOG_TAG, "Error on reading output from 'ps'", e)
                } finally {
                    try {
                        `is`.close()
                    } catch (e: IOException) {
                        // Don't care
                    }

                    try {
                        ps.waitFor()
                        ps.exitValue()
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "Error on destroy ps", e)
                    }

                }
            }

            return myUserName
        }

        fun getLogcatPIDRunningBy(user: String): String? {

            var pid: String? = null

            /*Don't user `grep` as it could be not available on some devices.*/
            // Execute `ps logcat` to find all logcat process
            val processBuilder = ProcessBuilder(COMMAND_PS)
            val ps: Process
            try {
                ps = processBuilder.start()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "Not able to run command on this device!", e)
                return null
            }

            // Read the output
            val `is` = ps.inputStream
            val bf = BufferedReader(InputStreamReader(`is`))
            try {

                Log.d(LOG_TAG, "======`ps` look for logcat output start======")

                // Read the first line and find the target column
                var line: String? = bf.readLine()
                if (line == null) {
                    Log.e(LOG_TAG, "'ps' no output?!")
                    return null
                }
                Log.d(LOG_TAG, line)

                // Split by space
                var columns = line.split(REGEX_COLUMN_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                var userColumn = -1
                var pidColumn = -1
                var nameColumn = -1
                for (i in columns.indices) {
                    when {
                        PS_COL_USER.equals(columns[i], ignoreCase = true) -> userColumn = i
                        PS_COL_PID.equals(columns[i], ignoreCase = true) -> pidColumn = i
                        PS_COL_NAME.equals(columns[i], ignoreCase = true) -> nameColumn = i
                    }
                }
                if (userColumn == -1 ||
                        pidColumn == -1 ||
                        nameColumn == -1) {
                    Log.e(LOG_TAG, "Some column cannot be found from output.")
                    return null
                }

                line = bf.readLine()
                while (line != null) {
                    Log.d(LOG_TAG, line)
                    // Split by space
                    columns = line.split(REGEX_COLUMN_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                    if (columns[nameColumn].toLowerCase().contains(COMMAND_LOGCAT) && user == columns[userColumn]) {
                        // Found the current user's process
                        pid = columns[pidColumn]
                        Log.v(LOG_TAG, "Logcat is running by user [$user] pid : $pid")
                    }

                    line = bf.readLine()
                }
                Log.d(LOG_TAG, "======`ps` look for logcat output end======")
            } catch (e: IOException) {
                Log.e(LOG_TAG, "Error on reading output from 'ps'", e)
            } finally {
                try {
                    `is`.close()
                } catch (e: IOException) {
                    // Don't care
                }

                try {
                    ps.waitFor()
                    ps.exitValue()
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Error on destroy ps", e)
                }

            }

            return pid
        }

        fun isLogcatRunningBy(user: String): Boolean {
            return getLogcatPIDRunningBy(user) != null
        }

        private fun getEditor(context: Context): SharedPreferences.Editor {
            val sharedPref = context.getSharedPreferences(SHARED_PREF_FILE_KEY, Context.MODE_PRIVATE)
            return sharedPref.edit()
        }

        private fun getSharedPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(SHARED_PREF_FILE_KEY, Context.MODE_PRIVATE)
        }
    }
}
