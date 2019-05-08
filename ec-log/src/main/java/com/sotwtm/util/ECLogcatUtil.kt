package com.sotwtm.util

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

/**
 * A class to run logcat to record app's log.
 * The logcat process will be running still until the app is force stopped or device restarted.
 *
 * @author sheungon
 */
class ECLogcatUtil private constructor(private val application: Application) {

    /**
     * Start a logcat process and log the log to `logFile`.
     * Only one concurrent logcat process will be created even call this method multiple times.
     *
     * @return true if a logcat process created successfully or a logcat process already running before.
     * @throws NullPointerException if the logcat path is not set by [setLogcatDest] yet.
     * @see stopLogcat
     */
    @Synchronized
    fun startLogcat(): Boolean {
        return startLogcat(true)
    }

    /**
     * Start a logcat process and log the log to `logFile`.
     * Only one concurrent logcat process will be created even call this method multiple times.
     *
     * @param clearPreviousLog true to clear the destination log file. Otherwise, new log is appended to the end of the file.
     * @return true if a logcat process created successfully or a logcat process already running before.
     * @throws NullPointerException if the logcat path is not set by [setLogcatDest] yet.
     * @see stopLogcat
     */
    @Synchronized
    fun startLogcat(clearPreviousLog: Boolean): Boolean {

        val username = getAppRunByUser(application) ?: run {
            // Not starting logcat in this case to avoid repeatedly start many logcat process
            Log.w(LOG_TAG, "Cannot start logcat due to app user is unknown.")
            return false
        }
        Log.v(LOG_TAG, "App running by : $username")

        if (isLogcatRunningBy(username)) {
            Log.v(LOG_TAG, "logcat running already")
            return true
        }

        val sharedPreferences = getSharedPreferences(application)

        val logcatPath: String? = sharedPreferences.getString(PREF_KEY_LOGCAT_PATH, null) ?: run {
            throw NullPointerException("Logcat path is not set yet!")
        }

        if (clearPreviousLog) {
            deleteOldLog(logcatPath)
        }

        val processParams = createProcessParams(logcatPath, sharedPreferences)

        // Run logcat here
        val processBuilder = ProcessBuilder(processParams)
        processBuilder.redirectErrorStream(true)

        return try {
            processBuilder.start()
            Log.v(LOG_TAG, "Started logcat")
            isLogcatRunningBy(username)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error on starting logcat", e)
            false
        }
    }

    /**
     * Stop any running logcat instance
     *
     * @return true if a logcat can be stopped by this. Or no logcat process was running.
     */
    fun stopLogcat(): Boolean {

        val username = getAppRunByUser(application)
        if (username == null) {
            Log.e(LOG_TAG, "Cannot get ps user!")
            return false
        }

        val pid = getLogcatPIDRunBy(username) ?: return true

        val processBuilder = ProcessBuilder("kill", pid)
        Log.d(Arrays.toString(processBuilder.command().toTypedArray()))
        try {
            val process = processBuilder.start()
            process.waitFor()
            val exitCode = process.exitValue()
            Log.v(LOG_TAG, "Stopped logcat exit code : $exitCode")
            return true
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error on kill logcat", e)
        }

        return false
    }

    /**
     * Reset logcat to log down all logs again.
     *
     * @return true if a logcat process has been recreated.
     * @see clearLogcat
     */
    fun resetLogcat(): Boolean {

        val logcatStopped = stopLogcat()
        Log.d("Logcat stopped : $logcatStopped")

        getEditor(application).remove(PREF_KEY_LOGCAT_SINCE).apply()
        Log.d("Reset logcat")

        return startLogcat()
    }

    /**
     * Clear and start to print log since now only
     *
     * @return true if a logcat process has been recreated.
     * @see resetLogcat
     */
    fun clearLogcat(): Boolean {

        val logcatSince = LOGCAT_SINCE_FORMAT.format(Date())

        val logcatStopped = stopLogcat()
        Log.d("Logcat stopped : $logcatStopped")

        getEditor(application).putString(PREF_KEY_LOGCAT_SINCE, logcatSince).apply()
        Log.d("Clear logcat since : $logcatSince")

        return startLogcat()
    }

    /**
     * Set the maximum size of each logcat file.
     *
     * @param logcatFileMaxSize Size in KB
     * @see setMaxLogFile
     */
    fun setLogcatFileMaxSize(logcatFileMaxSize: Int) {
        getEditor(application).putInt(PREF_KEY_LOGCAT_FILE_MAX_SIZE, logcatFileMaxSize).apply()
    }

    /**
     * Set the format of logcat.
     *
     * @param logFormat Possible values are [LogFormat]
     */
    fun setLogcatFormat(logFormat: LogFormat) {
        getEditor(application).putString(PREF_KEY_LOGCAT_FORMAT, logFormat.toString()).apply()
    }

    /**
     * Set the num of log file will be created if a log file excesses the max size
     *
     * @param maxLogFile The maximum number of log file will be created before overwriting the first log file.
     * @see setLogcatFileMaxSize
     */
    fun setMaxLogFile(maxLogFile: Int) {
        getEditor(application).putInt(PREF_KEY_LOGCAT_MAX_LOG_FILE, maxLogFile).apply()
    }

    /**
     * Set logcat should be filtered by the given log tag.
     *
     * @param filterLogTag The log tag. `null` means filtered by nothing.
     */
    fun setFilterLogTag(filterLogTag: String?) {
        val editor = getEditor(application)
        if (filterLogTag?.isEmpty() == true) {
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
        getEditor(application).putString(PREF_KEY_LOGCAT_PATH, file.absolutePath).apply()
    }

    private fun createProcessParams(logcatPath: String?, sharedPreferences: SharedPreferences): List<String> {
        val commandBuilder = StringBuilder(COMMAND_LOGCAT)
        commandBuilder
            .append(COMMAND_SEPARATOR).append("-f").append(COMMAND_SEPARATOR).append(logcatPath)
            .append(COMMAND_SEPARATOR).append("-r").append(COMMAND_SEPARATOR)
            .append(sharedPreferences.getInt(PREF_KEY_LOGCAT_FILE_MAX_SIZE, DEFAULT_LOGCAT_FILE_SIZE))
            .append(COMMAND_SEPARATOR).append("-n").append(COMMAND_SEPARATOR)
            .append(sharedPreferences.getInt(PREF_KEY_LOGCAT_MAX_LOG_FILE, DEFAULT_LOGCAT_MAX_NO_OF_LOG_FILES))
            .append(COMMAND_SEPARATOR).append("-v").append(COMMAND_SEPARATOR)
            .append(sharedPreferences.getString(PREF_KEY_LOGCAT_FORMAT, DEFAULT_LOGCAT_FORMAT.toString()))

        val logcatSince = sharedPreferences.getString(PREF_KEY_LOGCAT_SINCE, null)
        if (logcatSince != null) {
            commandBuilder.append(COMMAND_SEPARATOR).append("-T").append(COMMAND_SEPARATOR).append("0")
        }

        val filterTag = sharedPreferences.getString(PREF_KEY_LOGCAT_FILTER_LOG_TAG, null)
        if (filterTag != null) {
            // Filter all logs by the log tag
            commandBuilder.append(COMMAND_SEPARATOR).append("*:S").append(COMMAND_SEPARATOR).append(filterTag)
        }
        return commandBuilder.toString().split(COMMAND_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }.toList()
    }

    private fun deleteOldLog(logcatPath: String?) {
        val oldLog = File(logcatPath)
        val deletedOldLog = !oldLog.isFile || oldLog.delete()
        if (deletedOldLog) {
            Log.d(LOG_TAG, "Deleted old log.")
        } else {
            Log.e(LOG_TAG, "Error on delete old log.")
        }
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

        /**The log TAG used by this Util*/
        const val LOG_TAG = "ECLogcatUtil"
        /**The SharedPreferences Key of this Util*/
        const val SHARED_PREF_FILE_KEY = "LogcatPref"

        /**The default logcat format used by this Util*/
        @JvmStatic
        val DEFAULT_LOGCAT_FORMAT = LogFormat.Time
        /**The default largest file size of a logcat file before split to another log file*/
        const val DEFAULT_LOGCAT_FILE_SIZE = 256 // KB
        /**
         * The default maximum log file the logcat process will create.
         * If the max number has been reached, the oldest log will be override.
         * */
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

        @JvmStatic
        internal val LOGCAT_SINCE_FORMAT = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

        private const val REGEX_COLUMN_SEPARATOR = "(\\s+[A-Z]?\\s+|\\s+)"

        private const val PS_COL_USER = "USER"
        private const val PS_COL_PID = "PID"
        private const val PS_COL_NAME = "NAME"

        @Volatile
        @JvmStatic
        private var instance: ECLogcatUtil? = null

        /**
         * @param application Application of the app
         * @return The same instance if there is an instance created before
         * */
        @Synchronized
        @JvmStatic
        fun getInstance(application: Application): ECLogcatUtil =
            instance ?: ECLogcatUtil(application).apply {
                instance = this
            }

        /**
         * @param context For SharedPreferences access
         * @return The app executed by which Linux user.
         */
        @JvmStatic
        fun getAppRunByUser(context: Context): String? =
            getSharedPreferences(context).getString(PREF_KEY_APP_LINUX_USER_NAME, null)
                ?.takeIf { !it.isEmpty() }
                ?: findAppRunByUser(context)
                    ?.takeIf { !it.isEmpty() }
                    ?.apply {
                        // Cache the user name in preference as it remind the same since installed
                        getEditor(context).putString(PREF_KEY_APP_LINUX_USER_NAME, this).apply()
                    }
                ?: run {
                    Log.e(LOG_TAG, "Cannot find the owner of current app...")
                    null
                }

        /**
         * @param user The Linux user get from `ps` command
         * @return The process ID of `logcat` command run by user
         * */
        @JvmStatic
        fun getLogcatPIDRunBy(user: String): String? {

            val ps: Process = runPSCommand() ?: return null

            // Read the output
            val inputStream = ps.inputStream
            val bf = BufferedReader(InputStreamReader(inputStream))
            try {
                Log.d(LOG_TAG, "======`ps` look for logcat output start======")

                // Read the first line and find the target column
                var line: String? = bf.readLine()
                Log.d(LOG_TAG, line)

                // Split by space and form a column name to index map
                val columnIndexMap = line?.toColumnIndexMap()
                val userColumn = columnIndexMap?.get(PS_COL_USER)
                val pidColumn = columnIndexMap?.get(PS_COL_PID)
                val nameColumn = columnIndexMap?.get(PS_COL_NAME)
                if (userColumn == null ||
                    pidColumn == null ||
                    nameColumn == null
                ) {
                    Log.e(LOG_TAG, "Some column cannot be found from output.")
                    return null
                }

                while (true) {
                    line = bf.readLine() ?: break
                    Log.d(LOG_TAG, line)
                    // Split by space
                    val columns = line.split(REGEX_COLUMN_SEPARATOR.toRegex())
                        .dropLastWhile { it.isEmpty() }

                    if (columns[nameColumn].contains(COMMAND_LOGCAT, true) &&
                        user == columns[userColumn]
                    ) {
                        // Found the current user's process
                        return columns[pidColumn].apply {
                            Log.v(LOG_TAG, "Logcat is running by user [$user] pid : $this")
                        }
                    }
                }
                Log.d(LOG_TAG, "======`ps` look for logcat output end======")
            } catch (e: IOException) {
                Log.e(LOG_TAG, "Error on reading output from 'ps'", e)
            } finally {
                try {
                    inputStream.close()
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

            return null
        }

        /**
         * @param user The Linux user get from `ps` command
         * @return true if logcat process is already running by user
         * */
        @JvmStatic
        fun isLogcatRunningBy(user: String): Boolean {
            return getLogcatPIDRunBy(user) != null
        }

        @JvmStatic
        private fun getEditor(context: Context): SharedPreferences.Editor {
            val sharedPref = context.getSharedPreferences(SHARED_PREF_FILE_KEY, Context.MODE_PRIVATE)
            return sharedPref.edit()
        }

        @JvmStatic
        private fun getSharedPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(SHARED_PREF_FILE_KEY, Context.MODE_PRIVATE)
        }

        @JvmStatic
        private fun findAppRunByUser(context: Context): String? {

            val packageName = context.packageName
            Log.d(LOG_TAG, "Retrieving application username. ApplicationPackage = $packageName")

            val ps = runPSCommand() ?: return null

            // Read the output from the command
            val inputStream = ps.inputStream
            val bf = BufferedReader(InputStreamReader(inputStream))
            try {
                Log.d(LOG_TAG, "======`ps` output start======")

                // Read the first line and find the target column
                var line: String? = bf.readLine()
                Log.d(LOG_TAG, line)

                // Split by space and form a column name to index map
                val columnIndexMap = line.toColumnIndexMap()
                val userColumn = columnIndexMap?.get(PS_COL_USER) ?: return run {
                    Log.e(LOG_TAG, "$PS_COL_USER cannot be found from output.")
                    null
                }
                val nameColumn = columnIndexMap[PS_COL_NAME] ?: return run {
                    Log.e(LOG_TAG, "$PS_COL_NAME cannot be found from output.")
                    null
                }

                while (true) {
                    line = bf.readLine() ?: break
                    Log.d(LOG_TAG, line)
                    // Split by space
                    val columns = line.split(REGEX_COLUMN_SEPARATOR.toRegex())
                        .dropLastWhile { it.isEmpty() }

                    if (packageName == columns.getOrNull(nameColumn)) {
                        return columns.getOrNull(userColumn)?.apply {
                            Log.i(LOG_TAG, "Application executed by user : $this")
                        }
                    }
                }

            } catch (e: IOException) {
                Log.e(LOG_TAG, "Error on reading output from 'ps'", e)
            } finally {
                Log.d(LOG_TAG, "======`ps` output end======")
                try {
                    inputStream.close()
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
            return null
        }

        private fun String?.toColumnIndexMap(): Map<String, Int>? {
            var i = 0
            return this?.split(REGEX_COLUMN_SEPARATOR.toRegex())
                ?.dropLastWhile { it.isEmpty() }
                ?.associateWith { i++ }
        }

        @JvmStatic
        private fun runPSCommand(): Process? = try {
            /* Don't user `grep` as this command could be not available on some devices. */
            // Execute `ps`
            ProcessBuilder(COMMAND_PS).start()
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Not able to run command on this device!", e)
            null
        }
    }
}
