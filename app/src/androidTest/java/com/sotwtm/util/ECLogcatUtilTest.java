package com.sotwtm.util;

import android.content.Context;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

/**
 * Test case for {@link ECLogcatUtil}
 * @author sheungon
 */
@RunWith(AndroidJUnit4.class)
public class ECLogcatUtilTest extends TestCase {

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

        File logFile = new File(Environment.getExternalStorageDirectory().getPath(), "log.txt");
        assertTrue(!logFile.isFile() || logFile.delete());
        logcatUtil.setLogcatDest(logFile);

        assertTrue(logcatUtil.startLogcat());
        assertTrue(logFile.isFile());
        assertTrue(logcatUtil.stopLogcat());
    }
}