package com.sotwtm.log.sample;

import android.app.Application;
import android.os.Environment;
import android.support.annotation.NonNull;

import com.sotwtm.util.ECLogcatUtil;
import com.sotwtm.util.Log;

import java.io.File;

/**
 * Created by sheungon on 2017-01-20.
 */

public class ApplicationClass extends Application {

    private static ApplicationClass _instance;
    private File mLogFile;

    @NonNull
    public static ApplicationClass getInstance() {
        return _instance;
    }

    @Override
    public void onCreate() {
        _instance = this;

        super.onCreate();

        Log.setLogLevel(Log.VERBOSE);

        // Initialize logcat export location
        mLogFile = new File(Environment.getExternalStorageDirectory().getPath(), "log.txt");
        ECLogcatUtil.getInstance(this).setLogcatDest(mLogFile);
    }

    public File getLogFile() {
        return mLogFile;
    }
}
