package com.sotwtm.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * A class to run logcat to record app's log.
 * The logcat process will be running still until the app is force stopped or device restarted.
 *
 * @author sheungon
 */
public class ECLogcatUtil {

    private static final String COMMAND_SEPARATOR = "\n\r";
    static final String SHARED_PREF_FILE_KEY = "LogcatPref";

    public static final int DEFAULT_LOGCAT_FILE_SIZE = 256; // KB
    public static final LogFormat DEFAULT_LOGCAT_FORMAT = LogFormat.Time;
    public static final int DEFAULT_LOGCAT_MAX_NO_OF_LOG_FILES = 1;

    static final String LOG_TAG = "ECLogcatUtil";

    static final String PREF_KEY_APP_LINUX_USER_NAME = "AppLinuxUserName";
    static final String PREF_KEY_LOGCAT_SINCE = "LogcatSince";
    static final String PREF_KEY_LOGCAT_FILE_MAX_SIZE = "LogcatFileMaxSize";
    static final String PREF_KEY_LOGCAT_FORMAT = "LogcatFormat";
    static final String PREF_KEY_LOGCAT_MAX_LOG_FILE = "LogcatMaxLogFile";
    static final String PREF_KEY_LOGCAT_FILTER_LOG_TAG = "LogcatFilterLogTag";
    static final String PREF_KEY_LOGCAT_PATH = "LogcatPath";

    static final SimpleDateFormat LOGCAT_SINCE_FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US);

    static final String REGEX_COLUMN_SEPARATOR = "(\\s+[A-Z]?\\s+|\\s+)";

    static final String PS_COL_USER = "USER";
    static final String PS_COL_PID = "PID";
    static final String PS_COL_NAME = "NAME";

    private static volatile ECLogcatUtil _instance;

    private final WeakReference<Context> mContextRef;

    @NonNull
    public static synchronized ECLogcatUtil getInstance(@NonNull Context context) {

        if (_instance == null ||
                _instance.mContextRef.get() == null) {
            _instance = new ECLogcatUtil(context);
        }

        return _instance;
    }

    private ECLogcatUtil(@NonNull Context context) {
        mContextRef = new WeakReference<>(context.getApplicationContext());
    }

    /**
     * Start a logcat process and log the log to {@code logFile}.
     * Only one concurrent logcat process will be created even call this method multiple times.
     *
     * @return {@code true} if a logcat process created successfully or a logcat process already running before.
     * @throws NullPointerException if the logcat path is not set by {@link #setLogcatDest(File)} yet.
     * @see #startLogcat(boolean)
     * */
    public synchronized boolean startLogcat() {
        return startLogcat(true);
    }

    /**
     * Start a logcat process and log the log to {@code logFile}.
     * Only one concurrent logcat process will be created even call this method multiple times.
     *
     * @param clearPreviousLog {@code true} to clear the destination log file. Otherwise, new log is appended to the end of the file.
     * @return {@code true} if a logcat process created successfully or a logcat process already running before.
     * @throws NullPointerException if the logcat path is not set by {@link #setLogcatDest(File)} yet.
     * */
    public synchronized boolean startLogcat(boolean clearPreviousLog) {

        Context context = mContextRef.get();
        if (context == null) {
            Log.e("No Context!!!");
            return false;
        }

        String username = getAppRunByUser(context);
        if (username == null) {
            // Not starting logcat in this case to avoid repeatedly start many logcat process
            Log.w(LOG_TAG, "Cannot start logcat due to app user is unknown.");
            return false;
        }
        Log.v(LOG_TAG, "App running by : " + username);

        if (isLogcatRunningBy(username)) {
            Log.v(LOG_TAG, "logcat running already");
            return true;
        }

        SharedPreferences sharedPreferences = getSharedPreferences(context);

        String logcatPath = sharedPreferences.getString(PREF_KEY_LOGCAT_PATH, null);
        if (TextUtils.isEmpty(logcatPath)) {
            throw new NullPointerException("Logcat path is not set yet!!!");
        }

        if (clearPreviousLog) {
            File oldLog = new File(logcatPath);
            boolean deletedOldLog = !oldLog.isFile() || oldLog.delete();
            if (deletedOldLog) {
                Log.d(LOG_TAG, "Deleted old log.");
            } else {
                Log.e(LOG_TAG, "Error on delete old log.");
            }
        }

        StringBuilder commandBuilder = new StringBuilder("logcat");
        commandBuilder
                .append(COMMAND_SEPARATOR).append("-f").append(COMMAND_SEPARATOR).append(logcatPath)
                .append(COMMAND_SEPARATOR).append("-r").append(COMMAND_SEPARATOR).append(sharedPreferences.getInt(PREF_KEY_LOGCAT_FILE_MAX_SIZE, DEFAULT_LOGCAT_FILE_SIZE))
                .append(COMMAND_SEPARATOR).append("-n").append(COMMAND_SEPARATOR).append(sharedPreferences.getInt(PREF_KEY_LOGCAT_MAX_LOG_FILE, DEFAULT_LOGCAT_MAX_NO_OF_LOG_FILES))
                .append(COMMAND_SEPARATOR).append("-v").append(COMMAND_SEPARATOR).append(sharedPreferences.getString(PREF_KEY_LOGCAT_FORMAT, DEFAULT_LOGCAT_FORMAT.toString()));

        String logcatSince = sharedPreferences.getString(PREF_KEY_LOGCAT_SINCE, null);
        if (logcatSince != null) {
            commandBuilder.append(COMMAND_SEPARATOR).append("-T").append(COMMAND_SEPARATOR).append("0");
        }

        String filterTag = sharedPreferences.getString(PREF_KEY_LOGCAT_FILTER_LOG_TAG, null);
        if (filterTag != null) {
            // Filter all logs by the log tag
            commandBuilder.append(COMMAND_SEPARATOR).append("*:S").append(COMMAND_SEPARATOR).append(filterTag);
        }

        String[] processParams = commandBuilder.toString().split(COMMAND_SEPARATOR);

        // Run logcat here
        ProcessBuilder processBuilder = new ProcessBuilder(processParams);
        processBuilder.redirectErrorStream(true);
        try {
            processBuilder.start();
            Log.v(LOG_TAG, "Started logcat");
            return isLogcatRunningBy(username);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error on starting logcat", e);
        }

        return false;
    }

    /**
     * Stop any running logcat instance
     *
     * @return {@code true} if a logcat can be stopped by this. Or no logcat process was running.
     * */
    public boolean stopLogcat() {

        Context context = mContextRef.get();
        if (context == null) {
            Log.e("No Context!!!");
            return false;
        }

        String username = getAppRunByUser(context);
        if (username == null) {
            Log.e(LOG_TAG, "Cannot get ps user!!!");
            return false;
        }

        String pid = getLogcatPIDRunningBy(username);
        if (pid == null) {
            return true;
        }

        ProcessBuilder processBuilder = new ProcessBuilder("kill", pid);
        Log.d(Arrays.toString(processBuilder.command().toArray()));
        try {
            Process process = processBuilder.start();
            process.waitFor();
            int exitCode = process.exitValue();
            Log.v(LOG_TAG, "Stopped logcat exit code : " + exitCode);
            return true;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error on kill logcat", e);
        }

        return false;
    }

    /**
     * Reset logcat to log down all logs again.
     *
     * @return {@code true} if a logcat process has been recreated.
     * @see #clearLogcat()
     * */
    public boolean resetLogcat() {

        Context context = mContextRef.get();
        if (context == null) {
            Log.e("No Context!!!");
            return false;
        }


        boolean logcatStopped = stopLogcat();
        Log.d("Logcat stopped : " + logcatStopped);

        getEditor(context).remove(PREF_KEY_LOGCAT_SINCE).apply();
        Log.d("Reset logcat");

        return startLogcat();
    }

    /**
     * Reset and start to print log since now only
     *
     * @return {@code true} if a logcat process has been recreated.
     * @see #resetLogcat()
     * */
    public boolean clearLogcat() {

        Context context = mContextRef.get();
        if (context == null) {
            Log.e("No Context!!!");
            return false;
        }

        String logcatSince = LOGCAT_SINCE_FORMAT.format(new Date());

        boolean logcatStopped = stopLogcat();
        Log.d("Logcat stopped : " + logcatStopped);

        getEditor(context).putString(PREF_KEY_LOGCAT_SINCE, logcatSince).apply();
        Log.d("Clear logcat since : " + logcatSince);

        return startLogcat();
    }

    /**
     * Set the maximum size of each logcat file.
     *
     * @param logcatFileMaxSize Size in KB
     * @see #setMaxLogFile(int)
     * */
    public void setLogcatFileMaxSize(int logcatFileMaxSize) {
        Context context = mContextRef.get();
        if (context == null) {
            return;
        }
        getEditor(context).putInt(PREF_KEY_LOGCAT_FILE_MAX_SIZE, logcatFileMaxSize).apply();
    }

    /**
     * Set the format of logcat.
     *
     * @param logFormat Possible values are {@link LogFormat}
     * */
    public void setLogcatFormat(@NonNull LogFormat logFormat) {
        Context context = mContextRef.get();
        if (context == null) {
            return;
        }
        getEditor(context).putString(PREF_KEY_LOGCAT_FORMAT, logFormat.toString()).apply();
    }

    /**
     * Set the num of log file will be created if a log file excesses the max size
     *
     * @param maxLogFile The maximum number of log file will be created before overwriting the first log file.
     * @see #setLogcatFileMaxSize(int)
     * */
    public void setMaxLogFile(int maxLogFile) {
        Context context = mContextRef.get();
        if (context == null) {
            return;
        }
        getEditor(context).putInt(PREF_KEY_LOGCAT_MAX_LOG_FILE, maxLogFile).apply();
    }

    /**
     * Set logcat should be filtered by the given log tag.
     *
     * @param filterLogTag The log tag. {@code null} means filtered by nothing.
     * */
    public void setFilterLogTag(@Nullable String filterLogTag) {
        Context context = mContextRef.get();
        if (context == null) {
            return;
        }
        SharedPreferences.Editor editor = getEditor(context);
        if (TextUtils.isEmpty(filterLogTag)) {
            editor.remove(PREF_KEY_LOGCAT_FILTER_LOG_TAG);
        } else {
            editor.putString(PREF_KEY_LOGCAT_FILTER_LOG_TAG, filterLogTag);
        }
        editor.apply();
    }

    /**
     * Set the destination the logcat should save to.
     *
     * @param file The file indicate the path to save the logcat (e.g. /sdcard/log.txt)
     * */
    public void setLogcatDest(@NonNull File file) {
        Context context = mContextRef.get();
        if (context == null) {
            return;
        }
        getEditor(context).putString(PREF_KEY_LOGCAT_PATH, file.getAbsolutePath()).apply();
    }

    /**
     * @return The app executed by which Linux user.
     * */
    @Nullable
    static String getAppRunByUser(@NonNull Context context) {

        SharedPreferences sharedPreferences = getSharedPreferences(context);
        String myUserName = sharedPreferences.getString(PREF_KEY_APP_LINUX_USER_NAME, null);

        if (TextUtils.isEmpty(myUserName)) {
            String packageName = context.getPackageName();
            Log.d(LOG_TAG, "Retrieving application username. ApplicationPackage = " + packageName);

            /*Don't user `grep` as it could be not available on some devices.*/
            // Execute `ps`
            ProcessBuilder psBuilder = new ProcessBuilder("ps");
            Process ps;
            try {
                ps = psBuilder.start();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Not able to run command on this device!!!", e);
                return null;
            }

            // Read the output
            InputStream is = ps.getInputStream();
            BufferedReader bf = new BufferedReader(new InputStreamReader(is));
            try {

                Log.d(LOG_TAG, "======`ps` output start======");

                // Read the first line and find the target column
                String line = bf.readLine();
                if (line == null) {
                    Log.e(LOG_TAG, "'ps' no output?!");
                    return null;
                }
                Log.d(LOG_TAG, line);

                // Split by space
                String[] columns = line.split(REGEX_COLUMN_SEPARATOR);
                int userColumn = -1;
                int nameColumn = -1;
                for (int i = 0; i < columns.length; i++) {
                    if (PS_COL_USER.equalsIgnoreCase(columns[i])) {
                        userColumn = i;
                    } else if (PS_COL_NAME.equalsIgnoreCase(columns[i])) {
                        nameColumn = i;
                    }
                }
                if (userColumn == -1 ||
                        nameColumn == -1) {
                    Log.e(LOG_TAG, "Some column cannot be found from output.");
                    return null;
                }

                while ((line = bf.readLine()) != null) {
                    Log.d(LOG_TAG, line);
                    // Split by space
                    columns = line.split(REGEX_COLUMN_SEPARATOR);

                    if (packageName.equals(columns[nameColumn])) {
                        myUserName = columns[userColumn];
                        Log.d(LOG_TAG, "Application executed by user : " + myUserName);
                        break;
                    }
                }
                Log.d(LOG_TAG, "======`ps` output end======");

                if (TextUtils.isEmpty(myUserName)) {
                    Log.e(LOG_TAG, "Cannot find the owner of current app...");
                } else {
                    // Cache the user name in preference as it remind the same since installed
                    getEditor(context).putString(PREF_KEY_APP_LINUX_USER_NAME, myUserName).apply();
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error on reading output from 'ps'", e);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    // Don't care
                }
                try {
                    ps.waitFor();
                    ps.exitValue();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error on destroy ps", e);
                }
            }
        }

        return myUserName;
    }

    @Nullable
    static String getLogcatPIDRunningBy(@NonNull String user) {

        String pid = null;

        /*Don't user `grep` as it could be not available on some devices.*/
        // Execute `ps logcat` to find all logcat process
        ProcessBuilder processBuilder = new ProcessBuilder("ps", "logcat");
        Process ps;
        try {
            ps = processBuilder.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Not able to run command on this device!!!", e);
            return null;
        }

        // Read the output
        InputStream is = ps.getInputStream();
        BufferedReader bf = new BufferedReader(new InputStreamReader(is));
        try {

            Log.d(LOG_TAG, "======`ps logcat` output start======");

            // Read the first line and find the target column
            String line = bf.readLine();
            if (line == null) {
                Log.e(LOG_TAG, "'ps' no output?!");
                return null;
            }
            Log.d(LOG_TAG, line);

            // Split by space
            String[] columns = line.split(REGEX_COLUMN_SEPARATOR);
            int userColumn = -1;
            int pidColumn = -1;
            for (int i = 0; i < columns.length; i++) {
                if (PS_COL_USER.equalsIgnoreCase(columns[i])) {
                    userColumn = i;
                } else if (PS_COL_PID.equalsIgnoreCase(columns[i])) {
                    pidColumn = i;
                }
            }
            if (userColumn == -1 ||
                    pidColumn == -1) {
                Log.e(LOG_TAG, "Some column cannot be found from output.");
                return null;
            }

            while ((line = bf.readLine()) != null) {
                Log.d(LOG_TAG, line);
                // Split by space
                columns = line.split(REGEX_COLUMN_SEPARATOR);

                if (user.equals(columns[userColumn])) {
                    // Found the current user's process
                    pid = columns[pidColumn];
                    Log.v(LOG_TAG, "Logcat is already running by user [" + user + "] pid : " + pid);
                }
            }
            Log.d(LOG_TAG, "======`ps logcat` output end======");
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error on reading output from 'ps'", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // Don't care
            }
            try {
                ps.waitFor();
                ps.exitValue();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error on destroy ps", e);
            }
        }

        return pid;
    }

    static boolean isLogcatRunningBy(@NonNull String user) {
        return getLogcatPIDRunningBy(user) != null;
    }

    @NonNull
    private static SharedPreferences.Editor getEditor(@NonNull Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(SHARED_PREF_FILE_KEY, Context.MODE_PRIVATE);
        return sharedPref.edit();
    }

    @NonNull
    private static SharedPreferences getSharedPreferences(@NonNull Context context) {
        return context.getSharedPreferences(SHARED_PREF_FILE_KEY, Context.MODE_PRIVATE);
    }


    ////////////////////////////////
    // Class
    ////////////////////////////////
    /**
     * The log format for command {@code logcat}
     * */
    public enum LogFormat {
        Brief,
        Process,
        Tag,
        Thread,
        Raw,
        Time,
        ThreadTime,
        Long;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }
}
