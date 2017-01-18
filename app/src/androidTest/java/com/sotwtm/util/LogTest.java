package com.sotwtm.util;


import android.support.test.runner.AndroidJUnit4;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

/**
 * Test case for {@link Log}
 * @author sheungon
 */
@RunWith(AndroidJUnit4.class)
public class LogTest extends TestCase {

    /**
     * Test default log level
     * */
    @Test
    public void testDefaultLogLevel() throws Exception {

        if (BuildConfig.DEBUG) {
            assertEquals(Log.getLogLevel(), Log.VERBOSE);
            assertTrue(Log.isDebuggable());
        } else {
            assertEquals(Log.getLogLevel(), Log.NONE);
            assertFalse(Log.isDebuggable());
        }
    }

    /**
     * Test set and get log level
     * */
    @Test
    public void testLogger() throws Exception {

        Random random = new Random();
        String logMsg = "This is Log.";

        // This priority must be ensure correct
        int[] allLogLevels = new int[] {
                Log.VERBOSE,
                Log.DEBUG,
                Log.INFO,
                Log.WARN,
                Log.ERROR,
                Log.ASSERT,
                Log.NONE,
        };

        for (int i = 0; i < allLogLevels.length; i++) {
            @Log.LogLevel
            int logLevel = allLogLevels[i];
            Log.setLogLevel(logLevel);
            assertEquals(Log.getLogLevel(), logLevel);

            // Check log config

            // Initial expected result
            String testLog = logMsg + ECStringUtil.randomString(random.nextInt(10));
            Exception exception = new Exception(ECStringUtil.randomString(random.nextInt(10)));
            boolean[] expectedResult = new boolean[allLogLevels.length];
            for (int j=i; j<allLogLevels.length; j++) {
                expectedResult[j] = true;
            }

            // The index must be ensure correct
            assertEquals(expectedResult[0], Log.v(testLog)>0);
            assertEquals(expectedResult[0], Log.v(testLog, "")>0);
            assertEquals(expectedResult[0], Log.v(testLog, "", exception)>0);

            assertEquals(expectedResult[1], Log.d(testLog)>0);
            assertEquals(expectedResult[1], Log.d(testLog, "")>0);
            assertEquals(expectedResult[1], Log.d(testLog, "", exception)>0);

            assertEquals(expectedResult[2], Log.i(testLog)>0);
            assertEquals(expectedResult[2], Log.i(testLog, "")>0);
            assertEquals(expectedResult[2], Log.i(testLog, "", exception)>0);

            assertEquals(expectedResult[3], Log.w(testLog)>0);
            assertEquals(expectedResult[3], Log.w(testLog, "")>0);
            assertEquals(expectedResult[3], Log.w(testLog, "", exception)>0);

            assertEquals(expectedResult[4], Log.e(testLog)>0);
            assertEquals(expectedResult[4], Log.e(testLog, "")>0);
            assertEquals(expectedResult[4], Log.e(testLog, exception)>0);
            assertEquals(expectedResult[4], Log.e(testLog, "", exception)>0);

            try {
                assertEquals(expectedResult[5], Log.wtf(testLog) > 0);
                if (!BuildConfig.DEBUG) {
                    assertTrue(true);
                }
            } catch (Exception e) {
                assertTrue(BuildConfig.DEBUG);
            }
            try {
                assertEquals(expectedResult[5], Log.wtf(testLog, "") > 0);
                if (!BuildConfig.DEBUG) {
                    assertTrue(true);
                }
            } catch (Exception e) {
                assertTrue(BuildConfig.DEBUG);
            }
            try {
                assertEquals(expectedResult[5], Log.wtf(testLog, exception) > 0);
                if (!BuildConfig.DEBUG) {
                    assertTrue(true);
                }
            } catch (Exception e) {
                assertTrue(BuildConfig.DEBUG);
            }
            try {
                assertEquals(expectedResult[5], Log.wtf(testLog, "", exception) > 0);
                if (!BuildConfig.DEBUG) {
                    assertTrue(true);
                }
            } catch (Exception e) {
                assertTrue(BuildConfig.DEBUG);
            }

            // Check debuggable value
            if (logLevel == Log.NONE) {
                assertFalse(Log.isDebuggable());
            } else {
                assertTrue(Log.isDebuggable());
            }
        }

        // Test illegal log level
        int originalLogLevel = Log.getLogLevel();
        try {
            //noinspection WrongConstant
            Log.setLogLevel(101);
            assertFalse(true);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        assertEquals(Log.getLogLevel(), originalLogLevel);
    }

    @Test
    public void testGetStackTraceString() throws Exception {
        assertNotNull(Log.getStackTraceString(new Throwable("")));
        assertNotNull(Log.getStackTraceString(new Exception("Exception")));
    }
}
