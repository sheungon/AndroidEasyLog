package com.sotwtm.log.sample

import android.app.Application
import android.os.Environment

import com.sotwtm.util.ECLogcatUtil
import com.sotwtm.util.Log

import java.io.File

/**
 * Created by sheungon on 2017-01-20.
 * @author sheungon
 */

class ApplicationClass : Application() {

    lateinit var logFile: File
        private set

    override fun onCreate() {
        instance = this

        super.onCreate()

        Log.logLevel = Log.VERBOSE

        // Initialize logcat export location
        logFile = File(Environment.getExternalStorageDirectory().path, "android_ec_log.txt")
        ECLogcatUtil.getInstance(this).setLogcatDest(logFile)
    }

    companion object {

        lateinit var instance: ApplicationClass
            private set
    }
}
