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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * A class to run log cat so record current app's log
 * @author John
 */
public class LogcatUtil {

    private static final String SHARED_PREF_FILE_KEY = "LogcatPref";

    public static final int DEFAULT_LOGCAT_FILE_SIZE = 256; // KB
    public static final LogFormat DEFAULT_LOGCAT_FORMAT = LogFormat.Time;
    public static final int DEFAULT_LOGCAT_MAX_NO_OF_LOG_FILES = 1;

    private static final String LOG_TAG = "LogcatUtil";

    private static final String PREF_KEY_APP_LINUX_USER_NAME = "AppLinuxUserName";
    private static final String PREF_KEY_LOGCAT_SINCE = "LogcatSince";

    private static final String LOGCAT_BEGIN_OF_TIME = "01-01 00:00:00.000";

    private static final SimpleDateFormat LOGCAT_SINCE_FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US);

    private static final String REGEX_COLUMN_SEPARATOR = "(\\s+[A-Z]?\\s+|\\s+)";

    private static final String PS_COL_USER = "USER";
    private static final String PS_COL_PID = "PID";
    private static final String PS_COL_NAME = "NAME";

    private static int _logcatFileMaxSize = DEFAULT_LOGCAT_FILE_SIZE;
    @NonNull
    private static LogFormat _logFormat = DEFAULT_LOGCAT_FORMAT;
    private static int _maxLogFile = DEFAULT_LOGCAT_MAX_NO_OF_LOG_FILES;
    @Nullable
    private static String _filterLogTag = null;

    /**
     * Start a logcat process and log the log to {@code logFile}.
     * Only one concurrent logcat process will be created even call this method multiple times.
     * @param logFile The designation of the log file.
     *
     * @return {@code true} if a logcat process created successfully or a logcat process already running before.
     * */
    public static synchronized boolean startLogcatAt(@NonNull Context context, @NonNull File logFile) {

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

        StringBuilder commandBuilder = new StringBuilder("logcat");
        commandBuilder.append(" -f ").append(logFile.getAbsolutePath())
                .append(" -r ").append(_logcatFileMaxSize)
                .append(" -n ").append(_maxLogFile)
                .append(" -v ").append(_logFormat.toString());

        String logcatSince = sharedPreferences.getString(PREF_KEY_LOGCAT_SINCE, null);
        if (logcatSince != null) {
            commandBuilder.append(" -T ").append(logcatSince);
        }

        if (_filterLogTag != null) {
            commandBuilder.append("*:S").append(_filterLogTag);
        }

        String[] processParams = commandBuilder.toString().split(" ");

        // Run logcat here
        ProcessBuilder processBuilder = new ProcessBuilder(processParams);
        processBuilder.redirectErrorStream(true);
        try {
            processBuilder.start();
            Log.v(LOG_TAG, "Started logcat");
            return true;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error on starting logcat", e);
        }

        return false;
    }

    /**
     * Stop any running logcat instance
     * @return {@code true} if a logcat can be stopped by this. Or no logcat process was running.
     * */
    public static boolean stopLogcat(@NonNull Context context) {

        String username = getAppRunByUser(context);
        if (username == null) {
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
     * Reset and start to print log since now only
     *
     * @param logFile The designation of the log file.
     *
     * @return {@code true} if a logcat process created successfully or a logcat process already running before.
     * */
    public static boolean resetLogcat(@NonNull Context context,
                                      @NonNull File logFile) {

        String logcatSince = LOGCAT_SINCE_FORMAT.format(new Date());

        boolean logcatStopped = stopLogcat(context);
        Log.d("Logcat stopped : " + logcatStopped);

        getEditor(context).putString(PREF_KEY_LOGCAT_SINCE, logcatSince).apply();
        Log.d("Reset logcat since : " + logcatSince);

        return startLogcatAt(context, logFile);
    }

    public static void setLogcatFileMaxSize(int logcatFileMaxSize) {
        _logcatFileMaxSize = logcatFileMaxSize;
    }

    public static void setLogFormat(@NonNull LogFormat logFormat) {
        _logFormat = logFormat;
    }

    public static void setMaxLogFile(int maxLogFile) {
        _maxLogFile = maxLogFile;
    }

    public static void setFilterLogTag(@Nullable String filterLogTag) {
        _filterLogTag = filterLogTag;
    }

    /**
     * @return The app executed by which Linux user.
     * */
    @Nullable
    private static String getAppRunByUser(@NonNull Context context) {

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
    private static String getLogcatPIDRunningBy(@NonNull String user) {

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

    private static boolean isLogcatRunningBy(@NonNull String user) {

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
