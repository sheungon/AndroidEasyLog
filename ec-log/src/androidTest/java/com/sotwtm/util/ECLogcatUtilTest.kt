package com.sotwtm.util

import android.app.Application
import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import junit.framework.TestCase
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Test case for [ECLogcatUtil]
 * @author sheungon
 */
@RunWith(AndroidJUnit4::class)
class ECLogcatUtilTest : TestCase() {

    /**
     * Clean up SharedPreferences before test
     * */
    @Before
    @Throws(Exception::class)
    fun clearSettings() {
        val context = InstrumentationRegistry.getTargetContext()
        context.getSharedPreferences(ECLogcatUtil.SHARED_PREF_FILE_KEY, Context.MODE_PRIVATE).edit().clear().apply()
    }

    /**
     * Test case for [ECLogcatUtil] to check if it works normally.
     * */
    @Test
    @Throws(Exception::class)
    fun testLogcat() {

        val application = InstrumentationRegistry.getTargetContext().applicationContext as Application
        val logcatUtil = ECLogcatUtil.getInstance(application)

        Assert.assertNotNull(logcatUtil)

        try {
            logcatUtil.resetLogcat()
            Assert.assertTrue(false)
        } catch (e: NullPointerException) {
            Assert.assertTrue(true)
        }

        try {
            logcatUtil.startLogcat()
            Assert.assertTrue(false)
        } catch (e: NullPointerException) {
            Assert.assertTrue(true)
        }

        try {
            logcatUtil.clearLogcat()
            Assert.assertTrue(false)
        } catch (e: NullPointerException) {
            Assert.assertTrue(true)
        }

        val appRunByUser = ECLogcatUtil.getAppRunByUser(application)
        if (appRunByUser == null) {
            Assert.assertNotNull(appRunByUser)
            return
        }
        Assert.assertFalse(appRunByUser.isEmpty())
        Assert.assertNull(ECLogcatUtil.getLogcatPIDRunBy(appRunByUser))
        Assert.assertFalse(ECLogcatUtil.isLogcatRunningBy(appRunByUser))

        val logFile = File(application.cacheDir, "log.txt")
        Assert.assertTrue(!logFile.isFile || logFile.delete())
        logcatUtil.setLogcatDest(logFile)
        logcatUtil.setFilterLogTag(TAG)

        Assert.assertTrue(logcatUtil.startLogcat())
        Log.d(TAG, "This is a test log.")
        Thread.sleep(100)

        Assert.assertNotNull(ECLogcatUtil.getLogcatPIDRunBy(appRunByUser))
        Assert.assertTrue(ECLogcatUtil.isLogcatRunningBy(appRunByUser))

        Assert.assertTrue(logFile.isFile)
        Assert.assertTrue(logcatUtil.stopLogcat())
        Assert.assertFalse(ECLogcatUtil.isLogcatRunningBy(appRunByUser))
    }

    companion object {
        internal const val TAG = "ECLogcatUtilTest"
    }
}