package com.sotwtm.util;

import android.content.Context;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.text.TextUtils;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

/**
 * Test case for {@link ECLogcatUtil}
 * @author sheungon
 */
@RunWith(AndroidJUnit4.class)
public class ECLogcatUtilTest extends TestCase {

    static final String TAG = "ECLogcatUtilTest";

    @Before
    public void clearSettings() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        context.getSharedPreferences(ECLogcatUtil.SHARED_PREF_FILE_KEY, Context.MODE_PRIVATE).edit().clear().apply();
    }

    @Test
    public void testLogcat() throws Exception {

        Context context = InstrumentationRegistry.getTargetContext();
        ECLogcatUtil logcatUtil = ECLogcatUtil.getInstance(context);

        assertNotNull(logcatUtil);

        try {
            logcatUtil.resetLogcat();
            assertTrue(false);
        } catch (NullPointerException e) {
            assertTrue(true);
        }

        try {
            logcatUtil.startLogcat();
            assertTrue(false);
        } catch (NullPointerException e) {
            assertTrue(true);
        }

        try {
            logcatUtil.clearLogcat();
            assertTrue(false);
        } catch (NullPointerException e) {
            assertTrue(true);
        }

        String appRunByUser = ECLogcatUtil.getAppRunByUser(context);
        assertFalse(TextUtils.isEmpty(appRunByUser));
        assertNull(ECLogcatUtil.getLogcatPIDRunningBy(appRunByUser));
        assertFalse(ECLogcatUtil.isLogcatRunningBy(appRunByUser));

        File logFile = new File(Environment.getExternalStorageDirectory().getPath(), "log.txt");
        assertTrue(!logFile.isFile() || logFile.delete());
        logcatUtil.setLogcatDest(logFile);
        logcatUtil.setFilterLogTag(TAG);

        assertTrue(logcatUtil.startLogcat());
        Log.d(TAG, "This is a test log.");
        Thread.sleep(100);

        assertNotNull(ECLogcatUtil.getLogcatPIDRunningBy(appRunByUser));
        assertTrue(ECLogcatUtil.isLogcatRunningBy(appRunByUser));

        assertTrue(logFile.isFile());
        assertTrue(logcatUtil.stopLogcat());
        assertFalse(ECLogcatUtil.isLogcatRunningBy(appRunByUser));
    }
}