package com.sotwtm.util


import android.support.test.runner.AndroidJUnit4
import org.junit.Assert
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

/**
 * Test case for [Log]
 * @author sheungon
 */
@RunWith(AndroidJUnit4::class)
class LogTest : TestCase() {

    /**
     * Test default log level
     */
    @Test
    @Throws(Exception::class)
    fun testDefaultLogLevel() {

        if (BuildConfig.DEBUG) {
            Assert.assertEquals(Log.logLevel, Log.VERBOSE)
            Assert.assertTrue(Log.isDebuggable)
        } else {
            Assert.assertEquals(Log.logLevel, Log.NONE)
            Assert.assertFalse(Log.isDebuggable)
        }
    }

    /**
     * Test set and get log level
     */
    @Test
    @Throws(Exception::class)
    fun testLogger() {

        val random = Random()
        val logMsg = "This is Log."

        // This priority must be ensure correct
        val allLogLevels = intArrayOf(Log.VERBOSE, Log.DEBUG, Log.INFO, Log.WARN, Log.ERROR, Log.ASSERT, Log.NONE)

        for (i in allLogLevels.indices) {
            @Log.LogLevel
            val logLevel = allLogLevels[i]
            Log.logLevel = logLevel
            Assert.assertEquals(Log.logLevel, logLevel)

            // Check log config

            // Initial expected result
            val testLog = logMsg + ECStringUtil.randomString(random.nextInt(10))
            val exception = Exception(ECStringUtil.randomString(random.nextInt(10)))
            val expectedResult = BooleanArray(allLogLevels.size)
            for (j in i until allLogLevels.size) {
                expectedResult[j] = true
            }

            // The index must be ensure correct
            Assert.assertEquals(expectedResult[0], Log.v(testLog) > 0)
            Assert.assertEquals(expectedResult[0], Log.v(testLog, "") > 0)
            Assert.assertEquals(expectedResult[0], Log.v(testLog, "", exception) > 0)

            Assert.assertEquals(expectedResult[1], Log.d(testLog) > 0)
            Assert.assertEquals(expectedResult[1], Log.d(testLog, "") > 0)
            Assert.assertEquals(expectedResult[1], Log.d(testLog, "", exception) > 0)

            Assert.assertEquals(expectedResult[2], Log.i(testLog) > 0)
            Assert.assertEquals(expectedResult[2], Log.i(testLog, "") > 0)
            Assert.assertEquals(expectedResult[2], Log.i(testLog, "", exception) > 0)

            Assert.assertEquals(expectedResult[3], Log.w(testLog) > 0)
            Assert.assertEquals(expectedResult[3], Log.w(testLog, "") > 0)
            Assert.assertEquals(expectedResult[3], Log.w(testLog, "", exception) > 0)

            Assert.assertEquals(expectedResult[4], Log.e(testLog) > 0)
            Assert.assertEquals(expectedResult[4], Log.e(testLog, "") > 0)
            Assert.assertEquals(expectedResult[4], Log.e(testLog, exception) > 0)
            Assert.assertEquals(expectedResult[4], Log.e(testLog, "", exception) > 0)

            try {
                Assert.assertEquals(expectedResult[5], Log.wtf(testLog) > 0)
                if (!BuildConfig.DEBUG) {
                    Assert.assertTrue(true)
                }
            } catch (e: Exception) {
                Assert.assertTrue(BuildConfig.DEBUG)
            }

            try {
                Assert.assertEquals(expectedResult[5], Log.wtf(testLog, "") > 0)
                if (!BuildConfig.DEBUG) {
                    Assert.assertTrue(true)
                }
            } catch (e: Exception) {
                Assert.assertTrue(BuildConfig.DEBUG)
            }

            try {
                Assert.assertEquals(expectedResult[5], Log.wtf(testLog, exception) > 0)
                if (!BuildConfig.DEBUG) {
                    Assert.assertTrue(true)
                }
            } catch (e: Exception) {
                Assert.assertTrue(BuildConfig.DEBUG)
            }

            try {
                Assert.assertEquals(expectedResult[5], Log.wtf(testLog, "", exception) > 0)
                if (!BuildConfig.DEBUG) {
                    Assert.assertTrue(true)
                }
            } catch (e: Exception) {
                Assert.assertTrue(BuildConfig.DEBUG)
            }

            // Check debuggable value
            if (logLevel == Log.NONE) {
                Assert.assertFalse(Log.isDebuggable)
            } else {
                Assert.assertTrue(Log.isDebuggable)
            }
        }

        // Test illegal log level
        val originalLogLevel = Log.logLevel
        try {

            Log.logLevel = 101
            Assert.assertFalse(true)
        } catch (e: IllegalArgumentException) {
            Assert.assertTrue(true)
        }

        Assert.assertEquals(Log.logLevel, originalLogLevel)
    }

    @Test
    @Throws(Exception::class)
    fun testGetStackTraceString() {
        Assert.assertNotNull(Log.getStackTraceString(Throwable("")))
        Assert.assertNotNull(Log.getStackTraceString(Exception("Exception")))
    }
}
